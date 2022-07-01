package dk.statsbiblioteket.mediestream.loar.veda;

public class VedaPackagerInvoker {

    public static void main(String[] args) {
        VedaPackager.main(new String[]{
                "veda/src/test/resources/input",
                "veda/src/test/resources/Veda-metadata_jamo_short.csv",
                "veda/src/test/resources/output"});
    }
}
