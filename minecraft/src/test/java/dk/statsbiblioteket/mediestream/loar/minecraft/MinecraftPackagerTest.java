package dk.statsbiblioteket.mediestream.loar.minecraft;

import org.testng.annotations.Test;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;

public class MinecraftPackagerTest {

    private final String input_dir = "/home/baj/Projects/meloar-transform/minecraft/src/main/resources/input";
    private final String scv_file = "/home/baj/Projects/meloar-transform/minecraft/src/main/resources/input/minecraft_filelist_short.csv";
    private final String output_dir = "/home/baj/Projects/meloar-transform/minecraft/src/main/resources/output";

    @org.testng.annotations.BeforeMethod
    public void setUp() {
    }

    @org.testng.annotations.AfterMethod
    public void tearDown() {
    }

    @Test
    public void testReadInputAndWriteToSAF() throws IOException, ParserConfigurationException, TransformerException {
        MinecraftReader.readCSVFileAndWriteToSAF(input_dir, scv_file, output_dir);
    }

}
