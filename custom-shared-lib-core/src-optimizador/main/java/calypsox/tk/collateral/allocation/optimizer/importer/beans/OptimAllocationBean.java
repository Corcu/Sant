/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.collateral.allocation.optimizer.importer.beans;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.calypso.infra.util.Util;
import com.calypso.tk.refdata.CollateralConfig;

public class OptimAllocationBean implements ExternalAllocation {

	private CollateralConfig mcc;

	private String collateralOwner;

	private String contractName;

	private String fatherId;

	private double assetAmount;

	private Date settlementDate;

	private String collateralBook;

	private String collateralType;

	private String underlyingType;

	private String assetOwner;

	private String assetISIN;

	private String assetCurrency;

	private Double assetPrice;

	private Double assetHaircut;

	private Double contractValue;

	private Double collateralCost;

	private Double assetRanking;

	private int rowNumber;

	private int nbCtrAllocs;

	private List<AllocImportErrorBean> errorsList = new ArrayList<AllocImportErrorBean>();

	/**
	 * @return the mcc
	 */
	public CollateralConfig getMcc() {
		return this.mcc;
	}

	/**
	 * @param mcc the mcc to set
	 */
	public void setMcc(CollateralConfig mcc) {
		this.mcc = mcc;
	}

	/**
	 * @return the poShortName
	 */
	public String getPoShortName() {
		return this.collateralOwner;
	}

	/**
	 * @param poShortName the poShortName to set
	 */
	public void setPoShortName(String poShortName) {
		this.collateralOwner = poShortName;
	}

	/**
	 * @return the ctrShortName
	 */
	public String getCtrShortName() {
		return this.contractName;
	}

	/**
	 * @param ctrShortName the ctrShortName to set
	 */
	public void setCtrShortName(String ctrShortName) {
		this.contractName = ctrShortName;
	}

	/**
	 * @return the fatherId
	 */
	public String getFatherId() {
		return this.fatherId;
	}

	/**
	 * @param fatherId the fatherId to set
	 */
	public void setFatherId(String fatherId) {
		this.fatherId = fatherId;
	}

	/**
	 * @return the nominal
	 */
	public double getNominal() {
		return this.assetAmount;
	}

	/**
	 * @param nominal the nominal to set
	 */
	public void setNominal(double nominal) {
		this.assetAmount = nominal;
	}

	/**
	 * @return the settlementDate
	 */
	public Date getSettlementDate() {
		return this.settlementDate;
	}

	/**
	 * @param settlementDate the settlementDate to set
	 */
	public void setSettlementDate(Date settlementDate) {
		this.settlementDate = settlementDate;
	}

	public boolean isCashAllocation() {
		return ("Cash".equals(getUnderlyingType()));
	}

	public boolean isSecurityAllocation() {
		return ("Security".equals(getUnderlyingType()));
	}

	/**
	 * @return the collateralOwner
	 */
	public String getCollateralOwner() {
		return this.collateralOwner;
	}

	/**
	 * @param collateralOwner the collateralOwner to set
	 */
	public void setCollateralOwner(String collateralOwner) {
		this.collateralOwner = collateralOwner;
	}

	/**
	 * @return the contractName
	 */
	public String getContractName() {
		return this.contractName;
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
		return this.assetAmount;
	}

	/**
	 * @param assetAmount the assetAmount to set
	 */
	public void setAssetAmount(double assetAmount) {
		this.assetAmount = assetAmount;
	}

	/**
	 * @return the collateralBook
	 */
	public String getCollateralBook() {
		return this.collateralBook;
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
		return this.collateralType;
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
		return this.underlyingType;
	}

	/**
	 * @param underlyingType the underlyingType to set
	 */
	public void setUnderlyingType(String underlyingType) {
		this.underlyingType = underlyingType;
	}

	/**
	 * @return the assetOwner
	 */
	public String getAssetOwner() {
		return this.assetOwner;
	}

	/**
	 * @param assetOwner the assetOwner to set
	 */
	public void setAssetOwner(String assetOwner) {
		this.assetOwner = assetOwner;
	}

	/**
	 * @return the assetISIN
	 */
	public String getAssetISIN() {
		return this.assetISIN;
	}

	/**
	 * @param assetISIN the assetISIN to set
	 */
	public void setAssetISIN(String assetISIN) {
		this.assetISIN = assetISIN;
	}

	/**
	 * @return the assetCurrency
	 */
	public String getAssetCurrency() {
		return this.assetCurrency;
	}

	/**
	 * @param assetCurrency the assetCurrency to set
	 */
	public void setAssetCurrency(String assetCurrency) {
		this.assetCurrency = assetCurrency;
	}

	/**
	 * @return the assetPrice
	 */
	public Double getAssetPrice() {
		return this.assetPrice;
	}

	/**
	 * @param assetPrice the assetPrice to set
	 */
	public void setAssetPrice(Double assetPrice) {
		this.assetPrice = assetPrice;
	}

	/**
	 * @return the assetHaircut
	 */
	public Double getAssetHaircut() {
		return this.assetHaircut;
	}

	/**
	 * @param assetHaircut the assetHaircut to set
	 */
	public void setAssetHaircut(Double assetHaircut) {
		this.assetHaircut = assetHaircut;
	}

	/**
	 * @return the contractValue
	 */
	public Double getContractValue() {
		return this.contractValue;
	}

	/**
	 * @param contractValue the contractValue to set
	 */
	public void setContractValue(Double contractValue) {
		this.contractValue = contractValue;
	}

	/**
	 * @return the rowNumber
	 */
	public int getRowNumber() {
		return this.rowNumber;
	}

	/**
	 * @param rowNumber the rowNumber to set
	 */
	public void setRowNumber(int rowNumber) {
		this.rowNumber = rowNumber;
	}

	/**
	 * @return the collateralCost
	 */
	public Double getCollateralCost() {
		return collateralCost;
	}

	/**
	 * @param collateralCost the collateralCost to set
	 */
	public void setCollateralCost(Double collateralCost) {
		this.collateralCost = collateralCost;
	}

	/**
	 * @return the assetRanking
	 */
	public Double getAssetRanking() {
		return assetRanking;
	}

	/**
	 * @param assetRanking the assetRanking to set
	 */
	public void setAssetRanking(Double assetRanking) {
		this.assetRanking = assetRanking;
	}

	/**
	 * @return the nbCtrAllocs
	 */
	public int getNbCtrAllocs() {
		return nbCtrAllocs;
	}

	/**
	 * @param nbCtrAllocs the nbCtrAllocs to set
	 */
	public void setNbCtrAllocs(int nbCtrAllocs) {
		this.nbCtrAllocs = nbCtrAllocs;
	}

	/**
	 * @return the key for the this allocation
	 */
	public String getKey() {
		StringBuffer sb = new StringBuffer("");
		//sb.append(nullIfEmpty(getContractName()));
		//sb.append("|");
		sb.append(nullIfEmpty(getUnderlyingType()));
		sb.append("|");
		if ("Cash".equals(getUnderlyingType())) {
			sb.append(nullIfEmpty(getAssetCurrency()));
		}
		else {
			sb.append(nullIfEmpty(getAssetISIN()));
		}
		 
		return sb.toString();
	}

	protected String nullIfEmpty(String string) {
		return (Util.isEmpty(string) ? "NULL" : string);
	}

	/**
	 * @return the errorsList
	 */
	public List<AllocImportErrorBean> getErrorsList() {
		return errorsList;
	}

	/**
	 * @param errorsList the errorsList to set
	 */
	public void setErrorsList(List<AllocImportErrorBean> errorsList) {
		this.errorsList = errorsList;
	}
}
