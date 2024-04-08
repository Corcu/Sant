package calypsox.tk.bo.workflow.rule;


import calypsox.tk.report.util.SecFinanceTradeUtil;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Collateral;
import com.calypso.tk.secfinance.SecFinanceTradeEntry;
import com.calypso.tk.secfinance.SecFinanceTradeEntryContext;
import com.calypso.tk.service.DSConnection;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Optional;
import java.util.Vector;

public class GenerateMXKeywordsTradeRule implements WfTradeRule {
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
        try{
            if(null!=trade) {
                String dirtyPrice = "";
                String price = "";
                SecFinanceTradeEntryContext context = new SecFinanceTradeEntryContext();

                SecFinanceTradeEntry externalSecFinanceTradeEntry = SecFinanceTradeEntry.createSecFinanceTradeEntry(trade, new JDatetime(), null, context);
                if (null != externalSecFinanceTradeEntry) {

                    DisplayValue collMarginValue = externalSecFinanceTradeEntry.getCollMarginValue();
                    String marginValue = null != collMarginValue ? collMarginValue.toString() : "";

                    Collateral collateral = SecFinanceTradeUtil.getInstance().getCollateral(trade);

                    double cleanPrice = null != collateral ? collateral.getInitialPrice() * 100 : 0.0;
                    double dirtyPriceValue = null != collateral ? collateral.getDirtyPrice() * 100 : 0.0;

                    String productFamily = Optional.ofNullable(externalSecFinanceTradeEntry.getCollSecurity()).map(Product::getProductFamily).orElse("");
                    if ("Bond".equalsIgnoreCase(productFamily)) {
                        dirtyPrice = formatValue(dirtyPriceValue);
                    } else {
                        price = formatValue(dirtyPriceValue);
                    }

                    trade.addKeyword("MXInitialEquityPrice", price);
                    trade.addKeyword("MXInitialDirtyPrice", dirtyPrice);
                    trade.addKeyword("MxInitialCleanPrice", formatValue(cleanPrice));
                    trade.addKeyword("MXInitialMargin", String.valueOf(marginValue));
                }
            }
        }catch (Exception e){
            Log.error(this,"Error generating MX Keywords for trade: " + e);
        }
        return true;
    }

    private String formatValue(Double value){
        if(!Double.isNaN(value)){

            final DecimalFormat myFormatter = new DecimalFormat("0.00000000");
            final DecimalFormatSymbols tmp = myFormatter.getDecimalFormatSymbols();
            tmp.setDecimalSeparator(',');
            myFormatter.setDecimalFormatSymbols(tmp);

            String format = myFormatter.format(value);
            return !"NaN".equalsIgnoreCase(format) && !"-0.00".equalsIgnoreCase(format) ? format : "0.00";
        }
        return "0.0";
    }
}
