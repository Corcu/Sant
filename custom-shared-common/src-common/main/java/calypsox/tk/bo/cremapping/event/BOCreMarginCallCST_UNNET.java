package calypsox.tk.bo.cremapping.event;

import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;

public class BOCreMarginCallCST_UNNET extends BOCreMarginCallCST {

    public BOCreMarginCallCST_UNNET(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    public void fillValues() {
        super.fillValues();
        this.nettingParent = null!=this.creBoTransfer && !Util.isEmpty(this.nettingType) && !"None".equalsIgnoreCase(this.creBoTransfer.getNettingType()) ? this.creBoTransfer.getNettedTransferLongId() : 0;
    }

}
