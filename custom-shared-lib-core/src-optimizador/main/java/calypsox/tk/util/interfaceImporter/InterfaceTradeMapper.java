package calypsox.tk.util.interfaceImporter;

import calypsox.tk.collateral.pdv.importer.PDVConstants;
import calypsox.tk.util.ScheduledTaskImportCSVExposureTrades;
import calypsox.tk.util.bean.InterfaceTradeBean;
import calypsox.util.*;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static calypsox.util.TradeInterfaceUtils.*;
import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

/**
 * builds a trade from the tradeMapper generated from a flat file. 1st it transforms a trade bean (built by the
 * FileReader) into a Calypso trade 2nd Performs some checks on trade data
 *
 * @author aela
 * @version 2.3
 */
public class InterfaceTradeMapper implements TradeMapper<InterfaceTradeBean> {

    protected final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private static final String ALIAS_BOOK_SYSTEM = "ALIAS_BOOK_";
    // GSM: 13/06/2013 to add short name
    private static final String END_LONG_ALIAS_BOOK_NAME = "_LONG";
    private static final String EXPOSURE_INTERFACE_FILTERS_NAME = "ExposureInterfaceExclusionFiltersNames";
    private static final String EXPOSURE_INTERFACE_FILTERS_NAME_START = "ExposureInterfaceExclusionFiltersStartNames";
    private static Map<String, String> KEYWORD_CM_AFTER_MATURITY;
    private static final String KEYWORD_SLB_BUNDLE = "SLB_BUNDLE"; //keyword SLB Bundle
    private Vector<String> ccyList = new Vector<String>();

    // GSM: this is to try to cache the exclusion filters names and pointers, as is going to be used with each exposure
    // line. 10/07/2013
    private static Map<String, StaticDataFilter> filtersMap = null;
    private static Collection<String> exclusionFiltersNames = null;


    // GSM: Log problems with Produban. This variable lets see the SOP
    private static boolean DEB = true;

    /**
     * Trades cache
     */
    private Map<String, SantTradeContainer> tradesMap;

    /**
     * If importer is going to use trade cache
     */
    private boolean useTradesCache;

    /*
     * GSM: This boolean allows: = true, the filter names are included in the DV = false, the DV just indicates start
     * name of the filters to be used
     */
    private final static boolean DV_FILTER_NAME_TYPE = ScheduledTaskImportCSVExposureTrades.USE_FULL_DV_FILTERS_NAMES;
    // GSM: 24/04/2014. PdV adaptation in exposure importation
    public static final String COLLATERAL_EXP_SEC_LENDING = "SECURITY_LENDING";

    protected ImportContext context;

    /**
     * Constructor
     *
     * @param context
     *
     */
    public InterfaceTradeMapper(Map<String, SantTradeContainer> tradesCacheMap, ImportContext context) {
        this.context = context;
        this.tradesMap = tradesCacheMap;
        this.useTradesCache = context.isCacheTradesEnable();
        KEYWORD_CM_AFTER_MATURITY = CollateralUtilities.getCMAfterMaturityKeywordFromDV();
    }

    /**
     * transforms a trade bean (built by the FileReader) into a Calypso trade
     */
    @Override
    public Trade map(InterfaceTradeBean tradeBean) throws Exception {

        long start = System.currentTimeMillis();
        // HashMap<String, String> productSubTypeMapping = this.context.getProductSubTypeMapping();
        TradeImportTracker tradeImportTracker = this.context.getTradeImportTracker();
        // String sourceSystem = this.context.getSourceSystem();

        InterfaceTradeBean tradeBeanLeg1 = tradeBean;
        InterfaceTradeBean tradeBeanLeg2 = tradeBean.getLegTwo();

        int lineNb = tradeBean.getLineNumber();

        Trade oldTrade = null;

        // Mig. v14 - GSM:02/05/2016 - Allow by ST attribute to use trades cache in map o improved method trade by trade
        if (this.useTradesCache) {
            //careful, must have same order for key as inserted in map (Bo ref + bo system)
            final String key = tradeBeanLeg1.getBoKey();
            SantTradeContainer oldTradeContainer = this.tradesMap.get(key);

            if (oldTradeContainer != null) {
                if (!oldTradeContainer.isTradeDuplicate()) {
                    oldTrade = oldTradeContainer.getTrade();
                } else {
                    tradeImportTracker.addError(tradeBean, 9,
                            "More than one trade exist for the same BO_SYSTEM and BO_REFERENCE: "
                                    + tradeBeanLeg1.getBoSystem() + "-" + tradeBeanLeg1.getBoReference() + " Line: " + tradeBeanLeg1.getLineNumber());
                    return null;
                }
            }

        } else {
            //original call
            TradeArray existingTrades = TradeInterfaceUtils.getTradeByBORefAndBOSystem(tradeBeanLeg1.getBoSystem(),
                    tradeBeanLeg1.getBoReference());
            //optimized call, trade by trade
            //TradeInterfaceUtils.getTradeV14ByBORefAndBOSystem(tradeBeanLeg1.getBoSystem(),
            //tradeBeanLeg1.getBoReference());

            if ((existingTrades != null) && (existingTrades.size() > 0)) {
                boolean isCollateralExposure = false;
                for (Trade trade : existingTrades.getTrades()) {
                    if (trade.getProduct() instanceof CollateralExposure) {
                        oldTrade = trade;
                        isCollateralExposure = true;
                        break;
                    }
                }
                if (oldTrade == null && isCollateralExposure) {

                    tradeImportTracker.addError(tradeBean, 9,
                            "More than one trade exist for the same BO_SYSTEM and BO_REFERENCE: "
                                    + tradeBeanLeg1.getBoSystem() + "-" + tradeBeanLeg1.getBoReference() + " Line: " + tradeBeanLeg1.getLineNumber());
                    return null;
                }
            }
        }

        // handle the case of MTM, since we will not need to map the whole trade
        String mappedAction = TradeInterfaceUtils.mapIncomingTradeAction(tradeBeanLeg1.getAction());

        if (TRADE_ACTION_MTM.equals(mappedAction)) {
            if (oldTrade != null) {
                oldTrade.setAction(Action.valueOf(TRADE_ACTION_AMEND));
                CollateralExposure product = (CollateralExposure) oldTrade.getProduct();
                oldTrade.addKeyword(TRANS_TRADE_KWD_MTM_DATE, tradeBeanLeg1.getMtmDate());
                oldTrade.addKeyword(TRADE_KWD_RIG_CODE, tradeBeanLeg1.getRigCode());

                if (CollateralUtilities.isOneLegProductType(product.getUnderlyingType())) {
                    // add the mtm information as trade keyword on the trade
                    // level
                    oldTrade.addKeyword(TRANS_TRADE_KWD_MTM, tradeBeanLeg1.getMtm());
                    oldTrade.addKeyword(TRANS_TRADE_KWD_MTM_CCY, tradeBeanLeg1.getMtmCcy());

                }
                Log.debug(TradeInterfaceUtils.LOG_CATERGORY, "end mapping trade (old trade found) for line " + lineNb
                        + " in " + (System.currentTimeMillis() - start));

                // Change - When action=MTM, we should save Trade with numFrontId and book received.
                try {
                    Book book = null;
                    try {
                        // GSM: 13/06/2013. Long/Short Portfolio Development.
                        // Read first short name
                        book = CollateralUtilities.mapBook(tradeBean.getPortfolio(),
                                ALIAS_BOOK_SYSTEM + this.context.getSourceSystem());

                    } catch (Exception e3) {
                        Log.error(this, e3); //sonar
                        // In case short is not found, try long name
                        book = CollateralUtilities.mapBook(tradeBean.getPortfolio(),
                                ALIAS_BOOK_SYSTEM + this.context.getSourceSystem() + END_LONG_ALIAS_BOOK_NAME);
                    }

                    oldTrade.setBook(book);
                    tradeBean.setBook(book);
                    if (tradeBeanLeg2 != null) {
                        tradeBeanLeg2.setBook(book);
                    }
                    oldTrade.setExternalReference(tradeBean.getNumFrontId());
                    oldTrade.addKeyword(TRADE_KWD_NUM_FRONT_ID, tradeBean.getNumFrontId());
                } catch (Exception e) {
                    // this error will be logged in the check process
                    Log.error(this, e);
                }

                // return oldTrade;
            }
        } else if (TRADE_ACTION_CANCEL.equals(TradeInterfaceUtils.mapIncomingTradeAction(tradeBeanLeg1.getAction()))) {

            if (oldTrade == null) {
                tradeImportTracker.addError(tradeBean, 10, "Record CANCEL received but transaction with BO_REFERENCE:"
                        + tradeBeanLeg1.getBoReference() + " and BO_SYSTEM: " + tradeBeanLeg1.getBoSystem()
                        + " is not in the system");
                return null;
            }
            oldTrade.setAction(Action.valueOf(TRADE_ACTION_CANCEL));
            //GSM 10/05/2016 - V14 mod. 2.7.9 - Remove from cancel always
            removeMaturityKeywordCM(oldTrade);
            return oldTrade;
        }
        // map the incoming trade into a calypso trade
        Trade trade = createTradeFromInterfaceBeans(oldTrade, tradeBeanLeg1, tradeBeanLeg2);
        // apply the receivd action on te new trade
        String incomingAction = trade.getAction().toString();

        // Change request: import a new action as an MTM one
        if (TRADE_ACTION_AMEND.equals(incomingAction) || TRADE_ACTION_NEW.equals(incomingAction)) {
            if (oldTrade == null) {
                trade.setAction(Action.valueOf(TRADE_ACTION_NEW));
            } else {
                trade.setAction(Action.valueOf(TRADE_ACTION_AMEND));
            }
        } else if (TRADE_ACTION_MTM.equals(incomingAction)) {
            if (oldTrade == null) {
                trade.setAction(Action.valueOf(TRADE_ACTION_NEW));
            } else {
                oldTrade.setAction(Action.valueOf(TRADE_ACTION_AMEND));
            }
        } else if (TRADE_ACTION_MATURE.equals(TradeInterfaceUtils.mapIncomingTradeAction(tradeBeanLeg1.getAction()))) {

            //refactored v14 - GSM 10/05/2016
            boolean mtmNotNul = tradeMtmIsNotEmpty(tradeBeanLeg1, tradeBeanLeg2);

            // if we receive a MATURE action but the MTM is not equal to zero then apply amend action instead
            if (oldTrade != null) {
                if (mtmNotNul) {
                    trade.setAction(Action.valueOf(TRADE_ACTION_AMEND));
                } else {
                    trade.setAction(Action.valueOf(TRADE_ACTION_MATURE));
                }

                Log.debug(TradeInterfaceUtils.LOG_CATERGORY, "end mapping trade (old trade found) for line " + lineNb
                        + " in " + (System.currentTimeMillis() - start));
                tradeBean.setBook(oldTrade.getBook());
                // return oldTrade;
            } else {
                tradeImportTracker.addError(tradeBean, 10, "Record MATURE received but transaction with BO_REFERENCE:"
                        + tradeBeanLeg1.getBoReference() + " and BO_SYSTEM: " + tradeBeanLeg1.getBoSystem()
                        + " is not in the system");
                return null;
            }
        } else {
            tradeImportTracker.addError(tradeBean, 6, "Required field ACTION not present");
            return null;
        }

        //end all actions received from source systems
        //Mig v14 - GSM 10/05/2016 - adaptation to module 2.7.9 - Mig 14.
        if (tradeMtmIsNotEmpty(tradeBeanLeg1, tradeBeanLeg2)) {

            if (checkMaturityCondition(trade, getTradeMainLeg(tradeBeanLeg1, tradeBeanLeg2))) {
                addMaturityKeywordCM(trade);
            }

        } else {
            //if != CANCEL or mtm == 0
            removeMaturityKeywordCM(trade);
        }

        long end = System.currentTimeMillis();
        Log.debug(TradeInterfaceUtils.LOG_CATERGORY, "end mapping trade for line " + lineNb + " in " + (end - start));

        //Add bew Optionals Keywords:
        Boolean optColumns = Optional.ofNullable(this.context)
                .map(ImportContext::getTradeImportTracker)
                .map(TradeImportTracker::isOptColumns).orElse(false);

        if(null!=trade && optColumns){
            //New optionals columns
            trade.addKeyword("SBSD_MSBSD",tradeBean.getSbsdMsbsd());
            trade.addKeyword("SBS_product",tradeBean.getSbsProduct());
            trade.addKeyword("Day_Count_Convention",tradeBean.getdayCountConvention());
            trade.addKeyword("Swap_Agent_Id",tradeBean.getSwapAgentId());
            trade.addKeyword("Swap_Agent",tradeBean.getSwapAgent());
        }

        return trade;

    }

    /**
     * @param trade
     * @param tradeBeanMainLeg
     * @return true if collateral end date is before than mtm date
     */
    private boolean checkMaturityCondition(final Trade trade, final InterfaceTradeBean tradeBeanMainLeg) {

        if ((trade.getProduct() != null) && (trade.getProduct() instanceof CollateralExposure)
                || (KEYWORD_CM_AFTER_MATURITY == null || Util.isEmpty(KEYWORD_CM_AFTER_MATURITY.values()))) {

            JDate mtmDate = null;
            try {

                mtmDate = JDate.valueOf(this.dateFormat.parse(tradeBeanMainLeg.getMtmDate()));

            } catch (ParseException e) {
                Log.error(this, "Line: " + tradeBeanMainLeg.getLineNumber() + "Cannot convert to mtm date " + tradeBeanMainLeg.getMtm());
            }

            if (mtmDate != null) {

                final CollateralExposure collateral = (CollateralExposure) trade.getProduct();
                //if collateral end date is before than mtm date
                return collateral.getMaturityDate().before(mtmDate);
            }
        }
        return false;
    }

    /**
     * Removes keyword CM acceptance for mature trades
     *
     * @param trade
     */
    private void removeMaturityKeywordCM(final Trade trade) {

        if (KEYWORD_CM_AFTER_MATURITY != null) {
            final String keywordValue = KEYWORD_CM_AFTER_MATURITY.keySet().iterator().next();
            trade.addKeyword(keywordValue, "false");
        }
    }

    /**
     * add keyword CM acceptance for mature trades
     *
     * @param trade
     */
    //GSM 03/05/2016 - Adaptation to module 2.7.9 - Mig 14.
    private void addMaturityKeywordCM(final Trade trade) {

        if (KEYWORD_CM_AFTER_MATURITY != null) {
            final String keywordValue = KEYWORD_CM_AFTER_MATURITY.keySet().iterator().next();
            final String KeywordComment = KEYWORD_CM_AFTER_MATURITY.get(keywordValue);
            trade.addKeyword(keywordValue, KeywordComment);
        }
    }


    /**
     * @param tradeBeanLeg1
     * @param tradeBeanLeg2
     * @return true if mtm is != 0
     */
    private boolean tradeMtmIsNotEmpty(InterfaceTradeBean tradeBeanLeg1, InterfaceTradeBean tradeBeanLeg2) {

        String mtm1 = tradeBeanLeg1.getMtm();
        String mtm2 = null;
        if (tradeBeanLeg2 != null) {
            mtm2 = tradeBeanLeg2.getMtm();
        }

        try {
            Double mtmValue = 0.0;
            if (!Util.isEmpty(mtm1)) {
                mtmValue += Math.abs(Double.valueOf(mtm1));
            }

            if (!Util.isEmpty(mtm2)) {
                mtmValue += Math.abs(Double.valueOf(mtm2));
            }
            if (mtmValue != 0) {
                return true;
            }
        } catch (Exception e) {
            Log.error(this, e);
        }

        return false;
    }

    /**
     * checks on trade data. In case of error, updates log.
     *
     * @param tradeBeanLeg1
     * @param tradeBeanLeg2
     * @return build a calypso trade using the given tow legs
     */
    Trade createTradeFromInterfaceBeans(Trade oldTrade, InterfaceTradeBean tradeBeanLeg1,
                                        InterfaceTradeBean tradeBeanLeg2) {

        InterfaceTradeBean tradeBeanMainLeg = getTradeMainLeg(tradeBeanLeg1, tradeBeanLeg2);
        long start = System.currentTimeMillis();
        Trade trade = oldTrade;
        if (trade == null) {
            trade = new Trade();
        }
        CollateralExposure product = null;
        if ((trade.getProduct() != null) && (trade.getProduct() instanceof CollateralExposure)) {
            product = (CollateralExposure) trade.getProduct();
        } else {
            product = new CollateralExposure();
            trade.setProduct(product);
        }
        // set trade properties
        trade.setAction(Action.valueOf(TradeInterfaceUtils.mapIncomingTradeAction(tradeBeanMainLeg.getAction())));
        trade.setExternalReference(tradeBeanMainLeg.getNumFrontId());
        trade.setTraderName(DSConnection.getDefault().getUser());
        trade.setCounterParty(BOCache.getLegalEntity(DSConnection.getDefault(), tradeBeanMainLeg.getCounterparty()));
        trade.setTradeCurrency(tradeBeanMainLeg.getNominalCcy());
        trade.setSettleCurrency(tradeBeanMainLeg.getNominalCcy());
        trade.setEnteredUser(DSConnection.getDefault().getUser());

        try {
            Book book = null;
            try {
                // GSM: 13/06/2013. Short Portfolio name DEV. Read first short name
                book = CollateralUtilities
                        .mapBook(
                                tradeBeanMainLeg.getPortfolio(),
                                /* ALIAS_BOOK_SYSTEM + tradeBeanLeg1.getBoSystem() */ALIAS_BOOK_SYSTEM
                                        + this.context.getSourceSystem());

            } catch (Exception e3) {
                Log.error(this, e3); //sonar
                try {
                    // In case short is not found, try long name
                    book = CollateralUtilities.mapBook(tradeBeanMainLeg.getPortfolio(), ALIAS_BOOK_SYSTEM
                            + this.context.getSourceSystem() + END_LONG_ALIAS_BOOK_NAME);

                } catch (Exception e4) {
                    // GSM: 11/02/14. Quick fix to, in case not found, try Calypso NAME
                    book = BOCache.getBook(DSConnection.getDefault(), tradeBeanMainLeg.getPortfolio().trim());
                    if (book == null) {
                        throw new Exception("BOOK_WARNING_NO_BOOK: Book with " + tradeBeanMainLeg.getPortfolio()
                                + " doesnot exist in the system");
                    }
                    Log.warn(this, e4);//sonar
                }
            }

            // end Short Portfolio name DEV.

            trade.setBook(book);
            // for performance reasons
            if (tradeBeanLeg1 != null) {
                tradeBeanLeg1.setBook(book);
            }
            if (tradeBeanLeg2 != null) {
                tradeBeanLeg2.setBook(book);
            }
        } catch (Exception e) {
            // this error will be logged in the check process
            Log.error(this, e);
        }
        // set trade and product dates
        try {
            this.dateFormat.setLenient(false);
            JDate jValueDate = null;
            try {
                trade.setSettleDate(null);
                JDatetime jdtValDate = new JDatetime(this.dateFormat.parse(tradeBeanMainLeg.getValueDate()));
                jValueDate = jdtValDate.getJDate(TimeZone.getDefault());
                trade.setSettleDate(jValueDate);

            } catch (Exception e) {
                Log.error(this, e);
            }
            try {
                trade.setTradeDate(null);
                JDatetime jdtTradeDate = new JDatetime(this.dateFormat.parse(tradeBeanMainLeg.getTradeDate()));
                if (tradeBeanLeg1 != null && tradeBeanLeg1.isPDV()) {
                    // use Murex valueDate instead of tradeDate, PDV trade should not be eligible on Murex tradeDate
                    jdtTradeDate = new JDatetime(this.dateFormat.parse(tradeBeanMainLeg.getValueDate()));
                }
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(jdtTradeDate.getTime());
                cal.set(Calendar.AM_PM, Calendar.AM);
                cal.set(Calendar.HOUR, 2);
                trade.setTradeDate(new JDatetime(cal.getTime()));
            } catch (Exception e) {
                Log.error(this, e);
            }
            product.setEnteredDatetime(new JDatetime());
            product.setStartDate(jValueDate);
            try {
                product.setMaturityDate(null);
                product.setEndDate(null);
                if (!Util.isEmpty(tradeBeanMainLeg.getMaturityDate())) {
                    Date matDate = this.dateFormat.parse(tradeBeanMainLeg.getMaturityDate());
                    JDate jMaturityDate = JDate.valueOf(matDate);
                    product.setMaturityDate(jMaturityDate);
                    // set the end date for the product
                    product.setEndDate(jMaturityDate);
                }
            } catch (Exception e) {
                Log.error(this, e);
            }
        } catch (Exception e) {
            Log.error(this, e);
        }

        // set the product direction
        String tradeBeanDirection = tradeBeanMainLeg.getDirection();
        if ("Loan".equalsIgnoreCase(tradeBeanDirection) || "Borrower".equalsIgnoreCase(tradeBeanDirection)) {
            String direction = tradeBeanMainLeg.getDirection();
            direction = ("Loan".equalsIgnoreCase(direction) ? "Buy" : direction);
            direction = ("Borrower".equalsIgnoreCase(direction) ? "Sell" : direction);
            product.setDirection(direction, trade);

            // GSM: hotfix 1.9.20 - 845 Informe FX Spot_Campo BUY/SELL mal informado
        } else if ("Sell".equalsIgnoreCase(tradeBeanDirection) || "Buy".equalsIgnoreCase(tradeBeanDirection)) {
            String direction = tradeBeanMainLeg.getDirection();
            direction = ("Buy".equalsIgnoreCase(direction) ? "Buy" : direction);
            direction = ("Sell".equalsIgnoreCase(direction) ? "Sell" : direction);
            product.setDirection(direction, trade);
        }

        // set the product properties
        product.setSubType(this.context.getProductSubTypeMapping().get(tradeBeanMainLeg.getInstrument()));
        product.setUnderlyingType(this.context.getProductSubTypeMapping().get(tradeBeanMainLeg.getInstrument()));
        product.setCurrency(tradeBeanMainLeg.getNominalCcy());

        // set trade keywords
        trade.addKeyword(TRADE_KWD_FO_SYSTEM, tradeBeanMainLeg.getFoSystem());
        trade.addKeyword(TRADE_KWD_BO_SYSTEM, tradeBeanMainLeg.getBoSystem());
        trade.addKeyword(TRADE_KWD_BO_REFERENCE, tradeBeanMainLeg.getBoReference());
        trade.addKeyword(TRADE_KWD_STRUCTURE_ID, tradeBeanMainLeg.getStructureId());
        trade.addKeyword(TRADE_KWD_NUM_FRONT_ID, tradeBeanMainLeg.getNumFrontId());
        // GSM: Added for BondForward: rig_code and new DFA, EMIR fields
        trade.addKeyword(TRADE_KWD_RIG_CODE, tradeBeanLeg1.getRigCode());
        trade.addKeyword(TRADE_KWD_USI_REFERENCE, tradeBeanLeg1.getUsi());
        trade.addKeyword(TRADE_KWD_SD_MSP, tradeBeanLeg1.getSdMsp());
        trade.addKeyword(TRADE_KWD_US_PARTY, tradeBeanLeg1.getUsParty());
        trade.addKeyword(TRADE_KWD_DFA, tradeBeanLeg1.getDfa());
        trade.addKeyword(TRADE_KWD_FC_NFC, tradeBeanLeg1.getFcNfc());
        trade.addKeyword(TRADE_KWD_EMIR, tradeBeanLeg1.getEmir());
        // GSM: 22/08/13. Added the field for Port. Reconciliation
        trade.addKeyword(TRADE_KWD_UTI, tradeBeanLeg1.getUti());
        //GSM: 10/05/2016 - remove CM keyword by default
        removeMaturityKeywordCM(trade);

        if (tradeBeanLeg1.isPDV()) {
            trade.addKeyword(PDVConstants.IS_FINANCEMENT_TRADE_KEYWORD, tradeBeanLeg1.getIsFinancement());
            trade.addKeyword(PDVConstants.DVP_FOP_TRADE_KEYWORD, tradeBeanLeg1.getDeliveryType());
        } else if (tradeBeanLeg1.isSLB()) {//SLB
            if (tradeBeanLeg1.getSLBBundle() != null) {
                trade.addKeyword(KEYWORD_SLB_BUNDLE, tradeBeanLeg1.getSLBBundle());
            }
        } else {
            trade.addKeyword(TRADE_KWD_UPI, tradeBeanMainLeg.getUpi());
        }

        // try to set/reset the keyword TRADE_KWD_IMPORT_REASON
        if (!Util.isEmpty(this.context.getImportType())) {
            // GSM: Hotfix 1.9.20 - 845: "once it's marked, keep it"
            trade.addKeyword(TRADE_KWD_IMPORT_REASON, this.context.getImportType());
        }
        trade.setTradeCurrency(tradeBeanMainLeg.getNominalCcy());
        trade.setSettleCurrency(tradeBeanMainLeg.getNominalCcy());
        trade.setExternalReference(tradeBeanMainLeg.getNumFrontId());
        product.addAttribute(COL_CTX_PROP_OWNER, tradeBeanMainLeg.getProcessingOrg());

        // set the product's attribute depending on it's number of legs
        if (CollateralUtilities.isTwoLegsProductType(product.getUnderlyingType())) {

            if (!Util.isEmpty(tradeBeanMainLeg.getNominal())) {
                Double principal = CollateralUtilities.parseStringAmountToDouble(tradeBeanMainLeg.getNominal());
                if (principal != null) {
                    product.setPrincipal(principal);
                }
            }

            product.addAttribute(COL_CTX_PROP_DIRECTION_1, tradeBeanLeg1.getDirection());

            if (!Util.isEmpty(tradeBeanLeg1.getNominal())) {
                product.addAttribute(COL_CTX_PROP_NOMINAL_1, getValueAsAmount(tradeBeanLeg1.getNominal()));
            }
            product.addAttribute(COL_CTX_PROP_CCY_1, tradeBeanLeg1.getNominalCcy());

            if (!Util.isEmpty(tradeBeanLeg1.getMtm())) {
                product.addAttribute(COL_CTX_PROP_MTM_1, getValueAsAmount(tradeBeanLeg1.getMtm()));
            }
            product.addAttribute(COL_CTX_PROP_MTM_CCY_1, tradeBeanLeg1.getMtmCcy());
            product.addAttribute(COL_CTX_PROP_UNDERLYING_TYPE_1, tradeBeanLeg1.getUnderlayingType());
            product.addAttribute(COL_CTX_PROP_UNDERLYING_1, tradeBeanLeg1.getUnderlaying());

            if (!Util.isEmpty(tradeBeanLeg1.getClosingPriceDaily())) {
                product.addAttribute(COL_CTX_PROP_CLOSING_PRICE_1,
                        getValueAsAmount(tradeBeanLeg1.getClosingPriceDaily()));
            }
            if (!Util.isEmpty(tradeBeanLeg2.getClosingPriceDaily())) {
                product.addAttribute(COL_CTX_PROP_CLOSING_PRICE_2,
                        getValueAsAmount(tradeBeanLeg2.getClosingPriceDaily()));
            }
            product.addAttribute(COL_CTX_PROP_DIRECTION_2, tradeBeanLeg2.getDirection());

            if (!Util.isEmpty(tradeBeanLeg2.getNominal())) {
                product.addAttribute(COL_CTX_PROP_NOMINAL_2, getValueAsAmount(tradeBeanLeg2.getNominal()));
            }

            if (!Util.isEmpty(tradeBeanLeg2.getNominal())) {
                product.addAttribute(COL_CTX_PROP_CCY_2, tradeBeanLeg2.getNominalCcy());
            }
            product.addAttribute(COL_CTX_PROP_MTM_2, getValueAsAmount(tradeBeanLeg2.getMtm()));
            product.addAttribute(COL_CTX_PROP_MTM_CCY_2, tradeBeanLeg2.getMtmCcy());
            product.addAttribute(COL_CTX_PROP_UNDERLYING_2, tradeBeanLeg2.getUnderlaying());
            product.addAttribute(COL_CTX_PROP_UNDERLYING_TYPE_2, tradeBeanLeg2.getUnderlayingType());

            // this will be used by the rule SantCalMarginCall
            trade.addKeyword(TRANS_TRADE_KWD_MTM_DATE, tradeBeanLeg1.getMtmDate());

        } else if (CollateralUtilities.isOneLegProductType(product.getUnderlyingType())) {

            if (!Util.isEmpty(tradeBeanLeg1.getNominal())) {
                Double principal = CollateralUtilities.parseStringAmountToDouble(tradeBeanLeg1.getNominal());
                if (principal != null) {
                    product.setPrincipal(principal);
                }
            }

            product.addAttribute(COL_CTX_PROP_UNDERLYING, tradeBeanLeg1.getUnderlaying());
            product.addAttribute(COL_CTX_PROP_UNDERLYING_TYPE, tradeBeanLeg1.getUnderlayingType());
            // GSM: Bond Forward

            if (!Util.isEmpty(tradeBeanLeg1.getClosingPriceDaily())) {
                product.addAttribute(COL_CTX_PROP_CLOSING_PRICE, getValueAsAmount(tradeBeanLeg1.getClosingPriceDaily()));
            }

            if (!Util.isEmpty(product.getSubType()) && product.getSubType().endsWith("_OPTION")) {
                product.addAttribute(COL_CTX_PROP_CALL_PUT, tradeBeanLeg1.getCallPut());
            }

            // add the mtm information as trade keyword on the trade level
            trade.addKeyword(TRANS_TRADE_KWD_MTM, tradeBeanLeg1.getMtm());
            trade.addKeyword(TRANS_TRADE_KWD_MTM_CCY, tradeBeanLeg1.getMtmCcy());
            trade.addKeyword(TRANS_TRADE_KWD_MTM_DATE, tradeBeanLeg1.getMtmDate());
        }

        if ("REPO".equals(product.getSubType())) {

            Percentage hairCut = new Percentage();
            Percentage repoRate = new Percentage();
            if (!Util.isEmpty(tradeBeanLeg1.getClosingPriceStart())) {
                product.addAttribute(TRD_IMP_FIELD_CLOSING_PRICE_AT_START,
                        getValueAsAmount(tradeBeanLeg1.getClosingPriceStart()));
            }
            if (!Util.isEmpty(tradeBeanLeg1.getNominalSec())) {
                product.addAttribute(TRD_IMP_FIELD_NOMINAL_SEC, getValueAsAmount(tradeBeanLeg1.getNominalSec()));
            }
            product.addAttribute(TRD_IMP_FIELD_NOMINAL_SEC_CCY, tradeBeanLeg1.getNominalSecCcy());
            if (!Util.isEmpty(tradeBeanLeg1.getHaircut())) {
                Double hairCutValue = CollateralUtilities.parseStringAmountToDouble(tradeBeanLeg1.getHaircut());
                if (hairCutValue != null) {
                    hairCut.set(hairCutValue);
                }

                product.addAttribute(TRD_IMP_FIELD_HAIRCUT, hairCut);
            }
            product.addAttribute(TRD_IMP_FIELD_HAIRCUT_DIRECTION, tradeBeanLeg1.getHaircutDirection());

            if (!Util.isEmpty(tradeBeanLeg1.getRepoRate())) {
                Double repotRateValue = CollateralUtilities.parseStringAmountToDouble(tradeBeanLeg1.getRepoRate());
                if (repotRateValue != null) {
                    repoRate.set(repotRateValue);
                }
                product.addAttribute(TRD_IMP_FIELD_REPO_RATE, repoRate);
            }

            // GSM: Added Product context data for the BOND FORWARD
        } else if ("BOND_FORWARD".equals(product.getSubType())) {

            if (!Util.isEmpty(tradeBeanLeg1.getNominalSec())) {
                product.addAttribute(TRD_IMP_FIELD_NOMINAL_SEC, getValueAsAmount(tradeBeanLeg1.getNominalSec()));
            }

            if (!Util.isEmpty(tradeBeanLeg1.getNominalSecCcy())) {
                product.addAttribute(TRD_IMP_FIELD_NOMINAL_SEC_CCY, tradeBeanLeg1.getNominalSecCcy());
            }

            if (!Util.isEmpty(tradeBeanLeg1.getHaircut())) {
                product.addAttribute(TRD_IMP_FIELD_HAIRCUT, tradeBeanLeg1.getHaircut());
            }

            if (!Util.isEmpty(tradeBeanLeg1.getHaircutDirection())) {
                product.addAttribute(TRD_IMP_FIELD_HAIRCUT_DIRECTION, tradeBeanLeg1.getHaircutDirection());
            }

            if (!Util.isEmpty(tradeBeanLeg1.getUnderlaying())) {
                product.addAttribute(COL_CTX_PROP_UNDERLYING, tradeBeanLeg1.getUnderlaying());
            }

            if (!Util.isEmpty(tradeBeanLeg1.getUnderlayingType())) {
                product.addAttribute(COL_CTX_PROP_UNDERLYING_TYPE, tradeBeanLeg1.getUnderlayingType());
            }
            // GSM: WTF closing price at start incidence
            if (!Util.isEmpty(tradeBeanLeg1.getClosingPriceStart())) {
                product.addAttribute(TRD_IMP_FIELD_CLOSING_PRICE_AT_START,
                        getValueAsAmount(tradeBeanLeg1.getClosingPriceStart()));
            }
            if (!Util.isEmpty(tradeBeanLeg1.getClosingPriceDaily())) {
                product.addAttribute(TRD_IMP_FIELD_CLOSING_PRICE,
                        getValueAsAmount(tradeBeanLeg1.getClosingPriceDaily()));
            }

            // GSM: 24/04/2014. PdV adaptation in exposure importation: mtm calculation & new keywords
        } else if (COLLATERAL_EXP_SEC_LENDING.equals(product.getSubType()) && !tradeBeanLeg1.isPDV()) {

            InterfaceSecLendingTradeMapper secLendingMapper = new InterfaceSecLendingTradeMapper(trade, tradeBeanLeg1,
                    this.context.getTradeImportTracker());
            secLendingMapper.buildSecLending();

        }

        // Add principal amount from Murex info
        if (tradeBeanLeg1.isPDV()) {
            final Double lotSize = CollateralUtilities.parseStringAmountToDouble(tradeBeanLeg1.getLotSize());
            final Double quantity = CollateralUtilities.parseStringAmountToDouble(tradeBeanLeg1.getNominal());

            if ((quantity != null && lotSize != null) && (quantity != 0.0d && lotSize != 0.0d)) {

                final Double nominal = quantity * lotSize;
                if (nominal != null) {
                    trade.getProduct().setPrincipal(nominal);
                }
            }
        }

        long end = System.currentTimeMillis();
        Log.debug(TradeInterfaceUtils.LOG_CATERGORY, "end creating a bean for line " + tradeBeanLeg1.getLineNumber()
                + " in " + (end - start));
        return trade;
    }

    /**
     * Ge the main leg to use to create the trade, the trade information will be retreived from this leg.
     *
     * @param tradeBeanLeg1
     * @param tradeBeanLeg2
     * @return
     */
    private InterfaceTradeBean getTradeMainLeg(InterfaceTradeBean tradeBeanLeg1, InterfaceTradeBean tradeBeanLeg2) {
        String tradeBean1Direction = "";
        if (tradeBeanLeg1 != null) {
            tradeBean1Direction = tradeBeanLeg1.getDirection();
        }
        String tradeBean2Direction = "";
        if (tradeBeanLeg2 != null) {
            tradeBean2Direction = tradeBeanLeg2.getDirection();
        }
        if ("Loan".equalsIgnoreCase(tradeBean1Direction) || "Buy".equalsIgnoreCase(tradeBean1Direction)) {
            return tradeBeanLeg1;
        } else if ("Loan".equalsIgnoreCase(tradeBean2Direction) || "Buy".equalsIgnoreCase(tradeBean2Direction)) {
            return tradeBeanLeg2;
        }
        // by default return the leg1, it should not happen!!!
        return tradeBeanLeg1;
    }

    /**
     * @param closingPriceDaily
     * @return the double value corresponding to the given string
     */
    private Amount getValueAsAmount(String closingPriceDaily) {
        Double doubleValue = CollateralUtilities.parseStringAmountToDouble(closingPriceDaily);
        if (doubleValue != null) {
            return new Amount(doubleValue);
        }

        return null;
    }

    // JRL Migration 14.4
    // /**
    // * @param closingPriceDaily
    // * @return the double value corresponding to the given string
    // */
    // private Double CollateralUtilities.parseStringAmountToDouble(String
    // closingPriceDaily) {
    // try {
    // return new Double(closingPriceDaily);
    // } catch (Exception e) {
    // Log.error(this, e);
    // return null;
    // }
    // }

    // Verification in trade wrapper to check if the trade is valid!

    /**
     * @param trade           trade to validate
     * @return
     */
    public boolean isValid(Trade trade, InterfaceTradeBean interfaceTradeBean) {

        long start = System.currentTimeMillis();

        InterfaceTradeBean tradeBean = interfaceTradeBean;
        InterfaceTradeBean tradeBeanLegTwo = interfaceTradeBean.getLegTwo();
        int lineNumber = interfaceTradeBean.getLineNumber();
        String lineContent = interfaceTradeBean.getLineContent();
        boolean tradeIsValid = true;
        if (trade == null) {
            return false;
        }

        CollateralExposure product = (CollateralExposure) trade.getProduct();

        if (Util.isEmpty(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE))) {
            TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRADE_KWD_BO_REFERENCE,
                    tradeBean.getBoReference());
            error.setRowBeingImportedContent(lineContent);
            error.setRowBeingImportedNb(lineNumber);
            error.setTradeId(trade.getLongId());
            error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
            error.setTradeBean(tradeBean);
            addError(error, tradeBean);
            tradeIsValid = false;
        }

        if (Util.isEmpty(trade.getKeywordValue(TRADE_KWD_BO_SYSTEM))) {
            TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRADE_KWD_BO_SYSTEM,
                    tradeBean.getBoSystem());
            error.setRowBeingImportedContent(lineContent);
            error.setRowBeingImportedNb(lineNumber);
            error.setTradeId(trade.getLongId());
            error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
            error.setTradeBean(tradeBean);
            addError(error, tradeBean);
            tradeIsValid = false;
        }

        // if it's a cancel action, then don't validate the rest, the trade will be canceled anyway and it will not be
        // be canceled anyway and it will not be
        // updated.
        if ((trade.getAction() != null) && TRADE_ACTION_CANCEL.equals(trade.getAction().toString())) {
            return tradeIsValid;
        }
        if (Util.isEmpty(trade.getKeywordValue(TRADE_KWD_NUM_FRONT_ID))) {
            TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRADE_KWD_NUM_FRONT_ID,
                    tradeBean.getBoReference());
            error.setRowBeingImportedContent(lineContent);
            error.setRowBeingImportedNb(lineNumber);
            error.setTradeId(trade.getLongId());
            error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
            error.setTradeBean(tradeBean);
            addError(error, tradeBean);
            tradeIsValid = false;
        }

        if (Util.isEmpty(trade.getKeywordValue(TRADE_KWD_FO_SYSTEM))) {
            TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRADE_KWD_FO_SYSTEM,
                    tradeBean.getBoReference());
            error.setRowBeingImportedContent(lineContent);
            error.setRowBeingImportedNb(lineNumber);
            error.setTradeId(trade.getLongId());
            error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
            error.setTradeBean(tradeBean);
            addError(error, tradeBean);
            tradeIsValid = false;
        }

        if (Util.isEmpty(product.getSubType())) {
            TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRD_IMP_FIELD_INSTRUMENT,
                    tradeBean.getInstrument());
            error.setRowBeingImportedContent(lineContent);
            error.setRowBeingImportedNb(lineNumber);
            error.setTradeId(trade.getLongId());
            error.setFieldName(TRD_IMP_FIELD_INSTRUMENT);
            error.setFieldValue(tradeBean.getInstrument());
            error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
            error.setTradeBean(tradeBean);
            addWarning(error, tradeBean);
            tradeIsValid = false;
        }

        if (trade.getCounterParty() == null) {
            TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRD_IMP_FIELD_COUNTERPARTY,
                    tradeBean.getCounterparty());
            error.setRowBeingImportedContent(lineContent);
            error.setRowBeingImportedNb(lineNumber);
            error.setTradeId(trade.getLongId());
            error.setFieldName(TRD_IMP_FIELD_COUNTERPARTY);
            error.setFieldValue(tradeBean.getCounterparty());
            error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
            error.setTradeBean(tradeBean);
            addWarning(error, tradeBean);
            tradeIsValid = false;
        }
        // get the saved book from the bean
        if (tradeBean.getBook() == null) {
            // if it's null then look for it from the trade (it load it from the books cache)
            TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRD_IMP_FIELD_PORTFOLIO,
                    tradeBean.getPortfolio());
            error.setRowBeingImportedContent(lineContent);
            error.setRowBeingImportedNb(lineNumber);
            error.setTradeId(trade.getLongId());
            error.setFieldName(TRD_IMP_FIELD_PORTFOLIO);
            error.setFieldValue(tradeBean.getPortfolio());
            error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
            error.setTradeBean(tradeBean);
            addWarning(error, tradeBean);
            tradeIsValid = false;
        } else {
            // now since the book is set, check that the attribute owner is well
            // set
            if (Util.isEmpty((String) product.getAttribute(TRD_IMP_FIELD_OWNER))) {
                TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRD_IMP_FIELD_OWNER,
                        tradeBean.getProcessingOrg());
                error.setRowBeingImportedContent(lineContent);
                error.setRowBeingImportedNb(lineNumber);
                error.setTradeId(trade.getLongId());
                error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
                error.setTradeBean(tradeBean);
                addError(error, tradeBean);
                tradeIsValid = false;
            }

            // check that the book is on the received processing
            // organization
            LegalEntity bookLe = trade.getBook().getLegalEntity();
            if ((bookLe != null) && !Util.isEmpty(tradeBean.getProcessingOrg())) {
                if (!tradeBean.getProcessingOrg().equals(bookLe.getCode())) {
                    TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRD_IMP_FIELD_OWNER
                            + TRD_IMP_FIELD_PORTFOLIO, tradeBean.getProcessingOrg());
                    error.setRowBeingImportedContent(lineContent);
                    error.setRowBeingImportedNb(lineNumber);
                    error.setTradeId(trade.getLongId());
                    error.setFieldName(TRD_IMP_FIELD_PORTFOLIO);
                    error.setFieldValue(tradeBean.getPortfolio());
                    error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
                    error.setTradeBean(tradeBean);
                    addWarning(error, tradeBean);
                    tradeIsValid = false;
                }
            }

            if (Util.isEmpty(trade.getKeywordValue(TRADE_KWD_UPI))) {
                if (checkUPI(bookLe)) {
                    TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRADE_KWD_UPI,
                            tradeBean.getInstrument());
                    error.setRowBeingImportedContent(lineContent);
                    error.setRowBeingImportedNb(lineNumber);
                    error.setTradeId(trade.getLongId());
                    error.setFieldName(TRADE_KWD_UPI);
                    error.setFieldValue(tradeBean.getInstrument());
                    error.setBoReference(trade.getKeywordValue(TRADE_KWD_UPI));
                    error.setTradeBean(tradeBean);
                    addWarning(error, tradeBean);
                }
            }
        }

        if (product.getMaturityDate() == null && !tradeBean.isPDV()) {
            TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRD_IMP_FIELD_MATURITY_DATE,
                    tradeBean.getMaturityDate());
            error.setRowBeingImportedContent(lineContent);
            error.setRowBeingImportedNb(lineNumber);
            error.setTradeId(trade.getLongId());
            error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
            error.setTradeBean(tradeBean);
            addError(error, tradeBean);
            tradeIsValid = false;
        }

        if (trade.getTradeDate() == null) {
            TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRD_IMP_FIELD_TRADE_DATE,
                    tradeBean.getTradeDate());
            error.setRowBeingImportedContent(lineContent);
            error.setRowBeingImportedNb(lineNumber);
            error.setTradeId(trade.getLongId());
            error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
            error.setTradeBean(tradeBean);
            addError(error, tradeBean);
            tradeIsValid = false;
        }

        if (trade.getSettleDate() == null) {
            TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRD_IMP_FIELD_VALUE_DATE,
                    tradeBean.getValueDate());
            error.setRowBeingImportedContent(lineContent);
            error.setRowBeingImportedNb(lineNumber);
            error.setTradeId(trade.getLongId());
            error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
            error.setTradeBean(tradeBean);
            addError(error, tradeBean);
            tradeIsValid = false;
        }
        String tradeBeanDirection = tradeBean.getDirection();
        if (Util.isEmpty(tradeBeanDirection)
                || (!"Sell".equalsIgnoreCase(tradeBeanDirection) && !"Buy".equalsIgnoreCase(tradeBeanDirection)
                && !"Loan".equalsIgnoreCase(tradeBeanDirection) && !"Borrower"
                .equalsIgnoreCase(tradeBeanDirection))) {

            TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRD_IMP_FIELD_DIRECTION,
                    tradeBean.getDirection());
            error.setRowBeingImportedContent(lineContent);
            error.setRowBeingImportedNb(lineNumber);
            error.setTradeId(trade.getLongId());
            error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
            error.setTradeBean(tradeBean);
            addError(error, tradeBean);
            tradeIsValid = false;
        }

        // TRD_IMP_FIELD_CLOSING_PRICE_AT_START
        // specific fields for REPO products
        if ("REPO".equals(product.getSubType())) {

            if (product.getAttribute(TRD_IMP_FIELD_CLOSING_PRICE_AT_START) == null) {
                TradeImportStatus error = TradeInterfaceUtils
                        .getErrorForFieldName(TRD_IMP_FIELD_CLOSING_PRICE_AT_START);
                error.setRowBeingImportedContent(lineContent);
                error.setRowBeingImportedNb(lineNumber);
                error.setTradeId(trade.getLongId());
                error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
                error.setTradeBean(tradeBean);
                addError(error, tradeBean);
                tradeIsValid = false;
            }

            if (product.getAttribute(TRD_IMP_FIELD_NOMINAL_SEC) == null) {

                TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRD_IMP_FIELD_NOMINAL_SEC,
                        tradeBean.getNominalSec());
                error.setRowBeingImportedContent(lineContent);
                error.setRowBeingImportedNb(lineNumber);
                error.setTradeId(trade.getLongId());
                error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
                error.setTradeBean(tradeBean);
                addError(error, tradeBean);
                tradeIsValid = false;

            }

            if (Util.isEmpty((String) product.getAttribute(TRD_IMP_FIELD_NOMINAL_SEC_CCY))) {

                TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRD_IMP_FIELD_NOMINAL_SEC_CCY,
                        tradeBean.getNominalSecCcy());
                error.setRowBeingImportedContent(lineContent);
                error.setRowBeingImportedNb(lineNumber);
                error.setTradeId(trade.getLongId());
                error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
                error.setTradeBean(tradeBean);
                addError(error, tradeBean);
                tradeIsValid = false;

            }

            if (product.getAttribute(TRD_IMP_FIELD_HAIRCUT) == null) {
                TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRD_IMP_FIELD_HAIRCUT,
                        tradeBean.getHaircut());
                error.setRowBeingImportedContent(lineContent);
                error.setRowBeingImportedNb(lineNumber);
                error.setTradeId(trade.getLongId());
                error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
                error.setTradeBean(tradeBean);
                addError(error, tradeBean);
                tradeIsValid = false;
            }

            if (Util.isEmpty((String) product.getAttribute(TRD_IMP_FIELD_HAIRCUT_DIRECTION))) {

                TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRD_IMP_FIELD_HAIRCUT_DIRECTION,
                        tradeBean.getHaircutDirection());
                error.setRowBeingImportedContent(lineContent);
                error.setRowBeingImportedNb(lineNumber);
                error.setTradeId(trade.getLongId());
                error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
                error.setTradeBean(tradeBean);
                addError(error, tradeBean);
                tradeIsValid = false;

            }
            if (product.getAttribute(TRD_IMP_FIELD_REPO_RATE) == null) {
                TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRD_IMP_FIELD_REPO_RATE,
                        tradeBean.getRepoRate());
                error.setRowBeingImportedContent(lineContent);
                error.setRowBeingImportedNb(lineNumber);
                error.setTradeId(trade.getLongId());
                error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
                error.setTradeBean(tradeBean);
                addError(error, tradeBean);
                tradeIsValid = false;
            }
        }

        // mandatory field for two legs trade
        if (CollateralUtilities.isTwoLegsProductType(product.getUnderlyingType())) {
            if ((product.getAttribute(COL_CTX_PROP_NOMINAL_1) == null)
                    || (product.getAttribute(COL_CTX_PROP_NOMINAL_2) == null)) {
                String tradeNominal = "";
                if (product.getAttribute(COL_CTX_PROP_NOMINAL_1) == null) {
                    tradeNominal = tradeBean.getNominal();
                } else {
                    tradeNominal = tradeBeanLegTwo.getNominal();
                }

                TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRD_IMP_FIELD_NOMINAL, tradeNominal);
                error.setRowBeingImportedContent(lineContent);
                error.setRowBeingImportedNb(lineNumber);
                error.setTradeId(trade.getLongId());
                error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
                error.setTradeBean(tradeBean);
                addError(error, tradeBean);
                tradeIsValid = false;
            }

            if ((product.getAttribute(COL_CTX_PROP_DIRECTION_1) == null)
                    || (product.getAttribute(COL_CTX_PROP_DIRECTION_2) == null)) {

                String direction = "";
                if (product.getAttribute(COL_CTX_PROP_DIRECTION_1) == null) {
                    direction = tradeBean.getDirection();
                } else {
                    direction = tradeBeanLegTwo.getDirection();
                }
                TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRD_IMP_FIELD_DIRECTION, direction);
                error.setRowBeingImportedContent(lineContent);
                error.setRowBeingImportedNb(lineNumber);
                error.setTradeId(trade.getLongId());
                error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
                error.setTradeBean(tradeBean);
                addError(error, tradeBean);
                tradeIsValid = false;
            }

            if (!isValidCurrency((String) product.getAttribute(COL_CTX_PROP_CCY_1))
                    || !isValidCurrency((String) product.getAttribute(COL_CTX_PROP_CCY_2))) {

                String ccy = "";
                if (!isValidCurrency((String) product.getAttribute(COL_CTX_PROP_CCY_1))) {
                    ccy = tradeBean.getNominalCcy();
                } else {
                    ccy = tradeBeanLegTwo.getNominalCcy();
                }

                TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRD_IMP_FIELD_NOMINAL_CCY, ccy);
                error.setRowBeingImportedContent(lineContent);
                error.setRowBeingImportedNb(lineNumber);
                error.setTradeId(trade.getLongId());
                error.setFieldName(TRD_IMP_FIELD_NOMINAL_CCY);
                error.setFieldValue(ccy);
                error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
                error.setTradeBean(tradeBean);
                addError(error, tradeBean);
                tradeIsValid = false;
            }

            if ((product.getAttribute(COL_CTX_PROP_MTM_1) == null)
                    || (product.getAttribute(COL_CTX_PROP_MTM_2) == null)) {

                String mtm = "";
                if (product.getAttribute(COL_CTX_PROP_MTM_1) == null) {
                    mtm = tradeBean.getMtm();
                } else {
                    mtm = tradeBeanLegTwo.getMtm();
                }

                TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRD_IMP_FIELD_MTM, mtm);
                error.setRowBeingImportedContent(lineContent);
                error.setRowBeingImportedNb(lineNumber);
                error.setTradeId(trade.getLongId());
                error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
                error.setTradeBean(tradeBean);
                addError(error, tradeBean);
                tradeIsValid = false;
            }

            if (!isValidCurrency((String) product.getAttribute(COL_CTX_PROP_MTM_CCY_1))
                    || !isValidCurrency((String) product.getAttribute(COL_CTX_PROP_MTM_CCY_2))) {

                String mtmCcy = "";
                if (!isValidCurrency((String) product.getAttribute(COL_CTX_PROP_MTM_CCY_1))) {
                    mtmCcy = tradeBean.getMtmCcy();
                } else {
                    mtmCcy = tradeBeanLegTwo.getMtmCcy();
                }

                TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRD_IMP_FIELD_MTM_CCY, mtmCcy);
                error.setRowBeingImportedContent(lineContent);
                error.setRowBeingImportedNb(lineNumber);
                error.setTradeId(trade.getLongId());
                error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
                error.setTradeBean(tradeBean);
                addError(error, tradeBean);
                tradeIsValid = false;
            }

        } else {
            // check that the trade nominal is a parasable amount
            Double principal = null;
            try {
                principal = new Double(tradeBean.getNominal());
            } catch (Exception e) {
                Log.warn(this, e); //sonar
                principal = null;
            }
            if (Util.isEmpty(tradeBean.getNominal()) || (principal == null)) {
                TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRD_IMP_FIELD_NOMINAL,
                        tradeBean.getNominal());
                error.setRowBeingImportedContent(lineContent);
                error.setRowBeingImportedNb(lineNumber);
                error.setTradeId(trade.getLongId());
                error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
                error.setTradeBean(tradeBean);
                addError(error, tradeBean);
                tradeIsValid = false;
            }

            if (!isValidCurrency(tradeBean.getNominalCcy())) {
                TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRD_IMP_FIELD_NOMINAL_CCY,
                        tradeBean.getNominalCcy());
                error.setRowBeingImportedContent(lineContent);
                error.setRowBeingImportedNb(lineNumber);
                error.setTradeId(trade.getLongId());
                error.setFieldName(TRD_IMP_FIELD_NOMINAL_CCY);
                error.setFieldValue(tradeBean.getNominalCcy());
                error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
                error.setTradeBean(tradeBean);
                addWarning(error, tradeBean);
                tradeIsValid = false;
            }

            if (Util.isEmpty(trade.getKeywordValue(TRANS_TRADE_KWD_MTM)) && !tradeBean.isPDV()) {
                TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRD_IMP_FIELD_MTM,
                        tradeBean.getMtm());
                error.setRowBeingImportedContent(lineContent);
                error.setRowBeingImportedNb(lineNumber);
                error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
                error.setTradeBean(tradeBean);
                addError(error, tradeBean);
                tradeIsValid = false;
            }

            if (!isValidCurrency(trade.getKeywordValue(TRANS_TRADE_KWD_MTM_CCY)) && !tradeBean.isPDV()) {
                TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRD_IMP_FIELD_MTM_CCY,
                        tradeBean.getMtmCcy());
                error.setRowBeingImportedContent(lineContent);
                error.setRowBeingImportedNb(lineNumber);
                error.setTradeId(trade.getLongId());
                error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
                error.setTradeBean(tradeBean);
                addError(error, tradeBean);
                tradeIsValid = false;
            }

            if (tradeBean.isPDV()) {
                if (Util.isEmpty(trade.getKeywordValue(PDVConstants.IS_FINANCEMENT_TRADE_KEYWORD))) {
                    TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRD_IMP_FIELD_IS_FINANCEMENT,
                            tradeBean.getIsFinancement());
                    error.setRowBeingImportedContent(lineContent);
                    error.setRowBeingImportedNb(lineNumber);
                    error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
                    error.setTradeBean(tradeBean);
                    addError(error, tradeBean);
                    tradeIsValid = false;
                }

                if (Util.isEmpty(trade.getKeywordValue(PDVConstants.DVP_FOP_TRADE_KEYWORD))) {
                    TradeImportStatus error = TradeInterfaceUtils.getErrorForFieldName(TRD_IMP_FIELD_DELIVERY_TYPE,
                            tradeBean.getDeliveryType());
                    error.setRowBeingImportedContent(lineContent);
                    error.setRowBeingImportedNb(lineNumber);
                    error.setBoReference(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE));
                    error.setTradeBean(tradeBean);
                    addError(error, tradeBean);
                    tradeIsValid = false;
                }
            }
        }

        long end = System.currentTimeMillis();
        Log.debug(TradeInterfaceUtils.LOG_CATERGORY, "end cheking trade validity for line " + tradeBean.getLineNumber()
                + " in " + (end - start));
        return tradeIsValid;
    }

    private void addError(TradeImportStatus error, InterfaceTradeBean tradeBean) {
        tradeBean.setErrorChecks(true);
        this.context.getTradeImportTracker().addError(error);
    }

    private void addWarning(TradeImportStatus error, InterfaceTradeBean tradeBean) {
        tradeBean.setWarningChecks(true);
        this.context.getTradeImportTracker().addWarning(error);
    }

    public InterfaceTradeAndPLMarks getTradeAndPlMarks(Trade trade, InterfaceTradeBean interfaceTradeBean) {

        InterfaceTradeAndPLMarks tradeWithPlMarks = new InterfaceTradeAndPLMarks();
        // check mtm date and mtm value
        InterfaceTradeBean tradeBean = interfaceTradeBean;
        InterfaceTradeBean tradeBeanLegTwo = interfaceTradeBean.getLegTwo();
        int tradeNbLegs = (tradeBeanLegTwo == null ? 1 : 2);

        if (((trade.getAction() != null) && TRADE_ACTION_CANCEL.equals(trade.getAction().toString()))
                || interfaceTradeBean.isPDV()) {

            tradeWithPlMarks.setTrade(trade);
            tradeWithPlMarks.setTradeBean(tradeBean);
            tradeWithPlMarks.setLineNumber(tradeBean.getLineNumber());

            return tradeWithPlMarks;
        }

        JDate mtmDate = null;
        JDate mtmDate2 = null;
        Double mtmAmount = null;
        Double mtmAmount2 = null;
        Double mtmAmountClosingPrice = null;
        Double mtmAmountNpv = null; // without haircut

        try {
            mtmDate = JDate.valueOf(this.dateFormat.parse(tradeBean.getMtmDate()));
            if (tradeBeanLegTwo != null) {
                mtmDate2 = JDate.valueOf(this.dateFormat.parse(tradeBeanLegTwo.getMtmDate()));
            }
        } catch (ParseException e) {
            this.context.getTradeImportTracker().addError(tradeBean, 31,
                    "Required field MTM_DATE not present or not valid");
            this.context.getTradeImportTracker().incrementKOImports(tradeNbLegs, TradeImportTracker.KO_ERROR);
            return null;
        }

        try {
            mtmAmount = CollateralUtilities.parseStringAmountToDouble(tradeBean.getMtm());
            // to check that the amount was well converted, otherwise it will be
            // null. the toString is to throw an exception.
            mtmAmount.toString();
            if (tradeBeanLegTwo != null) {
                mtmAmount2 = CollateralUtilities.parseStringAmountToDouble(tradeBeanLegTwo.getMtm());
                mtmAmount2.toString();
            }
        } catch (Exception e) {
            Log.error(this, e); //sonar
            this.context.getTradeImportTracker().addError(tradeBean, 34, "Required field MTM not present or not valid");
            this.context.getTradeImportTracker().incrementKOImports(tradeNbLegs, TradeImportTracker.KO_ERROR);
            return null;
        }
        // check mtm currency, this check is useful when we receive an MTM
        // action on an existing trade

        if (Util.isEmpty(tradeBean.getMtmCcy()) || !isValidCurrency(tradeBean.getMtmCcy())) {
            this.context.getTradeImportTracker().addError(tradeBean, 35, "Required field MTM_CCY "
                    + (Util.isEmpty(tradeBean.getMtmCcy()) ? "" : tradeBean.getMtmCcy()) + " not present or not valid");
            this.context.getTradeImportTracker().incrementKOImports(tradeNbLegs, TradeImportTracker.KO_ERROR);
            return null;
        }
        if (tradeBeanLegTwo != null) {
            if (Util.isEmpty(tradeBeanLegTwo.getMtmCcy()) || !isValidCurrency(tradeBeanLegTwo.getMtmCcy())) {
                this.context.getTradeImportTracker().addError(tradeBean, 35,
                        "Required field MTM_CCY "
                                + (Util.isEmpty(tradeBeanLegTwo.getMtmCcy()) ? "" : tradeBeanLegTwo.getMtmCcy())
                                + " not present or not valid");
                this.context.getTradeImportTracker().incrementKOImports(tradeNbLegs, TradeImportTracker.KO_ERROR);
                return null;
            }
        }

        // check the Indpendent amount currency:

        if (!Util.isEmpty(tradeBean.getIndependentAmountCcy())
                && !isValidCurrency(tradeBean.getIndependentAmountCcy())) {
            this.context.getTradeImportTracker().addError(tradeBean, 54, "Required field INDEPENDENT_AMOUNT_CCY "
                    + (Util.isEmpty(tradeBean.getIndependentAmountCcy()) ? "" : tradeBean.getIndependentAmountCcy())
                    + " not present or not valid");
            this.context.getTradeImportTracker().incrementKOImports(tradeNbLegs, TradeImportTracker.KO_ERROR);
            return null;
        }

        if (tradeBeanLegTwo != null) {
            if (!Util.isEmpty(tradeBeanLegTwo.getIndependentAmountCcy())
                    && !isValidCurrency(tradeBeanLegTwo.getIndependentAmountCcy())) {
                this.context.getTradeImportTracker()
                        .addError(tradeBean, 54,
                                "Required field INDEPENDENT_AMOUNT_CCY "
                                        + (Util.isEmpty(tradeBeanLegTwo.getIndependentAmountCcy()) ? ""
                                        : tradeBeanLegTwo.getIndependentAmountCcy())
                                        + " not present or not valid");
                this.context.getTradeImportTracker().incrementKOImports(tradeNbLegs, TradeImportTracker.KO_ERROR);
                return null;
            }
        }

        // check the independent amout value
        try {
            if (!Util.isEmpty(tradeBean.getIndependentAmount())) {
                CollateralUtilities.parseStringAmountToDouble(tradeBean.getIndependentAmount()).toString();
            }
            if (tradeBeanLegTwo != null) {
                if (!Util.isEmpty(tradeBeanLegTwo.getIndependentAmount())) {
                    CollateralUtilities.parseStringAmountToDouble(tradeBeanLegTwo.getIndependentAmount()).toString();
                }
            }

            // if the independent amount is set, then the currency is mandatory

            if (!Util.isEmpty(tradeBean.getIndependentAmount())
                    && (CollateralUtilities.parseStringAmountToDouble(tradeBean.getIndependentAmount()) != 0.0)// fix
                    // for
                    // FX
                    // file
                    && Util.isEmpty(tradeBean.getIndependentAmountCcy())) {
                this.context.getTradeImportTracker().addError(tradeBean, 54, "Required field INDEPENDENT_AMOUNT_CCY "
                        + (Util.isEmpty(tradeBean.getIndependentAmountCcy()) ? "" : tradeBean.getIndependentAmountCcy())
                        + " not present or not valid");
                this.context.getTradeImportTracker().incrementKOImports(tradeNbLegs, TradeImportTracker.KO_ERROR);
                return null;
            } else if ((tradeBeanLegTwo != null) && !Util.isEmpty(tradeBeanLegTwo.getIndependentAmount())
                    && (CollateralUtilities.parseStringAmountToDouble(tradeBeanLegTwo.getIndependentAmount()) != 0.0) // fix
                    // for
                    // FX
                    // file
                    && Util.isEmpty(tradeBeanLegTwo.getIndependentAmountCcy())) {

                this.context.getTradeImportTracker()
                        .addError(tradeBean, 54,
                                "Required field INDEPENDENT_AMOUNT_CCY "
                                        + (Util.isEmpty(tradeBeanLegTwo.getIndependentAmountCcy()) ? ""
                                        : tradeBeanLegTwo.getIndependentAmountCcy())
                                        + " not present or not valid");
                this.context.getTradeImportTracker().incrementKOImports(tradeNbLegs, TradeImportTracker.KO_ERROR);
                return null;

            }
        } catch (Exception e) {
            Log.error(this, e); //sonar
            this.context.getTradeImportTracker().addError(tradeBean, 60,
                    "Required field INDEPENDENT_AMOUT not present or not valid");
            this.context.getTradeImportTracker().incrementKOImports(tradeNbLegs, TradeImportTracker.KO_ERROR);
            return null;
        }

        try {
            InterfacePLMarkBean plMark1 = new InterfacePLMarkBean();
            plMark1.setPlMarkCurrency(tradeBean.getMtmCcy());
            plMark1.setPlMarkValue(mtmAmount);
            plMark1.setPlMarkDate(mtmDate);
            InterfacePLMarkBean plMark2 = new InterfacePLMarkBean();

            if (tradeBeanLegTwo != null) {
                plMark2.setPlMarkCurrency(tradeBeanLegTwo.getMtmCcy());
                plMark2.setPlMarkValue(mtmAmount2);
                plMark2.setPlMarkDate(mtmDate2);

            }

            InterfacePLMarkBean plMarkIA1 = new InterfacePLMarkBean();
            plMarkIA1.setPlMarkCurrency(tradeBean.getIndependentAmountCcy());
            if (!Util.isEmpty(tradeBean.getIndependentAmount())) {
                plMarkIA1.setPlMarkValue(
                        CollateralUtilities.parseStringAmountToDouble(tradeBean.getIndependentAmount()));
            }
            plMarkIA1.setPlMarkDate(mtmDate);

            InterfacePLMarkBean plMarkIA2 = new InterfacePLMarkBean();

            if (tradeBeanLegTwo != null) {
                plMarkIA2.setPlMarkCurrency(tradeBeanLegTwo.getIndependentAmountCcy());
                if (!Util.isEmpty(tradeBeanLegTwo.getIndependentAmount())) {
                    plMarkIA2.setPlMarkValue(
                            CollateralUtilities.parseStringAmountToDouble(tradeBeanLegTwo.getIndependentAmount()));
                }
                plMarkIA2.setPlMarkDate(mtmDate);
            }

            // GSM: 29/04/2014. PdV adaptation in exposure importation:
            // CLOSING_PRICE Pl_Mark added for
            // COLLATERAL_EXPOSURE.SECURITY_LENDING one leg instruments
            InterfacePLMarkBean plMarkClosingPrice = new InterfacePLMarkBean();
            InterfacePLMarkBean plMarkNpv = new InterfacePLMarkBean(); // without haircut

            if (tradeIsExpSecLending(trade, interfaceTradeBean) && tradeBean.getClosingPriceDaily() != null) {

                // store PlMark Closing Price
                plMarkClosingPrice.setPlMarkCurrency(tradeBean.getMtmCcy());
                mtmAmountClosingPrice = CollateralUtilities.parseStringAmountToDouble(tradeBean.getClosingPriceDaily());
                plMarkClosingPrice.setPlMarkValue(mtmAmountClosingPrice);
                plMarkClosingPrice.setPlMarkDate(mtmDate);

                // store PlMark NPV (without haircut applied)
                plMarkNpv.setPlMarkCurrency(tradeBean.getMtmCcy());
                mtmAmountNpv = CollateralUtilities.parseStringAmountToDouble(tradeBean.getMtmNpv());
                plMarkNpv.setPlMarkValue(mtmAmountNpv);
                plMarkNpv.setPlMarkDate(mtmDate);
            }

            tradeWithPlMarks.setPlMarkClosingPrice1(plMarkClosingPrice);
            tradeWithPlMarks.setPlMarkNpv(plMarkNpv);
            // end GSM

            tradeWithPlMarks.setTrade(trade);
            tradeWithPlMarks.setPlMark1(plMark1);
            tradeWithPlMarks.setPlMark2(plMark2);
            tradeWithPlMarks.setPlMarkIA1(plMarkIA1);
            tradeWithPlMarks.setPlMarkIA2(plMarkIA2);
            tradeWithPlMarks.setTradeBean(tradeBean);
            tradeWithPlMarks.setLineNumber(tradeBean.getLineNumber());

            return tradeWithPlMarks;

        } catch (Exception e) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK,
                    "Error while saving the trade with boReference:" + tradeBean.getBoReference(), e);
            this.context.getTradeImportTracker().addError(tradeBean, 4,
                    "Cannot create MTM from the received information");
            this.context.getTradeImportTracker().incrementKOImports(tradeNbLegs, TradeImportTracker.KO_ERROR);
        }
        return null;
    }

    // GSM: 29/04/2014. PdV adaptation in exposure importation:
    // CollateralExposure.SECURITY_LENDING must save the closing price

    /**
     * @param trade
     * @param interfaceTradeBean
     * @return true is type of instrument requires saving closing price
     */
    public static boolean tradeIsExpSecLending(Trade trade, InterfaceTradeBean interfaceTradeBean) {

        if ((trade != null) && (trade.getProduct() != null) && (trade.getProduct() instanceof CollateralExposure)) {

            CollateralExposure product = (CollateralExposure) trade.getProduct();
            if (product.getSubType().equals(COLLATERAL_EXP_SEC_LENDING)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the given currency id defined in the system
     *
     * @param ccy
     * @return
     */
    private boolean isValidCurrency(String ccy) {
        if (Util.isEmpty(ccy)) {
            return false;
        }

        if (Util.isEmpty(this.ccyList)) {
            this.ccyList = LocalCache.getCurrencies();
        }
        return this.ccyList.contains(ccy);
    }

    /**
     * @param trade to be checked
     * @return true if the trade must be excluded
     */
    // GSM: Trades Exclusions, based on DDR CalypsoColl - TradeInterface_Gaps_v1.2
    public boolean excludeFilter(final Trade trade) {

        boolean result = false;

        if (trade == null) {
            return false;
        }

        final Collection<String> filters = getExposureTradeFiltersNames();

        if ((filters == null) || filters.isEmpty()) {

            noFiltersNamesInDVLogError();
            return false;
        }

        for (String tradeFilterName : filters) {

            // recover the SDFilter created to exclude exposure Trades
            final StaticDataFilter tradeFilter = loadCacheFilter(tradeFilterName);

            if (tradeFilter == null) {
                noSDFilterFoundLogWarning(tradeFilterName);
                continue;

            }

            result = tradeFilter.accept(trade);

            if (result) {
                break; // no sense to continue testing filters, filter match
            }
        }

        return result;
    }

    /**
     * This method allows calling externally to the static cache method, so they are loaded in memory.
     */
    public static void cacheExclusionSDFilters() {

        final Collection<String> filters = getExposureTradeFiltersNames();

        if ((filters == null) || filters.isEmpty()) {
            return;
        }

        for (String tradeFilterName : filters) {

            loadCacheFilter(tradeFilterName);
        }
    }

    /**
     * Logs the warning to advice that there is a SD Filter name included in the
     * DV ExposureInterfaceExclusionFiltersNames. However, this SDFilter does
     * not exist on the system and thus cannot be tested
     *
     * @param tradeFilterName
     */
    private void noSDFilterFoundLogWarning(String tradeFilterName) {

        final String errorDesc = "The TradeExposureFilter name: " + tradeFilterName
                + " is not created on the system. Is not possible to test trades in the Exposure Interface \n";
        Log.error(LOG_CATEGORY_SCHEDULED_TASK, errorDesc);
        if (DEB) {
            System.out.println(errorDesc);// e.printStackTrace();
        }

    }

    /**
     * Logs the error to describe that there are not SD filters names to be
     * excluded in the DV. These must be in the Domain name
     * ExposureInterfaceExclusionFiltersNames
     */
    private void noFiltersNamesInDVLogError() {

        final String errorDesc = "The TradeExposureFilter name does NOT exist in the DVName: "
                + EXPOSURE_INTERFACE_FILTERS_NAME
                + ". Thus, is NOT POSSIBLE to Exclude trades in the Exposure Interface. \n";
        Log.error(LOG_CATEGORY_SCHEDULED_TASK, errorDesc);
        if (DEB) {
            System.out.println(errorDesc);
        }

    }

    /**
     * @param tradeFilterName to read
     * @return returns the SDFilter with that name
     * @throws RemoteException
     */
    private static StaticDataFilter loadCacheFilter(String tradeFilterName) {

        if (filtersMap == null) {
            filtersMap = new HashMap<String, StaticDataFilter>();
        }

        if (filtersMap.containsKey(tradeFilterName)) {
            return filtersMap.get(tradeFilterName);
        }

        // read the SD filter and cache it.
        try {
            final StaticDataFilter newFilter = DSConnection.getDefault().getRemoteReferenceData()
                    .getStaticDataFilter(tradeFilterName);

            filtersMap.put(tradeFilterName, newFilter);
            return newFilter;

        } catch (RemoteException e) {
            Log.error(InterfaceTradeMapper.class, e); //sonar
            return null;
        }
    }

    /**
     * @return a collection of names for the values of the DV ExposureInterfaceExclusionFiltersNames
     */
    private static Collection<String> getExposureTradeFiltersNames() {

        if (exclusionFiltersNames != null) {
            return exclusionFiltersNames;
        }

        // type of filter name in the DV
        if (DV_FILTER_NAME_TYPE) { // full filters names are in the DV

            final Collection<String> domainValues = LocalCache.getDomainValues(DSConnection.getDefault(),
                    EXPOSURE_INTERFACE_FILTERS_NAME);
            // cache filters names
            exclusionFiltersNames = domainValues;
            return domainValues;

        } else { // the domain value just includes the start name of the filters names

            return findSDFiltersNamesThatStartWith();

        }

    }

    /**
     * Finds all the SDFilters names that start with the prefix added in the DV EXPOSURE_INTERFACE_FILTERS_NAME_START
     *
     * @return collection of SDF names
     */
    private static Collection<String> findSDFiltersNamesThatStartWith() {

        final Collection<String> domainValues = LocalCache.getDomainValues(DSConnection.getDefault(),
                EXPOSURE_INTERFACE_FILTERS_NAME_START);
        final Collection<String> finalSdfNames = new ArrayList<String>();

        try {
            @SuppressWarnings("unchecked") final Collection<String> filtersList = DSConnection.getDefault().getRemoteReferenceData()
                    .getStaticDataFilterNames();

            /*
             * just looks for all the SDFilters names. If a name starts with the
             * declaration done on the DV, it will be taken.
             */
            for (String startsWith : domainValues) {

                for (String sdfName : filtersList) {

                    if (sdfName.trim().startsWith(startsWith)) {
                        finalSdfNames.add(sdfName.trim());
                    }

                }
            }
        } catch (RemoteException e) {
            Log.error(InterfaceTradeMapper.class, e); //sonar
        }

        // cache filters names
        exclusionFiltersNames = finalSdfNames;
        return finalSdfNames;
    }

    private boolean checkUPI(LegalEntity le) {
        if (le != null) {
            @SuppressWarnings("unchecked")
            Collection<LegalEntityAttribute> leAttributes = le.getLegalEntityAttributes();
            for (LegalEntityAttribute legalEntityAttribute : leAttributes) {
                if ("UPI_EXCEPTION".equals(legalEntityAttribute.getAttributeType()) && legalEntityAttribute.getAttributeValue().equalsIgnoreCase("true")) {
                    return false;
                }
            }
        }
        return true;
    }
}