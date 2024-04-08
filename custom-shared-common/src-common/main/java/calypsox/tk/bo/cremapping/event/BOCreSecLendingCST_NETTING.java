package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.Trade;

public class BOCreSecLendingCST_NETTING extends BOCreMarginCallCST_NETTING {
    public BOCreSecLendingCST_NETTING(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    public void fillValues() {
        super.fillValues();
        this.internal = BOCreUtils.getInstance().isInternal(this.trade);
    }
}
