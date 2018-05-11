package org.gssb.pdffiller.template;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.gssb.pdffiller.config.AppProperties;
import org.gssb.pdffiller.exception.UnrecoverableException;

public class TemplateBuilder {
   
   private final static String UNDEFINED_CHOICE      = 
         "The choice configuration '%s' does not define a mapping.";
   private final static String UNDEFINED_TEMPLATE   =
         "Undefined template '%s' referenced in choice options %s.";
   
   private final String folderPath;
   private final AppProperties props;

   public TemplateBuilder(final AppProperties props, final String folderPath) {
      super();
      this.props = props;
      this.folderPath = folderPath;
   }
   
   private Path getTemplatePath(final String key) {
      return Paths.get(this.folderPath,this.props.getSourceFolder(),
                       this.props.getFullTemplateKey(key));
   }
   
   private Map<String, Template> getTemplates() {
      return this.props
                 .getTemplateKeys()
                 .stream()
                 .collect(Collectors.toMap(n -> n, 
                                           n -> new Template(n, getTemplatePath(n))));
   }
   
   private Optional<String> findUndefinedTemplate(final Map<String, String> choices,
                                                  final Set<String> templateNames) {
      return choices.values()
                    .stream()
                    .filter(t -> !templateNames.contains(t))
                    .findFirst();
   }
   
   private Map<String, Template> joinChoices(final Map<String, String> choices,
                                             final Map<String, Template> templateDefinitions) {
      // stitch together choice definition and template definition
      return choices.entrySet()
                    .stream()
                    .collect(Collectors.toMap(e -> e.getKey(),
                                              e -> templateDefinitions.get(e.getValue())));
   }
   
   private Choice getChoice(final String choiceName, 
                            final String selectionColumn,
                            final Optional<String> baseName,
                            final Map<String, String> choices,
                            final Map<String, Template> templateDefinitions) {
      Optional<String> missingName =
            findUndefinedTemplate(choices, templateDefinitions.keySet());
      if (missingName.isPresent()) {
         String msg = String.format(UNDEFINED_TEMPLATE, missingName.get(),
                                    choiceName);
         throw new UnrecoverableException(msg);
      }
      Map<String, Template> joined = joinChoices(choices, templateDefinitions);
      
      if (joined==null || joined.isEmpty()) {
         String msg = String.format(UNDEFINED_CHOICE, choiceName);
         throw new UnrecoverableException(msg);
      }
      
      return new Choice(selectionColumn, baseName, joined);
   }

   public List<Choice> collectChoices() {
      Map<String, Template> templates = getTemplates();
      return this.props
                 .getChoiceKeys()
                 .stream()
                 .map(n -> getChoice(n, this.props.getSelectionColumn(n),
                                     this.props.getBaseName(n),
                                     this.props.getChoices(n), templates))
                 .collect(Collectors.collectingAndThen(Collectors.toList(),
                                                       Collections::unmodifiableList));
   }
   
   private Set<String> getTemplateNames(final Choice choice) {
      return choice.getKeys()
                   .stream()
                   .map(k -> choice.select(k).get().getKey())
                   .distinct()
                   .collect(Collectors.toSet());
   }
   
   public Set<Template> allwaysInclude() {
      Map<String, Template> templates = getTemplates();
      Set<String> usedInChoices =
            collectChoices().stream()
                            .flatMap(c -> getTemplateNames(c).stream())
                            .distinct()
                            .collect(Collectors.toSet());
      
      return templates.values()
                      .stream()
                      .filter(t -> !usedInChoices.contains(t.getKey()))
                      .collect(Collectors.collectingAndThen(Collectors.toSet(),
                                                            Collections::unmodifiableSet));
   }

}
