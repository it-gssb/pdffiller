package org.gssb.pdffiller.excel;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.gssb.pdffiller.excel.ExcelRow;
import org.gssb.pdffiller.excel.RowReader;
import org.junit.Test;

public class RowReaderTest {
   
   private final static String EXCEL_FILE = "src/test/resources/2018/sources/GSSB Raw Results.xlsx";
   private final static String SHEET_NAME = "Testergebnisse";

   @Test
   public void testReadSample() {
      RowReader reader = new RowReader();
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
