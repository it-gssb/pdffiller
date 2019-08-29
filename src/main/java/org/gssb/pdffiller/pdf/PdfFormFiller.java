package org.gssb.pdffiller.pdf;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
         "File '%s' does not contain a PDF form and will be copied as is.";
   private static final String ILLEGAL_CHAR_ERROR = 
         "Error while filling value '%1$s' into form field '%2$s' in file %3$s. " +
         "The most likely cause is an unsupported character in the suplied " +
         "value. The file was not created.";
   
   private static final int    KEY_STRENGTH = 128;
   
   private void encrypt(final PDDocument pdf, final String masterKey,
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
      try {
         field.setValue(value);
      } catch(java.lang.IllegalArgumentException e) {
         String msg = String.format("Error when assigning value to field %s.", 
                                    field.getFullyQualifiedName());
         throw new PdfUnsupportedCharacterException(msg, e, field, value);
      }
//    field.setReadOnly(true);
   }

   private Map<String, List<PDField>> getPdfFieldMap(final PDAcroForm acroForm) {
      return acroForm.getFields()
                     .stream()
                     .collect(Collectors.groupingBy(PDField::getFullyQualifiedName));
   }
   
   public Set<String> getFields(final File pdfFile) 
                      throws IOException, InvalidPasswordException {
      Set<String> result;
      try (PDDocument pdf = PDDocument.load(pdfFile)) {
         PDDocumentCatalog docCatalog = pdf.getDocumentCatalog();
         PDAcroForm acroForm = docCatalog.getAcroForm();
         result = acroForm.getFields()
                          .stream()
                          .map(f -> f.getFullyQualifiedName())
                          .collect(Collectors.toSet());         
      } catch (IOException e) {
         if (e.getMessage().contains(NOT_PDF_ERROR)) {
            // not a PDF document
            result = Collections.emptySet();
         } else {
            throw e;
         }
      }
      return result;
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
                               final Map<String, String> formFieldMap,
                               final Map<String, List<PDField>> pdfFields)
                throws IOException {
      for (Entry<String, List<PDField>> pdfField : pdfFields.entrySet()) {
         assert(pdfField.getValue().size() >0);
         String acroFieldName = pdfField.getKey();
         String columnName = formFieldMap.getOrDefault(acroFieldName,
                                                       acroFieldName);
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
   
   private boolean endsOnIndex(final String name) {
      int index = name.lastIndexOf("_");
      return index > 0 && isNumber(name.substring(index+1, name.length()));
   }
   
   private String getBaseName(final String name) {
      String base = name;
      if (endsOnIndex(name)) {
         int index = name.lastIndexOf("_");
         base = name.substring(0, index);
      }
      return base;
   }
   
   private boolean containsRepeatedFieldNames(final PDAcroForm acroForm) {
      return !acroForm.getFields()
                      .stream()
                      .map(f -> f.getFullyQualifiedName())
                      .filter(f -> endsOnIndex(f))
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
                               final Map<String, String> formFieldMap,
                               final String secret) 
               throws IOException, InvalidPasswordException {
      PDDocument pdf = PDDocument.load(templatePdf);
      PDDocumentCatalog docCatalog = pdf.getDocumentCatalog();
      PDAcroForm acroForm = docCatalog.getAcroForm();
      
      if (acroForm!=null) {
         logFormFields(acroForm);
         
         acroForm.setNeedAppearances(false);
         try {
            fillFormFields(formMap, formFieldMap, getPdfFieldMap(acroForm));
         } catch (PdfUnsupportedCharacterException e) {
            // do not create file and close template
            pdf.close();
            String msg = String.format(ILLEGAL_CHAR_ERROR,  
                                       e.getValue(), e.getFormField(),
                                       targetPdf.getName());
            logger.error(msg, e);
            return;
         }
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
