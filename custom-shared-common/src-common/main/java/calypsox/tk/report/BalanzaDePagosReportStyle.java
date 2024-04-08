package calypsox.tk.report;

import calypsox.tk.util.LegalEntityAttributesCache;
import com.calypso.tk.bo.BOCache;

import com.calypso.tk.core.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.Security;
import com.calypso.tk.refdata.LegalEntityAttribute;

import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;
import com.calypso.tk.service.DSConnection;


import java.security.InvalidParameterException;
import java.util.Vector;

public class BalanzaDePagosReportStyle extends ReportStyle {

    public static final String REPORT_DATE = "Report Date";
    public static final String ISIN = "ISIN";
    public static final String CURRENCY = "CURRENCY";
    public static final String TIPO_PRODUCTO = "TIPO PRODUCTO";
    public static final String CPTY_GLCS = "CONTRAPARTIDA GLCS";
    public static final String CPTY_NOMBRE = "CONTRAPARTIDA NOMBRE";
    public static final String TIPO_MOVIMENTO = "TIPO MOVIMIENTO";
    public static final String DESCRIPCION_ISIN = "DESCRIPCION DEL ISIN";
    public static final String DECLARACION = "DECLARACION";
    public static final String EPIGRAFE = "EPIGRAFE";
    public static final String INSTRUMENTO = "INSTRUMENTO";
    public static final String EMISOR_GLCS = "EMISOR GLCS";
    public static final String EMISOR_NOMBRE = "EMISOR NOMBRE";
    public static final String EMISOR_NIF = "EMISOR NIF";
    public static final String EMISOR_JMIN = "EMISOR JMINORISTA";
    public static final String TRESPASOS = "TRASPASOS";
    public static final String CUPONES = "CUPONES";

    public static final String SALDO_INICIAL_NOMINAL = "Saldo Inicial Nominal";
    public static final String SALDO_INICIAL_VALOR = "Saldo Inicial Valoracion";
    public static final String ENTRADAS_NOMINAL = "Entradas Nominal";
    public static final String ENTRADAS_VALOR = "Entradas Valor";
    public static final String SALIDAS_NOMINAL = "Salidas Nominal";
    public static final String SALIDAS_VALOR = "Salidas Valor";
    public static final String SALDO_FINAL_NOMINAL = "Saldo Final Nominal";
    public static final String SALDO_FINAL_VALOR = "Saldo Final Valoracion";
    public static final String PRECIO_INICIAL = "Precio Inicial";
    public static final String PRECIO_FINAL = "Precio Final";
    public static final String POSITION_DATE = "Position Date";
    public static final String POSITION_DETAIL = "Position Detail";
    public static final String POSITION_VALUATION_DATE = "Position Valuation Date";
    private static final String J_MINORISTA = "J_MINORISTA";

    private static Amount ZERO_AMOUNT = new Amount(0,2);

    @Override
    @SuppressWarnings("rawtypes")
    public Object getColumnValue(ReportRow row, String columnId, Vector errors)
            throws InvalidParameterException {

        BalanzaDePagosItem item =  row.getProperty(ReportRow.DEFAULT);
        if (item == null) {
            return null;
        }
        if (REPORT_DATE.equals(columnId)) {
            JDate valDate = JDate.valueOf(ReportRow.getValuationDateTime(row));
            return valDate;
        } else if (POSITION_DATE.equals(columnId)) {
            return item.getPositionDate();
        } else if (POSITION_VALUATION_DATE.equals(columnId)) {
            return row.getProperty(BalanzaDePagosReport.PROPERTY_POSITION_VALUATION_DATE);

        } else if (POSITION_DETAIL.equals(columnId)) {
            return item.getPositionDetail();

        } else if (ISIN.equals(columnId)) {
            Product product = item.getProduct();
            if (product != null) {
                return product.getSecCode("ISIN");
            }
        } else if (CURRENCY.equals(columnId)) {
            return item.getCurrency();
        } else  if (TIPO_PRODUCTO.equals(columnId)) {
            return item.getInventoryType();
        } else if (DESCRIPCION_ISIN.equals(columnId)) {
            Product product = item.getProduct();
            return product.getQuoteName();
        } else if (CPTY_GLCS.equals(columnId)) {
            LegalEntity le = BOCache.getLegalEntity(DSConnection.getDefault(), item.getEntityId());
            if (le!= null) {
                return le.getCode();
            }
        } else if (CPTY_NOMBRE.equals(columnId)) {
            LegalEntity le = BOCache.getLegalEntity(DSConnection.getDefault(), item.getEntityId());
            if (le!= null) {
                return le.getName();
            }
        } else if (DECLARACION.equals(columnId)) {
            return "1B";
        } else  if (EPIGRAFE.equals(columnId)) {
            return "5100";
        } else  if (INSTRUMENTO.equals(columnId)) {
            return getInstrumento(item.getProduct());
        } else  if (EMISOR_GLCS.equals(columnId)) {
            Product product = item.getProduct();
            if (product instanceof Security) {
                Security security = (Security) product;
                security.getIssuerId();
                LegalEntity le = BOCache.getLegalEntity(DSConnection.getDefault(), security.getIssuerId());
                if (le!= null) {
                    return le.getCode();
                }
            }
        } else  if (EMISOR_NOMBRE.equals(columnId)) {
            Product product = item.getProduct();
            if (product instanceof Security) {
                Security security = (Security) product;
                security.getIssuerId();
                LegalEntity le = BOCache.getLegalEntity(DSConnection.getDefault(), security.getIssuerId());
                if (le!= null) {
                    return le.getName();
                }
            }
        } else  if (EMISOR_NIF.equals(columnId)) {
            Product product = item.getProduct();
            if (product instanceof Security) {
                Security security = (Security) product;
                LegalEntityAttribute attr =  LegalEntityAttributesCache.getInstance().getAttribute(security.getIssuerId(), security.getIssuerId(),"ALL", "TAXID");
                if (null != attr) {
                    return attr.getAttributeValue();
                }
            }
            return "";
        } else  if (EMISOR_JMIN.equals(columnId)) {
            Product product = item.getProduct();
            if (product instanceof Security) {
                Security security = (Security) product;
                LegalEntityAttribute attr =  LegalEntityAttributesCache.getInstance().getAttribute(security.getIssuerId(), security.getIssuerId(),"ALL", J_MINORISTA);
                if (null != attr) {
                    return attr.getAttributeValue();
                }
            }
            return "";
        } else  if (TRESPASOS.equals(columnId)) {
            return 0.0d;
        } else  if (CUPONES.equals(columnId)) {
            return 0.00d;
        } else  if (SALDO_INICIAL_NOMINAL.equals(columnId)) {
            if (item.getPositionType().equals(BalanzaDePagosReport.INITIAL)) {
                Double d = item.getNominal();
                if (d == null) {
                    return 0.00d;
                }
                return new SignedAmount(d,2);
            }
            return 0.0d;
        } else  if (SALDO_INICIAL_VALOR.equals(columnId)) {
           if (item.getPositionType().equals(BalanzaDePagosReport.INITIAL)) {
                Double d = item.getMarketValue();
               if (d == null) {
                   return 0.00d;
               }
               return new SignedAmount(d,2);
            }
            return 0.0d;
        } else  if (SALDO_FINAL_NOMINAL.equals(columnId)) {
            if (item.getPositionType().equals(BalanzaDePagosReport.FINAL)) {
                Double d = item.getNominal();
                if (d == null) {
                    return 0.00d;
                }
                return new SignedAmount(d,2);
            }
            return 0.0d;
        } else  if (SALDO_FINAL_VALOR.equals(columnId)) {
            if (item.getPositionType().equals(BalanzaDePagosReport.FINAL)) {
                Double d = item.getMarketValue();
                if (d == null) {
                    return 0.00d;
                }
                return new SignedAmount(d,2);
            }
            return 0.0d;
        } else  if (ENTRADAS_NOMINAL.equals(columnId)) {

            if (item.getPositionType().equals(BalanzaDePagosReport.MOV_IN)) {
                Double d = item.getNominal();
                if (d == null) {
                    return 0.00d;
                }
                return new SignedAmount(d,2);
            }
            return 0.0d;

        } else  if (ENTRADAS_VALOR.equals(columnId)) {
            if (item.getPositionType().equals(BalanzaDePagosReport.MOV_IN)) {
                Double d = item.getMarketValue();
                if (d == null) {
                    return 0.00d;
                }
                return new SignedAmount(d,2);

            }
            return 0.0d;
        } else  if (SALIDAS_NOMINAL.equals(columnId)) {

            if (item.getPositionType().equals(BalanzaDePagosReport.MOV_OUT)) {
                Double d = item.getNominal();
                if (d == null) {
                    return 0.00d;
                }
                return new SignedAmount(d,2);

            }
        } else  if (SALIDAS_VALOR.equals(columnId)) {
            if (item.getPositionType().equals(BalanzaDePagosReport.MOV_OUT)) {
                Double d = item.getMarketValue();
                if (d == null) {
                    return 0.00d;
                }
                return new SignedAmount(d,2);

            }
            return 0.0d;
        } else  if (TIPO_MOVIMENTO.equals(columnId)) {
            return item.getPositionType();

        } else  if (PRECIO_INICIAL.equals(columnId)) {
            DisplayValue dv = row.getProperty(BalanzaDePagosReport.PROPERTY_START_PRICE);
            if (dv != null) {
                return dv;
            }
            return 0.00d;
        } else  if (PRECIO_FINAL.equals(columnId)) {
            DisplayValue dv = row.getProperty(BalanzaDePagosReport.PROPERTY_END_PRICE);
            if (dv != null) {
                return dv;
            }
            return 0.00d;
        }
        return null;
    }

    private String getInstrumento(Product product) {
        if (product != null) {
            if (product instanceof Equity) {
                return "1";
            }
            Bond bond = (Bond) product;
            String issueType = bond.getSecCode("ISSUE_TYPE");
            if ("LT".equals(issueType)) {
                return "4";
            } else if (null != bond.getEndDate() && null != bond.getIssueDate()) {
                long diff = bond.getEndDate().getDate().getTime() - bond.getIssueDate().getDate().getTime();
                if (diff>365) {
                    return "3";
                } else {
                    return "4";
                }
            }
        }
        return null;
    }


}
