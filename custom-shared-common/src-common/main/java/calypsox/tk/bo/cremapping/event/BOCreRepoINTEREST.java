package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.util.TransferArray;

import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

public class BOCreRepoINTEREST extends SantBOCre{

    private CashFlow cashFlow;
    TransferArray boTransfers = new TransferArray();

    public BOCreRepoINTEREST(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void fillValues() {
        this.creDescription = "Interest";
        BOCreUtils.getInstance().setRepoPartenonAccId(this.boCre,this.trade,this);
        this.internal = BOCreUtils.getInstance().isInternal(this.trade);
        this.maturityDate = getMaturityDate();
        this.tomadoPrestado = BOCreUtils.getInstance().getTomadoPrestado(this.trade);
        this.nettingType = getInstance().getNettingType(this.creBoTransfer);

        boolean increaseRepo = BOCreUtils.getInstance().isIncreaseRepo(this.trade, this.creBoTransfer);
        this.nettingParent = null!=this.creBoTransfer ? this.creBoTransfer.getLongId() : this.tradeId;
        this.nettingNumber = "1";

        this.sentDateTime = JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault());
        this.underlyingType = BOCreUtils.getInstance().getMaturityType(this.trade);
        if(!increaseRepo){
            checkRLInterest();
        }

    }

    @Override
    protected BOTransfer getCreBoTransfer(){
       if(!(boCre.getTransferLongId() > 0)){
           boTransfers = BOCreUtils.getInstance().getXfersByEffectiveDate(this.boCre,this.trade,"INTEREST");
            if(boTransfers.size()>1){
                BOTransfer transfer = boTransfers.stream().filter(xfer -> "INTEREST".equalsIgnoreCase(xfer.getTransferType())
                        && !xfer.getNettedTransfer()).findFirst().orElse(boTransfers.get(0));
                if(null!=transfer){
                    return transfer;
                }
            }else if(!boTransfers.isEmpty()){
                return boTransfers.get(0);
            }
        }else {
            return super.getCreBoTransfer();
        }
       return null;
    }

    @Override
    protected Double getPosition() {
        return null;
    }

    @Override
    protected JDate getCancelationDate() {
        String originalEventType = super.loadOriginalEventType();
        List<String> listevents = Util.stringToList("CANCELED_RECEIPT,CANCELED_PAYMENT");

        if(listevents.contains(originalEventType)){
            return JDate.getNow();
        }

        return null;
    }

    @Override
    protected CollateralConfig getContract() {
        return null;
    }

    @Override
    protected Account getAccount() {
        return null;
    }

    @Override
    protected String loadIdentifierIntraEOD() {
        return "INTRADAY";
    }

    @Override
    protected String loadSettlementMethod(){
        return "";
    }

    private void checkRLInterest(){
        if(null!=boTransfers && !boTransfers.isEmpty()){
            boolean anyMatch = boTransfers.stream().anyMatch(xfer -> "INTEREST".equalsIgnoreCase(xfer.getTransferType()) && "DFP".equalsIgnoreCase(xfer.getDeliveryType()));
            if(!anyMatch) {
                this.eventType = "RL_INTEREST";
                this.creDescription = "Return Leg Interest";
            }
        }
    }

    private JDate getMaturityDate(){
        JDate maturity = BOCreUtils.getInstance().loadRepoInterestMaturityDate(this.creBoTransfer);
        if(!Optional.ofNullable(maturity).isPresent()){
            this.cashFlow = BOCreUtils.getInstance().getRepoCashFlow(this.trade,this.boCre);
            return null!=cashFlow ? cashFlow.getEndDate() : null;
        }
        return maturity;
    }

}
