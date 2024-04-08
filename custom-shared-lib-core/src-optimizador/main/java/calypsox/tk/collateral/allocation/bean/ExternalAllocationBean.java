package calypsox.tk.collateral.allocation.bean;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.refdata.CollateralConfig;

public class ExternalAllocationBean implements ExternalAllocation {
	
	private String externalId;
	private String action;
	private String contractName;
	private String collateralDirection;
	private double assetAmount;
	private Date settlementDate;
	private String collateralBook;
	private String collateralType;
	private String underlyingType;
	private String assetCurrency;
	private Double contractValue;
	private Map<String, String> attributes;
	private Map<String, String> transientAttributes;
	private MarginCallEntry entry;
	private CollateralConfig collateralConfig;
	private String pdvMessageContent;
	private int allocationDirection;
	
	public String getPdvMessageContent() {
		return pdvMessageContent;
	}
	public void setPdvMessageContent(String pdvMessageContent) {
		this.pdvMessageContent = pdvMessageContent;
	}
	/**
	 * @return the contractName
	 */
	public String getContractName() {
		return contractName;
	}
	/**
	 * @param contractName the contractName to set
	 */
	public void setContractName(String contractName) {
		this.contractName = contractName;
	}
	/**
	 * @return the assetAmount
	 */
	public double getAssetAmount() {
		return assetAmount;
	}
	/**
	 * @param assetAmount the assetAmount to set
	 */
	public void setAssetAmount(double assetAmount) {
		this.assetAmount = assetAmount;
	}
	/**
	 * @return the settlementDate
	 */
	public Date getSettlementDate() {
		return settlementDate;
	}
	/**
	 * @param settlementDate the settlementDate to set
	 */
	public void setSettlementDate(Date settlementDate) {
		this.settlementDate = settlementDate;
	}
	/**
	 * @return the collateralBook
	 */
	public String getCollateralBook() {
		return collateralBook;
	}
	/**
	 * @param collateralBook the collateralBook to set
	 */
	public void setCollateralBook(String collateralBook) {
		this.collateralBook = collateralBook;
	}
	/**
	 * @return the collateralType
	 */
	public String getCollateralType() {
		return collateralType;
	}
	/**
	 * @param collateralType the collateralType to set
	 */
	public void setCollateralType(String collateralType) {
		this.collateralType = collateralType;
	}
	/**
	 * @return the underlyingType
	 */
	public String getUnderlyingType() {
		return underlyingType;
	}
	/**
	 * @param underlyingType the underlyingType to set
	 */
	public void setUnderlyingType(String underlyingType) {
		this.underlyingType = underlyingType;
	}
	/**
	 * @return the assetCurrency
	 */
	public String getAssetCurrency() {
		return assetCurrency;
	}
	/**
	 * @param assetCurrency the assetCurrency to set
	 */
	public void setAssetCurrency(String assetCurrency) {
		this.assetCurrency = assetCurrency;
	}
	/**
	 * @return the contractValue
	 */
	public Double getContractValue() {
		return contractValue;
	}
	/**
	 * @param contractValue the contractValue to set
	 */
	public void setContractValue(Double contractValue) {
		this.contractValue = contractValue;
	}
	/**
	 * @return the entry
	 */
	public MarginCallEntry getEntry() {
		return entry;
	}
	/**
	 * @param entry the entry to set
	 */
	public void setEntry(MarginCallEntry entry) {
		this.entry = entry;
	}
	/**
	 * @return the collateralConfig
	 */
	public CollateralConfig getCollateralConfig() {
		return collateralConfig;
	}
	/**
	 * @param collateralConfig the collateralConfig to set
	 */
	public void setCollateralConfig(CollateralConfig collateralConfig) {
		this.collateralConfig = collateralConfig;
	}
	/**
	 * @return the attributes
	 */
	public Map<String, String> getAttributes() {
		if(attributes == null) {
			attributes = new HashMap<String, String>();
		}
		return attributes;
	}
	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}
	/**
	 * @return the transientAttributes
	 */
	public Map<String, String> getTransientAttributes() {
		if(transientAttributes == null) {
			transientAttributes = new HashMap<String, String>();
		}
		return transientAttributes;
	}
	/**
	 * @param transientAttributes the transientAttributes to set
	 */
	public void setTransientAttributes(Map<String, String> transientAttributes) {
		this.transientAttributes = transientAttributes;
	}
	
	public void addAttribute(String name, String value) {
		getAttributes().put(name, value);
	}
	
	public void addTransientAttribute(String name, String value) {
		getTransientAttributes().put(name, value);
	}
	/**
	 * @return the action
	 */
	public String getAction() {
		return action;
	}
	/**
	 * @param action the action to set
	 */
	public void setAction(String action) {
		this.action = action;
	}
	/**
	 * @return the externalId
	 */
	public String getExternalId() {
		return externalId;
	}
	/**
	 * @param externalId the externalId to set
	 */
	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}
	/**
	 * @return the collateralDirection
	 */
	public String getCollateralDirection() {
		return collateralDirection;
	}
	/**
	 * @param collateralDirection the collateralDirection to set
	 */
	public void setCollateralDirection(String collateralDirection) {
		this.collateralDirection = collateralDirection;
	}
	/**
	 * @return the allocationDirection
	 */
	public int getAllocationDirection() {
		return allocationDirection;
	}
	/**
	 * @param allocationDirection the allocationDirection to set
	 */
	public void setAllocationDirection(int allocationDirection) {
		this.allocationDirection = allocationDirection;
	}
	
	
	/*
	private Double collateralCost;
	private Double assetRanking;
	private String fatherId;
	*/

}
