package org.gssb.pdffiller.excel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RowGroup {
   
   private final Set<String>    groupColumns;
   private final ExcelRow       headerColumns;
   private final List<ExcelRow> detailRecords;
   
   protected RowGroup(final List<ExcelRow> rowGroup, final Set<String> groupColumns) {
      assert(rowGroup!=null && rowGroup.size()>0 && groupColumns!=null);
      this.groupColumns = groupColumns;
      this.headerColumns = createHeaderRow(rowGroup, groupColumns);
      // TODO maybe remove header rows
      this.detailRecords = new ArrayList<>(rowGroup);
   }

   private ExcelRow createHeaderRow(final List<ExcelRow> rowGroup,
                                    final Set<String> groupColumns) {
      ExcelRow headerColumns = new ExcelRow();
      ExcelRow firstRow = rowGroup.get(0);
      for (String columnName : groupColumns) {
         ExcelCell cell = firstRow.getValue(columnName);
         headerColumns.addExcelCell(cell);
      }
      return headerColumns;
   }
   
   public int size() {
      return detailRecords.size();
   }
   
   public ExcelCell getValue(final String columnName, final int index) {
      if (index-1 > size()) {
         throw new IndexOutOfBoundsException("Index must be in range 0 to " + (size()-1));
      }
      
      ExcelCell value;
      if (this.groupColumns.contains(columnName)) {
         value = this.headerColumns.getValue(columnName);
      } else {
         value = this.detailRecords.get(index).getValue(columnName);
      }
      return value;
   }

}
