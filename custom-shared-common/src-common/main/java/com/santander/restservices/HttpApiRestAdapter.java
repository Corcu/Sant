package com.santander.restservices;

import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.MimeType;
import com.calypso.tk.util.MimeTypes;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

//import javax.net.ssl.HttpsURLConnection;

/**
 * Utility class to send and recive to Rest Service
 */
public class HttpApiRestAdapter implements ApiRestAdapter
{
  protected ApiRestEndPoint endpoint = null;
  protected ApiRestCredential credential = null;
  protected ApiRestProxy proxy = null;
  private String sslalgorithm = null;
  private String ignorecertificate = null;
  private String boundary = null;

  public HttpApiRestAdapter()
  {
    super();
    boundary = UUID.randomUUID().toString();
  }

  /**
   * Initialize the utility class with a known set of properties and creating an
   * api rest session
   *
   * @return true if the object could be properly initialized
   */
  @Override
  public void init(ApiRestEndPoint endpoint)
  {
    this.endpoint = endpoint;
  }

  @Override
  public void init(ApiRestEndPoint endpoint, ApiRestCredential credential)
  {
    this.endpoint = endpoint;
    this.credential = credential;
  }

  @Override
  public void init(ApiRestEndPoint endpoint, ApiRestProxy proxy)
  {
    this.endpoint = endpoint;
    this.proxy = proxy;
  }

  @Override
  public void init(ApiRestEndPoint endpoint, ApiRestCredential credential, ApiRestProxy proxy)
  {
    this.endpoint = endpoint;
    this.credential = credential;
    this.proxy = proxy;
  }

  @Override
  public String getSslAlgorithm()
  {
    return sslalgorithm;
  }

  @Override
  public void setSslAlgorithm(String sslalgorithm)
  {
    this.sslalgorithm = sslalgorithm;
  }

  @Override
  public String getIgnoreCertificate()
  {
    return ignorecertificate;
  }

  @Override
  public void setIgnoreCertificate(String ignorecertificate)
  {
    this.ignorecertificate = ignorecertificate;
  }

  /**
   * create a HTTP conection
   *
   * @return true if the session could be created
   */
  protected HttpURLConnection createConnection(String uri, String method)
  {
    HttpURLConnection connection = null;

    String debug = null;

    try
    {
      if ((endpoint != null) && (endpoint.isValid()))
      {
        /*
         * Proxy
         */

        URL url = null;

        url = new URL(uri);

        if ((proxy != null) && (proxy.isValid()))
        {
          final Authenticator authenticator = new Authenticator()
          {
            @Override
            public PasswordAuthentication getPasswordAuthentication()
            {
              return (new PasswordAuthentication(proxy.getUser(), proxy.getPassword().toCharArray()));
            }
          };
          Authenticator.setDefault(authenticator);

          final Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(this.proxy.getHost(), this.proxy.getPort()));

          /*
                    if (this.endpoint.getProtocol().equals(ApiRestEndPoint.HTTPS_PROTOCOL))
                    {
                        connection = (HttpsURLConnection) url.openConnection(proxy);
                    }
                    else
                    {
                        connection = (HttpURLConnection) url.openConnection(proxy);
                    }
           */

          connection = (HttpURLConnection) url.openConnection(proxy);
        }
        else
        {
          /*
                  if (this.endpoint.getProtocol().equals(ApiRestEndPoint.HTTPS_PROTOCOL))
                    {
                        connection = (HttpsURLConnection) url.openConnection();
                    }
                    else
                    {
                        connection = (HttpURLConnection) url.openConnection();
                    }
           */

          connection = (HttpURLConnection) url.openConnection();
        }

        /*
         * Connection properties
         */

        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);

        connection.setRequestMethod(method);

        /*
         * Autentificacion
         */

        String authorization = null;

        if ((credential != null) && (credential.isValid()))
        {
          if (credential.getType().equals(ApiRestCredential.BASIC_CREDENTIAL_TYPE))
          {
            if ((credential.getUser() != null) && (credential.getPassword() != null))
            {
              authorization = credential.getUser() + ":" + credential.getPassword();
              authorization = "Basic " + DatatypeConverter.printBase64Binary(authorization.getBytes());
            }
          }
          else
            if (credential.getType().equals(ApiRestCredential.BEARER_CREDENTIAL_TYPE))
            {
              if (credential.getToken() != null)
              {
                authorization = credential.getUser() + ":" + credential.getPassword();
                authorization = "Bearer " + credential.getToken();
              }
            }

          if (authorization != null)
          {
            connection.setRequestProperty("Authorization", authorization);
          }
        }
      }
    }
    catch (final MalformedURLException e)
    {
      debug = (new StringBuilder())
          .append("Error creating https connection ")
          .append(",URI: ").append(uri).append(" ")
          .append(",METHOD: ").append(method).append(" ")
          //.append(",MIME: ").append(mime).append(" ")
          .append("with exception ").append(e.toString()).append(" - ").append(e.getMessage())
          .toString();

      Log.error(this, debug);
    }
    catch (final IOException e)
    {
      debug = (new StringBuilder())
          .append("Error creating https connection ")
          .append(",URI: ").append(uri).append(" ")
          .append(",METHOD: ").append(method).append(" ")
          //.append(",MIME: ").append(mime).append(" ")
          .append("with exception ").append(e.toString()).append(" - ").append(e.getMessage())
          .toString();

      Log.error(this, debug);
    }

    return connection;
  }

  /**
   * Call API REST whit method GET
   *
   * @param in   In data model
   * @param out  Out data model
   * @return int HTTP return error code
   */
  @Override
  public <K extends ApiRestModel> int get(String in, K out)
  {
    return doGet(in, out, null, null);
  }

  @Override
  public <K extends ApiRestModel, R extends ApiRestModel> int get(String in, K out, R error)
  {
    int status = -1;
    List<ApiRestModel> errors = null;

    errors = new ArrayList<>();
    errors.add(error);

    status = doGet(in, out, errors, null);

    return status;
  }

  @Override
  public <K extends ApiRestModel, R extends ApiRestModel> int get(String in, K out, List<R> errors)
  {
    return doGet(in, out, errors, null);
  }

  @Override
  public <K extends ApiRestModel> int get(String in, K out, Map<String,String> headers)
  {
    return doGet(in, out, null, headers);
  }

  @Override
  public <K extends ApiRestModel, R extends ApiRestModel> int get(String in, K out, R error, Map<String,String> headers)
  {
    int status = -1;
    List<ApiRestModel> errors = null;

    errors = new ArrayList<>();
    errors.add(error);

    status = doGet(in, out, errors, headers);

    return status;
  }

  @Override
  public <K extends ApiRestModel, R extends ApiRestModel> int get(String in, K out, List<R> errors, Map<String,String> headers)
  {
    return doGet(in, out, errors, headers);
  }

  private <K extends ApiRestModel, R extends ApiRestModel> int doGet(String in, K out, List<R> errors, Map<String,String> headers)
  {
    boolean control = false;
    HttpURLConnection connection = null;
    String uri = null;
    String mime = null;
    String accept = null;
    String content = null;
    String disposition = null;
    int status = -1;

    String debug = null;

    try
    {
      if ((endpoint != null) && (endpoint.isValid()))
      {
        if (in != null) {
          uri = endpoint.getEndPoint() + "/" + in;
        } else {
          uri = endpoint.getEndPoint();
        }
        connection = createConnection(uri, "GET");

        if (connection != null)
        {
          if (headers != null)
          {
            mime = ((headers.get(HTTP_HEADER_CONTENT_TYPE) != null) && (!headers.get(HTTP_HEADER_CONTENT_TYPE).trim().equals(""))) ?
                headers.get(HTTP_HEADER_CONTENT_TYPE) : MIME_TYPE_JSOM;

                accept = ((headers.get(HTTP_HEADER_ACCEPT) != null) && (!headers.get(HTTP_HEADER_ACCEPT).trim().equals(""))) ?
                    headers.get(HTTP_HEADER_ACCEPT) : MIME_TYPE_ALL;

                    connection.setRequestProperty(HTTP_HEADER_CONTENT_TYPE, mime);
                    connection.setRequestProperty(HTTP_HEADER_ACCEPT, accept);

                    for (final String header : headers.keySet())
                    {
                      connection.setRequestProperty(header, headers.get(header));
                    }
          }

          connection.connect();
          status = connection.getResponseCode();
          content = connection.getContentType();
          disposition = connection.getHeaderField(HTTP_HEADER_CONTENT_DISPOSITION);

          if (ApiRestAdapter.isOKHttpStatus(status))
          {
            debug = (new StringBuilder())
                .append("Response api rest method get ")
                .append(",URI: ").append(uri).append(" ")
                .append(",IN: ").append(in).append(" ")
                .toString();

            Log.info(this, debug);

            control = readResponse(content, disposition, connection.getInputStream(), out);

            if (!control)
            {
              debug = (new StringBuilder())
                  .append("Could not read api rest response whit method get ")
                  .toString();

              Log.error(this, debug);
            }
          }
          else
          {
            debug = (new StringBuilder())
                .append("Not OK status calling api rest method get ")
                .append(",URI: ").append(uri).append(" ")
                .append(",IN: ").append(in).append(" ")
                .append(",STATUS: ").append(status).append(" ")
                .toString();

            Log.warn(this, debug);

            if ((content != null) && (content.toUpperCase().startsWith(MIME_TYPE_JSOM.toUpperCase()))) {
              control = readJsonResponse(connection.getErrorStream(), errors);
            } else {
              control = readTextResponse(connection.getErrorStream(), errors);
            }

            if (!control)
            {
              debug = (new StringBuilder())
                  .append("Could not read api rest response whit method get ")
                  .toString();

              Log.error(this, debug);
            }
          }

          connection.disconnect();
        }
      }
    }
    catch (final IOException e)
    {
      debug = (new StringBuilder())
          .append("Error calling api rest method get ")
          .append(",URI: ").append(uri).append(" ")
          .append(",IN: ").append(in).append(" ")
          .append("with exception ").append(e.toString()).append(" - ").append(e.getMessage())
          .toString();

      Log.error(this, debug);
    }

    return status;
  }

  /**
   * Call API REST whit method POST
   *
   * @param in   In data model
   * @param out  Out data model
   * @return int HTTP return error code
   */
  @Override
  public <T extends ApiRestModel, K extends ApiRestModel> int post(T in, K out)
  {
    return doPost(in, out, null, null);
  }

  @Override
  public <T extends ApiRestModel, K extends ApiRestModel, R extends ApiRestModel> int post(T in, K out, R error)
  {
    int status = -1;
    List<ApiRestModel> errors = null;

    errors = new ArrayList<>();
    errors.add(error);

    status = doPost(in, out, errors, null);

    return status;
  }

  @Override
  public <T extends ApiRestModel, K extends ApiRestModel, R extends ApiRestModel> int post(T in, K out, List<R> errors)
  {
    return doPost(in, out, errors, null);
  }

  @Override
  public <T extends ApiRestModel, K extends ApiRestModel> int post(T in, K out, Map<String,String> headers)
  {
    return doPost(in, out, null, headers);
  }

  @Override
  public <T extends ApiRestModel, K extends ApiRestModel, R extends ApiRestModel> int post(T in, K out, R error, Map<String,String> headers)
  {
    int status = -1;
    List<ApiRestModel> errors = null;

    errors = new ArrayList<>();
    errors.add(error);

    status = doPost(in, out, errors, headers);

    return status;
  }

  @Override
  public <T extends ApiRestModel, K extends ApiRestModel, R extends ApiRestModel> int post(T in, K out, List<R> errors, Map<String,String> headers)
  {
    return doPost(in, out, errors, headers);
  }

  private <T extends ApiRestModel, K extends ApiRestModel, R extends ApiRestModel> int doPost(T in, K out, List<R> errors, Map<String,String> headers)
  {
    boolean control = false;
    HttpURLConnection connection = null;
    String uri = null;
    String mime = null;
    String accept = null;
    String content = null;
    String disposition = null;
    int status = -1;

    String debug = null;

    try
    {
      if ((endpoint != null) && (endpoint.isValid()))
      {
        uri = endpoint.getEndPoint();

        connection = createConnection(uri, "POST");

        if (connection != null)
        {
          debug = (new StringBuilder())
              .append("Request api rest method post ")
              .append(",URI: ").append(uri).append(" ")
              .toString();

          Log.info(this, debug);

          if (headers != null)
          {
            for (final String header : headers.keySet())
            {
              connection.setRequestProperty(header, headers.get(header));
            }

            mime = ((headers.get(HTTP_HEADER_CONTENT_TYPE) != null) && (!headers.get(HTTP_HEADER_CONTENT_TYPE).trim().equals(""))) ?
                headers.get(HTTP_HEADER_CONTENT_TYPE) : MIME_TYPE_JSOM;

                accept = ((headers.get(HTTP_HEADER_ACCEPT) != null) && (!headers.get(HTTP_HEADER_ACCEPT).trim().equals(""))) ?
                    headers.get(HTTP_HEADER_ACCEPT) : MIME_TYPE_ALL;

                    if (mime.equalsIgnoreCase(MIME_TYPE_MULTIPART_FORM_DATA)) {
                      mime = MIME_TYPE_MULTIPART_FORM_DATA + MIME_TYPE_MULTIPART_BOUNDARY + boundary;
                    }

                    connection.setRequestProperty(HTTP_HEADER_CONTENT_TYPE, mime);
                    connection.setRequestProperty(HTTP_HEADER_ACCEPT, accept);
          }

          control = writeRequest(mime, connection.getOutputStream(), in);

          if (!control)
          {
            debug = (new StringBuilder())
                .append("Could not write api rest request whit method post ")
                .toString();

            Log.error(this, debug);
          }

          connection.connect();
          status = connection.getResponseCode();
          content = connection.getContentType();
          disposition = connection.getHeaderField(HTTP_HEADER_CONTENT_DISPOSITION);

          if (ApiRestAdapter.isOKHttpStatus(status))
          {
            debug = (new StringBuilder())
                .append("Response api rest method post ")
                .append(",URI: ").append(uri).append(" ")
                .toString();

            Log.info(this, debug);

            control = readResponse(content, disposition, connection.getInputStream(), out);

            if (!control)
            {
              debug = (new StringBuilder())
                  .append("Could not read api rest response whit method post ")
                  .toString();

              Log.error(this, debug);
            }
          }
          else
          {
            debug = (new StringBuilder())
                .append("Not OK request status calling api rest method post ")
                .append(",URI: ").append(uri).append(" ")
                .append(",STATUS: ").append(status).append(" ")
                .toString();

            Log.warn(this, debug);

            if ((content != null) && (content.toUpperCase().startsWith(MIME_TYPE_JSOM.toUpperCase()))) {
              control = readJsonResponse(connection.getErrorStream(), errors);
            } else {
              control = readTextResponse(connection.getErrorStream(), errors);
            }

            if (!control)
            {
              debug = (new StringBuilder())
                  .append("Could not read api rest response whit method post ")
                  .toString();

              Log.error(this, debug);
            }
          }

          connection.disconnect();
        }
      }
    }
    catch (final IOException e)
    {
      debug = (new StringBuilder())
          .append("Error calling api rest method post ")
          .append(",URI: ").append(uri).append(" ")
          .append("with exception ").append(e.toString()).append(" - ").append(e.getMessage())
          .toString();

      Log.error(this, debug);
    }

    return status;
  }

  /**
   * Call API REST whit method PUT
   *
   * @param in   In data model
   * @param out  Out data model
   * @return int HTTP return error code
   */
  @Override
  public <T extends ApiRestModel, K extends ApiRestModel> int put(T in, K out)
  {
    return doPut(in, out, null, null);
  }

  @Override
  public <T extends ApiRestModel, K extends ApiRestModel, R extends ApiRestModel> int put(T in, K out, R error)
  {
    int status = -1;
    List<ApiRestModel> errors = null;

    errors = new ArrayList<>();
    errors.add(error);

    status = doPut(in, out, errors, null);

    return status;
  }

  @Override
  public <T extends ApiRestModel, K extends ApiRestModel, R extends ApiRestModel> int put(T in, K out, List<R> errors)
  {
    return doPut(in, out, errors, null);
  }

  @Override
  public <T extends ApiRestModel, K extends ApiRestModel> int put(T in, K out, Map<String,String> headers)
  {
    return doPut(in, out, null, headers);
  }

  @Override
  public <T extends ApiRestModel, K extends ApiRestModel, R extends ApiRestModel> int put(T in, K out, R error, Map<String,String> headers)
  {
    int status = -1;
    List<ApiRestModel> errors = null;

    errors = new ArrayList<>();
    errors.add(error);

    status = doPut(in, out, errors, headers);

    return status;
  }

  @Override
  public <T extends ApiRestModel, K extends ApiRestModel, R extends ApiRestModel> int put(T in, K out, List<R> errors, Map<String,String> headers)
  {
    return doPut(in, out, errors, headers);
  }

  private <T extends ApiRestModel, K extends ApiRestModel, R extends ApiRestModel> int doPut(T in, K out, List<R> errors, Map<String,String> headers)
  {
    boolean control =  false;
    HttpURLConnection connection = null;
    String uri = null;
    String mime = null;
    String accept = null;
    String content = null;
    String disposition = null;
    int status = -1;

    String debug = null;

    try
    {
      if ((endpoint != null) && (endpoint.isValid()))
      {
        uri = endpoint.getEndPoint();

        connection = createConnection(uri, "PUT");

        if (connection != null)
        {
          debug = (new StringBuilder())
              .append("Request api rest method put ")
              .append(",URI: ").append(uri).append(" ")
              .toString();

          Log.info(this, debug);

          if (headers != null)
          {
            for (final String header : headers.keySet())
            {
              connection.setRequestProperty(header, headers.get(header));
            }

            mime = ((headers.get(HTTP_HEADER_CONTENT_TYPE) != null) && (!headers.get(HTTP_HEADER_CONTENT_TYPE).trim().equals(""))) ?
                headers.get(HTTP_HEADER_CONTENT_TYPE) : MIME_TYPE_JSOM;

                accept = ((headers.get(HTTP_HEADER_ACCEPT) != null) && (!headers.get(HTTP_HEADER_ACCEPT).trim().equals(""))) ?
                    headers.get(HTTP_HEADER_ACCEPT) : MIME_TYPE_ALL;

                    if (mime.equalsIgnoreCase(MIME_TYPE_MULTIPART_FORM_DATA)) {
                      mime = MIME_TYPE_MULTIPART_FORM_DATA + MIME_TYPE_MULTIPART_BOUNDARY + boundary;
                    }

                    connection.setRequestProperty(HTTP_HEADER_CONTENT_TYPE, mime);
                    connection.setRequestProperty(HTTP_HEADER_ACCEPT, accept);
          }

          control = writeRequest(mime, connection.getOutputStream(), in);

          if (!control)
          {
            debug = (new StringBuilder())
                .append("Could not write api rest request whit method put ")
                .toString();

            Log.error(this, debug);
          }

          connection.connect();
          status = connection.getResponseCode();
          content = connection.getContentType();
          disposition = connection.getHeaderField(HTTP_HEADER_CONTENT_DISPOSITION);

          if (ApiRestAdapter.isOKHttpStatus(status))
          {
            debug = (new StringBuilder())
                .append("Response api rest method put ")
                .append(",URI: ").append(uri).append(" ")
                .toString();

            Log.info(this, debug);

            control = readResponse(content, disposition, connection.getInputStream(), out);

            if (!control)
            {
              debug = (new StringBuilder())
                  .append("Could not read api rest response whit method put ")
                  .toString();

              Log.error(this, debug);
            }
          }
          else
          {
            debug = (new StringBuilder())
                .append("Not OK request status calling api rest method put ")
                .append(",URI: ").append(uri).append(" ")
                .append(",STATUS: ").append(status).append(" ")
                .toString();

            Log.warn(this, debug);

            if ((content != null) && (content.toUpperCase().startsWith(MIME_TYPE_JSOM.toUpperCase()))) {
              control = readJsonResponse(connection.getErrorStream(), errors);
            } else {
              control = readTextResponse(connection.getErrorStream(), errors);
            }

            if (!control)
            {
              debug = (new StringBuilder())
                  .append("Could not read api rest response whit method put ")
                  .toString();

              Log.error(this, debug);
            }
          }

          connection.disconnect();
        }
      }
    }
    catch (final IOException e)
    {
      debug = (new StringBuilder())
          .append("Error calling api rest method put ")
          .append(",URI: ").append(uri).append(" ")
          .append("with exception ").append(e.toString()).append(" - ").append(e.getMessage())
          .toString();

      Log.error(this, debug);
    }

    return status;
  }

  /**
   * Call API REST whit method DELETE
   *
   * @param in   In data model
   * @param out  Out data model
   * @return int HTTP return error code
   */
  @Override
  public <K extends ApiRestModel> int delete(String in, K out)
  {
    return doDelete(in, out, null, null);
  }

  @Override
  public <K extends ApiRestModel, R extends ApiRestModel> int delete(String in, K out, R error)
  {
    int status = -1;
    List<ApiRestModel> errors = null;

    errors = new ArrayList<>();
    errors.add(error);

    status = doDelete(in, out, errors, null);

    return status;
  }

  @Override
  public <K extends ApiRestModel, R extends ApiRestModel> int delete(String in, K out, List<R> errors)
  {
    return doDelete(in, out, errors, null);
  }

  @Override
  public <K extends ApiRestModel> int delete(String in, K out, Map<String,String> headers)
  {
    return doDelete(in, out, null, headers);
  }

  @Override
  public <K extends ApiRestModel, R extends ApiRestModel> int delete(String in, K out, R error, Map<String,String> headers)
  {
    int status = -1;
    List<ApiRestModel> errors = null;

    errors = new ArrayList<>();
    errors.add(error);

    status = doDelete(in, out, errors, headers);

    return status;
  }

  @Override
  public <K extends ApiRestModel, R extends ApiRestModel> int delete(String in, K out, List<R> errors, Map<String,String> headers)
  {
    return doDelete(in, out, errors, headers);
  }

  private <K extends ApiRestModel, R extends ApiRestModel> int doDelete(String in, K out, List<R> errors, Map<String,String> headers)
  {
    boolean control = false;
    HttpURLConnection connection = null;
    String uri = null;
    String mime = null;
    String accept = null;
    String content = null;
    String disposition = null;
    int status = -1;

    String debug = null;

    try
    {
      if ((endpoint != null) && (endpoint.isValid()))
      {
        if (in != null) {
          uri = endpoint.getEndPoint() + "/" + in;
        } else {
          uri = endpoint.getEndPoint();
        }

        connection = createConnection(uri, "DELETE");

        if (connection != null)
        {
          if (headers != null)
          {
            mime = ((headers.get(HTTP_HEADER_CONTENT_TYPE) != null) && (!headers.get(HTTP_HEADER_CONTENT_TYPE).trim().equals(""))) ?
                headers.get(HTTP_HEADER_CONTENT_TYPE) : MIME_TYPE_JSOM;

                accept = ((headers.get(HTTP_HEADER_ACCEPT) != null) && (!headers.get(HTTP_HEADER_ACCEPT).trim().equals(""))) ?
                    headers.get(HTTP_HEADER_ACCEPT) : MIME_TYPE_ALL;

                    connection.setRequestProperty(HTTP_HEADER_CONTENT_TYPE, mime);
                    connection.setRequestProperty(HTTP_HEADER_ACCEPT, accept);

                    for (final String header : headers.keySet())
                    {
                      connection.setRequestProperty(header, headers.get(header));
                    }
          }

          connection.connect();
          status = connection.getResponseCode();
          content = connection.getContentType();
          disposition = connection.getHeaderField(HTTP_HEADER_CONTENT_DISPOSITION);

          if (ApiRestAdapter.isOKHttpStatus(status))
          {
            debug = (new StringBuilder())
                .append("Response api rest method delete ")
                .append(",URI: ").append(uri).append(" ")
                .append(",IN: ").append(in).append(" ")
                .toString();

            Log.info(this, debug);

            control = readResponse(content, disposition, connection.getInputStream(), out);

            if (!control)
            {
              debug = (new StringBuilder())
                  .append("Could not read api rest response whit method delete ")
                  .toString();

              Log.error(this, debug);
            }
          }
          else
          {
            debug = (new StringBuilder())
                .append("Not OK status calling api rest method delete ")
                .append(",URI: ").append(uri).append(" ")
                .append(",IN: ").append(in).append(" ")
                .append(",STATUS: ").append(status).append(" ")
                .toString();

            Log.warn(this, debug);

            if ((content != null) && (content.toUpperCase().startsWith(MIME_TYPE_JSOM.toUpperCase()))) {
              control = readJsonResponse(connection.getErrorStream(), errors);
            } else {
              control = readTextResponse(connection.getErrorStream(), errors);
            }

            if (!control)
            {
              debug = (new StringBuilder())
                  .append("Could not read api rest response whit method delete ")
                  .toString();

              Log.error(this, debug);
            }
          }

          connection.disconnect();
        }
      }
    }
    catch (final IOException e)
    {
      debug = (new StringBuilder())
          .append("Error calling api rest method delete ")
          .append(",URI: ").append(uri).append(" ")
          .append(",IN: ").append(in).append(" ")
          .append("with exception ").append(e.toString()).append(" - ").append(e.getMessage())
          .toString();

      Log.error(this, debug);
    }

    return status;
  }

  public boolean isValid()
  {
    boolean control = false;

    control = ((endpoint != null) && (credential != null));

    return control;
  }

  private <T extends ApiRestModel> boolean writeJsonRequest(OutputStream out, T model)
  {
    boolean control = false;

    ObjectMapper mapper = null;

    String debug = null;

    try
    {
      mapper = new ObjectMapper();
      mapper.setSerializationInclusion(Include.NON_NULL);

      debug = (new StringBuilder())
          .append("Request sent to api rest: ").append(mapper.writeValueAsString(model)).append(" ")
          .toString();

      Log.debug(this, debug);

      if (model != null) {
        mapper.writeValue(out, model);
      }

      control = true;
    }
    catch (final JsonParseException e)
    {
      debug = (new StringBuilder())
          .append("Error sending rest output ")
          .append("with exception ").append(e.toString()).append(" - ").append(e.getMessage()).append(" ")
          .toString();

      Log.error(this, debug);
    }
    catch (final JsonGenerationException e)
    {
      debug = (new StringBuilder())
          .append("Error sending rest output ")
          .append("with exception ").append(e.toString()).append(" - ").append(e.getMessage()).append(" ")
          .toString();

      Log.error(this, debug);
    }
    catch (final JsonMappingException e)
    {
      debug = (new StringBuilder())
          .append("Error sending rest output ")
          .append("with exception ").append(e.toString()).append(" - ").append(e.getMessage()).append(" ")
          .toString();

      Log.error(this, debug);
    }
    catch (final IOException e)
    {
      debug = (new StringBuilder())
          .append("Error sending rest output ")
          .append("with exception ").append(e.toString()).append(" - ").append(e.getMessage()).append(" ")
          .toString();

      Log.error(this, debug);
    }
    catch (final Exception e)
    {
      debug = (new StringBuilder())
          .append("Error reading rest output ")
          .append("with exception ").append(e.toString()).append(" - ").append(e.getMessage()).append(" ")
          .toString();

      Log.error(this, debug);
    }

    return control;
  }

  protected <T extends ApiRestModel> boolean readJsonResponse(InputStream in, List<T> models)
  {
    boolean control = false;

    ByteArrayOutputStream bytes = null;

    if (models != null)
    {
      bytes = new ByteArrayOutputStream();
      ResourceLoader.readInputStreamByBuffer(in, bytes);

      for (final T model : models)
      {
        control = readJsonResponse(bytes, model);

        if (control) {
          break;
        }
      }
    }

    return control;
  }

  private <T extends ApiRestModel> boolean readJsonResponse(InputStream in, T model)
  {
    boolean control = false;

    ByteArrayOutputStream bytes = null;

    bytes = new ByteArrayOutputStream();
    ResourceLoader.readInputStreamByBuffer(in, bytes);

    control = readJsonResponse(bytes, model);

    return control;
  }

  private <T extends ApiRestModel> boolean readJsonResponse(ByteArrayOutputStream bytes, T model)
  {
    boolean control = false;

    ApiRestModel data = null;
    ObjectMapper mapper = null;

    String debug = null;

    try
    {
      mapper = new ObjectMapper();

      debug = (new StringBuilder())
          .append("Response recived from api rest: ").append(bytes.toString()).append(" ")
          .toString();

      Log.debug(this, debug);

      if (model != null)
      {

        data = mapper.readValue(bytes.toByteArray(), model.retriveModelClass());
        model.loadModelData(data);

        control = true;
      }
    }
    catch (final JsonParseException e)
    {
      debug = (new StringBuilder())
          .append("Error reading rest output ")
          .append("with exception ").append(e.toString()).append(" - ").append(e.getMessage()).append(" ")
          .toString();

      Log.warn(this, debug);
    }
    catch (final JsonMappingException e)
    {
      debug = (new StringBuilder())
          .append("Error reading rest output ")
          .append("with exception ").append(e.toString()).append(" - ").append(e.getMessage()).append(" ")
          .toString();

      Log.warn(this, debug);
    }
    catch (final IOException e)
    {
      debug = (new StringBuilder())
          .append("Error reading rest output ")
          .append("with exception ").append(e.toString()).append(" - ").append(e.getMessage()).append(" ")
          .toString();

      Log.error(this, debug);
    }
    catch (final Exception e)
    {
      debug = (new StringBuilder())
          .append("Error reading rest output ")
          .append("with exception ").append(e.toString()).append(" - ").append(e.getMessage()).append(" ")
          .toString();

      Log.error(this, debug);
    }

    return control;
  }

  @SuppressWarnings("unchecked")
  private <T extends ApiRestModel> boolean writeMultipartFormDataRequest(OutputStream out, T model)
  {
    boolean control = false;

    Map<String,String> params = null;
    PrintWriter writer;

    String debug = null;

    try
    {
      if ((model != null) && (model instanceof Map))
      {
        params = (Map<String,String>) model;

        writer = new PrintWriter(new OutputStreamWriter(out, CHARSET_UTF_8), true);

        for (final Map.Entry<String, String> entry : params.entrySet()) {
          writer.append("--" + boundary).append(NEW_LINE);
          writer.append("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"").append(NEW_LINE);
          writer.append("Content-Type: text/plain; charset=" + CHARSET_UTF_8).append(NEW_LINE);
          writer.append(NEW_LINE);
          writer.append(entry.getValue()).append(NEW_LINE);
          writer.flush();
        }

        writer.append("--" + boundary + "--").append(NEW_LINE);

        writer.flush();
        writer.close();

        control = true;
      }
      else
      {
        debug = (new StringBuilder()).append("Error writing multipart form data ")
            .append(",Model is null ")
            .append(" ").toString();

        Log.error(this, debug);
      }
    }
    catch (final UnsupportedEncodingException e)
    {
      debug = (new StringBuilder())
          .append("Error writing multipart form data ")
          .append("with exception ").append(e.toString()).append(" - ").append(e.getMessage()).append(" ")
          .toString();

      Log.error(this, debug);
    }

    return control;
  }

  @SuppressWarnings("unchecked")
  private <T extends ApiRestModel> boolean writeUrlFormEncodedRequest(OutputStream out, T model)
  {
    boolean control = false;

    StringBuilder result = null;
    Writer wri = null;
    Map<String,String> params = null;
    boolean first = true;

    String debug = null;

    try
    {
      if ((model != null) && (model instanceof Map))
      {
        params = (Map<String,String>) model;

        result = new StringBuilder();

        for (final Map.Entry<String, String> entry : params.entrySet())
        {
          if (first) {
            first = false;
          } else {
            result.append("&");
          }

          result.append(URLEncoder.encode(entry.getKey(), CHARSET_UTF_8));
          result.append("=");
          result.append(URLEncoder.encode(entry.getValue(), CHARSET_UTF_8));
        }

        wri = new OutputStreamWriter(out, CHARSET_UTF_8);
        wri.write(result.toString());
        wri.flush();

        control = true;
      }
      else
      {
        debug = (new StringBuilder())
            .append("Error writing url form encoded ")
            .append(",Model is null ")
            .toString();

        Log.error(this, debug);
      }
    }
    catch (final UnsupportedEncodingException e)
    {
      debug = (new StringBuilder())
          .append("Error writing url form encoded ")
          .append("with exception ").append(e.toString()).append(" - ").append(e.getMessage()).append(" ")
          .toString();

      Log.error(this, debug);
    }
    catch (final IOException e)
    {
      debug = (new StringBuilder())
          .append("Error writing url form encoded ")
          .append("with exception ").append(e.toString()).append(" - ").append(e.getMessage()).append(" ")
          .toString();

      Log.error(this, debug);
    }

    return control;
  }

  private <T extends ApiRestModel> boolean writeTextRequest(OutputStream out, T model)
  {
    boolean control = false;

    PrintWriter writer;

    String debug = null;

    try
    {
      if (model != null)
      {
        writer = new PrintWriter(new OutputStreamWriter(out, CHARSET_UTF_8), true);

        writer.append(model.pullTextMessage()).append(NEW_LINE);

        writer.flush();
        writer.close();

        control = true;
      }
      else
      {
        debug = (new StringBuilder())
            .append("Error writing text request ")
            .append(",Model is null ")
            .append(" ").toString();

        Log.error(this, debug);
      }
    }
    catch (final UnsupportedEncodingException e)
    {
      debug = (new StringBuilder())
          .append("Error writing text request ")
          .append("with exception ").append(e.toString()).append(" - ").append(e.getMessage()).append(" ")
          .toString();

      Log.error(this, debug);
    }

    return control;
  }

  protected <T extends ApiRestModel> boolean readTextResponse(InputStream in, List<T> models)
  {
    boolean control = false;

    ByteArrayOutputStream bytes = null;

    if (models != null && in != null)
    {
      bytes = new ByteArrayOutputStream();
      ResourceLoader.readInputStreamByBuffer(in, bytes);

      for (final T model : models)
      {
        control = readTextResponse(bytes, model);

        if (control) {
          break;
        }
      }
    }

    return control;
  }

  private <T extends ApiRestModel> boolean readTextResponse(InputStream in, T model)
  {
    boolean control = false;

    ByteArrayOutputStream bytes = null;

    bytes = new ByteArrayOutputStream();
    ResourceLoader.readInputStreamByBuffer(in, bytes);

    control = readTextResponse(bytes, model);

    return control;
  }

  private <T extends ApiRestModel> boolean readTextResponse(ByteArrayOutputStream bytes, T model)
  {
    boolean control = false;

    String debug = null;

    try
    {
      if (model != null)
      {
        debug = (new StringBuilder())
            .append("Response recived: ").append(bytes.toString()).append(" ")
            .toString();

        Log.debug(this, debug);

        model.pushTextMessage(bytes.toString());

        control = true;
      }
      else
      {
        debug = (new StringBuilder())
            .append("Error reading text response ")
            .append(",Model is null ")
            .append(" ").toString();

        Log.error(this, debug);
      }
    }
    catch (final Exception e)
    {
      debug = (new StringBuilder())
          .append("Error reading text response ")
          .append("with exception ").append(e.toString()).append(" - ").append(e.getMessage()).append(" ")
          .toString();

      Log.error(this, debug);
    }

    return control;
  }

  private <T extends ApiRestModel> boolean readBinaryResponse(InputStream in, T model)
  {
    boolean control = false;

    ByteArrayOutputStream bytes = null;

    String debug = null;

    try
    {
      if (model != null)
      {
        bytes = new ByteArrayOutputStream();
        ResourceLoader.readInputStreamByBuffer(in, bytes);

        debug = (new StringBuilder())
            .append("Response recived: ").append(bytes.toString()).append(" ")
            .toString();

        Log.debug(this, debug);

        model.pushBinaryMessage(bytes.toByteArray());

        control = true;
      }
      else
      {
        debug = (new StringBuilder())
            .append("Error reading text response ")
            .append(",Model is null ")
            .append(" ").toString();

        Log.error(this, debug);
      }
    }
    catch (final Exception e)
    {
      debug = (new StringBuilder())
          .append("Error reading text response ")
          .append("with exception ").append(e.toString()).append(" - ").append(e.getMessage()).append(" ")
          .toString();

      Log.error(this, debug);
    }

    return control;
  }

  protected <T extends ApiRestModel> boolean writeRequest(String mime, OutputStream out, T model)
  {
    boolean control = false;

    if (mime != null)
    {
      if (mime.toUpperCase().startsWith(MIME_TYPE_MULTIPART_FORM_DATA.toUpperCase())) {
        control = writeMultipartFormDataRequest(out, model);
      } else
        if (mime.toUpperCase().startsWith(MIME_TYPE_URL_FORM_ENCOCODED.toUpperCase())) {
          control = writeUrlFormEncodedRequest(out, model);
        } else
          if (mime.toUpperCase().startsWith(MIME_TYPE_JSOM.toUpperCase())) {
            control = writeJsonRequest(out, model);
          } else {
            control = writeTextRequest(out, model);
          }
    }
    else
    {
      control = writeTextRequest(out, model);
    }

    return control;
  }

  protected <T extends ApiRestModel> boolean readResponse(String content, String disposition, InputStream in, T model)
  {
    boolean control = false;

    if ((disposition != null) && (disposition.toUpperCase().startsWith(HTTP_HEADER_CONTENT_DISPOSITION_ATTACHMENT.toUpperCase())))
    {
      String file = null;
      MimeType mimetype = null;

      file = RegExUtil.getMatchingGroup(disposition, HTTP_HEADER_CONTENT_DISPOSITION_REGEXP, Integer.parseInt(HTTP_HEADER_CONTENT_DISPOSITION_GROUP));

      if ((file != null) && (!file.trim().equals("")))
      {
        mimetype = MimeTypes.getFileType(file);

        if (mimetype != null)
        {
          model.setMimeType(mimetype.getType());

          if (mimetype.isBinaryFormat()) {
            control = readBinaryResponse(in, model);
          } else {
            control = readTextResponse(in, model);
          }
        }
        else
        {
          model.setMimeType(MIME_TYPE_DEFAULT);

          control = readBinaryResponse(in, model);
        }
      }
      else
      {
        model.setMimeType(MIME_TYPE_DEFAULT);

        control = readBinaryResponse(in, model);
      }
    }
    else
    {
      if (content != null)
      {
        model.setMimeType(content);

        if (content.toUpperCase().startsWith(MIME_TYPE_JSOM.toUpperCase())) {
          control = readJsonResponse(in, model);
        } else
          if (content.toUpperCase().startsWith(MIME_TYPE_TEXT_ROOT.toUpperCase())) {
            control = readTextResponse(in, model);
          } else {
            control = readBinaryResponse(in, model);
          }
      }
      else
      {
        model.setMimeType(MIME_TYPE_DEFAULT);

        control = readBinaryResponse(in, model);
      }
    }

    return control;
  }
}
