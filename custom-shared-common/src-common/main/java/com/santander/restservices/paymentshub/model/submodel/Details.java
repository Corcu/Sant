package com.santander.restservices.paymentshub.model.submodel;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Set of elements used to provide details on the regulatory reporting information. */

public class Details implements PaymentsHubSenderModel {

  /** Country of the entity that requires the regulatory reporting information. */
  @JsonProperty("ctry")
  private String ctry;

  /**
   * Specifies the nature, purpose, and reason for the transaction to be reported for regulatory and
   * statutory requirements in a coded form.
   */
  @JsonProperty("cd")
  private String cd;

  /** Additional details that cater for specific domestic regulatory requirements */
  @JsonProperty("inf")
  private String inf;

  public String getCtry() {
    return ctry;
  }

  public void setCtry(String ctry) {
    this.ctry = ctry;
  }

  public String getCd() {
    return cd;
  }

  public void setCd(String cd) {
    this.cd = cd;
  }

  public String getInf() {
    return inf;
  }

  public void setInf(String inf) {
    this.inf = inf;
  }

  @Override
  public boolean checkModelData() {
    return true;
  }

}
