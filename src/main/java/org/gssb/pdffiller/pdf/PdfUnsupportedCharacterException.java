package org.gssb.pdffiller.pdf;

import org.apache.pdfbox.pdmodel.interactive.form.PDField;

class PdfUnsupportedCharacterException extends RuntimeException {

   private static final long serialVersionUID = 9083465551819030174L;
   
   private final PDField formField;
   private final String value;
   
   PdfUnsupportedCharacterException(final String message, final Throwable t,
                                    final PDField formField, final String value) {
      super(message, t);
      this.formField = formField;
      this.value = value;
   }

   public PDField getFormField() {
      return this.formField;
   }

   public String getValue() {
      return this.value;
   }
   
}
