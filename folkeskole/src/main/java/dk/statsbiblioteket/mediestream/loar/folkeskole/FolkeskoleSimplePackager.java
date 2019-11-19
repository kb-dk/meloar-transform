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
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FolkeskoleSimplePackager {

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

        //name (kolonne 0) -> title
        addElement("title", null, item[0], dcdoc, dcroot);
        //date (kolonne 1) -> issued date
        String date = item[1];
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        if (!date.equals("")) {
        if (date.matches("\\d{4}") || date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            addElement("date", "issued", date, dcdoc, dcroot);
        } else {
            LocalDate parsedDate = LocalDate.parse(item[1], formatter);
            formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            addElement("date", "issued", parsedDate.format(formatter), dcdoc, dcroot);
        }}
        //write the pdf files from the external_resource fields in this item directory
        //and in the contents file and add to dublin core document
        if (item.length>2) {
            String pdfUrl = item[2];
            if (pdfUrl != null || !pdfUrl.equals("")) {
                URL url = new URL(pdfUrl);
                URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
                url = uri.toURL();

                try {
                    url.openStream();
                } catch (FileNotFoundException e) {
                    log.error("pdf does not exist: " + pdfUrl, e);
                    e.printStackTrace();
                    item_directory.delete();
                }

                String[] splitPdfUrl = pdfUrl.split("/");
                String pdfName = splitPdfUrl[splitPdfUrl.length - 1];

                ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
                File pdfFile = new File(item_directory, pdfName);
                FileOutputStream fileOutputStream = new FileOutputStream(pdfFile);
                fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

                //write the file name to the contents file
                contentsFileWriter.write(pdfName);

                //write the url to the dublin core file
                addElement("relation", "uri", pdfUrl, dcdoc, dcroot);
            }
        }
        if (item.length > 3) {
            //note (kolonne 3) -> alternative title
            addElement("title", "alternative", item[3], dcdoc, dcroot);
        }
        if (item.length > 4) {
            //content (kolonne 4) -> description
            addElement("description", null, item[4], dcdoc, dcroot);
        }
        //publisher "Royal Danish Library"
        addElement("publisher", null, "Royal Danish Library", dcdoc, dcroot);
        //vi mangler en forfatter
        //addElement("contributor", "author", item, dcdoc, dcroot);

        //type springer vi over
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
