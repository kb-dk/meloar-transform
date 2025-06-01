package dk.statsbiblioteket.mediestream.loar.daner;

import org.junit.jupiter.api.Test;

public class DanerPackagerTest {

    private String input_dir = "/home/baj/Projects/meloar-transform/daner/src/main/resources/input/faces_without_copyright";
    private String csv_file = "/home/baj/Projects/meloar-transform/daner/src/main/resources/input/short.csv";
    private String output_dir = "/home/baj/Projects/meloar-transform/daner/src/main/resources/output";

    @Test
    public void testReadInputAndWriteToSAF() {
        DanerCSVReader.readCSVFileAndWriteToSAF(input_dir, csv_file, output_dir);
    }

}
