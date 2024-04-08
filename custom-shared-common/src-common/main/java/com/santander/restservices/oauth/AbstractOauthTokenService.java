package com.santander.restservices.oauth;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.santander.restservices.*;
import com.santander.restservices.model.FormDataInput;
import com.santander.restservices.oauth.model.OauthTokenError;
import com.santander.restservices.oauth.model.OauthTokenOutput;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public abstract class AbstractOauthTokenService extends AbstractApiRestService {

  public static final String PREFIX_OUATH_TOKEN_SERVICE = "OauthToken";

  protected String jwt;
  protected String scope;
  protected String grant;
  protected OauthTokenOutput output;
  protected OauthTokenError error;

  protected String serviceName;
  protected String propertyFile;
  protected boolean isProxy;
  protected String credentialType;
  protected String credentialToken;

  public AbstractOauthTokenService() {
    super();
    output = new OauthTokenOutput();
    error = new OauthTokenError();
  }

  public AbstractOauthTokenService(final String serviceName) {
    super();
    output = new OauthTokenOutput();
    error = new OauthTokenError();
    this.serviceName = serviceName;
  }

  public AbstractOauthTokenService(final String serviceName, final String propertyFile, final boolean isProxy,
      final String apiRestAdapterType, final String credentialType) {
    super();
    output = new OauthTokenOutput();
    error = new OauthTokenError();
    this.serviceName = serviceName;
    this.propertyFile = propertyFile;
    this.isProxy = isProxy;
    this.apiRestAdapterType = apiRestAdapterType;
    this.credentialType = credentialType;
  }

  public void setJwtToken(String jwt) {
    this.jwt = jwt;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public void setGrantType(String grant) {
    this.grant = grant;
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
  public OauthTokenOutput getOutput() {
    return output;
  }

  @Override
  public OauthTokenError getError() {
    return error;
  }

  private void setOutput(OauthTokenOutput output) {
    this.output.loadModelData(output);
  }

  private void setError(OauthTokenError error) {
    this.error.loadModelData(error);
  }

  @Override
  public String getPropertyFileName() {
    return (!Util.isEmpty(getPropertyFile())) ? getPropertyFile() : super.getPropertyFileName();
  }

  @Override
  public boolean validateParameters() {
    return (jwt != null);
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
   * Call Service with a ApiRestAdapter
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

      final FormDataInput in = getFormDataInput();
      final OauthTokenOutput out = new OauthTokenOutput();
      final OauthTokenError error = new OauthTokenError();

      if (in.checkModelDataLoaded()) {

        // Headers
        final Map<String, String> headers = getHeaders();

        // Call POST
        status = getAdapter().post(in, out, error, headers);

        if (ApiRestAdapter.isOKHttpStatus(status)) {
          setOutput(out);
        } else {
          setError(error);

          debug = String.format("Error calling OauthToken service: status [%s]", status);
          Log.error(this, debug);
        }

      } else {
        debug = String.format("Input service error : check Input model data. ");
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
    headers.put(ApiRestAdapter.HTTP_HEADER_CONTENT_TYPE, ApiRestAdapter.MIME_TYPE_URL_FORM_ENCOCODED);
    return headers;
  }

  /**
   * Get FormDataInput
   *
   * @return
   */
  public FormDataInput getFormDataInput() {
    final FormDataInput in = new FormDataInput();
    in.put(ApiRestAdapter.HTTP_HEADER_AUTHORIZATION_SCOPE, scope);
    in.put(ApiRestAdapter.HTTP_HEADER_AUTHORIZATION_GRANT_TYPE, grant);
    in.put(ApiRestAdapter.HTTP_HEADER_AUTHORIZATION_ASSERTION, jwt);
    return in;
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

    String debug = "";
    boolean check = false;

    if (properties != null) {

      // Load properties
      final String user = getPropertyValue(PROPERTY_USR, properties);
      final String protocol = getPropertyValue(PROPERTY_PROTOCOL, properties);
      final String port = getPropertyValue(PROPERTY_PORT, properties);
      final String host = getPropertyValue(PROPERTY_HOST, properties);
      final String file = getPropertyValue(PROPERTY_FILE, properties);
      final String userProxy = getPropertyValue(PROPERTY_USR_PROXY, properties);
      final String portProxy = getPropertyValue(PROPERTY_PORT_PROXY, properties);
      final String hostProxy = getPropertyValue(PROPERTY_HOST_PROXY, properties);

      check = checkProperties(user, protocol, port, host, file);

      if (isProxy()) {
        check &= checkProperties(userProxy, portProxy, hostProxy);
      }

      // Check properties read.
      if (!check) {
        final String userOk = !Util.isEmpty(user) ? "OK" : "KO";
        final String protocolOk = !Util.isEmpty(protocol) ? "OK" : "KO";
        final String portOk = !Util.isEmpty(port) ? "OK" : "KO";
        final String hostOk = !Util.isEmpty(host) ? "OK" : "KO";
        final String fileOk = !Util.isEmpty(file) ? "OK" : "KO";
        debug = String.format(
            "Invalid service properties: [%s] - user [%s] protocol [%s] port [%s] host [%s] file [%s]",
            getPropertyFileName(), userOk, protocolOk, portOk, hostOk, fileOk);

        if (isProxy()) {
          final String userProxyOk = !Util.isEmpty(userProxy) ? "OK" : "KO";
          final String portProxyOk = !Util.isEmpty(portProxy) ? "OK" : "KO";
          final String hostProxyOk = !Util.isEmpty(hostProxy) ? "OK" : "KO";
          debug = debug
              + String.format(
                  " - Invalid service Proxy properties: [%s] - user proxy [%s] port proxy [%s] host proxy [%s]",
                  getPropertyFileName(), userProxyOk, portProxyOk, hostProxyOk);
        }

        Log.error(this, debug);
      }

    } else {
      debug = String.format("Invalid service properties: [%s] - No Properties found.", getPropertyFileName());
      Log.debug(this, debug);
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
      propertyName = (name.startsWith("_")) ? prefix.concat(name) : prefix.concat("_").concat(name);
      propertyValue = properties.getProperty(propertyName);

      if (Util.isEmpty(propertyValue)) {
        // only with serviceName
        propertyName = (name.startsWith("_")) ? getServiceName().concat(name) : getServiceName().concat("_").concat(
            name);
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
    final StringBuilder prefix = new StringBuilder(PREFIX_OUATH_TOKEN_SERVICE);
    if (!Util.isEmpty(getServiceName())) {
      prefix.append("_").append(getServiceName());
    }

    return prefix.toString();
  }
}
