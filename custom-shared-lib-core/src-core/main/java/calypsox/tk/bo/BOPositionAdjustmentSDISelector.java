/*
 *
 * Copyright (c) 2000 by Calypso Technology, Inc.
 * 595 Market Street, Suite 1980, San Francisco, CA  94105, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Calypso Technology, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Calypso Technology.
 *
 */

package calypsox.tk.bo;

import com.calypso.tk.bo.*;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

/**
 * This is the SDI validator for the BOPositionAdjustment trades. In the Payment Report Window when you click on the
 * process Validate SDI's The system will pick up the SDI validator from the tk.bo directory
 */
public class BOPositionAdjustmentSDISelector extends SDISelectorUtil implements SDISelector {

    public static final String KW_ACCOUNT_NUMBER = "AccountNumber";
    public static final String KW_INVENTORY_AGENT = "InventoryAgent";

    /**
     * Returns a list of Valid SDI List
     *
     * @param trade               Trade related.
     * @param transfer            Trade Transfer Rule
     * @param settleDate          Settle Date
     * @param legalEntity         Legal Entity Code
     * @param legalEntityRole     Role of the Legal Entity
     * @param exceptions          Vector of BO Exceptions
     * @param includeNotPreferred Restrict choice to preferred.
     * @param dsCon               A Data Server Connection
     * @return a Vector of SettleDeliveryInstruction objects or null if none found.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Vector getValidSDIList(final Trade trade, final TradeTransferRule transfer, final JDate settleDate,
                                  final String legalEntity, final String legalEntityRole, final Vector exceptions,
                                  final boolean includeNotPreferred, final DSConnection dsCon) {

        final Vector sdis = super.getValidSDIList(trade, transfer, settleDate, legalEntity, legalEntityRole,
                exceptions, includeNotPreferred, dsCon);
        if (trade == null) {
            return sdis;
        }
        if ((sdis == null) || (sdis.size() == 0)) {
            final BOException excp = new BOException(trade.getLongId(), "BOPositionAdjustmentSDISelector",
                    "No valid SDIs for this Trade");
            exceptions.addElement(excp);
            return sdis;
        }

        final Vector validSdis = new Vector<SettleDeliveryInstruction>();

        final String inventoryAgent = trade.getKeywordValue(KW_INVENTORY_AGENT);
        final String accountNumber = trade.getKeywordValue(KW_ACCOUNT_NUMBER);

        if (Util.isEmpty(inventoryAgent) || Util.isEmpty(accountNumber)) {
            BOException excp;
            excp = new BOException(trade.getLongId(), "BOPositionAdjustmentSDISelector", String.format(
                    "Trade keyword %s and/or %s are missing", KW_ACCOUNT_NUMBER, KW_INVENTORY_AGENT));
            exceptions.addElement(excp);
            return validSdis;
        }
        for (final SettleDeliveryInstruction sdi : (Vector<SettleDeliveryInstruction>) sdis) {

            final String agent = BOCache.getLegalEntityCode(dsCon, sdi.getAgentId());
            if (null == agent) {
                continue;
            }

            final String account = sdi.getAgentAccount();

            if (Util.isEmpty(account)) {
                continue;
            }

            if (accountNumber.equals(account) && inventoryAgent.equals(agent)) {
                validSdis.add(sdi);
            }

        }

        if (Util.isEmpty(validSdis)) {
            BOException excp;

            excp = new BOException(trade.getLongId(), "BOPositionAdjustmentSDISelector",
                    "No valid SDIs (matching Trade keywords) for this Trade");
            exceptions.addElement(excp);
        }
        return validSdis;
    }
}
