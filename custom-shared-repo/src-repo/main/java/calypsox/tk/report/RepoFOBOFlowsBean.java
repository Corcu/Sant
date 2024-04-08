package calypsox.tk.report;

import calypsox.tk.report.util.SecFinanceTradeUtil;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Cash;
import com.calypso.tk.product.SecFinance;

public class RepoFOBOFlowsBean {

    String contractId = "";
    String rootContractId = "";
    String externalNumber = "";
    boolean refIndex = false;
    String referenceIndex = "";

    public void init(Trade trade){

        boolean internal = SecFinanceTradeUtil.getInstance().isInternal(trade);
        boolean mirror = isMirror(trade);

        if(trade.getProduct() instanceof SecFinance){
            Cash cash = ((SecFinance) trade.getProduct()).getCash();
            if(null!=cash){
                refIndex = "Flotante".equalsIgnoreCase(SecFinanceTradeUtil.getInstance().getCashRateType(cash));
                if(refIndex){
                    referenceIndex = SecFinanceTradeUtil.getInstance().getIndexName(cash);
                }
            }
        }
        contractId = trade.getKeywordValue("MurexTradeID");
        rootContractId = trade.getKeywordValue("MurexRootContract");
        externalNumber = trade.getKeywordValue("MurexGlobalId");


        if(internal && mirror){
            contractId = "-"+contractId;
            rootContractId = "-"+rootContractId;
        }
    }

    public String getReferenceIndex() {
        return referenceIndex;
    }

    public void setReferenceIndex(String refernceIndex) {
        this.referenceIndex = refernceIndex;
    }

    public boolean isRefIndex() {
        return refIndex;
    }

    public void setRefIndex(boolean refIndex) {
        this.refIndex = refIndex;
    }

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public String getRootContractId() {
        return rootContractId;
    }

    public void setRootContractId(String rootContractId) {
        this.rootContractId = rootContractId;
    }

    public String getExternalNumber() {
        return externalNumber;
    }

    public void setExternalNumber(String externalNumber) {
        this.externalNumber = externalNumber;
    }

    private boolean isMirror(Trade trade){
        return null!=trade && trade.getMirrorTradeLongId()!=0 && trade.getLongId()>trade.getMirrorTradeLongId();
    }


}
