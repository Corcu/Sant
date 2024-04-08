
package calypsox.tk.report.emir.field;

import calypsox.tk.core.SantanderUtil;
import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;

public class EmirFieldBuilderCONFIRMTYPE implements EmirFieldBuilder {

    /*
    Nueva logica
        - Si la operación no esta confirmada debemos enviar "Not Confirmed".
            - Si la operación esta confirmada (pte de definir keyword a crear con el valor correpondientei) se informa el literal "Non-Electronic".
    */
    @Override
    public String getValue(Trade trade) {


        return EmirFieldBuilderUtil.getInstance().getLogicConfirmType(trade);
    }
}
