package calypsox.tk.util;

import calypsox.tk.bo.ACXHelper;
import calypsox.tk.core.SantPricerMeasure;
import calypsox.util.SantReportingUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSEventScheduledTask;
import com.calypso.tk.marketdata.*;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.CurrencyUtil;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TradeArray;
import com.santander.restservices.acx.model.*;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static calypsox.tk.core.CollateralStaticAttributes.MC_CONTRACT_NUMBER;
import static calypsox.tk.util.SantEquitySwapUtil.*;

/**
 * ScheduledTaskImportEquitySwapVMCalculation
 *
 * @author x865229
 * date 28/11/2022
 */
public class ScheduledTaskEquitySwapBRLCalculation extends ScheduledTask {

    private static final long serialVersionUID = 2525831835261366105L;

    private static final String COLLATERAL_EXPOSURE_TYPE = "Collateral Exposure type";
    private static final String ISIN_MASK = "ISIN Mask";

    private static final String CURRENCY = "CURRENCY";

    private static final String BASE_CURRENCY = "BASE_CURRENCY";

    private static final String LOAD_SECURITY_PRICES_FROM_QUOTE_SET = "Sec Prices from QuoteSet first";

    private static final String LOAD_FX_PRICES_FROM_QUOTE_SET = "FX Rates from QuoteSet first";
    private static final String SAVE_SECURITY_PRICES_TO_QUOTE_SET = "Save Sec Prices to QuoteSet";

    private static final String GET_BRL_FX_RATES_FROM_ACX = "Get BRL FX Rates from ACX";
    private static final String SAVE_FX_RATES_TO_QUOTE_SET = "Save FX Rates to QuoteSet";

    private static final String ACX_AREA = "ACX Area";
    private static final String ACX_LAYER_TYPE = "ACX Layer Type";
    private static final String ACX_UNIT = "ACX Unit";
    private static final String ACX_ASSET_CLASS = "ACX Asset Class";
    private static final String ACX_FACTOR_TYPE = "ACX Factor Type";

    private static final String ACX_UNDERLYING_TYPE = "ACX Underlying Type";

    private static final String ACX_BRL_MARKET = "ACX BRL Market";

    private static final String ACX_QUOTE_SET = "ACX Quote Set";

    private static final String ACX_FX_AREA = "ACX FX Area";
    private static final String ACX_FX_LAYER_TYPE = "ACX FX Layer Type";
    private static final String ACX_FX_UNIT = "ACX FX Unit";
    private static final String ACX_FX_ASSET_CLASS = "ACX FX Asset Class";
    private static final String ACX_FX_FACTOR_TYPE = "ACX FX Factor Type";
    private static final String ACX_FX_UNDERLYING_TYPE = "ACX FX Underlying Type";

    private static final String IGNORE_ERRORS = "Ignore Errors";

    private static final String BRL = "BRL";
    private static final String GMBRISK_CODE = "GMBRISK_NAME";


    private static final String KW_EQ_SWAP_QUANTITY = "EqSwapQuantity";

    private static final String ADJUSTMENT_COMMENT = "Custom calculation";

    private static final String PL_MARK_TYPE = "PL";

    private static final String CLOSE_FIELD_ID = "C0#CLOSE";

    private static final String NPV_LEG_PERFIX = "NPV_LEG";
    private static final String DM_POSTFIX = "_DM";

    private static final String BORROWER = "Borrower";

    private transient List<PLMark> marksToSave;
    private transient PricingEnv pricingEnv;
    private transient JDate valDate;
    //private transient String backupPricingEnv;
    private transient Pattern isinPattern;
    private transient List<String> exposureTypes;
    private transient boolean loadPricesFromQuoteSet = true;
    private transient boolean savePricesToQuoteSet = false;
    private transient boolean ignoreErrors = false;

    private transient Map<String, Double> cacheFXRatesValDate = new HashMap<>();

    @Override
    public String getTaskInformation() {
        return "Calculate Equity Swap VM";
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        // Get superclass attributes
        List<AttributeDefinition> attributeList = new ArrayList<>(super.buildAttributeDefinition());

        attributeList.addAll(Arrays.asList(
                attribute(COLLATERAL_EXPOSURE_TYPE)
                        .mandatory()
                        .domainName("CollateralExposure.subtype")
                        .multipleSelection(true),
                attribute(ISIN_MASK),
                attribute(LOAD_SECURITY_PRICES_FROM_QUOTE_SET)
                        .booleanType()
                        .description("Try to load security prices from QuoteSet first"),
                attribute(SAVE_SECURITY_PRICES_TO_QUOTE_SET)
                        .booleanType()
                        .description("Cache security prices loaded from ACX into QuoteSet"),
                attribute(ACX_AREA)
                        .mandatory()
                        .domain(Stream.of(EArea.values()).map(Enum::name).collect(Collectors.toList())),
                attribute(ACX_LAYER_TYPE)
                        .mandatory()
                        .domain(Stream.of(ELayerType.values()).map(Enum::name).collect(Collectors.toList())),
                attribute(ACX_UNIT)
                        .mandatory()
                        .domain(Stream.of(EUnit.values()).map(Enum::name).collect(Collectors.toList())),
                attribute(ACX_ASSET_CLASS)
                        .mandatory()
                        .domain(Stream.of(EAssetClass.values()).map(Enum::name).collect(Collectors.toList())),
                attribute(ACX_FACTOR_TYPE)
                        .mandatory()
                        .domain(Stream.of(EFactorType.values()).map(EFactorType::toString).collect(Collectors.toList())),
                attribute(ACX_UNDERLYING_TYPE)
                        .mandatory()
                        .domain(Arrays.asList(EUnderlyingIdType.ADO.name(), EUnderlyingIdType.NAME.name())),
                attribute(ACX_BRL_MARKET)
                        .mandatory()
                        .description("Market code to request API by security's Name;Market;Ccy." +
                                " If ACX Underlying Type = NAME"),
                attribute(IGNORE_ERRORS)
                        .booleanType()
                        .description("Ignore trade errors and continue processing. Default is 'true'."),
                attribute(ACX_QUOTE_SET)
                        .domain(AccessUtil.getAllNames(7))
                        .description("Quote set in which ACX prices will be saved/loaded. " +
                                "If it is not reported, the Pricing Environment quote set is used."),
                attribute(GET_BRL_FX_RATES_FROM_ACX).booleanType().description("Get the BRL FX Rates from API ACX, " +
                        "if false get the FX Rate from Pricing Environment quote set."),
                attribute(SAVE_FX_RATES_TO_QUOTE_SET).booleanType()
                        .description("Cache FX Rates loaded from ACX into QuoteSet"),
                attribute(LOAD_FX_PRICES_FROM_QUOTE_SET)
                        .booleanType()
                        .description("Try to load FX rates from QuoteSet first"),
                attribute(ACX_FX_AREA)
                        .mandatory()
                        .domain(Stream.of(EArea.values()).map(Enum::name).collect(Collectors.toList())),
                attribute(ACX_FX_LAYER_TYPE)
                        .mandatory()
                        .domain(Stream.of(ELayerType.values()).map(Enum::name).collect(Collectors.toList())),
                attribute(ACX_FX_UNIT)
                        .mandatory()
                        .domain(Stream.of(EUnit.values()).map(Enum::name).collect(Collectors.toList())),
                attribute(ACX_FX_ASSET_CLASS)
                        .mandatory()
                        .domain(Stream.of(EAssetClass.values()).map(Enum::name).collect(Collectors.toList())),
                attribute(ACX_FX_FACTOR_TYPE)
                        .mandatory()
                        .domain(Stream.of(EFactorType.values()).map(EFactorType::toString).collect(Collectors.toList())),
                attribute(ACX_FX_UNDERLYING_TYPE)
                        .mandatory()
                        .domain(Collections.singletonList(EUnderlyingIdType.CURRENCY.name()))
        ));

        return attributeList;
    }

    @Override
    protected boolean process(DSConnection ds, PSConnection ps) {

        if (Log.isCategoryLogged(LOG_CATEGORY)) {
            Log.debug(LOG_CATEGORY, "Calling Execute ON " + this + " PublishB: " + getPublishB());
        }

        boolean ret = true;

        if (getExecuteB()) {
            try {
                doProcess();
            } catch (Exception e) {
                Log.error(LOG_CATEGORY, e);
                ret = false;
            }
        }

        if (getPublishB()) {
            try {
                PSEventScheduledTask ev = new PSEventScheduledTask();
                ev.setScheduledTask(this);
                ps.publish(ev);

            } catch (Exception e) {
                Log.error(LOG_CATEGORY, e);
                ret = false;
            }
        }

        if (getSendEmailB() && ret) {
            sendMail(ds, ps);
        }

        return ret;
    }

    private void doProcess() throws Exception {

        collectTaskParams();

        // 1. Select Equity Swap Trades
        List<Trade> trades = selectExposureTrades();

        Log.system(LOG_CATEGORY, "CollateralExposure.EQUITY_SWAP trades selected: " + trades.size());

        List<Product> productList = trades.stream()
                .map(SantEquitySwapUtil::extractIsinLeg)
                .filter(Objects::nonNull)
                .filter(le -> !Util.isEmpty(le.getIsin()) && !Util.isEmpty(le.getCcy()))
                .filter(le -> isinPattern.matcher(le.getIsin()).matches())
                .map(le ->
                        Optional.ofNullable(SantEquitySwapUtil.getLegInfoBRLProduct(le))
                                .orElseThrow(() -> new RuntimeException("Product not found for ISIN code: " + le.getIsin()))
                )
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Log.system(LOG_CATEGORY, "Unique ISIN codes extracted: " + productList.size());

        Map<String, Double> prices = requestAndSaveClosingPrices(valDate, productList);

        if (getBooleanAttribute(GET_BRL_FX_RATES_FROM_ACX)) {
            cacheFXRatesValDate = requestAndSaveFXRates(valDate, trades);
        }

        Log.system(LOG_CATEGORY, "Closing prices returned from ACX: " + prices.values().size());

        marksToSave = new ArrayList<>();

        List<Long> tradeIds = new ArrayList<>();
        for (Trade trade : trades) {
            try {
                if (processTrade(trade, prices)) {
                    tradeIds.add(trade.getLongId());
                }
            } catch (Exception ex) {
                if (ignoreErrors) {
                    Log.error(LOG_CATEGORY, ex);
                } else {
                    throw ex;
                }
            }
        }

        Log.system(LOG_CATEGORY, "Trades processed: " + tradeIds.size());
/*
        int num = DSConnection.getDefault().getRemoteMark()
                .deleteMarksNoAudit(PL_MARK_TYPE, tradeIds, pricingEnv.getName(), valDate, true);

        Log.system(LOG_CATEGORY, "Old PLMarks deleted from " + pricingEnv.getName() + ": " + num);
*/
        int num = DSConnection.getDefault().getRemoteMark()
                .saveMarksWithAudit(marksToSave, false);

        Log.system(LOG_CATEGORY, "PLMarks saved to " + pricingEnv.getName() + ": " + num);
    }

    private void collectTaskParams() throws Exception {
        String pricingEnvName = getPricingEnv();
        pricingEnv = MarketDataCache.getPricingEnv(pricingEnvName);
        if (pricingEnv == null) {
            throw new Exception("Invalid PricingEnv name: " + pricingEnvName);
        }
        valDate = getValuationDatetime().getJDate(TimeZone.getDefault());

        String isinMask = Util.convertSQLLikeToRegularExpression(getAttribute(ISIN_MASK));
        if (Util.isEmpty(isinMask))
            isinMask = "BR.*";

        isinPattern = Pattern.compile(isinMask);
        exposureTypes = Util.stringToList(getAttribute(COLLATERAL_EXPOSURE_TYPE));
        loadPricesFromQuoteSet = getBooleanAttribute(LOAD_SECURITY_PRICES_FROM_QUOTE_SET, true);
        savePricesToQuoteSet = getBooleanAttribute(SAVE_SECURITY_PRICES_TO_QUOTE_SET, true);
        ignoreErrors = getBooleanAttribute(IGNORE_ERRORS, true);
    }

    private List<Trade> selectExposureTrades() throws CalypsoServiceException {

        TradeFilter tradeFilter = getTradeFilter() != null ? BOCache.getTradeFilter(DSConnection.getDefault(), getTradeFilter()) : null;

        String sqlFrom = "product_collateral_exposure ce";
        String sqlWhere = "ce.product_id = trade.product_id" +
                " and ce.underlying_type in " + Util.collectionToSQLString(exposureTypes) +
                " and ce.start_date <= " + Util.date2SQLString(valDate) +
                " and (ce.end_date is null or ce.end_date >=" + Util.date2SQLString(valDate) + ")" +
                " and product_desc.product_type = " + Util.string2SQLString(CollateralExposure.PRODUCT_TYPE) +
                " and trade.trade_status not in ('CANCELED')";

        TradeArray tradeArray = DSConnection.getDefault().getRemoteTrade()
                .getTrades(sqlFrom, sqlWhere, null, false, Collections.emptyList());

        return Stream.of(tradeArray.getTrades())
                .filter(t -> tradeFilter == null || tradeFilter.accept(t))
                .collect(Collectors.toList());
    }


    private boolean processTrade(Trade trade, Map<String, Double> prices) throws Exception {

        double quantity = trade.getKeywordAsDouble(KW_EQ_SWAP_QUANTITY);
        if (quantity == 0d) {
            Log.warn(LOG_CATEGORY, "Trade " + trade.getLongId() + " has invalid or empty/zero " + KW_EQ_SWAP_QUANTITY);
            //NO filter trade 24/05/2023
        }

        CollateralConfig marginCallConfig = Optional.ofNullable(getEligibleMCC(trade))
                .orElseThrow(() -> new Exception("Eligible MarginCall config not found, Trade: " + trade.getLongId()));

        SantEquitySwapUtil.LegInfo isinLeg = Optional.ofNullable(extractIsinLeg(trade))
                .orElseThrow(() -> new Exception("Invalid or absent an ISIN Leg, Trade: " + trade.getLongId()));

        SantEquitySwapUtil.LegInfo otherLeg = Optional.ofNullable(extractLeg(trade, 3 - isinLeg.legNum))
                .orElseThrow(() -> new Exception("Invalid or absent the second Leg, Trade: " + trade.getLongId()));

        double closingPriceBRL = Optional.ofNullable(prices.get(isinLeg.getIsin()))
                .orElseThrow(() -> new Exception("Closing price missing for ISIN code: [" + isinLeg.getIsin() + "], Trade: " + trade.getLongId()));

        marksToSave.addAll(calculatePLMarks(trade, isinLeg, otherLeg, marginCallConfig, quantity, closingPriceBRL));

        return true;
    }

    private List<PLMark> calculatePLMarks(Trade trade, SantEquitySwapUtil.LegInfo isinLeg, SantEquitySwapUtil.LegInfo otherLeg,
                                          CollateralConfig marginCallConfig, double quantity, double closingPriceBRL) throws Exception {

        String contractCcy = marginCallConfig.getCurrency();
        String tradeCcy = trade.getTradeCurrency();

        // Current PLMark
        PLMark mark_this_day = Optional.ofNullable(findThisDayMark(trade))
                .orElseThrow(() -> new Exception("PLMark is missing for trade " + trade.getLongId() + " on " + valDate + " [" + pricingEnv.getName() + "]. " +
                        "Please check if Lago file has been imported on this date."));

        // Latest PLMark before the value date - it can be null, that means we are at the first day of calculations
        PLMark mark_prev_day = findPrevDayMark(trade);

        // VARIATION_MARGIN_DM in contract currency
        double vm_dm_value = mark_prev_day != null
                ? Optional.ofNullable(mark_this_day.getPLMarkValueByName(SantPricerMeasure.S_NPV_BASE))
                .map(PLMarkValue::getMarkValue)
                .orElse(0d)
                -
                Optional.ofNullable(mark_prev_day.getPLMarkValueByName(SantPricerMeasure.S_NPV_BASE))
                        .map(PLMarkValue::getMarkValue)
                        .orElse(0d)
                : 0d;

        double margin_call_value = Optional.ofNullable(mark_this_day.getPLMarkValueByName(SantPricerMeasure.S_NPV_BASE))
                .map(PLMarkValue::getMarkValue)
                .orElse(0d);


        // NPV_LEGs in Leg's currencies
        double npv_leg_other_dm_value = otherLeg.getMtm().get();

        //NPV BRL
        double npv_leg_isin_dm_value_brl = quantity * closingPriceBRL * (BORROWER.equals(isinLeg.getDirection()) ? -1d : 1d);


        // converted to trace ccy
        double npv_leg_other_dm_trade_ccy = CurrencyUtil.convertAmount(pricingEnv, npv_leg_other_dm_value, otherLeg.getMtmCcy(), tradeCcy, valDate, pricingEnv.getQuoteSet());
        double npv_leg_isin_dm_trade_ccy = convertAmountUsingACXPrice(BRL, tradeCcy, npv_leg_isin_dm_value_brl, valDate);

        // NPV_DM in trade currency
        double npv_dm_value = npv_leg_other_dm_trade_ccy + npv_leg_isin_dm_trade_ccy;

        //converted to contract ccy
        double npv_leg_other_dm_contract_ccy = CurrencyUtil.convertAmount(pricingEnv, npv_leg_other_dm_value, otherLeg.getMtmCcy(), contractCcy, valDate, pricingEnv.getQuoteSet());
        double npv_leg_isin_dm_contract_ccy = convertAmountUsingACXPrice(BRL, contractCcy, npv_leg_isin_dm_value_brl, valDate);

        // NPV_BASE_DM in contract base currency
        double npv_base_dm_value = npv_leg_other_dm_contract_ccy + npv_leg_isin_dm_contract_ccy;


        //NPV ISIN leg mtm ccy
        double npv_leg_isin_dm_value = convertAmountUsingACXPrice(BRL, isinLeg.getMtmCcy(), npv_leg_isin_dm_value_brl, valDate);


        // INDEPENDENT_AMOUNT_DM in trade currency
        double percentage = SantEquitySwapUtil.getPercentage(isinLeg, trade.getCounterParty().getId());
        double ia_dm_value = CurrencyUtil.roundAmount(npv_leg_isin_dm_trade_ccy * percentage * 0.01d, tradeCcy);

        // INDEPENDENT_AMOUNT_BASE_DM in contract base currency
        double ia_base_dm_value = CurrencyUtil.roundAmount(npv_leg_isin_dm_contract_ccy * percentage * 0.01d, contractCcy);


        //IA_SETTLEMENT_DM in trade currency
        double ia_settle_dm_value;
        if (mark_prev_day != null) {
            Optional<Double> ia_dm_prev_day = Optional
                    .ofNullable(mark_prev_day.getPLMarkValueByName(SantPricerMeasure.S_INDEPENDENT_AMOUNT_DM))
                    .map(PLMarkValue::getMarkValue);

            if (!ia_dm_prev_day.isPresent()) {
                throw new Exception("PLMark value was not found on " + valDate + " [" + pricingEnv.getName() + "]: " + SantPricerMeasure.S_INDEPENDENT_AMOUNT_DM);
            }

            ia_settle_dm_value = ia_dm_value - ia_dm_prev_day.get();
        } else {
            ia_settle_dm_value = ia_dm_value;
        }

        //IA_SETTLEMENT_DM in contract currency
        double ia_settle_base_dm_value;
        if (mark_prev_day != null) {
            Optional<Double> ia_base_dm_prev_day = Optional
                    .ofNullable(mark_prev_day.getPLMarkValueByName(SantPricerMeasure.S_INDEPENDENT_AMOUNT_BASE_DM))
                    .map(PLMarkValue::getMarkValue);

            if (!ia_base_dm_prev_day.isPresent()) {
                throw new Exception("PLMark value was not found on " + valDate + " [" + pricingEnv.getName() + "]: " + SantPricerMeasure.S_INDEPENDENT_AMOUNT_BASE_DM);
            }

            ia_settle_base_dm_value = ia_base_dm_value - ia_base_dm_prev_day.get();
        } else {
            ia_settle_base_dm_value = ia_base_dm_value;
        }

        //MARGIN_CALL_DM in contract currency
        double margin_call_dm_value = vm_dm_value;

        double margin_call_dm_value_trade_ccy = CurrencyUtil.convertAmount(pricingEnv, margin_call_value, contractCcy, tradeCcy, valDate, pricingEnv.getQuoteSet());


        // PLMark Values
        PLMarkValue mark_npv_leg_other = buildPLMarkValue(NPV_LEG_PERFIX + otherLeg.getLegNum() + DM_POSTFIX, npv_leg_other_dm_value, otherLeg.getMtmCcy());
        PLMarkValue mark_npv_leg_isin = buildPLMarkValue(NPV_LEG_PERFIX + isinLeg.getLegNum() + DM_POSTFIX, npv_leg_isin_dm_value, isinLeg.getMtmCcy());
        PLMarkValue mark_npv = buildPLMarkValue(SantPricerMeasure.S_NPV_DM, npv_dm_value, tradeCcy);
        PLMarkValue mark_npv_base_dm = buildPLMarkValue(SantPricerMeasure.S_NPV_BASE_DM, npv_base_dm_value, contractCcy);
        PLMarkValue mark_vm_dm = buildPLMarkValue(SantPricerMeasure.S_VARIATION_MARGIN_DM, vm_dm_value, contractCcy);
        PLMarkValue mark_ia_dm = buildPLMarkValue(SantPricerMeasure.S_INDEPENDENT_AMOUNT_DM, ia_dm_value, tradeCcy);
        PLMarkValue mark_ia_base_dm = buildPLMarkValue(SantPricerMeasure.S_INDEPENDENT_AMOUNT_BASE_DM, ia_base_dm_value, contractCcy);
        PLMarkValue mark_ia_settle_dm = buildPLMarkValue(SantPricerMeasure.S_IA_SETTLEMENT_DM, ia_settle_dm_value, tradeCcy);
        PLMarkValue mark_ia_settle_base_dm = buildPLMarkValue(SantPricerMeasure.S_IA_SETTLEMENT_BASE_DM, ia_settle_base_dm_value, contractCcy);

        PLMarkValue mark_ia_settle_dm_ia_core = buildPLMarkValue(SantPricerMeasure.S_INDEPENDENT_AMOUNT, ia_dm_value, tradeCcy);
        // to-do: identify necessity of MARGIN_CALL mark, whether NPV measure is enough for CM or not
        PLMarkValue mark_margin_call = buildPLMarkValue(SantPricerMeasure.S_MARGIN_CALL_DM, margin_call_dm_value, contractCcy);
        //BRL
        PLMarkValue mark_margin_call_core = buildPLMarkValue(SantPricerMeasure.S_MARGIN_CALL, margin_call_dm_value_trade_ccy, tradeCcy);

        // Build PLMark for given date
        PLMark plMark = buildPLMark(trade, getPricingEnv(), valDate,
                Arrays.asList(
                        mark_npv_leg_isin,
                        mark_npv_leg_other,
                        mark_npv,
                        mark_npv_base_dm,
                        mark_vm_dm,
                        mark_ia_dm,
                        mark_ia_base_dm,
                        mark_ia_settle_dm,
                        mark_ia_settle_base_dm,
                        mark_margin_call,
                        mark_ia_settle_dm_ia_core,
                        mark_margin_call_core));

        List<PLMark> marks = new ArrayList<>();

        marks.add(plMark);

        marks.addAll(recalculateFutureMarks(trade, plMark));

        return marks;
    }

    private CollateralConfig getEligibleMCC(Trade trade) throws Exception {
        CollateralConfig marginCallConfig;
        int mccId = trade.getKeywordAsInt(MC_CONTRACT_NUMBER);
        if (mccId != 0) {
            marginCallConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), mccId);
        } else {
            marginCallConfig = SantReportingUtil.getSantReportingService(DSConnection.getDefault())
                    .getEligibleMarginCallConfigs(trade)
                    .stream()
                    .findFirst().orElse(null);
        }

        return marginCallConfig;
    }

    private PLMark findPrevDayMark(Trade trade) throws Exception {
        return DSConnection.getDefault().getRemoteMark()
                .getLatestMark(PL_MARK_TYPE, trade.getLongId(), null, pricingEnv.getName(), valDate.addDays(-1));
    }

    private PLMark findThisDayMark(Trade trade) throws Exception {
        return DSConnection.getDefault().getRemoteMark()
                .getLatestMark(PL_MARK_TYPE, trade.getLongId(), null, pricingEnv.getName(), valDate);
    }


    private List<PLMark> recalculateFutureMarks(Trade trade, PLMark today) throws Exception {

        List<PLMark> result = new ArrayList<>();

        CollateralExposure exposure = (CollateralExposure) trade.getProduct();

        Collection<PLMark> marks = DSConnection.getDefault().getRemoteMark()
                .getAllBefore(PL_MARK_TYPE, trade.getLongId(), pricingEnv.getName(), exposure.isOpen() ? valDate.addYears(1) : exposure.getMaturityDate());

        Optional<PLMark> nextMark = marks.stream()
                .filter(m -> m.getValDate().after(valDate))
                .min(Comparator.comparing(PLMark::getValDate));

        nextMark.ifPresent(m -> {
            /*
            //to-do : repairing of vm if core values change
            double vm = m.getPLMarkValueByName(PricerMeasure.S_NPV).getMarkValue()
                    - today.getPLMarkValueByName(PricerMeasure.S_NPV).getMarkValue();

            PLMarkValue vm_mark = m.getPLMarkValueByName(SantPricerMeasure.S_VARIATION_MARGIN_DM);
            if (vm_mark != null) {
                vm_mark.setMarkValue(vm);
            } else {
                m.addPLMarkValue(buildPLMarkValue(SantPricerMeasure.S_VARIATION_MARGIN_DM, vm, exposure.getCurrency()));
            }
            */
            PLMarkValue val = m.getPLMarkValueByName(SantPricerMeasure.S_INDEPENDENT_AMOUNT_DM);
            if (val != null) {
                double ia_change = val.getMarkValue()
                        - today.getPLMarkValueByName(SantPricerMeasure.S_INDEPENDENT_AMOUNT_DM).getMarkValue();

                PLMarkValue ia_settle_mark = m.getPLMarkValueByName(SantPricerMeasure.S_IA_SETTLEMENT_DM);
                if (ia_settle_mark != null) {
                    ia_settle_mark.setMarkValue(-ia_change);
                } else {
                    m.addPLMarkValue(buildPLMarkValue(SantPricerMeasure.S_IA_SETTLEMENT_DM, -ia_change, exposure.getCurrency()));
                    m.addPLMarkValue(buildPLMarkValue(SantPricerMeasure.S_INDEPENDENT_AMOUNT, -ia_change, exposure.getCurrency()));
                }
            }
            result.add(m);
        });

        return result;
    }


    private PLMarkValue buildPLMarkValue(String name, double value, String ccy) {
        PLMarkValue plMarkValue = new PLMarkValue();
        plMarkValue.setMarkName(name);
        plMarkValue.setMarkValue(value);
        plMarkValue.setCurrency(ccy);
        plMarkValue.setOriginalCurrency(plMarkValue.getCurrency());
        plMarkValue.setOriginalMarkValue(plMarkValue.getMarkValue());
        plMarkValue.setAdjustmentComment(ADJUSTMENT_COMMENT);
        plMarkValue.setDisplayDigits(CurrencyUtil.getRoundingUnit(ccy));
        return plMarkValue;
    }


    private PLMark buildPLMark(Trade trade, String pricingEnvName, JDate onDate,
                               Collection<PLMarkValue> plMarkValues) {

        PLMark plMark = new PLMark(trade, pricingEnvName, onDate);
        plMark.setType(PL_MARK_TYPE);
        plMark.setBookId(trade.getBookId());
        plMarkValues.forEach(plMark::addPLMarkValue);
        return plMark;
    }

    private Map<String, Double> requestAndSaveFXRates(JDate valDate, List<Trade> trades) throws Exception {

        Set<String> fxQuoteNames = new HashSet<>();

        for (Trade trade : trades) {
            CollateralConfig marginCallConfig = Optional.ofNullable(getEligibleMCC(trade))
                    .orElseThrow(() -> new Exception("Eligible MarginCall config not found, Trade: " + trade.getLongId()));

            SantEquitySwapUtil.LegInfo isinLeg = Optional.ofNullable(extractIsinLeg(trade))
                    .orElseThrow(() -> new Exception("Invalid or absent an ISIN Leg, Trade: " + trade.getLongId()));

            //Isin leg ccy
            fxQuoteNames.add(getFXName(isinLeg.getMtmCcy(), BRL));
            //Contract ccy
            fxQuoteNames.add(getFXName(marginCallConfig.getCurrency(), BRL));
            //Trade ccy
            fxQuoteNames.add(getFXName(trade.getTradeCurrency(), BRL));
        }

        String quoteSetName = getAttribute(ACX_QUOTE_SET);

        if (Util.isEmpty(quoteSetName)) {
            quoteSetName = pricingEnv.getQuoteSetName();
        }

        String finalQuoteSetName = quoteSetName;

        //Build needed fxQuote values
        Vector<QuoteValue> needQuotes = fxQuoteNames.stream().
                map(fxName -> new QuoteValue(finalQuoteSetName, fxName, valDate,
                        QuoteValue.PRICE)).distinct().collect(Collectors.toCollection(Vector::new));

        Vector<QuoteValue> dbQuotes = getBooleanAttribute(LOAD_FX_PRICES_FROM_QUOTE_SET) ?
                DSConnection.getDefault().getRemoteMarketData().getQuoteValues(needQuotes) : new Vector<>();

        Map<String, Double> currentQuotesCache = new HashMap<>();

        if (!Util.isEmpty(dbQuotes)) {
            currentQuotesCache = dbQuotes.stream()
                    .filter(qv -> !Double.isNaN(qv.getClose()))
                    .collect(Collectors.toMap(QuoteValue::getName, QuoteValue::getClose));
        }

        if (needQuotes.size() == currentQuotesCache.size()) {
            // all quotes have been found in QuoteValues
            return currentQuotesCache;
        }


        Map<String, Double> finalCurrentQuotesCache = currentQuotesCache;

        needQuotes = needQuotes.stream().filter(qv -> !finalCurrentQuotesCache.containsKey(qv.getName())).
                collect(Collectors.toCollection(Vector::new));

        if (!Util.isEmpty(needQuotes)) {
            List<String> underlyingList = new ArrayList<>();
            for (QuoteValue q : needQuotes) {
                String[] values = q.getName().split("\\.");
                if (values.length >= 3) {
                    //API return values like USD/BRL,EUR/BRL
                    underlyingList.add(values[1] + "/" + values[2]);
                }

            }

            String acxArea = getAttribute(ACX_FX_AREA);
            String acxLayerType = getAttribute(ACX_FX_LAYER_TYPE);
            String acxUnit = getAttribute(ACX_FX_UNIT);
            String acxAssetClass = getAttribute(ACX_FX_ASSET_CLASS);
            String acxFactorType = getAttribute(ACX_FX_FACTOR_TYPE);
            String acxUnderlyingType = getAttribute(ACX_FX_UNDERLYING_TYPE);

            List<ACXPriceResult> result = ACXHelper.INSTANCE.requestPrices(
                    EArea.valueOf(acxArea),
                    ELayerType.valueOf(acxLayerType),
                    EUnit.valueOf(acxUnit),
                    EAssetClass.valueOf(acxAssetClass),
                    EFactorType.ofValueName(acxFactorType),
                    EUnderlyingIdType.valueOf(acxUnderlyingType),
                    valDate.getDate(),
                    valDate.getDate(),
                    underlyingList
            );

            if (!Util.isEmpty(result)) {
                Log.system(LOG_CATEGORY, "ACXPriceResult for FX size: " + result.size());
                Vector<QuoteValue> acxQuotes = new Vector<>();
                for (ACXPriceResult r : result) {
                    List<ACXStaticAttribute> attrs = r.getStaticAttributes();
                    List<ACXPrice> prices = r.getPrices();
                    if (!Util.isEmpty(attrs) && !Util.isEmpty(prices)) {
                        String currencyBase = null;
                        String currency = null;
                        for (ACXStaticAttribute attr : attrs) {
                            if (!Util.isEmpty(attr.getName())) {
                                if (BASE_CURRENCY.equals(attr.getName())) {
                                    currencyBase = attr.getValue();
                                } else if (CURRENCY.equals(attr.getName())) {
                                    currency = attr.getValue();
                                }
                            }
                        }
                        if (!Util.isEmpty(currencyBase) && !Util.isEmpty(currency)) {
                            Optional<Double> price = prices.stream()
                                    .filter(p -> valDate.equals(JDate.valueOf(p.getRecordDate())))
                                    .findFirst()
                                    .flatMap(p -> p.getFields().stream()
                                            .filter(f -> CLOSE_FIELD_ID.equals(f.getId()))
                                            .map(f -> f.getValueField().getValue())
                                            .findFirst());
                            if (price.isPresent()) {
                                //Save values USD/BRL, EUR/BRL
                                acxQuotes.add(new QuoteValue(finalQuoteSetName, getFXName(currencyBase, currency),
                                        valDate, QuoteValue.PRICE, Double.NaN, Double.NaN, Double.NaN, price.get()));
                                currentQuotesCache.put(getFXName(currency, currencyBase), 1.0 / price.get());
                                currentQuotesCache.put(getFXName(currencyBase, currency), price.get());
                            }
                        }
                    }
                }
                if (!Util.isEmpty(acxQuotes) && getBooleanAttribute(SAVE_FX_RATES_TO_QUOTE_SET)) {
                    DSConnection.getDefault().getRemoteMarketData().saveQuoteValues(acxQuotes);
                }

                if (acxQuotes.size() < needQuotes.size()) {
                    String missingFX = needQuotes.stream()
                            .filter(q -> !isFXInQuoteSet(q, acxQuotes))
                            .map(QuoteValue::getName)
                            .collect(Collectors.joining(","));
                    throw new Exception("No closing price(s) reported for FX(s): " + missingFX);
                }

            } else {
                Log.system(LOG_CATEGORY, "ACXPriceResult for FX is empty");
            }
        }
        return currentQuotesCache;
    }

    private String getFXName(String originCcy, String destinationCcy) {
        return "FX." + originCcy + "." + destinationCcy;
    }

    private boolean isFXInQuoteSet(QuoteValue qv1, Vector<QuoteValue> quotes) {
        if (!Util.isEmpty(qv1.getName())) {
            for (QuoteValue qv2 : quotes) {
                if (!Util.isEmpty(qv2.getName()) && qv1.getName().equals(qv2.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private double convertAmountUsingACXPrice(String originCcy, String destinationCcy, double amount, JDate date) throws Exception {

        if (!getBooleanAttribute(GET_BRL_FX_RATES_FROM_ACX)) {
            return CurrencyUtil.convertAmount(pricingEnv, amount, originCcy, destinationCcy, date, pricingEnv.getQuoteSet());
        }
        if (date != null && date.equals(valDate)) {
            Double rate = cacheFXRatesValDate.get(getFXName(originCcy, destinationCcy));

            if (rate == null) {
                rate = cacheFXRatesValDate.get(getFXName(destinationCcy, originCcy));
                if (rate != null) {
                    rate = 1.0 / rate;
                    return CurrencyUtil.roundAmount(amount * rate, destinationCcy);
                }
            } else {
                return CurrencyUtil.roundAmount(amount * rate, destinationCcy);
            }
        }

        //Get rate from DB
        String quoteSetName = getAttribute(ACX_QUOTE_SET);

        if (Util.isEmpty(quoteSetName)) {
            quoteSetName = pricingEnv.getQuoteSetName();
        }


        QuoteValue qv = DSConnection.getDefault().getRemoteMarketData().getQuoteValue(new QuoteValue(quoteSetName, getFXName(originCcy, destinationCcy),
                date, QuoteValue.PRICE));
        if (qv != null && !Double.isNaN(qv.getClose())) {
            return CurrencyUtil.roundAmount(amount * qv.getClose(), destinationCcy);
        }

        qv = DSConnection.getDefault().getRemoteMarketData().getQuoteValue(new QuoteValue(quoteSetName, getFXName(destinationCcy, originCcy),
                date, QuoteValue.PRICE));
        if (qv != null && !Double.isNaN(qv.getClose())) {
            double rate = 1.0 / qv.getClose();
            return CurrencyUtil.roundAmount(amount * rate, destinationCcy);
        } else {
            throw new Exception("No FX price(s) reported for FX originCCY: " + originCcy + ", destinationCCY: " +
                    destinationCcy + ", quoteSetName: " + quoteSetName + ", valDate: " + valDate);
        }


    }

    private Map<String, Double> requestAndSaveClosingPrices(JDate valDate, List<Product> productList) throws Exception {

        String quoteSetName = getAttribute(ACX_QUOTE_SET);

        if (Util.isEmpty(quoteSetName)) {
            quoteSetName = pricingEnv.getQuoteSetName();
        }

        String finalQuoteSetName = quoteSetName;

        final Map<String, String> quoteNameToISIN = productList.stream()
                .collect(Collectors.toMap(Product::getQuoteName, p -> p.getSecCode(ISIN)));

        final Map<String, String> isinToQuoteName = new HashMap<>();
        quoteNameToISIN.forEach((k, v) -> isinToQuoteName.put(v, k));


        Vector<QuoteValue> quotes = productList.stream()
                .map(p -> new QuoteValue(finalQuoteSetName, p.getQuoteName(), valDate, p.getQuoteType()))
                .collect(Collectors.toCollection(Vector::new));

        @SuppressWarnings("unchecked")
        Vector<QuoteValue> quoteValues = loadPricesFromQuoteSet ? DSConnection.getDefault().getRemoteMarketData().getQuoteValues(quotes) : new Vector<>();

        Map<String, Double> pricesByIsin = quoteValues.stream()
                .filter(qv -> !Double.isNaN(qv.getClose()))
                .collect(Collectors.toMap(qv -> quoteNameToISIN.get(qv.getName()), QuoteValue::getClose));

        if (pricesByIsin.size() == quotes.size()) {
            // all quotes have been found in QuoteValues
            return pricesByIsin;
        }

        String acxUnderlyingType = getAttribute(ACX_UNDERLYING_TYPE);
        String acxBrlMarket = getAttribute(ACX_BRL_MARKET);

        List<String> underlyingList = productList.stream()
                .filter(p -> !pricesByIsin.containsKey(p.getSecCode(ISIN)))
                .map(p -> {
                    if (EUnderlyingIdType.ADO.name().equals(acxUnderlyingType)) {
                        String riskCode = p.getSecCode(GMBRISK_CODE);
                        if (riskCode == null) {
                            String err = "GMBRISK_CODE is not set for Product: " + p.getName();
                            Log.error(LOG_CATEGORY, err);
                            throw new RuntimeException(err);
                        }
                        return riskCode;
                    } else {
                        return p.getSecCode(ISIN) + ";" + acxBrlMarket + ';' + BRL;
                    }
                })
                .collect(Collectors.toList());

        String acxArea = getAttribute(ACX_AREA);
        String acxLayerType = getAttribute(ACX_LAYER_TYPE);
        String acxUnit = getAttribute(ACX_UNIT);
        String acxAssetClass = getAttribute(ACX_ASSET_CLASS);
        String acxFactorType = getAttribute(ACX_FACTOR_TYPE);

        List<ACXPriceResult> result = ACXHelper.INSTANCE.requestPrices(
                EArea.valueOf(acxArea),
                ELayerType.valueOf(acxLayerType),
                EUnit.valueOf(acxUnit),
                EAssetClass.valueOf(acxAssetClass),
                EFactorType.ofValueName(acxFactorType),
                EUnderlyingIdType.valueOf(acxUnderlyingType),
                valDate.getDate(),
                valDate.getDate(),
                underlyingList
        );

        if (!Util.isEmpty(result)) {
            Log.system(LOG_CATEGORY, "ACXPriceResult size " + result.size());
        } else {
            Log.system(LOG_CATEGORY, "ACXPriceResult is empty");
        }

        Map<String, Optional<Double>> apiPricesByIsin = result.stream()
                .collect(Collectors
                        .toMap(r -> r.getStaticAttributes().stream()
                                        .filter(sa -> ISIN.equals(sa.getName()))
                                        .findFirst()
                                        .map(ACXStaticAttribute::getValue)
                                        .orElse(null),
                                r -> r.getPrices().stream()
                                        .filter(p -> valDate.equals(JDate.valueOf(p.getRecordDate())))
                                        .findFirst()
                                        .flatMap(p -> p.getFields().stream()
                                                .filter(f -> CLOSE_FIELD_ID.equals(f.getId()))
                                                .map(f -> f.getValueField().getValue())
                                                .findFirst()
                                        )
                                , (x1, x2) -> x1));

        if (!Util.isEmpty(apiPricesByIsin)) {
            apiPricesByIsin.forEach((key, value) -> Log.system(LOG_CATEGORY, "API price for ISIN " + key + ":" + value));
        }

        quoteValues.clear();
        apiPricesByIsin.forEach((isin, priceOptional) ->
                priceOptional.ifPresent(price -> {
                    pricesByIsin.put(isin, price);

                    QuoteValue qv = new QuoteValue(finalQuoteSetName, isinToQuoteName.get(isin), valDate, QuoteValue.PRICE, Double.NaN, Double.NaN, Double.NaN, price);
                    quoteValues.add(qv);
                })
        );

        if (savePricesToQuoteSet) {
            DSConnection.getDefault().getRemoteMarketData().saveQuoteValues(quoteValues);
        }

        if (pricesByIsin.size() < quotes.size()) {
            String missingPrices = quotes.stream()
                    .map(q -> quoteNameToISIN.get(q.getName()))
                    .filter(isin -> !pricesByIsin.containsKey(isin))
                    .collect(Collectors.joining(","));
            throw new Exception("No closing price(s) reported for ISIN(s): " + missingPrices);
        }

        return pricesByIsin;
    }

}
