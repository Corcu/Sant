package calypsox.util.collateral;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.core.SantPricerMeasure;
import calypsox.tk.refdata.SantCreditRatingStaticDataFilter;
import calypsox.util.*;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Fee;
import com.calypso.tk.bo.workflow.TradeWorkflow;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.command.ExecutionContext;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.filter.CollateralFilterProxy;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.filter.impl.CachedCollateralFilterProxy;
import com.calypso.tk.collateral.manager.worker.CollateralTaskWorker;
import com.calypso.tk.collateral.optimization.candidat.CollateralCandidate;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.CreditRating;
import com.calypso.tk.marketdata.HaircutProxy;
import com.calypso.tk.marketdata.HaircutProxyFactory;
import com.calypso.tk.marketdata.MarginCallCreditRating;
import com.calypso.tk.marketdata.MarginCallCreditRatingConfiguration;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.CurrencyDefault;
import com.calypso.tk.refdata.CurrencyPair;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.email.MailException;
import com.santander.collateral.util.email.EmailMessage;
import com.santander.collateral.util.email.EmailSender;
import com.santander.collateral.util.email.SantanderEmailSender;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.calypso.tk.core.PricerMeasure.S_INDEPENDENT_AMOUNT;
import static com.calypso.tk.core.PricerMeasure.S_MARGIN_CALL;
import static com.calypso.tk.core.PricerMeasure.S_NPV;

/**
 * Several utilities for Calypso Collateral in Banco Santander
 *
 * @author several authors
 * @version 4.0ContractBaseCcyFeeCalculator.java
 */
public class CollateralUtilities {

    private static HashMap<String, String> productLegNumber = new HashMap<>();
    private static HashMap<String, String> productSubTypeMapping = new HashMap<>();
    private static Map<String, String> domainValueCommentMap = new HashMap<>();
    public static final String PRODUCT_MAPPING_DOMAIN_VALUE = "ColExpProductsMapping";
    private static final String PRODUCT_LEG_NB_DOMAIN_VALUE = "CollateralExposure.subtype";
    private static final String DAILY_DATE_RULE = "@Daily Date Rule";
    private static final String CALC_PERIOD_VALUE = "1D";
    private static final String CASH = "CASH";
    private static final String CASH_VALUE = "Cash";
    private static final String SECURITY = "SECURITY";
    private static final String SECURITY_VALUE = "Securities";
    private static final String BOTH = "BOTH";
    private static final String BOTH_VALUE = "Cash/Securities";
    private static final String HEAD = "HEAD";
    private static final String HEAD_VALUE = "Head";
    private static final String CLONE = "CLONE";
    private static final String CLONE_VALUE = "Clone";
    private static final String ALIAS_ENTITY_KGR = "ALIAS_ENTITY_KGR";
    private static final String ISIN = "ISIN";
    // GSM 23/07/15. SBNA Multi-PO filter, attribute name added by
    // ScheduledTaskCSVREPORT
    public static final String PROCESS_BY_ST = "ReportProcessedByST";

    protected static final String mtmDateFormat = "dd/MM/yyyy";
    private static HashMap<String, PricingEnv> pricingEnvsCache = new HashMap<>();

    // GSM: 26/06/2013
    private static final String LONG_APPEND_BOOK_NAME = "_LONG";
    private static final String LEGAL_ENTITY_ROLE_PROCESSING_ORG = "ProcessingOrg";
    // AAP 11/02/2016 PLMARK TYPES
    private static final String PLTYPE = "PL";
    private static final String NONETYPE = "NONE";
    // AAP 29/03 Migrated from remoteService
    private static final Object COLLATERAL_EXP_SEC_LENDING = "SECURITY_LENDING";
    // GSM: 03/05/2016 - v14 - Logic to add keyword from DV to
    private static final String EXPOSURE_DV_KEYWORD_CM_AFTER_MATURITY_NAME =
            "ExposureInterfaceTradeKeywordAfterMaturity";

    public static final String PROCESSING_ORG_NAMES = "ProcessingOrg";
    public static final String PROCESSING_ORG_IDS = "OWNER_AGR_IDS";
    
    private static int SQL_GET_SIZE = 999;
    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();

    /**
     * Method to map the book between the source system and Calypso, using the book received from the
     * source. First tries to match with the long book alias, in case it fails it tries the short name
     * (appending _CORTO to the book alias).
     *
     * @param sourceBook      Book in the source system, like SUSI or GBO to map to the book in Calypso.
     * @param sourceAliasName Alias used to map the book from the source to the book for Calypso.
     * @return The calypso book if it was found.
     */
    // GSM: 14/06/2013. Short Portfolio DEV. Read first short name, if not read
    // long
    public static Book mapBookShortOrLongAlias(final String sourceBook, final String sourceAliasName)
            throws Exception {

        if ((sourceBook == null)
                || (sourceAliasName == null)
                || sourceBook.isEmpty()
                || sourceAliasName.isEmpty()) {
            return null;
        }

        String aliasShort = "";
        String aliasLong = "";

        // just checks if the alias ends with the long prefix. Builds the
        // aproppiate alias for each case
        if (sourceAliasName.contains(LONG_APPEND_BOOK_NAME)) // first short name
        {
            aliasLong = sourceAliasName.trim();
            aliasShort = sourceAliasName.replace(LONG_APPEND_BOOK_NAME, "").trim();

        } else {

            aliasLong = sourceAliasName.trim() + LONG_APPEND_BOOK_NAME;
            aliasShort = sourceAliasName.trim();
        }

        try {
            // GSM: 13/06/2013. Long/Short Portfolio Development.
            // Read first short name
            return mapBook(sourceBook, aliasShort);

        } catch (Exception e) {

            try {
                // In case short is not found, try long name
                return mapBook(sourceBook, aliasLong);

            } catch (Exception e1) {

                if (e1 instanceof RemoteException) {
                    throw e1;
                }

                return null;
            }
        }
    }

    /**
     * Method to map the book between the source system and Calypso, using the book received from the
     * source. retrieves the String name of the book! This methods checks for the short or the long
     * possible names.
     *
     * @param sourceBook      Book in the source system, like SUSI or GBO to map to the book in Calypso.
     * @param sourceAliasName Alias used to map the book from the source to the book for Calypso.
     * @return An string with the book in Calypso, or a WARNING message.
     */
    // GSM: 26/06/2013. Short/long Portfolio Development. Returns first short
    // name, if not tries long
    public static String getBookMappedName(final String sourceBook, final String sourceAliasName) {

        String EMPTY = "";
        try {
            Book book = mapBookShortOrLongAlias(sourceBook, sourceAliasName);

            if (book != null) {
                return book.getName();
            }

        } catch (Exception e) {
            return EMPTY;
        }

        return EMPTY;
    }

    /**
     * @param jdt datetime to convert
     * @return String in format YYYYMMDD
     */
    public static String getValDateString(final JDatetime jdt) {
        String date = "";

        try {
            String day = jdt.toString().substring(0, 2);
            String month = jdt.toString().substring(3, 5);
            String year = jdt.toString().substring(6, 8);
            date = "20" + year + month + day;
            if (date.contains("/") || date.contains(".")) {
                day = "0" + jdt.toString().substring(0, 1);
                month = jdt.toString().substring(2, 4);
                year = jdt.toString().substring(5, 7);
                date = "20" + year + month + day;
            }
            return date;
        } catch (final Exception e) {

        }
        return date;
    }

    /**
     * Better split method with does not consider if the last "|" is included or not.
     *
     * @param numFields
     * @param separator
     * @param finalSeparator
     * @param lineFile
     * @return
     */
    public static String[] splitMejorado(
            final int numFields, final String separator, final boolean finalSeparator, String lineFile) {
        final String[] line = new String[numFields];

        if (!finalSeparator) {
            for (int i = 0; i < line.length; i++) {
                if (i != (line.length - 1)) {
                    line[i] = lineFile.substring(0, lineFile.indexOf(separator));
                    lineFile = lineFile.substring(lineFile.indexOf(separator));
                    lineFile = lineFile.substring(1);
                } else {
                    line[i] = lineFile;
                }
            }
            return line;
        } else {
            for (int i = 0; i < line.length; i++) {
                line[i] = lineFile.substring(0, lineFile.indexOf(separator));
                lineFile = lineFile.substring(lineFile.indexOf(separator));
                lineFile = lineFile.substring(1);
            }
            return line;
        }
    }

    /**
     * Method to map the book between the source system and Calypso, using the book received from the
     * source.
     *
     * @param sourceBook      Book in the source system, like SUSI or GBO to map to the book in Calypso.
     * @param sourceAliasName Alias used to map the book from the source to the book for Calypso.
     * @return An string with the book in Calypso, or a WARNING message.
     */
    @SuppressWarnings("rawtypes")
    public static String getBookMapped(final String sourceBook, final String sourceAliasName) {
        String strToReturn = "";
        List<Book> bookVector = null;

        try {
            bookVector = DSConnection.getDefault().getRemoteReferenceData().getBooksFromAttribute(sourceAliasName, sourceBook);
            if ((null != bookVector) && !bookVector.isEmpty()) {
                if (bookVector.size() == 1) {
                    final Book book = (Book) bookVector.get(0);
                    strToReturn = book.getAuthName();
                } else {
                    strToReturn =
                            "BOOK_WARNING_MORE_BOOKS: There are more books than 1 in Calypso with the name: "
                                    + sourceBook;
                }
            } else {
                strToReturn =
                        "BOOK_WARNING_NO_BOOK: Book with "
                                + sourceAliasName
                                + " = "
                                + sourceBook
                                + " doesnot exist in the system";
            }
        } catch (final RemoteException e) {
            strToReturn = "BOOK_WARNING: Problem accessing to the DataBase";
        }

        return strToReturn;
    }

    /**
     * Method to map the book between the source system and Calypso, using the book received from the
     * source.
     *
     * @param sourceBook      Book in the source system, like SUSI or GBO to map to the book in Calypso.
     * @param sourceAliasName Alias used to map the book from the source to the book for Calypso.
     * @return The calypso book if it was found.
     */
    @SuppressWarnings("rawtypes")
    public static Book mapBook(final String sourceBook, final String sourceAliasName)
            throws Exception {
        Book book = null;

        final List<Book> bookVector = DSConnection.getDefault().getRemoteReferenceData().getBooksFromAttribute(sourceAliasName, sourceBook);
        if (!Util.isEmpty(bookVector)) {
            if (bookVector.size() == 1) {
                book = (Book) bookVector.get(0);
            } else {
                throw new Exception(
                        "BOOK_WARNING_MORE_BOOKS: There are more books than 1 in Calypso with the name: "
                                + sourceBook);
            }
        } else {
            throw new Exception(
                    "BOOK_WARNING_NO_BOOK: Book with "
                            + sourceAliasName
                            + " = "
                            + sourceBook
                            + " doesnot exist in the system");
        }

        return book;
    }

    /**
     * Method to map the book between the source system and Calypso, using the book received from the
     * source.
     *
     * @param sourceBook      Book in the source system, like SUSI or GBO to map to the book in Calypso.
     * @param sourceAliasName Alias used to map the book from the source to the book for Calypso.
     * @return The calypso book if it was found.
     */
    @SuppressWarnings("rawtypes")
    public static List<Book> mapToCalypsoBook(final String sourceBook, final String sourceAliasName)
            throws Exception {
        return DSConnection.getDefault().getRemoteReferenceData().getBooksFromAttribute(sourceAliasName, sourceBook);
    }

    /**
     * Method to map between a book in Calypso and the book for the target system, just to send the
     * correct value to the other part.
     *
     * @param bookInCalypso     Book to map with the book for the target system.
     * @param aliasTargetSystem Alias used to get the attribute related to the book in Calypso, and
     *                          then do the mapping.
     * @return An string with the book for the target system mapped, or a WARNING message.
     */
    public static String getBookAliasMapped(
            final String bookInCalypso, final String aliasTargetSystem) {
        String strToReturn = "";
        Book book = null;

        try {
            book = DSConnection.getDefault().getRemoteReferenceData().getBook(bookInCalypso);
            if (null != book) {
                strToReturn = book.getAttribute(aliasTargetSystem);
                if ((null == strToReturn) || "".equals(strToReturn)) {
                    strToReturn =
                            "BOOK_WARNING_NO_ATT: The book "
                                    + bookInCalypso
                                    + " in Calypso doesn't have"
                                    + " an attribute to map with: "
                                    + aliasTargetSystem;
                }
            } else {
                strToReturn =
                        "BOOK_WARNING_NO_BOOK: Impossible to get the book from Calypso with the name: "
                                + bookInCalypso;
            }
        } catch (final RemoteException e) {
            strToReturn = "BOOK_WARNING: Problem accessing to the DataBase";
        }

        return strToReturn;
    }

    /**
     * Retrieves the list of files in the path
     *
     * @param path
     * @param startFileName
     * @return
     */
    public static ArrayList<String> getListFiles(final String path, final String startFileName) {
        final ArrayList<String> array = new ArrayList<String>();
        final File files = new File(path);
        final String[] listFiles = files.list();

        for (int i = 0; i < listFiles.length; i++) {
            if (listFiles[i].toLowerCase().startsWith(startFileName.toLowerCase())) {
                final String f = listFiles[i];
                array.add(f);
            }
        }

        return array;
    }

    /**
     * Recovers the JDate from the file name
     *
     * @param startFileName
     * @return
     */
    public static JDate getFileNameDate(final String startFileName) {
        try {
            // We obtain the date to insert the data into Calypso.
            final String str1 = startFileName.substring(0, startFileName.lastIndexOf('.'));
            final String date = str1.substring(str1.length() - 8);
            final int year = Integer.parseInt(date.substring(0, 4));
            final int month = Integer.parseInt(date.substring(4, 6));
            final int day = Integer.parseInt(date.substring(6));
            return JDate.valueOf(year, month, day);
        } catch (final Exception e) {
            return null;
        }
    }

    /**
     * This method checks f the passed n user is a system user. List of system users are maintained in
     * a system property called SKIP_LDAP_AUTH_FOR_USERS which is used in LDAP Authentication.
     *
     * @param userName user name to be checked.
     * @return true if the user is syetem user, false otherwise.
     */
    public static boolean checkIsSystemUser(final String userName) {
        boolean isSystemUser = false;
        final String skipLdapAuth = Defaults.getProperty("SKIP_LDAP_AUTH_FOR_USERS");
        final String[] users = skipLdapAuth.split(",");
        for (final String user : users) {
            if (userName.equalsIgnoreCase(user)) {
                isSystemUser = true;
                break;
            }
        }

        return isSystemUser;
    }

    /**
     * @param isin      the product isin for which to get the quouteName,
     * @param quoteDate date at which the product should be alive (not matured)
     * @return the quote name for this product
     * @throws RemoteException if a problem occurs when getting quotes names from the DataServer
     */
    @SuppressWarnings("rawtypes")
    public static String getQuoteNameFromISIN(final String isin, final JDate quoteDate)
            throws RemoteException {
        String quoteName = "";
        final Vector quoteNames =
                DSConnection.getDefault()
                        .getRemoteMarketData()
                        .getAllQuoteName(quoteDate, "ISIN", isin, false);
        if ((quoteNames != null) && (quoteNames.size() > 0)) {
            // we're querying by isin so it should give only one quote name
            final String name = (String) quoteNames.get(0);
            if (name != null) {
                final int separtaorIndex = name.indexOf('|');
                quoteName = (separtaorIndex > 0 ? name.substring(0, separtaorIndex) : name);
            }
        }
        return quoteName;
    }

    /**
     * @param isin      the product isin for which to get the quouteName,
     * @param quoteDate date at which the product should be alive (not matured)
     * @return the quote name for this product
     * @throws RemoteException if a problem occurs when getting quotes names from the DataServer
     */
    @SuppressWarnings("rawtypes")
    public static String getQuoteNameFromISIN(
            final String isin, final JDate quoteDate, Vector quoteNames, int pos) throws RemoteException {
        String quoteName = "";
        if ((quoteNames != null) && (quoteNames.size() > 0)) {
            // we're querying by isin so it should give only one quote name
            final String name = (String) quoteNames.get(pos);
            if (name != null) {
                final int separtaorIndex = name.indexOf('|');
                quoteName = (separtaorIndex > 0 ? name.substring(0, separtaorIndex) : name);
            }
        }
        return quoteName;
    }

    /**
     * @param isin      the product isin for which to get the quouteName,
     * @param quoteDate date at which the product should be alive (not matured)
     * @return the quote name for this product
     * @throws RemoteException if a problem occurs when getting quotes names from the DataServer
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Double> getQuoteValuesFromISIN(final String isin, final JDate quoteDate)
            throws RemoteException {

        String quoteName = CollateralUtilities.getQuoteNameFromISIN(isin, quoteDate);

        String sql =
                "quote_name = '" + quoteName + "' and quote_date=" + Util.date2SQLString(quoteDate);

        Vector<QuoteValue> quoteValues =
                DSConnection.getDefault().getRemoteMarketData().getQuoteValues(sql);

        Map<String, Double> map = new HashMap<String, Double>();

        for (QuoteValue value : quoteValues) {
            map.put(value.getQuoteType(), value.getClose());
        }

        return map;
    }

    /**
     * Recover the PL Mark of the trade for a day and pricing environment
     *
     * @param trade
     * @param dsCon
     * @param pricingEnv
     * @param processDate
     * @return
     * @throws RemoteException
     */
    // JRL Migratio 14.4
    public static PLMark retrievePLMark(
            final Trade trade, final DSConnection dsCon, final String pricingEnv, final JDate processDate)
            throws RemoteException {
        PLMark plMark = null;
        try {
            plMark = retrievePLMarkBothTypes(trade.getLongId(), pricingEnv, processDate);
        } catch (PersistenceException e) {
            Log.error(CollateralUtilities.class, e);
        }
        return plMark;
    }

    // JRL Migration 14.4
    public static PLMark retrievePLMark(
            final long tradeId,
            final DSConnection dsCon,
            final String pricingEnv,
            final JDate processDate)
            throws RemoteException {
        PLMark plMark = null;
        try {
            plMark = retrievePLMarkBothTypes(tradeId, pricingEnv, processDate);
        } catch (PersistenceException e) {
            Log.error(CollateralUtilities.class, e);
        }
        return plMark;
    }

    /**
     * Checks if the Trade is Exposure Trade with underlying Type CONTRACT_IA. If so it is an
     * Independent_Amount Trade for the Contract and we dont need to have NPV.
     *
     * @param trade
     * @return
     */
    public static boolean isIAExposureTrade(final Trade trade) {
        if (trade.getProductType().equals(CollateralExposure.PRODUCT_TYPE)
                && ((CollateralExposure) trade.getProduct())
                .getUnderlyingType()
                .equals(CollateralStaticAttributes.CTX_CONTRACT_IA)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * retrive the PL mark value
     *
     * @param trade
     * @param dsCon
     * @param markName
     * @param pricingEnv
     * @param processDate
     * @return
     * @throws RemoteException
     */
    public static PLMarkValue retrievePLMarkValue(
            final Trade trade,
            final DSConnection dsCon,
            final String markName,
            final String pricingEnv,
            final JDate processDate)
            throws RemoteException {
        final PLMark plMark = retrievePLMark(trade, dsCon, pricingEnv, processDate);
        return retrievePLMarkValue(plMark, markName);
    }

    /**
     * This method checks if there PLMark exists for the trade/pricingEnv/processDate combination. If
     * exists it returns it otherwise it creates a new one and returns it.
     *
     * @param trade
     * @param dsCon
     * @param pricingEnv
     * @param processDate
     * @return
     * @throws RemoteException
     */
    public static PLMark createPLMarkIfNotExists(
            final Trade trade, final DSConnection dsCon, final String pricingEnv, final JDate processDate)
            throws RemoteException {
        PLMark plMark =
                CollateralUtilities.retrievePLMark(
                        trade, DSConnection.getDefault(), pricingEnv, processDate);
        if (plMark == null) {
            plMark = new SantPLMarkBuilder().forTrade(trade).withPricingEnv(pricingEnv).atDate(processDate).build();
        }

        return plMark;
    }

    /**
     * retrive the PL mark value
     *
     * @param plMark
     * @param markName
     * @return
     */
    public static PLMarkValue retrievePLMarkValue(final PLMark plMark, final String markName) {
        // MIGRATION V14.4 18/01/2015
        if (plMark != null) {
            return plMark.getPLMarkValueByName(markName);
        }
        return null;
    }

    /**
     * //JRL Migration 14.4 This method checks if there PLMark exists for the
     * trade/pricingEnv/processDate combination. If exists it returns it otherwise it creates a new
     * one and returns it.
     *
     * @param trade
     * @param dsCon
     * @param pricingEnv
     * @param processDate
     * @return
     * @throws RemoteException
     */
    public static PLMark createPLMarkIfNotExists(
            final Trade trade, final String pricingEnv, final JDate processDate) {
        PLMark plMark = null;
        try {
            plMark = retrievePLMarkBothTypes(trade.getLongId(), pricingEnv, processDate);

            if (plMark == null) {
                plMark = new SantPLMarkBuilder().forTrade(trade).withPricingEnv(pricingEnv).atDate(processDate).build();
            }

        } catch (PersistenceException e) {
            Log.error(CollateralUtilities.class, e);
        }
        return plMark;
    }

    /**
     * This method assumes the PLMark passed in contains NPV. It creates NPV_BASE doing the conversion
     * and MARGIN_CALL PLMarkValues and adds it to the plMark passed in.
     *
     * @param dsCon
     * @param trade
     * @param plMark
     * @param pricingEnv
     * @param date
     * @param haircut
     * @param errorMsgs
     */
    public static void calculateMCAndNpvBase(
            final DSConnection dsCon,
            final Trade trade,
            final PLMark plMark,
            final PricingEnv pricingEnv,
            final JDate date,
            final Double haircut,
            final ArrayList<String> errorMsgs) {
        if (plMark == null) {
            errorMsgs.add("PLMark cannot be null.");
        }

        CollateralConfig marginCallConfig = null;

        final PLMarkValue npvPLMarkValue =
                CollateralUtilities.retrievePLMarkValue(plMark, PricerMeasure.S_NPV);

        final PLMarkValue mcPLMarkValue =
                calculateMARGIN_CALL(
                        dsCon, trade, npvPLMarkValue, pricingEnv.getName(), date, haircut, errorMsgs);
        if (mcPLMarkValue != null) {
            plMark.addPLMarkValue(mcPLMarkValue);
        }

        try {
            marginCallConfig = getMarginCallConfig(trade, errorMsgs);
        } catch (final RemoteException e) {
            Log.error(CollateralUtilities.class, e);
            errorMsgs.add("Error occured getting Contract. So Couldn't create NPV_BASE");
            return;
        }

        if (marginCallConfig == null) {
            errorMsgs.add("No marginCall Contract found for the trade.");
            return;
        }
        final String requiredCcy = marginCallConfig.getCurrency();
        final PLMarkValue npvBasePLMarkValue =
                convertPLMarkValueToBase(npvPLMarkValue, pricingEnv, requiredCcy, date, errorMsgs);

        if (npvBasePLMarkValue != null) {
            plMark.addPLMarkValue(npvBasePLMarkValue);
        }
    }

    /**
     * This method assumes the PLMark passed in contains NPV_BASE. It creates NPV doing the conversion
     * and MARGIN_CALL PLMarkValues and adds it to the plMark passed in.
     *
     * @param dsCon
     * @param trade
     * @param plMark
     * @param pricingEnv
     * @param date
     * @param haircut
     * @param errorMsgs
     */
    public static void calculateMCAndNpvFromBase(
            DSConnection dsCon,
            Trade trade,
            PLMark plMark,
            PricingEnv pricingEnv,
            JDate date,
            Double haircut,
            ArrayList<String> errorMsgs) {
        if (plMark == null) {
            errorMsgs.add("PLMark cannot be null.");
        }

        PLMarkValue npvBasePLMarkValue =
                CollateralUtilities.retrievePLMarkValue(plMark, SantPricerMeasure.S_NPV_BASE);

        PLMarkValue npvPLMarkValue =
                convertPLMarkValueFromBase(
                        npvBasePLMarkValue, pricingEnv, trade.getTradeCurrency(), date, errorMsgs);

        if (npvPLMarkValue != null) {
            plMark.addPLMarkValue(npvPLMarkValue);
        }

        PLMarkValue mcPLMarkValue =
                calculateMARGIN_CALL(
                        dsCon, trade, npvPLMarkValue, pricingEnv.getName(), date, haircut, errorMsgs);

        if (mcPLMarkValue != null) {
            plMark.addPLMarkValue(mcPLMarkValue);
        }
    }

    /**
     * This method calculates MARGIN_CALL pricer measure for the given trade and returns PLMarkValue.
     * Any errors will be set to erroMessages ArrayList. This method is used in workflow rule and MTM
     * Import Window 1. It gets nPVMarkValue for the Trade if it is not passed as a param 2. If the
     * haircut is not passed then then it gets trade keyword value FO_HAIRCUT. If not found gets it
     * OSLA_FACTOR from the contract
     *
     * <p>3. Calculates MARGIN_CALL pricer measure 4. if NPV currency and contracts currency are
     * different then it converts the MARGIN_CALL to contracts currency using quote values.
     *
     * @param dsCon
     * @param trade
     * @param nPVMarkValue
     * @param pricingEnv
     * @param date
     * @param haircut
     * @param errorMessages
     * @return
     */
    public static PLMarkValue calculateMARGIN_CALL(
            final DSConnection dsCon,
            final Trade trade,
            PLMarkValue nPVMarkValue,
            final String pricingEnv,
            final JDate date,
            Double haircut,
            final ArrayList<String> errorMessages) {

        // 1. Check if nPVMarkValue is null, if so get it from DB
        if (nPVMarkValue == null) {
            try {
                nPVMarkValue =
                        CollateralUtilities.retrievePLMarkValue(trade, dsCon, S_NPV, pricingEnv, date);
            } catch (final RemoteException e) {
                final String msg =
                        "Error retrieving NPV for the date " + date + ", TradeId=" + trade.getLongId();
                errorMessages.add(msg);
                return null;
            }

            if (nPVMarkValue == null) {
                final String msg = "NPV not found for the date " + date + ", TradeId=" + trade.getLongId();
                errorMessages.add(msg);
                return null;
            }
        }
        // GSM: 06/05/2014 - Adaptation to CollateralExposure.SECURITY_LENDING
        // If the trade is not SecLending we save MARGIN_CALL same as NPV
        if (!trade.getProductType().equals(CollateralStaticAttributes.SEC_LENDING)
                && !trade.getProductSubType().equals("SECURITY_LENDING")) {
            return buildPLMarkValue(
                    S_MARGIN_CALL,
                    nPVMarkValue.getCurrency(),
                    nPVMarkValue.getMarkValue(),
                    nPVMarkValue.getAdjustmentType());
        }

        // 2. get the haircut. If the haircut is passed in then we use it.
        // Otherwise get it from Trade keyword FO_HAIRCUT or OSLA_FACTOR.
        if (haircut == null) {
            String hairCutStr = trade.getKeywordValue(CollateralStaticAttributes.FO_HAIRCUT);

            // 3. If no FO_HAIRCUT on trade then get OSLA_FACTOR from MC
            // Contract
            if (Util.isEmpty(hairCutStr)) {
                CollateralConfig marginCallConfig = null;
                try {
                    marginCallConfig = getMarginCallConfig(trade, errorMessages);
                } catch (final RemoteException e) {
                    Log.error(CollateralUtilities.class, e);
                    errorMessages.add("Error occured getting Contract. " + e.getMessage());
                }

                if (marginCallConfig == null) {
                    return null;
                }

                hairCutStr = marginCallConfig.getAdditionalField(CollateralStaticAttributes.MCC_HAIRCUT);

                if (Util.isEmpty(hairCutStr)) {
                    final String msg =
                            "Additional field "
                                    + CollateralStaticAttributes.MCC_HAIRCUT
                                    + " is missing for Contract "
                                    + marginCallConfig.getId();
                    errorMessages.add(msg);
                    return null;
                }
            }

            // 4. Calculate MARGIN_CALL valuation for the trade. If haircut is
            // zero then treat it as 100
            haircut = Double.parseDouble(hairCutStr);
            if (haircut == 0.0d) {
                haircut = 100.0d;
            }
        }

        final double marginCallAmount = (nPVMarkValue.getMarkValue() * haircut) / 100;

        return buildPLMarkValue(
                S_MARGIN_CALL,
                nPVMarkValue.getCurrency(),
                marginCallAmount,
                CollateralStaticAttributes.PL_MARK_AMEND_DEFAULT_REASON);
    }

    /**
     * Builds a PL Mark
     *
     * @param measure
     * @param currency
     * @param amount
     * @param reason
     * @return
     */
    public static PLMarkValue buildPLMarkValue(
            final String measure, final String currency, final double amount, final String reason) {
        final PLMarkValue markValue = new PLMarkValue();
        markValue.setMarkName(measure);
        markValue.setCurrency(currency);
        // Migration V14 - 04012016
        markValue.setOriginalCurrency(currency);
        markValue.setMarkValue(amount);
        markValue.setAdjustmentType(reason);

        return markValue;
    }

    /**
     * This method converts the passed in PLMarkvalue to requiredCcy using the pricingEnv. NPV is
     * converted to NPV_BASE and INDEPENDENT_AMOUNT is converted to INDEPENDENT_AMOUNT_BASE
     *
     * @param markValue
     * @param pricingEnv
     * @param requiredCcy
     * @return
     */
    public static PLMarkValue convertPLMarkValueToBase(
            final PLMarkValue markValue,
            final PricingEnv pricingEnv,
            final String requiredCcy,
            final JDate processDate,
            final ArrayList<String> errorMsgs) {
        PLMarkValue convertedMarkValue = null;

        if (markValue == null) {
            errorMsgs.add("MarkValue passed in is null.");
            return convertedMarkValue;
        }

        final double amount = markValue.getMarkValue();
        final String ccy = markValue.getCurrency();
        String requiredMarkName = null;

        try {
            final double convertedAmount =
                    convertCurrency(ccy, amount, requiredCcy, processDate, pricingEnv);
            if (markValue.getMarkName().equals(SantPricerMeasure.S_NPV)) {
                requiredMarkName = SantPricerMeasure.S_NPV_BASE;
            } else if (markValue.getMarkName().equals(SantPricerMeasure.S_INDEPENDENT_AMOUNT)) {
                requiredMarkName = SantPricerMeasure.S_INDEPENDENT_AMOUNT_BASE;
            }

            convertedMarkValue =
                    buildPLMarkValue(
                            requiredMarkName, requiredCcy, convertedAmount, markValue.getAdjustmentType());

        } catch (final MarketDataException e) {
            errorMsgs.add(e.getMessage());
            Log.error(CollateralUtilities.class, e);
        }

        return convertedMarkValue;
    }

    /**
     * This method converts the passed in PLMarkvalue to requiredCcy using the pricingEnv. NPV_BASE is
     * converted to NPV and INDEPENDENT_AMOUNT_BASE is converted to INDEPENDENT_AMOUNT
     *
     * @param markValue
     * @param pricingEnv
     * @param requiredCcy
     * @return
     */
    public static PLMarkValue convertPLMarkValueFromBase(
            PLMarkValue markValue,
            PricingEnv pricingEnv,
            String requiredCcy,
            JDate processDate,
            ArrayList<String> errorMsgs) {
        PLMarkValue convertedMarkValue = null;

        if (markValue == null) {
            errorMsgs.add("MarkValue passed in is null.");
            return convertedMarkValue;
        }

        final double amount = markValue.getMarkValue();
        final String ccy = markValue.getCurrency();
        String requiredMarkName = null;

        try {
            final double convertedAmount =
                    convertCurrency(ccy, amount, requiredCcy, processDate, pricingEnv);
            if (markValue.getMarkName().equals(SantPricerMeasure.S_NPV_BASE)) {
                requiredMarkName = S_NPV;
            } else if (markValue.getMarkName().equals(SantPricerMeasure.S_INDEPENDENT_AMOUNT_BASE)) {
                requiredMarkName = S_INDEPENDENT_AMOUNT;
            }

            convertedMarkValue =
                    buildPLMarkValue(
                            requiredMarkName, requiredCcy, convertedAmount, markValue.getAdjustmentType());

        } catch (final MarketDataException e) {
            errorMsgs.add(e.getMessage());
            Log.error(CollateralUtilities.class, e);
        }

        return convertedMarkValue;
    }

    /**
     * Converts the amount to the required currency passed as a parameter.
     *
     * @param ccy         Currency from the trade price.
     * @param amount      Measure value from the trade price.
     * @param requiredCcy Required currency to do the conversion.
     * @param date        Date.
     * @param pricingEnv  Pricing Environment.
     * @return New amount converted to the required currency.
     * @throws MarketDataException
     */
    public static double convertCurrency(
            final String ccy,
            final double amount,
            final String requiredCcy,
            final JDate date,
            final PricingEnv pricingEnv)
            throws MarketDataException {

        double convertedAmt = 0.0;

        // Don't convert if the amount is zero
        if (amount == 0.0) {
            return convertedAmt;
        }

        // Convert the amount only if default currency for PO and sell currency
        // are different
        if (requiredCcy.equals(ccy)) {
            convertedAmt = amount;
        } else {
            QuoteValue quote;

            quote = pricingEnv.getQuoteSet().getFXQuote(requiredCcy, ccy, requiredCcy, date, false);

            if (quote == null) {
                throw new MarketDataException(
                        "FX Quote not found for the currency combination " + ccy + "/" + requiredCcy);
            }

            if (!Double.isNaN(quote.getClose()) && (quote.getClose() != 0.0)) {

                convertedAmt = amount / quote.getClose();

            } else {

                throw new MarketDataException(
                        "The close quote is NaN or zero for the quote : " + quote.toString());
            }
        }
        return convertedAmt;
    }

    /**
     * If the MTM imported is non zero and Trade MaturityDate < processDate, Then check if the trade
     * has any fee associated with it. 1. If there is a Fee a. if the Fee End Date >= ValuationDate,
     * nothing to do. b. if the Fee End Date < ValuationDate then change the end Date of the Fee to
     * ValuationDate. 2. If there is no Fee, then create a Fee with End Date=ValuationDate
     *
     * <p>Finally it returns true if the trade has been amended with Fee, false otherwise.
     *
     * @param trade
     * @param values
     * @param processDate
     * @return boolean
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public static boolean handleUnSettledTrade(
            final Trade trade, final double npvValue, final JDate processDate) {
        boolean isTradeModified = false;
        Fee unSettledFee = null;

        if (trade.getMaturityDate() != null) { // For term Deals
            if (trade.getMaturityDate().gte(processDate)) {
                // nothing to do
                return false;
            }
        } else {
            // If it is open term do nothing
            return false;
        }

        if (npvValue != 0.0) {
            final Vector feeVect = trade.getFees();
            if (!Util.isEmpty(feeVect)) {
                // Check if there is a fee with EndDate>=ValuationDate
                for (int i = 0; i < feeVect.size(); i++) {
                    final Fee temp = (Fee) feeVect.get(i);
                    if (temp.getType().equals(CollateralStaticAttributes.FEE_TYPE_FAIL_SETTL_MAT)) {
                        unSettledFee = temp;
                        break;
                    }
                }

                if (unSettledFee != null) {
                    if (unSettledFee.getEndDate().gte(processDate)) {
                        return false;
                    } else {
                        // extend the end Date
                        unSettledFee.setEndDate(processDate);
                        isTradeModified = true;
                    }
                }
            }

            if (unSettledFee == null) {
                // No Fee found so create a fee with endDate=processDate
                unSettledFee = buildUnSettledFee(trade, processDate);
                trade.addFee(unSettledFee);
                isTradeModified = true;
            }
        }

        return isTradeModified;
    }

    private static Fee buildUnSettledFee(final Trade trade, final JDate processDate) {
        final Fee fee = new Fee();
        fee.setFeeDate(processDate);
        fee.setStartDate(processDate.addDays(-1));
        fee.setEndDate(processDate);
        fee.setType(CollateralStaticAttributes.FEE_TYPE_FAIL_SETTL_MAT);
        fee.setCurrency("EUR");
        fee.setLegalEntityId(trade.getCounterParty().getId());
        fee.setAmount(0.0);
        return fee;
    }

    /**
     * This method builds PLMarkValue for the INDEPENDENT_AMOUNT Pricermeasure and adds it to the
     * plMark passed in.
     *
     * @param plMark      The current PLMark for the processDate. This value shouldn't be null
     * @param value
     * @param ccy
     * @param processDate
     * @return
     * @throws Exception
     */
    public static void handleIndependantAmountmarks(
            final PLMark plMark, final double amount, final String ccy, final JDate processDate)
            throws Exception {
        if (plMark == null) {
            throw new Exception("PLMark cannot be null");
        }
        if (amount != 0.0) {
            final PLMarkValue indAmountMarkValue =
                    buildPLMarkValue(
                            S_INDEPENDENT_AMOUNT,
                            ccy,
                            amount,
                            CollateralStaticAttributes.PL_MARK_AMEND_DEFAULT_REASON);
            if (indAmountMarkValue != null) {
                plMark.addPLMarkValue(indAmountMarkValue);
            }
        }
    }

    /**
     * @param plMark
     * @param amount
     * @param curr
     * @throws Exception
     */
    public static void handleClosingPrice(PLMark plMark, double amount, String curr)
            throws Exception {
        if (plMark == null) {
            throw new Exception("PLMark cannot be null");
        }

        final PLMarkValue closingPriceMarkValue = new PLMarkValue();

        closingPriceMarkValue.setMarkName(SantPricerMeasure.S_CLOSING_PRICE);
        closingPriceMarkValue.setCurrency(curr);
        // Migration V14 - 04012016
        closingPriceMarkValue.setOriginalCurrency(curr);
        closingPriceMarkValue.setMarkValue(amount);
        closingPriceMarkValue.setAdjustmentType(
                CollateralStaticAttributes.PL_MARK_AMEND_DEFAULT_REASON);

        plMark.addPLMarkValue(closingPriceMarkValue);
    }

    /**
     * @param plMark
     * @param amount
     * @param curr
     * @throws Exception
     */
    public static void handleNPvPrice(PLMark plMark, double amount, String curr) throws Exception {

        if (plMark == null) {
            throw new Exception("PLMark cannot be null");
        }

        final PLMarkValue npvPriceMarkValue = new PLMarkValue();

        npvPriceMarkValue.setMarkName(SantPricerMeasure.S_NPV);
        npvPriceMarkValue.setCurrency(curr);
        // Migration V14 - 04012016
        npvPriceMarkValue.setOriginalCurrency(curr);
        npvPriceMarkValue.setMarkValue(amount);
        npvPriceMarkValue.setAdjustmentType(CollateralStaticAttributes.PL_MARK_NPV_REASON);

        plMark.addPLMarkValue(npvPriceMarkValue);
    }

    /**
     * @param plMark
     * @param trade
     * @param marginCallConfig
     * @param amount
     * @param ccy
     * @param processDate
     * @param pricingEnv
     * @param errorMsgs
     * @throws Exception
     */
    public static void handleIndAmountAndBaseMarks(
            final PLMark plMark,
            final Trade trade,
            CollateralConfig marginCallConfig,
            final double amount,
            final String ccy,
            final JDate processDate,
            final PricingEnv pricingEnv,
            final ArrayList<String> errorMsgs)
            throws Exception {

        handleIndependantAmountmarks(plMark, amount, ccy, processDate);

        final PLMarkValue indAmount = retrievePLMarkValue(plMark, S_INDEPENDENT_AMOUNT);
        if (indAmount == null) {
            errorMsgs.add(S_INDEPENDENT_AMOUNT + " PLMarkValue not found");
            return;
        }

        // Load MarginCall if it is null
        if (marginCallConfig == null) {
            if (trade == null) {
                errorMsgs.add(" Trade can't be null.");
                return;
            }
            marginCallConfig = getMarginCallConfig(trade, errorMsgs);
            if (marginCallConfig == null) {
                return;
            }
        }

        final String baseCcy = marginCallConfig.getCurrency();
        final PLMarkValue indAmountBase =
                convertPLMarkValueToBase(indAmount, pricingEnv, baseCcy, processDate, errorMsgs);
        if (indAmountBase != null) {
            plMark.addPLMarkValue(indAmountBase);
        }
    }

    // management for RepoEnhacements fields (stored as several mark into a
    // PLMark)
    public static void handleRepoEnhacementField(
            String field, PLMark plMark, double amount, String curr) throws Exception {
        if (plMark == null) {
            throw new Exception("PLMark cannot be null");
        }

        final PLMarkValue bondAccruedInterestMarkValue = new PLMarkValue();

        bondAccruedInterestMarkValue.setMarkName(field);
        bondAccruedInterestMarkValue.setCurrency(curr);
        // Migration V14 - 04012016
        bondAccruedInterestMarkValue.setOriginalCurrency(curr);
        bondAccruedInterestMarkValue.setMarkValue(amount);
        bondAccruedInterestMarkValue.setAdjustmentType(
                CollateralStaticAttributes.PL_MARK_AMEND_DEFAULT_REASON);

        plMark.addPLMarkValue(bondAccruedInterestMarkValue);
    }

    /**
     * Gets list of Trade fees associated with the trade. If a fee is found with the feeType passed in
     * thenit returns it otherwise returns null.
     *
     * @param trade
     * @param feeType
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static Fee getTradeFee(final Trade trade, final String feeType) {
        Fee fee = null;
        final Vector feeVect = trade.getFees();
        if (!Util.isEmpty(feeVect)) {
            // Check if there is a fee of Type feeType
            for (int i = 0; i < feeVect.size(); i++) {
                final Fee temp = (Fee) feeVect.get(i);
                if (temp.getType().equals(feeType)) {
                    fee = temp;
                    break;
                }
            }
        }
        return fee;
    }

    /**
     * Method to send the email to the BOUsers when we create or modify a Margin Call Trade, and we
     * send the new/modified trade to Kondor+ through JMS.
     *
     * @param to       List with all the email address target of the email.
     * @param subject  String that indicates the topic for the email.
     * @param textBody Body of the email.
     * @param from     String that indicates the email address source of the email.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void sendEmail(
            final List<String> to, final String subject, final String textBody, final String from) {
        final EmailMessage msg = new EmailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(textBody);
        msg.setFrom(from);
        msg.setAttachments(new ArrayList());

        try {
            EmailSender.send(msg);
        } catch (final MailException ignored) {
        }
    }

    /**
     * Method to send an email with email headers, for example, used for indicating that an email is high priority
     *
     * @param to       List with all the email address target of the email.
     * @param subject  String that indicates the topic for the email.
     * @param textBody Body of the email.
     * @param from     String that indicates the email address source of the email.
     * @param headers  Map of key value pairs with email specific headers
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void sendEmail(
            final List<String> to, final String subject, final String textBody, final String from, final HashMap<String,String> headers) throws MailException {
        final EmailMessage msg = new EmailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(textBody);
        msg.setFrom(from);
        msg.setAttachments(new ArrayList());

        // send the email, new authenticated version
        try {
            SantanderEmailSender authServer = new SantanderEmailSender();
            authServer.send(msg,headers);

        } catch (Exception exc) {
            throw new MailException(new CalypsoException(exc), exc.getLocalizedMessage());
        }
    }

    /**
     * Method to send the email with one more parameter than the above one (attachment list). We need
     * a log file attached t othe email sent to the users passed as the first parameter. In the body
     * we will have the summary of the process to send in the email as well.
     *
     * @param to          The list with the destination addresses to send the email.
     * @param subject     Topic selected to send the email.
     * @param textBody    Summary for the process, to inform the users quickly.
     * @param from        Source email address, to identify the sender.
     * @param attachments List of documents attached to the email.
     * @throws MailException Exception thrown to the caller method, to get the error in the send.
     * @throws IOException
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void sendEmail(
            final List<String> to,
            final String subject,
            final String textBody,
            final String from,
            final ArrayList attachments)
            throws MailException, IOException {
        final EmailMessage msg = new EmailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(textBody);
        msg.setFrom(from);
        msg.addAttachment(attachments);

        EmailSender.send(msg);
    }

    /**
     * @param s         the String to trim
     * @param maxLength maximum size of the String
     * @return if lenght lower than max, the String; otherwise the string with the first max-1
     * characters size
     */
    public static String trimStringForSize(final String s, final Integer maxLength) {
        return s.substring(0, Math.min(s.length(), maxLength));
    }

    /**
     * @param productType
     * @return true if the product type is a two legs product
     */
    public static boolean isTwoLegsProductType(final String productType) {
        return checkProductTypeLegs(productType, 2);
    }

    /**
     * @param productType
     * @return true if the product type is a one leg product
     */
    public static boolean isOneLegProductType(final String productType) {
        return checkProductTypeLegs(productType, 1);
    }

    /**
     * @param productType
     * @param legNumber
     * @return
     */
    private static boolean checkProductTypeLegs(final String productType, int legNumber) {
        initProductLegNumberDomainValues(DSConnection.getDefault());
        int nbLegs = 0;
        String productTypeLegNumber = productLegNumber.get(productType);
        if (!Util.isEmpty(productTypeLegNumber)) {
            try {
                nbLegs = Integer.valueOf(productTypeLegNumber);

            } catch (final NumberFormatException e) {
                Log.error(CollateralUtilities.class, e);
            }
        } else {
            Log.warn(CollateralUtilities.class, "CollateralExposure.subtype domainValue is not configured for product: " + productType);
        }
        return nbLegs == legNumber;
    }

    @SuppressWarnings("rawtypes")
    public static void initProductLegNumberDomainValues(final DSConnection dsConn) {
        productLegNumber =
                (productLegNumber != null ? productLegNumber : new HashMap<String, String>());
        if (productLegNumber.size() == 0) {
            Vector domainValueProductSubTypes = null;
            domainValueProductSubTypes = LocalCache.getDomainValues(dsConn, PRODUCT_LEG_NB_DOMAIN_VALUE);
            if ((domainValueProductSubTypes != null) && (domainValueProductSubTypes.size() > 0)) {
                for (int i = 0; i < domainValueProductSubTypes.size(); i++) {
                    final String domainValue = (String) domainValueProductSubTypes.get(i);
                    if (domainValue != null) {
                        final String domainComment =
                                LocalCache.getDomainValueComment(dsConn, PRODUCT_LEG_NB_DOMAIN_VALUE, domainValue);
                        productLegNumber.put(domainValue, domainComment);
                    }
                }
            }
        }
    }

    /**
     * Get the list of domain values sourceSystem+ColExpProductsMapping.
     *
     * @param dsConn
     * @param sourceSystem
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static synchronized HashMap<String, String> initMappingFromDomainValues(
            final DSConnection dsConn, final String sourceSystem) {

        // if ((productSubTypeMapping != null) && (productSubTypeMapping.size()
        // > 0)) {
        // return productSubTypeMapping;
        // }
        // productSubTypeMapping = (productSubTypeMapping != null ?
        // productSubTypeMapping : new HashMap<String, String>());
        productSubTypeMapping = new HashMap<String, String>();
        
        String domainName = sourceSystem + PRODUCT_MAPPING_DOMAIN_VALUE;

        // if (productSubTypeMapping.size() == 0) {
        Vector domainValueProductSubTypes = null;
//        try {
//            domainValueProductSubTypes =
//                    DSConnection.getDefault()
//                            .getRemoteReferenceData()
//                            .getDomainValues(sourceSystem + PRODUCT_MAPPING_DOMAIN_VALUE);
            domainValueProductSubTypes = LocalCache.getDomainValues(DSConnection.getDefault(), domainName, true);
//        } catch (CalypsoServiceException e) {
//            Log.error(
//                    CollateralUtilities.class,
//                    "Error loading domainNames for: "
//                            + sourceSystem
//                            + PRODUCT_MAPPING_DOMAIN_VALUE
//                            + ": "
//                            + e.getMessage());
//        }
        if ((domainValueProductSubTypes != null) && (domainValueProductSubTypes.size() > 0)) {

            for (int i = 0; i < domainValueProductSubTypes.size(); i++) {
                final String domainValue = (String) domainValueProductSubTypes.get(i);
                if (domainValue != null) {
                    String domainComment = "";
//                    try {
                        domainComment = LocalCache.getDomainValueComment(DSConnection.getDefault(), domainName, domainValue);
//                                DSConnection.getDefault()
//                                        .getRemoteReferenceData()
//                                        .getDomains()
//                                        .getDomainValueComment(
//                                                sourceSystem + PRODUCT_MAPPING_DOMAIN_VALUE, domainValue);
//                    } catch (CalypsoServiceException e) {
//                        Log.error(
//                                CollateralUtilities.class,
//                                "Error loading domainComment for: " + domainValue + ": " + e.getMessage());
//                    }
                    productSubTypeMapping.put(domainValue, domainComment);
                }
            }
        }
        // }
        return productSubTypeMapping;
    }

    // START CALYPCROSS-38 - mromerod

    /**
     * Get the list of domain values sourceSystem+ColExpProductsMapping for the ST
     * ScheduledTaskImportCSVExposureTrades
     *
     * @param dsConn
     * @param sourceSystem
     * @return
     */
    public static synchronized HashMap<String, String> initMappingFromDomainValuesExposureTrades(
            final DSConnection dsConn, final String sourceSystem) {

        productSubTypeMapping = new HashMap<String, String>();

        List<DomainValues.DomainValuesRow> domainValueProductSubTypes =
                new ArrayList<DomainValues.DomainValuesRow>();
        try {
            // We get the domains
            domainValueProductSubTypes =
                    DSConnection.getDefault()
                            .getRemoteReferenceData()
                            .getDomainValuesRows(sourceSystem + PRODUCT_MAPPING_DOMAIN_VALUE);
        } catch (CalypsoServiceException e) {
            Log.error(
                    CollateralUtilities.class,
                    "Error loading domainNames for: "
                            + sourceSystem
                            + PRODUCT_MAPPING_DOMAIN_VALUE
                            + ": "
                            + e.getMessage());
        }
        if ((domainValueProductSubTypes != null) && (domainValueProductSubTypes.size() > 0)) {

            for (DomainValues.DomainValuesRow valueRow : domainValueProductSubTypes) {
                if (valueRow != null) {
                    // We get the corresponding domain value
                    final String domainValue = valueRow.getValue();
                    if (domainValue != null) {
                        String domainComment = "";
                        // We get the corresponding domain comment
                        domainComment = valueRow.getComment();
                        productSubTypeMapping.put(domainValue, domainComment);
                    }
                }
            }
        }
        return productSubTypeMapping;
    }
    // END CALYPCROSS-38 - mromerod

    /**
     * .
     *
     * @param dsConn
     * @param sourceSystem
     * @return
     */
    public static synchronized HashMap<String, String> initMappingInstrumentValues(
            final DSConnection dsConn, final String sourceSystem) {

        if ((productSubTypeMapping != null) && (productSubTypeMapping.size() > 0)) {
            return productSubTypeMapping;
        }
        productSubTypeMapping =
                (productSubTypeMapping != null ? productSubTypeMapping : new HashMap<String, String>());
        if (productSubTypeMapping.size() == 0) {
            Vector domainValueProductSubTypes = null;
            domainValueProductSubTypes =
                    LocalCache.getDomainValues(dsConn, sourceSystem + PRODUCT_MAPPING_DOMAIN_VALUE);
            if ((domainValueProductSubTypes != null) && (domainValueProductSubTypes.size() > 0)) {
                for (int i = 0; i < domainValueProductSubTypes.size(); i++) {
                    final String domainValue = (String) domainValueProductSubTypes.get(i);
                    if (domainValue != null) {
                        final String domainComment =
                                LocalCache.getDomainValueComment(
                                        dsConn, sourceSystem + PRODUCT_MAPPING_DOMAIN_VALUE, domainValue);
                        productSubTypeMapping.put(domainComment, domainValue);
                    }
                }
            }
        }
        return productSubTypeMapping;
    }

    public static String converseCalcPeriodKGRContracts(final String calcPeriod) {
        if (calcPeriod != null) {
            if (calcPeriod.equals(DAILY_DATE_RULE)) {
                return CALC_PERIOD_VALUE;
            }
        }
        return calcPeriod;
    }

    /**
     * @param tradeId
     * @param pricingEnv
     * @param processDate
     * @return
     * @throws PersistenceException
     * @author aalonsop Replaces old getPLMarks methods, now it's using the new RemoteMark service and
     * searches PLMarks with NONE and PL type
     */
    public static PLMark retrievePLMarkBothTypes(long tradeId, String pricingEnv, JDate processDate)
            throws PersistenceException {
        PLMark plMark = null;
        plMark =
                DSConnection.getDefault()
                        .getRemoteMark()
                        .getMark(PLTYPE, tradeId, "", pricingEnv, processDate);
        if (plMark == null)
            plMark =
                    DSConnection.getDefault()
                            .getRemoteMark()
                            .getMark(NONETYPE, tradeId, "", pricingEnv, processDate);
        return plMark;
    }

    /**
     * @param amount
     * @return The parsed double value
     * @author aalonsop AAP
     * @description Handles different incoming number formats and nullPointers to avoid crashes,
     * designed to parse Credit Rating Values
     */
    public static Double parseStringAmountToDouble(String amount) {
        NumberFormat formatter;
        try {
            return Double.valueOf(amount);
        } catch (NumberFormatException e) {
            formatter = NumberFormat.getNumberInstance();
            try {
                return formatter.parse(amount).doubleValue();
            } catch (ParseException e1) {
                Log.error(
                        CollateralUtilities.class, "Error while trying to get Double from String: " + amount);
            }
        } catch (NullPointerException e) {
            Log.warn(
                    CollateralUtilities.class, "Null MTA value received, a zero value double is returned");
            return 0.0D;
        }
        return null;
    }

    // JRL Migration

    /**
     * @param tradeIds
     * @param pricingEnv
     * @param processDate
     * @return
     * @throws PersistenceException
     */
    public static Collection<PLMark> retrievePLMarkBothTypes(
            Collection<Long> tradeIds, String pricingEnv, JDate processDate) throws PersistenceException {
        Collection<PLMark> plMarksrdo = new ArrayList<>();
        Collection<PLMark> plMarksTypePl =
                DSConnection.getDefault()
                        .getRemoteMark()
                        .getMarks(PLTYPE, tradeIds, pricingEnv, processDate);
        Collection<PLMark> plMarksTypeNone =
                DSConnection.getDefault()
                        .getRemoteMark()
                        .getMarks(NONETYPE, tradeIds, pricingEnv, processDate);
        plMarksrdo.addAll(plMarksTypePl);
        plMarksrdo.addAll(plMarksTypeNone);
        return plMarksrdo;
    }

    /**
     * This method returns previous business day for the given Trade. If the Trade Date is today then
     * it returns today.
     *
     * @param trade
     * @return
     */
    public static JDate getPrevBusinessDay(final Trade trade) {
        // We need to take previous working day as processDay in this rule
        // final CurrencyDefault settleCcy =
        // LocalCache.getCurrencyDefault(trade.getSettleCurrency());
        JDate processDate = JDate.getNow().addBusinessDays(-1, Util.string2Vector("SYSTEM"));
        if (processDate.before(trade.getTradeDate().getJDate(TimeZone.getDefault()))) {
            processDate = JDate.getNow();
        }
        return processDate;
    }

    /**
     * Checks if the trade has MTM_DATE keyword value. If present it returns it otherwise it returns
     * prev business day ProccessDate
     *
     * @param trade
     * @return
     */
    public static JDate getMTMProcessDate(final Trade trade) {
        JDate processDate = getMTMDateFromTradeKeyword(trade);
        // otherwise take previous working day as processDay
        if (processDate == null) {
            processDate = getPrevBusinessDay(trade);
        }
        return processDate;
    }

    /**
     * Used in Fee calculators, the Fee process date cannot be before the trade date so this method
     * checks it
     *
     * @param trade
     * @return
     */
    public static JDate getMTMProcessDateForFeeCalc(final Trade trade, JDate processDate) {
        JDate tradeDate = JDate.valueOf(trade.getTradeDate());
        if (processDate.before(tradeDate)) processDate = tradeDate;
        return processDate;
    }

    /**
     * AAP Refactor
     *
     * @param trade
     * @return
     */
    public static JDate getMTMDateFromTradeKeyword(final Trade trade) {
        JDate processDate = null;
        SimpleDateFormat mtmDateFormatter = new SimpleDateFormat(mtmDateFormat);
        // Check if we have MTM_DATE as a keyword
        String proccessStr = trade.getKeywordValue(TradeInterfaceUtils.TRD_IMP_FIELD_MTM_DATE);
        try {
            if (!proccessStr.isEmpty()) {
                try {
                    processDate = JDate.valueOf(mtmDateFormatter.parse(proccessStr));
                } catch (ParseException e) {
                    Log.error(
                            CollateralUtilities.class, "An exception ocurred while trying to parde the MTMDate");
                }
            }
        } catch (NullPointerException e1) {
            return null;
        }
        return processDate;
    }

    /**
     * Returns the Date as String which is used to add as Trade Keyword
     *
     * @param date
     * @return
     */
    public static String getMTMDate(JDate date) {
        SimpleDateFormat mtmDateFormatter = new SimpleDateFormat(mtmDateFormat);
        return mtmDateFormatter.format(date.getDate(TimeZone.getDefault()));
    }

    /**
     * @param fee
     * @param trade
     * @return The processed Fee with the updated dates
     * @author aalonsop
     */
    public static boolean isFeeDateBeforeGivenDate(Fee fee, JDate tradeDate) {
        boolean hasChanged = false;
        if (fee.getStartDate().before(tradeDate)) {
            fee.setStartDate(tradeDate);
            hasChanged = true;
        }
        if (fee.getFeeDate().before(tradeDate)) {
            fee.setFeeDate(tradeDate);
            hasChanged = true;
        }
        if (fee.getEndDate().before(fee.getStartDate())) {
            fee.setEndDate(fee.getStartDate());
            hasChanged = true;
        }
        return hasChanged;
    }

    public static String converseAssetTypeKGRContracts(final String assetType) {
        if (assetType != null) {
            if (assetType.equals(CASH)) {
                return CASH_VALUE;
            }
            if (assetType.equals(SECURITY)) {
                return SECURITY_VALUE;
            }
            if (assetType.equals(BOTH)) {
                return BOTH_VALUE;
            }
        }
        return "";
    }

    public static String converseHeadCloneKGRContracts(final String headClone) {
        if (headClone != null) {
            if (headClone.equals(HEAD)) {
                return HEAD_VALUE;
            }
            if (headClone.equals(CLONE)) {
                return CLONE_VALUE;
            }
        }
        return "";
    }

    /**
     * @param dsConn
     * @param id
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static String getAttributeValueFromLE(
            final DSConnection dsConn, final LegalEntity le, final String attributeName) {
        try {
            if (le != null) {
                final Vector attributes = dsConn.getRemoteReferenceData().getAttributes(le.getId());
                if (attributes != null) {
                    final Iterator it = attributes.iterator();
                    while (it.hasNext()) {
                        final LegalEntityAttribute attribute = (LegalEntityAttribute) it.next();
                        if (attribute != null) {
                            if (attribute.getAttributeType().equals(attributeName.trim())) {
                                return attribute.getAttributeValue();
                            }
                        }
                    }
                }
            }
        } catch (final RemoteException e) {
            Log.error(
                    CollateralUtilities.class,
                    "error recovering attribute " + attributeName + " from LE " + le.getName());
        }
        return "";
    }

    /**
     * @param dsConn
     * @param id
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static String getAliasEntityKGR(final DSConnection dsConn, final int id) {
        try {
            final LegalEntity le = dsConn.getRemoteReferenceData().getLegalEntity(id);
            if (le != null) {
                final Vector attributes = dsConn.getRemoteReferenceData().getAttributes(le.getId());
                if (attributes != null) {
                    final Iterator it = attributes.iterator();
                    while (it.hasNext()) {
                        final LegalEntityAttribute attribute = (LegalEntityAttribute) it.next();
                        if (attribute != null) {
                            if (attribute.getAttributeType().equals(ALIAS_ENTITY_KGR)) {
                                return attribute.getAttributeValue();
                            }
                        }
                    }
                }
            }
        } catch (final RemoteException e) {
            Log.error(
                    CollateralUtilities.class,
                    "error recovering attribute " + ALIAS_ENTITY_KGR + " from LE id " + id);
        }
        return "";
    }

    /**
     * Method to convert from a double number to a numeric value with the format '#0.00'.
     *
     * @param number Value to convert.
     * @return The converted value.
     */
    public static String formatDouble(final Double number) {
        if (number == null) {
            return "";
        } else {
            final NumberFormat numberFormatter = new DecimalFormat("#0.00");
            return numberFormatter.format(number);
        }
    }

    /**
     * Method to convert from a double number to a numeric value with the format '#0.00'.
     *
     * @param number Value to convert.
     * @return The converted value.
     */
    public static String formatNumber(final double number) {
        final NumberFormat numberFormatter = new DecimalFormat("#0.00");
        return numberFormatter.format(number);
    }

    /**
     * Method to convert from a double number to a numeric value with the given format
     *
     * @param number Value to convert.
     * @return The converted value.
     */
    public static String formatNumber(final double number, String format) {
        final NumberFormat numberFormatter = new DecimalFormat(format);
        return numberFormatter.format(number);
    }

    /**
     * Format number according pattern and Locale
     *
     * @param number
     * @param pattern
     * @param locale
     * @return
     */
    public static String formatNumber(
            final double number, final String pattern, final Locale locale) {
        final NumberFormat numberFormatter =
                new DecimalFormat("#0.00", new DecimalFormatSymbols(locale));
        return numberFormatter.format(number);
    }

    /**
     * Calculate margin call entries for the given contracts using the given processing date
     *
     * @param contractIds list of contracts ids for which margin call calculation should be launched
     * @param processDate date to use for margin call calculation
     * @param messages    errors encountered while calculating
     * @return
     */
    public static boolean calculateContracts(
            final Set<Integer> contractIds, final JDatetime processDate, final List<String> messages) {
        ExecutionContext context = CollateralManagerUtil.getDefaultExecutionContext();
        context.setProcessDate(processDate.getJDate(TimeZone.getDefault()));
        // load the margin call entries
        List<MarginCallEntry> entries =
                CollateralManagerUtil.loadEntries(new ArrayList<>(contractIds), context, messages);
        CollateralTaskWorker rePriceTaskWorker = CollateralTaskWorker.getInstance(CollateralTaskWorker.TASK_REPRICE, context, entries);
        rePriceTaskWorker.process();
        // We keep Error messages for all contracts in this List.
        messages.clear();
        return true;
    }

    /**
     * Retrieve the actual date in string format.
     *
     * @param
     * @return String with the date in dd/mm/yyyy fomat.
     */
    public static String getActualDate() {

        final Date date = new Date();
        final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        final String stringDate = sdf.format(date);
        return stringDate;
    }

    /**
     * Retrieve date converted in dd/mm/yyyy format.
     *
     * @param date String with the date, format String with the previous format
     * @return String with the date in dd/mm/yyyy fomat.
     */
    public static String convertDate(final String date, final String prevFormat) {

        final SimpleDateFormat sdf = new SimpleDateFormat(prevFormat);
        Date d = null;
        try {
            d = sdf.parse(date);
        } catch (final ParseException e) {
            Log.error(CollateralUtilities.class, e);
        }
        final SimpleDateFormat sdf2 = new SimpleDateFormat("dd/mm/yyyy");
        final String stringDate = sdf2.format(d);

        return stringDate;
    }

    /**
     * Convert a date from String to JDate format
     *
     * @param
     * @return String with the date in dd/mm/yyyy fomat.
     */
    public static JDate getJDate(final String stringDate) {

        JDate jdate;

        // convert String to JDate
        final int day = Integer.parseInt(stringDate.substring(0, 2));
        final int month = Integer.parseInt(stringDate.substring(3, 5));
        final int year = Integer.parseInt(stringDate.substring(6));
        jdate = JDate.valueOf(year, month, day);
        return jdate;
    }

    @SuppressWarnings("unchecked")
    public static double getFXRate(final JDate date, final String ccy1, final String ccy2) {

        // check currencies
        if (Util.isEmpty(ccy1) || Util.isEmpty(ccy1)) {
            return 0.00;
        }

        if (!ccy1.equals(ccy2)) {
            String rate = "FX." + ccy1 + "." + ccy2;
            String clausule =
                    "quote_name = '" + rate + "' and trunc(quote_date) = to_date('" + date + "', 'dd/mm/yy')";
            Vector<QuoteValue> vQuotes;
            try {
                vQuotes = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(clausule);
                if ((vQuotes != null) && (vQuotes.size() > 0)) {
                    return vQuotes.get(0).getClose();
                }
                // COL_OUT_019
                // Carlos Cejudo: If the quote cannot be found try to find it
                // with the currencies reversed. Then the
                // value to return will be 1/getClose()
                else {
                    rate = "FX." + ccy2 + "." + ccy1;
                    clausule =
                            "quote_name = '"
                                    + rate
                                    + "' and trunc(quote_date) = to_date('"
                                    + date
                                    + "', 'dd/mm/yy')";
                    vQuotes = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(clausule);
                    if ((vQuotes != null) && (vQuotes.size() > 0)) {
                        return 1.0 / vQuotes.get(0).getClose();
                    }
                    return 0.00; // no encuentra rate para fecha
                }
            } catch (final RemoteException e) {
                Log.error(CollateralUtilities.class, e);
                return 0.00;
            }
        }
        return 1.00;
    }

    public static double getFXRatebyQuoteSet(final JDate date, final String ccy1, final String ccy2, PricingEnv pricingEnv) {

        // check currencies
        if (Util.isEmpty(ccy1) || Util.isEmpty(ccy1)) {
            return 0.00;
        }
        String quoteSetName = "OFFICIAL";
        if (pricingEnv != null) {
            quoteSetName = pricingEnv.getQuoteSetName();
        }

        if (!ccy1.equals(ccy2)) {
            String rate = "FX." + ccy1 + "." + ccy2;
            String clausule = "quote_name = '" + rate + "' AND quote_set_name ='" + quoteSetName + "'  AND trunc(quote_date) = to_date('" + date + "', 'dd/mm/yy')";
            Vector<QuoteValue> vQuotes;
            try {
                vQuotes = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(clausule);
                if ((vQuotes != null) && (vQuotes.size() > 0)) {
                    return vQuotes.get(0).getClose();
                }
                // COL_OUT_019
                // Carlos Cejudo: If the quote cannot be found try to find it
                // with the currencies reversed. Then the
                // value to return will be 1/getClose()
                else {
                    rate = "FX." + ccy2 + "." + ccy1;
                    clausule = "quote_name = '" + rate + "' AND quote_set_name ='" + quoteSetName + "' AND trunc(quote_date) = to_date('" + date
                            + "', 'dd/mm/yy')";
                    vQuotes = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(clausule);
                    if ((vQuotes != null) && (vQuotes.size() > 0)) {
                        return 1.0 / vQuotes.get(0).getClose();
                    }
                    return 0.00; // no encuentra rate para fecha
                }
            } catch (final RemoteException e) {
                Log.error(CollateralUtilities.class, e);
                return 0.00;
            }

        }
        return 1.00;
    }

    public static CollateralConfig getMarginCallConfig(final Trade trade, final ArrayList<String> errorMsgs)
            throws RemoteException {
        CollateralConfig marginCallConfig = null;
        int mccId = 0;
        try {
            mccId = trade.getKeywordAsInt("MC_CONTRACT_NUMBER");

        } catch (final Exception e) {
            mccId = 0;
        }

        if (mccId == 0) {
            final ArrayList<CollateralConfig> eligibleMarginCallConfigs =
                    SantReportingUtil.getSantReportingService(DSConnection.getDefault())
                            .getEligibleMarginCallConfigs(trade);
            if (Util.isEmpty(eligibleMarginCallConfigs)) {
                errorMsgs.add("No MarginCall Contract found for the Trade");
                return marginCallConfig;
            }
            if (eligibleMarginCallConfigs.size() > 1) {
                errorMsgs.add("More than one MarginCall Contract found for the Trade");
                return marginCallConfig;
            }
            marginCallConfig = eligibleMarginCallConfigs.get(0);

        } else {
            marginCallConfig =
                    CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), mccId);
        }
        return marginCallConfig;
    }

    /**
     * AAP
     *
     * @param context
     * @param mcFilter
     * @return A list of the contracts ids
     * @throws CollateralServiceException
     * @author aalonsop
     */
    public static List<Integer> getAllMarginCallContractIds(MarginCallConfigFilter mcFilter)
            throws CollateralServiceException {
        final List<CollateralConfig> contracts = CollateralManagerUtil.loadCollateralConfigs(mcFilter);
        final List<Integer> configIds = new ArrayList<Integer>();
        // Parallel for
        ExecutorService parallel =
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try {
            parallel.execute(
                    new Runnable() {
                        @Override
                        public void run() {
                            for (CollateralConfig contract : contracts) {
                                configIds.add(contract.getId());
                            }
                        }
                    });
        } finally {
            parallel.shutdown();
            try {
                parallel.awaitTermination(2, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Log.error(CollateralUtilities.class, "Error while waiting to finish the execution");
            }
        }
        return configIds;
    }

    /**
     * @param trade
     * @param plMark1
     * @param plMark2
     * @param lineNb
     * @param line
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unused")
    public static PLMark createPLMarkForTrade(
            final Trade trade,
            final InterfacePLMarkBean plMark1,
            final InterfacePLMarkBean plMark2,
            final InterfacePLMarkBean plMarkIA1,
            final InterfacePLMarkBean plMarkIA2)
            throws Exception {

        double mtmIAAmount = 0;
        String mtmIACcy = "";
        PLMark plMark = null;
        final List<PLMarkValue> markValues = new ArrayList<PLMarkValue>();
        PricingEnv pricingEnv = null;
        CollateralConfig marginCallConfig = null;
        final DSConnection dsCon = DSConnection.getDefault();
        final CollateralExposure product = (CollateralExposure) trade.getProduct();

        int mccId = 0;
        try {
            mccId = trade.getKeywordAsInt("MC_CONTRACT_NUMBER");

        } catch (final Exception e) {
            mccId = 0;
        }

        if (mccId == 0) {
            final ArrayList<CollateralConfig> eligibleMarginCallConfigs =
                    SantReportingUtil.getSantReportingService(DSConnection.getDefault())
                            .getEligibleMarginCallConfigs(trade);
            if (!Util.isEmpty(eligibleMarginCallConfigs) && (eligibleMarginCallConfigs.size() == 1)) {
                marginCallConfig = eligibleMarginCallConfigs.get(0);
            }

        } else {
            marginCallConfig =
                    CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), mccId);
        }

        double mtmAmount = plMark1.getPlMarkValue();
        final JDate mtmDate = plMark1.getPlMarkDate();
        // String mtmCcy = plMark1.getPlMarkCurrency();
        // String mtmCcy = trade.getTradeCurrency();

        if (marginCallConfig != null) {
            String mtmCcy = marginCallConfig.getCurrency();
            String mtmDateString = Util.dateToString(mtmDate);
            String envName =
                    (Util.isEmpty(mtmDateString)
                            ? marginCallConfig.getPricingEnvName()
                            : mtmDateString + "_" + marginCallConfig.getPricingEnvName());

            if (pricingEnvsCache.get(envName) == null) {
                pricingEnv =
                        dsCon
                                .getRemoteMarketData()
                                .getPricingEnv(marginCallConfig.getPricingEnvName(), trade.getTradeDate());
                pricingEnvsCache.put(envName, pricingEnv);

            } else {
                pricingEnv = pricingEnvsCache.get(envName);
            }

            // mtmCcy = marginCallConfig.getCurrency();
            // then convert the mtm into the contract
            // currency

            if (CollateralUtilities.isTwoLegsProductType(product.getSubType())) {

                final double mtmAmount1 = plMark1.getPlMarkValue();
                final String mtmCcy1 = plMark1.getPlMarkCurrency();
                final double mtmAmount2 = plMark2.getPlMarkValue();
                final String mtmCcy2 = plMark2.getPlMarkCurrency();

                mtmAmount = 0;
                if (!Util.isEmpty(mtmCcy1) && !Util.isEmpty(mtmCcy2) && !mtmCcy1.equals(mtmCcy2)) {
                    // mtmCcy = mtmCcy1;
                    // mtmCcy = marginCallConfig.getCurrency();
                    // trade.setTradeCurrency(mtmCcy);
                    // trade.setSettleCurrency(mtmCcy);
                    // product.setCurrency(mtmCcy);
                    mtmAmount +=
                            CollateralUtilities.convertCurrency(mtmCcy1, mtmAmount1, mtmCcy, mtmDate, pricingEnv);
                    // mtmAmount += mtmAmount1;
                    mtmAmount +=
                            CollateralUtilities.convertCurrency(mtmCcy2, mtmAmount2, mtmCcy, mtmDate, pricingEnv);
                } else {
                    mtmAmount =
                            CollateralUtilities.convertCurrency(
                                    mtmCcy1, (mtmAmount1 + mtmAmount2), mtmCcy, mtmDate, pricingEnv);
                }

                double mtmIAAmount1 = 0;
                String mtmIACcy1 = "";
                if (!Util.isEmpty(plMarkIA1.getPlMarkCurrency())) {

                    mtmIAAmount1 = plMarkIA1.getPlMarkValue();
                    mtmIACcy1 = plMarkIA1.getPlMarkCurrency();
                }

                double mtmIAAmount2 = 0;
                String mtmIACcy2 = "";

                if (!Util.isEmpty(plMarkIA2.getPlMarkCurrency())) {

                    mtmIAAmount2 = plMarkIA2.getPlMarkValue();
                    mtmIACcy2 = plMarkIA2.getPlMarkCurrency();
                }
                // it's the same independent amount value on both trade's legs
                // so import just one of them
                if (!Util.isEmpty(mtmIACcy1)) {
                    // mtmCcy = marginCallConfig.getCurrency();
                    mtmIACcy = mtmIACcy1;
                    mtmIAAmount +=
                            CollateralUtilities.convertCurrency(
                                    mtmIACcy1, mtmIAAmount1, mtmCcy, mtmDate, pricingEnv);
                    // mtmIAAmount +=
                    // CollateralUtilities.convertCurrency(mtmIACcy2,
                    // mtmIAAmount2, mtmCcy, mtmDate,
                    // pricingEnv);

                }

                // if (!Util.isEmpty(mtmIACcy2) && !Util.isEmpty(mtmIACcy1) &&
                // !mtmIACcy2.equals(mtmIACcy1)) {
                // // mtmCcy = marginCallConfig.getCurrency();
                // mtmIAAmount += CollateralUtilities.convertCurrency(mtmIACcy1,
                // mtmIAAmount1, mtmCcy, mtmDate,
                // pricingEnv);
                // mtmIAAmount += CollateralUtilities.convertCurrency(mtmIACcy2,
                // mtmIAAmount2, mtmCcy, mtmDate,
                // pricingEnv);
                //
                // }

            } else {
                // handle NPV pl Mark
                mtmAmount =
                        CollateralUtilities.convertCurrency(
                                plMark1.getPlMarkCurrency(), mtmAmount, mtmCcy, mtmDate, pricingEnv);

                // handle the IA pl Mark
                if (!Util.isEmpty(plMarkIA1.getPlMarkCurrency())) {
                    mtmIACcy = plMarkIA1.getPlMarkCurrency();
                    mtmIAAmount =
                            CollateralUtilities.convertCurrency(
                                    mtmIACcy, plMarkIA1.getPlMarkValue(), mtmCcy, mtmDate, pricingEnv);
                }
            }
            // else {
            // mtmAmount = CollateralUtilities.convertCurrency(mtmCcy,
            // mtmAmount, marginCallConfig.getCurrency(),
            // mtmDate, pricingEnv);
            // }

            // NPV

            PLMarkValue npvBaseValue =
                    CollateralUtilities.buildPLMarkValue(
                            SantPricerMeasure.S_NPV_BASE,
                            mtmCcy,
                            mtmAmount,
                            CollateralStaticAttributes.PL_MARK_AMEND_DEFAULT_REASON);

            // PLMarkValue npvValue =
            // CollateralUtilities.buildPLMarkValue(PricerMeasure.S_NPV, mtmCcy,
            // mtmAmount,
            // CollateralStaticAttributes.PL_MARK_AMEND_DEFAULT_REASON);
            markValues.add(npvBaseValue);
            // if it's a two legs deal, save the original pl marks of each leg
            if (CollateralUtilities.isTwoLegsProductType(product.getSubType())) {
                markValues.add(
                        createPLMarkValue(
                                plMark1.getPlMarkValue(),
                                plMark1.getPlMarkCurrency(),
                                plMark1.getPlMarkDate(),
                                SantPricerMeasure.S_NPV_LEG1));
                markValues.add(
                        createPLMarkValue(
                                plMark2.getPlMarkValue(),
                                plMark2.getPlMarkCurrency(),
                                plMark2.getPlMarkDate(),
                                SantPricerMeasure.S_NPV_LEG2));
            }
            // create and save the plMark
            plMark =
                    createPLMarks(trade, dsCon, mtmDate, marginCallConfig.getPricingEnvName(), markValues);
            final ArrayList<String> errors = new ArrayList<String>();
            // MARGIN_CALL
            // CollateralUtilities.calculateMCAndNpvFromBase(dsCon, trade,
            // plMark, pricingEnv, mtmDate, null, errors);

            CollateralUtilities.calculateMCAndNpvFromBase(
                    dsCon, trade, plMark, pricingEnv, mtmDate, null, errors);
            if (!Util.isEmpty(errors)) {
                final StringBuffer errorsList = new StringBuffer("");
                for (final String error : errors) {
                    if (errorsList.length() == 0) {
                        errorsList.append(error);
                    } else {
                        errorsList.append(", ");
                        errorsList.append(error);
                    }
                }
                final String errMsg = errorsList.toString();
                Log.error(CollateralUtilities.class, errMsg);
                errors.clear();
                throw new Exception(errMsg);
            }
            // INDEPENDENT_AMOUNT

            if ((mtmIAAmount != 0) && !Util.isEmpty(mtmIACcy)) {
                PLMarkValue indAmoutBaseValue =
                        CollateralUtilities.buildPLMarkValue(
                                SantPricerMeasure.S_INDEPENDENT_AMOUNT_BASE,
                                mtmCcy,
                                mtmIAAmount,
                                CollateralStaticAttributes.PL_MARK_AMEND_DEFAULT_REASON);

                if (indAmoutBaseValue != null) {
                    plMark.addPLMarkValue(indAmoutBaseValue);
                }

                PLMarkValue indAmoutValue =
                        convertPLMarkValueFromBase(indAmoutBaseValue, pricingEnv, mtmIACcy, mtmDate, errors);

                if (indAmoutValue != null) {
                    plMark.addPLMarkValue(indAmoutValue);
                }

                // CollateralUtilities.handleIndAmountAndBaseMarks(plMark,
                // trade, marginCallConfig, mtmIAAmount,
                // mtmIACcy,
                // mtmDate, pricingEnv, errors);
            }

            if (!Util.isEmpty(errors)) {
                final StringBuffer errorsList = new StringBuffer("");
                for (final String error : errors) {
                    if (errorsList.length() == 0) {
                        errorsList.append(error);
                    } else {
                        errorsList.append(", ");
                        errorsList.append(error);
                    }
                }
                final String errMsg = errorsList.toString();
                Log.error(CollateralUtilities.class, errMsg);
                errors.clear();
                throw new Exception(errMsg);
            }
        }

        return plMark;
    }

    /**
     * Method for create PLMarks
     *
     * @param tradeId   id for the trade
     * @param bookID
     * @param repoBean2 Array of strings for get the data, the currency and the mark value
     * @return
     * @throws RemoteException
     */
    private static PLMark createPLMarks(
            final Trade trade,
            final DSConnection dsCon,
            final JDate plMarkDate,
            final String pricingEnvName,
            final List<PLMarkValue> plmarkValues)
            throws RemoteException {
        final PLMark plMark =
                CollateralUtilities.createPLMarkIfNotExists(trade, dsCon, pricingEnvName, plMarkDate);
        // Fill the PLMark.
        plMark.setTradeLongId(trade.getLongId());
        plMark.setBookId(trade.getBookId());
        plMark.setPricingEnvName(pricingEnvName);
        plMark.setValDate(plMarkDate);
        // add the plMarkValues

        for (final PLMarkValue value : plmarkValues) {
            plMark.addPLMarkValue(value);
        }

        return plMark;
    }

    /**
     * @param mtmValue
     * @param mtmCurrency
     * @param mtmDate
     * @param plMarkValueName
     * @return a PLMarkValue with the given values
     */
    private static PLMarkValue createPLMarkValue(
            final Double mtmValue,
            final String mtmCurrency,
            final JDate mtmDate,
            final String plMarkValueName) {
        final PLMarkValue plMarkValue = new PLMarkValue();
        // Fill the PLMarketValue.
        plMarkValue.setMarkValue(mtmValue);
        plMarkValue.setCurrency(mtmCurrency);
        // Migration V14 - 04012016
        plMarkValue.setOriginalCurrency(mtmCurrency);
        plMarkValue.setMarkName(plMarkValueName);

        return plMarkValue;
    }

    /**
     * This method check a string decimal value and convert decimal separator to report's decimal
     * separator passed as parameter
     *
     * @param value String with decimal value
     * @param ds1   String with decimal separator from convert
     * @param ds2   String with decimal separator to convert
     * @return String
     */
    public static String convertToReportDecimalFormat(
            final String value, final String ds1, final String ds2) {
        if (value.contains(ds1)) {
            return value.replace(ds1, ds2);
        }
        return value;
    }

    /**
     * This method is used to fill the left an alfanumeric string until complete the required length.
     */
    public static String fillWithBlanks(String value, final int fieldLength) {
        // COL_OUT_015
        // Carlos Cejudo: If value string is null treat it as an empty string
        if (value == null) {
            value = "";
        }
        if (value.length() < fieldLength) {
            final int num = fieldLength - value.length();
            for (int i = 0; i < num; i++) {
                value = " " + value;
            }
        }
        if (value.length() > fieldLength) {
            value = value.substring(0, fieldLength);
        }
        return value;
    }

    /**
     * This method is used to fill the left a numeric string until complete the required length.
     */
    public static String fillWithZeros(String value, final int fieldLength) {
        // COL_OUT_015
        // Carlos Cejudo: If value string is null treat it as an empty string
        if (value == null) {
            value = "";
        }
        // empty value
        if (value.equals("")) {
            for (int i = 0; i < fieldLength; i++) {
                value = "0" + value;
            }
        } else {
            if (value.length() < fieldLength) {
                final int num = fieldLength - value.length();
                // negative value
                if (value.startsWith("-")) {
                    value = value.replace('-', '0');
                    for (int i = 0; i < (num - 1); i++) {
                        value = "0" + value;
                    }
                    value = '-' + value;
                }
                // positive value
                else {
                    for (int i = 0; i < num; i++) {
                        value = "0" + value;
                    }
                }
            }
            if (value.length() > fieldLength) {
                value = value.substring(0, fieldLength);
            }
        }

        return value;
    }

    /**
     * Clear the local cache for procing environements
     */
    public static void clearPricingEnvCache() {
        if (pricingEnvsCache != null) {
            pricingEnvsCache.clear();
        }
    }

    /**
     * This method is used to check the number of fields of a register line (Used for Import tasks)
     */
    public static boolean checkFields(final String str, final char separator, final int n) {
        int counter = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == separator) {
                counter++;
            }
        }
        return (counter == n);
    }

    public static String checkBlankFieldELBE(final String str, final int fieldLength) {
        if (str.equals("")) {
            return fillWithBlanks(str, fieldLength);
        }
        return str;
    }

    public static PricingEnv getPricingEnvFromTempCache(
            DSConnection dsCon, String pricingEnvName, JDatetime date) throws RemoteException {
        // this cahce should not be initied by the callers of this method
        if ((pricingEnvsCache == null) || (pricingEnvsCache.size() == 0)) {
            return null;
        }
        PricingEnv pricingEnv = null;
        if (!Util.isEmpty(pricingEnvName)) {
            String mtmDate = Util.dateToString(date);
            String envName = (Util.isEmpty(mtmDate) ? pricingEnvName : mtmDate + "_" + pricingEnvName);
            if (pricingEnvsCache.get(envName) == null) {
                pricingEnv = dsCon.getRemoteMarketData().getPricingEnv(pricingEnvName, date);
                pricingEnvsCache.put(envName, pricingEnv);

            } else {
                pricingEnv = pricingEnvsCache.get(envName);
            }
        }

        return pricingEnv;
    }

    /**
     * format a quote value according its currency pair definition
     *
     * @param quoteValue
     * @param ccy1
     * @param ccy2
     * @return
     * @throws MarketDataException
     */
    public static DisplayValue formatFXQuote(Double quoteValue, String ccy1, String ccy2) {
        if (quoteValue == null) {
            return null;
        }
        int defaultDigit = 4;
        try {
            if (!ccy1.equals(ccy2)) {
                CurrencyPair cp = LocalCache.getCurrencyPair(ccy1, ccy2);
                if (cp == null) {
                    cp = LocalCache.getCurrencyPair(ccy2, ccy1);
                }
                if (cp != null) {
                    defaultDigit = cp.getRounding();
                }
            }
        } catch (MarketDataException e) {
            Log.error(CollateralUtilities.class, "Cannot retrieve currency pair", e);
        }
        return new Amount(quoteValue, defaultDigit);
    }

    public static DisplayValue formatAmount(Double amount, String ccy) {
        if (amount == null) {
            return null;
        }
        int defaultDigit = 2;
        CurrencyDefault ccyDefault = LocalCache.getCurrencyDefault(ccy);
        if (ccyDefault != null) {
            defaultDigit = (int) ccyDefault.getRounding();
        }
        return new Amount(amount, defaultDigit);
    }

    public static DisplayValue formatRate(Double rate, String ccy) {
        if (rate == null) {
            return null;
        }
        int defaultDigit = 6;
        CurrencyDefault ccyDefault = LocalCache.getCurrencyDefault(ccy);
        if (ccyDefault != null) {
            defaultDigit = ccyDefault.getRateDecimals();
        }
        return new Amount(rate, defaultDigit);
    }

    /**
     * Returns the start Date of the trade if it is Repo, SecLending or CollateralExposure Trade.
     * Otherwise returns Trade Date.
     *
     * @param trade
     * @return
     */
    public static JDate getTradeStartDate(Trade trade) {
        JDate date = null;

        if (trade.getProductType().equals(Repo.REPO)) {
            date = ((Repo) trade.getProduct()).getStartDate();
        } else if (trade.getProductType().equals(SecLending.SEC_LENDING)) {
            date = ((SecLending) trade.getProduct()).getStartDate();
        } else if (trade.getProductType().equals(CollateralExposure.PRODUCT_TYPE)) {
            date = ((CollateralExposure) trade.getProduct()).getStartDate();
        } else {
            date = trade.getTradeDate().getJDate(TimeZone.getDefault());
        }

        return date;
    }

    public static Vector<String> getSystemHolidays() {
        Vector<String> sysHol = new Vector<String>();
        sysHol.add("SYSTEM");
        return sysHol;
    }

    /**
     * Can be used in the SQL where clause when you want to limit the items to 1000. It splits the
     * list passed into sublists(not exceeding noOfItems), then the sublist is converted to SQLString
     * and adds to the return List.
     *
     * @param list                List to be split
     * @param noOfItems
     * @param putEachItemInQuotes when true it puts each item in quotes like ('123','456'), other wise
     *                            it looks like (123,456)
     * @return
     */
    public static List<String> getSqlStringList(
            List<?> list, int noOfItems, boolean putEachItemInQuotes) {

        List<String> resultList = new ArrayList<String>();
        if (Util.isEmpty(list)) {
            return resultList;
        }

        int start = 0;
        for (int i = 0; i <= (list.size() / noOfItems); i++) {
            int end = (i + 1) * noOfItems;
            if (end > list.size()) {
                end = list.size();
            }
            final List<?> subList = list.subList(start, end);

            if (subList.size() > 0) {
                String collectionToSQLString = Util.collectionToSQLString(subList);
                resultList.add(collectionToSQLString);
            }
            start = end;
        }

        return resultList;
    }

    @SuppressWarnings("unused")
    private void getProductCreditRating(int productId) {
        try {
            DSConnection.getDefault().getRemoteMarketData().getProductRating(null);
        } catch (RemoteException e) {
            Log.error(CollateralUtilities.class, e);
        }
    }

    /**
     * Gets the Latest, Current Credit Rating for the given LE, ratingAgency and processDate
     *
     * @param leId
     * @param agency
     * @param processDate
     * @return
     */
    public static String getCreditRatingValue(
            final int leId, final String agency, JDate processDate) {
        JDate asOfDate = processDate;
        if (processDate == null) {
            asOfDate = JDate.getNow();
        }
        CreditRating cr = new CreditRating();
        cr.setLegalEntityId(leId);
        cr.setDebtSeniority("SENIOR_UNSECURED");
        cr.setAgencyName(agency);
        cr.setRatingType(CreditRating.CURRENT);
        cr.setAsOfDate(asOfDate);
        cr = BOCache.getLatestRating(DSConnection.getDefault(), cr);
        if (cr != null) {
            return cr.getRatingValue();
        }
        return null;
    }

    /**
     * Gets the Latest, Current Credit Rating for the given LE and ratingAgency
     *
     * @param leId
     * @param agency
     * @param processDate
     * @return
     */
    public static String getCreditRatingValue(final int leId, final String agency) {
        return getCreditRatingValue(leId, agency, JDate.getNow());
    }

    /**
     * Gets the MarginCallCreditRating for the given priority and Date
     *
     * @param mcRatingConfigId
     * @param priority
     * @param date
     * @return
     * @throws Exception
     */
    public static MarginCallCreditRating getLatestMCCreditRating(
            int mcRatingConfigId, int priority, JDate date) throws Exception {
        MarginCallCreditRating mcCreditrating = new MarginCallCreditRating();
        mcCreditrating.setMarginCallCreditRatingId(mcRatingConfigId);
        mcCreditrating.setPriority(priority);

        MarginCallCreditRating latestMarginCallCreditRating =
                ServiceRegistry.getDefault()
                        .getCollateralServer()
                        .getLatestMarginCallCreditRating(mcCreditrating, date);
        return latestMarginCallCreditRating;
    }

    /**
     * Gets the MarginCallCreditRating for the given RatingValue, Agency and Date
     *
     * @param mcRatingConfigId
     * @param ratingValue
     * @param ratingAgency
     * @param date
     * @return
     * @throws Exception
     */
    public static MarginCallCreditRating getLatestMCCreditRating(
            int mcRatingConfigId, String ratingValue, String ratingAgency, JDate date) throws Exception {

        int priority = SantCreditRatingStaticDataFilter.getGlobalPriority(ratingAgency, ratingValue);
        if (priority == -1) {
            return null;
        }

        return getLatestMCCreditRating(mcRatingConfigId, priority, date);
    }

    public static MarginCallCreditRatingConfiguration getMCRatingConfiguration(int mcRatingConfigId)
            throws Exception {

        MarginCallCreditRatingConfiguration ratingConfig =
                ServiceRegistry.getDefault()
                        .getCollateralServer()
                        .getMarginCallCreditRatingById(mcRatingConfigId);

        return ratingConfig;
    }

    /**
     * Checks if the trade still belongs to the assigned margin call config
     *
     * @param trade
     * @return
     */
    public static boolean needMrgCallReindexation(Trade trade) {

        long start = System.currentTimeMillis();
        boolean needReindex = false;
        if (trade == null) {
            return needReindex;
        }

        // get the calculated margin call contract
        CollateralConfig marginCallConfig = null;
        int mccId = 0;
        try {
            mccId = trade.getKeywordAsInt("MC_CONTRACT_NUMBER");

        } catch (final Exception e) {
            mccId = 0;
        }

        if (mccId != 0) {
            CollateralConfig tmpMarginCallConfig =
                    CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), mccId);

            // for performance reasons, let's skip the sdf check
            try {
                marginCallConfig = (CollateralConfig) tmpMarginCallConfig.clone();
                marginCallConfig.setProdStaticDataFilterName(null);
            } catch (CloneNotSupportedException e) {
                marginCallConfig = null;
            }
        }
        // Log.error(CollateralUtilities.class, "get the contract " +
        // (System.currentTimeMillis() - start) + " ms.");
        // start = System.currentTimeMillis();
        if (marginCallConfig != null) {
            needReindex = !marginCallConfig.accept(trade, DSConnection.getDefault());
        }

        // Log.error(CollateralUtilities.class, "check trade<>contract " +
        // (System.currentTimeMillis() - start) +
        // " ms.");
        if (needReindex) {
            Log.debug(
                    CollateralUtilities.class,
                    "the trade "
                            + trade.getLongId()
                            + " don't belong anymore to the contract "
                            + mccId
                            + ", it needs to be reindexed.");
        }
        Log.debug(
                CollateralUtilities.class,
                "Check if trade needs reindexation in " + (System.currentTimeMillis() - start) + " ms.");
        return needReindex;
    }

    /**
     * Explicitly check if one of the trade's properties changed: PO, LE, BOOK, Product type or
     * currency
     *
     * @param trade
     * @param oldTrade
     * @return
     */
    public static boolean needMrgCallReindexation(Trade trade, Trade oldTrade) {

        long start = System.currentTimeMillis();
        boolean needReindex = false;
        if ((trade == null) || (oldTrade == null)) {
            return needReindex;
        }
        int mccId = 0;
        try {
            mccId = trade.getKeywordAsInt("MC_CONTRACT_NUMBER");

        } catch (final Exception e) {
            mccId = 0;
        }
        // check first if the trade was indexed or not
        if (mccId == 0) {
            needReindex = true;

        } else
            // check the currency
            if (!trade.getSettleCurrency().equals(oldTrade.getSettleCurrency())) {
                needReindex = true;
            } else if (trade.getBookId() != oldTrade.getBookId()) {
                // check the po/book
                needReindex = true;
            } else if (trade.getCounterParty().getId() != oldTrade.getCounterParty().getId()) {
                // check the le
                needReindex = true;
            } else if (!trade.getProductType().equals(oldTrade.getProductType())) {
                // check the product
                needReindex = true;
            }

        if (needReindex) {
            Log.debug(
                    CollateralUtilities.class,
                    "the trade "
                            + trade.getLongId()
                            + " don't belong anymore to the contract "
                            + mccId
                            + ", it needs to be reindexed.");
        }
        Log.debug(
                CollateralUtilities.class,
                "Check if trade needs reindexation in " + (System.currentTimeMillis() - start) + " ms.");
        return needReindex;
    }

    /**
     * Trunc a double in specified number of decimals
     *
     * @param int,    number of decimals
     * @param double, number you want to trunc
     * @return double
     */
    public static double truncDecimal(int numeroDecimales, double decimal) {
        decimal = decimal * (java.lang.Math.pow(10, numeroDecimales));
        decimal = java.lang.Math.round(decimal);
        decimal = decimal / java.lang.Math.pow(10, numeroDecimales);

        return decimal;
    }

    public static JDate getEntryAttributeAsJDate(MarginCallEntryDTO entryDTO, String key) {
        JDate date = null;
        Object object = entryDTO.getAttribute(key);
        if (object == null) {
            date = null;
        } else if (object instanceof Date) {
            date = JDate.valueOf((Date) object);
        } else if (object instanceof JDate) {
            date = (JDate) object;
        }

        return date;
    }

    // public static void recalculatePLMarksIfNeeded(Trade trade, JDate
    // valuationDate) {
    //
    // DSConnection dsCon = DSConnection.getDefault();
    //
    // boolean plMarksNeedRcalc = false;
    // if (trade == null) {
    // return;
    // }
    //
    // // get the calculated margin call contract
    // MarginCallConfig marginCallConfig = null;
    // int mccId = 0;
    // try {
    // mccId = trade.getKeywordAsInt("MC_CONTRACT_NUMBER");
    //
    // } catch (final Exception e) {
    // mccId = 0;
    // }
    //
    // if (mccId != 0) {
    // marginCallConfig = BOCache.getMarginCallConfig(dsCon, mccId);
    // }
    //
    // if (marginCallConfig == null) {
    // return;
    // }
    //
    // // get the pl mark for the trade if it exists
    //
    // PLMark plMark = retrievePLMark(trade,
    // marginCallConfig.getPricingEnvName(), valuationDate);
    // if (plMark == null) {
    // // nothing to do
    // return;
    // }
    //
    // // get the NPV pl mark
    // PLMarkValue npvPLMarkValue =
    // CollateralUtilities.retrievePLMarkValue(plMark, PricerMeasure.S_NPV);
    // if (npvPLMarkValue == null) {
    // return;
    // }
    // // case we're the trade's currency is changed
    // if
    // (!trade.getProduct().getCurrency().equals(npvPLMarkValue.getCurrency()))
    // {
    // // get the pricing env
    // PricingEnv pe;
    // try {
    // pe = getPricingEnvFromTempCache(dsCon,
    // marginCallConfig.getPricingEnvName(), new JDatetime(
    // valuationDate));
    // if (pe == null) {
    // pe =
    // dsCon.getRemoteMarketData().getPricingEnv(marginCallConfig.getPricingEnvName(),
    // new JDatetime(valuationDate));
    // }
    // // recalculate the npv
    // convertCurrency(npvPLMarkValue.getCurrency(),
    // npvPLMarkValue.getMarkValue(), trade.getProduct()
    // .getCurrency(), valuationDate, pe);
    // } catch (RemoteException e) {
    // Log.error(CollateralUtilities.class, e);
    // }
    // }
    //
    // PLMarkValue npvBasePLMarkValue =
    // CollateralUtilities.retrievePLMarkValue(plMark,
    // SantPricerMeasure.S_NPV_BASE);
    // if (npvBasePLMarkValue == null) {
    // return;
    // }
    //
    // }

    /**
     * Splits the given Collection in to sub Lists with each sublist contains atmost limit no of items
     */
    public static <T> List<List<T>> splitCollection(Collection<T> collection, int limit) {
        List<List<T>> finalList = new ArrayList<List<T>>();

        List<T> list = new ArrayList<T>(collection);
        int start = 0;

        for (int i = 0; i <= (list.size() / limit); i++) {
            int end = (i + 1) * limit;
            if (end > list.size()) {
                end = list.size();
            }
            List<T> subList = list.subList(start, end);
            finalList.add(subList);
            start = end;
        }

        return finalList;
    }

    public static String getSantDirtyPrice(Amount allInValue, Amount nominal, Rate haircut) {
        if ((allInValue != null) && (nominal != null) && (haircut != null) && (nominal.get() != 0.0)) {
            double dirtyPrice = (allInValue.get() / (1 - haircut.get())) / nominal.get();

            final NumberFormat numberFormatter = new DecimalFormat("###,###.#######");
            return numberFormatter.format(dirtyPrice);

        } else {
            return "";
        }
    }

    /**
     * This method returns the CreditRatings with agencies in eligibleAgencies
     *
     * @param ratings
     * @param eligibleAgencies
     * @return
     */
    public static Vector<CreditRating> getEligibleAgenciesOnly(
            Vector<CreditRating> ratings, Vector<String> eligibleAgencies) {
        Vector<CreditRating> result = new Vector<CreditRating>();
        if (Util.isEmpty(eligibleAgencies)) {
            result = ratings;
        } else {
            for (CreditRating rating : ratings) {
                if (eligibleAgencies.contains(rating.getAgencyName())) {
                    result.add(rating);
                }
            }
        }

        return result;
    }

    public static JDate getMCValDate(JDate processDate) {
        if (processDate != null) {
            final int calculationOffSet = ServiceRegistry.getDefaultContext().getValueDateDays() * -1;
            final JDate valDate =
                    Holiday.getCurrent()
                            .addBusinessDays(
                                    processDate,
                                    DSConnection.getDefault().getUserDefaults().getHolidays(),
                                    calculationOffSet);
            return valDate;
        }
        return null;
    }

    public static String getValuationAgentFromContract(CollateralConfig mcc) {
        String valAgentType = mcc.getValuationAgentType();
        String valAgent = null;
        if (!Util.isEmpty(valAgentType)) {
            if (valAgentType.equals(CollateralConfig.PARTY_A)) {
                valAgent = mcc.getProcessingOrg().getCode();
            } else if (valAgentType.equals(CollateralConfig.PARTY_B)) {
                valAgent = mcc.getLegalEntity().getCode();
            } else if (valAgentType.equals(CollateralConfig.THIRD_PARTY)) {
                LegalEntity thirdParty = BOCache.getLEFromCache(mcc.getValuationAgentId());
                if (thirdParty != null) {
                    valAgent = thirdParty.getCode();
                }
            } else {
                valAgent = valAgentType;
            }
        }
        return valAgent;
    }

    /* get haircut for an eligible collateral related to a contract */
    public static double getProductHaircut(
            CollateralConfig agreement, Product product, JDate valDate) {

        CollateralFilterProxy filterProxy = new CachedCollateralFilterProxy();
        HaircutProxyFactory fact = new HaircutProxyFactory(filterProxy);
        HaircutProxy haircutProxy = fact.getProxy(agreement.getPoHaircutName());

        // get haircut value for security
        return Math.abs(
                haircutProxy.getHaircut(
                        agreement.getCurrency(),
                        new CollateralCandidate(product),
                        valDate,
                        true,
                        agreement,
                        "Pay"))
                * 100;
    }

    /**
     * Calcutes the plMarkInterface in the contract base
     *
     * @param trade
     * @param plMark
     * @param plMarkNpv
     * @throws Exception
     */
    public static void handleNpvBase(Trade trade, PLMark plMark, InterfacePLMarkBean plMarkNpv)
            throws Exception {

        final List<PLMarkValue> markValues = new ArrayList<PLMarkValue>();
        PricingEnv pricingEnv = null;
        CollateralConfig marginCallConfig = null;
        final DSConnection dsCon = DSConnection.getDefault();
        int mccId = 0;

        if (plMark == null) {
            throw new Exception("PLMark cannot be null");
        }

        // get contract id
        try {
            mccId = trade.getKeywordAsInt("MC_CONTRACT_NUMBER");

        } catch (final Exception e) {
            mccId = 0;
        }

        if (mccId == 0) {
            final ArrayList<CollateralConfig> eligibleMarginCallConfigs =
                    SantReportingUtil.getSantReportingService(DSConnection.getDefault())
                            .getEligibleMarginCallConfigs(trade);
            if (!Util.isEmpty(eligibleMarginCallConfigs) && (eligibleMarginCallConfigs.size() == 1)) {
                marginCallConfig = eligibleMarginCallConfigs.get(0);
            }

        } else {
            marginCallConfig =
                    CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), mccId);
        }

        double mtmAmount = plMarkNpv.getPlMarkValue();
        final JDate mtmDate = plMarkNpv.getPlMarkDate();

        // if MCC was found
        if (marginCallConfig != null) {

            String mtmCcy = marginCallConfig.getCurrency();

            String mtmDateString = Util.dateToString(mtmDate);
            String envName =
                    (Util.isEmpty(mtmDateString)
                            ? marginCallConfig.getPricingEnvName()
                            : mtmDateString + "_" + marginCallConfig.getPricingEnvName());

            if (pricingEnvsCache.get(envName) == null) {
                pricingEnv =
                        dsCon
                                .getRemoteMarketData()
                                .getPricingEnv(marginCallConfig.getPricingEnvName(), trade.getTradeDate());
                pricingEnvsCache.put(envName, pricingEnv);

            } else {
                pricingEnv = pricingEnvsCache.get(envName);
            }

            // handle NPV pl Mark
            mtmAmount =
                    CollateralUtilities.convertCurrency(
                            plMarkNpv.getPlMarkCurrency(), mtmAmount, mtmCcy, mtmDate, pricingEnv);

            // NPV without haircut and in the base currency
            PLMarkValue npvBaseValue =
                    CollateralUtilities.buildPLMarkValue(
                            SantPricerMeasure.S_NPV_BASE,
                            mtmCcy,
                            mtmAmount,
                            CollateralStaticAttributes.PL_MARK_AMEND_DEFAULT_REASON);

            markValues.add(npvBaseValue);
            // add to plMark before saving it
            plMark.addPLMarkValue(npvBaseValue);
        }
    }

    /**
     * Get domain values for a domain name
     *
     * @param domainName
     * @return
     */
    public static Vector<String> getDomainValues(String domainName) {
        return LocalCache.getDomainValues(DSConnection.getDefault(), domainName);
    }

    /**
     * Get comment for a domain value
     *
     * @param fatherDomainName
     * @param domainName
     * @return
     */
    public static String getDomainValueComment(String fatherDomainName, String domainName) {
        return LocalCache.getDomainValueComment(
                DSConnection.getDefault(), fatherDomainName, domainName);
    }

    /**
     * Update domain value comment
     *
     * @param domainName
     * @param domainValue
     * @param comment
     */
    public static void updateDomainValue(String domainName, String domainValue, String comment)
            throws RemoteException {

        DSConnection.getDefault()
                .getRemoteReferenceData()
                .addDomainValue(
                        domainName,
                        domainValue,
                        comment,
                        DSConnection.getDefault()
                                .getRemoteReferenceData()
                                .getObjectVersion("DomainValues", 0, null),
                        "user");
    }

    public static void removeDomainValue(String domainName, String domainValue)
            throws RemoteException {

        DSConnection.getDefault()
                .getRemoteReferenceData()
                .removeDomainValue(
                        domainName,
                        domainValue,
                        DSConnection.getDefault()
                                .getRemoteReferenceData()
                                .getObjectVersion("DomainValues", 0, null),
                        "user");
    }

    /**
     * Get file from path and specific date
     *
     * @param path
     * @param fileName
     * @param date
     * @return
     */
    public static File getFile(String path, String fileName, JDate date) {

        final String fileNameFilter = fileName;
        // name filter
        FilenameFilter filter =
                new FilenameFilter() {
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

    /**
     * @param domainName to search
     * @return a map with the set {k=domainValue, v=domainComment}
     */
    public static synchronized Map<String, String> initDomainValueComments(final String domainName) {

        if ((domainValueCommentMap != null) && (domainValueCommentMap.size() > 0)) {
            return domainValueCommentMap;
        }

        domainValueCommentMap =
                (domainValueCommentMap != null ? domainValueCommentMap : new HashMap<String, String>());
        Vector<String> domainValues = getDomainValues(domainName);

        for (String dv : domainValues) {

            final String domainComment = getDomainValueComment(domainName, dv);
            domainValueCommentMap.put(dv, domainComment);
        }
        return domainValueCommentMap;
    }

    // GSM 15/07/15. SBNA Multi-PO Common methods to filter POs in different
    // code sections

    /**
     * @param reportTemplate
     * @return a String of PO IDs separated by commas read from template attributes
     */
    public static String filterPoIdsByTemplate(final ReportTemplate reportTemplate) {

        return (String) reportTemplate.get(PROCESSING_ORG_IDS);
    }

    /**
     * @param reportTemplate
     * @return a String of PO Names separated by commas read from template attributes
     */
    public static String filterPoNamesByTemplate(final ReportTemplate reportTemplate) {

        return (String) reportTemplate.get(PROCESSING_ORG_NAMES);
    }

    /**
     * @param reportTemplate
     * @return a String of PO IDs separated by commas, if null tries to return a set of Names,
     * otherwise will return null
     */
    public static String filterPoByTemplate(final ReportTemplate reportTemplate) {

        final String leIds = filterPoIdsByTemplate(reportTemplate);

        if (!Util.isEmpty(leIds)) {
            return leIds;
        }

        final String leNames = filterPoNamesByTemplate(reportTemplate);
        if (!Util.isEmpty(leNames)) {
            return leNames;
        }
        return null;
    }

    /**
     * @param reportTemplate name
     * @param contractId     ID of the MC contract
     * @return true POs is not contained in attribute "ProcessingOrg" of the template
     */
    public static boolean filterPoByTemplate(
            final ReportTemplate reportTemplate, final Integer contractId) {

        @SuppressWarnings("static-access") final CollateralConfig marginCallConfig =
                CacheCollateralClient.getInstance()
                        .getCollateralConfig(DSConnection.getDefault(), contractId);

        if (marginCallConfig != null) {
            return filterPoByTemplate(reportTemplate, marginCallConfig);
        }

        return false;
    }

    /**
     * @param reportTemplate to get the name
     * @param trade          to find the contract
     * @return true POs is not contained in attribute "ProcessingOrg" of the template
     */
    public static boolean filterPoByTemplate(ReportTemplate reportTemplate, Trade trade) {

        try {
            final CollateralConfig collateralContract =
                    getMarginCallConfig(trade, new ArrayList<String>());
            if (collateralContract != null) {
                return filterPoByTemplate(reportTemplate, collateralContract);
            }
        } catch (RemoteException e) {
            Log.error(CollateralUtilities.class, e);
        }
        return false;
    }

    /**
     * @param reportTemplate     name
     * @param collateralContract MC contract
     * @return true POs is not contained in attribute "ProcessingOrg" of the template
     */
    public static boolean filterPoByTemplate(
            ReportTemplate reportTemplate, CollateralConfig collateralContract) {
        return filterPoByTemplate(reportTemplate, collateralContract.getProcessingOrg().getCode());
    }

    /**
     * @param reportTemplate     name
     * @param collateralContract MC contract
     * @return true POs is not contained in attribute "ProcessingOrg" of the template
     */
    public static boolean filterPoByTemplate(
            ReportTemplate reportTemplate, String contractPOCode) {
        final String POsAllowed = reportTemplate.get(PROCESSING_ORG_NAMES);

        if (Util.isEmpty(POsAllowed)) {
            return false;
        }
        HashSet<String> posAllowedSet = new HashSet<>(Util.string2Vector(POsAllowed));

        return !posAllowedSet.contains(contractPOCode);
    }

    /**
     * @param reportTemplate
     * @return true if "ProcessingOrg" attribute in template is not empty or null
     */
    public static boolean isFilteredByST(ReportTemplate reportTemplate) {

        return !Util.isEmpty((String) reportTemplate.get(PROCESS_BY_ST));
    }

    /**
     * @param template
     * @return Collection of LegalEntities of type ProcessingOrg, build 1? by attribute OWNER_AGR_IDS,
     * otherwise by attribute name ProcessingOrg, otherwise by all LegalEntities defined as POs
     */
    public static Collection<LegalEntity> filterLEPoByTemplate(final ReportTemplate reportTemplate) {

        List<LegalEntity> legalEntities = new Vector<LegalEntity>();
        String leIds = (String) reportTemplate.get(PROCESSING_ORG_IDS);

        if (!Util.isEmpty(leIds)) {

            for (String leId : Util.string2Vector(leIds)) {

                LegalEntity legalEntity =
                        BOCache.getLegalEntity(DSConnection.getDefault(), Integer.valueOf(leId));

                if (legalEntity != null) {
                    legalEntities.add(legalEntity);
                }
            }
            return legalEntities;
        }

        String leNames =
                (String) reportTemplate.get(PROCESSING_ORG_NAMES);
        if (!Util.isEmpty(leNames)) {

            for (String leId : Util.string2Vector(leNames)) {

                LegalEntity legalEntity = BOCache.getLegalEntity(DSConnection.getDefault(), leId);

                if (legalEntity != null) {
                    legalEntities.add(legalEntity);
                }
            }
            return legalEntities;
        }

        return BOCache.getLegalEntitiesForRole(
                DSConnection.getDefault(), LEGAL_ENTITY_ROLE_PROCESSING_ORG);
    }

    /**
     * @param Set                of POs Ids allowed
     * @param collateralContract
     * @return if the owner of the MC is not included in the allowed ID set of POs
     */
    public static boolean filterOwners(
            final Set<String> posIdsAllowed, final CollateralConfig collateralContract) {

        if ((posIdsAllowed == null) || posIdsAllowed.isEmpty()) {
            return false;
        }

        if (collateralContract != null) {
            LegalEntity po = collateralContract.getProcessingOrg();
            return !posIdsAllowed.contains("" + po.getId());
        }

        return false;
    }

    /**
     * @param reportTemplateName in DV SantReportTemplate.FilterPO defining banned POs
     * @param query              to modify
     * @return a String attaching the list of banned POs
     */
    public static String filterPoByQuery(final ReportTemplate reportTemplate, final String query) {

        final String finalQuery = (filterPoByQuery(reportTemplate, new StringBuffer(query))).toString();
        return finalQuery;
    }

    /**
     * @param reportTemplate in DV SantReportTemplate.FilterPO defining banned POs
     * @param filterByStOpt
     * @param stringBuffer   query to modify
     * @return StringBuffer attaching the list of banned POs
     */
    public static StringBuffer filterPoByQuery(
            final ReportTemplate reportTemplate, StringBuffer query) {

        final StringBuffer sb = new StringBuffer();
        HashSet<String> bannedPosDV = new HashSet<String>();

        if (Util.isEmpty(query.toString())) {
            return query;
        }

        if (query.toString().toLowerCase().contains("where")) {
            sb.append(" AND ");
        } else {
            sb.append(" WHERE ");
        }
        sb.append("mrgcall_config.PROCESS_ORG_ID ");

        final String allowedPOsST =
                (String) reportTemplate.get(PROCESSING_ORG_IDS);
        // empty do nothing
        if (Util.isEmpty(allowedPOsST)) {
            return query;
        }

        if (!Util.isEmpty(allowedPOsST)) {
            bannedPosDV = new HashSet<String>(Util.string2Vector(allowedPOsST));
        }

        sb.append("IN ");
        sb.append(Util.collectionToSQLString(bannedPosDV));
        sb.append(" ");
        query.append(sb);

        return query;
    }

    public static Map<Integer, List<TradeImportStatus>> saveTradesWithPLMarks(
            List<InterfaceTradeAndPLMarks> tradesToSave) throws RemoteException {

        Map<Integer, List<TradeImportStatus>> saveStatus =
                new HashMap<Integer, List<TradeImportStatus>>();

        if (!Util.isEmpty(tradesToSave)) {

            Trade trade = null;
            InterfacePLMarkBean plMark1 = null;
            InterfacePLMarkBean plMark2 = null;
            InterfacePLMarkBean plMarkIA1 = null;
            InterfacePLMarkBean plMarkIA2 = null;
            // GSM: 29/04/2014. PdV adaptation in exposure importation:
            // CLOSING_PRICE & NPV (mtm without haircut)
            InterfacePLMarkBean plMarkClosingPrice1 = null;
            InterfacePLMarkBean plMarkNpv1 = null;
            List<TradeImportStatus> tradeSaveErrors = new ArrayList<TradeImportStatus>();

            for (InterfaceTradeAndPLMarks tradeWithPlMark : tradesToSave) {

                if (tradeWithPlMark != null) {
                    trade = tradeWithPlMark.getTrade();
                    plMark1 = tradeWithPlMark.getPlMark1();
                    plMark2 = tradeWithPlMark.getPlMark2();
                    plMarkIA1 = tradeWithPlMark.getPlMarkIA1();
                    plMarkIA2 = tradeWithPlMark.getPlMarkIA2();
                    plMarkClosingPrice1 = tradeWithPlMark.getPlMarkClosingPrice1();
                    plMarkNpv1 = tradeWithPlMark.getPlMarkNpv1();
                    // save trade and pl marks
                    tradeSaveErrors =
                            saveTradeWithPLMarks(
                                    trade, plMark1, plMark2, plMarkIA1, plMarkIA2, plMarkClosingPrice1, plMarkNpv1);
                    // add save status
                    saveStatus.put(tradeWithPlMark.getLineNumber(), tradeSaveErrors);
                }
            }
        }
        return saveStatus;
    }

    // AAP METHOD MOVED FROM SANTCOLLATERALSERVICE TOO SLOW METHOD
    /*
     * (non-Javadoc)
     *
     * @see calypsox.tk.collateral.service.RemoteSantCollateralService#
     * saveTradeWithPLMarks(com.calypso.tk.core.Trade,
     * calypsox.util.InterfacePLMarkBean, calypsox.util.InterfacePLMarkBean,
     * calypsox.util.InterfacePLMarkBean, calypsox.util.InterfacePLMarkBean,
     * java.util.List)
     */
    public long saveTradeWithPLMarks(
            Trade trade,
            InterfacePLMarkBean plMark1,
            InterfacePLMarkBean plMark2,
            InterfacePLMarkBean plMarkIA1,
            InterfacePLMarkBean plMarkIA2,
            List<TradeImportStatus> errors)
            throws RemoteException {
        long tradeId = 0;
        try {
            tradeId = DSConnection.getDefault().getRemoteTrade().save(trade);
        } catch (RemoteException e) {
            Log.error(this, e);
            TradeImportStatus error =
                    new TradeImportStatus(4, "Cannot save the trade", TradeImportStatus.ERROR);
            errors.add(error);
        }

        if (tradeId > 0) {
            Trade savedTrade = DSConnection.getDefault().getRemoteTrade().getTrade(tradeId);
            try {
                Trade copyOfTradeToSave = (Trade) savedTrade.clone();
                PLMark plMark =
                        CollateralUtilities.createPLMarkForTrade(
                                copyOfTradeToSave, plMark1, plMark2, plMarkIA1, plMarkIA2);

                // everis - improve performance avoiding write null PLMarks 11/07/2017
                if (plMark != null) {
                    // v14 Migration GSM 22/03/2016 - save PLMarks into DB
                    Collection<PLMark> collectionPlMark = new Vector<>(1);
                    collectionPlMark.add(plMark);
                    DSConnection.getDefault().getRemoteMark().saveMarksWithAudit(collectionPlMark, false);
                    // this.marketDataServerImpl.savePLMark(plMark);
                    copyOfTradeToSave.setAction(Action.AMEND);
                    DSConnection.getDefault().getRemoteTrade().save(copyOfTradeToSave);

                } else {
                    Log.error(
                            CollateralUtilities.class,
                            "\n PlMark is null for Trade "
                                    + copyOfTradeToSave.getLongId()
                                    + "-ccy: "
                                    + copyOfTradeToSave.getTradeCurrency());
                    TradeImportStatus error =
                            new TradeImportStatus(11, "Error creating PL Mark. ", TradeImportStatus.ERROR);
                    errors.add(error);
                }

            } catch (Exception e) {
                Log.error(this, e);
                TradeImportStatus error =
                        new TradeImportStatus(11, "Error creating PL Mark. ", TradeImportStatus.ERROR);
                errors.add(error);
            }
        }
        return tradeId;
    }

    // GSM: 29/04/2014. PdV adaptation in exposure importation:
    // CollateralExposure.SECURITY_LENDING must save the
    // closing price
    // AAP METHOD MOVED FROM SANTCOLLATERALSERVICE
    public static List<TradeImportStatus> saveTradeWithPLMarks(
            Trade trade,
            InterfacePLMarkBean plMark1,
            InterfacePLMarkBean plMark2,
            InterfacePLMarkBean plMarkIA1,
            InterfacePLMarkBean plMarkIA2,
            InterfacePLMarkBean plMarkClosingPrice1,
            InterfacePLMarkBean plMarkNpv)
            throws RemoteException {

        long start = System.currentTimeMillis();
        long intstart = System.currentTimeMillis();

        List<TradeImportStatus> errors = new ArrayList<>();
        long tradeId = trade.getLongId();

        try {
            tradeId = DSConnection.getDefault().getRemoteTrade().save(trade);
            Log.debug(
                    TradeInterfaceUtils.LOG_CATERGORY,
                    " end saving a trade " + tradeId + " in " + (System.currentTimeMillis() - intstart));
            intstart = System.currentTimeMillis();

        } catch (RemoteException e) {
            Log.error(CollateralUtilities.class, e);
            TradeImportStatus error =
                    new TradeImportStatus(4, "Cannot save the trade", TradeImportStatus.ERROR);
            error.setTradeId(tradeId);
            errors.add(error);
        }

        if (tradeId > 0) {

            Trade savedTrade = DSConnection.getDefault().getRemoteTrade().getTrade(tradeId);

            try {
                Trade copyOfTradeToSave = (Trade) savedTrade.clone();

                PLMark plMark =
                        CollateralUtilities.createPLMarkForTrade(
                                copyOfTradeToSave, plMark1, plMark2, plMarkIA1, plMarkIA2);

                // GSM: 29/04/2014. CollateralExposure.SECURITY_LENDING closing
                // price PLMark
                if (tradeIsExpSecLendingPLMarks(copyOfTradeToSave)) {

                    CollateralUtilities.handleClosingPrice(
                            plMark,
                            plMarkClosingPrice1.getPlMarkValue(),
                            plMarkClosingPrice1.getPlMarkCurrency());

                    CollateralUtilities.handleNPvPrice(
                            plMark, plMarkNpv.getPlMarkValue(), plMarkNpv.getPlMarkCurrency());

                    // Convert Npv into base ccy
                    CollateralUtilities.handleNpvBase(copyOfTradeToSave, plMark, plMarkNpv);
                }

                // everis - improve performance avoiding write null PLMarks 11/07/2017
                if (plMark != null) {
                    // v14 Migration GSM 22/03/2016 - save PLMarks into DB
                    Collection<PLMark> collectionPlMark = new Vector<PLMark>(1);
                    collectionPlMark.add(plMark);
                    try {
                        DSConnection.getDefault().getRemoteMark().saveMarksWithAudit(collectionPlMark, false);
                    } catch (Exception e) {
                        Log.error(
                                CollateralUtilities.class,
                                e.getLocalizedMessage()
                                        + "\n PlMark Error for Trade "
                                        + copyOfTradeToSave.getLongId()
                                        + "-ccy: "
                                        + copyOfTradeToSave.getTradeCurrency());
                    }
                } else {
                    Log.error(
                            CollateralUtilities.class,
                            "\n PlMark is null for Trade "
                                    + copyOfTradeToSave.getLongId()
                                    + "-ccy: "
                                    + copyOfTradeToSave.getTradeCurrency());
                }

                copyOfTradeToSave.setAction(Action.AMEND);
                PLMarkValue npv = CollateralUtilities.retrievePLMarkValue(plMark, SantPricerMeasure.S_NPV);

                if (npv != null) {
                    CollateralUtilities.handleUnSettledTrade(
                            copyOfTradeToSave, npv.getMarkValue(), plMark.getValDate());
                }

                DSConnection.getDefault().getRemoteTrade().save(copyOfTradeToSave);
                Log.debug(
                        TradeInterfaceUtils.LOG_CATERGORY,
                        " end saving  the trade "
                                + tradeId
                                + "  for the second time in "
                                + (System.currentTimeMillis() - intstart));
                intstart = System.currentTimeMillis();

            } catch (Exception e) {
                Log.error(CollateralUtilities.class, e);
                TradeImportStatus error =
                        new TradeImportStatus(11, "Error creating PL Mark. ", TradeImportStatus.ERROR);
                error.setTradeId(tradeId);
                errors.add(error);
            }
        }

        if (Util.isEmpty(errors) || (tradeId > 0)) {
            // if the trade is created or if it already exists in the system ,
            // then consider that the import is ok
            errors.clear();
            TradeImportStatus error = new TradeImportStatus(0, "", TradeImportStatus.OK);
            error.setTradeId(tradeId);
            errors.add(error);
        }
        Log.debug(
                TradeInterfaceUtils.LOG_CATERGORY,
                " end saving trade (whole process) "
                        + tradeId
                        + "  in "
                        + (System.currentTimeMillis() - start));

        return errors;
    }

    // AAP MOVED FROM SANTCOLLATERALSERVICE

    /**
     * @param trade
     * @param interfaceTradeBean
     * @return true is type of instrument requires saving closing price
     */
    private static boolean tradeIsExpSecLendingPLMarks(Trade trade) {

        if ((trade != null)
                && (trade.getProduct() != null)
                && (trade.getProduct() instanceof CollateralExposure)) {

            CollateralExposure product = (CollateralExposure) trade.getProduct();
            if (product.getSubType().equals(COLLATERAL_EXP_SEC_LENDING)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return maturity keyword from DV + value
     */
    public static Map<String, String> getCMAfterMaturityKeywordFromDV() {

        final Vector<String> domainValues =
                LocalCache.getDomainValues(
                        DSConnection.getDefault(), EXPOSURE_DV_KEYWORD_CM_AFTER_MATURITY_NAME);
        if (domainValues.isEmpty()) {
            Log.error(
                    CollateralUtilities.class,
                    "In DomainName "
                            + EXPOSURE_DV_KEYWORD_CM_AFTER_MATURITY_NAME
                            + "DomainValue not found. Please check configuration.");
            return null;
        }
        if (domainValues.size() > 1) {
            Log.error(
                    CollateralUtilities.class,
                    "In DomainName "
                            + EXPOSURE_DV_KEYWORD_CM_AFTER_MATURITY_NAME
                            + "More than one DomainValue found. Please check configuration.");
            return null;
        }
        final String comment =
                CollateralUtilities.getDomainValueComment(
                        EXPOSURE_DV_KEYWORD_CM_AFTER_MATURITY_NAME, domainValues.get(0));

        if (Util.isEmpty(comment)) {
            Log.error(
                    CollateralUtilities.class,
                    "DomainVAlue "
                            + EXPOSURE_DV_KEYWORD_CM_AFTER_MATURITY_NAME
                            + "has no comment -keyword value-. Please check configuration.");
            return null;
        }

        final Map<String, String> KeywordMaturity = new HashMap<String, String>(1);
        KeywordMaturity.put(domainValues.get(0), comment);

        return KeywordMaturity;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static double getDirtyPrice(
            Product product, JDate valDate, PricingEnv pricingEnv, Vector holidays) {
        Vector<QuoteValue> vQuotes = new Vector<QuoteValue>();
        if (product != null && pricingEnv != null && !"".equals(pricingEnv.getQuoteSetName())) {
            String isin = product.getSecCode(ISIN);
            String quoteName;
            String quotesetName = pricingEnv.getQuoteSetName();
            if (!Util.isEmpty(holidays)) {
                valDate = valDate.addBusinessDays(-1, holidays);
            } else {
                valDate = valDate.addBusinessDays(-1, Util.string2Vector("SYSTEM"));
            }
            try {
                quoteName = product.getQuoteName();

                if (!Util.isEmpty(quoteName)) {
                    if (product instanceof Bond) {
                        String clausule =
                                "quote_name = "
                                        + "'"
                                        + quoteName
                                        + "' AND trunc(quote_date) = "
                                        + Util.date2SQLString(valDate)
                                        + " AND quote_set_name = '"
                                        + quotesetName
                                        + "'";
                        vQuotes = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(clausule);
                        if ((vQuotes != null) && (vQuotes.size() > 0)) {
                            return vQuotes.get(0).getClose() * 100;
                        }
                    } else if (product instanceof Equity) {
                        String clausule =
                                "quote_name = "
                                        + "'"
                                        + quoteName
                                        + "' AND trunc(quote_date) = "
                                        + Util.date2SQLString(valDate)
                                        + " AND quote_set_name = 'OFFICIAL'";
                        vQuotes = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(clausule);
                        if ((vQuotes != null) && (vQuotes.size() > 0)) {
                            return vQuotes.get(0).getClose();
                        }
                    }
                }

            } catch (RemoteException e) {
                Log.error(CollateralUtilities.class, "Cannot retrieve dirty price", e);
            }
        }
        return 0.00;
    }

    /**
     * Searches for a quote price in the quoteset of pricingEnv param. If not found, then searches
     * again in the parent quoteset.
     *
     * @return Returns the product price. Returns 0 if a quote price wasn't found after searching in
     * both quotesets.
     */
    public static double getQuotePriceWithParentQuoteSet(
            Product product, JDate valDate, PricingEnv pricingEnv) {

        if (pricingEnv != null && !Util.isEmpty(pricingEnv.getQuoteSetName())) {
            double price = getQuotePrice(product, valDate, pricingEnv.getQuoteSetName());
            if (price != 0) // price found in the quoteset of pricingEnv
                return price;
            else if ( // not found -> search in parent quoteset
                    pricingEnv.getQuoteSet() != null
                            && pricingEnv.getQuoteSet().getParent() != null
                            && pricingEnv.getQuoteSet().getParent().getName() != null)
                return getQuotePrice(product, valDate, pricingEnv.getQuoteSet().getParent().getName());
        }

        return 0.00;
    }

    /**
     * @return Price of the product
     * @author acd
     */
    public static double getQuotePrice(Product product, JDate valDate, String quotesetName) {

        if (product != null && !Util.isEmpty(quotesetName) && valDate != null) {
            String quoteName;
            String quoteType;

            try {
                quoteName = product.getQuoteName();
                quoteType = product.getQuoteType();

                if (!Util.isEmpty(quoteName)) {

                    QuoteValue value = new QuoteValue();
                    value.setName(quoteName);
                    value.setDate(valDate);
                    value.setQuoteType(quoteType);

                    if (product instanceof Bond) {
                        value.setQuoteSetName(quotesetName);
                        value = DSConnection.getDefault().getRemoteMarketData().getQuoteValue(value);

                        if ((value != null) && value.getClose() != 0.0) {
                            return value.getClose() * 100;
                        } else {
                            return getQuotePriceOLD(product, valDate, quotesetName);
                        }

                        // All price of equities set in OFFICIAL (temporarily)
                    } else if (product instanceof Equity) {

                        value.setQuoteSetName("OFFICIAL");
                        value = DSConnection.getDefault().getRemoteMarketData().getQuoteValue(value);
                        if ((value != null) && value.getClose() != 0.0) {
                            return value.getClose();
                        } else {
                            return getQuotePriceOLD(product, valDate, quotesetName);
                        }
                    }

                    Log.info(
                            CollateralUtilities.class,
                            " For date "
                                    + valDate.toString()
                                    + " and PE "
                                    + quotesetName
                                    + " QuoteValue is EMPTY");
                }

            } catch (Exception e) {
                Log.error(
                        CollateralUtilities.class, "Cannot retrieve price for product: " + product.getId(), e);
            }
        }

        return 0.00;
    }

    private static Double getQuotePriceOLD(Product product, JDate valDate, String quotesetName) {

        Vector<QuoteValue> vQuotes = new Vector<QuoteValue>();
        if (product != null) {
            String isin = product.getSecCode("ISIN");
            String quoteName;

            if (quotesetName == null) quotesetName = "";

            if (product instanceof Bond) {
                try {
                    final Vector quoteNames =
                            DSConnection.getDefault()
                                    .getRemoteMarketData()
                                    .getAllQuoteName(valDate, "Name", isin, false);
                    for (int i = 0; i < quoteNames.size(); i++) {
                        quoteName = CollateralUtilities.getQuoteNameFromISIN(isin, valDate, quoteNames, i);
                        if (!quoteName.equals("")) {
                            String clausule =
                                    "quote_name = "
                                            + "'"
                                            + quoteName
                                            + "' AND trunc(quote_date) = to_date('"
                                            + valDate
                                            + "', 'dd/mm/yy') AND quote_set_name = '"
                                            + quotesetName
                                            + "'";
                            vQuotes = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(clausule);
                            if ((vQuotes != null) && (vQuotes.size() > 0 && vQuotes.get(0).getClose() != 0)) {
                                return vQuotes.get(0).getClose() * 100;
                            } else { // GSM: 18/10/16
                                Log.info(
                                        CollateralUtilities.class,
                                        quoteName.toString()
                                                + " for date "
                                                + valDate.toString()
                                                + " and PE "
                                                + quotesetName
                                                + " is EMPTY");
                            }
                        }
                    }
                } catch (RemoteException e1) {
                    Log.error(CollateralUtilities.class, "Cannot retrieve dirty price", e1);
                }
            } else if (product instanceof Equity) {
                try {
                    quoteName = CollateralUtilities.getQuoteNameFromISIN(isin, valDate);
                    if (!quoteName.equals("")) {
                        String clausule =
                                "quote_name = "
                                        + "'"
                                        + quoteName
                                        + "' AND trunc(quote_date) = to_date('"
                                        + valDate
                                        + "', 'dd/mm/yy') AND quote_set_name = 'OFFICIAL'";
                        vQuotes = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(clausule);
                        if ((vQuotes != null) && (vQuotes.size() > 0)) {
                            return vQuotes.get(0).getClose();
                        }
                    }
                } catch (RemoteException e1) {
                    Log.error(CollateralUtilities.class, "Cannot retrieve dirty price", e1);
                }
            }
        }

        return 0.00;
    }

    // Eliminar una vez corregido el método original getDirtyPrice()
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static double getMarginCallDirtyPrice(
            Product product, JDate valDate, PricingEnv pricingEnv, Vector holidays) {
        Vector<QuoteValue> vQuotes = new Vector<QuoteValue>();
        if (product != null && pricingEnv != null && !"".equals(pricingEnv.getQuoteSetName())) {
            String isin = product.getSecCode(ISIN);
            String quoteName;
            String quotesetName = pricingEnv.getQuoteSetName();
            if (!Util.isEmpty(holidays)) {
                valDate = valDate.addBusinessDays(-1, holidays);
            } else {
                valDate = valDate.addBusinessDays(-1, Util.string2Vector("SYSTEM"));
            }
            try {
                quoteName =
                        !Util.isEmpty(product.getQuoteName())
                                ? product.getQuoteName()
                                : CollateralUtilities.getQuoteNameFromISIN(isin, valDate);

                if (!Util.isEmpty(quoteName)) {
                    if (product instanceof Bond) {
                        String clausule =
                                "quote_name = "
                                        + "'"
                                        + quoteName
                                        + "' AND trunc(quote_date) = "
                                        + Util.date2SQLString(valDate)
                                        + " AND quote_set_name = '"
                                        + quotesetName
                                        + "'";
                        vQuotes = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(clausule);
                        if ((vQuotes != null) && (vQuotes.size() > 0)) {
                            return vQuotes.get(0).getClose() * 100;
                        }
                    } else if (product instanceof Equity) {
                        String clausule =
                                "quote_name = "
                                        + "'"
                                        + quoteName
                                        + "' AND trunc(quote_date) = "
                                        + Util.date2SQLString(valDate)
                                        + " AND quote_set_name = 'OFFICIAL'";
                        vQuotes = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(clausule);
                        if ((vQuotes != null) && (vQuotes.size() > 0)) {
                            return vQuotes.get(0).getClose();
                        }
                    }
                }

            } catch (RemoteException e) {
                Log.error(CollateralUtilities.class, "Cannot retrieve dirty price", e);
            }
        }
        return 0.00;
    }

    /**
     * Checks if the trade action is applicable.
     *
     * @param transfer the trade
     * @return true if sucess, false otherwise
     */
    public static boolean isTradeActionApplicable(
            final Trade trade, final com.calypso.tk.core.Action action) {
        return TradeWorkflow.isTradeActionApplicable(trade, action, DSConnection.getDefault(), null);
    }

    /**
     * @param todayDate
     * @param holidays
     * @return
     */
    public static boolean isBusinessDay(final JDate todayDate,
                                        final Vector<String> holidays) {
        JDate aux = todayDate.addBusinessDays(1, holidays);
        aux = aux.addBusinessDays(-1, holidays);
        return todayDate.equals(aux);
    }

    /**
     * @param todayDate
     * @param holidays
     * @return
     */
    public static JDate getPreviousBusinessDay(final JDate todayDate,
                                               final Vector<String> holidays) {
        JDate aux = todayDate;
        while (!isBusinessDay(aux, holidays)) {
            aux = aux.addBusinessDays(-1, holidays);
        }
        return aux;

    }

    public static JDate getMonFridayBefore(JDate processDate) {
        JDate jDatePrev = processDate.addDays(-1);

        while (jDatePrev.getDayOfWeek() == JDate.SATURDAY || jDatePrev.getDayOfWeek() == JDate.SUNDAY) {
            jDatePrev = jDatePrev.addDays(-1);
        }

        return jDatePrev;
    }
    
    /**
     * @param plMarks
     * @throws InterruptedException
     */
    public static void savePLMarks(List<PLMark> plMarks) throws InterruptedException {
        if (!Util.isEmpty(plMarks)) {
            int size = SQL_GET_SIZE;
            Log.info("CollateralUtilities", "Saving " + plMarks.size() + " PLMarks.");
            ExecutorService exec = Executors.newFixedThreadPool(NUM_CORES);
            try {
                for (int start = 0; start < plMarks.size(); start += size) {
                    int end = Math.min(start + size, plMarks.size());
                    List<PLMark> plMarksToSave = new ArrayList<>(plMarks.subList(start, end));
                    exec.execute(
                            new Runnable() {
                                public void run() {
                                    try {
                                        DSConnection.getDefault().getRemoteMark().saveMarksWithAudit(plMarksToSave, true);
                                    } catch (PersistenceException e) {
                                        Log.error("CollateralUtilities", "Cannot save PLMarks. " + e);
                                    }
                                }
                            });
                }
            } finally {
                exec.shutdown();
                exec.awaitTermination(40, TimeUnit.MINUTES);
            }
        }
    }

    public static boolean isValidISINValue(String isin, Vector messages) {
        final String validPattern = "(.*)NO_USAR(.*)|(.*)USE(.*)|(.*)USAR(.*)|(.*)no_usar(.*)|(.*)use(.*)|(.*)usar(.*)|(.*)DUMMY(.*)";
        String isActive = LocalCache.getDomainValueComment(DSConnection.getDefault(), "CodeActivationDV", "IS_VALID_ISIN_VALUE");
        if (!Util.isEmpty(isActive) && Boolean.parseBoolean(isActive)
                && !Util.isEmpty(isin) && isin.length() != 12) {
            Pattern pattern = Pattern.compile(validPattern);
            Matcher matcher = pattern.matcher(isin);
            if (!matcher.find()) {
                messages.add("INVALID ISIN CODE, It must have 12 characters or end with _NO_USAR");
                return false;
            }
        }
        return true;
    }

    public static String getCusipFromIsin(String isin) {
        return isin.substring(2, 11);
    }

    public static boolean checkIsinCode(String isin) {
        List<String> filterByCountry = LocalCache.getDomainValues(DSConnection.getDefault(), "ProductCUSIPByPrefix");
        return Optional.ofNullable(isin).filter(s -> s.length() == 12).isPresent() && filterByCountry.contains(isin.substring(0,2));
    }

    public static BOTransfer cloneTransferIfInmutable(BOTransfer transfer){
        BOTransfer mutableTransfer=null;
        if(!transfer.isMutable()){
            try {
                mutableTransfer= (BOTransfer) transfer.clone();
            } catch (CloneNotSupportedException exc) {
                Log.warn("Transfer "+ transfer.getLongId() + " cant be clone", exc.getCause());
            }
        }else{
            mutableTransfer=transfer;
        }
        return mutableTransfer;
    }
    
    public static void updateSentinelValues(CollateralConfig margincallconfig) {
        if (margincallconfig.getId() != 0) {
            String af = margincallconfig.getAdditionalField("TH_MTA_€_NY_DISABLED");
            if (Boolean.parseBoolean(af)) {
                margincallconfig.setAdditionalField("SENTINEL_BLOCKED_PO", "BSNY");
                
                String mccCpty = retrieveMccCpty(margincallconfig);
                
                if (!mccCpty.isEmpty() && mccCpty.length() <= 255) {
                	margincallconfig.setAdditionalField("SENTINEL_BLOCKED_CPTY", mccCpty);
                }                 
                
            } else {
                margincallconfig.setAdditionalField("SENTINEL_BLOCKED_PO", "");
                margincallconfig.setAdditionalField("SENTINEL_BLOCKED_CPTY", removeMccCpty(margincallconfig));
            }
        } else{
            margincallconfig.setAdditionalField("SENTINEL_BLOCKED_PO", "");
            margincallconfig.setAdditionalField("SENTINEL_BLOCKED_CPTY", "");
            margincallconfig.setAdditionalField("TH_MTA_€_NY_DISABLED", "");
        }
    }

    public static String retrieveMccCpty(CollateralConfig marCollateralConfig) {
        StringBuilder cpty = new StringBuilder();
        String aditionalLE = removeMccCpty(marCollateralConfig);
        cpty.append(marCollateralConfig.getLegalEntity().getAuthName());
        marCollateralConfig.getAdditionalLE().forEach(s -> cpty.append(";").append(s));
        if (!aditionalLE.isEmpty()) {
            cpty.append(";").append(aditionalLE);
        }
        return cpty.toString();
    }
    
    public static String removeMccCpty(CollateralConfig marCollateralConfig) {
        StringBuilder cpty = new StringBuilder();
        String sentimentBlockedCpty = marCollateralConfig.getAdditionalField("SENTINEL_BLOCKED_CPTY");

        if (sentimentBlockedCpty != null && !sentimentBlockedCpty.isEmpty()) {
            String[] values = sentimentBlockedCpty.split(";");
            List<String> additionalLE = getLEandAdditionalLE(marCollateralConfig);

            Arrays.stream(values).filter(s -> !additionalLE.contains(s)).forEach(s -> cpty.append(s).append(";"));
        }
        return cpty.length() > 0 ? cpty.delete(cpty.length() - 1, cpty.length()).toString(): "";
    }

    public static List<String> getLEandAdditionalLE(CollateralConfig marCollateralConfig){
        List<String> additionalLE = marCollateralConfig.getAdditionalLE().stream().map(LegalEntity::getAuthName).collect(Collectors.toList());
        additionalLE.add(marCollateralConfig.getLegalEntity().getAuthName());
        return additionalLE;
    }

}
