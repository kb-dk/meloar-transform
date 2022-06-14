package dk.statsbiblioteket.mediestream.loar.veda;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static dk.statsbiblioteket.mediestream.loar.veda.VedaPackager.writeItemAsSAF;

public class VedaCSVReader {
    private static Logger log = LoggerFactory.getLogger(VedaCSVReader.class);

    public static final String datadirectory = "/home/baj/Projects/meloar-transform/veda/src/test/resources/Veda-metadata_jamo.xlsx";
    public static final String outputdirectory = "/home/baj/Projects/meloar-transform/veda/src/test/resources/output";
    public static final String LICENSEFILE_NAME = "default.license";
    public static final int BUFFER = 2048;

    /**
     * Read the VEDA metadata csv-file AND write a LOAR input file for each Work.
     *
     * Metadata-filen indeholder:
     * A0 Kolonne1, B1 Keywords, C2 Lokalitet, D3 Topografinr, E4 Categories, F5 Record Name, G6 Dato ikke efter, H7 Under-titel,
     * I8 År, J9 Intern note, K10 Genre, L11 Opstilling, M12 Udgiver, N13 Medvirkende rolle, O14 Udgiver rolle, P15 Alternativ titel,
     * Q16 Medvirkende, R17 Samling, S18 Titel, T19 Materialebetegnelse, U20 Sprog, V21 Note
     *
     * @param csvFile the file to read
     */
    public static void readCSVFileAndWriteToSAF(String datadirectory, String csvFile, String outputdirectory) {

        CSVReader reader = null;
        CSVWriter writer = null;
        try {
            reader = new CSVReader(new FileReader(csvFile),',');
            writer = new CSVWriter(new FileWriter(new File(outputdirectory, "kom_ikke_med.csv")), ',');

            List<String[]> listOflines = reader.readAll();
            String[] line;
            String[] m;
            //headlines
            line = listOflines.get(0);
            log.debug("headlines?!" +Arrays.toString(line));
            //Introduktion-Introduction_Veda.pdf
            line = listOflines.get(1);
            log.debug("introduktion?!" +Arrays.toString(line));
            int index = 2;
            int j = 2;
            log.debug("listOflines.size()="+listOflines.size());
            while (index < listOflines.size()) {
                line = listOflines.get(index);
                String title = line[18];
                String subtitle = line[7];
                String alttitle = line[15];
                LinkedList<String> records = new LinkedList<String>();
                records.add(line[5]);
                index++;
                if (index < listOflines.size()) {
                    j = index;
                    m = listOflines.get(j);
                    while (j < listOflines.size() && m[18].equals(title)
                            && m[7].equals(subtitle) && m[15].equals(alttitle)) {
                        records.add(m[5]);
                        j++;
                        if (j < listOflines.size()) {
                            m = listOflines.get(j);
                        }
                    }
                }
                boolean success = writeItemAsSAF(datadirectory, line, records, outputdirectory);
                if (!success) {
                        //write the records that were NOT added to a LOAR item
                        writer.writeNext(records.toArray(new String[0]));
                }

                index = j;

            }
            writer.flush();
            writer.close();
        } catch (IOException | TransformerException | ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

}
