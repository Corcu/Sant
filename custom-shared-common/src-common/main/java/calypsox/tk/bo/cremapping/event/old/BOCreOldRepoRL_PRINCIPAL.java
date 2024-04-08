package calypsox.tk.bo.cremapping.event.old;

import calypsox.tk.bo.cremapping.event.SantBOCre;
import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;

import java.util.List;
import java.util.TimeZone;

public class BOCreOldRepoRL_PRINCIPAL extends SantBOCre {

    public BOCreOldRepoRL_PRINCIPAL(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void fillValues() {
        this.creDescription = "Return Leg Principal";
        this.nettingType = null!=this.creBoTransfer && !Util.isEmpty(this.creBoTransfer.getNettingType()) ? this.creBoTransfer.getNettingType() : "";
        this.nettingParent = null!=this.creBoTransfer && !Util.isEmpty(this.nettingType) && !"None".equalsIgnoreCase(this.creBoTransfer.getNettingType()) ? this.creBoTransfer.getLongId() : 0;
        BOCreUtils.getInstance().setRepoPartenonAccId(this.boCre,this.trade,this);
        this.internal = BOCreUtils.getInstance().isInternal(this.trade);
        this.tomadoPrestado = BOCreUtils.getInstance().getTomadoPrestado(this.trade);
        this.nettingNumber = "2";
        this.sentDateTime = JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault());
        this.maturityDate = BOCreUtils.getInstance().getRepoMaturityDate(this.trade);
    }

    @Override
    protected JDate getCancelationDate() {
        String originalEventType = super.loadOriginalEventType();
        List<String> listevents = Util.stringToList("CANCELED_SEC_PAYMENT,CANCELED_SEC_RECEIPT,CANCELED_RECEIPT,CANCELED_PAYMENT");

        if(listevents.contains(originalEventType)){
            return JDate.getNow();
        }

        return null;
    }

    @Override
    protected Double getPosition() {
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
        return "INTRADAY";
    }

    @Override
    protected String loadSettlementMethod(){
        return "";
    }

}
