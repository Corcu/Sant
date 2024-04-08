package com.santander.restservices.paymentshub.model.submodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Envelope implements PaymentsHubSenderModel {

  @JsonProperty("backOfficeInfo")
  private BackOfficeInfo backOfficeInfo;

  @JsonProperty("legacyInfo")
  private String legacyInfo;

  public BackOfficeInfo getBackOfficeInfo() {
    return backOfficeInfo;
  }

  public void setBackOfficeInfo(BackOfficeInfo backOfficeInfo) {
    this.backOfficeInfo = backOfficeInfo;
  }

  public String getLegacyInfo() {
    return legacyInfo;
  }

  public void setLegacyInfo(String legacyInfo) {
    this.legacyInfo = legacyInfo;
  }

  @Override
  public boolean checkModelData() {
    boolean rst = true;
    if (backOfficeInfo != null) {
      rst &= backOfficeInfo.checkModelData();
    }

    return rst;
  }

}
