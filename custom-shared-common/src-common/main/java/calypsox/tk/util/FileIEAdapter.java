/*
 * Calypso API -  February 2008.
 *
 * Copyright © 2008 Calypso Technology, Inc. All Rights Reserved
 */

package calypsox.tk.util;

import calypsox.tk.bo.JMSQueueMessage;
import com.calypso.io.FileReader;
import com.calypso.io.FileWriter;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.IEAdapterListener;

import java.io.File;
import java.io.FilenameFilter;
import java.security.InvalidParameterException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/* Expected to have in resources the file updatepositionfileadapter_config.properties
 * with the next lines
 */
//########################################
//#      For LOCAL test purpose          #
//########################################
//
//file.import.directory.path=C:/Temp/updateposition/import
//file.import.directory.scan.frequency=5000
//file.ready.ext=
//file.processed.directory.path=C:/Temp/updateposition/processed
//file.export.directory.path=C:/Temp/updateposition/export
//file.output.name=test

public class FileIEAdapter extends SantanderIEAdapter {

    private IEAdapterMode mode;

    // import settings
    private String importDirectory = null;
    private long directoryScanFrequency = 1000; // default 1 seconds
    private String fileReadyExt = "ready";
    private String fileProcessedDirectory = "processed";
    private DirectoryFileListener dirScanner = null;

    // export settings
    private String exportDirectory = null;
    private String outputFileName = "output-file";
    private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssS");

    @Override
    public void commit() throws ConnectException {
    }

    @Override
    public boolean getTransactionEnabled() throws ConnectException {
        return false;
    }

    @Override
    public void init() throws com.calypso.tk.util.ConnectException {
        initProperties();
        init(false);
    }

    /**
     * Lookup for import and export directories; and start the ball rolling.
     *
     * @param wait the wait
     * @throws ConnectException the connect exception
     */
    public void init(final boolean wait) throws ConnectException {
        boolean initOk = true;
        final ConnectException error = new ConnectException("init IE Adapter failed");

        if (IEAdapterMode.WRITE.equals(this.mode)) {
            if (!Util.isEmpty(getExportDirectory())) {
                initOk = false;
                // Check that output file can be created
                final String fileName = buildFileName() + ".testinit";
                write("init() test", fileName);
                final File file = new File(fileName);
                initOk = file.delete();
            }
        }

        if (IEAdapterMode.READ.equals(this.mode)) {
            // start scanning import directory
            if (!Util.isEmpty(getImportDirectory())) {
                initOk = false;
                final File directory = new File(getImportDirectory());
                if (directory.isDirectory()) {
                    directory.list(this.importFileNameFilter);
                    this.dirScanner = new DirectoryFileListener(getImportDirectory());
                    final Thread dirScannerThread = new Thread(this.dirScanner, "ImportDirectoryScan");
                    dirScannerThread.start();
                    initOk = true;
                }
            }
        }

        if (!initOk) {
            throw error;
        }
        this._isOnline = true;
        Log.info(this, "Started FileIEAdapter: " + toString());
    }

    /**
     * Set the properties for the Sender/Receiver
     *
     * @param Properties for the adapter
     */
    protected void initProperties() {
        String propVal = null;

        if ((propVal = getProperty("file.import.directory.path")) != null) {
            setImportDirectory(propVal);
        }

        if ((propVal = getProperty("file.import.directory.scan.frequency")) != null) {
            setDirectoryScanFrequency(Long.valueOf(propVal));
        }

        if ((propVal = getProperty("file.ready.ext")) != null) {
            setFileReadyExt(propVal);
        }

        if ((propVal = getProperty("file.processed.directory.path")) != null) {
            setFileProcessedDirectory(propVal);
        }

        if ((propVal = getProperty("file.export.directory.path")) != null) {
            setExportDirectory(propVal);
        }

        if ((propVal = getProperty("file.output.name")) != null) {
            setOutputFileName(propVal);
        }

        Log.info(this, "Loaded properties: " + toString());
    }

    @Override
    public String toString() {
        return getClass().getName() + " importDirectory = " + getImportDirectory() + ", directoryScanFrequency = "
                + getDirectoryScanFrequency() + "ms, fileReadyExt = " + getFileReadyExt()
                + ", fileProcessedDirectory = " + getFileProcessedDirectory() + ", exportDirectory = "
                + getExportDirectory() + ", outputFileName = " + getOutputFileName();
    }

    @Override
    public void reinit() throws com.calypso.tk.util.ConnectException {
        init(false);
    }

    private final FilenameFilter importFileNameFilter = new FilenameFilter() {
        @Override
        public boolean accept(final File dir, final String name) {
            return name.endsWith(getFileReadyExt());
        }
    };

    @Override
    protected boolean callBackListener(final String message) {
        // return _listener.newMessage(this, message);
        final JMSQueueMessage externalMessage = new JMSQueueMessage();
        externalMessage.setText(message);
        return this._listener.newMessage(this, externalMessage);

    }

    private class DirectoryFileListener implements Runnable {
        private File dir = null;//Sonar
        private boolean run = true;

        public DirectoryFileListener(final String directoryName) {
            this.dir = new File(directoryName);
        }

        public void stop() {
            this.run = false;
        }

        @Override
        public void run() {
            while (this.run) {
                try {
                    Thread.sleep(getDirectoryScanFrequency());
                    final File[] foundFiles = this.dir.listFiles(FileIEAdapter.this.importFileNameFilter);
                    if ((foundFiles != null) && (foundFiles.length > 0)) {
                        if (Log.isDebug()) {
                            Log.debug(this, "Found files: " + Arrays.asList(foundFiles).toString());
                        }

                        File file = null;
                        for (int i = 0; i < foundFiles.length; i++) {
                            if (!this.run) { // we may want to stop process even
                                // if
                                // all files not processed
                                break;
                            }
                            file = foundFiles[i];
                            final FileReader fileReader = new FileReader(file);
                            final String message = fileReader.readFile();
                            if (Log.isDebug()) {
                                Log.debug(this, file.getName() + " contains : " + message);
                            }
                            final boolean success = callBackListener(message);
                            String newFileName = null;
                            if (success) {
                                newFileName = getFileProcessedDirectory() + "/" + file.getName();
                            } else {
                                newFileName = file.getCanonicalPath() + ".error";
                            }
                            final File renamed = new File(newFileName);
                            if (!file.renameTo(renamed)) {
                                throw new InvalidParameterException("Failed to rename from file: " + file
                                        + " to file: " + newFileName);
                            }
                        }

                    }
                } catch (final Exception e) {
                    Log.error(this, e);
                    stop();
                }
            }
        }

    }

    @Override
    public void rollback() throws ConnectException {
    }

    @Override
    public void stop() throws Exception {
        if (this.dirScanner != null) {
            this.dirScanner.stop();
        }
    }

    /**
     * Could be overriden to customize the filename
     *
     * @return
     */
    public String buildFileName() {
        return getExportDirectory() + "/" + this.dateFormat.format(new Date()) + "-" + Math.random() + "-"
                + getOutputFileName();
    }

    @Override
    public boolean write(final String message) {
        return write(message, buildFileName());
    }

    protected boolean write(final String message, final String filename) {
        boolean ret = false;
        final FileWriter writer = new FileWriter(filename);
        try {
            if (Log.isDebug()) {
                Log.debug(this, "Writing in file: " + filename + " message:\n" + message);
            }
            writer.writeFile(message);
            ret = true;
        } catch (final Exception e) {
            Log.error(this, e);
        }
        return ret;
    }

    public long getDirectoryScanFrequency() {
        return this.directoryScanFrequency;
    }

    public void setDirectoryScanFrequency(final long directoryScanFrequency) {
        this.directoryScanFrequency = directoryScanFrequency;
    }

    public String getExportDirectory() {
        return this.exportDirectory;
    }

    public void setExportDirectory(final String exportDirectory) {
        this.exportDirectory = exportDirectory;
    }

    public String getFileProcessedDirectory() {
        return this.fileProcessedDirectory;
    }

    public void setFileProcessedDirectory(final String fileProcessedExt) {
        this.fileProcessedDirectory = fileProcessedExt;
    }

    public String getFileReadyExt() {
        return this.fileReadyExt;
    }

    public void setFileReadyExt(final String fileReadyExt) {
        this.fileReadyExt = fileReadyExt;
    }

    public String getImportDirectory() {
        return this.importDirectory;
    }

    public void setImportDirectory(final String importDirectory) {
        this.importDirectory = importDirectory;
    }

    public String getOutputFileName() {
        return this.outputFileName;
    }

    public void setOutputFileName(final String outputFileName) {
        this.outputFileName = outputFileName;
    }

    /**
     * Purpose of this override is to give the IEAdapterListener visibility on this Adapter. Mainly because
     * ImportMessageEngine.IEAdapter is not visible to extensions.
     */
    @Override
    public void setListener(final IEAdapterListener listener) {
        this._listener = listener;
        if (listener instanceof ExtendedIEAdapterListener) {
            ((ExtendedIEAdapterListener) listener).setIEAdapter(this);
        }
    }

    /**
     * Instantiates a new file ie adapter.
     *
     * @param mode the mode
     */
    public FileIEAdapter(final IEAdapterMode mode) {
        super();
        setMode(mode);
    }

    /**
     * Sets the mode.
     *
     * @param mode the new mode
     */
    public void setMode(final IEAdapterMode mode) {
        this.mode = mode;
    }

    @Override
    public boolean write(JMSQueueMessage message) {
        return false;
    }

    @Override
    public boolean write(String message, BOMessage boMessage) {
        return false;
    }

    @Override
    boolean createQueueDynamicallyAndWrite(JMSQueueMessage message) {
        return false;
    }
}
