/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario"," S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.audit;

import java.util.Properties;

public class ExcludeFromAuditProperties {

	private static Properties props = new Properties();

	static {
		props.put("_CREATE_", "");
		props.put("_DELETE_", "");
		props.put("LEContact", "");
		props.put("_id", "");
		props.put("_legalEntityId", "");
		props.put("_legalEntityRole", "");
		props.put("_processingOrgId", "");
		props.put("_title", "");
		props.put("_state", "");
		props.put("_productTypeList", "");
		props.put("__expandedProductList", "");
		props.put("_effectiveFrom", "");
		props.put("_effectiveTo", "");
		props.put("_staticDataFilter", "");
		props.put("_externalRef", "");
		props.put("__gdIdentifiers", "");
		props.put("_enteredDatetime", "");
	}

	public static Properties get() {
		return props;
	}

}
