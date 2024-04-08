package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.tk.bo.cremapping.util.RepoBOCreSettleMethodHandler;
import com.calypso.tk.bo.BOCre;
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

public class BOCreRepoCST_UNNET extends SantBOCre{

    public BOCreRepoCST_UNNET(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void fillValues() {
        this.creDescription = "Cash Settlement Unnet";
        this.settlementMethod = new RepoBOCreSettleMethodHandler().getCreSettlementMethod(this.creBoTransfer);
        this.transferAccount = getInstance().getTransferAccount(this.settlementMethod,this.creBoTransfer);
        this.nettingType = getInstance().getNettingType(this.creBoTransfer);
        this.nettingParent = null!=this.creBoTransfer && !Util.isEmpty(this.nettingType) && !"None".equalsIgnoreCase(this.creBoTransfer.getNettingType()) ? this.creBoTransfer.getNettedTransferLongId() : 0;
        //BOCreUtils.getInstance().setRepoPartenonAccId(this.boCre,this.trade,this);
        this.loadPartenonId();
        this.internal = BOCreUtils.getInstance().isInternal(this.trade);
        this.tomadoPrestado = BOCreUtils.getInstance().getTomadoPrestado(this.trade);
        this.sentDateTime = JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault());
        this.underlyingType = BOCreUtils.getInstance().getMaturityType(this.trade);
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

    public void loadPartenonId() {
        if (BOCreUtils.getInstance().isPartenonChange(boCre, trade)) {
            BOCre boCreNew = BOCreUtils.getInstance().getLinkCre(this.creLinkedId);
            if (boCreNew != null && !Util.isEmpty(BOCreUtils.getInstance().loadPartenonIdFromCre(boCreNew))) {
                String oldPartenonTrade = BOCreUtils.getInstance().loadPartenonId(trade, "OldPartenonAccountingID");
                String oldPartenonCre = BOCreUtils.getInstance().loadPartenonIdFromCre(boCreNew);
                if (oldPartenonTrade.equals(oldPartenonCre)) {
                    this.setOriginalEvent("PARTENON_CHANGE");
                    this.setPartenonId(oldPartenonTrade);
                } else {
                    this.setPartenonId(oldPartenonCre);
                }
            }
        } else {
            this.setPartenonId(BOCreUtils.getInstance().loadPartenonId(this.trade));
        }
    }
}
