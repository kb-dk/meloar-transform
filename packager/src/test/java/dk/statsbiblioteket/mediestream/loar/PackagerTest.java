package dk.statsbiblioteket.mediestream.loar;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PackagerTest {
    private String input_file = "/home/baj/Projects/meloar-transform/packager/src/test/resources/input/input.xml";
    private String output_dir = "/home/baj/Projects/meloar-transform/packager/src/test/resources/output";

    @BeforeMethod
    public void setUp() throws Exception {
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }

    @Test
    public void testTranslateFile() {

        Packager.translateFile(input_file, output_dir, 1);
    }


}
