package org.gssb.pdffiller.text;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.github.mustachejava.MustacheException;

public class TextBuilderTest {

   private final static String M_TEMPLATE = 
         "src/test/resources/2018/sources/earth_type.mustache";

   private TextBuilder textBuilder;

   @Before
   public void setUp() throws Exception {
      this.textBuilder = new TextBuilder();
   }

   @Test
   public void testSubstituteInString() {
      Map<String, String> variables = new HashMap<>();
      variables.put("type", "Goldilocks");
      String template = "Earth is in the {{type}} zone.";
      try {
         assertEquals("Earth is in the Goldilocks zone.",
               this.textBuilder.substitute(template, variables));
      } catch (IOException e) {
         fail("unexpected i/o error");
      }
   }

   @Test(expected = MustacheException.class)
   public void testSubstituteInStringMissingVariable() throws IOException {
      Map<String, String> variables = new HashMap<>();
      variables.put("x", "Goldilocks");
      String template = "Earth is in the {{type}} zone.";
      this.textBuilder.substitute(template, variables);
   }

   @Test
   public void testSubstituteInFile() {
      File templateFile = new File(M_TEMPLATE);
      Map<String, String> variables = new HashMap<>();
      variables.put("type", "Goldilocks");
      try {
         assertEquals("Earth is in the Goldilocks zone.",
               this.textBuilder.substitute(templateFile, variables));
      } catch (IOException e) {
         fail("unexpected i/o error");
      }
   }

}
