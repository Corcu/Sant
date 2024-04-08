package calypsox.tk.report;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.CurrencyDefault;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.TimeZone;
import java.util.Vector;

import static calypsox.util.TradeInterfaceUtils.*;

/**
 * Class with the necessary logic to retrieve the different values for the
 * triResolve derivatives report.
 *
 * @author David Porras Mart?nez
 */
public class CSAExposureLogic {

    private static final String NPV_BASE = "NPV_BASE";
    private static final String INDEPENDENT_AMOUNT = "INDEPENDENT_AMOUNT";
    private static final String BLANK = "";
    private static final String NOMINAL_1 = "NOMINAL_1";
    private static final String NOMINAL_2 = "NOMINAL_2";
    private static final String CCY_1 = "CCY_1";
    private static final String CCY_2 = "CCY_2";
    private static final String UNDERLYING_1 = "UNDERLYING_1";
    private static final String UNDERLYING_2 = "UNDERLYING_2";
    private static final String UNDERLYING = "UNDERLYING";
    private static final String BUY = "Buy";
    private static final String SELL = "Sell";
    private static final String STRUCTURE_ID = "STRUCTURE_ID";
    private static final String USI_REFERENCE = TRADE_KWD_USI_REFERENCE;
    private static final String SD_MSP = TRADE_KWD_SD_MSP;
    private static final String US_PARTY = TRADE_KWD_US_PARTY;
    private static final String DFA_APPLICABLE = TRADE_KWD_DFA;
    private static final String FC_NFC = TRADE_KWD_FC_NFC;
    private static final String EMIR_APPLICABLE = TRADE_KWD_EMIR;
    // GSM: 22/08/13. Added the 7? field for Port. Reconciliation
    private static final String UTI_APPLICABLE = TRADE_KWD_UTI;
    private static final String EMPTY = "";
    private static final String NO = "NO";
    private static final String PRODUCT_MAPPING_FX_SPOT = "FX_DELIVERABLE_SPOT";
    private static final String PRODUCT_SUBTYPE_DOMAIN_VALUE = "CollateralExposure.subtype";
    private static final String SP_PATTERN = "dd/MM/YYYY";

    private final Hashtable<String, CurrencyDefault> currencies = LocalCache
            .getCurrencyDefaults();

    @SuppressWarnings("unused")
    private ReposTradeItem getReposTradeItem(final Vector<String> errors) {
        return null;
    }

    // UPDATED: cast format error
    public String getNotional(final CollateralExposure ce) {
        String value = "0";
        if ((ce.getSubType() != null) && (!ce.getSubType().equals(BLANK))) {
            if (CollateralUtilities.isTwoLegsProductType(ce.getSubType())) {
                if (ce.getAttribute(NOMINAL_1) != null) {
                    String strVal = ce.getAttribute(NOMINAL_1).toString();
                    if (!strVal.equals(BLANK)) {
                        value = strVal;
                    }
                }
            } else {
                value = Util.numberToString(ce.getPrincipal());
            }
        }
        return value;
    }

    public String getNotional2(final CollateralExposure ce) {
        String value = "0";
        if ((ce.getSubType() != null) && (!ce.getSubType().equals(BLANK))) {
            if (CollateralUtilities.isTwoLegsProductType(ce.getSubType())) {
                if (ce.getAttribute(NOMINAL_2) != null) {
                    String strVal = ce.getAttribute(NOMINAL_2).toString();
                    if (!strVal.equals(BLANK)) {
                        value = strVal;
                    }
                }
            }
        }
        return value;
    }

    public String getCurrency(final CollateralExposure ce) {
        String value = BLANK;
        if ((ce.getSubType() != null) && (!ce.getSubType().equals(BLANK))) {
            if (CollateralUtilities.isTwoLegsProductType(ce.getSubType())) {
                if (ce.getAttribute(CCY_1) != null) {
                    value = ce.getAttribute(CCY_1).toString();
                    if (!value.equals(BLANK)) {
                        return this.currencies.get(value).getDescription()
                                .toUpperCase();
                    }
                }
            } else {
                value = ce.getCurrency();
                if (!value.equals(BLANK)) {
                    return this.currencies.get(value).getDescription()
                            .toUpperCase();
                }
            }
        }
        return value;
    }

    public String getCurrencyValue(final CollateralExposure ce) {
        String value = BLANK;
        if ((ce.getSubType() != null) && (!ce.getSubType().equals(BLANK))) {
            if (CollateralUtilities.isTwoLegsProductType(ce.getSubType())) {
                if (ce.getAttribute(CCY_1) != null) {
                    value = ce.getAttribute(CCY_1).toString();
                }
            } else {
                value = ce.getCurrency();
            }
        }
        return value;
    }

    public String getCurrency2(final CollateralExposure ce) {
        String value = BLANK;
        if ((ce.getSubType() != null) && (!ce.getSubType().equals(BLANK))) {
            if (CollateralUtilities.isTwoLegsProductType(ce.getSubType())) {
                if (ce.getAttribute(CCY_2) != null) {
                    value = ce.getAttribute(CCY_2).toString();
                    if (!value.equals(BLANK)) {
                        return this.currencies.get(value).getDescription()
                                .toUpperCase();
                    }
                }
            }
        }
        return value;
    }

    public String getCurrency2Value(final CollateralExposure ce) {
        String value = BLANK;
        if ((ce.getSubType() != null) && (!ce.getSubType().equals(BLANK))) {
            if (CollateralUtilities.isTwoLegsProductType(ce.getSubType())) {
                if (ce.getAttribute(CCY_2) != null) {
                    value = ce.getAttribute(CCY_2).toString();

                }
            }
        }
        return value;
    }

    public String getUnderlying(final CollateralExposure ce) {
        String value = BLANK;
        if ((ce.getSubType() != null) && (!ce.getSubType().equals(BLANK))) {
            if (CollateralUtilities.isTwoLegsProductType(ce.getSubType())) {
                if (null != ce.getAttribute(UNDERLYING_1)) {
                    value = ce.getAttribute(UNDERLYING_1).toString();
                }
                if (null != ce.getAttribute(UNDERLYING_2)) {
                    value += "  " + ce.getAttribute(UNDERLYING_2).toString();
                }
            } else {
                if (null != ce.getAttribute(UNDERLYING)) {
                    value = ce.getAttribute(UNDERLYING).toString();
                }
            }
        }
        return value;
    }

    public String getFloatRate(CollateralExposure ce, CollateralConfig mcc,
                               JDate date) {

        return Util.numberToString(getFXRate(
                date, mcc.getCurrency(), getCurrencyValue(ce)));

    }

    public String getFloatRate2(CollateralExposure ce, CollateralConfig mcc,
                                JDate date) {

        return Util.numberToString(getFXRate(
                date, mcc.getCurrency(), getCurrency2Value(ce)));

    }

    /**
     * Retrieve the mark-to-Market date.
     *
     * @param collMarks Collection with the PLMarks obtained from the database.
     * @return The double value for the Mark-to-Market.
     */
    public String getMtmDate(PLMark mark, JDate date) {
        if (null != mark) {
            return mark.getValDate().toString();
        }
        return date.toString();
    }

    public String getMtmDatesp(PLMark mark, JDate date) {
        if (null != mark) {
            return Util.dateToString(mark.getValDate(), SP_PATTERN);
        }
        return Util.dateToString(date, SP_PATTERN);
    }

    /**
     * Retrieve the mark-to-Market value.
     *
     * @param collMarks Collection with the PLMarks obtained from the database.
     * @return The double value for the Mark-to-Market.
     */
    public String getMtmValue(PLMark mark) {
        if (mark != null) {
            // MIGRATION V14.4 18/01/2015
            PLMarkValue markValue = mark.getPLMarkValueFromList(NPV_BASE);
            if (markValue != null) {
                return Util.numberToString(markValue
                        .getMarkValue());
            }
        }
        return "0";
    }

    /**
     * Retrieve the Mark-to-Market currency code.
     *
     * @param collMarks Collection with the PLMarks obtained from the database.
     * @return The currency code for the Mark-to-Market.
     */
    public String getMtmCurr(PLMark mark) {
        if (null != mark) {
            // MIGRATION V14.4 18/01/2015
            PLMarkValue markValue = mark.getPLMarkValueFromList(NPV_BASE);
            if (markValue != null) {
                if (!markValue.getCurrency().equals(BLANK)) {
                    return this.currencies.get(markValue.getCurrency())
                            .getDescription().toUpperCase();
                }
            }
        }
        return BLANK;
    }

    public String getBaseCcy(CollateralConfig mcc) {
        String ccy = mcc.getCurrency();
        if ((ccy != null) && (!ccy.equals(BLANK))) {
            return this.currencies.get(ccy).getDescription().toUpperCase();
        }
        return BLANK;
    }

    public String getTradeDate(final Trade t) {
        if ((t != null) && (t.getTradeDate() != null)) {
            return t.getTradeDate().getJDate(TimeZone.getDefault()).toString();
        }
        return BLANK;
    }

    public String getTradeDatesp(final Trade t) {
        if ((t != null) && (t.getTradeDate() != null)) {
            return Util.dateToString(t.getTradeDate().getJDate(TimeZone.getDefault()), SP_PATTERN);
        }
        return BLANK;
    }

    public String getStartProductDatesp(final Trade t) {
        if ((t != null) && null != t.getProduct()) {
            CollateralExposure produ = (CollateralExposure) t.getProduct();
            if (null != produ && null != produ.getStartDate()) {
                return Util.dateToString(produ.getStartDate(), SP_PATTERN);
            }
        }
        return BLANK;
    }

    public String getMatDate(final Trade t) {
        if ((t != null) && (t.getProduct() != null)
                && (t.getProduct().getMaturityDate() != null)) {
            return t.getProduct().getMaturityDate().toString();

        }
        return BLANK;
    }

    public String getMatDatesp(final Trade t) {
        if ((t != null) && (t.getProduct() != null)
                && (t.getProduct().getMaturityDate() != null)) {
            return Util.dateToString(t.getProduct().getMaturityDate(), SP_PATTERN);
        }
        return BLANK;
    }

    public String getPayRec(CollateralExposure ce, Trade t) {
        if (ce != null) {
            final String direction = ce.getDirection(t);
            if (direction.equals(BUY)) {
                return "Buy";
            }
            if (direction.equals(SELL)) {
                return "Sell";
            }
        }
        return BLANK;
    }

    // new
    public String getCollatAgree(CollateralConfig mcc) {
        return mcc.getName();
    }

    public CollateralConfig getMCContract(int id) {
        CollateralConfig cc = null;

        if (id > 0) {
            try {
                cc = ServiceRegistry.getDefault().getCollateralDataServer()
                        .getMarginCallConfig(id);
            } catch (CollateralServiceException exc) {
                Log.error("Error retrieving contract", exc);
            }
        }
        return cc;
    }


    public String getCollatAgreeType(final Trade t) {
        if (t != null) {
            return t.getKeywordValue("CONTRACT_TYPE");
        }
        return BLANK;
    }

    public String getOwner(CollateralConfig mcc) {
        if (mcc.getProcessingOrg() != null) {
            return mcc.getProcessingOrg().getName();
        }
        return BLANK;
    }

    public String getValAgent(CollateralConfig mcc) {
        return mcc.getAdditionalField("CALC_AGENT");
    }

    public String getStructure(Trade t) {
        if (t != null) {
            return t.getKeywordValue(STRUCTURE_ID);
        }
        return BLANK;

    }

    public String getCounterparty(CollateralConfig mcc) {
        if ((mcc.getLegalEntity() != null)
                && (mcc.getLegalEntity().getName() != null)) {
            if (!mcc.getLegalEntity().getName().equals(BLANK)) {
                return mcc.getLegalEntity().getName();
            }
        }

        return BLANK;

    }

    public String getTradeID(Trade t) {
        if (t != null) {
            String id = t.getKeywordValue("BO_REFERENCE");
            if (id != null) {
                return id;
            }
        }

        return BLANK;

    }

    public String formatAmount(String amount) {
        if ((amount != null) && (!amount.equals(BLANK))) {
            return amount.replace('.', '*').replace(',', '.').replace('*', ',');
        }
        return amount;

    }

    public String getIndependentAmount(PLMark mark) {
        String result = "0";
        if (mark != null) {
            // MIGRATION V14.4 18/01/2015
            PLMarkValue markValue = mark
                    .getPLMarkValueByName(INDEPENDENT_AMOUNT);
            if (markValue != null) {
                result = Util.numberToString(markValue
                        .getMarkValue());
            }
        }
        return result;
    }

    // COL_OUT_016

    /**
     * Method that retrieve row by row from Calypso, to insert in the vector
     * with the result to show. Changed by Carlos Cejudo: No DSConnection is
     * needed anymore, all data is passed through the PLMarksMap.
     *
     * @param trade      Trade associated with the Repo object.
     * @param plMarksMap Map containing the PLMarks for each trade.
     * @param errors     Vector with the different errors occurred.
     * @return The row retrieved from the system, with the necessary
     * information.
     */
    protected CSAExposureItem getCSAExposureItem(final Trade trade,
                                                 JDate valDate, final Vector<String> errors, DSConnection dsConn,
                                                 PricingEnv pricingEnv) {
        final CSAExposureItem csaExpItem = new CSAExposureItem();
        CollateralExposure ce = null;
        CollateralConfig mcc = getMCContract(trade.getKeywordAsInt("MC_CONTRACT_NUMBER"));

        try {
            if (null != mcc) {
                // get plmarks
                PLMark plMark = CollateralUtilities.retrievePLMark(
                        trade.getLongId(), dsConn, pricingEnv.getName(), valDate);

                // get product
                ce = (CollateralExposure) trade.getProduct();

                // set fields
                csaExpItem.setCollatAgree(getCollatAgree(mcc));
                csaExpItem.setCollatAgreeType(getCollatAgreeType(trade));
                // GSM: 31/07/2013. Fix to show trade CTPY, not the contract
                // CTPY
                csaExpItem.setCpty(getCounterpartyTrade(trade, mcc));
                // csaExpItem.setCpty(getCounterparty(mcc));
                csaExpItem.setTradeID(getTradeID(trade));
                // tradeID - COL
                // frontID (extRef) - COL
                csaExpItem.setMtmDate(getMtmDate(plMark, valDate));
                //new spanish
                csaExpItem.setMtmDatesp(getMtmDatesp(plMark, valDate));
//				csaExpItem.setMtmDatesp();
                csaExpItem.setStructure(getStructure(trade));
                csaExpItem.setTradeDate(getTradeDate(trade));
                //new spanish
                csaExpItem.setTradeDatesp(getTradeDatesp(trade));
                csaExpItem.setStartTradeDatesp(getStartProductDatesp(trade));
                // valueDate (product->startDate) - COL
                csaExpItem.setMatDate(getMatDate(trade));
                //new spanish
                csaExpItem.setMatDatesp(getMatDatesp(trade));
                csaExpItem.setValAgent(getValAgent(mcc));
                // portfolio - COL
                csaExpItem.setOwner(getOwner(mcc)); // para owner y dealOwner
                // instrument - COL
                csaExpItem.setUnderlying(getUnderlying(ce));
                csaExpItem.setTradeCurr(getCurrency(ce));
                csaExpItem.setNotional(getNotional(ce));
                csaExpItem.setTradeCurr2(getCurrency2(ce));
                csaExpItem.setNotional2(getNotional2(ce));
                csaExpItem.setIndAmount(getIndependentAmount(plMark));
                csaExpItem.setFloatRate(getFloatRate(ce, mcc, valDate));
                csaExpItem.setFloatRate2(getFloatRate2(ce, mcc, valDate));
                csaExpItem.setPayRec(getPayRec(ce, trade));
                csaExpItem.setMtmCurr(getBaseCcy(mcc));
                csaExpItem.setMtmValue(getMtmValue(plMark));
                // GSM: portfolio reconciliation
                setNewPortfolioReconciliationFields(csaExpItem, trade);

            }
        } catch (RemoteException e) {
            Log.error(this, e); //sonar purpose
        }

        return csaExpItem;
    }

    /*
     * Fix to show trade CTPY, not the contract CTPY. In case the trade it's
     * null, returns the mcc Legal entity (as was before)
     */
    // GSM: 31/07/2013.
    private String getCounterpartyTrade(Trade trade, CollateralConfig mcc) {

        if ((trade != null) && (trade.getCounterParty() != null)
                && (trade.getCounterParty().getName() != null)) {
            return trade.getCounterParty().getName();
        }
        return getCounterparty(mcc);
    }

    /**
     * Adds the six new fields for DFA and EMIR for the portfolio
     * reconciliation.
     *
     * @param csaExpItem
     * @param trade
     */
    private void setNewPortfolioReconciliationFields(
            CSAExposureItem csaExpItem, Trade trade) {

        if ((trade == null) || (csaExpItem == null)) {
            return;
        }

        String temp = EMPTY;
        temp = (trade.getKeywordValue(USI_REFERENCE) != null) ? trade
                .getKeywordValue(USI_REFERENCE) : EMPTY;
        csaExpItem.setUsiReference(temp);

        temp = (trade.getKeywordValue(SD_MSP) != null) ? trade
                .getKeywordValue(SD_MSP) : EMPTY;
        csaExpItem.setSdMsp(temp);

        temp = (trade.getKeywordValue(US_PARTY) != null) ? trade
                .getKeywordValue(US_PARTY) : EMPTY;
        csaExpItem.setUsParty(temp);

        if (tradeIsFXSpot(trade)) {
            temp = NO;
        } else {
            temp = (trade.getKeywordValue(DFA_APPLICABLE) != null) ? trade
                    .getKeywordValue(DFA_APPLICABLE) : EMPTY;
        }
        csaExpItem.setDfaApplicable(temp);

        temp = (trade.getKeywordValue(FC_NFC) != null) ? trade
                .getKeywordValue(FC_NFC) : EMPTY;
        csaExpItem.setFcNfc(temp);

        temp = (trade.getKeywordValue(EMIR_APPLICABLE) != null) ? trade
                .getKeywordValue(EMIR_APPLICABLE) : EMPTY;
        csaExpItem.setEmirApplicable(temp);

        // GSM: 22/08/13. Added the 7? field for Port. Reconciliation
        temp = (trade.getKeywordValue(UTI_APPLICABLE) != null) ? trade
                .getKeywordValue(UTI_APPLICABLE) : EMPTY;
        csaExpItem.setUti(temp);
    }

    /*
     * If the subtype is FX SPOT return true.
     */
    private boolean tradeIsFXSpot(Trade trade) {

        Vector<String> domainValueProductSubTypes = null;
        domainValueProductSubTypes = LocalCache.getDomainValues(
                DSConnection.getDefault(), PRODUCT_SUBTYPE_DOMAIN_VALUE);

        if (!Util.isEmpty(domainValueProductSubTypes)) {
            if (domainValueProductSubTypes.contains(PRODUCT_MAPPING_FX_SPOT)) {
                if (trade.getProductSubType().equalsIgnoreCase(
                        PRODUCT_MAPPING_FX_SPOT)) {
                    return true;
                }
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private double getFXRate(final JDate date, final String ccy1, final String ccy2) {

        // check currencies
        if (Util.isEmpty(ccy1) || Util.isEmpty(ccy2)) {
            return 0.00;
        }

        if (!ccy1.equals(ccy2)) {
            String rate = "FX." + ccy1 + "." + ccy2;
            String clausule = "quote_name = '" + rate + "' and trunc(quote_date) = " + Util.date2SQLString(date);
            Vector<QuoteValue> vQuotes;
            try {
                vQuotes = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(clausule);
                if (!Util.isEmpty(vQuotes)) {
                    return vQuotes.get(0).getClose();
                }
                // COL_OUT_019
                // Carlos Cejudo: If the quote cannot be found try to find it
                // with the currencies reversed. Then the
                // value to return will be 1/getClose()
                else {
                    rate = "FX." + ccy2 + "." + ccy1;
                    clausule = "quote_name = '" + rate + "' and trunc(quote_date) = " + Util.date2SQLString(date);
                    vQuotes = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(clausule);
                    if (!Util.isEmpty(vQuotes)) {
                        return 1.0 / vQuotes.get(0).getClose();
                    }
                    return 0.00; // no encuentra rate para fecha
                }
            } catch (final RemoteException e) {
                Log.error(CollateralUtilities.class, "Cannot load Rate: " + rate + " : " + e);
                return 0.00;
            }

        }
        return 1.00;
    }
}
