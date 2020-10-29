package dk.statsbiblioteket.mediestream.loar.lyna;

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
 * The Lyna data is 55 csv files with newspaper text from 55 years.
 * The data will be put in 55 items corresponding to the csv files / years.
 * The relevant metadata will be Newspaper (same for all), publication year and?
 */
public class LynaPackager {
    private static Logger log = LoggerFactory.getLogger(LynaPackager.class);

    public static void writeItemAsSAF(String csvfile, String outputdirectory) throws IOException,
            ParserConfigurationException, TransformerException {
        log.debug("entering writeItemAsSAF method with parameters: ("+csvfile+", "+outputdirectory+")");

        //todo: do we need to read the csv-file?
        //We can get the year from the filename
        File file = new File(csvfile);
        String filename = file.getName();
        String year = filename.substring(5,9);
        System.out.println("year: "+year);

        //First we need a directory for this item
        File item_directory = new File(outputdirectory, "item_" + year);
        item_directory.mkdir();

        //The contents file simply enumerates, one file per line, the bitstream file names
        //The bitstream name may optionally be followed by \tpermissions:PERMISSIONS
        File contents = new File(item_directory, "contents");
        contents.createNewFile();
        FileWriter contentsFileWriter = new FileWriter(contents);

        //Copy the csv to this directory
        Path fileSource = file.toPath();
        Path fileDest = new File(item_directory, filename).toPath();
        try {
            Files.copy(fileSource, fileDest, REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //write the file name to the contents file
        contentsFileWriter.write(filename+"\n");

        //The dublin_core.xml file contains some of the metadata as dublin core
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document dcdoc = builder.newDocument();
        Element dcroot = dcdoc.createElement("dublin_core");
        dcroot.setAttribute("schema", "dc");//First we need a directory for this item
        dcdoc.appendChild(dcroot);

        //Date issued
        addElement("date", "issued", year, dcdoc, dcroot);
        //Data type
        addElement("type", null, "Dataset", dcdoc, dcroot);
        //Author
        addElement("contributor", "author", "Royal Danish Library", dcdoc, dcroot);
        //Language: Deutch
        addElement("language", null, "de", dcdoc, dcroot);
        //Rights
        addElement("rights", null, "CC0 1.0 Universal",dcdoc, dcroot);
        addElement("rights", "uri", "http://creativecommons.org/publicdomain/zero/1.0/", dcdoc, dcroot);
        //Subject
        addElement("subject", null, "newspaper", dcdoc, dcroot);

        //todo Slightly different names in different periods
        if (Integer.parseInt(year)<=1839) {//Lyna. Eine Wochenschrift (1797-1839)
            //Title
            addElement("title", null, "Lyna. Eine Wochenschrift ("+year+")", dcdoc, dcroot);
            //Subject
            addElement("subject", null, "Lyna. Eine Wochenschrift (1797-1839)", dcdoc, dcroot);
            //Description
            addElement("description", "abstract", "Newspaper from "+year+": Lyna. Eine Wochenschrift.", dcdoc, dcroot);
            //Link til mediestream
            addElement("relation", "uri", "http://www2.statsbiblioteket.dk/mediestream/avis/list/" +
                    "doms_newspaperAuthority%3Auuid%3A9f16621f-4756-465e-8315-bc86cbea4de8", dcdoc, dcroot);
        }

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
        dcvalue.setAttribute("element", element);
        if (qualifier!=null) {
            dcvalue.setAttribute("qualifier", qualifier);
        }
        dcvalue.setAttribute("language", "en_US");
        dcvalue.setTextContent(textContent);
        root.appendChild(dcvalue);
    }

}
