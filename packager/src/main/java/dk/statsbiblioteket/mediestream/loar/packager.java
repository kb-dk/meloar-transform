package dk.statsbiblioteket.mediestream.loar;

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
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

public class packager {

    public static void translateFile(String inputfile, String outputdirectory) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            File file = new File(inputfile);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document inputdoc = builder.parse(file);

            //Vi skal finde <digitalDocumentation> med <documentType> <term>Beretning</term>
            NodeList digitalDocumentationList = inputdoc.getElementsByTagName("digitalDocumentation");
            System.out.println(digitalDocumentationList.getLength());
            //We need a directory for each Beretning
            for (int i = 0; i < digitalDocumentationList.getLength(); i++) {
                Element digitalDocumentation = (Element) digitalDocumentationList.item(i);
                NodeList documentTypeList = digitalDocumentation.getElementsByTagName("documentType");
                if (documentTypeList.getLength()>0) {
                    Element documentType = (Element) documentTypeList.item(0);
                    NodeList termList = documentType.getElementsByTagName("term");
                    if (termList.getLength()>0) {
                        Node term = termList.item(0);
                        System.out.println("term.getTextContent()"+term.getTextContent());
                        if (term.getTextContent()!=null && term.getTextContent().equalsIgnoreCase("Beretning")) {
                            File item_directory = new File(outputdirectory, "item_"+i);
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
                                String link = tmpList.item(0).getTextContent();
                                if (link!=null && !link.equals("")) {
                                    URL url = new URL(link);
                                    ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
                                    FileOutputStream fileOutputStream = new FileOutputStream(new File(item_directory, pdfFileName));
                                    FileChannel fileChannel = fileOutputStream.getChannel();
                                    fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                                }
                            }
                            // todo and the xml for the parent record
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
                                            StreamResult consoleResult = new StreamResult(System.out);
                                            transformer.transform(source, consoleResult);
                                            System.out.println("\n");

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
                            fileWriter.write(pdfFileName + "\tpermissions:-[r|w] 'Administrator'\n");
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

                            //title
                            tmpList = digitalDocumentation.getElementsByTagName("name");
                            if (tmpList.getLength()>0) {
                                String name = tmpList.item(0).getTextContent();
                                if (name!=null && !name.equals("")) {
                                    addElement("title", null, name, outputdoc, root);
                                }// else todo alternativ titel
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
                            //todo link: download and write filename to contents
                            tmpList = digitalDocumentation.getElementsByTagName("link");
                            if (tmpList.getLength()>0) {
                                String link = tmpList.item(0).getTextContent();
                                if (link!=null && !link.equals("")) {
                                    addElement("relation", "uri", link, outputdoc, root);
                                }
                            }

                            //contributors
                            NodeList persons = digitalDocumentation.getElementsByTagName("ff:persons");
                            if (persons!=null && persons.getLength()>0) {
                                NodeList personList = persons.item(0).getChildNodes();
                                for (int j = 0; j < personList.getLength(); j++) {
                                    Node person = personList.item(j);
                                    NodeList person_child_node_list = person.getChildNodes();
                                    for (int k = 0; k < person_child_node_list.getLength(); k++) {
                                        Node child_node = person_child_node_list.item(k);
                                        if (child_node.getNodeName().equalsIgnoreCase("term")) {
                                            String person_term = child_node.getTextContent();
                                            addElement("contributor", null, person_term, outputdoc, root);
                                        }
                                    }
                                }
                            }
                            // todo author

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
            e.printStackTrace();
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
}
