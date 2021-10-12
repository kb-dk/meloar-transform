package dk.statsbiblioteket.mediestream.loar.aviser;

public class AvisPackagerMain {
    /**
     * Package aviser batches for ingest into LOAR
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
            AvisCSVReader.readCSVFileAndWriteToSAF(input_dir, csv_file, output_dir);
        } catch(Exception e) {
            System.out.println(e);
        }
    }
}
