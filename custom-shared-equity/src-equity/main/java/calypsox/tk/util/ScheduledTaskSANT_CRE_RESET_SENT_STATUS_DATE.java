package calypsox.tk.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;
import calypsox.util.FileUtility;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSEventCre;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.CreArray;
import com.calypso.tk.util.ScheduledTask;


/**
 * The Class ScheduledTaskSANT_CRE_RESET_SENT_STATUS_DATE.
 */
public class ScheduledTaskSANT_CRE_RESET_SENT_STATUS_DATE extends ScheduledTask {
    private static final long serialVersionUID = -1L;
    private static final String CRES = "Cres to reset:";
    private static final String TASK_INFORMATION = "This scheduled task put the status_sent and de status_date of all the trade's cres to empty";
    private static final String RESET_FROM_FILE = "Reset from File";
    private static final String FILE_PATH = "File Path";
    private static final String FILE_NAME = "File Name";
    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private static final String OK = "ok";
    private static final String FAIL = "fail";
    private static final int BLOCK = 900;
    private static final String CRE_IDS_SEPARATOR = ",";


    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.util.ScheduledTask#getTaskInformation()
     */
    @Override
    public String getTaskInformation() {
        return TASK_INFORMATION;
    }


    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.util.ScheduledTask#isValidInput(java.util.Vector)
     */
    @Override
    public boolean isValidInput(@SuppressWarnings("rawtypes") final Vector errorsP) {
        @SuppressWarnings("unchecked")
        final Vector<String> errors = errorsP;
        if (Boolean.valueOf(getAttribute(RESET_FROM_FILE))) {
            if (Util.isEmpty(getAttribute(FILE_PATH))) {
                errors.add("The field '" + FILE_PATH + "' must be poputated.");
            }
            if (Util.isEmpty(getAttribute(FILE_NAME))) {
                errors.add("The field '" + FILE_NAME + "' must be poputated.");
            }
        } else {
            final String creS = getAttribute(CRES);
            if (!Util.isEmpty(creS)) {
                final String[] cres = creS.split(CRE_IDS_SEPARATOR);
                for (int i = 0; i < cres.length; i++) {
                    if (Util.isEmpty(cres[i])) {
                        errors.add("Put no empty cre ids separated with \"" + CRE_IDS_SEPARATOR + "\".");
                    } else {
                        try {
                            Long.parseLong(cres[i]);
                        } catch (final NumberFormatException nfe) {
                            errors.add("The cre:" + cres[i] + " is not a correct cre id.Should be a number.");
                        }
                    }
                }
            } else {
                errors.add("You must put at least one cre id.");
            }
        }
        return errors.size() == 0;
    }


    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        final List<AttributeDefinition> attrDefList = new ArrayList<>();

        final AttributeDefinition attrCres = attribute(CRES);
        final AttributeDefinition attrFilePath = attribute(FILE_PATH);
        final AttributeDefinition attrFileName = attribute(FILE_NAME);

        final List<String> domainResetFromFile = Arrays.asList(TRUE, FALSE);
        final AttributeDefinition attrResetFromFile = attribute(RESET_FROM_FILE).domain(domainResetFromFile).mandatory();

        attrDefList.add(attrCres);
        attrDefList.add(attrResetFromFile);
        attrDefList.add(attrFilePath);
        attrDefList.add(attrFileName);

        return attrDefList;
    }


    /*
     * (non-Javadoc)
     *
     * @see com.calypso.tk.util.ScheduledTask#process(com.calypso.tk.service. DSConnection ,
     * com.calypso.tk.event.PSConnection)
     */
    @Override
    public final boolean process(final DSConnection ds, final PSConnection ps) {
        boolean bReturn = true;
        final String resetFromFile = getAttribute(RESET_FROM_FILE);

        if (Boolean.valueOf(resetFromFile)) {
            bReturn = resetCresFromFile();
        } else {
            bReturn = resetCresFromAttribute();
        }

        return bReturn;
    }


    /**
     * When the attribute 'Reset From File' is true, the CREs to reset comes
     *
     * @return a boolean that shows ok or worng depending of the result of the process
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private boolean resetCresFromFile() {
        boolean bReturn = true;
        final Vector<String> errors = new Vector<String>();
        final String filePath = getAttribute(FILE_PATH);
        final String fileName = getAttribute(FILE_NAME);
        final String file = filePath + "/" + fileName;
        try {
            final List<Long> creList = processFile(file);
            if (!creList.isEmpty()) {
                final Vector<BOCre> cres = new Vector();
                final Iterator<Long> iterator = creList.iterator();
                while (iterator.hasNext()) {
                    Long creId = iterator.next();
                    final BOCre cre = DSConnection.getDefault().getRemoteBO().getBOCre(creId);
                    if (cre == null) {
                        continue;
                    }
                    cres.add(cre);
                }
                resendCres(cres);
            } else {
                final String message = "This file has not CREs to proccess";
                Log.info(this, message);
                bReturn = false;
            }
        } catch (final FileNotFoundException exFile) {
            Log.error(this, "Exception while managing the file", exFile);
            bReturn = false;
        } catch (final IOException exIO) {
            Log.error(this, "IO Exception", exIO);
            bReturn = false;
        } catch (final NumberFormatException exNum) {
            Log.error(this, "Exception with the format of number", exNum);
            bReturn = false;
        }
        try {
            // Creation of the subfolder
            final String dir = createStringSubfolderOkFail(bReturn);
            // Add the date and time at the end of the name of the file
            final String dateToAdd = getDate();
            // Make a copy of the file in the folder 'ok' or 'fail'
            FileUtility.moveFile(file, filePath + dir + fileName + dateToAdd);
        } catch (final IOException e) {
            Log.error(this, "IO Exception", e);
            bReturn = false;
        }

        // Create a empty file with the same name for next executions
        try {
            if (!createEmptyFile(file)) {
                bReturn = false;
            }
        } catch (final IOException e) {
            Log.error(this, "IO Exception", e);
            bReturn = false;
        }

        return bReturn;
    }


    /**
     * When the attribute 'Reset From File' is false, the CREs to reset comes from attribute 'Cres to
     * reset'
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private boolean resetCresFromAttribute() {
        boolean rst = true;
        final Vector<String> errors = new Vector<String>();
        final String creIds = getAttribute(CRES);
        if (!Util.isEmpty(creIds)) {
            //resetCres(errors, creIds);
            Log.info(this, "CRES reset: " + creIds);
            final String[] creIdsSplitted = creIds.split(CRE_IDS_SEPARATOR);
            final Vector<BOCre> cres = new Vector();
            for (int i = 0; i < creIdsSplitted.length; i++) {
                final String currentCreIdS = creIdsSplitted[i];
                if (Util.isEmpty(currentCreIdS)) {
                    continue;
                }
                final long currentCreId = Long.valueOf(currentCreIdS);
                BOCre cre = null;
                try {
                    cre = DSConnection.getDefault().getRemoteBO().getBOCre(currentCreId);
                } catch (final RemoteException e) {
                    Log.error(this, "Error while get Cre with Id " + currentCreId, e);
                    rst = false;
                }

                if (cre != null) {
                    cres.add(cre);
                }
            }

            resendCres(cres);
        }
        return rst;
    }


    /**
     * This method joins the CREs into a string * @param list with the CREs
     *
     * @param ini index begining of the list to process
     * @param end index end of the list list to process
     * @return a string with the CREs joined
     */
    private String creListToString(final List<Long> list, final int ini, final int end) {
        final StringBuffer creBuffer = new StringBuffer();
        for (int x = ini; x < end; x++) {
            creBuffer.append(list.get(x).toString());
            if (x != (end - 1)) {
                creBuffer.append(CRE_IDS_SEPARATOR);
            }
        }
        return creBuffer.toString();
    }


    /**
     * This method process the file reading each line and adding then to a list of CREs
     *
     * @param file represents the file to process
     * @return a list of CREs
     * @throws IOException in case of a problem with the file
     */
    private ArrayList<Long> processFile(final String file) throws IOException {

        final ArrayList<Long> creList = new ArrayList<Long>();
        BufferedReader reader = null;
        String line = "";

        try {
            reader = createBufferedReader(file);
            while (reader.ready()) {
                line = reader.readLine();
                line = removeWhiteSpaces(line);
                if (!Util.isEmpty(line)) {
                    final long creId = Long.parseLong(line);
                    creList.add(creId);
                }
            }
        } catch (final IOException eIo) {
            final String message = eIo.toString() + " : " + eIo.getMessage() + " : Error using the file: " + file;
            Log.error(this, message, eIo);
            throw eIo;
        } catch (final NumberFormatException eNum) {
            final String message = eNum.toString() + " : " + eNum.getMessage() + " : Error with the format of the CRE: "
                    + line;
            Log.error(this, message, eNum);
            throw eNum;
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return creList;
    }


    /**
     * This method create the end of the moved file adding the date and time
     *
     * @return a string with the date and time concatenated
     */
    private String getDate() {
        final Locale locale = new Locale("es", "ES");
        Locale.setDefault(locale);

        final SimpleDateFormat formater = new SimpleDateFormat("yyyyMMdd_kkmmss", locale);
        final String dateString = "_" + formater.format(new Date());
        return dateString;
    }


    /**
     * Create a empty file in the path showed in fileName
     *
     * @param fileName is the complete path of the file
     * @return a boolean, true if it goes right
     * @throws Exception if there is something worng with the file
     */
    private boolean createEmptyFile(final String fileName) throws IOException {
        Boolean breturn = null;

        try {
            final File file = new File(fileName);
            breturn = file.createNewFile();
        } catch (final IOException e) {
            final String message = e.toString() + " : " + e.getMessage() + " : Error with the file" + fileName;
            Log.error(this, message, e);
            breturn = false;
        }

        return breturn;
    }


    /**
     * This method add the route ok or file to the path to move the file
     *
     * @param bReturn , param that show if the process is ok or not
     * @return a String with the subfolder
     */
    private String createStringSubfolderOkFail(final boolean bReturn) {
        String dir = "";
        if (bReturn) {
            dir = "/" + OK + "/";
        } else {
            dir = "/" + FAIL + "/";
        }
        return dir;
    }


    /**
     * Create an instance of FileInputStream from an input file path.
     *
     * @param file
     * @return
     * @throws FileNotFoundException
     */
    protected FileInputStream createFileInputStream(final String file) throws FileNotFoundException {
        return new FileInputStream(file);
    }


    /**
     * Create an instance of InputStreamReader from a FileInputStream object.
     *
     * @param fileInputStream
     * @return
     */
    protected InputStreamReader createInputStreamReader(final FileInputStream fileInputStream) {
        return new InputStreamReader(fileInputStream, Charset.defaultCharset());
    }


    protected BufferedReader createBufferedReader(final String file) throws FileNotFoundException {
        final InputStream inputStream = createFileInputStream(file);
        final Reader reader = createInputStreamReader((FileInputStream) inputStream);
        return new BufferedReader(reader);
    }


    public void resendCres(final Vector<BOCre> cres) {
        if (cres == null) {
            return;
        }
        for (int i = cres.size() - 1; i >= 0; i--) {
            final BOCre cre = cres.get(i);
            if (cre == null) {
                continue;
            }
            cre.setSentStatus(null);
            cre.setSentDate(null);
        }
        saveCres(cres);
    }


    private void saveCre(final BOCre cre) {
        final CreArray cres = new CreArray();
        cres.add(cre);
        boolean saveOk = false;
        try {
            DSConnection.getDefault().getRemoteBO().saveCres(cres);
            saveOk = true;
        } catch (final RemoteException e1) {
            Log.error(this, "Error while saving BOCre", e1);
        }
        if (saveOk) {
            final PSEventCre event = new PSEventCre();
            try {
                event.setBoCre(DSConnection.getDefault().getRemoteBO().getBOCre(cre.getId()));
                DSConnection.getDefault().getRemoteTrade().saveAndPublish(event);
            } catch (final RemoteException e1) {
                Log.error(this, "Error while publishing PSEventSaveCre", e1);
            }
        }
    }


    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void saveCres(final Vector<BOCre> cres) {
        if ((cres == null) || (cres.size() <= 0)) {
            return;
        }
        final CreArray cresArray = new CreArray();
        for (int i = 0; i < cres.size(); i++) {
            final BOCre cre = cres.get(i);
            if (cre == null) {
                continue;
            }
            cresArray.add(cre);
        }
        boolean saveOk = false;
        try {
            DSConnection.getDefault().getRemoteBO().saveCres(cresArray);
            saveOk = true;
        } catch (final RemoteException e1) {
            Log.error(this, "Error while saving BOCre", e1);
        }
        if (saveOk) {
            final Vector<PSEventCre> events = new Vector();
            for (int i = 0; i < cres.size(); i++) {
                final PSEventCre event = new PSEventCre();
                try {
                    event.setBoCre(DSConnection.getDefault().getRemoteBO().getBOCre(cres.get(i).getId()));
                    events.add(event);
                } catch (final RemoteException e1) {
                    Log.error(this, "Error while getting the cre", e1);
                }
            }
            try {
                DSConnection.getDefault().getRemoteTrade().saveAndPublish(events);
            } catch (final RemoteException e1) {
                Log.error(this, "Error while publishing PSEventCre", e1);
            }
        }
    }


    public String removeWhiteSpaces(final String string) {
        return string.replaceAll("\\s+", "");
    }


}