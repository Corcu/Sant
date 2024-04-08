/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.util;

import calypsox.ErrorCodeEnum;
import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.product.BondCustomData;
import calypsox.tk.util.bean.ECBAttributesBean;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.infra.util.Util;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.product.Bond;
import com.calypso.tk.service.DSConnection;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

public class ScheduledTaskImportBondECBAttributes extends AbstractProcessFeedScheduledTask {

    private String fileName = "";
    private static final int NUMBER_OF_FIELDS = 6;

    // START OA 27/11/2013
    // Oracle recommendation : declare a serialVersionUID for each Serializable class in order to avoid
    // InvalidClassExceptions.
    // Please refer to Serializable javadoc for more details
    private static final long serialVersionUID = 149875004569L;
    // END OA OA 27/11/2013

    private static final String ATT_SEPERATOR = "Separator";
    protected static final String ATT_SUMMARY_LOG = "Summary Log";
    protected static final String ATT_DETAILED_LOG = "Detailed Log";
    protected static final String ATT_FULL_LOG = "Full Log";


    @Override
    public Vector<String> getDomainAttributes() {
        final Vector<String> attr = super.getDomainAttributes();
        attr.add(ATT_SEPERATOR);
        attr.add(ATT_SUMMARY_LOG);
        attr.add(ATT_DETAILED_LOG);
        attr.add(ATT_FULL_LOG);
        return attr;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
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

    @Override
    public boolean process(final DSConnection conn, final PSConnection connPS) {
        boolean result = true;

        try {
            final String path = getAttribute(FILEPATH);
            final String startFileName = getAttribute(STARTFILENAME);
            final String date = CollateralUtilities.getValDateString(this.getValuationDatetime());
            @SuppressWarnings("unused") final ArrayList<String> files = CollateralUtilities.getListFiles(path, startFileName + date);

            this.fileName = getAndChekFileToProcess(path, startFileName);

            if (!Util.isEmpty(this.fileName)) {
                result = processFile(path + this.fileName);

            } else {
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

    private boolean processFile(String file) {
        BufferedReader reader = null;
        try {
            Map<String, Bond> allBondsMap = loadBondsMap();
            Map<String, Bond> bondsWithECB = new HashMap<String, Bond>();

            reader = new BufferedReader(new FileReader(file));
            String line = null;
            int lineNumber = -1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 0) {
                    continue;
                }

                if (line.startsWith("*****")) {
                    break;
                }

                if (!CollateralUtilities.checkFields(line, '|', NUMBER_OF_FIELDS - 1)) {
                    Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Line number-" + lineNumber
                            + " : Number of fields didn't match, should  be " + NUMBER_OF_FIELDS);
                    continue;
                }

                ECBAttributesBean bean = buildBean(line);
                Bond bond = allBondsMap.get(getKey(bean.getIsin(), bean.getCcy()));
                if (bond == null) {
                    Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Line number-" + lineNumber + " : Bond not found with ISIN"
                            + bean.getIsin() + ", Currency=" + bean.getCcy());
                    continue;
                }

                processLine(bond, bean);

                try {
                    bondsWithECB.put(getKey(bean.getIsin(), bean.getCcy()), bond);
                    getDSConnection().getRemoteProduct().saveBond(bond, true);
                } catch (RemoteException re) {
                    Log.error(LOG_CATEGORY_SCHEDULED_TASK,
                            "Line number-" + lineNumber + " : Error while saving the Bond with ISIN=" + bean.getIsin()
                                    + ", Currency=" + bean.getCcy() + ", Error=" + re.getLocalizedMessage());
                    Log.error(this, re); //sonar
                }
            }
            // Remove ECB Attributes for the remaining Bonds.
            Collection<Bond> bondsWithNoECB = allBondsMap.values();
            bondsWithNoECB.removeAll(bondsWithECB.values());
            for (Bond bond : bondsWithNoECB) {
                removeECBAttributes(bond);
                try {
                    getDSConnection().getRemoteProduct().saveBond(bond, true);
                } catch (RemoteException re) {
                    Log.error(LOG_CATEGORY_SCHEDULED_TASK,
                            "Error saving Bond after removing ECB Attribute, ISIN=" + bond.getSecCode("ISIN")
                                    + ", Currency=" + bond.getCurrency() + ", Error=" + re.getLocalizedMessage());
                    Log.error(this, re); //Sonar
                }
            }

        } catch (Exception exc) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, exc);
            return false;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.error(this, e); //sonar
                }
            }
        }

        return true;
    }

    private void processLine(Bond bond, ECBAttributesBean bean) {

        if (!Util.isEmpty(bean.getECBHaircut())) {
            BondCustomData customData = (BondCustomData) bond.getCustomData();
            if (customData == null) {
                customData = new BondCustomData();
                customData.setProductId(bond.getId());
                customData.setVersion(bond.getVersion());
                bond.setCustomData(customData);
            }
            customData.setHaircut_ecb(Double.parseDouble(bean.getECBHaircut()));
            bond.setSecCode(CollateralStaticAttributes.ECB_DISCOUNTABLE, "TRUE");
        } else {
            BondCustomData customData = (BondCustomData) bond.getCustomData();
            if (customData != null) {
                customData.setHaircut_ecb(null);
            }
            if (bond.getSecCodes() != null) {
                bond.getSecCodes().remove(CollateralStaticAttributes.ECB_DISCOUNTABLE);
            }
        }

        if (!Util.isEmpty(bean.getECBLiquidityClass())) {
            bond.setSecCode(CollateralStaticAttributes.ECB_LIQUIDITY_CLASS, bean.getECBLiquidityClass());
        } else {
            if (bond.getSecCodes() != null) {
                bond.getSecCodes().remove(CollateralStaticAttributes.ECB_LIQUIDITY_CLASS);
            }
        }

        if (!Util.isEmpty(bean.getECBAssetType())) {
            bond.setSecCode(CollateralStaticAttributes.ECB_ASSET_TYPE, bean.getECBAssetType());
        } else {
            if (bond.getSecCodes() != null) {
                bond.getSecCodes().remove(CollateralStaticAttributes.ECB_ASSET_TYPE);
            }
        }

    }

    private void removeECBAttributes(Bond bond) {
        BondCustomData customData = (BondCustomData) bond.getCustomData();
        if (customData != null) {
            customData.setHaircut_ecb(null);
        }
        if (bond.getSecCodes() != null) {
            bond.getSecCodes().remove(CollateralStaticAttributes.ECB_LIQUIDITY_CLASS);
            bond.getSecCodes().remove(CollateralStaticAttributes.ECB_DISCOUNTABLE);
            bond.getSecCodes().remove(CollateralStaticAttributes.ECB_ASSET_TYPE);
        }
    }

    @SuppressWarnings({"unchecked"})
    private Map<String, Bond> loadBondsMap() throws RemoteException {
        Map<String, Bond> bondsMap = new HashMap<>();
        String from = null;
        String where = " product_desc.product_family='Bond' ";
        Vector<Bond> allBonds = getDSConnection().getRemoteProduct().getAllProducts(null, where, null);
        for (Bond bond : allBonds) {
            bondsMap.put(getKey(bond.getSecCode("ISIN"), bond.getCurrency()), bond);
        }
        return bondsMap;
    }

    private String getKey(String isin, String ccy) {
        return isin + "_" + ccy;
    }

    private ECBAttributesBean buildBean(String line) {
        String[] fields = CollateralUtilities.splitMejorado(NUMBER_OF_FIELDS, getAttribute(ATT_SEPERATOR), false, line);
        for (int i = 0; i < fields.length; i++) {
            fields[i] = fields[i].trim();
        }
        return new ECBAttributesBean(fields);
    }

    /**
     * @param path
     * @param startFileName
     * @return the file name to import if every thing is okay (only one file found as expected and the content of the
     * file is correct)
     */
    private String getAndChekFileToProcess(String path, String startFileName) {
        String fileToProcess = "";
        ArrayList<String> files = CollateralUtilities.getListFiles(path, startFileName);
        // We check if the number of matche?s files is 1.
        if (files.size() == 1) {
            fileToProcess = files.get(0);
            this.fileName = fileToProcess;
            try {
                if (!feedPreProcess(path + fileToProcess)) {
                    Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Incorrect control line in the file");
                    ControlMErrorLogger.addError(ErrorCodeEnum.ControlLine, "Incorrect control line in the file");
                    return null;
                }
            } catch (Exception e) {
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, e);
                ControlMErrorLogger.addError(ErrorCodeEnum.UndefinedException, e.getLocalizedMessage());
                return null;
            }
        } else {

            Log.error(LOG_CATEGORY_SCHEDULED_TASK,
                    "The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem.");

            ControlMErrorLogger
                    .addError(ErrorCodeEnum.InputFileNotFound,
                            "The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem");
            return null;
        }
        return fileToProcess;
    }

    @Override
    public String getFileName() {
        return this.fileName;
    }

    @Override
    public String getTaskInformation() {
        return "Import CSV Bond ECBAttributes.";
    }

}
