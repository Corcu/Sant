package com.santander.restservices.paymentshub.model.submodel;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Clearing System Identification */

public class ClearingSystemIdentification implements PaymentsHubSenderModel {

  private static final String CLEAR_SYSTEM_CODE = "Clearing System Code";

  /**
   * Clearing System Code. If the value is TGT the payment will be processed though TARGET. If it's
   * empty or it doesn't appear the payment will be processed through CORRESPONDENT
   */
  @JsonProperty("cd")
  private String cd;

  public String getCd() {
    return cd;
  }

  public void setCd(String cd) {
    this.cd = cd;
  }

  @Override
  public boolean checkModelData() {
    boolean rst = true;

    // Clearing System Code
    rst &= PaymentsHubSenderModel.checkValue(CLEAR_SYSTEM_CODE, cd);

    return rst;
  }

}
