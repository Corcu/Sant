package calypsox.engine.gestorstp;

import calypsox.engine.inventory.SantUpdatePositionEngine;
import calypsox.tk.event.PSEventMTSwiftMessage;
import com.calypso.engine.Engine;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.ExternalMessageHandler;
import com.calypso.tk.bo.ExternalMessageParser;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.SwiftParserUtil;

import java.rmi.RemoteException;

public class GestorSTPSwiftMessageEngine extends Engine {

    private static final String SWIFT_FORMAT = "SWIFT";

    public GestorSTPSwiftMessageEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
    }

    @Override
    public boolean process(final PSEvent event) {
        boolean result = true;
        if (event instanceof PSEventMTSwiftMessage) {
            handleEvent((PSEventMTSwiftMessage) event);
        }
        return result;
    }

    /**
     * @param event
     */
    private void handleEvent(PSEventMTSwiftMessage event){
        final String mtSwiftMessage = event.getMtSwiftMessage();
        if(!Util.isEmpty(mtSwiftMessage)){
           Log.debug(this.getClass().getSimpleName(), " Process GSTP Swift Message - EngineName: " + this.getEngineName() + " PSEventID: " + event.getLongId() + " ThreadID: " + Thread.currentThread().getId());
           Log.info(this.getClass().getSimpleName(), "Processing message: " + mtSwiftMessage);
           try {
               //1. Parse message to Swfit
               ExternalMessageParser parser = SwiftParserUtil.getParser(SWIFT_FORMAT);
               String incomingMessageText = GestorSTPUtil.fixSwiftEndOfLineCharacters(mtSwiftMessage);
               ExternalMessage mtSwiftExternalMessage = parser.readExternal(incomingMessageText, "");

               //2. Get Swfit handler
               ExternalMessageHandler handler = SwiftParserUtil.getHandler(SWIFT_FORMAT, event.getMtType());

               //3. Process Swfit message
               if (handler != null) {
                   handler.handleExternalMessage(mtSwiftExternalMessage, null, null, null, super.getDS(), null);
               } else {
                   SwiftParserUtil.processExternalMessage(mtSwiftExternalMessage, null, null, null, super.getDS(),
                           null);
               }
           } catch (Exception e) {
               Log.error(this.getClass().getSimpleName(), "Parsing error message = " + mtSwiftMessage);
               Log.error(this.getClass().getSimpleName(), "Error: " + e.getCause());
           }
           //Send Response if needed
       }
        //4. Consume Event
        try {
            this._ds.getRemoteTrade().eventProcessed(event.getLongId(), getEngineName());
        } catch (RemoteException e) {
            Log.error(SantUpdatePositionEngine.class.getSimpleName(), e);
        }
    }
}
