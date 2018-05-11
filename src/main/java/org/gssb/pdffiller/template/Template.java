package org.gssb.pdffiller.template;

import java.nio.file.Path;
import java.util.Objects;

public class Template {

   private final String key;
   private final Path   templatePath;

   Template(final String key, final Path templatePath) {
      super();
      Objects.requireNonNull(key);
      assert (!key.isEmpty());
      Objects.requireNonNull(templatePath);
      assert (!templatePath.toString().isEmpty());
   
      this.key = key;
      this.templatePath = templatePath;
   }

   public String getKey() {
      return this.key;
   }

   public Path getTemplatePath() {
      return this.templatePath;
   }

   public String getTemplateFileName() {
      return this.templatePath.getFileName().toString();
   }
	
}
