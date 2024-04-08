package com.santander.restservices.paymentshub.model.submodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InstructionForCreditorAgent implements PaymentsHubSenderModel {

  private static final String INSTRUCTION_CODE = "Instruction Code";
  private static final String INSTRUCTION_CODE_INFO = "Instruction Code Info";

  private static final int INSTRUCTION_CODE_INFO_MAX_LENGTH = 30;

  /** Instruction Code */
  @JsonProperty("cd")
  private String cd;

  /** Instruction Code Info */
  @JsonProperty("instrInf")
  private String instrInf;

  public String getCd() {
    return cd;
  }

  public void setCd(String cd) {
    this.cd = cd;
  }

  public String getInstrInf() {
    return instrInf;
  }

  public void setInstrInf(String instrInf) {
    this.instrInf = instrInf;
  }

  @Override
  public boolean checkModelData() {
    boolean rst = true;

    // Instruction Code
    rst &= PaymentsHubSenderModel.checkValue(INSTRUCTION_CODE, cd);

    // Instruction Code Info
    rst &= PaymentsHubSenderModel.checkMaximumLength(INSTRUCTION_CODE_INFO, instrInf, INSTRUCTION_CODE_INFO_MAX_LENGTH);

    return rst;
  }

}
