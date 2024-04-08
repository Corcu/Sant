package com.santander.restservices.paymentshub;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.santander.restservices.ApiRestAdapter;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.HttpApiRestAdapter;

public class PaymentsHubApiRestAdapter extends HttpApiRestAdapter {

  private String boundary = null;
  private final Map<String, List<String>> responseHeaders;
  private final List<String> phApiRestAdapterErrors;

  public PaymentsHubApiRestAdapter() {
    super();
    boundary = UUID.randomUUID().toString();
    responseHeaders = new HashMap<String, List<String>>();
    phApiRestAdapterErrors = new ArrayList<String>();
  }

  public Map<String, List<String>> getResponseHeaders() {
    return (responseHeaders != null) ? new HashMap<String, List<String>>(responseHeaders) : null; // Kiuwan
  }

  public void addResponseHeader(final String key, final List<String> value) {
    responseHeaders.put(key, value);
  }

  private void addApiRestAdapterError(final String error) {
    phApiRestAdapterErrors.add(error);
  }

  public List<String> getApiRestAdapterErrors() {
    return (phApiRestAdapterErrors != null) ? new ArrayList<String>(phApiRestAdapterErrors) : null; // Kiuwan
  }

  @Override
  public <T extends ApiRestModel, K extends ApiRestModel, R extends ApiRestModel> int post(T in, K out, R error,
                                                                                           Map<String, String> headers) {
    int status = -1;
    List<ApiRestModel> errors = null;

    errors = new ArrayList<>();
    errors.add(error);

    status = doPost(in, out, errors, headers);

    return status;
  }

  @Override
  public <T extends ApiRestModel, K extends ApiRestModel, R extends ApiRestModel> int post(T in, K out, List<R> errors,
                                                                                           Map<String, String> headers) {
    return doPost(in, out, errors, headers);
  }

  /**
   * Do POST.
   *
   * @param in
   * @param out
   * @param errors
   * @param headers
   * @return
   */
  private <T extends ApiRestModel, K extends ApiRestModel, R extends ApiRestModel> int doPost(T in, K out,
                                                                                              List<R> errors, Map<String, String> headers) {
    boolean control = false;
    HttpURLConnection connection = null;
    String uri = null;
    String contentType = null;
    String disposition = null;
    int status = -1;

    String debug = null;

    try {
      if ((endpoint != null) && (endpoint.isValid())) {
        uri = endpoint.getEndPoint();

        connection = createConnection(uri, "POST");

        if (connection != null) {
          debug = String.format("Request api rest method post, URI %s ", uri);
          Log.info(this, debug);

          if (headers != null) {
            // Add Headers
            addHeaders(connection, headers);
          }

          // Write Request
          final String mime = connection.getRequestProperty(HTTP_HEADER_CONTENT_TYPE);
          control = writeRequest(mime, connection.getOutputStream(), in);

          if (!control) {
            debug = "Could not write api rest request with method post. ";
            addApiRestAdapterError(debug);
            Log.error(this, debug);
          }

          connection.connect();

          // Write Response
          status = connection.getResponseCode();
          contentType = connection.getContentType();
          disposition = connection.getHeaderField(HTTP_HEADER_CONTENT_DISPOSITION);

          // Fill response headers
          connection.getHeaderFields().forEach((k, v) -> {
            if (k != null) {
              addResponseHeader(k.toString(), v);
            }
          });

          if (ApiRestAdapter.isOKHttpStatus(status)) {
            debug = String.format("Response api rest method post: URI: %s.", uri);
            Log.info(this, debug);

            control = readResponse(contentType, disposition, connection.getInputStream(), out);

            if (!control) {
              debug = "Could not read api rest response with method post.";
              Log.error(this, debug);
            }
          } else {
            debug = String.format("Not OK request status calling api rest method post, URI: %s STATUS: %s ", uri,
                    String.valueOf(status));
            Log.warn(this, debug);

            if (!Util.isEmpty(contentType) && (contentType.toUpperCase().startsWith(MIME_TYPE_JSOM.toUpperCase()))) {
              control = readJsonResponse(connection.getErrorStream(), errors);
            } else {
              control = readTextResponse(connection.getErrorStream(), errors);
            }

            if (!control) {
              debug = "Could not read api rest response whit method post.";
              Log.error(this, debug);
            }
          }

          connection.disconnect();
        } else { // connection is null
          debug = String.format("Error calling api rest method post: URI: %s ", uri);
          addApiRestAdapterError(debug);
        }
      } else { // endpoint fail
        debug = String.format("The EndPoint %s is not valid ", endpoint);
        addApiRestAdapterError(debug);
      }
    } catch (final IOException e) {
      debug = String.format("Error calling api rest method post: URI: %s with exception: %s - %s", uri, e.toString(),
              e.getMessage());
      Log.error(this, debug);
      addApiRestAdapterError(debug);
    }

    return status;
  }

  /**
   * Add Headers to URL Connection.
   *
   * @param connection
   * @param headers
   */
  protected void addHeaders(final HttpURLConnection connection, final Map<String, String> headers) {

    String contentType = null;
    String accept = null;

    // Content-Type header
    contentType = !Util.isEmpty(headers.get(HTTP_HEADER_CONTENT_TYPE)) ? headers.get(HTTP_HEADER_CONTENT_TYPE)
            : MIME_TYPE_JSOM;

    if (MIME_TYPE_MULTIPART_FORM_DATA.equalsIgnoreCase(contentType)) {
      contentType = MIME_TYPE_MULTIPART_FORM_DATA + MIME_TYPE_MULTIPART_BOUNDARY + boundary;
    }

    // Set response format type
    accept = !Util.isEmpty(headers.get(HTTP_HEADER_ACCEPT)) ? headers.get(HTTP_HEADER_ACCEPT) : MIME_TYPE_ALL;

    // Set RequestProperty
    connection.setRequestProperty(HTTP_HEADER_CONTENT_TYPE, contentType);
    connection.setRequestProperty(HTTP_HEADER_ACCEPT, accept);

    // Custom headers
    for (final String header : headers.keySet()) {
      connection.setRequestProperty(header, headers.get(header));
    }

  }

}
