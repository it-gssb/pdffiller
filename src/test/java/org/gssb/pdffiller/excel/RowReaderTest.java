package org.gssb.pdffiller.excel;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.gssb.pdffiller.config.AppProperties;
import org.gssb.pdffiller.excel.ExcelReader;
import org.junit.Test;

public class RowReaderTest {
   
   private final static String EXCEL_FILE = "src/test/resources/2018/sources/GSSB Raw Results.xlsx";
   private final static String SHEET_NAME = "Testergebnisse";

   private AppProperties createMockProperties() {
      AppProperties props = mock(AppProperties.class);
      List<String> emailColumn = new ArrayList<>();
      emailColumn.add("PrimaryParentEmail");
      when(props.getGroupColumns()).thenReturn(emailColumn);
      when(props.getTargetEmailColumns()).thenReturn(emailColumn);
      when(props.getExcelSecretColumnName()).thenReturn("Key");
      return props;
   }
   
   @Test
   public void testReadSample() {
      AppProperties props = createMockProperties();
      ExcelReader reader = new ExcelReader(props);
      List<ExcelRow> excelRows = null;
      try {
         excelRows = reader.read(new File(EXCEL_FILE), SHEET_NAME);
      } catch (EncryptedDocumentException e) {
         fail("unexpected exception");
      } catch (InvalidFormatException e) {
         fail("unexpected exception");
      } catch (IOException e) {
         e.printStackTrace();
         fail("unexpected exception");
      }
      
      assertNotNull(excelRows);
      assertEquals("incorrect count", 5, excelRows.size());

//      System.err.println(excelRows.get(0).printHeaders());
//      for (ExcelRow row : excelRows) {
//         System.err.println(row.printValues());
//      }

   }

}
