package dk.statsbiblioteket.mediestream.loar;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * The UpdateDOIS class is used to update DOIs in the metadata of the items,
 * which we have just fixed! They now have working DOIs, but the DOI's were not
 * updated in the metadata as expected. This is what this class fixes.
 *
 * The metadata can be exported as csv, one item or one collection at a time or all content...
 * The csv can then be updated and imported.
 *
 * Note: It is not recommended to import CSV files of more than 1,000 lines (i.e. 1,000 items).
 * We have a total of 4122 items in LOAR as of Tue Nov 19 12:40:38 CET 2019
 * We do not have that many collections, so I think we will do this on collection level.
 *
 * First Collection: "1902/333" (Beretningsarkiv for Arkæologiske Undersøgelser) (fof)
 * Size:3781
 * Wait, maybe we should just do all!
 * No, I think one collection at a time is safer.
 * Then we won't update the items that already have correct DOIs.
 *
 * 2. collection: "1902/4276" (Datasprint 2019)
 * Size:2
 * By hand! Can only find one of two DOIs
 *
 * 3. collection: "1902/4119" (Ruben Recordings)
 * Size:154
 *
 * 4. collection: Department of Agroecology: AnaEE "1902/4286"
 * Size:1
 * Hvorfor har den ikke automatisk fået en DOI?
 * Something is broken!
 *
 * 5. collection:  Royal Danish Library → Events → Archive for Danish Literature in 3D:
 * Data, Danish Literature & Distant Reading "1902/316"
 * Size:5
 * 4 of 5 already have DOIs
 * By hand!
 * Wait! "./bin/dspace doi-organiser -u" ser ud til at virke i dag...
 * No, sorry!
 * By hand!
 *
 * 6. collection: Royal Danish Library → Open Digital Collections → IT Development → Machine learning "1902/326"
 * Size:2
 * By hand!
 *
 * 7. collection: Royal Danish Library → Open Digital Collections → Newspapers from Royal Danish Library →
 * Front pages of Berlingske "1902/322"
 * Size:1
 * By hand!
 *
 * 8. collection: Royal Danish Library → Research Data → Military land use in Denmark 1870-2017 "1902/4117"
 * Size:1
 * By hand!
 *
 * 9. collection: LOAR Home → Royal Danish Library → Research Data → Soviet and Warsaw-pact maps "1902/4116"
 * Size:1
 * By hand!
 *
 */
public class UpdateDOIs {

    /**
     * Update metadata CSV with DOIs from the "dois and urls CSV".
     * @param metadatacsv metadatacsv file path
     * @param doisandurlscsv doisandurlscsv file path
     * @param outputdir output dir path
     */
    public static void updateCSVwithDOIs(String metadatacsv, String doisandurlscsv, String outputdir) {
        // read metadata csv into a METADATA MAP
        CSVReader reader = null;
        Map<Integer, String[]> metadataMap= new HashMap<Integer, String[]>();
        Integer lineNumber = 0;
        String[] line;
        String[] headings_line = null;
        try {
            reader = new CSVReader(new FileReader(metadatacsv), ',');
            // the first line is the headings line
            headings_line = reader.readNext();
            lineNumber++;
            metadataMap.put(lineNumber, headings_line);
            while ((line = reader.readNext()) != null) {
                lineNumber++;
                metadataMap.put(lineNumber, line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // the url is in column (dc.identifier.uri) or (dc.identifier.uri[])
        // the url and the doi (as url) should be in column (dc.identifier.uri[]) separated by ||
        // the (dc.identifier.uri) column can be deleted
        int columnURI;
        for (columnURI = 0; columnURI<headings_line.length; columnURI++) {
            if (headings_line[columnURI].trim().equals("dc.identifier.uri")) {
                break;
            }
        }
        int columnURIs;
        for (columnURIs = 0; columnURIs<headings_line.length; columnURIs++) {
            if (headings_line[columnURIs].trim().equals("dc.identifier.uri[]")) {
                break;
            }
        }
        // there may not be a column "dc.identifier.uri[]" (Ruben collection)
        // in which case we add this column at the end
        if (columnURIs==headings_line.length) {
            headings_line = Arrays.copyOf(headings_line, columnURIs+1);
            headings_line[columnURIs] = "dc.identifier.uri[]";
            for (Integer key: metadataMap.keySet()) {
                String[] metadata = metadataMap.get(key);
                metadataMap.put(key, Arrays.copyOf(metadata, columnURIs+1));
            }
        }

        // read "dois and urls csv" into a DOIS-AND-URLS MAP (it is really a DOI and HANDLE CSV,
        // and the map is from HANDLE to DOI)
        Map<String, String> mapFromHandlesToDOIs = new HashMap<String, String>();
        try {
            reader = new CSVReader(new FileReader(doisandurlscsv), ',');
            String[] lineArray;
            while ((lineArray = reader.readNext()) != null) {
                mapFromHandlesToDOIs.put(lineArray[1], lineArray[0]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // update the metadata MAP with the DOIs
        // (todo we could choose to update other metadata such as publisher and type,
        // as they are missing for the fof collection)
        // remember to skip the heading line
        String loar_uri = "https://loar.kb.dk/handle/";
        for (lineNumber=2;lineNumber<metadataMap.size();lineNumber++) {
            String[] metadata = metadataMap.get(lineNumber);
            System.out.println("log DEBUG: lineNumber="+lineNumber);
            //find handle
            String identifier = null;
            String[] identifierList = null;
            String handle = null;
            if (metadata[columnURI]!=null && !metadata[columnURI].isEmpty() && metadata[columnURI].contains(loar_uri)) {
                identifier = metadata[columnURI];
                handle = identifier.substring(loar_uri.length());
            } else if (columnURIs<metadata.length && metadata[columnURIs]!=null && !metadata[columnURIs].isEmpty()) {
                identifierList = metadata[columnURIs].split("\\|\\|");
                for (String id: identifierList) {
                    System.out.println("log DEBUG: id="+id);
                    if (id.contains(loar_uri)) {
                        handle = id.substring(loar_uri.length());
                    }
                }
            }
            if (handle==null) {
                System.out.println("log WARNING: could not find handle! metadata= "+ Arrays.deepToString(metadata));
                break;
            }
            //look up DOI
            String doi = mapFromHandlesToDOIs.get(handle);

            //update metadata map
            metadata[columnURI] = "";

            Set<String> identifierSet = new HashSet<String>();
            if (identifier!=null) {
                identifierSet.add(identifier);
            }
            if (identifierList!=null) {
                for (String id: identifierList) {
                    identifierSet.add(id);
                }
            }
            if (doi!=null) {
                identifierSet.add("http://dx.doi.org/" + doi);
            } else {
                System.out.println("log WARNING: could not find DOI!");
            }

            Iterator<String> stringIterator= identifierSet.iterator();
            metadata[columnURIs] = stringIterator.next();
            while (stringIterator.hasNext()) {
                metadata[columnURIs]+="||"+stringIterator.next();
            }
        }

        // write the updated metadata csv to outputdir
        // or rather now write the metadata map back to a csv file in outputdir
        File newCSVMetadataFile = new File(outputdir + "/new_"+
                metadatacsv.substring("/home/baj/Projects/meloar-transform/loar/src/test/resources/input/".length()));
        CSVWriter writer = null;
        try {
            writer = new CSVWriter(new FileWriter(newCSVMetadataFile), ',');
            for (int i = 0; i < metadataMap.size(); i++) {
                writer.writeNext(metadataMap.get(i));
            }
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
