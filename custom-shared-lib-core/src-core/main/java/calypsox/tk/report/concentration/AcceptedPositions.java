/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.concentration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.refdata.StaticDataFilter;

public class AcceptedPositions implements Serializable {

	// START OA 28/11/2013
	// Oracle recommendation : declare a serialVersionUID for each Serializable class in order to avoid
	// InvalidClassExceptions.
	// Please refer to Serializable javadoc for more details
	private static final long serialVersionUID = 4347977955942874742L;
	// END OA OA 28/11/2013

	private StaticDataFilter filter;
	private final List<InventorySecurityPosition> positions = new ArrayList<InventorySecurityPosition>();
	private double limitedPosValue;
	private double percentage;
	private double totalIssued;
	private String posOrTotalIssued;
	private String criteria;

	public AcceptedPositions(StaticDataFilter filter, double percentage, String posOrTotalIssued, String criteria) {
		this.filter = filter;
		this.percentage = percentage;
		this.posOrTotalIssued = posOrTotalIssued;
		this.criteria = criteria;
	}

	public StaticDataFilter getFilter() {
		return this.filter;
	}

	public void setFilter(StaticDataFilter filter) {
		this.filter = filter;
	}

	public List<InventorySecurityPosition> getPositions() {
		return this.positions;
	}

	public void addPosition(InventorySecurityPosition position) {
		if (Util.isEmpty(getPositions())) {
			getPositions().add(position);
		} else {
			boolean found = false;
			for (InventorySecurityPosition secPos : getPositions()) {
				if ((secPos.getSecurityId() == position.getSecurityId())
						&& (secPos.getBookId() == position.getBookId())) {
					found = true;
					break;
				}
			}
			if (!found) {
				getPositions().add(position);
			}
		}
	}

	public double getLimitedPosValue() {
		return this.limitedPosValue;
	}

	public void setLimitedPosValue(double limitedPosValue) {
		this.limitedPosValue = limitedPosValue;
	}

	public void addLimitedPosValue(double amount) {
		this.limitedPosValue = getLimitedPosValue() + amount;
	}

	public double getPercentage() {
		return this.percentage;
	}

	public void setPercentage(double percentage) {
		this.percentage = percentage;
	}

	public double getTotalIssued() {
		return this.totalIssued;
	}

	public void setTotalIssued(double totalIssued) {
		this.totalIssued = totalIssued;
	}

	public void addTotalIssued(double amount) {
		this.totalIssued = getTotalIssued() + amount;
	}

	public String getPosOrTotalIssued() {
		return this.posOrTotalIssued;
	}

	public void setPosOrTotalIssued(String posOrTotalIssued) {
		this.posOrTotalIssued = posOrTotalIssued;
	}

	public String getCriteria() {
		return this.criteria;
	}

	public void setCriteria(String criteria) {
		this.criteria = criteria;
	}

}
