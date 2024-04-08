package calypsox.tk.report;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.mapping.triparty.SecCode;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.*;

import java.util.Vector;

public class AnexoRVReportStyle extends TradeReportStyle {

    public static final String OFFICIAL_ACCOUNTING = "OFFICIAL_ACCOUNTING";


    public static final String ENTIDAD_PARTICIPADA = "ENTIDAD_PARTICIPADA";
    public static final String CLASE_TITULOS = "CLASE_TITULOS";
    public static final String NIF_PARTICIPADA = "NIF_PARTICIPADA";
    public static final String ACTIVIDAD = "ACTIVIDAD";
    public static final String COD_PAIS = "COD_PAIS";
    public static final String CODIGO_MONEDA = "CODIGO_MONEDA";
    public static final String COTIZACION = "COTIZACION";
    public static final String CODIGO_CARGABAL = "CODIGO_CARGABAL";
    public static final String ISIN = "ISIN";
    public static final String TIPO_CARTERA = "TIPO_CARTERA";
    public static final String CAPITAL_SOCIAL = "CAPITAL_SOCIAL";
    public static final String TITULOS_EMITIDOS = "TITULOS_EMITIDOS";
    public static final String TITULOS = "TITULOS";
    public static final String NOMINAL = "NOMINAL";
    public static final String DERECHO_VOTO_PORCENTAJE = "DERECHO_VOTO_PORCENTAJE";
    public static final String COSTE_ORIGEN = "COSTE_ORIGEN";
    public static final String COSTE_EUR = "COSTE_EUR";
    public static final String VALOR_CONTABLE = "VALOR_CONTABLE";
    public static final String VALOR_RAZONABLE = "VALOR_RAZONABLE";
    public static final String AJUSTE_POR_VALORACION = "AJUSTE_POR_VALORACION";
    public static final String IMPORTE_BRUTO = "IMPORTE_BRUTO";
    public static final String PATRIMONIO_NETO = "PATRIMONIO_NETO";
    public static final String EFECTO_FISCAL = "EFECTO_FISCAL";
    public static final String CORRECION_DE_VALOR = "CORRECION_DE_VALOR";
    public static final String CORRECION_DE_CAMBIO = "CORRECION_DE_CAMBIO";


    public static final String[] ADDITIONAL_COLUMNS = new String[]{ENTIDAD_PARTICIPADA, CLASE_TITULOS, NIF_PARTICIPADA, ACTIVIDAD, COD_PAIS, CODIGO_MONEDA, COTIZACION,
            CODIGO_CARGABAL, ISIN, TIPO_CARTERA, CAPITAL_SOCIAL, TITULOS_EMITIDOS, TITULOS, NOMINAL, DERECHO_VOTO_PORCENTAJE, COSTE_ORIGEN, COSTE_EUR, VALOR_CONTABLE,
            VALOR_RAZONABLE, AJUSTE_POR_VALORACION, IMPORTE_BRUTO, PATRIMONIO_NETO, EFECTO_FISCAL, CORRECION_DE_VALOR, CORRECION_DE_CAMBIO};


    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) {

        Trade trade = (Trade) row.getProperty("Trade");
        SecLending secLending = (SecLending) trade.getProduct();
        PricingEnv pricingEnv = ReportRow.getPricingEnv(row);
        JDatetime valDateTime = ReportRow.getValuationDateTime(row);
        JDate valDate = valDateTime.getJDate(pricingEnv.getTimeZone());

        if (columnName.equalsIgnoreCase(ENTIDAD_PARTICIPADA)) {
            return getLogicEntidadParticipada(secLending);
        } else if (columnName.equalsIgnoreCase(CLASE_TITULOS)) {
            return getLogicClaseTitulos(secLending);
        } else if (columnName.equalsIgnoreCase(NIF_PARTICIPADA)) {
            return getLogicIssuerAttribute(secLending, "TAXID");
        } else if (columnName.equalsIgnoreCase(ACTIVIDAD)) {
            return getLogicIssuerAttribute(secLending, "CNAE");
        } else if (columnName.equalsIgnoreCase(COD_PAIS)) {
            return getLogicCodPais(secLending, "cod_pais_bde");
        } else if (columnName.equalsIgnoreCase(CODIGO_MONEDA)) {
            return getLogicCodMoneda(secLending, "cod_DMN");
        } else if (columnName.equalsIgnoreCase(COTIZACION)) {
            return "S";
        } else if (columnName.equalsIgnoreCase(CODIGO_CARGABAL)) {
            return "N/A"; //Empty
        } else if (columnName.equalsIgnoreCase(ISIN)) {
            return secLending.getSecurity().getSecCode(SecCode.ISIN);
        } else if (columnName.equalsIgnoreCase(TIPO_CARTERA)) {
            return "90";
        } else if (columnName.equalsIgnoreCase(CAPITAL_SOCIAL)) {
            return "N/A"; //Empty
        } else if (columnName.equalsIgnoreCase(TITULOS_EMITIDOS)) {
            return "N/A"; //Empty
        } else if (columnName.equalsIgnoreCase(TITULOS)) {
            return secLending.computeNominal(trade, valDate);
        } else if (columnName.equalsIgnoreCase(NOMINAL)) {
            return getLogicNominal(secLending, trade, valDate);
        } else if (columnName.equalsIgnoreCase(DERECHO_VOTO_PORCENTAJE)) {
            return "N/A"; //Empty
        } else if (columnName.equalsIgnoreCase(COSTE_ORIGEN)) {
            return new Amount(secLending.getInitialMarginValue(), 2);
        } else if (columnName.equalsIgnoreCase(COSTE_EUR)) {
            Double fxQuote = row.getProperty(AnexoRVReport.FX_EUR_CCY);
            return getLogicCosteEUR(secLending, fxQuote);
        } else if (columnName.equalsIgnoreCase(VALOR_CONTABLE)) {
            return getPrice(valDateTime, trade, new PricerMeasure(PricerMeasure.NPV_COLLAT));
        } else if (columnName.equalsIgnoreCase(VALOR_RAZONABLE)) {
            return getPrice(valDateTime, trade, new PricerMeasure(PricerMeasure.NPV_COLLAT));
        } else if (columnName.equalsIgnoreCase(AJUSTE_POR_VALORACION)) {
            return "N/A"; //Empty
        } else if (columnName.equalsIgnoreCase(IMPORTE_BRUTO)) {
            return "N/A"; //Empty
        } else if (columnName.equalsIgnoreCase(PATRIMONIO_NETO)) {
            return "N/A"; //Empty
        } else if (columnName.equalsIgnoreCase(EFECTO_FISCAL)) {
            return "N/A"; //Empty
        } else if (columnName.equalsIgnoreCase(CORRECION_DE_VALOR)) {
            return "N/A"; //Empty
        } else if (columnName.equalsIgnoreCase(CORRECION_DE_CAMBIO)) {
            return "N/A"; //Empty
        }
        return super.getColumnValue(row, columnName, errors);
    }


    private String getLogicEntidadParticipada(SecLending secLending) {
        if (secLending.getSecurity() instanceof Equity) {
            Equity equity = (Equity) secLending.getSecurity();
            if (equity.getIssuer() != null) {
                return equity.getIssuer().getName();
            }
        }
        return "N/A";
    }

    private String getLogicClaseTitulos(SecLending secLending) {
        if (secLending.getSecurity() instanceof Equity) {
            Equity equity = (Equity) secLending.getSecurity();
            if (!Util.isEmpty(equity.getType()) && equity.getType().equalsIgnoreCase("PS")) {
                return "P";
            } else return "A";
        }
        return "N/A";
    }

    private String getLegalEntityAtribute(LegalEntity le, String keyword) {
        LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, le.getLegalEntityId(),
                "ALL", keyword);
        if (attr != null)
            return attr.getAttributeValue();

        return "N/A";
    }

    private String getLogicIssuerAttribute(SecLending secLending, String name) {
        if (secLending.getSecurity() instanceof Equity) {
            Equity equity = (Equity) secLending.getSecurity();
            if (equity.getIssuer() != null) {
                return getLegalEntityAtribute(equity.getIssuer(), name);
            }
        }
        return "N/A";
    }

    private String getLogicCodPais(SecLending secLending, String name) {
        Attributes attributes = BOCache.getCountry(DSConnection.getDefault(), secLending.getCountry()).getAttributes();
        if (!attributes.isEmpty()) {
            Attribute attribute = attributes.get(name);
            if (null != attribute) {
                return attribute.getValue();
            }
        }
        return "N/A";
    }

    private String getLogicCodMoneda(SecLending secLending, String name) {
        if (secLending.getSecurity() instanceof Equity) {
            Equity equity = (Equity) secLending.getSecurity();
            String cod_DMN = CurrencyUtil.getCurrencyAttribute(name, equity.getCurrency());
            if (!Util.isEmpty(cod_DMN)) {
                return cod_DMN;
            }
        }
        return "N/A";
    }

    private Double getLogicNominal(SecLending secLending, Trade trade, JDate valDate) {
        if (secLending.getSecurity() instanceof Equity) {
            Equity equity = (Equity) secLending.getSecurity();
            if (equity.getTradingSize() > 0.0) {
                return secLending.computeNominal(trade, valDate) * equity.getTradingSize();
            }
            return secLending.computeNominal(trade, valDate);
        }
        return 0.0;
    }

    private Amount getLogicCosteEUR(SecLending secLending, Double fxQuote) {
        if (null != fxQuote && fxQuote > 0.0) {
            return new Amount(secLending.getInitialMarginValue() / fxQuote, 2);
        } else {
            return new Amount(secLending.getInitialMarginValue(), 2);
        }
    }

/*    private Amount getLogicValorRazonable(Double fxQuote, Trade trade, JDatetime valDateTime) {
        Amount price = getPrice(valDateTime, trade, new PricerMeasure(PricerMeasure.NPV_COLLAT));
        if (price.get() == 0.0)
            return price;

        if (null != fxQuote && fxQuote > 0.0) {
            return new Amount(price.get() * fxQuote);
        }
        return price;
    }*/

    public static Amount getPrice(JDatetime valDatetime, Trade trade, PricerMeasure pm) {
        try {
            PricingEnv officialAcc = PricingEnv.loadPE(OFFICIAL_ACCOUNTING, valDatetime);
            Pricer pricer = officialAcc.getPricerConfig().getPricerInstance(trade.getProduct());
            PricerMeasure[] pricerMeasures = new PricerMeasure[]{pm};
            pricer.price(trade, valDatetime, officialAcc, pricerMeasures);
            Double value = pm.getValue();
            return new Amount(value, 2);
        } catch (Exception ex) {
            Log.error("AnexoRV", ex);
        }
        return new Amount(0, 1);
    }

}
