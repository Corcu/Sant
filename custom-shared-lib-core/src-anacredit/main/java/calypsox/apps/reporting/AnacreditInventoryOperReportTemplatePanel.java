package calypsox.apps.reporting;

import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Arrays;
import java.util.List;

public class AnacreditInventoryOperReportTemplatePanel extends AnacreditInventoryGenericReportTemplatePanel {
    @Override
    protected List<String> getExtractionTypesDomain() {
        List<String> exTypes = LocalCache.getDomainValues(DSConnection.getDefault(), DV_ANACREDIT_EXTRACTION_TYPES);
        if (Util.isEmpty(exTypes)) {
            exTypes = Arrays.asList("", "SecuritiesRF", "SecuritiesRV");
        }
        return exTypes;
    }

}
