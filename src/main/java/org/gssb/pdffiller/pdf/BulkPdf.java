package org.gssb.pdffiller.pdf;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.gssb.pdffiller.config.AppProperties;
import org.gssb.pdffiller.excel.ColumnNotFoundException;
import org.gssb.pdffiller.excel.ExcelRow;
import org.gssb.pdffiller.excel.RowReader;
import org.gssb.pdffiller.exception.UnrecoverableException;
import org.gssb.pdffiller.template.Choice;
import org.gssb.pdffiller.template.Template;
import org.gssb.pdffiller.text.TextBuilder;

import com.github.mustachejava.MustacheException;

public class BulkPdf {
   
   private static class Target {
      private final Template template;
      private final String   baseFileName;
      
      Target(final Template template, final Optional<String> baseFileName) {
         this.template = Objects.requireNonNull(template);
         if (Objects.requireNonNull(baseFileName).isPresent()) {
            this.baseFileName = baseFileName.get();
         } else {
            String fileName = template.getTemplateFileName();
            int idx = fileName.indexOf(".");
            this.baseFileName = (idx == -1) ? fileName
                                            : fileName.substring(0, idx);
         }
      }

      public Template getTemplate() {
         return template;
      }

      public String getBaseFileName() {
         return baseFileName;
      }
      
   }
   
   private final static Logger logger = LogManager.getLogger(BulkPdf.class);
   
   private static final String CREATE_NAME_ERROR = 
         "An unexpected error occured when creating the file name using " +
         "the name template %s.";
   
   private static final String CREATE_NAME_SUB_ERROR = 
         "The file name template %s contains an undefined variable.";
   
   private static final String BASE_NAME = "_BaseName_";
   
   private final PrintStream outstream;

   private final String sourceFolder;
   private final String generatedFolder;
   private final String excelInputFile;
   
   private final RowReader rowReader;
   private final PdfFormFiller pdfFormFiller;
   private final TextBuilder textBuilder;
   
   private final String fileNameTemplate;
   
   BulkPdf(final AppProperties properties, final RowReader rowReader,
           final TextBuilder textBuilder, final PdfFormFiller pdfFormFiller,
           final PrintStream outstream) {
      super();
      this.sourceFolder = properties.getSourceFolder();
      this.generatedFolder = properties.getGeneratedFolder();
      this.excelInputFile = properties.getExcelFileName();
      
      this.rowReader = rowReader;
      this.textBuilder = textBuilder;
      this.pdfFormFiller = pdfFormFiller;
      
      this.fileNameTemplate = properties.getFileNameTemplate();
      
      this.outstream = outstream;
   }
   
   public BulkPdf(final AppProperties properties) {
      this(properties, new RowReader(), new TextBuilder(), new PdfFormFiller(),
           System.out);
   }
   
   private void printProgress(final int count, final char character) {
      if (count % 100 == 0) {
         this.outstream.println(character);
      } else {
         this.outstream.print(character);
      }
   }
   
   protected List<ExcelRow> createRows(final File excelFile,
                                       final String sheetName) {
      List<ExcelRow> rows = null;
      try {
         rows = this.rowReader.read(excelFile, sheetName);
      } catch (EncryptedDocumentException e) {
         String msg = "PDF Template is encrypted.";
         logger.error(msg, e);
         throw new UnrecoverableException(msg, e);
      } catch (InvalidFormatException e) {
         String msg = "PDF Template is encrypted.";
         logger.error(msg, e);
         throw new UnrecoverableException(msg, e);
      } catch (IOException e) {
         String msg = e.getMessage();
         logger.error(msg, e);
         throw new UnrecoverableException(msg, e);
      }
      return rows;
   }
   
   // TODO: externalize expression for name
   protected String getTargetFileName(final ExcelRow row, 
                                      final Path templatePath,
                                      final Map<String, String> nameValuePairs) {
      String name;
      try {
         name = textBuilder.substitute(this.fileNameTemplate, nameValuePairs);
      } catch (IOException e) {

         String msg = String.format(CREATE_NAME_ERROR, this.fileNameTemplate);
         logger.error(msg, e);
         throw new UnrecoverableException(msg, e);
      } catch(MustacheException e) {

         String msg = String.format(CREATE_NAME_SUB_ERROR,
                                    this.fileNameTemplate) + e.getMessage();
         logger.error(msg, e);
         throw new UnrecoverableException(msg, e);
      }
      return name;
   }

   protected File getTargetPdf(final String rootPath, final ExcelRow row,
                               final Path templatePath,
                               final String baseFileName) {
	   // define additional 
	   Map<String, String> nameValuePairs = new HashMap<>(row.getRowMap());
	   nameValuePairs.put(BASE_NAME, baseFileName);
      String targetPath = rootPath + File.separator + 
                          this.generatedFolder + File.separator +
                          getTargetFileName(row, templatePath, nameValuePairs);
      return new File(targetPath);
   }
   
   private File createFilledFile(final String rootPath, final ExcelRow row,
                                 final String masterKey,
                                 final String secretColumnName,
                                 final Path templatePath, 
                                 final String baseFileName,
                                 final Map<String, String> formFieldMap) {
      // uses by default reference to PDF file if it does not contain a form
      File targetPdf = templatePath.toFile();
      try {
         if (this.pdfFormFiller.isPdfForm(templatePath.toFile())) {
            targetPdf = getTargetPdf(rootPath, row, templatePath, baseFileName);
            String secret = row.getValue(secretColumnName).getColumnValue();
            this.pdfFormFiller
                .populateAndCopy(templatePath.toFile(), targetPdf, row,
                                 masterKey, formFieldMap, secret);
         } else {
            logger.debug("Include plain PDF document " + 
                         targetPdf.toString() + " into UoW.");
         }
      } catch (InvalidPasswordException e) {
         String msg = "PDF template '" + templatePath.toFile().getAbsolutePath() +
                      "' is encrypted.";
         logger.error(msg, e);
         throw new UnrecoverableException(msg, e);
      } catch (IOException e) {
         String msg = "Error while creating file '" +
                      targetPdf.getAbsolutePath() + "'.";
         logger.error(msg, e);
      }
      
      return targetPdf;
   }
   
   private Optional<Target> findMatchingTemplate(final ExcelRow row,
                                                 final Choice choice) {
      String optionColumn = choice.getSelectionColumn();
      
      String optionValue;
      try {
         optionValue = row.getValue(optionColumn).getColumnValue();
      } catch (ColumnNotFoundException e) {
         String msg = "Excel column name defined in choice does not exist.";
         logger.error(msg, e);
         throw new UnrecoverableException(msg, e);
      }
      
      if (optionValue==null || optionValue.isEmpty() || optionValue.equals("0")) {
         return Optional.empty();
      } else if (!choice.getKeys().contains(optionValue)) {
         String msg = "Choice value " + optionValue + " is undefined.";
         logger.error(msg);
         throw new UnrecoverableException(msg);
      }
      
      Optional<Template> template = choice.select(optionValue);
      if (!template.isPresent()) {
         return Optional.empty();
      }
      
      return Optional.of(new Target(template.get(), choice.getBaseName()));
   }
   
   private List<Target> findMatchingTemplates(final ExcelRow row,
                                              final List<Choice> choices) {
      return choices.stream()
                    .map(c -> findMatchingTemplate(row, c))
                    .flatMap(o -> o.isPresent() ? Stream.of(o.get()) 
                                                : Stream.empty())
                    .collect(Collectors.toList());
   }
   
   private List<File> createPdfFiles(final String rootPath, final ExcelRow row,
                                    final String masterKey,
                                    final String secretColumnName,
                                    final List<Template> alwaysInclude,
                                    final List<Choice> choices,
                                    final Map<String, Map<String, String>> formFieldMaps) {
      List<File> createdFiles =
            Stream.concat(alwaysInclude.stream()
                                       .map(a -> new Target(a, Optional.empty())), 
                          findMatchingTemplates(row, choices).stream())
                  .map(t -> createFilledFile(rootPath, row, 
                                             masterKey, secretColumnName, 
                                             t.getTemplate().getTemplatePath(),
                                             t.getBaseFileName(),
                                             formFieldMaps.get(t.getTemplate()
                                                                .getKey())))
                  .collect(Collectors.toList());
      
      return createdFiles;
   }

   public List<UnitOfWork> createPdfs(final String rootPath,
                                      final String sheetName,
                                      final String masterKey,
                                      final String secretColumnName,
                                      final List<Template> alwaysInclude,
                                      final List<Choice> choices,
                                      final Map<String, Map<String, String>> formFieldMaps) {
      String excelPath = rootPath + File.separator + this.sourceFolder +
                         File.separator + this.excelInputFile;
      List<ExcelRow> rows = createRows(new File(excelPath), sheetName);
      
      this.outstream.println();
      int processed = 0;
      int count = 0;
      List<UnitOfWork> resultSets = new ArrayList<>();
      for (ExcelRow row : rows) {
         List<File> generated = createPdfFiles(rootPath, row, masterKey, 
                                               secretColumnName, alwaysInclude,
                                               choices, formFieldMaps);
         resultSets.add(new UnitOfWork(row, generated));
         
         processed++;
         count+=generated.size();
         printProgress(processed, '.');
      }
      this.outstream.println();
      this.outstream.println("Created " + count + " files for " + rows.size() +
                             " input rows.");
      
      return resultSets;
   }

}
