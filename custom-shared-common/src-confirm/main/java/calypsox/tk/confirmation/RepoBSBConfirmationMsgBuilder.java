package calypsox.tk.confirmation;

import calypsox.tk.confirmation.builder.repo.RepoBsbConfirmDataBuilder;
import calypsox.tk.confirmation.model.jaxb.CalypsoConfirmationMsgBean;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;

public class RepoBSBConfirmationMsgBuilder extends RepoConfirmationMsgBuilder{

    RepoBsbConfirmDataBuilder bsbDataBuilder;

    public RepoBSBConfirmationMsgBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
        bsbDataBuilder = new RepoBsbConfirmDataBuilder(boMessage,boTransfer,trade);
    }

    @Override
    public CalypsoConfirmationMsgBean build() {
        super.build();
        setBsbData();
        return messageBean;
    }

    private void setBsbData(){
        addFieldToMsgBean("REPO_BASIS[1]",bsbDataBuilder.buildBasis());
        addFieldToMsgBean("REPO_CUPONCO",bsbDataBuilder.buildCouponCO());
        addFieldToMsgBean("REPO_NET_TOTAL_INI",bsbDataBuilder.buildNetTotalInit());
        addFieldToMsgBean("REPO_UNDRLYING_DIRTY_PRICE_FWD[1]",bsbDataBuilder.buildBsbFwdDirtyPrice());
        addFieldToMsgBean("REPO_CASH_INTEREST",bsbDataBuilder.buildInterest());
        addFieldToMsgBean("REPO_BOND_CUPON",bsbDataBuilder.buildCoupon());
        addFieldToMsgBean("REPO_INDEMNITY",bsbDataBuilder.buildIndemnity());
    }
}
