package com.santander.restservices.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.santander.restservices.ApiRestModel;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.util.HashMap;
import java.util.Map;

public class FormDataInput extends HashMap<String,String> implements ApiRestModel
{
	private static final long serialVersionUID = -2551329290761097628L;
	
	public FormDataInput() 
	{
		super();
	}

    public FormDataInput(FormDataInput input)
    {
		this();
		loadModelData(input);
	}
	
	@JsonAnyGetter
	public Map<String, String> getFormData() 
	{
		return this;
	}

	public void setFormData(Map<String, String> formdata) 
	{
		this.clear();
		this.putAll(formdata);
	}

	public String getFormData(String key)
	{ 
		return this.get(key); 
	}
	
	@JsonAnySetter
	public void addFormData(String key, String value)
	{ 
		this.put(key, value); 
	}

	@Override
	public boolean checkModelDataLoaded() 
	{
    	boolean control = true;
    	
        control = (control) && (this.size() > 0);
        
        return control;
	}
	
	@Override
	public void loadModelData(ApiRestModel model)
	{
        if (model != null)
        {
            if (model instanceof FormDataInput)
            {
            	FormDataInput data = (FormDataInput) model;
                
            	this.setFormData(data.getFormData());
            } 
        }
	}

	@Override
	public void setMimeType(String mimeType) 
	{
	}

	@Override
	public String getMimeType() 
	{
		return null;
	}

	@Override
	public void pushTextMessage(String message) 
	{
	}

	@Override
	public String pullTextMessage() 
	{
		return null;
	}

	@Override
	public void pushBinaryMessage(byte[] message) 
	{
	}

	@Override
	public byte[] pullBinaryMessage() 
	{
		return null;
	}

	@Override
	public Class<InsertDocumentInput> retriveModelClass() 
	{
        return InsertDocumentInput.class;
	}

    @Override
    public String toString()
    {
        ReflectionToStringBuilder tsb = new ReflectionToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
        return tsb.toString();
    }
}
