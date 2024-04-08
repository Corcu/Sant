package com.santander.restservices.paymentshub.model.submodel;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Information needed due to regulatory and statutory requirements. */

public class RegulatoryReporting implements PaymentsHubSenderModel {

  /** Details */
  @JsonProperty("dtls")
  private Details dtls;

  public Details getDtls() {
    return dtls;
  }

  public void setDtls(Details dtls) {
    this.dtls = dtls;
  }

  @Override
  public boolean checkModelData() {
    boolean rst = true;

    // Details
    if (dtls != null) {
      rst &= dtls.checkModelData();
    }

    return rst;
  }

}
