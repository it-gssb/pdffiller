package org.gssb.pdffiller.config;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import org.gssb.pdffiller.exception.UnrecoverableException;
import org.junit.Before;
import org.junit.Test;

public class PropertiesTest {
	
	private final static String P_PATH   = "src/test/resources/2018/config";
	private final static String PROPS1   = "test1.properties";
	private final static String PROPS2   = "test2.properties";
	private final static File   propFile1 = new File(P_PATH, PROPS1);
	
	private AppProperties props;
	
	@Before
	public void setup() {
	    props = new AppProperties(propFile1.toPath());
	}

	@Test
	public void testEmailPropertySetup() {
		assertEquals("smtp.xyz.org", props.getEmailHost());
		assertEquals("587", props.getEmailPort());
		assertEquals("test@xyz.org", props.getEmailAddress());
		assertEquals("return@xyz.org", props.getEmailReturnAddress());
	}
	
	@Test
	public void testExcelSetup() {
		assertEquals("Ergebnisse", props.getExcelSheetName());
		assertEquals(2, props.getTargetEmailColumns().size());
		assertEquals("PrimaryEmail", props.getTargetEmailColumns().get(0));
		assertEquals("SecondaryEmail", props.getTargetEmailColumns().get(1));
	}
	
	@Test
	public void testEmptyNameOrValueMap() {
		assertNotNull(props.getMappings("pdf3"));
		assertEquals(0, props.getMappings("pdf3").size());
	}
	
	@Test
	public void testNotEmptyMap() {
		assertNotNull(props.getMappings("pdf2"));
		assertEquals(2, props.getMappings("pdf2").size());
		assertEquals("excelfield1", props.getMappings("pdf2").get("pdffield1"));
		assertEquals("excelfield2", props.getMappings("pdf2").get("pdffield2"));
	}
	
	@Test
	public void testUndefinedMap() {
		assertNotNull(props.getMappings("pdf20"));
		assertEquals(0, props.getMappings("pdf20").size());
	}
	
	@Test (expected = UnrecoverableException.class)
	public void testIncorrectDefinition() {
		assertNotNull(props.getMappings("pdf4"));
	}
	
	@Test (expected = NullPointerException.class)
	public void testMissingTemplateName() {
		assertNotNull(props.getMappings(null));
	}
	
	@Test
	public void testTemplateNames() {
		List<String> templateNames = props.getKeysWithBase("template");
		assertNotNull(templateNames);
		assertEquals(6, templateNames.size());
		assertEquals("pdf1", templateNames.get(0));
	}
	
	@Test
	public void testBaseChoiceCertificate() {
		List<String> choiceName = props.getKeysWithBase("choice");
		assertNotNull(choiceName);
		assertEquals(3, choiceName.size());
		assertEquals("certificate", choiceName.get(0));
		
		List<String> certificateProps = props.getKeysWithBase("choice.certificate");
		assertNotNull(certificateProps);
		assertEquals(3, certificateProps.size());
		assertEquals("selectcolumn", certificateProps.get(0));
		assertEquals("basename", certificateProps.get(1));
		assertEquals("select", certificateProps.get(2));
	}
	
	@Test
	public void testBaseChoiceMissingDetailedKey() {
		List<String> choiceName = props.getKeysWithBase("error1");
		assertNotNull(choiceName);
		assertEquals(0, choiceName.size());
		
		List<String> choiceName2 = props.getKeysWithBase("error2");
		assertNotNull(choiceName2);
		assertEquals(0, choiceName2.size());
	}
		
   @Test
   public void testChoiceCertificate() {
      List<String> choiceName = this.props.getChoiceKeys();
      assertNotNull(choiceName);
      assertEquals(3, choiceName.size());
      assertEquals("certificate", choiceName.get(0));
      assertEquals("error1", choiceName.get(1));
      assertEquals("error2", choiceName.get(2));
      
      assertEquals("Template2", this.props.getSelectionColumn("certificate"));
      assertEquals("Template", this.props.getSelectionColumn("error1"));
      assertEquals("Template", this.props.getSelectionColumn("error2"));

      assertFalse(this.props.getChoices("certificate").isEmpty());
      assertEquals("pdf3", this.props.getChoices("certificate").get("silver"));
      assertEquals("pdf4", this.props.getChoices("certificate").get("bronze"));
   }
	
	@Test
	public void testExcelDefaultSetup() {
	   File  propFile2 = new File(P_PATH, PROPS2);
		AppProperties props = new AppProperties(propFile2.toPath());
		assertEquals("Testergebnisse", props.getExcelSheetName());
		assertEquals("test@xyz.org", props.getEmailReturnAddress());
	}

}
