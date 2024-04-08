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
import com.santander.restservices.filenet.model.InsertDocumentError;
import com.santander.restservices.filenet.model.InsertDocumentErrorFunctional;
import com.santander.restservices.filenet.model.InsertDocumentErrorInternal;
import com.santander.restservices.filenet.model.InsertDocumentErrorTechnical;
import com.santander.restservices.filenet.model.InsertDocumentInput;
import com.santander.restservices.filenet.model.InsertDocumentOutput;

public class InsertDocumentService implements ApiRestService
{
  public static final String FILENET_APPLICATION_HEADER = "x-ibm-client-id";
  public static final String FILENET_AUTHORIZATION_HEADER = "Authorization";
  public static final String AUTHORIZATION_HEADER_BEARER = "Bearer ";
  public static final String FILENET_PROPETIES_FILE = "restservices.properties";

  public static final int AUTHENTICATION_ERROR = 401;

  private String accessToken = null;

  private String fileName = null;
  private String docClass = null;
  private String file = null;
  private String typeMIME = null;
  private String storagePath = null;
  private Map<String,Object> metadata = null;
  private String serviceName = null;

  InsertDocumentInput input = null;
  InsertDocumentOutput output = null;
  InsertDocumentError error = null;

  public InsertDocumentService(String serviceName)
  {
    super();
    metadata = new HashMap<String,Object>();
    input = new InsertDocumentInput();
    output = new InsertDocumentOutput();
    error = new InsertDocumentError();
    this.serviceName = serviceName;
  }

  public void setAccessToken(String accessToken)
  {
    this.accessToken = accessToken;
  }

  public void setFileName(String fileName)
  {
    this.fileName = fileName;
  }

  public void setDocClass(String docClass)
  {
    this.docClass = docClass;
  }

  public void setFile(String file)
  {
    this.file = file;
  }

  public void setFile(byte[] file)
  {
    this.file = (file != null) ? Base64.getEncoder().encodeToString(file) : null;
  }

  public void setTypeMIME(String typeMIME)
  {
    this.typeMIME = typeMIME;
  }

  public void setStoragePath(String storagePath)
  {
    this.storagePath = storagePath;
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
  public InsertDocumentInput getInput()
  {
    return input;
  }

  @Override
  public InsertDocumentOutput getOutput()
  {
    return output;
  }

  @Override
  public InsertDocumentError getError()
  {
    return error;
  }

  private void setInput(InsertDocumentInput input)
  {
    this.input.loadModelData(input);
  }

  private void setOutput(InsertDocumentOutput output)
  {
    this.output.loadModelData(output);
  }

  private void setError(InsertDocumentError error)
  {
    this.error.loadModelData(error);
  }

  public boolean validateParameters()
  {
    boolean control = true;

    control = (control) && (fileName != null);
    control = (control) && (docClass != null);
    control = (control) && (file != null);
    control = (control) && (typeMIME != null);
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
    InsertDocumentInput in = null;
    InsertDocumentOutput out = null;
    InsertDocumentError error = null;
    InsertDocumentErrorFunctional error1 = null;
    InsertDocumentErrorTechnical error2 = null;
    InsertDocumentErrorInternal error3 = null;
    List<ApiRestModel> errors = null;
    DocumentMetadata metadata = null;
    int status = -1;

    String debug = null;

    if (validateParameters())
    {
      properties = UtilRestServices.getPropertyFile(FILENET_PROPETIES_FILE);

      if (properties != null)
      {
        application = properties.getProperty("OauthToken_".concat(serviceName.concat("_").concat("User")));
        protocol = properties.getProperty(serviceName.concat("_").concat("InsertDocument_Protocol"));
        port = properties.getProperty(serviceName.concat("_").concat("InsertDocument_Port"));
        host = properties.getProperty(serviceName.concat("_").concat("InsertDocument_Host"));
        file = properties.getProperty(serviceName.concat("_").concat("InsertDocument_File"));
        sslalgorithm = properties.getProperty(serviceName.concat("_").concat("InsertDocument_SslAlgorithm"));
        ignorecertificate =properties.getProperty(serviceName.concat("_").concat("InsertDocument_IgnoreCertificate"));
        //Obtiene el path para guardar en filenet de las properties
        if(this.storagePath.isEmpty()) {
            this.setStoragePath(properties.getProperty(serviceName.concat("_").concat("StoragePath")));
        }
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

          in = new InsertDocumentInput();
          out = new InsertDocumentOutput();
          error = new InsertDocumentError();
          error1 = new InsertDocumentErrorFunctional();
          error2 = new InsertDocumentErrorTechnical();
          error3 = new InsertDocumentErrorInternal();
          metadata = new DocumentMetadata();

          in.setFileName(fileName);
          in.setDocClass(docClass);
          in.setFile(this.file);
          in.setTypeMIME(typeMIME);
          in.setStoragePath(storagePath);

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

          status = adapter.post(in, out, errors, headers);

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
                if (item instanceof InsertDocumentErrorFunctional)
                {
                  final InsertDocumentErrorFunctional obj = (InsertDocumentErrorFunctional) item;

                  error.setErrorCode(obj.getErrorCode());
                  error.setMessage(obj.getMessage());
                }
                else
                  if (item instanceof InsertDocumentErrorTechnical)
                  {
                    final InsertDocumentErrorTechnical obj = (InsertDocumentErrorTechnical) item;

                    error.setErrorCode(Integer.parseInt(obj.getHttpCode()));
                    error.setMessage(obj.getHttpMessage() + " - " + obj.getMoreInformation());
                  }
                  else
                    if (item instanceof InsertDocumentErrorInternal)
                    {
                      final InsertDocumentErrorInternal obj = (InsertDocumentErrorInternal) item;

                      error.setErrorCode(obj.getStatus());
                      error.setMessage(obj.getError() + " - " + obj.getMessage());
                    }

                break;
              }
            }

            setError(error);

            debug = (new StringBuilder("Error calling FileNet InsertDocument service: "))
                .append("docClass [").append(docClass).append("] ")
                .append("fileName [").append(fileName).append("] ")
                .append("typeMIME [").append(typeMIME).append("] ")
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
              .append(FILENET_PROPETIES_FILE).append(" ")
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
            .append(FILENET_PROPETIES_FILE).append(" ")
            .toString();

        Log.error(this, debug);
      }
    }
    else
    {
      debug = (new StringBuilder("Invalid parameters calling FileNet InsertDocument service: "))
          .append("docClass [").append(docClass).append("] ")
          .append("fileName [").append(fileName).append("] ")
          .append("typeMIME [").append(typeMIME).append("] ")
          .toString();

      Log.error(this, debug);
    }

    return status;
  }
}
