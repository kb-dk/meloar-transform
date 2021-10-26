package dk.statsbiblioteket.mediestream.loar.aviser;

public class AvisPackagerInvoker {

    public static void main(String[] args) {
        AvisPackager.main(new String[]{
                "aviser/src/test/resources/aviser_input",
                "aviser/src/main/resources/aviser_20211026.csv",
                "aviser/src/main/resources/output"});
    }
}
