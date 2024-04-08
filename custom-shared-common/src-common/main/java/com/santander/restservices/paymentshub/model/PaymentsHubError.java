package com.santander.restservices.paymentshub.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestModelRoot;

public class PaymentsHubError extends ApiRestModelRoot {

  @JsonProperty("code")
  private String code;

  @JsonProperty("message")
  private String message;

  @JsonProperty("level")
  private String level;

  @JsonProperty("description")
  private String description;

  // Constructors
  public PaymentsHubError() {
    super();
  }

  public PaymentsHubError(final PaymentsHubError error) {
    this();
    loadModelData(error);
  }

  // Setters and Getters
  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getLevel() {
    return level;
  }

  public void setLevel(String level) {
    this.level = level;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public boolean checkModelDataLoaded() {
    return true;
  }

  @Override
  public void loadModelData(ApiRestModel model) {
    if (model != null && model instanceof PaymentsHubError) {
      final PaymentsHubError data = (PaymentsHubError) model;
      setCode(data.getCode());
      setMessage(data.getMessage());
      setLevel(data.getLevel());
      setDescription(data.getDescription());
    }
  }

  @Override
  public Class<PaymentsHubError> retriveModelClass() {
    return PaymentsHubError.class;
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer();
    sb.append("Code [").append((code != null) ? code : "").append("] - ");
    sb.append("Message [").append((message != null) ? message : "").append("] - ");
    sb.append("Level [").append((level != null) ? level : "").append("] - ");
    sb.append("Description [").append((description != null) ? description : "").append("]");
    return sb.toString();
  }

  public String getInfoMessage() {
    final StringBuffer info = new StringBuffer();
    info.append("Message [").append((message != null) ? message : "").append("] - ");
    info.append("Description [").append((description != null) ? description : "").append("]");
    return info.toString();
  }

}
