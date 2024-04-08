package calypsox.tk.report;


import java.security.InvalidParameterException;
import java.util.Optional;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.tk.core.Amount;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.SignedAmount;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Equity;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;


public class EquityConciliationTradeReportStyle extends TradeReportStyle {
    public static final String FHCONCILIA = "FHCONCILIA";
    public static final String SOURCESYSTEM = "SOURCESYSTEM";
    public static final String DIRECTION = "SOURCESYSTEM";
    public static final String INTERNA = "INTERNA";
    public static final String IDOPER = "IDOPER";
    public static final String ROOTCONTRACT = "ROOTCONTRACT";
    public static final String TITULOS = "TITULOS";
    public static final String EFECTIVO = "EFECTIVO";
    public static final String EFECTIVO_REAL = "EFECTIVO_REAL";
    public static final String PRECIO = "PRECIO";
    public static final String CORPORATE = "Corporate";
    public static final String FO_SOURCE = "FO_SOURCE";
    public static final String IS_SPOT = "IS_SPOT";
    public static final String PRODUCTO_CONCRETO = "PRODUCTO_CONCRETO";
    public static final String FECHA_VTO = "FECHA VTO";
    public static final String INDICE = "INDICE";
    public static final String UNIDAD_MEDIDA = "UNIDAD MEDIDA";
    public static final String ADD_SPREAD = "ADD SPREAD";
    public static final String MULTI_SPREAD = "MULTI SPREAD";
    public static final String MUREX_PRODUCT = "MUREX_PRODUCT";


    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {
        Trade trade = null != row && row.getProperty("Trade") instanceof Trade ? (Trade)row.getProperty("Trade") : null;
        JDatetime valuationDatetime = null != row ? (JDatetime) row.getProperty("ValuationDatetime") : null;
        JDate jDate = null != valuationDatetime ? valuationDatetime.getJDate(TimeZone.getDefault()) : null;

        if (FHCONCILIA.equalsIgnoreCase(columnId)) {
            return jDate;
        }
        else if (SOURCESYSTEM.equalsIgnoreCase(columnId)) {
            return "CALYPSO STC";
        }
        else if (DIRECTION.equalsIgnoreCase(columnId)) {
        	Equity eq = (Equity)trade.getProduct();
            if (eq.getBuySell(trade) == 1) {
            	return "Compra";
            }
            else {
            	return "Venta";
            }
        }
        else if (INTERNA.equalsIgnoreCase(columnId)) {
        	return getInternal(trade);
        }
        else if (IDOPER.equalsIgnoreCase(columnId)) {
        	return getTradeKW(trade, "Contract ID");
        }
        else if (ROOTCONTRACT.equalsIgnoreCase(columnId)) {
        	String value = getTradeKW(trade, "MurexRootContract");
        	if (isInternal(trade)) {
                if (trade.getMirrorTradeId()<trade.getLongId()){
                        value = "-" + value;
                }
            }
        	return value;
        }
        else if (TITULOS.equalsIgnoreCase(columnId)) {
        	return new EquityConciliationPositionReportStyle().formatNumeric(((SignedAmount)super.getColumnValue(row, "Quantity", errors)).get(), '.', 2); 
        }
        else if (EFECTIVO.equalsIgnoreCase(columnId)) {
        	return new EquityConciliationPositionReportStyle().formatNumeric(((SignedAmount)super.getColumnValue(row, "SettlementAmount", errors)).get() * -1.0d, '.', 2); 
        }
        else if (EFECTIVO_REAL.equalsIgnoreCase(columnId)) {
            return new EquityConciliationPositionReportStyle().formatNumeric(((SignedAmount)super.getColumnValue(row, "SettlementAmount", errors)).get(), '.', 2);
        }
        else if (PRECIO.equalsIgnoreCase(columnId)) {
        	return new EquityConciliationPositionReportStyle().formatNumeric(((Amount)super.getColumnValue(row, "Trade Price", errors)).get(), '.', 8); 
        }
        else if (CORPORATE.equalsIgnoreCase(columnId)) {
            Equity eq = (Equity)trade.getProduct();
            String equityType = eq.getSecCode("EQUITY_TYPE");
            if(!Util.isEmpty(equityType) && "CO2".equalsIgnoreCase(equityType)){
                return "EUA TRANSACTION – FORWARD";
            } else if(!Util.isEmpty(equityType) && "VCO2".equalsIgnoreCase(equityType)){
                return "VCO2";
            }
            return super.getColumnValue(row, columnId, errors);
        }
        else if (FO_SOURCE.equalsIgnoreCase(columnId)) {
            return "MUREX";
        }
        else if (IS_SPOT.equalsIgnoreCase(columnId)) {
            Equity eq = (Equity)trade.getProduct();
            String equityType = eq.getSecCode("EQUITY_TYPE");
            if(!Util.isEmpty(equityType) && ("CO2".equalsIgnoreCase(equityType) || "VCO2".equalsIgnoreCase(equityType))){
                String mxProductSubType = trade.getKeywordValue("Mx_Product_SubType");
                if(!Util.isEmpty(mxProductSubType) && "SPOT".equalsIgnoreCase(mxProductSubType)){
                    return "Y";
                }
                else if(!Util.isEmpty(mxProductSubType) && "FORWARD".equalsIgnoreCase(mxProductSubType)){
                    return "N";
                }
            }
            return "";
        }
        else if (PRODUCTO_CONCRETO.equalsIgnoreCase(columnId)) {
            return "EUA TRANSACTION – FORWARD";
        }
        else if (FECHA_VTO.equalsIgnoreCase(columnId)) {
            return super.getColumnValue(row, "Trade Settle Date", errors);
        }
        else if (INDICE.equalsIgnoreCase(columnId)) {
            Equity eq = (Equity)trade.getProduct();
            String equityType = eq.getSecCode("EQUITY_TYPE");
            if(!Util.isEmpty(equityType) && "CO2".equalsIgnoreCase(equityType)){
                String mxProductType = trade.getKeywordValue("Mx_Product_Type");
                if(!Util.isEmpty(mxProductType) && "Carbon".equalsIgnoreCase(mxProductType)){
                    return "EUA ICE";
                }
            }
            else if(!Util.isEmpty(equityType) && "VCO2".equalsIgnoreCase(equityType)){
                return "";
            }
            return "";
        }
        else if (MUREX_PRODUCT.equalsIgnoreCase(columnId)) {
            Equity eq = (Equity)trade.getProduct();
            String equityType = eq.getSecCode("EQUITY_TYPE");
            if(!Util.isEmpty(equityType) && "VCO2".equalsIgnoreCase(equityType)) {
                return "Carbon";
            }
            else{
                return trade.getKeywordValue("Mx_Product_Type");
            }
        }
        else if (UNIDAD_MEDIDA.equalsIgnoreCase(columnId)) {
            Equity eq = (Equity)trade.getProduct();
            String equityType = eq.getSecCode("EQUITY_TYPE");
            if(!Util.isEmpty(equityType) && "CO2".equalsIgnoreCase(equityType)){
                String mxProductType = trade.getKeywordValue("Mx_Product_Type");
                if(!Util.isEmpty(mxProductType) && "Carbon".equalsIgnoreCase(mxProductType)){
                    return "MT";
                }
            }
            else if(!Util.isEmpty(equityType) && "VCO2".equalsIgnoreCase(equityType)){
                return "MT";
            }
            return "";
        }
        else if (ADD_SPREAD.equalsIgnoreCase(columnId)) {
            return "";
        }
        else if (MULTI_SPREAD.equalsIgnoreCase(columnId)) {
            return "";
        }
        else {
            return super.getColumnValue(row, columnId, errors);
        }
    }

    private boolean isInternal(Trade trade) {
    	if (trade == null) {
    		return false;
    	}
        return Optional.ofNullable(trade.getMirrorBook()).isPresent();
    }

    private String getInternal(Trade trade) {
        return isInternal(trade) ? "Y": "N";
    }
    
    public String getTradeKW(Trade trade, String kw) {
        return  Optional.ofNullable(trade).map(t -> t.getKeywordValue(kw)).orElse("");
    }
}
