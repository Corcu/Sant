
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.*;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.product.SwapLeg;
import org.jfree.util.Log;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class EmirFieldBuilderSETTLEMENTDATE implements EmirFieldBuilder {
    @Override
    /*
    "Nueva logica
    Si somos el Pay del swap entonces informamos este campo con el valor ""LEI""
    Si somos el rec del swap entonces revisamos si la contrapartida tiene LEI. Si lo tiene informamos el valor ""LEI"". Si no lo tiene fijamos el valor ""INTERNAL""."
    */

    public String getValue(Trade trade) {
        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;

        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd",
                Locale.getDefault());

        JDate maxSettleDate = null;

        if (trade.getProduct() instanceof PerformanceSwap)  {
            SwapLeg pLeg = EmirFieldBuilderUtil.getInstance().getSwapLeg(trade);
            if(pLeg != null) {
                CashFlowSet flows = pLeg.getFlows();
                if (flows != null) {
                    try  {
                        for (CashFlow cf : flows) {
                            JDate cfDate = cf.getDate();
                            if (cfDate != null) {
                                if (maxSettleDate  == null)  {
                                    maxSettleDate = cfDate;
                                    continue;
                                }
                                if (maxSettleDate.lte(cfDate)) {
                                    maxSettleDate = cfDate;
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.error(this, e);
                    }
                }
            }
        }

        if (maxSettleDate != null) {
            rst =sdf.format(maxSettleDate.getDate());
        }
        return rst;
    }

}
