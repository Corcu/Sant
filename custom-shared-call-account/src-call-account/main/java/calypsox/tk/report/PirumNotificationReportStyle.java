package calypsox.tk.report;

import calypsox.tk.report.generic.loader.interestnotif.SantInterestNotificationEntry;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.report.ReportRow;

import java.security.InvalidParameterException;
import java.util.Vector;

public class PirumNotificationReportStyle extends SantInterestNotificationReportStyle{

    public static final String LE_FULL_NAME = "Full name Legal entity";
    public static final String LE_SHORT_NAME = "Short Name";
    public static final String CONTRACT_TYPE = "Contract type";
    public static final String CONTRACT_ID = "Contract id";

    public static final String[] ADDITIONAL_COLUMNS = {LE_FULL_NAME,LE_SHORT_NAME,CONTRACT_TYPE,CONTRACT_ID};

    @Override
    public String[] getDefaultColumns() {
        return SantInterestNotificationReportStyle.DEFAULTS_COLUMNS;
    }

    @Override
    public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors)
            throws InvalidParameterException {

        if ((row == null) || (row.getProperty("SantInterestNotificationEntry") == null)) {
            return null;
        }

        final SantInterestNotificationEntry entry = (SantInterestNotificationEntry) row
                .getProperty(SantInterestNotificationReportTemplate.ROW_DATA);

        if (LE_FULL_NAME.equals(columnName)) {
            LegalEntity counterparty = entry.getCounterparty();
            return null!=counterparty ? counterparty.getName() : "";
        }else if(LE_SHORT_NAME.equals(columnName)){
            LegalEntity counterparty = entry.getCounterparty();
            return null!=counterparty ? counterparty.getCode() : "";
        }else if(CONTRACT_TYPE.equals(columnName)){
            return entry.getContractType();
        }else if(CONTRACT_ID.equals(columnName)){
            return entry.getContractId();
        }else{
            return super.getColumnValue(row, columnName, errors);
        }
    }
}
