package calypsox.tk.bo.document;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.document.DocumentSender;
import com.calypso.tk.bo.document.SenderConfig;
import com.calypso.tk.bo.document.SenderCopyConfig;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.IEAdapter;
import com.calypso.tk.util.IEAdapterConfig;

import java.util.Vector;

public class GatewayP37DocumentSender implements DocumentSender {
	
	protected IEAdapter _mqSender;
	
	public GatewayP37DocumentSender() {

	IEAdapterConfig config = IEAdapter.getConfig("Uploader");
		if(!config.isConfigured("calypso_p37_config.properties")) {
		   Log.error("P37", "*** CALYPSOML Gateway not configured properly  Please check calypso_p37_config.properties " + new JDatetime());
		} else {
		   this._mqSender = config.getSenderIEAdapter();
		   try {
		      this._mqSender.init();
		   } catch (Exception arg2) {
		      Log.error("P37", " unable to init Adapter ", arg2);
		   }
		}
	}
	
	@Override
	public boolean isOnline() {
		return this._mqSender.isOnline();
	}

	@Override
	@SuppressWarnings("rawtypes")
	public boolean send(DSConnection ds, SenderConfig config, SenderCopyConfig copyConfig, long eventId,
			AdviceDocument document, Vector copies, BOMessage message, Vector errors, String engineName,
			boolean[] saved) {

		boolean send = _mqSender.write(document.getDocument().toString());
		if(send){
			message.setAction(Action.SEND);
			try {
				DSConnection.getDefault().getRemoteBO().save(message, 0, engineName);
			} catch (CalypsoServiceException exc) {
				Log.error(this.getClass().getSimpleName(), "Error while applying SENT action to msg", exc.getCause());
			}
		}
		return true;
	}


}
