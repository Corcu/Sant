package com.santander.restservices.paymentshub.model.submodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PartyId implements PaymentsHubSenderModel {

  /** Code use to identify the external system in which the information will be obtain */
  @JsonProperty("code")
  private String code;

  /** Number use to identify the party */
  @JsonProperty("number")
  private String number;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getNumber() {
    return number;
  }

  public void setNumber(String number) {
    this.number = number;
  }

  @Override
  public boolean checkModelData() {
    return true;
  }

}
