/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.io.Serializable;

public class SantOptimumReportItemLight implements Serializable {
	private static final long serialVersionUID = 1L;

	private String contractName;
	private String contractBaseCcy;

	private Double categoryTotal = 0.0;
	private boolean eligible;
	private String optCategoryName;
	private String contractCategory;
	private int productId;
	private String currency;
	private String categoryType;

	private Double optimizationPrice;

	SantOptimumReportItemLight(SantOptimumReportItem item) {
		this.contractName = item.getCollateralConfig().getName();
		this.contractBaseCcy = item.getCollateralConfig().getCurrency();
		this.categoryTotal = item.getCategoryTotal();
		this.optCategoryName = item.getOptCategoryName();
		this.contractCategory = item.getContractCategory();
		this.productId = item.getProductId();
		this.currency = item.getAllocationCurrency();
		this.categoryType = item.getCategoryType();
		this.eligible = item.isEligible();
		this.optimizationPrice = item.getOptimizationPrice();
	}

	public String getCategoryType() {
		return this.categoryType;
	}

	public void setCategoryType(String categoryType) {
		this.categoryType = categoryType;
	}

	public boolean isEligible() {
		return this.eligible;
	}

	public void setEligible(boolean eligible) {
		this.eligible = eligible;
	}

	public String getOptCategoryName() {
		return this.optCategoryName;
	}

	public void setOptCategoryName(String optCategoryName) {
		this.optCategoryName = optCategoryName;
	}

	public String getContractCategory() {
		return this.contractCategory;
	}

	public void setContractCategory(String contractCategory) {
		this.contractCategory = contractCategory;
	}

	public int getProductId() {
		return this.productId;
	}

	public void setProductId(int productId) {
		this.productId = productId;
	}

	public Double getCategoryTotal() {
		if (this.categoryTotal == null) {
			return 0.0;
		} else {
			return this.categoryTotal;
		}
	}

	public void setCategoryTotal(Double categoryTotal) {
		this.categoryTotal = categoryTotal;
	}

	public String getCurrency() {
		return this.currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getEligibleString() {
		return this.eligible ? "Y" : "N";
	}

	public String getContractName() {
		return this.contractName;
	}

	public void setContractName(String contractName) {
		this.contractName = contractName;
	}

	public String getContractBaseCcy() {
		return this.contractBaseCcy;
	}

	public void setContractBaseCcy(String contractBaseCcy) {
		this.contractBaseCcy = contractBaseCcy;
	}

	public Double getOptimizationPrice() {
		return this.optimizationPrice;
	}

	public void setOptimizationPrice(Double optimizationPrice) {
		this.optimizationPrice = optimizationPrice;
	}

	public Double calcOptimizationPrice() {
		if ((getCategoryTotal() != null) && (this.optimizationPrice != null)) {
			return getCategoryTotal() * this.optimizationPrice;
		}
		return 0.0;
	}

}
