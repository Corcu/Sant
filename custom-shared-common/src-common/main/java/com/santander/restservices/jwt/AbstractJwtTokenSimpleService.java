package com.santander.restservices.jwt;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.santander.restservices.*;
import com.santander.restservices.jwt.model.JwtTokenSimpleErrors;
import com.santander.restservices.jwt.model.JwtTokenSimpleOutput;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public abstract class AbstractJwtTokenSimpleService extends AbstractApiRestService {

  public static final String PREFIX_JWT_TOKEN_SIMPLE_SERVICE = "JwtToken_Simple";

  protected JwtTokenSimpleOutput output;
  protected JwtTokenSimpleErrors error;

  protected String serviceName;
  protected String propertyFile;
  protected boolean isProxy;
  protected String credentialType;
  protected String credentialToken;

  public AbstractJwtTokenSimpleService() {
    super();
    output = new JwtTokenSimpleOutput();
    error = new JwtTokenSimpleErrors();
  }

  public AbstractJwtTokenSimpleService(final String serviceName) {
    super();
    output = new JwtTokenSimpleOutput();
    error = new JwtTokenSimpleErrors();
    this.serviceName = serviceName;
  }

  public AbstractJwtTokenSimpleService(final String serviceName, final String propertyFile, final boolean isProxy,
      final String apiRestAdapterType, final String credentialType) {
    super();
    output = new JwtTokenSimpleOutput();
    error = new JwtTokenSimpleErrors();
    this.serviceName = serviceName;
    this.propertyFile = propertyFile;
    this.isProxy = isProxy;
    this.apiRestAdapterType = apiRestAdapterType;
    this.credentialType = credentialType;
  }

  @Override
  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getPropertyFile() {
    return propertyFile;
  }

  public void setPropertyFile(String propertyFile) {
    this.propertyFile = propertyFile;
  }

  public boolean isProxy() {
    return isProxy;
  }

  public void setProxy(boolean isProxy) {
    this.isProxy = isProxy;
  }

  public String getCredentialType() {
    return credentialType;
  }

  public void setCredentialType(String credentialType) {
    this.credentialType = credentialType;
  }

  public String getCredentialToken() {
    return credentialToken;
  }

  public void setCredentialToken(String credentialToken) {
    this.credentialToken = credentialToken;
  }

  @Override
  public ApiRestModel getInput() {
    return null;
  }

  @Override
  public JwtTokenSimpleOutput getOutput() {
    return output;
  }

  @Override
  public JwtTokenSimpleErrors getError() {
    return error;
  }

  private void setOutput(JwtTokenSimpleOutput output) {
    this.output.loadModelData(output);
  }

  private void setError(JwtTokenSimpleErrors error) {
    this.error.loadModelData(error);
  }

  @Override
  public boolean validateParameters() {
    return !Util.isEmpty(credentialType);
  }

  @Override
  public String getPropertyFileName() {
    return (!Util.isEmpty(getPropertyFile())) ? getPropertyFile() : super.getPropertyFileName();
  }

  /**
   * Init Service
   */
  @Override
  public boolean configureService() {

    // Configure Credential Type
    setCredentialType(ApiRestCredential.BASIC_CREDENTIAL_TYPE);

    return super.configureService();
  }

  /**
   * Call Service
   *
   * @param adapter
   * @return
   */
  @Override
  public int callService() {

    int status = -1;
    String debug = null;

    // init - read Properties and Get ApiRestAdapter
    if (configureService()) {

      final JwtTokenSimpleOutput out = new JwtTokenSimpleOutput();
      final JwtTokenSimpleErrors error = new JwtTokenSimpleErrors();

      // Headers
      final Map<String, String> headers = getHeaders();

      // Call POST
      status = getAdapter().post(null, out, error, headers);

      if (ApiRestAdapter.isOKHttpStatus(status)) {
        setOutput(out);
      } else {
        setError(error);

        debug = String.format("Error calling JwtTokenSimple service: status [%s]", status);
        Log.error(this, debug);
      }

    } else {
      debug = "Error initializing service : " + getServiceName();
      Log.debug(this, debug);
    }

    return status;
  }

  /**
   * Get Headers
   *
   * @return
   */
  public Map<String, String> getHeaders() {
    final Map<String, String> headers = new HashMap<String, String>();
    headers.put(ApiRestAdapter.HTTP_HEADER_CONTENT_TYPE, ApiRestAdapter.MIME_TYPE_JSOM);
    headers.put(ApiRestAdapter.HTTP_HEADER_ACCEPT, ApiRestAdapter.MIME_TYPE_JSOM);
    return headers;
  }

  /**
   * Get ApiRestAdapter.
   *
   * @param properties
   * @param adapterType
   * @return
   */
  @Override
  public ApiRestAdapter getApiRestAdapter(final Properties properties, final String adapterType) {

    // Get Endpoint
    final ApiRestEndPoint endpoint = getApiRestEndPoint(properties);

    // Get Credential
    final ApiRestCredential credential = getApiRestCredential(properties, getCredentialType(), getCredentialToken());

    // Get Proxy
    final ApiRestProxy proxy = (isProxy()) ? getApiRestProxy(properties) : null;

    // From Properties
    final String sslalgorithm = getPropertyValue(PROPERTY_SSL_ALGORITHM, properties);
    final String ignorecertificate = getPropertyValue(PROPERTY_IGNORE_CERTIFICATE, properties);

    // Get Adapter
    final ApiRestAdapter adapter = getApiRestAdapter(adapterType, endpoint, credential, proxy, sslalgorithm,
        ignorecertificate);

    return adapter;
  }

  /**
   * Check all defined properties.
   *
   * @param properties
   * @return
   */
  @Override
  public boolean checkProperties(final Properties properties) {

    boolean check = false;
    if (properties != null) {

      final String user = getPropertyValue(PROPERTY_USR, properties);
      final String password = getPropertyValue(PROPERTY_PASSWORD, properties);
      final String protocol = getPropertyValue(PROPERTY_PROTOCOL, properties);
      final String port = getPropertyValue(PROPERTY_PORT, properties);
      final String host = getPropertyValue(PROPERTY_HOST, properties);
      final String file = getPropertyValue(PROPERTY_FILE, properties);
      check = checkProperties(user, password, protocol, port, host, file);

      if (!check) {
        final String debug = String.format(
            "Invalid service properties: [%s] - protocol [%s] port [%s] host [%s] file [%s]", getPropertyFileName(),
            protocol, port, host, file);
        Log.error(this, debug);
      }
    } else {
      final String debug = "Can not access to service properties. ";
      Log.error(this, debug);
    }
    return check;

  }

  /**
   * Get Property Value.
   *
   * First, property name search with prefix. Second, property name search only with service name.
   *
   * @param name
   * @param properties
   * @return
   */
  @Override
  public String getPropertyValue(final String name, final Properties properties) {

    String propertyValue = "";
    String propertyName = "";
    String prefix = "";

    if (name != null) {

      // Get prefix
      prefix = getPrefixProperty();

      // Get PropertyValue: with prefix
      propertyName = (name.contains("_")) ? prefix.concat(name) : prefix.concat("_").concat(name);
      propertyValue = properties.getProperty(propertyName);

      if (Util.isEmpty(propertyValue)) {
        // only with serviceName
        propertyName = (name.contains("_")) ? getServiceName().concat(name) : getServiceName().concat("_").concat(name);
        propertyValue = properties.getProperty(propertyName);
      }

    }

    return propertyValue;
  }

  /**
   * Build the prefix property.
   *
   * @return
   */
  protected String getPrefixProperty() {

    // Build Prefix
    final StringBuilder prefix = new StringBuilder(PREFIX_JWT_TOKEN_SIMPLE_SERVICE);
    if (!Util.isEmpty(getServiceName())) {
      prefix.append("_").append(getServiceName());
    }

    return prefix.toString();
  }
}
