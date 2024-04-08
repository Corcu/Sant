package com.santander.restservices.paymentshub.model.submodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaymentTypeInformation implements PaymentsHubSenderModel {

  private static final String INSTRUCTION_PRIORITY = "Instruction Priority";

  /** Instruction priority */
  @JsonProperty("instrPrty")
  private String instrPrty;

  /** LocalInstrument */
  @JsonProperty("lclInstrm")
  private LocalInstrument lclInstrm;

  /** CategoryPurpose */
  @JsonProperty("ctgyPurp")
  private CategoryPurpose ctgyPurp;

  /** ServiceLevel */
  @JsonProperty("svcLvl")
  private ServiceLevel svcLvl;

  public String getInstrPrty() {
    return instrPrty;
  }

  public void setInstrPrty(String instrPrty) {
    this.instrPrty = instrPrty;
  }

  public LocalInstrument getLclInstrm() {
    return lclInstrm;
  }

  public void setLclInstrm(LocalInstrument lclInstrm) {
    this.lclInstrm = lclInstrm;
  }

  public CategoryPurpose getCtgyPurp() {
    return ctgyPurp;
  }

  public void setCtgyPurp(CategoryPurpose ctgyPurp) {
    this.ctgyPurp = ctgyPurp;
  }

  public ServiceLevel getSvcLvl() {
    return svcLvl;
  }

  public void setSvcLvl(ServiceLevel svcLvl) {
    this.svcLvl = svcLvl;
  }

  @Override
  public boolean checkModelData() {
    boolean rst = true;

    // Instruction priority
    rst &= PaymentsHubSenderModel.checkValue(INSTRUCTION_PRIORITY, instrPrty);

    // LocalInstrument
    if (lclInstrm != null) {
      rst &= lclInstrm.checkModelData();
    }

    // CategoryPurpose
    if (ctgyPurp != null) {
      rst &= ctgyPurp.checkModelData();
    }

    // ServiceLevel
    if (svcLvl != null) {
      rst &= svcLvl.checkModelData();
    }

    return rst;
  }

}
