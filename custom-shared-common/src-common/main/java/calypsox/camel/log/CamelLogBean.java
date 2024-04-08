package calypsox.camel.log;

import com.calypso.analytics.Util;
import com.calypso.tk.core.Log;
import org.apache.camel.Exchange;
import org.apache.camel.MessageHistory;

import java.util.List;

/**
 * @author aalonsop
 */
public class CamelLogBean{

    private static final String DEFAULT_CAMEL_ROUTE="DefaultCamelRoute";
    private static final String DEFAULT_CAMEL_LOG_MSG=" New camel exchange:\n";

    private CamelLogBean(){

    }

    public static CamelLogBean getNewInstance(){
        return new CamelLogBean();
    }

    public void logSystem(String body, Exchange exchange) {
        Log.system(buildLogCategory(exchange),buildLogMsg(body,exchange));
    }

    private String buildLogCategory(Exchange exchange){
        String routeName=DEFAULT_CAMEL_ROUTE;
        if(exchange!=null) {
            routeName=exchange.getFromRouteId();
        }
        return routeName;
    }

    private String buildLogMsg(String body,Exchange exchange){
        Object endPoint = exchange.getProperty("CamelToEndpoint");
        String endPointStr="";
        if(endPoint instanceof String){
            endPointStr= (String) endPoint;
        }

        return endPointStr +" "+DEFAULT_CAMEL_LOG_MSG +
                body;
    }
}
