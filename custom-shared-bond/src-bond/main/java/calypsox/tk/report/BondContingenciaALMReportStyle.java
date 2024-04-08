package calypsox.tk.report;


import calypsox.tk.bo.fiflow.builder.handler.FIFlowTradeSecurityHandler;
import calypsox.tk.core.SantanderUtil;
import calypsox.tk.report.util.UtilReport;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.*;
import com.calypso.tk.bo.mapping.triparty.SecCode;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.flow.CashFlowCoupon;
import com.calypso.tk.product.flow.CashFlowOptionCoupon;
import com.calypso.tk.product.util.NotionalDate;
import com.calypso.tk.refdata.Country;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.FdnUtilProvider;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;


public class BondContingenciaALMReportStyle extends TradeReportStyle {


    public static final String FEC_DATO = "FEC_DATO";
    public static final String COD_ENTIDAD = "COD_ENTIDAD";
    public static final String COD_SECURITY = "COD_SECURITY";
    public static final String COD_BO = "COD_BO";
    public static final String COD_SUCURSALCONT = "COD_SUCURSALCONT";
    public static final String COD_PRDT = "COD_PRDT";
    public static final String COD_MONE_O = "COD_MONE_O";
    public static final String COD_PORTF = "COD_PORTF";
    public static final String COD_COBE = "COD_COBE";
    public static final String COD_FRT = "COD_FRT";
    public static final String COD_TIP_REF = "COD_TIP_REF";
    public static final String FEC_VALOR = "FEC_VALOR";
    public static final String FEC_INICIO = "FEC_INICIO";
    public static final String FEC_VENCIMIENTO = "FEC_VENCIMIENTO";
    public static final String IMP_NOM_O = "IMP_NOM_O";
    public static final String IMP_NOMINAL = "IMP_NOMINAL";
    public static final String IMP_VAL_CONT_O = "IMP_VAL_CONT_O";
    public static final String IMP_VAL_CONT_LCL = "IMP_VAL_CONT_LCL";
    public static final String IMP_VAL_MTM_O = "IMP_VAL_MTM_O";
    public static final String IMP_VAL_MTM_LCL = "IMP_VAL_MTM_LCL";
    public static final String IND_DIREC = "IND_DIREC";
    public static final String IND_TIP_CARTERA = "IND_TIP_CARTERA";
    public static final String IND_INTRAAREA = "IND_INTRAAREA";
    public static final String COD_NMOT = "COD_NMOT";
    public static final String COD_SECTOR_CONTABLE = "COD_SECTOR_CONTABLE";
    public static final String COD_AMORT = "COD_AMORT";
    public static final String COD_PAIS = "COD_PAIS";
    public static final String FEC_PROX_AMORT = "FEC_PROX_AMORT";
    public static final String FEC_PROX_LIQ_INT = "FEC_PROX_LIQ_INT";
    public static final String FEC_PROX_REP = "FEC_PROX_REP";
    public static final String FEC_ULT_REP = "FEC_ULT_REP";
    public static final String IND_LIQ = "IND_LIQ";
    public static final String IND_GRUPO = "IND_GRUPO";
    public static final String IND_COTIZ = "IND_COTIZ";
    public static final String IND_TIP_FREC_AMORT = "IND_TIP_FREC_AMORT";
    public static final String IND_TIP_FREC_REP = "IND_TIP_FREC_REP";
    public static final String IND_TIP_FREC_LIQ_INT = "IND_TIP_FREC_LIQ_INT";
    public static final String PER_TASA_INT = "PER_TASA_INT";
    public static final String SPREAD = "SPREAD";
    public static final String CLEAN_PRICE = "CLEAN_PRICE";
    public static final String DIRTY_PRICE = "DIRTY_PRICE";
    public static final String IND_ELEGIBLE_BCE = "IND_ELEGIBLE_BCE";
    public static final String IND_ELEGIBLE_BDE = "IND_ELEGIBLE_BDE";
    public static final String IND_ELEGIBLE_FED = "IND_ELEGIBLE_FED";
    public static final String IND_ELEGIBLE_FSA = "IND_ELEGIBLE_FSA";
    private static final String ALM_BRANCH_CODE = "ALM_BRANCH_CODE";
    private static final String ALM_ENTITY_CODE = "ALM_ENTITY_CODE";

    private final FIFlowTradeSecurityHandler securityWrapper = new FIFlowTradeSecurityHandler();


    @SuppressWarnings("rawtypes")
    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) {
        final Trade trade = row.getProperty(ReportRow.TRADE);
        final PricingEnv pricingEnv = ReportRow.getPricingEnv(row);
        final JDatetime valDateTime = ReportRow.getValuationDateTime(row);
        final JDate valDate = valDateTime.getJDate(pricingEnv.getTimeZone());

        if (trade != null) {
            this.securityWrapper.initRelatedSecutityData(trade.getProduct());
            this.securityWrapper.initRelatedSecPricesData(getPreviousBusinessDay(valDate));
        }
        if (!(trade.getProduct() instanceof Bond)) {
            return null;
        }

        final Bond bond = (Bond) trade.getProduct();

        if (columnName.equals(FEC_DATO)) {
            return formatDate(valDate);
        }

        if (columnName.equals(COD_ENTIDAD)) {
            LegalEntityAttribute almEntityCode = BOCache.getLegalEntityAttribute(DSConnection.getDefault(),
                    trade.getBook().getProcessingOrgBasedId(), trade.getBook().getProcessingOrgBasedId(), LegalEntity.PROCESSINGORG, ALM_ENTITY_CODE);

            if (almEntityCode != null)
                return almEntityCode.getAttributeValue();
            String partenonId = trade.getKeywordValue("PartenonAccountingID");
            return (!Util.isEmpty(partenonId) && partenonId.length() == 21) ? partenonId.substring(0, 4) : "";
        }

        if (columnName.equals(COD_SECURITY)) {
            return bond.getSecCode(SecCode.ISIN);
        }

        if (columnName.equals(COD_BO)) {
            return trade.getLongId();
        }

        if (columnName.equals(COD_SUCURSALCONT)) {
            LegalEntityAttribute almBranchCode = BOCache.getLegalEntityAttribute(DSConnection.getDefault(),
                    trade.getBook().getProcessingOrgBasedId(), trade.getBook().getProcessingOrgBasedId(), LegalEntity.PROCESSINGORG, ALM_BRANCH_CODE);
            if (almBranchCode != null)
                return almBranchCode.getAttributeValue();
            String partenonId = trade.getKeywordValue("PartenonAccountingID");
            return (!Util.isEmpty(partenonId) && partenonId.length() == 21) ? partenonId.substring(4, 8) : "";
        }

        if (columnName.equals(COD_PRDT)) {
            return bond.getBondType();
        }

        if (columnName.equals(COD_MONE_O)) {
            return trade.getTradeCurrency();
        }

        if (columnName.equals(COD_PORTF)) {
            return trade.getBook().getName();
        }

        if (columnName.equals(COD_COBE)) {
            return "*";
        }

        if (columnName.equals(COD_FRT)) {
            final Object value = super.getColumnValue(row, TRADE_KEYWORD_PREFIX + "MurexRootContract", errors);
            if (value != null) {
                return value.toString();
            }
            return null;
        }

        if (columnName.equals(COD_TIP_REF)) {
            StringBuilder codTipoReferencia = new StringBuilder();
            if(!bond.getFixedB()){
                codTipoReferencia.append(bond.getRateIndex().getName());
                codTipoReferencia.append(" ");
                codTipoReferencia.append(bond.getRateIndex().getTenor());
                codTipoReferencia.append(" ");
                codTipoReferencia.append(bond.getRateIndex().getDayCount());
            }
            return codTipoReferencia.toString();
        }

        if (columnName.equals(FEC_VALOR)) {
            return formatDate(trade.getSettleDate());
        }

        if (columnName.equals(FEC_INICIO)) {
            return formatDate(trade.getTradeDate().getJDate(TimeZone.getDefault()));
        }

        if (columnName.equals(FEC_VENCIMIENTO)) {
            return formatDate(bond.getMaturityDate());
        }

        if (columnName.equals(IMP_NOM_O)) {
            return formatResult(trade.computeNominal());
        }

        if (columnName.equals(IMP_NOMINAL)) {
            return formatResult(trade.computeNominal(JDate.valueOf(trade.getTradeDate())));
        }

        if (columnName.equals(IMP_VAL_CONT_O)) {
            Double mtmFullLago = row.getProperty("MTM_NET_MUREX");
            return formatResult(mtmFullLago);
        }

        if (columnName.equals(IMP_VAL_CONT_LCL)) {
            try {
                Double mtmFullLago = CollateralUtilities.convertCurrency(trade.getTradeCurrency(), row.getProperty("MTM_NET_MUREX"),  trade.getBook().getBaseCurrency(), valDate, pricingEnv);
                if(mtmFullLago != null){
                    return formatResult(mtmFullLago);
                }
            } catch (MarketDataException e) {
                Log.error(this, "Could not convert currency for field Nominal on trade "+ trade.getLongId());
            }
            return null;
        }

        if (columnName.equals(IMP_VAL_MTM_O)) {
            Double mtmFullLago = row.getProperty("MTM_NET_MUREX");
            return formatResult(mtmFullLago);
        }

        if (columnName.equals(IMP_VAL_MTM_LCL)) {
            try {
                Double mtmFullLago = CollateralUtilities.convertCurrency(trade.getTradeCurrency(), row.getProperty("MTM_NET_MUREX"),  trade.getBook().getBaseCurrency(), valDate, pricingEnv);
                if(mtmFullLago != null){
                    return formatResult(mtmFullLago);
                }
            } catch (MarketDataException e) {
                Log.error(this, "Could not convert currency for field Nominal on trade "+ trade.getLongId());
            }
            return null;

        }

        if (columnName.equals(IND_DIREC)) {
            int buySell = bond.getBuySell(trade);
            return buySell==1 ? "B" : "S";
        }

        if (columnName.equals(IND_TIP_CARTERA)) {
            Book book = trade.getBook();
            if(book!=null) {
                AccountingBook acctBook = book.getAccountingBook();
                if(acctBook != null) {
                    String acctBookName = acctBook.getName();
                    if(acctBookName.equals("Negociacion")) {
                        return "NE";
                    }
                    else if(acctBookName.equals("Inversion crediticia")) {
                        return "IC";
                    }
                    else if(acctBookName.equals("Inversion a vencimiento")) {
                        return "IV";
                    }
                    else if(acctBookName.equals("Disponible para la venta")) {
                        return "DV";
                    }
                    else if(acctBookName.equals("Designados a valor razonable")) {
                        return "DVR";
                    }
                }
            }
            return "NEG";
        }

        if (columnName.equals(IND_INTRAAREA)) {
            final Object isInternalDeal = super.getColumnValue(row, IS_INTERNAL_DEAL, errors);
            if (Util.isTrue(isInternalDeal, false)) {
                return "I";
            }
            return "M";
        }

        if (columnName.equals(COD_NMOT)) {
            if(bond.getIssuerId() > 0) {
                LegalEntity issuer = null;
                try {
                    issuer = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(bond.getIssuerId());
                    if (issuer != null) {
                        return issuer.getCode();
                    }
                } catch (CalypsoServiceException e) {
                    Log.error(this, "Could not get the bond issuer with id: " + bond.getIssuerId());
                }
            }
            return "";
        }

        if (columnName.equals(COD_SECTOR_CONTABLE)) {
            if(bond.getIssuerId() > 0) {
                LegalEntity issuer = null;
                try {
                    issuer = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(bond.getIssuerId());
                    return issuer != null ? getLEAttr(issuer.getId(), "SECTORCONTABLE") : "";
                } catch (CalypsoServiceException e) {
                    Log.error(this, "Could not get the bond issuer with id: " + bond.getIssuerId());
                }
            }
            return "";
        }

        if (columnName.equals(COD_AMORT)) {
            return bond.getAmortStructure();
        }

        if (columnName.equals(COD_PAIS)) {
            if(bond.getIssuerId() > 0) {
                LegalEntity issuer = null;
                try {
                    issuer = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(bond.getIssuerId());
                    if(issuer != null){
                        Country country = BOCache.getCountry(DSConnection.getDefault(), issuer.getCountry());
                        return country != null ? country.getISOCode(): "";
                    }
                } catch (CalypsoServiceException e) {
                    Log.error(this, "Could not get the bond issuer with id: " + bond.getIssuerId());
                }
            }
            return null;
        }

        if (columnName.equals(FEC_PROX_AMORT)) {
            JDate nextAmortDate = null;
            Vector<NotionalDate> amortSchedule = (Vector<NotionalDate>)bond.getAmortSchedule();
            if (amortSchedule != null && amortSchedule.size() > 0) {
                for (int i = 0; i < amortSchedule.size(); i++) {
                    JDate amortDate = amortSchedule.get(i).getStartDate();
                    if(amortDate.after(JDate.getNow())){
                        if(nextAmortDate == null){
                            nextAmortDate = amortDate;
                        }
                        else if(amortDate.before(nextAmortDate)){
                            nextAmortDate = amortDate;
                        }
                    }
                }
                if(nextAmortDate != null) {
                    return formatDate(nextAmortDate);
                }
            }
            return "";
        }

        if (columnName.equals(FEC_PROX_LIQ_INT)) {
            return null != bond.getNextCouponDate(valDate) ? formatDate(bond.getNextCouponDate(valDate)) : "" ;
        }

        if (columnName.equals(FEC_PROX_REP)) {
            if (!bond.getFixedB()) {
                return null != bond.getNextResetDate(valDate) ? formatDate(bond.getNextResetDate(valDate)) : "";
            }
            return null;
        }

        if (columnName.equals(FEC_ULT_REP)) {
            if (!bond.getFixedB()) {
                JDate prevResetDate = null;
                CashFlowSet cfs = bond.getFlows();
                if (cfs != null) {
                    for (int i = 0; i < cfs.size(); i++) {
                        CashFlow cashFlow = cfs.get(i);
                        JDate flowResetDate = null;
                        if (cashFlow instanceof CashFlowCoupon) {
                            flowResetDate = ((CashFlowCoupon) cashFlow).getResetDate();
                        } else if (cashFlow instanceof CashFlowOptionCoupon) {
                            flowResetDate = ((CashFlowOptionCoupon) cashFlow).getResetDate();
                        }
                        if(flowResetDate != null){
                            if(flowResetDate.before(JDate.getNow())){
                                if(prevResetDate == null) {
                                    prevResetDate = flowResetDate;
                                }
                                else{
                                   if(prevResetDate.before(flowResetDate)){
                                       prevResetDate = flowResetDate;
                                   }
                                }
                            }
                        }
                    }
                    return prevResetDate != null ? formatDate(prevResetDate) : "";
                }
            }
            return null;
        }

        if (columnName.equals(IND_LIQ)) {
            return "*";
        }

        if (columnName.equals(IND_GRUPO)) {
            if(bond.getIssuerId() > 0) {
                LegalEntity issuer = null;
                try {
                    issuer = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(bond.getIssuerId());
                    if(issuer != null) {
                        final Vector leAtt = BOCache.getLegalEntityAttributes(DSConnection.getDefault(), issuer.getId());
                        String intragrupo = SantanderUtil.getInstance().getLEAttributeValue(leAtt,"INTRAGROUP");
                        return (!Util.isEmpty(intragrupo) && "S".equalsIgnoreCase(intragrupo)) ? "S" : "N";
                    }
                } catch (CalypsoServiceException e) {
                    Log.error(this, "Could not get the bond issuer with id: " + bond.getIssuerId());
                }
            }
            return "";
        }

        if (columnName.equals(IND_COTIZ)) {
            return "*";
        }

        if (columnName.equals(IND_TIP_FREC_AMORT)) {
            Vector<NotionalDate> amortSchedule = (Vector<NotionalDate>)bond.getAmortSchedule();
            if (amortSchedule != null && amortSchedule.size() > 0) {
                return bond.getAmortFrequency().getTenor().getName();
            }
            return "";
        }

        if (columnName.equals(IND_TIP_FREC_REP)) {
            if (!bond.getFixedB()) {
                return bond.getRateIndex().getTenor().getName();
            }
            return null;
        }

        if (columnName.equals(IND_TIP_FREC_LIQ_INT)) {
            return bond.getCouponFrequency().getTenor().getName();
        }

        if (columnName.equals(PER_TASA_INT)) {
            CashFlowSet cfs = bond.getFlows();
            if(cfs != null){
                CashFlow cashFlow = getEnclosingCashFlow(cfs);
                if(cashFlow != null) {
                    if(cashFlow instanceof CashFlowCoupon){
                        return cashFlow != null ? ((CashFlowCoupon) cashFlow).getRate()*100 : null;
                    }
                    else if(cashFlow instanceof CashFlowOptionCoupon) {
                        return cashFlow != null ? ((CashFlowOptionCoupon) cashFlow).getRate()*100 : null;
                    }
                }
                return null;
            }
        }

        if (columnName.equals(SPREAD)) {
            return !bond.getFixedB() ? bond.getRateIndexSpread() * 100 : "";
        }

        if (columnName.equals(CLEAN_PRICE)) {
            return formatResult(Optional.of(trade)
                    .map(Trade::getTradePrice)
                    .map(this.securityWrapper::getSecurityPriceDisplayValue)
                    .map(this::adjustPriceBase)
                    .orElse(0.0D));
        }

        if (columnName.equals(DIRTY_PRICE)) {
            return formatResult(Optional.ofNullable(trade)
                    .map(Trade::getNegociatedPrice)
                    .map(this.securityWrapper::getSecurityPriceDisplayValue)
                    .map(this::adjustPriceBase)
                    .orElse(0.0D));
        }

        if (columnName.equals(IND_ELEGIBLE_BCE)) {
            return "*";
        }

        if (columnName.equals(IND_ELEGIBLE_BDE)) {
            return "*";
        }

        if (columnName.equals(IND_ELEGIBLE_FED)) {
            return "*";
        }

        if (columnName.equals(IND_ELEGIBLE_FSA)) {
            return "*";
        }

        return formatResult(super.getColumnValue(row, columnName, errors));
    }


    public static Object formatResult(Object o) {
        return UtilReport.formatResult(o, '.');
    }


    private String formatDate(JDate jDate){
        String date = "";
        if (jDate != null) {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            date = format.format(jDate.getDate());
        }
        return date;
    }


    private CashFlow getEnclosingCashFlow(CashFlowSet cashFlowSet) {
        CashFlow cashFlow = (CashFlow) cashFlowSet.findEnclosingCashFlow(JDate.getNow(), CashFlow.INTEREST);
        if(cashFlow == null){
            cashFlow = (CashFlow) cashFlowSet.findEnclosingCashFlow(JDate.getNow(), CashFlow.PRINCIPAL);
        }
        return cashFlow;
    }


    private String getLEAttr(int leId, String attributeName){
        LegalEntityAttribute attribute = BOCache.getLegalEntityAttribute(DSConnection.getDefault(),0, leId,"ALL", attributeName);
        return Optional.ofNullable(attribute).map(LegalEntityAttribute::getAttributeValue).orElse("");
    }

    /**
     * For quote retrieval
     *
     * @return JDate
     */
    private JDate getPreviousBusinessDay(JDate valDate) {
        Vector<String> holidays = Util.string2Vector("SYSTEM");
        return valDate.addBusinessDays(-1, holidays);
    }

    /**
     *
     * @param displayValue
     * @return
     */
    private double adjustPriceBase(DisplayValue displayValue) {
        return Optional.ofNullable(displayValue)
                .map(DisplayValue::toString)
                .map(stringValue -> FdnUtilProvider.getNumberFormattingUtil().stringToNumber(stringValue, null, null))
                .orElse(1.00D);
    }

}
