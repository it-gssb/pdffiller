package org.gssb.pdffiller;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
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
	
   private static final String CHARACTER_SET = "utf-8";
   
   private final static String EOL = System.getProperty("line.separator");
	
	private final static String INCORRECT_TEMPLATE_FILE_ERROR =
	      "File '%s' does not exist. Please correct your configurations.";
	
	private static final String TEMPLATE_NOT_READ_ERROR =
	      "The template body located in file '%s' cannot be read.";
	
   private static final String EMAIL_BODY_ENCODING_ERROR =
         "An text encoding error occured after the template body was read.";
	
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
	
   private void deliverEmails(final List<UnitOfWork> createdUnits, 
                              final Configuration config,
                              final AppProperties properties) { 
      File bodyTemplate = new File (new File(config.root,
                                             properties.getSourceFolder()), 
                                    properties.getEmailBodyFile());
      if (bodyTemplate.isDirectory() || !bodyTemplate.exists()) {
         String msg = String.format(INCORRECT_TEMPLATE_FILE_ERROR, 
                                    bodyTemplate.getAbsolutePath());
         throw new UnrecoverableException(msg);
      }
    
      String bodyTemplateText;
      try {
         byte[] encoded = Files.readAllBytes(Paths.get(bodyTemplate.toURI()));
         bodyTemplateText = new String(encoded, CHARACTER_SET);
      } catch (UnsupportedEncodingException e) {
         String msg = String.format(EMAIL_BODY_ENCODING_ERROR);
         logger.error(msg, e);
         throw new UnrecoverableException(msg, e);
      } catch (IOException e) {
         String msg = String.format(TEMPLATE_NOT_READ_ERROR,
                                    bodyTemplate.getAbsolutePath());
         logger.error(msg, e);
         throw new UnrecoverableException(msg, e);
      }
      
      BulkEmail bulkEmail = createBulkEmail(properties);
      bulkEmail.sendEmails(createdUnits, config.simulate,
                           properties.getEmailHost(),
                           properties.getEmailPort(), 
                           properties.getEmailAddress(),
                           properties.getEmailReturnAddress(),
                           config.emailPassword, bodyTemplateText);
   }
	
   	public void run(final String[] args) {
	   Configuration config = parse(args);
      AppProperties properties = createConfiguration(config);
      try {
         TemplateBuilder choiceBuilder = 
               createTemplateBuilder(config, properties);
         List<Template> alwaysInclude = choiceBuilder.allwaysInclude();
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
            logger.error(msg, e);
            throw new UnrecoverableException(msg, e);
         }
         if (!config.emailPassword.isEmpty()) {
            deliverEmails(createdUnits, config, properties);
         }
      } catch (UnrecoverableException e) {
         String cause = e.getCause()!=null && e.getCause().getMessage()!=null
                             ?  EOL + e.getCause().getMessage()
                             : "";
         String msg = "Application stopped after failure and is unable to proceed. ";
         System.err.println();
         System.err.println(msg + e.getMessage() + cause);
         logger.error(msg, e);
         System.exit(1);
      } catch (Throwable e) {
         String msg = "Application stopped after an unexpected error occurred: ";
         System.err.println();
         System.err.println(msg + e.getMessage());
         logger.error(msg, e);
         System.exit(1);
      }
      System.exit(0);
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
