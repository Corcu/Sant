package com.santander.restservices.paymentshub.model.submodel;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AmountCurrency implements PaymentsHubSenderModel {

  private static final String CURRENCY = "Currency";
  private static final int CCY_LENGTH = 3;

  /** Amount */
  @JsonProperty("value")
  private double value;

  /** Currency. Data format in ISO 4217 (alpha-3) is required */
  @JsonProperty("ccy")
  private String ccy;

  public double getValue() {
    return value;
  }

  public void setValue(double value) {
    this.value = value;
  }

  public String getCcy() {
    return ccy;
  }

  public void setCcy(String ccy) {
    this.ccy = ccy;
  }

  @Override
  public boolean checkModelData() {
    // required:
    // - value
    // - ccy

    if (Util.isEmpty(ccy)) {
      final String error = String.format("The field '%s' is mandatory.", CURRENCY);
      Log.error(this, error);
      errors.add(error);
      return false;
    }

    return PaymentsHubSenderModel.checkValueLength(CURRENCY, ccy, CCY_LENGTH, CCY_LENGTH);

  }
}
