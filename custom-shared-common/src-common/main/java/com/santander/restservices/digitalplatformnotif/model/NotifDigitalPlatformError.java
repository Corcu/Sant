package com.santander.restservices.digitalplatformnotif.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestModelRoot;

public class NotifDigitalPlatformError extends ApiRestModelRoot {
	
  /** Error code */
  @JsonProperty("code")
  private int code = -1;

  /** Error description */
  @JsonProperty("description")
  private String description;

  /** Level of the error */
  @JsonProperty("level")
  private String level;

  /** Message of the error */
  @JsonProperty("message")
  private String message;

  public NotifDigitalPlatformError() {
    super();
  }

  public NotifDigitalPlatformError(final NotifDigitalPlatformError error) {
    this();
    loadModelData(error);
  }

  public int getCode() {
    return code;
  }

  public void setCode(int code) {
    this.code = code;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getLevel() {
    return level;
  }

  public void setLevel(String level) {
    this.level = level;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public boolean checkModelDataLoaded() {
    return (code > 0) && (description != null) && (level != null) && (message != null);
  }

  @Override
  public void loadModelData(ApiRestModel model) {
    if (model != null && model instanceof NotifDigitalPlatformError) {
      final NotifDigitalPlatformError notifError = (NotifDigitalPlatformError) model;
      setCode(notifError.getCode());
      setDescription(notifError.getDescription());
      setLevel(notifError.getLevel());
      setMessage(notifError.getMessage());
    }

  }

  @Override
  public Class<NotifDigitalPlatformError> retriveModelClass() {
    return NotifDigitalPlatformError.class;
  }

  @Override
  public String toString() {
    final StringBuffer info = new StringBuffer();
    info.append("Code : [").append(code).append("] - ");
    info.append("Message : [").append((message != null) ? message : "").append("] - ");
    info.append("Level : [").append((level != null) ? level : "").append("] - ");
    info.append("Description : [").append((description != null) ? description : "");
    return info.toString();
  }

}