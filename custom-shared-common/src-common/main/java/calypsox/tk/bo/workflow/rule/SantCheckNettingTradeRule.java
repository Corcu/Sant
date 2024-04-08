package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.*;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;

import java.util.*;

public class SantCheckNettingTradeRule implements WfTradeRule {
    @Override
    public boolean check(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        Vector<?> customTransferRules = trade.getCustomTransferRuleB() ? trade.getTransferRules() : new Vector<>();

        BOProductHandler handler = BOProductHandler.getHandler(trade.getProduct());
        Vector<String> errors = new Vector<>();
        Vector<?> generated = handler.generateTransferRules(trade, trade.getProduct(), errors, dsCon);

        List<TradeTransferRule> transferRules = merge(customTransferRules, generated);

        Optional<TradeTransferRule> dfpRule = transferRules.stream().filter(r -> {
            if ("NONE".equals(r.getNettingType()) || "DAP".equals(r.getDeliveryType()) || "SECURITY".equals(r.getTransferType()))
                return false;
            else {
                HashMap<String, String> nettingKey = BOCache.getNettingConfig(dsCon, r.getNettingType());

                return nettingKey.containsKey("AllowCashSecurityMix") || nettingKey.containsKey("AllowCashSecurityMixDiffCcy");
            }
        }).findAny();

        if (dfpRule.isPresent()) {
            messages.add(String.format("Transfer rule %s, DFP cash netted with DAP SECURITY may cause problems with SPLIT. Correct SDI and reprocess trade.", dfpRule.get()));
            return false;
        }

        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<TradeTransferRule> merge(Vector customRules, Vector defaultRules) {
        if (Util.isEmpty(customRules))
            return new ArrayList<>(defaultRules);

        final List<TradeTransferRule> merged = new ArrayList<>(customRules);
        defaultRules.forEach(r -> {
            if (!customRules.contains(r))
                merged.add((TradeTransferRule) r);
        });

        return merged;
    }

    @Override
    public String getDescription() {
        return "Workaround for a Calypso issue. Calypso incorrectly splits netted SECURITY transfers with DAP PAYMENT/RECEIPT underlyings causing breaks in inventory.\n" +
                "FALSE is netting contains SECURITY and DFP cash transfers, TRUE otherwise. Intended for use on SPLIT action.";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }
}
