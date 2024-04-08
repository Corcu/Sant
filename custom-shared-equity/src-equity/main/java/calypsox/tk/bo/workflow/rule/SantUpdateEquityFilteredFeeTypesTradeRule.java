package calypsox.tk.bo.workflow.rule;


import com.calypso.tk.bo.Fee;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Iterator;
import java.util.Vector;


public class SantUpdateEquityFilteredFeeTypesTradeRule implements WfTradeRule {


    private static String FILTER = "SDF_FILTER_EQUITY_FEES";
    private static String DV_FEE_TYPES_TO_FILTER = "EquityFeeTypesToFilter";
    private static String FEE_TYPE_NO_SETTLE = "BRK_FEE_NO_SETTLE";



    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }


    @Override
    public String getDescription() {
        return "";
    }


    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        StaticDataFilter sdf = null;
        try {
            sdf = DSConnection.getDefault().getRemoteReferenceData().getStaticDataFilter(FILTER);
            if (sdf != null && sdf.accept(trade)) {
                Vector<Fee> fees = trade.getFeesList();
                if(fees!=null && fees.size()>0) {
                    Vector<String> feeTypesToFilter = LocalCache.getDomainValues(DSConnection.getDefault(), DV_FEE_TYPES_TO_FILTER);
                    Iterator<Fee> feeIt = fees.iterator();
                    while(feeIt.hasNext()) {
                        Fee fee = feeIt.next();
                        if(feeTypesToFilter!=null && feeTypesToFilter.size()>0){
                            if(feeTypesToFilter.contains(fee.getType())) {
                                fee.setType(FEE_TYPE_NO_SETTLE);
                            }
                        }
                        else{
                            fee.setType(FEE_TYPE_NO_SETTLE);
                        }
                    }
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this.getClass(), "the SDF does not exist");
        }
        return true;
    }


}
