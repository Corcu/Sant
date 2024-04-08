package com.santander.restservices.datasign.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestModelRoot;

public class DataSignError extends ApiRestModelRoot {
  /** Code of the error */
  @JsonProperty("code")
  private int code = -1;

  /** Description of the error */
  @JsonProperty("description")
  private String description;

  /** Level of the error */
  @JsonProperty("level")
  private String level;

  /** Message of the error */
  @JsonProperty("message")
  private String message;

  public DataSignError() {
    super();
  }

  public DataSignError(final DataSignError error) {
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
    if (model != null && model instanceof DataSignError) {
      final DataSignError dataSignError = (DataSignError) model;
      setCode(dataSignError.getCode());
      setDescription(dataSignError.getDescription());
      setLevel(dataSignError.getLevel());
      setMessage(dataSignError.getMessage());
    }

  }

  @Override
  public Class<DataSignError> retriveModelClass() {
    return DataSignError.class;
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
