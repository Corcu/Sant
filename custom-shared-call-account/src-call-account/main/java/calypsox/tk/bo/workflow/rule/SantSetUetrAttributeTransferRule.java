/*
 *
 * Copyright (c) ISBAN: Ingenierna de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.bo.workflow.rule;

import calypsox.tk.core.KeywordConstantsUtil;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTransferRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;

import java.util.UUID;
import java.util.Vector;

public class SantSetUetrAttributeTransferRule implements WfTransferRule {

    /**
     * Description of the rule.
     * 
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Set the UETR Attribute to the Transfer with a UUID code.";
    }

    /**
     * Return true.
     * 
     * @param wc
     *            the wc
     * @param transfer
     *            the transfer
     * @param oldTransfer
     *            the old transfer
     * @param trade
     *            the trade
     * @param messages
     *            the messages
     * @param dsCon
     *            the ds con
     * @param excps
     *            the excps
     * @param task
     *            the task
     * @param dbCon
     *            the db con
     * @param events
     *            the events
     * @return true, if successful
     */
    @SuppressWarnings({ "rawtypes" })
    @Override
    public boolean check(final TaskWorkflowConfig wc,
            final BOTransfer transfer, final BOTransfer oldTransfer,
            final Trade trade, final Vector messages, final DSConnection dsCon,
            final Vector excps, final Task task, final Object dbCon,
            final Vector events) {
        return true;
    }

    /**
     * Create the attribute 'UETR' with a UUID code.
     * 
     * @param wc
     *            the wc
     * @param transfer
     *            the transfer
     * @param oldTransfer
     *            the old transfer
     * @param trade
     *            the trade
     * @param messages
     *            the messages
     * @param dsCon
     *            the ds con
     * @param excps
     *            the excps
     * @param task
     *            the task
     * @param dbCon
     *            the db con
     * @param events
     *            the events
     * @return true, if successful.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public boolean update(final TaskWorkflowConfig paramTaskWorkflowConfig,
            final BOTransfer transfer, final BOTransfer paramBOTransfer2,
            final Trade paramTrade, final Vector paramVector1,
            final DSConnection paramDSConnection, final Vector paramVector2,
            final Task paramTask, final Object paramObject,
            final Vector paramVector3) {

        final boolean isDirect = "Direct".equalsIgnoreCase(transfer
                .getSettlementMethod());
        final boolean isPayment = "PAY".equalsIgnoreCase(transfer
                .getPayReceiveType()) ? true : false;
      
        final String uetrAtt = transfer.getAttribute(KeywordConstantsUtil.TRANSFER_ATTRIBUTE_UETR);
        
        if ((transfer.getTradeLongId() == 0)
                || (!isDirect && isPayment  && Util.isEmpty(uetrAtt))) {

            final UUID uuid = UUID.randomUUID();
            transfer.setAttribute(KeywordConstantsUtil.TRANSFER_ATTRIBUTE_UETR,
                    uuid.toString());
        }
        
        return true;
    }

}
