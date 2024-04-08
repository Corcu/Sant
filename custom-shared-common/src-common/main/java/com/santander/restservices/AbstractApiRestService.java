package com.santander.restservices;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;

import java.util.Arrays;
import java.util.Properties;

public abstract class AbstractApiRestService implements ApiRestService {

  // Properties from file.
  public static final String PROPERTIES_FILE_NAME = "restservices.properties";
  public static final String PROPERTY_USR = "User";
  public static final String PROPERTY_PASSWORD = "Password";
  public static final String PROPERTY_PROTOCOL = "Protocol";
  public static final String PROPERTY_HOST = "Host";
  public static final String PROPERTY_PORT = "Port";
  public static final String PROPERTY_FILE = "File";
  public static final String PROPERTY_SSL_ALGORITHM = "SslAlgorithm";
  public static final String PROPERTY_IGNORE_CERTIFICATE = "IgnoreCertificate";
  public static final String PROPERTY_APPLICATION = "Application";
  public static final String PROPERTY_USR_PROXY = "User_Proxy";
  public static final String PROPERTY_PASSWORD_PROXY = "Password_Proxy";
  public static final String PROPERTY_HOST_PROXY = "Host_Proxy";
  public static final String PROPERTY_PORT_PROXY = "Port_Proxy";

  protected ApiRestAdapter adapter = null;
  protected String apiRestAdapterType = null;
  protected Properties properties = null;

  public AbstractApiRestService() {
    super();
  }

  /** Get ServiceName */
  public abstract String getServiceName();

  /** Validate parameters */
  public abstract boolean validateParameters();

  /** Check the Properties */
  public abstract boolean checkProperties(final Properties properties);

  /** Call the Service */
  public abstract int callService();

  /**
   * Init Configure Service.
   *
   * Read Properties.
   *
   * Get ApiRestAdapter.
   *
   * @return
   */
  public boolean configureService() {

    boolean isConfigure = false;
    String debug = null;

    if (validateParameters()) {
      // Read Properties file
      final Properties properties = getProperties(getPropertyFileName());
      if (checkProperties(properties)) {

        // Set Properties
        setProperties(properties);

        // Get adapter
        final ApiRestAdapter adapter = getApiRestAdapter(properties, getApiRestAdapterType());
        setAdapter(adapter);

        isConfigure = true;

      } else {
        debug = (properties == null) ? "Can not access to service properties. "
            : "Some properties are incorrect. Check properties file.";
        Log.error(this, debug);
      }

    } else {
      debug = "Invalid parameters calling service : " + getServiceName();
      Log.error(this, debug);
    }

    return isConfigure;
  }

  /**
   * Get Property File Name
   *
   * @return
   */
  public String getPropertyFileName() {
    return PROPERTIES_FILE_NAME;
  }

  /**
   * Get Properties from fileName
   *
   * @param propertiesFileName
   * @return
   */
  public Properties getProperties(final String propertiesFileName) {
    return UtilRestServices.getPropertyFile(propertiesFileName);
  }

  /**
   * Check Properties
   */
  public boolean checkProperties(String... properties) {
    return Arrays.stream(properties).allMatch(prop -> !Util.isEmpty(prop));
  }

  /**
   * Get ApiRestAdapter.
   *
   * @param properties
   * @param adapterType
   * @return
   */
  public ApiRestAdapter getApiRestAdapter(final Properties properties, final String adapterType) {

    // Get Endpoint
    final ApiRestEndPoint endpoint = getApiRestEndPoint(properties);

    // From Properties
    final String sslalgorithm = getPropertyValue(PROPERTY_SSL_ALGORITHM, properties);
    final String ignorecertificate = getPropertyValue(PROPERTY_IGNORE_CERTIFICATE, properties);

    // Get Adapter
    final ApiRestAdapter adapter = getApiRestAdapter(adapterType, endpoint, sslalgorithm, ignorecertificate);
    return adapter;
  }

  /**
   * Get ApiRestAdapter.
   *
   * @param adapterType
   * @param endpoint
   * @param sslalgorithm
   * @param ignorecertificate
   * @return
   */
  public static ApiRestAdapter getApiRestAdapter(final String adapterType, final ApiRestEndPoint endpoint,
      final String sslalgorithm, final String ignorecertificate) {

    ApiRestAdapter adapter = null;
    if (endpoint != null) {
      adapter = (Util.isEmpty(adapterType)) ? FactoryApiRestAdapter.getApiRestService() : FactoryApiRestAdapter
          .getApiRestService(adapterType);
      adapter.init(endpoint);
      adapter.setSslAlgorithm(sslalgorithm);
      adapter.setIgnoreCertificate(ignorecertificate);
    }

    return adapter;
  }

  /**
   * Get ApiRestAdapter.
   *
   * @param adapterType
   * @param endpoint
   * @param credential
   * @param proxy
   * @param sslalgorithm
   * @param ignorecertificate
   * @return
   */
  public static ApiRestAdapter getApiRestAdapter(final String adapterType, final ApiRestEndPoint endpoint,
                                                 final ApiRestCredential credential, final ApiRestProxy proxy, final String sslalgorithm,
                                                 final String ignorecertificate) {

    ApiRestAdapter adapter = null;
    if (endpoint != null) {
      adapter = (Util.isEmpty(adapterType)) ? FactoryApiRestAdapter.getApiRestService() : FactoryApiRestAdapter
          .getApiRestService(adapterType);
      adapter.init(endpoint, credential, proxy);
      adapter.setSslAlgorithm(sslalgorithm);
      adapter.setIgnoreCertificate(ignorecertificate);
    }

    return adapter;
  }

  /**
   * Get ApiRestEndPoint
   *
   * @param properties
   * @return
   */
  public ApiRestEndPoint getApiRestEndPoint(final Properties properties) {

    // From Properties
    final String protocol = getPropertyValue(PROPERTY_PROTOCOL, properties);
    final String port = getPropertyValue(PROPERTY_PORT, properties);
    final String host = getPropertyValue(PROPERTY_HOST, properties);
    final String file = getPropertyValue(PROPERTY_FILE, properties);

    final ApiRestEndPoint endpoint = getEndPoint(protocol, port, host, file);
    return endpoint;
  }

  /**
   * Get Endpoint.
   *
   * @param protocol
   * @param port
   * @param host
   * @param file
   * @return
   */
  public static ApiRestEndPoint getEndPoint(final String protocol, final String port, final String host,
                                            final String file) {

    final int intPort = (!Util.isEmpty(port)) ? Integer.valueOf(port) : 0;

    final ApiRestEndPoint endpoint = new ApiRestEndPoint();
    endpoint.setProtocol(protocol);
    endpoint.setPort(intPort);
    endpoint.setHost(host);
    endpoint.setFile(file);

    return endpoint;
  }

  /**
   * Get ApiRestProxy.
   *
   * @param properties
   * @return
   */
  public ApiRestProxy getApiRestProxy(final Properties properties) {

    // From Properties
    final String userProxy = getPropertyValue(PROPERTY_USR_PROXY, properties);
    final String passwordProxy = getPropertyValue(PROPERTY_PASSWORD_PROXY, properties);
    final String portProxy = getPropertyValue(PROPERTY_PORT_PROXY, properties);
    final String hostProxy = getPropertyValue(PROPERTY_HOST_PROXY, properties);

    final ApiRestProxy proxy = getApiRestProxy(userProxy, passwordProxy, portProxy, hostProxy);
    return proxy;
  }

  /**
   * Get ApiRestProxy.
   *
   * @param user
   * @param password
   * @param port
   * @param host
   * @return
   */
  public static ApiRestProxy getApiRestProxy(final String user, final String password, final String port,
                                             final String host) {

    final int intPort = (!Util.isEmpty(port)) ? Integer.valueOf(port) : 0;

    final ApiRestProxy proxy = new ApiRestProxy();
    proxy.setHost(host);
    proxy.setUser(user);
    proxy.setPassword(password);
    proxy.setPort(intPort);
    return proxy;
  }

  /**
   * Get ApiRestCredential.
   *
   * @param properties
   * @param credentialType
   * @param credentialToken
   * @return
   */
  public ApiRestCredential getApiRestCredential(final Properties properties, final String credentialType,
                                                final String credentialToken) {

    // From Properties
    final String user = getPropertyValue(PROPERTY_USR, properties);
    final String password = getPropertyValue(PROPERTY_PASSWORD, properties);

    final ApiRestCredential credential = getApiRestCredential(credentialType, user, password, credentialToken);
    return credential;
  }

  /**
   * Build Credential.
   *
   * @param credentialType
   * @param user
   * @param password
   * @param credentialToken
   * @return
   */
  protected ApiRestCredential getApiRestCredential(final String credentialType, final String user,
                                                   final String password, final String credentialToken) {
    final ApiRestCredential credential = new ApiRestCredential();
    credential.setType(credentialType);
    credential.setUser(user);
    credential.setPassword(password);
    credential.setToken(credentialToken);
    return credential;
  }

  /**
   * Get Property Value.
   *
   * @param propertyName
   * @param properties
   * @return
   */
  public String getPropertyValue(final String propertyName, final Properties properties) {
    String value = "";
    String name = "";

    if (propertyName != null) {
      name = (propertyName.startsWith("_")) ? getServiceName().concat(propertyName) : getServiceName().concat("_")
          .concat(propertyName);
      value = properties.getProperty(name);
    }

    return value;
  }


  // Setters and Getters

  public ApiRestAdapter getAdapter() {
    return adapter;
  }

  public void setAdapter(ApiRestAdapter adapter) {
    this.adapter = adapter;
  }

  public String getApiRestAdapterType() {
    return apiRestAdapterType;
  }

  public void setApiRestAdapterType(String apiRestAdapterType) {
    this.apiRestAdapterType = apiRestAdapterType;
  }

  public Properties getProperties() {
    return properties;
  }

  public void setProperties(Properties properties) {
    this.properties = properties;
  }

}
