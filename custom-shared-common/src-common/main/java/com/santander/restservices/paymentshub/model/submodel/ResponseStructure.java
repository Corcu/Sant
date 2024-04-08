package com.santander.restservices.paymentshub.model.submodel;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Provides information on the requested settlement time(s) of the payment instruction.
 */
public class ResponseStructure implements PaymentsHubSenderModel {

  private static final String VALUE_DATE = "Value Date";

  private static final String VALUE_DATE_PATTERN = "([12]\\d{3}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01]))";

  /** Instant payment identifier */
  @JsonProperty("instantPaymentId")
  private String instantPaymentId;

  /** Value date */
  @JsonProperty("valueDate")
  private String valueDate;

  /** Transformed Message - Not Apply */
  @JsonProperty("transformedMessage")
  private Object transformedMessage;

  public String getInstantPaymentId() {
    return instantPaymentId;
  }

  public void setInstantPaymentId(String instantPaymentId) {
    this.instantPaymentId = instantPaymentId;
  }

  public String getValueDate() {
    return valueDate;
  }

  public void setValueDate(String valueDate) {
    this.valueDate = valueDate;
  }

  public Object getTransformedMessage() {
    return transformedMessage;
  }

  public void setTransformedMessage(Object transformedMessage) {
    this.transformedMessage = transformedMessage;
  }

  @Override
  public boolean checkModelData() {

    boolean rst = true;

    // Value date
    rst &= PaymentsHubSenderModel.checkValue(VALUE_DATE, valueDate, VALUE_DATE_PATTERN);

    return rst;

  }

}
