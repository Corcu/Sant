
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.tk.util.emir.EmirSnapshotReduxUtil;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.PerformanceSwapLeg;

public class EmirFieldBuilderUNDERLYINGASSETIDTYPE implements EmirFieldBuilder {
    @Override
    /*
    "Nueva logica
    Si somos el Pay del swap entonces informamos este campo con el valor ""LEI""
    Si somos el rec del swap entonces revisamos si la contrapartida tiene LEI. Si lo tiene informamos el valor ""LEI"". Si no lo tiene fijamos el valor ""INTERNAL""."
    */

    public String getValue(Trade trade) {
        String rst = EmirFieldBuilderUtil.getInstance().getLogicUNDERLYNGASSETTYPE(trade);
        return rst;
    }



}
