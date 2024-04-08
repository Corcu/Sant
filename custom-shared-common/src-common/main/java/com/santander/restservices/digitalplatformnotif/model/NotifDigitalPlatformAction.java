package com.santander.restservices.digitalplatformnotif.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public class NotifDigitalPlatformAction {
	private Map<String, Object> action = null;

	public NotifDigitalPlatformAction() {
		super();
		this.action = new HashMap<String, Object>();
	}

	@JsonAnyGetter
	public Map<String, Object> getAction() {
		return action;
	}

	@JsonAnySetter
	public void setAction(String property, String value) {
		action.put(property, value);
	}

	@SuppressWarnings("unchecked")
	public void load(Map<String, Object> data) {
		if (data != null) {
			for (String key : data.keySet()) {
				Object value = data.get(key);

				if (value instanceof Map) {
					this.action.put(key, (Map<String, String>) value);
				}
				if (value instanceof List) {
					this.action.put(key, (List<String>) value);
				} else {
					this.action.put(key, value);
				}
			}
		}
	}

	public Object get(String key) {
		return this.action.get(key);
	}

	public void put(String key, Object value) {
		this.action.put(key, value);
	}
}
