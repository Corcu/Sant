package calypsox.tk.report;

import java.util.*;

import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;
import com.calypso.tk.util.DisplayInBrowser;

public class AnacreditOperacionesReport extends AnacreditAbstractReport {

    @Override
    protected List<ReportRow> extendReportRows(List<ReportRow> allRows, Vector<String> errors) {
        // For Operaciones nothing to do bacause the data is the list itself
        return allRows;
    }

}
