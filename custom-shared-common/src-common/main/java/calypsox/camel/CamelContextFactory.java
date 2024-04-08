package calypsox.camel;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import org.apache.camel.CamelContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class CamelContextFactory {
    private static final String CAMEL_CONFIG = "/calypsox/camel/config/camel-config.xml";
    private ApplicationContext appContext = null;
    private URL resource;

    private static CamelContextFactory instance;

    public static synchronized CamelContextFactory getInstance() {
        if (instance == null) {
            instance = new CamelContextFactory();
        }
        return instance;
    }

    /**
     *  Init Camel Contexts
     */
    public void initAppContext() {
        resource = CamelContextFactory.class.getResource(CAMEL_CONFIG);
        //Init Camel connection
        if(resource!=null && appContext==null){
            Log.system(CamelContextFactory.class.getName(),"Initializing CamelContexts.");
            appContext = new ClassPathXmlApplicationContext(resource.toString());
        }else{
            Log.system(CamelContextFactory.class.getName(),"Contexts already initiated.");
        }
    }

    /**
     * Restart All Camel Contexts
     */
    public void restartAppContext() {
        if(resource!=null){
            Log.system(CamelContextFactory.class.getName(),"Restarting CamelContexts.");
            appContext = new ClassPathXmlApplicationContext(resource.toString());
        }else{
            Log.system(CamelContextFactory.class.getName(),"Error restarting camel context.");
        }
    }

    /**
     * @param contextName
     * @return @{@link CamelContext}
     */
    public CamelContext getCamelContext(String contextName){
        try {
            if(!Util.isEmpty(contextName) && null!=appContext){
                return (CamelContext) appContext.getBean(contextName);
            }
        }catch (ClassCastException e){
            Log.error(this,"Error casting bean to CamelContext: " + e);
        }
        return null;
    }

    /**
     * @return
     */
    public Map<String, CamelContext> getAllCamelContext(){
        Map<String, CamelContext> beansOfType = new HashMap<>();
        try {
            if(null!=appContext){
                beansOfType = appContext.getBeansOfType(CamelContext.class);
            }
        }catch (ClassCastException e){
            Log.error(this,"Error casting bean to CamelContext: " + e);
        }
        return beansOfType;
    }

    public static void main(String[] args){
        CamelContextFactory.getInstance().initAppContext();
        System.out.println("s");
    }
}
