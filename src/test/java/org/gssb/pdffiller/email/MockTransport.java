package org.gssb.pdffiller.email;

import java.net.SocketTimeoutException;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Mock implementation of {@link Transport} for unit testing. See
 * http://sujitpal.blogspot.com/2006/12/mock-objects-for-javamail-unit-tests.html
 * for more information. This implementation stores the last message sent and
 * adds a getter for retrieval and verification.
 */
public class MockTransport extends Transport {
   
   private static final Logger logger =
                        LogManager.getLogger(MockTransport.class);
   
   private static int connectionFailures = 0;
   private static int conectionFailureDelay = 0;
   private static int sendFailures = 0;
   private static int sendFailureDelay = 0;
   
   private static boolean isMessagingException = true;

   private Message lastMessage;

   public MockTransport(Session session, URLName urlName) {
      super(session, urlName);
      logger.warn("constructed MockTransport instance - javamail is mocked");
   }
   
   public static void setMessagingException(final boolean messaging) {
      MockTransport.isMessagingException = messaging;
   }
   
   public static void setConectionFailures(final int connectionFailures, 
                                    final int conectionFailureDelay) {
      MockTransport.connectionFailures = connectionFailures;
      MockTransport.conectionFailureDelay = conectionFailureDelay;
   }
   
   public static void setSendFailures(final int sendFailures,
                               final int sendFailureDelay) {
      MockTransport.sendFailures = sendFailures;
      MockTransport.sendFailureDelay = sendFailureDelay;
   }
   
   private void checkConnection() throws MessagingException {
      if (MockTransport.conectionFailureDelay>0) {
         MockTransport.conectionFailureDelay--;
         return;
      }
      // no further delays; create failure
      if (MockTransport.connectionFailures>0) {
         MockTransport.connectionFailures--;
         if (MockTransport.isMessagingException) {
            throw new MessagingException("cannot connect"); 
         } else {
            throw new NoSuchProviderException("missing provider");
         }
      }
   }
   
   private void checkSendMessage(final Address[] targetEmails) 
                throws MessagingException {
      assert(targetEmails.length>0);
      if (MockTransport.sendFailureDelay>0) {
         MockTransport.sendFailureDelay--;
         return;
      }
      // no further delays; create exception
      if (MockTransport.sendFailures>0) {
          MockTransport.sendFailures--;
          if (MockTransport.isMessagingException) {
             SocketTimeoutException ex =
                   new SocketTimeoutException("Read timed out");
             throw new MessagingException("unable to send message",  ex);
          } else {
             Address[] sent      = new Address[targetEmails.length-1];
             Address[] unsent    = new Address[1];
             Address[] incorrect = new Address[0];
             
             if (sent.length>0) {
                System.arraycopy(targetEmails, 0, sent, 0, targetEmails.length-1);
             }
             unsent[0] = targetEmails[targetEmails.length-1];
             
             SendFailedException ex = new SendFailedException("unable to send message");
             throw new SendFailedException("cannot send all emails to destination",
                                           ex, sent, unsent, incorrect);
          }
      }
   }
   

   public Message getLastMessage() {
      return lastMessage;
   }
   
   //
   // mock implementation of Transport class
   //

   /**
    * Stores the message to send in this instance.
    */
   @Override
   public void sendMessage(Message arg0, Address[] arg1)
               throws MessagingException {
      checkSendMessage(arg1);

      String subject = arg0.getSubject();
      StringBuffer addresses = new StringBuffer("[");
      for (int i=0; i<arg1.length; i++) {
         addresses.append(arg1[i]);
         if (i<arg1.length-1) addresses.append(",");
      }
      addresses.append("]");
      logger.debug("sendMessage(\"{}\",{})", subject, addresses.toString());
      lastMessage = arg0;
   }
   

   @Override
   public void connect() throws MessagingException {
      checkConnection();
      logger.debug("connect()");
   }

   @Override
   public void connect(String arg0, int arg1, String arg2, String arg3)
               throws MessagingException {
      checkConnection();
      logger.debug("connect({},{},{},{})", new Object[] {arg0,arg1,arg2,arg3});
   }

   @Override
   public void connect(String arg0, String arg1, String arg2)
               throws MessagingException {
      checkConnection();
      logger.debug("connect({},{},{})", new Object[] {arg0,arg1,arg2});
   }

   @Override
   public void connect(String arg0, String arg1) throws MessagingException {
      checkConnection();
      logger.debug("connect({},{})", arg0,arg1);
   }

   @Override
   public synchronized void close() throws MessagingException {
      logger.debug("close()");
   }

}
