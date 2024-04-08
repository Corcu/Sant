package com.santander.restservices.digitalplatformnotif.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestModelRoot;
import com.santander.restservices.filenet.model.InsertDocumentInput;

public class NotifDigitalPlatformInput extends ApiRestModelRoot{

	private String tittle = null;
	private String content = null;
	private String application = null;
	private boolean isImportant = false;
	private NotifDigitalPlatformAction action = null;
	private List<NotifDigitalPlatformRecipient> recipients = null;
	
	@JsonGetter("title")
	public String getTittle() {
		return tittle;
	}
	@JsonSetter("title")
	public void setTittle(String tittle) {
		this.tittle = tittle;
	}
	
	@JsonGetter("content")
	public String getContent() {
		return content;
	}
	@JsonSetter("content")
	public void setContent(String content) {
		this.content = content;
	}
	
	@JsonGetter("application")
	public String getApplication() {
		return application;
	}
	@JsonSetter("application")
	public void setApplication(String application) {
		this.application = application;
	}
	@JsonGetter("isImportant")
	public boolean isImportant() {
		return isImportant;
	}
	@JsonSetter("isImportant")
	public void setImportant(boolean isImportant) {
		this.isImportant = isImportant;
	}
	
	@JsonGetter("action")
	public NotifDigitalPlatformAction getAction() {
		return action;
	}
	@JsonSetter("action")
	public void setAction(NotifDigitalPlatformAction action) {
		this.action = action;
	}
	
	@JsonGetter("recipients")
	public List<NotifDigitalPlatformRecipient> getRecipients() {
		return recipients;
	}
	@JsonSetter("recipients")
	public void setRecipients(List<NotifDigitalPlatformRecipient> recipients) {
		this.recipients = recipients;
	}


	
	public NotifDigitalPlatformInput() {
		super();
		this.action = new NotifDigitalPlatformAction();
		this.recipients = new ArrayList<>();
	}
	public NotifDigitalPlatformInput(NotifDigitalPlatformInput input) {
		this();
		loadModelData(input);
	}
	
	@Override
	public boolean checkModelDataLoaded() {
		
		boolean control = true;
        control = (control) && (this.tittle != null);
        control = (control) && (this.content != null);
        control = (control) && (this.action != null);
        control = (control) && (this.application != null);
        control = (control) && (this.recipients != null);
        
        return control;
	}

	@Override
	public void loadModelData(ApiRestModel model) {
		if (model instanceof InsertDocumentInput)
        {
			NotifDigitalPlatformInput data = (NotifDigitalPlatformInput) model;
            
        	this.setTittle(data.getTittle());
        	this.setContent(data.getContent()); 
        	this.setAction(data.getAction());
        	this.setApplication(data.getApplication());
        	this.setRecipients(data.getRecipients());
        	this.setImportant(isImportant());
        } 
		
	}

	@Override
	public Class<? extends ApiRestModel> retriveModelClass() {
		
		return NotifDigitalPlatformInput.class;
	}

}
