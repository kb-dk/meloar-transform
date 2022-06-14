package dk.statsbiblioteket.mediestream.loar.veda;

import org.testng.annotations.Test;

public class VedaPackagerTest {

    private final String input_dir = "/home/baj/Projects/meloar-transform/veda/src/test/resources/input";
    private final String csv_file = "/home/baj/Projects/meloar-transform/veda/src/test/resources/Veda-metadata_jamo_short.csv";
    private final String output_dir = "/home/baj/Projects/meloar-transform/veda/src/test/resources/output";

    @org.testng.annotations.BeforeMethod
    public void setUp() {
    }

    @org.testng.annotations.AfterMethod
    public void tearDown() {
    }

    @Test
    public void testReadInputAndWriteToSAF() {
        VedaCSVReader.readCSVFileAndWriteToSAF(input_dir, csv_file, output_dir);
    }

}
