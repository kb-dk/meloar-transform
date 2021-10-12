package dk.statsbiblioteket.mediestream.loar.aviser;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import static dk.statsbiblioteket.mediestream.loar.aviser.AvisPackager.writeItemAsSAF;

public class AvisCSVReader {

    public static final String datadirectory = "/home/baj/Projects/meloar-transform/aviser/src/main/resources/aviser_20210920_baj.csv";
    public static final String outputdirectory = "/home/baj/Projects/meloar-transform/aviser/src/main/resources/output";
    public static final String LICENSEFILE_NAME = "default.license";
    public static final int BUFFER = 2048;

    /**
     * Read the avis metadata csv-file AND write a LOAR input file for each line.
     *
     * @param csvFile the file to read
     */
    public static void readCSVFileAndWriteToSAF(String datadirectory, String csvFile, String outputdirectory) {

        System.out.println(csvFile);
        CSVReader reader = null;
        CSVWriter writer = null;
        try {
            reader = new CSVReader(new FileReader(csvFile),'\t');
            writer = new CSVWriter(new FileWriter(new File(outputdirectory, "kom_ikke_med.csv")), ',');
            String[] line;
            reader.readNext();//overskrifter

            while ((line = reader.readNext()) != null) {
                if (!line[0].isEmpty()) {
                    System.out.println("line[0]="+line[0]);
                    boolean success = writeItemAsSAF(datadirectory, line, outputdirectory);
                    if (!success) {
                        //write the lines that were not transformed to a LOAR item
                        writer.writeNext(line);
                    }
                }
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

}
