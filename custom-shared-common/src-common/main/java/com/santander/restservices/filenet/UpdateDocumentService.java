package com.santander.restservices.filenet;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.calypso.tk.core.Log;
import com.santander.restservices.ApiRestAdapter;
import com.santander.restservices.ApiRestEndPoint;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestService;
import com.santander.restservices.FactoryApiRestAdapter;
import com.santander.restservices.UtilRestServices;
import com.santander.restservices.filenet.model.DocumentMetadata;
import com.santander.restservices.filenet.model.UpdateDocumentError;
import com.santander.restservices.filenet.model.UpdateDocumentErrorFunctional;
import com.santander.restservices.filenet.model.UpdateDocumentErrorInternal;
import com.santander.restservices.filenet.model.UpdateDocumentErrorTechnical;
import com.santander.restservices.filenet.model.UpdateDocumentInput;
import com.santander.restservices.filenet.model.UpdateDocumentOutput;

public class UpdateDocumentService implements ApiRestService
{
  public static final String FILENET_APPLICATION_HEADER = "x-ibm-client-id";
  public static final String FILENET_AUTHORIZATION_HEADER = "Authorization";
  public static final String FILENET_PROPERTIES_FILE = "restservices.properties";
  public static final String AUTHORIZATION_HEADER_BEARER = "Bearer ";
  public static final int AUTHENTICATION_ERROR = 401;

  private String accessToken = null;

  private String idDocument = null;
  private String file = null;
  private Map<String,Object> metadata = null;
  private String serviceName = null;
  
  UpdateDocumentInput input = null;
  UpdateDocumentOutput output = null;
  UpdateDocumentError error = null;

  public UpdateDocumentService(String serviceName)
  {
    super();
    this.serviceName= serviceName; 
    metadata = new HashMap<String,Object>();
    input = new UpdateDocumentInput();
    output = new UpdateDocumentOutput();
    error = new UpdateDocumentError();
  }

  public void setAccessToken(String accessToken)
  {
    this.accessToken = accessToken;
  }

  public void setIdDocument(String idDocument)
  {
    this.idDocument = idDocument;
  }

  public void setFile(String file)
  {
    this.file = file;
  }

  public void setFile(byte[] file)
  {
    this.file = (file != null) ? Base64.getEncoder().encodeToString(file) : null;
  }

  public void setMetadata(Map<String,Object> metadata)
  {
    this.metadata = metadata;
  }

  public void addMetadataItem(String key, Object value)
  {
    metadata.put(key, value);
  }

  @Override
  public UpdateDocumentInput getInput()
  {
    return input;
  }

  @Override
  public UpdateDocumentOutput getOutput()
  {
    return output;
  }

  @Override
  public UpdateDocumentError getError()
  {
    return error;
  }

  private void setInput(UpdateDocumentInput input)
  {
    this.input.loadModelData(input);
  }

  private void setOutput(UpdateDocumentOutput output)
  {
    this.output.loadModelData(output);
  }

  private void setError(UpdateDocumentError error)
  {
    this.error.loadModelData(error);
  }

  public boolean validateParameters()
  {
    boolean control = true;

    control = (control) && (idDocument != null);
    control = (control) && (file != null);
    control = (control) && (metadata != null);

    return control;
  }

  public int callService()
  {
    String application = null;
    String protocol = null;
    String port = null;
    String host = null;
    String file = null;
    String sslalgorithm = null;
    String ignorecertificate = null;

    Properties properties = null;

    ApiRestAdapter adapter = null;
    ApiRestEndPoint endpoint = null;
    Map<String,String> headers = null;
    UpdateDocumentInput in = null;
    UpdateDocumentOutput out = null;
    UpdateDocumentError error = null;
    UpdateDocumentErrorFunctional error1 = null;
    UpdateDocumentErrorTechnical error2 = null;
    UpdateDocumentErrorInternal error3 = null;
    List<ApiRestModel> errors = null;
    DocumentMetadata metadata = null;
    int status = -1;

    String debug = null;

    if (validateParameters())
    {
      properties = UtilRestServices.getPropertyFile(FILENET_PROPERTIES_FILE);

      if (properties != null)
      { 
	    application = properties.getProperty("OauthToken_".concat(serviceName.concat("_").concat("User")));
        protocol = properties.getProperty(serviceName.concat("_").concat("InsertDocument_Protocol"));
        port = properties.getProperty(serviceName.concat("_").concat("InsertDocument_Port"));
        host = properties.getProperty(serviceName.concat("_").concat("InsertDocument_Host"));
        file = properties.getProperty(serviceName.concat("_").concat("InsertDocument_File"));
        sslalgorithm = properties.getProperty(serviceName.concat("_").concat("InsertDocument_SslAlgorithm"));
        ignorecertificate =properties.getProperty(serviceName.concat("_").concat("InsertDocument_IgnoreCertificate"));
        
    	  
        if (((application != null) && (!application.equals(""))) &&
            ((protocol != null) && (!protocol.equals(""))) &&
            ((port != null) && (!port.equals(""))) &&
            ((host != null) && (!host.equals(""))) &&
            ((file != null) && (!file.equals("")))
            )
        {
          adapter = FactoryApiRestAdapter.getApiRestService();
          endpoint = new ApiRestEndPoint();

          endpoint.setProtocol(protocol);
          endpoint.setPort(Integer.parseInt(port));
          endpoint.setHost(host);
          endpoint.setFile(file);

          adapter.init(endpoint);
          adapter.setSslAlgorithm(sslalgorithm);
          adapter.setIgnoreCertificate(ignorecertificate);

          in = new UpdateDocumentInput();
          out = new UpdateDocumentOutput();
          error = new UpdateDocumentError();
          error1 = new UpdateDocumentErrorFunctional();
          error2 = new UpdateDocumentErrorTechnical();
          error3 = new UpdateDocumentErrorInternal();
          metadata = new DocumentMetadata();

          in.setIdDocument(idDocument);
          in.setFile(this.file);

          errors = new ArrayList<ApiRestModel>();
          errors.add(error1);
          errors.add(error2);
          errors.add(error3);

          metadata.load(this.metadata);
          in.setMetadata(metadata);

          headers = new HashMap<String,String>();
          headers.put(FILENET_APPLICATION_HEADER, application);
          headers.put(FILENET_AUTHORIZATION_HEADER, AUTHORIZATION_HEADER_BEARER + accessToken);

          setInput(in);

          status = adapter.put(in, out, errors, headers);

          if (ApiRestAdapter.isOKHttpStatus(status))
          {
            setOutput(out);
          }
          else
          {
            for (final ApiRestModel item : errors)
            {
              if (item.checkModelDataLoaded())
              {
                if (item instanceof UpdateDocumentErrorFunctional)
                {
                  final UpdateDocumentErrorFunctional obj = (UpdateDocumentErrorFunctional) item;

                  error.setErrorCode(obj.getErrorCode());
                  error.setMessage(obj.getMessage());
                }
                else
                  if (item instanceof UpdateDocumentErrorTechnical)
                  {
                    final UpdateDocumentErrorTechnical obj = (UpdateDocumentErrorTechnical) item;

                    error.setErrorCode(Integer.parseInt(obj.getHttpCode()));
                    error.setMessage(obj.getHttpMessage() + " - " + obj.getMoreInformation());
                  }
                  else
                    if (item instanceof UpdateDocumentErrorInternal)
                    {
                      final UpdateDocumentErrorInternal obj = (UpdateDocumentErrorInternal) item;

                      error.setErrorCode(obj.getStatus());
                      error.setMessage(obj.getError() + " - " + obj.getMessage());
                    }

                break;
              }
            }

            setError(error);

            debug = (new StringBuilder("Error calling FileNet UpdateDocument service: "))
                .append("idDocument [").append(idDocument).append("] ")
                .append("status [").append(status).append("] ")
                .append("error [").append(error.getMessage()).append("] ")
                .toString();

            if (status != AUTHENTICATION_ERROR) {
              Log.error(this, debug);
            }
          }
        }
        else
        {
          debug = (new StringBuilder("Invalid service properties: "))
              .append(FILENET_PROPERTIES_FILE).append(" ")
              .append("protocol [").append(protocol).append("] ")
              .append("port [").append(port).append("] ")
              .append("host [").append(host).append("] ")
              .append("file [").append(file).append("] ")
              .toString();

          Log.error(this, debug);
        }
      }
      else
      {
        debug = (new StringBuilder("Can not access to service properties: "))
            .append(FILENET_PROPERTIES_FILE).append(" ")
            .toString();

        Log.error(this, debug);
      }
    }
    else
    {
      debug = (new StringBuilder("Invalid parameters calling FileNet UpdateDocument service: "))
          .append("idDocument [").append(idDocument).append("] ")
          .toString();

      Log.error(this, debug);
    }

    return status;
  }
}
