package dk.statsbiblioteket.mediestream.loar.fof;

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
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The class Packager is home to the translateFile method, which "translates" oai-harvested xml
 * from REPOSITORY="http://www.kulturarv.dk/ffrepox/OAIHandler" METADATA_PREFIX="ff" PROJECT="ff_slks"
 * into the DSpace Simple Archive Format, which can be ingested into a DSpace archive
 * (https://wiki.duraspace.org/display/DSDOC6x/Importing+and+Exporting+Items+via+Simple+Archive+Format).
 */
public class Packager {
    private static Logger log = LoggerFactory.getLogger(Packager.class);

    /**
     * Translate oai-harvested xml file into a number of "beretning" items in simple archive format
     * to be ingested into LOAR. The method finds the "beretning" entries in the input file, downloads
     * the corresponding pdf's, creates a corresponding Dublin Core metadata file and a contents file, and
     * writes all three files to a new item directory in the output directory.
     * @param inputfile oai-harvested xml from REPOSITORY="http://www.kulturarv.dk/ffrepox/OAIHandler"
     * @param outputdirectory the output directory for items in DSpace Simple Archive Format
     * @param seed the seed ensures that item directory names are unique
     */
    public static void translateFile(String inputfile, String outputdirectory, int seed) {
        log.debug("entering translateFile method with parameters: ("+inputfile+", "+outputdirectory+", "+seed+")");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            File file = new File(inputfile);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document inputdoc = builder.parse(file);

            //Vi skal finde <digitalDocumentation> med <documentType> <term>Beretning</term>
            NodeList digitalDocumentationList = inputdoc.getElementsByTagName("digitalDocumentation");
            System.out.println("digitalDocumentationList.getLength() = "+digitalDocumentationList.getLength());
            //We need a directory for each Beretning, iff there is a pdf document at the other end
            for (int i = 0; i < digitalDocumentationList.getLength(); i++) {
                Element digitalDocumentation = (Element) digitalDocumentationList.item(i);
                NodeList documentTypeList = digitalDocumentation.getElementsByTagName("documentType");
                String link = null;
                if (documentTypeList.getLength()>0) {
                    Element documentType = (Element) documentTypeList.item(0);
                    NodeList termList = documentType.getElementsByTagName("term");
                    if (termList.getLength()>0) {
                        Node term = termList.item(0);
                        //System.out.println("term.getTextContent()"+term.getTextContent());
                        if (term.getTextContent()!=null && term.getTextContent().equalsIgnoreCase("Beretning")) {
                            File item_directory = new File(outputdirectory, "item_"+seed+"_"+i);
                            item_directory.mkdir();

                            // write the pdf file from <link> to <filename> in this directory
                            String pdfFileName = "file_"+i+".pdf";
                            NodeList tmpList = digitalDocumentation.getElementsByTagName("filename");
                            if (tmpList.getLength()>0) {
                                String tmpFileName = tmpList.item(0).getTextContent();
                                if (tmpFileName!=null || !tmpFileName.equals("")) {
                                    pdfFileName = tmpFileName;
                                }
                            }
                            tmpList = digitalDocumentation.getElementsByTagName("link");
                            if (tmpList.getLength()>0) {
                                link = tmpList.item(0).getTextContent();
                                if (link!=null && !link.equals("")) {
                                    URL url = new URL(link);
                                    try {
                                        url.openStream();
                                    } catch (FileNotFoundException e) {
                                        log.error("pdf does not exist: "+link, e);
                                        e.printStackTrace();
                                        item_directory.delete();
                                        continue;
                                    }

                                    ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
                                    File pdfFile = new File(item_directory, pdfFileName);
                                    FileOutputStream fileOutputStream = new FileOutputStream(pdfFile);
                                    //FileChannel fileChannel = fileOutputStream.getChannel();
                                    fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                                }
                            }
                            // and the xml for the parent record
                            String recordxmlFileName = "recordxml_item_"+i+".xml";
                            boolean recordxmlexists = false;
                            Node ff_digitalDocumentations = digitalDocumentation.getParentNode();
                            if (ff_digitalDocumentations!=null) {
                                Node site = ff_digitalDocumentations.getParentNode();
                                if (site!=null) {
                                    Node metadata = site.getParentNode();
                                    if (metadata!=null) {
                                        Node record = metadata.getParentNode();
                                        if (record!=null) {
                                            builder = factory.newDocumentBuilder();
                                            Document recordxml = builder.newDocument();
                                            recordxml.adoptNode(record);
                                            recordxml.appendChild(record);

                                            // write the content into xml file
                                            TransformerFactory transformerFactory = TransformerFactory.newInstance();
                                            Transformer transformer = transformerFactory.newTransformer();
                                            transformer.setOutputProperty("indent", "yes");
                                            DOMSource source = new DOMSource(recordxml);
                                            StreamResult streamResult = new StreamResult(new File(item_directory,recordxmlFileName));
                                            transformer.transform(source, streamResult);

                                            // Output to console for testing
                                            //StreamResult consoleResult = new StreamResult(System.out);
                                            //transformer.transform(source, consoleResult);
                                            //System.out.println("\n");

                                            recordxmlexists=true;
                                        }
                                    }
                                }
                            }

                            // todo and maybe the license...

                            //The contents file simply enumerates, one file per line, the bitstream file names
                            //The bitstream name may optionally be followed by \tpermissions:PERMISSIONS
                            File contents = new File(item_directory, "contents");
                            contents.createNewFile();
                            FileWriter fileWriter = new FileWriter(contents);
                            fileWriter.write(pdfFileName + "\tpermissions:-r 'Administrator'\n");
                            if (recordxmlexists) {fileWriter.write(recordxmlFileName);}
                            //fileWriter.write(licensefileName + "\n");
                            fileWriter.flush();
                            fileWriter.close();

                            //The dublin_core.xml file contains some of the metadata as dublin core
                            builder = factory.newDocumentBuilder();
                            Document outputdoc = builder.newDocument();
                            Element root = outputdoc.createElement("dublin_core");
                            root.setAttribute("schema", "dc");
                            outputdoc.appendChild(root);
                            //todo skal id med i top-level metadata, når det egentlig er id på filen?

                            //title (mandatory when requesting a DOI)
                            tmpList = digitalDocumentation.getElementsByTagName("name");
                            if (tmpList.getLength()>0) {
                                String name = tmpList.item(0).getTextContent();
                                if (name!=null && !name.equals("")) {
                                    addElement("title", null, name, outputdoc, root);
                                } else {
                                    if (link!=null) {
                                        addElement("title", null, link, outputdoc, root);
                                    }
                                }
                            }
                            //date
                            tmpList = digitalDocumentation.getElementsByTagName("date");
                            if (tmpList.getLength()>0) {
                                String date = tmpList.item(0).getTextContent();
                                if (date!=null && !date.equals("")) {
                                    addElement("date", "issued", date, outputdoc, root);
                                }
                            }

                            //copyright
                            tmpList = digitalDocumentation.getElementsByTagName("copyright");
                            if (tmpList.getLength()>0) {
                                String copyright = tmpList.item(0).getTextContent();
                                if (copyright!=null && !copyright.equals("")) {
                                    addElement("rights", "holder", copyright, outputdoc, root);
                                }
                            }

                            //legalUse
                            tmpList = digitalDocumentation.getElementsByTagName("legalUse");
                            if (tmpList.getLength()>0) {
                                String legaluse = tmpList.item(0).getTextContent();
                                if (legaluse!=null && !legaluse.equals("")) {
                                    addElement("rights", null, legaluse, outputdoc, root);
                                }
                            }
                            //description
                            tmpList = digitalDocumentation.getElementsByTagName("description");
                            if (tmpList.getLength()>0) {
                                String description = tmpList.item(0).getTextContent();
                                if (description!=null && !description.equals("")) {
                                    addElement("description", null, description, outputdoc, root);
                                }
                            }
                            //link: download and write filename to contents
                            if (link!=null) {
                                addElement("relation", "uri", link, outputdoc, root);
                            }


                            //contributors are all listed as authors
                            //author is mandatory when requesting a DOI
                            //sometimes they can be found in the "beretning" metadata
                            Set<String> contributors = new LinkedHashSet<String>();
                            extractPersons(contributors, digitalDocumentation);
                            if (contributors.isEmpty()) {
                                //sometimes we can find contributors in surrounding metadata
                                if (ff_digitalDocumentations != null) {
                                    Node site = ff_digitalDocumentations.getParentNode();
                                    if (site != null) {
                                        Node metadata = site.getParentNode();
                                        if (metadata != null) {
                                            Element record = (Element) metadata.getParentNode();
                                            if (record != null) {
                                                extractPersons(contributors, record);
                                            }
                                        }
                                    }
                                }
                            }
                            if (contributors.isEmpty()) {
                                //otherwise we will use an institution as author
                                if (ff_digitalDocumentations != null) {
                                    Node site = ff_digitalDocumentations.getParentNode();
                                    if (site != null) {
                                        Node metadata = site.getParentNode();
                                        if (metadata != null) {
                                            Element record = (Element) metadata.getParentNode();
                                            if (record != null) {
                                                extractInstitutions(contributors, record);
                                            }
                                        }
                                    }
                                }
                            }
                            for (String contributor: contributors) {
                                addElement("contributor", "author", contributor, outputdoc, root);
                            }

                            // write the content into xml file
                            TransformerFactory transformerFactory = TransformerFactory.newInstance();
                            Transformer transformer = transformerFactory.newTransformer();
                            transformer.setOutputProperty("indent", "yes");
                            DOMSource source = new DOMSource(outputdoc);
                            StreamResult streamResult = new StreamResult(new File(item_directory,"dublin_core.xml"));
                            transformer.transform(source, streamResult);

                            // Output to console for testing
                            StreamResult consoleResult = new StreamResult(System.out);
                            transformer.transform(source, consoleResult);
                            System.out.println("\n");



                        }
                    }
                }
            }


        } catch (ParserConfigurationException e) {
            log.error("ParserConfigurationException", e);
            e.printStackTrace();
            return;
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    private static void extractInstitutions(Set<String> contributors, Element top) {
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
                        contributors.add(institution_term.trim());
                        //addElement("contributor", "author", institution_term, outputdoc, root);
                    }
                }
            }
        }

    }

    private static void extractPersons(Set<String> contributors, Element top) {
        System.out.println("extractPersons");
        NodeList persons = top.getElementsByTagName("ff:persons");
        System.out.println("persons.getLength():"+persons.getLength());
        if (persons!=null) {
            for (int i = 0; i<persons.getLength(); i++) {
                NodeList personList = persons.item(i).getChildNodes();
                for (int j = 0; j < personList.getLength(); j++) {
                    Node person = personList.item(j);
                    NodeList person_child_node_list = person.getChildNodes();
                    for (int k = 0; k < person_child_node_list.getLength(); k++) {
                        Node child_node = person_child_node_list.item(k);
                        if (child_node.getNodeName().equalsIgnoreCase("term")) {
                            String person_term = child_node.getTextContent();
                            contributors.add(person_term.trim());
                            //addElement("contributor", "author", person_term, outputdoc, root);
                        }
                    }
                }
            }
        }
    }

    /**
     *
     * @param element
     * @param qualifier
     * @param textContent
     * @param doc
     * @param root
     * @return
     */
    private static void addElement(String element, String qualifier, String textContent, Document doc, Element root) {
        Element dcvalue = doc.createElement("dcvalue");
        dcvalue.setAttribute("element", element);//no qualifier
        if (qualifier!=null) {
            dcvalue.setAttribute("qualifier", qualifier);
        }
        dcvalue.setAttribute("language", "da_DK");//no qualifier
        dcvalue.setTextContent(textContent);
        root.appendChild(dcvalue);
    }

    /**
     * TODO
     * @param Args
     */
    public static void main(String[] Args) {
        File inputdir = new File(Args[0]);
        String outputdir = Args[1];
        if (inputdir.isDirectory()) {
            String[] inputfiles = inputdir.list();
            System.out.println("inputfiles.length = "+inputfiles.length);
            for (int seed = 0; seed<inputfiles.length; seed++) {
                String file = inputfiles[seed];
                System.out.println("processing file "+file);
                translateFile(inputdir.getAbsolutePath()+"/"+file, outputdir, seed);
                System.out.println("finished file "+file);
            }
        }

    }
}
