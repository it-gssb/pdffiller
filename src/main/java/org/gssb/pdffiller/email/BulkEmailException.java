package org.gssb.pdffiller.email;

public class BulkEmailException extends RuntimeException {

   private static final long serialVersionUID = -8818575400686010419L;
   
   private final int errorMessageIndex;

   public BulkEmailException(final String message, 
                             final int errorMessageIndex) {
      super(message);
      this.errorMessageIndex = errorMessageIndex;
   }
   
   public BulkEmailException(final String message, final Throwable t,
                                    final int errorMessageIndex) {
      super(message, t);
      this.errorMessageIndex = errorMessageIndex;
   }

   public int getErrorMessageIndex() {
      return this.errorMessageIndex;
   }
   
}
