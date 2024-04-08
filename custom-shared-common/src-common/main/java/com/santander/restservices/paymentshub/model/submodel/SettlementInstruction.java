package com.santander.restservices.paymentshub.model.submodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SettlementInstruction implements PaymentsHubSenderModel {

  @JsonProperty("clrSys")
  private ClearingSystemIdentification clrSys;

  public ClearingSystemIdentification getClrSys() {
    return clrSys;
  }

  public void setClrSys(ClearingSystemIdentification clrSys) {
    this.clrSys = clrSys;
  }

  @Override
  public boolean checkModelData() {
    if (clrSys != null) {
      return clrSys.checkModelData();
    }
    return true;
  }

}
