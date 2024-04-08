package com.santander.restservices;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public abstract class ApiRestModelRoot implements ApiRestModel
{
	@JsonIgnore
	private String mimeType = null;
	@JsonIgnore
	private byte[] messageData = null;
	
	@Override
	public void setMimeType(String mimeType) 
	{
		this.mimeType = mimeType;
	}

	@Override
	public String getMimeType() 
	{
		return this.mimeType;
	}

	@Override
	public void pushTextMessage(String message) 
	{
		if (message != null)
			this.messageData = message.getBytes();
	}

	@Override
	public String pullTextMessage() 
	{
		String out = null;
		
		if (this.messageData != null)
			out = new String(this.messageData);
		
		return out;
	}

	@Override
	public void pushBinaryMessage(byte[] message) 
	{
		this.messageData = message;
	}

	@Override
	public byte[] pullBinaryMessage() 
	{
		return this.messageData;
	}

    @Override
    public String toString()
    {
        ReflectionToStringBuilder tsb = new ReflectionToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
        return tsb.toString();
    }
}
