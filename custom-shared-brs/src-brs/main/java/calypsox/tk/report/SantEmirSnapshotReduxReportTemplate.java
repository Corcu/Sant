/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import com.calypso.tk.report.TradeReportTemplate;

/**
 * 
 */
public class SantEmirSnapshotReduxReportTemplate extends
        TradeReportTemplate {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static final String REPORT_TYPES = "REPORT_TYPES";

    @Override
    public void setDefaults() {
        super.setDefaults();
        setColumns(SantEmirSnapshotReduxReportStyle.DEFAULTS_COLUMNS);
    }

}
