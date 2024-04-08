package com.santander.restservices.oauth.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestModelRoot;

public class OauthTokenOutput extends ApiRestModelRoot
{
    private String accessToken = null;
    private String tokenType = null;
    private int expiresIn = -1;
    private String scope = null;
    
    public OauthTokenOutput() 
    {
		super();
	}

    public OauthTokenOutput(OauthTokenOutput output) 
    {
		this();
		loadModelData(output);
	}
	
    @JsonGetter("access_token")
    public String getAccessToken() 
    {
		return accessToken;
	}

    @JsonSetter("access_token")
	public void setAccessToken(String accessToken) 
	{
		this.accessToken = accessToken;
	}

    @JsonGetter("token_type")
	public String getTokenType() 
	{
		return tokenType;
	}

    @JsonSetter("token_type")
	public void setTokenType(String tokenType) 
	{
		this.tokenType = tokenType;
	}

    @JsonGetter("expires_in")
	public int getExpiresIn() 
	{
		return expiresIn;
	}

    @JsonSetter("expires_in")
	public void setExpiresIn(int expiresIn) 
	{
		this.expiresIn = expiresIn;
	}

    @JsonGetter("scope")
	public String getScope() 
	{
		return scope;
	}

    @JsonSetter("scope")
	public void setScope(String scope) 
	{
		this.scope = scope;
	}

	@Override
	public boolean checkModelDataLoaded() 
	{
    	boolean control = true;
    	
        control = (control) && (this.accessToken != null);
        
        return control;
	}
	
    @Override
    public void loadModelData(ApiRestModel model)
    {
        if (model != null)
        {
            if (model instanceof OauthTokenOutput)
            {
                OauthTokenOutput data = (OauthTokenOutput) model;
                
                this.setAccessToken(data.getAccessToken());
                this.setTokenType(data.getTokenType());
                this.setExpiresIn(data.getExpiresIn());
                this.setScope(data.getScope());
            } 
        }
    }

	@Override
    public Class<OauthTokenOutput> retriveModelClass()
    {
        return OauthTokenOutput.class;
    }
}
