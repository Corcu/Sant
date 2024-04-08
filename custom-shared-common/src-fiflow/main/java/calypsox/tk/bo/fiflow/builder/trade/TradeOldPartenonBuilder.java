package calypsox.tk.bo.fiflow.builder.trade;

import calypsox.tk.bo.fiflow.builder.handler.FIFlowTransferNetHandler;

/**
 * @author aalonsop
 */
public class TradeOldPartenonBuilder extends TradePartenonBuilder{


    public TradeOldPartenonBuilder(FIFlowTransferNetHandler handler) {
        super(handler);
    }

    @Override
    protected String getPartenonKwdName(){
        return "OldPartenonAccountingID";
    }
}
