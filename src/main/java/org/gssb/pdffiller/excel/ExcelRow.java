package org.gssb.pdffiller.excel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ExcelRow {
   
   private static final String INDEX_FORM = "_%s";
   
   private final Map<Integer, ExcelCell> definedIndexes = new HashMap<>();
   private final Map<String, ExcelCell> row = new HashMap<>();
   
   public Map<String, ExcelCell> getRow() {
      return Collections.unmodifiableMap(this.row);
   }
   
   public Map<String, String> getRowMap() {
      return this.row
                 .entrySet()
                 .stream()
                 .collect(Collectors.toMap(e -> e.getKey(),
                                           e -> e.getValue()
                                                 .getColumnValue()));
   }
   
   public void addExcelCell(final ExcelCell cell) {
      ExcelCell original = this.row.put(cell.getColumnName(), cell);
      // check that cell is new or replaces a cell with the same index
      assert(original == null ||
            this.definedIndexes.get(cell.getColumnIndex())==original);
      this.definedIndexes.put(cell.getColumnIndex(), cell);
   }
   
   public ExcelCell getValue(final String columnName) {
      ExcelCell value = this.row.get(columnName);
      if (value==null && this.row.keySet().contains(columnName)) {
         throw new ColumnNotFoundException("Value for expected column not defined");
      }
      return value;
   }
   
   private String getFormNamePostfix(final int index) {
      return index>=0 ? String.format(INDEX_FORM, index) : "";
   }
   
   private Map<String, String> createKeyValueMap(final int index,
                                                 final Predicate<String> keyCondition) {
      String post = getFormNamePostfix(index);
      return this.row
                 .entrySet()
                 .stream()
                 .filter(e -> keyCondition.test(e.getKey()))
                 .collect(Collectors.toMap(e -> e.getKey() + post,
                                           e -> e.getValue()
                                                 .getColumnValue()));
   }
   
   Map<String, String> createHeaderFormMap(final Set<String> groupColumns) {
      Predicate<String> keyCondition = s -> groupColumns.contains(s);
      return createKeyValueMap(-1, keyCondition);
   }
   
   Map<String, String> createFormMap(final int index,
                                     final Set<String> groupColumns) {
      Predicate<String> keyCondition = s -> !groupColumns.contains(s);
      return createKeyValueMap(index, keyCondition);
   }
   
   public Map<String, String> createRowMap() {
      return createFormMap(-1, Collections.emptySet());
   }
   
   public String printHeaders() {
      return this.row.values()
                 .stream()
                 .sorted((c1, c2) -> c1.getColumnIndex() - c2.getColumnIndex())
                 .map(c -> c.getColumnName())
                 .collect(Collectors.joining(", "));
   }
   
   public String printValues() {
      return this.row.values()
                 .stream()
                 .sorted((c1, c2) -> c1.getColumnIndex() - c2.getColumnIndex())
                 .map(c -> c.getColumnValue())
                 .collect(Collectors.joining(", "));
   }
   
}
