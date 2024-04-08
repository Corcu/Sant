package calypsox.tk.bo.engine.util;

import com.calypso.engine.context.EngineContext;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.upload.services.GatewayUtil;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

/**
 * @author acd
 */
public class SantEngineUtil {

    private static SantEngineUtil instance;

    private static final String CONFIG = "config";
    private static final String TYPE = "type";
    private static final String SEND_RESPONSE = "sendResponse";

    public synchronized static SantEngineUtil getInstance() {
        if (instance == null) {
            instance = new SantEngineUtil();
        }
        return instance;
    }

    /**
     * Read properties file from engineContext on -config parameter
     * @param engineContext
     * @return @{@link Properties}
     */
    public synchronized Properties readProperties(EngineContext engineContext){
        Properties properties = null;

        if(null!=engineContext){
            String propertyFileFromEngine = engineContext.getInitParameter(CONFIG, (String)null);
            String propertiesName = "";

            if (!Util.isEmpty(propertyFileFromEngine)) {
                propertiesName = propertyFileFromEngine;
            } else {
                Log.debug(engineContext.getEngineName(), "The config name was not found in " + engineContext.getEngineName() + " ,Hence default property file " + propertiesName + " will be used.");
            }
            if (!Util.isEmpty(propertiesName)) {
                try {
                    properties = GatewayUtil.readPropertyFile(propertiesName);
//                    System.out.println(properties.toString());
                } catch (IOException var5) {
                    Log.error(engineContext.getEngineName(), "Unable to read the property file " + propertiesName);
                }
            }
        }
        return properties;
    }

    /**
     * Read properties file from engineContext on -config parameter
     * @param engineContext
     * @return @{@link Properties}
     */
    public synchronized Properties readPropertiesLocal(String propsName){
        Properties properties = null;

            String propertyFileFromEngine =propsName;
            String propertiesName = "";

            if (!Util.isEmpty(propertyFileFromEngine)) {
                propertiesName = propertyFileFromEngine;
            } else {
            }
            if (!Util.isEmpty(propertiesName)) {
                try {
                    properties = GatewayUtil.readPropertyFile(propertiesName);
//                    System.out.println(properties.toString());
                } catch (IOException var5) {
                }
            }
        return properties;
    }

    /**
     * Read -type property form engineContext
     * @param engineContext
     * @return
     */
    public String readPropertiesType(EngineContext engineContext,String defaultValue){
        return Optional.ofNullable(engineContext.getInitParameter(TYPE, defaultValue)).orElse("");
    }

    /**
     * Read -type property form engineContext
     * @param engineContext
     * @return
     */
    public boolean readPropertiesSendResponse(EngineContext engineContext,String defaultValue){
        return Optional.ofNullable(engineContext.getInitParameter(SEND_RESPONSE, defaultValue)).map(Boolean::parseBoolean).orElse(false);
    }

}
