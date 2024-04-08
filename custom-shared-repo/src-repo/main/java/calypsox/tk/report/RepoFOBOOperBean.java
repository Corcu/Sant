package calypsox.tk.report;

import calypsox.tk.report.util.SecFinanceTradeUtil;
import com.calypso.apps.trading.secfinance.tradewindow.CashFlowPreview;
import com.calypso.apps.trading.secfinance.tradewindow.CashFlowPreviewFactory;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.*;
import com.calypso.tk.product.flow.CashFlowInterest;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.secfinance.SecFinanceTradeEntry;
import com.calypso.tk.secfinance.SecFinanceTradeEntryContext;
import com.calypso.tk.util.ComparatorFactory;
import com.calypso.tk.util.CurrencyUtil;
import com.calypso.tk.util.fieldentry.FieldEntry;

import java.util.*;

public class RepoFOBOOperBean {

    Long tradeId = 0L;
    String contractId = "";
    String rootContractId = "";
    String externalNumber = "";
    String book = "";
    String counterParty = "";
    String mirrorBook = "";
    String direction = "";
    String internalOper = "";
    boolean internal = false;
    boolean mirror = false;
    String isinCcy = "";
    String cashCcy = "";
    String quantity = "";
    String cleanPrice = "";
    String dirtyPrice = "";
    String nominal = "";
    String settlementType = "";
    String open = "";
    String couponType = "";
    String refernceIndex = "";
    String rateCoupon = "";
    String effInicial = "0.0";
    String effFinal = "0.0";
    String nomNoIndx = "";
    String marcaIdx = "";
    String action = "";
    String actionEffDate = "";


    ReportRow row = null;

    public void init(ReportRow row, Trade trade, JDate valDate, PricingEnv env){
        this.row = row;
        tradeId = trade.getLongId();
        Product product = trade.getProduct();
        if(product instanceof Repo){
            Repo repo = (Repo)product;
            Cash cash = repo.getCash();

            Collateral collateral = SecFinanceTradeUtil.getInstance().getCollateral(trade);


            SecFinanceTradeEntryContext context = new SecFinanceTradeEntryContext();
            SecFinanceTradeEntry externalSecFinanceTradeEntry = SecFinanceTradeEntry.createSecFinanceTradeEntry(trade, valDate.getJDatetime(TimeZone.getDefault()), env, context);


            marcaIdx = SecFinanceTradeUtil.getInstance().getInflationFactor(repo);
            internal = SecFinanceTradeUtil.getInstance().isInternal(trade);
            internalOper = SecFinanceTradeUtil.getInstance().isInternalOper(trade);
            contractId = trade.getKeywordValue("MurexTradeID");
            rootContractId = trade.getKeywordValue("MurexRootContract");
            externalNumber = trade.getKeywordValue("MurexGlobalId");
            counterParty = trade.getCounterParty().getCode();
            mirrorBook = null!=trade.getMirrorBook() ? trade.getMirrorBook().getName() : "";
            cashCcy = null!=cash ? cash.getCurrency() : "";

            quantity = SecFinanceTradeUtil.getInstance().formatValue(collateral.getQuantity(),"0.00");

            double cp =  ((Repo)trade.getProduct()).getCollaterals().get(0).getInitialPrice()*100;
            cleanPrice = String.valueOf(cp);

            dirtyPrice = SecFinanceTradeUtil.getInstance().formatValue(trade.getKeywordValue("MXInitialDirtyPrice"));

            setNominal(trade,externalSecFinanceTradeEntry);
            settlementType = SecFinanceTradeUtil.getInstance().getSettlementType(trade);
            couponType = SecFinanceTradeUtil.getInstance().getCashRateType(cash);
            refernceIndex = getIndexName(trade);
            rateCoupon = getRetoCoupond(cash, trade, valDate, env);

            setEffectivo(trade,valDate, externalSecFinanceTradeEntry);
            setIfInternal(trade);
            setIfOpen(SecFinanceTradeUtil.getInstance().isOpen(trade),repo);
            setDirec(repo.getDirection(Repo.REPO,repo.getSign()));

            if(internal && trade.getMirrorTradeLongId()!=0 && trade.getLongId()>trade.getMirrorTradeLongId()){
                setIfMirror();
            }
            setAction(repo);
        }

    }

    private void setDirec(String direc){
        switch (direc){
            case "Repo":
                direction = "Venta";
                break;
            case "Reverse":
                direction = "Compra";
                break;
        }
    }

    public void setIfInternal(Trade trade){
        if(internal){
            counterParty = "";
            mirrorBook = trade.getMirrorBook().getName();
        }
    }

    public void setIfMirror(){
        contractId = !Util.isEmpty(contractId) ? "-"+contractId : "";
        rootContractId = !Util.isEmpty(rootContractId) ? "-"+rootContractId : "";
        externalNumber = !Util.isEmpty(externalNumber) ? "-"+externalNumber : "";

    }

    private void setIfOpen(boolean isOpen,Repo repo){
        if(!isOpen){
            open = "N";
        }else {
            open = "Y";
        }
    }


    private void setEffectivo(Trade trade,JDate valDate,SecFinanceTradeEntry externalSecFinanceTradeEntry){
        try {
            Product product = trade.getProduct();
            CashFlowSet flows = product.getFlows(trade, valDate, false, -1, true);
            flows.sort(ComparatorFactory.getCashFlowDateComparator());
            CashFlow principal = flows.findFirstFlow("PRINCIPAL");
            if(null!=principal){
                effInicial = SecFinanceTradeUtil.getInstance().formatValue(principal.getAmount(),"0.00");
            }

            if(null!=externalSecFinanceTradeEntry){
                CashFlowPreview[] cashFlowPreviews = null;

                CashFlowPreviewFactory factory = new CashFlowPreviewFactory();
                cashFlowPreviews = factory.buildCashFlowPreviews(externalSecFinanceTradeEntry);

                //sum all intermediary INTEREST and PRINCIPAL cashflows after the startDate
                if(!Util.isEmpty(cashFlowPreviews) && product instanceof Repo){
                    final JDate startDate = ((Repo)product).getStartDate();
                    final JDate endDate = ((Repo)product).getEndDate();

                    Double value = Arrays.stream(cashFlowPreviews)
                            .filter(cashPrev -> "INTEREST".equalsIgnoreCase(cashPrev.getType()))
                            .filter(cashPrev -> cashPrev.getDate().after(startDate))
                            .mapToDouble(cashPrev -> cashPrev.getAmount().get()).sum();

                    Double valuePrincipal = Arrays.stream(cashFlowPreviews)
                            .filter(cashPrev -> "PRINCIPAL".equalsIgnoreCase(cashPrev.getType()))
                            .filter(cashPrev -> cashPrev.getDate().after(startDate))
                            .filter(cashPrev -> -256 != cashPrev.getRowBgColor().getRGB())
                            .mapToDouble(cashPrev -> cashPrev.getAmount().get()).sum();

                    Double valueBSB = "BSB".equalsIgnoreCase(product.getSubType()) ? Arrays.stream(cashFlowPreviews)
                            .filter(cashPrev -> "COUPON".equalsIgnoreCase(cashPrev.getType())
                                    || "INDEMNITY".equalsIgnoreCase(cashPrev.getType()))
                            .mapToDouble(cashPrev -> cashPrev.getAmount().get()).sum() : 0.0;

                    effFinal = SecFinanceTradeUtil.getInstance().formatValue(value + valuePrincipal + valueBSB,"0.00");
                }
            }
        } catch (FlowGenerationException e) {
            Log.error(this,"Error loading CashFlow for trade: " +trade.getLongId() + " " + e);
        }catch (Exception e){
            Log.error(this,"Cannot calculate Repo:" + e);
        }
    }

    public String getInternalOper() {
        return internalOper;
    }

    public void setInternalOper(String internalOper) {
        this.internalOper = internalOper;
    }

    public String getEffInicial() {
        return effInicial;
    }

    public String getEffFinal() {
        return effFinal;
    }

    public String getNomNoIndx() {
        return nomNoIndx;
    }

    public String getMarcaIdx() {
        return marcaIdx;
    }
    
    private String getRetoCoupond(Cash cash, Trade trade,JDate valDate, PricingEnv env){
        Optional<Double> value = Optional.ofNullable(cash).map(ca -> {
            if ("Fijo".equalsIgnoreCase(couponType)) {
                return ca.getCurrentFixedRate();
            }else {
                Product product = trade.getProduct();
                CashFlowSet flows = null;
                try {
                    flows = product.getFlows(trade, valDate, false, -1, true);
                    product.calculateAll(flows, env, valDate);
                } catch (FlowGenerationException e) {
                    throw new RuntimeException(e);
                }
                Double valueInterest = 0.0;

                if(null != cash.getFlows() && product instanceof Repo) {
                    valueInterest = Arrays.stream(cash.getFlows().getFlows())
                            .filter(cashPrev -> "INTEREST".equalsIgnoreCase(cashPrev.getType()))
                            .filter(cashPrev -> cashPrev.getCashFlowDefinition().getProduct() instanceof Repo)
                            .filter(cashPrev -> valDate.after(cashPrev.getStartDate()))
                            .filter(cashPrev -> valDate.before(cashPrev.getEndDate()) || valDate.equals(cashPrev.getEndDate()))
                            .mapToDouble(cashPrev -> ( ((CashFlowInterest)cashPrev).getRate())).sum();
                }
                return ca.getSpread() + valueInterest;
            }
        });

        return SecFinanceTradeUtil.getInstance().formatValue(value.map(aDouble -> aDouble * 100).orElse(0.0),"0.00###");
    }

    private String setNominal(Trade trade,SecFinanceTradeEntry externalSecFinanceTradeEntry){
        if(null!=externalSecFinanceTradeEntry){
            Object secValue = Optional.ofNullable(externalSecFinanceTradeEntry.get("Sec. Nominal")).map(FieldEntry::getValue).orElse("");
            if(secValue instanceof Amount){
                Double value = ((Amount) secValue).get();
                nomNoIndx = SecFinanceTradeUtil.getInstance().formatValue(value,"");
                if("S".equalsIgnoreCase(marcaIdx)){
                    double capitalFactor = trade.getKeywordAsDouble("CapitalFactor");
                    value = value * capitalFactor;
                }
                nominal = SecFinanceTradeUtil.getInstance().formatValue(value,"");
            }
        }
        return "0.0";
    }

    public String getIndexName(Trade trade){
        String referenceIndex = "";
        if(trade.getProduct() instanceof SecFinance){
            Cash cash = ((SecFinance) trade.getProduct()).getCash();
            if(null!=cash){
                boolean refIndex = "Flotante".equalsIgnoreCase(SecFinanceTradeUtil.getInstance().getCashRateType(cash));
                if(refIndex){
                    referenceIndex = SecFinanceTradeUtil.getInstance().getIndexName(cash);
                }
            }
        }
        return referenceIndex;
    }

    public void setAction(Repo repo){
        Vector <EventTypeAction>eventTypeActions = repo.getEventTypeActions();
        if(!Util.isEmpty(eventTypeActions)){
            Collections.sort(eventTypeActions, Comparator.comparingInt(EventTypeAction::getId));
            for(int i = 0; i < eventTypeActions.size();i++){
                EventTypeAction action = eventTypeActions.get(i);
                if("Partial Return".equalsIgnoreCase(action.getActionType())
                        || "Rate".equalsIgnoreCase(action.getActionType())){
                    this.action = action.getActionType();
                    this.actionEffDate = String.valueOf(action.getEffectiveDate());
                }
            }
        }
    }

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public boolean isMirror() {
        return mirror;
    }

    public void setMirror(boolean mirror) {
        this.mirror = mirror;
    }

    public Long getTradeId() {
        return tradeId;
    }

    public void setTradeId(Long tradeId) {
        this.tradeId = tradeId;
    }

    public String getContractId() {
        return contractId;
    }

    public String getRootContractId() {
        return rootContractId;
    }

    public String getExternalNumber() {
        return externalNumber;
    }

    public String getBook() {
        return book;
    }

    public void setBook(String book) {
        this.book = book;
    }

    public String getCounterParty() {
        return counterParty;
    }

    public String getMirrorBook() {
        return mirrorBook;
    }

    public String getDirection() {
        return direction;
    }

    public String getIsinCcy() {
        return isinCcy;
    }

    public String getCashCcy() {
        return cashCcy;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getCleanPrice() {
        return cleanPrice;
    }

    public String getDirtyPrice() {
        return dirtyPrice;
    }

    public String getNominal() {
        return nominal;
    }

    public String getSettlementType() {
        return settlementType;
    }

    public String getOpen() {
        return open;
    }

    public String getCouponType() {
        return couponType;
    }

    public String getRefernceIndex() {
        return refernceIndex;
    }

    public String getRateCoupon() {
        return rateCoupon;
    }

    public String getAction() {
        return action;
    }

    public String getActionEffDate() {
        return actionEffDate;
    }

    private Amount getFlowAmount(CashFlow flow, JDate asOfDate, String type) {
        double amount;
        if ((type == null || !"COUPON".equals(type)) && !"INDEMNITY".equals(flow.getType())) {
            amount = flow.isKnown(asOfDate) ? flow.getAmount() : flow.getProjectedAmount();
        } else if ("INTEREST".equalsIgnoreCase(flow.getType())) {
            amount = flow.getAmount();
        } else if (!flow.isKnown(asOfDate) && flow.getProjectedAmount() != 0.0D) {
            amount = flow.getProjectedAmount();
        } else {
            amount = flow.getAmount();
        }

        String ccy = flow.getCurrency();
        int roundingUnit = ccy != null ? CurrencyUtil.getRoundingUnit(ccy) : 0;
        return new Amount(amount, roundingUnit);
    }

}
