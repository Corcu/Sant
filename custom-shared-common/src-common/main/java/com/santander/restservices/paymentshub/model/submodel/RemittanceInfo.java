package com.santander.restservices.paymentshub.model.submodel;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Specifies the local instrument, as published in an external local instrument code list. Bank
 * Operation Code in ISO15022 format.
 */
public class RemittanceInfo implements PaymentsHubSenderModel {

  private static final String UNSTRD = "Unstructured";

  private static final int UNSTRD_SIZE = 4;

  /** Unstructured */
  @JsonProperty("ustrd")
  private List<String> ustrd;

  public List<String> getUstrd() {
    return ustrd;
  }

  public void setUstrd(List<String> ustrd) {
    this.ustrd = ustrd;
  }

  @Override
  public boolean checkModelData() {
    // Unstructured
    return PaymentsHubSenderModel.checkMaximumSize(UNSTRD, ustrd, UNSTRD_SIZE);
  }
}
