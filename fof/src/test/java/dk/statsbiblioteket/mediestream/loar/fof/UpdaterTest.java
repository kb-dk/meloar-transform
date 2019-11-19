package dk.statsbiblioteket.mediestream.loar.fof;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UpdaterTest {
    private String csvfile = "/home/baj/Downloads/1902-333.csv";//prod
    private String csvfile_2 = "/home/baj/Downloads/1902-364.csv";//stage
    private String output_dir = "/home/baj/Projects/meloar-transform/fof/src/test/resources/output";
    private String xmlfile = "/mnt/data/data/meloar/input/ff_raw_all.xml";
    private String output_dir_2 = "/mnt/data/data/meloar/output";

    @BeforeMethod
    public void setUp() throws Exception {
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }

    @Test
    public void testTranslateFile() {
        Updater.updateCSVfromOriginalCSV(csvfile, xmlfile, output_dir);
        //Updater.updateCSV(csvfile_2, xmlfile, output_dir_2);
    }

}
