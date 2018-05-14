
# PDF Filler

Tool to support bulk PDF Mail Merge using data in Excel spreadsheet and sending of generated PDF files to email recipients defined in the same spreadsheet.

## Use Cases 

* **PDF Mail Merge**<p>
  Create PDF files by merging records retrieved from an Excel spreadsheet into PDF form fields in one or multiple PDF form files.
* **Bulk Email**<p>
  Email generated PDF files to one or multiple email recipients. Subject line and email body are customizable using spreadsheet records.
* **Secure PDF**<p>
  PDF documents may be encrypted and can be decrypted using a master key or record-specific key.

# Installation

Download and install the latest [Java SE Runtime Environment 1.8](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html).

Download the latest released version  `pdffiller-0.1.0.zip` of the [PDF Filler application](https://github.com/it-gssb/pdffiller/releases) and unzip it in the location of your choice. 

The folders of the distribution has the following structure:

```
  pdffiller-<version>
       |-- bin
       |-- conf
       |-- lib
       |-- licenses
```

The `bin` folder contains the Linux and Windows `pdffiller` shells that are used to invoke the application from the command line.
The `lib` folder contains the PDF Filler jar file and all jars of the utilized [third-party open source libraries](#list-of-third-party-components).
The `conf` folder contains optional configuration definitions for logging, the version file, and the license file. 
The folder `licenses` contains the licenses of the third party libraries this application depends on at run-time.

The application can be used after [defining a project](#project-structure) and calling the `pdffiller`
command with the proper parameters as described [here](#command-line).


# User Guide

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

The project folder name can be chosen but the name of the child folders `sources` and `generated` is defined by default. The configuration child folder should have the name `config`. The `config` folder contains the project configuration file. The `sources` folder contains PDF templates, text templates, and spreadsheets with the records to be merged. Generated PDF files are placed in the `generated` folder.

## Project Configuration

A configuration file defines all non-confidential settings of the application. Confidential information such as the email account password or the master PDF encryption key are not included and must be passed as parameters of the application command line interface.

### Basic PDF Mail Merge Configurations

This section contains the configurations required to create PDF files from PDF form templates. 

Configuration Key   | Mandatory | Description
--------------------| --------- | ---------------------------------------
excel.file\_name | Y | Name of the Excel xslx spreadsheet in the `sources` folder that contains the records to be merged. For each record one or multiple PDF files are created according to the configuration.
excel.sheet\_name | Y | Name of the sheet in the excel spreadsheet with the records.
excel.secret_column | Y | Column in selected input sheet that contains the PDF encryption key.
file.name\_template | Y | Text template using [Mustache](https://mustache.github.io/mustache.5.html) syntax to create the name of the generated file. The system variable {{\_BaseName\_}} contains the template file name without the postfix (e.g. '.pdf').
template.\<alias\> | Y | Defines a name of a PDF form template located in the `sources` folder. The name is associated with the alias \<alias\>. Multiple PDF files may be defined, each with a different alias name \<alias\>.
mappings.\<alias\> | N | Defines for PDF template with alias name <alias> a list of column mappings \<form field\> : \<sheet column\> that is used to map excel sheet columns to PDF form fields.

The option to map PDF form columns to spreadsheet columns is useful if you need to map data to PDF forms for which you cannot define the column names.

:heavy_exclamation_mark: Please note that PDF files **without** a form will not be used to generate a derived PDF document. However, the original PDF document will be included into emails. This feature is helpful for emailing plain PDF documents with generated documents. 

#### Example Configuration

```
excel.file_name     = GSSB Raw Results.xlsx
excel.sheet_name    = Testergebnisse
excel.secret_column = Key
file.name_template  = {{_BaseName_}} - {{Name}}.pdf
```

The configuration determines that the source of the mail merge operations is a sheet called 'Testergebnisse' in the Excel spreadsheet 'GSSB Raw Results.xlsx'.
The encryption key is extracted column `Key` of the current excel sheet row. The named of the generated files are defined to begin with the base name of the PDF form template, which is expressed by `{{_BaseName_}}` followed by ' - ' and the record's value of the `Name` column, which is expressed by the Mustache expression `{{Name}}`.

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

The configuration defines six PDF form documents with alias names pdf1 through pdf6.
PDF forms pdf2 through pdf6 define mappings between PDF form fields and Excel columns. 
For example, Excel column `Name` is mapped to PDF Form field `Text1` for pdf2 through pdf5 and to PDF form field `Text5` for pdf6.

### Advanced PDF Mail Merge Configuration for PDF Form Choices

The PDF Filler application supports data-driven decision for the selection of a PDF form using the choice configuration.
For example, students may receive a certificate based on a PDF form that reflects the level of accomplishment.

PDF Filler selects a PDF form using the value of a spreadsheet column and the name supplied to the '\_BaseName\_` is either the base name of the selected PDF form file or the name that is supplied in the configuration.

Multiple PDF files may be generated based on separate sets of choice configurations.
Each group of choice definitions must use the same choice.\<name\> key prefix.


Configuration Key          | Mandatory | Description
-------------------------- | --------- | ---------------------------------------
choice.\<name\>.select | Y | A list of selection criteria \<value\> : \<alias\>. \<value\> defines the expected value and \<alias\> is the PDF file alias defined in the template.\<alias\> configuration.
choice.\<name\>.selectcolumn | N | Column in the spreadsheet that defines the value used for selecting a PDF Form template. The default value is `Template`.
choice.\<name\>.basename | N | Is used as the value of the system variable '\_BaseName\_`. The default value is the template file name without its type postfix. 

:heavy_exclamation_mark: PDF form templates are always processed if they are not referenced in any choice definition.

#### Example Configuration

```
choice.certificate.selectcolumn = Award
choice.certificate.basename     = AATG Certificate
choice.certificate.select       = Goldurkunde:pdf2, Silberurkunde:pdf3, Bronzeurkunde:pdf4, Achievement:pdf5, Participation:pdf6
```

The configuration defines a choice configuration named 'certificate' that selects one of the five PDF forms pdf2 through pdf6.
The value that the decision is based on is located in column `Award` of the sheet. For example, pdf2 (mapped to PDF form 'AATG Gold.pdf') is selected if the sheet column for the current record contains the value 'Goldurkunde'.

The definition of the `basename` with value 'ATTG Certificate' in conjunction with the previously defined PDF file name expression `{{_BaseName_}} - {{Name}}.pdf` implies that all generated PDF files have a name that begind with  'ATTG Certificate - ' followed by the value in the records `name` column.

PDF form pdf1 is not used in the choice configuration and therefore is always used to create a new PDF document per record.


### Sending Email with PDF Document Attachments

Generated PDF documents may be send to one or multiple recipients that are defined in the spreadsheet record used to generate the PDF documents. Each email subject and plain text body are generated using [Mustache](https://mustache.github.io/mustache.5.html) templates.

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
email.subject | Y | text template using Mustache syntax to create subject line
email.body\_file | Y | text template using the Mustache syntax that is used to create the body of the email

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
The subject line is defined using a Mustache expression which includes the value of sheet column `Name` into the subject line.
The body of the email message is defined in the Mustache template 'aatg_certificates.mustache', which is located in the `sources` folder.

## Command Line

The application is invoked by calling the `pdffiller` shell that is located in the `bin` folder of the distribution.

The command
```
   pdffiller -c path-to-configuration-file 
```
executes the basic PDF Mail Merge functionality that produces PDF files as described in the configuration files and the spreadsheet data.

The use of master encryption key option ` -m master-key` triggers encryption of PDF documents. The command
```
   pdffiller -c path-to-configuration-file -m master-key
```
generates encrypted PDF documents, which may be decrypted with the master key or the key supplied in the sheets encryption key column.

:heavy_exclamation_mark: Please note that PDF documents are encrypted only if the sheet's encryption key column contains a non-empty value.

Adding the email account password with option `-p password-email-account` triggers sending of emails to recipients defined in the sheet's email address columns. The resulting command looks as follows:
```
   pdffiller -c path-to-configuration-file -m master-key -p password-email-account
```
Please ensure that you properly configure email server, port, account, subject, and body in the properties file before attempting to send emails.

During the development of a mail merge project it may be helpful to avoid sending emails.
Using the command line option `-s` results in emails being logged without sending them.

### Sample Configurations

The PDF Filler tool defines a sample project in the `src/test/resources/2018` that is included when you clone the GitHub project.

Simply issue the command and supply the path to the configuration file 'sample\_raw.properties' or 'sample\_cert.properties'.
Let's assume that you defined a shell variable `PROJECT_ROOT` that defines the path to the PDF Filler git project clone.
Issuing the command
```
   pdffiller -c $PROJECT_ROOT/src/test/resources/2018/config/sample_cert.properties
```
will generate 10 files - two per record in the spreadsheet - in folder `$PROJECT_ROOT/src/test/resources/2018/generated`
that reflect the sample records in sheet 'Testergebnisse' in spreadsheet `$PROJECT_ROOT/src/test/resources/2018/sources/GSSB Raw results.xlsx`.


# Build

Download and install the most recent [Java SE Development Kit 1.8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).

Clone the [PDF Filler](https://github.com/it-gssb/pdffiller.git) repository using your IDE or the Git [command line tool](https://git-scm.com/downloads).

Open a command shell in the root directory `pdffiller` of the PDF Filler project and run the command ./gradlew build.
This creates distribution in `pdffiller/build/distributions/pdffiller-<version number>.zip` that you can unzip in a directory of your choice.

# List of Third Party Components

PDF Filler has the following runtime dependencies on third party open source components:

* Apache PdfBox 2.0.9 and its two dependencies Apache Fontbox 2.0.9 and Commons Logging 1.2
* Commons Command Line Interface 1.4
* Apache Configuration 2.2.2 and dependencies on Apache Commons Lang 3.6, Commons Beanutils 1.9.3, Commons Logging 1.2, and Commons Collections 3.2.2
* Apache POI 3.17 and dependencies on Apache Commons Collections 4.1 and Apache Commons Codec 1.10
* Apache POI OOXML 3.17 and dependencies on Apache POI OOXML Schemas 3.17, Apache XML Beans 2.6.0, Stax API 1.0.1, Virtuald Curves API 1.04
* Javax Mail 1.6.1 and dependency on Javax Activation 1.1
* Sun SMPT 1.6.1 and dependencies on Sun Mail API 1.6.1 and Javax Activation 1.1
* Spullara Mustache Java compiler 0.9.5
* Apache Log4j 2.11.0

A detailed dependency report can be generated by running the gradle command line `./gradlew projectReport`. 
Results will be available in the file `$PROJECT_ROOT/build/reports/project/dependencies.txt`.

# Contributors
* Michael Sassin created the initial version
