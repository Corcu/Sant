package calypsox.tk.report;

import calypsox.tk.report.generic.SantGenericTradeReportStyle;
import com.calypso.tk.bo.InventoryCashPosition;
import com.calypso.tk.core.*;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.CollateralConfigReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

public class SantCashFlowsReportStyle extends SantGenericTradeReportStyle {

    private static final long serialVersionUID = -7858001643474984530L;
    private static final String CALYPSO = "CALYPSO";
    private static final String PAY = "Pay";
    private static final String RECEIVE = "Receive";
    public static final String PROCESS_DATE = "Process Date";
    public static final String PO = "ProcessingOrg";
    public static final String MARGIN_CALL_CONTRACT = "Margin Call Contract";
    private TradeReportStyle tradeReportStyle = null;
    private CollateralConfigReportStyle marginCallContractReportStyle = null;

    public static final String[] DEFAULTS_COLUMNS = {SantCashFlowsReportTemplate.TRADE_DATE,
            SantCashFlowsReportTemplate.SISTEMA_ORIGEN, SantCashFlowsReportTemplate.PORTFOLIO,
            SantCashFlowsReportTemplate.BASE_CCY, SantCashFlowsReportTemplate.COUNTERPARTY,
            SantCashFlowsReportTemplate.COUNTERPARTY_NAME, SantCashFlowsReportTemplate.CALL_ACCOUNT,
            SantCashFlowsReportTemplate.DEAL_TYPE, SantCashFlowsReportTemplate.PRINCIPAL_AMOUNT,
            SantCashFlowsReportTemplate.VALUE_DATE, SantCashFlowsReportTemplate.GLOBAL_BALANCE,
            SantCashFlowsReportTemplate.VAL_CAP_REP, SantCashFlowsReportTemplate.AGREEMENT_NAME};

    /**
     * Calculate and get the result for every column of a row.
     */
    @SuppressWarnings({"rawtypes"})
    @Override
    public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors)
            throws InvalidParameterException {

        // GSM: 28/06/2013 - deprecated new core.
        // final MarginCallConfig marginCallConfig = (MarginCallConfig) row.getProperty("SantMarginCallConfig");
        final CollateralConfig marginCallConfig = (CollateralConfig) row.getProperty("SantMarginCallConfig");

        final Trade trade = (Trade) row.getProperty("Trade");
        @SuppressWarnings("unused") final Account account = (Account) row.getProperty("Account");

        if (columnName.equals(SantCashFlowsReportTemplate.SISTEMA_ORIGEN)) {
            return SantCashFlowsReportStyle.CALYPSO;
        } else if (columnName.equals(SantCashFlowsReportTemplate.CALL_ACCOUNT)) {
            MarginCall marginCall = (MarginCall) trade.getProduct();
            return getAccountByContractAndCurrency(Math.toIntExact(marginCall.getLinkedLongId()), marginCall.getCurrency());
        } else if (columnName.equals(SantCashFlowsReportTemplate.VALUE_DATE)) {
            return trade.getSettleDate();
        } else if (columnName.equals(SantCashFlowsReportTemplate.GLOBAL_BALANCE)) {
            final double result = getGlobalBalance(marginCallConfig, trade);
            return formatValueIfAmount(new Amount(result));
        } else if (columnName.equals(SantCashFlowsReportTemplate.DEAL_TYPE)) {
            return getDirection(trade);
        } else if (columnName.equals(SantCashFlowsReportTemplate.PRINCIPAL_AMOUNT)) {
            return formatValueIfAmount(new Amount(getPrincipalAmount(trade)));
        } else if (columnName.equals(SantCashFlowsReportTemplate.AGREEMENT_NAME)) {
            if (marginCallConfig != null) {
                return marginCallConfig.getName();
            } else {
                return "";
            }
        } else if (columnName.equals(PO)) {
            if (marginCallConfig != null) {
                return marginCallConfig.getProcessingOrg().getName();
            } else {
                return "";
            }
        } else if (columnName.equals(MARGIN_CALL_CONTRACT)) {
            if (marginCallConfig != null) {
                return marginCallConfig.getName();
            } else {
                return "";
            }
        } else {
            row.setProperty("MarginCallConfig", marginCallConfig);
            if (this.marginCallContractReportStyle == null) {
                this.marginCallContractReportStyle = super.getCollateralConfigReportStyle();
            }

            if (this.tradeReportStyle == null) {
                this.tradeReportStyle = super.getTradeReportStyle();
            }

            Object retVal = null;

            if (columnName.equals(SantCashFlowsReportTemplate.BASE_CCY)) {
                retVal = this.marginCallContractReportStyle.getColumnValue(row, "Currency", errors);
            } else if (columnName.equals(SantCashFlowsReportTemplate.COUNTERPARTY)) {
                retVal = this.tradeReportStyle.getColumnValue(row, "CounterParty", errors);
            } else if (columnName.equals(SantCashFlowsReportTemplate.COUNTERPARTY_NAME)) {
                retVal = this.tradeReportStyle.getColumnValue(row, "CPTY Full Name", errors);
            } else if (columnName.equals(SantCashFlowsReportTemplate.VAL_CAP_REP)) {
                retVal = this.tradeReportStyle.getColumnValue(row, "Trade Id", errors);
            } else {
                retVal = this.tradeReportStyle.getColumnValue(row, columnName, errors);
            }

            return retVal;
        }
    }

    /**
     * Get the direction of a trade. If the amount is negative, its a pay. if not, its a receive
     *
     * @param trade trade to check
     * @return direction
     */
    private String getDirection(final Trade trade) {
        String sToReturn = "";

        final double principalAmount = getPrincipalAmount(trade);

        if (principalAmount != 0) {
            if (principalAmount > 0) {
                sToReturn = SantCashFlowsReportStyle.RECEIVE;
            } else if (principalAmount < 0) {
                sToReturn = SantCashFlowsReportStyle.PAY;
            }
        }

        return sToReturn;
    }

    /**
     * Get the principal amount of the trade
     *
     * @param trade trade
     * @return amount
     */
    private double getPrincipalAmount(final Trade trade) {
        double principalAmount = 0.0;

        if (trade != null) {
            final Product product = trade.getProduct();
            principalAmount = product.getPrincipal();
        }
        return principalAmount;
    }

    /**
     * Get the balance of the account for every trade date
     *
     * @param marginCallConfig
     * @param trade
     * @return balance of the account
     */
    private double getGlobalBalance(final CollateralConfig marginCallConfig, final Trade trade) {
        double balance = 0.0;

        try {
            final InventoryCashPosition cashPosition = getAccountPosition(marginCallConfig, trade.getTradeDate()
                    .getJDate(TimeZone.getDefault()));
            if (cashPosition != null) {
                balance = cashPosition.getTotal();
            }
        } catch (final RemoteException e) {
            final StringBuffer message = new StringBuffer(
                    "Error loading the cash position to get the balance for account=" + marginCallConfig.getName()
                            + " in SantCashFlowsReport ");
            Log.error(message, e.getCause());
            Log.error(this, e); //sonar
        }

        return balance;
    }

    /**
     * Get the cash position of an account for a specified date
     *
     * @param marginCallConfig
     * @param date
     * @return last position of the account
     * @throws RemoteException
     */
    private InventoryCashPosition getAccountPosition(final CollateralConfig marginCallConfig, final JDate date)
            throws RemoteException {
        InventoryCashPosition accountPosition = null;

        if ((marginCallConfig != null) && (date != null)) {
            final StringBuilder where = new StringBuilder();
            where.append("internal_external = 'MARGIN_CALL'");
            where.append(" AND ");
            where.append("position_type = 'THEORETICAL'");
            where.append(" AND ");
            where.append("date_type = 'TRADE'");
            where.append(" AND ");
            //GSM 11/04/2016 - Fix to get current position
            where.append("mcc_id = " + marginCallConfig.getId());
            //where.append("config_id = " + marginCallConfig.getId());
            where.append(" AND ");
            where.append("position_date <= ");
            where.append("to_date ('" + date.toString() + "', 'dd/MM/yyyy')");

            final Vector<InventoryCashPosition> positions = DSConnection.getDefault().getRemoteBO()
                    .getLastInventoryCashPositions("", where.toString(), null);

            for (final InventoryCashPosition positionFromResult : positions) {
                if ((accountPosition == null)
                        || positionFromResult.getPositionDate().after(accountPosition.getPositionDate())) {
                    accountPosition = positionFromResult;
                }
            }
        }

        return accountPosition;
    }

    /**
     * Format the values to retrieve the data in the specified format. 2 decimals and no separator in thousands
     *
     * @param value value to format
     * @return value formatted
     */
    private String formatValueIfAmount(final Object value) {
        if (value instanceof Amount) {
            final NumberFormat numberFormatter = new DecimalFormat("#0.00", new DecimalFormatSymbols(Locale.ENGLISH));

            final String numberString = numberFormatter.format(((Amount) value).get());

            return numberString;
        }

        return value.toString();
    }

    @SuppressWarnings({"rawtypes", "unchecked", "unused"})
    private Account getAccount(final int counterPartyId, final String currency) {

        Account res = null;
        Vector accounts = null;
        Vector currencies = new Vector();
        currencies.add(currency);
        try {
            accounts = DSConnection.getDefault().getRemoteAccounting()
                    .getAccounts("SETTLE", true, counterPartyId, "ALL", 39709, currencies);

            if ((null == accounts) || (accounts.size() == 0)) {
                Log.error(this, String.format("Could not find account"));
                return res;
            }

        } catch (final RemoteException e) {
            Log.error(this, e);
            return res;
        }

        // filter foun accounts by currency
        Account account = (Account) accounts.get(0);

        res = account;
        return res;

    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private String getAccountByContractAndCurrency(final int contractNumber, final String currency) {

        final String res = "";
        Vector accounts = null;
        Vector validAccounts = new Vector();
        try {
            accounts = DSConnection.getDefault().getRemoteAccounting()
                    .getAccountByAttribute("MARGIN_CALL_CONTRACT", String.valueOf(contractNumber));
            if ((null == accounts) || (accounts.size() == 0)) {
                Log.error(this, String.format("Account with contract %s not configured in Calypso", contractNumber));
                return res;
            }

        } catch (final RemoteException e) {
            Log.error(this, e);
            return res;
        }

        // filter foun accounts by currency
        for (final Object object : accounts) {
            if (Util.isEqualStrings(currency, ((Account) object).getCurrency())) {
                validAccounts.add(object);
            }
        }

        if (validAccounts.size() != 1) {
            Log.error(this,
                    String.format("Account with contract %s and  currency %s is not found", contractNumber, currency));
            return res;
        }

        return ((Account) validAccounts.get(0)).getExternalName();
    }

}
