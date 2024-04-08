package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;

public class BOCreMarginCallCST_NETTING extends BOCreMarginCallCST {

    public BOCreMarginCallCST_NETTING(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected BOTransfer getClientBoTransfer(){
        return BOCreUtils.getInstance().getTransfer(this.boCre);
    }
    @Override
    protected String loadProductType() {
        return "Netting";
    }
    @Override
    protected JDate loadSettlemetDate(){ //TODO settleDate from BOTrasnfer
        return null!=this.creBoTransfer ? this.creBoTransfer.getSettleDate() : null;
    }
    @Override
    protected Integer loadAccountID() {
        return null!=this.creBoTransfer ? this.creBoTransfer.getExternalSettleDeliveryId() : 0;
    }

    //Remove values for Netting Cre
    @Override
    protected JDate loadTradeDate(){ return null; }
    @Override
    protected Long loadTradeId(){ return 0L; }
    @Override
    protected Integer loadContractID() { return 0;}
    @Override
    protected String loadContractType() { return ""; }
    @Override
    protected Double getPosition() { return 0D; }
    @Override
    protected String loadAccountCurrency() { return ""; }
    @Override
    protected String getDebitCredit(double value) { return ""; }
}
