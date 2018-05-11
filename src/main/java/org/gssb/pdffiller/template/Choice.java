package org.gssb.pdffiller.template;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class Choice {

   private final String selectColumn;
   private final Optional<String> baseName;
   private final Map<String, Template> templateChoices;

   Choice(final String excelColumn,
          final Optional<String> baseName,
          final Map<String, Template> templateChoices) {
      super();
      Objects.requireNonNull(excelColumn);
      assert (!excelColumn.isEmpty());
      this.selectColumn = excelColumn;

      this.baseName = baseName;
      
      Objects.requireNonNull(templateChoices);
      assert (!templateChoices.isEmpty());
      this.templateChoices = new HashMap<>(templateChoices);
   }

   public String getSelectionColumn() {
      return this.selectColumn;
   }
   
   public Optional<String> getBaseName() {
      return this.baseName;
   }

   public Set<String> getKeys() {
      return this.templateChoices.keySet();
   }
   
   public Optional<Template> select(final String key) {
      return Optional.ofNullable(this.templateChoices.get(key));
   }

}
