package com.santander.restservices.filenet.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestModelRoot;

public class UpdateDocumentErrorFunctional extends ApiRestModelRoot
{
    private int errorCode = -1;
    private String message = null;
    
    public UpdateDocumentErrorFunctional()
    {
        super();
    }

    public UpdateDocumentErrorFunctional(UpdateDocumentErrorFunctional error) 
    {
		this();
		loadModelData(error);
	}
	
	@JsonGetter("errorCode")
    public int getErrorCode() 
    {
		return errorCode;
	}

    @JsonSetter("errorCode")
	public void setErrorCode(int errorCode) 
	{
		this.errorCode = errorCode;
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

	@Override
	public boolean checkModelDataLoaded() 
	{
    	boolean control = true;
    	
        control = (control) && (this.errorCode > 0);
        
        return control;
	}
	
	@Override
	public void loadModelData(ApiRestModel model) 
	{
        if (model != null)
        {
            if (model instanceof UpdateDocumentErrorFunctional)
            {
            	UpdateDocumentErrorFunctional data = (UpdateDocumentErrorFunctional) model;
                
                this.setErrorCode(data.getErrorCode());
                this.setMessage(data.getMessage());
            } 
        }
	}

	@Override
	public Class<UpdateDocumentErrorFunctional> retriveModelClass() 
	{
        return UpdateDocumentErrorFunctional.class;
	}
}
