package com.santander.restservices.datasign;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.santander.restservices.AbstractApiRestService;
import com.santander.restservices.ApiRestAdapter;
import com.santander.restservices.ApiRestCredential;
import com.santander.restservices.ApiRestEndPoint;
import com.santander.restservices.datasign.model.DataSignError;
import com.santander.restservices.datasign.model.DataSignInput;
import com.santander.restservices.datasign.model.DataSignOutput;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DataSignService extends AbstractApiRestService {

  public static final String SERVICE_NAME = "DataSign";

  public static final String JWT_TYPE = "JWT";
  public static final String REJECTED_TIME_PROPERTY = "_RejectedTime";
  public static final String TYPE_PROPERTY = "_Type";

  private String token = null;
  private String credentialType;
  private String credentialToken;

  private DataSignInput input = null;
  private DataSignOutput output = null;
  private DataSignError error = null;

  public DataSignService() {
    super();
    input = new DataSignInput();
    output = new DataSignOutput();
    error = new DataSignError();
  }

  public DataSignService(final DataSignInput input) {
    this();
    this.input = input;
    output = new DataSignOutput();
    error = new DataSignError();
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  @Override
  public DataSignInput getInput() {
    return input;
  }

  @Override
  public DataSignOutput getOutput() {
    return output;
  }

  @Override
  public DataSignError getError() {
    return error;
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

  @Override
  public boolean validateParameters() {
    return getToken() != null;
  }

  /**
   * Check Properties.
   *
   * @return
   *
   */
  @Override
  public boolean checkProperties(final Properties properties) {
    if (properties != null) {
      // Load properties
      final String protocol = getPropertyValue(PROPERTY_PROTOCOL, properties);
      final String port = getPropertyValue(PROPERTY_PORT, properties);
      final String host = getPropertyValue(PROPERTY_HOST, properties);
      final String file = getPropertyValue(PROPERTY_FILE, properties);

      // Check properties read.
      if (checkProperties(protocol, port, host, file)) {
        return true;
      } else {
        final String debug = String.format(
            "Invalid service properties: [%s] - protocol [%s] port [%s] host [%s] file [%s]", getPropertyFileName(),
            protocol, port, host, file);
        Log.error(this, debug);

      }
    }

    return false;
  }

  @Override
  public boolean configureService() {

    // Configure Credential Type and Token
    setCredentialType(ApiRestCredential.BEARER_CREDENTIAL_TYPE);
    setCredentialToken(getToken()); // JWT token

    return super.configureService();
  }

  /**
   * Call Service.
   *
   * @param adapter.
   */
  @Override
  public int callService() {

    int status = -1;
    String debug = null;

    // init - read Properties and Get ApiRestAdapter
    if (configureService()) {

      // Headers
      final Map<String, String> headers = getHeaders();

      // init
      final DataSignInput in = new DataSignInput(getInput());
      enrichDataSignInput(in, getProperties());

      final DataSignOutput out = new DataSignOutput();
      final DataSignError err = new DataSignError();

      if (in.checkModelDataLoaded()) {

        // Do POST
        status = getAdapter().post(in, out, err, headers);

        if (ApiRestAdapter.isOKHttpStatus(status)) {
          // Set output
          output = out;

        } else {
          // Set Errors
          error = err;

          debug = String.format("Error calling DataSign service : status [%s]. Description : %s ", status,
              err.toString());
          Log.error(this, debug);
        }
      } else {
        debug = String.format("Input DataSign service error : check DataSignInput model data. ");
        Log.error(this, debug);
      }

    } else {
      debug = "Error initializing service : " + getServiceName();
      Log.debug(this, debug);
    }


    return status;
  }

  /**
   * Get Request Headers
   *
   * @return
   */
  private Map<String, String> getHeaders() {
    final Map<String, String> headers = new HashMap<String, String>();
    headers.put(ApiRestAdapter.HTTP_HEADER_CONTENT_TYPE, ApiRestAdapter.MIME_TYPE_JSOM); // Content-type
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

    // From Properties
    final String sslalgorithm = getPropertyValue(PROPERTY_SSL_ALGORITHM, properties);
    final String ignorecertificate = getPropertyValue(PROPERTY_IGNORE_CERTIFICATE, properties);

    // Get Adapter
    final ApiRestAdapter adapter = getApiRestAdapter(adapterType, endpoint, credential, null, sslalgorithm,
        ignorecertificate);

    return adapter;
  }

  /**
   * Enrich the DataSignInput
   *
   * @param in
   * @param properties
   */
  private void enrichDataSignInput(final DataSignInput in, final Properties properties) {
    // Algorithm
    final String sslalgorithm = getPropertyValue(PROPERTY_SSL_ALGORITHM, properties);
    if (!Util.isEmpty(sslalgorithm)) {
      in.setAlgorithm(sslalgorithm);
    }

    // NBF - rejectedTime
    final String rejectedTime = getPropertyValue(REJECTED_TIME_PROPERTY, properties);
    if (!Util.isEmpty(rejectedTime) && isCreatable(rejectedTime)) {
      in.setRejectedTime(Integer.parseInt(rejectedTime));
    }

    // typ - type
    final String type = getPropertyValue(TYPE_PROPERTY, properties);
    if (!Util.isEmpty(type)) {
      in.setType(type);
    } else {
      in.setType(JWT_TYPE); // By Default
    }

  }


  public static boolean isCreatable(String str) {
    if (Util.isEmpty(str)) {
      return false;
    } else {
      char[] chars = str.toCharArray();
      int sz = chars.length;
      boolean hasExp = false;
      boolean hasDecPoint = false;
      boolean allowSigns = false;
      boolean foundDigit = false;
      int start = chars[0] != '-' && chars[0] != '+' ? 0 : 1;
      int i;
      if (sz > start + 1 && chars[start] == '0') {
        if (chars[start + 1] == 'x' || chars[start + 1] == 'X') {
          i = start + 2;
          if (i == sz) {
            return false;
          }
          while(i < chars.length) {
            if ((chars[i] < '0' || chars[i] > '9') && (chars[i] < 'a' || chars[i] > 'f') && (chars[i] < 'A' || chars[i] > 'F')) {
              return false;
            }
            ++i;
          }
          return true;
        }
        if (Character.isDigit(chars[start + 1])) {
          for(i = start + 1; i < chars.length; ++i) {
            if (chars[i] < '0' || chars[i] > '7') {
              return false;
            }
          }
          return true;
        }
      }
      --sz;
      for(i = start; i < sz || i < sz + 1 && allowSigns && !foundDigit; ++i) {
        if (chars[i] >= '0' && chars[i] <= '9') {
          foundDigit = true;
          allowSigns = false;
        } else if (chars[i] == '.') {
          if (hasDecPoint || hasExp) {
            return false;
          }
          hasDecPoint = true;
        } else if (chars[i] != 'e' && chars[i] != 'E') {
          if (chars[i] != '+' && chars[i] != '-') {
            return false;
          }
          if (!allowSigns) {
            return false;
          }
          allowSigns = false;
          foundDigit = false;
        } else {
          if (hasExp) {
            return false;
          }
          if (!foundDigit) {
            return false;
          }
          hasExp = true;
          allowSigns = true;
        }
      }
      if (i < chars.length) {
        if (chars[i] >= '0' && chars[i] <= '9') {
          return true;
        } else if (chars[i] != 'e' && chars[i] != 'E') {
          if (chars[i] == '.') {
            return !hasDecPoint && !hasExp ? foundDigit : false;
          } else if (!allowSigns && (chars[i] == 'd' || chars[i] == 'D' || chars[i] == 'f' || chars[i] == 'F')) {
            return foundDigit;
          } else if (chars[i] != 'l' && chars[i] != 'L') {
            return false;
          } else {
            return foundDigit && !hasExp && !hasDecPoint;
          }
        } else {
          return false;
        }
      } else {
        return !allowSigns && foundDigit;
      }
    }
  }


}
