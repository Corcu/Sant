package com.santander.restservices.jwt.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestModelRoot;

public class JwtTokenSimpleOutput extends ApiRestModelRoot
{
    private String token = null;
    
    public JwtTokenSimpleOutput() 
    {
		super();
	}

    public JwtTokenSimpleOutput(JwtTokenSimpleOutput output) 
    {
		this();
		loadModelData(output);
	}
	
    @JsonGetter("token")
    public String getToken()
    {
        return token;
    }

    @JsonSetter("token")
    public void setToken(String token)
    {
        this.token = token;
    }

	@Override
	public boolean checkModelDataLoaded() 
	{
    	boolean control = true;
    	
        control = (control) && (this.token != null);
        
        return control;
	}
	
    @Override
    public void loadModelData(ApiRestModel model)
    {
        if (model != null)
        {
            if (model instanceof JwtTokenSimpleOutput)
            {
                JwtTokenSimpleOutput data = (JwtTokenSimpleOutput) model;
                
                this.setToken(data.getToken());
            } 
        }
    }

    @Override
    public Class<JwtTokenSimpleOutput> retriveModelClass()
    {
        return JwtTokenSimpleOutput.class;
    }
}
