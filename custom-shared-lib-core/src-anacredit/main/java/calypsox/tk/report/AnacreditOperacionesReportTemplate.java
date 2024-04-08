package calypsox.tk.report;

import com.calypso.tk.core.Util;
import com.calypso.tk.report.MarginCallPositionEntryReportTemplate;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Arrays;
import java.util.List;

public class AnacreditOperacionesReportTemplate extends MarginCallPositionEntryReportTemplate {

    public static final String ROW_DATA = "AnacreditOperacionesItem";
    public static String DV_ANACREDIT_EXTRACTION_TYPES = "Anacredit.extractionTypes";
    public static String ANACREDIT_EXTRACTION_TYPE = "EXTRACTION_TYPE";
    public static String ANACREDIT_USE_IDS_FROM_PANEL = "USE_IDS_FROM_PANEL";

    public AnacreditOperacionesReportTemplate() {
        super();
        setDefaults();
    }

}
