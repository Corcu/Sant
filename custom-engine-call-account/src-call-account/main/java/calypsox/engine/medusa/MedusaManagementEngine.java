package calypsox.engine.medusa;

import calypsox.camel.CamelConnectionManagement;
import calypsox.camel.routes.MedusaLolRoutesBuilder;
import calypsox.camel.routes.MedusaRouteBuilder;
import calypsox.engine.medusa.builder.MedusaTransferBuilder;
import calypsox.engine.medusa.utils.xml.CashManagement;
import calypsox.tk.bo.engine.util.SantEngineUtil;
import com.calypso.engine.Engine;
import com.calypso.engine.context.EngineContext;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTransfer;
import com.calypso.tk.service.DSConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class MedusaManagementEngine extends Engine {
    /**
     * - ConfigFile con las colas "medusa.connection.properties"
     * - Iniciar CamelContext
     */
    private String configFile = null;
    private Properties properties;
    private static final String SEND_ROUTE = "direct:medusaMessages";
    private static final String MEDUSA_LOL_ROUTES = "MedusaLolRoutesBuilder";

    CamelConnectionManagement camelManagement = new CamelConnectionManagement();;

    public MedusaManagementEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    protected void init(EngineContext engineContext) {
        super.init(engineContext);
        properties = SantEngineUtil.getInstance().readProperties(getEngineContext());

        if(MEDUSA_LOL_ROUTES.equalsIgnoreCase(Optional.ofNullable(properties).map(p -> p.getProperty("routes")).orElse(""))){
            List<String> prefixList=new ArrayList<>();
            prefixList.add("");
            prefixList.add("medusa");
            camelManagement.initMultiCamelContexts(properties,new MedusaLolRoutesBuilder(),prefixList).start();
        }else {
            camelManagement.initCamelContext(properties,new MedusaRouteBuilder()).start();
        }


        if(null!=camelManagement){
            //Init Camel connection
            Log.system(MedusaManagementEngine.class.getName(),"Init camel connection for MedusaManagementEngine: ");
        }

    }

    @Override
    public boolean process(PSEvent psEvent) {

        boolean processed = true;
        BOTransfer xfer = null;
        Trade trade = null;
        if (psEvent instanceof PSEventTransfer) {
            final PSEventTransfer psEventTransfer = (PSEventTransfer) psEvent;
            xfer = psEventTransfer.getBoTransfer();
            if (xfer.getTradeLongId() != 0) {
                trade = psEventTransfer.getTrade();
            }
        }


        //TODO Validate Trasnfer Date
        if (xfer != null) {
            final CashManagement cm = new CashManagement();

            try {
                cm.setBotransfer(new MedusaTransferBuilder(xfer,trade).buildCMTransfer());
                final String cmtMessage = cm.marshall();
                sendMessage(cmtMessage);

            } catch (final Exception re) {
                Log.error(this.getEngineName(), "Error building CashManagement Obj", re);
            }

        }

        if (processed) {
            consumeEvent(psEvent);
        }
        return true;
    }

    private boolean consumeEvent(final PSEvent event) {
        try {
            DSConnection.getDefault().getRemoteTrade().eventProcessed(event.getLongId(),
                    getEngineName());
        } catch (final Exception e) {
            Log.error(this, e);
            return false;
        }
        return true;
    }
    private boolean sendMessage(String message){
        if(null!=camelManagement && !Util.isEmpty(message) ){
            try {
                camelManagement.sendMessage(SEND_ROUTE,message);
                Log.system(MedusaManagementEngine.class.getName(),"Medusa Message send: " + message );
                return true;
            } catch (Exception e) {
                Log.error(this,"Error sending message: " + e.getCause() );
            }
        }
        return false;
    }

    @Override
    public void stop() {
        if( camelManagement.getContext() !=null){
            try {
                camelManagement.stopConnection();
            } catch (Exception exc) {
                Log.error(this.getClass().getSimpleName(), "Errors while stopping camel connection", exc.getCause());
            }
        }
        super.stop();
    }
}
