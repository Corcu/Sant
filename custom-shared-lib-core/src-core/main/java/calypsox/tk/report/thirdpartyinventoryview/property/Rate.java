/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.thirdpartyinventoryview.property;

import java.math.BigDecimal;

import com.calypso.tk.core.Util;

public class Rate {

	private String sign;

	private BigDecimal rate;

	public String getSign() {
		return this.sign;
	}

	public void setSign(String sign) {
		this.sign = sign;
	}

	public BigDecimal getRate() {
		return this.rate;
	}

	public void setRate(BigDecimal rate) {
		this.rate = rate;
	}

	@Override
	public String toString() {
		String result = "";
		if (Util.isEmpty(this.rate.toString()) || Util.isEmpty(this.sign)) {
			return result;
		}

		result += this.sign;
		result += " ";
		result += this.rate;

		return result;
	}
}