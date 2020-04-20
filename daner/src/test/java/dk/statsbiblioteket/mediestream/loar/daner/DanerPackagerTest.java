package dk.statsbiblioteket.mediestream.loar.daner;

import org.testng.annotations.Test;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;

public class DanerPackagerTest {

    private String input_dir = "/home/baj/Projects/meloar-transform/daner/src/main/resources/input/faces2";
    private String output_dir = "/home/baj/Projects/meloar-transform/daner/src/main/resources/output";

    @org.testng.annotations.BeforeMethod
    public void setUp() {
    }

    @org.testng.annotations.AfterMethod
    public void tearDown() {
    }

    @Test
    public void testReadInputAndWriteToSAF() throws IOException, ParserConfigurationException, TransformerException {
        DanerPackager.readInputAndWriteToSAF(input_dir, output_dir);
    }

}