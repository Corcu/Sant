/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.util.bean;

import java.io.Serializable;

public class DiscountableProductBean implements Serializable {

	// START OA 27/11/2013
	// Oracle recommendation : declare a serialVersionUID for each Serializable class in order to avoid
	// InvalidClassExceptions.
	// Please refer to Serializable javadoc for more details
	private static final long serialVersionUID = 4202411876011903322L;
	// END OA OA 27/11/2013

	protected String productId;
	protected String date;
	protected String discountableECB;
	protected String haircutECB;

	protected String discountableFED;
	protected String haircutFED;

	protected String discountableBOE;
	protected String haircutBOE;

	protected String discountableSwiss;
	protected String haircutSwiss;

	protected String discountableEurex;
	protected String haircutEurex;

	protected String discontableMeff;
	protected String haircutMeff;

	// GSM: 03/09/2014: Added ccy for equity and type
	private String ccy;

	public enum TYPE {
		BOND, EQUITY;
	}

	private TYPE type;

	public DiscountableProductBean(String[] fields) {

		// GSM: 03/09/2014: first field will be the INTERNAL_REF for bonds, or EQ-ISIN-CCY for equities
		// this.productId = fields[0];
		this.productId = buildProductId(fields[0]);

		this.date = fields[1];

		if (fields.length >= 4) {
			this.discountableECB = fields[2];
			this.haircutECB = fields[3];
		}
		if (fields.length >= 6) {
			this.discountableFED = fields[4];
			this.haircutFED = fields[5];
		}
		if (fields.length >= 8) {
			this.discountableBOE = fields[6];
			this.haircutBOE = fields[7];
		}
		if (fields.length >= 10) {
			this.discountableSwiss = fields[8];
			this.haircutSwiss = fields[9];
		}

		if (fields.length >= 12) {
			this.discountableEurex = fields[10];
			this.haircutEurex = "-"+fields[11];
		}
		if (fields.length >= 14) {
			this.discontableMeff = fields[12];
			this.haircutMeff = fields[13];
		}

	}

	/**
	 * @param string
	 * @return first field will be the INTERNAL_REF for bonds, or EQ-ISIN-CCY for equities this.productId = fields[0];
	 */
	private String buildProductId(final String id) {

		String retId = id.trim();

		// GSM: 03/09/2014: Probably will have to change this part when AC defines the field
		// is an Equity
		if (id.trim().startsWith("EQ")) {
			String[] cachos = id.split("-");
			if (cachos.length == 3) { // ISIN+CCY
				retId = cachos[1].trim();
				setCcy(cachos[2].trim());
			} else {
				return null; // incorrect format
			}
			retId += getCcy();
			setType(TYPE.EQUITY);
			return retId;
		}

		// Otherwise, is a Bond
		setCcy("");
		setType(TYPE.BOND);
		return retId;
	}

	public String getProductId() {
		return this.productId;
	}

	/**
	 * @return the ccy
	 */
	public String getCcy() {
		return this.ccy;
	}

	/**
	 * @param ccy
	 *            the ccy to set
	 */
	public void setCcy(String ccy) {
		this.ccy = ccy;
	}

	/**
	 * @return the type
	 */
	public TYPE getType() {
		return this.type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(TYPE type) {
		this.type = type;
	}

	// GSM: 03/09/2014 END

	public void setInternalRef(String internalRef) {
		this.productId = internalRef;
	}

	public String getDate() {
		return this.date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getDiscountableECB() {
		return this.discountableECB;
	}

	public String isDiscountableECB() {
		if ("S".equals(this.discountableECB)) {
			return "true";
		}
		return "false";
	}

	public void setDiscountableECB(String discountableECB) {
		this.discountableECB = discountableECB;
	}

	public String getHaircutECB() {
		return this.haircutECB;
	}

	public void setHaircutECB(String haircutECB) {
		this.haircutECB = haircutECB;
	}

	public String getDiscountableFED() {
		return this.discountableFED;
	}

	public String isDiscountableFED() {
		if ("S".equals(this.discountableFED)) {
			return "true";
		}
		return "false";
	}

	public void setDiscountableFED(String discountableFED) {
		this.discountableFED = discountableFED;
	}

	public String getHaircutFED() {
		return this.haircutFED;
	}

	public void setHaircutFED(String haircutFED) {
		this.haircutFED = haircutFED;
	}

	public String getDiscountableBOE() {
		return this.discountableBOE;
	}

	public String isDiscountableBOE() {
		if ("S".equals(this.discountableBOE)) {
			return "true";
		}
		return "false";
	}

	public void setDiscountableBOE(String discountableBOE) {
		this.discountableBOE = discountableBOE;
	}

	public String getHaircutBOE() {
		return this.haircutBOE;
	}

	public void setHaircutBOE(String haircutBOE) {
		this.haircutBOE = haircutBOE;
	}

	public String getDiscountableSwiss() {
		return this.discountableSwiss;
	}

	public String isDiscountableSwiss() {
		if ("S".equals(this.discountableSwiss)) {
			return "true";
		}
		return "false";
	}

	public void setDiscountableSwiss(String discountableSwiss) {
		this.discountableSwiss = discountableSwiss;
	}

	public String getHaircutSwiss() {
		return this.haircutSwiss;
	}

	public void setHaircutSwiss(String haircutSwiss) {
		this.haircutSwiss = haircutSwiss;
	}

	public String getDiscountableEurex() {
		return this.discountableEurex;
	}

	public String isDiscountableEurex() {
		if ("S".equals(this.discountableEurex)) {
			return "true";
		}
		return "false";
	}

	public boolean isDiscountableByEurex() {
		return isDiscountableEurex().equals("true");
	}

	public void setDiscountableEurex(String discountableEurex) {
		this.discountableEurex = discountableEurex;
	}

	public String getHaircutEurex() {
		return this.haircutEurex;
	}

	public void setHaircutEurex(String haircutEurex) {
		this.haircutEurex = haircutEurex;
	}

	public String getDiscountableMeff() {
		return this.discontableMeff;
	}

	public String isDiscountableMeff() {
		if ("S".equals(this.discontableMeff)) {
			return "true";
		}
		return "false";
	}

	public boolean isDiscountableByMeff() {
		return isDiscountableMeff().equals("true");
	}

	public void setDiscountableMeff(String discontableMeff) {
		this.discontableMeff = discontableMeff;
	}

	public String getHaircutMeff() {
		return this.haircutMeff;
	}

	public void setHaircutMeff(String haircutMeff) {
		this.haircutMeff = haircutMeff;
	}

}
