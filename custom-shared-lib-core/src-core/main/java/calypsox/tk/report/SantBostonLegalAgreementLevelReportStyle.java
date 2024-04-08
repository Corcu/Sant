package calypsox.tk.report;

import calypsox.util.collateral.SantCollateralConfigUtil;
import com.calypso.apps.cws.presentation.format.JDateFormat;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportRow;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Vector;

public class SantBostonLegalAgreementLevelReportStyle extends CollateralConfigReportStyle {
    private static final long serialVersionUID = 1L;
    private static final String PROCCES_DATE = "ProccesDate";
    private static final String MA_SIGN_DATE = "ADDITIONAL_FIELD.MA_SIGN_DATE";

    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) {

        String value = SantCollateralConfigUtil.overrideBookAndContractDirectionReportColumnValue(row, columnName, this);
        if (!Util.isEmpty(value)) {
            return value;
        }

        if (PROCCES_DATE.equalsIgnoreCase(columnName)) {
            JDatetime jTime = ReportRow.getValuationDateTime(row);
            return SantBostonUtil.getDate(jTime);
        } else if (MA_SIGN_DATE.equalsIgnoreCase(columnName)) {
            String date = "";
            CollateralConfig config = row.getProperty("MarginCallConfig");
            if (config != null) {
                try {
                    date = config.getAdditionalField("MA_SIGN_DATE");
                    JDate date1 = JDate.valueOf(date);
                    JDateFormat dateformat = new JDateFormat(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()));
                    date = dateformat.format(date1);
                } catch (Exception e) {
                    Log.error(this, "Cannot get Date from MA_SIGN_DATE, Additional Field = " + date);
                }
            }
            return date;
        } else {
            return super.getColumnValue(row, columnName, errors);
        }


    }

    @Override
    public TreeList getTreeList() {
        TreeList treeList = super.getTreeList();
        treeList.add(PROCCES_DATE);
        treeList.add(MA_SIGN_DATE);
        return treeList;
    }

}
