package calypsox.tk.event;

import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTrade;
import com.calypso.tk.product.Repo;

public class SantExcludeReposTradesEventFilter implements EventFilter {
    @Override
    public boolean accept(PSEvent event) {

        if (event instanceof PSEventTrade) {
            Trade trade = ((PSEventTrade) event).getTrade();
            if (trade.getProduct() instanceof Repo && (isPartenonStatus(trade) || !isRepoMurexWorkflowSubType(trade))) {
                return false;
            }
        }
        return true;
    }

    private boolean isRepoMurexWorkflowSubType(Trade trade) {
        if (!Util.isEmpty(trade.getKeywordValue("WorkflowSubType"))
                && trade.getKeywordValue("WorkflowSubType").equalsIgnoreCase("RepoMurex")) {
            return true;
        }
        return false;
    }

    private boolean isPartenonStatus(Trade trade) {
        if ("PARTENON".equalsIgnoreCase(trade.getStatus().getStatus())) {
            return true;
        }
        return false;
    }
}
