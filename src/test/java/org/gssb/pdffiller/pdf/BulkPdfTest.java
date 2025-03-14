package org.gssb.pdffiller.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.gssb.pdffiller.excel.ExcelReader;
import org.gssb.pdffiller.excel.ExcelRow;
import org.gssb.pdffiller.template.Choice;
import org.gssb.pdffiller.template.Template;
import org.gssb.pdffiller.template.TemplateHelper;
import org.gssb.pdffiller.text.TextBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BulkPdfTest extends PDFValidator {

   private final static String ROOT = "src/test/resources/2018";
   private final static String TEMPLATE1 = ROOT + "/sources/AATG Raw Score.pdf";
   private final static String TEMPLATE2 = ROOT + "/sources/AATG Gold.pdf";
   private final static String TEMPLATE3 = ROOT + "/sources/AATG Participation.pdf";
   private final static String TEMPLATE4 = ROOT + "/sources/2018-2019 Acceptance.pdf";
   private final static String GENERATED_DIR = ROOT + "/generated";

   private final static String MASTER_KEY =  "MASTER";
   private final static String EMAIL_ADDRESS = "PrimaryParentEmail";
   private final static String FILE_NAME_TEMPLATE = "{{_BaseName_}} - {{Name}}.pdf";
   private final static String FILE_GROUP_NAME_TEMPLATE =
                                    "{{_BaseName_}} - {{" + EMAIL_ADDRESS + "}}.pdf";

   private PrintStream printStream;
   private ExcelReader rowReader= null;
   private TextBuilder textBuilder = null;
   private List<String> groupColumns = null;
   private BulkPdf bulkPdf;

   private static void deleteGeneratedFiles(final File generatedDir) {
      assertTrue(generatedDir.isDirectory());
      List<String> bulkGenerated =
            Arrays.asList(generatedDir.list())
                  .stream()
                  .filter(n -> n.startsWith("AATG ") || n.contains("Acceptance"))
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

      emptyFieldMap.put("pdf4", Collections.emptyMap());
      return emptyFieldMap;
   }

   private List<ExcelRow> getMockRows(final boolean includeEmail) {
      String email = EMAIL_ADDRESS + ":mary.and.michael@somedomain.org";

      List<ExcelRow> rows = new ArrayList<>();
      List<String> rowDef1 = new ArrayList<>(Arrays.asList(new String[]
            {"Name:S, Leo", "LehrerIn:Mr. Cool1", "Level:4", "Room:123",
             "Schule:GSSB", "Klasse:1B", "secret:abc1", "Award:Goldurkunde",
             "FamilyID:S1234", "ParentName:Mary & Michael S"}));
      if (includeEmail) rowDef1.add(email);
      rows.add(createMockRow(rowDef1));
      List<String> rowDef2 = new ArrayList<>(Arrays.asList(new String[]
            {"Name:S, Gwen", "LehrerIn:Mr. Cool2", "Level:5", "Room:223",
             "Schule:GSSB", "Klasse:2B", "secret:abc2", "Award:Goldurkunde",
             "FamilyID:S1234", "ParentName:Mary & Michael S"}));
      if (includeEmail) rowDef2.add(email);
      rows.add(createMockRow(rowDef2));
      List<String> rowDef3 = new ArrayList<>(Arrays.asList(new String[]
            {"Name:S, Helene", "LehrerIn:Mr. Cool3", "Level:6", "Room:323",
             "Schule:GSSB", "Klasse:3B", "secret:abc3", "Award:Participation",
             "FamilyID:S1234", "ParentName:Mary & Michael S"}));
      if (includeEmail) rowDef3.add(email);
      rows.add(createMockRow(rowDef3));
      return rows;
   }

   @BeforeAll
   public static void clean() {
      deleteGeneratedFiles(new File(GENERATED_DIR));
   }

   @BeforeEach
   public void setUp() throws Exception {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      this.printStream = new PrintStream(baos, true, "UTF-8");
      AppProperties props = mock(AppProperties.class);
      when(props.getSourceFolder()).thenReturn("sources");
      when(props.getGeneratedFolder()).thenReturn("generated");
      when(props.getExcelFileName()).thenReturn("Dummy.xlsx");
      when(props.getFileNameTemplate()).thenReturn(FILE_NAME_TEMPLATE);
      when(props.getFileGroupNameTemplate()).thenReturn(FILE_GROUP_NAME_TEMPLATE);


      List<String> emailColumn = new ArrayList<>();
      emailColumn.add(EMAIL_ADDRESS);
      when(props.getTargetEmailColumns()).thenReturn(emailColumn);
      this.groupColumns = new ArrayList<>();
      when(props.getGroupColumns()).thenReturn(this.groupColumns);
      when(props.getExcelSecretColumnName()).thenReturn("Key");

      this.rowReader = mock(ExcelReader.class);
      this.textBuilder = new TextBuilder();
      PdfFormFiller pdfFormFiller = new PdfFormFiller();
      this.bulkPdf = new BulkPdf(props, this.rowReader, this.textBuilder,
                                 pdfFormFiller, this.printStream);
   }

   @AfterEach
   public void closeResource() throws Exception {
      this.printStream.close();
   }

   @Test
   public void testThreeRow() throws EncryptedDocumentException,
                                     InvalidFormatException, IOException {
      when(this.rowReader.read(new File(ROOT + "/sources/Dummy.xlsx"), "Dummy"))
          .thenReturn(getMockRows(false));

      Template template =
            TemplateHelper.createTemplate("pdf1", Paths.get(TEMPLATE1));
      List<Template> alwaysInclude = new ArrayList<>();
      alwaysInclude.add(template);

      List<Choice> choices = createMockChoices("AATG Cert");

      Map<String, Map<String, String>> fieldMaps = defineFieldMaps();

      List<UnitOfWork> uows =
         this.bulkPdf.createPdfs(ROOT, "Dummy", MASTER_KEY, "secret",
                                 alwaysInclude, choices, fieldMaps,
                                 Optional.empty(), false);
      assertEquals(3, uows.size());

      int i=0;
      List<String> names = Arrays.asList("Leo", "Gwen", "Helene");
      for (UnitOfWork uow : uows) {
         List<String> expected0 = Arrays.asList(new String[]
               {names.get(i), "Mr. Cool" + (i+1), (4+i)+"", (i+1)+"B", "TESTERGEBNIS"});
         String reward = i>1 ? "Participation" : "Goldurkunde";
         List<String> expected1 = Arrays.asList(new String[]
               {names.get(i), "Mr. Cool" + (i+1), (4+i)+"", "GSSB", reward});

         assertEquals(2, uow.getGeneratedFiles().size());

         File validate0 = uow.getGeneratedFiles().get(0);
         assertTrue(validate0.getName().startsWith("AATG Raw Score"));
         assertTrue(validate0.getName().endsWith("S, " + names.get(i)+".pdf"));
         validatePDFDocument(validate0, "abc"+(i+1), expected0);

         File validate1 = uow.getGeneratedFiles().get(1);
         assertTrue(validate1.getName().startsWith("AATG Cert"));
         assertTrue(validate1.getName().endsWith("S, " + names.get(i)+".pdf"));
         validatePDFDocument(validate1, "abc"+(i+1), expected1);
         i++;
      }
   }

   @Test
   public void testMultiRowPdf() throws EncryptedDocumentException,
                                        InvalidFormatException, IOException {
      this.groupColumns.add(EMAIL_ADDRESS);
      this.groupColumns.add("FamilyID");
      this.groupColumns.add("ParentName");
      when(this.rowReader.read(new File(ROOT + "/sources/Dummy.xlsx"), "Dummy"))
          .thenReturn(getMockRows(true));

      Template template = TemplateHelper.createTemplate("pdf4", Paths.get(TEMPLATE4));
      List<Template> alwaysInclude = new ArrayList<>();
      alwaysInclude.add(template);

      List<Choice> choices = createMockChoices("AATG Cert");

      Map<String, Map<String, String>> fieldMaps = defineFieldMaps();

      List<UnitOfWork> uows =
         this.bulkPdf.createPdfs(ROOT, "Dummy", MASTER_KEY, "secret",
                                 alwaysInclude, choices, fieldMaps,
                                 Optional.empty(), false);
      assertEquals(1, uows.size());


      List<String> expected = new ArrayList<>(Arrays.asList(
                               "S1234", "Mary & Michael S"));
      List<String> names = Arrays.asList("Leo", "Gwen", "Helene");
      for (int i=0; i<3; i++) {
         expected.addAll(Arrays.asList("S, " + names.get(i), (i+1)+"23",
                                       "Mr. Cool" + (i+1)));
      }

      File validate = uows.get(0).getGeneratedFiles().get(0);
      assertTrue(validate.getName().startsWith("2018-2019 Acceptance - "));
      assertTrue(validate.getName().endsWith("mary.and.michael@somedomain.org.pdf"));
      validatePDFDocument(validate, null, expected);

   }

}
