package com.calypso.jaxb.xml;

import java.util.ArrayList;
import java.util.List;

import com.calypso.tk.publish.jaxb.CollateralAllocation;
import com.calypso.tk.publish.jaxb.CollateralAllocations;

public class CustomCollateralAllocations extends CollateralAllocations {
	private static final long serialVersionUID = 1L;

	public void setCollateralAllocation(List<CollateralAllocation> collateralAllocation) {
		if (this.collateralAllocation == null) {
			this.collateralAllocation = new ArrayList();
		}
		collateralAllocation.forEach(c -> {
			this.collateralAllocation.add(c);
		});
	}
}
