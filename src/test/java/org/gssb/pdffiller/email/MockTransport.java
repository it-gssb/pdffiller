package org.gssb.pdffiller.email;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
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
   
   private static final Logger logger = LogManager.getLogger(MockTransport.class);

   private Message lastMessage;

   public MockTransport(Session session, URLName urlName) {
      super(session, urlName);
      logger.warn("constructed MockTransport instance - javamail is mocked");
   }

   /**
    * Stores the message to send in this instance.
    */
   @Override
   public void sendMessage(Message arg0, Address[] arg1)
         throws MessagingException {

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
      logger.debug("connect()");
   }

   @Override
   public void connect(String arg0, int arg1, String arg2, String arg3)
         throws MessagingException {
      logger.debug("connect({},{},{},{})", new Object[] {arg0,arg1,arg2,arg3});
   }

   @Override
   public void connect(String arg0, String arg1, String arg2)
         throws MessagingException {
      logger.debug("connect({},{},{})", new Object[] {arg0,arg1,arg2});
   }

   @Override
   public void connect(String arg0, String arg1) throws MessagingException {
      logger.debug("connect({},{})", arg0,arg1);
   }

   @Override
   public synchronized void close() throws MessagingException {
      logger.debug("close()");
   }

   public Message getLastMessage() {
      return lastMessage;
   }

}
