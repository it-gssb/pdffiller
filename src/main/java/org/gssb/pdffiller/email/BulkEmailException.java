package org.gssb.pdffiller.email;

public class BulkEmailException extends RuntimeException {

   private static final long serialVersionUID = -8818575400686010419L;
   
   private final int errorMessageIndex;
   private final String groupId;

   public BulkEmailException(final String message, 
                             final int errorMessageIndex,
                             final String groupId) {
      super(message);
      this.errorMessageIndex = errorMessageIndex;
      this.groupId = groupId;
   }
   
   public BulkEmailException(final String message, final Throwable t,
                             final int errorMessageIndex,
                             final String groupId) {
      super(message, t);
      this.errorMessageIndex = errorMessageIndex;
      this.groupId = groupId;
   }

   public int getErrorMessageIndex() {
      return this.errorMessageIndex;
   }

   public String getGroupId() {
      return this.groupId;
   }
   
}
