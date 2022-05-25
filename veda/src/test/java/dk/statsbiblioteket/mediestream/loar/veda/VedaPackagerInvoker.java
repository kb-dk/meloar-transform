package dk.statsbiblioteket.mediestream.loar.veda;

public class VedaPackagerInvoker {

    public static void main(String[] args) {
        VedaPackager.main(new String[]{
                "aviser/src/test/resources/aviser_input",
                "aviser/src/main/resources/aviser_20211026.csv",
                "aviser/src/main/resources/output"});
    }
}
