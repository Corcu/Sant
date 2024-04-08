package calypsox.tk.bo;

import java.util.List;
import java.util.stream.Collectors;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.event.PSEventStatement;
import com.calypso.tk.service.DSConnection;

public class BOACADIAMessageHandler extends com.calypso.tk.bo.BOACADIAMessageHandler {

	@Override
	protected List<BOMessage> getOldMessages(PSEventStatement statement, DSConnection dsCon) {
		List<BOMessage> result = super.getOldMessages(statement, dsCon);
		if(result != null && !result.isEmpty()) {
			//delete Filenet messages from list to avoid reprocessing
			result = result.stream().filter(msg -> !(msg.getGateway().equals("MCNOTIFFileNet") && 
					msg.getMessageType().equals(("MC_NOTIFICATION")))).collect(Collectors.toList());
		}

		return result;
	}
}
