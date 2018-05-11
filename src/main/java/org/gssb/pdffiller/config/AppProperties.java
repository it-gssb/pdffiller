package org.gssb.pdffiller.config;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gssb.pdffiller.exception.UnrecoverableException;

public class AppProperties extends AbstractConfiguration {
   
   private static final Logger logger = LogManager.getLogger(AppProperties.class);
   
   private static final String MISSING_CONFIGURATION = 
         "The mandatory property '%s' is not defined in the properties file.";
   
   private static final String MALFORMED_INTEGER  = 
         "The property value '%s' for property key '%s' must be an integer.";
   
   private static final String FOLDER_GENERATED             = "folder.generated";
   private static final String FOLDER_GENERATED_DEFAULT     = "generated";
   private static final String FOLDER_SOURCE                = "folder.sources";
   private static final String FOLDER_SOURCE_DEFAULT        = "sources";
   
   private final static String XLS_INPUT_FILE_NAME          = "excel.file_name";
   private final static String XLS_INPUT_FILE_NAME_DEFAULT  = "Raw_Input.xlsx";
   private final static String XLS_SHEET_NAME_KEY           = "excel.sheet_name";
   private final static String XLS_TEMPLATE_NAME_COLUMN_KEY = "excel.template_name_column";
   private final static String XLS_TARGET_EMAIL_COLUMNS_KEY = "excel.target_email_columns";
   private final static String XLS_SECRET_COLUMNS_KEY       = "excel.secret_column";
   private final static String XLS_SECRET_COLUMNS_DEFAULT   = "Key";
   
   private final static String XLS_SHEET_NAME_DEFAULT       = "Testergebnisse";
   private final static String XLS_TEMPLATE_NAME_COLUMN_DEFAULT = "Template";
   private final static List<String> XLS_TARGET_EMAIL_COLUMNS_DEFAULT = 
         Arrays.asList(new String[]{"PrimaryEmail", "SecondaryEmail"});
   
   private final static String EMAIL_HOST_KEY               = "email.host";
   private final static String EMAIL_PORT_KEY               = "email.port";
   private final static String USER_EMAIL_ADDR_KEY          = "email.user_email_address";
   private final static String USER_EMAIL_RETURN_KEY        = "email.user_return_address";
   
   private final static String EMAIL_TIMEOUT                = "email.timeout";
   private final static int    EMAIL_TIMEOUT_DEFAULT        = 10000; // 10 seconds
   private final static String EMAIL_WAIT_TIME              = "email.wait";
   private final static int    EMAIL_WAIT_TIME_DEFAULT      = 5000;  // 5 seconds
   private final static String EMAIL_RETRIES                = "email.retries";
   private final static int    EMAIL_RETRIES_DEFAULT        = 3;
   
   private final static String EMAIL_SUBJECT_MESSAGE        = "email.subject";
   private final static String EMAIL_BODY_MESSAGE_FILE      = "email.body_file";
   
   private final static String TEMPLATE                     = "template";
   
   private final static String CHOICE                       = "choice";
   private final static String CHOICE_SELECT_COLUMN         = "selectcolumn";
   private final static String CHOICE_SELECT_COLUMN_DEFAULT = "Template";
   private final static String CHOICE_BASE_NAME             = "basename";
   private final static String CHOICE_SELECT                = "select";
   
   private final static String MAPPINGS_BASE                = "mappings";

   
   public AppProperties(final Path propertyFile) {
      super(propertyFile);
   }
   
   private void createError(final String missingProperty) {
      String msg = String.format(MISSING_CONFIGURATION, missingProperty);
      logger.error(msg);
      throw new UnrecoverableException(msg);
   }
   
   private String getMandatoryProperty(final String propertyKey) {
      String value = getProperty(propertyKey);
      if (value==null) {
         createError(propertyKey);
      }
      return value;
   }
   
   //
   // folder
   //
   
   public String getGeneratedFolder() {
      return Optional.ofNullable(getProperty(FOLDER_GENERATED))
                     .orElse(FOLDER_GENERATED_DEFAULT);
   }
   
   public String getSourceFolder() {
      return Optional.ofNullable(getProperty(FOLDER_SOURCE))
                     .orElse(FOLDER_SOURCE_DEFAULT);
   }
   
   
   //
   // Spreadsheet information
   //
   
   public String getExcelFileName() {
      return Optional.ofNullable(getProperty(XLS_INPUT_FILE_NAME))
                     .orElse(XLS_INPUT_FILE_NAME_DEFAULT);
   }
   
   public String getExcelSheetName() {
	  String value = getProperty(XLS_SHEET_NAME_KEY);
	  if (value==null || value.isEmpty()) {
		  value = XLS_SHEET_NAME_DEFAULT;
	  }
      return value;
   }
   
   public String getExcelPdfTemplateName() {
	  String value = getProperty(XLS_TEMPLATE_NAME_COLUMN_KEY);
	  if (value==null || value.isEmpty()) {
		  value = XLS_TEMPLATE_NAME_COLUMN_DEFAULT;
	  }
      return value;
   }
   
   public List<String> getTargetEmailAddresses() {
      List<String> values = getPropertyList(XLS_TARGET_EMAIL_COLUMNS_KEY);
      if (values==null || values.isEmpty()) {
         return XLS_TARGET_EMAIL_COLUMNS_DEFAULT;
      }
      return Collections.unmodifiableList(values);
   }
   
   public String getExcelSecretColumnName() {
      String value = getProperty(XLS_SECRET_COLUMNS_KEY);
      if (value == null || value.isEmpty()) {
         value = XLS_SECRET_COLUMNS_DEFAULT;
      }
      return value;
   }

   //
   // email server properties
   //
   
   public String getEmailHost() {
      return getMandatoryProperty(EMAIL_HOST_KEY);
   }
   
   public String getEmailPort() {
      return getMandatoryProperty(EMAIL_PORT_KEY);
   }
   
   public String getEmailAddress() {
      return getMandatoryProperty(USER_EMAIL_ADDR_KEY);
   }
   
   public String getEmailReturnAddress() {
      String value = getProperty(USER_EMAIL_RETURN_KEY);
      if (value==null) {
         value = getEmailAddress();
      }
      return value;
   }
   
   public int getEmailTimeout() {
      int result = EMAIL_TIMEOUT_DEFAULT;
      String value = getProperty(EMAIL_TIMEOUT);
      if (value != null) {
         try {
            Integer.parseInt(value);
         } catch (NumberFormatException e) {
            String msg = String.format(MALFORMED_INTEGER, value, EMAIL_TIMEOUT);
            throw new UnrecoverableException(msg, e);
         }
      }
      return result;
   }
   
   public int getEmailWaitTime() {
      int result = EMAIL_WAIT_TIME_DEFAULT;
      String value = getProperty(EMAIL_WAIT_TIME);
      if (value != null) {
         try {
            Integer.parseInt(value);
         } catch (NumberFormatException e) {
            String msg = String.format(MALFORMED_INTEGER, value, EMAIL_WAIT_TIME);
            throw new UnrecoverableException(msg, e);
         }
      }
      return result;
   }

   public int getEmailSendRetries() {
      int result = EMAIL_RETRIES_DEFAULT;
      String value = getProperty(EMAIL_RETRIES);
      if (value != null) {
         try {
            Integer.parseInt(value);
         } catch (NumberFormatException e) {
            String msg = String.format(MALFORMED_INTEGER, value, EMAIL_RETRIES);
            throw new UnrecoverableException(msg, e);
         }
      }
      return result;
   }
   
   public String getEmailSubjectMessage() {
      return getMandatoryProperty(EMAIL_SUBJECT_MESSAGE);
   }
   
   public String getEmailBodyFile() {
      return getMandatoryProperty(EMAIL_BODY_MESSAGE_FILE);
   }
   
   //
   // Templates
   //
   
   public List<String> getTemplateKeys() {
      return Collections.unmodifiableList(getKeysWithBase(TEMPLATE));
   }
   
   public String getFullTemplateKey(final String templateName) {
      return getMandatoryProperty(TEMPLATE + "." + templateName);
   }
   
   //
   // Choice
   //
   
   public List<String> getChoiceKeys() {
      return getKeysWithBase(CHOICE);
   }

   public String getSelectionColumn(final String choiceName) {
      String key = CHOICE + "." + Objects.requireNonNull(choiceName) + "." + CHOICE_SELECT_COLUMN;
      return Optional.ofNullable(getProperty(key)).orElse(CHOICE_SELECT_COLUMN_DEFAULT);
   }
   
   public Optional<String> getBaseName(final String choiceName) {
      String key = CHOICE + "." + Objects.requireNonNull(choiceName) + "." + CHOICE_BASE_NAME;
      return Optional.ofNullable(getProperty(key));
   }

   public Map<String, String> getChoices(final String choiceName) {
      String key = CHOICE + "." + Objects.requireNonNull(choiceName) + "." + CHOICE_SELECT;
      return Collections.unmodifiableMap(getPropertyMap(key));
   }

   //
   // Mappings for form fields
   //
   
   public Map<String, String> getMappings(final String templateKey) {
      Map<String, String> value = getPropertyMap(MAPPINGS_BASE + "." + Objects.requireNonNull(templateKey));
      return Collections.unmodifiableMap(value);
   }
   
}
