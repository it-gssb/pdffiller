package org.gssb.pdffiller.text;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;

public class TextBuilder {
   
   private MustacheFactory createSimpleMustacheFactory() {
     return new DefaultMustacheFactory() {
                  @Override
                  public void encode(String value, Writer writer) {
                     try {
                        // avoid encoding characters such as & and ' because we 
                        // create plain text only
                        int length = value.length();
                        for (int i = 0; i < length; i++) {
                          writer.write(value.charAt(i));
                        }
                      } catch (IOException e) {
                        throw new MustacheException("Failed to encode value: " + value, e);
                      }
                  }
               };
   }
   
   public String substitute(final String message, 
                            final Map<String, String> row) throws IOException {
      MustacheFactory mf = createSimpleMustacheFactory();
      Mustache mustache = 
            mf.compile(new StringReader(message), "message.text");
      try (StringWriter stringWriter = new StringWriter()) {
         mustache.execute(stringWriter, new MustacheMapDecorator(row))
                 .flush();
         return stringWriter.toString();
      }
   }

   public String substitute(final File template,
                            final Map<String, String> row) throws IOException {
      FileReader templateReader =  new FileReader(template);
      MustacheFactory mf = createSimpleMustacheFactory();
      Mustache mustache = mf.compile(templateReader, "message.file");
      try (StringWriter stringWriter = new StringWriter()) {
	     mustache.execute(stringWriter, new MustacheMapDecorator(row))
	             .flush();
	     return stringWriter.toString();
      }
   }
      
}
