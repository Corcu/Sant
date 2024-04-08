package calypsox.tk.bo.workflow.rule;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;

import java.util.ArrayList;
import java.util.Vector;

public class CheckCollateralExcludeTradeRule implements WfTradeRule {
    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        final String desc = "Adds true to the CollateralExclude keyword if it does not meet the SDF, and removes it if it does.";
        return desc;
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {

        if (trade == null) {
            return false;
        }
        Log.debug("CheckCollateralExcludeKeywordTradeRule", "Trade id = " + trade.getLongId());

        try {
            ArrayList<String> errorMsgs = new ArrayList<String>();
            CollateralConfig marginCallConfig = CollateralUtilities.getMarginCallConfig(trade, errorMsgs);
                checkCollateralExclude(marginCallConfig, trade);

        } catch (final Exception e) {
            Log.error("CheckCollateralExcludeKeywordTradeRule", e);
            return false;
        }

        return true;
    }

    private void checkCollateralExclude(CollateralConfig marginCallConfig, Trade trade) {
        if (marginCallConfig == null || !isMccSDFAccepted(marginCallConfig, trade) || marginCallConfig.getAgreementStatus().equals("CLOSED")) {
            trade.addKeyword("CollateralExclude", "true");
            Log.debug("CheckCollateralExcludeKeywordTradeRule", "Filling CollateralExclude keyword");
        } else {
            if (!Util.isEmpty(trade.getKeywordValue("CollateralExclude"))) {
                trade.removeKeyword("CollateralExclude");
                Log.debug("CheckCollateralExcludeKeywordTradeRule", "Removing CollateralExclude keyword");
            }
        }
    }

    private boolean isMccSDFAccepted(CollateralConfig marginCallConfig, Trade trade) {
        StaticDataFilter sdf = StaticDataFilter.valueOf(marginCallConfig.getProdStaticDataFilterName());
        return sdf != null && sdf.accept(trade);
    }
}
