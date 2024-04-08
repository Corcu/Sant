package calypsox.tk.util;


import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import com.calypso.tk.core.Log;


public class FileUtilityEquity {


    private FileUtilityEquity() {
        // do nothing
    }


    /**
     * Moves a file to another directory. It is a two step(copy and delete) process.
     * 
     * @param inputFileName
     *            input file name
     * @param outputFileName
     *            output file name
     * @throws IOException
     *             throws IOExeption in case it can't copy or delete
     */
    public static void moveFile(final String inputFileName,
            final String outputFileName) throws IOException {
        Log.debug("calypsox.util.FileUtilityEquity", "moveFile Start with inputFileName="
        		   + inputFileName + " and outputFileName=" + outputFileName);

        FileUtilityEquity.copyFile(inputFileName, outputFileName);
        Log.debug("calypsox.util.FileUtilityEquity", inputFileName
                + " has been copied to " + outputFileName);

        final File fileToDelete = new File(inputFileName);
        final boolean deleteSucceeded = fileToDelete.delete();
        if (!deleteSucceeded) {
            Log.error("calypsox.util.FileUtilityEquity", "Failed to delete file "
                    + fileToDelete + " as part of move to " + outputFileName);
            throw new IOException("Failed to delete file " + fileToDelete
                    + " as part of move to " + outputFileName);
        }

        Log.debug("calypsox.util.FileUtilityEquity", "moveFile End");
    }
	
	
    /**
     * Copy a file to another directory
     * 
     * @param inputFileName
     *            input file name
     * @param outputFileName
     *            output file name
     * @throws IOException
     *             if an error occurred
     */
    public static void copyFile(final String inputFileName, final String outputFileName) {
        Log.debug("calypsox.util.FileUtility", "FileUtilityEquity.copyFile Start with inputFileName="
                  + inputFileName + " and outputFileName=" + outputFileName);

        FileReader in = null;
        FileWriter out = null;
        try {
            final File inputFile = new File(inputFileName);
            final File outputFile = new File(outputFileName);

            in = new FileReader(inputFile);
            out = new FileWriter(outputFile);
            int c;
            while ((c = in.read()) != -1) {
                out.write(c);
            }
        } catch (final IOException e) {
            Log.error(FileUtilityEquity.class, e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (final IOException e) {
                Log.debug(FileUtilityEquity.class, "Couldn't close Stream: " + e.getMessage(), e);
            }
        }

        Log.info("calypsox.util.FileUtilityEquity", "FileUtilityEquity.copyFile End");
    }
	
}
