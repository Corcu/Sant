package calypsox.tk.report;

import calypsox.util.FormatUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.product.PerformanceSwapLeg;
import com.calypso.tk.product.PerformanceSwappableLeg;
import com.calypso.tk.refdata.Country;
import com.calypso.tk.report.CashFlowReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import java.security.InvalidParameterException;
import java.util.TimeZone;
import java.util.Vector;

public class PerSwapALMNotificationReportStyle extends CashFlowReportStyle {

    public static final String  ID_LAYOUT = "ID_LAYOUT";
    public static final String  FX_EXTRACCION = "FX_EXTRACCION";
    public static final String  FX_DATOS = "FX_DATOS";
    public static final String  ID_FUENTE = "ID_FUENTE";
    public static final String  ID_ENTIDAD = "ID_ENTIDAD";
    public static final String  ID_TIPO_PRODUCTO = "ID_TIPO_PRODUCTO";
    public static final String  CLAVE_OPERACION = "CLAVE_OPERACION";
    public static final String  COD_CLIENTE = "COD_CLIENTE";
    public static final String  DESC_CLIENTE = "DESC_CLIENTE";
    public static final String  FX_CONTRATACION = "FX_CONTRATACION";
    public static final String  FX_VALOR = "FX_VALOR";
    public static final String  FX_VENCIMIENTO = "FX_VENCIMIENTO";
    public static final String  IMP_MONTANTE_NOMINAL = "IMP_MONTANTE_NOMINAL";
    public static final String  FX_INICIO_CF = "FX_INICIO_CF";
    public static final String  FX_FIN_CF = "FX_FIN_CF";
    public static final String  FX_FIXING_CF = "FX_FIXING_CF";
    public static final String  FX_PAYMENT_CF = "FX_PAYMENT_CF";
    public static final String  IND_DIRECCION_CF = "IND_DIRECCION_CF";
    public static final String  IND_MONTANTE_CF = "IND_MONTANTE_CF";
    public static final String  IND_TIPO_COMPRAVENTA = "IND_TIPO_COMPRAVENTA";
    public static final String  PORTFOLIO_BS = "PORTFOLIO_BS";
    public static final String  MONEDA_COMPRAVENTA = "MONEDA_COMPRAVENTA";
    public static final String  CONVENCION_COMPRAVENTA = "CONVENCION_COMPRAVENTA";
    public static final String  PER_RATE = "PER_RATE";
    public static final String  PER_SPREAD = "PER_SPREAD";
    public static final String  INDEX = "INDEX";
    public static final String  ISIN = "ISIN";
    public static final String  DESC_NOMBRE_SUBYACENTE = "DESC_NOMBRE_SUBYACENTE";
    public static final String  DESC_NOMBRE_EMISOR = "DESC_NOMBRE_EMISOR";
    public static final String  COD_PAIS = "COD_PAIS";
    public static final String  NUM_CANTIDAD = "NUM_CANTIDAD";
    public static final String  IND_LIQUIDO = "IND_LIQUIDO";
    public static final String  IND_COTIZADO = "IND_COTIZADO";
    public static final String  RATING_SP = "RATING_SP";
    public static final String  RATING_MD = "RATING_MD";
    public static final String  RATING_FT = "RATING_FT";
    public static final String  PRECIO_MERCADO = "PRECIO_MERCADO";
    public static final String  VALOR_MERCADO = "VALOR_MERCADO";
    public static final String  IND_MARCA_AUTONOMIA = "IND_MARCA_AUTONOMIA";
    public static final String  MONEDA_VALOR_MERCADO = "MONEDA_VALOR_MERCADO";
    public static final String  IND_RED_TESORERIA = "IND_RED_TESORERIA";
    public static final String  IND_GRUPO = "IND_GRUPO";
    public static final String  CLAVE_COLATERAL = "CLAVE_COLATERAL";
    public static final String  IND_TIPO_LIQUIDACION = "IND_TIPO_LIQUIDACION";
    public static final String  MONEDA_VENDIDAED = "MONEDA_VENDIDAED";
    public static final String  MONEDA_COMPRADAED = "MONEDA_COMPRADAED";
    public static final String  IMP_MONTANTE_MONEDA_VENDIDAED = "IMP_MONTANTE_MONEDA_VENDIDAED";
    public static final String  IMP_MONTANTE_MONEDA_COMPRADAED = "IMP_MONTANTE_MONEDA_COMPRADAED";
    public static final String  INTERCAMBIO = "INTERCAMBIO";
    public static final String  NPV_MONEDA_COMPRADA = "NPV_MONEDA_COMPRADA";
    public static final String  NPV_MONEDA_VENDIDA = "NPV_MONEDA_VENDIDA";
    public static final String  PORTFOLIO = "PORTFOLIO";
    public static final String  PERIODICA = "PERIODICA";
    public static final String  COD_ENTIDAD = "COD_ENTIDAD";

    private final String DATE_FORMAT = "ddMMyyyy";

    private Double interestAmt = 0.0;

    public static final String[] ADDITIONAL_COLUMNS = {ID_LAYOUT,FX_EXTRACCION,FX_DATOS,ID_FUENTE,ID_ENTIDAD,
            ID_TIPO_PRODUCTO,CLAVE_OPERACION,COD_CLIENTE,DESC_CLIENTE,FX_CONTRATACION,FX_VALOR,FX_VENCIMIENTO,
            IMP_MONTANTE_NOMINAL,FX_INICIO_CF,FX_FIN_CF,FX_FIXING_CF,FX_PAYMENT_CF,IND_DIRECCION_CF,
            IND_MONTANTE_CF,IND_TIPO_COMPRAVENTA,PORTFOLIO_BS,MONEDA_COMPRAVENTA,CONVENCION_COMPRAVENTA,
            PER_RATE,PER_SPREAD,INDEX,ISIN,DESC_NOMBRE_SUBYACENTE,DESC_NOMBRE_EMISOR,COD_PAIS,NUM_CANTIDAD,
            IND_LIQUIDO,IND_COTIZADO,RATING_SP,RATING_MD,RATING_FT,PRECIO_MERCADO,VALOR_MERCADO,IND_MARCA_AUTONOMIA,
            MONEDA_VALOR_MERCADO,IND_RED_TESORERIA,IND_GRUPO,CLAVE_COLATERAL,IND_TIPO_LIQUIDACION,MONEDA_VENDIDAED,
            MONEDA_COMPRADAED,IMP_MONTANTE_MONEDA_VENDIDAED,IMP_MONTANTE_MONEDA_COMPRADAED,INTERCAMBIO,
            NPV_MONEDA_COMPRADA,NPV_MONEDA_VENDIDA,PORTFOLIO,PERIODICA,COD_ENTIDAD,};


    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {

        final JDatetime valuationDatetime = (JDatetime) row.getProperty("ValuationDatetime");
        CashFlow cf = (CashFlow)row.getProperty("CashFlow");

        if(ID_LAYOUT.equalsIgnoreCase(columnId)){
            return "10";
        } else if(FX_EXTRACCION.equalsIgnoreCase(columnId)){
            return FormatUtil.formatDate(valuationDatetime.getJDate(TimeZone.getDefault()),DATE_FORMAT);
        } else if(FX_DATOS.equalsIgnoreCase(columnId)){
            return FormatUtil.formatDate(valuationDatetime.getJDate(TimeZone.getDefault()),DATE_FORMAT);
        } else if(ID_FUENTE.equalsIgnoreCase(columnId)){
            return "015";
        } else if(ID_ENTIDAD.equalsIgnoreCase(columnId)){
            return "1";
        } else if(ID_TIPO_PRODUCTO.equalsIgnoreCase(columnId)){
            return "52";
        } else if(CLAVE_OPERACION.equalsIgnoreCase(columnId)){
            return "";
        } else if(COD_CLIENTE.equalsIgnoreCase(columnId)){
            return "";
        } else if(DESC_CLIENTE.equalsIgnoreCase(columnId)){
            final Object columnValue = super.getColumnValue(row, "CounterParty.Full Name", errors);
            return FormatUtil.splitString((String) columnValue, 30);
        } else if(FX_CONTRATACION.equalsIgnoreCase(columnId)){
            return "";
        } else if(FX_VALOR.equalsIgnoreCase(columnId)){
            return "";
        } else if(FX_VENCIMIENTO.equalsIgnoreCase(columnId)){
            return "";
        } else if(IMP_MONTANTE_NOMINAL.equalsIgnoreCase(columnId)){
            return "";
        } else if(FX_INICIO_CF.equalsIgnoreCase(columnId)){
            return "";
        } else if(FX_FIN_CF.equalsIgnoreCase(columnId)){
            return "";
        } else if(FX_FIXING_CF.equalsIgnoreCase(columnId)){
            return "";
        } else if(FX_PAYMENT_CF.equalsIgnoreCase(columnId)){
            return  "";
        } else if(IND_DIRECCION_CF.equalsIgnoreCase(columnId)){
            final Integer cashFlow_sub_id = (Integer)super.getColumnValue(row, "CashFlow Sub Id", errors);
            final Trade trade = (Trade) row.getProperty("Trade");
            String primaryLegDirection = loadPrimaryLegDirection(trade);
            if(1 == cashFlow_sub_id){
               return primaryLegDirection;
            }else if(2 == cashFlow_sub_id){
                if("P".equalsIgnoreCase(primaryLegDirection)){
                    return "R";
                }else{
                    return "P";
                }
            }
            return "";
        } else if(IND_MONTANTE_CF.equalsIgnoreCase(columnId)){
            return "";
        } else if(IND_TIPO_COMPRAVENTA.equalsIgnoreCase(columnId)){
            final Boolean fixed = (Boolean)super.getColumnValue(row, "Fixed", errors);
            if(null!=fixed){
                if(fixed){
                    return "F";
                }else{
                    return "V";
                }
            }
            return "";
        } else if(PORTFOLIO_BS.equalsIgnoreCase(columnId)){
            return "";
        } else if(MONEDA_COMPRAVENTA.equalsIgnoreCase(columnId)){
            return cf.getCurrency();
        } else if(CONVENCION_COMPRAVENTA.equalsIgnoreCase(columnId)){
            final Object security_name = super.getColumnValue(row, "FX Rate", errors);
            return security_name;
        } else if(PER_RATE.equalsIgnoreCase(columnId)){
            return "";
        } else if(PER_SPREAD.equalsIgnoreCase(columnId)){
            return "";
        } else if(INDEX.equalsIgnoreCase(columnId)){
            return "";
        } else if(ISIN.equalsIgnoreCase(columnId)){
            return "";
        } else if(DESC_NOMBRE_SUBYACENTE.equalsIgnoreCase(columnId)){
            final Object security_name = super.getColumnValue(row, "Underlying Security Name", errors);
            return FormatUtil.splitString(security_name, 30);
        } else if(DESC_NOMBRE_EMISOR.equalsIgnoreCase(columnId)){
            return "";
        } else if(COD_PAIS.equalsIgnoreCase(columnId)){
            final Trade trade = (Trade) row.getProperty("Trade");
            return getCountryCode(trade);
        } else if(NUM_CANTIDAD.equalsIgnoreCase(columnId)){
            return "";
        } else if(IND_LIQUIDO.equalsIgnoreCase(columnId)){
            return "";
        } else if(IND_COTIZADO.equalsIgnoreCase(columnId)){
            return "";
        } else if(RATING_SP.equalsIgnoreCase(columnId)){
            return "";
        } else if(RATING_MD.equalsIgnoreCase(columnId)){
            return "";
        } else if(RATING_FT.equalsIgnoreCase(columnId)){
            return "";
        } else if(PRECIO_MERCADO.equalsIgnoreCase(columnId)){
            return "";
        } else if(VALOR_MERCADO.equalsIgnoreCase(columnId)){
            return "";
        } else if(IND_MARCA_AUTONOMIA.equalsIgnoreCase(columnId)){
            return "";
        } else if(MONEDA_VALOR_MERCADO.equalsIgnoreCase(columnId)){
            return "";
        } else if(IND_RED_TESORERIA.equalsIgnoreCase(columnId)){
            return "T";
        } else if(IND_GRUPO.equalsIgnoreCase(columnId)){
            return "N";
        } else if(CLAVE_COLATERAL.equalsIgnoreCase(columnId)){
            return "N/A";
        } else if(IND_TIPO_LIQUIDACION.equalsIgnoreCase(columnId)){
            return "001";
        } else if(MONEDA_VENDIDAED.equalsIgnoreCase(columnId)){
            return "";
        } else if(MONEDA_COMPRADAED.equalsIgnoreCase(columnId)){
            return "";
        } else if(IMP_MONTANTE_MONEDA_VENDIDAED.equalsIgnoreCase(columnId)){
            return "";
        } else if(IMP_MONTANTE_MONEDA_COMPRADAED.equalsIgnoreCase(columnId)){
            return "";
        } else if(INTERCAMBIO.equalsIgnoreCase(columnId)){
            return "";
        } else if(NPV_MONEDA_COMPRADA.equalsIgnoreCase(columnId)){
            return "";
        } else if(NPV_MONEDA_VENDIDA.equalsIgnoreCase(columnId)){
            return "";
        } else if(PORTFOLIO.equalsIgnoreCase(columnId)){
            final Object book = super.getColumnValue(row, "Book", errors);
            return book;
        } else if(PERIODICA.equalsIgnoreCase(columnId)){
            return "";
        } else if(COD_ENTIDAD.equalsIgnoreCase(columnId)){
            return "0049";
        }
        Object columnValue = super.getColumnValue(row, columnId, errors);
        if(columnValue instanceof JDate){
            columnValue = FormatUtil.formatDate((JDate)columnValue,DATE_FORMAT);
        }else if (columnValue instanceof Amount){
            double value = ((Amount) columnValue).get();
            if("-Infinity".equalsIgnoreCase(String.valueOf(value)) || Double.isNaN(value) || Double.isInfinite(value)){
                columnValue = FormatUtil.formatAmount(0.0);
            }else{
                columnValue = FormatUtil.formatAmount(value);
            }
        }else if (columnValue instanceof Rate){
            String rate = ((Rate) columnValue).toString();
            columnValue = FormatUtil.formatRate(!Util.isEmpty(rate) ? rate : "000000000000000.00");
        }else if(columnValue instanceof DisplayDatetime){
            final JDate jDate = ((DisplayDatetime) columnValue).getJDate(TimeZone.getDefault());
            columnValue = FormatUtil.formatDate(jDate,DATE_FORMAT);
        }else if (columnValue==null){
            columnValue = formEmptyColum(columnId);
        }

        return columnValue;
    }


    /**
     * @param trade
     * @return
     */
    private String getCountryCode(Trade trade){
        if(null!=trade && null!=trade.getProduct() && trade.getProduct() instanceof PerformanceSwap){
            if(((PerformanceSwap) trade.getProduct()).getReferenceProduct() instanceof Bond){
                final String country = ((Bond) ((PerformanceSwap) trade.getProduct()).getReferenceProduct()).getCountry();
                return getISOCode((String) country);
            }
        }
        return "";
    }

    private String getISOCode(String countryName){
        try {
            final Country country = BOCache.getCountry(DSConnection.getDefault(), countryName);
            if(null!=country){
                return country.getISOCode();
            }
        } catch (Exception e) {
            Log.error(this, "Error Extractin ISO Country from " + countryName + ": ", e);
        }
        return "";
    }

    private String loadPrimaryLegDirection(Trade trade){
        String primayLegDesc = "";
        if(null!=trade && null!=trade.getProduct() && trade.getProduct() instanceof PerformanceSwap){
            final PerformanceSwap product = (PerformanceSwap) trade.getProduct();
            PerformanceSwappableLeg primaryLeg = product.getPrimaryLeg();
            PerformanceSwapLeg primLeg = null;
            boolean perfLeg = false;

            if (primaryLeg instanceof PerformanceSwapLeg) {
                perfLeg = true;
                primLeg = (PerformanceSwapLeg)primaryLeg;
            }
            if (perfLeg) {
                if (primLeg.getNotional() < 0.0D) {
                    primayLegDesc = "P";
                } else if (primLeg.getNotional() == 0.0D && (trade.isAllocationParent() || trade.isAllocationChild()) && trade.getQuantity() < 0.0D) {
                    primayLegDesc = "P";
                } else {
                    primayLegDesc = "R";
                }
            }
        }
        return primayLegDesc;
    }


    private String formEmptyColum(String columnId){
        if("Cash Flow Notional".equalsIgnoreCase(columnId)
                || "Rate".equalsIgnoreCase(columnId)
                || "Spread".equalsIgnoreCase(columnId)
                || "Cash Flow Amount".equalsIgnoreCase(columnId)){
            return FormatUtil.formatAmount(0.0);
        }
        return "";
    }

}
