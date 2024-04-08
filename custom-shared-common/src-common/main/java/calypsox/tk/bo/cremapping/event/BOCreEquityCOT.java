package calypsox.tk.bo.cremapping.event;


import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Equity;


public class BOCreEquityCOT extends BOCreEquity {


    public BOCreEquityCOT(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    public void fillValues() {
        super.fillValues();
        this.multiccy = BOCreUtils.getInstance().loadEquityMulticcy(trade);
    }

}
