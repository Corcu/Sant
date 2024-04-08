package com.santander.restservices.paymentshub.model.submodel;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Underlying reason for the payment transaction. Purpose is used by the end-customers, that is
 * initiating party, (ultimate) debtor, (ultimate) creditor to provide information concerning the
 * nature of the payment. Purpose is a content element, which is not used for processing by any of
 * the agents involved in the payment chain.
 */
public class Purpose implements PaymentsHubSenderModel {

  private static final String PURPOSE_CODE = "Purpose code";
  private static final String PURPOSE_PROPIETARY = "Purpose propietary";

  private static final int PURPOSE_CODE_MIN_LENGTH = 1;
  private static final int PURPOSE_CODE_MAX_LENGTH = 4;

  private static final int PURPOSE_PROPIETARY_MIN_LENGTH = 1;
  private static final int PURPOSE_PROPIETARY_MAX_LENGTH = 35;

  /** Purpose code */
  @JsonProperty("cd")
  private String cd;

  /** Purpose propietary */
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

    // Purpose code
    rst &= PaymentsHubSenderModel.checkValueLength(PURPOSE_CODE, cd, PURPOSE_CODE_MIN_LENGTH, PURPOSE_CODE_MAX_LENGTH);

    // Purpose propietary
    rst &= PaymentsHubSenderModel.checkValueLength(PURPOSE_PROPIETARY, prtry, PURPOSE_PROPIETARY_MIN_LENGTH,
        PURPOSE_PROPIETARY_MAX_LENGTH);

    return rst;
  }

}
