package calypsox.tk.report;

import calypsox.util.FormatUtil;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.apps.util.CalypsoTreeNode;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.mapping.triparty.SecCode;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Collateral;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.product.feature.impl.ProductAttributes;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import org.apache.commons.lang.StringUtils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

public class PdvContingenciaReportStyle extends TradeReportStyle {

    //FORMATS
    public static final String DATE_FORMAT = "yyyyMMdd";
    public static final String FORMAT_NUMBER_23 = "##############0.########";
    public static final String FORMAT_NUMBER_13 = "0.############";

    //CONSTANTS
    public static final String SL = "SL";
    public static final String PARTENONACCOUNTINGID = "PartenonAccountingID";
    public static final String SECTORCONTABLE = "SECTORCONTABLE";
    public static final String INTRAGROUP = "INTRAGROUP";
    public static final String CLEAN_PRICE = "CleanPrice";
    public static final String OFFICIAL = "OFFICIAL";

    private static final String ALM_BRANCH_CODE = "ALM_BRANCH_CODE";
    private static final String ALM_ENTITY_CODE = "ALM_ENTITY_CODE";

    //COLUMNS
    public static final String COD_BO = "COD_BO";
    public static final String COD_CNTR_CON = "COD_CNTR_CON";
    public static final String COD_COBE = "COD_COBE";
    public static final String COD_ENTD = "COD_ENTD";
    public static final String COD_FRT = "COD_FRT";
    public static final String COD_MONE_O = "COD_MONE_O";
    public static final String COD_NMOT = "COD_NMOT";
    public static final String COD_PORTF = "COD_PORTF";
    public static final String COD_PRDT = "COD_PRDT";
    public static final String COD_SECTOR_CONTABLE = "COD_SECTOR_CONTABLE";
    public static final String COD_SECURITY = "COD_SECURITY";
    public static final String COD_SEGM = "COD_SEGM";
    public static final String COD_AMORT = "COD_AMORT";
    public static final String COD_TIP_PERF = "COD_TIP_PERF";
    public static final String COD_TIP_REF = "COD_TIP_REF";
    public static final String DEF_PAIS = "DEF_PAIS";
    public static final String DEF_SECURITY = "DEF_SECURITY";
    public static final String DIV_MONEDA = "DIV_MONEDA";
    public static final String FEC_EJERCICIO = "FEC_EJERCICIO";
    public static final String FEC_INICIO = "FEC_INICIO";
    public static final String FEC_VALOR = "FEC_VALOR";
    public static final String FEC_PROX_AMORT = "FEC_PROX_AMORT";
    public static final String FEC_PROX_LIQ_INT = "FEC_PROX_LIQ_INT";
    public static final String FEC_PROX_REP = "FEC_PROX_REP";
    public static final String FEC_ULT_REP = "FEC_ULT_REP";
    public static final String FEC_VENCIMIENTO = "FEC_VENCIMIENTO";
    public static final String IMP_MED_POSICION = "IMP_MED_POSICION";
    public static final String IMP_NOM_O = "IMP_NOM_O";
    public static final String IMP_NOMINAL_LCL = "IMP_NOMINAL_LCL";
    public static final String IMP_PGND = "IMP_PGND";
    public static final String IMP_POLZ = "IMP_POLZ";
    public static final String IMP_VAL_CONT_O = "IMP_VAL_CONT_O";
    public static final String IMP_VAL_CONT_LCL = "IMP_VAL_CONT_LCL";
    public static final String IMP_VAL_MTM_O = "IMP_VAL_MTM_O";
    public static final String IMP_VAL_MTM_LCL = "IMP_VAL_MTM_LCL";
    public static final String IND_LQ = "IND_LQ";
    public static final String IND_COTZ = "IND_COTZ";
    public static final String IND_DIREC = "IND_DIREC";
    public static final String IND_ELEGIBLE_BDE = "IND_ELEGIBLE_BDE";
    public static final String IND_ELEGIBLE_BCE = "IND_ELEGIBLE_BCE";
    public static final String IND_ELEGIBLE_FED = "IND_ELEGIBLE_FED";
    public static final String IND_ELEGIBLE_FSA = "IND_ELEGIBLE_FSA";
    public static final String IND_ENTO = "IND_ENTO";
    public static final String IND_ESTRAT_CONT = "IND_ESTRAT_CONT";
    public static final String IND_GRUPO = "IND_GRUPO";
    public static final String IND_INTRAAREA = "IND_INTRAAREA";
    public static final String IND_PERIM = "IND_PERIM";
    public static final String IND_TIP_CARTERA = "IND_TIP_CARTERA";
    public static final String IND_TIP_COLAT = "IND_TIP_COLAT";
    public static final String IND_TIP_EMIS = "IND_TIP_EMIS";
    public static final String IND_TIP_FREC_CAP_PPAL = "IND_TIP_FREC_CAP_PPAL";
    public static final String IND_TIP_FREC_CAP_INT = "IND_TIP_FREC_CAP_INT";
    public static final String IND_TIP_FREC_LIQ_INT = "IND_TIP_FREC_LIQ_INT";
    public static final String IND_TIP_FREC_REP = "IND_TIP_FREC_REP";
    public static final String IND_TIPPLAZTEMP = "IND_TIPPLAZTEMP";
    public static final String NOM_COLAT = "NOM_COLAT";
    public static final String NUM_FREC_AMORT = "NUM_FREC_AMORT";
    public static final String NUM_FREC_LIQ_INT = "NUM_FREC_LIQ_INT";
    public static final String NUM_FREC_REP = "NUM_FREC_REP";
    public static final String NUM_PLAZTEMP = "NUM_PLAZTEMP";
    public static final String PER_CAP = "PER_CAP";
    public static final String PER_FLOOR = "PER_FLOOR";
    public static final String PER_SPREAD = "PER_SPREAD";
    public static final String PER_TASA_INT = "PER_TASA_INT";
    public static final String PER_TIP_INTERES = "PER_TIP_INTERES";
    public static final String PRECIO_RF = "PRECIO_RF";
    public static final String TIPO_PRESTAMO = "TIPO_PRESTAMO";

    public static final String[] ADDITIONAL_COLUMNS = new String[]{COD_BO, COD_CNTR_CON, COD_COBE,
            COD_ENTD, COD_FRT, COD_MONE_O, COD_NMOT, COD_PORTF, COD_PRDT, COD_SECTOR_CONTABLE, COD_SECURITY,
            COD_SEGM, COD_AMORT, COD_TIP_PERF, COD_TIP_REF, DEF_PAIS, DEF_SECURITY,
            DIV_MONEDA, FEC_EJERCICIO, FEC_INICIO, FEC_VALOR, FEC_PROX_AMORT, FEC_PROX_LIQ_INT, FEC_PROX_REP,
            FEC_ULT_REP, FEC_VENCIMIENTO, IMP_MED_POSICION, IMP_NOM_O, IMP_NOMINAL_LCL, IMP_PGND, IMP_POLZ, IMP_VAL_CONT_O,
            IMP_VAL_CONT_LCL, IMP_VAL_MTM_O, IMP_VAL_MTM_LCL, IND_LQ, IND_COTZ, IND_DIREC,
            IND_ELEGIBLE_BDE, IND_ELEGIBLE_BCE, IND_ELEGIBLE_FED, IND_ELEGIBLE_FSA, IND_ENTO, IND_ESTRAT_CONT, IND_GRUPO, IND_INTRAAREA,
            IND_PERIM, IND_TIP_CARTERA, IND_TIP_COLAT, IND_TIP_EMIS, IND_TIP_FREC_CAP_PPAL, IND_TIP_FREC_CAP_INT,
            IND_TIP_FREC_LIQ_INT, IND_TIP_FREC_REP, IND_TIPPLAZTEMP, NOM_COLAT, NUM_FREC_AMORT, NUM_FREC_LIQ_INT, NUM_FREC_REP, NUM_PLAZTEMP,
            PER_CAP, PER_FLOOR, PER_SPREAD, PER_TASA_INT, PER_TIP_INTERES, PRECIO_RF, TIPO_PRESTAMO};

    //ensure tha numbers are parsed as ####,###.##, no matter the locale
    private static final NumberFormat numberFormatUK = NumberFormat.getInstance(Locale.UK);
    private final boolean adjustNominal = Util.isTrue(LocalCache.getDomainValueComment(DSConnection.getDefault(), "PdvContingencia.config", "InflationBondAdjustNominal"), false);

    public TreeList getTreeList() {
        TreeList treeList = super.getTreeList();// 68
        CalypsoTreeNode pdvContingenciaNode = new CalypsoTreeNode("PdvContingencia");// 69
        for (String column : ADDITIONAL_COLUMNS) {
            treeList.add(pdvContingenciaNode, column);// 70
        }
        return treeList;// 74
    }

    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) {
        Trade trade = (Trade) row.getProperty("Trade");
        SecLending secLending = (SecLending) trade.getProduct();
        PricingEnv pricingEnv = ReportRow.getPricingEnv(row);
        JDatetime valDateTime = ReportRow.getValuationDateTime(row);
        JDate valDate = valDateTime.getJDate(pricingEnv.getTimeZone());

        if (null != secLending) {

            if (columnName.equalsIgnoreCase(COD_BO)) {
                return formatLeftString(String.valueOf(trade.getLongId()), 20);

            } else if (columnName.equalsIgnoreCase(COD_CNTR_CON)) {
                return getCodigoCentro(trade, getKeywordValue(row, PARTENONACCOUNTINGID, errors));

            } else if (columnName.equalsIgnoreCase(COD_COBE)) {
                return formatLeftString(getMarginCallConfigName(trade), 20);

            } else if (columnName.equalsIgnoreCase(COD_ENTD)) {
                return getCodigoEntidad(trade, getKeywordValue(row, PARTENONACCOUNTINGID, errors));

            } else if (columnName.equalsIgnoreCase(COD_FRT)) {
                return formatLeftString(getKeywordValue(row, "MurexRootContract", errors), 20);

            } else if (columnName.equalsIgnoreCase(COD_MONE_O)) {
                return formatLeftString(getUnderlyingCcy(secLending), 20);

            } else if (columnName.equalsIgnoreCase(COD_NMOT)) {
                return formatLeftString(trade.getCounterParty().getCode(), 20);

            } else if (columnName.equalsIgnoreCase(COD_PORTF)) {
                return formatLeftString(trade.getBook().getName(), 20);

            } else if (columnName.equalsIgnoreCase(COD_PRDT)) {
                return formatLeftString(SL, 20);

            } else if (columnName.equalsIgnoreCase(COD_SECTOR_CONTABLE)) {
                return formatLeftString(getLegalEntityAtribute(trade, SECTORCONTABLE), 20);

            } else if (columnName.equalsIgnoreCase(COD_SECURITY)) {
                return formatLeftString(secLending.getSecurity().getSecCode(SecCode.ISIN), 20);

            } else if (columnName.equalsIgnoreCase(COD_SEGM)) {
                return formatLeftString("", 20);

            } else if (columnName.equalsIgnoreCase(COD_AMORT)) {
                return getAmortizationCode(secLending);

            } else if (columnName.equalsIgnoreCase(COD_TIP_PERF)) {
                return formatLeftString("", 20);

            } else if (columnName.equalsIgnoreCase(COD_TIP_REF)) {
                return formatLeftString("", 20);

            } else if (columnName.equalsIgnoreCase(DEF_PAIS)) {
                return formatLeftString(trade.getCounterParty().getCountry(), 250);

            } else if (columnName.equalsIgnoreCase(DEF_SECURITY)) {
                return formatLeftString(getUnderlyingISIN(secLending), 250);

            } else if (columnName.equalsIgnoreCase(DIV_MONEDA)) {
                return formatLeftString(getUnderlyingCcy(secLending), 3);

            } else if (columnName.equalsIgnoreCase(FEC_EJERCICIO)) {
                return FormatUtil.formatDate(valDate, DATE_FORMAT);

            } else if (columnName.equalsIgnoreCase(FEC_INICIO)) {
                return FormatUtil.formatDate(trade.getTradeDate().getJDate(pricingEnv.getTimeZone()), DATE_FORMAT);

            } else if (columnName.equalsIgnoreCase(FEC_VALOR)) {
                return FormatUtil.formatDate(secLending.getStartDate(), DATE_FORMAT);

            } else if (columnName.equalsIgnoreCase(FEC_PROX_AMORT)) {
                return getNextAmortizationDate(secLending, valDate);

            } else if (columnName.equalsIgnoreCase(FEC_PROX_LIQ_INT)) {
                return getEndDate(secLending, valDate);

            } else if (columnName.equalsIgnoreCase(FEC_PROX_REP)) {
                return getEndDate(secLending, valDate);

            } else if (columnName.equalsIgnoreCase(FEC_ULT_REP)) {
                return FormatUtil.formatDate(secLending.getStartDate(), DATE_FORMAT);

            } else if (columnName.equalsIgnoreCase(FEC_VENCIMIENTO)) {
                return getEndDate(secLending, valDate);

            } else if (columnName.equalsIgnoreCase(IMP_MED_POSICION)) {
                return formatLeftString("0", 23);

            } else if (columnName.equalsIgnoreCase(IMP_NOM_O)) {
                return formatDecimal(getNominal(trade, secLending, valDate, row, errors), FORMAT_NUMBER_23);

            } else if (columnName.equalsIgnoreCase(IMP_NOMINAL_LCL)) {
                Double fxQouteEUR = row.getProperty(PdvContingenciaReport.FX_EUR_CCY);

                if (fxQouteEUR != null && fxQouteEUR > 0.0D) {
                    return formatDecimal(getNominal(trade, secLending, valDate, row, errors) / fxQouteEUR, FORMAT_NUMBER_23);
                } else {
                    return formatDecimal(getNominal(trade, secLending, valDate, row, errors), FORMAT_NUMBER_23);
                }

            } else if (columnName.equalsIgnoreCase(IMP_PGND)) {
                return formatLeftString("", 23);

            } else if (columnName.equalsIgnoreCase(IMP_POLZ)) {
                return formatLeftString("", 23);

            } else if (columnName.equalsIgnoreCase(IMP_VAL_CONT_O)) {
                Double dirtyPrice = row.getProperty(PdvContingenciaReport.DIRTY_PRICE);
                Double notional = getNotional(trade, secLending, valDate, null);

                if (secLending.getSecurity() instanceof Equity) {
                    if (null != dirtyPrice) {
                        return formatDecimal(dirtyPrice * notional, FORMAT_NUMBER_23);
                    }
                }
                if (secLending.getSecurity() instanceof Bond && !"Negociacion".equalsIgnoreCase(trade.getBook().getAccountingBook().getName())) {
                    return formatDecimal(notional, FORMAT_NUMBER_23);
                } else {
                    if (null != dirtyPrice) {
                        return formatDecimal((dirtyPrice / 100) * notional, FORMAT_NUMBER_23);
                    }
                }
                return "";

            } else if (columnName.equalsIgnoreCase(IMP_VAL_CONT_LCL)) {
                Double dirtyPrice = row.getProperty(PdvContingenciaReport.DIRTY_PRICE);
                Double fxQoute = row.getProperty(PdvContingenciaReport.FX_EUR_CCY);
                Double notional = getNotional(trade, secLending, valDate, fxQoute);

                if (secLending.getSecurity() instanceof Equity) {
                    if (null != dirtyPrice) {
                        return formatDecimal(dirtyPrice * notional, FORMAT_NUMBER_23);
                    }
                }
                if (secLending.getSecurity() instanceof Bond && !"Negociacion".equalsIgnoreCase(trade.getBook().getAccountingBook().getName())) {
                    return formatDecimal(notional, FORMAT_NUMBER_23);
                } else {
                    if (null != dirtyPrice) {
                        return formatDecimal((dirtyPrice / 100) * notional, FORMAT_NUMBER_23);
                    }
                }
                return "";

            } else if (columnName.equalsIgnoreCase(IMP_VAL_MTM_O)) {
                Double dirtyPrice = row.getProperty(PdvContingenciaReport.DIRTY_PRICE);
                Double notional = getNotional(trade, secLending, valDate, null);

                if (secLending.getSecurity() instanceof Equity) {
                    if (null != dirtyPrice) {
                        return formatDecimal(dirtyPrice * notional, FORMAT_NUMBER_23);
                    }
                }
                if (secLending.getSecurity() instanceof Bond && !"Negociacion".equalsIgnoreCase(trade.getBook().getAccountingBook().getName())) {
                    return formatDecimal(notional, FORMAT_NUMBER_23);
                } else {
                    if (null != dirtyPrice) {
                        return formatDecimal((dirtyPrice / 100) * notional, FORMAT_NUMBER_23);
                    }
                }
                return "";

            } else if (columnName.equalsIgnoreCase(IMP_VAL_MTM_LCL)) {
                Double dirtyPrice = row.getProperty(PdvContingenciaReport.DIRTY_PRICE);
                Double fxQoute = row.getProperty(PdvContingenciaReport.FX_EUR_CCY);
                Double notional = getNotional(trade, secLending, valDate, fxQoute);

                if (secLending.getSecurity() instanceof Equity) {
                    if (null != dirtyPrice) {
                        return formatDecimal(dirtyPrice * notional, FORMAT_NUMBER_23);
                    }
                }

                if (secLending.getSecurity() instanceof Bond && !"Negociacion".equalsIgnoreCase(trade.getBook().getAccountingBook().getName())) {
                    return formatDecimal(notional, FORMAT_NUMBER_23);
                } else {
                    if (null != dirtyPrice) {
                        return formatDecimal((dirtyPrice / 100) * notional, FORMAT_NUMBER_23);
                    }
                }
                return "";

            } else if (columnName.equalsIgnoreCase(IND_LQ)) {
                return formatLeftString("", 1);

            } else if (columnName.equalsIgnoreCase(IND_COTZ)) {
                return formatLeftString("", 1);

            } else if (columnName.equalsIgnoreCase(IND_DIREC)) {
                String direction = (String) super.getColumnValue(row, "Direction", errors);
                if (direction.equals("Sec. Lending"))
                    return "S";
                if (direction.equals("Sec. Borrowing"))
                    return "B";

            } else if (columnName.equalsIgnoreCase(IND_ELEGIBLE_BDE)) {
                return formatLeftString("", 1);

            } else if (columnName.equalsIgnoreCase(IND_ELEGIBLE_BCE)) {
                return formatLeftString("", 1);

            } else if (columnName.equalsIgnoreCase(IND_ELEGIBLE_FED)) {
                return formatLeftString("", 1);

            } else if (columnName.equalsIgnoreCase(IND_ELEGIBLE_FSA)) {
                return formatLeftString("", 1);

            } else if (columnName.equalsIgnoreCase(IND_ENTO)) {
                return formatLeftString("", 1);

            } else if (columnName.equalsIgnoreCase(IND_ESTRAT_CONT)) {
                return getPorfolioType(trade);

            } else if (columnName.equalsIgnoreCase(IND_GRUPO)) {
                return formatLeftString(getLegalEntityAtribute(trade, INTRAGROUP), 1);

            } else if (columnName.equalsIgnoreCase(IND_INTRAAREA)) {
                return getInternalOperative(trade);

            } else if (columnName.equalsIgnoreCase(IND_PERIM)) {
                return "*";

            } else if (columnName.equalsIgnoreCase(IND_TIP_CARTERA)) {
                return "1";

            } else if (columnName.equalsIgnoreCase(IND_TIP_COLAT)) {
                return "2"; //Contract type is always OSLA

            } else if (columnName.equalsIgnoreCase(IND_TIP_EMIS)) {
                return formatLeftString("", 5);

            } else if (columnName.equalsIgnoreCase(IND_TIP_FREC_CAP_PPAL)) {
                return formatLeftString("", 3);

            } else if (columnName.equalsIgnoreCase(IND_TIP_FREC_CAP_INT)) {
                return formatLeftString("", 3);

            } else if (columnName.equalsIgnoreCase(IND_TIP_FREC_LIQ_INT)) {
                return formatLeftString("", 3);

            } else if (columnName.equalsIgnoreCase(IND_TIP_FREC_REP)) {
                return formatLeftString("", 1);

            } else if (columnName.equalsIgnoreCase(IND_TIPPLAZTEMP)) {
                return formatLeftString("", 1);

            } else if (columnName.equalsIgnoreCase(NOM_COLAT)) {
                return formatLeftString("OSLA", 250);

            } else if (columnName.equalsIgnoreCase(NUM_FREC_AMORT)) {
                return "";

            } else if (columnName.equalsIgnoreCase(NUM_FREC_LIQ_INT)) {
                return formatLeftString("", 1);

            } else if (columnName.equalsIgnoreCase(NUM_FREC_REP)) {
                return formatLeftString("", 1);

            } else if (columnName.equalsIgnoreCase(NUM_PLAZTEMP)) {
                return formatLeftString("", 1);

            } else if (columnName.equalsIgnoreCase(PER_CAP)) {
                return formatLeftString("", 13);

            } else if (columnName.equalsIgnoreCase(PER_FLOOR)) {
                return formatLeftString("", 13);

            } else if (columnName.equalsIgnoreCase(PER_SPREAD)) {
                return formatLeftString("", 13);

            } else if (columnName.equalsIgnoreCase(PER_TASA_INT)) {
                return formatLeftString("", 13);

            } else if (columnName.equalsIgnoreCase(PER_TIP_INTERES)) {
                return formatLeftString("", 13);
                //return getUnderlyingInterestType(secLending);

            } else if (columnName.equalsIgnoreCase(PRECIO_RF)) {

                if (secLending.getSecurity() instanceof Bond) {
                    Double cleanPrice = row.getProperty(CLEAN_PRICE);
                    if (cleanPrice != null)
                        return formatDecimal(cleanPrice, FORMAT_NUMBER_23);

                } else if (secLending.getSecurity() instanceof Equity) {
                    Double officialPrice = row.getProperty(OFFICIAL);
                    if (officialPrice != null)
                        return formatDecimal(officialPrice, FORMAT_NUMBER_23);
                }
                return formatLeftString("", 23);

            } else if (columnName.equalsIgnoreCase(TIPO_PRESTAMO)) {
                if ("Fee Cash Pool".equalsIgnoreCase(secLending.getSubType())) {
                    return 'C';
                } else if ("Fee Non Cash Pool".equalsIgnoreCase(secLending.getSubType())) {
                    return 'L';
                }
                return " ";
            }
        }
        return super.

                getColumnValue(row, columnName, errors);

    }

    private String getMarginCallConfigName(Trade trade) {
        String mccId = "MARGIN_CALL_CONFIG_ID";
        if (null != trade.getKeywordValue(mccId)) {
            int contract_id = trade.getKeywordAsInt(mccId);
            return Optional.ofNullable(BOCache.getMarginCallConfig(DSConnection.getDefault(), contract_id))
                    .map(MarginCallConfig::getName).orElse("");
        }
        return "";
    }

    private String getLegalEntityAtribute(Trade trade, String keyword) {
        LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, trade.getCounterParty().getId(),
                "ALL", keyword);
        if (attr != null)
            return attr.getAttributeValue();

        return "";
    }

    public String getCodigoCentro(Trade trade, String keyword) {
        LegalEntityAttribute almBranchCode = BOCache.getLegalEntityAttribute(DSConnection.getDefault(),
                trade.getBook().getProcessingOrgBasedId(), trade.getBook().getProcessingOrgBasedId(), LegalEntity.PROCESSINGORG, ALM_BRANCH_CODE);

        return almBranchCode != null && !Util.isEmpty(almBranchCode.getAttributeValue()) ? almBranchCode.getAttributeValue() : formatLeftString(checkString(keyword, 4, 8), 20);
    }

    public String getCodigoEntidad(Trade trade, String keyword) {
        LegalEntityAttribute almEntityCode = BOCache.getLegalEntityAttribute(DSConnection.getDefault(),
                trade.getBook().getProcessingOrgBasedId(), trade.getBook().getProcessingOrgBasedId(), LegalEntity.PROCESSINGORG, ALM_ENTITY_CODE);
        return almEntityCode != null && !Util.isEmpty(almEntityCode.getAttributeValue()) ? almEntityCode.getAttributeValue() : formatLeftString(checkString(keyword, 0, 4), 20);
    }

    public String getUnderlyingISIN(SecLending secLending) {
        if (secLending.getSecurity() instanceof Bond) {
            Bond bond = (Bond) secLending.getSecurity();
            return bond.getDescription();

        } else if (secLending.getSecurity() instanceof Equity) {
            Equity equity = (Equity) secLending.getSecurity();
            return equity.getDescription();

        }
        return "";
    }

    public String getUnderlyingCcy(SecLending secLending) {
        if (secLending.getSecurity() instanceof Bond) {
            Bond bond = (Bond) secLending.getSecurity();
            return bond.getCurrency();

        } else if (secLending.getSecurity() instanceof Equity) {
            Equity equity = (Equity) secLending.getSecurity();
            return equity.getCurrency();
        }
        return "";
    }

    public String getKeywordValue(ReportRow row, String keyword, Vector errors) {
        Object value = super.getColumnValue(row, TRADE_KEYWORD_PREFIX + keyword, errors);
        if (value != null) {
            return value.toString();
        }
        return "";
    }

    private String getAmortizationCode(SecLending secLending) {

        if (secLending.getSecurity() instanceof Bond) {
            Bond bond = (Bond) secLending.getSecurity();
            return formatLeftString(getAmortizationType(bond.getPrincipalStructure()), 1);
        }
        return formatLeftString("", 1);
    }


    public String getUnderlyingInterestType(SecLending secLending) {
        Double result = null;
        if (secLending.getSecurity() instanceof Bond) {
            Bond bond = (Bond) secLending.getSecurity();
            result = bond.getCoupon() * 100;
        }
        return formatDecimalWithZero(result, FORMAT_NUMBER_13);
    }

    public Double getNominal(Trade trade, SecLending secLending, JDate valDate, ReportRow row, Vector<Error> errors) {
        double nominal = secLending.getRemainingQuantity(valDate);
        if (secLending.getSecurity() instanceof Bond) {
            nominal = secLending.computeNominal(trade, valDate);
            if (adjustNominal && ((Bond) secLending.getSecurity()).getNotionalIndex() != null) {
                double factor = 1D;
                String capitalFactor = trade.getKeywordValue("CapitalFactor");
                try {
                    if (Util.isEmpty(capitalFactor)) {
                        Optional<Collateral> collat = secLending.getCollaterals().stream().filter(c -> ((Collateral) c).getSecurityId() == secLending.getSecurity().getId()).findFirst();
                        if (collat.isPresent()) {
                            ProductAttributes attributes = collat.get().getProductFeature(ProductAttributes.class);
                            factor = attributes == null ? 1D : attributes.get("Index Factor");
                        }
                    } else {
                        factor = numberFormatUK.parse(capitalFactor).doubleValue();
                    }
                    nominal *= factor;
                } catch (ParseException e) {
                    String errMsg = String.format("CapitalFactor KW has invalid format %s, #.###### expected.", capitalFactor);
                    Log.error(this, errMsg, e);
                    errors.add(new Error(String.format("%s %s: %s", errMsg, e.getClass().getSimpleName(), e.getMessage())));
                }
            }

        } else {
            Object initialPrice = super.getColumnValue(row, PdvMISReportStyle.SEC_EQ_PRICE_INITIAL, errors);
            if (initialPrice instanceof DisplayValue) {
                nominal = nominal * ((DisplayValue) initialPrice).get();
            }
        }
        return Math.abs(nominal);
    }

    public Double getNotional(Trade trade, SecLending secLending, JDate valDate, Double fxQoute) {
        double notional = 0D;
        if (fxQoute != null && fxQoute > 0.0) {
            notional = secLending.computeNominal(trade, valDate) / fxQoute;
        } else {
            notional = secLending.computeNominal(trade, valDate);
        }
        return Math.abs(notional);
    }

    public String getNextAmortizationDate(SecLending secLending, JDate valDate) {

        if (secLending.getSecurity() instanceof Bond) {
            Bond bond = (Bond) secLending.getSecurity();
            try {
                CashFlowSet cashFlowSet = bond.getFlows(valDate);
                return FormatUtil.formatDate(cashFlowSet.findEnclosingCashFlow(valDate, "INTEREST").getEndDate(), DATE_FORMAT);
            } catch (FlowGenerationException e) {
                Log.error(this, "Can't retrieve any flows in bond: " + bond.getName(), e);
            }
        } else if (secLending.getSecurity() instanceof Equity) {
            return getEndDate(secLending, valDate);
        }

        return formatLeftString("", 8);
    }

    public String getEndDate(SecLending secLending, JDate valdate) {
        String endDate = "";
        if (!secLending.getMaturityType().equalsIgnoreCase("OPEN")) {
            endDate = FormatUtil.formatDate(secLending.getEndDate(), DATE_FORMAT);

        } else if (secLending.getSecurity() instanceof Bond) {
            Bond bond = (Bond) secLending.getSecurity();
            endDate = FormatUtil.formatDate(bond.getEndDate(), DATE_FORMAT);

        } else if (secLending.getSecurity() instanceof Equity) {
            endDate = getThirdFridayDecember(valdate);
        }

        return endDate;
    }

    public String getPorfolioType(Trade trade) {
        Book book = trade.getBook();
        String accountingBook = book.getAccountingBook().getName();
        if (accountingBook.equalsIgnoreCase("Negociacion")) {
            return "1";
        } else if (accountingBook.equalsIgnoreCase("Disponible para la venta")) {
            return "2";
        } else if (accountingBook.equalsIgnoreCase("Inversion crediticia")
                || accountingBook.equalsIgnoreCase("Inversion a vencimiento")) {
            return "4";
        } else if (accountingBook.equalsIgnoreCase("Otros a valor razonable")) {
            return "5";
        }
        return "0";
    }

    public String getInternalOperative(Trade trade) {

        if (trade.getMirrorTradeId() > 0) {
            return "I";
        } else return "M";

    }

    public String getAmortizationType(String strAmortizationType) {
        if ("Bullet".equals(strAmortizationType)) {
            return "0";
        } else if ("Amortizing".equals(strAmortizationType)) {
            return "1";
        } else if ("Sinking".equals(strAmortizationType)) {
            return "2";
        } else if ("Amortizing / Schedule".equalsIgnoreCase(strAmortizationType) || "Sinking / Schedule".equalsIgnoreCase(strAmortizationType)) {
            return "3";
        }
        return "";
    }


    public String checkString(String value, int init, int fin) {
        if (value.length() >= fin) {
            return Optional.ofNullable(value.substring(init, fin)).orElse(emptyString(fin - init));
        } else {
            return "";
        }
    }

    public String emptyString(int lenght) {
        return StringUtils.leftPad("", lenght, " ");
    }


    public String formatLeftString(String value, int lenght) {
        final String substring = StringUtils.substring(value, 0, lenght);
        return substring;
    }

    public String formatDecimalWithZero(Double value, String formatSize) {
        if (null != value) {
            DecimalFormat decimalFormat = new DecimalFormat(formatSize);
            decimalFormat.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.UK));
            decimalFormat.setDecimalSeparatorAlwaysShown(false);
            return decimalFormat.format(value);
        }
        return formatLeftString("", formatSize.length() - 1);
    }

    public String formatDecimal(Double value, String formatSize) {
        if (null != value && 0.0 != value) {
            DecimalFormat decimalFormat = new DecimalFormat(formatSize);
            decimalFormat.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.UK));
            decimalFormat.setDecimalSeparatorAlwaysShown(false);
            return decimalFormat.format(value);
        }
        return formatLeftString("", formatSize.length() - 1);
    }

    private String getThirdFridayDecember(JDate valdate) {
        JDate thirdFridayDecember = getThirdFridayDecemberDate(valdate);
        if (valdate.after(thirdFridayDecember) || valdate.equals(thirdFridayDecember)) {
            JDate thirFridayDecemberNextYear = getThirdFridayDecemberDate(valdate.addMonths(12));
            return FormatUtil.formatDate(thirFridayDecemberNextYear, DATE_FORMAT);

        }
        return FormatUtil.formatDate(thirdFridayDecember, DATE_FORMAT);
    }

    public JDate getThirdFridayDecemberDate(JDate valdate) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getDefault());
        calendar.set(valdate.getYear(), Calendar.DECEMBER, 1);
        valdate = JDate.valueOf(calendar.getTime());
        int thirdFriday = 0;
        while (thirdFriday != 3) {
            valdate = valdate.addBusinessDays(1, CollateralUtilities.getSystemHolidays());
            if (valdate.getDayOfWeek() == Calendar.FRIDAY) {
                thirdFriday++;
            }
        }
        return valdate;
    }


}
