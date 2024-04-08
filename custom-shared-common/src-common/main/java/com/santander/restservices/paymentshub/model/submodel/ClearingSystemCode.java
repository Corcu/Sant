package com.santander.restservices.paymentshub.model.submodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClearingSystemCode implements PaymentsHubSenderModel {

  private static final String CLEARING_CODE = "Clearing system code";

  private static final int CODE_MIN_LENGTH = 1;
  private static final int CODE_MAX_LENGTH = 5;

  /** Clearing system code */
  @JsonProperty("cd")
  private String cd;

  @Override
  public boolean checkModelData() {
    boolean rst = true;

    // Clearing system code
    rst &= PaymentsHubSenderModel.checkValueLength(CLEARING_CODE, cd, CODE_MIN_LENGTH, CODE_MAX_LENGTH);

    return rst;
  }

}
