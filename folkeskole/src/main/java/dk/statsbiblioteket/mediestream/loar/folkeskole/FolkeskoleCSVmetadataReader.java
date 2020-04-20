package dk.statsbiblioteket.mediestream.loar.folkeskole;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

import static dk.statsbiblioteket.mediestream.loar.folkeskole.FolkeSkolePackagerV2.writeItemAsSAF;

//import static dk.statsbiblioteket.mediestream.loar.folkeskole.FolkeskoleSimplePackager.writeItemAsSAF;

public class FolkeskoleCSVmetadataReader {
    public static final String outputdirectory = "/home/baj/Projects/meloar-transform/folkeskole/data/output";
    public static final String LICENSEFILE_NAME = "default.license";
    public static final int BUFFER = 2048;

    /**
     * Read the folkeskole metadata csv-file AND write a LOAR input file for each line.
     *
     * @param csvFile the file to read
     */
    public static void readCSVFileAndWriteToSAF(String csvFile, String outputdirectory) {


        CSVReader reader = null;
        CSVWriter writer = null;
        try {
            reader = new CSVReader(new FileReader(csvFile), ',');
            writer = new CSVWriter(new FileWriter(new File(outputdirectory, "kom_ikke_med.csv")), ',');
            String[] line;
            reader.readNext();//overskrifter
            while ((line = reader.readNext()) != null) {
                if (!line[0].isEmpty() ) {
                    boolean success = writeItemAsSAF(line, outputdirectory);
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
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

}
