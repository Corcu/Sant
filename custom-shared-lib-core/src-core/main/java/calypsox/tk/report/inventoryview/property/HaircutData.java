/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.inventoryview.property;

import java.io.Serializable;

public class HaircutData implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String entity;

	private Double min;

	private Double max;

	public HaircutData(String entity) {
		this.entity = entity;
	}

	public String getEntity() {
		return this.entity;
	}

	public Double getMin() {
		return this.min;
	}

	public void setMin(Double min) {
		this.min = min;
	}

	public Double getMax() {
		return this.max;
	}

	public void setMax(Double max) {
		this.max = max;
	}

}
