package calypsox.tk.util;


import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.MessageArray;
import com.calypso.tk.util.ScheduledTask;

import java.rmi.RemoteException;
import java.util.Optional;


public class ScheduledTaskInitialProcessDateMT527Fix extends ScheduledTask {
    @Override
    public String getTaskInformation() {
        return null;
    }

    public boolean process(final DSConnection conn, final PSConnection connPS) {
        MessageArray initMessages = new MessageArray();
        try {
            initMessages = conn.getRemoteBO().getMessages("message_status = 'MATCHED' and message_type = 'TRIPARTYCONF' and template_name = 'MT527BONYCollateral' and message_id IN (select message_id from mess_attributes where  attr_name = 'Triparty Amendment Type' and attr_value = 'Init')", null);
            for (BOMessage initMsg : initMessages) {
                String mccId = initMsg.getAttribute("marginCallConfigId");
                String entryId = initMsg.getAttribute("marginCallEntryId");
                if (!Util.isEmpty(mccId) && !Util.isEmpty(entryId)) {
                    CollateralConfig contract = Optional.ofNullable(CacheCollateralClient.getCollateralConfig(conn, Integer.parseInt(mccId))).orElse(null);
                    if (null != contract) {
                        MarginCallEntryDTO entryDTO = loadEntry(Integer.parseInt(entryId));
                        MessageArray messages = conn.getRemoteBO().getMessages("message_status <> 'CANCELED' and message_type = 'TRIPARTYCONF' and template_name = 'MT527BONYCollateral' and message_id IN (select message_id from mess_attributes where  attr_name = 'InitialProcessDate' and attr_value <> ' ' ) and message_id IN (select message_id from mess_attributes where attr_name = 'marginCallConfigId' and attr_value = '" + mccId + "') and message_id IN (select message_id from mess_attributes where  attr_name = 'Triparty Amendment Type' and attr_value = 'Price Adjustment' )", null);
                        for (BOMessage message : messages) {
                            if (Util.isEmpty(message.getAttribute("InitialProcessDate"))) {
                                if (null != entryDTO) {
                                    message.setAttribute("InitialProcessDate", entryDTO.getProcessDate().toString());
                                    conn.getRemoteBO().updateAttributes(message);
                                }
                            }
                        }
                    }
                }
            }
        } catch (
                CalypsoServiceException e) {
            Log.error(Log.CALYPSOX, e);
        }
        return true;
    }

    private MarginCallEntryDTO loadEntry(int mcEntryId) {
        MarginCallEntryDTO mcEntryDTO = null;
        try {
            mcEntryDTO = ServiceRegistry.getDefault(DSConnection.getDefault())
                    .getCollateralServer().loadEntry(mcEntryId, true);
        } catch (final RemoteException e) {
            Log.error(Log.CALYPSOX, e);
        }
        return mcEntryDTO;
    }

}
