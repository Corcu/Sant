/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.MarginCallAllocationFacade;
import com.calypso.tk.collateral.dto.SecurityAllocationDTO;
import com.calypso.tk.core.Product;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.optimization.impl.Category;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

/**
 * This class wraps an Allocation, just prepared to be shown in row data for the report. It has some logic and it's
 * prepare to show dummy Allocations lines (the ones that did not match an optimization category).
 * 
 * @author Guillermo Solano
 * 
 */
public class SantOptimumAllocationItem implements Comparable<SantOptimumAllocationItem> {

	// class constant
	public final static String EMPTY = "";
	public final static String HYPHEN = "-";

	// class variables
	private MarginCallAllocationFacade allocation;
	private CollateralConfig collateralConfig;
	private Double sumContractValues;
	private Boolean eligible;
	private String dummyContractName;
	private String optimizationCategory;
	private String mCContractCurrency;
	private String categoryType;

	/**
	 * @param row
	 *            recieved from a MarginCallAllocationReport row (MarginCallAllocationFacade)
	 * @param dsconnection
	 *            Dataserver pointer
	 */
	public SantOptimumAllocationItem(final MarginCallAllocationFacade allocation, DSConnection dsconnection) {
		this.allocation = allocation;
		this.collateralConfig = CacheCollateralClient.getCollateralConfig(dsconnection,
				this.allocation.getCollateralConfigId());
		// this.sumContractValues = this.allocation.getContractValue(); // initial contract value
		this.eligible = new Boolean(false);
		this.optimizationCategory = this.allocation.getOptimizationCategory();

	}

	/**
	 * @param row
	 *            MarginCallAllocationFacade row
	 */
	public SantOptimumAllocationItem(final MarginCallAllocationFacade allocation) {

		this(allocation, DSConnection.getDefault());
	}

	/**
	 * Dummy constructor
	 */
	public SantOptimumAllocationItem() {
	}

	/**
	 * @param dummyName
	 *            contractName of the dummy line
	 * @param category
	 *            optization
	 * @return generates a dummy row line
	 */
	public static SantOptimumAllocationItem generateDummyEntry(final CollateralConfig mcConfig, Category category) {

		SantOptimumAllocationItem temp = new SantOptimumAllocationItem();
		temp.sumContractValues = 0.0;
		temp.dummyContractName = mcConfig != null ? mcConfig.getName() : null; // item.getMarginCallContractName();
		temp.eligible = false;
		temp.allocation = null;
		temp.collateralConfig = mcConfig;
		temp.optimizationCategory = category.getName();
		temp.mCContractCurrency = mcConfig != null ? mcConfig.getCurrency() : null;// item.getMCBaseCurrency();
		temp.categoryType = category.getType();
		return temp;
	}

	/**
	 * @return Product if the allocation is a Security, null otherwise
	 */
	public Product getAllocationProduct() {
		if (this.allocation instanceof SecurityAllocationDTO) {
			return ((SecurityAllocationDTO) this.allocation).getProduct();
		}

		return null;
	}

	/**
	 * @return true if current allocation is a product
	 */
	public boolean isProduct() {
		return getAllocationProduct() != null;
	}

	/**
	 * @return true if current allocation is cash (when is not a Product)
	 */
	public boolean isCash() {
		return !isProduct();
	}

	public boolean isDummy() {
		return this.dummyContractName != EMPTY;
	}

	/**
	 * Override toString
	 */
	@Override
	// test purposes
	public String toString() {
		return toStringShort(true);
	}

	/**
	 * @param choose
	 *            true, short toString; false, more data
	 * @return toString input
	 */
	private String toStringShort(final boolean choose) {

		// to show dummy
		if (this.allocation == null) {
			return "CONTRACT_NAME: " + this.dummyContractName + " IS DUMMY LINE!" + " ;OPTIMIZATION: "
					+ getOptimizationCategory();
		}

		String text = "CONTRACT_NAME: " + getMarginCallContractName() + " ;CATEGORY_NAME: " + getCategoryName()
				+ " ;OPTIMIZATION: " + getOptimizationCategory();
		if (!choose) {
			text += ": " + " ;CATEGORY_TYPE: " + getCategoryType() + " ;BASE_CURRENCY: " + getMCBaseCurrency()
					+ " ;ELIGIBLE: " + this.eligible.toString();
		}
		return text;
	}

	/**
	 * Checks input String is not null or empty. If is the case, return EMPTY; otherwise input.
	 */
	private String checkIfEmptyOrNullString(final String input) {

		if (Util.isEmpty(input)) {
			return EMPTY;
		}

		return input.trim();
	}

	/**
	 * Override compareTo (Comparable interface).
	 * 
	 * @return true if the allocation is the same
	 */
	@Override
	public int compareTo(final SantOptimumAllocationItem o) {
		int r = 0;
		if (isDummy()) {
			return this.dummyContractName.compareTo(o.dummyContractName);
		}

		r = getMarginCallContractName().compareTo(o.getMarginCallContractName());

		if (r == 0) {
			r = getCategoryType().compareTo(o.getCategoryType());
		}
		if (r == 0) {
			r = getMCBaseCurrency().compareTo(o.getMCBaseCurrency());
		}
		// if (r == 0) {
		// r = getAllocation().getContractValue().compareTo(o.getAllocationValue());
		// }

		return r;
	}

	// GETTERS AND SETTERS
	// Margin Call Contract Name
	public String getMarginCallContractName() {

		String mcContractName = "";

		if (this.collateralConfig != null) {
			mcContractName = this.collateralConfig.getName();

		}

		return mcContractName;
	}

	// Category Name (SD Filter Name). Equity.ALL, EUR, USD
	public String getCategoryName() {

		if (this.allocation != null) {
			return checkIfEmptyOrNullString(this.allocation.getCategory());
		}
		return EMPTY;

	}

	// Category Type (Security / Cash)
	public String getCategoryType() {

		if (this.allocation != null) {
			return checkIfEmptyOrNullString(this.allocation.getUnderlyingType());
		} else if (this.categoryType != null) {
			return this.categoryType;
		}

		return HYPHEN;
	}

	// allocation sum
	// public Double getAllocationValue() {
	// return this.sumContractValues;
	// }

	// Margin Call Contract Base Currency
	public String getMCBaseCurrency() {

		if (this.collateralConfig != null) {
			return checkIfEmptyOrNullString(this.collateralConfig.getCurrency());

		} else if (this.mCContractCurrency != null) {
			return this.mCContractCurrency;
		}

		return HYPHEN;

	}

	// true if is eligible for the Category optimization
	public Boolean getEligible() {
		return this.eligible;
	}

	public String getEligibleString() {
		return this.eligible ? "Y" : "N";
	}

	// returns the optimization category
	public String getOptimizationCategory() {
		return this.optimizationCategory;
	}

	public void setOptimizationCategory(String optimizationCategory) {
		this.optimizationCategory = optimizationCategory;
	}

	// set the total for this allocation
	public void setTotalSumContractValues(Double sumAllocations) {
		this.sumContractValues = sumAllocations;

	}

	public Double getTotalSumContractValues() {
		return this.sumContractValues;
	}

	public void setEligible(boolean isEligible) {
		this.eligible = isEligible;
	}

	public CollateralConfig getCollateralConfig() {
		return this.collateralConfig;
	}

	public MarginCallAllocationFacade getAllocation() {
		return this.allocation;
	}

	public void setAllocation(MarginCallAllocationFacade allocation) {
		this.allocation = allocation;
	}

} // END SantOptimumEntry

