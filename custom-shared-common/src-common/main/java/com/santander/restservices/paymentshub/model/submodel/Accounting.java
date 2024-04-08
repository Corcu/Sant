package com.santander.restservices.paymentshub.model.submodel;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Accounting implements PaymentsHubSenderModel {

  private static final String MIRROR_ACCOUNT = "Mirror Account";
  private static final String DESCRIPTION_1 = "Description GLE";
  private static final String DESCRIPTION_2 = "Porfolio + / + ExpenseCode";
  private static final String DESCRIPTION_3 = "Narrative 3";
  private static final String DESCRIPTION_4 = "ID Owner";
  private static final String ACCOUNTING_TYPE = "Accounting Type";
  private static final String NARRATIVES = "Narratives";

  private static final int MIRROR_ACCOUNT_MAX_LENGTH = 30;
  private static final int DESC_MAX_LENGTH = 35;
  public static final int NARRATIVES_LENGTH = 10;

  // Accounting Type
  public static final String ACCOUNTING_TYPE_DEFAULT = "transitory_to_nostro";

  /** mirrorAccount */
  @JsonProperty("mirrorAccount")
  private String mirrorAccount;

  /** Description GLE */
  @JsonProperty("desc1")
  private String desc1;

  /** Porfolio + / + ExpenseCode */
  @JsonProperty("desc2")
  private String desc2;

  /** Narrative 3 */
  @JsonProperty("desc3")
  private String desc3;

  /** ID Owner */
  @JsonProperty("desc4")
  private String desc4;

  /** Delivery flag, this field specifies if the payment should generate a message or not */
  @JsonProperty("accountingFlag")
  private Boolean accountingFlag = Boolean.TRUE;

  /** Flag netted payments */
  @JsonProperty("netted")
  private Boolean netted = Boolean.FALSE;

  /** Accounting to be done in PaymentHub */
  @JsonProperty("accountingType")
  private String accountingType = ACCOUNTING_TYPE_DEFAULT;

  /** Narratives - List Length 10 */
  @JsonProperty("narratives")
  private List<String> narratives;

  public String getMirrorAccount() {
    return mirrorAccount;
  }

  public void setMirrorAccount(String mirrorAccount) {
    this.mirrorAccount = mirrorAccount;
  }

  public String getDesc1() {
    return desc1;
  }

  public void setDesc1(String desc1) {
    this.desc1 = desc1;
  }

  public String getDesc2() {
    return desc2;
  }

  public void setDesc2(String desc2) {
    this.desc2 = desc2;
  }

  public String getDesc3() {
    return desc3;
  }

  public void setDesc3(String desc3) {
    this.desc3 = desc3;
  }

  public String getDesc4() {
    return desc4;
  }

  public void setDesc4(String desc4) {
    this.desc4 = desc4;
  }

  public Boolean getAccountingFlag() {
    return accountingFlag;
  }

  public void setAccountingFlag(Boolean accountingFlag) {
    if (accountingFlag != null) {
      this.accountingFlag = accountingFlag;
    }
  }

  public Boolean getNetted() {
    return netted;
  }

  public void setNetted(Boolean netted) {
    if (netted != null) {
      this.netted = netted;
    }
  }

  public String getAccountingType() {
    return accountingType;
  }

  public void setAccountingType(String accountingType) {
    if (accountingType != null) {
      this.accountingType = accountingType.toLowerCase();
    }
  }

  public List<String> getNarratives() {
    return narratives;
  }

  public void setNarratives(List<String> narratives) {
    this.narratives = narratives;
  }

  @Override
  public boolean checkModelData() {
    boolean rst = true;

    // Mirror Account
    rst &= PaymentsHubSenderModel.checkMaximumLength(MIRROR_ACCOUNT, mirrorAccount, MIRROR_ACCOUNT_MAX_LENGTH);

    // Description GLE
    rst &= PaymentsHubSenderModel.checkMaximumLength(DESCRIPTION_1, desc1, DESC_MAX_LENGTH);

    // Porfolio + / + ExpenseCode
    rst &= PaymentsHubSenderModel.checkMaximumLength(DESCRIPTION_2, desc2, DESC_MAX_LENGTH);

    // Narrative 3
    rst &= PaymentsHubSenderModel.checkMaximumLength(DESCRIPTION_3, desc3, DESC_MAX_LENGTH);

    // ID Owner
    rst &= PaymentsHubSenderModel.checkMaximumLength(DESCRIPTION_4, desc4, DESC_MAX_LENGTH);

    // Accounting Type
    rst &= PaymentsHubSenderModel.checkValue(ACCOUNTING_TYPE, accountingType);

    // Narratives
    rst &= PaymentsHubSenderModel.checkSize(NARRATIVES, narratives, NARRATIVES_LENGTH, NARRATIVES_LENGTH);

    return rst;
  }

}
