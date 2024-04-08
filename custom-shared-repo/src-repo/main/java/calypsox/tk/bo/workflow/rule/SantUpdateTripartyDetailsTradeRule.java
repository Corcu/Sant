package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.bo.swift.MT527Helper;
import com.calypso.tk.bo.workflow.rule.UpdateTripartyDetailsTradeRule;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;
import java.util.Vector;

public class SantUpdateTripartyDetailsTradeRule extends UpdateTripartyDetailsTradeRule {


    public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        boolean ret = super.update(wc, trade, oldTrade, messages, dsCon, excps, task, dbCon, events);

        if (ret) {
            try {
                final TradeTransferRule secXferRule = MT527Helper.getStartLegTransferRule(trade, DSConnection.getDefault(), "SECURITY");
                if (Optional.ofNullable(secXferRule).isPresent()) {
                    final SettleDeliveryInstruction settleDeliveryInstruction = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), secXferRule.getProcessingOrgSDId());
                    if (Optional.ofNullable(settleDeliveryInstruction).isPresent()) {
                        final LegalEntity legalEntity = BOCache.getLegalEntity(DSConnection.getDefault(), settleDeliveryInstruction.getAgentId());
                        String tripartyAgent = Optional.ofNullable(legalEntity).map(LegalEntity::getCode).orElse("");
                        trade.addKeyword("TripartyAgent", tripartyAgent);
                    }
                }
            } catch (Exception e) {
                Log.error(this, "Error getting Repo TripartyAgent: " + e);
            }
        }
        return ret;
    }

}