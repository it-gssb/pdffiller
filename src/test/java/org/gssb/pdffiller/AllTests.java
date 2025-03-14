package org.gssb.pdffiller;

import org.gssb.pdffiller.config.PropertiesTest;
import org.gssb.pdffiller.email.BulkEmailTest;
import org.gssb.pdffiller.excel.RowReaderTest;
import org.gssb.pdffiller.pdf.BulkPdfTest;
import org.gssb.pdffiller.pdf.PdfFormFillerTest;
import org.gssb.pdffiller.template.TemplateBuilderTest;
import org.gssb.pdffiller.text.TextBuilderTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({PropertiesTest.class, RowReaderTest.class,
                PdfFormFillerTest.class, TemplateBuilderTest.class,
                TextBuilderTest.class, BulkPdfTest.class,
                BulkEmailTest.class})
public class AllTests {
}
