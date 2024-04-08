package com.santander.restservices;

import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public interface ApiRestAdapter
{
  public static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
  public static final String HTTP_HEADER_CONTENT_DISPOSITION = "Content-Disposition";
  public static final String HTTP_HEADER_CONTENT_DISPOSITION_ATTACHMENT = "attachment;";
  public static final String HTTP_HEADER_CONTENT_DISPOSITION_REGEXP = "filename[^;=\\n]*=((['\"]).*?\\2|[^;\\n]*)";
  public static final String HTTP_HEADER_CONTENT_DISPOSITION_GROUP = "1";
  public static final String HTTP_HEADER_ACCEPT = "Accept";
  public static final String HTTP_HEADER_AUTHORIZATION = "Authorization";
  public static final String HTTP_HEADER_SCA_TOKEN = "SCA-Token";
  public static final String HTTP_HEADER_X_SANTANDER_CLIENTID = "X-Santander-Client-Id";
  public static final String HTTP_HEADER_X_PAYMENT_SYSTEM = "X-Payment-System";
  public static final String HTTP_HEADER_AUTHORIZATION_SCOPE = "scope";
  public static final String HTTP_HEADER_AUTHORIZATION_GRANT_TYPE = "grant_type";
  public static final String HTTP_HEADER_AUTHORIZATION_ASSERTION = "assertion";

  public static final String MIME_TYPE_TEXT_ROOT = "text/";
  public static final String MIME_TYPE_TEXT_PLAIN = "text/plain";
  public static final String MIME_TYPE_MULTIPART_FORM_DATA = "multipart/form-data";
  public static final String MIME_TYPE_MULTIPART_BOUNDARY = "; boundary=";
  public static final String MIME_TYPE_URL_FORM_ENCOCODED = "application/x-www-form-urlencoded";
  public static final String MIME_TYPE_JSOM = "application/json";
  public static final String MIME_TYPE_XML = "application/xml";
  public static final String MIME_TYPE_DEFAULT = "application/octet-stream";
  public static final String MIME_TYPE_ALL = "*/*";

  public static final String CHARSET_UTF_8 = "UTF-8";
  public static final String NEW_LINE = "\r\n";

  public static final String DEFAULT_SSL_ALGORITHM = "TLSv1.2";
  public static final String NO_IGNORE_CERTIFICATE = "N";
  public static final String SI_IGNORE_CERTIFICATE = "S";

  public void init(ApiRestEndPoint endpoint);
  public void init(ApiRestEndPoint endpoint, ApiRestCredential credential);
  public void init(ApiRestEndPoint endpoint, ApiRestProxy proxy);
  public void init(ApiRestEndPoint endpoint, ApiRestCredential credential, ApiRestProxy proxy);

  public String getSslAlgorithm();
  public void setSslAlgorithm(String sslalgorithm);
  public String getIgnoreCertificate();
  public void setIgnoreCertificate(String ignorecertificate);

  public <K extends ApiRestModel> int get(String in, K out);
  public <K extends ApiRestModel, R extends ApiRestModel> int get(String in, K out, R error);
  public <K extends ApiRestModel, R extends ApiRestModel> int get(String in, K out, List<R> errors);
  public <K extends ApiRestModel> int get(String in, K out, Map<String,String> headers);
  public <K extends ApiRestModel, R extends ApiRestModel> int get(String in, K out, R error, Map<String,String> headers);
  public <K extends ApiRestModel, R extends ApiRestModel> int get(String in, K out, List<R> errors, Map<String,String> headers);

  public <T extends ApiRestModel, K extends ApiRestModel> int post(T in, K out);
  public <T extends ApiRestModel, K extends ApiRestModel, R extends ApiRestModel> int post(T in, K out, R error);
  public <T extends ApiRestModel, K extends ApiRestModel, R extends ApiRestModel> int post(T in, K out, List<R> errors);
  public <T extends ApiRestModel, K extends ApiRestModel> int post(T in, K out, Map<String,String> headers);
  public <T extends ApiRestModel, K extends ApiRestModel, R extends ApiRestModel> int post(T in, K out, R error, Map<String,String> headers);
  public <T extends ApiRestModel, K extends ApiRestModel, R extends ApiRestModel> int post(T in, K out, List<R> errors, Map<String,String> headers);

  public <T extends ApiRestModel, K extends ApiRestModel> int put(T in, K out);
  public <T extends ApiRestModel, K extends ApiRestModel, R extends ApiRestModel> int put(T in, K out, R error);
  public <T extends ApiRestModel, K extends ApiRestModel, R extends ApiRestModel> int put(T in, K out, List<R> errors);
  public <T extends ApiRestModel, K extends ApiRestModel> int put(T in, K out, Map<String,String> headers);
  public <T extends ApiRestModel, K extends ApiRestModel, R extends ApiRestModel> int put(T in, K out, R error, Map<String,String> headers);
  public <T extends ApiRestModel, K extends ApiRestModel, R extends ApiRestModel> int put(T in, K out, List<R> errors, Map<String,String> headers);

  public <K extends ApiRestModel> int delete(String in, K out);
  public <K extends ApiRestModel, R extends ApiRestModel> int delete(String in, K out, R error);
  public <K extends ApiRestModel, R extends ApiRestModel> int delete(String in, K out, List<R> errors);
  public <K extends ApiRestModel> int delete(String in, K out, Map<String,String> headers);
  public <K extends ApiRestModel, R extends ApiRestModel> int delete(String in, K out, R error, Map<String,String> headers);
  public <K extends ApiRestModel, R extends ApiRestModel> int delete(String in, K out, List<R> errors, Map<String,String> headers);

  public static boolean isOKHttpStatus(int status)
  {
    boolean control = false;

    if ((status >= HttpsURLConnection.HTTP_OK) && (status < HttpsURLConnection.HTTP_OK + 100))
    {
      control = true;
    }

    return control;
  }

  public static boolean isUnauthorizedHttpStatus(int status)
  {
    boolean control = false;

    if (status == HttpsURLConnection.HTTP_UNAUTHORIZED)
    {
      control = true;
    }

    return control;
  }

  public static boolean isServerErrorHttpStatus(final int status) {
    return status >= HttpsURLConnection.HTTP_INTERNAL_ERROR && status < HttpsURLConnection.HTTP_INTERNAL_ERROR + 100;
  }
}
