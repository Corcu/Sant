/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.audit;

import java.util.Properties;

public class ContactAuditProperties {

	private static Properties props = new Properties();

	static {
		props.put("_firstName", "FIRSTNAME");
		props.put("_lastName", "LASTNAME");
		props.put("_cityName", "CITYNAME");
		props.put("_comment", "COMMENT");
		props.put("_phone", "PHONE");
		props.put("_contactType", "CONTACTTYPE");
		props.put("__addressCodes", "ADDRESSCODES");
		props.put("__user", "USER");
	}

	public static String getProperty(String key) {
		return props.getProperty(key);
	}

	public static boolean isAuditable(String key) {
		return props.getProperty(key) != null;
	}

}
