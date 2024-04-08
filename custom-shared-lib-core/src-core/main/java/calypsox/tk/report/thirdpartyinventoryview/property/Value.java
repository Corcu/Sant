/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.thirdpartyinventoryview.property;
import java.math.BigDecimal;

import com.calypso.tk.core.Util;

public class Value {

	private String sign;

	private String name;

	private BigDecimal value;

	private int days;

	public String getSign() {
		return this.sign;
	}

	public void setSign(String sign) {
		this.sign = sign;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BigDecimal getValue() {
		return this.value;
	}

	public String getName() {
		return this.name;
	}

	public void setValue(BigDecimal value) {
		this.value = value;
	}

	@Override
	public String toString() {
		String result = "";
		if (Util.isEmpty(this.name.toString()) || Util.isEmpty(this.value.toString()) || Util.isEmpty(this.sign)) {
			return result;
		}

		result += this.name;
		result += " ";
		result += this.sign;
		result += " ";
		result += this.value;
		result += " ";
		result += "in " + this.days + " days";

		return result;
	}

	public int getDays() {
		return this.days;
	}

	public void setDays(int days) {
		this.days = days;
	}
}