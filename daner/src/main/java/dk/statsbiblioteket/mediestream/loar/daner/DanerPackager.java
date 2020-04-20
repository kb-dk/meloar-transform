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
     * Read the input images AND write a LOAR input file for each image.
     * @param inputdirectory the image directory
     */
    public static void readInputAndWriteToSAF(String inputdirectory, String outputdirectory) throws IOException, ParserConfigurationException, TransformerException {
        File inputdir = new File(inputdirectory);
        if (inputdir.isDirectory()) {
            File[] images = inputdir.listFiles();
            for (File image: images) {
                writeItemAsSAF(image, outputdirectory);
            }
        }
    }

    /**
     * Write a LOAR DSpace Simple Archive Format structure based on the given line.
     * @param item String array with metadata for 1 item
     * @param outputdirectory String directory name where to put structure
     */
    public static void writeItemAsSAF(File item, String outputdirectory) throws IOException, ParserConfigurationException, TransformerException {
        System.out.println(count);

        //First we need a directory for this item
        File item_directory = new File(outputdirectory, "item" + count);
        item_directory.mkdir();
        count++;

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

        Path imageFileSource = item.toPath();
        Path imageFileDest = new File(item_directory, item.getName()).toPath();
        try {
            Files.copy(imageFileSource, imageFileDest, REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //write the file name to the contents file
        contentsFileWriter.write(item.getName());

        //write the file to the dublin core file
        addElement("title", null, item.getName(), dcdoc, dcroot);

        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(dcdoc);
        StreamResult streamResult = new StreamResult(new File(item_directory, "dublin_core.xml"));
        transformer.transform(source, streamResult);

        //remember to write the contents file
        contentsFileWriter.flush();
        contentsFileWriter.close();

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
