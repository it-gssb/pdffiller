package org.gssb.pdffiller.excel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.gssb.pdffiller.exception.UnrecoverableException;

public class RowReader {
   
   private final static Logger logger = LogManager.getLogger(RowReader.class);
   
   private static final String MISSING_SHEET_ERROR = 
         "The Excel sheet %s is not defined in the Excel workbook %s.";
   
   private String getValue(final Cell cell, final FormulaEvaluator evaluator) {
      CellValue cellValue = evaluator.evaluate(cell);

      String value;
      switch (cellValue.getCellType()) {
      case BOOLEAN:
         value = Boolean.toString(cellValue.getBooleanValue());
         break;
      case NUMERIC:
         double dvalue = cellValue.getNumberValue();
         int    ivalue = (int) dvalue;
         value = (dvalue - ivalue < 0.000000001)
                         ? value = "" + ivalue
                         : Double.toString(cellValue.getNumberValue());
         break;
      case STRING:
         value = cellValue.getStringValue();
         break;
      case BLANK:
         value = "";
         break;
      case ERROR:
         value = "";
         break;
      // CELL_TYPE_FORMULA will never happen
      case FORMULA:
         value = "";
         break;
      default:
         value = "";
      } 
      return value;
   }
   
   private boolean isRowEmpty(final Row row, final FormulaEvaluator evaluator) {
      boolean isEmpty = true;
      for (Cell cell : row) {
         String cellValue = getValue(cell, evaluator);
         if (cellValue != null && !cellValue.equals("") && !cellValue.equals("0")) {
            isEmpty = false;
            break;
         }
      }
      return isEmpty;
   }

   public List<ExcelRow> read(final File excelFile, final String sheetName)
                         throws IOException, EncryptedDocumentException,
                                             InvalidFormatException {

      // Creating a Workbook from an Excel file (.xls or .xlsx)
      assert(excelFile.exists()) : "File does not exist: " + excelFile.toString();
      Workbook workbook = WorkbookFactory.create(excelFile);
      FormulaEvaluator evaluator = workbook.getCreationHelper()
                                           .createFormulaEvaluator();

      Sheet sheet = workbook.getSheet(sheetName);
      if (sheet == null) {
         String msg = String.format(MISSING_SHEET_ERROR, sheetName, 
                                    excelFile.getCanonicalPath());
         logger.error(msg);
         throw new UnrecoverableException(msg);
      }

      assert (sheet.getPhysicalNumberOfRows() > 0);

      // read header
      List<String> header = new ArrayList<>();
      Row topRow = sheet.getRow(0);
      assert(!isRowEmpty(topRow, evaluator));
      for (Cell cell : topRow) {
         String cellValue = getValue(cell, evaluator);
         header.add(cellValue);
      }

      // read data rows
      List<ExcelRow> excelRows = new ArrayList<>();
      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
         Row currentRow = sheet.getRow(i);
         // skip empty row
         if (isRowEmpty(currentRow, evaluator)) continue;
         
         int columnNumber = 0;
         ExcelRow excelRow = new ExcelRow();
         for (Cell cell : currentRow) {
            assert(columnNumber < header.size());
            String cellValue = getValue(cell, evaluator);
            excelRow.addExcelCell(new ExcelCell(columnNumber, header.get(columnNumber),
                                                cellValue));
            columnNumber++;
         }
         excelRows.add(excelRow);
      }

      // Closing the workbook
      workbook.close();
      
      return Collections.unmodifiableList(excelRows);
   }
}
