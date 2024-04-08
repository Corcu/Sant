package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

public class BOCreCustomerXferPairOffCST_UNNET extends BOCreCustomerTransferCST_UNNET {

    public BOCreCustomerXferPairOffCST_UNNET(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    public void fillValues() {
        super.fillValues();
        this.internal = "NO";
        this.creDescription = this.creBoTransfer!=null ? this.creBoTransfer.getTransferType() : "";
        this.partenonId = BOCreUtils.getInstance().loadPartenonId(this.trade);
        this.tradeId = null!=this.trade ? this.trade.getLongId() : 0L;
        this.nettingParent = null!=this.creBoTransfer && !Util.isEmpty(this.nettingType) && !"None".equalsIgnoreCase(this.creBoTransfer.getNettingType()) ? this.creBoTransfer.getNettedTransferLongId() : 0;
    }

    @Override
    protected String getSubType() {
        return null!=creBoTransfer ? this.creBoTransfer.getTransferType() : "";
    }

    /*
    public CollateralConfig getContract() {
        return null;
    }

    protected Account getAccount() {
        return null;
    }

     */
}
