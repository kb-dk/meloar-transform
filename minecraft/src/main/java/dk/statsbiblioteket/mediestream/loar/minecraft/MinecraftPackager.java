package dk.statsbiblioteket.mediestream.loar.minecraft;

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
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The "Danmark i Minecraft" data consist of approx 600 zip files, totalling 350 GB
 * The minecraft_filelist file matches the files to coordinates.
 * Each zip file corresponds to an item.
 */
public class MinecraftPackager {
    private static Logger log = LoggerFactory.getLogger(MinecraftPackager.class);
    private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private static int count = 0;
    private static String attribute_language_da = "da";
    private static String attribute_language_en_US = "en_US";

    /**
     * Write a LOAR DSpace Simple Archive Format structure based on the given line.
     *  @param datadirectory   String directoryname name where to read data
     * @param line line in csv with filename and metadata
     * @param outputdirectory String directory name where to put structure
     */
    public static boolean writeItemAsSAF(String datadirectory, String[] line, String outputdirectory) throws IOException,
            ParserConfigurationException, TransformerException {
        log.info("entering writeItemAsSAF method with parameters: (" + datadirectory + ", " + outputdirectory + ")");

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

        /*
        //We would like a zip file with all the zip files?
        File dataDir = new File(datadirectory);
        List<File> inputFiles = Arrays.asList(dataDir.listFiles());
        File zipFile = new File(item_directory, "all.zip");
        zip(inputFiles, zipFile);
        contentsFileWriter.write(zipFile.getName());
        log.debug("zipfile: "+ Arrays.toString(zipFile.list()));
         */

        //Write the zip file to the item directory
        File zipFile = new File(datadirectory, line[0]);
        Path zip_file_path = zipFile.toPath();
        Files.copy(zip_file_path, item_directory.toPath().resolve(zip_file_path.getFileName()));
        contentsFileWriter.write(zipFile.getName() + '\n');

        //The dublin_core.xml file contains some metadata as dublin core
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document dcdoc = builder.newDocument();
        Element dcroot = dcdoc.createElement("dublin_core");
        dcroot.setAttribute("schema", "dc");
        dcdoc.appendChild(dcroot);

        //Title
        addElement("title", null, "Danmark i Minecraft ("+line[5]+","+line[6]+
                ") UTM32-ETRS89 (øst, nord)", attribute_language_da, dcdoc, dcroot);
        //Spatial coverage coordinates in UTM32-ETRS89
        addElement("coverage", "spatial", "UTM32-ETRS89 ("+line[5]+","+line[6]+")",
                null, dcdoc, dcroot);
        //Date issued
        addElement("date", "issued", "2014", null, dcdoc, dcroot);
        //Data type: Dataset
        addElement("type", null, "Dataset", attribute_language_en_US, dcdoc, dcroot);
        //Author: ???
        addElement("contributor", "author", "Styrelsen for Dataforsyning og Infrastruktur", null, dcdoc, dcroot);
        //Publisher: Geodatastyrelsen
        addElement("publisher", null, "Styrelsen for Dataforsyning og Infrastruktur", null, dcdoc, dcroot);
        //Language: da
        addElement("language", null, attribute_language_da, null, dcdoc, dcroot);
        //Rights: Public Domain
        addElement("rights", null, "CC Public Domain", attribute_language_en_US, dcdoc, dcroot);
        addElement("rights", "uri", "https://creativecommons.org/publicdomain/mark/1.0/deed.en",
                attribute_language_en_US, dcdoc, dcroot);
        //Subjects: Minecraft, Danmark
        addElement("subject", null, "Minecraft", attribute_language_en_US, dcdoc, dcroot);
        addElement("subject", null, "Danmark", attribute_language_da, dcdoc, dcroot);
        //Description:
        addElement("description", "abstract", "Centrum for dette kort er ("+line[5]+","+line[6]+
                ") i UTM32-ETRS89 (øst, nord). Disse koordinater kan du bruge på et krak-kort i din browser. Kortet er 10*10 km." +
                "Centrum for kortet i Minecraft er x="+line[3]+" og z="+line[4]+".", attribute_language_da, dcdoc, dcroot);

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
     * Package minecraft files for ingest into LOAR
     * @param args String input_dir String output_dir
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
            MinecraftReader.readCSVFileAndWriteToSAF(input_dir, csv_file, output_dir);
        } catch(Exception e) {
            System.out.println(e);
        }
    }
}
