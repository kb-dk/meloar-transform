package dk.statsbiblioteket.mediestream.loar.fof;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PackagerTest {
    private String input_file = "/home/baj/Projects/meloar-transform/fof/src/test/resources/input/input.xml";
    private String output_dir = "/home/baj/Projects/meloar-transform/fof/src/test/resources/output";

    private String input_file_2 = "/home/baj/Projects/meloar-solr/ff_slks/oai-pmh.page_14.xml";

    private String input_file_3 = "/mnt/data/data/meloar/input/ff_raw_all.xml";
    private String output_dir_3 = "/mnt/data/data/meloar/output";

    @BeforeMethod
    public void setUp() throws Exception {
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }

    @Test
    public void testTranslateFile() {

        Packager.translateFile(input_file_3, output_dir_3, 0);
    }


}
