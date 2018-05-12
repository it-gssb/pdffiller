package org.gssb.pdffiller;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gssb.pdffiller.config.AppProperties;
import org.gssb.pdffiller.email.BulkEmail;
import org.gssb.pdffiller.excel.ColumnNotFoundException;
import org.gssb.pdffiller.exception.UnrecoverableException;
import org.gssb.pdffiller.pdf.BulkPdf;
import org.gssb.pdffiller.pdf.UnitOfWork;
import org.gssb.pdffiller.template.Choice;
import org.gssb.pdffiller.template.TemplateBuilder;
import org.gssb.pdffiller.template.Template;

@SuppressWarnings("deprecation")
public class BulkNotificator {
	private final static Logger logger = 
			             LogManager.getLogger(BulkNotificator.class);

	private static class Configuration {
	   Path    configFile    = null;
	   String  root          = "";
	   boolean simulate      = false;
	   String  emailPassword = "";
	   String  masterKey     = "";
	}
	
	private final static String INCORRECT_SECRET_COLUMN =
	      "The defined secret column %s is not available in the " +
	      "input spreadsheet. Please correct the configuration.";
	
	private final static String NO_ENCRYPTION = "";

	private final Options options;

	public BulkNotificator() {
		super();
		options = new Options();
		options.addOption("h", "help", false, "Show help.");
		options.addOption("c", "configuration", true, "Configuration file for PDF Mail Merge application.");
		options.addOption("p", "password", true, "Email account user password.");
		options.addOption("m", "master-key", true, "Master key for PDF encryption");
		options.addOption("s", "suppress", false, "Logs email instead of sending them.");
	}
	
	//
	// factory methods to aid validation
	//
	
	protected AppProperties createConfiguration(final Configuration config) {
      return new AppProperties(config.configFile);
   }
   
   protected BulkEmail createBulkEmail(final AppProperties properties) {
      return new BulkEmail(properties);
   }

   protected BulkPdf createBulkPdf(final AppProperties properties) {
      return new BulkPdf(properties);
   }

   protected TemplateBuilder createTemplateBuilder(
                                         final Configuration config,
                                         final AppProperties properties) {
      return new TemplateBuilder(properties, config.root);
   }
   
   //
   // Solution
   //
	
	private void help() {
		HelpFormatter formater = new HelpFormatter();
		formater.printHelp(BulkNotificator.class.getCanonicalName(), options);
		System.exit(1);
	}
	
	private String getRoot(final String pathToConfig) {
	   Path configPath = Paths.get(pathToConfig);
	   return configPath.getParent()
	                    .getParent()
	                    .toString();
	}
	
	private Configuration parse(final String[] args) {
		Configuration config = new Configuration();
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);

			if (cmd.hasOption("h"))
				help();

			if (cmd.hasOption("c")) {
			   config.configFile = Paths.get(cmd.getOptionValue("c"));
				config.root = getRoot(cmd.getOptionValue("c"));
			} else {
				logger.error("Missing configuration file option");
				help();
			}
			
         if (cmd.hasOption("m")) {
            config.masterKey = cmd.getOptionValue("m");
         } else {
            config.masterKey = NO_ENCRYPTION;
         }
			
			if (args.length==2 || args.length==4 && cmd.hasOption("m")) {
				// this implies that only PDF documents are produced
				return config;
			}
			
			if (cmd.hasOption("p")) {
				config.emailPassword = cmd.getOptionValue("p");
			} else {
				logger.error("Missing email account password.");
				help();
			}

         if (cmd.hasOption("s")) {
            config.simulate = true;
         }
				
		} catch (ParseException e) {
			logger.error("Failed to parse comand line properties", e);
			help();
		}
		return config;
	}
	
	private Map<String, Map<String, String>> 
	        getFormFieldMaps(final AppProperties properties) {
	   return properties.getTemplateKeys()
	                    .stream()
	                    .collect(Collectors.toMap(k -> k,
	                                              k -> properties.getMappings(k)));
	}
	
   	public void run(final String[] args) {
	   Configuration config = parse(args);
      AppProperties properties = createConfiguration(config);
      try {
         TemplateBuilder choiceBuilder = 
               createTemplateBuilder(config, properties);
         Set<Template> alwaysInclude = choiceBuilder.allwaysInclude();
         List<Choice>  choices = choiceBuilder.collectChoices();
         
         // pdf key -> (acro field -> spreadsheet field)
         Map<String, Map<String, String>> formFieldMaps =
               getFormFieldMaps(properties);
         
         String secretColumnName = properties.getExcelSecretColumnName();
         
         BulkPdf bulkPdf = createBulkPdf(properties);
         List<UnitOfWork> createdUnits;
         try {
            createdUnits = 
               bulkPdf.createPdfs(config.root, properties.getExcelSheetName(),
                                  config.masterKey, secretColumnName,
                                  alwaysInclude, choices, formFieldMaps);
         } catch (ColumnNotFoundException e) {
            String msg = String.format(INCORRECT_SECRET_COLUMN,
                                       secretColumnName);
            throw new UnrecoverableException(msg, e);
         }
         if (!config.emailPassword.isEmpty()) {
            BulkEmail bulkEmail = createBulkEmail(properties);
            bulkEmail.sendEmails(createdUnits, config.root,
                                 config.simulate,
                                 properties.getEmailHost(), 
                                 properties.getEmailPort(),
                                 properties.getEmailAddress(),
                                 properties.getEmailReturnAddress(),
                                 config.emailPassword);
         }
      } catch (UnrecoverableException e) {
         String cause = e.getCause()!=null && e.getCause().getMessage()!=null
                          ?  " - " + e.getCause().getMessage()
                          : "";
         System.err.println("Unable to proceed due to error: " + 
                             e.getMessage() + cause);
         System.exit(1);
      } catch (Throwable e) {
         System.err.println("An unexpected error occurred: " + e.getMessage());
         e.printStackTrace();
         System.exit(1);
      }
	}

	/**
	 * Configuration:
	 * 
	 * -c /Users/michaelsassin/Documents/GSSB/tools/pdffiller/src/test/resources/2018 [-m **master-key***] [-p ***pwd***] [-s]
	 * 
	 * @param args
	 */
	public static void main(final String[] args) {
		BulkNotificator bn = new BulkNotificator();
		bn.run(args);
	}

}
