package com.santander.restservices.jwt.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestModelRoot;

import java.util.ArrayList;
import java.util.List;

public class JwtTokenSimpleErrors extends ApiRestModelRoot
{
    private List<JwtTokenSimpleError> errors = null;
    
    public JwtTokenSimpleErrors()
    {
        super();
        this.errors = new ArrayList<JwtTokenSimpleError>();
    }

    public JwtTokenSimpleErrors(JwtTokenSimpleErrors errors) 
    {
		this();
		loadModelData(errors);
	}
	
    @JsonGetter("errors")
    public List<JwtTokenSimpleError> getErrors()
    {
        return errors;
    }

    @JsonSetter("errors")
    public void setErrors(List<JwtTokenSimpleError> errors)
    {
        this.errors = errors;
    }

	@Override
	public boolean checkModelDataLoaded() 
	{
    	boolean control = true;
    	
        control = (control) && (this.errors != null);
        
        return control;
	}
	
    @Override
    public void loadModelData(ApiRestModel model)
    {
        if (model != null)
        {
            if (model instanceof JwtTokenSimpleErrors)
            {
                JwtTokenSimpleErrors data = (JwtTokenSimpleErrors) model;
                
                this.setErrors(data.getErrors());
            } 
        }
    }

    @Override
    public Class<JwtTokenSimpleErrors> retriveModelClass()
    {
        return JwtTokenSimpleErrors.class;
    }
}
