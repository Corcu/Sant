package com.santander.restservices.jwt.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestModelRoot;

public class JwtTokenSimpleError extends ApiRestModelRoot
{
    private int code = -1;
    private String message = null;
    private String level = null;
    private String description = null;
    private String moreInfo = null;
    
    public JwtTokenSimpleError()
    {
        super();
    }
    
    public JwtTokenSimpleError(JwtTokenSimpleError error) 
    {
		this();
		loadModelData(error);
	}
	
    @JsonGetter("code")
    public int getCode()
    {
        return code;
    }
    
    @JsonSetter("code")
    public void setCode(int code)
    {
        this.code = code;
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
    
    @JsonGetter("level")
    public String getLevel()
    {
        return level;
    }
    
    @JsonSetter("level")
    public void setLevel(String level)
    {
        this.level = level;
    }
    
    @JsonGetter("description")
    public String getDescription()
    {
        return description;
    }
    
    @JsonSetter("description")
    public void setDescription(String description)
    {
        this.description = description;
    }
    
    @JsonGetter("moreInfo")
    public String getMoreInfo()
    {
        return moreInfo;
    }
    
    @JsonSetter("moreInfo")
    public void setMoreInfo(String moreInfo)
    {
        this.moreInfo = moreInfo;
    }

	@Override
	public boolean checkModelDataLoaded() 
	{
    	boolean control = true;
    	
        control = (control) && (this.code > 0);
        
        return control;
	}
	
    @Override
    public void loadModelData(ApiRestModel model)
    {
        if (model != null)
        {
            if (model instanceof JwtTokenSimpleError)
            {
                JwtTokenSimpleError data = (JwtTokenSimpleError) model;
                
                this.setCode(data.getCode());
                this.setMessage(data.getMessage());
                this.setLevel(data.getLevel());
                this.setDescription(data.getDescription());
                this.setMoreInfo(data.getMoreInfo());
            } 
        }
    }

    @Override
    public Class<JwtTokenSimpleError> retriveModelClass()
    {
        return JwtTokenSimpleError.class;
    }
}
