/*
 *
 * Copyright (c) 2011 Banco Santander
 * Author: Samuel Bartolome (samuel.bartolome@siag-management.com)
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import com.calypso.tk.report.ReportTemplate;

public class UserAuditReportTemplate extends ReportTemplate {

    private static final long serialVersionUID = -1767179612975677205L;
    public static final String USER_NAME = "USER_NAME";
    public static final String USER_GROUP = "USER_GROUP";

    @Override
    public void setDefaults() {
        super.setDefaults();
        setColumns(UserAuditReportStyle.DEFAULTS_COLUMNS);
    }
}
