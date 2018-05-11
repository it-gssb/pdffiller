package org.gssb.pdffiller.excel;

public class ExcelCell {

   private final int columnIndex;
   private final String columnName;
   private final String columnValue;
   
   public ExcelCell(final int columnIndex, final String columnName,
                    final String columnValue) {
      super();
      this.columnIndex = columnIndex;
      this.columnName = columnName;
      this.columnValue = columnValue;
   }

   public int getColumnIndex() {
      return columnIndex;
   }

   public String getColumnName() {
      return columnName;
   }

   public String getColumnValue() {
      return columnValue;
   }
   
}
