package com.santander.restservices.paymentshub.model.submodel;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Provides information on the charges to be paid by the charge bearer(s) related to the payment
 * transaction
 */

public class ChargesInformation implements PaymentsHubSenderModel {

  @JsonProperty("amt")
  private AmountCurrency amt;

  @JsonProperty("agt")
  private FinancialInstitution agt;

  public AmountCurrency getAmt() {
    return amt;
  }

  public void setAmt(AmountCurrency amt) {
    this.amt = amt;
  }

  public FinancialInstitution getAgt() {
    return agt;
  }

  public void setAgt(FinancialInstitution agt) {
    this.agt = agt;
  }

  @Override
  public boolean checkModelData() {
    boolean rst = true;

    if (amt != null) {
      rst &= amt.checkModelData();
    }

    if (agt != null) {
      rst &= agt.checkModelData();
    }

    return rst;
  }

}
