package calypsox.tk.bo.cremapping.event.old;

import calypsox.tk.bo.cremapping.event.SantBOCre;
import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.CashFlow;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;

import java.util.TimeZone;

public class BOCreOldRepoACCRUAL extends SantBOCre {

    private CashFlow cashFlow;

    public BOCreOldRepoACCRUAL(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void init() {
        super.init();
        this.cashFlow = BOCreUtils.getInstance().getRepoCashFlow(this.trade,this.boCre);
    }

    @Override
    protected void fillValues() {
        this.creDescription = "Accrual";
        this.maturityDate = null!=cashFlow ? cashFlow.getEndDate() : null;
        BOCreUtils.getInstance().setRepoPartenonAccId(this.boCre,this.trade,this);
        this.internal = BOCreUtils.getInstance().isInternal(this.trade);
        this.tomadoPrestado = BOCreUtils.getInstance().getTomadoPrestado(this.trade);
        this.sentDateTime = JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault());

    }

    @Override
    protected JDate loadSettlemetDate(){
        return null!=cashFlow ? cashFlow.getDate() : null;
    }

    @Override
    protected JDate loadTradeDate() {
        return null!=cashFlow ? cashFlow.getStartDate() : null;
    }

    @Override
    protected Double getPosition() {
        return null;
    }

    @Override
    protected JDate getCancelationDate() {
        String originalEventType = super.loadOriginalEventType();
        if("CANCELED_TRADE".equalsIgnoreCase(originalEventType)){
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
        return "EOD";
    }

    @Override
    protected String loadSettlementMethod(){
        return "";
    }

}
