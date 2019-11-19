package org.gssb.pdffiller.pdf;

import javax.mail.Message;

public class MessageWrapper {
   
   private final String groudId;
   private final Message message;
   
   public MessageWrapper(final String groudId, final Message message) {
      super();
      assert(message!=null);
      this.groudId = groudId;
      this.message = message;
   }

   public String getGroudId() {
      return groudId;
   }

   public Message getMessage() {
      return message;
   }

}
