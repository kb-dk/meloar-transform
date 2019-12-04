package dk.statsbiblioteket.mediestream.loar;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class UpdateDOIsTest {
    private String metadatacsvall = "/home/baj/Projects/meloar-transform/loar/src/test/resources/input/metadata_all.csv";
    private String metadatacsvfof = "/home/baj/Projects/meloar-transform/loar/src/test/resources/input/metadata_coll_1902-333.csv";
    private String metadatacsvdatasprint2019 = "/home/baj/Projects/meloar-transform/loar/src/test/resources/input/metadata_coll_1902-4276.csv";
    private String metadatacsvruben = "/home/baj/Projects/meloar-transform/loar/src/test/resources/input/metadata_coll_1902-4119.csv";
    private String doisandurlscsv = "/home/baj/Projects/meloar-transform/loar/src/test/resources/input/doisandurls.csv";
    private String outputdir = "/home/baj/Projects/meloar-transform/loar/src/test/resources/output/";

    @BeforeMethod
    public void setUp() {
    }

    @AfterMethod
    public void tearDown() {
    }

    @Test
    public void testUpdateCSVwithDOIs() {
        //UpdateDOIs.updateCSVwithDOIs(metadatacsvall, doisandurlscsv, outputdir);
        //UpdateDOIs.updateCSVwithDOIs(metadatacsvfof, doisandurlscsv, outputdir);
        //UpdateDOIs.updateCSVwithDOIs(metadatacsvdatasprint2019, doisandurlscsv, outputdir);
        UpdateDOIs.updateCSVwithDOIs(metadatacsvruben, doisandurlscsv, outputdir);
    }
}