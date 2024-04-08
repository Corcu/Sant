package com.santander.restservices.paymentshub.model.submodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Institution implements PaymentsHubSenderModel {

  /** Institution Identification */
  @JsonProperty("instId")
  private Party instId;

  public Party getInstId() {
    return instId;
  }

  public void setInstId(Party instId) {
    this.instId = instId;
  }

  @Override
  public boolean checkModelData() {
    boolean rst = true;

    // Institution Identification
    if (instId != null) {
      rst &= instId.checkModelData();
    }

    return rst;
  }

}
