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
        // Resource kolonne 7
        File batch = null;
        if (line.length>7) {
            batch = new File(datadirectory, line[7]);
            log.debug("Batch file = " + batch.toString());
            if (!batch.exists()) {
                return false;
            }
        } else {
            return false;
        }

        //First we need a directory for this item
        File item_directory = new File(outputdirectory, "item" + count);
        item_directory.mkdir();
        count++;
        log.debug("count = " + count);

        //We also need to move the zip file to this new directory
        //Files.copy() // I don't want to copy big zip files if I don't have to
        Path batch_path = batch.toPath();
        File item_bitstream = new File(item_directory, line[7]);
        Path item_bitstream_path = item_bitstream.toPath();
        if (!item_bitstream.exists()) {
            Files.createSymbolicLink(item_bitstream_path, batch_path);
        }

        //The contents file simply enumerates, one file per line, the bitstream file names
        //The bitstream name may optionally be followed by \tpermissions:PERMISSIONS
        File contents = new File(item_directory, "contents");
        contents.createNewFile();
        FileWriter contentsFileWriter = new FileWriter(contents);
        contentsFileWriter.write(batch + "\tpermissions:-r 'Administrator'\n");

        //The dublin_core.xml file contains some of the metadata as dublin core
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document dcdoc = builder.newDocument();
        Element dcroot = dcdoc.createElement("dublin_core");
        dcroot.setAttribute("schema", "dc");
        dcdoc.appendChild(dcroot);

        //Title: Avis id kolonne 2 + Batch kolonne 0
        addElement("title", null, line[2]+" Batch "+line[0], dcdoc, dcroot);
        //Dates issued: Start Date kolonne 3 / End Date kolonne 4
        addElement("date", "issued", line[3] + "/" + line[4], dcdoc, dcroot);
        //Place issued: Denmark
        addElement("coverage", "spatial", "Denmark", dcdoc, dcroot);
        //Data type: Dataset
        addElement("type", null, "Dataset", dcdoc, dcroot);
        //Author: Royal Danish Library
        addElement("contributor", "author", "Royal Danish Library", dcdoc, dcroot);
        //Language: da
        addElement("language", null, "da", dcdoc, dcroot);
        //Rights: Public Domain
        addElement("rights", null, "CC0 1.0 Universal", dcdoc, dcroot);
        addElement("rights", "uri", "http://creativecommons.org/publicdomain/zero/1.0/", dcdoc, dcroot);
        //Subjects: newspaper
        addElement("subject", null, "newspaper", dcdoc, dcroot);
        //Description: Avis id kolonne 2 + Batch kolonne 0 + Roundtrip kolonne 1
        // + Start Date kolonne 3 / End Date kolonne 4
        // + Pages kolonne 5 + Unmatched Pages kolonne 6
        addElement("description", "abstract", line[2]+". Batch "+line[0]+". Roundtrip "+line[1]+
                ". "+line[3] + "/" + line[4]+". Pages: "+line[5]+". Unmatched Pages: "+line[6]+"." , dcdoc, dcroot);

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
    public static void addElement(String element, String qualifier, String textContent, Document doc, Element root) {
        Element dcvalue = doc.createElement("dcvalue");
        dcvalue.setAttribute("element", element);
        if (qualifier!=null) {
            dcvalue.setAttribute("qualifier", qualifier);
        }
        dcvalue.setAttribute("language", "en_US");
        dcvalue.setTextContent(textContent);
        root.appendChild(dcvalue);
    }

}
