package dk.statsbiblioteket.mediestream.loar;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class FixDOIsTest {
    private String input_file = "/home/baj/Projects/meloar-transform/loar/src/test/resources/input/List_of_all_objects_to_be_reserved_registered_deleted_or_updated.txt";
    private String output_dir = "/home/baj/Projects/meloar-transform/loar/src/test/resources/output/";

    @BeforeMethod
    public void setUp() throws Exception {
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }

    @Test
    public void cleanDOIlist() {
        FixDOIs.cleanDOIlist(input_file, output_dir);
    }
}
