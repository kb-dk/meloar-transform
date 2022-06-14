package dk.statsbiblioteket.mediestream.loar.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipFiles {
    public static void zip(List<File> files, String destFile) throws IOException {
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
}
