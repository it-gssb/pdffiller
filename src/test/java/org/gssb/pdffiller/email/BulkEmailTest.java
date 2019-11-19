package org.gssb.pdffiller.email;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
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
import org.gssb.pdffiller.excel.RowGroup;
import org.gssb.pdffiller.exception.UnrecoverableException;
import org.gssb.pdffiller.pdf.UnitOfWork;
import org.junit.Before;
import org.junit.Test;

public class BulkEmailTest {
   
   private final static String ROOT = "src/test/resources/2018";
   private final static String TEMPLATE1 = ROOT + "/sources/AATG Raw Score.pdf";
   
   private final static int RETRIES = 3;

   private ByteArrayOutputStream baos;
   private PrintStream printStream;
   private String bodyTemplateText;
   private MockProvider mockProvider = 
         new MockProvider(Type.TRANSPORT, "smtp", 
                          MockTransport.class.getCanonicalName(), "GSSB", "0.1");
   
   private List<MockTransport> usedTransports = new ArrayList<>();
   
   @Before
   public void setUp() throws Exception {
      setFailures(0,0,0,0, true); // reset to default no failures
      this.baos = new ByteArrayOutputStream();
      this.printStream = new PrintStream(baos, true, "UTF-8");
      
      this.bodyTemplateText = "The email body for student {{Name}}.";
   }
   
   private void setFailures(final int connectionFailures, 
                            final int conectionFailureDelay,
                            final int sendFailures,
                            final int sendFailureDelay, 
                            final boolean useMessagingException) {
         MockTransport.setConectionFailures(connectionFailures,
                                            conectionFailureDelay);
         MockTransport.setSendFailures(sendFailures, sendFailureDelay);
         MockTransport.setMessagingException(useMessagingException);
   }
   
   private BulkEmail createBulkEmail(final boolean oneRecipient) {
      AppProperties props = mock(AppProperties.class);
      when(props.getEmailSendRetries()).thenReturn(RETRIES);
      when(props.getEmailWaitTime()).thenReturn(10);
      when(props.getEmailTimeout()).thenReturn(100);
      if (oneRecipient) {
         when(props.getTargetEmailColumns()).thenReturn(Arrays.asList("email1"));
         when(props.getGroupColumns()).thenReturn(Arrays.asList("email1"));
      } else {
         when(props.getTargetEmailColumns()).thenReturn(Arrays.asList("email1", "email2"));
         when(props.getGroupColumns()).thenReturn(Arrays.asList("email1", "email2"));
      }
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
                  MockTransport mt = (MockTransport)t;
                  usedTransports.add(mt);
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
   
   private RowGroup createRowGroup(final String groupIdName,
                                   final String[] columns,
                                   final List<String> columnNames) {
      List<ExcelRow> rows = new ArrayList<>();
      rows.add(createExcelRow(columns));
      return new RowGroup(groupIdName, rows);
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

   private List<String> getAttachmentNamesFromMimeMultipart(
                                            final MimeMultipart mimeMultipart)
                        throws MessagingException, IOException{
     List<String> attachments = new ArrayList<>();
     int count = mimeMultipart.getCount();
     for (int i = 0; i < count; i++) {
         BodyPart bodyPart = mimeMultipart.getBodyPart(i);
         if (bodyPart.getContent() instanceof MimeMultipart){
            attachments.addAll(getAttachmentNamesFromMimeMultipart(
                                         (MimeMultipart)bodyPart.getContent()));
         } else if (bodyPart.getContent() instanceof InputStream &&
                    Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) ||
                    bodyPart.getFileName()!=null) {
               attachments.add(bodyPart.getFileName());
         } 
     }
     return attachments;
  }
   
   private List<String> getAttachmentNamesFromMessage(final Message message) 
                        throws MessagingException, IOException {
      List<String> result = Collections.emptyList();
      if (message.getContent() instanceof MimeMultipart) {
         MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
         result = getAttachmentNamesFromMimeMultipart(mimeMultipart);
      }
      return result;
   }
   
   private void sendEmails(final int emailCount, boolean oneRecipient) {
      String[] rowDesc;
      if (oneRecipient) {
         rowDesc = new String[] { "email1:p1@domain1.ccc", 
                                  "Name:Student Name 1" };
      } else {
         rowDesc = new String[] { "email1:p1@domain1.ccc", 
                                  "email2:p2@domain2.ccc",
                                  "Name:Student Name 1" };
      }
      
      File attachment1 = new File(TEMPLATE1);
      
      RowGroup rowGroup;
      if (oneRecipient) {
         rowGroup = createRowGroup(null, rowDesc, Arrays.asList("email1"));
      } else {
         rowGroup = createRowGroup("email1", rowDesc,
                                   Arrays.asList("email1", "email2"));
      }
      
      List<UnitOfWork> work = new ArrayList<>();
      for (int i=0; i< emailCount; i++) {
         UnitOfWork uow = mock(UnitOfWork.class);
         when(uow.getGeneratedFiles()).thenReturn(Arrays.asList(attachment1));
         when(uow.getRow()).thenReturn(rowGroup);
         work.add(uow);
      }
      
      String host = "localhost";
      String port = "0";
      String userName = "user";
      String fromAddress = "from@userdomain.abc";
      String password = "abc";
      
      BulkEmail bulkEmail = createBulkEmail(oneRecipient);
      
      bulkEmail.sendEmails(work, false, host, port, userName, fromAddress, 
                           password, bodyTemplateText);
   }
   
   @Test
   public void testExpectedUseCase() throws MessagingException, IOException {
      try {
         sendEmails(1, false);
      } catch (UnrecoverableException e) {
         fail("unexpected error ocured when sending email" + e.getMessage());
      }
      
      assertEquals(1, this.usedTransports.size());
      
      // message is not set if sending of message is skipped die to some error conditions
      assertNotNull(this.usedTransports.get(0).getLastMessage());
      
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

      List<String> attachments = getAttachmentNamesFromMessage(
                                         this.usedTransports.get(0)
                                                            .getLastMessage());
      assertEquals(1, attachments.size());
      assertEquals("AATG Raw Score.pdf", attachments.get(0));
      
   }

   @Test
   public void testTwoFailuresConnectingToEmailServer() 
               throws MessagingException, IOException {
      setFailures(2,0,0,0, true);
      try {
         sendEmails(1, false);
      } catch (UnrecoverableException e) {
         fail("unexpected error ocured when sending email" + e.getMessage());
      }
      String data = new String(baos.toByteArray(), StandardCharsets.UTF_8);
      assertTrue(data.contains("RR."));     
   }
   
   @Test
   public void testThreeFailuresConnectingToEmailServer() 
               throws MessagingException, IOException {
      setFailures(RETRIES+2,0,0,0, true);
      try {
         sendEmails(1, false);
      } catch (UnrecoverableException e) {
         assertTrue(e.getCause() instanceof MessagingException);
         assertEquals("Unable to connect to email server after 3 retries.",
                      e.getMessage());
      }
      String data = new String(baos.toByteArray(), StandardCharsets.UTF_8);
      assertTrue(data.contains("RRR"));
      assertEquals(RETRIES, this.usedTransports.size());
   }
   
   @Test
   public void testProviderFailure() 
               throws MessagingException, IOException {
      setFailures(RETRIES+2,0,0,0, false);
      try {
         sendEmails(1, false);
      } catch (UnrecoverableException e) {
         assertTrue(e.getCause() instanceof MessagingException);
         assertEquals("Failure to connect to Email server.", e.getMessage());
      }
      assertEquals(1, this.usedTransports.size());
   }
   
   @Test
   public void testTwoFailedEmailDeliveries() 
               throws MessagingException, IOException {
      setFailures(0,0,2,0, true);
      try {
         sendEmails(1, false);
      } catch (UnrecoverableException e) {
         fail("unexpected error ocured when sending email" + e.getMessage());
      }
      String data = new String(baos.toByteArray(), StandardCharsets.UTF_8);
      assertEquals(3, this.usedTransports.size());
      assertTrue(data.contains("rr."));
   }
   
   @Test
   public void testThreeFailedEmailDeliveries() 
               throws MessagingException, IOException {
      setFailures(0,0,RETRIES+2,0, true);
      try {
         sendEmails(1, false);
      } catch (UnrecoverableException e) {
         assertTrue(e.getCause() instanceof BulkEmailException);
         assertTrue(e.getCause().getCause() instanceof MessagingException);
         assertEquals("Unable to send email for group ID p1@domain1.ccc after 3 atempts.",
                      e.getMessage());
      }
      String data = new String(baos.toByteArray(), StandardCharsets.UTF_8);
      assertEquals(RETRIES, this.usedTransports.size());
      assertTrue(data.contains("rrr"));
   }
   
   @Test
   public void testSendThreeEmailsSuccess() throws MessagingException, IOException {
      try {
         sendEmails(3, false);
      } catch (UnrecoverableException e) {
         fail("unexpected error ocured when sending email" + e.getMessage());
      }
      String data = new String(baos.toByteArray(), StandardCharsets.UTF_8);
      assertTrue(data.contains("..."));
      assertEquals(1, this.usedTransports.size());
   }
   
   @Test
   public void testTwoSuccessAndThirdWithDeliveryFailure() 
               throws MessagingException, IOException {
      setFailures(0,0,RETRIES-1,2, true);
      try {
         sendEmails(3, false);
      } catch (UnrecoverableException e) {
         fail("unexpected error ocured when sending email" + e.getMessage());
      }
      String data = new String(baos.toByteArray(), StandardCharsets.UTF_8);
      assertTrue(data.contains("..rr."));
      assertEquals(3, this.usedTransports.size());
   }
   
   @Test
   public void testThreeEmailsOneAddressNotDelivable() 
               throws MessagingException, IOException {
      setFailures(0,0,1,2, false);
      try {
         sendEmails(3, false);
      } catch (UnrecoverableException e) {
         fail("unexpected error ocured when sending email" + e.getMessage());
      }
      String data = new String(baos.toByteArray(), StandardCharsets.UTF_8);
      assertTrue(data.contains("..a"));     
   }
   
   @Test
   public void ThreeEmailsAllAddressNotDelivable() 
               throws MessagingException, IOException {
      setFailures(0,0,1,2, false);
      try {
         // all recipient emails will fail to deliver
         sendEmails(3, true);
      } catch (UnrecoverableException e) {
         e.printStackTrace();
         fail("unexpected error ocured when sending email" + e.getMessage());
      }
      String data = new String(baos.toByteArray(), StandardCharsets.UTF_8);
      assertTrue(data.contains("..A"));     
   }
   
}
