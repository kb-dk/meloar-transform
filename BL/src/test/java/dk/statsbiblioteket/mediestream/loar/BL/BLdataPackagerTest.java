package dk.statsbiblioteket.mediestream.loar.BL;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class BLdataPackagerTest {

    String outputdir = "BL/src/test/resources/output/0000";
    String datadir = "BL/data/0000";
    String csvfile = "BL/src/test/resources/input/Microsoft Books records 2019-09-19.csv";

    @BeforeMethod
    public void setUp() {
    }

    @AfterMethod
    public void tearDown() {
    }

    @Test
    public void testMatchDataToCSV() {
        BLdataPackager.matchDataToCSV(datadir, csvfile, outputdir);
    }
}