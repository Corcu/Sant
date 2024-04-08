package calypsox.tk.report;

import calypsox.tk.confirmation.builder.repo.RepoReportUtil;
import calypsox.util.FormatUtil;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.mapping.triparty.SecCode;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.*;
import com.calypso.tk.product.flow.CashFlowInterest;
import com.calypso.tk.product.flow.flowDefinition.impl.FdnCashFlowCouponDefinitionImpl;
import com.calypso.tk.product.flow.flowDefinition.impl.FdnCashFlowOptionCouponDefinitionImpl;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.refdata.RateIndex;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.service.DSConnection;
import org.apache.commons.lang.StringUtils;

import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

import static calypsox.tk.pledge.util.TripartyPledgeProrateCalculator.PLEDGE_PRORATE_PRINCIPAL;
import static calypsox.tk.report.RepoContingenciaReport.FATHER_REPO;
import static calypsox.tk.report.RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_MARKETVALUEMAN;

public class RepoContingenciaReportStyle extends TradeReportStyle {

    //FORMATS
    private static final String DATE_FORMAT = "yyyyMMdd";
    private static final String FORMAT_NUMBER_23 = "##############0.########";
    private static final String FORMAT_NUMBER_5 = "0.00000";
    private static final String FORMAT_NUMBER_8 = "0.00000000";

    //CONSTANTS
    public static final String OFFICIAL = "OFFICIAL";
    private static final String CLEAN_PRICE = "CleanPrice";
    public static final String RFR = "RFR";

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
    public static final String IMP_NOMINAL = "IMP_NOMINAL";
    public static final String IMP_PGND = "IMP_PGND";
    public static final String IMP_POLZ = "IMP_POLZ";
    public static final String IMP_VAL_CONT_O = "IMP_VAL_CONT_O";
    public static final String IMP_VAL_CONT_LCL = "IMP_VAL_CONT_LCL";
    public static final String IMP_VAL_MTM_O = "IMP_VAL_MTM_O";
    public static final String IMP_VAL_MTM_LCL = "IMP_VAL_MTM_LCL";
    public static final String IND_LIQ = "IND_LIQ";
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
    public static final String TRIPARTYREPOTYPE = "TRIPARTYREPOTYPE";
    private static final String ALM_BRANCH_CODE = "ALM_BRANCH_CODE";
    private static final String ALM_ENTITY_CODE = "ALM_ENTITY_CODE";
    private final Map<String, String> valueToValue = new HashMap<>();
    private final List<String> emptyColumns = new ArrayList<>();

    RepoReportUtil repoReportUtil;

    PricingEnv pricingEnvOfficial = null;
    private final String currentTradeSecPrincipal = "Principal Amount";

    public RepoContingenciaReportStyle() {
        emptyColumns.add(COD_SEGM);
        emptyColumns.add(IMP_PGND);
        emptyColumns.add(IMP_POLZ);
        emptyColumns.add(IND_LIQ);
        emptyColumns.add(IND_COTZ);
        emptyColumns.add(IND_ELEGIBLE_BDE);
        emptyColumns.add(IND_ELEGIBLE_BCE);
        emptyColumns.add(IND_ELEGIBLE_FED);
        emptyColumns.add(IND_ELEGIBLE_FSA);
        emptyColumns.add(IND_ENTO);
        emptyColumns.add(NUM_FREC_AMORT);
        emptyColumns.add(PER_CAP);
        emptyColumns.add(PER_FLOOR);
        emptyColumns.add(IND_TIP_FREC_CAP_INT);
        emptyColumns.add(NUM_FREC_LIQ_INT);
        emptyColumns.add(NUM_FREC_REP);
        emptyColumns.add(NUM_PLAZTEMP);
        emptyColumns.add(COD_COBE);
        emptyColumns.add(COD_MONE_O);

        valueToValue.put("Negociacion", "1");
        valueToValue.put("Inversion crediticia", "2");
        valueToValue.put("Inversion a vencimiento", "3");

        this.repoReportUtil=new RepoReportUtil();
    }

    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) {
        Trade trade = (Trade) row.getProperty("Trade");
        Product product = trade.getProduct();
        Repo repo = null;
        Trade fatherRepoTrade = null;
        Repo fatherRepo = null;
        Pledge pledge = null;
        Product security = null;
        Cash cash = null;
        Double pledgePrincipal = 0.0D;
        Double pledgeMTM = 0.0D;
        boolean isRepo = false;
        boolean isPledge = false;
        if(trade.getProduct() instanceof Repo){
            repo = (Repo) trade.getProduct();
            security = repo.getSecurity();
            cash = repo.getCash();
            isRepo = true;
        }else if(trade.getProduct() instanceof Pledge){
            isPledge = true;
            pledge = (Pledge) trade.getProduct();
            security = pledge.getSecurity();
            fatherRepoTrade = (Trade) row.getProperty(FATHER_REPO);
            if(null!=fatherRepoTrade && fatherRepoTrade.getProduct() instanceof Repo){
                fatherRepo = (Repo) fatherRepoTrade.getProduct();
                cash = fatherRepo.getCash();
            }
            pledgePrincipal = Optional.ofNullable(row.getProperty(PLEDGE_PRORATE_PRINCIPAL)).filter(Amount.class::isInstance).map(Amount.class::cast).map(Amount::get).orElse(0.0D);
            pledgeMTM =  Optional.ofNullable(row.getProperty(PLEDGE_PRORATE_MARKETVALUEMAN)).filter(Amount.class::isInstance).map(Amount.class::cast).map(Amount::get).orElse(0.0D);
        }

        PricingEnv pricingEnv = ReportRow.getPricingEnv(row);
        JDatetime valDateTime = ReportRow.getValuationDateTime(row);
        JDate valDate = JDate.getNow();
        final JDatetime valuationDatetime = (JDatetime) row.getProperty("ValuationDatetime");
        if(null!=valuationDatetime){
            valDate = valuationDatetime.getJDate(TimeZone.getDefault());
        }
        if(pricingEnv!=null){
            valDate = valDateTime.getJDate(pricingEnv.getTimeZone());
        }

        if(pricingEnvOfficial==null){
            try {
                pricingEnvOfficial = DSConnection.getDefault().getRemoteMarketData().getPricingEnv("OFFICIAL", valDateTime);
            } catch (CalypsoServiceException e) {
                Log.error(this,e);
            }
        }

        if (emptyColumns.contains(columnName)) {
            return "";

        } else if (columnName.equalsIgnoreCase(COD_BO)) {
            return trade.getLongId();

        } else if (columnName.equalsIgnoreCase(COD_CNTR_CON)) {
            return getCodigoCentro(trade, getKeywordValue(row, "PartenonAccountingID", errors));

        } else if (columnName.equalsIgnoreCase(COD_ENTD)) {
            return getCodigoEntidad(trade, getKeywordValue(row, "PartenonAccountingID", errors));

        } else if (columnName.equalsIgnoreCase(COD_FRT)) {
            return getKeywordValue(row, "MurexTradeID", errors);
        } else if (columnName.equalsIgnoreCase(COD_NMOT)) {
            return trade.getCounterParty().getCode();

        } else if (columnName.equalsIgnoreCase(COD_PORTF)) {
            return trade.getBook().getName();

        } else if (columnName.equalsIgnoreCase(COD_PRDT)) {
            return "RP";

        } else if (columnName.equalsIgnoreCase(COD_SECTOR_CONTABLE)) {
            return getLegalEntityAtribute(trade, "SECTORCONTABLE");

        } else if (columnName.equalsIgnoreCase(COD_SECURITY)) {
            return null!=security ? security.getSecCode(SecCode.ISIN) : "";

        } else if (columnName.equalsIgnoreCase(COD_AMORT)) {
            return getAmortizationCode(security);

        } else if (columnName.equalsIgnoreCase(COD_TIP_PERF)) {
            if(isPledge){
                return null!=fatherRepoTrade ? ((Repo)fatherRepoTrade.getProduct()).getDayCount() : "";
            }
            return super.getColumnValue(row, "Cash. DayCount", errors);

        } else if (columnName.equalsIgnoreCase(COD_TIP_REF)) {
            if(cash!=null){
                return cash.getRateType().equalsIgnoreCase("FLOATING") ? cash.getRateIndex().getName() : "";
            }
        } else if (columnName.equalsIgnoreCase(DEF_PAIS)) {
            return trade.getCounterParty().getCountry();

        } else if (columnName.equalsIgnoreCase(DEF_SECURITY)) {
            return getUnderlyingISIN(security);

        } else if (columnName.equalsIgnoreCase(DIV_MONEDA)) {
            if(isPledge){
                return null!=fatherRepo ? fatherRepo.getCurrency() : "";
            }
            return getCashCcy(cash);

        } else if (columnName.equalsIgnoreCase(FEC_INICIO)) {
            return FormatUtil.formatDate(trade.getTradeDate().getJDate(TimeZone.getDefault()), DATE_FORMAT);

        } else if (columnName.equalsIgnoreCase(FEC_VALOR)) {
            if(isPledge && null!=pledge){
                return FormatUtil.formatDate(pledge.getStartDate(), DATE_FORMAT);
            }else if(repo!=null) {
                return FormatUtil.formatDate(repo.getStartDate(), DATE_FORMAT);
            }
        } else if (columnName.equalsIgnoreCase(FEC_PROX_LIQ_INT)) {
            if(isPledge && isFloatingFatherRepo(fatherRepo)){
                CashFlow cashFlow = getCurrentCashFlow(fatherRepo, valDate);
                if (cashFlow != null) {
                    return FormatUtil.formatDate(cashFlow.getDate(), DATE_FORMAT);
                }
            }else if(null!=repo){
                if (Repo.OPEN.equals(repo.getMaturityType())) {
                    return FormatUtil.formatDate(repoReportUtil.getCallableOrProjectedDate(repo, valDate), DATE_FORMAT);
                } else {
                    return getNextIntFlowSettleDate(trade, repo, valDate);
                }
            }
            return "";
        } else if (columnName.equalsIgnoreCase(FEC_PROX_REP)) {
            if(isPledge && isFloatingFatherRepo(fatherRepo)){
                JDate date = getNEXTFIXINGDATE(fatherRepo, valDate);
                if(null==date){
                    date = pledge.getEndDate();
                }
                return FormatUtil.formatDate(date, DATE_FORMAT);
            }else {
                return getFecProxRep(repo, trade, valDate);
            }

        } else if (columnName.equalsIgnoreCase(FEC_ULT_REP)) {
            if(isPledge){
                return "";
            }else if(repo!=null) {
                return getFecUltRep(repo, trade, valDate);
            }
        } else if (columnName.equalsIgnoreCase(FEC_VENCIMIENTO)) {
            if(isPledge){
                return FormatUtil.formatDate(pledge.getEndDate(), DATE_FORMAT);
            }else {
                return FormatUtil.formatDate(repoReportUtil.getCallableOrProjectedDate(repo, valDate), DATE_FORMAT);
            }
        } else if (columnName.equalsIgnoreCase(FEC_EJERCICIO)) {
            return FormatUtil.formatDate(valDate, DATE_FORMAT);

        } else if (columnName.equalsIgnoreCase(IMP_MED_POSICION)) {
            return "0";

        } else if (columnName.equalsIgnoreCase(IMP_NOM_O)) {
            if(isPledge){
                double principal = pledge.computeNominal(trade, valDate, pledge.getSecurityId());
                try {
                    principal = CollateralUtilities.convertCurrency(pledge.getCurrency(), principal, null != fatherRepo ? fatherRepo.getCurrency() : "", valDate, pricingEnvOfficial);
                    return formatDecimal(Math.abs(principal), FORMAT_NUMBER_23);
                }  catch (MarketDataException e) {
                    Log.error(this,e);
                }
                return 0.0;
            }else if(null!=repo){
                return formatDecimal(Math.abs(calculateImporte(repo, valDate, trade)), FORMAT_NUMBER_23);
            }
        } else if (columnName.equalsIgnoreCase(IMP_NOMINAL)) {
            if(isPledge){
                String securityCcy = security.getCurrency();
                double fxQouteEUR = getFxPrice(OFFICIAL, securityCcy, valDate);
                final double nominal = Math.abs(pledge.computeNominal(trade));
                if (fxQouteEUR > 0.0) {
                    return formatDecimal(nominal / fxQouteEUR, FORMAT_NUMBER_23);
                } else {
                    return formatDecimal(nominal, FORMAT_NUMBER_23);
                }
            }else if(security!=null){
                String securityCcy = security.getCurrency();
                double fxQouteEUR = getFxPrice(OFFICIAL, securityCcy, valDate);
                double nominalDouble = Math.abs(calculateImporte(repo, valDate, trade));
                if (fxQouteEUR > 0.0) {
                    return formatDecimal(nominalDouble / fxQouteEUR, FORMAT_NUMBER_23);
                } else {
                    return formatDecimal(nominalDouble, FORMAT_NUMBER_23);
                }
            }
        } else if (columnName.equalsIgnoreCase(IMP_VAL_CONT_O)) {
            double principal = 0.0D;
            if(isPledge){
                principal = Math.abs(pledgePrincipal);
            }else if(repo!=null) {
                principal = Math.abs(repo.getPrincipal(valDate));
            }
            return formatDecimal(principal, FORMAT_NUMBER_23);

        } else if (columnName.equalsIgnoreCase(IMP_VAL_CONT_LCL)) {
            if(isPledge){
                final String currency = null!=fatherRepo ? fatherRepo.getCurrency() : "";
                if(!(fatherRepo.getCurrency().equalsIgnoreCase("EUR"))){
                    double fxQouteEUR = getFxPrice(OFFICIAL, currency, valDate);
                    if (fxQouteEUR > 0.0D) {
                        return formatDecimal(pledgePrincipal / fxQouteEUR, FORMAT_NUMBER_23);
                    }
                }else {
                    return pledgePrincipal;
                }
                return formatDecimal(0.0D, FORMAT_NUMBER_23);
            }else if(null!=repo){
                double principal = Math.abs(repo.getPrincipal(valDate));
                Double fxQouteEUR = getFxPrice(OFFICIAL, repo.getCash().getCurrency(), valDate);
                if (fxQouteEUR > 0.0D) {
                    return formatDecimal(principal / fxQouteEUR, FORMAT_NUMBER_23);
                }
                return formatDecimal(0.0D, FORMAT_NUMBER_23);
            }


        } else if (columnName.equalsIgnoreCase(IMP_VAL_MTM_O) || columnName.equalsIgnoreCase("Pricer.MARKETVALUEMAN")) {
            if(isPledge){
                return formatDecimal(pledgeMTM, FORMAT_NUMBER_23);
            }else {
                SignedAmount pmMARKETVALUEMAN = (SignedAmount) super.getColumnValue(row, "Pricer.MARKETVALUEMAN", errors);
                if (null != pmMARKETVALUEMAN && pmMARKETVALUEMAN.get() != 0.0) {
                    return formatDecimal(pmMARKETVALUEMAN.get(), FORMAT_NUMBER_23);
                }
            }
            return "";

        } else if (columnName.equalsIgnoreCase(IMP_VAL_MTM_LCL)) {
            if(isPledge){
                final String currency = null!=fatherRepo ? fatherRepo.getCurrency() : "";
                if(!(currency.equalsIgnoreCase("EUR"))){
                    double fxQouteEUR = getFxPrice(OFFICIAL, currency, valDate);
                    if (fxQouteEUR > 0.0D) {
                        return formatDecimal(pledgeMTM / fxQouteEUR, FORMAT_NUMBER_23);
                    }
                }else {
                    return pledgeMTM;
                }
                return formatDecimal(0.0D, FORMAT_NUMBER_23);

            }else {
                String plMarkCcy = getPLMarkCcy(trade, valDate, "MARKETVALUEMAN");
                Double fxQouteEUR = getFxPrice(OFFICIAL, plMarkCcy, valDate);
                SignedAmount pmMARKETVALUEMAN = (SignedAmount) super.getColumnValue(row, "Pricer.MARKETVALUEMAN", errors);
                if (null != pmMARKETVALUEMAN && pmMARKETVALUEMAN.get() != 0.0) {
                    return formatDecimal(pmMARKETVALUEMAN.get() / fxQouteEUR, FORMAT_NUMBER_23);
                }
            }
            return "";
        } else if (columnName.equalsIgnoreCase(IND_DIREC)) {
            if(isPledge){
                if(pledge.getQuantity()>=0){
                    return "B";
                }else {
                    return "S";
                }
            }else if(product instanceof Repo){
                    int directionSign=((Repo) product).getSign();
                    if (directionSign<0) {
                        return "B";
                    } else{
                        return "S";
                    }
            }else{
                return (String) super.getColumnValue(row, "Direction", errors);
            }
        } else if (columnName.equalsIgnoreCase(IND_ESTRAT_CONT)) {
            return getAccountingLinkFirstChar(trade);

        } else if (columnName.equalsIgnoreCase(IND_GRUPO)) {
            return getLegalEntityAtribute(trade, "INTRAGROUP");

        } else if (columnName.equalsIgnoreCase(IND_INTRAAREA)) {
            return getInternalOperative(trade);

        } else if (columnName.equalsIgnoreCase(IND_PERIM)) {
            return "*";

        } else if (columnName.equalsIgnoreCase(IND_TIP_CARTERA)) {
            return "";

        } else if (columnName.equalsIgnoreCase(IND_TIP_COLAT)) {
            return "2"; //Contract type is always OSLA

        } else if (columnName.equalsIgnoreCase(IND_TIP_FREC_LIQ_INT)) { //TODO father repo
            if(isPledge && isFloatingFatherRepo(fatherRepo)){
                return null!=cash ? cash.getPaymentFrequency() : "";
            }else if( isRepo && cash!=null){
                Frequency paymentFreq = cash.getPaymentFrequency();
                if (paymentFreq != null && !paymentFreq.equals(Frequency.F_NONE)) {
                    return repo.getCash().getPaymentFrequency().getTenor().toString();
                }
            }
            return "";

        } else if (columnName.equalsIgnoreCase(IND_TIP_FREC_REP)) {
            if(isPledge && isFloatingFatherRepo(fatherRepo)){
                final Cash repoCash = fatherRepo.getCash();
                Frequency cmpFreq = null!= repoCash ? repoCash.getCompoundFrequency() : null;
                if (cmpFreq != null && !cmpFreq.equals(Frequency.F_NONE)) {
                    return repoCash.getCompoundFrequency();
                }
                else {
                    return repoCash.getPaymentFrequency();
                }
            }else if(repo!=null){
                return getRateIndexTenor(repo);
            }
            return "";
        } else if (columnName.equalsIgnoreCase(NOM_COLAT)) {
            return "ISMA";

        } else if (columnName.equalsIgnoreCase(PER_SPREAD)) {
            if(isPledge && null!=fatherRepo){
                final Cash pledgeFatherCash = fatherRepo.getCash();
                if(pledgeFatherCash.getFixedRateB()){
                    return "";
                }
                return pledgeFatherCash.getSpread() * 100;
            }
            if(cash!=null){
                String rateString = new Rate(cash.getSpread()).toString();
                return rateString.replace(',', '.');
            }
            return "";

        } else if (columnName.equalsIgnoreCase(PER_TASA_INT)) {
            if(isPledge && fatherRepo!=null){
                return generateRate(fatherRepo,valDateTime,valDate);
            }else if(repo!=null){
                return formatDecimalWithZero(getPerTasaInt(repo, trade, valDate, valDateTime), FORMAT_NUMBER_8);
            }
            return "";
        } else if (columnName.equalsIgnoreCase(PER_TIP_INTERES)) {
            if(isPledge ){
                return "";
            }else if(repo!=null){
                return formatDecimalWithZero(getPerTasaInt(repo, trade, valDate, valDateTime) * 100, FORMAT_NUMBER_5);
            }
            return "";
        } else if (columnName.equalsIgnoreCase(PRECIO_RF)) {
            if(isPledge && fatherRepo!=null){
                Double cleanPrice = getCleanPriceQuote(valDateTime, valDate, security);
                if (cleanPrice != null) {
                    return formatDecimal(cleanPrice, FORMAT_NUMBER_23);
                }
            }else if(repo!=null){
                Double cleanPrice = getCleanPriceQuote(valDateTime, valDate, security);
                if (cleanPrice != null) {
                    return formatDecimal(cleanPrice, FORMAT_NUMBER_23);
                }
            }
            return "";

        } else if (columnName.equalsIgnoreCase(IND_TIP_EMIS)) {
            if(security!=null){
                return getBondTypeIndicator(security);
            }
            return "";
        } else if (columnName.equalsIgnoreCase(IND_TIPPLAZTEMP)) {
            if(isPledge && isFloatingFatherRepo(fatherRepo)){
                if (fatherRepo.getCash().getRateIndex() != null) {
                    return fatherRepo.getCash().getRateIndex().getTenor();
                }
            }else if(repo!=null){
                return getRateIndexTenor(repo);
            }
            return "";
        } else if (columnName.equalsIgnoreCase(FEC_PROX_AMORT)) {
            if(isPledge){
                return "";
            }
            return FormatUtil.formatDate(repoReportUtil.getCallableOrProjectedDate(repo, valDate), DATE_FORMAT);
        }else  if(columnName.equals(TRIPARTYREPOTYPE)){
            return security instanceof Bond ? "BOND" : security instanceof Equity ? "EQUITY" : "";
        }

        return super.getColumnValue(row, columnName, errors);
    }

    private double getPerTasaInt(Repo repo,Trade trade,JDate valDate, JDatetime valDateTime){
        double res=repo.getFixedRate();
        if(!repo.isFixedRate()){
            PricingEnv OficialPricePE = AppUtil.loadPE(OFFICIAL, valDateTime);
            double closePrice = RepoReportUtil.getIndexbyQuoteSet(valDate, OficialPricePE, repo);
            double roundedSpread=RoundingMethod.roundNearest(repo.getCash().getSpread(), 8);
            res=roundedSpread+closePrice;
        }
        return res;
    }

    private String getRateIndexTenor(Repo repo) {
        RateIndex rateIndex = repo.getRateIndex();
        if (null != rateIndex) {
            return repo.getRateIndex().getTenor().toString();
        }
        return "";
    }

    private String getBondTypeIndicator(Product security){
        String bondTypology="BOND_TIPOLOGY_3";
        String bondTypologyValue=security.getSecCode(bondTypology);
        String indicator="C";
        if("muni".equalsIgnoreCase(bondTypologyValue)||"govt".equalsIgnoreCase(bondTypologyValue)){
            indicator="P";
        }
        return indicator;
    }

    private String getLegalEntityAtribute(Trade trade, String keyword) {
        LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, trade.getCounterParty().getId(),
                "ALL", keyword);
        if (attr != null)
            return attr.getAttributeValue();

        return "";
    }

    private String getCodigoCentro(Trade trade, String keyword) {
        LegalEntityAttribute almBranchCode = BOCache.getLegalEntityAttribute(DSConnection.getDefault(),
                trade.getBook().getProcessingOrgBasedId(), trade.getBook().getProcessingOrgBasedId(), LegalEntity.PROCESSINGORG, ALM_BRANCH_CODE);

        return almBranchCode!=null&&!Util.isEmpty(almBranchCode.getAttributeValue())?almBranchCode.getAttributeValue():checkString(keyword, 4, 8);
    }

    private String getCodigoEntidad(Trade trade, String keyword) {
        LegalEntityAttribute almEntityCode = BOCache.getLegalEntityAttribute(DSConnection.getDefault(),
                trade.getBook().getProcessingOrgBasedId(), trade.getBook().getProcessingOrgBasedId(), LegalEntity.PROCESSINGORG, ALM_ENTITY_CODE);
        return almEntityCode!=null&&!Util.isEmpty(almEntityCode.getAttributeValue())?almEntityCode.getAttributeValue():checkString(keyword, 0, 4);
    }

    private String getUnderlyingISIN(Product security) {
        if (security instanceof Bond) {
            Bond bond = (Bond) security;
            return bond.getDescription();
        }
        if (security instanceof Equity) {
            Equity equity = (Equity) security;
            return equity.getDescription();
        }
        return "";
    }

    private String getCashCcy(Cash cash){
        return null!=cash ? cash.getCurrency() : "";
    }


    private String getKeywordValue(ReportRow row, String keyword, Vector errors) {
        Object value = super.getColumnValue(row, TRADE_KEYWORD_PREFIX + keyword, errors);
        if (value != null) {
            return value.toString();
        }
        return "";
    }

    private String getAmortizationCode(Product security) {
        if (security instanceof Bond) {
            Bond bond = (Bond) security;
            return bond.getPrincipalStructure();
        }
        return "";
    }

    private String getFecUltRep(Repo repo, Trade trade, JDate valDate){
        JDate resDate;
        boolean isFixedRate=Optional.ofNullable(repo.getCash())
                .map(Cash::getFixedRateB).orElse(true);
        if(isFixedRate) {
            resDate=repo.getStartDate();
        }else{
            resDate=Optional.ofNullable(getPreviousIntFlowResetDate(trade,repo,valDate))
                    .orElse(repo.getStartDate());
        }
        return FormatUtil.formatDate(resDate,DATE_FORMAT);
    }

    private String getFecProxRep(Repo repo, Trade trade, JDate valDate){
        if(null!=repo){
            JDate resDate=repoReportUtil.getCallableOrProjectedDate(repo,valDate);
            if(null!=repo.getCash().getRateIndex()){
                boolean rfr = Boolean.parseBoolean(repo.getCash().getRateIndex().getDefaults().getAttribute(RFR));
                if(rfr){
                    return FormatUtil.formatDate(RepoReportUtil.getNextBusinessDay(valDate),DATE_FORMAT);
                }
            }
            boolean isFixedRate=Optional.ofNullable(repo.getCash())
                    .map(Cash::getFixedRateB).orElse(true);
            if(!isFixedRate) {
                resDate=Optional.ofNullable(getNextIntFlowResetDate(trade,repo,valDate))
                        .orElse(repoReportUtil.getCallableOrProjectedDate(repo,valDate));
            }
            return FormatUtil.formatDate(resDate,DATE_FORMAT);
        }
        return "";
    }

    private String getNextIntFlowSettleDate(Trade trade,Repo repo, JDate valDate){
        CashFlowSet flows=getCalculateCashFlows(trade,repo,valDate);
        return Optional.ofNullable(flows)
                .map(cf -> cf.findNextFlowByPaymentDate(CashFlow.INTEREST, valDate))
                .map(CashFlow::getDate)
                .map(date->FormatUtil.formatDate(date,DATE_FORMAT))
                .orElse("");
    }

    private String getPreviousIntFlowSettleDate(Trade trade,Repo repo, JDate valDate){
        CashFlowSet flows=getCalculateCashFlows(trade,repo,valDate);
        return Optional.ofNullable(flows)
                .map(cf -> cf.findPreviousFlowByPaymentDate(CashFlow.INTEREST, valDate))
                .map(CashFlow::getDate)
                .map(date->FormatUtil.formatDate(date,DATE_FORMAT))
                .orElse("");
    }


    private JDate getNextIntFlowResetDate(Trade trade,Repo repo, JDate valDate){
        CashFlowSet flows=getCalculateCashFlows(trade,repo,valDate);
        return Optional.ofNullable(flows).map(fl->repoReportUtil.findNextIntFlowByResetDate(fl,valDate))
                .map(CashFlowInterest::getResetDate)
                .orElse(null);
    }

    private JDate getPreviousIntFlowResetDate(Trade trade,Repo repo, JDate valDate){
        CashFlowSet flows=getCalculateCashFlows(trade,repo,valDate);
        return Optional.ofNullable(flows).map(fl->repoReportUtil.findPreviousIntFlowByResetDate(fl,valDate))
                .map(CashFlowInterest::getResetDate)
                .orElse(null);
    }

    private CashFlowSet getCalculateCashFlows(Trade trade,Repo repo, JDate valDate){
        CashFlowSet flows=null;
        try {
            flows=repo.getCash().getFlows(trade, valDate, true, -1, true);
        } catch (FlowGenerationException exc) {
            Log.error(RepoContingenciaReport.class.getSimpleName(),"Error while generating flows for trade id:"+trade.getLongId());
            Log.debug(RepoContingenciaReport.class.getSimpleName(),exc);
        }
        return flows;
    }


    private String getAccountingLinkFirstChar(Trade trade) {
        Book book = trade.getBook();
        return Optional.ofNullable(book)
                .map(Book::getAccountingBook).map(AccountingBook::getName)
                .map(name->name.substring(0,1)).orElse("");

    }

    private String getInternalOperative(Trade trade) {

        if (trade.getMirrorTradeId() > 0) {
            return "I";
        } else return "M";

    }

    private String checkString(String value, int init, int fin) {
        if (value.length() >= fin) {
            return Optional.ofNullable(value.substring(init, fin)).orElse("");
        } else {
            return "";
        }
    }

    private String formatLeftString(String value, int lenght) {
        final String substring = StringUtils.substring(value, 0, lenght);
        return substring;
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

    public String formatDecimalWithZero(Double value, String formatSize) {
        if (null != value) {
            DecimalFormat decimalFormat = new DecimalFormat(formatSize);
            decimalFormat.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.UK));
            decimalFormat.setDecimalSeparatorAlwaysShown(false);
            return decimalFormat.format(value);
        }
        return formatLeftString("", formatSize.length() - 1);
    }

    private double getFxPrice(String quoteSetName, String ccy2, JDate date) {
        PricingEnv env = new PricingEnv();
        env.setQuoteSetName(quoteSetName);
        return CollateralUtilities.getFXRatebyQuoteSet(date, "EUR", ccy2, env);
    }

    private String getPLMarkCcy(Trade trade, JDate valDate, String plMarkName) {

        try {
            PLMark plMark = CollateralUtilities.retrievePLMark(trade, DSConnection.getDefault(), "OFFICIAL_ACCOUNTING", valDate);
            return Optional.ofNullable(plMark).map(mark->mark.getPLMarkValueByName(plMarkName)).map(PLMarkValue::getCurrency)
                    .orElse("");
        } catch (RemoteException e) {
            Log.error(this, "Can't retrieve any PLMark for trade with id:" + trade.getLongId(), e);
        }

        return "";
    }

    private Double getCleanPriceQuote(JDatetime valDatetime, JDate valDate,Product security){
        PricingEnv cleanPricePE = AppUtil.loadPE(CLEAN_PRICE, valDatetime);
        if(null!=security){
            double close = CollateralUtilities.getQuotePrice(security, valDate, cleanPricePE.getName());
            if (close > 0.0) {
                return close;
            }
        }
        return null;
    }

    public boolean isAmortizationSinking(String strAmortizationType) {
        if(strAmortizationType ==null || "Bullet".equals(strAmortizationType)) {
            return false;
        }
        return true;
    }

    public boolean checkValidation (int i, JDate dateChecked, JDate dateToCheck){
        return i != 0 ? dateChecked != null ? dateToCheck.after(dateChecked) ?  dateToCheck.after(JDate.getNow()) ? false : true : false : true : true;
    }

    private Double calculateImporte(Repo repo, JDate valDate, Trade trade) {
        if (repo.getEndDate() != null && valDate.after(repo.getEndDate())) {
            return 0.0;
        }
        if (null != trade && repo.getSecurity() instanceof Bond) {
            Bond bond = (Bond) repo.getSecurity();
            if (isAmortizationSinking(bond.getPrincipalStructure())) {
                if (repo.getCustomFlowsB()) {
                    try {
                        bond.generateFlows(valDate);
                    } catch (FlowGenerationException e) {
                        Log.error(RepoContingenciaReport.class.getSimpleName(),"Error while generating flows for trade id: "+trade.getLongId());
                    }
                }
                CashFlowSet cashFlows = bond.getFlows();
                JDate datecheked = null;
                double amount = 0.0;
                for (int i = 0; i < cashFlows.size(); i++) {
                    CashFlow cf = cashFlows.get(i);
                    if(checkValidation(i, datecheked, cf.getStartDate())) {
                        double poolFactor = 0;
                        if (cashFlows.get(i).getCashFlowDefinition() instanceof FdnCashFlowCouponDefinitionImpl){
                            poolFactor = ((FdnCashFlowCouponDefinitionImpl)cashFlows.get(i).getCashFlowDefinition()).getPoolFactor();
                        } else if (cashFlows.get(i).getCashFlowDefinition() instanceof FdnCashFlowOptionCouponDefinitionImpl){
                            poolFactor = ((FdnCashFlowOptionCouponDefinitionImpl)cashFlows.get(i).getCashFlowDefinition()).getPoolFactor();
                        }
                        if (0 != poolFactor) {
                            amount = trade.computeNominal() * poolFactor;
                            datecheked = cf.getStartDate();
                        }
                    }
                }
                return amount;
            } else if(getInflationIndicator(repo)){
                String capitalFactor = trade.getKeywordValue("CapitalFactor");
                return trade.computeNominal(valDate) * Double.parseDouble(capitalFactor);
            } else
                return trade.computeNominal(valDate);
        }
        return 0.0;
    }

    public Boolean getInflationIndicator(Repo repo) {
        final String comment = DomainValues.comment("CodeActivationDV", "ActivateCapitalFactor");
        if(!Util.isEmpty(comment)){
            return true;
        }else if (repo.getSecurity() instanceof Bond) {
            Bond bond = (Bond) repo.getSecurity();
            return bond.getNotionalIndex() != null;
        }
        return false;
    }

    private boolean isFloatingFatherRepo(Repo repo){
        return null!=repo && "FLOATING".equalsIgnoreCase(repo.getCash().getRateType());
    }

    public CashFlow getCurrentCashFlow(Repo repo, JDate valDate) {
        CashFlowSet cashFlows;
        try {
            cashFlows = repo.getFlows(valDate);
        } catch (FlowGenerationException e) {
            Log.error(this, "Could not retrive Flows for Product " + repo.getId());
            return null;
        }

        if (cashFlows != null && !cashFlows.isEmpty()) {
            CashFlow cashFlow = (CashFlow) cashFlows.findEnclosingCashFlow(valDate, CashFlow.INTEREST);
            return cashFlow;
        }
        return null;
    }

    private JDate getNEXTFIXINGDATE(Repo repo,JDate valDate){
        if (repo.getCash().getFixedRateB()) {
            return null;
        }
        boolean rfr = Boolean.valueOf(repo.getCash().getRateIndex().getDefaults().getAttribute(RFR));
        if(rfr){
            return RepoReportUtil.getNextBusinessDay(valDate);
        }
        ArrayList<JDate> resetDates = new ArrayList<JDate>();
        try {
            CashFlowSet cashFlows = repo.getFlows(valDate);
            if (cashFlows != null) {
                for (int i = 0; i < cashFlows.size(); i++) {
                    CashFlow cf = cashFlows.get(i);
                    if (cf instanceof CashFlowInterest) {
                        CashFlowInterest cfi = (CashFlowInterest)cf;
                        if (cfi.getResetDate() != null) {
                            resetDates.add(cfi.getResetDate());
                        }
                    }
                }
            }
        } catch (FlowGenerationException e) {
            Log.error(this, "Could not retrive Flows for Product " + repo.getId());
            return null;
        }

        for (JDate resetDate : resetDates) {
            if (resetDate.after(valDate)) {
                return resetDate;
            }
        }
        return null;
    }

    private Object generateRate(Repo repo,JDatetime valDateTime, JDate valDate){
        if ("FLOATING".equalsIgnoreCase(repo.getCash().getRateType())) {
            PricingEnv OficialPricePE = AppUtil.loadPE(OFFICIAL, valDateTime);
            double closePrice = RepoReportUtil.getIndexbyQuoteSet(valDate, OficialPricePE, repo);
            CashFlow cf = (CashFlow) getCurrentCashFlow(repo, valDate);
            if (cf != null && cf instanceof CashFlowInterest) {
                CashFlowInterest cfi = (CashFlowInterest) cf;
                double res = (closePrice + cfi.getSpread()) * 100.0;
                return formatRate(res, '.');
            }
        } else if ("FIXED".equalsIgnoreCase(repo.getCash().getRateType())) {
            double res = repo.getCash().getFixedRate() * 100.0;
            return formatRate(res, '.');
        }
        return "0.00000";
    }

    public static Object formatRate(double number, char decimalSeparator) {
        DecimalFormat df = new DecimalFormat("0.00000");
        df.setGroupingUsed(false);
        DecimalFormatSymbols newSymbols = new DecimalFormatSymbols();
        newSymbols.setDecimalSeparator(decimalSeparator);
        df.setDecimalFormatSymbols(newSymbols);
        return df.format(number);
    }

    private String getFinancialFatherRepo(Trade trade){
        String value = "";
        if(null!=trade && trade.getProduct() instanceof Repo){
            Repo repo = (Repo)trade.getProduct();
            String secCurrency = repo.getSecurity().getCurrency();
            String cashCurrency = repo.getCash().getCurrency();
            if (secCurrency.equals(cashCurrency)) {
                value =  RepoMISReport.IDISSUEFINAN_ISSUE_FINANCIAL;
            }
            else {
                value = RepoMISReport.IDISSUEFINAN_FINANCIAL;
            }
        }
        return value;
    }

}
