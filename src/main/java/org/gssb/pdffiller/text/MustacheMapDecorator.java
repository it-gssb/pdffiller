package org.gssb.pdffiller.text;

import java.util.Map;

import org.apache.commons.collections4.map.AbstractMapDecorator;

public class MustacheMapDecorator 
                     extends AbstractMapDecorator<String, String> {
   
   MustacheMapDecorator(final Map<String, String> map) {
      super(map);
   }
   
   @Override
   public boolean containsKey(final Object key) {
       if (!super.containsKey(key)) {
           throw new IllegalStateException("Missing key: '" + key +"'");
       }
       return super.containsKey(key);
   }

   @Override
   public String get(final Object key) {
       if (!super.containsKey(key)) {
           throw new IllegalStateException("Missing key: '" + key +"'");
       }
       return super.get(key);
   }
}
