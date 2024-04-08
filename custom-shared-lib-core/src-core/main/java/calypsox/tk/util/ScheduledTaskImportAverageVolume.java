/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.util;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.bean.AverageVolumeBean;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.FileUtility;
import com.calypso.infra.util.Util;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.product.Equity;
import com.calypso.tk.service.DSConnection;

import java.io.*;
import java.rmi.RemoteException;
import java.util.*;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

/**
 * Processes equities to inform 90 day average volume
 *
 * @author Diego Cano Rodr?guez <diego.cano.rodriguez@accenture.com>
 */
public class ScheduledTaskImportAverageVolume extends AbstractProcessFeedScheduledTask {

    private static final long serialVersionUID = 3194368663628571947L;

    /**
     * Varibles
     */
    private File file;

    /**
     * Constants
     */
    private static final String ATT_SEPERATOR = "Separator";
    private static final String AVERAGE_VOLUME = "90DAYS_AVERAGE_VOLUME";
    private static final String ISIN = "ISIN";

    /**
     * Main process
     */

    @Override
    public boolean process(final DSConnection conn, final PSConnection connPS) {

        boolean result = true;
        try {
            final String path = getAttribute(FILEPATH);
            final String startFileName = getAttribute(STARTFILENAME);
            JDate fileDate = getValuationDatetime().getJDate(TimeZone.getDefault());

            // BAU - Use different way to import data in order to avoid multiple
            // files problem
            this.file = lookForFile(path, startFileName, fileDate);

            if (this.file != null) {

                // Just after file verifications, this method will make a copy
                // into the
                // ./import/copy/ directory
                FileUtility.copyFileToDirectory(path + this.file.getName(), path + "/copy/");

                // file process
                result = processFile(path + this.file.getName());
            } else {
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, "No matches found for filename in the path specified.");
                ControlMErrorLogger.addError(ErrorCodeEnum.InputFileNotFound,
                        "No matches found for filename in the path specified.");
                result = false;
            }

        } catch (Exception exc) {
            Log.error(this, exc); //sonar
        } finally {
            try {
                feedPostProcess();
            } catch (Exception e) {
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while moving the files to OK/Fail folder");
                Log.error(this, e); //sonar
                ControlMErrorLogger.addError(ErrorCodeEnum.UndefinedException,
                        "Error while moving the files to OK/Fail folder");
                result = false;
            }
        }
        return result;
    }

    /**
     * @param file
     * @return ok if file was correctly processed
     */
    private boolean processFile(final String file) {

        Log.info(LOG_CATEGORY_SCHEDULED_TASK, "Starting process of file " + file);

        BufferedReader reader = null;
        try {

            Log.info(LOG_CATEGORY_SCHEDULED_TASK, "Loading all equities");
            final Map<String, Equity> allEquitiesMap = loadEquitiesMap();

            reader = new BufferedReader(new FileReader(file));
            String line = null;
            int lineNumber = -1;
            while ((line = reader.readLine()) != null) {

                lineNumber++;
                if (lineNumber == 0) {
                    continue;
                }

                final AverageVolumeBean bean = buildBean(line);

                String key = bean.getIsin() + bean.getCurrency();

                Equity equity = allEquitiesMap.get(key);

                if (equity == null) {
                    Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Line number-" + lineNumber
                            + " : Equity not found with ISIN & CCY" + key);
                    continue;
                }

                equity.setSecCode(AVERAGE_VOLUME, bean.getSec_code());

                try {
                    getDSConnection().getRemoteProduct().saveProduct(equity, true); // save equity
                } catch (RemoteException re) {
                    Log.error(LOG_CATEGORY_SCHEDULED_TASK,
                            "Line number-" + lineNumber + " : Error while saving the Equity with ISIN&CCY="
                                    + key + ", Error=" + re.getLocalizedMessage());
                    Log.error(this, re); //sonar
                }
            }
            Log.info(LOG_CATEGORY_SCHEDULED_TASK, file + " processed");

        } catch (Exception exc) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, exc);
            return false;

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.error(LOG_CATEGORY_SCHEDULED_TASK, e.getMessage());
                    Log.error(this, e); //sonar
                }
            }
        }
        return true;
    }

    /**
     * ST attributes
     */
    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        // Gets superclass attributes
        attributeList.addAll(super.buildAttributeDefinition());
        attributeList.add(attribute(ATT_SEPERATOR));

        return attributeList;
    }

    /**
     * Check ST attributes are valid
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public boolean isValidInput(final Vector messages) {
        boolean retVal = super.isValidInput(messages);

        final String seperator = getAttribute(ATT_SEPERATOR);
        if (Util.isEmpty(seperator)) {
            messages.addElement(ATT_SEPERATOR + " is not specified");
            retVal = false;
        }
        return retVal;
    }

    /**
     * @return a map with all the equities in the system where Map {ISIN+CCY,
     * Equity}
     * @throws RemoteException
     */
    @SuppressWarnings({"unchecked"})
    private Map<String, Equity> loadEquitiesMap() throws RemoteException {

        final Map<String, Equity> equitiesMap = new HashMap<>();
        final String where = " product_desc.product_family='Equity' ";
        final Vector<Equity> allEquities = getDSConnection().getRemoteProduct().getAllProducts(null, where, null);

        for (Equity e : allEquities) {

            final String isin = e.getSecCode(ISIN);
            final String ccy = e.getCurrency();

            if (checkEquity(isin, e)) {

                final String key = isin.trim() + ccy.trim();
                if (!equitiesMap.containsKey(key)) {
                    equitiesMap.put(key, e);
                }
            }
        }
        return equitiesMap;
    }

    /**
     * @param isin
     * @param e    Equity
     * @return true if the equity has the isin + currency
     */
    private boolean checkEquity(final String isin, final Equity e) {

        final String ccy = e.getCurrency();

        boolean ok = true;
        if (Util.isEmpty(isin)) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Equity doesn't have ISIN. Equity Id =" + e.getId());
            ok = false;
        }
        if (Util.isEmpty(ccy)) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Equity doesn't have CCY. Equity Id =" + e.getId());
            ok = false;
        }
        return ok;
    }

    /**
     * @param line
     * @return the AverageVolume bean. If product id cannot be processed, it will
     * return null;
     */
    private AverageVolumeBean buildBean(final String line) {
        String[] fields = CollateralUtilities.splitMejorado(3, getAttribute(ATT_SEPERATOR), false,
                line);
        for (int i = 0; i < fields.length; i++) {
            fields[i] = fields[i].trim();
        }
        return new AverageVolumeBean(fields);
    }

    // BAU - Use different way to import data in order to avoid multiple files
    // problem

    /**
     * @param path
     * @param fileName
     * @param date
     * @return the file to import
     */
    public File lookForFile(String path, String fileName, JDate date) {

        final String fileNameFilter = fileName;
        // name filter
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File directory, String fileName) {
                return fileName.startsWith(fileNameFilter);
            }
        };

        final File directory = new File(path);
        final File[] listFiles = directory.listFiles(filter);

        for (File file : listFiles) {

            final Long dateFileMilis = file.lastModified();
            final Date dateFile = new Date(dateFileMilis);
            final JDate jdateFile = JDate.valueOf(dateFile);

            @SuppressWarnings("unused")
            double aux = JDate.diff(date, jdateFile);

            if (JDate.diff(date, jdateFile) == 0) {
                return file;
            }

        }

        return null;

    }

    /**
     * Retrieves the file name
     */
    @Override
    public String getFileName() {
        return this.file.getName();
    }

    /**
     * Task Info
     */
    @Override
    public String getTaskInformation() {
        return "Import 90 days average volume info.";
    }

}
