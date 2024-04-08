/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.generic.columns;

import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;

public class SantValuationAgentColumn extends SantColumn {

	private final CollateralConfig marginCallConfig;

	public SantValuationAgentColumn(final CollateralConfig marginCallConfig) {
		this.marginCallConfig = marginCallConfig;
	}

	public Object get() {
		if (this.marginCallConfig == null) {
			return null;
		}
		final String valuationType = this.marginCallConfig.getValuationAgentType();
		if (Util.isEmpty(valuationType) || CollateralConfig.NONE.equals(valuationType)) {
			return null;
		}

		if (CollateralConfig.PARTY_A.equals(valuationType)) {
			return this.marginCallConfig.getProcessingOrg().getCode();
		}

		if (CollateralConfig.PARTY_B.equals(valuationType)) {
			return this.marginCallConfig.getLegalEntity().getCode();
		}

		if (CollateralConfig.BOTH.equals(valuationType)) {
			return new StringBuilder(this.marginCallConfig.getProcessingOrg().getCode()).append(" ")
					.append(this.marginCallConfig.getLegalEntity().getCode()).toString();
		}

		if (CollateralConfig.THIRD_PARTY.equals(valuationType)) {
			return getLegalEntity(this.marginCallConfig.getValuationAgentId());
		}

		return null;
	}

}
