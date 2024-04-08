package com.santander.restservices.paymentshub.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestModelRoot;
import com.santander.restservices.paymentshub.model.submodel.ResponseStructure;

public class PaymentsHubOutput extends ApiRestModelRoot {

  /** Response Structure */
  @JsonProperty("responseStructure")
  private ResponseStructure responseStructure;

  // Constructors
  public PaymentsHubOutput() {
    super();
  }

  public PaymentsHubOutput(final PaymentsHubOutput output) {
    this();
    loadModelData(output);
  }

  // Setters and Getters
  public ResponseStructure getResponseStructure() {
    return responseStructure;
  }

  public void setResponseStructure(ResponseStructure responseStructure) {
    this.responseStructure = responseStructure;
  }

  @Override
  public boolean checkModelDataLoaded() {
    if (responseStructure != null) {
      return responseStructure.checkModelData();
    }
    return true;

  }

  @Override
  public void loadModelData(final ApiRestModel model) {
    if (model != null && model instanceof PaymentsHubOutput) {
      final PaymentsHubOutput data = (PaymentsHubOutput) model;
      setResponseStructure(data.getResponseStructure());
    }
  }

  @Override
  public Class<PaymentsHubOutput> retriveModelClass() {
    return PaymentsHubOutput.class;
  }

  @Override
  public String toString() {

    final StringBuffer sb = new StringBuffer();
    if (responseStructure != null) {
      final String paymentId = responseStructure.getInstantPaymentId();
      final String valueDate = responseStructure.getValueDate();
      final Object transMessage = responseStructure.getTransformedMessage();
      sb.append("InstantPaymentId : [").append((paymentId != null) ? paymentId : "").append("] - ");
      sb.append("ValueDate : [").append((valueDate != null) ? valueDate : "").append("] - ");
      sb.append("TransformedMessage : [").append((transMessage != null) ? transMessage : "").append("]");
    }

    return sb.toString();
  }

  public String getInfo() {

    final StringBuffer info = new StringBuffer();
    if (responseStructure != null) {
      final String paymentId = responseStructure.getInstantPaymentId();
      final String valueDate = responseStructure.getValueDate();
      info.append("InstantPaymentId : [").append((paymentId != null) ? paymentId : "").append("] - ");
      info.append("ValueDate : [").append((valueDate != null) ? valueDate : "").append("]");
    }

    return info.toString();
  }

}
