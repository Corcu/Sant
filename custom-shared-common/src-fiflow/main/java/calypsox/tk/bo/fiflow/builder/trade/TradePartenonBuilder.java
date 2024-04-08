package calypsox.tk.bo.fiflow.builder.trade;

import calypsox.tk.bo.fiflow.builder.handler.FIFlowTransferNetHandler;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class TradePartenonBuilder {

    //00491999406BBBCMDM626

    private String empr="";
    private String cent="";
    private String productType="";
    private String productSubType="";
    private String partenonCode="";

    private String fullPartenonCode="";

    public TradePartenonBuilder(FIFlowTransferNetHandler handler) {
        String partenonKwd=Optional.ofNullable(handler.getTradeKwdFromTransfer(getPartenonKwdName()))
                .map(kwds->kwds.get(getPartenonKwdName())).orElse("");
        initPartenonValues(partenonKwd);
    }

    public TradePartenonBuilder(Trade trade){
        String partenonKwd=Optional.ofNullable(trade).map(t -> t.getKeywordValue(getPartenonKwdName())).orElse("");
        initPartenonValues(partenonKwd);
    }
    public TradePartenonBuilder(String fullPartenonCode){
        initPartenonValues(fullPartenonCode);
    }


    private void initPartenonValues(String partenonKwd){
        this.fullPartenonCode=partenonKwd;
        if(!Util.isEmpty(partenonKwd)&&21==partenonKwd.length()){
            this.empr=partenonKwd.substring(0,4);
            this.cent=partenonKwd.substring(4,8);
            this.productType=partenonKwd.substring(8,11);
            this.productSubType=partenonKwd.substring(18,21);
            this.partenonCode=partenonKwd.substring(11,18);
        }
    }

    public String buildCodEmprField() {
        return this.empr;
    }

    public String buildCodCentField() {
        return this.cent;
    }

    public String buildCodProdField() {
        return this.productType;
    }

    public String buildCodsProdField() {
        return this.productSubType;
    }

    public String buildCdoPerboField() {
        return this.partenonCode;
    }

    public String buildFullPartenon(){
        return this.fullPartenonCode;
    }

    protected String getPartenonKwdName(){
        return "PartenonAccountingID";
    }
    public boolean isEmptyPartenon(){
        return this.partenonCode.isEmpty() && this.productType.isEmpty();
    }
}
