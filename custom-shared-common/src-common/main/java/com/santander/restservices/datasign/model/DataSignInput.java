package com.santander.restservices.datasign.model;

import com.calypso.tk.core.Util;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestModelRoot;

public class DataSignInput extends ApiRestModelRoot {

  /** Algorithm to hash the hd field. (MANDATORY) */
  @JsonProperty("alg")
  private String algorithm;

  /** Business data payload. (MANDATORY) */
  @JsonProperty("data")
  private String data;

  /** Indicates token expiration time (in seconds).(MANDATORY) */
  @JsonProperty("exp")
  private int expirationTime;

  /** Time before which the JWT must not be accepted for processing. (OPTIONAL) */
  @JsonProperty("nbf")
  private int rejectedTime;

  /** Unique transaction identifier. (MANDATORY) */
  @JsonProperty("nonce")
  private String uniqueTransactionId;

  /** Unique payment identifier (OPTIONAL) */
  @JsonProperty("paymentid")
  private String paymentId;

  /** By default it should always be JWT. (OPTIONAL) */
  @JsonProperty("typ")
  private String type;

  public DataSignInput() {
    super();
  }

  public DataSignInput(final DataSignInput input) {
    this();
    loadModelData(input);
  }

  public String getAlgorithm() {
    return algorithm;
  }

  public void setAlgorithm(String algorithm) {
    this.algorithm = algorithm;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  public int getExpirationTime() {
    return expirationTime;
  }

  public void setExpirationTime(int expirationTime) {
    this.expirationTime = expirationTime;
  }

  public int getRejectedTime() {
    return rejectedTime;
  }

  public void setRejectedTime(int rejectedTime) {
    this.rejectedTime = rejectedTime;
  }

  public String getUniqueTransactionId() {
    return uniqueTransactionId;
  }

  public void setUniqueTransactionId(String uniqueTransactionId) {
    this.uniqueTransactionId = uniqueTransactionId;
  }

  public String getPaymentId() {
    return paymentId;
  }

  public void setPaymentId(String paymentId) {
    this.paymentId = paymentId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public boolean checkModelDataLoaded() {
    return (!Util.isEmpty(data)) && (!Util.isEmpty(algorithm)) && (!Util.isEmpty(uniqueTransactionId))
        && expirationTime >= 0;
  }

  @Override
  public void loadModelData(final ApiRestModel model) {
    if (model != null && model instanceof DataSignInput) {
      final DataSignInput dataSignInput = (DataSignInput) model;
      setAlgorithm(dataSignInput.getAlgorithm());
      setData(dataSignInput.getData());
      setExpirationTime(dataSignInput.getExpirationTime());
      setRejectedTime(dataSignInput.getRejectedTime());
      setUniqueTransactionId(dataSignInput.getUniqueTransactionId());
      setPaymentId(dataSignInput.getPaymentId());
      setType(dataSignInput.getType());
    }
  }

  @Override
  public Class<DataSignInput> retriveModelClass() {
    return DataSignInput.class;
  }

}
