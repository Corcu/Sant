package calypsox.tk.bo;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOMessageHandler;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.refdata.AdviceConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageArray;

import java.util.List;

/**
 * @author aalonsop
 */
public class BODATAEXPORTERMSGMarginCallMessageHandler extends BOMessageHandler {

    private static final String OBJECT_TYPE_FIELD = "ObjectType";
    private static final String OBJECT_ID_FIELD = "ObjectId";
    private static final String EXPORT_FORMAT_FIELD = "exportFormat";
    private static final String SOURCE_NAME_FIELD = "SourceName";

    private static final String TRADE_TYPE = "TRADE";
    private static final String EXPORT_FORMAT = "UploaderXML";
//    private static final String DATAEXPORTER_ENGINE_NAME = "DataExporterEngine";


    @Override
    public MessageArray generateMessages(AdviceConfig config, LegalEntity leReceiver, LegalEntity leSender, Trade trade, BOTransfer transfer, PSEvent event, List<Task> exceptions, DSConnection dsCon) {
        MessageArray messArray = super.generateMessages(config, leReceiver, leSender, trade, transfer, event, exceptions, dsCon);
        if (trade != null && config != null) {
            for (BOMessage message : messArray) {
                message.setAttribute(OBJECT_TYPE_FIELD, TRADE_TYPE);
                message.setAttribute(EXPORT_FORMAT_FIELD, EXPORT_FORMAT);
                message.setAttribute(OBJECT_ID_FIELD, String.valueOf(trade.getLongId()));
                message.setAttribute(SOURCE_NAME_FIELD,  config.getAddressMethod());
            }
        }
        return messArray;
    }
}
