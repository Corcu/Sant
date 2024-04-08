package com.santander.restservices.paymentshub.model.submodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InstructedInfo implements PaymentsHubSenderModel {

  /** Sender to Receiver Information */
  @JsonProperty("instrInf")
  private String instrInf;

  public String getInstrInf() {
    return instrInf;
  }

  public void setInstrInf(String instrInf) {
    this.instrInf = instrInf;
  }

  @Override
  public boolean checkModelData() {
    return true;
  }

}
