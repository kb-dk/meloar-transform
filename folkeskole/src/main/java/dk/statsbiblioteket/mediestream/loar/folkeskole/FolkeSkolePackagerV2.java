package dk.statsbiblioteket.mediestream.loar.folkeskole;

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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;


/**
 * The class FolkeSkolePackagerV2 is home to the writeItemAsSAF method, which "translates" a line from the
 * "skolelove.csv" file detailing metadata from https://library.au.dk/materialer/saersamlinger/skolelove/ and
 * the corresponding pdf file into the DSpace Simple Archive Format, which can be ingested into a DSpace archive
 * (https://wiki.duraspace.org/display/DSDOC6x/Importing+and+Exporting+Items+via+Simple+Archive+Format).
 */
public class FolkeSkolePackagerV2 {

    private static Logger log = LoggerFactory.getLogger(dk.statsbiblioteket.mediestream.loar.folkeskole.FolkeskoleSimplePackager.class);
    private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private static int count = 0;

    /**
     * Write a LOAR DSpace Simple Archive Format structure based on the given line.
     * @param item String array with metadata for 1 item
     * @param outputdirectory String directory name where to put structure
     */
    public static void writeItemAsSAF(String[] item, String outputdirectory)
            throws ParserConfigurationException, IOException, TransformerException, ParseException, URISyntaxException {

        System.out.println(count);
        //First we need a directory for this item
        File item_directory = new File(outputdirectory, "item"+count);
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

        //uid (kolonne 0) -> identifier
        //todo

        //titel (kolonne 1) -> title
        addElement("title", null, item[1], dcdoc, dcroot);

        //lawtype (kolonne 2) -> ??
        //todo
        //year (kolonne 3)
        // date (kolonne 4) -> issued date
        String date = item[4];
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        if (!date.equals("")) {
            if (date.matches("\\d{4}") || date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                addElement("date", "issued", date, dcdoc, dcroot);
            } else {
                LocalDate parsedDate = LocalDate.parse(item[4], formatter);
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                addElement("date", "issued", parsedDate.format(formatter), dcdoc, dcroot);
            }
        }
        //regulation_no (kolonne 5) -> ?? det er vel en slags identifier
        if (item.length>5 && !item[5].equals("")) {
            addElement("identifier", "other", item[5], dcdoc, dcroot);
        }
        //educationtype (kolonne 6) -> ?? det må være en type
        if (item.length>6 && !item[6].equals("")) {
            addElement("type", null, item[6], dcdoc, dcroot);
        }

        //write the pdf files from the "pdf_version" (kolonne 7) fields in this item directory
        //and in the contents file
        //and add to dublin core document preceded by "https://library.au.dk/uploads/tx_lfskolelov/"
        if (item.length>7) {
            String pdfName = item[7];
            if (pdfName != null || !pdfName.equals("")) {
                String pdfUrl = "https://library.au.dk/uploads/tx_lfskolelov/" + pdfName;

                Path pdfFileSource = new File("/home/baj/Projects/meloar-transform/folkeskole/data/skolelove/pdf", pdfName).toPath();
                Path pdfFileDest = new File(item_directory, pdfName).toPath();
                try {
                    Files.copy(pdfFileSource, pdfFileDest, REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //write the file name to the contents file
                contentsFileWriter.write(pdfName);

                //write the url to the dublin core file
                addElement("relation", "uri", pdfUrl, dcdoc, dcroot);
            }
        }

        //internt link (kolonne 8) -> virker ikke (nyt bibliotekssystem), så den springer vi over
        //eksternt link (kolonne 9) -> ser ud til at de virker -> related
        if (item.length>9 && !item[9].equals("")) {
            addElement("relation", "references", item[9], dcdoc, dcroot);
        }

        //content (kolonne 10) giver ikke rigtig mening ud af kontekst
        //note (kolonne 11) -> giver nogen gange mening...
        if (item.length > 11 && !item[11].equals("")) {
            addElement("description", "notes", item[11], dcdoc, dcroot);
        }
        //resten af kolonnerne giver heller ikke mening uden for kontekst
        //vi mangler desciption
        //    addElement("description", null, item[4], dcdoc, dcroot);
        //publisher er ikke "Royal Danish Library", men nok nærmere "Aarhus University Library", men det er
        //måske det samme?
        addElement("publisher", null, "Aarhus University Library", dcdoc, dcroot);
        //vi mangler en forfatter
        //addElement("contributor", "author", item, dcdoc, dcroot);

        //keywords
        addElement("subject", null, "folkeskole", dcdoc, dcroot);

        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(dcdoc);
        StreamResult streamResult = new StreamResult(new File(item_directory,"dublin_core.xml"));
        transformer.transform(source, streamResult);

        // Output to console for testing
        //StreamResult consoleResult = new StreamResult(System.out);
        //transformer.transform(source, consoleResult);
        //System.out.println("\n");

        //XMLUtil.write(doc, System.out, "UTF8");
        //System.out.println(doc.toString());

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
