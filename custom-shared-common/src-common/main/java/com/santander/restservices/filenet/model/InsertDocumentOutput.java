package com.santander.restservices.filenet.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestModelRoot;

public class InsertDocumentOutput extends ApiRestModelRoot
{
    private String idDocument = null;
	
    public InsertDocumentOutput() 
    {
		super();
	}

    public InsertDocumentOutput(InsertDocumentOutput output) 
    {
		this();
		loadModelData(output);
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
    	
        control = (control) && (this.idDocument != null);
        
        return control;
	}
	
	@Override
	public void loadModelData(ApiRestModel model) 
	{
        if (model != null)
        {
            if (model instanceof InsertDocumentOutput)
            {
            	InsertDocumentOutput data = (InsertDocumentOutput) model;
                
                this.setIdDocument(data.getIdDocument());
            } 
        }
	}

	@Override
	public Class<InsertDocumentOutput> retriveModelClass() 
	{
        return InsertDocumentOutput.class;
	}
}
