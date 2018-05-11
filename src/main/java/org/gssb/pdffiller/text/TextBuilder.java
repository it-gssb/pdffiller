package org.gssb.pdffiller.text;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

public class TextBuilder {
   
   public String substitute(final String message, 
                            final Map<String, String> row) throws IOException {
      MustacheFactory mf = new DefaultMustacheFactory();
      Mustache mustache = 
            mf.compile(new StringReader(message), "message.text");
      StringWriter stringWriter = new StringWriter();
      mustache.execute(stringWriter, new MustacheMapDecorator(row))
              .flush();
      return stringWriter.toString();
    }
   
   public String substitute(final File template,
                            final Map<String, String> row) throws IOException {
      FileReader templateReader =  new FileReader(template);
      MustacheFactory mf = new DefaultMustacheFactory();
      Mustache mustache = mf.compile(templateReader, "message.file");
      StringWriter stringWriter = new StringWriter();
      mustache.execute(stringWriter, new MustacheMapDecorator(row))
              .flush();
      return stringWriter.toString();
   }
      
}
