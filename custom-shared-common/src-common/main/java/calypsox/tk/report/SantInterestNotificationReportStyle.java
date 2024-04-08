/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import calypsox.tk.report.generic.loader.interestnotif.SantInterestNotificationEntry;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.Rate;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.security.InvalidParameterException;
import java.util.Vector;

public class SantInterestNotificationReportStyle extends ReportStyle {

    private static final long serialVersionUID = 6426246892458625976L;

    public static final String TRADE_ID = "Trade Id";
    public static final String CALL_ACCOUNT = "Call Account";
    public static final String ADHOC_PAYMENT = "AdHoc Payment";
    public static final String DATE = "Date";
    public static final String MOVEMENT = "Movement";
    public static final String CURRENCY = "Currency";
    public static final String PRINCIPAL = "Principal";
    public static final String RATE = "Rate";
    public static final String SPREAD = "Spread";
    public static final String ADJUSTED_RATE = "Adjusted Rate";
    public static final String DAILY_ACCRUAL = "Daily Accrual";
    public static final String TOTAL_ACCRUAL = "Total Accrual";
    public static final String CONTRACT_NAME = "Contract Name";
    public static final String INDEX = "Index";
    public static final String PO_OWNER = "PO Owner";
    public static final String WATCH_INTEREST = "Watch Interest";
    public static final String MOVEMENT_TYPE = "Movement Type";

    public static final String CONTRACT_TYPE_CSD = "CSD";
    public static final String PO_SOVEREIGN = "SBWO";
    public static final String IM = "IM";
    public static final String VM = "VM";
    public static final String IM_CSD_TYPE = "IM_CSD_TYPE";

    public static final String[] DEFAULTS_COLUMNS = {CALL_ACCOUNT, ADHOC_PAYMENT, CONTRACT_NAME, PO_OWNER, DATE,
            TRADE_ID, MOVEMENT, CURRENCY, PRINCIPAL, INDEX, RATE, SPREAD, ADJUSTED_RATE, DAILY_ACCRUAL, TOTAL_ACCRUAL,
            WATCH_INTEREST, MOVEMENT_TYPE};

    @SuppressWarnings("rawtypes")
    @Override
    public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors)
            throws InvalidParameterException {

        if ((row == null) || (row.getProperty("SantInterestNotificationEntry") == null)) {
            return null;
        }

        final SantInterestNotificationEntry entry = (SantInterestNotificationEntry) row
                .getProperty(SantInterestNotificationReportTemplate.ROW_DATA);

        if (CALL_ACCOUNT.equals(columnName)) {
            return entry.getCallAccountName();
        } else if (ADHOC_PAYMENT.equals(columnName)) {
            return entry.getAdHoc();
        } else if (CONTRACT_NAME.equals(columnName)) {
            return entry.getContractName();
        } else if (PO_OWNER.equals(columnName)) {
            return entry.getPoName();
        } else if (DATE.equals(columnName)) {
            return entry.getDate();
        } else if (MOVEMENT.equals(columnName)) {
            return entry.getMovement();
        } else if (CURRENCY.equals(columnName)) {
            return entry.getCurrency();
        } else if (PRINCIPAL.equals(columnName)) {
            return new Amount(entry.getPrincipal(), 2);
        } else if (INDEX.equals(columnName)) {
            return entry.getIndexName();
        } else if (RATE.equals(columnName)) {
            return new Rate(entry.getRate());
        } else if (SPREAD.equals(columnName)) {
            return new Rate(entry.getSpread());
        } else if (ADJUSTED_RATE.equals(columnName)) {
            return new Rate(entry.getAdjustedRate());
        } else if (DAILY_ACCRUAL.equals(columnName)) {
            return new Amount(entry.getDailyAccrual(), 2);
        } else if (TOTAL_ACCRUAL.equals(columnName)) {
            return new Amount(entry.getTotalAccrual(), 2);
        } else if (TRADE_ID.equals(columnName)) {
            return entry.getTradeId();
        } else if (WATCH_INTEREST.equals(columnName)) {
            return entry.getWatchInterest();
        } else if (MOVEMENT_TYPE.equals(columnName)) {
            return getMovementType(entry.getContractId());
        }
        return null;
    }

    private String getMovementType(int contractid) {
        CollateralConfig config = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), contractid);
        if (config != null) {
            if (CONTRACT_TYPE_CSD.equals(config.getContractType())) {
                if (PO_SOVEREIGN.equals(config.getProcessingOrg().getCode())) {
                    return IM + "-" + config.getAdditionalField(IM_CSD_TYPE);
                } else {
                    return IM;
                }
            } else {
                return VM;
            }
        }
        return "";
    }

}
