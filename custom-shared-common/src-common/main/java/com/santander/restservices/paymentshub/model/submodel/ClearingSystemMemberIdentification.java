package com.santander.restservices.paymentshub.model.submodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClearingSystemMemberIdentification implements PaymentsHubSenderModel {

  private static final String MEMBER_ID = "Member identification";
  private static final int MEMBERID_MAX_LENGTH = 28;

  /** Clearing system code */
  @JsonProperty("clrSysId")
  private ClearingSystemCode clrSysId;

  /** Member identification */
  @JsonProperty("mmbId")
  private String mmbId;

  @Override
  public boolean checkModelData() {
    boolean rst = true;

    // Clearing system code
    if (clrSysId != null) {
      rst &= clrSysId.checkModelData();
    }

    // Member identification
    rst &= PaymentsHubSenderModel.checkMaximumLength(MEMBER_ID, mmbId, MEMBERID_MAX_LENGTH);

    return rst;
  }

}
