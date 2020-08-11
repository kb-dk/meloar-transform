package dk.statsbiblioteket.mediestream.loar.BL;

import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * The class BLdataPackager is home to the writeItemAsSAF method, which "translates" a line from the
 * "BL/data/Microsoft Books records 2019-09-19.xlsx" file detailing metadata from the BL Microsoft Digitised Books
 * project along with the corresponding data files into the DSpace Simple Archive Format, which can be ingested into a
 * DSpace archive
 * (https://wiki.duraspace.org/display/DSDOC6x/Importing+and+Exporting+Items+via+Simple+Archive+Format).
 */
public class BLdataPackager {

    private static Logger log = LoggerFactory.getLogger(dk.statsbiblioteket.mediestream.loar.BL.BLdataPackager.class);
    private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private static int count = 0;
    private static int count_text = 0;

    public static boolean matchDataToCSV(String dataDirectory, String csvFile, String outputdirectory) {
        //Read CSVfile into java
        CSVReader reader = null;
        List<String[]> csv = null;
        try {
            reader = new CSVReader(new FileReader(csvFile), ',');
            csv = reader.readAll();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] head = csv.get(0);
        int shelfmarkIndex = -1;
        for (int index = 0; index < head.length; index++) {
            if (head[index].trim().equals("BL shelfmark")) {
                shelfmarkIndex = index;
                break;
            }
        }
        if (shelfmarkIndex==-1) {
            return false;
        }

        //Read all xml files in data directory; find their BL Shelfmark; use this to find the corresponding item
        //in the csv file; then write the item as SAF
        File inputdir = new File(dataDirectory);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        if (inputdir.isDirectory()) {
            File[] files = inputdir.listFiles();
            assert files != null;
            for (File file: files) {
                if (file.getName().endsWith(".xml")) {
                    try {
                        DocumentBuilder builder = factory.newDocumentBuilder();
                        Document inputdoc = builder.parse(file);
                        // find <MARC:subfield code="j">
                        NodeList marcSubfields = inputdoc.getElementsByTagNameNS("MARC", "subfield");
                        Node field;
                        for (int i = 0; i < marcSubfields.getLength(); i++) {
                            field = marcSubfields.item(i);
                            NamedNodeMap atts = field.getAttributes();
                            Node code = atts.getNamedItem("code");
                            if (code!=null && code.getTextContent().equals("j")) {
                                String blShelfmark = field.getTextContent().trim();
                                //now find the BL Shelfmark in the CSV file
                                for (String[] line: csv) {
                                    if (line[shelfmarkIndex].contains(blShelfmark)) {
                                        writeItemAsSAF(line, dataDirectory, file.getName(), outputdirectory);
                                    }
                                }
                            }
                        }

                    } catch (ParserConfigurationException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (SAXException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return true;
    }



    /**
     * Write a LOAR DSpace Simple Archive Format structure based on the given line.
     * @param item String array with metadata for 1 item
     * @param outputdirectory String directory name where to put structure
     */
    public static boolean writeItemAsSAF(String[] item, String dataDirectory, String xmlFileName, String outputdirectory)
            throws ParserConfigurationException, IOException, TransformerException, ParseException, URISyntaxException {

        if (true) {
            //System.out.println(count);

            //First we need a directory for this item
            File item_directory = new File(outputdirectory, "item" + count);
            item_directory.mkdir();
            count++;

            //The contents file simply enumerates, one file per line, the bitstream file names
            //The bitstream name may optionally be followed by \tpermissions:PERMISSIONS
            File contents = new File(item_directory, "contents");
            contents.createNewFile();
            FileWriter contentsFileWriter = new FileWriter(contents);

            //We copy the data to this directory and update the contents file
            //todo right now it's just the xml file along with the zip files - is that what we want?
            String fileNameID = xmlFileName.substring(0, 9);
            File[] files = new File(dataDirectory).listFiles();
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                String fileName = file.getName();
                if (fileName.startsWith(fileNameID) && (fileName.endsWith(".xml") || fileName.endsWith(".zip"))) {
                    Path fileSource = file.toPath();
                    Path fileDest = new File(item_directory, fileName).toPath();
                    try {
                        Files.copy(fileSource, fileDest, REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //write the file name to the contents file
                    contentsFileWriter.write(fileName);
                }
            }

            //The dublin_core.xml file contains some of the metadata as dublin core
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document dcdoc = builder.newDocument();
            Element dcroot = dcdoc.createElement("dublin_core");
            dcroot.setAttribute("schema", "dc");
            dcdoc.appendChild(dcroot);

            //BL record ID (kolonne 0) -> identifier
            //may not be relevant
            addElement("identifier", "other", "BL record ID: " + item[0], dcdoc, dcroot);

            //ARK (kolonne 1) -> identifier
            addElement("identifier", "other", item[1], dcdoc, dcroot);

            //URL (kolonne 2) -> relation:uri
            addElement("relation", "uri", item[2], dcdoc, dcroot);

            //Type of resource (kolonne 3) -> type
            addElement("type", null, item[3], dcdoc, dcroot);

            //Content type (kolonne 4) -> type
            addElement("type", null, item[4], dcdoc, dcroot);

            //Material type (kolonne 5) -> type
            addElement("type", null, item[5], dcdoc, dcroot);

            //Name (kolonne 8) -> author
            addElement("contributor", "author", item[8],dcdoc, dcroot);

            //All names (kolonne 12) -> contributors
            //Den kræver lige noget parsing...

            //Title (kolonne 13) -> title
            addElement("title", null, item[13], dcdoc, dcroot);

            
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
            if (item.length > 5 && !item[5].equals("")) {
                addElement("identifier", "other", "regulation_no: " + item[5], dcdoc, dcroot);
            }
            //educationtype (kolonne 6) -> ?? det må være en type
            if (item.length > 6 && !item[6].equals("")) {
                addElement("type", null, item[6], dcdoc, dcroot);
            }

            //write the pdf files from the "pdf_version" (kolonne 7) fields in this item directory
            //and in the contents file
            //and add to dublin core document preceded by "https://library.au.dk/uploads/tx_lfskolelov/"
            if (item.length > 7) {
                String pdfName = item[7];
                if (pdfName != null && !pdfName.equals("")) {

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
                    //addElement("relation", "uri", pdfUrl, dcdoc, dcroot);
                    //not necessary
                }
            }
            //internt link (kolonne 8) -> virker ikke (nyt bibliotekssystem), så den springer vi over
            //eksternt link (kolonne 9) -> ser ud til at de virker -> related
            if (item.length > 9 && !item[9].equals("")) {
                addElement("relation", "uri", item[9], dcdoc, dcroot);
            }

            //content (kolonne 10) giver ikke rigtig mening ud af kontekst
            //note (kolonne 11) -> giver nogen gange mening...
            if (item.length > 11 && !item[11].equals("")) {
                addElement("description", null, item[11], dcdoc, dcroot);
            }
            //resten af kolonnerne giver heller ikke mening uden for kontekst
            //vi mangler description
            //    addElement("description", null, item[4], dcdoc, dcroot);
            //publisher er ikke "Royal Danish Library", men nok nærmere "Aarhus University Library", men det er
            //måske det samme?
            addElement("publisher", null, "Aarhus University Library", dcdoc, dcroot);
            //vi mangler en forfatter
            //addElement("contributor", "author", item, dcdoc, dcroot);

            //keywords
            addElement("subject", null, "skolelove", dcdoc, dcroot);

            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(dcdoc);
            StreamResult streamResult = new StreamResult(new File(item_directory, "dublin_core.xml"));
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
            return true;
        }
        else {
            //det er dem her der ikke er kommet med
            System.out.println(count);
            System.out.println(Arrays.deepToString(item));
            return false;
        }
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
