package com.santander.restservices.acx;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.Log;
import com.santander.restservices.*;
import com.santander.restservices.oauth.ACXOauthTokenService;

import java.util.HashMap;
import java.util.Properties;

/**
 * common base for ACX rest clients
 *
 * @author x865229
 * date 25/11/2022
 */
public abstract class ACXServiceBase implements ApiRestService {

    public static final String APPLICATION_HEADER = "X-IBM-Client-Id";
    public static final String SANTANDER_HEADER = "X-Santander-Client-Id";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String REST_SERVICES_PROPERTIES_FILE = "restservices.properties";
    public static final String AUTHORIZATION_HEADER_BEARER = "Bearer ";
    public static final int AUTHENTICATION_ERROR = 401;

    public static final String USER = "_User";
    public static final String PROTOCOL = "_Protocol";
    public static final String PORT = "_Port";
    public static final String HOST = "_Host";
    public static final String FILE = "_File";
    public static final String SSL_ALGORITHM = "_SslAlgorithm";
    public static final String IGNORE_CERTIFICATE = "_IgnoreCertificate";

    private final String serviceName;

    private String accessToken = null;


    public ACXServiceBase(String serviceName) {
        this.serviceName = serviceName;
    }

    public abstract boolean validateParameters();

    public int callService() {

        int status = -1;

        if (!validateParameters()) {
            String debug = "Invalid parameters calling service " + serviceName + ": " + getInput();
            Log.error(this, debug);
            return status;
        }

        Properties properties = UtilRestServices.getPropertyFile(REST_SERVICES_PROPERTIES_FILE);
        if (properties == null) {
            String debug = "Cannot load service properties: " + REST_SERVICES_PROPERTIES_FILE;
            Log.error(this, debug);
            return status;
        }

        String application = properties.getProperty(
                ACXOauthTokenService.PREFIX_OUATH_TOKEN_SERVICE + "_" + ACXOauthTokenService.SERVICE_NAME + "_" + ACXOauthTokenService.PROPERTY_USR);
        String protocol = properties.getProperty(serviceName + PROTOCOL);
        String port = properties.getProperty(serviceName + PORT);
        String host = properties.getProperty(serviceName + HOST);
        String file = properties.getProperty(serviceName + FILE);
        String sslAlgorithm = properties.getProperty(serviceName + SSL_ALGORITHM);
        String ignoreCertificate = properties.getProperty(serviceName + IGNORE_CERTIFICATE);

        if (Util.isEmpty(application) || Util.isEmpty(protocol) || Util.isEmpty(port)
                || Util.isEmpty(host) || Util.isEmpty(file)) {

            String debug = "Invalid service properties: " +
                    REST_SERVICES_PROPERTIES_FILE + " " +
                    "protocol [" + protocol + "] " +
                    "port [" + port + "] " +
                    "host [" + host + "] " +
                    "file [" + file + "]";

            Log.error(this, debug);
            return status;
        }

        ApiRestAdapter adapter = FactoryApiRestAdapter.getApiRestService();
        ApiRestEndPoint endpoint = new ApiRestEndPoint();

        endpoint.setProtocol(protocol);
        endpoint.setPort(Integer.parseInt(port));
        endpoint.setHost(host);
        endpoint.setFile(file);

        adapter.init(endpoint);
        adapter.setSslAlgorithm(sslAlgorithm);
        adapter.setIgnoreCertificate(ignoreCertificate);

        HashMap<String, String> headers = new HashMap<>();
        headers.put(APPLICATION_HEADER, application);
        headers.put(SANTANDER_HEADER, application);
        headers.put(AUTHORIZATION_HEADER, AUTHORIZATION_HEADER_BEARER + accessToken);

        status = adapter.post(getInput(), getOutput(), getError(), headers);

        if (!ApiRestAdapter.isOKHttpStatus(status)) {
            String debug = "Error calling service " + serviceName + ": " +
                    "status [" + status + "] " +
                    "error [" + getError().pullTextMessage() + "] ";

            //if (status != AUTHENTICATION_ERROR) {
            Log.error(this, debug);
            //}
        }

        return status;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
