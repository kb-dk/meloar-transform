package dk.statsbiblioteket.mediestream.loar.fof;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
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
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The class Updater is home to the updateCSVfromOriginalCSV method, which updates the metadata csv file
 * from collection "Beretningsarkiv for Arkæologiske Undersøgelser"=https://loar.kb.dk/handle/1902/333"
 * The collection can then be updated by importing the csv.
 */
public class Updater {
    private static Logger log = LoggerFactory.getLogger(Updater.class);

    /**
     * Update the metadata csv inputfile harvested from loar
     * using the original xml harvested from REPOSITORY="http://www.kulturarv.dk/ffrepox/OAIHandler"
     * Write the updated csv to outputdirectory.
     *
     * The fof items needed an update to make sure they all have an author, such that they can all get a DOI!
     *
     * @param csvfile csv metadata file harvested from loar
     * @param xmlfile xml metadata file harvested from REPOSITORY="http://www.kulturarv.dk/ffrepox/OAIHandler"
     * @param outputdirectory the output directory for the updated csv metadata file
     */
    public static void updateCSVfromOriginalCSV(String csvfile, String xmlfile, String outputdirectory) {
        CSVReader reader = null;
        Map<String, String[]> metadataMap= new HashMap<String, String[]>();
        Integer lineNumber = 0;
        String[] line;
        String[] headings_line = null;
        try {
            reader = new CSVReader(new FileReader(csvfile), ',');
            // the first line is the headings line
            headings_line = reader.readNext();
            lineNumber++;
            // transform csv file to java map using the "dc.relation.uri[da_DK]" as key,
            // but remembering the line numbers for writing the updated map to a scv file.
            // The "dc.relation.uri[da_DK]" is in coloumn J, which is entry
            // number 9 in the String Array line, when counting from 0.
            while ((line = reader.readNext()) != null) {
                lineNumber++;
                if (!line[9].isEmpty()) {
                    String key = line[9];
                    line[9] = lineNumber.toString();
                    metadataMap.put(key, line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Now we read the original xml and update the map
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            File file = new File(xmlfile);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document inputdoc = builder.parse(file);

            //Vi skal finde <digitalDocumentation> med <documentType> <term>Beretning</term>
            NodeList digitalDocumentationList = inputdoc.getElementsByTagName("digitalDocumentation");
            for (int i = 0; i < digitalDocumentationList.getLength(); i++) {
                Element digitalDocumentation = (Element) digitalDocumentationList.item(i);
                Node ff_digitalDocumentations = digitalDocumentation.getParentNode();
                NodeList documentTypeList = digitalDocumentation.getElementsByTagName("documentType");
                String link = null;
                if (documentTypeList.getLength() > 0) {
                    Element documentType = (Element) documentTypeList.item(0);
                    NodeList termList = documentType.getElementsByTagName("term");
                    if (termList.getLength() > 0) {
                        Node term = termList.item(0);
                        if (term.getTextContent() != null &&
                                term.getTextContent().equalsIgnoreCase("Beretning")) {
                            // Nu skal vi finde xml elementet "link", som svarer til
                            // "dc.relation.uri[da_DK]" feltet i csv-filen
                            NodeList tmpList = digitalDocumentation.getElementsByTagName("link");
                            if (tmpList.getLength() > 0) {
                                link = tmpList.item(0).getTextContent();
                                if (link != null && !link.equals("") && metadataMap.containsKey(link)) {
                                    // så kan vi slå op i vores map
                                    line = metadataMap.get(link);
                                    // så skal vi finde forfattere til at opdatere med

                                    //contributors are all listed as authors
                                    //author is mandatory when requesting a DOI
                                    //sometimes they can be found in the "beretning" metadata
                                    //så er de allerede i "dc.contributor[da_DK]" i kolonne C
                                    //som er line[2] ellers
                                    if (line[2].equals("")) {
                                        //sometimes we can find contributors in surrounding metadata
                                        if (ff_digitalDocumentations != null) {
                                            Node site = ff_digitalDocumentations.getParentNode();
                                            if (site != null) {
                                                Node metadata = site.getParentNode();
                                                if (metadata != null) {
                                                    Element record = (Element) metadata.getParentNode();
                                                    if (record != null) {
                                                        line[2] = extractPersons(line[2], record);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (line[2].equals("")) {
                                        //otherwise we will use an institution as author
                                        if (ff_digitalDocumentations != null) {
                                            Node site = ff_digitalDocumentations.getParentNode();
                                            if (site != null) {
                                                Node metadata = site.getParentNode();
                                                if (metadata != null) {
                                                    Element record = (Element) metadata.getParentNode();
                                                    if (record != null) {
                                                        line[2] = extractInstitutions(line[2], record);
                                                    }
                                                }
                                            }
                                        }
                                    }


                                }
                            }
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

        //Now write the metadata map back to a csv file
        //We use a map with lineNumber keys as intermediate format
        Map<Integer, String[]> intermediateMap = new HashMap<Integer, String[]>();
        for (Map.Entry<String, String[]> entry:metadataMap.entrySet()) {
            line = entry.getValue();
            lineNumber = Integer.parseInt(line[9]);
            line[9] = entry.getKey();
            intermediateMap.put(lineNumber, line);
        }
        File newCSVMetadataFile = new File(outputdirectory + "newMetadataFile.csv");
        CSVWriter writer = null;
        try {
            writer = new CSVWriter(new FileWriter(newCSVMetadataFile), ',');
            writer.writeNext(headings_line);
            for (int i = 0; i < intermediateMap.size(); i++) {
                writer.writeNext(intermediateMap.get(i));
            }
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static String extractInstitutions(String contributors, Element top) {
        System.out.println("extractInstitutions");
        NodeList institutions = top.getElementsByTagName("institution");
        System.out.println("institutions.getLength()"+institutions.getLength());
        if (institutions!=null) {
            for (int j = 0; j < institutions.getLength(); j++) {
                Node institution = institutions.item(j);
                NodeList institution_child_node_list = institution.getChildNodes();
                for (int k = 0; k < institution_child_node_list.getLength(); k++) {
                    Node child_node = institution_child_node_list.item(k);
                    if (child_node.getNodeName().equalsIgnoreCase("term")) {
                        String institution_term = child_node.getTextContent();
                        if (!contributors.contains(institution_term.trim())) {
                            if (!contributors.equals("")) {
                                contributors += "||";
                            }
                            contributors += (institution_term.trim());
                        }
                    }
                }
            }
        }
        return contributors;
    }
    private static String extractPersons(String contributors, Element top) {
        System.out.println("extractPersons");
        NodeList persons = top.getElementsByTagName("ff:persons");
        System.out.println("persons.getLength():"+persons.getLength());
        for (int i = 0; i<persons.getLength(); i++) {
            NodeList personList = persons.item(i).getChildNodes();
            for (int j = 0; j < personList.getLength(); j++) {
                Node person = personList.item(j);
                NodeList person_child_node_list = person.getChildNodes();
                for (int k = 0; k < person_child_node_list.getLength(); k++) {
                    Node child_node = person_child_node_list.item(k);
                    if (child_node.getNodeName().equalsIgnoreCase("term")) {
                        String person_term = child_node.getTextContent();
                        if (!contributors.contains(person_term.trim())) {
                            if (!contributors.equals("")) {
                                contributors += "||";
                            }
                            contributors += person_term.trim();
                        }
                    }
                }
            }
        }
        return contributors;
    }
}
