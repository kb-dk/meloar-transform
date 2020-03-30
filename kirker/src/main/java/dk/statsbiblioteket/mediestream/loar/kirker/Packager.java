package dk.statsbiblioteket.mediestream.loar.kirker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
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

/**
 * The class Packager is home to the translateFile method, which "translates" oai-harvested xml
 * from "http://danmarkskirker.natmus.dk/"
 * into the DSpace Simple Archive Format, which can be ingested into a DSpace archive
 * (https://wiki.duraspace.org/display/DSDOC6x/Importing+and+Exporting+Items+via+Simple+Archive+Format).
 */
public class Packager {
    private static Logger log = LoggerFactory.getLogger(Packager.class);

    /**
     * Translate xml file into item in simple archive format
     * to be ingested into LOAR. The method reads the xml in the input file, downloads
     * the corresponding pdf's, creates a corresponding Dublin Core metadata file and a contents file, and
     * writes all files to a new item directory in the output directory.
     * @param inputfile xml from "http://danmarkskirker.natmus.dk/"
     * @param outputdirectory the output directory for items in DSpace Simple Archive Format
     * @param seed the seed ensures that item directory names are unique
     */
    public static void translateFile(String inputfile, String outputdirectory, int seed) {
        log.debug("entering translateFile method with parameters: ("+inputfile+", "+outputdirectory+", "+seed+")");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            File input = new File(inputfile);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document inputdoc = builder.parse(input);

            //We need a directory for this item
            File item_directory = new File(outputdirectory, "item_"+seed);
            item_directory.mkdir();

            //The contents file simply enumerates, one file per line, the bitstream file names
            //The bitstream name may optionally be followed by \tpermissions:PERMISSIONS
            File contents = new File(item_directory, "contents");
            contents.createNewFile();
            FileWriter contentsFileWriter = new FileWriter(contents);

            //The dublin_core.xml file contains some of the metadata as dublin core
            builder = factory.newDocumentBuilder();
            Document dcdoc = builder.newDocument();
            Element dcroot = dcdoc.createElement("dublin_core");
            dcroot.setAttribute("schema", "dc");
            dcdoc.appendChild(dcroot);

            NodeList fields = inputdoc.getElementsByTagName("field");
            System.out.println("fields.getLength() = "+fields.getLength());
            for (int i = 0; i<fields.getLength(); i++) {
                Node node = fields.item(i);
                NamedNodeMap attributes = node.getAttributes();
                Node attribute = attributes.item(0);
                System.out.println(attribute.getNodeValue());
                switch (attribute.getNodeValue())
                {
                    case "title": //add title to dublin core document
                        String title = node.getTextContent();
                        if (title!=null && !title.equals("")) {
                            addElement("title", null, title, dcdoc, dcroot);
                        }// else todo alternativ titel
                        break;
                    case "id": //add id to dublin core document
                        String id = node.getTextContent();
                        if (id!=null && !id.equals("")) {
                            addElement("identifier", "other", id, dcdoc, dcroot);
                        }
                        break;
                    case "external_resource":
                        //write the pdf files from the external_resource fields in this item directory
                        //and in the contents file and add to dublin core document
                        String pdfUrl = node.getTextContent();
                        if (pdfUrl != null || !pdfUrl.equals("")) {
                            URL url = new URL(pdfUrl);
                            try {
                                url.openStream();
                            } catch (FileNotFoundException e) {
                                log.error("pdf does not exist: " + pdfUrl, e);
                                e.printStackTrace();
                                item_directory.delete();
                                continue;
                            }

                            String [] splitPdfUrl = pdfUrl.split("/");
                            String pdfName = splitPdfUrl[splitPdfUrl.length-1];

                            ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
                            File pdfFile = new File(item_directory, pdfName);
                            FileOutputStream fileOutputStream = new FileOutputStream(pdfFile);
                            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

                            //write the file name to the contents file
                            contentsFileWriter.write(pdfName + "\tpermissions:-r 'Administrator'\n");

                            //write the url to the dublin core file
                            addElement("relation", "uri", pdfUrl, dcdoc, dcroot);
                        }
                        break;
                    case "place_name": //add place name to dublin core document
                        String place_name = node.getTextContent();
                        if (place_name!=null && !place_name.equals("")) {
                            addElement("coverage", "spatial", place_name, dcdoc, dcroot);
                        }
                        break;
                    case "place_coordinates": //add place coordinates to dublin core document
                        String place_coordinates = node.getTextContent();
                        if (place_coordinates!=null && !place_coordinates.equals("")) {
                            addElement("coverage", "spatial", place_coordinates, dcdoc, dcroot);
                        }
                        break;
                    case "volume_ss": //add volumes to dublin core document
                        String volume = node.getTextContent();
                        if (volume!=null && !volume.equals("")) {
                            addElement("relation", "ispartof", volume, dcdoc, dcroot);
                        }
                        break;
                    case "date_issued_is": //add date issued to dublin core document
                        String date = node.getTextContent();
                        if (date!=null && !date.equals("")) {
                            addElement("date", "issued", date, dcdoc, dcroot);
                        }
                        break;
                    case "author": //add author to DC
                        String author = node.getTextContent();
                        if (author!=null && !author.equals("")) {
                            addElement("contributor", "author", author, dcdoc, dcroot);
                        }
                }
            }

            //add publisher to DC
            addElement("publisher", null, "Nationalmuseet", dcdoc, dcroot);

            // todo write xml file?
            // todo and maybe the license...
            //contentsFileWriter.write(licensefileName + "\n");
            contentsFileWriter.flush();
            contentsFileWriter.close();

            // clean up the dublin core
            dcdoc.normalizeDocument();
            // write the dublin core into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty("indent", "yes");
            DOMSource source = new DOMSource(dcdoc);
            StreamResult streamResult = new StreamResult(new File(item_directory,"dublin_core.xml"));
            transformer.transform(source, streamResult);

            // Output to console for testing
            StreamResult consoleResult = new StreamResult(System.out);
            transformer.transform(source, consoleResult);
            System.out.println("\n");



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
        if (root.hasChildNodes()) {
            NodeList nodelist = root.getChildNodes();
            for (int i = 0; i < nodelist.getLength(); i++) {
                Node node = nodelist.item(i);
                if (node.isEqualNode(dcvalue)) {
                    return;
                }
            }
        }
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
