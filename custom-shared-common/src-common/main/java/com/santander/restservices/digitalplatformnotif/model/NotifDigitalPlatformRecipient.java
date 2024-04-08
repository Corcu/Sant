package com.santander.restservices.digitalplatformnotif.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class NotifDigitalPlatformRecipient {
	private Map<String, Object> recipient = null;

	public NotifDigitalPlatformRecipient() {
		super();
		this.recipient = new HashMap<String, Object>();
	}

	@JsonAnyGetter
	public Map<String, Object> getRecipient() {
		return recipient;
	}

	@JsonAnySetter
	public void setRecipients(Map<String, Object> value) {
		recipient = value;
		
	}

	@SuppressWarnings("unchecked")
	public void load(Map<String, Object> data) {
		{
			if (data != null)
			{
				for (String key : data.keySet())
				{
					Object value = data.get(key);

					if (value instanceof Map)
					{
						this.recipient.put(key, (Map<String,String>)value);
					}
					if (value instanceof List)
					{
						this.recipient.put(key, (List<String>)value);
					}
					else
					{
						this.recipient.put(key, value);
					}
				}
			}
		}
	}

	
}
