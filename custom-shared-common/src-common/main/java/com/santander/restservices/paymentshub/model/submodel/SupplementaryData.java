package com.santander.restservices.paymentshub.model.submodel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SupplementaryData implements PaymentsHubSenderModel {

  @JsonProperty("envlp")
  private Envelope envlp;

  public Envelope getEnvlp() {
    return envlp;
  }

  public void setEnvlp(Envelope envlp) {
    this.envlp = envlp;
  }

  @JsonIgnore
  private String messageError;

  public String getMessageError() {
    return messageError;
  }

  public void setMessageError(String messageError) {
    this.messageError = messageError;
  }

  @Override
  public boolean checkModelData() {

    boolean rst = true;
    PaymentsHubSenderModel.clearErrors();

    if (envlp != null) {
      rst = envlp.checkModelData();
    }

    if (!rst) {
      setMessageError(PaymentsHubSenderModel.errorsToString());
    }

    return rst;
  }

}
