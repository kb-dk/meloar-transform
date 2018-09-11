package dk.statsbiblioteket.mediestream.loar.beretningsarkiv;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class BeretningsarkivPackagerTest {
    private String input_file = "/home/baj/Projects/beretningsarkiv/transformation/test/small_example.xml";
    private String input_file_2 = "/home/baj/Projects/beretningsarkiv/transformation/test/input_example.xml";
    private String output_dir = "/home/baj/Projects/beretningsarkiv/packager/test/output";

    @BeforeMethod
    public void setUp() throws Exception {
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }

    @Test
    public void testTranslateFile() {
        packager.translateFile(input_file_2, output_dir);
    }


}
