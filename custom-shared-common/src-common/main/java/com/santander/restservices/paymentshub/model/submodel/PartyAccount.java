package com.santander.restservices.paymentshub.model.submodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PartyAccount implements PaymentsHubSenderModel {

  /** Id */
  @JsonProperty("id")
  private Account id;

  /** Other */
  @JsonProperty("othr")
  private Account othr;

  public Account getId() {
    return id;
  }

  public void setId(Account id) {
    this.id = id;
  }

  public Account getOthr() {
    return othr;
  }

  public void setOthr(Account othr) {
    this.othr = othr;
  }

  @Override
  public boolean checkModelData() {
    boolean rst = true;

    // Id
    if (id != null) {
      rst &= id.checkModelData();
    }

    // Other
    if (othr != null) {
      rst &= othr.checkModelData();
    }

    return rst;
  }

}
