package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;

public class BOCreSecLendingADJUST_FEE extends SantBOCre {

    public BOCreSecLendingADJUST_FEE(BOCre cre, Trade trade) {
        super(cre, trade);
    }


    @Override
    protected void fillValues() {
        this.partenonId = BOCreUtils.getInstance().loadPartenonId(this.trade);
        this.internal = BOCreUtils.getInstance().isInternal(this.trade);
    }

    @Override
    protected Double getPosition() {
        return null;
    }

    protected long loadLinkedId(){
        if("REVERSAL".equalsIgnoreCase(this.boCre.getEventType())){
            return this.boCre.getId();
        }else{
            return this.boCre.getLinkedId();
        }
    }

    @Override
    protected JDate getCancelationDate() {
        return "CANCELED_TRADE".equalsIgnoreCase(this.boCre.getOriginalEventType()) ? JDate.getNow() : null;
    }

    @Override
    protected String getSubType() {
        return "";
    }

    @Override
    protected String loadIdentifierIntraEOD() {
        return "";
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
    protected String loadSettlementMethod(){
        return "";
    }


}
