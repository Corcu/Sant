package com.santander.restservices.paymentshub.model.submodel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Provides information on the requested settlement time(s) of the payment instruction.
 */
public class CreditTransferInstruction implements PaymentsHubSenderModel {

  private static final String SETTLEMENT_PRIORITY = "Settlement Priority";
  private static final String INTERBANK_SETTL_DATE = "Interbank Settlement Date";
  private static final String CHARGE_TO_BEAR = "Charges To Bear";
  private static final String INSTRUCTION_INFO = "Instruction Info";
  private static final String REGULATORY_REPORTING = "Regulatory Reporting";

  private static final String INTERBANK_SETT_DATE_PATTERN = "([12]\\d{3}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01]))";

  private static final int INTERBANK_SETT_DATE_MIN_LENGTH = 0;
  private static final int INTERBANK_SETT_DATE_MAX_LENGTH = 10;

  private static final int INSTRUCTION_INFO_SIZE = 6;

  private static final int REGULATORY_REPORTING_MAX_SIZE = 10;

  /** PaymentIdentification */
  @JsonProperty("pmtId")
  private PaymentIdentification pmtId;

  /** Settlement Instruction */
  @JsonProperty("sttlmInf")
  private SettlementInstruction sttlmInf;

  /** Settlement Priority */
  @JsonProperty("sttlmPrty")
  private String sttlmPrty;

  /** Total Interbank Settlement Amount */
  @JsonProperty("ttlIntrBkSttlmAmt")
  private AmountCurrency ttlIntrBkSttlmAmt;

  /** Interbank Settlement Date */
  @JsonProperty("intrBkSttlmDt")
  private String intrBkSttlmDt;

  /** Interbank Settlement Amount */
  @JsonProperty("intrBkSttlmAmt")
  private AmountCurrency intrBkSttlmAmt;

  /** Instructed Amount */
  @JsonProperty("instdAmt")
  private AmountCurrency instdAmt;

  /** Exchange Rate */
  @JsonProperty("xchgRate")
  private Double xchgRate;

  /** This field specifies which party will bear the charges for the transaction. */
  @JsonProperty("chrgBr")
  private String chrgBr;

  /** Charges. */
  @JsonProperty("chrgsInf")
  private List<ChargesInformation> chrgsInf;

  /** Instruction Info */
  @JsonProperty("instrForNxtAgt")
  private List<InstructedInfo> instrForNxtAgt;

  /** Remittance Info */
  @JsonProperty("rmtInf")
  private RemittanceInfo rmtInf;

  /** Payment Type Information */
  @JsonProperty("pmtTpInf")
  private PaymentTypeInformation pmtTpInf;

  /** Purpose */
  @JsonProperty("purp")
  private Purpose purp;

  /** Instruction For Creditor Agent */
  @JsonProperty("instrForCdtrAgt")
  private List<InstructionForCreditorAgent> instrForCdtrAgt;

  /** Regulatory Reporting. */
  @JsonProperty("rgltryRptg")
  private List<RegulatoryReporting> rgltryRptg;

  /** SettlementTimeRequest */
  @JsonProperty("sttlmTmReq")
  private SettlementTimeRequest sttlmTmReq;

  /** InstructedAgent */
  @JsonProperty("instdAgt")
  private FinancialInstitution instdAgt;

  /** InstructingAgent */
  @JsonProperty("instgAgt")
  private FinancialInstitution instgAgt;

  /** Debtor */
  @JsonProperty("dbtr")
  private Institution dbtr;

  /** Debtor Account */
  @JsonProperty("dbtrAcct")
  private PartyAccount dbtrAcct;

  /** Debtor Agent */
  @JsonProperty("dbtrAgt")
  private FinancialInstitution dbtrAgt;

  /** Debtor Agent Account */
  @JsonProperty("dbtrAgtAcct")
  private PartyAccount dbtrAgtAcct;

  /** Creditor */
  @JsonProperty("cdtr")
  private Institution cdtr;

  /** Creditor Account */
  @JsonProperty("cdtrAcct")
  private PartyAccount cdtrAcct;

  /** Creditor Agent */
  @JsonProperty("cdtrAgt")
  private FinancialInstitution cdtrAgt;

  /** Creditor Agent Account */
  @JsonProperty("cdtrAgtAcct")
  private PartyAccount cdtrAgtAcct;

  /** Sender Correspondent Agent */
  @JsonProperty("sndrsCorrespdntAgt")
  private FinancialInstitution sndrsCorrespdntAgt;

  /** Sender Correspondent Agent Account */
  @JsonProperty("sndrsCorrespdntAgtAcct")
  private PartyAccount sndrsCorrespdntAgtAcct;

  /** Receiver Correspondent Agent */
  @JsonProperty("rcvrsCorrespdntAgt")
  private FinancialInstitution rcvrsCorrespdntAgt;

  /** Receiver Correspondent Agent Account */
  @JsonProperty("rcvrsCorrespdntAgtAcct")
  private PartyAccount rcvrsCorrespdntAgtAcct;

  /** Third Reimbursement Agent */
  @JsonProperty("thrdRmbrsmntAgt")
  private FinancialInstitution thrdRmbrsmntAgt;

  /** Third Reimbursement Agent Account */
  @JsonProperty("thrdRmbrsmntAgtAcct")
  private PartyAccount thrdRmbrsmntAgtAcct;

  /** Intermediary Agent1 */
  @JsonProperty("intrmyAgt")
  private FinancialInstitution intrmyAgt;

  /** Intermediary Agent1 Account */
  @JsonProperty("intrmyAgtAcct")
  private PartyAccount intrmyAgtAcct;

  /** Previous Instructing Agent1 */
  @JsonProperty("prvsInstgAgt1")
  private FinancialInstitution prvsInstgAgt1;

  /** Previous Instructing Agent2 */
  @JsonProperty("prvsInstgAgt2")
  private FinancialInstitution prvsInstgAgt2;

  /** Previous Instructing Agent3 */
  @JsonProperty("prvsInstgAgt3")
  private FinancialInstitution prvsInstgAgt3;

  /** Intermediary Agent2 */
  @JsonProperty("intrmyAgt2")
  private FinancialInstitution intrmyAgt2;

  /** Intermediary Agent2 Account */
  @JsonProperty("intrmyAgt2Acct")
  private PartyAccount intrmyAgt2Acct;

  /** Intermediary Agent3 */
  @JsonProperty("intrmyAgt3")
  private FinancialInstitution intrmyAgt3;

  /** Intermediary Agent3 Account */
  @JsonProperty("intrmyAgt3Acct")
  private PartyAccount intrmyAgt3Acct;

  /** Ultimate Debtor */
  @JsonProperty("ultmtDbtr")
  private Institution ultmtDbtr;

  /** Ultimate Creditor */
  @JsonProperty("ultmtCdtr")
  private Institution ultmtCdtr;

  /** Initiating Party */
  @JsonProperty("initgPty")
  private Institution initgPty;

  @JsonIgnore
  private String messageError;

  public PaymentIdentification getPmtId() {
    return pmtId;
  }

  public void setPmtId(PaymentIdentification pmtId) {
    this.pmtId = pmtId;
  }

  public SettlementInstruction getSttlmInf() {
    return sttlmInf;
  }

  public void setSttlmInf(SettlementInstruction sttlmInf) {
    this.sttlmInf = sttlmInf;
  }

  public String getSttlmPrty() {
    return sttlmPrty;
  }

  public void setSttlmPrty(String sttlmPrty) {
    this.sttlmPrty = sttlmPrty;
  }

  public AmountCurrency getTtlIntrBkSttlmAmt() {
    return ttlIntrBkSttlmAmt;
  }

  public void setTtlIntrBkSttlmAmt(AmountCurrency ttlIntrBkSttlmAmt) {
    this.ttlIntrBkSttlmAmt = ttlIntrBkSttlmAmt;
  }

  public String getIntrBkSttlmDt() {
    return intrBkSttlmDt;
  }

  public void setIntrBkSttlmDt(String intrBkSttlmDt) {
    this.intrBkSttlmDt = intrBkSttlmDt;
  }

  public AmountCurrency getIntrBkSttlmAmt() {
    return intrBkSttlmAmt;
  }

  public void setIntrBkSttlmAmt(AmountCurrency intrBkSttlmAmt) {
    this.intrBkSttlmAmt = intrBkSttlmAmt;
  }

  public AmountCurrency getInstdAmt() {
    return instdAmt;
  }

  public void setInstdAmt(AmountCurrency instdAmt) {
    this.instdAmt = instdAmt;
  }

  public Double getXchgRate() {
    return xchgRate;
  }

  public void setXchgRate(Double xchgRate) {
    this.xchgRate = xchgRate;
  }

  public String getChrgBr() {
    return chrgBr;
  }

  public void setChrgBr(String chrgBr) {
    this.chrgBr = chrgBr;
  }

  public List<ChargesInformation> getChrgsInf() {
    return chrgsInf;
  }

  public void setChrgsInf(List<ChargesInformation> chrgsInf) {
    this.chrgsInf = chrgsInf;
  }

  public List<InstructedInfo> getInstrForNxtAgt() {
    return instrForNxtAgt;
  }

  public void setInstrForNxtAgt(List<InstructedInfo> instrForNxtAgt) {
    this.instrForNxtAgt = instrForNxtAgt;
  }

  public RemittanceInfo getRmtInf() {
    return rmtInf;
  }

  public void setRmtInf(RemittanceInfo rmtInf) {
    this.rmtInf = rmtInf;
  }

  public PaymentTypeInformation getPmtTpInf() {
    return pmtTpInf;
  }

  public void setPmtTpInf(PaymentTypeInformation pmtTpInf) {
    this.pmtTpInf = pmtTpInf;
  }

  public Purpose getPurp() {
    return purp;
  }

  public void setPurp(Purpose purp) {
    this.purp = purp;
  }

  public List<InstructionForCreditorAgent> getInstrForCdtrAgt() {
    return instrForCdtrAgt;
  }

  public void setInstrForCdtrAgt(List<InstructionForCreditorAgent> instrForCdtrAgt) {
    this.instrForCdtrAgt = instrForCdtrAgt;
  }

  public List<RegulatoryReporting> getRgltryRptg() {
    return rgltryRptg;
  }

  public void setRgltryRptg(List<RegulatoryReporting> rgltryRptg) {
    this.rgltryRptg = rgltryRptg;
  }

  public SettlementTimeRequest getSttlmTmReq() {
    return sttlmTmReq;
  }

  public void setSttlmTmReq(SettlementTimeRequest sttlmTmReq) {
    this.sttlmTmReq = sttlmTmReq;
  }

  public FinancialInstitution getInstdAgt() {
    return instdAgt;
  }

  public void setInstdAgt(FinancialInstitution instdAgt) {
    this.instdAgt = instdAgt;
  }

  public FinancialInstitution getInstgAgt() {
    return instgAgt;
  }

  public void setInstgAgt(FinancialInstitution instgAgt) {
    this.instgAgt = instgAgt;
  }

  public Institution getDbtr() {
    return dbtr;
  }

  public void setDbtr(Institution dbtr) {
    this.dbtr = dbtr;
  }

  public PartyAccount getDbtrAcct() {
    return dbtrAcct;
  }

  public void setDbtrAcct(PartyAccount dbtrAcct) {
    this.dbtrAcct = dbtrAcct;
  }

  public FinancialInstitution getDbtrAgt() {
    return dbtrAgt;
  }

  public void setDbtrAgt(FinancialInstitution dbtrAgt) {
    this.dbtrAgt = dbtrAgt;
  }

  public PartyAccount getDbtrAgtAcct() {
    return dbtrAgtAcct;
  }

  public void setDbtrAgtAcct(PartyAccount dbtrAgtAcct) {
    this.dbtrAgtAcct = dbtrAgtAcct;
  }

  public Institution getCdtr() {
    return cdtr;
  }

  public void setCdtr(Institution cdtr) {
    this.cdtr = cdtr;
  }

  public PartyAccount getCdtrAcct() {
    return cdtrAcct;
  }

  public void setCdtrAcct(PartyAccount cdtrAcct) {
    this.cdtrAcct = cdtrAcct;
  }

  public FinancialInstitution getCdtrAgt() {
    return cdtrAgt;
  }

  public void setCdtrAgt(FinancialInstitution cdtrAgt) {
    this.cdtrAgt = cdtrAgt;
  }

  public PartyAccount getCdtrAgtAcct() {
    return cdtrAgtAcct;
  }

  public void setCdtrAgtAcct(PartyAccount cdtrAgtAcct) {
    this.cdtrAgtAcct = cdtrAgtAcct;
  }

  public FinancialInstitution getSndrsCorrespdntAgt() {
    return sndrsCorrespdntAgt;
  }

  public void setSndrsCorrespdntAgt(FinancialInstitution sndrsCorrespdntAgt) {
    this.sndrsCorrespdntAgt = sndrsCorrespdntAgt;
  }

  public PartyAccount getSndrsCorrespdntAgtAcct() {
    return sndrsCorrespdntAgtAcct;
  }

  public void setSndrsCorrespdntAgtAcct(PartyAccount sndrsCorrespdntAgtAcct) {
    this.sndrsCorrespdntAgtAcct = sndrsCorrespdntAgtAcct;
  }

  public FinancialInstitution getRcvrsCorrespdntAgt() {
    return rcvrsCorrespdntAgt;
  }

  public void setRcvrsCorrespdntAgt(FinancialInstitution rcvrsCorrespdntAgt) {
    this.rcvrsCorrespdntAgt = rcvrsCorrespdntAgt;
  }

  public PartyAccount getRcvrsCorrespdntAgtAcct() {
    return rcvrsCorrespdntAgtAcct;
  }

  public void setRcvrsCorrespdntAgtAcct(PartyAccount rcvrsCorrespdntAgtAcct) {
    this.rcvrsCorrespdntAgtAcct = rcvrsCorrespdntAgtAcct;
  }

  public FinancialInstitution getThrdRmbrsmntAgt() {
    return thrdRmbrsmntAgt;
  }

  public void setThrdRmbrsmntAgt(FinancialInstitution thrdRmbrsmntAgt) {
    this.thrdRmbrsmntAgt = thrdRmbrsmntAgt;
  }

  public PartyAccount getThrdRmbrsmntAgtAcct() {
    return thrdRmbrsmntAgtAcct;
  }

  public void setThrdRmbrsmntAgtAcct(PartyAccount thrdRmbrsmntAgtAcct) {
    this.thrdRmbrsmntAgtAcct = thrdRmbrsmntAgtAcct;
  }

  public FinancialInstitution getIntrmyAgt() {
    return intrmyAgt;
  }

  public void setIntrmyAgt(FinancialInstitution intrmyAgt) {
    this.intrmyAgt = intrmyAgt;
  }

  public PartyAccount getIntrmyAgtAcct() {
    return intrmyAgtAcct;
  }

  public void setIntrmyAgtAcct(PartyAccount intrmyAgtAcct) {
    this.intrmyAgtAcct = intrmyAgtAcct;
  }

  public FinancialInstitution getPrvsInstgAgt1() {
    return prvsInstgAgt1;
  }

  public void setPrvsInstgAgt1(FinancialInstitution prvsInstgAgt1) {
    this.prvsInstgAgt1 = prvsInstgAgt1;
  }

  public FinancialInstitution getPrvsInstgAgt2() {
    return prvsInstgAgt2;
  }

  public void setPrvsInstgAgt2(FinancialInstitution prvsInstgAgt2) {
    this.prvsInstgAgt2 = prvsInstgAgt2;
  }

  public FinancialInstitution getPrvsInstgAgt3() {
    return prvsInstgAgt3;
  }

  public void setPrvsInstgAgt3(FinancialInstitution prvsInstgAgt3) {
    this.prvsInstgAgt3 = prvsInstgAgt3;
  }

  public FinancialInstitution getIntrmyAgt2() {
    return intrmyAgt2;
  }

  public void setIntrmyAgt2(FinancialInstitution intrmyAgt2) {
    this.intrmyAgt2 = intrmyAgt2;
  }

  public PartyAccount getIntrmyAgt2Acct() {
    return intrmyAgt2Acct;
  }

  public void setIntrmyAgt2Acct(PartyAccount intrmyAgt2Acct) {
    this.intrmyAgt2Acct = intrmyAgt2Acct;
  }

  public FinancialInstitution getIntrmyAgt3() {
    return intrmyAgt3;
  }

  public void setIntrmyAgt3(FinancialInstitution intrmyAgt3) {
    this.intrmyAgt3 = intrmyAgt3;
  }

  public PartyAccount getIntrmyAgt3Acct() {
    return intrmyAgt3Acct;
  }

  public void setIntrmyAgt3Acct(PartyAccount intrmyAgt3Acct) {
    this.intrmyAgt3Acct = intrmyAgt3Acct;
  }

  public Institution getUltmtDbtr() {
    return ultmtDbtr;
  }

  public void setUltmtDbtr(Institution ultmtDbtr) {
    this.ultmtDbtr = ultmtDbtr;
  }

  public Institution getUltmtCdtr() {
    return ultmtCdtr;
  }

  public void setUltmtCdtr(Institution ultmtCdtr) {
    this.ultmtCdtr = ultmtCdtr;
  }

  public Institution getInitgPty() {
    return initgPty;
  }

  public void setInitgPty(Institution initgPty) {
    this.initgPty = initgPty;
  }

  public String getMessageError() {
    return messageError;
  }

  public void setMessageError(String messageError) {
    this.messageError = messageError;
  }

  @Override
  public boolean checkModelData() {
    boolean rst = true;

    // PaymentIdentification
    if (pmtId != null) {
      rst &= pmtId.checkModelData();
    }

    // Settlement Instruction
    if (sttlmInf != null) {
      rst &= sttlmInf.checkModelData();
    }

    // Settlement Priority
    rst &= PaymentsHubSenderModel.checkValue(SETTLEMENT_PRIORITY, sttlmPrty);

    // Total Interbank Settlement Amount
    if (ttlIntrBkSttlmAmt != null) {
      rst &= ttlIntrBkSttlmAmt.checkModelData();
    }

    // Interbank Settlement Date
    rst &= PaymentsHubSenderModel.checkValue(INTERBANK_SETTL_DATE, intrBkSttlmDt, INTERBANK_SETT_DATE_PATTERN);
    rst &= PaymentsHubSenderModel.checkValueLength(INTERBANK_SETTL_DATE, intrBkSttlmDt, INTERBANK_SETT_DATE_MIN_LENGTH,
        INTERBANK_SETT_DATE_MAX_LENGTH);

    // Interbank Settlement Amount
    if (intrBkSttlmAmt != null) {
      rst &= intrBkSttlmAmt.checkModelData();
    }

    // Instructed Amount
    if (instdAmt != null) {
      rst &= instdAmt.checkModelData();
    }

    // ChargesBear
    rst &= PaymentsHubSenderModel.checkValue(CHARGE_TO_BEAR, chrgBr);

    // InstructionInfo
    rst &= PaymentsHubSenderModel.checkMaximumSize(INSTRUCTION_INFO, instrForNxtAgt, INSTRUCTION_INFO_SIZE);

    // Remittance Info
    if (rmtInf != null) {
      rst &= rmtInf.checkModelData();
    }

    // Payment Type Information
    if (pmtTpInf != null) {
      rst &= pmtTpInf.checkModelData();
    }

    // Purpose
    if (purp != null) {
      rst &= purp.checkModelData();
    }

    // Regulatory Reporting
    rst &= PaymentsHubSenderModel.checkMaximumSize(REGULATORY_REPORTING, rgltryRptg, REGULATORY_REPORTING_MAX_SIZE);

    // SettlementTimeRequest
    if (sttlmTmReq != null) {
      rst &= sttlmTmReq.checkModelData();
    }

    // InstructedAgent
    if (instdAgt != null) {
      rst &= instdAgt.checkModelData();
    }

    // InstructingAgent
    if (instgAgt != null) {
      rst &= instgAgt.checkModelData();
    }

    // Debtor
    if (dbtr != null) {
      rst &= dbtr.checkModelData();
    }

    // Debtor Account
    if (dbtrAcct != null) {
      rst &= dbtrAcct.checkModelData();
    }

    // Debtor Agent
    if (dbtrAgt != null) {
      rst &= dbtrAgt.checkModelData();
    }

    // Debtor Agent Account
    if (dbtrAgtAcct != null) {
      rst &= dbtrAgtAcct.checkModelData();
    }

    // Creditor
    if (cdtr != null) {
      rst &= cdtr.checkModelData();
    }

    // Creditor Account
    if (cdtrAcct != null) {
      rst &= cdtrAcct.checkModelData();
    }

    // Creditor Agent
    if (cdtrAgt != null) {
      rst &= cdtrAgt.checkModelData();
    }

    // Creditor Agent Account
    if (cdtrAgtAcct != null) {
      rst &= cdtrAgtAcct.checkModelData();
    }

    // Sender Correspondent Agent
    if (sndrsCorrespdntAgt != null) {
      rst &= sndrsCorrespdntAgt.checkModelData();
    }

    // Sender Correspondent Agent Account
    if (sndrsCorrespdntAgtAcct != null) {
      rst &= sndrsCorrespdntAgtAcct.checkModelData();
    }

    // Receiver Correspondent Agent
    if (rcvrsCorrespdntAgt != null) {
      rst &= rcvrsCorrespdntAgt.checkModelData();
    }

    // Receiver Correspondent Agent Account
    if (rcvrsCorrespdntAgtAcct != null) {
      rst &= rcvrsCorrespdntAgtAcct.checkModelData();
    }

    // Third Reimbursement Agent
    if (thrdRmbrsmntAgt != null) {
      rst &= thrdRmbrsmntAgt.checkModelData();
    }

    // Third Reimbursement Agent Account
    if (thrdRmbrsmntAgtAcct != null) {
      rst &= thrdRmbrsmntAgtAcct.checkModelData();
    }

    // Intermediary Agent1
    if (intrmyAgt != null) {
      rst &= intrmyAgt.checkModelData();
    }

    // Intermediary Agent1 Account
    if (intrmyAgtAcct != null) {
      rst &= intrmyAgtAcct.checkModelData();
    }

    // Previous Instructing Agent1
    if (prvsInstgAgt1 != null) {
      rst &= prvsInstgAgt1.checkModelData();
    }

    // Previous Instructing Agent2
    if (prvsInstgAgt2 != null) {
      rst &= prvsInstgAgt2.checkModelData();
    }

    // Previous Instructing Agent3
    if (prvsInstgAgt3 != null) {
      rst &= prvsInstgAgt3.checkModelData();
    }

    // Intermediary Agent2
    if (intrmyAgt2 != null) {
      rst &= intrmyAgt2.checkModelData();
    }

    // Intermediary Agent2 Account
    if (intrmyAgt2Acct != null) {
      rst &= intrmyAgt2Acct.checkModelData();
    }

    // Intermediary Agent3
    if (intrmyAgt3 != null) {
      rst &= intrmyAgt3.checkModelData();
    }

    // Intermediary Agent3 Account
    if (intrmyAgt3Acct != null) {
      rst &= intrmyAgt3Acct.checkModelData();
    }

    // Ultimate Debtor
    if (ultmtDbtr != null) {
      rst &= ultmtDbtr.checkModelData();
    }

    // Ultimate Creditor
    if (ultmtCdtr != null) {
      rst &= ultmtCdtr.checkModelData();
    }

    // Initiating Party
    if (initgPty != null) {
      rst &= initgPty.checkModelData();
    }

    if (!rst) {
      setMessageError(PaymentsHubSenderModel.errorsToString());
    }

    return rst;
  }


}
