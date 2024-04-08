package com.santander.restservices.paymentshub.model.submodel;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GroupHeader implements PaymentsHubSenderModel {

  private static final String EVENT_TYPE = "EventType";
  private static final String CONCEPT_TYPE = "ConceptType";
  private static final String MESSAGE_TYPE = "MessageType";

  /** Difference between new message and other messages related (cancelations) */
  @JsonProperty("eventType")
  private String eventType;

  /** Identify the transaction as a Payment by "P" or Collector by "C" */
  @JsonProperty("conceptType")
  private String conceptType;

  /** The type of the message as it will be sent to the camera, mandatory if direction value is "P" */
  @JsonProperty("messageType")
  private String messageType;

  /** Number of individual transactions contained in the message */
  @JsonProperty("nbOfTxs")
  private int nbOfTxs;

  @JsonIgnore
  private String messageError;

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getConceptType() {
    return conceptType;
  }

  public void setConceptType(String conceptType) {
    this.conceptType = conceptType;
  }

  public String getMessageType() {
    return messageType;
  }

  public void setMessageType(String messageType) {
    this.messageType = messageType;
  }

  public int getNbOfTxs() {
    return nbOfTxs;
  }

  public void setNbOfTxs(int nbOfTxs) {
    this.nbOfTxs = nbOfTxs;
  }

  public String getMessageError() {
    return messageError;
  }

  public void setMessageError(String messageError) {
    this.messageError = messageError;
  }

  @Override
  public boolean checkModelData() {
    // required:
    // - conceptType
    // - eventType
    // - messageType

    String error = "";
    boolean rst = false;
    PaymentsHubSenderModel.clearErrors();

    if (Util.isEmpty(eventType) || Util.isEmpty(conceptType) || Util.isEmpty(messageType)) {
      error = "The fields of the GroupHeader block: EventType, ConceptType and MessageType are mandatory.";
      Log.error(this, error);
      setMessageError(error);
      return rst;
    }

    rst = PaymentsHubSenderModel.checkValue(EVENT_TYPE, eventType)
        && PaymentsHubSenderModel.checkValue(CONCEPT_TYPE, conceptType)
        && PaymentsHubSenderModel.checkValue(MESSAGE_TYPE, messageType);

    if (!rst) {
      setMessageError(PaymentsHubSenderModel.errorsToString());
    }

    return rst;

  }

}
