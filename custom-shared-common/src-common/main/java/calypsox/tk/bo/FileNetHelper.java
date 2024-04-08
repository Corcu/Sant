package calypsox.tk.bo;

import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.DomainValues;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.santander.restservices.ApiRestAdapter;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.filenet.InsertDocumentService;
import com.santander.restservices.filenet.UpdateDocumentService;
import com.santander.restservices.filenet.model.InsertDocumentInput;
import com.santander.restservices.filenet.model.UpdateDocumentInput;
import com.santander.restservices.jwt.JwtTokenSimpleServiceFileNet;
import com.santander.restservices.oauth.FileNetOauthTokenService;
import calypsox.tk.bo.util.FormatUtil;
import calypsox.tk.bo.util.StringUtil;


public class FileNetHelper{

private static final String LOG_CATEGORY = FileNetHelper.class.getName();
    
    public static final String FILENET_AUTHORIZATION_SCOPE_READ = "filenet.read";
    public static final String FILENET_AUTHORIZATION_SCOPE_WRITE = "filenet.write";
    public static final String FILENET_AUTHORIZATION_GRANT_TYPE_BAERER = "urn:ietf:params:oauth:grant-type:jwt-bearer";
       
    public static final String DN_FILENET_PARAMS = "FILENET.parameters";
    public static final String DV_FILENET_ACCESS_TOKEN_EXPIRY_TIME = "AccessTokenExpiryTime";
    public static final String DV_FILENET_ACCESS_TOKEN_GAP_TIME = "AccessTokenGapTime";
    
    public static final int INIT_SESION_MAX_ATTEMPTS = 3;
        
    private static String _jwt = null;
    private static String _access_read = null;
    private static String _access_write = null;
    private static JDatetime _access_read_timestamp = null;
    private static JDatetime _access_write_timestamp = null;
    
    private int status = -1;
    private String errorMsg = null;
    private String textMsg = null;
    private String versionId = null;
    private String documentId = null;
    private String fileMimeType = null;
    private byte[] fileBytes = null;
    
    private  FileNetHelper()
    {
    	super();	
    }
    
    public static FileNetHelper getInstance()
    {
        return new FileNetHelper();
    }

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	public String getTextMsg() {
		return textMsg;
	}

	public void setTextMsg(String textMsg) {
		this.textMsg = textMsg;
	}

	public String getVersionId() {
		return versionId;
	}

	public void setVersionId(String versionId) {
		this.versionId = versionId;
	}

	public String getDocumentId() {
		return documentId;
	}

	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}

	public String getFileMimeType() {
		return fileMimeType;
	}

	public void setFileMimeType(String fileMimeType) {
		this.fileMimeType = fileMimeType;
	}

	public byte[] getFileBytes() {
		return fileBytes;
	}

	public void setFileBytes(byte[] fileBytes) {
		this.fileBytes = fileBytes;
	}
	
	 private static synchronized  boolean initSession(String nameService, String mode, int attempts)
	    {
	        boolean control = false;
	        
	    	if (_jwt == null)
	    		getJwtToken(nameService);
	        
	        if (_jwt != null)
	        {
	            getAccessToken(nameService,mode);
	            
	            if ((_access_read != null) && (mode.equalsIgnoreCase(FILENET_AUTHORIZATION_SCOPE_READ)))
	            {
	                control = true;
	            }
	            else
	            if ((_access_write != null) && (mode.equalsIgnoreCase(FILENET_AUTHORIZATION_SCOPE_WRITE)))
	            {
	                control = true;
	            }
	            else
	            {
	            	if (attempts > 0)
	            		control = initSession(nameService,mode, --attempts);
	            }
	        }
	        else
	        {
	        	if (attempts > 0)
	        		control = initSession(nameService,mode, --attempts);
	        }
	    
	        return control;
	    }
	 
	 private static boolean getJwtToken(String nameService)
	    {
	        boolean control = false;
	        JwtTokenSimpleServiceFileNet service = null;
	        int status = -1;
	        
	        service = new JwtTokenSimpleServiceFileNet(nameService);
	        status = service.callService();
	        
	        if (ApiRestAdapter.isOKHttpStatus(status))
	        {
	          _jwt = (service.getOutput() != null) ? service.getOutput().getToken() : null;
	          _access_read = null;
	          _access_write = null;
	          _access_read_timestamp = null;
	          _access_write_timestamp = null;
	          control = true;
	        }
	        else
	        {
	          final String msg = String.format("Status JwtToken service : %d ", status);
	          Log.error(LOG_CATEGORY, msg);
	          Log.error(LOG_CATEGORY, service.getError().getErrors().toString());
	          
	          _jwt = null;
	          _access_read = null;
	          _access_write = null;
	          _access_read_timestamp = null;
	          _access_write_timestamp = null;

	        }
	        
	        return control;
	    }
	 
	 
	 
	 private static boolean getAccessToken(String nameService, String mode)
	    {
		 	
	        boolean control = false;
	        FileNetOauthTokenService service = null;
	        int status = -1;
	        
	        if (_jwt != null)
	        {
	            service = new FileNetOauthTokenService(nameService);
	            
	            service.setScope(mode);
	            service.setGrantType(FILENET_AUTHORIZATION_GRANT_TYPE_BAERER);
	            service.setJwtToken(_jwt);
	            
	            status = service.callService();
	            
	            if (ApiRestAdapter.isOKHttpStatus(status))
	            {
	            	if (FILENET_AUTHORIZATION_SCOPE_READ.equalsIgnoreCase(mode)) {
	            		_access_read = (service.getOutput() != null) ? service.getOutput().getAccessToken() : null;
	            		_access_read_timestamp = (_access_read != null) ? new JDatetime() : null;
	            	} else if (FILENET_AUTHORIZATION_SCOPE_WRITE.equalsIgnoreCase(mode)) {
	            		_access_write = (service.getOutput() != null) ? service.getOutput().getAccessToken() : null;
	            		_access_write_timestamp = (_access_write != null) ? new JDatetime() : null;
	            	} else {
	            	  _access_read = null;
	                _access_write = null;
	                _access_read_timestamp = null;
	                _access_write_timestamp = null;
	            	}
	            	
	            	control = true;
	            }
	            else
	            {
	              
	              final String msg = String.format(
	                  "Status AccessToken service : [%d] . Mode [%s]. Tokens to null : JWT [%s] - AccessToken [%s] ", status,
	                  mode, _jwt, FILENET_AUTHORIZATION_SCOPE_READ.equalsIgnoreCase(mode) ? _access_read : _access_write);
	              Log.error(LOG_CATEGORY, msg);
	              Log.error(LOG_CATEGORY, service.getError().getErrorDescription());
	              
	              _jwt = null;
	              _access_read = null;
	              _access_write = null;
	              _access_read_timestamp = null;
	              _access_write_timestamp = null;
	              
	            }
	        }
	        
	        return control;
	    }
	 
	 private static synchronized  boolean isAccessTokenSessionExpired(final boolean isReadAccess) {
	      String debug = "";

	      JDatetime limitDatetime = (isReadAccess) ? _access_read_timestamp : _access_write_timestamp;

	      // if is null, init the accesToken session
	      if (limitDatetime == null) {
	        return true;
	      }

	      // Get Values from DomainValues
	      final String expiryTime = DomainValues.comment(DN_FILENET_PARAMS, DV_FILENET_ACCESS_TOKEN_EXPIRY_TIME); // 600 seconds
	      final String gapTime = DomainValues.comment(DN_FILENET_PARAMS, DV_FILENET_ACCESS_TOKEN_GAP_TIME); // 10 seconds

	      if (NumberUtils.isCreatable(expiryTime) && NumberUtils.isCreatable(gapTime)) {
	        final int expiryTimeInt = Integer.parseInt(expiryTime);
	        final int gapTimeInt = Integer.parseInt(gapTime);
	        final int time = (expiryTimeInt - gapTimeInt)*(1000); // milliseconds
	        limitDatetime = (isReadAccess) ? _access_read_timestamp.add(time) : _access_write_timestamp.add(time);

	      } else {
	        debug = String.format("Check values for Expiry Time [%s] and Gap Time [%s], they are not correct values.", expiryTime, gapTime);
	        Log.debug(LOG_CATEGORY, debug);
	      }

	      // Get Now
	      final JDatetime now = new JDatetime();
	      
	      final String nowStr = FormatUtil.getInstance(now).parseDateTimeToString(FormatUtil.DATE_TIME_DEFAULT_FORMAT);
	      final String limitStr = FormatUtil.getInstance(limitDatetime).parseDateTimeToString(FormatUtil.DATE_TIME_DEFAULT_FORMAT);
	      final JDatetime accessDateTime = (isReadAccess) ? _access_read_timestamp : _access_write_timestamp;
	      final String accessDatetimeStr = FormatUtil.getInstance(accessDateTime).parseDateTimeToString(FormatUtil.DATE_TIME_DEFAULT_FORMAT);
	      
	      if (now.after(limitDatetime)) {
	        // Session expired
	        debug = String.format("Session expired : Mode [%s]. Log in at [%s]. Expired Token [%s]. Try to call service at [%s] - Limit at [%s]",
	            (isReadAccess) ? FILENET_AUTHORIZATION_SCOPE_READ : FILENET_AUTHORIZATION_SCOPE_WRITE, accessDatetimeStr,
	            (isReadAccess) ? _access_read : _access_write, nowStr, limitStr);
	        Log.debug(LOG_CATEGORY, debug);

	        if (isReadAccess) {
	          _access_read_timestamp = null;
	          _access_read = null;
	        } else {
	          _access_write_timestamp = null;
	          _access_write = null;
	        }

	        return true; // Session expired

	      }

	      // Session Active
	      debug = String.format("Session active : Mode [%s]. Log in at [%s]. Active Token [%s]. Try to call service at [%s] - Limit at [%s].",
	          (isReadAccess) ? FILENET_AUTHORIZATION_SCOPE_READ : FILENET_AUTHORIZATION_SCOPE_WRITE, accessDatetimeStr,
	          (isReadAccess) ? _access_read : _access_write, nowStr, limitStr);
	      Log.debug(LOG_CATEGORY, debug);
	      return false; // Session not expired

	    }
	 
	 
		 
	 public boolean callInsertDocumentService(String nameService, String fileName, String typeMIME, byte[] file, String docClass, Map<String,Object> metadata, String storagePath)
	    {
		 	boolean control = true;
		 	String debug = null;

	      	// Check Expired write access token
	      	final boolean isExpired = isAccessTokenSessionExpired(false);

	      	// Init session
	      	control = (isExpired) ? initSession(nameService, FILENET_AUTHORIZATION_SCOPE_WRITE, INIT_SESION_MAX_ATTEMPTS) : control;

	      	if(!control) {
	      		debug = (new StringBuilder())
	      	            .append("Unable to invoke filenet service: ")
	      	            .append("jwt [").append((_jwt != null) ? "OK" : "KO").append("] ")
	      	            .append("_access_write [").append((_access_write != null) ? "OK" : "KO").append("] ")
	      	            .toString();

	      	        Log.error(LOG_CATEGORY, debug);
	      	        return control;
	      	}

	      	control = false;
	        InsertDocumentInput input = null;
	        InsertDocumentService service = null;
	        StringBuilder msg = null;
	        
	        
	        try
	        {
	            service = new InsertDocumentService(nameService);
	            
	            service.setAccessToken(_access_write);
	            
	            service.setFileName(fileName); 
	            service.setDocClass(docClass);
	            service.setFile(file);
	            service.setTypeMIME(typeMIME); 
	            service.setMetadata(metadata);
	            service.setStoragePath(storagePath);
	            
	            this.status = service.callService();
	            
	            if (ApiRestAdapter.isOKHttpStatus(this.status))
	            {
	                this.versionId = null;
	                this.documentId = (service.getOutput() != null) ? service.getOutput().getIdDocument() : null;
	                this.errorMsg = "Document stored in FileNet successfully";
	                
	                control = true;
	            }
	            else
	            {
	                this.errorMsg = (service.getError() != null) ? service.getError().getMessage() : null;
	                
	            	debug = (new StringBuilder())
	                        .append("Error inserting FileNet document: ")
	                        .append("docClass [").append(docClass).append("] ")
	                        .append("fileName [").append(fileName).append("] ")
	                        .append("typeMIME [").append(typeMIME).append("] ")
	                        .append("status [").append(status).append("] ")
	                        .append("error [").append(this.errorMsg).append("] ")
	                        .toString();

	            	if (InsertDocumentService.AUTHENTICATION_ERROR != status) {
	            		Log.error(LOG_CATEGORY, debug);
	            	}
	            }
	            
	            input = service.getInput();
	            
	            msg = new StringBuilder();
	            msg.append("Document ID: ").append((this.documentId != null) ? this.documentId : "").append("\n");
	            msg.append("Version ID: ").append((this.versionId != null) ? this.versionId : "").append("\n");
	            msg.append("Status Code: ").append(this.status).append("\n");
	            msg.append("Status Message: ").append((this.errorMsg != null) ? this.errorMsg : "").append("\n");
	            msg.append("\n ");
	            
	            if (input != null)
	            {
	            	input.setFile("");
	            	msg.append(printJsonString(input));
	                msg.append("\n ");
	            }
	            
	        	this.textMsg = msg.toString();
	        	
	        	if (!ApiRestAdapter.isOKHttpStatus(this.status)) {
					if (InsertDocumentService.AUTHENTICATION_ERROR == status) {
						final String accessDatetimeStr = (_access_write_timestamp != null)
								? FormatUtil.getInstance(_access_write_timestamp).parseDateTimeToString(
										FormatUtil.DATE_TIME_DEFAULT_FORMAT)
								: "";
						debug = String.format(
								"Authentication Error Inserting FileNet Document : Status [%d] - Mode [%s] - Log in at [%s] - Access Token [%s].",
								status, FILENET_AUTHORIZATION_SCOPE_WRITE, accessDatetimeStr, _access_write);
						Log.debug(LOG_CATEGORY, debug);
					} else {
						Log.error(LOG_CATEGORY, this.textMsg);
					}
	        	}
	        		
	        }
	        catch (Exception e)
	        {
	            this.errorMsg = (e.getMessage() != null) ? StringUtil.truncate(e.getMessage(), 255) : "Error inserting FileNet document";
	            
	            debug = (new StringBuilder())
	                    .append("Error inserting FileNet document: ")
	                    .append("docClass [").append(docClass).append("] ")
	                    .append("fileName [").append(fileName).append("] ")
	                    .append("typeMIME [").append(typeMIME).append("] ")
	                    .append("with exception ").append(e.toString()).append(" - ").append(e.getMessage()).append(" ")
	                    .toString();

	            Log.error(LOG_CATEGORY, debug, e);
	        }
	        
	        return control;
	    }
	 
	 
	 
	    
	   
	    
	    public boolean callUpdateDocumentService(String nameService, String idDocument, byte[] file, Map<String,Object> metadata)
	    {
	    	boolean control = true;
		 	String debug = null;

	      	// Check Expired write access token
	      	final boolean isExpired = isAccessTokenSessionExpired(false);

	      	// Init session
	      	control = (isExpired) ? initSession(nameService, FILENET_AUTHORIZATION_SCOPE_WRITE, INIT_SESION_MAX_ATTEMPTS) : control;

	      	if(!control) {
	      		debug = (new StringBuilder())
	      	            .append("Unable to invoke filenet service: ")
	      	            .append("jwt [").append((_jwt != null) ? "OK" : "KO").append("] ")
	      	            .append("_access_write [").append((_access_write != null) ? "OK" : "KO").append("] ")
	      	            .toString();

	      	        Log.error(LOG_CATEGORY, debug);
	      	        return control;
	      	}

	      	control = false;
	        UpdateDocumentInput input = null;
	        UpdateDocumentService service = null;
	        StringBuilder msg = null;
	        
	        
	        try
	        {
	            service = new UpdateDocumentService(nameService);
	            
	            service.setAccessToken(_access_write);
	            
	            service.setIdDocument(idDocument); 
	            service.setFile(file);
	            service.setMetadata(metadata);
	            
	            this.status = service.callService();
	            
	            if (ApiRestAdapter.isOKHttpStatus(this.status))
	            {
	                this.versionId = (service.getOutput() != null) ? service.getOutput().getIdVersion() : null;
	                this.documentId = (service.getOutput() != null) ? service.getOutput().getIdDocument() : null;
	                this.errorMsg = "Document stored in FileNet successfully";
	                
	                control = true;
	            }
	            else
	            {
	                this.errorMsg = (service.getError() != null) ? service.getError().getMessage() : null;
	                
	                debug = (new StringBuilder())
	                        .append("Error updating FileNet document: ")
	                        .append("idDocument [").append(idDocument).append("] ")
	                        .append("status [").append(status).append("] ")
	                        .append("error [").append(this.errorMsg).append("] ")
	                        .toString();

	                if (UpdateDocumentService.AUTHENTICATION_ERROR != status) {
	                	Log.error(LOG_CATEGORY, debug);
	                }
	            }
	            
	            input = service.getInput();
	            
	            msg = new StringBuilder();
	            msg.append("Document ID: ").append((this.documentId != null) ? this.documentId : "").append("\n");
	            msg.append("Version ID: ").append((this.versionId != null) ? this.versionId : "").append("\n");
	            msg.append("Status Code: ").append(this.status).append("\n");
	            msg.append("Status Message: ").append((this.errorMsg != null) ? this.errorMsg : "").append("\n");
	            msg.append("\n ");
	            
	            if (input != null)
	            {
	            	input.setFile("");
	            	msg.append(printJsonString(input));
	                msg.append("\n ");
	            }
	            
	        	this.textMsg = msg.toString();
	        	
	        	if (!ApiRestAdapter.isOKHttpStatus(this.status)) {
	        		if (UpdateDocumentService.AUTHENTICATION_ERROR == status) {
	        		  final String accessDatetimeStr = (_access_write_timestamp != null) ? FormatUtil.getInstance(_access_write_timestamp).parseDateTimeToString(FormatUtil.DATE_TIME_DEFAULT_FORMAT) : "";
	              debug = String.format("Authentication Error Updating FileNet Document : Status [%d] - Mode [%s] - Log in at [%s] - Access Token [%s].", 
	                  status, FILENET_AUTHORIZATION_SCOPE_WRITE, accessDatetimeStr, _access_write);
	              Log.debug(LOG_CATEGORY, debug);
	        		} else {
	        		  Log.error(LOG_CATEGORY, this.textMsg);
	        		}
	        	}
	        }
	        catch (Exception e)
	        {
	            this.errorMsg = (e.getMessage() != null) ? StringUtil.truncate(e.getMessage(), 255) : "Error updating FileNet document";
	            
	            debug = (new StringBuilder())
	                    .append("Error updating FileNet document: ")
	                    .append("idDocument [").append(idDocument).append("] ")
	                    .append("with exception ").append(e.toString()).append(" - ").append(e.getMessage()).append(" ")
	                    .toString();

	            Log.error(LOG_CATEGORY, debug, e);
	        }
	        
	        return control;
	    }
	    
	    private String printJsonString(ApiRestModel model)
	    {
	    	String out = null;
	    	
	        ObjectMapper mapper = null;

	        String debug = null;
	    	
	        try 
	        {
	            mapper = new ObjectMapper();
	            mapper.setSerializationInclusion(Include.NON_NULL);

	            if (model != null)
	            	out = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model);
		} 
	        catch (JsonProcessingException e) 
	        {
	            debug = (new StringBuilder())
	                    .append("Error printing json ")
	                    .append("with exception ").append(e.toString()).append(" - ").append(e.getMessage()).append(" ")
	                    .toString();

	            Log.error(LOG_CATEGORY, debug);
			} 
	        
	    	
	    	return out;
	    }
	 
	 
	 
	 
	 
	 
}
