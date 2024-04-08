package com.santander.restservices.paymentshub.model.submodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FinancialInstitution implements PaymentsHubSenderModel {

  /** Financial Institution Identification */
  @JsonProperty("finInstnId")
  private Party finInstnId;

  public Party getFinInstnId() {
    return finInstnId;
  }

  public void setFinInstnId(Party finInstnId) {
    this.finInstnId = finInstnId;
  }

  @Override
  public boolean checkModelData() {
    boolean rst = true;

    // Financial Institution Identification
    if (finInstnId != null) {
      rst &= finInstnId.checkModelData();
    }

    return rst;
  }

}
