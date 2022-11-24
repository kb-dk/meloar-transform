package dk.statsbiblioteket.mediestream.loar.veda;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The VEDA data consist of approx 500 wav files, totalling 157,9 GB
 * The veda_opdeling_i_værker.xlsx file matches the files to titles.
 * Each title corresponds to an item including the files that map to this title.
 */
public class VedaPackager {
    private static Logger log = LoggerFactory.getLogger(VedaPackager.class);
    private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private static int count = 0;
    private static String attribute_language_da = "da";
    private static String attribute_language_en_US = "en_US";

    /**
     * Write a LOAR DSpace Simple Archive Format structure based on the given line.
     *
     * @param datadirectory   String directoryname name where to read data
     * @param outputdirectory String directory name where to put structure
     */
    public static boolean writeItemAsSAF(String datadirectory, String[] line, LinkedList<String> records, String outputdirectory) throws IOException,
            ParserConfigurationException, TransformerException {
        log.info("entering writeItemAsSAF method with parameters: (" + datadirectory + ", " + Arrays.toString(line) + "," + outputdirectory + ")");

        // Only do something if you have the files
        LinkedList<File> wavFiles = new LinkedList<>();
        for (String filename: records) {
            File wavFile = new File(datadirectory, filename);
            wavFiles.add(wavFile);
            if (!wavFile.exists()) {
                log.error("!wavFile.exists(). wav file = " + wavFile);
                return false;
            }
        }

        //First we need a directory for this item
        File item_directory = new File(outputdirectory, "item" + count);
        item_directory.mkdir();
        count++;
        log.debug("count = " + count);

        //The contents file simply enumerates, one file per line, the bitstream file names
        //The bitstream name may optionally be followed by \tpermissions:PERMISSIONS
        File contents = new File(item_directory, "contents");
        contents.createNewFile();
        FileWriter contentsFileWriter = new FileWriter(contents);

        //We also need to move or link or copy the wav files to this new directory
        //Files.copy()
        for (File file: wavFiles) {
            Path wavfile_path = file.toPath();
            Files.copy(wavfile_path, item_directory.toPath().resolve(wavfile_path.getFileName()));
            contentsFileWriter.write(file.getName() + '\n');
        }

        //And we would like a zip file with all the wav files
        File zipFile = new File(item_directory, "all.zip");
        zip(wavFiles, zipFile);
        contentsFileWriter.write(zipFile.getName());
        log.debug("zipfile: "+ Arrays.toString(zipFile.list()));


        //The dublin_core.xml file contains some of the metadata as dublin core
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document dcdoc = builder.newDocument();
        Element dcroot = dcdoc.createElement("dublin_core");
        dcroot.setAttribute("schema", "dc");
        dcdoc.appendChild(dcroot);

        //Title: S18 Titel (U20 Sprog)
        addElement("title", null, line[18], line[20], dcdoc, dcroot);
        //Subtitle: H7 Under-titel (U20 Sprog)
        addElement("title", "subtitle", line[7], line[20], dcdoc, dcroot);
        //Alternative title: P15 Alternativ titel (U20 Sprog)
        addElement("title", "alternative", line[15], line[20], dcdoc, dcroot);
        //Date issued: G6 Dato ikke efter
        addElement("date", "issued", line[6], null, dcdoc, dcroot);
        //Data type: Recording, oral
        addElement("type", null, "Recording, oral", attribute_language_en_US, dcdoc, dcroot);
        //Author: Q16 Medvirkende
        addElement("contributor", "author", line[16], null, dcdoc, dcroot);
        //Publisher: M12 Udgiver
        addElement("publisher", null, line[12], null, dcdoc, dcroot);
        //Language: U20 Sprog
        addElement("language", null, line[20], null, dcdoc, dcroot);
        //Rights: Public Domain
        addElement("rights", null, "CC Public Domain", attribute_language_en_US, dcdoc, dcroot);
        addElement("rights", "uri", "https://creativecommons.org/publicdomain/mark/1.0/deed.en",
                attribute_language_en_US, dcdoc, dcroot);
        //Subjects: B1 Keywords, T19 Materialebetegnelse
        addElement("subject", null, line[1], attribute_language_en_US, dcdoc, dcroot);
        addElement("subject", null, line[19], attribute_language_da, dcdoc, dcroot);
        //Description:
        addElement("description", "abstract", "This work was recorded on "+wavFiles.size()+" tapes. " +
                "The digitized tapes can be downloaded separately, or you can download them all as a zip file.", null, dcdoc, dcroot);

        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(dcdoc);
        StreamResult streamResult = new StreamResult(new File(item_directory, "dublin_core.xml"));
        transformer.transform(source, streamResult);

        //remember to write the contents file
        contentsFileWriter.flush();
        contentsFileWriter.close();

        return true;
    }



    /**
     * Add DC element to document.
     * @param element
     * @param qualifier
     * @param textContent
     * @param doc
     * @param root
     */
    public static void addElement(String element, String qualifier, String textContent, String attribute_language, Document doc, Element root) {
        Element dcvalue = doc.createElement("dcvalue");
        dcvalue.setAttribute("element", element);
        if (qualifier!=null) {
            dcvalue.setAttribute("qualifier", qualifier);
        }
        if (attribute_language!=null) {
            dcvalue.setAttribute("language", attribute_language);
        }
        dcvalue.setTextContent(textContent);
        root.appendChild(dcvalue);
    }

    /**
     * zip files into destFile.
     * @param files
     * @param destFile
     * @throws IOException
     */
    public static void zip(List<File> files, File destFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(destFile);
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        for (File f : files) {
            FileInputStream fis = new FileInputStream(f);
            ZipEntry zipEntry = new ZipEntry(f.getName());
            zipOut.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            fis.close();
        }
        zipOut.close();
        fos.close();
    }
    /**
     * Package veda files for ingest into LOAR
     * @param args String input_dir String csv_file String output_dir
     */
    public static void main(String[] args) {
        if (args.length!=3) {
            System.out.println("This method takes 3 arguments:\nString input_dir\nString csv_file\nString output_dir");
            return;
        }
        try {
            String input_dir = args[0];
            String csv_file = args[1];
            String output_dir = args[2];
            VedaCSVReader.readCSVFileAndWriteToSAF(input_dir, csv_file, output_dir);
        } catch(Exception e) {
            System.out.println(e);
        }
    }
}
