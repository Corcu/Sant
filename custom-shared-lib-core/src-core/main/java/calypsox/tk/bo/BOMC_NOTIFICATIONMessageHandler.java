package calypsox.tk.bo;

import java.util.List;
import java.util.Vector;

import com.calypso.helper.CoreAPI;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageAttributeCopierHelper;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventStatement;
import com.calypso.tk.refdata.AdviceConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageArray;

public class BOMC_NOTIFICATIONMessageHandler extends com.calypso.tk.bo.BOMC_NOTIFICATIONMessageHandler {

	private static final String MCNOTIF_FILE_NET = "MCNOTIFFileNet";
	private static final String REPLACE = "REPLACE";

	@Override
	public MessageArray generateBOMessages(AdviceConfig config, LegalEntity leReceiver, LegalEntity leSender,
			Trade trade, BOTransfer transfer, PSEvent event, Vector exceptions, DSConnection dsCon) {

		MessageArray messages = super.generateBOMessages(config, leReceiver, leSender, trade, transfer, event,
				exceptions, dsCon);
		if (event instanceof PSEventStatement && config.getGateway().equals(MCNOTIF_FILE_NET)) {
			PSEventStatement statement = (PSEventStatement) event;
			List<BOMessage> oldMessages = this.getOldMessages(statement, dsCon);
			messages = this.matching(config, messages, oldMessages, dsCon);
		}
		return messages;
	}

	private MessageArray matching(AdviceConfig config, MessageArray newMessages, List<BOMessage> oldMessages,
			DSConnection dsCon) {
		MessageArray result = new MessageArray();
		if (!Util.isEmpty(oldMessages)) {

			for (BOMessage oldMessage : oldMessages) {
				for (BOMessage newMessage : newMessages) {
					if (this.match(oldMessage, newMessage, config, dsCon)) {
						if (this.isActionPossible(oldMessage, Action.AMEND, dsCon)) {
							CoreAPI.setId(newMessage, CoreAPI.getId(oldMessage));
							newMessage.setVersion(oldMessage.getVersion());
							newMessage.setStatus(oldMessage.getStatus());
							newMessage.setAction(Action.AMEND);
							newMessage.setSubAction(oldMessage.getSubAction());
							if (newMessage.getSubAction().equals(Action.CANCEL)) {
								newMessage.setSubAction(Action.AMEND);
							}
							CoreAPI.setLinkedId(newMessage, CoreAPI.getLinkedId(oldMessage));
							MessageAttributeCopierHelper.copyAttributes(newMessage, oldMessage.getAttributes());
							this.addMessage(result, newMessage);
						} else if (this.isActionPossible(oldMessage, Action.CANCEL, dsCon)) {
							oldMessage.setAction(Action.CANCEL);
							this.addMessage(result, oldMessage);
							this.addMessage(result, newMessage);
						} else if (this.isActionPossible(oldMessage, Action.valueOf(REPLACE), dsCon)) {
							newMessage.setSubAction(oldMessage.getSubAction());
							CoreAPI.setLinkedId(newMessage, CoreAPI.getLinkedId(oldMessage));
							MessageAttributeCopierHelper.copyAttributes(newMessage, oldMessage.getAttributes());
							oldMessage.setAction(Action.valueOf(REPLACE));
							this.addMessage(result, oldMessage);
							this.addMessage(result, newMessage);
						}
					} else {
						this.addMessage(result, newMessage);
					}
				}
			}
			return result;
		} else {
			result.add(newMessages);
			return result;
		}
	}

	private void addMessage(MessageArray messages, BOMessage newMessage) {
		if (messages.indexOf(newMessage) < 0) {
			messages.add(newMessage);
		}
	}

	private boolean match(BOMessage oldMessage, BOMessage newMessage, AdviceConfig newConfig, DSConnection dsConn) {
		AdviceConfig oldConfig;
		try {
			oldConfig = dsConn.getRemoteReferenceData().getAdviceConfig(oldMessage.getAdviceConfigId());
		} catch (CalypsoServiceException e) {
			return false;
		}
		return oldMessage.getGateway().equals(newMessage.getGateway())
				&& oldMessage.getTemplateName().equals(newMessage.getTemplateName())
				&&  ((oldConfig.getFilterSet() == null && newConfig.getFilterSet() == null) || 
						(oldConfig.getFilterSet() != null && oldConfig.getFilterSet().equals(newConfig.getFilterSet())))
				&& oldConfig.getFormatType().equals(newConfig.getFormatType())
				&& oldConfig.getLanguage().equals(newConfig.getLanguage())
				&& oldConfig.getAddressMethod().equals(newConfig.getAddressMethod())
				&& oldConfig.getReceiverContactType().equals(newConfig.getReceiverContactType())
				&& oldConfig.getSenderContactType().equals(newConfig.getSenderContactType());
	}
}
