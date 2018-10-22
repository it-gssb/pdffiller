package org.gssb.pdffiller.excel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RowGroup {
   
   private final List<ExcelRow> detailRecords;
   
   public RowGroup(final List<ExcelRow> rowGroup, final Set<String> groupColumns) {
      assert(rowGroup!=null && rowGroup.size()>0 && groupColumns!=null);
      this.detailRecords = new ArrayList<>(rowGroup);
   }

   public ExcelRow getHeadRow() {
      return this.detailRecords.get(0);
   }
   
   public List<ExcelRow> getRows() {
      return Collections.unmodifiableList(this.detailRecords);
   }
   
   public Map<String, String> createFormMap(final Set<String> groupColumns) {
      if (this.detailRecords.isEmpty()) {
         return Collections.emptyMap();
      }
      Map<String, String> headerMap = getHeadRow().createHeaderFormMap(groupColumns);
      
      Map<String, String> allMaps = new HashMap<>(headerMap);
      int index = 1;
      for (ExcelRow row : this.detailRecords) {
         allMaps.putAll(row.createFormMap(index++, groupColumns));
      }
      return allMaps;
   }
   
}
