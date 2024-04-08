package calypsox.tk.report;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.*;
import com.calypso.tk.refdata.Country;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.service.DSConnection;
import java.util.*;

public class FI105_RepoReportStyle extends TradeReportStyle {

    public static final String OPERACION = "OPERACION";
    public static final String PRODUCTO = "PRODUCTO";
    public static final String CONTRA = "CONTRA";
    public static final String VAL = "VAL";
    public static final String PAIS = "PAIS";
    public static final String GRUPO_PAIS = "GRUPO_PAIS";
    public static final String SECTOR = "SECTOR";
    public static final String SIGNO = "SIGNO";
    public static final String ACTIVO_INI = "ACTIVO_INI";
    public static final String PASIVO_INI = "PASIVO_INI";
    public static final String OPE_CONTRATADA = "OPE_CONTRATADA";
    public static final String LIQ_FIN = "LIQ_FIN";
    public static final String LIQ_INT = "LIQ_INT";
    public static final String VARIACIONES = "VARIACIONES";
    public static final String ACTIVO_FIN = "ACTIVO_FIN";
    public static final String PASIVO_FIN = "PASIVO_FIN";
    public static final String OPERACION_ESTADO = "OPERACION_ESTADO";

    private static final String ATTR_CLASIFICACION_SECTOR = "CLASIFICACION_SECTOR";

    @Override
    public TreeList getTreeList() {
        final TreeList treeList = super.getTreeList();
        treeList.add(OPERACION);
        treeList.add(PRODUCTO);
        treeList.add(CONTRA);
        treeList.add(VAL);
        treeList.add(PAIS);
        treeList.add(GRUPO_PAIS);
        treeList.add(SECTOR);
        treeList.add(SIGNO);
        treeList.add(ACTIVO_INI);
        treeList.add(PASIVO_INI);
        treeList.add(OPE_CONTRATADA);
        treeList.add(LIQ_FIN);
        treeList.add(LIQ_INT);
        treeList.add(VARIACIONES);
        treeList.add(ACTIVO_FIN);
        treeList.add(PASIVO_FIN);
        treeList.add(OPERACION_ESTADO);
        return treeList;
    }

    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) {
        Trade trade = (Trade) row.getProperty("Trade");
        Repo repo = (Repo) trade.getProduct();
        PricingEnv pricingEnv = ReportRow.getPricingEnv(row);
        JDatetime valDateTime = ReportRow.getValuationDateTime(row);
        JDate valDate = valDateTime.getJDate(pricingEnv.getTimeZone());
        FI105RepoBean bean = row.getProperty(FI105RepoBean.class.getSimpleName());

        if (null != repo && bean != null) {
            if (columnName.equalsIgnoreCase(OPERACION)) {
                return bean.getOperacion();
            } else if (columnName.equalsIgnoreCase(PRODUCTO)) {
                return "REPO";
            } else if (columnName.equalsIgnoreCase(CONTRA)) {
                return bean.getFechaContratacion();
            } else if (columnName.equalsIgnoreCase(VAL)) {
                return bean.getFechaValor();
            } else if (columnName.equalsIgnoreCase(PAIS)) {
                return trade.getCounterParty().getCountry();
            } else if (columnName.equalsIgnoreCase(GRUPO_PAIS)) {
                String countryName = trade.getCounterParty().getCountry();
                Country country = BOCache.getCountry(DSConnection.getDefault(), countryName);
                String sector = "";
                if (country != null) {
                    Attribute attr = country.getAttributes().getAttribute("CLASIFICACION_CONTABLE");
                    if (attr != null) {
                        sector = attr.getValue();
                    }
                }
                return sector;
            } else if (columnName.equalsIgnoreCase(SECTOR)) {
                LegalEntity cpty = trade.getCounterParty();
                LegalEntityAttribute leAttr =
                           BOCache.getLegalEntityAttribute(DSConnection.getDefault(),
                                   cpty.getId(),  cpty.getId(), "ALL", ATTR_CLASIFICACION_SECTOR);
                if (leAttr != null) {
                    return leAttr.getAttributeValue();
                }
            } else if (columnName.equalsIgnoreCase(SIGNO)) {
                return bean.getSigno();

            } else if (columnName.equalsIgnoreCase(ACTIVO_INI)) {
                return toAmount(bean.getActivoIni());
            } else if (columnName.equalsIgnoreCase(PASIVO_INI)) {
                return toAmount(bean.getPasivoIni());
            } else if (columnName.equalsIgnoreCase(OPE_CONTRATADA)) {
                return toAmount(bean.getOperacionContratada());
            } else if (columnName.equalsIgnoreCase(LIQ_FIN)) {
                return "";
            } else if (columnName.equalsIgnoreCase(LIQ_INT)) {
                return "";
            } else if (columnName.equalsIgnoreCase(VARIACIONES)) {
                return toAmount(bean.getVariaciones());
            } else if (columnName.equalsIgnoreCase(ACTIVO_FIN)) {
                return toAmount(bean.getActivoFin());
            } else if (columnName.equalsIgnoreCase(PASIVO_FIN)) {
                return toAmount(bean.getPasivoFin());
            } else if (columnName.equalsIgnoreCase(OPERACION_ESTADO)) {
                if (trade.getStatus().equals(Status.S_CANCELED)) {
                    return "Anulada";
                }
                return "Aceptada";
            }
        }
        return super.getColumnValue(row, columnName, errors);
    }

    private SignedAmount toAmount(Double dbl) {
        if (dbl!= null) {
            return new SignedAmount(dbl, 2);
        }
        return null;
    }
}
