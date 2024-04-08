package calypsox.tk.bo.cremapping.event;


import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Equity;


public class BOCreEquityCOT_REV extends BOCreEquity {


    public BOCreEquityCOT_REV(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    public void fillValues() {
        super.fillValues();
        this.multiccy = BOCreUtils.getInstance().loadEquityMulticcy(trade);
        if("Y".equalsIgnoreCase(this.multiccy)){
            this.amount2 = BOCreUtils.getInstance().loadEquityMulticcyAmount2(trade, (Equity)trade.getProduct());
            this.currency2 = BOCreUtils.getInstance().loadEquityMulticcyCurrency2(trade);
            this.amount3 = BOCreUtils.getInstance().loadEquityMulticcyAmount3(trade, (Equity)trade.getProduct());
            this.currency3 = BOCreUtils.getInstance().loadEquityMulticcyCurrency3(trade);;
        }
    }

}
