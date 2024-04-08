/**
 * 
 */
package calypsox.tk.bo;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.TemplateSelector;
import com.calypso.tk.core.Trade;

/**
 * Custom template selector for MarginCall notifications
 * 
 * @author aela
 * 
 */
public class MarginCallTemplateSelector implements TemplateSelector {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.calypso.tk.bo.TemplateSelector#getTemplate(com.calypso.tk.core.Trade,
	 * com.calypso.tk.bo.BOMessage, java.lang.String)
	 */
	@Override
	public String getTemplate(Trade trade, BOMessage message, String name) {
		// MarginCallConfig mcc = null;
		// MarginCallEntryDTO mce = null;
		// try {
		// mcc =
		// DSConnection.getDefault().getRemoteReferenceData().getMarginCallConfig(message.getStatementId());
		// mce = SantMarginCallUtil.getMarginCallEntryDTO(message,
		// DSConnection.getDefault());
		// } catch (Exception e) {
		// Log.error(this, e);
		// }
		// // if there is no MarginCallConig then return the default template
		// if (mcc == null || mce == null)
		// return name;
		//
		// if
		// (NotificationFactory.MESSAGE_TYPE_MC_NOTIFICATION.equals(message.getMessageType()))
		// {
		// // decide if its a MarginCall Notice or a Recouponing Notice
		// if
		// (Boolean.parseBoolean(mcc.getAdditionalField(MarginCallConstants.MC_RECOUPONING)))
		// {
		// return "MarginCallRecouponing.htm";
		// } else {
		// return "MarginCallNotice.htm";
		// }
		// }
		return name;
	}

}
