package com.santander.restservices.filenet.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestModelRoot;

public class UpdateDocumentErrorTechnical extends ApiRestModelRoot
{
    private String httpCode = null;
    private String httpMessage = null;
    private String moreInformation = null;
    
    public UpdateDocumentErrorTechnical()
    {
        super();
    }

    public UpdateDocumentErrorTechnical(UpdateDocumentErrorTechnical error) 
    {
		this();
		loadModelData(error);
	}
	
	@JsonGetter("httpCode")
	public String getHttpCode() 
	{
		return httpCode;
	}

    @JsonSetter("httpCode")
	public void setHttpCode(String httpCode) 
	{
		this.httpCode = httpCode;
	}

	@JsonGetter("httpMessage")
	public String getHttpMessage() 
	{
		return httpMessage;
	}

    @JsonSetter("httpMessage")
	public void setHttpMessage(String httpMessage) 
	{
		this.httpMessage = httpMessage;
	}

	@JsonGetter("moreInformation")
	public String getMoreInformation() 
	{
		return moreInformation;
	}

    @JsonSetter("moreInformation")
	public void setMoreInformation(String moreInformation) 
	{
		this.moreInformation = moreInformation;
	}

	@Override
	public boolean checkModelDataLoaded() 
	{
    	boolean control = true;
    	
        control = (control) && (this.httpCode != null);
        
        return control;
	}
	
	@Override
	public void loadModelData(ApiRestModel model) 
	{
        if (model != null)
        {
            if (model instanceof UpdateDocumentErrorTechnical)
            {
            	UpdateDocumentErrorTechnical data = (UpdateDocumentErrorTechnical) model;
                
                this.setHttpCode(data.getHttpCode());
                this.setHttpMessage(data.getHttpMessage());
                this.setMoreInformation(data.getMoreInformation());
            } 
        }
	}

	@Override
	public Class<UpdateDocumentErrorTechnical> retriveModelClass() 
	{
        return UpdateDocumentErrorTechnical.class;
	}
}
