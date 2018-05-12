
# PDF Filler

Tool to support bulk PDF Mail Merge using data in Excel spreadsheet and sending of generated PDF files to email addresses included in the spreadsheet.

## Use Cases 

* Fill one or multiple files for each record in the Excel spreadsheet and save as files.
* Send generated files associated with each spreadsheet record to one or multiple email addresses. Subject line and email body are customizable and can contain data from the spreadsheet record.

# Install

TBD

# Use PDF Filler Application

## Project Structure

A Mail Merge project has the following folder structure:

```
  project folder
   |
   |-- config
   |    |---- configuration file (*.properties)
   |
   |-- sources
   |    |---- PDF Template files (*.pdf)
   |    |---- Mail body text Mustache templates (*.mustache)
   |    |---- Spreadsheet (*.xlsx)
   |
   |-- generated
```

The project folder name can be chosen but the name of the child folders `sources` and `generated` is defined by default. The configuration child folder should have the name `config`.

The `config` folder contains the configuration files for the project that defines details about
* the XLSX spreadsheet file with the records to be merged, sheet name, and column definitions,
* the [Mustache](https://mustache.github.io/mustache.5.html) template file with the email body and template string for the email subject line
* the Mustache template string for the name of generated files,
* PDF form template files and selection criteria, and
* PDF form field mapping instructions.

The `sources` folder contains the 
* PDF Template files, 
* Mustache mail body template, and
* the XSLX spreadsheet that contains the record to be merged into the PDF templates and mail boy templates.

Generated PDF files are placed in the `generated` folder.

## Configuration File

The configuration file defines all non-confidential settings of the application.

The email user password and master PDF encryption key are not part of the configuration file and must be passed as parameters of the application command line interface.

### Basic PDF Mail Merge Configurations

This section contains the configurations required to create PDF files from PDF form templates. Each row in the provided xslx spreadsheet sheet is used to populate the PDF form and define the names of the generated files.

Configuration Key   | Mandatory | Description
--------------------| --------- | ---------------------------------------
excel.file\_name | Y | name of the Excel xslx spreadsheet in the `sources` folder that contains the records to be merged
excel.sheet\_name | Y | name of the sheet in the excel spreadsheet with the records
excel.secret_column | Y | column in selected input sheet that contains the PDF encryption key
file.name\_template | Y | text template using [Mustache](https://mustache.github.io/mustache.5.html) syntax to create the name of the generated file. The system variable {{\_BaseName\_}} contains the template file name without the postfix (e.g. '.pdf')
template.\<key\> | Y | defines a name of a PDF form template located in the `sources` folder and associates the alias \<key\>
mappings.\<key\> | N | defines for PDF template with alias <key> a list of column mappings \<form field\> : \<sheet column\> that is used to map excel sheet columns to PDF form fields.

The option to map PDF form columns to spreadsheet columns is useful if you need to map data to PDF forms for which you cannot define the column names.

:heavy_exclamation_mark: Please note that PDF files **without** a form will not be used to generate a derived PDF document. However, the original PDF document will be included into emails. This feature is helpful if you like to include PDF documents in combination with generated documents. 

#### Example Configuration

```
excel.file_name     = GSSB Raw Results.xlsx
excel.sheet_name    = Testergebnisse
excel.secret_column = Key
file.name_template  = {{_BaseName_}} - {{Name}}.pdf
```

The configuration indicates that the source of the mail merge operations is a sheet called 'Testergebnisse' in the Excel spreadsheet 'GSSB Raw Results.xlsx'.
The column with the encryption key is `Key`. The PDF Filler application creates files, which names begin with the base name  of the PDF Form template followed by ' - ' and the record's value of the `Name` column.

```
template.pdf1 = AATG Raw Score.pdf
template.pdf2 = AATG Gold.pdf
template.pdf3 = AATG Silver.pdf
template.pdf4 = AATG Bronze.pdf
template.pdf5 = AATG Achievement.pdf
template.pdf6 = AATG Participation.pdf

mappings.pdf2 = Text1 : Name, Text3 : Level, Text4 : LehrerIn, Text5 : Schule
mappings.pdf3 = Text1 : Name, Text3 : Level, Text4 : LehrerIn, Text5 : Schule
mappings.pdf4 = Text1 : Name, Text3 : Level, Text4 : LehrerIn, Text5 : Schule
mappings.pdf5 = Text1 : Name, Text2 : Level, Text3 : LehrerIn, Text4 : Schule
mappings.pdf6 = Text5 : Name, Text6 : Level, Text7 : LehrerIn, Text8 : Schule
```

The configuration defines six PDF form documents with PDF Alias names pdf1 through pdf6.
PDF Forms pdf2 through pdf6 define mappings between PDF form fields and Excel columns. 
For example, Excel column `Name` is mapped to PDF Form field `Text1` for pdf2 through pdf5 and to PDF form field `Text5` for pdf6.

### Advanced PDF Mail Merge Configuration for PDF Form Choices

The PDF Filler applications supports data-driven decision for the selection of a PDF form using the choice configuration.
For example, students may receive a certificate based on a PDF form that reflects the level of accomplishment such as 'gold', 'silver', 'bronze'. 

A PDF Form is selected using the choice feature based on the value of a spreadsheet column. The name of the generated file may incorporate the base name of the template or a generic base name that is supplied by a configuration.

Multiple PDF files may be generated based on separate sets of choice configurations.
Each group of choice definitions must use the same choice.\<name\> key prefix.


Configuration Key          | Mandatory | Description
-------------------------- | --------- | ---------------------------------------
choice.\<name\>.select | Y | a list of selection criteria \<value\> : \<key\>. \<value\> defines the expected value and \<key\> is a PDF form template alias defined in the template.\<key\> configuration
choice.\<name\>.selectcolumn | N | column in the spreadsheet that defines the value used for selecting a PDF Form template. The default value is `Template`.
choice.\<name\>.basename | N | is used as the base name of the generated PDF file. The default value is the template file name without its type postfix. 

PDF Form templates are not used for standard mail merge operations if their alias names that are referenced in the choice.<name>.select expressions.

#### Example Configuration

```
choice.certificate.selectcolumn = Award
choice.certificate.basename     = AATG Certificate
choice.certificate.select       = Goldurkunde:pdf2, Silberurkunde:pdf3, Bronzeurkunde:pdf4, Achievement:pdf5, Participation:pdf6
```

The configuration defines a choice configuration named 'certificate' that selects one of the five PDF forms pdf2 through pdf6.
The value that the decision is based on is located in column `Award` of the sheet. For example, pdf2 (mapped to PDF form 'AATG Gold.pdf') is selected if the sheet column for the current record contains value 'Goldurkunde'.

PDF form pdf1 is not used in the choice configuration and therefore is always used for creating a new PDF document per record.


### Sending Email with PDF Document Attachments

Generated PDF documents may be send to one or multiple recipients.
Recipients' email addresses need to be included into each spreadsheet record and only the documents generated based on the same record are included in the email.

Emails' subject and email body are generated using Mustache templates.

Configuration Key            | Mandatory | Description
---------------------------- | --------- | ---------------------------------------
excel.target\_email\_columns | N | list of columns in selected sheet that contain email addresses. The default values are 'PrimaryEmail' and 'SecondaryEmail'.
email.host | Y | smtp server name or IP address
email.port | Y | smtp server port
email.user\_email\_address | Y | sender's email account / email address
email.user\_return\_address | N | email address used as the return address of the sent email (default is email address email.user\_email\_address)
email.timeout | N | time in ms until an attempt to send email times out (default is 10000 = 10 seconds)
email.wait | N | wait time in ms before another attempt to email is made after a failure (default is 3000 = 3 seconds)
email.retries | N | number of attempts to send email before email operation (default value is 3)
email.subject | Y | text template using [Mustache](https://mustache.github.io/mustache.5.html) syntax to create subject line
email.body\_file | Y | text template using the [Mustache](https://mustache.github.io/mustache.5.html) syntax that is used to create the body of the email

#### Example Configuration

```
email.host                = smtp.myemaildomain.com
email.port                = 587
email.user_email_address  = noreply@myemaildomain.com
email.user_return_address = principal@myemaildomain.com
email.subject             = GSSB: AATG award for {{Name}}
email.body_file           = aatg_certificates.mustache
```

The configuration defines the email smtp host, port, and the sender's email address noreply@myemaildomain.com.
The return address is defined as principal@myemaildomain.com and will be displayed in the recipients' email.
The subject line is defined using a Mustache expression which includes the value of sheet column 'Name' into the subject line.
The body of the email message is defined in the Mustache template 'aatg_certificates.mustache', which is located in the 'sources' folder.

## Command Line

The application is invoked by calling the `pdffiller` shell that is located in the `bin` folder of the distribution.

The command line

` pdffiller -c path-to-configuration-file `

executes the basic PDF Mail Merge functionality that produces PDF files as described in the configuration files and the spreadsheet data.

The additional master encryption key triggers encryption of PDF documents. The command line

` pdffiller -c path-to-configuration-file -m master-key`

the generated PDF documents are encrypted and may be decrypted with the master key or the key supplied in the sheet key column.
Please note that PDF documents are only encrypted if the sheet's key column contains a non-empty value.

The additional email account password triggers triggers sending of emails to the recipients defined in the sheet's email columns.
The resulting command line looks as follows:

` pdffiller -c path-to-configuration-file -m master-key -p password-email-account`

This assumes that the email server, port, account, subject, and body files are properly configured.

During the development of the mail merge project it may be helpful avoiding sending emails.
The command line option `-s` prevents that emails are sent and instead emails are only logged.

### Sample Configurations

The PDF Filler tool defines a sample project in the src/test/resources/2018 that is included when you clone the GitHub project.

Simply issue the command and supply the path to the configuration file 'sample\_raw.properties' or 'sample\_cert.properties'.


# Build

TBD

# List of Third Party Components

PDF Filler has the following runtime dependencies on third party open source components:

* Apache PdfBox 2.0.8 and its two dependencies Apache Fontbox 2.0.9 and Apache Commons Logging 1.2
* Apache Command Line Interface 1.4
* Apache Configuration 2.2.2 and dependencies on Apache Commons Lang 3.6, Apache Commons Beanutils 1.9.3, and Apache Commons Logging 1.2
* Apache POI 3.17 and dependencies on Apache Commons Collections 4.1 and Apache Commons Codec 1.10
* Apache POI OOXML 3.17 and dependencies on Apache POI OOXML Schemas 3.17, Apache XML Beans 2.6.0, Stax API 1.0.1, Virtuald Curves API 1.04
* Javax Mail 1.6.1 and dependency on Javax Activation 1.1
* Sun SMPT 1.6.1 and dependencies Sun Mail API 1.6.1 and Javax Activation 1.1
* Spullara Mustache Java compiler 0.9.5
* Apache Log4j 2.11.0

# Contributors
* Michael Sassin created the initial version
