package org.gssb.pdffiller.excel;

public class ColumnNotFoundException extends RuntimeException {
   
   private static final long serialVersionUID = 3947489103577453763L;

   public ColumnNotFoundException(final String message) {
      super(message);
   }
}
