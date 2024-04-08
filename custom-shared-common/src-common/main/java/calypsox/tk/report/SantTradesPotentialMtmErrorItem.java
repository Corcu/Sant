/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.core.JDate;

import java.io.Serializable;

/**
 * This class stores the data needed for row of the Report "List of trades with Potential MTM Errors".
 */
public class SantTradesPotentialMtmErrorItem implements Serializable {

	// START OA 28/11/2013
	// Oracle recommendation : declare a serialVersionUID for each Serializable class in order to avoid
	// InvalidClassExceptions.
	// Please refer to Serializable javadoc for more details
	private static final long serialVersionUID = -4701093509786743049L;
	// END OA OA 28/11/2013

	private int trade_id;
	private int mccId;
	private JDate processDate;
	private String boRef;
	private String extRef;
	private String mccDesc;
	private JDate maturityDate;
	private double mtmPrevious;

	private String owner;
	private String structureId;
	private String productType;
	private String productSubType;

	public int getTrade_id() {
		return this.trade_id;
	}

	public void setTrade_id(int trade_id) {
		this.trade_id = trade_id;
	}

	public int getMccId() {
		return this.mccId;
	}

	public void setMccId(int mccId) {
		this.mccId = mccId;
	}

	public JDate getProcessDate() {
		return this.processDate;
	}

	public void setProcessDate(JDate processDate) {
		this.processDate = processDate;
	}

	public String getBoRef() {
		return this.boRef;
	}

	public void setBoRef(String boRef) {
		this.boRef = boRef;
	}

	public String getExtRef() {
		return this.extRef;
	}

	public void setExtRef(String extRef) {
		this.extRef = extRef;
	}

	public String getMccDesc() {
		return this.mccDesc;
	}

	public void setMccDesc(String mccDesc) {
		this.mccDesc = mccDesc;
	}

	public JDate getMaturityDate() {
		return this.maturityDate;
	}

	public void setMaturityDate(JDate maturityDate) {
		this.maturityDate = maturityDate;
	}

	public double getMtmPrevious() {
		return this.mtmPrevious;
	}

	public void setMtmPrevious(double mtm_previous) {
		this.mtmPrevious = mtm_previous;
	}

	public String getOwner() {
		return this.owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getStructureId() {
		return this.structureId;
	}

	public void setStructureId(String structureId) {
		this.structureId = structureId;
	}

	public String getProductType() {
		return this.productType;
	}

	public void setProductType(String productType) {
		this.productType = productType;
	}

	public String getProductSubType() {
		return this.productSubType;
	}

	public void setProductSubType(String productSubType) {
		this.productSubType = productSubType;
	}

}
