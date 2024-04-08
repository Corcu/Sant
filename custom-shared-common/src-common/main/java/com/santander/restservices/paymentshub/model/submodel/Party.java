package com.santander.restservices.paymentshub.model.submodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Party implements PaymentsHubSenderModel {

  private static final String BANK_IDENTIFIER_CODE = "Bank identifier code (Bicfi)";
  private static final String PARTY_NAME = "Party name";

  private static final String BICFI_PATTERN = "[A-Z0-9]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?";
  private static final int BICFI_MIN_LENGTH = 8;
  private static final int BICFI_MAX_LENGTH = 11;

  private static final int PARTY_NAME_MAX_LENGTH = 140;

  /** Bank identifier code */
  @JsonProperty("bicfi")
  private String bicfi;

  /** Party name */
  @JsonProperty("nm")
  private String nm;

  /** Clearing system Member Identification */
  @JsonProperty("clrSysMmbId")
  private ClearingSystemMemberIdentification clrSysMmbId;

  /** Party Id */
  @JsonProperty("partyId")
  private PartyId partyId;

  /** Address */
  @JsonProperty("pstlAdr")
  private Address pstlAdr;

  public String getBicfi() {
    return bicfi;
  }

  public void setBicfi(String bicfi) {
    this.bicfi = bicfi;
  }

  public String getNm() {
    return nm;
  }

  public void setNm(String nm) {
    this.nm = nm;
  }

  public ClearingSystemMemberIdentification getClrSysMmbId() {
    return clrSysMmbId;
  }

  public void setClrSysMmbId(ClearingSystemMemberIdentification clrSysMmbId) {
    this.clrSysMmbId = clrSysMmbId;
  }

  public PartyId getPartyId() {
    return partyId;
  }

  public void setPartyId(PartyId partyId) {
    this.partyId = partyId;
  }

  public Address getPstlAdr() {
    return pstlAdr;
  }

  public void setPstlAdr(Address pstlAdr) {
    this.pstlAdr = pstlAdr;
  }

  @Override
  public boolean checkModelData() {
    boolean rst = true;

    // Bicfi
    rst &= PaymentsHubSenderModel.checkValue(BANK_IDENTIFIER_CODE, bicfi, BICFI_PATTERN)
        && PaymentsHubSenderModel.checkValueLength(BANK_IDENTIFIER_CODE, bicfi, BICFI_MIN_LENGTH, BICFI_MAX_LENGTH);

    // Party Name
    rst &= PaymentsHubSenderModel.checkMaximumLength(PARTY_NAME, nm, PARTY_NAME_MAX_LENGTH);

    // ClearingSystemMemberIdentification
    if (clrSysMmbId != null) {
      rst &= clrSysMmbId.checkModelData();
    }

    // Party Id
    if (partyId != null) {
      rst &= partyId.checkModelData();
    }

    // Address
    if (pstlAdr != null) {
      rst &= pstlAdr.checkModelData();
    }

    return rst;
  }

}
