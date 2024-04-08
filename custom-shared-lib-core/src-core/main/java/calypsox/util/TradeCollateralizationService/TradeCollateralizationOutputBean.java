/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.util.TradeCollateralizationService;
import static calypsox.util.TradeCollateralizationService.TradeCollateralizationConstants.EMPTY;
/**
 * Output Bean of the service Trade TradeCollateralizationServiceEngine.
 * 
 * @author Guillermo Solano
 * @version 1.1
 * @date 19/07/2013
 */
public class TradeCollateralizationOutputBean {
	private String isCollateralizedDeal;

	private String boExternalReference;
	private String boSystem;
	private String valueDate;
	private String collateralName;
	private String collateralType;
	private String productType;
	private String contractDirection;
	private String collateralEndDate;
	private String collateralStartDate;
	// For Phoenix
	private String foExternalReference;
	private String foSystem;
	private String contractId;
	private String contractName;
	private String isTriparty;
	private String tripartyAgent;
	private String collateralPortfolioCode;

	/**
	 * Puts in all variables an empty String
	 */
	public TradeCollateralizationOutputBean() {
		this.isCollateralizedDeal = EMPTY;
		this.foExternalReference = EMPTY;
		this.foSystem = EMPTY;
		this.boExternalReference = EMPTY;
		this.boSystem = EMPTY;
		this.valueDate = EMPTY;
		this.collateralName = EMPTY;
		this.collateralType = EMPTY;
		this.productType = EMPTY;
		this.contractDirection = EMPTY;
		this.collateralEndDate = EMPTY;
		this.collateralStartDate = EMPTY;
		this.isTriparty = EMPTY;
		this.tripartyAgent = EMPTY;
		this.collateralPortfolioCode = EMPTY;
	}



	/**
	 * @return the isCollateralizedDeal
	 */
	public String getIsCollateralizedDeal() {
		return this.isCollateralizedDeal;
	}

	/**
	 * @return the collateralStartDate
	 */
	public String getCollateralStartDate() {
		return this.collateralStartDate;
	}

	/**
	 * @param collateralStartDate
	 *            the collateralStartDate to set
	 */
	public void setCollateralStartDate(String collateralStartDate) {
		this.collateralStartDate = collateralStartDate;
	}

	/**
	 * @param isCollateralizedDeal
	 *            the isCollateralizedDeal to set
	 */
	public void setIsCollateralizedDeal(String isCollateralizedDeal) {
		this.isCollateralizedDeal = isCollateralizedDeal;
	}

	/**
	 * @param isCollateralizedDeal
	 *            the isCollateralizedDeal to set
	 */
	public void setIsCollateralizedDeal(Integer responseValue) {
		this.setIsCollateralizedDeal("" + responseValue);

	}

	/**
	 * @return the collateralName
	 */
	public String getCollateralName() {
		return this.collateralName;
	}

	/**
	 * @param collateralName
	 *            the collateralName to set
	 */
	public void setCollateralName(String collateralName) {
		this.collateralName = collateralName;
	}

	/**
	 * @return the collateralType
	 */
	public String getCollateralType() {
		return this.collateralType;
	}

	/**
	 * @param collateralType
	 *            the collateralType to set
	 */
	public void setCollateralType(String collateralType) {
		this.collateralType = collateralType;
	}

	/**
	 * @return the boExternalReference
	 */
	public String getBOExternalReference() {
		return this.boExternalReference;
	}

	/**
	 * @param externalReference
	 *            the externalReference to set
	 */
	public void setBOExternalReference(String externalReference) {
		this.boExternalReference = externalReference;
	}

	/**
	 * @return the productType
	 */
	public String getProductType() {
		return this.productType;
	}

	/**
	 * @param productType
	 *            the productType to set
	 */
	public void setProductType(String productType) {
		this.productType = productType;
	}

	/**
	 * @return the contractDirection
	 */
	public String getContractDirection() {
		return this.contractDirection;
	}

	/**
	 * @param contractDirection
	 *            the contractDirection to set
	 */
	public void setContractDirection(String contractDirection) {
		this.contractDirection = contractDirection;
	}

	/**
	 * @return the collateralEndDate
	 */
	public String getCollateralEndDate() {
		return this.collateralEndDate;
	}

	/**
	 * @param collateralEndDate
	 *            the collateralEndDate to set
	 */
	public void setCollateralEndDate(String collateralEndDate) {
		this.collateralEndDate = collateralEndDate;
	}

	/**
	 * @return the boSystem
	 */
	public String getBOSourceSystem() {
		return this.boSystem;
	}

	/**
	 * @param boSystem
	 *            the boSystem to set
	 */
	public void setBOSourceSystem(String boSystem) {
		this.boSystem = boSystem;
	}

	/**
	 * @return the valueDate
	 */
	public String getValueDate() {
		return this.valueDate;
	}

	/**
	 * @param valueDate
	 *            the valueDate to set
	 */
	public void setValueDate(String valueDate) {
		this.valueDate = valueDate;
	}

	public void setIsTriparty(String istriparty) {
		this.isTriparty = istriparty;
	}

	public void setTripartyAgent(String tripartyAgent) {
		this.tripartyAgent = tripartyAgent;
	}

	public void setCollateralPortfolioCode(String collateralPortfolioCode) {
		this.collateralPortfolioCode = collateralPortfolioCode;
	}
	
	public String getIsTriparty() {
		return isTriparty;
	}

	public String getTripartyAgent() {
		return tripartyAgent;
	}

	public String getCollateralPortfolioCode() {
		return collateralPortfolioCode;
	}



	public String getFOExternalReference() {
		return foExternalReference;
	} 



	public void setFOExternalReference(String foExternalReference) {
		this.foExternalReference = foExternalReference;
	}



	public String getFOSourceSystem() {
		return foSystem;
	}



	public void setFOSourceSystem(String foSystem) {
		this.foSystem = foSystem;
	}



	public String getContractId() {
		return contractId;
	}



	public void setContractId(String contractId) {
		this.contractId = contractId;
	}



	public String getContractName() {
		return contractName;
	}



	public void setContractName(String contractName) {
		this.contractName = contractName;
	}

}
