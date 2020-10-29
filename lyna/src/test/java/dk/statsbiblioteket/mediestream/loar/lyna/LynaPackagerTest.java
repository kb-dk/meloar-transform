package dk.statsbiblioteket.mediestream.loar.lyna;

import org.testng.annotations.Test;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;

public class LynaPackagerTest {

    private String csvfile = "src/main/resources/lyna_1797.csv";
    private String csvdirectory = "src/main/resources/";
    private String outputdirectory = "src/test/resources/output/";

    @Test
    public void testWriteItemAsSAF() throws ParserConfigurationException, TransformerException, IOException {
        File dir = new File(csvdirectory);
        File[] files = dir.listFiles();
        for (File f: files) {
            LynaPackager.writeItemAsSAF(f.getPath(), outputdirectory);
        }
    }
}