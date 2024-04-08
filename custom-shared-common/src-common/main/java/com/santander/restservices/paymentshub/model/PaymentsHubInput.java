package com.santander.restservices.paymentshub.model;

import com.calypso.tk.core.Util;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.santander.restservices.ApiRestModel;
import com.santander.restservices.ApiRestModelRoot;
import com.santander.restservices.paymentshub.model.submodel.CreditTransferInstruction;
import com.santander.restservices.paymentshub.model.submodel.GroupHeader;
import com.santander.restservices.paymentshub.model.submodel.SupplementaryData;

import java.util.List;

public class PaymentsHubInput extends ApiRestModelRoot {

  /** Group Header */
  @JsonProperty("grpHdr")
  private GroupHeader grpHdr;

  /** Credit Transfer Instruction */
  @JsonProperty("cdtTrfTxInf")
  private List<CreditTransferInstruction> cdtTrfTxInf;

  /** SupplementaryData */
  @JsonProperty("splmtryData")
  private SupplementaryData splmtryData;

  @JsonIgnore
  private String messageError;

  // Constructors
  public PaymentsHubInput() {
    super();
  }

  public PaymentsHubInput(final PaymentsHubInput input) {
    this();
    loadModelData(input);
  }

  // Setters and Getters
  public GroupHeader getGrpHdr() {
    return grpHdr;
  }

  public void setGrpHdr(GroupHeader grpHdr) {
    this.grpHdr = grpHdr;
  }

  public List<CreditTransferInstruction> getCdtTrfTxInf() {
    return cdtTrfTxInf;
  }

  public void setCdtTrfTxInf(List<CreditTransferInstruction> cdtTrfTxInf) {
    this.cdtTrfTxInf = cdtTrfTxInf;
  }

  public SupplementaryData getSplmtryData() {
    return splmtryData;
  }

  public void setSplmtryData(SupplementaryData splmtryData) {
    this.splmtryData = splmtryData;
  }

  public String getMessageError() {
    return messageError;
  }

  public void setMessageError(String messageError) {
    this.messageError = messageError;
  }

  @Override
  public boolean checkModelDataLoaded() {
    // required:
    // - grpHdr
    // - cdtTrfTxInf

    String msgError = "";
    boolean control = grpHdr != null && !Util.isEmpty(cdtTrfTxInf);

    if (control) {

      // GroupHeader
      if (!grpHdr.checkModelData()) {
        control = false;
        msgError = grpHdr.getMessageError();
      } else {

        // CreditTransferInstruction
        if (!cdtTrfTxInf.stream().allMatch(c -> c.checkModelData())) {
          control = false;
          final StringBuffer sb = new StringBuffer();
          cdtTrfTxInf.stream().forEach(c -> sb.append(c.getMessageError()));
          msgError = sb.toString();
        } else {

          // SupplementaryData
          if (splmtryData != null) {
            if (!splmtryData.checkModelData()) {
              control = false;
              msgError = splmtryData.getMessageError();
            }
          }
        }
      }

    } else {
      msgError = "The Blocks GroupHeader and CreditTransferInstruction are mandatory.";
    }

    // if error
    if (!Util.isEmpty(msgError)) {
      setMessageError(msgError);
    }

    return control;

  }

  @Override
  public void loadModelData(final ApiRestModel model) {
    if (model != null && model instanceof PaymentsHubInput) {
      final PaymentsHubInput data = (PaymentsHubInput) model;
      setGrpHdr(data.getGrpHdr());
      setCdtTrfTxInf(data.getCdtTrfTxInf());
      setSplmtryData(data.getSplmtryData());
    }
  }

  @Override
  public Class<PaymentsHubInput> retriveModelClass() {
    return PaymentsHubInput.class;
  }

}
