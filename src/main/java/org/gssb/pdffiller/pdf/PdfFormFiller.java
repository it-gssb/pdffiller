package org.gssb.pdffiller.pdf;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.gssb.pdffiller.excel.ExcelCell;
import org.gssb.pdffiller.excel.ExcelRow;

public class PdfFormFiller {
//   private final static Logger logger = LogManager.getLogger(PdfFormFiller.class);
   
   private static final int    KEY_STRENGTH = 128;

   public void encrypt(final PDDocument pdf, final String masterKey,
                       final String key) throws IOException {
      AccessPermission ap = new AccessPermission();

      // Disable form filling everything else is allowed
      ap.setCanFillInForm(false);
      ap.setCanModify(false);

      StandardProtectionPolicy spp = new StandardProtectionPolicy(masterKey, key, ap);
      spp.setEncryptionKeyLength(KEY_STRENGTH);
      spp.setPermissions(ap);
      pdf.protect(spp);
   }
   
   private void setField(final PDAcroForm acroForm, final String fieldName,
                         final String value) throws IOException {
      PDField field = acroForm.getField( fieldName );
      if (field != null ) {
          field.setValue(value);
         // set the field to read only after inserting content
//         field.setReadOnly(true);
      } else {
          assert(0==1) : "No field found with name:" + fieldName;
      }
  }
   
   public void populateAndCopy(final File templatePdf, final File targetPdf,
                               final ExcelRow excelRow, final String masterKey,
                               final Map<String, String> fieldMap,
                               final String secret) 
               throws IOException, InvalidPasswordException {
      PDDocument pdf = PDDocument.load(templatePdf);
      
      PDDocumentCatalog docCatalog = pdf.getDocumentCatalog();
      PDAcroForm acroForm = docCatalog.getAcroForm();
      
      Set<String> fieldNames =
            acroForm.getFields()
                    .stream()
                    .map(f -> f.getFullyQualifiedName())
                    .collect(Collectors.toSet());
      acroForm.setNeedAppearances(false);
      
      // iterate over all pdf fields
      for (String acroFieldName : fieldNames) {
         String columnName = fieldMap.getOrDefault(acroFieldName, acroFieldName);
         ExcelCell cell = excelRow.getRow().get(columnName);
         
         if (cell==null) continue;
         
         setField(acroForm, acroFieldName, cell.getColumnValue());
      }
      
      acroForm.flatten();
      
      if (masterKey!=null && !masterKey.isEmpty() && 
          secret!=null && !secret.isEmpty() && !secret.equals("0")) {
        encrypt(pdf, masterKey, secret);
      }
      pdf.save(targetPdf);
      pdf.close();
   }
   
}