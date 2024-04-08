package calypsox.camel.engine;

import calypsox.camel.CamelContextComponentBean;
import calypsox.tk.camel.AbstractCamelRouteBuilder;
import com.calypso.engine.Engine;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.services.GatewayUtil;

import java.util.Optional;

/**
 * @author paisanu
 */
public abstract class CamelBasedEngine extends Engine {

    protected CamelContextComponentBean camelContextBean;

    public CamelBasedEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
    }

    @Override
    public boolean start(boolean batch) {
        boolean allowStartup = false;
        try {
            allowStartup=initCamelContextBean();
            if(allowStartup){
                camelContextBean.start();
                allowStartup = super.start(batch);
            }
        } catch (Exception exc) {
            Log.error(this.getClass().getSimpleName(), "Errors while starting Apache Camel based engine: " + exc.getMessage());
        }
        return allowStartup;
    }

    @Override
    public void stop() {
        try {
            camelContextBean.stop();
        } catch (Exception exc) {
            Log.error(this.getClass().getSimpleName(), "Errors while stopping Apache Camel based engine: " + exc.getCause());
        }
        super.stop();
    }

    public boolean initCamelContextBean() throws Exception {
        String propertiesName= Optional.ofNullable(getEngineContext())
                .map(engCtx -> engCtx.getInitParameter("config",""))
                .orElse("");
        //String propertiesName = "ctm_uploader.connection.properties";
        if (!Util.isEmpty(propertiesName)) {
            this.camelContextBean = new CamelContextComponentBean(GatewayUtil.readPropertyFile(propertiesName));
            this.camelContextBean.buildCamelContext(getCamelRouteBuilder());
        } else {
            Log.error(getLogCategory(), "Connection properties not found, so " + getEngineName() + " couldn't be initialized");
            return false;
        }
        return true;
    }

    protected abstract AbstractCamelRouteBuilder getCamelRouteBuilder();
}
