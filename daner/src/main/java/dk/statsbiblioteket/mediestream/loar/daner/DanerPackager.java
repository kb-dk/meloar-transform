package dk.statsbiblioteket.mediestream.loar.daner;

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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * The class DanerPackager is home to the writeItemAsSAF method, which "translates" an image from the
 * daner project into the DSpace Simple Archive Format, which can be ingested into a DSpace archive
 * (https://wiki.duraspace.org/display/DSDOC6x/Importing+and+Exporting+Items+via+Simple+Archive+Format).
 */
public class DanerPackager {

    private static Logger log = LoggerFactory.getLogger(dk.statsbiblioteket.mediestream.loar.daner.DanerPackager.class);
    private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private static int count = 0;

    /**
     * Write a LOAR DSpace Simple Archive Format structure based on the given line.
     * @param line String array with metadata for 1 item
     * @param outputdirectory String directory name where to put structure
     */
    public static boolean writeItemAsSAF(String datadirectory, String[] line, String outputdirectory) throws IOException, ParserConfigurationException, TransformerException {

        // Only do something if you have the file
        File image = new File(line[0]);
        if (!image.exists()) { return false;}

        log.debug("Entering writeItemAsSAF with line = "+ Arrays.toString(line));

        //First we need a directory for this item
        File item_directory = new File(outputdirectory, "item" + count);
        item_directory.mkdir();
        count++;
        log.debug("count = "+count);

        //The contents file simply enumerates, one file per line, the bitstream file names
        //The bitstream name may optionally be followed by \tpermissions:PERMISSIONS
        File contents = new File(item_directory, "contents");
        contents.createNewFile();
        FileWriter contentsFileWriter = new FileWriter(contents);

        //The dublin_core.xml file contains some of the metadata as dublin core
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document dcdoc = builder.newDocument();
        Element dcroot = dcdoc.createElement("dublin_core");
        dcroot.setAttribute("schema", "dc");
        dcdoc.appendChild(dcroot);

            //Copy the image file to the item directory
            Path imageFileSource = image.toPath();
            Path imageFileDest = new File(item_directory, line[0]).toPath();
            try {
                Files.copy(imageFileSource, imageFileDest, REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //write the file name to the contents file
            contentsFileWriter.write(line[0]);

        // Write metadata to DC file
        // write the FileName (line[0]) to the dublin core file: NO

        // write the PersonsName (line[1]) to the dublin core file
        // write the PersonsFamilyName (line[2]) to the dublin core file
        // Combine the two into title
        String title = line[1] + " " + line[2];
        addElement("title", null, title, dcdoc, dcroot);

        // write the DateOfBirth (line[3]) to the dublin core file
        // write the DateOfDeath (line[4]) to the dublin core file
        // write the PersonsJob (line[6]) to the dublin core file
        // Combine the three into description - or rather the five
        String description = line[2] + ", " + line[1] + " (" + line[3] + "-" + line[4] + ") " + line[6];
        addElement("description", null, description, dcdoc, dcroot);

        // write the DateOfPhotography (line[5]) to the dublin core file
        String date = line[5];
        if (!date.equals("")) {
            if (date.matches("\\d{4}")) {
                addElement("date", "issued", date, dcdoc, dcroot);
            } else {
                if (date.matches("\\d{4}-\\d{4}")) {
                    addElement("date", "issued", date.substring(0,4), dcdoc, dcroot);
                    addElement("date", "issued", date.substring(5,9), dcdoc, dcroot);
                }
            }
        }

        // write the Photographer (line[7]) to the dublin core file
        addElement("contributor", "author", line[7],dcdoc, dcroot);

        // write the LinkToRoyalDanishLibrarysDigitalCollections (line[8]) to the dublin core file
        addElement("relation", "uri", line[8], dcdoc, dcroot);

        // write type
        addElement("type", null, "Image", dcdoc, dcroot);
        // write subjects
        addElement("subject", null, "Photograph", dcdoc, dcroot);
        //add publisher to DC
        addElement("publisher", null, "Det Kgl. Bibliotek", dcdoc, dcroot);


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
        dcvalue.setAttribute("element", element);//no qualifier
        if (qualifier!=null) {
            dcvalue.setAttribute("qualifier", qualifier);
        }
        dcvalue.setAttribute("language", "da_DK");//no qualifier
        dcvalue.setTextContent(textContent);
        root.appendChild(dcvalue);
    }
}
