package org.gssb.pdffiller.pdf;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.text.PDFTextStripper;

public class PDFValidator {

   public PDFValidator() {
      super();
   }

   protected void validatePDFDocument(final File templatePdf, 
                                      final String password,
                                      final List<String> expected)
                  throws InvalidPasswordException, IOException {
      PDDocument pdf;
      if (password == null) {
         pdf = PDDocument.load(templatePdf);
      } else {
         pdf = PDDocument.load(templatePdf, password);
      }

      PDDocumentCatalog docCatalog = pdf.getDocumentCatalog();
      PDAcroForm acroForm = docCatalog.getAcroForm();
      assertTrue(acroForm.getFields().isEmpty());

      COSDocument cosDoc = pdf.getDocument();
      PDFTextStripper pdfStripper = new PDFTextStripper();
      PDDocument pdDoc = new PDDocument(cosDoc);
      pdfStripper.setStartPage(1);
      pdfStripper.setEndPage(1);
      String parsedText = pdfStripper.getText(pdDoc);
      
      expected.forEach(
            s -> assertTrue("expected '" + s + "'", parsedText.contains(s)));
      pdf.close();
   }

}