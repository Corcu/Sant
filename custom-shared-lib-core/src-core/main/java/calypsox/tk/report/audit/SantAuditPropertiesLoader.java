/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.audit;

import com.calypso.tk.core.AuditValue;

public class SantAuditPropertiesLoader {

	public boolean isRiskPropertyAuditable(AuditValue av) {
		return RiskAuditProperties.isAuditable(av.getFieldName());
	}

	public boolean isContactPropertyAuditable(AuditValue av) {
		return ContactAuditProperties.isAuditable(av.getFieldName());
	}

	public boolean isAgreementPropertyAuditable(AuditValue av) {
		return AgreementAuditProperties.isAuditable(av.getFieldName());
	}

	public String getRiskProperty(AuditValue av) {
		String fieldName = av.getFieldName();
		String riskPropValue = RiskAuditProperties.getProperty(av.getFieldName());
		if (fieldName.equals("_ratingValue")) {
			String[] splits = av.getEntityName().split("\\|");
			if (splits.length == 1) {
				splits = av.getEntityName().split("#");
			}
			if (splits.length >= 2) {
				return riskPropValue + " " + splits[2];
			}
			return riskPropValue + " " + av.getEntityName();
		}
		return riskPropValue;

	}

	public String getContactProperty(AuditValue av) {
		return ContactAuditProperties.getProperty(av.getFieldName());
	}

	public String getAgreementProperty(AuditValue av) {
		return AgreementAuditProperties.getProperty(av.getFieldName());
	}

}
