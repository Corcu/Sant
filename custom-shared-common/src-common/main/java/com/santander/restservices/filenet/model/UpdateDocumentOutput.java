package com.santander.restservices.filenet.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestModelRoot;

public class UpdateDocumentOutput extends ApiRestModelRoot
{
    private String idVersion = null;
    private String idDocument = null;
	
    public UpdateDocumentOutput() 
    {
		super();
	}

    public UpdateDocumentOutput(UpdateDocumentOutput output) 
    {
		this();
		loadModelData(output);
	}
	
    @JsonGetter("versionId")
	public String getIdVersion() 
	{
		return idVersion;
	}

    @JsonSetter("versionId")
	public void setIdVersion(String idVersion) 
	{
		this.idVersion = idVersion;
	}

    @JsonGetter("GN_ID")
    public String getIdDocument() 
	{
		return idDocument;
	}

    @JsonSetter("GN_ID")
	public void setIdDocument(String idDocument) 
	{
		this.idDocument = idDocument;
	}

	@Override
	public boolean checkModelDataLoaded() 
	{
    	boolean control = true;
    	
        control = (control) && (this.idVersion != null);
        control = (control) && (this.idDocument != null);
        
        return control;
	}
	
	@Override
	public void loadModelData(ApiRestModel model) 
	{
        if (model != null)
        {
            if (model instanceof UpdateDocumentOutput)
            {
            	UpdateDocumentOutput data = (UpdateDocumentOutput) model;
                
                this.setIdVersion(data.getIdVersion());
                this.setIdDocument(data.getIdDocument());
            } 
        }
	}

	@Override
	public Class<UpdateDocumentOutput> retriveModelClass() 
	{
        return UpdateDocumentOutput.class;
	}
}
