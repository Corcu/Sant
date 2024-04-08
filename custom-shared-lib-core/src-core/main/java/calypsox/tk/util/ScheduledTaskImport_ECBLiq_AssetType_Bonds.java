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
import calypsox.tk.util.bean.ECBLiquidityClassAssetTypeBean;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.FileUtility;
import com.calypso.infra.util.Util;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.product.Bond;
import com.calypso.tk.service.DSConnection;

import java.io.*;
import java.rmi.RemoteException;
import java.util.*;

import static calypsox.tk.core.CollateralStaticAttributes.BOND_SEC_CODE_REF_INTERNA;
import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

public class ScheduledTaskImport_ECBLiq_AssetType_Bonds extends AbstractProcessFeedScheduledTask {
    private static final long serialVersionUID = 1L;

    // BAU - Use different way to import data in order to avoid multiple files problem
    private File file;

    private static final int NUMBER_OF_FIELDS = 3;

    private static final String ATT_SEPERATOR = "Separator";
    private static final String CONTROL_LINE = "Control Line";
    protected static final String ATT_SUMMARY_LOG = "Summary Log";
    protected static final String ATT_DETAILED_LOG = "Detailed Log";
    protected static final String ATT_FULL_LOG = "Full Log";

    // START CALYPCROSS-38 - mromerod
    /**
     * Attribute that indicates the number of threads to use
     */
    private static final int NUMBER_THREADS = 4; //everis
    private static final double NUMBER_THREADS_DOUBLE = 4; //everis P
    // END CALYPCROSS-38 - mromerod

    /**
     * ST Attributes Definition
     */
    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        // Gets superclass attributes
        attributeList.addAll(super.buildAttributeDefinition());

        attributeList.add(attribute(ATT_SEPERATOR));
        attributeList.add(attribute(CONTROL_LINE).booleanType());
        attributeList.add(attribute(ATT_SUMMARY_LOG));
        attributeList.add(attribute(ATT_DETAILED_LOG));
        attributeList.add(attribute(ATT_FULL_LOG));

        return attributeList;
    }

//	@SuppressWarnings("unchecked")
//	@Override
//	public Vector<String> getDomainAttributes() {
//		final Vector<String> attr = super.getDomainAttributes();
//		attr.add(ATT_SEPERATOR);
//		attr.add(CONTROL_LINE);
//		attr.add(ATT_SUMMARY_LOG);
//		attr.add(ATT_DETAILED_LOG);
//		attr.add(ATT_FULL_LOG);
//		return attr;
//	}

//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	@Override
//	public Vector getAttributeDomain(final String attribute, final Hashtable hashtable) {
//		Vector vector = new Vector();
//		if (CONTROL_LINE.equals(attribute)) {
//			vector.add("true");
//			vector.add("false");
//		}
//		return vector;
//	}

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public boolean isValidInput(final Vector messages) {
        boolean retVal = super.isValidInput(messages);

        final String seperator = getAttribute(ATT_SEPERATOR);
        if (Util.isEmpty(seperator)) {
            messages.addElement(ATT_SEPERATOR + " is not specified");
            retVal = false;
        }

        final String controlLine = getAttribute(CONTROL_LINE);
        if (Util.isEmpty(controlLine)) {
            messages.addElement(CONTROL_LINE + " is not specified");
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
            JDate fileDate = getValuationDatetime().getJDate(TimeZone.getDefault());

            // BAU - Use different way to import data in order to avoid multiple files problem
            this.file = lookForFile(path, startFileName, fileDate);

            if (this.file != null) {

                // Just after file verifications, this method will make a copy into the
                // ./import/copy/ directory
                FileUtility.copyFileToDirectory(path + this.file.getName(), path + "/copy/");

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

    private boolean processFile(String file) {
        BufferedReader reader = null;
        try {
            List<Bond> bondsNoIntRef = new ArrayList<Bond>();

            Map<String, Bond> bondsWithIntRef = loadBondsMap(bondsNoIntRef);
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

                if (!CollateralUtilities.checkFields(line, getAttribute(ATT_SEPERATOR).charAt(0), NUMBER_OF_FIELDS - 1)) {
                    Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Line number-" + lineNumber
                            + " : Number of fields didn't match, should  be " + NUMBER_OF_FIELDS);
                    continue;
                }

                ECBLiquidityClassAssetTypeBean bean = buildBean(line);
                Bond bond = bondsWithIntRef.get(bean.getInternalRef());
                if (bond == null) {
                    Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Line number-" + lineNumber
                            + " : Bond not found with REF_INTERNA" + bean.getInternalRef());
                    continue;
                }

                processLine(bond, bean);
                // START CALYPCROSS-38 - mromerod
//				try {
                // END CALYPCROSS-38 - mromerod
                bondsWithECB.put(bean.getInternalRef(), bond);
                // START CALYPCROSS-38 - mromerod
//					getDSConnection().getRemoteProduct().saveBond(bond, true);
//				} catch (RemoteException re) {
//					Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Line number-" + lineNumber
//							+ " : Error while saving the Bond with REF_INTERNA=" + bean.getInternalRef() + ", Error="
//							+ re.getLocalizedMessage());
//				}
                // END CALYPCROSS-38 - mromerod
            }

            // START CALYPCROSS-38 - mromerod
            // We prepare the threads to launch by segments
            SegmentThread[] intRefThread = manageSavingThreads(bondsWithECB.values(), false);

            // Remove ECB Attributes for the remaining Bonds.
            List<Bond> bondsWithNoECB = new ArrayList<Bond>(bondsWithIntRef.values());
            bondsWithNoECB.removeAll(bondsWithECB.values());
            bondsWithNoECB.addAll(bondsNoIntRef);

//			for (Bond bond : bondsWithNoECB) {
//				removeECBAttributes(bond);
//				try {
//					getDSConnection().getRemoteProduct().saveBond(bond, true);
//				} catch (RemoteException re) {
//					Log.error(
//							LOG_CATEGORY_SCHEDULED_TASK,
//							"Error saving Bond after removing ECB Attribute, REF_INTERNA="
//									+ bond.getSecCode(BOND_SEC_CODE_REF_INTERNA) + ", Currency=" + bond.getCurrency()
//									+ ", Error=" + re.getLocalizedMessage());
//				}
//			}

            // We prepare the threads to launch by segments
            SegmentThread[] noIntRefThread = manageSavingThreads(bondsWithNoECB, true);

            // We execute the previously prepared threads
            joinThreads(intRefThread);
            joinThreads(noIntRefThread);
            // END CALYPCROSS-38 - mromerod

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

    private void processLine(Bond bond, ECBLiquidityClassAssetTypeBean bean) {

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
            bond.getSecCodes().remove(CollateralStaticAttributes.ECB_ASSET_TYPE);
        }
    }

    @SuppressWarnings({"unchecked"})
    private Map<String, Bond> loadBondsMap(List<Bond> bondsNoIntRef) throws RemoteException {
        Map<String, Bond> bondsMap = new HashMap<>();
        String where = " product_desc.product_family='Bond' ";
        Vector<Bond> allBonds = getDSConnection().getRemoteProduct().getAllProducts(null, where, null);
        for (Bond bond : allBonds) {
            String refInterna = bond.getSecCode(BOND_SEC_CODE_REF_INTERNA);
            if (Util.isEmpty(refInterna)) {
                bondsNoIntRef.add(bond);
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Bond doesn't have REF_INTERNA. Bond Id =" + bond.getId());
            } else {
                // Check if the Bond already exists in the map. If so it is duplicate REF_INTERNA
                if (bondsMap.get(refInterna) != null) {
                    bondsNoIntRef.add(bond);
                    Log.error(
                            LOG_CATEGORY_SCHEDULED_TASK,
                            "Duplicate REF_INTERNA for Bond ids =" + bondsMap.get(refInterna).getId() + ", "
                                    + bond.getId());
                } else {
                    bondsMap.put(refInterna, bond);
                }
            }
        }
        return bondsMap;
    }

    private ECBLiquidityClassAssetTypeBean buildBean(String line) {
        String[] fields = CollateralUtilities.splitMejorado(NUMBER_OF_FIELDS, getAttribute(ATT_SEPERATOR), false, line);
        for (int i = 0; i < fields.length; i++) {
            fields[i] = fields[i].trim();
        }

        return new ECBLiquidityClassAssetTypeBean(fields);
    }

    // BAU - Use different way to import data in order to avoid multiple files problem

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

            if (JDate.diff(date, jdateFile) == 0) {
                return file;
            }

        }

        return null;

    }

    // BAU - Use different way to import data in order to avoid multiple files problem
    @Override
    public String getFileName() {
        return this.file.getName();
    }

    @Override
    public String getTaskInformation() {
        return "Import CSV Bond ECBAttributes.";
    }

    // START CALYPCROSS-38 - mromerod

    /**
     * Inner class to manage massive saving Bond threads
     *
     * @author everis
     */
    class SegmentThread extends Thread {

        private Object[] tradeArray;
        private boolean noIntRefs = false;
        private int start;
        private double start_double;
        private int end;
        private double end_double;

        /**
         * Thread segmentation method
         *
         * @param segment
         * @param tradeArray
         * @param noIntRefs
         */
        public SegmentThread(int segment, Object[] tradeArray, boolean noIntRefs) {
            this.tradeArray = tradeArray;
            this.noIntRefs = noIntRefs;
			
			/*Cambios en la l?gica -> c?lculos con doubles, resultados parseados a int. P
			 * this.start = (this.tradeArray.length / NUMBER_THREADS) * segment;
			this.end = Math.min(this.start + this.tradeArray.length/ NUMBER_THREADS, this.tradeArray.length);*/
            this.start_double = (this.tradeArray.length / NUMBER_THREADS_DOUBLE) * segment;
            this.end_double = Math.min(this.start_double + this.tradeArray.length / NUMBER_THREADS_DOUBLE, this.tradeArray.length);
            this.start = (int) this.start_double;
            this.end = (int) this.end_double;

        }

        public SegmentThread(int segment, Object[] tradeArray) {
            this(segment, tradeArray, false);

        }

        /**
         * Thread start method
         */
        @Override
        public void run() {
            //  Iterate and save
            for (int i = start; i < end; i++) {
                Bond bond = (Bond) tradeArray[i];

                if (bond != null) {
                    try {
                        if (noIntRefs) {
                            removeECBAttributes(bond);
                        }
                        getDSConnection().getRemoteProduct().saveBond(bond, true);
                    } catch (RemoteException re) {
                        Log.error(this, re); //sonar
                        if (noIntRefs) {
                            Log.error(
                                    LOG_CATEGORY_SCHEDULED_TASK,
                                    "Error saving Bond after removing ECB Attribute, REF_INTERNA="
                                            + bond.getSecCode(BOND_SEC_CODE_REF_INTERNA) + ", Currency=" + bond.getCurrency()
                                            + ", Error=" + re.getLocalizedMessage());
                        } else {
                            Log.error(LOG_CATEGORY_SCHEDULED_TASK,
                                    "Error while saving the Bond with REF_INTERNA="
                                            + bond.getSecCode(BOND_SEC_CODE_REF_INTERNA) + ", Error="
                                            + re.getLocalizedMessage());
                        }

                    }
                }
            }

        }

    }


    /**
     * Manage Saving threads
     *
     * @author everis
     */
    private SegmentThread[] manageSavingThreads(Collection<Bond> collection, boolean noIntRefs) {
        SegmentThread threads[] = new SegmentThread[NUMBER_THREADS];

        for (int i = 0; i < NUMBER_THREADS; i++) {
            threads[i] = null;

            if (collection.size() >= NUMBER_THREADS || i == 0) {
                threads[i] = new SegmentThread(i, collection.toArray(), noIntRefs);
            }
        }

        for (int i = 0; i < NUMBER_THREADS; i++) {
            if (threads[i] != null) {
                threads[i].start();
            }
        }

        return threads;


    }

    /**
     * Join Threads
     *
     * @param threads
     */
    private void joinThreads(SegmentThread[] threads) {
        try {
            for (int i = 0; i < NUMBER_THREADS; i++) {
                if (threads[i] != null) {
                    threads[i].join();
                }
            }

        } catch (InterruptedException e) {
            Log.error(this, "Thread interruption building threads");

        }
    }
    // END CALYPCROSS-38 - mromerod

}
