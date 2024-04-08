/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.inventoryview.property;

import java.io.Serializable;

import com.calypso.tk.core.Util;

public class HaircutMethod implements Serializable {

	// START OA 28/11/2013
	// Oracle recommendation : declare a serialVersionUID for each Serializable class in order to avoid
	// InvalidClassExceptions.
	// Please refer to Serializable javadoc for more details
	private static final long serialVersionUID = 8887247705319790058L;
	// END OA OA 28/11/2013

	private final String greaterThan = HaircutHelper.VALUES[0];
	private final String lowerThan = HaircutHelper.VALUES[1];

	private double minAmount = 0.0;
	private double maxAmount = 0.0;

	public Double getMinAmount() {
		return this.minAmount;
	}

	public void setMinAmount(double minAmount) {
		this.minAmount = Math.abs(minAmount);
	}

	public double getMaxAmount() {
		return this.maxAmount;
	}

	public void setMaxAmount(double maxAmount) {
		this.maxAmount = Math.abs(maxAmount);
	}

	@Override
	public String toString() {
		if ((this.minAmount == 0) && (this.maxAmount == 0)) {
			return "";
		}
		String result = "";
		if (this.minAmount > 0) {
			result = this.greaterThan;
			result += " ";
			result += Util.numberToString(this.minAmount);
			if (this.maxAmount > 0) {
				result += " and ";
			}
		}
		if (this.maxAmount > 0) {
			result += this.lowerThan;
			result += " ";
			result += Util.numberToString(this.maxAmount);
		}

		return result;
	}
}