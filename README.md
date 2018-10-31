
# PDF Filler

Tool to support bulk PDF Mail Merge / PDF Filler using data in Excel spreadsheet and sending of generated PDF files to email recipients defined in the same spreadsheet.

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
excel.secret\_column | Y | Column in selected input sheet that contains the PDF encryption key.
excel.group\_columns | N | Columns that have have the same values for each group of records. The first column is used as the group criterion. 
file.name\_template | Y | Text template using [Mustache](https://mustache.github.io/mustache.5.html) syntax to create the name of the generated file. The system variable {{\_BaseName\_}} contains the template file name without the postfix (e.g. '.pdf').
file.group\_name\_template | N | Text template using [Mustache](https://mustache.github.io/mustache.5.html) syntax to create the name of the generated file if it contains grouped records. Only use the system variable {{\_BaseName\_}} or a reference to an Excel column referenced in the `excel.group_columns` property.
template.\<alias\> | Y | Defines a name of a PDF form template located in the `sources` folder. The name is associated with the alias \<alias\>. Multiple PDF files may be defined, each with a different alias name \<alias\>.
mappings.\<alias\> | N | Defines for PDF template with alias name <alias> a list of column mappings \<form field\> : \<sheet column\> that is used to map excel sheet columns to PDF form fields.

The option to map PDF form columns to spreadsheet columns is useful if you need to map data to PDF forms for which you cannot define the column names.

:heavy_exclamation_mark: Please note that PDF files **without** a form or a file with a non-PDF type will not be used to generate a derived PDF document. Instead, the original document will be included into emails. This feature is helpful for emailing unmodified documents in addition to generated ones. 

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
choice.\<name\>.basename | N | Is used as the value of the system variable `_BaseName_`. The default value is the template file name without its type postfix. 

:heavy_exclamation_mark: PDF form templates are always processed if they are not referenced in any choice definition.

#### Example Configuration

```
choice.certificate.selectcolumn = Award
choice.certificate.basename     = AATG Certificate
choice.certificate.select       = Goldurkunde:pdf2, Silberurkunde:pdf3, Bronzeurkunde:pdf4, Achievement:pdf5, Participation:pdf6
```

The configuration defines a choice configuration named 'certificate' that selects one of the five PDF forms pdf2 through pdf6.
The value that the decision is based on is located in column `Award` of the sheet. For example, pdf2 (mapped to PDF form 'AATG Gold.pdf') is selected if the sheet column for the current record contains the value 'Goldurkunde'.

The definition of the `basename` with value 'ATTG Certificate' in conjunction with the previously defined PDF file name expression `{{_BaseName_}} - {{Name}}.pdf` implies that all generated PDF files have a name that begins with  'ATTG Certificate - ' followed by the value in the records `name` column.

PDF form pdf1 is not used in the choice configuration and therefore is always used to create a new PDF document per record.

### Grouping Records and Merging Into one Document

By default, PDF Filler processes the source spreadsheet row-by-row to create PDF documents and to send them to a set of email recipients. It is sometimes desirable to create documents using multiple related records and send documents created from related records in one email to the recipients.

For example, it may be desirable to send documents related to multiple students of the same family in one email message to their parents. Some documents may be created once for each student and other documents may be created once per family. 

One supported use case is the student enrollment notification for a family, which makes it necessary to deliver registration information once per family instead of once per student. This implies that documents related to the entire family and individual students need to be created and sent once even though each student has a separate record. Some documents related to the entire family such as the class assignment must contain data elements of all student records and of parent records. However, student test results and certificates are created for each individual student and are sent in one email to the parents.

#### Enabling Grouping

The PDF Filler tool allows grouping records using one Excel input column, which is the first column defined in the `excel.group_columns` property. Records are grouped based on the values in this column and created documents are sent to the the recipients in one email. PDF Filler continues to process Excel rows row-by-row if the group column is undefined.

The property `excel.group_columns` define **all Excel columns** that are the same for a group of records. Columns defined in properties `excel.secret_column` and `excel.target_email_columns` are automatically included in the non-empty column list defined by property `excel.group_columns`.

#### Use Multiple Records in One Documents

Some PDF form may contain fields for different records. For example, the Excel spreadsheet may contain columns `Student Name`, `Teacher Name`, `Class`, and `Room` and multiple records may be grouped by column `Parent Name`.

The PDF form must have the following structure to be able to accept multiple records:
* Form fields with a name defined as a group columns may accept the content of group columns. For example, the field name for group column `Parent Name` is the default '\[Parent Name\]'. 
* Form fields for fields with multiple records consist of the column name followed by a '\_' and subsequent integer numbers greater than 0. For example, the field names to represent 5 records of column `Student Name` are '\[Student Name\_1\]' through '\[Student Name\_5\]'. In a group of several records the value of Excel column `Student Name` for the `i`-th record is assigned to form field '\[Student Name\_i\]'.

For example, a PDF form could contain a table with four rows with each row referring to a different record for students class assignment. The column names are repeated with  postfixes `_1` to `_4` to represent entries for up to four records.

Student Name        | Teacher Name        | Class        | Room
------------------- | ------------------- | ------------ | --------------
\[Student Name\_1\] | \[Teacher Name\_1\] | \[Class\_1\] | \[Room\_1\]
\[Student Name\_2\] | \[Teacher Name\_2\] | \[Class\_2\] | \[Room\_2\]
\[Student Name\_3\] | \[Teacher Name\_3\] | \[Class\_3\] | \[Room\_3\]
\[Student Name\_4\] | \[Teacher Name\_4\] | \[Class\_4\] | \[Room\_4\]

The group column `Parent Name` may be used anywhere in the document in the form field named '\[Parent Name\]'.

#### Creation Rules

For each document included in the `template` definition PDF Filler determines if it is processed once per record or once per record group. The following rules are applied:
* Documents without PDF form are attached once to the outgoing email per record group.
* PDF forms with form fields that map to group columns `excel.group_columns` and automatically included columns are processed once per record group.
* PDF forms with two or more form fields with naming convention `<name>_i` with `i` a number greater than 0.  
* PDF forms that are included in a [PDF Form Choice](#advanced-pdf-mail-merge-configuration-for-pdf-form-choices) configuration are processed row-by-row.
* All other PDF forms are processed row-by-row.

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

* Apache PdfBox 2.0.11 and its two dependencies Apache Fontbox 2.0.11 and Commons Logging 1.2
* Commons Command Line Interface 1.4
* Apache Configuration 2.3 and dependencies on Apache Commons Lang 3.7, Commons Beanutils 1.9.3, Commons Logging 1.2, Commons Collections 3.2.2, and Commons Collections 4.2
* Apache POI 4.0.0 and dependencies on Apache Commons Collections 4.1 and Apache Commons Codec 1.10, Apache Commons Math3 3.6.1.
* Apache POI OOXML 4.0.0 and dependencies on Apache POI OOXML Schemas 4.0.0, Apache XML Beans 3.0.1, Apache Commons Compress 1.18, Virtuald Curves API 1.04
* Javax Mail 1.6.2 and dependency on Javax Activation 1.1
* Sun SMTP 1.6.2 and dependencies on Sun Mail API 1.6.2 and Javax Activation 1.1
* Spullara Mustache Java compiler 0.9.5
* Apache Log4j 2.11.1

A detailed dependency report can be generated by running the gradle command line `./gradlew projectReport`. 
Results will be available in the file `$PROJECT_ROOT/build/reports/project/dependencies.txt`.

# Contributors
* Michael Sassin created initial versions
