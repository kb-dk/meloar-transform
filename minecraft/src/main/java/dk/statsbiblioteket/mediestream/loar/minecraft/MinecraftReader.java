package dk.statsbiblioteket.mediestream.loar.minecraft;

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
import java.util.List;

import static dk.statsbiblioteket.mediestream.loar.minecraft.MinecraftPackager.writeItemAsSAF;


public class MinecraftReader {
    private static Logger log = LoggerFactory.getLogger(MinecraftReader.class);

    public static final String datadirectory = "/home/baj/Projects/meloar-transform/minecraft/src/test/resources/input";
    public static final String outputdirectory = "/home/baj/Projects/meloar-transform/minecraft/src/test/resources/output";
    public static final String LICENSEFILE_NAME = "default.license";
    public static final int BUFFER = 2048;

    /**
     * The Minecraft Denmark map is simply a large number of zip-files.
     * We will zip those in fewer zip-files, and explain how to unpack these.
     * We have generated a csv file with coordinate metadata.
     * We will create one LOAR item for each zip file.
     */
    public static void readCSVFileAndWriteToSAF(String datadirectory, String csvFile, String outputdirectory) throws IOException, ParserConfigurationException, TransformerException {
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
            log.debug("headlines?!" + Arrays.toString(line));
            line = listOflines.get(1);
            log.debug("headlines?!" + Arrays.toString(line));
            for (int index=2;index<listOflines.size();index++) {
                boolean success = writeItemAsSAF(datadirectory, listOflines.get(index), outputdirectory);
                if (!success) {
                    //write the records that were NOT added to a LOAR item
                    writer.writeNext(listOflines.get(index));
                }
            }
            writer.flush();
            writer.close();
        } catch (IOException | TransformerException | ParserConfigurationException e) {
            e.printStackTrace();
        }
    }
}

