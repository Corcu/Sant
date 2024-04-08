package com.santander.restservices.paymentshub.model.submodel;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Set of elements providing information specific to the credit transfer */
public class PaymentIdentification implements PaymentsHubSenderModel {

  private static final String UNIQUEID_PAYMENT_TRANSACTION = "Unique identifier of the payment transaction";
  private static final String END_TO_ENDID = "End To End Identification";
  private static final String TXID = "Transaction Identification";

  private static final int UNIQUE_INSTRID_MAX_LENGTH = 35;
  private static final int END_TO_END_MAX_LENGTH = 35;

  private static final int TRANSACTIONID_MIN_LENGTH = 1;
  private static final int TRANSACTIONID_MAX_LENGTH = 35;

  /** Unique identifier of the payment transaction */
  @JsonProperty("instrId")
  private String instrId;

  /** End To End Identification */
  @JsonProperty("endToEndId")
  private String endToEndId;

  /** Transaction Identification */
  @JsonProperty("txId")
  private String txId;

  /** Unique End-to-end Transaction Reference */
  @JsonProperty("uetr")
  private String uetr;

  public String getInstrId() {
    return instrId;
  }

  public void setInstrId(String instrId) {
    this.instrId = instrId;
  }

  public String getEndToEndId() {
    return endToEndId;
  }

  public void setEndToEndId(String endToEndId) {
    this.endToEndId = endToEndId;
  }

  public String getTxId() {
    return txId;
  }

  public void setTxId(String txId) {
    this.txId = txId;
  }

  public String getUetr() {
    return uetr;
  }

  public void setUetr(String uetr) {
    this.uetr = uetr;
  }

  @Override
  public boolean checkModelData() {
    // required:
    // - instrId

    if (Util.isEmpty(instrId)) {
      final String error = String.format("The field '%s' is mandatory.", UNIQUEID_PAYMENT_TRANSACTION);
      Log.error(this, error);
      errors.add(error);
      return false;
    }

    boolean rst = true;

    // Unique identifier of the payment transaction
    rst &= PaymentsHubSenderModel.checkMaximumLength(UNIQUEID_PAYMENT_TRANSACTION, instrId, UNIQUE_INSTRID_MAX_LENGTH);

    // End To End Identification
    rst &= PaymentsHubSenderModel.checkMaximumLength(END_TO_ENDID, endToEndId, END_TO_END_MAX_LENGTH);

    // Transaction Identification
    rst &= PaymentsHubSenderModel.checkValueLength(TXID, txId, TRANSACTIONID_MIN_LENGTH, TRANSACTIONID_MAX_LENGTH);

    return rst;

  }

}
