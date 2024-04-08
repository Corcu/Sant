package com.santander.restservices.paymentshub.model.submodel;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Agreement under which or rules under which the transaction should be processed.
 */
public class ServiceLevel implements PaymentsHubSenderModel {
  private static final String SERVICE_LEVEL_CODE = "Service level code";
  private static final String SERVICE_LEVEL_PROPIETARY = "Service level propietary";

  private static final int SERVICE_LEVEL_CODE_MIN_LENGTH = 1;
  private static final int SERVICE_LEVEL_CODE_MAX_LENGTH = 4;

  private static final int SERVICE_LEVEL_PROPIETARY_MIN_LENGTH = 1;
  private static final int SERVICE_LEVEL_PROPIETARY_MAX_LENGTH = 35;

  /** Service level code */
  @JsonProperty("cd")
  private String cd;

  /** Service level propietary */
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

    // Service level code
    rst &= PaymentsHubSenderModel.checkValueLength(SERVICE_LEVEL_CODE, cd, SERVICE_LEVEL_CODE_MIN_LENGTH,
        SERVICE_LEVEL_CODE_MAX_LENGTH);

    // Service level propietary
    rst &= PaymentsHubSenderModel.checkValueLength(SERVICE_LEVEL_PROPIETARY, prtry,
        SERVICE_LEVEL_PROPIETARY_MIN_LENGTH,
        SERVICE_LEVEL_PROPIETARY_MAX_LENGTH);

    return rst;
  }

}
