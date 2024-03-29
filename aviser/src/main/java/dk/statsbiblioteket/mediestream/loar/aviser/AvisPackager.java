package dk.statsbiblioteket.mediestream.loar.aviser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * The new newspaper data is packed in batches such as sample.B400027040304-RT2
 * This batch contains 15 smaller batches such as sample.B400027040304-RT2.400027040304-01
 * This batch contains more than a hundred directories such as sample.B400027040304-RT2.400027040304-01.1863-01-13-01
 * with matching xml files sample/B400027040304-RT2/400027040304-01/1863-01-13-01-edition.xml
 * The directory contains ALTO, MODS and JP2 for an number of newspaper pages such as
 * sample/B400027040304-RT2/400027040304-01/1863-01-13-01/lemvigavis-1863-01-13-01-0005-alto.xml
 * sample/B400027040304-RT2/400027040304-01/1863-01-13-01/lemvigavis-1863-01-13-01-0005-mods.xml
 * sample/B400027040304-RT2/400027040304-01/1863-01-13-01/lemvigavis-1863-01-13-01-0005-presentation.jp2
 *
 * The question is how to share the data.
 * The size of the zip file is 12,5 GB.
 * If we want to share the data as batches, we need to extract metadata for each batch somehow.
 * We now also have an Excel document with metadata on Batch Level
 *
 * TODO
 */
public class AvisPackager {
    private static Logger log = LoggerFactory.getLogger(AvisPackager.class);
    private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private static int count = 0;
    private static String attribute_language_da = "da";
    private static String attribute_language_en_US = "en_US";

    /**
     * Write a LOAR DSpace Simple Archive Format structure based on the given line.
     *
     * @param datadirectory   String directoryname name where to read data
     * @param line            String array with metadata for 1 item
     * @param outputdirectory String directory name where to put structure
     */
    public static boolean writeItemAsSAF(String datadirectory, String[] line, String outputdirectory) throws IOException,
            ParserConfigurationException, TransformerException {
        log.debug("entering writeItemAsSAF method with parameters: (" + datadirectory + ", " + Arrays.toString(line) + "," + outputdirectory + ")");

        // Only do something if you have the file
        // Resource kolonne 6
        File batch = null;
        if (line.length>6) {
            batch = new File(datadirectory, line[6]);
            log.debug("Batch file = " + batch.toString());
            if (!batch.exists()) {
                return false;
            }
        } else {
            log.debug("line.length="+line.length);
            return false;
        }

        //First we need a directory for this item
        File item_directory = new File(outputdirectory, "item" + count);
        item_directory.mkdir();
        count++;
        log.debug("count = " + count);

        //We also need to move or link or copy the zip file to this new directory
        //Files.copy()
        Path batch_path = batch.toPath();

            Files.copy(batch_path, item_directory.toPath().resolve(batch_path.getFileName()));

        //The contents file simply enumerates, one file per line, the bitstream file names
        //The bitstream name may optionally be followed by \tpermissions:PERMISSIONS
        File contents = new File(item_directory, "contents");
        contents.createNewFile();
        FileWriter contentsFileWriter = new FileWriter(contents);
        contentsFileWriter.write(batch.getName());

        //The dublin_core.xml file contains some of the metadata as dublin core
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document dcdoc = builder.newDocument();
        Element dcroot = dcdoc.createElement("dublin_core");
        dcroot.setAttribute("schema", "dc");
        dcdoc.appendChild(dcroot);

        //Title: Avis id kolonne 2 + Batch kolonne 0
        addElement("title", null, line[2]+" Batch "+line[0], attribute_language_da, dcdoc, dcroot);
        //Dates issued: Start Date kolonne 3 / End Date kolonne 4
        addElement("date", "issued", line[3] + "/" + line[4], null, dcdoc, dcroot);
        //Place issued: Denmark
        addElement("coverage", "spatial", "Denmark",attribute_language_en_US, dcdoc, dcroot);
        //Data type: Dataset
        addElement("type", null, "Dataset", attribute_language_en_US, dcdoc, dcroot);
        //Author: Royal Danish Library
        addElement("contributor", "author", "Royal Danish Library", attribute_language_en_US, dcdoc, dcroot);
        //Language: da
        addElement("language", null, "da", null, dcdoc, dcroot);
        //Rights: Public Domain
        addElement("rights", null, "CC Public Domain", attribute_language_en_US, dcdoc, dcroot);
        addElement("rights", "uri", "https://creativecommons.org/publicdomain/mark/1.0/deed.en",
                attribute_language_en_US, dcdoc, dcroot);
        //Subjects: newspaper
        addElement("subject", null, "newspaper", attribute_language_en_US, dcdoc, dcroot);
        //Description: Avis id kolonne 2 + Batch kolonne 0 + Roundtrip kolonne 1
        // + Start Date kolonne 3 / End Date kolonne 4
        // + Pages kolonne 5 + Unmatched Pages kolonne 6
        addElement("description", "abstract", line[2]+". Batch "+line[0]+". Roundtrip "+line[1]+
                ". "+line[3] + "/" + line[4]+". Pages: "+line[5]+".", null, dcdoc, dcroot);

        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(dcdoc);
        StreamResult streamResult = new StreamResult(new File(item_directory, "dublin_core.xml"));
        transformer.transform(source, streamResult);

        //remember to write the contents file
        contentsFileWriter.flush();
        contentsFileWriter.close();

        return true;
    }



    /**
     *
     * @param element
     * @param qualifier
     * @param textContent
     * @param doc
     * @param root
     */
    public static void addElement(String element, String qualifier, String textContent, String attribute_language, Document doc, Element root) {
        Element dcvalue = doc.createElement("dcvalue");
        dcvalue.setAttribute("element", element);
        if (qualifier!=null) {
            dcvalue.setAttribute("qualifier", qualifier);
        }
        if (attribute_language!=null) {
            dcvalue.setAttribute("language", attribute_language);
        }
        dcvalue.setTextContent(textContent);
        root.appendChild(dcvalue);
    }
    /**
     * Package aviser batches for ingest into LOAR
     * @param args String input_dir String csv_file String output_dir
     */
    public static void main(String[] args) {
        if (args.length!=3) {
            System.out.println("This method takes 3 arguments:\nString input_dir\nString csv_file\nString output_dir");
            return;
        }
        try {
            String input_dir = args[0];
            String csv_file = args[1];
            String output_dir = args[2];
            AvisCSVReader.readCSVFileAndWriteToSAF(input_dir, csv_file, output_dir);
        } catch(Exception e) {
            System.out.println(e);
        }
    }
}
