package org.gssb.pdffiller.email;

import javax.mail.Provider;

public class MockProvider extends Provider {

   public MockProvider(Type type, String protocol, String classname,
                       String vendor, String version) {
      super(type, protocol, classname, vendor, version);
   }

}
