package org.gssb.pdffiller.template;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public class TemplateHelper {
    public static Template createTemplate(final String key, final Path path) {
       return new Template (key, path);
    }
    
    public static Choice createChoice(final String excelColumn,
                                      final Optional<String> baseName,
                                      final Map<String, Template> templateChoices) {
       return new Choice(excelColumn, baseName, templateChoices);
    }
}
