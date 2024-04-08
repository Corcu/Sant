package com.santander.restservices.filenet.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestModelRoot;

public class UpdateDocumentInput extends ApiRestModelRoot
{
    private String idDocument = null;
    private String file = null;
    private DocumentMetadata metadata = null;
	
    public UpdateDocumentInput() 
    {
		super();
		this.metadata = new DocumentMetadata();
	}

    public UpdateDocumentInput(UpdateDocumentInput input) 
    {
		this();
		loadModelData(input);
	}
	
    @JsonGetter("idDocument")
	public String getIdDocument() 
	{
		return idDocument;
	}

    @JsonSetter("idDocument")
	public void setIdDocument(String idDocument) 
	{
		this.idDocument = idDocument;
	}

    @JsonGetter("file")
	public String getFile() 
	{
		return file;
	}

    @JsonSetter("file")
	public void setFile(String file) 
	{
		this.file = file;
	}

    @JsonGetter("metadata")
	public DocumentMetadata getMetadata() 
	{
		return metadata;
	}

    @JsonSetter("metadata")
	public void setMetadata(DocumentMetadata metadata) 
	{
		this.metadata = metadata;
	}

	public Object getMetadataItem(String key) 
	{
		return this.metadata.get(key);
	}

	public void addMetadataItem(String key, Object value) 
	{
		this.metadata.put(key, value);
	}

	@Override
	public boolean checkModelDataLoaded() 
	{
    	boolean control = true;
    	
        control = (control) && (this.idDocument != null);
        control = (control) && (this.file != null);
        control = (control) && (this.metadata != null);
        
        return control;
	}
	
	@Override
	public void loadModelData(ApiRestModel model) 
	{
        if (model != null)
        {
            if (model instanceof UpdateDocumentInput)
            {
            	UpdateDocumentInput data = (UpdateDocumentInput) model;
                
            	this.setIdDocument(data.getIdDocument());
            	this.setFile(data.getFile());
            	this.setMetadata(data.getMetadata());
            } 
        }
	}

	@Override
	public Class<UpdateDocumentInput> retriveModelClass() 
	{
        return UpdateDocumentInput.class;
	}
}
