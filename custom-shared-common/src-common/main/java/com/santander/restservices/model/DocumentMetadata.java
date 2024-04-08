package com.santander.restservices.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentMetadata 
{
	private Map<String,Object> metadata = null;

	public DocumentMetadata() 
	{
		super();
	    this.metadata = new HashMap<String,Object>();
	}
	
	@JsonAnyGetter
	public Map<String, Object> getMetadata() 
	{
		return metadata;
	}

	@JsonAnySetter
	public void setMetadata(String property, String value)
	{ 
		metadata.put(property, value); 
	}

	@SuppressWarnings("unchecked")
	public void load(Map<String,Object> data)
	{
		if (data != null)
		{
			for (String key : data.keySet())
			{
				Object value = data.get(key);

				if (value instanceof Map)
				{
					this.metadata.put(key, (Map<String,String>)value);
				}
				if (value instanceof List)
				{
					this.metadata.put(key, (List<String>)value);
				}
				else
				{
					this.metadata.put(key, value);
				}
			}
		}
	}
	
	public Object get(String key)
	{
		return this.metadata.get(key);
	}
	
	public void put(String key, Object value)
	{
		this.metadata.put(key, value);
	}
}
