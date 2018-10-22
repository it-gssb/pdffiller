package org.gssb.pdffiller.pdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.gssb.pdffiller.config.AppProperties;
import org.gssb.pdffiller.excel.ExcelCell;
import org.gssb.pdffiller.excel.ExcelRow;
import org.gssb.pdffiller.excel.ExcelReader;
import org.gssb.pdffiller.template.Choice;
import org.gssb.pdffiller.template.Template;
import org.gssb.pdffiller.template.TemplateHelper;
import org.gssb.pdffiller.text.TextBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class BulkPdfTest extends PDFValidator {
   
   private final static String ROOT = "src/test/resources/2018";
   private final static String TEMPLATE1 = ROOT + "/sources/AATG Raw Score.pdf";
   private final static String TEMPLATE2 = ROOT + "/sources/AATG Gold.pdf";
   private final static String TEMPLATE3 = ROOT + "/sources/AATG Participation.pdf";
   private final static String GENERATED_DIR = ROOT + "/generated";
   
   private final static String MASTER_KEY =  "MASTER";
   private final static String FILE_NAME_TEMPLATE = "{{_BaseName_}} - {{Name}}.pdf";
   
   private PrintStream printStream;
   private ExcelReader rowReader= null;
   private TextBuilder textBuilder = null;
   private BulkPdf bulkPdf;

   private void deleteGeneratedFiles(final File generatedDir) {
      assertTrue(generatedDir.isDirectory());
      List<String> bulkGenerated = 
            Arrays.asList(generatedDir.list())
                  .stream()
                  .filter(n -> n.substring(0,n.length()-5).endsWith("Leon"))
                  .collect(Collectors.toList());
      try {
         bulkGenerated.forEach(n -> new File(generatedDir, n).delete());
      } catch (Exception e) {
         e.printStackTrace();
         fail("Unable to delete all files starting with name 'bulk_'");
      }
   }
   
   private Map<String, String> createMap(final List<String> keyValues) {
      Map<String, String> fileMap = new HashMap<>();
      for (String kv : keyValues) {
         String[] pair = kv.split(":", 2);
         assertEquals(2, pair.length);
         fileMap.put(pair[0].trim(), pair[1].trim());
      }
      return fileMap;
   }
   
   private List<ExcelCell> createMockCells(final List<String> keyValueList) {
      List<ExcelCell> cells = new ArrayList<>();
      int i = 1;
      for (String kv : keyValueList) {
         String[] pair = kv.split(":", 2);
         assertEquals(2, pair.length);
         cells.add(new ExcelCell(i++, pair[0].trim(), pair[1].trim()));
      }
      return cells;
   }
   
   private ExcelRow createMockRow(final List<String> keyValuePairs) {
      ExcelRow row = new ExcelRow();
      createMockCells(keyValuePairs).forEach(kv -> row.addExcelCell(kv));
      return row;
   }
   
   private List<Choice> createMockChoices(final String baseName) {
      Map<String, Template> templateChoices = new HashMap<>();
      
      Template template2 = 
            TemplateHelper.createTemplate("pdf2", Paths.get(TEMPLATE2));
      templateChoices.put("Goldurkunde", template2);
      
      Template template3 = 
            TemplateHelper.createTemplate("pdf3", Paths.get(TEMPLATE3));
      templateChoices.put("Participation", template3);
      
      return Arrays.asList(
            TemplateHelper.createChoice("Award",
                                        Optional.ofNullable(baseName),
                                        templateChoices)); 
   }

   private Map<String, Map<String, String>> defineFieldMaps() {
      Map<String, Map<String, String>> emptyFieldMap = new HashMap<>();
      emptyFieldMap.put("pdf1", Collections.emptyMap());
      
      List<String> pdf2KeyValues = Arrays.asList(new String[]
            {"Text1 : Name", "Text3 : Level", "Text4 : LehrerIn",
             "Text5 : Schule"});
      Map<String, String> pdf2FieldMap = createMap(pdf2KeyValues);
      emptyFieldMap.put("pdf2", pdf2FieldMap);
      
      List<String> pdf3KeyValues = Arrays.asList(new String[]
            {"Text5 : Name", "Text6 : Level", "Text7 : LehrerIn",
             "Text8 : Schule"});
      Map<String, String> pdf3FieldMap = createMap(pdf3KeyValues);
      emptyFieldMap.put("pdf3", pdf3FieldMap);
      return emptyFieldMap;
   }
   
   private List<ExcelRow> getMockRows() {
      List<ExcelRow> rows = new ArrayList<>();
      List<String> rowDef1 = Arrays.asList(new String[] 
            {"Name:Sasson, Leon1", "LehrerIn:Mr. Cool", "Level:3", 
             "Schule:GSSB", "Klasse:1B", "secret:abc1", "Award:Goldurkunde"});
      rows.add(createMockRow(rowDef1));
      List<String> rowDef2 = Arrays.asList(new String[] 
            {"Name:Sasson, Leon2", "LehrerIn:Mr. Cool", "Level:3", 
             "Schule:GSSB", "Klasse:2B", "secret:abc2", "Award:Goldurkunde"});
      rows.add(createMockRow(rowDef2));
      List<String> rowDef3 = Arrays.asList(new String[] 
            {"Name:Sasson, Leon3", "LehrerIn:Mr. Cool", "Level:3", 
             "Schule:GSSB", "Klasse:3B", "secret:abc3", "Award:Participation"});
      rows.add(createMockRow(rowDef3));
      return rows;
   }
   
   @Before
   public void setUp() throws Exception {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      this.printStream = new PrintStream(baos, true, "UTF-8");
      AppProperties props = mock(AppProperties.class);
      when(props.getSourceFolder()).thenReturn("sources");
      when(props.getGeneratedFolder()).thenReturn("generated");
      when(props.getExcelFileName()).thenReturn("Dummy.xlsx");
      when(props.getFileNameTemplate()).thenReturn(FILE_NAME_TEMPLATE);
      
      List<String> emailColumn = new ArrayList<>();
      emailColumn.add("PrimaryParentEmail");
      when(props.getGroupColumns()).thenReturn(emailColumn);
      when(props.getTargetEmailColumns()).thenReturn(emailColumn);
      when(props.getExcelSecretColumnName()).thenReturn("Key");
      
      this.rowReader = mock(ExcelReader.class);
      this.textBuilder = new TextBuilder();
      PdfFormFiller pdfFormFiller = new PdfFormFiller();
      this.bulkPdf = new BulkPdf(props, this.rowReader, this.textBuilder,
                                 pdfFormFiller, this.printStream);
      deleteGeneratedFiles(new File(GENERATED_DIR));
   }

   @After
   public void closeResource() throws Exception {
      this.printStream.close();
   }
   
   @Test
   public void testThreeRow() throws EncryptedDocumentException,
                                     InvalidFormatException, IOException {
      when(this.rowReader.read(new File(ROOT + "/sources/Dummy.xlsx"), "Dummy"))
          .thenReturn(getMockRows());
      
      Template template =
            TemplateHelper.createTemplate("pdf1", Paths.get(TEMPLATE1));
      List<Template> alwaysInclude = new ArrayList<>();
      alwaysInclude.add(template);
      
      List<Choice> choices = createMockChoices("AATG Cert");
      
      Map<String, Map<String, String>> fieldMaps = defineFieldMaps();
      
      List<UnitOfWork> uows =
         this.bulkPdf.createPdfs(ROOT, "Dummy", MASTER_KEY, "secret", 
                                 alwaysInclude, choices, fieldMaps);
      assertEquals(3, uows.size());
      
      int i=1;
      for (UnitOfWork uow : uows) {
         List<String> expected0 = Arrays.asList(new String[] 
               {"Sasson, Leon" + i, "Mr. Cool", "3", i+"B", "TESTERGEBNIS"});
         String reward = i>2 ? "Participation" : "Goldurkunde";
         List<String> expected1 = Arrays.asList(new String[] 
               {"Sasson, Leon" + i, "Mr. Cool", "3", "GSSB", reward});
         
         assertEquals(2, uow.getGeneratedFiles().size());
         
         File validate0 = uow.getGeneratedFiles().get(0);
         assertTrue(validate0.getName().startsWith("AATG Raw Score"));
         assertTrue(validate0.getName().endsWith("Sasson, Leon"+i+".pdf"));
         validatePDFDocument(validate0, "abc"+i, expected0);
         
         File validate1 = uow.getGeneratedFiles().get(1);
         assertTrue(validate1.getName().startsWith("AATG Cert"));
         assertTrue(validate1.getName().endsWith("Sasson, Leon"+i+".pdf"));
         validatePDFDocument(validate1, "abc"+i, expected1);
         i++;
      }
      
   }

}
