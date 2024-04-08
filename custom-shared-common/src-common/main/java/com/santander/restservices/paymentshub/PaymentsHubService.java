package com.santander.restservices.paymentshub;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.santander.restservices.AbstractApiRestService;
import com.santander.restservices.ApiRestAdapter;
import com.santander.restservices.ApiRestCredential;
import com.santander.restservices.ApiRestEndPoint;
import com.santander.restservices.ApiRestProxy;
import com.santander.restservices.FactoryApiRestAdapter;
import com.santander.restservices.paymentshub.model.PaymentsHubErrors;
import com.santander.restservices.paymentshub.model.PaymentsHubInput;
import com.santander.restservices.paymentshub.model.PaymentsHubOutput;

public class PaymentsHubService extends AbstractApiRestService {

  public static final String SERVICE_NAME = "PaymentsHub";

  private static final String LOG_CATEGORY = PaymentsHubService.class.getName();

  private String accessToken = null;
  private String scaToken = null;
  private Map<String, List<String>> responseHeaders = null;

  private String credentialType;
  private String credentialToken;
  private boolean isProxy;

  PaymentsHubInput input = null;
  PaymentsHubOutput output = null;
  PaymentsHubErrors errors = null;

  public PaymentsHubService() {
    super();
    input = new PaymentsHubInput();
    output = new PaymentsHubOutput();
    errors = new PaymentsHubErrors();
    responseHeaders = new HashMap<String, List<String>>();
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getScaToken() {
    return scaToken;
  }

  public void setScaToken(String scaToken) {
    this.scaToken = scaToken;
  }

  public Map<String, List<String>> getResponseHeaders() {
    return (responseHeaders != null) ? new HashMap<String, List<String>>(responseHeaders) : null; // Kiuwan
  }

  public void addResponseHeader(final String key, final List<String> value) {
    responseHeaders.put(key, value);
  }

  public void setErrors(PaymentsHubErrors errors) {
    this.errors.loadModelData(errors);
  }

  public void setInput(PaymentsHubInput input) {
    this.input.loadModelData(input);
  }

  public void setOutput(PaymentsHubOutput output) {
    this.output.loadModelData(output);
  }

  @Override
  public PaymentsHubInput getInput() {
    return input;
  }

  @Override
  public PaymentsHubOutput getOutput() {
    return output;
  }

  @Override
  public PaymentsHubErrors getError() {
    return errors;
  }

  @Override
  public String getServiceName() {
    return SERVICE_NAME;
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

  public boolean isProxy() {
    return isProxy;
  }

  public void setProxy(boolean isProxy) {
    this.isProxy = isProxy;
  }

  @Override
  public boolean validateParameters() {
    return !Util.isEmpty(getAccessToken()) && !Util.isEmpty(getScaToken());
  }

  /**
   * Check Properties.
   *
   * @return
   *
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

        Log.error(LOG_CATEGORY, debug);
      }

    } else {
      debug = String.format("Invalid service properties: [%s] - No Properties found.", getPropertyFileName());
      Log.error(LOG_CATEGORY, debug);
    }

    return check;
  }

  @Override
  public boolean configureService() {

    // Configure ApiRestAdapter Type
    setApiRestAdapterType(FactoryApiRestAdapter.PAYMENTSHUB_APIREST_ADAPTER);

    // Configure Credential Type and Token
    setCredentialType(ApiRestCredential.BEARER_CREDENTIAL_TYPE);
    setCredentialToken(getAccessToken()); // OAuth access token

    return super.configureService();
  }

  /**
   * Call Service.
   *
   * @param adapter.
   */
  @Override
  public int callService() {

    int status = 0;
    final PaymentsHubInput in = new PaymentsHubInput(getInput());
    final PaymentsHubOutput out = new PaymentsHubOutput();
    final PaymentsHubErrors err = new PaymentsHubErrors();

    // init - read Properties and Get ApiRestAdapter
    if (configureService()) {

      // Headers
      final Map<String, String> headers = getHeaders(getProperties());

      // Check PaymentsHubInput
      final boolean isInputOk = in.checkModelDataLoaded();
      if (isInputOk) {

        // Do POST
        status = getAdapter().post(in, out, err, headers);

        if (ApiRestAdapter.isOKHttpStatus(status)) { // Status == 2xx
          // Set output
          setOutput(out);

        } else { // Status != 2xx
          // Handler Message Errors
          handlerMessageError(status, err);
        }

        // Actions post call service
        postCallService();

      } else {
        // Check PaymentsHubInput
        handlerInputError(in, err);
      }

    } else {
      // PaymentsHub service call could not be started
      handlerConfigureServiceError(err);
    }

    return status;

  }

  /**
   * Actions post call Service.
   */
  private void postCallService() {
    // Get Response Headers
    if (getAdapter() instanceof PaymentsHubApiRestAdapter) {
      final PaymentsHubApiRestAdapter phAdapter = (PaymentsHubApiRestAdapter) getAdapter();
      if (phAdapter.getResponseHeaders() != null) {
        phAdapter.getResponseHeaders().forEach((header, value) -> {
          if (header != null) {
            addResponseHeader(header.toString(), value);
          }
        });
      }
    }
  }

  /**
   * Get Request Headers
   *
   * @param properties
   * @return
   */
  private Map<String, String> getHeaders(final Properties properties) {

    final Map<String, String> headers = new HashMap<String, String>();

    // Client ID
    final String clientId = getPropertyValue(PROPERTY_USR, properties);

    headers.put(ApiRestAdapter.HTTP_HEADER_SCA_TOKEN, getScaToken()); // JWT token for payload
    // validation
    headers.put(ApiRestAdapter.HTTP_HEADER_X_SANTANDER_CLIENTID, clientId); // Client ID
    headers.put(ApiRestAdapter.HTTP_HEADER_CONTENT_TYPE, ApiRestAdapter.MIME_TYPE_JSOM);

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
   * Handler errors when the Service Output is Status != 2xx.
   *
   * @param status
   * @param errors
   */
  private void handlerMessageError(final int status, final PaymentsHubErrors errors) {
    String msgError = "";

    // Get possible adapter errors
    String adapterErrors = "";
    if (getAdapter() instanceof PaymentsHubApiRestAdapter) {
      final PaymentsHubApiRestAdapter adapter = (PaymentsHubApiRestAdapter) getAdapter();
      final List<String> adapterErrorsList = adapter.getApiRestAdapterErrors();
      adapterErrors = !Util.isEmpty(adapterErrorsList) ? StringUtils.join(adapterErrorsList, ";") : "";
    }

    // Check if the PaymentsHub service call could be initiated
    if (status > 0) {
      final String cause = !Util.isEmpty(errors.toString()) ? errors.toString().concat(" ").concat(adapterErrors)
              : adapterErrors;
      msgError = String.format("Error calling PaymentsHub service : status [%s]. Causes : %s ", status, cause);
    } else {
      // PaymentsHub service call could not be started
      msgError = "Error calling PaymentsHub service : PaymentsHub service call could not be started. "
              .concat(adapterErrors);

      // Set error Message
      errors.setMessage(msgError);
    }

    // Set Errors
    setErrors(errors);

    Log.error(LOG_CATEGORY, msgError);

  }

  /**
   * Handler errors when the Input is incorrect.
   *
   * @param input
   * @param errors
   */
  private void handlerInputError(final PaymentsHubInput input, final PaymentsHubErrors errors) {
    // The JSON is incorrect.
    String msgError = "Error checking PaymentsHub Input : the JSON text does not include required fields or they have wrong values.";
    errors.setMessage(msgError);

    if (!Util.isEmpty(input.getMessageError())) {
      final String[] inputMsgErrors = input.getMessageError().split("\\|");
      msgError = "Error checking PaymentsHub Input : " + input.getMessageError();
      errors.setMessage("Error checking PaymentsHub Input : " + inputMsgErrors[0]);
    }

    // Set Errors
    setErrors(errors);

    Log.error(LOG_CATEGORY, msgError);
  }

  /**
   * Handler ConfigureService Error
   *
   * @param errors
   */
  private void handlerConfigureServiceError(final PaymentsHubErrors errors) {
    // PaymentsHub service call could not be started
    final String msgError = String.format("Error initializing PaymentsHub service : %s. Check the Properties file. ",
            getServiceName());

    // Set error Message
    errors.setMessage(msgError);

    // Set Errors
    setErrors(errors);

    Log.error(LOG_CATEGORY, msgError);
  }

}
