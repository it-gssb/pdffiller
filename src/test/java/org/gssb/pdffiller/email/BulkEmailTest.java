package org.gssb.pdffiller.email;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Provider.Type;
import javax.mail.internet.MimeMultipart;
import javax.mail.Session;
import javax.mail.Transport;

import org.gssb.pdffiller.config.AppProperties;
import org.gssb.pdffiller.excel.ExcelCell;
import org.gssb.pdffiller.excel.ExcelRow;
import org.gssb.pdffiller.pdf.UnitOfWork;
import org.junit.Before;
import org.junit.Test;

public class BulkEmailTest {

   private PrintStream printStream;
   private String bodyTemplateText;
   private MockProvider mockProvider = 
         new MockProvider(Type.TRANSPORT, "smtp", 
                          MockTransport.class.getCanonicalName(), "UGBU", "0.1");
   
   private List<MockTransport> usedTransports = new ArrayList<>();
   
   @Before
   public void setUp() throws Exception {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      this.printStream = new PrintStream(baos, true, "UTF-8");
      
      this.bodyTemplateText = "The email body for student {{Name}}.";
   }
   
   private BulkEmail createBulkEmail() {
      AppProperties props = mock(AppProperties.class);
      when(props.getEmailSendRetries()).thenReturn(100);
      when(props.getEmailWaitTime()).thenReturn(10);
      when(props.getEmailTimeout()).thenReturn(100);
      when(props.getTargetEmailColumns()).thenReturn(Arrays.asList("email1", "email2"));
      when(props.getEmailSubjectMessage()).thenReturn("German Saturday School Boston for {{Name}}");
      when(props.getSourceFolder()).thenReturn("sources");
      
      return new BulkEmail(props, this.printStream) {
               @Override
               protected Session createSession(final String host, 
                                               final String port,
                                               final String userName,
                                               final String password) {
                  Session session = super.createSession(host, port, userName,
                                                        password);
                  try {
                     session.setProvider(mockProvider);
                  } catch (NoSuchProviderException e) {
                     fail("Unexpected exception" + e.getMessage());
                  }
                  return session;
               }
         
               // store Transport instances used in a session
               @Override
               protected Transport createTransport(final Session session,
                                                   final String protocol)
                                   throws NoSuchProviderException {
                  Transport t = super.createTransport(session, protocol);
                  usedTransports.add((MockTransport)t);
                  return t;
               }
      };
   }
   
   private ExcelRow createExcelRow(final String[] columns) {
      int index = 1;
      ExcelRow excelRow = new ExcelRow();
      for (String column : columns) {
         String[] keyValue = column.split(":");
         assert(keyValue.length==2);
         
         ExcelCell cell = new ExcelCell(index++, keyValue[0], keyValue[1]);
         excelRow.addExcelCell(cell);
      }
      return excelRow;
   }
   
   private String getTextFromMimeMultipart(final MimeMultipart mimeMultipart)
         throws MessagingException, IOException{
     StringBuffer sb = new StringBuffer();
     int count = mimeMultipart.getCount();
     for (int i = 0; i < count; i++) {
         BodyPart bodyPart = mimeMultipart.getBodyPart(i);
         if (bodyPart.getContent() instanceof MimeMultipart){
            sb.append(getTextFromMimeMultipart((MimeMultipart)bodyPart.getContent()));
         } else if (bodyPart.isMimeType("text/plain")) {
            sb.append(bodyPart.getContent());
         } else if (bodyPart.isMimeType("text/html")) {
             sb.append((String) bodyPart.getContent());
         }
     }
     return sb.toString();
  }
   
   private String getTextFromMessage(final Message message) 
         throws MessagingException, IOException {
    String result = "";
    if (message.getContent() instanceof MimeMultipart) {
        MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
        result = getTextFromMimeMultipart(mimeMultipart);
    } else if (message.isMimeType("text/plain")) {
       result = message.getContent().toString();
   } 
    return result;
}
   
   @Test
   public void test() throws MessagingException, IOException {
      String[] rowDesc = new String[] { 
            "email1:p1@domain1.ccc", "email2:p2@domain2.ccc",
            "Name:Student Name 1" };
      
      UnitOfWork uow = mock(UnitOfWork.class);
      when(uow.getGeneratedFiles()).thenReturn(Collections.emptyList());
      when(uow.getRow()).thenReturn(createExcelRow(rowDesc));
      
      List<UnitOfWork> work = Arrays.asList(uow);
      String host = "localhost";
      String port = "0";
      String userName = "user";
      String fromAddress = "from@userdomain.abc";
      String password = "abc";
      
      BulkEmail bulkEmail = createBulkEmail();
      
      bulkEmail.sendEmails(work, false, host, port, userName, fromAddress, 
                           password, bodyTemplateText);
      
      assertEquals(1, this.usedTransports.size());
      
      Address[] from = this.usedTransports.get(0)
                           .getLastMessage().getFrom();
      assertEquals(1, from.length);
      assertEquals("\"from@userdomain.abc\" <user>", from[0].toString());
      
      Address[] addresses = this.usedTransports.get(0)
                                .getLastMessage().getAllRecipients();
      assertEquals(2, addresses.length);
      assertEquals("p1@domain1.ccc", addresses[0].toString());
      assertEquals("p2@domain2.ccc", addresses[1].toString());
      
      assertEquals("German Saturday School Boston for Student Name 1",
                   this.usedTransports.get(0).getLastMessage().getSubject());
      
      assertEquals("The email body for student Student Name 1.",
                   getTextFromMessage(this.usedTransports.get(0).getLastMessage()));

   }

}
