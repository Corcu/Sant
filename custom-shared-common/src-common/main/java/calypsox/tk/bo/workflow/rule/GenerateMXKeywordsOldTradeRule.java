package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.Task;
        import com.calypso.tk.bo.TaskWorkflowConfig;
        import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.*;
import com.calypso.tk.secfinance.SecFinanceTradeEntry;
        import com.calypso.tk.secfinance.SecFinanceTradeEntryContext;
        import com.calypso.tk.service.DSConnection;
        import com.calypso.tk.util.fieldentry.FieldEntry;

        import java.util.Optional;
        import java.util.Vector;

public class GenerateMXKeywordsOldTradeRule  implements WfTradeRule {
    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Generate Trade Keywords MXInitialEquityPrice , MXInitialDirtyPrice , MXInitialMargin ";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        try {
            if(null!=trade) {
                String dirtyPrice = "";
                String price = "";
                SecFinanceTradeEntryContext context = new SecFinanceTradeEntryContext();
                SecFinanceTradeEntry externalSecFinanceTradeEntry = SecFinanceTradeEntry.createSecFinanceTradeEntry(trade, new JDatetime(), null, context);
                if (null != externalSecFinanceTradeEntry) {

                    DisplayValue collMarginValue = externalSecFinanceTradeEntry.getCollMarginValue();
                    String marginValue = null != collMarginValue ? collMarginValue.toString() : "";
                    DisplayValue collDirtyPriceValue = externalSecFinanceTradeEntry.getCollDirtyPriceValue();
                    String dirtyPriceValue = null != collDirtyPriceValue ? collDirtyPriceValue.toString() : "";
                    DisplayValue collCleanPriceValue = externalSecFinanceTradeEntry.getCollCleanPriceValue();
                    String cleanPrice = null != collCleanPriceValue ? collCleanPriceValue.toString() : "";

                    String productFamily = Optional.ofNullable(externalSecFinanceTradeEntry.getCollSecurity()).map(Product::getProductFamily).orElse("");
                    if ("Bond".equalsIgnoreCase(productFamily)) {
                        dirtyPrice = String.valueOf(dirtyPriceValue);
                    } else {
                        price = String.valueOf(dirtyPriceValue);
                    }

                    trade.addKeyword("MXInitialEquityPrice", price);
                    trade.addKeyword("MXInitialDirtyPrice", dirtyPrice);
                    trade.addKeyword("MxInitialCleanPrice", String.valueOf(cleanPrice));
                    trade.addKeyword("MXInitialMargin", String.valueOf(marginValue));
                }
            }
        }catch (Exception e){
            Log.error(this,"Error:" + e);
        }
        return true;
    }
}
