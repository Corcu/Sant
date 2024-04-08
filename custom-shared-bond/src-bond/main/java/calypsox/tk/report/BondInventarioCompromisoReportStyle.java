package calypsox.tk.report;

import calypsox.tk.report.util.UtilReport;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Fee;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Cash;
import com.calypso.tk.product.FX;
import com.calypso.tk.product.FXForward;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import static calypsox.tk.report.FXPLMarkReportStyle.getOtherMultiCcyTrade;
import static calypsox.util.collateral.CollateralUtilities.getFXRate;

/**
 * @author dmenendd
 */
public class BondInventarioCompromisoReportStyle extends TradeReportStyle {

    public static final String ENTIDAD = "Entidad";
    public static final String FEC_PROCESO = "Fecha Proceso";
    public static final String TRADE_STATUS = "Trade Status";
    public static final String TRADE_ID = "Trade ID";
    public static final String FRONT_ID = "Front ID";
    public static final String PARTENON = "Partenon";
    public static final String CENTRO_CONTABLE = "Centro contable";
    public static final String PORTFOLIO = "Portfolio";
    public static final String MIRROR_BOOK = "Mirror Book";
    public static final String ROI = "ROI";
    public static final String ESTRATEGIA = "Estrategia";
    public static final String FEC_ENTRADA = "Fecha Entrada";
    public static final String TIPO_BONO = "Tipo Bono";
    public static final String TIPO_OPERACION = "Tipo Operacion";
    public static final String SENTIDO = "Sentido";
    public static final String FEC_CONTRATACION = "Fecha Contratacion";
    public static final String FEC_VALOR = "Fecha Valor";
    public static final String CTPY_GLCS = "Ctpy GLCS";
    public static final String CTPY_CODIGO_J = "Ctpy Codigo J";
    public static final String CTPTY_NOMBRE = "Ctpty Nombre";
    public static final String CTPTY_SECTOR = "Ctpty Sector";
    public static final String EMISOR_GLCS = "Emisor GLCS";
    public static final String EMISOR_CODIGO_J = "Emisor Codigo J";
    public static final String EMISOR_NOMBRE = "Emisor Nombre";
    public static final String EMISOR_SECTOR = "Emisor Sector";
    public static final String DIV_NEGOCIACION = "Divisa Negociacion";
    public static final String DIV_LIQUIDACION = "Divisa Liquidacion";
    public static final String ISIN = "ISIN";
    public static final String NOMINAL = "Nominal";
    public static final String PRINCIPAL = "Principal";
    public static final String EFECTIVO_OPER_DIV_EMISION = "Efectivo Operacion divisa emision";
    public static final String EFECTIVO_OPER_DIV_LIQUIDACION = "Efectivo Operacion divisa liquidacion";
    public static final String MTM_NETO = "MtM Neto";
    public static final String FIXING_LIQUIDACION_BOND_FWD_CASH = "Fixing Liquidacion Bond FWD Cash";
    public static final String MARCA_DUAL = "MARCA DUAL";
    public static final String FECHA_VALOR_FX = "FECHA VALOR FX";
    public static final String FX_TRADE_ID = "FX Trade ID";
    public static final String FX_FRONT_ID = "FX Front ID";
    public static final String CCY_FX_COMPRA = "DIVISA FX COMPRA";
    public static final String NOMINAL_FX_COMPRA = "NOMINAL FX COMPRA";
    public static final String CONTRAVALOR_NOMINAL_FX_COMPRA = "CONTRAVALOR NOMINAL FX COMPRA";
    public static final String NPV_FX_COMPRA = "NPV FX COMPRA";
    public static final String MARCA_INFL = "MARCA INFLACION";
    public static final String CALYPSO_CAPITAL_FACTOR = "CalypsoCapitalFactor";

    public static final String CONTRAVALOR_NPV_FX_COMPRA = "CONTRAVALOR NPV FX COMPRA";
    public static final String DIVISA_FX_VENTA = "DIVISA FX VENTA";
    public static final String NOMINAL_FX_VENTA = "NOMINAL FX VENTA";
    public static final String CONTRAVALOR_NOMINAL_FX_VENTA = "CONTRAVALOR NOMINAL FX VENTA";
    public static final String NPV_FX_VENTA = "NPV FX VENTA";
    public static final String CONTRAVALOR_NPV_FX_VENTA = "CONTRAVALOR NPV FX VENTA";
    public static final String NETO_CONTRAVALOR_NOMINALES = "NETO CONTRAVALOR NOMINALES";
    public static final String NETO_CONTRAVALOR_NPV = "NETO CONTRAVALOR NPV";


    private static final String TRADE_KEYWORD_BONDFORWARD = "BondForward";
    private static final String TRADE_KEYWORD_BONDFORWARDTYPE = "BondForwardType";
    private static final String TRADE_KEYWORD_BFFIXINGDATE = "BF_FixingDate";

    private static final String FWD_CASH_FIXING = "FWD_CASH_FIXING";

    @SuppressWarnings("rawtypes")
    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) {
        final String partenonKwdName = "PartenonAccountingID";
        final String ISSUE_TYPE = "ISSUE_TYPE";

        final Trade trade = row.getProperty(ReportRow.TRADE);
        if (!(trade.getProduct() instanceof Bond)) {
            return null;
        }

        String partenonKwd= row.getProperty(partenonKwdName);

        final Bond bond = row.getProperty("Bond Product");
        final JDatetime valDateTime = row.getProperty("valDateTime");
        final JDate valDate = row.getProperty("valDate");
        final PricingEnv pricingEnvOA = row.getProperty("pricingEnvOA");
        final LegalEntity issuer = row.getProperty("Issuer");
        Trade fxTrade = row.getProperty("fxTrade");

        //Capital Factor values
        double calypsoPoolFactor = row.getProperty("calypsoPoolFactor");
        double calypsoCapitalFactor = row.getProperty("calypsoCapitalFactor") ;

        //Init values FX
        String ccyCompra = row.getProperty("ccyCompra");
        double nominalFXCompra = row.getProperty("nominalFXCompra");
        double nominalFXCompraContravalor = row.getProperty("nominalFXCompraContravalor");
        double npvCompra = row.getProperty("npvCompra");
        double npvCompraContravalor = row.getProperty("npvCompraContravalor");
        String ccyVenta = row.getProperty("ccyVenta");
        double nominalFXVenta = row.getProperty("nominalFXVenta");
        double nominalFXVentaContravalor = row.getProperty("nominalFXVentaContravalor");
        double npvVenta = row.getProperty("npvVenta");
        double npvVentaContravalor = row.getProperty("npvVentaContravalor");


        if (columnName.equals(ENTIDAD)) {
            return initPartenonValues(partenonKwd,0,4);
        }
        if (columnName.equals(FEC_PROCESO)) {
            return formatDate(valDate);
        }
        if (columnName.equals(TRADE_STATUS)) {
            return Optional.ofNullable(trade).map(Trade::getStatus).map(Status::getStatus).orElse("");
        }
        if (columnName.equals(TRADE_ID)) {
            return Optional.ofNullable(trade).map(Trade::getLongId).orElse(0L);
        }
        if (columnName.equals(FRONT_ID)) {
            return Optional.ofNullable(trade).map(t -> t.getExternalReference()).orElse("");
        }
        if (columnName.equals(PARTENON)) {
            return partenonKwd;
        }
        if (columnName.equals(CENTRO_CONTABLE)) {
            return initPartenonValues(partenonKwd,4,8);
        }
        if (columnName.equals(PORTFOLIO)) {
            return Optional.ofNullable(trade).map(Trade::getBook)
                    .map(Book::getName).orElse("");
        }
        if (columnName.equals(MIRROR_BOOK)) {
            return Optional.ofNullable(trade.getMirrorBook()).isPresent() ? trade.getMirrorBook().getName() : "";
        }
        if (columnName.equals(ROI)) {
            return Optional.ofNullable(trade).map(t -> t.getKeywordValue(ROI)).orElse("");
        }
        if (columnName.equals(ESTRATEGIA)) {
            return Optional.ofNullable(trade).map(Trade::getBook)
                    .map(Book::getAccountingBook).map(AccountingBook::getName).orElse("");
        }
        if (columnName.equals(FEC_ENTRADA)) {
            return Optional.ofNullable(trade).map(t -> formatDate(t.getEnteredDate())).orElse("");
        }
        if (columnName.equals(TIPO_BONO)) {
            return Optional.ofNullable(bond).map(b -> getIssueTypeformat(b.getSecCode(ISSUE_TYPE))).orElse("");
        }
        if (columnName.equals(TIPO_OPERACION)) {
            return Optional.ofNullable(trade).map(t -> loadTipoOperacion(trade, bond)).orElse("");
        }
        if (columnName.equals(SENTIDO)) {
            return bond != null ? bond.getBuySell(trade) == 1 ? "BUY" : "SELL" : "";
        }
        if (columnName.equals(FEC_CONTRATACION)) {
            return Optional.ofNullable(trade).map(t -> formatDate(t.getTradeDate())).orElse("");
        }
        if (columnName.equals(FEC_VALOR)) {
            return Optional.ofNullable(trade).map(t -> formatDate(t.getSettleDate())).orElse("");
        }
        if (columnName.equals(CTPY_GLCS)) {
            return Optional.ofNullable(trade).map(t -> t.getCounterParty())
                    .map(l -> l.getCode()).orElse("");
        }
        if (columnName.equals(CTPY_CODIGO_J)) {
            return Optional.ofNullable(trade).map(t -> t.getCounterParty())
                    .map(l -> l.getExternalRef()).orElse("");
        }
        if (columnName.equals(CTPTY_NOMBRE)) {
            return Optional.ofNullable(trade).map(t -> t.getCounterParty())
                    .map(l -> l.getName()).orElse("");
        }
        if (columnName.equals(CTPTY_SECTOR)) {
            return Optional.ofNullable(trade).map(t -> t.getCounterParty())
                    .map(l -> getSector(l)).orElse("");
        }
        if (columnName.equals(EMISOR_GLCS)) {
            return Optional.ofNullable(issuer).map(l -> l.getCode()).orElse("");
        }
        if (columnName.equals(EMISOR_CODIGO_J)) {
            return Optional.ofNullable(issuer).map(l -> l.getExternalRef()).orElse("");
        }
        if (columnName.equals(EMISOR_NOMBRE)) {
            return Optional.ofNullable(issuer).map(l -> l.getName()).orElse("");
        }
        if (columnName.equals(EMISOR_SECTOR)) {
            return Optional.ofNullable(issuer).map(l -> getSector(l)).orElse("");
        }
        if (columnName.equals(DIV_NEGOCIACION)) {
            return Optional.ofNullable(trade).map(Trade::getTradeCurrency).orElse("");
        }
        if (columnName.equals(DIV_LIQUIDACION)) {
            return Optional.ofNullable(trade).map(Trade::getSettleCurrency).orElse("");
        }
        if (columnName.equals(ISIN)) {
            return Optional.ofNullable(bond).map(b -> b.getSecCode(ISIN)).orElse("");
        }
        if (columnName.equals(NOMINAL)) {
            return formatResult(bond.computeNominal(trade));
        }
        if (columnName.equals(PRINCIPAL)) {
            return formatResult(bond.computeNominal(trade)* calypsoCapitalFactor);
        }
        if (columnName.equals(EFECTIVO_OPER_DIV_EMISION)) {
            //Total
            Double nominal = Optional.ofNullable(bond).map(b -> b.computeNominal(trade))
                    .orElse(0.0D);
            Double dirtyPrice = Optional.ofNullable(trade).map(Trade::getNegociatedPrice).orElse(0.0D);
            return formatResult(nominal * dirtyPrice * calypsoPoolFactor);
        }
        if (columnName.equals(EFECTIVO_OPER_DIV_LIQUIDACION)) {
            //Multicurrency. settlement
            Double nominal = Optional.ofNullable(bond).map(b -> b.computeNominal(trade))
                    .orElse(0.0D);
            Double dirtyPrice = Optional.ofNullable(trade).map(Trade::getNegociatedPrice).orElse(0.0D);
            Double fx = Optional.ofNullable(trade).map(Trade::getSplitBasePrice).orElse(1D);
            fx = fx == 0.0D ? 1 : fx;
            return formatResult(nominal * dirtyPrice * calypsoPoolFactor / fx);
        }
        if (columnName.equals(MTM_NETO)) {
            if (trade.getBook().getAccountingBook().getName().equalsIgnoreCase("Inversion Crediticia"))
                return formatResult(0D);
            PLMark plMarkValueNPVD = getPLMarkValue(pricingEnvOA, trade, valDate);
            Double plMarkValue = getPLMark(plMarkValueNPVD, "MTM_NET_MUREX");
            if (isBondForwardCash(trade) && isInsideFwdPeriodDate(trade, valDateTime)) {
                plMarkValue = plMarkValue - getFwdCashFixingFeeAmt(trade);
            }
            return formatResult(plMarkValue);
        }
        if (columnName.equals(FIXING_LIQUIDACION_BOND_FWD_CASH)) {
            Vector<Fee> feesList = trade.getFeesList();
            if (!Util.isEmpty(feesList)){
                Optional<Fee> fwdCashFixing = feesList.stream().filter(f -> f.isType("FWD_CASH_FIXING")).findFirst();
                return formatResult(fwdCashFixing.<Object>map(CashFlow::getAmount).orElse(0));
            }
            return 0;
        }
        if (columnName.equals(MARCA_DUAL)) {
            return fxTrade != null ? "FX": "BONO";
        }
        if (columnName.equals(FECHA_VALOR_FX)) {
            return fxTrade != null ? formatDate(fxTrade.getSettleDate()): "";
        }
        if (columnName.equals(FX_TRADE_ID)) {
            return fxTrade != null ? fxTrade.getLongId(): "";
        }
        if (columnName.equals(FX_FRONT_ID)) {
            return fxTrade != null ? fxTrade.getExternalReference(): "";
        }
        if (columnName.equals(CCY_FX_COMPRA)) {
            return fxTrade != null ? ccyCompra : "";
        }
        if (columnName.equals(NOMINAL_FX_COMPRA)) {
            return fxTrade != null ? formatResult(nominalFXCompra) : "";
        }
        if (columnName.equals(CONTRAVALOR_NOMINAL_FX_COMPRA)) {
            return fxTrade != null ? formatResult(nominalFXCompraContravalor) : "";
        }
        if (columnName.equals(NPV_FX_COMPRA)) {
            return fxTrade != null ? formatResult(npvCompra) : "";
        }
        if (columnName.equals(CONTRAVALOR_NPV_FX_COMPRA)) {
            return fxTrade != null ? formatResult(npvCompraContravalor) : "";
        }
        if (columnName.equals(DIVISA_FX_VENTA)) {
            return fxTrade != null ? ccyVenta : "";
        }
        if (columnName.equals(NOMINAL_FX_VENTA)) {
            return fxTrade != null ? formatResult(nominalFXVenta) : "";
        }
        if (columnName.equals(CONTRAVALOR_NOMINAL_FX_VENTA)) {
            return fxTrade != null ? formatResult(nominalFXVentaContravalor) : "";
        }
        if (columnName.equals(NPV_FX_VENTA)) {
            return fxTrade != null ? formatResult(npvVenta) : "";
        }
        if (columnName.equals(CONTRAVALOR_NPV_FX_VENTA)) {
            return fxTrade != null ? formatResult(npvVentaContravalor) : "";
        }
        if (columnName.equals(NETO_CONTRAVALOR_NOMINALES)) {
            return fxTrade != null ? formatResult(nominalFXCompraContravalor + nominalFXVentaContravalor) : "";
        }
        if (columnName.equals(NETO_CONTRAVALOR_NPV)) {
            return fxTrade != null ? formatResult(npvCompraContravalor + npvVentaContravalor) : "";
        }
        if (columnName.equals(MARCA_INFL)) {
            return Optional.ofNullable(bond.getNotionalIndex())
                    .map(idx -> "Y")
                    .orElse("N");
        }
        if (columnName.equals(CALYPSO_CAPITAL_FACTOR)) {
            return calypsoCapitalFactor;
        }
        return super.getColumnValue(row, columnName, errors);
    }

    /**
     *
     * @param partenonKwd
     * @param first
     * @param last
     * @return
     */
    private String initPartenonValues(String partenonKwd, int first, int last){
        String out = "";
        if(!Util.isEmpty(partenonKwd)&&21==partenonKwd.length()){
            out=partenonKwd.substring(first,last);
        }
        return out;
    }

    /**
     *
     * @param jDate
     * @return
     */
    private String formatDate(JDate jDate){
        String date = "";
        if (jDate != null) {
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            date = format.format(jDate.getDate());
        }
        return date;
    }

    /**
     *
     * @param jDateTime
     * @return
     */
    private String formatDate(JDatetime jDateTime){
        String date = "";
        if (jDateTime != null) {
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            date = format.format(jDateTime);
        }
        return date;
    }

    /**
     *
     * @param o
     * @return
     */
    public static Object formatResult(Object o) {
        return UtilReport.formatResult(o, '.');
    }

    /**
     *
     * @param trade
     * @return
     */
    private String loadTipoOperacion(Trade trade, Bond bond){
        if ("true".equalsIgnoreCase(trade.getKeywordValue(TRADE_KEYWORD_BONDFORWARD))) {
            return "Delivery".equalsIgnoreCase(trade.getKeywordValue(TRADE_KEYWORD_BONDFORWARDTYPE)) ? JDate.getNow().before(loadForwardDate(trade, bond)) ?"Bond FWD Delivery" : "Bond Spot" : "Bond FWD Cash";
        }
        else
            return "Bond Spot";
    }

    /**
     *
     * @param trade
     * @param bond
     * @return
     */
    private JDate loadForwardDate(Trade trade, Bond bond) {
        return trade.getSettleDate().addBusinessDays((-1) * Integer.valueOf(trade.getKeywordValue("BondSettleDays")), bond.getHolidays());
    }

    /**
     *
     * @param legalEntity
     * @return
     */
    private String getSector(LegalEntity legalEntity) {
        Collection attributes = legalEntity.getLegalEntityAttributes();
        if (null == attributes) return "";

        for (Object object : attributes) {
            LegalEntityAttribute attribute = (LegalEntityAttribute) object;
            if ("SECTORCONTABLE".equalsIgnoreCase(attribute.getAttributeType())) {
                return attribute.getAttributeValue();
            }
        }
        return "";
    }

    /**
     *
     * @param pricingEnv
     * @param trade
     * @param date
     * @return
     */
    private PLMark getPLMarkValue(PricingEnv pricingEnv, Trade trade, JDate date) {
        PLMark plMark = null;
        try {
            plMark = CollateralUtilities.retrievePLMark(trade, DSConnection.getDefault(), pricingEnv.getName(), date);
            return plMark;
        } catch (RemoteException e) {
            Log.error(this, e);
            return null;

        }
    }

    /**
     *
     * @param plMark
     * @param type
     * @return
     */
    private Double getPLMark(PLMark plMark, String type){
        return null!=plMark && null!=plMark.getPLMarkValueByName(type) ? plMark.getPLMarkValueByName(type).getMarkValue() : 0.0D;
    }

    /**
     *
     * @param issueType
     * @return
     */
    private String getIssueTypeformat(String issueType){
        if (!Util.isEmpty(issueType)){
            if ("LT".equalsIgnoreCase(issueType)) {
                return "LETRA";
            } else if ("PG".equalsIgnoreCase(issueType)) {
                return "PAGARE";
            } else if ("BO".equalsIgnoreCase(issueType)) {
                return "BONO";
            } else if ("DD".equalsIgnoreCase(issueType)) {
                return "DEUDA";
            }
        }
        return "";
    }

    /**
     *
     * @param strAmortizationType
     * @return
     */
    public boolean isAmortizationSinking(String strAmortizationType) {
        return (strAmortizationType ==null || "Bullet".equals(strAmortizationType)) ? false : true;
    }

    /**
     *
     * @param i
     * @param dateChecked
     * @param dateToCheck
     * @return
     */
    public boolean checkValidation (int i, JDate dateChecked, JDate dateToCheck){
        return i != 0 ? dateChecked != null ? dateToCheck.after(dateChecked) ? dateToCheck.after(JDate.getNow()) ? false : true : false : true : true;
    }

    public double getFwdCashFixingFeeAmt(Trade trade){
        double fwdCashAmt=0.0d;
        Vector<Fee> fees= Optional.ofNullable(trade).map(Trade::getFeesList)
                .orElse(new Vector<>());
        for(Fee fee:fees){
            if(FWD_CASH_FIXING.equals(fee.getType())){
                fwdCashAmt=fee.getAmount();
            }
        }
        return fwdCashAmt;
    }

    /**
     *
     * @param trade
     * @return
     */
    public JDate getFwdFixingDate(Trade trade){
        JDate fixingDate=null;
        String dateStr= Optional.ofNullable(trade).map(t->t.getKeywordValue(TRADE_KEYWORD_BFFIXINGDATE))
                .orElse("19700101");
        SimpleDateFormat formatter=new SimpleDateFormat("yyyyMMdd");
        try {
            fixingDate=JDate.valueOf(formatter.parse(dateStr));
        } catch (ParseException exc) {
            Log.error(this,exc.getCause());
        }
        return fixingDate;
    }

    /**
     *
     * @param trade
     * @param valDate
     * @return
     */
    public boolean isInsideFwdPeriodDate(Trade trade, JDatetime valDate){
        JDate fixingJDate=getFwdFixingDate(trade);
        JDate valDateJDate=valDate.getJDate(TimeZone.getDefault());
        boolean isAfterFixingDate=valDateJDate.gte(fixingJDate);
        boolean isBeforeSettleDate= valDateJDate.before(trade.getSettleDate());
        return isAfterFixingDate && isBeforeSettleDate;
    }

    /**
     *
     * @param trade
     * @return
     */
    public boolean isBondForwardCash(Trade trade){
        boolean isBondFwd=isBondForward(trade);
        boolean isBondFwdCash=Optional.ofNullable(trade).map(t->t.getKeywordValue(TRADE_KEYWORD_BONDFORWARDTYPE))
                .map(kwd->kwd.equalsIgnoreCase(Cash.class.getSimpleName())).orElse(false);
        return isBondFwd && isBondFwdCash;
    }

    /**
     *
     * @param trade
     * @return
     */
    public boolean isBondForward(Trade trade){
        return Optional.ofNullable(trade).map(t->t.getKeywordValue(TRADE_KEYWORD_BONDFORWARD))
                .map(Boolean::parseBoolean).orElse(false);
    }
}
