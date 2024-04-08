package com.santander.restservices.paymentshub.model.submodel;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Specifies the high level purpose of the instruction based on a set of pre-defined categories.
 * This is used by the initiating party to provide information concerning the processing of the
 * payment. It is likely to trigger special processing by any of the agents involved in the payment
 * chain.
 */
public class CategoryPurpose implements PaymentsHubSenderModel {

  private static final String CATEGORY_CODE = "Category purpose code";
  private static final String CATEGORY_PROPIETARY = "Category purpose propietary";

  private static final int CAT_PURPOSE_CODE_MIN_LENGTH = 1;
  private static final int CAT_PURPOSE_CODE_MAX_LENGTH = 4;

  private static final int CAT_PURPOSE_PROPIETARY_MIN_LENGTH = 1;
  private static final int CAT_PURPOSE_PROPIETARY_MAX_LENGTH = 35;

  /** Category purpose code */
  @JsonProperty("cd")
  private String cd;

  /** Category purpose propietary */
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

    // Category purpose code
    rst &= PaymentsHubSenderModel.checkValueLength(CATEGORY_CODE, cd, CAT_PURPOSE_CODE_MIN_LENGTH,
        CAT_PURPOSE_CODE_MAX_LENGTH);

    // Category purpose propietary
    rst &= PaymentsHubSenderModel.checkValueLength(CATEGORY_PROPIETARY, prtry, CAT_PURPOSE_PROPIETARY_MIN_LENGTH,
        CAT_PURPOSE_PROPIETARY_MAX_LENGTH);

    return rst;
  }

}
