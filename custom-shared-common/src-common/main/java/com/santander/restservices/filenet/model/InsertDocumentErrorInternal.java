package com.santander.restservices.filenet.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestModelRoot;

public class InsertDocumentErrorInternal extends ApiRestModelRoot
{
    private String timestamp = null;
    private int status = -1;
    private String error = null;
    private String message = null;
    private String path = null;
    
    public InsertDocumentErrorInternal()
    {
        super();
    }

    public InsertDocumentErrorInternal(InsertDocumentErrorInternal error) 
    {
		this();
		loadModelData(error);
	}
	
	@JsonGetter("timestamp")
	public String getTimestamp() 
	{
		return timestamp;
	}

    @JsonSetter("timestamp")
	public void setTimestamp(String timestamp) 
	{
		this.timestamp = timestamp;
	}

	@JsonGetter("status")
	public int getStatus() 
	{
		return status;
	}

    @JsonSetter("status")
	public void setStatus(int status) 
	{
		this.status = status;
	}

	@JsonGetter("error")
	public String getError() 
	{
		return error;
	}

    @JsonSetter("error")
	public void setError(String error) 
	{
		this.error = error;
	}

	@JsonGetter("message")
	public String getMessage() 
	{
		return message;
	}

    @JsonSetter("message")
	public void setMessage(String message) 
	{
		this.message = message;
	}

	@JsonGetter("path")
	public String getPath() 
	{
		return path;
	}

    @JsonSetter("path")
	public void setPath(String path) 
	{
		this.path = path;
	}

	@Override
	public boolean checkModelDataLoaded() 
	{
    	boolean control = true;
    	
        control = (control) && (this.status > 0);
        
        return control;
	}
	
	@Override
	public void loadModelData(ApiRestModel model) 
	{
        if (model != null)
        {
            if (model instanceof InsertDocumentErrorInternal)
            {
            	InsertDocumentErrorInternal data = (InsertDocumentErrorInternal) model;
                
                this.setTimestamp(data.getTimestamp());
                this.setStatus(data.getStatus());
                this.setError(data.getError());
                this.setMessage(data.getMessage());
                this.setPath(data.getPath());
            } 
        }
	}

	@Override
	public Class<InsertDocumentErrorInternal> retriveModelClass() 
	{
        return InsertDocumentErrorInternal.class;
	}
}
