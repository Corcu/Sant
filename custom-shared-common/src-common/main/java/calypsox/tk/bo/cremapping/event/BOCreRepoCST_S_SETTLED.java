package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.tk.bo.cremapping.util.RepoBOCreSettleMethodHandler;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Repo;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;

import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

public class BOCreRepoCST_S_SETTLED extends SantBOCre {

    public BOCreRepoCST_S_SETTLED(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void fillValues() {
        this.creDescription = "Cash Settlement";
        this.settlementMethod = new RepoBOCreSettleMethodHandler().getCreSettlementMethod(this.creBoTransfer);
        this.transferAccount = getInstance().getTransferAccount(this.settlementMethod,this.creBoTransfer);
      //  BOCreUtils.getInstance().setRepoPartenonAccId(this.boCre,this.trade,this);
        this.loadPartenonId();
        this.internal = BOCreUtils.getInstance().isInternal(this.trade);
        this.tomadoPrestado = BOCreUtils.getInstance().getTomadoPrestado(this.trade);
        this.sentDateTime = JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault());
        this.underlyingType = BOCreUtils.getInstance().getMaturityType(this.trade);
        this.nettingType = getInstance().getNettingType(this.creBoTransfer);
    }

    @Override
    protected String loadOriginalEventType() {
        String originalEventType = super.loadOriginalEventType();
        boolean failed = Boolean.parseBoolean(Optional.ofNullable(this.creBoTransfer).map(boTransfer -> boTransfer.getAttribute("Failed")).orElse("false"));
        if(failed && null!=originalEventType && originalEventType.contains("SETTLED")){
            originalEventType = "RE".concat(originalEventType);
        }
        return originalEventType;
    }

    @Override
    protected Double getPosition() {
        return null;
    }

    @Override
    protected JDate getCancelationDate() {
        String originalEventType = super.loadOriginalEventType();
        List<String> listevents = Util.stringToList("CANCELED_SEC_PAYMENT,CANCELED_SEC_RECEIPT,CANCELED_RECEIPT,CANCELED_PAYMENT");

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
        return getInstance().getSettleMethod(this.creBoTransfer);
    }

    public void loadPartenonId() {
        if (((Repo) this.trade.getProduct()).isTriparty() && isBOTransferReversal(this.creBoTransfer)) {
            BOCre previousCST = BOCreUtils.getInstance().getPreviousCST_S_SETTLED(this.creBoTransfer);
            String oldPartenonTrade = BOCreUtils.getInstance().loadPartenonId(trade, "OldPartenonAccountingID");
            String oldPartenonCre = BOCreUtils.getInstance().loadPartenonIdFromCre(previousCST);
            if (!Util.isEmpty(oldPartenonCre) && !Util.isEmpty(oldPartenonTrade) && oldPartenonTrade.equals(oldPartenonCre)) {
                this.setOriginalEvent("PARTENON_CHANGE");
                this.setPartenonId(oldPartenonTrade);
            } else {
                this.setPartenonId(oldPartenonCre);
            }
        } else {
            BOCreUtils.getInstance().setRepoPartenonAccId(this.boCre, this.trade, this);
        }
    }

    protected boolean isBOTransferReversal(BOTransfer boTransfer) {
        return boTransfer.getLinkedLongId() != 0;
    }
}
