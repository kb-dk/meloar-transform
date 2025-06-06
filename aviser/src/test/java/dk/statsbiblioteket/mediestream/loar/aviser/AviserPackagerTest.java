package dk.statsbiblioteket.mediestream.loar.aviser;

import org.junit.jupiter.api.Test;

public class AviserPackagerTest {

    private final String input_dir = "/home/baj/Tmp/aviser_input";
    private final String csv_file = "/home/baj/Projects/meloar-transform/aviser/src/main/resources/aviser_20211026_del1_002-021.csv";
    private final String output_dir = "/home/baj/Projects/meloar-transform/aviser/src/main/resources/output";

    @Test
    public void testReadInputAndWriteToSAF() {
        AvisCSVReader.readCSVFileAndWriteToSAF(input_dir, csv_file, output_dir);
    }

}
