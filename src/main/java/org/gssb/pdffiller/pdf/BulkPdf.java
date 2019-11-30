package org.gssb.pdffiller.pdf;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.gssb.pdffiller.config.AppProperties;
import org.gssb.pdffiller.excel.ColumnNotFoundException;
import org.gssb.pdffiller.excel.ExcelCell;
import org.gssb.pdffiller.excel.ExcelRow;
import org.gssb.pdffiller.excel.RowGroup;
import org.gssb.pdffiller.excel.ExcelReader;
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
   
   private static final String SECRET_COLUMN_DOES_NOT_EXISTS =
         "The secret column %s does not exist in the selected " +
         "Excel workbook sheet.";
   
   private static final String BASE_NAME = "_BaseName_";
   
   private final PrintStream outstream;

   private final String sourceFolder;
   private final String generatedFolder;
   private final String excelInputFile;
   
   private final List<String> groupColumns;
   
   private final ExcelReader rowReader;
   private final PdfFormFiller pdfFormFiller;
   private final TextBuilder textBuilder;
   
   private final String fileNameTemplate;
   private final String fileGroupNameTemplate;
   
   BulkPdf(final AppProperties properties, final ExcelReader rowReader,
           final TextBuilder textBuilder, final PdfFormFiller pdfFormFiller,
           final PrintStream outstream) {
      super();
      this.sourceFolder = properties.getSourceFolder();
      this.generatedFolder = properties.getGeneratedFolder();
      this.excelInputFile = properties.getExcelFileName();
      
      this.groupColumns = properties.getGroupColumns();
      
      this.rowReader = rowReader;
      this.textBuilder = textBuilder;
      this.pdfFormFiller = pdfFormFiller;
      
      this.fileNameTemplate = properties.getFileNameTemplate();
      this.fileGroupNameTemplate = properties.getFileGroupNameTemplate();
      
      this.outstream = outstream;
   }
   
   public BulkPdf(final AppProperties properties) {
      this(properties, new ExcelReader(properties), new TextBuilder(),
           new PdfFormFiller(), System.out);
   }
   
   private void printProgress(final int count, final char character) {
      if (count % 100 == 0) {
         this.outstream.println(character);
      } else {
         this.outstream.print(character);
      }
   }
   
   private List<RowGroup> groupRows(final List<ExcelRow> allRows,
                                    final Optional<String> groupId,
                                    final boolean singleRecord) {
      if (allRows.isEmpty()) {
         return Collections.emptyList();
      }
      
      // grouping column not found -> process row by row
      if (this.groupColumns==null || this.groupColumns.isEmpty() ||
          allRows.get(0).getValue(this.groupColumns.get(0)) == null) {
         return allRows.stream()
                       .map(r -> new RowGroup(null, Arrays.asList(r)))
                       .collect(Collectors.toList());
      }
      
      String groupColumn = this.groupColumns.get(0);
      Map<String, List<ExcelRow>> groups =
            allRows.stream()
                   .collect(Collectors.groupingBy(r -> r.getValue(groupColumn)
                                                        .getColumnValue()));
      
      Comparator<RowGroup> comp = 
            (g1, g2) -> g1.getHeadRow()
                          .getValue(groupColumn)
                          .getColumnValue()
                          .compareTo(g2.getHeadRow()
                                       .getValue(groupColumn)
                                       .getColumnValue());
      
       List<RowGroup> groupRows =
             groups.entrySet()
                   .stream()
                   .map(l -> new RowGroup(groupColumn, l.getValue()))
                   .sorted(comp)
                   .collect(Collectors.toList());
       
       List<RowGroup> resultRows = new ArrayList<>();
       boolean foundFirst = !groupId.isPresent();
       for (RowGroup rowGroup : groupRows) {
          foundFirst = foundFirst ||
                       groupId.isPresent() &&
                       rowGroup.getGroupId().isPresent() &&
                       rowGroup.getGroupId().get().equals(groupId.get());
          if (foundFirst) {
             resultRows.add(rowGroup);
          }
       }
       return resultRows;
   }
   
   protected List<RowGroup> createGroups(final File excelFile,
                                         final String sheetName,
                                         final Optional<String> startGroupId,
                                         final boolean singleRecord) {
      List<RowGroup> groups = null;
      try {
         groups = groupRows(this.rowReader.read(excelFile, sheetName),
                            startGroupId, singleRecord);
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
      return groups;
   }
   
   // TODO: externalize expression for name
   protected String getTargetFileName(final Map<String, String> formMap, 
                                      final Path templatePath,
                                      final Map<String, String> nameValuePairs,
                                      final boolean isGroup) {
      String name;
      String templateString = isGroup ? this.fileGroupNameTemplate
                                      : this.fileNameTemplate; 
      try {
         name = textBuilder.substitute(templateString, nameValuePairs);
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

   protected File getTargetPdf(final String rootPath,
                               final Map<String, String> formMap,
                               final Path templatePath,
                               final String baseFileName,
                               final boolean isGroup) {
	   // define additional key-value pair for file base name
	   Map<String, String> nameValuePairs = new HashMap<>(formMap);
	   nameValuePairs.put(BASE_NAME, baseFileName);
      String targetPath = rootPath + File.separator + 
                          this.generatedFolder + File.separator +
                          getTargetFileName(formMap, templatePath, 
                                            nameValuePairs, isGroup);
      return new File(targetPath);
   }

   private File createFilledFile(final String rootPath,
                                 final Map<String, String> formMap,
                                 final String masterKey,
                                 final String secretColumnName,
                                 final Path templatePath, 
                                 final String baseFileName,
                                 final Map<String, String> formFieldMap,
                                 final boolean isGroup) {
      // uses by default reference to PDF file if it does not contain a form
      File targetPdf = templatePath.toFile();
      try {
         if (this.pdfFormFiller.isPdfForm(templatePath.toFile())) {
            targetPdf = getTargetPdf(rootPath, formMap, templatePath,
                                     baseFileName, isGroup);
            String secret = formMap.get(secretColumnName);
            this.pdfFormFiller
                .populateAndCopy(templatePath.toFile(), targetPdf, formMap,
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
         throw new UnrecoverableException(msg, e);
      }
      
      return targetPdf;
   }
   
   private Optional<Target> findMatchingTemplate(final ExcelRow row,
                                                 final Choice choice) {
      String optionColumn = choice.getSelectionColumn();
      
      String optionValue;
      try {
         ExcelCell cell = row.getValue(optionColumn);
		 if (cell==null) {
	        String msg = "Choice column " + optionColumn + " is undefined.";
	        logger.error(msg);
	        throw new UnrecoverableException(msg);
	     }	 
         optionValue = cell.getColumnValue();
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
   
   private boolean containsOnlyGroupFields(final Path templatePath,
                                           final Map<String, String> formFieldMap) {
      Set<String> fields;
      try {
         fields = this.pdfFormFiller.getFields(templatePath.toFile());
      } catch (IOException e) {
         return false;
      }
      
      // map form field names to the excel column names if specified
      Set<String> mappedFields = 
            fields.stream()
                  .map(f -> formFieldMap.getOrDefault(f,f))
                  .collect(Collectors.toSet());
      return this.groupColumns.containsAll(mappedFields);
   }
   
   private boolean isPdfForm(final Path templatePath) {
      try {
         return this.pdfFormFiller.isPdfForm(templatePath.toFile());
      } catch (IOException e) {
         return false;
      }
   }

   private boolean useOneRecordPerForm(final Path templatePath) {
      try {
         return this.pdfFormFiller
                    .containsRepeatedFieldNames(templatePath.toFile());
      } catch (IOException e) {
         return false;
      }
   }
   
   private List<File> createFilledFiles(final String rootPath,
                                        final RowGroup group,
                                        final String masterKey,
                                        final String secretColumnName,
                                        final List<Choice> choices,
                                        final Map<String, Map<String, String>> formFieldMaps) {
      List<File> files = new ArrayList<>();
      for (ExcelRow row : group.getRows()) {
         List<Target> targets = findMatchingTemplates(row, choices);
         for (Target target : targets) {
            files.add(createFilledFile(rootPath, row.createRowMap(), masterKey,
                                       secretColumnName,
                                       target.getTemplate().getTemplatePath(),
                                       target.getBaseFileName(),
                                       formFieldMaps.get(target.getTemplate()
                                                               .getKey()),
                                       false));
         }
      }
      
      return files;
   }
   
   private List<File> createFilledFiles(final String rootPath,
                                        final RowGroup group,
                                        final String masterKey,
                                        final String secretColumnName,
                                        final Path templatePath, 
                                        final String baseFileName,
                                        final Map<String, String> formFieldMap) {
      List<File> files = new ArrayList<>();
      if (!isPdfForm(templatePath)) {
         files.add(createFilledFile(rootPath, group.getHeadRow().getRowMap(),
                                    masterKey, secretColumnName, templatePath,
                                    baseFileName, formFieldMap, false));
      } else if (useOneRecordPerForm(templatePath) ||
                 containsOnlyGroupFields(templatePath, formFieldMap)) {
         files.add(createFilledFile(rootPath,
                                    group.createFormMap(new HashSet<>(this.groupColumns)),
                                    masterKey, secretColumnName, templatePath,
                                    baseFileName, formFieldMap, true));
      } else {
         for (ExcelRow row : group.getRows()) {
            files.add(createFilledFile(rootPath, row.createRowMap(), masterKey,
                                       secretColumnName, templatePath, baseFileName,
                                       formFieldMap, false));
         }
      }
      return files;
   }
   
   private List<File> createPdfFiles(final String rootPath, final RowGroup group,
                                     final String masterKey,
                                     final String secretColumnName,
                                     final List<Template> alwaysInclude,
                                     final List<Choice> choices,
                                     final Map<String, Map<String, String>> formFieldMaps) {
      List<File> createdFiles =
            alwaysInclude.stream()
                         .map(a -> new Target(a, Optional.empty()))
                         .flatMap(t -> createFilledFiles(rootPath, group, 
                                                         masterKey, secretColumnName, 
                                                         t.getTemplate()
                                                          .getTemplatePath(),
                                                         t.getBaseFileName(),
                                                         formFieldMaps.get(t.getTemplate()
                                                                            .getKey()))
                                                       .stream())
                         .collect(Collectors.toList());
      
      createdFiles.addAll(createFilledFiles(rootPath, group, masterKey, secretColumnName,
                                            choices, formFieldMaps));
      return createdFiles;
   }

   public List<UnitOfWork> createPdfs(final String rootPath,
                                      final String sheetName,
                                      final String masterKey,
                                      final String secretColumnName,
                                      final List<Template> alwaysInclude,
                                      final List<Choice> choices,
                                      final Map<String, Map<String, String>> formFieldMaps,
                                      final Optional<String> startGroupId,
                                      final boolean singleRecord) {
	   
      String generatedFolderPath = rootPath + File.separator + this.generatedFolder;
      File generateFolder = new File(generatedFolderPath);
      if (!generateFolder.exists() && !generateFolder.mkdir()) {
    	   String msg = String.format("Unable to create directory %s.",
    			                        generatedFolderPath);
    	   logger.error(msg);
         throw new UnrecoverableException(msg);
      }
	   
      String excelPath = rootPath + File.separator + this.sourceFolder +
                         File.separator + this.excelInputFile;
      List<RowGroup> groups = createGroups(new File(excelPath), sheetName,
                                           startGroupId, singleRecord);
      
      if (masterKey != null && !masterKey.isEmpty() && 
          !secretColumnName.isEmpty() && !groups.isEmpty() &&
          groups.get(0).getHeadRow().getValue(secretColumnName) == null) {
         String msg = String.format(SECRET_COLUMN_DOES_NOT_EXISTS,
                                    secretColumnName);
         logger.error(msg);
         throw new UnrecoverableException(msg);
      }
      
      this.outstream.println();
      int processed = 0;
      int count = 0;
      int recordCount = 0;
      List<UnitOfWork> resultSets = new ArrayList<>();
      for (RowGroup group : groups) {
         List<File> generated = createPdfFiles(rootPath, group, masterKey, 
                                               secretColumnName, alwaysInclude,
                                               choices, formFieldMaps);
         resultSets.add(new UnitOfWork(group, generated));
         
         processed++;
         count+=generated.size();
         recordCount+=group.getRows().size();
         printProgress(processed, '.');
      }
      this.outstream.println();
      this.outstream.println("Created " + count + " files for " + groups.size() +
                             " groups with " + recordCount + " records.");
      
      return resultSets;
   }

}
