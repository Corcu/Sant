package calypsox.tk.report;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.*;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteSet;
import com.calypso.tk.product.*;
import com.calypso.tk.refdata.*;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.SecLendingReportStyle;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.secfinance.SecFinanceTradeEntry;
import com.calypso.tk.service.DSConnection;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

public class PdvInformesReportStyle extends TradeReportStyle {


    public final static String REPORT_DATE = "Report Date";
    public final static String MC_CONTRACT_ID = "Margin Call Contract Id";
    public final static String ACCRUAL = "Accrual";
    public static final String SECTOR_CPTY = "Sector CPTY";
    public static final String SECTOR_EMISOR = "Sector EMISOR";
    public static final String START_DATE = "Start Date";
    public static final String END_DATE = "End Date";
    public static final String SIGNO = "Signo";
    public static final String EFECTIVO_CONTRATACION = "Efecivo Contratacion";
    public static final String MARKET_VALUE = "Market Value";
    public static final String MARKET_QUOTE  = "Market Quote";
    public static final String QUANTITY_BOLETA  = "Quantity de la Boleta";
    public static final String TIPO_CONTRATO = "Tipo Contrato";
    public static final String SITUACION = "Situacion";

    private static final String SECTORCONTABLE = "SECTORCONTABLE";


    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) {
        Object columnValue = "";
        Trade trade = (Trade) row.getProperty("Trade");
        SecLending secLending = (SecLending) trade.getProduct();
        JDatetime valDateTime = ReportRow.getValuationDateTime(row);
        JDate valDate = valDateTime.getJDate(TimeZone.getDefault());

        if (secLending == null) {
            return null;
        }

        if (REPORT_DATE.equals(columnName)) {
            if (valDate == null) {
                valDate = new JDate();
            }
            return valDate;

        } else if (ACCRUAL.equals(columnName)) {
            return doCalculation(valDateTime, trade,new PricerMeasure(PricerMeasure.INDEMNITY_ACCRUAL), "PdvInformes");
        } else if (START_DATE.equals(columnName)) {
            return  secLending.getStartDate();
        } else if (END_DATE.equals(columnName)) {
            return  secLending.getEndDate();
        } else if (SIGNO.equals(columnName)) {
            return getSigno(trade);
        } else if (EFECTIVO_CONTRATACION.equals(columnName)) {
            return new Amount(secLending.getInitialMarginValue(),2);
        } else if (MARKET_VALUE.equals(columnName)) {
            return doCalculation(valDateTime, trade,new PricerMeasure(PricerMeasure.NPV_COLLAT), "PdvInformes");
        } else if (MARKET_QUOTE.equals(columnName)) {
            return getMarketQuote(row);
        } else if (SECTOR_CPTY.equals(columnName)) {
            return getSectorContable(columnName, trade, secLending);
        } else if (SECTOR_EMISOR.equals(columnName)) {
            return getSectorContable(columnName, trade, secLending);
        } else if (QUANTITY_BOLETA.equals(columnName)) {
            Object obj =  this.getColumnValue(row, "Quantity", errors);
            return formatNumber(obj);
        } else if (TIPO_CONTRATO.equals(columnName)) {
            return "BILATERAL";
        } else if (MC_CONTRACT_ID.equals(columnName)) {
            MarginCallConfigInterface<?, ?> mcConfig = SecLendingReportStyle.getSecFinanceTradeEntry(row).getMarginCallContract();
            if (mcConfig != null
                    && mcConfig.getId() > 0) {
                return  mcConfig.getId();
            }
            return null;
        } else if (SITUACION.equals(columnName)) {
            JDate maturityDate = secLending.getMaturityDate();
            if (maturityDate != null) {
                if (maturityDate.before(valDate)) {
                    return "VENCIDA";
                }
            }
            return "VIVA";
        } else  {
            columnValue = super.getColumnValue(row, columnName, errors);
        }
        return columnValue;
    }

    private Object getMarketQuote(ReportRow row) {
        Double dirtyPrice = row.getProperty((PdvInformesReport.STR_DIRTY_PRICE));
        if (dirtyPrice != null) {
            return dirtyPrice;
        }
        return null;
    }

    private String getSigno(Trade trade) {
        if (trade != null) {
            if (trade.getQuantity() > 0 )  {
                return "TOMADO";
            } else  {
                return "PRESTADO";
            }
        }
        return null;
    }

    private String getSectorContable(String columnName, Trade trade, SecLending secLending) {
        String rst = "";
        int legalEntityId = 0;
        if (columnName.equals(SECTOR_CPTY)) {
            legalEntityId = trade.getCounterParty().getLegalEntityId();
        } else {
            legalEntityId = secLending.getIssuerId();
        }
        LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), legalEntityId, legalEntityId, "ALL", SECTORCONTABLE);
        if (attr != null) {
            rst = attr.getAttributeValue();
        }
        return rst;
    }

    public static Amount doCalculation(JDatetime valDatetime, Trade trade, PricerMeasure pm, String strctgry) {
        try {
            PricingEnv officialAcc = PricingEnv.loadPE(PdvInformesReport.STR_OFFICIAL_ACCOUNTING, valDatetime );
            Pricer pricer = officialAcc.getPricerConfig().getPricerInstance(trade.getProduct());
            PricerMeasure[] pricerMeasures = new PricerMeasure[] {pm};
            pricer.price(trade, valDatetime, officialAcc, pricerMeasures);
            Double value = pm.getValue();
            return new Amount(value, 2);
        } catch (Exception ex) {
            Log.error(strctgry, ex);
        }
        return null;
    }

    public Object formatNumber(Object obj) {

        if(obj instanceof SignedAmount) {
            SignedAmount amt = (SignedAmount) obj;
            NumberFormat numberFormatter = new DecimalFormat("#0");
            numberFormatter.setGroupingUsed(false);
            return numberFormatter.format(amt.get());
        }
        return null;

    }

}


