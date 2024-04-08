package calypsox.tk.report;

import calypsox.tk.confirmation.builder.repo.RepoReportUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Pledge;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.Security;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.service.DSConnection;

import java.security.InvalidParameterException;
import java.util.Optional;
import java.util.TimeZone;
import java.util.Vector;

public class RepoInformesReportStyle extends TradeReportStyle {

    public static final String REPORT_DATE = "Report Date";
    public static final String SENTIDO = "SENTIDO";
    public static final String SITUACION = "SITUACION";
    public static final String EMISOR_J = "EMISOR J";
    public static final String ISSUE_TYPE_DESCRIPCION = "Descripcion ISSUE TYPE";
    public static final String ESTRATEGIA = "ESTRATEGIA";
    public static final String FH_VTO  = "FH_VTO";
    public static final String PRICER_MTM_NET_MUREX = "Pricer.MTM_NET_MUREX";
    public static final String REPO_INTEREST_RATE = "Repo Interest Rate";

    public static final String START_DATE = "StartDate";
    public static final String NOMINAL = "NOMINAL";
    public static final String TIPO = "TIPO";
    public static final String REPO_TYPE = "RepoType";

    public static final String EFECTIVO_IDA = "EFECTIVO_IDA";
    public static final String EFECTIVO_VUELTA = "EFECTIVO_VUELTA";
    public static final String DEVENGO_ACUMULADO = "Pricer.ACCRUAL_FIRST";
    public static final String VALOR_CONTABLE = "VALOR_CONTABLE";
    public static final String CUPONES_INTERMEDIOS = "CUPONES_INTERMEDIOS";


    private static final String COUPON = "COUPON";
    private static final String INTEREST = "INTEREST";
    private static final String INDEMNITY = "INDEMNITY";
    private static final String PRINCIPAL = "PRINCIPAL";



    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {

        if (row == null)
            return row;

        Trade trade = row.getProperty("Trade");
        final PricingEnv pricingEnv = (PricingEnv) row.getProperty("PricingEnv");
        JDatetime valDateTime = row.getProperty(ReportRow.VALUATION_DATETIME);

        if (null == valDateTime){
            valDateTime = new JDatetime(JDate.getNow(), TimeZone.getDefault());
        }

        JDate valDate = valDateTime.getJDate(TimeZone.getDefault());

        if (REPORT_DATE.equals(columnId)){
            return valDateTime;
        }

        if(EFECTIVO_IDA.equalsIgnoreCase(columnId)){
            if(isRepo(trade)){
                if(trade.getProduct().getSubType().equalsIgnoreCase(Repo.SUBTYPE_BSB)){
                    return row.getProperty(EFECTIVO_IDA);
                }else {
                    return super.getColumnValue(row,"Principal Amount",errors);
                }
            }else {
                return getAmount(row.getProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_PRINCIPAL));
            }
        }

        if(EFECTIVO_VUELTA.equalsIgnoreCase(columnId) || "End Cash".equalsIgnoreCase(columnId)){
            if(trade.getProduct().getSubType().equalsIgnoreCase(Repo.SUBTYPE_BSB)){
                return row.getProperty(EFECTIVO_VUELTA);
            }
            return super.getColumnValue(row,"End Cash",errors);
        }

        if(CUPONES_INTERMEDIOS.equalsIgnoreCase(columnId)){
            if(trade.getProduct().getSubType().equalsIgnoreCase(Repo.SUBTYPE_BSB)){
                return row.getProperty(CUPONES_INTERMEDIOS);
            }
            return 0.0D;
        }

        if(DEVENGO_ACUMULADO.equalsIgnoreCase(columnId)){
            if(isRepo(trade)){
                return super.getColumnValue(row,columnId,errors);
            }else {
                return getAmount(row.getProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_ACCRUAL));
            }
        }

        if(VALOR_CONTABLE.equalsIgnoreCase(columnId)){
            if(isRepo(trade)){
                return getAmount(row.getProperty(RepoInformesReport.MTM_NET_MUREX));
            }else {
                return getAmount(row.getProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_MTM));
            }
        }

        if (ESTRATEGIA.equals(columnId)) {
            if (null != trade.getBook().getAccountingBook())  {
                return trade.getBook().getAccountingBook().getName();
            }
            return "";
        }

        if (SENTIDO.equalsIgnoreCase(columnId)){
            if (trade != null)  {
                return trade.computeNominal(valDate) > 0 ? "TOMADO" : "CEDIDO";
            }
            return "";
        }

        if (SITUACION.equalsIgnoreCase(columnId)){
            if (trade != null)  {
                JDate startDate = null;
                if (trade.getProduct() instanceof Repo) {
                    Repo repo = (Repo) trade.getProduct();
                    startDate = repo.getStartDate();
                }else if(trade.getProduct() instanceof Pledge){
                    startDate = ((Pledge)trade.getProduct()).getStartDate();
                }
                if (null!=startDate && startDate.after(valDate)) {
                    return "COMPROMISO";
                } else {
                    return "VALOR";
                }
            }
            return "";
        }

        if (EMISOR_J.equalsIgnoreCase(columnId)){
            return getEmisorJMin(trade);
        }

        if (ISSUE_TYPE_DESCRIPCION.equalsIgnoreCase(columnId)){
            return getIssueTypeDescripcion(trade);
        }

        if (FH_VTO.equalsIgnoreCase(columnId)){
            if (trade != null){
                if( trade.getProduct() instanceof Repo)  {
                    return new RepoReportUtil().getCallableOrProjectedDate((Repo) trade.getProduct(), valDate);
                }else if(trade.getProduct() instanceof Pledge){
                    if(((Pledge) trade.getProduct()).isOpenTerm()){
                        return valDate.addBusinessDays(1, Util.string2Vector("TARGET"));
                    }else {
                        return ((Pledge) trade.getProduct()).getEndDate();
                    }
                }
            }
            return "";
        }

        if(NOMINAL.equalsIgnoreCase(columnId)){
            if( trade.getProduct() instanceof Repo)  {
                return super.getColumnValue(row,"Sec. Nominal (Current)",errors);
            }else if(trade.getProduct() instanceof Pledge){
                return new Amount(trade.computeNominal(valDate));
            }
        }

        if(REPO_TYPE.equalsIgnoreCase(columnId)){
            if (trade != null ) {
                if(trade.getProduct() instanceof Repo){
                    String isGCPoolingKW = trade.getKeywordValue("isGCPooling");
                    if(!Util.isEmpty(isGCPoolingKW) && "true".equalsIgnoreCase(isGCPoolingKW)){
                        return "GCPooling";
                    }else if(trade.getProduct().getSubType().equalsIgnoreCase(Repo.SUBTYPE_TRIPARTY)){
                        return "RepoTriparty";
                    } else if(trade.getProduct().getSubType().equalsIgnoreCase(Repo.SUBTYPE_BSB)){
                        return "BSB";
                    }
                    return "Repo";
                }else if(trade.getProduct() instanceof Pledge){
                    return "Pledge";
                }
            }
        }

        if(START_DATE.equalsIgnoreCase(columnId)){
            if(trade.getProduct() instanceof Repo){
                return super.getColumnValue(row,"Repo Start Date",errors);
            }else if(trade.getProduct() instanceof Pledge){
                return ((Pledge) trade.getProduct()).getStartDate();
            }
            return null;
        }

        if(TIPO.equalsIgnoreCase(columnId)){
            if( trade.getProduct() instanceof Repo)  {
                return super.getColumnValue(row,"Maturity Type",errors);
            }else if(trade.getProduct() instanceof Pledge){
                if(((Pledge) trade.getProduct()).isOpenTerm()){
                    return "OPEN";
                }else {
                    return "TERM";
                }
            }
        }

        if (REPO_INTEREST_RATE.equalsIgnoreCase(columnId)){
            return row.getProperty(RepoInformesReport.REPO_INTEREST_RATE);
        }

        if (PRICER_MTM_NET_MUREX.equalsIgnoreCase(columnId)){
            return row.getProperty(RepoInformesReport.MTM_NET_MUREX);
        }

        if ("Settle Cur.".equalsIgnoreCase(columnId)){
            if(isRepo(trade)){
                super.getColumnValue(row, columnId, errors);
            }else {
                final Trade fatherRepoTrade = Optional.ofNullable(row.getProperty(RepoTripartyPledgeReportTemplate.REPO_TRADE)).filter(Trade.class::isInstance).map(Trade.class::cast).orElse(null);
                return null!=fatherRepoTrade ? fatherRepoTrade.getSettleCurrency() : "";
            }
        }

        return super.getColumnValue(row, columnId, errors);

    }

    private String getIssueTypeDescripcion(Trade trade) {
        String issueType = "";
        if (trade.getProduct() instanceof  Repo) {
            Repo repo = (Repo) trade.getProduct();
            issueType = repo.getSecurity().getSecCode("ISSUE_TYPE");
        }else if(trade.getProduct() instanceof Pledge){
            issueType = ((Pledge) trade.getProduct()).getSecurity().getSecCode("ISSUE_TYPE");
        } else {
            issueType = trade.getProduct().getSecCode("ISSUE_TYPE");
        }

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

    private String getEmisorJMin(Trade trade) {
        if (trade != null) {
            if (trade.getProduct().getUnderlyingProduct() instanceof Security){
                Security security = (Security) trade.getProduct().getUnderlyingProduct();
                LegalEntityAttribute attribute=
                        BOCache.getLegalEntityAttribute(DSConnection.getDefault(),0,security.getIssuerId(),"ALL","J_MINORISTA");
                return Optional.ofNullable(attribute).map(LegalEntityAttribute::getAttributeValue).orElse("");
            }
        }
        return "";
    }

    private boolean isRepo(Trade trade){
        return Optional.of(trade).map(Trade::getProduct).filter(Repo.class::isInstance).isPresent();
    }

    private Amount getAmount(Amount amount){
        if(null!=amount && amount.get()==0.0){
            return null;
        }
        return amount;
    }


}
