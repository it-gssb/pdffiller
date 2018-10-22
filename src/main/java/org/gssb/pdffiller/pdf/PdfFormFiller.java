package org.gssb.pdffiller.pdf;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;

public class PdfFormFiller {
   
   private final static Logger logger =
                        LogManager.getLogger(PdfFormFiller.class);
   
   private static final String NOT_PDF_ERROR = 
         "Error: Header doesn't contain versioninfo";
   private static final String NON_FORM_WARN = 
         "The file '%s' does not contain a PDF form and will be copied as is.";
   
   private static final int    KEY_STRENGTH = 128;
   
   public void encrypt(final PDDocument pdf, final String masterKey,
                       final String key) throws IOException {
      AccessPermission ap = new AccessPermission();

      // Disable form filling everything else is allowed
      ap.setCanFillInForm(false);
      ap.setCanModify(false);

      StandardProtectionPolicy spp = new StandardProtectionPolicy(masterKey,
                                                                  key, ap);
      spp.setEncryptionKeyLength(KEY_STRENGTH);
      spp.setPermissions(ap);
      pdf.protect(spp);
   }
   
   private void setField(final PDField field, final String value) 
                throws IOException {
      assert(field != null);
      field.setValue(value);
//    field.setReadOnly(true);
   }

   private Map<String, List<PDField>> getPdfFieldMap(final PDAcroForm acroForm) {
      return acroForm.getFields()
                     .stream()
                     .collect(Collectors.groupingBy(PDField::getFullyQualifiedName));
   }
   
   private boolean condition(final File pdfFile,
                             final Predicate<PDAcroForm> formPredicate) 
                   throws IOException, InvalidPasswordException{
      boolean result;
      try (PDDocument pdf = PDDocument.load(pdfFile)) {
         PDDocumentCatalog docCatalog = pdf.getDocumentCatalog();
         PDAcroForm acroForm = docCatalog.getAcroForm();
         result = formPredicate.test(acroForm);
      } catch (IOException e) {
         if (e.getMessage().contains(NOT_PDF_ERROR)) {
            // not a PDF document
            result = false;
         } else {
            throw e;
         }
      }
      return result;
   }
   
   public boolean isPdfForm(final File pdfFile) 
                  throws IOException, InvalidPasswordException{
      return condition(pdfFile, a -> a!=null);
   }

   private void fillFormFields(final Map<String, String> formMap,
                               final Map<String, String> fieldMap,
                               final Map<String, List<PDField>> pdfFields)
                throws IOException {
      for (Entry<String, List<PDField>> pdfField : pdfFields.entrySet()) {
         assert(pdfField.getValue().size() >0);
         String acroFieldName = pdfField.getKey();
         String columnName = fieldMap.getOrDefault(acroFieldName, acroFieldName);
         String value = formMap.get(columnName);
         if (value==null) continue;
         
         // handle multiple fields with the same name
         for (PDField field : pdfField.getValue()) {
            setField(field, value);
         }
      }
   }
   
   private void logFormFields(PDAcroForm acroForm) {
      String fieldNames = acroForm.getFields()
                                  .stream()
                                  .map(f -> f.getFullyQualifiedName())
                                  .collect(Collectors.joining(", "));
      logger.debug(fieldNames);
   }
   
   private boolean isNumber(final String string) {
       try {
         Integer.valueOf(string);
          return true;
      } catch (NumberFormatException e) {
         return false;
      }
   }
   
   private String getBaseName(final String name) {
      String base = name;
      int index = name.lastIndexOf("_");
      if (index > 0 && isNumber(name.substring(index+1, name.length()))) {
         base = name.substring(0, index);
      }
      return base;
   }
   
   private boolean containsRepeatedFieldNames(final PDAcroForm acroForm) {
      return !acroForm.getFields()
                      .stream()
                      .map(f -> f.getFullyQualifiedName())
                      .collect(Collectors.groupingBy(s -> getBaseName(s)))
                      .values()
                      .stream()
                      .filter(l -> l.size()>1)
                      .findFirst()
                      .orElse(Collections.emptyList())
                      .isEmpty();
   }
   
   public boolean containsRepeatedFieldNames(final File pdfFile)
                  throws IOException, InvalidPasswordException {
      return condition(pdfFile,
                       a -> (a != null) && containsRepeatedFieldNames(a));
   }
      

   public void populateAndCopy(final File templatePdf,
                               final File targetPdf,
                               final Map<String, String> formMap,
                               final String masterKey,
                               final Map<String, String> fieldMap,
                               final String secret) 
               throws IOException, InvalidPasswordException {
      PDDocument pdf = PDDocument.load(templatePdf);
      PDDocumentCatalog docCatalog = pdf.getDocumentCatalog();
      PDAcroForm acroForm = docCatalog.getAcroForm();
      
      if (acroForm!=null) {
         logFormFields(acroForm);
         
         acroForm.setNeedAppearances(false);
         fillFormFields(formMap, fieldMap, getPdfFieldMap(acroForm));
         acroForm.flatten();
      
         if (masterKey!=null && !masterKey.isEmpty() && 
             secret!=null && !secret.isEmpty() && !secret.equals("0")) {
           encrypt(pdf, masterKey, secret);
         }
      } else {
         String msg = String.format(NON_FORM_WARN, templatePdf.getName());
         logger.warn(msg);
      }
      pdf.save(targetPdf);
      pdf.close();
   }

}
