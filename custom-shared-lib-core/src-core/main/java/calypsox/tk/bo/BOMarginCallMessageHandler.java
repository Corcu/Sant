/*
 * $Id: BOMarginCallMessageHandler.java,v 1.2.4.3 2016/12/05 16:28:20 xIS15672 Exp $
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

import java.util.List;

import com.calypso.tk.bo.BOMessageHandler;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.CashFlow;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.refdata.AdviceConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageArray;

public class BOMarginCallMessageHandler extends BOMessageHandler {
/*
	@Override
	@SuppressWarnings("rawtypes")
	public MessageArray generateBOMessages(AdviceConfig config, LegalEntity leReceiver, LegalEntity leSender,
			Trade trade, BOTransfer transfer, PSEvent event, Vector exceptions, DSConnection dsCon) {

		if ((transfer != null) && (transfer.getTransferType() != null)) {
			if (transfer.getTransferType().equals(CashFlow.ROLLED_INTEREST)) {
				return new MessageArray();
			}
		}
		return super.generateBOMessages(config, leReceiver, leSender, trade, transfer, event, exceptions, dsCon);
	}
*/
	@Override
	public MessageArray generateMessages(AdviceConfig config, LegalEntity leReceiver, LegalEntity leSender, Trade trade,
			BOTransfer transfer, PSEvent event, List<Task> exceptions, DSConnection dsCon) {
		// TODO Auto-generated method stub
		if ((transfer != null) && (transfer.getTransferType() != null)) {
			if (transfer.getTransferType().equals(CashFlow.ROLLED_INTEREST)) {
				return new MessageArray();
			}
		}
		return super.generateMessages(config, leReceiver, leSender, trade, transfer, event, exceptions, dsCon);
	}
}
