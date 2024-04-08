package com.santander.restservices.paymentshub.model.submodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Account implements PaymentsHubSenderModel {

  private static final String ACCOUNT_ID = "Account Id";
  private static final String ACCOUNT_IBAN = "Account IBAN";

  private static final int ACCOUNT_MAX_LENGTH = 34;

  /** Account */
  @JsonProperty("id")
  private String id;

  @JsonProperty("iban")
  private String iban;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getIban() {
    return iban;
  }

  public void setIban(String iban) {
    this.iban = iban;
  }

  @Override
  public boolean checkModelData() {
    boolean rst = true;

    if (id != null) {
      rst &= PaymentsHubSenderModel.checkMaximumLength(ACCOUNT_ID, id, ACCOUNT_MAX_LENGTH);
    }

    if (iban != null) {
      rst &= PaymentsHubSenderModel.checkMaximumLength(ACCOUNT_IBAN, iban, ACCOUNT_MAX_LENGTH);
    }

    return rst;
  }

}
