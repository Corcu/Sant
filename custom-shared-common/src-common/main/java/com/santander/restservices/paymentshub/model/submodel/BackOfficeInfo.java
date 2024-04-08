package com.santander.restservices.paymentshub.model.submodel;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

public class BackOfficeInfo implements PaymentsHubSenderModel {

  private static final String ENTITY_CODE = "Entity code";
  private static final String PROVIDER = "Provider";
  private static final String FOLDER = "Branch subcategory";
  private static final String CONTRACT = "Accounting contract";
  private static final String PRODUCT_TYPE = "Product type";
  private static final String PRODUCT_SUBTYPE = "Product SubType";
  private static final String PRODUCT_BUNDING = "Product Bunding";
  private static final String FRONT_REF = "Front reference";
  private static final String BACK_REF = "Back/Operation reference";
  private static final String BRANCH = "Branch";

  private static final int PROVIDER_MAX_LENGTH = 50;
  private static final int FOLDER_MAX_LENGTH = 50;
  private static final int CONTRACT_MAX_LENGTH = 20;
  private static final int PRODUCT_TYPE_MAX_LENGTH = 40;
  private static final int PRODUCT_SUBTYPE_MAX_LENGTH = 40;
  private static final int PRODUCT_BUNDING_MAX_LENGTH = 30;
  private static final int FRONT_REF_MAX_LENGTH = 20;
  private static final int BACK_REF_MAX_LENGTH = 20;
  private static final int BRANCH_LENGTH = 9;
  private static final String ENTITY_CODE_PATTERN = "\\d{4}";

  /** The application that send the message to Payments Hub */
  @JsonProperty("provider")
  private String provider;

  /** The entity code */
  @JsonProperty("entityCode")
  private String entityCode;

  /** Entitie's branch */
  @JsonProperty("branch")
  private String branch;

  /** Branch subcategory */
  @JsonProperty("folder")
  private String folder;

  /** Accounting contract */
  @JsonProperty("contract")
  private String contract;

  /** Resident = True. Not Resident = False */
  @JsonProperty("creditorResidence")
  private Boolean creditorResidence;

  /** Identify the operation netted and the boxes */
  @JsonProperty("creditorNetting")
  private String creditorNetting;

  /** Product Type */
  @JsonProperty("productType")
  private String productType;

  /** Product SubType */
  @JsonProperty("productSubType")
  private String productSubType;

  /** Product Bunding */
  @JsonProperty("productBunding")
  private String productBunding;

  /** Front reference */
  @JsonProperty("frontRef")
  private String frontRef;

  /** Back/Operation reference */
  @JsonProperty("backRef")
  private String backRef;

  /** Accounting */
  @JsonProperty("accounting")
  private Accounting accounting;

  /**
   * Delivery flag, this field specifies if the payment should generate a message or not - default
   * true
   */
  @JsonProperty("deliveryFlag")
  private boolean deliveryFlag = true;

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getEntityCode() {
    return entityCode;
  }

  public void setEntityCode(String entityCode) {
    this.entityCode = entityCode;
  }

  public String getBranch() {
    return branch;
  }

  public void setBranch(String branch) {
    this.branch = branch;
  }

  public String getFolder() {
    return folder;
  }

  public void setFolder(String folder) {
    this.folder = folder;
  }

  public String getContract() {
    return contract;
  }

  public void setContract(String contract) {
    this.contract = contract;
  }

  public Boolean isCreditorResidence() {
    return creditorResidence;
  }

  public void setCreditorResidence(Boolean creditorResidence) {
    this.creditorResidence = creditorResidence;
  }

  public String getCreditorNetting() {
    return creditorNetting;
  }

  public void setCreditorNetting(String creditorNetting) {
    this.creditorNetting = creditorNetting;
  }

  public String getProductType() {
    return productType;
  }

  public void setProductType(String productType) {
    this.productType = productType;
  }

  public String getProductSubType() {
    return productSubType;
  }

  public void setProductSubType(String productSubType) {
    this.productSubType = productSubType;
  }

  public String getProductBunding() {
    return productBunding;
  }

  public void setProductBunding(String productBunding) {
    this.productBunding = productBunding;
  }

  public String getFrontRef() {
    return frontRef;
  }

  public void setFrontRef(String frontRef) {
    this.frontRef = frontRef;
  }

  public String getBackRef() {
    return backRef;
  }

  public void setBackRef(String backRef) {
    this.backRef = backRef;
  }

  public Accounting getAccounting() {
    return accounting;
  }

  public void setAccounting(Accounting accounting) {
    this.accounting = accounting;
  }

  public boolean isDeliveryFlag() {
    return deliveryFlag;
  }

  @JsonSetter("deliveryFlag")
  public void setDeliveryFlag(Boolean deliveryFlag) {
    if (deliveryFlag != null) {
      this.deliveryFlag = deliveryFlag;
    }

  }

  @Override
  public boolean checkModelData() {

    boolean rst = true;

    // Provider
    rst &= PaymentsHubSenderModel.checkMaximumLength(PROVIDER, provider, PROVIDER_MAX_LENGTH);

    // Folder
    rst &= PaymentsHubSenderModel.checkMaximumLength(FOLDER, folder, FOLDER_MAX_LENGTH);

    // Contract
    rst &= PaymentsHubSenderModel.checkMaximumLength(CONTRACT, contract, CONTRACT_MAX_LENGTH);

    // Product Type
    rst &= PaymentsHubSenderModel.checkMaximumLength(PRODUCT_TYPE, productType, PRODUCT_TYPE_MAX_LENGTH);

    // Product SubType
    rst &= PaymentsHubSenderModel.checkMaximumLength(PRODUCT_SUBTYPE, productSubType, PRODUCT_SUBTYPE_MAX_LENGTH);

    // Product Bunding
    rst &= PaymentsHubSenderModel.checkMaximumLength(PRODUCT_BUNDING, productBunding, PRODUCT_BUNDING_MAX_LENGTH);

    // FrontRef
    rst &= PaymentsHubSenderModel.checkMaximumLength(FRONT_REF, frontRef, FRONT_REF_MAX_LENGTH);

    // BackRef
    rst &= PaymentsHubSenderModel.checkMaximumLength(BACK_REF, backRef, BACK_REF_MAX_LENGTH);

    // Branch
    rst &= checkBranchLength();

    // EntityCode
    rst &= PaymentsHubSenderModel.checkValue(ENTITY_CODE, entityCode, ENTITY_CODE_PATTERN);

    if (accounting != null) {
      rst &= accounting.checkModelData();
    }
    return rst;
  }

  /**
   * Check Branch Length
   * @return
   */
  private boolean checkBranchLength() {
    if (!Util.isEmpty(branch)) {
      if (branch.length() != BRANCH_LENGTH) {
        final String error = String
            .format(
                "The field '%s' is incorrect. The field value '%s' does not have the correct length: value length '%s'; Allowed length '%s'.",
                BRANCH, branch, String.valueOf(branch.length()), String.valueOf(BRANCH_LENGTH));
        Log.error(this, error);
        errors.add(error);
        return false;
      }
    }
    return true;
  }

}
