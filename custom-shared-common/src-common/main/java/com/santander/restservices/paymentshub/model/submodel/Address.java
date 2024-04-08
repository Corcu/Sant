package com.santander.restservices.paymentshub.model.submodel;

import com.calypso.tk.core.Util;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/** Information to identify the party's address */
public class Address implements PaymentsHubSenderModel {

  private static final String STREET_NAME = "Street Name";
  private static final String BUILDING_NUMBER = "Building Number";
  private static final String POSTCODE = "Postcode";
  private static final String TOWN_NAME = "Town name";
  private static final String COUNTRY = "Country";

  private static final int STREET_NAME_MAX_LENGTH = 70;
  private static final int BUILDING_NUMBER_MAX_LENGTH = 16;
  private static final int POSTCODE_MAX_LENGTH = 16;
  private static final int TOWN_NAME_MAX_LENGTH = 35;
  private static final int COUNTRY_LENGTH = 2;

  private static final int ADDRESS_LINE_MAX_LENGTH = 35;
  private static final int ADDRESS_MAX_SIZE = 7;

  /** Street name */
  @JsonProperty("strtNm")
  private String strtNm;

  /** Building Number */
  @JsonProperty("bldgNb")
  private String bldgNb;

  /** Postcode */
  @JsonProperty("pstCd")
  private String pstCd;

  /** Town name */
  @JsonProperty("twnNm")
  private String twnNm;

  /** Country - Length 2 */
  @JsonProperty("ctry")
  private String ctry;

  /** Address Line */
  @JsonProperty("adrLine")
  private List<String> adrLine;

  public String getStrtNm() {
    return strtNm;
  }

  public void setStrtNm(String strtNm) {
    this.strtNm = strtNm;
  }

  public String getBldgNb() {
    return bldgNb;
  }

  public void setBldgNb(String bldgNb) {
    this.bldgNb = bldgNb;
  }

  public String getPstCd() {
    return pstCd;
  }

  public void setPstCd(String pstCd) {
    this.pstCd = pstCd;
  }

  public String getTwnNm() {
    return twnNm;
  }

  public void setTwnNm(String twnNm) {
    this.twnNm = twnNm;
  }

  public String getCtry() {
    return ctry;
  }

  public void setCtry(String ctry) {
    this.ctry = ctry;
  }

  public List<String> getAdrLine() {
    return adrLine;
  }

  public void setAdrLine(List<String> adrLine) {
    this.adrLine = adrLine;
  }

  @Override
  public boolean checkModelData() {
    boolean rst = true;

    // Street Name
    rst &= PaymentsHubSenderModel.checkMaximumLength(STREET_NAME, strtNm, STREET_NAME_MAX_LENGTH);

    // Building Number
    rst &= PaymentsHubSenderModel.checkMaximumLength(BUILDING_NUMBER, bldgNb, BUILDING_NUMBER_MAX_LENGTH);

    // Postcode
    rst &= PaymentsHubSenderModel.checkMaximumLength(POSTCODE, pstCd, POSTCODE_MAX_LENGTH);

    // Town name
    rst &= PaymentsHubSenderModel.checkMaximumLength(TOWN_NAME, twnNm, TOWN_NAME_MAX_LENGTH);

    // Country
    rst &= PaymentsHubSenderModel.checkValueLength(COUNTRY, ctry, COUNTRY_LENGTH, COUNTRY_LENGTH);

    return rst;
  }

  /**
   * Build the correct Address Line
   *
   * @param address
   * @return
   */
  public static List<String> buildAddress(final String address) {

    final List<String> addressList = new ArrayList<String>();

    if (Util.isEmpty(address)) {
      return addressList;
    }

    String addressLine = address;
    final int maxAddressLength = ADDRESS_LINE_MAX_LENGTH * ADDRESS_MAX_SIZE;

    if (address.length() > maxAddressLength) {
      addressLine = address.substring(0, maxAddressLength);
    }

    while (addressLine.length() >= ADDRESS_LINE_MAX_LENGTH) {
      addressList.add(addressLine.substring(0, ADDRESS_LINE_MAX_LENGTH));
      addressLine = addressLine.substring(ADDRESS_LINE_MAX_LENGTH);
    }

    addressList.add(addressLine);

    return addressList;

  }

}
