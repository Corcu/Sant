package calypsox.tk.util;

import com.calypso.tk.core.Log;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Several Utilities for managing files
 * 
 */
public final class EmirFileUtility {
    private static final int BUFFER_SIZE = 1024;

    private EmirFileUtility() {
        // do nothing
    }

    /**
     * Create a PrintStream to manage a file
     * 
     * @param fileName
     *            FileName with path
     * @param bAppend
     *            true to add lines to an existing file, false to create a new
     *            file
     * @return PrintStream to write in the file
     * @throws FileNotFoundException
     *             if the file can not be found
     */
    public static PrintStream getPrintStream(final String fileName,
            final boolean bAppend) throws FileNotFoundException {
        Log.debug("calypsox.util.FileUtility",
                "FileUtility.getPrintStream Start with fileName=" + fileName);
        PrintStream pStr = null;
        try {
            pStr = new PrintStream(new BufferedOutputStream(
                    new FileOutputStream(fileName, bAppend)));
        } catch (final FileNotFoundException ex) {
            Log.error(Log.CALYPSOX, "FileUtility.getPrintStream Exception:", ex);
            throw ex;
        }
        Log.debug("calypsox.util.FileUtility", "FileUtility.getPrintStream End");
        return pStr;
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
    public static void copyFile(final String inputFileName,
            final String outputFileName) {
        Log.debug("calypsox.util.FileUtility",
                "FileUtility.copyFile Start with inputFileName="
                        + inputFileName + " and outputFileName="
                        + outputFileName);

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
            Log.error(EmirFileUtility.class, e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {

                    in.close();
                }
            } catch (final IOException e) {
                Log.debug(EmirFileUtility.class,
                        "Couldn't close Stream: " + e.getMessage(), e);
            }
        }

        Log.info("calypsox.util.FileUtility", "FileUtility.copyFile End");
    }

    /**
     * Moves a file to another directory. It is a two step(copy and delete)
     * process.
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
        Log.debug("calypsox.util.FileUtility",
                "moveFile Start with inputFileName=" + inputFileName
                        + " and outputFileName=" + outputFileName);

        EmirFileUtility.copyFile(inputFileName, outputFileName);
        Log.debug("calypsox.util.FileUtility", inputFileName
                + " has been copied to " + outputFileName);

        final File fileToDelete = new File(inputFileName);
        final boolean deleteSucceeded = fileToDelete.delete();
        if (!deleteSucceeded) {
            Log.error("calypsox.util.FileUtility", "Failed to delete file "
                    + fileToDelete + " as part of move to " + outputFileName);
            throw new IOException("Failed to delete file " + fileToDelete
                    + " as part of move to " + outputFileName);
        }

        Log.debug("calypsox.util.FileUtility", "moveFile End");
    }

    /**
     * Delete the given file name
     * 
     * @param fileName
     *            file name
     */
    public static void delete(final String fileName) {

        // A File object to represent the filename
        final File f = new File(fileName);

        // Make sure the file or directory exists and isn't write protected
        if (!f.exists()) {
            throw new IllegalArgumentException(
                    "Delete: no such file or directory: " + fileName);
        }

        if (!f.canWrite()) {
            throw new IllegalArgumentException("Delete: write protected: "
                    + fileName);
        }

        // If it is a directory, make sure it is empty
        if (f.isDirectory()) {
            final String[] files = f.list();
            if (files.length > 0) {
                throw new IllegalArgumentException(
                        "Delete: directory not empty: " + fileName);
            }
        }

        // Attempt to delete it
        final boolean success = f.delete();

        if (!success) {
            throw new IllegalArgumentException("Delete: deletion failed");
        }
    }

    /**
     * Create a zip file containing the given filenames in arguments
     * 
     * @param filenames
     *            an array containing the file names
     * @param outFilename
     *            output file name
     */
    public static void createZip(final String[] filenames,
            final String outFilename) {
        // Create a buffer for reading the files
        final byte[] buf = new byte[BUFFER_SIZE];

        ZipOutputStream out = null;
        FileInputStream in = null;
        try {
            // Create the ZIP file
            out = new ZipOutputStream(new FileOutputStream(outFilename));

            // Compress the files
            for (int i = 0; i < filenames.length; i++) {
                in = new FileInputStream(filenames[i]);

                final File file = new File(filenames[i]);
                // Add ZIP entry to output stream.
                out.putNextEntry(new ZipEntry(file.getName()));

                // Transfer bytes from the file to the ZIP file
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                // Complete the entry
                out.closeEntry();
                in.close();
            }

            // Complete the ZIP file
            out.close();
        } catch (final IOException e) {
            Log.error(EmirFileUtility.class, e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {

                    in.close();
                }
            } catch (final IOException e) {
                Log.error(EmirFileUtility.class,
                        "Couldn't close Stream: " + e.getMessage(), e);
            }
        }

    }

}
