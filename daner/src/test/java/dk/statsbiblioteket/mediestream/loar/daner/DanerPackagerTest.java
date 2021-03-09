package dk.statsbiblioteket.mediestream.loar.daner;

import org.testng.annotations.Test;

public class DanerPackagerTest {

    private String input_dir = "/home/baj/Projects/meloar-transform/daner/src/main/resources/input/faces2";
    private String csv_file = "/home/baj/Projects/meloar-transform/daner/src/main/resources/input/newest.csv";
    private String output_dir = "/home/baj/Projects/meloar-transform/daner/src/main/resources/output";

    @org.testng.annotations.BeforeMethod
    public void setUp() {
    }

    @org.testng.annotations.AfterMethod
    public void tearDown() {
    }

    @Test
    public void testReadInputAndWriteToSAF() {
        DanerCSVReader.readCSVFileAndWriteToSAF(input_dir, csv_file, output_dir);
    }

}