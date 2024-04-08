package com.santander.restservices.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestModelRoot;

public class InsertDocumentInput extends ApiRestModelRoot
{
	private String fileName = null;
    private String docClass = null;
    private String file = null;
    private String typeMIME = null;
    private String storagePath = null;
    private DocumentMetadata metadata = null;
	
    public InsertDocumentInput() 
    {
		super();
		this.metadata = new DocumentMetadata();
	}

    public InsertDocumentInput(InsertDocumentInput input) 
    {
		this();
		loadModelData(input);
	}
	
    @JsonGetter("fileName")
	public String getFileName() 
	{
		return fileName;
	}

    @JsonSetter("fileName")
	public void setFileName(String fileName) 
	{
		this.fileName = fileName;
	}

    @JsonGetter("docClass")
	public String getDocClass() 
	{
		return docClass;
	}

    @JsonSetter("docClass")
	public void setDocClass(String docClass) 
	{
		this.docClass = docClass;
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

    @JsonGetter("typeMIME")
	public String getTypeMIME() 
	{
		return typeMIME;
	}

    @JsonSetter("typeMIME")
	public void setTypeMIME(String typeMIME) 
	{
		this.typeMIME = typeMIME;
	}

    @JsonGetter("storagePath")
	public String getStoragePath() 
	{
		return storagePath;
	}

    @JsonSetter("storagePath")
	public void setStoragePath(String storagePath) 
	{
		this.storagePath = storagePath;
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
    	
        control = (control) && (this.fileName != null);
        control = (control) && (this.docClass != null);
        control = (control) && (this.file != null);
        control = (control) && (this.typeMIME != null);
        control = (control) && (this.storagePath != null);
        control = (control) && (this.metadata != null);
        
        return control;
	}
	
	@Override
	public void loadModelData(ApiRestModel model) 
	{
        if (model != null)
        {
            if (model instanceof InsertDocumentInput)
            {
            	InsertDocumentInput data = (InsertDocumentInput) model;
                
            	this.setFileName(data.getFileName());
            	this.setDocClass(data.getDocClass()); 
            	this.setFile(data.getFile());
            	this.setTypeMIME(data.getTypeMIME());
            	this.setStoragePath(data.getStoragePath());
            	this.setMetadata(data.getMetadata());
            } 
        }
	}

	@Override
	public Class<InsertDocumentInput> retriveModelClass() 
	{
        return InsertDocumentInput.class;
	}
}
