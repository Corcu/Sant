package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;

import java.util.TimeZone;

public class BOCreRepoMTM_NET extends SantBOCre{

    public BOCreRepoMTM_NET(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void fillValues() {
        this.creDescription = "Mark-to-Market Net";
        BOCreUtils.getInstance().setRepoPartenonAccId(this.boCre,this.trade,this);
        this.internal = BOCreUtils.getInstance().isInternal(this.trade);
        this.tomadoPrestado = BOCreUtils.getInstance().getTomadoPrestado(this.trade);
        this.sentDateTime = JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault());
        this.maturityDate = BOCreUtils.getInstance().getRepoMaturityDate(this.trade);
        this.underlyingType = BOCreUtils.getInstance().getMaturityType(this.trade);
    }

    @Override
    protected Double getPosition() {
        return null;
    }

    @Override
    protected JDate getCancelationDate() {
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
