package calypsox.camel.engine;

import calypsox.camel.CamelContextBean;
import calypsox.camel.CamelContextBeanBased;
import calypsox.camel.CamelContextBeanPool;
import calypsox.tk.bo.engine.util.SantEngineUtil;
import com.calypso.engine.advice.SenderEngine;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;

import java.util.Properties;

/**
 * @author aalonsop
 * Starts and stops Camel's related context and routes
 */
public abstract class CamelBasedSenderEngine extends SenderEngine implements CamelContextBeanBased {

    protected CamelContextBean camelContextBean;

    protected CamelBasedSenderEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
    }


    @Override
    public boolean start(boolean batch) {
        boolean allowStartup=false;
        try {
            initCamelContextBean();
            camelContextBean.start();
            CamelContextBeanPool.getInstance().putCamelContextBean(getCamelContextBeanName(),this.camelContextBean);
            allowStartup = super.start(batch);
        } catch (Exception exc) {
            Log.error(this.getClass().getSimpleName(), "Errors while starting Apache Camel based engine: "+ exc.getMessage());
        }
        return allowStartup;
    }

    @Override
    public void stop() {
        try {
            camelContextBean.stop();
            CamelContextBeanPool.getInstance().removeCamelContextBean(getCamelContextBeanName(),this.camelContextBean);
        } catch (Exception exc) {
            Log.error(this.getClass().getSimpleName(), "Errors while stopping Apache Camel based engine: "+ exc.getCause());
        }
        super.stop();
    }

    public void initCamelContextBean() throws Exception {
          Properties camelProperties= SantEngineUtil.getInstance().readProperties(getEngineContext());
          this.camelContextBean = new CamelContextBean(camelProperties);
          this.camelContextBean.buildCamelContext(getCamelRouteBuilder());
    }

}
