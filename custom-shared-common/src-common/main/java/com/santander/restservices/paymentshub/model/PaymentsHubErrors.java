package com.santander.restservices.paymentshub.model;

import com.calypso.tk.core.Util;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestModelRoot;

import java.util.List;

public class PaymentsHubErrors extends ApiRestModelRoot {

  @JsonProperty("errors")
  private List<PaymentsHubError> errors;

  private String message;

  // Constructors
  public PaymentsHubErrors() {
    super();
  }

  public PaymentsHubErrors(final PaymentsHubErrors errors) {
    this();
    loadModelData(errors);
  }

  // Setters and Getters
  public List<PaymentsHubError> getErrors() {
    return errors;
  }

  public void setErrors(List<PaymentsHubError> errors) {
    this.errors = errors;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public boolean checkModelDataLoaded() {
    return errors != null;
  }

  @Override
  public void loadModelData(ApiRestModel model) {
    if (model != null && model instanceof PaymentsHubErrors) {
      final PaymentsHubErrors data = (PaymentsHubErrors) model;
      setErrors(data.getErrors());
      setMessage(data.getMessage());
    }
  }

  @Override
  public Class<PaymentsHubErrors> retriveModelClass() {
    return PaymentsHubErrors.class;
  }

  @Override
  public String toString() {
    final StringBuffer info = new StringBuffer();
    if (!Util.isEmpty(errors)) {
      final boolean isSeveralErrors = errors.size() > 1;
      int numberOfError = 0;
      for (final PaymentsHubError error : errors) {

        info.append("Error");

        if (isSeveralErrors) {
          numberOfError++;
          info.append(" ").append(numberOfError);
        }

        info.append(" : [").append(error.toString()).append("]");

        if (isSeveralErrors) {
          info.append(". ");
        }

      }
    }

    if (!Util.isEmpty(message)) {
      info.append(" Message : [").append(message).append("]");
    }

    return info.toString();
  }

}
