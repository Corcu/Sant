package calypsox.tk.report;

import calypsox.tk.report.util.UtilReport;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.apps.reporting.ReportUtil;
import com.calypso.apps.util.CalypsoTreeNode;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOACC_SEC_TRAN_STATEMENTMessageHandler;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.refdata.FdnHoliday;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

import javax.ejb.Local;
import java.rmi.RemoteException;
import java.util.*;

public class FI105_BRSReportStyle extends TradeReportStyle {
    private static final long serialVersionUID = -1690776136700779143L;

    private static final String SECTOR_CONTABLE = "SECTORCONTABLE";
    public static final String PRODUCT_TYPE_VALUE = "BRS";
    public static final String CENTROALCO_VALUE = "1999";
    public static final String PRICING_ENV = "DirtyPrice";
    public static final String PLMARK_NAME = "NPV";

    public static final String CLOSING_DATE = "Fecha Cierre";
    public static final String PRODUCTO = "Producto";
    public static final String INSTRUMENT_TYPE = "Instrument Type";
    public static final String SIGNO = "Signo";
    public static final String CPTY_TESOR = "Contrapartida TESOR";
    public static final String CPTY_GLCS = "Contrapartida GLCS";
    public static final String COUNTRY = "Pais";
    public static final String SECTOR = "Sector";
    public static final String COUNTRY_BDE = "Pais BdE";
    public static final String SECTOR_BDE = "Sector BdE";
    public static final String VALUE_DATE = "Value Date";
    public static final String MATURITY_DATE = "Maturity Date";
    public static final String ACTIVO_INICIAL = "Activo Inicial";
    public static final String PASIVO_INICIAL = "Pasivo Inicial";
    public static final String OPERACIONES_CONTRATADAS = "Operaciones Contratadas";
    public static final String LIQUIDACIONES_FINALES = "Liquidaciones Finales";
    public static final String LIQUIDACIONES_INTERMEDIAS = "Liquidaciones Intermedias";
    public static final String VARIACIONES_VALOR_RAZON = "Variaciones Valor Razon";
    public static final String ACTIVO_FINAL = "Activo Final";
    public static final String PASIVO_FINAL = "Pasivo Final";
    public static final String CENTROALCO = "CentroALCO";

    public static final String[] ADDITIONAL_COLUMNS = new String[]{TradeReportStyle.TRADE_ID, CLOSING_DATE, PRODUCTO,
            INSTRUMENT_TYPE, SIGNO, CPTY_TESOR, CPTY_GLCS, COUNTRY, SECTOR, COUNTRY_BDE, SECTOR_BDE,
            TradeReportStyle.TRADE_DATE, VALUE_DATE, MATURITY_DATE, ACTIVO_INICIAL, PASIVO_INICIAL, OPERACIONES_CONTRATADAS,
            LIQUIDACIONES_FINALES, LIQUIDACIONES_INTERMEDIAS, VARIACIONES_VALOR_RAZON, ACTIVO_FINAL, PASIVO_FINAL, CENTROALCO};

    public FI105_BRSReportStyle() {
    }// 23

    public Object getColumnValue(ReportRow row, String columnName, Vector errors) {
        Trade trade = (Trade) row.getProperty(ReportRow.TRADE);// 61
        JDatetime date = (JDatetime) row.getProperty("ValuationDatetime");
        JDate valdate = date.getJDate(TimeZone.getDefault());

        if (trade != null) {
            if (trade.getProduct() instanceof PerformanceSwap) {

                if (columnName.equalsIgnoreCase(TradeReportStyle.TRADE_ID)) {
                    return trade.getLongId();
                } else if (columnName.equalsIgnoreCase(CLOSING_DATE)) {
                    return getClosingDate(valdate);
                } else if (columnName.equalsIgnoreCase(PRODUCTO)) {
                    return PRODUCT_TYPE_VALUE;
                } else if (columnName.equalsIgnoreCase(INSTRUMENT_TYPE)) {
                    return "";
                } else if (columnName.equalsIgnoreCase(SIGNO)) {
                    return "";
                } else if (columnName.equalsIgnoreCase(CPTY_TESOR)) {
                    return "";
                } else if (columnName.equalsIgnoreCase(CPTY_GLCS)) {
                    return trade.getCounterParty().getCode();
                } else if (columnName.equalsIgnoreCase(COUNTRY)) {
                    return getCptyCountry(trade);
                } else if (columnName.equalsIgnoreCase(SECTOR)) {
                    return getSector(trade);
                } else if (columnName.equalsIgnoreCase(COUNTRY_BDE)) {
                    return "";
                } else if (columnName.equalsIgnoreCase(SECTOR_BDE)) {
                    return "";
                } else if (columnName.equalsIgnoreCase(TradeReportStyle.TRADE_DATE)) {
                    return !Util.isEmpty(trade.getTradeDate().toString()) ? Util.dateToString(trade.getTradeDate()) : "";
                } else if (columnName.equalsIgnoreCase(VALUE_DATE)) {
                    return !Util.isEmpty(trade.getSettleDate().toString()) ? Util.dateToString(trade.getSettleDate()) : "";
                } else if (columnName.equalsIgnoreCase(MATURITY_DATE)) {
                    return getMaturityDate(trade);
                } else if (columnName.equalsIgnoreCase(ACTIVO_INICIAL)) {
                    return Util.numberToString(getActivoInicial(trade, valdate), Locale.ENGLISH);
                } else if (columnName.equalsIgnoreCase(PASIVO_INICIAL)) {
                    return Util.numberToString(getPasivoInicial(trade, valdate), Locale.ENGLISH);
                } else if (columnName.equalsIgnoreCase(OPERACIONES_CONTRATADAS)) {
                    return Util.numberToString(getOperacionesContratadas(trade, valdate), Locale.ENGLISH);
                } else if (columnName.equalsIgnoreCase(LIQUIDACIONES_FINALES)) {
                    return Util.numberToString(getLiquidacionesFinales(trade, valdate), Locale.ENGLISH);
                } else if (columnName.equalsIgnoreCase(LIQUIDACIONES_INTERMEDIAS)) {
                    return Util.numberToString(getLiquidacionesIntermedias(trade, valdate), Locale.ENGLISH);
                } else if (columnName.equalsIgnoreCase(VARIACIONES_VALOR_RAZON)) {
                    return Util.numberToString(getVariacionesValorRazon(trade, valdate), Locale.ENGLISH);
                } else if (columnName.equalsIgnoreCase(ACTIVO_FINAL)) {
                    return Util.numberToString(getActivoFinal(trade, valdate), Locale.ENGLISH);
                } else if (columnName.equalsIgnoreCase(PASIVO_FINAL)) {
                    return Util.numberToString(getPasivoFinal(trade, valdate), Locale.ENGLISH);
                } else if (columnName.equalsIgnoreCase(CENTROALCO)) {
                    return CENTROALCO_VALUE;
                }
            }
        }
        return super.getColumnValue(row, columnName, errors);// 62
    }

    public TreeList getTreeList() {
        TreeList treeList = super.getTreeList();// 68
        CalypsoTreeNode dummyNode = new CalypsoTreeNode("Dummy");// 69
        for (String column : ADDITIONAL_COLUMNS) {
            treeList.add(dummyNode, column);// 70
        }
        return treeList;// 74
    }

    private String getClosingDate(JDate valdate) {
        // Ultimo día hábil del mes actual
        return Util.dateToString(getLastDayOfMonth(0, valdate));
    }

    private String getCptyCountry(Trade trade) {
        String country = trade.getCounterParty().getCountry();// 77
        String isoCode = BOCache.getCountry(DSConnection.getDefault(), country).getISOCode();// 78
        return !Util.isEmpty(isoCode) ? isoCode : "";// 79 82
    }

    private String getSector(Trade trade) {
        Collection attributes = trade.getCounterParty().getLegalEntityAttributes();// 86

        if (null == attributes) return "";

        for (Object object : attributes) {
            LegalEntityAttribute attribute = (LegalEntityAttribute) object;
            if (SECTOR_CONTABLE.equalsIgnoreCase(attribute.getAttributeType())) {
                return attribute.getAttributeValue();
            }
        }
        return "";// 91
    }

    private double getOperacionesContratadas(Trade trade, JDate valdate) {
        if (compareDates(valdate, trade.getTradeDate().getJDate(TimeZone.getDefault()))) {
            return getActivoPasivoInicial(trade, valdate);
        }
        return 0D;// 112
    }

    private double getLiquidacionesFinales(Trade trade, JDate valdate) {
        double amount = 0.0;

        PerformanceSwap brs = (PerformanceSwap) trade.getProduct();
        if (compareDates(brs.getMaturityDate(), valdate)) {
            CashFlowSet cashFlowSet = brs.getFlows();
            for (CashFlow cashFlow : cashFlowSet) {
                if (compareDates(cashFlow.getEndDate(), valdate)) {
                    amount += cashFlow.getAmount();
                }
            }
        }
        return amount;// 136
    }

    private double getActivoInicial(Trade trade, JDate valdate) {
        JDate lastDayOfPreviousMonth = getLastDayOfMonth(-1, valdate);
        return getActivo(trade, lastDayOfPreviousMonth);
    }

    private double getPasivoInicial(Trade trade, JDate valdate) {
        JDate lastDayOfPreviousMonth = getLastDayOfMonth(-1, valdate);
        return getPasivo(trade, lastDayOfPreviousMonth);
    }

    private String getMaturityDate(Trade trade) {
        PerformanceSwap brs = (PerformanceSwap) trade.getProduct();
        if (brs.getMaturityDate() != null) {
            return Util.dateToString(brs.getMaturityDate());
        }
        return "";// 132
    }

    private double getLiquidacionesIntermedias(Trade trade, JDate valdate) {
        double amount = 0D;

        try {
            TransferArray opTransfer = Optional.ofNullable(DSConnection.getDefault().getRemoteBO().getBOTransfers(trade.getLongId())).orElse(null);
            if (opTransfer != null) {
                amount = opTransfer.stream().filter(s -> compareDates(s.getValueDate(), valdate))
                        .filter(s -> s.getStatus().getStatus().equalsIgnoreCase("SETTLED")).mapToDouble(BOTransfer::getSettlementAmount).sum();
                return amount;
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, "Any BOTransfers retrieved for trade id " + trade.getLongId(), e);
        }
        return amount;// 136
    }

    private double getVariacionesValorRazon(Trade trade, JDate valdate) {
        double result = getActivoPasivoFinal(trade, valdate) - getActivoPasivoInicial(trade, valdate) - getLiquidacionesIntermedias(trade, valdate);
        return result;
    }

    private double getActivoPasivoInicial(Trade trade, JDate valdate) {
        if (getActivoInicial(trade, valdate) != 0D) {
            return getActivoInicial(trade, valdate);
        } else if (getPasivoInicial(trade, valdate) != 0D) {
            return getPasivoInicial(trade, valdate);
        }
        return 0D;
    }

    private double getActivoPasivoFinal(Trade trade, JDate valdate) {
        if (getActivoFinal(trade, valdate) != 0D) {
            return getActivoFinal(trade, valdate);
        } else if (getPasivoFinal(trade, valdate) != 0D) {
            return getPasivoFinal(trade, valdate);
        }
        return 0D;
    }

    private double getActivoFinal(Trade trade, JDate valdate) {
        JDate lastDayOfMonth = getLastDayOfMonth(0, valdate);
        return getActivo(trade, lastDayOfMonth);
    }

    private double getPasivoFinal(Trade trade, JDate valdate) {
        JDate lastDayOfMonth = getLastDayOfMonth(0, valdate);
        return getPasivo(trade, lastDayOfMonth);
    }

    private double getActivo(Trade trade, JDate date) {
        PLMarkValue plMarkValue;
        try {
            plMarkValue = CollateralUtilities.retrievePLMarkValue(trade, DSConnection.getDefault(), PLMARK_NAME, PRICING_ENV, date);
            return null != plMarkValue && plMarkValue.getMarkValue() > 0.0 ? plMarkValue.getMarkValue() : 0D;

        } catch (RemoteException e) {
            Log.error(this, "Any PLMarks retrieved for trade id " + trade.getLongId() + " and pricingEnv " + PRICING_ENV, e);
            return 0D;
        }
    }

    private double getPasivo(Trade trade, JDate date) {
        PLMarkValue plMarkValue;
        try {
            plMarkValue = CollateralUtilities.retrievePLMarkValue(trade, DSConnection.getDefault(), PLMARK_NAME, PRICING_ENV, date);
            return null != plMarkValue && plMarkValue.getMarkValue() < 0.0 ? plMarkValue.getMarkValue() : 0D;

        } catch (RemoteException e) {
            Log.error(this, "Any PLMarks retrieved for trade id " + trade.getLongId() + " and pricingEnv " + PRICING_ENV, e);
            return 0D;
        }
    }

    private JDate getLastDayOfMonth(int month, JDate valdate) {
        Calendar lastDayCal = valdate.getEOM().asCalendar();
        lastDayCal.add(Calendar.MONTH, month);
        lastDayCal.set(Calendar.DATE, lastDayCal.getActualMaximum(Calendar.DAY_OF_MONTH));
        JDate.valueOf(lastDayCal);
        if (CollateralUtilities.isBusinessDay(JDate.valueOf(lastDayCal), Util.string2Vector("SYSTEM"))) {
            return JDate.valueOf(lastDayCal);
        } else {
            return DateUtil.getPrevBusDate(JDate.valueOf(lastDayCal), Util.string2Vector("SYSTEM"));
        }
    }

    private boolean compareDates(JDate date1, JDate date2) {
        if (date1.getMonth() == date2.getMonth() && date1.getYear() == date2.getYear()) {
            return true;
        }
        return false;
    }

}
