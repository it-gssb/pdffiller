package org.gssb.pdffiller.excel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RowGroup {
   
   private final String groupIdName;
   private final List<ExcelRow> detailRecords;
   
   public RowGroup(final String groupIdName, final List<ExcelRow> rowGroup) {
      assert(rowGroup!=null && rowGroup.size()>0);
      this.groupIdName = groupIdName;
      this.detailRecords = new ArrayList<>(rowGroup);
   }

   public Optional<String> getGroupId() {
      return this.groupIdName != null ? Optional.of(this.detailRecords.get(0)
                                                        .getValue(this.groupIdName)
                                                        .getColumnValue())
                                      : Optional.empty();
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
