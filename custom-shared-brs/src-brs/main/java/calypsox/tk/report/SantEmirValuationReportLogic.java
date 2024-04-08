/*
 *
 * Copyright (c) ISBAN: Ingeniería de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;


import calypsox.tk.core.KeywordConstantsUtil;
import calypsox.tk.report.emir.field.EmirFieldBuilderUtil;
import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.tk.util.LegalEntityAttributesCache;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

/**
 * SantEmirSnapshotReportLogic add the logic needed in DFA Material Terms
 * report.
 *
 */
// CAL_EMIR_026
public class SantEmirValuationReportLogic {
    //extends GenericReg_SantDTCCGTRSnapshotLogic {

    public static final String INTERNAL = "INTERNAL";

    private static final String EMTPY_STRING = "";

    private static final String VALUATION = "Valuation";

    private static final String MARKET_TO_MARKET = "MarkToMarket";

    private static final String SENDTO_DEFAULT = "DTCCEU";

    private static final String ESMA = "ESMA";

    private static final String CREDIT = "Credit";

    private static final String VALUATION_TYPE = "VAL";

    private static final String DEFAULT_TIME = "T00:00:00Z";
    private static final String CCP = "CCP";

    private Trade currentTrade = null;

    private JDatetime currentValDatetime = null;

    // FullToDeltaSubmitter
    private static final String LEI = "LEI";

    private static final String EMPTY_SPACE = "";

    // FullToDeltaSubmitter - End

    /**
     * Literal NEW.
     */
    private static final String NEW = "New";

    /**
     * SantEmirValuationReportLogic constructor.
     *
     * @param trade Trade.
     * @param valDatetime Valuation date of the report.
     */
    public SantEmirValuationReportLogic(final Trade trade, final JDatetime valDatetime) {
        currentTrade = trade;
        currentValDatetime = valDatetime;
    }

    /**
     * Fills the item with the logic.
     *
     * @return SantEmirValuationReportItem Item to be filled.
     */
    public SantEmirValuationReportItem fillItem() {
        final SantEmirValuationReportItem item = new SantEmirValuationReportItem();

        //TODO a ver UTI Logic
        item.setColumnValue(SantEmirValuationColumns.VALUATIONTYPEPARTY1.toString(), getLogicValTypeParty1());
        item.setReportTypeValue(SantEmirValuationColumns.VALUATIONTYPEPARTY1.toString(), VALUATION_TYPE);

        item.setReportTypeValue(SantEmirValuationColumns.VALDATETIME.toString(), VALUATION_TYPE);
        item.setColumnValue(SantEmirValuationColumns.VALDATETIME.toString(), getLogicValdatetime());

        item.setColumnValue(SantEmirValuationColumns.UTIPREFIX.toString(), getLogicUtiPrefix());
        item.setReportTypeValue(SantEmirValuationColumns.UTIPREFIX.toString(), VALUATION_TYPE);

        item.setColumnValue(SantEmirValuationColumns.UTI.toString(), getLogicUtiValue());
        item.setReportTypeValue(SantEmirValuationColumns.UTI.toString(), VALUATION_TYPE);

        item.setColumnValue(SantEmirValuationColumns.TRADEPARTYVAL2.toString(), getLogicTRADEPARTYVAL2());
        item.setReportTypeValue(SantEmirValuationColumns.TRADEPARTYVAL2.toString(), VALUATION_TYPE);

        item.setColumnValue(SantEmirValuationColumns.TRADEPARTYVAL1.toString(), getLogicTradePartyVal1());
        item.setReportTypeValue(SantEmirValuationColumns.TRADEPARTYVAL1.toString(), VALUATION_TYPE);

        item.setColumnValue(SantEmirValuationColumns.TRADEPARTYTRANSACTIONID1.toString(), getLogicOurRef());
        item.setReportTypeValue(SantEmirValuationColumns.TRADEPARTYTRANSACTIONID1.toString(), VALUATION_TYPE);

        item.setColumnValue(SantEmirValuationColumns.TRADEPARTYPREF2.toString(), getLogicTRADEPARTYPREF2());
        item.setReportTypeValue(SantEmirValuationColumns.TRADEPARTYPREF2.toString(), VALUATION_TYPE);

        item.setColumnValue(SantEmirValuationColumns.TRADEPARTYPREF1.toString(), getLogicTradePartyPref1());
        item.setReportTypeValue(SantEmirValuationColumns.TRADEPARTYPREF1.toString(), VALUATION_TYPE);

        item.setColumnValue(SantEmirValuationColumns.SUBMITTEDVALUE.toString(), getLogicSubmittedValue());
        item.setReportTypeValue(SantEmirValuationColumns.SUBMITTEDVALUE.toString(), VALUATION_TYPE);

        item.setColumnValue(SantEmirValuationColumns.SUBMITTEDFORPREFIX.toString(),getLogicSubmittedForPrefix());
        item.setReportTypeValue(SantEmirValuationColumns.SUBMITTEDFORPREFIX.toString(), VALUATION_TYPE);

        item.setColumnValue(SantEmirValuationColumns.SENDTO.toString(), getLogicSendTo());
        item.setReportTypeValue(SantEmirValuationColumns.SENDTO.toString(), VALUATION_TYPE);

        item.setColumnValue(SantEmirValuationColumns.PRODUCTVALUE.toString(), getProductValue());
        item.setReportTypeValue(SantEmirValuationColumns.PRODUCTVALUE.toString(), VALUATION_TYPE);

        item.setColumnValue(SantEmirValuationColumns.PRIMASSETCLASS.toString(), CREDIT);
        item.setReportTypeValue(SantEmirValuationColumns.PRIMASSETCLASS.toString(), VALUATION_TYPE);

        item.setColumnValue(SantEmirValuationColumns.PARTY1REPORTINGONL.toString(), ESMA);
        item.setReportTypeValue(SantEmirValuationColumns.PARTY1REPORTINGONL.toString(), VALUATION_TYPE);

        item.setColumnValue(SantEmirValuationColumns.PARTYREPOBLIGATION1.toString(), getLogicPartyRepObligation1());
        item.setReportTypeValue(SantEmirValuationColumns.PARTYREPOBLIGATION1.toString(), VALUATION_TYPE);

        item.setColumnValue(SantEmirValuationColumns.MTMVALUE.toString(),
                EmirFieldBuilderUtil.getInstance().roundAmountByLength(getLogicMtmValue(), 20, 4));
        item.setReportTypeValue(SantEmirValuationColumns.MTMVALUE.toString(), VALUATION_TYPE);

        item.setColumnValue(SantEmirValuationColumns.MTMCURRENCY.toString(), getLogicMtmCcy());
        item.setReportTypeValue(SantEmirValuationColumns.MTMCURRENCY.toString(), VALUATION_TYPE);

        item.setColumnValue(SantEmirValuationColumns.MESSAGETYPE.toString(), getLogicMessageType());
        item.setReportTypeValue(SantEmirValuationColumns.MESSAGETYPE.toString(), VALUATION_TYPE);

        item.setColumnValue(SantEmirValuationColumns.LEIVALUE.toString(), getLogicLeiValue());
        item.setReportTypeValue(SantEmirValuationColumns.LEIVALUE.toString(), VALUATION_TYPE);

        item.setColumnValue(SantEmirValuationColumns.LEIPREFIX.toString(), getLogicLeiPrefix());
        item.setReportTypeValue(SantEmirValuationColumns.LEIPREFIX.toString(), VALUATION_TYPE);

        item.setColumnValue(SantEmirValuationColumns.CLEARINGSTATUS.toString(), getLogicCLEARINGSTATUS());
        item.setReportTypeValue(SantEmirValuationColumns.CLEARINGSTATUS.toString(), VALUATION_TYPE);

        item.setColumnValue(SantEmirValuationColumns.ASSETCLASS.toString(), getLogicAssetClass());
        item.setReportTypeValue(SantEmirValuationColumns.ASSETCLASS.toString(), VALUATION_TYPE);

        item.setColumnValue(SantEmirValuationColumns.ADDCOMMENTS.toString(), getLogicAddComments());
        item.setReportTypeValue(SantEmirValuationColumns.ADDCOMMENTS.toString(), VALUATION_TYPE);

        item.setColumnValue(SantEmirValuationColumns.ACTIONTYPE1.toString(), getLogicActionType1());
        item.setReportTypeValue(SantEmirValuationColumns.ACTIONTYPE1.toString(), VALUATION_TYPE);

        item.setColumnValue(SantEmirValuationColumns.ACTION.toString(), getLogicAction());
        item.setReportTypeValue(SantEmirValuationColumns.ACTION.toString(), VALUATION_TYPE);


        return item;
    }

    private Object getLogicAction() {
        return NEW;
    }

    private String getLogicTRADEPARTYVAL2() {
    /*
            Si en el campo TRADEPARTYPREF2 se ha informado con valor LEI, entonces el campo se rellena con el valor del atributo LEI de la contrapartida.
            Si en el campo TRADEPARTYPREF2 se ha informado con valor INTERNAL, entonces el campo se rellena con el valor del Short Name (Codigo GLCS) de la contrapartida.
            Misma logica FX
     */
        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;

        String pref2 = getLogicTRADEPARTYPREF2();
        if (LEI.equalsIgnoreCase(pref2)) {
            rst = getLeiAttributeValue(true);
        }
        else if (INTERNAL.equalsIgnoreCase(pref2)) {
            rst = currentTrade.getCounterParty().getName();
        }

        return rst;


    }

    private String getLogicTRADEPARTYPREF2() {

        String rst = EmirSnapshotReduxConstants.LEI;

        final String lei = LegalEntityAttributesCache.getInstance()
                .getAttributeValue(currentTrade, KeywordConstantsUtil.LE_ATTRIBUTE_LEI,
                        true);

        if (Util.isEmpty(lei)) {
            rst = EmirSnapshotReduxConstants.INTERNAL;
        }
        return rst;

    }

    private Object getLogicAssetClass() {
        return EmirSnapshotReduxConstants.CREDIT;
    }

    /**
     * Returns the party rep obligation 1.
     *
     * @return "ESMA".
     */
    private Object getLogicPartyRepObligation1() {
        return ESMA;
    }

    /**
     * Returns the message type.
     *
     * @return "Valuation".
     */
    protected String getLogicMessageType() {
        return VALUATION;
    }

    /**
     * get the send to value
     *
     * @return value
     */
    private Object getLogicSendTo() {
        return SENDTO_DEFAULT;
    }

    /**
     * get the valuation type party 1 value
     *
     * @return value
     */
    private Object getLogicValTypeParty1() {
        String rst =  MARKET_TO_MARKET;
        String strClearingStatus = getLogicCLEARINGSTATUS();
        if (Boolean.TRUE.toString().equalsIgnoreCase(strClearingStatus)) {
            rst = CCP;
        }
        return rst;
    }

    /**
     * get the mtm ccy value
     *
     * @return value
     */
    private Object getLogicMtmCcy() {
        // EMIR_Clearing_House
    /*
    Nueva logica
    Si la contrapartida no es de camara se reporta el campo Currency de la pata bono de la operación
    SI la contrapartida es de camara se reporta el MtM recibido como divisa en el fichero de la camara.
     */

        String rst = "EUR";
        if (currentTrade != null) {
            PLMarkValue mtmValue = getPLMarkValue();
            if (mtmValue != null
                        && (!Util.isEmpty(mtmValue.getCurrency()))) {
                rst = mtmValue.getCurrency();
            }
        }

        return rst;
    }

    /**
     * get the mtm value
     *
     * @return value
     */
    protected String getLogicMtmValue() {
        String rst = EMPTY_SPACE;
        PLMarkValue plMarkValue = getPLMarkValue();
        Double mtmValue = 0.0d;
        if (plMarkValue != null) {
            mtmValue = plMarkValue.getMarkValue();
        }

        rst = new BigDecimal(mtmValue.doubleValue()).setScale(2 , RoundingMode.HALF_EVEN).toPlainString();
        //rst = String.format("%.2f", mtmValue);
        rst = rst.replaceAll(",", ".");
        return rst;
    }

    protected PLMarkValue getPLMarkValue() {
        PricingEnv pricingEnv =  PricingEnv.loadPE("DirtyPrice", currentValDatetime);
        PLMarkValue result = null;

        JDate currentDate = currentValDatetime.getJDate(TimeZone.getDefault());

        try {

            PLMark plMark  =  CollateralUtilities.retrievePLMark(currentTrade, DSConnection.getDefault(),
                    pricingEnv.getName(), currentDate);

            if (plMark!= null) {
                result  = plMark.getPLMarkValueByName("MIS_NPV");
                 if (result !=  null
                        && result.getMarkValue() != 0.0) {
                    return result;
                }

                result = plMark.getPLMarkValueByName("NPV");
                if (result !=  null
                        && result.getMarkValue() != 0.0) {
                    return result;
                }
            }

        } catch (RemoteException e) {
            Log.error(this, e);
        }

        return null;
    }

    /**
     * get the valdatetime value
     *
     * @return value
     */
    private Object getLogicValdatetime() {
        String rst = EMTPY_STRING;

        // return the day of MTM
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        rst = sdf.format(currentValDatetime);

        return rst + DEFAULT_TIME;
    }

    // FullToDeltaSubmitter
    /**
     * Returns LEIAttribute value
     *
     * @param isCptyAttr
     * @return LEIAttribute value
     */
    private String getLeiAttributeValue(final boolean isCptyAttr) {
        String rst = EMPTY_SPACE;

        final String leiAttr = LegalEntityAttributesCache.getInstance().getAttributeValue(currentTrade,
                KeywordConstantsUtil.LE_ATTRIBUTE_LEI, isCptyAttr);
        if (!Util.isEmpty(leiAttr)) {
            rst = leiAttr;
        }
        return rst;
    }

    /**
     * Returns SubmittedForPrefix value.
     *
     * @return SubmittedForPrefix value.
     */
    private String getLogicSubmittedForPrefix() {
        return LEI;
    }

    /**
     * Returns SubmittedValue value.
     *
     * @return SubmittedValue value.
     */
    private String getLogicSubmittedValue() {
        return getLeiAttributeValue(false);
    }

    // FullToDeltaSubmitter - End

    /**
     * Get the action.
     *
     * @return action
     */
    protected String getLogicActionEmir() {
        // String rst = getLogicActionTag();
        final String rst = NEW;
        return rst;
    }

    /**
     * Returns the Product Type, from the following list: ForeignExchange:Spot,
     * ForeignExchange:Forward o ForeignExchange:NDF.
     *
     * @return Reporting Product Type.
     */
    public String getProductValue() {

        String rst = null;

        if (currentTrade.getProduct() instanceof PerformanceSwap) {
            rst = EmirSnapshotReduxConstants.CREDIT_TOTAL_RETURN_SWAP;
        }

        return rst;

    }

    /**
     * Returns TradePartyPref1 value.
     *
     * @return TradePartyPref1 value.
     */
    private String getLogicTradePartyPref1() {
        return LEI;
    }

    /**
     * Returns LEIPrefix value.
     *
     * @return LEIPrefix value.
     */
    private String getLogicLeiPrefix() {
        return LEI;
    }

    /**
     * Returns LEI value.
     *
     * @return LEI value.
     */
    private String getLogicLeiValue() {
        return getLeiAttributeValue(false);
    }

    /**
     * Get our ref.
     *
     * @return our ref
     */
    protected String getLogicOurRef() {
        String rst = EMPTY_SPACE;
        if (currentTrade != null) {
            rst = String.valueOf(currentTrade.getLongId());
        }

        return rst;
    }

    /**
     * Get the uti prefix.
     *
     * @return uti prefix
     */
    protected String getLogicUtiPrefix() {

        String rst = currentTrade
                .getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_UTI_REFERENCE);
        if (Util.isEmpty(rst)) {
            rst = EmirFieldBuilderUtil.getInstance().getUtiTemporal(currentTrade);
        }


        // CAL_EMIR_011
        if (Util.isEmpty(rst)) {
            rst = EMPTY_SPACE;
        } else {
            if (rst.length() >= 10) {
                rst = rst.substring(0, 10);
            }
        }
        return rst;

    }

    /**
     * Get the uti value.
     *
     * @return uti value
     */
    protected String getLogicUtiValue() {

        String utiValue = currentTrade
                .getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_UTI_REFERENCE);

        if (Util.isEmpty(utiValue)) {
            utiValue = EmirFieldBuilderUtil.getInstance().getUtiTemporal(currentTrade);
        }

        if (!Util.isEmpty(utiValue)) {
            if (utiValue.length() >= 10) {
                return utiValue.substring(10);
            }
        }

        if (Util.isEmpty(utiValue)) {
            utiValue = EMPTY_SPACE;
        }

        return utiValue;

    }

    /**
     * Returns TradePartyVal1.
     *
     * @return TradePartyVal1.
     */
    private String getLogicTradePartyVal1() {
        return getLeiAttributeValue(false);
    }

    /**
     * Returns ClearingStatus.
     *
     * @return false.
     */
    private String getLogicCLEARINGSTATUS() {
        String rst = Boolean.FALSE.toString().toLowerCase();

        Vector<String> clearingHouses  = LocalCache.getDomainValues(DSConnection.getDefault(), EmirSnapshotReduxConstants.DV_EMIR_CLEARING_HOUSE);
        String cptyCode = currentTrade.getCounterParty().getCode();

        if (!Util.isEmpty(clearingHouses) && clearingHouses.contains(cptyCode))  {
            rst =  Boolean.TRUE.toString().toLowerCase();
        }
        return rst;
    }

    /**
     * Returns ActionType1.
     *
     * @return V.
     */
    private String getLogicActionType1() {
        return "V";
    }

    // DDR - Inform GLCS 08/18
    /**
     * Returns the Add Comments.
     *
     * @return Add Comments.
     */
    protected String getLogicAddComments() {
        final LegalEntity cp = currentTrade.getCounterParty();
        if (cp != null) {
            return cp.getCode();
        }
        return EMPTY_SPACE;
    }
    // DDR - Inform GLCS 08/18 - End

}
