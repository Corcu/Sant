package com.santander.restservices.oauth.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestModelRoot;

public class OauthTokenError extends ApiRestModelRoot
{
    private String error = null;
    private String errorDescription = null;
    
    public OauthTokenError() 
    {
		super();
	}

    public OauthTokenError(OauthTokenError error) 
    {
		this();
		loadModelData(error);
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

    @JsonGetter("error_description")
	public String getErrorDescription() 
	{
		return errorDescription;
	}

    @JsonSetter("error_description")
	public void setErrorDescription(String errorDescription) 
	{
		this.errorDescription = errorDescription;
	}

	@Override
	public boolean checkModelDataLoaded() 
	{
    	boolean control = true;
    	
        control = (control) && (this.error != null);
        
        return control;
	}
	
	@Override
    public void loadModelData(ApiRestModel model)
    {
        if (model != null)
        {
            if (model instanceof OauthTokenError)
            {
                OauthTokenError data = (OauthTokenError) model;
                
                this.setError(data.getError());
                this.setErrorDescription(data.getErrorDescription());
            } 
        }
    }

    @Override
    public Class<OauthTokenError> retriveModelClass()
    {
        return OauthTokenError.class;
    }
}
