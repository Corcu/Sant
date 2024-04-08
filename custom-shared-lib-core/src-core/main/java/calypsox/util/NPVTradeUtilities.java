/*
 *
 * Copyright (c) ISBAN: Ingeniería de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.util;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.report.SantExcelReportViewer;
import calypsox.tk.report.SantTradeBrowserReportTemplate;
import calypsox.tk.util.SantImportMTMUtil;
import calypsox.tk.util.ScheduledTaskNPVOverrun.OverrunResult;
import calypsox.tk.util.bean.SantTradeBean;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.CollateralConfigCurrency;
import com.calypso.tk.refdata.MimeType;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.TradeArray;
import com.calypso.tk.util.email.MailException;
import com.santander.collateral.util.email.EmailMessage;
import com.santander.collateral.util.email.EmailSender;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import static calypsox.tk.core.SantPricerMeasure.S_NPV_BASE;
import static com.calypso.tk.core.PricerMeasure.S_MARGIN_CALL;
import static com.calypso.tk.core.PricerMeasure.S_NPV;

/**
 * Utils for the ST NPV Overrun, static methods
 *
 * @author Guillermo Solano
 * @version 1.0
 */
public class NPVTradeUtilities {

    // separators
    protected static String SEPARATOR = "\\|";
    public static final String COMMA = ",";

    // patterns to check the date format of field 9 - positionDate
    public final static Pattern datePattern = Pattern.compile("\\d{2}/\\d{2}/[1-3]\\d{3}");

    // default overrun reason
    public final static String OVERRUN_REASON = "Automatic MtM Reprocess by ST NPV Overrun";

    /**
     * Template Report of the SantTradeReport
     */
    private static String NPV_REPORT_TEMPLATE = "NPVOverrun_Template";

    /**
     * Report owner of the Template
     */
    public static final String TRADE_REPORT_NAME = "SantTradeBrowser";

    /**
     * As we don't know the exact order of the fields, this enum keeps this info
     */
    private enum FILE_FIELDS {

        MTM_DATE(0), // 1
        CURRENCY(1), // 2
        NPV_MTM(2), // 3
        EXTERNAL_REF(3), // 4
        BOOK(4);

        private final Integer pos;

        FILE_FIELDS(Integer i) {
            this.pos = i;
        }

        public Integer getPos() {
            return this.pos;
        }
    } // end enum

    /**
     * @param directory to check
     * @param filter    with the name of the file
     * @return the last file (that accepts the filter) modified in time
     */
    // note, the latest modified file is the newest one!
    public static File getLastModifiedFile(final File directory, FilenameFilter filter) {

        if (directory.exists() && directory.isDirectory()) {

            final File[] listFiles = directory.listFiles(filter);
            // useless if only one file
            if (listFiles.length < 1) {
                return null;
            }
            if (listFiles.length == 1) {
                if (listFiles[0].isDirectory()) {
                    return null;
                }
                return listFiles[0];
            }
            // take a sorted map to order files based on modification
            TreeMap<Long, File> sortedMap = new TreeMap<Long, File>();

            // we take all filenames of the directory and read their last modification time
            for (File file : listFiles) {

                if (file.isDirectory()) { // looking only for files
                    continue;
                }

                final long timeMil = file.lastModified();
                sortedMap.put(timeMil, file);
            }

            // this was the last one modified (newest)
            final Long fileTimeSelection = sortedMap.lastKey();
            return sortedMap.get(fileTimeSelection);
        }
        return null;
    }

    /**
     * Build the NPV bean if all info in the line is correct
     *
     * @param lineNumber
     * @param line
     * @param log
     * @return Bean SantTradeBean with required data to save
     */
    public static SantTradeBean buildNPVTradeBean(final Integer lineNumber, final String line,
                                                  final Collection<String> log) {

        if (Util.isEmpty(line)) {
            log.add("\nline: " + lineNumber + " is empty");
            return null;
        }

        String[] v = line.split(SEPARATOR);

        if (v.length < FILE_FIELDS.values().length) {
            log.add("\nline: " + lineNumber + " Missing fields. Line -> " + line);
            return null;
        }

        final String date = getCleanChunk(v, FILE_FIELDS.MTM_DATE.getPos());
        // check date field has a correct format
        if (!datePattern.matcher(date).matches()) {
            log.add("\nline: " + lineNumber + " Position format not valid = " + date + ". Line -> " + line);
            return null;
        }

        final String currency = getCleanChunk(v, FILE_FIELDS.CURRENCY.getPos());
        if (LocalCache.getCurrencyDefault(currency) == null) {
            log.add("\nline: " + lineNumber + " Currency = " + currency + " Not Found." + " Line -> " + line);
            return null;
        }
        String mtm = getCleanChunk(v, FILE_FIELDS.NPV_MTM.getPos());
        if (LocalCache.getCurrencyDefault(currency) == null) {
            log.add("\nline: " + lineNumber + " NPV value is EMPTY. Line -> " + line);
            return null;
        }

        // in case commas as separators
        if (mtm.contains(COMMA)) {
            mtm = mtm.replaceAll(COMMA, ".");
        }

        Double d = -1.0d;
        try {
            d = Double.parseDouble(mtm);
        } catch (NumberFormatException e) {
            log.add("\nline: " + lineNumber + " NPV value = " + mtm + " Not a number. Line -> " + line);
            return null;
        }

        final String extRef = getCleanChunk(v, FILE_FIELDS.EXTERNAL_REF.getPos());
        if (Util.isEmpty(extRef)) {
            log.add("\nline number: " + lineNumber + " External Reference is empty. Line -> " + line);
            return null;
        }

        final String book = getCleanChunk(v, FILE_FIELDS.BOOK.getPos());
        if (Util.isEmpty(book)) {
            log.add("\nline number: " + lineNumber + " Book is empty. Line -> " + line);
            return null;
        }

        SantTradeBean b = new SantTradeBean();
        b.setNPVDate(date);
        b.setNPVCcy(currency);
        b.setNPV(d);
        b.setExternalReference(extRef);
        b.setLineNumber(lineNumber);
        b.setBook(book);

        return b;
    }

    /**
     * @param s
     * @param pos
     * @return clean fields with no besides blank spaces
     */
    private static String getCleanChunk(String[] s, Integer pos) {

        String prov = s[pos];
        if (!Util.isEmpty(prov)) {
            return prov.trim();
        }
        return "";
    }

    /**
     * Generates a Map with the trades read from the External reference to improve performance
     *
     * @param npvTrades
     * @param log
     * @return Map list associating external references to a list of trades
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Map<String, List<Trade>> initTradeMap(Map<String, SantTradeBean> npvSantTradesBeanMap,
                                                        final Collection<String> log) {

        Map<String, List<Trade>> tradeMap = new HashMap<String, List<Trade>>();
        // 1. get list of ExtRefs from Excel
        List<String> extRefList = new ArrayList<String>();
        // start log phase:
        log.add("\nStarting recovering Calypso Trades");

        for (Map.Entry<String, SantTradeBean> entry : npvSantTradesBeanMap.entrySet()) {
            extRefList.add(entry.getKey());
        }
        // 2. Load Trades Map.
        if (!Util.isEmpty(extRefList)) {
            List<List<String>> splitCollection = CollateralUtilities.splitCollection(extRefList, 999);
            for (List<String> subList : splitCollection) {
                if (Util.isEmpty(subList)) {
                    continue;
                }
                final String where = " trade.trade_status<>'CANCELED' and  trade.external_reference in "
                        + Util.collectionToSQLString(subList);
                TradeArray tradeArray = null;
                try {
                    tradeArray = DSConnection.getDefault().getRemoteTrade().getTrades(null, where, null, null);
                } catch (RemoteException e) {
                    Log.error(NPVTradeUtilities.class, "Error reading DS to retrieve trades where: " + where);
                    Log.error(NPVTradeUtilities.class, e); //sonar
                    log.add("\nError reading DS to retrieve trades where: " + where);
                }

                if (!Util.isEmpty(tradeArray)) {
                    Iterator iterator = tradeArray.iterator();

                    while (iterator.hasNext()) {

                        final Trade trade = (Trade) iterator.next();
                        List<Trade> list = tradeMap.get(trade.getExternalReference());

                        if (list == null) {
                            list = new ArrayList();
                            tradeMap.put(trade.getExternalReference(), list);
                        }
                        list.add(trade);
                    }
                }
            }
        }

        log.add("\nTotal Trades Found: " + tradeMap.keySet().size());
        log.add("\nExt. Reference Ids: " + Util.collectionToString(tradeMap.keySet(), ","));

        // link tradeMap Trade with each SantTradeBean
        for (Map.Entry<String, List<Trade>> entry : tradeMap.entrySet()) {

            final String externalRef = entry.getKey();

            if (npvSantTradesBeanMap.containsKey(externalRef)) {

                final List<Trade> list = entry.getValue();
                final SantTradeBean b = npvSantTradesBeanMap.get(externalRef);

                if (b != null) {
                    // search for book if more than one trade found
                    b.setTradeList(cleanTradesByBook(b.getBook(), list));
                }
            }
        }
        log.add("\nCalypso Trades Successfully recovered.");
        return tradeMap;
    }

    /**
     * @param book
     * @param list with trades
     * @return a list with the trades que matches the book of the bean
     */
    private static List<Trade> cleanTradesByBook(final String book, final List<Trade> list) {

        // more than one trade for this External reference
        if (list.size() > 2) {

            ArrayList<Trade> clean = new ArrayList<Trade>(list.size());
            // last try to find a match with the book
            for (Trade t : list) {

                if (t.getBook().getName().equals(book)) {
                    clean.add(t);
                }
            }

            // if at least one match with book
            if (clean.size() != list.size()) {
                return clean;
            }
        }
        return list;
    }

    /**
     * @param tradeMap
     * @param npvTradesMap
     * @param useContractCCY
     * @param log
     * @return a Set of trades whose MtM have changed
     */
    public static HashSet<Trade> processPlMarks(Map<String, List<Trade>> tradeMap,
                                                Map<String, SantTradeBean> npvTradesMap, boolean useContractCCY, Collection<String> log) {

        final JDate processDateToday = JDate.getNow().addBusinessDays(-1,
                LocalCache.getCurrencyDefault("EUR").getDefaultHolidays());

        PricingEnv pricingEnv = null;
        String pricingEnvName = "";
        log.add("\n\nStarting processing PlMarks.");

        try {
            pricingEnv = DSConnection.getDefault().getRemoteMarketData()
                    .getPricingEnv(DSConnection.getDefault().getDefaultPricingEnv(), new JDatetime(processDateToday, TimeZone.getDefault()));
        } catch (RemoteException e) {
            Log.error(NPVTradeUtilities.class, "Error retrieving Pricing Env from DB. \n" + e); //sonar
            pricingEnvName = "DirtyPrice";
        }
        pricingEnvName = pricingEnv.getName();
        final HashMap<Trade, PLMark> plMarks = new HashMap<Trade, PLMark>();
        final ArrayList<String> errorMessages = new ArrayList<String>();
        final HashSet<Trade> tradesToSave = new HashSet<Trade>();

        // constants for trades overruns
        final String markNameBase = S_NPV_BASE;
        final String reason = OVERRUN_REASON;
        final Double hairCut = 100.0d;

        // for each SantTradeBean
        for (Map.Entry<String, SantTradeBean> entry : npvTradesMap.entrySet()) {

            final SantTradeBean bean = entry.getValue();
            Trade trade = null;

            // no valid bean data, log and continue
            if (!isValidInput(bean, tradeMap)) {
                if (bean.hasError()) {
                    log.addAll(bean.getErrors());
                }
                continue;
            }

            trade = bean.getTrade();
            errorMessages.clear();
            final Double valueBase = Double.valueOf(bean.getNPV());
            String mtmDate = bean.getNPVDate();
            JDate processDate = JDate.valueOf(mtmDate);

            // All the checks have now been done. retreive PLMarks from DB if exists,
            // set new value, calculate MARGIN_CALL if the markName is NPV
            PLMark currentPLMark = null;
            PLMarkValue pLMarkValueBase = null;
            try {
                if (plMarks.get(trade) != null) {
                    currentPLMark = plMarks.get(trade);
                } else {
                    currentPLMark = CollateralUtilities.createPLMarkIfNotExists(trade, DSConnection.getDefault(),
                            pricingEnvName, processDate);

                    plMarks.put(trade, currentPLMark);
                }
            } catch (final RemoteException e1) {
                final String error = "\nError retrieving PLMark for trade ExternalRef=" + trade.getExternalReference();
                Log.error(NPVTradeUtilities.class, error, e1);
                log.add(error);
            }

            //v14 GSM 16/05/2016 - fix to use contract base ccy instead of mercury
            String ccy = bean.getNPVCcy();

            if (useContractCCY) {

                final CollateralConfig contract = getContract(trade);
                if (contract != null)
                    ccy = contract.getCurrency();

                Log.info(NPVTradeUtilities.class, "Using contract ccy for contract " + contract.getId() + " ccy " + ccy);
            } else
                Log.info(NPVTradeUtilities.class, "Using mercury ccy for NPV_BASE, ccy " + ccy);

            //SETS NPV_BASE VALUE AAP
            pLMarkValueBase = CollateralUtilities.buildPLMarkValue(markNameBase, bean.getNPVCcy(), valueBase, reason);
            pLMarkValueBase = SantImportMTMUtil.convertPLMarkValueToTargetCurrency(pLMarkValueBase, pricingEnv, ccy, processDate, errorMessages, false);
            //Hot fix... sucks
            pLMarkValueBase.setCurrency(ccy);
            SantImportMTMUtil.adjustAndAddPLMarkValue(plMarks, trade, pLMarkValueBase, pricingEnvName, processDate);

            final PLMarkValue plmValueTradeCcy = SantImportMTMUtil.convertPLMarkValueToTradeCcy(pLMarkValueBase,
                    pricingEnv, trade.getTradeCurrency(), processDate, errorMessages);

            // Display Error messages after trades have been saved
            if (errorMessages.size() > 0) {
                final String error = "\nError retrieving trade ExternalRef = " + trade.getExternalReference()
                        + errorMessages;
                log.add(error);
                bean.addError(error);
                plMarks.remove(trade);
                continue;
            }

            SantImportMTMUtil.adjustAndAddPLMarkValue(plMarks, trade, plmValueTradeCcy, pricingEnvName, processDate);

            if (markNameBase.equals(S_NPV_BASE)) {
                // Handle unsettled Trades
                CollateralUtilities.handleUnSettledTrade(trade, valueBase, processDate);
                // calculate MARGIN_CALL if the markName is NPV
                if (trade.getProductType().equals(CollateralStaticAttributes.SEC_LENDING)
                        || trade.getProductType().equals(CollateralStaticAttributes.REPO)
                        || trade.getProductType().equals(CollateralExposure.PRODUCT_TYPE)) {
                    try {
                        // GSM: 06/05/2014 - Adaptation to CollateralExposure.SECURITY_LENDING
                        if (trade.getProductSubType().equals("SECURITY_LENDING") && (hairCut != 0.0d)) {
                            trade.addKeyword(CollateralStaticAttributes.FO_HAIRCUT, hairCut.toString());
                        }

                        // Calculate MARGIN_CALL
                        final PLMarkValue mcPLMarkValue = CollateralUtilities.calculateMARGIN_CALL(
                                DSConnection.getDefault(), trade, plmValueTradeCcy, pricingEnvName, processDate,
                                hairCut, errorMessages);

                        SantImportMTMUtil.adjustAndAddPLMarkValue(plMarks, trade, mcPLMarkValue, pricingEnvName,
                                processDate);

                        // GSM: 06/05/2014 - Adaptation to CollateralExposure.SECURITY_LENDING
                        if ((hairCut != null)
                                && ((trade.getProductType().equals(CollateralStaticAttributes.SEC_LENDING) || trade
                                .getProductSubType().equals("SECURITY_LENDING")))) {

                            final String trdHairCutStr = trade.getKeywordValue(CollateralStaticAttributes.FO_HAIRCUT);
                            double trdHairCut = 0.0;

                            if (!Util.isEmpty(trdHairCutStr)) {
                                trdHairCut = Double.parseDouble(trdHairCutStr);
                            }
                            if (trdHairCut != hairCut) {
                                trade.addKeyword(CollateralStaticAttributes.FO_HAIRCUT, hairCut);
                            }
                        }
                    } catch (final Exception e) {
                        Log.error(NPVTradeUtilities.class, e);
                    }
                }
            }
            // Finally save all trades.
            tradesToSave.add(trade);

            // set MC id in the Trade Bean
            if (trade.getKeywordAsInt(CollateralStaticAttributes.MC_CONTRACT_NUMBER) != 0) {
                bean.setConfigId(trade.getKeywordAsInt(CollateralStaticAttributes.MC_CONTRACT_NUMBER));
            }
        }
        // end processing while Trades
        errorMessages.clear();
        // We built all plMarks. Now save all of them.
        try {
            final Iterator<PLMark> iterator = plMarks.values().iterator();
            while (iterator.hasNext()) {
                PLMark plmark = iterator.next();
                PLMark copy = plmark;
                plmark = insertDummyPLMark(plmark, pricingEnvName, processDateToday);
                if (plmark != null) {
                    //fix v14: 16/05/2016
                    //DSConnection.getDefault().getRemoteMarketData().savePLMark(plmark);
                    try {
                        DSConnection.getDefault().getRemoteMark().saveMarksWithAudit(Arrays.asList(plmark), true);
                    } catch (PersistenceException e) {
                        Log.error(NPVTradeUtilities.class, "Not able to save PlMark for trade id " + copy.getTradeLongId());
                        Log.error(NPVTradeUtilities.class, e); //sonar
                    }
                } else {
                    Log.error(NPVTradeUtilities.class, "Not able to save PlMark for trade id " + copy.getTradeLongId());
                }
            }
        } catch (final RemoteException e) {
            errorMessages.add(e.getMessage());
            Log.error(NPVTradeUtilities.class, e);
        }

        // Display Error messages
        if (errorMessages.size() > 0) {
            log.add("\nAbove errors occured while saving Pricer Measures.\n");
        } else {
            log.add("\nSaved Pricer Measures Successfully.");

        }
        return tradesToSave;
    }

    /**
     * @param trade
     * @return recover contract from the trade
     */
    private static CollateralConfig getContract(final Trade trade) {

        try {
            int contractId = trade.getKeywordAsInt(CollateralStaticAttributes.MC_CONTRACT_NUMBER);
            if (contractId == 0) {
                contractId = Integer.parseInt(trade.getInternalReference());
            }

            if (contractId > 0) {

                final CollateralConfig marginCallConfig = CacheCollateralClient
                        .getCollateralConfig(DSConnection.getDefault(), contractId);

                if (marginCallConfig != null)
                    return marginCallConfig;
            }

        } catch (NumberFormatException e) {
            Log.error(NPVTradeUtilities.class, "Cannot convert number from trade: " + trade.getLongId());
        }

        return null;
    }

    /**
     * Verifies the Bean has coherent data: points to one Trade ID, status is valid, ccy exists, has a MC contract
     * linked and CCy is an eligible ccy in the contract
     *
     * @param bean
     * @param tradeMap
     * @return true if the bean can be processed
     */
    private static boolean isValidInput(final SantTradeBean bean, final Map<String, List<Trade>> tradeMap) {

        // Check externalRef
        String externalRef = bean.getExternalReference();
        Trade trade = bean.getTrade();

        if (trade == null) {
            if (bean.moreTrades4ExtRef()) {
                bean.addError("There are more than 1 Trade for the ExternalRef " + externalRef);
            } else {
                bean.addError("Trade not found for ExternalRef " + externalRef + " in the file ");
            }
            return false;
        }

        final JDate processDate = Util.stringToJDate(bean.getNPVDate());

        // check status and process date
        if ((trade.getStatus().getStatus().equals(Status.CANCELED))
                || (trade.getStatus().getStatus().equals(Status.MATURED) && trade.getMaturityDateInclFees().before(processDate))) {

            bean.addError("Invalid/non-live Trade for ExternalRef " + externalRef + " in the file ");
            return false;

        } else if (trade.getProductType().equals(Repo.REPO) || trade.getProductType().equals(SecLending.SEC_LENDING)
                || trade.getProductType().equals(CollateralExposure.PRODUCT_TYPE)) {

            if (trade.getTradeDate().getJDate(TimeZone.getDefault()).after(processDate)) {

                bean.addError("Process date is earlier than Trade Date for ExternalRef " + externalRef
                        + " in the file ");
                return false;
            }
        }
        // Check Ccy
        final String ccy = bean.getNPVCcy();
        final Vector<String> validCurrencies = LocalCache.getDomainValues(DSConnection.getDefault(),
                CollateralStaticAttributes.DOMAIN_CURRENCY);
        if (!validCurrencies.contains(ccy)) {

            bean.addError("Invalid Currency for ExternalRef " + externalRef + " in the file ");
            return false;

        } else {
            final int contractId = trade.getKeywordAsInt(CollateralStaticAttributes.MC_CONTRACT_NUMBER);
            if (contractId == 0) {

                bean.addError("Trade is not indexed to any contract for ExternalRef " + externalRef + " in the file ");
                return false;
            }
            final CollateralConfig marginCallConfig = CacheCollateralClient.getCollateralConfig(
                    DSConnection.getDefault(), contractId);
            if (marginCallConfig == null) {

                bean.addError("MarginCall Contract for Trade is not found for ExternalRef " + externalRef
                        + " in the file ");
                return false;
            }
            // check to ensure that the Trade CCY is at least an eligible ccy by the contract
            boolean acceptedCCyByMC = false;

            // check base currency || ANY
            if (ccy.equals(marginCallConfig.getCurrency()) || (marginCallConfig.getCurrencyList() == null)) {
                acceptedCCyByMC = true;

            } else {
                // check eligible currencies
                for (CollateralConfigCurrency mcCcy : marginCallConfig.getEligibleCurrencies()) {
                    if ((mcCcy != null) && mcCcy.getCurrency().equals(ccy)) {
                        acceptedCCyByMC = true;
                        break;
                    }
                }
            }
            // check eligible admitted currencies in details
            if (!acceptedCCyByMC && (marginCallConfig.getCurrencyList() != null)) {

                for (String mcCurrency : marginCallConfig.getCurrencyList()) {
                    if (mcCurrency.equals(ccy)) {
                        acceptedCCyByMC = true;
                        break;
                    }
                }
            }

            if (!acceptedCCyByMC) {

                bean.addError("\nError CCY for ExternalRef = " + trade.getExternalReference()
                        + "[currency MUST be admitted by the contract " + marginCallConfig.getCurrency() + "]");
                return false;
            }
        }
        // everything ok
        return true;
    }

    /**
     * Saves all the trades in parallel as AMEND
     *
     * @param tradesToSave
     * @param log
     */
    public static void saveModifiedTrades(HashSet<Trade> tradesToSave, final Collection<String> log) {

        log.add("\n\nStarting saving of modified & Unsettled Trades.");
        // We also need to save trades modified and unsettled Trades.
        final ArrayList<String> errorMessages = new ArrayList<String>(log);
        ExecutorService executor = SantImportMTMThreadPolFactory.getBoundedQueueThreadPoolExecutor(
                SantImportMTMThreadPolFactory.calculateThredPoolSize(tradesToSave), errorMessages);

        for (final Trade tradeTemp : tradesToSave) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        // amend
                        tradeTemp.setAction(com.calypso.tk.core.Action.AMEND);
                        DSConnection.getDefault().getRemoteTrade().save(tradeTemp);
                    } catch (final RemoteException e) {
                        errorMessages.add("\nFailed to save the Trade , externalRef="
                                + tradeTemp.getExternalReference() + ", TradeId=" + tradeTemp.getLongId() + ". "
                                + e.getLocalizedMessage());
                        Log.error(this.getClass().getName(), e);
                    }
                }
            });
        }
        SantImportMTMThreadPolFactory.shutdownExecutor(executor);
        log.add("\nSuccessfully Saved Trades (Amend Action).");
    }

    /**
     * It inserts a Zero value PLMarkValue for NPV, NPV_BASE and MARGIN_CALL if it is the first time we are saving
     * plmarks for the trade. Reason is that users want, anything saved in the MTMBlotter, to see in MTMVariation and
     * MTMAudit reports.
     *
     * @param plmark
     * @return dummy PlMakr
     * @throws RemoteException
     */
    @SuppressWarnings("deprecation")
    private static PLMark insertDummyPLMark(PLMark plmark, String priceEnvName, JDate processDate)
            throws RemoteException {

        if ((plmark.getId() != 0) || (CollateralUtilities.retrievePLMarkValue(plmark, S_NPV_BASE) == null)) {
            return plmark;
        }
        // save a dummy Zero PLMarkValue for NPV, NPV_BASE and MARGIN_CALL so when you save the actual one, becomes an
        // AMEND and an audit line is created which is used in MTMVariation and MTMAudit reports.
        PLMarkValue origNpvBasePLMarkValue = CollateralUtilities.retrievePLMarkValue(plmark, S_NPV_BASE);
        PLMarkValue origNpvPLMarkValue = CollateralUtilities.retrievePLMarkValue(plmark, S_NPV);
        PLMarkValue origMarginCallPLMarkValue = CollateralUtilities.retrievePLMarkValue(plmark, S_MARGIN_CALL);

        double npvBase = origNpvBasePLMarkValue.getMarkValue();
        double npv = origNpvPLMarkValue.getMarkValue();
        double marginCall = origMarginCallPLMarkValue.getMarkValue();

        origNpvBasePLMarkValue.setMarkValue(0.0);
        origNpvPLMarkValue.setMarkValue(0.0);
        origMarginCallPLMarkValue.setMarkValue(0.0);
        DSConnection.getDefault().getRemoteMarketData().savePLMark(plmark);

        PLMark newPLMark = CollateralUtilities.retrievePLMark(plmark.getTradeLongId(), DSConnection.getDefault(),
                priceEnvName, processDate);

        origNpvBasePLMarkValue.setMarkValue(npvBase);
        origNpvPLMarkValue.setMarkValue(npv);
        origMarginCallPLMarkValue.setMarkValue(marginCall);

        SantImportMTMUtil.adjustPLMarkValues(newPLMark, origNpvBasePLMarkValue);
        SantImportMTMUtil.adjustPLMarkValues(newPLMark, origNpvPLMarkValue);
        SantImportMTMUtil.adjustPLMarkValues(newPLMark, origMarginCallPLMarkValue);

        return newPLMark;
    }

    /**
     * Builds the list of report (for each 999 trades) for the trades which PlMark have been changed
     *
     * @param result
     * @param template2
     * @param currentLog
     * @return report SantTradesBrowser
     */
    public static List<byte[]> buildBinaryReport(final OverrunResult result, String template2,
                                                 final JDatetime jdatetime, final Collection<String> currentLog) {

        currentLog.add("\n Generating report attachment.");

        final List<byte[]> reportList = new ArrayList<byte[]>();
        PricingEnv pricingEnv = null;
        DSConnection server = DSConnection.getDefault();
        try {
            pricingEnv = server.getRemoteMarketData().getPricingEnv(server.getUserDefaults().getPricingEnvName(),
                    new JDatetime());
        } catch (RemoteException e1) {
            Log.error(NPVTradeUtilities.class, e1);
            currentLog.add("\n Error DS recovering PricingEnv.");
        }
        Integer i = 1;
        // in case we request a report with more than 999 trades ids, we build several reports
        List<List<Long>> splitCollection = CollateralUtilities.splitCollection(result.getTradesOk(), 999);
        for (List<Long> subList : splitCollection) {

            final String templateName = Util.isEmpty(template2) ? NPV_REPORT_TEMPLATE : template2.trim();
            ReportTemplateName reortTemplateName = new ReportTemplateName(templateName);
            ReportTemplate template = BOCache.getReportTemplate(DSConnection.getDefault(), TRADE_REPORT_NAME,
                    reortTemplateName);

            final StringBuffer tradesList = new StringBuffer();
            for (Long t : subList) {
                tradesList.append(t);
                tradesList.append(COMMA);
            }

            if (tradesList.length() > COMMA.length()) {
                tradesList.delete(tradesList.length() - COMMA.length(), tradesList.length());
            }
            JDate prevWorkingDay = JDate.valueOf(jdatetime).addBusinessDays(-1, Util.string2Vector("SYSTEM"));
            // template values
            // As many reports as sublist of 999 Trades, DS cannot accept more item in each query
            template.put(TradeReportTemplate.TRADE_ID, tradesList.toString());
            template.put(SantTradeBrowserReportTemplate.VAL_DATE_FROM, prevWorkingDay);
            template.put(SantTradeBrowserReportTemplate.VAL_DATE_TO, prevWorkingDay);
            // generate report
            Report report = Report.getReport(TRADE_REPORT_NAME);
            report.setReportTemplate(template);
            report.setValuationDatetime(jdatetime);
            report.setPricingEnv(pricingEnv);
            ReportOutput output = report.load(new Vector<String>());

            SantExcelReportViewer viewer = new SantExcelReportViewer();
            output.format(viewer);

            try {
                reportList.add(viewer.getContentAsBytes());
                currentLog.add("\n Report " + i++ + " Successfully generated.");

            } catch (IOException e) {
                Log.error(NPVTradeUtilities.class, e);
                currentLog.add("\n Error generating report" + i++ + " attachment.");
            }
        }
        return reportList;
    }

    /**
     * Builds the email, attachs the reports and calls the email service
     *
     * @param emailsToSend
     * @param report
     * @param currentLog
     * @param result
     */
    public static void sendEmail(final List<String> emailsToSend, final List<byte[]> reportList,
                                 final Collection<String> currentLog, OverrunResult result) {

        ArrayList<String> log = new ArrayList<String>();
        // build the email message
        final EmailMessage email = new EmailMessage();
        // set the email properties
        email.setFrom("calypso@gruposantander.com");
        email.setTo(emailsToSend);
        email.setSubject("Automatic Overrun Confirmation");
        // email body
        final StringBuffer emailBody = new StringBuffer(
                "\nThe processing of the automatic MtM/NPV Overrun is completed. The process result file is attached.\n");

        // We had error processing some trades
        if (!result.getTradesKO().isEmpty() || !result.getMoreThan1Trade().isEmpty()) {
            emailBody.append(processingErrors(result, log));
        }

        Integer i = 1;
        for (byte[] report : reportList) {

            // send report with the results of the overruns
            if (report != null) {
                AdviceDocument resultDocument = new AdviceDocument();
                resultDocument.setId(0);
                resultDocument.setMimeType(new MimeType("application/vnd.ms-excel"));
                resultDocument.setDocument(null);
                resultDocument.setTemplateName("Overrun Trades & Contracts " + i++ + ".xlsx");
                resultDocument.setBinaryDocument(report);
                email.addAttachment(resultDocument.getMimeType().getType(), getNotificationFileName(resultDocument),
                        resultDocument.getBinaryDocument());
            } else {
                emailBody.append("\n\n Attachment ERROR. See Log for more details.");
            }
        }

        final String emailFinalBody = emailBody.toString().replaceAll("\\n", " <br>");
        email.setText(emailFinalBody);

        // send the email
        try {
            EmailSender.send(email);
        } catch (MailException e) {
            Log.error(NPVTradeUtilities.class, e);
            log.add("\n" + e.getLocalizedMessage() + " .Mail Exception, check server configuration. ");
        }

        currentLog.clear();
        currentLog.addAll(log);
    }

    /**
     * Appends to the log the external references that link to many trades ids
     *
     * @param result
     * @param log
     */
    private static StringBuffer processingErrors(final OverrunResult result, ArrayList<String> log) {

        final StringBuffer sb = new StringBuffer();

        if (result.getMoreThan1Trade().isEmpty() && result.getTradesKO().isEmpty()) {
            return sb;
        }
        sb.append("\nNOTICE: Some errors occurred during NPV import processing");

        // info with duplicates trades for one external Reference
        for (Map.Entry<String, List<Long>> entry : result.getMoreThan1Trade().entrySet()) {

            sb.append("External Ref: ");
            sb.append(entry.getKey());
            sb.append(" has DUPLICATES trades Ids -> ");
            for (Long i : entry.getValue()) {
                sb.append(i + COMMA);
            }
            sb.delete(sb.length() - COMMA.length(), sb.length());
            sb.append("\n");
        }
        // not elegant to put it here, but list of duplicate ids already done for the email
        log.add(sb.toString());
        // other errors detail
        for (String ref : result.getTradesKO()) {
            final SantTradeBean error = result.getBeansMap().get(ref);
            if (error != null) {
                for (String s : error.getErrors()) {
                    sb.append(s);
                }
            }
        }
        return sb;
    }

    /**
     * @return a file name with the right extention
     */
    private static String getNotificationFileName(final AdviceDocument document) {
        String fileName = "";
        if (document != null) {
            fileName = document.getTemplateName();
            final int extentionStart = fileName.lastIndexOf('.');
            if (extentionStart > 0) {
                final MimeType mime = document.getMimeType();
                if ((mime != null) && "PDF".equals(mime.getType())) {
                    fileName = fileName.substring(0, extentionStart + 1) + "pdf";
                }
            }
        }
        return fileName;
    }

    /**
     * @return the sEPARATOR
     */
    public static String getSEPARATOR() {
        return SEPARATOR;
    }

    /**
     * @param sEPARATOR the sEPARATOR to set
     */
    public static void setSEPARATOR(final String sEPARATOR) {
        SEPARATOR = sEPARATOR.trim();
        if (!sEPARATOR.contains("\\")) {
            SEPARATOR = "\\" + SEPARATOR;
        }
    }

}
