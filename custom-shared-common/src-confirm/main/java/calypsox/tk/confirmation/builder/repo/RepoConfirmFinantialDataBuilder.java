package calypsox.tk.confirmation.builder.repo;

import calypsox.tk.confirmation.builder.CalConfirmationFinantialDataBuilder;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Cash;
import com.calypso.tk.product.Collateral;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.Security;
import com.calypso.tk.refdata.FdnRateIndex;
import com.calypso.tk.refdata.RateIndex;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class RepoConfirmFinantialDataBuilder extends CalConfirmationFinantialDataBuilder {

    Repo repo;
    JDate msgCreationDate;

    public RepoConfirmFinantialDataBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
        if(trade.getProduct() instanceof Repo){
            repo= (Repo) trade.getProduct();
            this.referenceSecurity = (Security) repo.getSecurity();
             this.msgCreationDate=JDate.valueOf(this.boMessage.getCreationDate());
            }
        }

    public String buildFixedIRPeriodRate(){
        String formattedRate="";
        if(repo.isFixedRate()){
            double rate=repo.getCash().getFixedRate()*100;
            formattedRate=formatNumber(rate);
        }
        return formattedRate;
    }

    public String buildFloatingIRPeriodRate(){
        return Optional.ofNullable(repo.getCash().getRateIndex())
                .map(this::formatRateIndexName)
                .orElse("Not applicable");
    }

    public String buildRepoUnderlyingNominalAmount(){
        return Optional.ofNullable(repo).map(rp->rp.getCollaterals().get(0))
                .map(Collateral::getNominal)
                .map(this::formatNumber).orElse(String.valueOf(0));
    }

    public String buildRepoInitialSettleAmount(){
        return Optional.ofNullable(repo).map(Repo::getPrincipal)
                .map(this::formatNumber).orElse(String.valueOf(0));
    }

    public String buildRepoFinalSettleAmount(){
        return Optional.ofNullable(repo)
                .map(this::getEndCash)
                .map(this::formatNumber)
                .orElse(String.valueOf(0));
    }

    public String buildRepoCleanPrice(){
        return Optional.ofNullable(repo).map(Repo::getCollaterals)
                .map(collats->collats.get(0)).map(Collateral::getInitialPrice)
                .map(dp->dp*100)
                .map(this::formatNumber).orElse(String.valueOf(0));
    }

    public String buildDirection(){
        String buySell="Sell";
        int buySellInd=Optional.ofNullable(repo).map(rep->rep.getBuySell(trade))
                .orElse(1);
        if(buySellInd!=1){
            buySell="Buy";
        }
        return buySell;

    }

    public String buildDualCurrency(){
        return Optional.ofNullable(repo).map(Repo::getCash).map(Cash::getCurrency).orElse("");
    }

    public String buildDualAmount(){
        return Optional.ofNullable(repo).map(Repo::getCash).map(Cash::getPrincipal)
                .map(this::formatNumber).orElse("");
    }

    private double getEndCash(Repo repo) {
        double endAmount=0.0D;
        try {
            CashFlowSet flowSet = repo.generateFlows(this.msgCreationDate);
            repo.calculate(flowSet, DSConnection.getDefault().getRemoteMarketData().getPricingEnv("OFFICIAL"), this.msgCreationDate);
            Repo.computePrincipalAndInterest(flowSet, this.repo);
            CashFlow lastFlow = Repo.getNetTotalFlow(flowSet);
            if (lastFlow != null) {
                endAmount=lastFlow.getAmount();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return endAmount;
    }

    protected String formatRateIndexName(RateIndex rateIndex){
        String name=Optional.ofNullable(rateIndex).map(FdnRateIndex::getName).orElse("");
        String tenor=Optional.ofNullable(rateIndex).map(FdnRateIndex::getTenor)
                .map(Tenor::getName).orElse("");
        return  "FLOATING "+name +
                " " + tenor;
    }
    
    public String buildFixedRateInd(){
        boolean isFixed=Optional.ofNullable(repo).map(Repo::isFixedRate).orElse(true);
        return isFixed ? "1":"0";
    }
}
