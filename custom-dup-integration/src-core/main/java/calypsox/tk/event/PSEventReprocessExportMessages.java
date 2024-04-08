package calypsox.tk.event;

import java.util.HashSet;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Status;
import com.calypso.tk.event.PSEvent;

public class PSEventReprocessExportMessages extends PSEvent {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	long tradeLongId=0L;
	BOMessage message=null;
	boolean sendAccountClosing = true;
	Action messageAction;
	HashSet<Status> status;

	public HashSet<Status> getStatus() {
		return status;
	}
	public void setStatus(HashSet<Status> status) {
		this.status = status;
	}
	public PSEventReprocessExportMessages(){}
	public PSEventReprocessExportMessages(BOMessage message) {
		this.message=message;
	}
	public PSEventReprocessExportMessages(long tradeLongId) {
		this.tradeLongId=tradeLongId;
	}
	public long getTradeLongId() {
		return tradeLongId;
	}
	public void setTradeLongId(long tradeLongId) {
		this.tradeLongId = tradeLongId;
	}
	public BOMessage getMessage() {
		return message;
	}
	public void setMessage(BOMessage message) {
		this.message = message;
	}
	public boolean isTradeReprocess() {
		return message==null;
	}
	public boolean isMessageReprocess() {
		return tradeLongId==0L;
	}
	public boolean isSendAccountClosing() {
		return sendAccountClosing;
	}
	public void setSendAccountClosing(boolean sendAccountClosing) {
		this.sendAccountClosing = sendAccountClosing;
	}
	public Action getMessageAction() {
		return messageAction;
	}
	public void setMessageAction(Action messageAction) {
		this.messageAction = messageAction;
	}
}
