package org.gssb.pdffiller.pdf;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.gssb.pdffiller.excel.ExcelCell;
import org.gssb.pdffiller.excel.ExcelRow;
import org.gssb.pdffiller.pdf.PdfFormFiller;
import org.junit.Test;

public class PdfFormFillerTest extends PDFValidator {

   private ExcelRow createRow(final List<String> keys,
                              final List<String> values) {
      assert(keys.size() == values.size());
      ExcelRow row = new ExcelRow();
      for (int i=0; i<keys.size(); i++) {
         row.addExcelCell(new ExcelCell(i, keys.get(i), values.get(i)));
      }
      return row;
   }
   
   @Test
   public void testCreatePdf() {
      String originalPdf = "src/test/resources/2018/sources/AATG Raw Score.pdf";
      String targetPdf   = "src/test/resources/2018/generated/Leo Test.pdf";
      String masterKey   = "MASTER";
      String secret      = "abc";
      
      List<String> keys = 
            Arrays.asList(new String[]{"Klasse", "Name", "LehrerIn", "Level",
                                       "Points_out_of_100", "Listening_and_Viewing",
                                       "Reading", "Percentile"});
      List<String> values = 
            Arrays.asList(new String[]{"6B", "Sasson, Leo", "Mr. Cool", "02",
                                       "43", "21", "22", "N/A"});

      PdfFormFiller filler = new PdfFormFiller();
      ExcelRow row = createRow(keys, values);
      File pdfTemplate = new File(originalPdf);
      File targetFile = new File(targetPdf);
      if (targetFile.exists()) {
         targetFile.delete();
      }
      
      try {
         filler.populateAndCopy(pdfTemplate, targetFile, row.getRowMap(), masterKey, 
                                Collections.emptyMap(), secret);
      } catch (InvalidPasswordException e) {
         e.printStackTrace();
         fail();
      } catch (IOException e) {
         e.printStackTrace();
         fail();
      }
      
      assertTrue(targetFile.exists());
      
      try {
         List<String> expected = Arrays.asList(new String[] 
               {"Sasson, Leo", "6B", "Mr. Cool", 
                "02", "21", "22", "43", "N/A"});
         validatePDFDocument(targetFile, secret, expected);
      } catch (IOException e) {
         e.printStackTrace();
         fail("Unexpected exception while inspecting generated PDF");
      } 
   }

}
