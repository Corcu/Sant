package com.isban.api.webservice;


import calypsox.tk.bo.CustomFallidasfallidasReportClientCacheAdapter;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.ConnectException;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Path;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public abstract class SantanderRestWebServices {


    private static final String CALYPSO_ENGINE_MANAGER_CONFIG = "calypso.engine.manager.config";
    private static final String ACCEPTED_ENGINE_SERVER_WS = "AcceptedEngineServerWS";


    public SantanderRestWebServices() {
        super();
    }


    protected boolean isEngineServer(Class myClass, String methodName, Class[] paramTypes){
        String path = "";
        Method method = null;
        if(myClass!=null) {
            for (Annotation classAnnotation : myClass.getAnnotations()) {
                if (classAnnotation.toString().contains("Path")) {
                    path = path + classAnnotation.toString().substring(classAnnotation.toString().indexOf("/"), classAnnotation.toString().indexOf(")"));
                }
            }
            try {
                method = myClass.getMethod(methodName, paramTypes);
                if(method!=null) {
                    path = path + method.getDeclaredAnnotation(Path.class).value();
                }
            } catch (NoSuchMethodException e) {
                Log.error(this.getClass().getName(), "Could not get the method " + methodName);
            }

            if(!Util.isEmpty(path)) {
                String engineServerName = System.getProperty(CALYPSO_ENGINE_MANAGER_CONFIG);
                DSConnection dsConn = getDSConnection();
                if (dsConn != null) {
                    String engineServerDV = LocalCache.getDomainValueComment(dsConn, ACCEPTED_ENGINE_SERVER_WS, path);
                    if (Util.isEmpty(engineServerDV)) {
                        Log.error(this, "WebService with name '" + path + "' is not defined in Calypso.");
                    } else if (engineServerDV.equalsIgnoreCase(engineServerName)) {
                        return true;
                    }
                }
            }
        }
        return false;
   }


    public DSConnection getDSConnection() {
        DSConnection dsConn = null;
        try {
            dsConn = DSConnection.getDefault().getReadOnlyConnection();
        } catch (ConnectException e1) {
            Log.error(CustomFallidasfallidasReportClientCacheAdapter.class, "Error getting DSConnection. ", e1);
        }
        if (dsConn == null) {
            dsConn = DSConnection.getDefault();
        }
        return dsConn;
    }


    protected String buildJsonResponse(String message) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("message", message);
        JsonObject jsonObject = builder.build();
        String jsonMessage = jsonObject.toString();
        return jsonMessage;
    }


    protected String buildJsonResponse(String message, List<String> errors) {
        StringBuilder fullMessage = new StringBuilder(message);
        fullMessage.append(". ERRORS: ");
        for (int iError = 0; iError < errors.size(); iError++) {
            String error = errors.get(iError);
            if (iError > 0) {
                fullMessage.append(", ");
            }
            fullMessage.append(error);
        }
        fullMessage.append(".");
        return buildJsonResponse(fullMessage.toString());
    }


    protected String getPath(String methodName){
        String path = "";

        return path;
    }


}
