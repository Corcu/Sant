package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;

public class BOCreSecLendingSECLENDING_FEE extends SantBOCre{

    public BOCreSecLendingSECLENDING_FEE(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void fillValues() {
        this.partenonId = BOCreUtils.getInstance().loadPartenonId(this.trade);
        this.internal = BOCreUtils.getInstance().isInternal(this.trade);
        this.maturityDate = BOCreUtils.getInstance().getMaturityPDVFee(this.boCre,this.trade);
        this.tomadoPrestado = BOCreUtils.getInstance().getTomadoPrestado(this.trade);
    }

    @Override
    protected String loadCreEventType(){
        if(null!= this.boCre){
            if("SECLENDING_FEE_FAILED".equalsIgnoreCase(this.boCre.getEventType())){
                return "SECLENDING_FEE";
            }else{
                return this.boCre.getEventType();
            }
        }
        return "";
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
    protected String loadIdentifierIntraEOD(){
        return "";
    }

    @Override
    protected String loadSettlementMethod(){
        return "";
    }


}
