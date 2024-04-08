package calypsox.camel.processor;

import com.calypso.tk.bo.ExternalMessageHandler;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.InstantiateUtil;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class MexicoDataUploaderProcessor implements Processor {

    /**
     * Generate PSEventUpload for Mex
     * @param exchange
     * @throws Exception
     */
    @Override
    public void process(Exchange exchange) throws Exception {
        String message = exchange.getIn().getBody(String.class);
        boolean status = false;

        try {
            MexicoExternalMessage externalMessage = new MexicoExternalMessage();
            externalMessage.setText(message);

            ExternalMessageHandler handler = null;
            String handlerClassName = "tk.bo.MexicoMessageHandler";

            try {
                handler = (ExternalMessageHandler) InstantiateUtil.getInstance(handlerClassName, true);
            } catch (Exception var9) {
                Log.info(this, handlerClassName + " not found.");
            }
            if (handler != null) {
                status = handler.handleExternalMessage(externalMessage, null, null,"", DSConnection.getDefault(), (Object) null);
            }
        }catch (Exception e){
            Log.error(this,"Error generating PSEventUpload: " + e);
        }
    }
}
