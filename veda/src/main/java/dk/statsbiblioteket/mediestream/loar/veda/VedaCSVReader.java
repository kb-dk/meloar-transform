package dk.statsbiblioteket.mediestream.loar.veda;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static dk.statsbiblioteket.mediestream.loar.veda.VedaPackager.writeItemAsSAF;

public class VedaCSVReader {

    public static final String datadirectory = "/home/baj/Projects/meloar-transform/veda/src/test/resources/veda_opdeling_i_værker.xlsx";
    public static final String outputdirectory = "/home/baj/Projects/meloar-transform/veda/src/test/resources/output";
    public static final String LICENSEFILE_NAME = "default.license";
    public static final int BUFFER = 2048;

    /**
     * Read the VEDA metadata csv-file AND write a LOAR input file for each Title.
     *
     * @param csvFile the file to read
     */
    public static void readCSVFileAndWriteToSAF(String datadirectory, String csvFile, String outputdirectory) {

        CSVReader reader = null;
        CSVWriter writer = null;
        try {
            reader = new CSVReader(new FileReader(csvFile),'\t');
            writer = new CSVWriter(new FileWriter(new File(outputdirectory, "kom_ikke_med.csv")), ',');
            String[] line;
            reader.readNext();//overskrifter

            List<String[]> listOflines = reader.readAll();

            while ((line = reader.readNext()) != null) {// && !line[0].isEmpty() && !line[1].isEmpty()
                String title = line[1];
                LinkedList<String> records = new LinkedList<String>();
                records.add(line[0]);
                while ((line = reader.readNext()) != null && line[1].equals(title)) {// && !line[0].isEmpty() && !line[1].isEmpty()
                    records.add(line[0]);
                }
                    boolean success = writeItemAsSAF(datadirectory, line, outputdirectory);
//Todo du er kommet en linje for langt!
                    if (!success) {
                        //write the lines that were not added to a LOAR item
                        writer.writeNext(line);
                }
            }
            writer.flush();
            writer.close();
        } catch (IOException | TransformerException | ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

}
