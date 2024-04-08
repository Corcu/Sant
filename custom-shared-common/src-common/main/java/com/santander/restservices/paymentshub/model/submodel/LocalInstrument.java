package com.santander.restservices.paymentshub.model.submodel;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Specifies the local instrument, as published in an external local instrument code list. Bank
 * Operation Code in ISO15022 format.
 */
public class LocalInstrument implements PaymentsHubSenderModel {

  private static final String LOCAL_INSTRUMENT_CODE = "Local instrument code";
  private static final String LOCAL_INSTRUMENT_PROPIETARY = "Local instrument propietary";

  private static final int LOCAL_INSTRUMENT_CODE_MIN_LENGTH = 1;
  private static final int LOCAL_INSTRUMENT_CODE_MAX_LENGTH = 35;

  private static final int LOCAL_INSTRUMENT_PROPIETARY_MIN_LENGTH = 1;
  private static final int LOCAL_INSTRUMENT_PROPIETARY_MAX_LENGTH = 35;

  /** Local instrument code */
  @JsonProperty("cd")
  private String cd;

  /** Local instrument propietary */
  @JsonProperty("prtry")
  private String prtry;

  public String getCd() {
    return cd;
  }

  public void setCd(String cd) {
    this.cd = cd;
  }

  public String getPrtry() {
    return prtry;
  }

  public void setPrtry(String prtry) {
    this.prtry = prtry;
  }

  @Override
  public boolean checkModelData() {
    boolean rst = true;

    // Local instrument code
    rst &= PaymentsHubSenderModel.checkValueLength(LOCAL_INSTRUMENT_CODE, cd, LOCAL_INSTRUMENT_CODE_MIN_LENGTH,
        LOCAL_INSTRUMENT_CODE_MAX_LENGTH);

    // Local instrument propietary
    rst &= PaymentsHubSenderModel.checkValueLength(LOCAL_INSTRUMENT_PROPIETARY, prtry,
        LOCAL_INSTRUMENT_PROPIETARY_MIN_LENGTH,
        LOCAL_INSTRUMENT_PROPIETARY_MAX_LENGTH);

    return rst;
  }
}
