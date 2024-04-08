/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.apps.inventory;

import com.calypso.infra.util.Util;

public class InventarioTercerosBean {

	private String secCodeType;
	private String secCode;
	private String secValueType; // Nominal or Qty
	private Double secValue;
	private String ccy;

	private int line;

	InventarioTercerosBean(String[] fields, int lineNumber) {
		this.secCodeType = fields[0].trim();
		this.secCode = fields[1].trim();
		this.secValueType = fields[2].trim();
		if (!Util.isEmpty(fields[3].trim())) {
			this.secValue = Double.valueOf(fields[3].trim());
		}
		this.ccy = fields[4].trim();

		this.line = lineNumber;
	}

	public String getSecCodeType() {
		return this.secCodeType;
	}

	public void setSecCodeType(String secCodeType) {
		this.secCodeType = secCodeType;
	}

	public String getSecCode() {
		return this.secCode;
	}

	public void setSecCode(String secCode) {
		this.secCode = secCode;
	}

	public String getSecValueType() {
		return this.secValueType;
	}

	public void setSecValueType(String secValueType) {
		this.secValueType = secValueType;
	}

	public boolean isSecValueTypeQty() {
		return "QTY".equals(getSecValueType()) ? true : false;
	}

	public Double getSecValue() {
		return this.secValue;
	}

	public void setSecValue(Double secValue) {
		this.secValue = secValue;
	}

	public String getCcy() {
		return this.ccy;
	}

	public void setCcy(String ccy) {
		this.ccy = ccy;
	}

	public void setLine(int line) {
		this.line = line;
	}

	public int getLine() {
		return this.line;
	}

}
