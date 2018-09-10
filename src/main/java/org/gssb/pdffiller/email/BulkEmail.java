package org.gssb.pdffiller.email;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gssb.pdffiller.config.AppProperties;
import org.gssb.pdffiller.excel.ExcelRow;
import org.gssb.pdffiller.exception.UnrecoverableException;
import org.gssb.pdffiller.pdf.UnitOfWork;
import org.gssb.pdffiller.text.TextBuilder;

import com.github.mustachejava.MustacheException;

public class BulkEmail {

   private final static Logger logger = LogManager.getLogger(BulkEmail.class);

   private final static String EOL = System.getProperty("line.separator");
   
   private static final String CHARACTER_SET = "utf-8";

   private static final String MISSING_COLUMN =
         "The spreadsheet does not contain the columns %s that define " +
         "target email addresses.";

   private static final String CREATE_SUBJECT_ERROR = 
         "An unexpected error occured when creating the subject text " +
         "of the email message to be sent to %s.";

   private static final String CREATE_SUBJECT_SUB_ERROR = 
         "The subject of the email to be sent to %s contains an " +
         "undefined variable.";
   
   private static final String CREATE_BODY_ERROR = 
         "An unexpected error occured when creating the body text of " +
         "the email message to be sent to %s.";
   
   private static final String CREATE_BODY_SUB_ERROR = 
         "The email message to be sent to %s contains an undefined variable.";

   private static final String UNKNOWN_ERROR =
         "Email server was unable to deliver message in record %s to %s " +
         "due to an unknown issue. The email was not sent.";

   private static final String SERVER_ERROR = 
         "Email server was unable to deliver message of record %s to %s. " +
         "The email was not sent.";
   
   private static final String CONNECTION_ERROR =
         "Unable to connect to email server after %s retries.";
   
   private static final String EMAIL_SENT =
         "Sent message of record %s to email addresses %s.";

   private static final String SEND_ERROR = 
         "Emails sent to '%s' and emails not delivered to '%s'.";

   private final PrintStream outstream;
   
   private final String emailSendProtocol;
   
   private final int retries;
   private final int waitTime;
   private final int timeout;
   
   private final String subjectTemplate;
   
   private final List<String> emailColumns;

   BulkEmail(final AppProperties props, final PrintStream outstream) {
      super();
      this.retries = props.getEmailSendRetries();
      this.waitTime = props.getEmailWaitTime();
      this.timeout = props.getEmailTimeout();
      this.emailColumns = props.getTargetEmailColumns();
      this.subjectTemplate = props.getEmailSubjectMessage();
      this.emailSendProtocol = "smtp";
      this.outstream = outstream;
   }
   
   public BulkEmail(final AppProperties props) {
      this(props, System.out);
   }
   
   private void printProgress(final int count, final char character) {
      if (count % 100 == 0) {
         this.outstream.println(character);
      } else {
         this.outstream.print(character);
      }
   }

   private Properties setEmailProperties(final String host, 
                                         final String port,
                                         final String userName,
                                         final String password) {
      Properties properties = new Properties();
      properties.put("mail.smtp.host", host);
      properties.put("mail.smtp.port", port);

      properties.put("mail.smtp.timeout", this.timeout);
      properties.put("mail.smtp.connectiontimeout", this.timeout);

      properties.put("mail.smtp.auth", "true");
      properties.put("mail.smtp.starttls.enable", "true");

      properties.put("mail.user", userName);
      properties.put("mail.password", password);
      return properties;
   }

   // protected to  enable testing of failure cases
   protected Session createSession(final String host, final String port,
                                   final String userName, final String password) {
      // sets SMTP server properties
      Properties properties = setEmailProperties(host, port, userName, password);

      // creates a new session with an authenticator
      Authenticator auth = new Authenticator() {
         public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(userName, password);
         }
      };

      return Session.getInstance(properties, auth);
   }

   private Optional<Message> createMessage(final Session session,
                                           final String userName, 
                                           final String fromAddress,
                                           final List<String> recipients,
                                           final String subject,
                                           final String body,
                                           final List<File> attachedFiles)
                            throws MessagingException {
      MimeMessage msg = new MimeMessage(session);

      try {
         msg.setFrom(new InternetAddress(userName, fromAddress));
      } catch (UnsupportedEncodingException e) {
         logger.error("Email address incorrectly formed for " + userName +
                      " or " + fromAddress + ". " + "Email was not sent.");
         return Optional.empty();
      }

      InternetAddress[] toAddresses = new InternetAddress[recipients.size()];
      for (int i = 0; i < recipients.size(); i++) {
         toAddresses[i] = new InternetAddress(recipients.get(i));
      }

      msg.setRecipients(Message.RecipientType.TO, toAddresses);
      
      msg.setSubject(subject, CHARACTER_SET);
      msg.setSentDate(new Date());

      // creates message part
      MimeBodyPart messageBodyPart = new MimeBodyPart();
      messageBodyPart.setContent(body, "text/plain; charset=" + CHARACTER_SET);

      // creates multi-part
      Multipart multipart = new MimeMultipart();
      multipart.addBodyPart(messageBodyPart);

      // adds attachments
      for (File attachedFile : attachedFiles) {
         if (attachedFile != null) {
            MimeBodyPart attachPart = new MimeBodyPart();

            try {
               attachPart.attachFile(attachedFile);
            } catch (IOException ex) {
               logger.error("File " + attachedFile.toString() + 
                            " was not found and could not be attached. " +
                            "Email was not sent.", ex);
               return Optional.empty();
            }

            multipart.addBodyPart(attachPart);
         } else {
            logger.error("No attachment found for recipients. " +
                         "Email was not sent.");
            return Optional.empty();
         }
      }

      // sets the multi-part as e-mail's content
      msg.setContent(multipart);

      return Optional.of(msg);
   }
   
   // factory method - for test purposes only!
   protected Transport createTransport(final Session session,
                                       final String protocol)
                       throws NoSuchProviderException {
      return session.getTransport(protocol);
   }

   private Transport getTransport(final Session session, final String protocol) {
      Exception lastException = null;
      int count = 0;
      while (count < this.retries) {
         try {
            Transport t = createTransport(session, protocol);
            t.connect();
            return t;
         } catch (NoSuchProviderException e) {
            String msg = "Failure to connect to Email server.";
            logger.error(msg, e);
            throw new UnrecoverableException(msg, e);
         } catch (MessagingException e) {
            logger.debug("Failure to connect to email server.", e);
            lastException = e;
         } catch (RuntimeException e) {
            logger.debug("Unknown issue prevented connection to email server.", e);
            lastException = e;
         }
         count++;
         printProgress(count, 'R');
         try {
            Thread.sleep(this.waitTime); // sleep a bit before retrying
         } catch (InterruptedException e) {
            logger.debug("Done waiting before attempting to connect to email server.");
         }
      }
      String msg = String.format(CONNECTION_ERROR, retries);
      logger.error(msg, lastException);
      throw new UnrecoverableException(msg, lastException);
   }

   private String getAddressList(final Address[] addresses)
                  throws MessagingException {
      return Arrays.asList(addresses).stream()
                                     .map(a -> a.toString())
                                     .collect(Collectors.joining(", "));
   }

   private String getRecipients(final Message message) {
      String addresses;
      try {
         addresses = getAddressList(message.getAllRecipients());
      } catch (MessagingException e) {
         logger.warn("Unable to get email addresses from message.", e);
         addresses = "unknown email addresses";
      }
      return addresses;
   }
   
   private String getRecipients(final Address[] addresses) {
      String addressText;
      try {
         addressText = getAddressList(addresses);
      } catch (MessagingException e) {
         logger.warn("Unable to get email addresses from message.", e);
         addressText = "unknown email addresses";
      }
      return addressText;
   }

   private int sendMessage(final Transport t, final Message message,
                           final int index, final int alreadyProcessed) {
      int successCode = 0; // no error
      try {
         message.saveChanges();
         t.sendMessage(message, message.getAllRecipients());
      } catch (SendFailedException e) {
         String msg = String.format(SEND_ERROR, 
                                    getRecipients(e.getValidSentAddresses()),
                                    getRecipients(e.getValidUnsentAddresses()));
         logger.error(msg, e);
         successCode = 1; // unable to send to at least one address
      } catch (MessagingException e) {
         String msg = String.format(SERVER_ERROR, alreadyProcessed + index,
                                    getRecipients(message));
         logger.error(msg, e);
         throw new BulkEmailException(msg, e, index);
      } catch (RuntimeException e) {
         String msg = String.format(UNKNOWN_ERROR, alreadyProcessed + index,
                                    getRecipients(message));
         logger.error(msg, e);
         throw new UnrecoverableException(msg, e);
      }
      return successCode; 
   }
   
   private int sendMessages(final Session session,
                            final List<Message> messages,
                            final int alreadyProcessed) {
      int processed = 0;
      int sentEmails = 0;
      try (Transport t = getTransport(session, this.emailSendProtocol)) {
         for (int i=0; i < messages.size(); i++) {
            Message message = messages.get(i);
            int successCode = sendMessage(t, message, i, alreadyProcessed);
            processed++;
            if (successCode == 0) {
               sentEmails++;
               printProgress(alreadyProcessed + processed, '.');
               try {
                  String msg = String.format(EMAIL_SENT, alreadyProcessed + i,
                                             getAddressList(message.getAllRecipients()));
                  logger.info(msg);
               } catch (MessagingException e) {
                  logger.debug("Could not log email address for record " + 
                               sentEmails);
               }
            } else {
               // at least one email recipient was not reached
               printProgress(alreadyProcessed + processed, 'a');
            }
         }
      } catch (MessagingException e) {
         logger.warn("Unable to close email transportation layer.", e);
      } 
      return sentEmails;
   }

   private int sendMessagesWithRetry(final Session session,
                                      final List<Message> messages) {
      int emailsSent = 0;
      Exception lastException = null;
      int retriedCount = 0;
      int messageIndex = 0;
      while (retriedCount < this.retries) {
         try {
            emailsSent += sendMessages(session, messages.subList(messageIndex,
                                                                 messages.size()),
                                       messageIndex);
            // exit retry loop after messages were sent successfully 
            return emailsSent;
         } catch (BulkEmailException e) {
            int failedIndex = e.getErrorMessageIndex();
            if (failedIndex == 0) {
               // increment retry count only if no message was sent
               retriedCount++;
            }
            // increment message index to failed message to prepare retry
            messageIndex += failedIndex;
            emailsSent += failedIndex; // TODO - approximation 
         }
         printProgress(messageIndex, 'r');
         try {
            Thread.sleep(this.waitTime); // sleep a bit before retrying
         } catch (InterruptedException e) {
            logger.debug("Done waiting before attempting to connect to email server.");
         }
      }
      String msg = "Unable to send email after " + retries + " retries.";
      logger.error(msg, lastException);
      throw new UnrecoverableException(msg, lastException);
   }

   private String getTextFromMessage(Message message) 
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

  private String getTextFromMimeMultipart(final MimeMultipart mimeMultipart)
          throws MessagingException, IOException{
      StringBuffer sb = new StringBuffer();
      int count = mimeMultipart.getCount();
      for (int i = 0; i < count; i++) {
          BodyPart bodyPart = mimeMultipart.getBodyPart(i);
          if (bodyPart.getContent() instanceof MimeMultipart){
             sb.append(getTextFromMimeMultipart((MimeMultipart)bodyPart.getContent()));
          } else if (bodyPart.getContent() instanceof InputStream) {
             if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) ||
                 bodyPart.getFileName()!=null) {
            	 sb.append(EOL)
            	   .append(bodyPart.getFileName());
            } else {
                return "";
            }
          } else if (bodyPart.isMimeType("text/plain")) {
	        	 sb.append(EOL)
	        	   .append(bodyPart.getContent());
          } else if (bodyPart.isMimeType("text/html")) {
              sb.append(EOL)
                .append((String) bodyPart.getContent());
          }
      }
      return sb.toString();
   }
   
   private String renderMessage(final Message message) {
      StringBuilder sb = new StringBuilder();
      try {
         sb.append(EOL)
           .append("FROM: ")
           .append(getAddressList(message.getFrom()))
           .append(EOL)
           .append("TO: ")
           .append(getRecipients(message))
           .append(EOL).append("RE: ")
           .append(message.getSubject())
           .append(EOL)
           .append(getTextFromMessage(message))
           .append(EOL);
      } catch (MessagingException | IOException e) {
         logger.error("Unable to render the email message.", e);
      }
      return sb.toString();
   }
   
   private List<String> getValidEmailAddresses(final ExcelRow row) {
      String missing = this.emailColumns
                           .stream()
                           .filter(c -> row.getRow().get(c)==null)
                           .collect(Collectors.joining(", "));
      if (!missing.isEmpty()) {
         String msg = String.format(MISSING_COLUMN, missing);
         throw new UnrecoverableException(msg);
      }
      
      return this.emailColumns
                 .stream()
                 .map(c -> row.getValue(c.trim()).getColumnValue())
                 .filter(v -> v!=null && v.contains("@"))
                 .collect(Collectors.toList());
   }

   private String createSubjectMessage(final TextBuilder textBuilder, 
                                       final ExcelRow row,
                                       final String recipients) {
      try {
         return textBuilder.substitute(subjectTemplate, row.getRowMap());
      } catch (IOException e) {
         String msg = String.format(CREATE_SUBJECT_ERROR, recipients);
         logger.error(msg, e);
         throw new UnrecoverableException(msg, e);
      } catch(MustacheException e) {
         String msg = String.format(CREATE_SUBJECT_SUB_ERROR, recipients) +
                                    e.getMessage();
         logger.error(msg, e);
         throw new UnrecoverableException(msg, e);
      }
   }
   
   private String createEmailBodyMessage(final TextBuilder textBuilder,
                                         final String bodyTemplateText,
                                         final ExcelRow row,
                                         final String recipients) {
      try {
         return textBuilder.substitute(bodyTemplateText, row.getRowMap());
      } catch (IOException e) {
         String msg = String.format(CREATE_BODY_ERROR, recipients);
         logger.error(msg, e);
         throw new UnrecoverableException(msg, e);
      } catch(MustacheException e) {
         String msg = String.format(CREATE_BODY_SUB_ERROR, recipients) +
                      e.getMessage();
         logger.error(msg, e);
         throw new UnrecoverableException(msg, e);
      }
   }

   public void sendEmails(final List<UnitOfWork> createdUnits,
                          final boolean simulate,
                          final String host, 
                          final String port, 
                          final String userName,
                          final String fromAddress,
                          final String password, 
                          final String bodyTemplateText) {
      Session session = createSession(host, port, userName, password);
      List<Message> messages = new ArrayList<>();

      TextBuilder textBuilder = new TextBuilder();
      
      this.outstream.println();
      for (UnitOfWork unit : createdUnits) {
         ExcelRow row = unit.getRow();
         List<File> attachedFiles = unit.getGeneratedFiles();
         
         List<String> emails = getValidEmailAddresses(row);
         if (emails.isEmpty()) {
            logger.warn("No email sent for '" +
                        row.getValue("Name").getColumnValue() + 
                        "' because the required email address was unavailable.");
            continue;
         }
         String recipients = emails.stream() 
                                   .collect(Collectors.joining(","));

         boolean allFound = attachedFiles.stream()
                                         .allMatch(f -> f.exists());
         if (!allFound) {
            String fileNames =
                  attachedFiles.stream()
                               .map(f -> f.getName())
                               .collect(Collectors.joining(", "));
            logger.warn("Files '" + fileNames + "' not found. " + 
                        "Skip sending email.");
            continue;
         }
         
         String subject = createSubjectMessage(textBuilder, row, recipients);
         String message = createEmailBodyMessage(textBuilder, bodyTemplateText,
                                                 row, recipients);
         Optional<Message> msg;
         try {
            msg = createMessage(session, userName, fromAddress, emails,
                                subject, message, attachedFiles);
            if (!msg.isPresent()) {
               continue;
            }
         } catch (MessagingException e) {
            logger.error("Unable to create message", e);
            continue;
         }
         messages.add(msg.get());
      }

      // send messages
      if (simulate) {
         for (Message m : messages) {
            logger.info(renderMessage(m));
         }
      } else {
         int emailsSent = sendMessagesWithRetry(session, messages);
         this.outstream.println();
         this.outstream.println("Sent " + emailsSent + " of " +
                                messages.size() + " possible emails.");
      }
   }

}
