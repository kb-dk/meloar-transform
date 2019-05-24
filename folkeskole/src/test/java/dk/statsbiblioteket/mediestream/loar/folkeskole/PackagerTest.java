package dk.statsbiblioteket.mediestream.loar.folkeskole;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PackagerTest {
    private String input_file = "/home/baj/Projects/meloar-transform/folkeskole/data/folkeskole.csv";
    private String output_dir = "/home/baj/Projects/meloar-transform/folkeskole/data/output";

    @BeforeMethod
    public void setUp() throws Exception {
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }

    @Test
    public void testTranslateFile() {

        FolkeskoleCSVmetadataReader.readCSVFileAndWriteToSAF(input_file, output_dir);
    }


}
