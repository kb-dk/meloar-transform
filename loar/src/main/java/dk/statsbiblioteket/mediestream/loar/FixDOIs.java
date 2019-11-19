package dk.statsbiblioteket.mediestream.loar;

import com.opencsv.CSVWriter;

import java.io.*;

/**
 * We have a number of DOIs that were somehow registered with DataCite without URLs.
 * These cannot be updated through the DSpace doi-organiser.
 * To update these, we use the doi-organiser to list the concerned DOIs and the associated handles.
 * This list is transformed to a CSV-file with two columns.
 * The first column with DOIs and the second with the associated URLs.
 * The URLs are "https://loar.kb.dk/handle/"+HANDLE
 * We can then use a script to update all the URLs using this command
 *
 * curl -H "Content-Type:text/plain;charset=UTF-8" -X PUT --user DK.SB:*** -d
 * "$(printf 'doi=DOI\nurl=https://loar.kb.dk/handle/HANDLE')" https://mds.datacite.org/doi/DOI -i
 *
 * This could probably all be done in the same script!
 *
 */
public class FixDOIs {

    public static void cleanDOIlist(String txtfile, String outputdirectory) {
        File doisandurls = new File(outputdirectory + "doisandurls.csv");
        CSVWriter writer = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(txtfile));
            writer = new CSVWriter(new FileWriter(doisandurls), ',');

            //writer.writeNext(headings_line);

            String line;
            String[] splitLine;
            String doi;
            String url;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("doi:")) {
                    splitLine = line.split(" ");
                    doi = splitLine[0];
                    url = splitLine[splitLine.length-1];
                    url = url.substring(0, url.length()-1);
                    writer.writeNext(new String[]{doi, url});
                }
            }

            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
