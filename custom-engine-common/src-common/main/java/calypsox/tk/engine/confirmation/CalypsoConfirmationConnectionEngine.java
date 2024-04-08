package calypsox.tk.engine.confirmation;

import calypsox.camel.CamelContextBean;
import calypsox.camel.engine.CamelBasedSenderEngine;
import calypsox.tk.bo.engine.util.SantEngineUtil;
import calypsox.tk.camel.route.confirmation.CalypsoConfirmRouteBuilder;
import com.calypso.tk.service.DSConnection;
import org.apache.camel.builder.RouteBuilder;

import java.util.ArrayList;
import java.util.List;


/**
 * @author aalonsop
 * Start and stops Camel's related context and routes
 */
public class CalypsoConfirmationConnectionEngine extends CamelBasedSenderEngine {

    public CalypsoConfirmationConnectionEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
    }

    @Override
    public void initCamelContextBean() throws Exception {
          this.camelContextBean = new CamelContextBean(SantEngineUtil.getInstance()
                 .readProperties(getEngineContext()));
        List<String> prefixList=new ArrayList<>();
        prefixList.add("");
        prefixList.add("bond");
        this.camelContextBean.buildCamelContextMultipleEms(getCamelRouteBuilder(),prefixList);
    }

    @Override
    public RouteBuilder getCamelRouteBuilder() {
        return new CalypsoConfirmRouteBuilder();
    }
}
