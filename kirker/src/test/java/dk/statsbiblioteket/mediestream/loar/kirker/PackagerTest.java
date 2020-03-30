package dk.statsbiblioteket.mediestream.loar.kirker;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;

public class PackagerTest {
    private String input_dir = "/home/baj/Projects/meloar-transform/kirker/data/kirker/xml";
    private String output_dir = "/home/baj/Projects/meloar-transform/kirker/data/output/kirker_20200318";


    @BeforeMethod
    public void setUp() throws Exception {
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }

    @Test
    public void testTranslateFile() {
        File dir = new File(input_dir);
        File[] files = dir.listFiles();
        for (int seed = 1;seed<files.length;seed++) {
            Packager.translateFile(files[seed].toString(), output_dir, seed);
        }
    }
}
