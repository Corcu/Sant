/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.optimization.impl.Category;

import java.io.Serializable;

public class SantOptimumReportItem implements Serializable {
	private static final long serialVersionUID = 1L;

	private CollateralConfig collateralConfig;
	private Double contractValue;
	private Double categoryTotal = 0.0;; // The above needs to be renamed to contractValue
	private boolean eligible;
	private String optCategoryName;
	private String contractCategory;
	private int productId;
	private String allocationCurrency;
	private String categoryType; // Cash or Security
	private Double optimizationPrice;

	public CollateralConfig getCollateralConfig() {
		return this.collateralConfig;
	}

	public void setCollateralConfig(CollateralConfig collateralConfig) {
		this.collateralConfig = collateralConfig;
	}

	public Double getContractValue() {
		return this.contractValue;
	}

	public void setContractValue(Double contractValue) {
		this.contractValue = contractValue;
	}

	public boolean isEligible() {
		return this.eligible;
	}

	public void setEligible(boolean eligible) {
		this.eligible = eligible;
	}

	public String getCategoryType() {
		return this.categoryType;
	}

	public void setCategoryType(String categoryType) {
		this.categoryType = categoryType;
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

	public static SantOptimumReportItem generateDummyEntry(final CollateralConfig mcConfig, Category category) {
		SantOptimumReportItem item = new SantOptimumReportItem();
		item.setCollateralConfig(mcConfig);

		item.setContractValue(0.0);
		item.setContractCategory(null);
		item.setOptCategoryName(category.getName());
		item.setCategoryType(category.getType());
		item.setOptimizationPrice(category.getWeight());
		return item;
	}

	public Double getCategoryTotal() {
		return this.categoryTotal;
	}

	public void setCategoryTotal(Double categoryTotal) {
		this.categoryTotal = categoryTotal;
	}

	public void addToCategoryTotal(Double value) {
		if (value != null) {
			this.categoryTotal += value;
		} else {
			// Nothing
		}
	}

	public String getAllocationCurrency() {
		return this.allocationCurrency;
	}

	public void setAllocationCurrency(String currency) {
		this.allocationCurrency = currency;
	}

	public String getEligibleString() {
		return this.eligible ? "Y" : "N";
	}

	public Double getOptimizationPrice() {
		return this.optimizationPrice;
	}

	public void setOptimizationPrice(Double optimizationPrice) {
		this.optimizationPrice = optimizationPrice;
	}

}
