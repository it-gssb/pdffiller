package org.gssb.pdffiller.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gssb.pdffiller.exception.UnrecoverableException;

public abstract class AbstractConfiguration {

	private static final Logger logger = LogManager.getLogger(AbstractConfiguration.class);

	private static final String ERROR_MSG_1 = 
			"The required configuration file %s has not been found. Please " +
			"define the property file %s and rerun the command.";

	private static final String ERROR_MSG_2 = 
			"The required properties file '%s' does not exist.";

	private static final String ERROR_MSG_3 =
			"Difficulties accessing properties file '%s'.";

	private static final String ERROR_MSG_4 =
			"The configuration file '%s' is not correctly defined. " +
			"Please correct the issue.";

	private static final String ERROR_MSG_5 = 
			"The properties file name must not be 'null'";
	
	private static final String ERROR_MSG_6 =
			"The property '%s' expects a comma-separted list of name-value " +
			"pairs that are separated by a ':' character. This ':' " +
         "separator is missing. The key '%s' is ignored from the map.";
	
	private static final String ERROR_MSG_7 =
			"The property '%s' expects a comma-separted list of name-value " +
			"pairs that have a non-empty name. The entry is ignored";
	
	private static final String ERROR_MSG_8 =
			"The property '%s' expects a comma-separted list of name-value " +
			"pairs that have a non-empty value. The entry is ignored";

	private Configuration config = null;
	
	private String stripOuterQuotes(final String property) {
	   String result = property;
	   if (property.startsWith("\"") && property.endsWith("\"")) {
	      result = property.substring(1, property.length()-1);
	   }
	   return result;
	}

	protected AbstractConfiguration(final Path propertyFile) {
		Objects.requireNonNull(propertyFile, ERROR_MSG_5);
		if (!propertyFile.toFile().exists() ||
		    propertyFile.toFile().isDirectory()) {
			String msg = String.format(ERROR_MSG_2, propertyFile.toAbsolutePath());
			logger.error(msg);
			throw new UnrecoverableException(msg);
		}

		String absoluteFilePath = "";
		try {
			absoluteFilePath = propertyFile.toFile().getCanonicalPath();
		} catch (IOException e) {
			String msg = String.format(ERROR_MSG_3, propertyFile);
			logger.error(msg, e);
			throw new UnrecoverableException(msg, e);
		}

		if (!propertyFile.toFile().exists()) {
			String msg = String.format(ERROR_MSG_1, absoluteFilePath,
			                           propertyFile);
			logger.error(msg);
			throw new UnrecoverableException(msg);
		}

		init(absoluteFilePath);
	}

	private void init(final String propertyFileName) {
		assert (propertyFileName != null);
		Parameters params = new Parameters();
		FileBasedConfigurationBuilder<PropertiesConfiguration> builder = new FileBasedConfigurationBuilder<>(
				PropertiesConfiguration.class)
						.configure(params.fileBased().setListDelimiterHandler(new DefaultListDelimiterHandler(','))
								.setFileName(propertyFileName));
		builder.setAutoSave(true);

		PropertiesConfiguration propertiesConfig;
		try {
			propertiesConfig = builder.getConfiguration();
			this.config = propertiesConfig.interpolatedConfiguration();
		} catch (ConfigurationException e) {
			String msg = String.format(ERROR_MSG_4, propertyFileName);
			logger.error(msg, e);
			throw new UnrecoverableException(msg, e);
		}
	}

	protected List<String> getPropertyKeys() {
		List<String> keys = new ArrayList<>();
		Iterator<String> iterator = this.config.getKeys();
		while (iterator.hasNext()) {
			keys.add(iterator.next());
		}
		return keys;
	}
	
	protected List<String> getKeysWithBase(final String base) {
      return getPropertyKeys().stream()
                              .filter(k -> k.startsWith(base + ".") &&
                                      k.length() > base.length() + 1)
                              .map(k -> k.substring(base.length() + 1, 
                                                    k.indexOf(".", base.length() + 1)>0
                                                     ? k.indexOf(".", base.length() + 1)
                                                     : k.length()) )
                             .distinct()
                             .collect(Collectors.toList());
   }

   protected String getProperty(final String key) {
      Object property = this.config.getProperty(key);
      if (property != null) {
         return stripOuterQuotes(property.toString());
      }
      return null;
   }

   protected List<String> getPropertyList(final String key) {
      String[] properties = this.config.getStringArray(key);
      return Arrays.asList(properties)
                   .stream()
                   .map(p -> stripOuterQuotes(p))
                   .collect(Collectors.collectingAndThen(Collectors.toList(),
                                                         Collections::unmodifiableList));
   }

   protected Map<String, String> getPropertyMap(final String key) {
      Map<String, String> pMap = new HashMap<>();
      List<String> list = getPropertyList(key);
      for (String entry : list) {
         String[] pair = entry.split(":", 2);
         if (pair.length != 2) {
            String name = !pair[0].isEmpty() ? pair[0] : "undefined";
            String msg = String.format(ERROR_MSG_6, key, name);
            logger.error(msg);
            throw new UnrecoverableException(msg);
         } else if (pair[0].isEmpty()) {
            String msg = String.format(ERROR_MSG_7, key);
            logger.error(msg);
            continue;
         } else if (pair[1].isEmpty()) {
            String msg = String.format(ERROR_MSG_8, key);
            logger.error(msg);
            continue;
         } else {
            pMap.put(stripOuterQuotes(pair[0].trim()), 
                     stripOuterQuotes(pair[1].trim()));
         }
      }
      return Collections.unmodifiableMap(pMap);
   }

}
