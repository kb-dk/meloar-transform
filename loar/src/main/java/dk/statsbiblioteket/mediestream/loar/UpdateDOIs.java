package dk.statsbiblioteket.mediestream.loar;

/**
 * The UpdateDOIS class is used to update DOIs in the metadata of the items,
 * which we have just fixed! They now have working DOIs, but the DOI's were not
 * updated in the metadata as expected. This is what this class fixes.
 *
 * The metadata can be exported as csv, one item or one collection at a time or all content...
 * The csv can the be updated and imported.
 *
 * Note: It is not recommended to import CSV files of more than 1,000 lines (i.e. 1,000 items).
 * We have a total of 4122 items in LOAR as of Tue Nov 19 12:40:38 CET 2019
 * We do not have that many collections, so I think we will do this on collection level.
 *
 *
 */
public class UpdateDOIs {

    /**
     * Update metadata CSV with DOIs from the "dois and urls CSV".
     * @param metadatacsv metadatacsv file path
     * @param doisandurlscsv doisandurlscsv file path
     * @param outoutdir output dir path
     */
    public static void updateCSVwithDOIs(String metadatacsv, String doisandurlscsv, String outoutdir) {

    }
}
