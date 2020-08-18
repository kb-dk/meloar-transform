package dk.statsbiblioteket.mediestream.loar.BL;

import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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
import java.util.List;

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
        log.debug("CSV read.");
        String[] head = csv.get(0);
        int shelfmarkIndex = -1;
        for (int index = 0; index < head.length; index++) {
            if (head[index].trim().equals("BL shelfmark")) {
                shelfmarkIndex = index;
                break;
            }
        }
        log.debug("shelfmarkIndex = "+shelfmarkIndex);
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
                    log.debug("file.getName() = "+file.getName());
                    try {
                        DocumentBuilder builder = factory.newDocumentBuilder();
                        Document inputdoc = builder.parse(file);
                        Element root = inputdoc.getDocumentElement();
                        NodeList marcSubfields =root.getElementsByTagName("MARC:subfield");
                        log.debug("marcSubfields.getLength() = "+marcSubfields.getLength());
                        String blShelfmark = null;
                        for (int i = 0; i < marcSubfields.getLength(); i++) {
                            Node node = marcSubfields.item(i);
                            if (node.getAttributes().getNamedItem("code").getNodeValue().equals("j")) {
                                blShelfmark = node.getTextContent().trim();
                                log.debug("blShelfmark = "+blShelfmark);
                            }
                        }
                        if (blShelfmark.endsWith(".")) {
                            blShelfmark = blShelfmark.substring(0, blShelfmark.length()-1);
                        }
                        if (blShelfmark.contains("(")) {
                            int index = blShelfmark.indexOf("(");
                            blShelfmark = blShelfmark.substring(0, index) + " " + blShelfmark.substring(index);
                        }
                        //now find the BL Shelfmark in the CSV file
                        for (String[] line: csv) {
                            //log.debug(line[shelfmarkIndex]);
                            String csvBlShelfmark = line[shelfmarkIndex];
                            String[] csvBlShelfmarkArray = null;
                            if (csvBlShelfmark.contains(";")) {
                                csvBlShelfmarkArray = csvBlShelfmark.split(";");
                            } else {
                                csvBlShelfmarkArray = new String[]{csvBlShelfmark};
                            }
                            if (csvBlShelfmarkArray!=null) {
                                for (int i = 0; i < csvBlShelfmarkArray.length; i++) {
                                    String csvBlSh = csvBlShelfmarkArray[i];
                                    if (csvBlSh.contains("Digital Store ")) {
                                        csvBlSh = csvBlSh.trim().substring("Digital Store ".length());
                                        if (csvBlSh.equals(blShelfmark)) {
                                            writeItemAsSAF(line, dataDirectory, file.getName(), outputdirectory);
                                        }
                                    }
                                }
                            }
                            if (csvBlShelfmark.equals(blShelfmark)) {
                                writeItemAsSAF(line, dataDirectory, file.getName(), outputdirectory);
                            }
                        }
                    } catch (ParserConfigurationException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (SAXException e) {
                        e.printStackTrace();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    } catch (TransformerException e) {
                        e.printStackTrace();
                    } catch (URISyntaxException e) {
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

        log.debug("Entering writeItemAsSAF with xmlFileName = "+xmlFileName);
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

        //We copy the data to this directory and update the contents file
        //todo right now it's just the xml file along with the zip files - is that what we want?
        String fileNameID = xmlFileName.substring(0, 11);
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
                contentsFileWriter.write(fileName+"\n");
                log.debug("fileName = "+fileName);
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
        String contributors = item[12];
        String[] contributorArray = contributors.split(";");
        for (int index = 0; index < contributorArray.length; index++) {
            String contributor = contributorArray[index];
            if (contributor.contains("[")) {
                contributor = contributor.split("\\[")[0];
            }
            if (!contributor.trim().equals(item[8].trim())) {
                addElement("contributor", null, contributor, dcdoc, dcroot);
            }
        }

        //Title (kolonne 13) -> title
        String title = item[13];
        addElement("title", null, title, dcdoc, dcroot);

        //Uniform Title (kolonne 14) -> title
        String utitle = item[14];
        if (utitle!=null && !utitle.equals("") && !utitle.equals(title)) {
            addElement("title", "alternative", utitle, dcdoc, dcroot);
        }

        //Variant Titles (kolonne 15) -> title
        String vtitle = item[15];
        if (vtitle!=null && !vtitle.equals("") && !vtitle.equals(title) && !vtitle.equals(utitle)) {
            addElement("title", "alternative", vtitle, dcdoc, dcroot);
        }

        //Series title (kolonne 16) -> relation.ispartofseries
        if (!item[16].equals("")) {
            addElement("relation", "ispartofseries", item[16], dcdoc, dcroot);
        }

        //todo Number within series (kolonne 17) -> ???

        //Country of publication (kolonne 18) -> coverage.spatial
        String countries = item[18];
        String[] countryArray = countries.split(";");
        for (int i = 0; i < countryArray.length; i++) {
            addElement("coverage", "spatial", countryArray[i], dcdoc, dcroot);
        }

        //Place of publication (kolonne 19) -> coverage.spatial
        String places = item[19];
        String[] placeArray = places.split(";");
        for (int i = 0; i < placeArray.length; i++) {
            addElement("coverage", "spatial", placeArray[i], dcdoc, dcroot);
        }


        //Publisher (kolonne 20) -> publisher
        String publishers = item[20];
        String[] publisherArray = publishers.split(";");
        for (int i = 0; i < publisherArray.length; i++) {
            addElement("coverage", "spatial", publisherArray[i], dcdoc, dcroot);
        }

        //Date of publication (standardised) (kolonne 20) -> issued date
        //I am not sure about required format for dates...
        String date = item[21];
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

        //Edition (kolonne 24) -> ?
        //Physical description (kolonne 24) -> description
        if (!item[24].equals("")) {
            addElement("description", null, "Physical description: "+item[24], dcdoc, dcroot);
        }

        //BL Shelfmark (kolonne 27) -> ?
        //Topics (kolonne 28) -> subject
        String topics = item[28];
        log.debug("topics = "+topics);
        if (!topics.equals("")) {
            String[] topicList = topics.split(";");
            for (String topic: topicList) {
                addElement("subject", null, topic, dcdoc, dcroot);
            }
        }

        //Genre
        //Literary form
        //Languages (kolonne 31) -> language
        String languages = item[31];
        if (!languages.equals("")) {
            String[] languageList = languages.split(";");
            for (String language: languageList) {
                addElement("language", null, language, dcdoc, dcroot);
            }
        }

        //Notes (kolonne 34) -> description
        if (!item[33].equals("")) {
            addElement("description", null, "Notes: "+item[33], dcdoc, dcroot);
        }

        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(dcdoc);
        StreamResult streamResult = new StreamResult(new File(item_directory, "dublin_core.xml"));
        transformer.transform(source, streamResult);

        // Output to console for testing
        StreamResult consoleResult = new StreamResult(System.out);
        transformer.transform(source, consoleResult);
        System.out.println("\n");

        //XMLUtil.write(doc, System.out, "UTF8");
        //System.out.println(doc.toString());

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
        dcvalue.setTextContent(textContent);
        root.appendChild(dcvalue);
    }
}
