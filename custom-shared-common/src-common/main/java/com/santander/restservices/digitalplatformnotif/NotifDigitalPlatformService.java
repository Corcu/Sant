package com.santander.restservices.digitalplatformnotif;

import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.Log;
import com.santander.restservices.ApiRestAdapter;
import com.santander.restservices.ApiRestEndPoint;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestService;
import com.santander.restservices.FactoryApiRestAdapter;
import com.santander.restservices.UtilRestServices;
import com.santander.restservices.digitalplatformnotif.model.NotifDigitalPlatformAction;
import com.santander.restservices.digitalplatformnotif.model.NotifDigitalPlatformError;
import com.santander.restservices.digitalplatformnotif.model.NotifDigitalPlatformInput;
import com.santander.restservices.digitalplatformnotif.model.NotifDigitalPlatformOutput;
import com.santander.restservices.digitalplatformnotif.model.NotifDigitalPlatformRecipient;

public class NotifDigitalPlatformService implements ApiRestService{
    public static final String APPLICATION_HEADER = "X-IBM-Client-Id";
    public static final String SANTANDER_HEADER = "X-Santander-Client-Id";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String REST_SERVICES_PROPERTIES_FILE = "restservices.properties";
    public static final String AUTHORIZATION_HEADER_BEARER = "Bearer ";

    public static final String USER = "_User";
    public static final String PROTOCOL = "_Protocol";
    public static final String PORT = "_Port";
    public static final String HOST = "_Host";
    public static final String FILE = "_File";
    public static final String SSL_ALGORITHM = "_SslAlgorithm";
    public static final String IGNORE_CERTIFICATE = "_IgnoreCertificate";

    private final String serviceName;
    
    private String accessToken = null;
    private NotifDigitalPlatformInput input = new NotifDigitalPlatformInput();
    private NotifDigitalPlatformOutput output = new NotifDigitalPlatformOutput();
    private NotifDigitalPlatformError error = new NotifDigitalPlatformError();
    
    private String tittle = null;
	private String content = null;
	private String application = null;
	private boolean isImportant = false;
	private NotifDigitalPlatformAction action = null;
	private List<NotifDigitalPlatformRecipient> recipients = null;

    public String getTittle() {
		return tittle;
	}

	public void setTittle(String tittle) {
		this.tittle = tittle;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public boolean isImportant() {
		return isImportant;
	}

	public void setImportant(boolean isImportant) {
		this.isImportant = isImportant;
	}

	public NotifDigitalPlatformAction getAction() {
		return action;
	}

	public void setAction(NotifDigitalPlatformAction action) {
		this.action = action;
	}

	public List<NotifDigitalPlatformRecipient> getRecipients() {
		return recipients;
	}

	public void setRecipients(List<NotifDigitalPlatformRecipient> recipients) {
		this.recipients = recipients;
	}

	public String getAccessToken() {
		return accessToken;
	}

	
	



    public NotifDigitalPlatformService(String serviceName) {
        this.serviceName = serviceName;
    }

    public  boolean validateParameters() {
		return true;
    }

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

        String application = properties.getProperty("OauthToken" + "_" + serviceName + "_User" );
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

        //Set header fields
        HashMap<String, String> headers = new HashMap<>();
        headers.put(APPLICATION_HEADER, application);
        headers.put(SANTANDER_HEADER, application);
        headers.put(AUTHORIZATION_HEADER, AUTHORIZATION_HEADER_BEARER + accessToken);
        //Set body fields
        input.setAction(action);
        input.setApplication(this.application);
        input.setContent(content);
        input.setImportant(isImportant);
        input.setRecipients(recipients);
        input.setTittle(tittle);

        status = adapter.post(input, getOutput(), getError(), headers);

        if (!ApiRestAdapter.isOKHttpStatus(status)) {
            String debug = "Error calling service " + serviceName + ": " +
                    "status [" + status + "] " +
                    "error [" + getError().toString() + "] ";

            Log.error(this, debug);
        }

        return status;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

	@Override
	public NotifDigitalPlatformInput getInput() {
		return input;
	}

	@Override
	public ApiRestModel getOutput() {
		return output;
	}

	@Override
	public ApiRestModel getError() {
		
		return error;
	}


}
