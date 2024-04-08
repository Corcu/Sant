/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.audit;

import java.util.Properties;

public class RiskAuditProperties {

	private static Properties props = new Properties();

	static {
		props.put("_ratingValue", "RISK RATE -");
	}

	public static String getProperty(String key) {
		return props.getProperty(key);
	}

	public static boolean isAuditable(String key) {
		return props.getProperty(key) != null;
	}

}
