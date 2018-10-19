package org.gssb.pdffiller.template;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.gssb.pdffiller.config.AppProperties;
import org.gssb.pdffiller.exception.UnrecoverableException;
import org.gssb.pdffiller.template.Choice;
import org.gssb.pdffiller.template.TemplateBuilder;
import org.gssb.pdffiller.template.Template;
import org.junit.Before;
import org.junit.Test;

public class TemplateBuilderTest {

   private final static String BASE_PATH   = "src/test/resources/2018";
   private final static String P_PATH      = BASE_PATH + "/config";
   private final static String PROPS2      = "test2.properties";
   
   private TemplateBuilder select;
   
   @Before
   public void setUp() throws Exception {
       Path config = new File(P_PATH, PROPS2).toPath();
       AppProperties props = new AppProperties(config);
       this.select = new TemplateBuilder(props, BASE_PATH + "/" + props.getSourceFolder());
   }
   
   @Test
   public void testCollectOneChoice() {
      List<Choice> choices = this.select.collectChoices();
      assertNotNull(choices);
      assertEquals(1, choices.size());
      
      assertEquals("Template2", choices.get(0).getSelectionColumn());
      assertTrue(choices.get(0).getBaseName().isPresent());
      assertEquals("Certificate", choices.get(0).getBaseName().get());
      
      assertEquals(5, choices.get(0).getKeys().size());
      assertTrue(choices.get(0).getKeys().contains("gold"));
      assertTrue(choices.get(0).getKeys().contains("silver"));
      assertTrue(choices.get(0).getKeys().contains("bronze"));
      assertTrue(choices.get(0).getKeys().contains("achievement"));
      assertTrue(choices.get(0).getKeys().contains("participation"));
      
      Optional<Template> template = choices.get(0).select("gold");
      assertTrue(template.isPresent());
      
      assertEquals("pdf2", template.get().getKey());
      assertEquals("AATG Gold.pdf", template.get().getTemplateFileName());
      File expectedFile = 
            new File ("src/test/resources/2018/sources/sources/AATG Gold.pdf");
      assertEquals(expectedFile.getPath(), template.get().getTemplatePath().toString());
   }
   
   @Test
   public void testAlwaysInclude() {
      List<Template> always = this.select.allwaysInclude();
      assertNotNull(always);
      assertEquals(1, always.size());
      assertEquals("pdf1", always.iterator().next().getKey());
   }

   @Test (expected=UnrecoverableException.class)
   public void testMissingChoiceMap() {
      final String PROPS1 = "test1.properties";
      Path config = new File(P_PATH, PROPS1).toPath();
      AppProperties props = new AppProperties(config);
      this.select = new TemplateBuilder(props, BASE_PATH + "/" + props.getSourceFolder());
      this.select.collectChoices();
   }
   
}
