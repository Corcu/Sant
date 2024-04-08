package calypsox.tk.confirmation.builder.repo;

import calypsox.tk.confirmation.builder.CalypsoConfirmationConcreteBuilder;
import com.calypso.apps.trading.secfinance.tradewindow.CashFlowPreview;
import com.calypso.apps.trading.secfinance.tradewindow.CashFlowPreviewFactory;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Cash;
import com.calypso.tk.product.Collateral;
import com.calypso.tk.product.Repo;
import com.calypso.tk.secfinance.SecFinanceTradeEntry;
import com.calypso.tk.secfinance.SecFinanceTradeEntryContext;
import com.calypso.tk.service.DSConnection;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class RepoBsbConfirmDataBuilder extends CalypsoConfirmationConcreteBuilder {

    List<CashFlowPreview> cashFlowsPrev;
    Repo repo;
    JDatetime msgCreationDate;
    PricingEnv pricingEnv;
    final DecimalFormat format=new DecimalFormat("0.######",  new DecimalFormatSymbols(Locale.ENGLISH));

    public RepoBsbConfirmDataBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
        if(trade.getProduct() instanceof Repo){
            cashFlowsPrev = new ArrayList<>();
            repo = (Repo) trade.getProduct();
            this.msgCreationDate = this.boMessage.getCreationDate();
            loadPricingEnv();
            generateFlows();
        }
    }

    public String buildCoupon(){
        return getSum("COUPON");
    }
    public String buildInterest(){
        return getSum("INTEREST");
    }
    public String buildIndemnity(){
        return getSum("INDEMNITY");
    }
    public String buildBsbFwdDirtyPrice(){
        return String.valueOf(repo.getBSBForwardDirtyPrice(trade, pricingEnv, JDate.getNow().getJDatetime(TimeZone.getDefault()))*100);
    }

    public String buildBasis(){
        return Optional.ofNullable(repo.getCash()).map(Cash::getFixedDayCount).map(DayCount::toString).orElse("");
    }

    public String buildCouponCO(){
        final Collateral collateral = repo.getCollaterals().get(0);
        final double coupon = collateral.getNominal() * collateral.getAccrual();
        return formatDouble(coupon);
    }

    public String buildNetTotalInit(){
       return getPrincipalOnStartDate();
    }

    public void generateFlows(){
        try {
            SecFinanceTradeEntry externalSecFinanceTradeEntry = SecFinanceTradeEntry.createSecFinanceTradeEntry(trade,msgCreationDate, pricingEnv, new SecFinanceTradeEntryContext());
            CashFlowPreview[] cashFlowPreviews = new CashFlowPreviewFactory().buildCashFlowPreviews(externalSecFinanceTradeEntry);
            if(!Util.isEmpty(cashFlowPreviews)){
                cashFlowsPrev.addAll(Arrays.asList(cashFlowPreviews));
            }
        }catch (Exception e){
            Log.error("","Cannot calculate Repo:" + e);
        }
    }

    private String getSum(String type){
        double thesum = 0.0;
        thesum = cashFlowsPrev.stream()
                .filter( cashF -> type.equalsIgnoreCase(cashF.getType()))
                .map(CashFlowPreview::getAmount)
                .filter(Amount.class::isInstance)
                .map(Amount.class::cast).map(Amount::get)
                .reduce(thesum, Double::sum);
        return formatDouble(thesum);
    }

    private String getPrincipalOnStartDate(){
        double thesum = 0.0;
        thesum = cashFlowsPrev.stream()
                .filter( cashF -> "PRINCIPAL".equalsIgnoreCase(cashF.getType()))
                .filter(c -> c.getDate().equals(repo.getStartDate()))
                .map(CashFlowPreview::getAmount)
                .filter(Amount.class::isInstance)
                .map(Amount.class::cast).map(Amount::get)
                .reduce(thesum, Double::sum);
        return formatDouble(thesum);
    }

    private void loadPricingEnv(){
        try {
            pricingEnv = DSConnection.getDefault().getRemoteMarketData().getPricingEnv("OFFICIAL_ACCOUNTING");
        }catch (Exception e){
            Log.error(this,e);
        }
    }


    private String formatDouble(Double value){
        return Optional.ofNullable(value)
                .map(format::format)
                .orElse("0");
    }

}
