/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.concentration;

import java.io.Serializable;

import com.calypso.tk.core.JDate;

public class ConcentrationReportItem implements Serializable {

	// START OA 28/11/2013
	// Oracle recommendation : declare a serialVersionUID for each Serializable class in order to avoid
	// InvalidClassExceptions.
	// Please refer to Serializable javadoc for more details
	private static final long serialVersionUID = -6826470218964287414L;

	// END OA OA 28/11/2013

	private JDate date;
	private String filterName;
	private String bookList;
	private double globalPosValue;
	private double limitedPosValue;
	private double percentage;
	private double calculatedPercentage;
	private String movementType;
	private String isin;
	private String criteria;

	public JDate getDate() {
		return this.date;
	}

	public void setDate(JDate date) {
		this.date = date;
	}

	public String getFilterName() {
		return this.filterName;
	}

	public void setFilterName(String filterName) {
		this.filterName = filterName;
	}

	public String getBookList() {
		return this.bookList;
	}

	public void setBookList(String bookList) {
		this.bookList = bookList;
	}

	public double getGlobalPosValue() {
		return this.globalPosValue;
	}

	public void setGlobalPosValue(double globalPosValue) {
		this.globalPosValue = globalPosValue;
	}

	public double getLimitedPosValue() {
		return this.limitedPosValue;
	}

	public void setLimitedPosValue(double limitedPosValue) {
		this.limitedPosValue = limitedPosValue;
	}

	public String getMovementType() {
		return this.movementType;
	}

	public void setMovementType(String movementType) {
		this.movementType = movementType;
	}

	public double getPercentage() {
		return this.percentage;
	}

	public void setPercentage(double percentage) {
		this.percentage = percentage;
	}

	public String getIsin() {
		return this.isin;
	}

	public void setIsin(String isin) {
		this.isin = isin;
	}

	public double getCalculatedPercentage() {
		return this.calculatedPercentage;
	}

	public void setCalculatedPercentage(double calculatedPercentage) {
		this.calculatedPercentage = calculatedPercentage;
	}

	public String getCriteria() {
		return this.criteria;
	}

	public void setCriteria(String criteria) {
		this.criteria = criteria;
	}

}
