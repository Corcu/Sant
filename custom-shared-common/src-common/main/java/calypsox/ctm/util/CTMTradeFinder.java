package calypsox.ctm.util;

import com.calypso.tk.core.*;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.mo.TradeFilterCriterion;
import com.calypso.tk.mo.TradeKeywordFilterCriterion;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;
import com.calypso.ui.component.condition.ConditionTree;
import com.calypso.ui.component.condition.ConditionTreeNode;

import java.util.Optional;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class CTMTradeFinder {

    public static Trade findIONBlockTrade(String blockTradeReference){
        return findBlockTrade(blockTradeReference,CTMUploaderConstants.TRADE_KEYWORD_MX_GLOBAL_ID);
    }

    public static Trade findCTMBlockTrade(String blockTradeReference){
        return findBlockTrade(blockTradeReference,CTMUploaderConstants.TRADE_KEYWORD_MXROOTCONTRACT);
    }

    public static Trade findVanillaBlockTrade(String allocatedFromValue){
        Trade trade=null;
        long longId=Optional.ofNullable(allocatedFromValue)
                .filter(id -> !id.isEmpty())
                .map(Long::parseLong)
                .orElse(0L);
        if(longId>0) {
            try {
                trade = DSConnection.getDefault().getRemoteTrade().getTrade(longId);
            } catch (CalypsoServiceException exc) {
                Log.error(CTMTradeFinder.class.getSimpleName(), exc.getCause());
            }
        }
        return trade;
    }

    private static Trade findBlockTrade(String blockTradeReference, String referenceName){
        Trade blockTrade=null;
        TradeFilter tradeFilter=new TradeFilter("CTM Block Trade TradeFilter",
                "Used to search trades by "+referenceName+" kwd");
        tradeFilter.addCriterion(buildTradeKwdFilterCriterion(blockTradeReference,referenceName));
        tradeFilter.addCriterion(buildTradeStatusFilterCriterion());
        try {
            TradeArray trades= DSConnection.getDefault().getRemoteTrade().getTrades(tradeFilter,new JDatetime());
            blockTrade=Optional.ofNullable(trades)
                    .filter(tradeArray->!tradeArray.isEmpty())
                    .map(TradeArray::firstElement)
                    .orElse(null);
        } catch (CalypsoServiceException exc) {
            Log.error(CTMTradeFinder.class.getSimpleName(),exc.getCause());
        }
        return blockTrade;
    }

    private static TradeFilterCriterion buildTradeKwdFilterCriterion(String kwdValue,String referenceName) {
        ConditionTreeNode childTreeNode=new ConditionTreeNode("==", referenceName, new String[]{kwdValue});
        ConditionTreeNode appendingTreeNode=new ConditionTreeNode("&&", null, new Object[]{childTreeNode});
        ConditionTree conditionTree=new ConditionTree();
        conditionTree.setName("KeywordTradeFilter");
        conditionTree.getRoot().setOperands(new Object[]{appendingTreeNode});
        TradeFilterCriterion tfc = new TradeKeywordFilterCriterion(conditionTree);
        tfc.setIsInB(true);
        return tfc;
    }

    private static TradeFilterCriterion buildTradeStatusFilterCriterion() {
        Vector<String> statuses=new Vector<>();
        statuses.add(Status.VERIFIED);
        statuses.add("ALLOCATED");
        statuses.add("DUMMY_FULL_ALLOC");
        statuses.add("PARTIAL_ALLOC");

        TradeFilterCriterion tfc = new TradeFilterCriterion(Status.class.getSimpleName());
        tfc.setIsInB(true);
        tfc.setValues(statuses);
        return tfc;
    }


}
