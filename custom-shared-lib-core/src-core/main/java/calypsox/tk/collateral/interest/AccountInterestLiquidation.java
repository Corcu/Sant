/**
 *
 */
package calypsox.tk.collateral.interest;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

/**
 * @author aalonsop
 * @description Created in Calypso v12 to v14.4 migration. Sets the
 *              InterestBearing isClient keyword to its posible
 *              values(true/false). This class should be called before and after
 *              an Account_Interest_Transfer liquidation, supplies the methods
 *              to make the generation of a SimpleTransfer instead of a
 *              CustomerTransfer posible.
 *
 */
public final class AccountInterestLiquidation {

    private static final String IS_CLIENT_KEYWORD = "IS_CLIENT";
    private Vector<Account> accountVector = new Vector<Account>();
    private JDate valDate = null;
    protected String accId = null;

    /**
     * Gets the variables from the attributes of the ST or the Report
     *
     * @param accId
     * @param valDate
     * @param timezone
     */
    public AccountInterestLiquidation(String accId, JDatetime valDate, TimeZone timezone) {
        this.valDate = convertValuationDatetime(valDate, timezone);
        this.accId = accId;
    }

    /**
     *
     * @param accounts
     * @param valDate
     * @param timezone
     */
    public AccountInterestLiquidation(Vector<Account> accounts, JDatetime valDate, TimeZone timezone) {
        this.valDate = convertValuationDatetime(valDate, timezone);
        if (accounts != null) {
            this.accountVector = new Vector<Account>();
            this.accountVector.addAll(accounts);
        }
    }

    /**
     *
     * @param acc
     * @param ds
     * @throws CalypsoServiceException
     */
    protected List<Trade> setInterestBearingIsClientKey(Vector<Account> accVector, DSConnection ds, boolean value)
            throws CalypsoServiceException {
        TradeArray tradesToProcess = loadInterestBearingsFromAccount(ds, accVector);
        for (Trade ibTrade : tradesToProcess.getTrades()) {
            ibTrade.addKeyword(IS_CLIENT_KEYWORD, mapBoolean(value));
            ibTrade.setAction(Action.AMEND);
            if (Log.isDebug())
                Log.debug(super.getClass(), "InterestBearing with id= " + ibTrade.getLongId()
                        + " has the isClient keyword set to: " + ibTrade.getKeywordValue(IS_CLIENT_KEYWORD));
        }
        return tradesToProcess.toList();
    }

    /**
     * Must be called before the BOUtil.generateTransferInsterests call!!!.
     * Iterates over the accounts and do the required post-proccesing to the
     * InterestBearing trades that are going to be settled. Should only be call
     * if the ST valuation date is equals to the IB paymentDate.
     *
     * @param ds
     * @throws CalypsoServiceException
     */
    public void processGeneratedInterestBearing(DSConnection ds, boolean value) throws CalypsoServiceException {
        if (this.accountVector.isEmpty()) {
            Account acc = getAccount(ds);
            if (acc != null)
                this.accountVector.addElement(acc);
        }
        List<Trade> auxList = new ArrayList<Trade>();
        auxList.addAll(setInterestBearingIsClientKey(accountVector, ds, value));
        try {
            ExternalArray procesedTrades = new ExternalArray(auxList);
            ds.getRemoteTrade().saveTrades(procesedTrades);
        } catch (InvalidClassException e) {
            Log.error(super.getClass(),
                    "Couldn't instantiate ExternalArray object with all the procesed InterestBearing " + e.getMessage()
                            + " \n No trade was updated");
            Log.error(this, e); //sonar
        }
    }

    /**
     *
     * @param ds
     * @return All the accounts that fit the interest criteria
     * @throws CalypsoServiceException
     */
    protected Account getAccount(DSConnection ds) throws CalypsoServiceException {
        Account acc = null;
        if (accId != null) {
            Integer accInt = Integer.valueOf(accId);
            if (accInt.intValue() != 0) {
                acc = BOCache.getAccount(ds, accInt.intValue());
            }
        }
        return acc;
    }

    /**
     *
     * @param acc
     * @param le
     * @return The related InterestBearings from an Account Splits the query
     *         because it not posible to set more than 1000 indexes inside an IN
     *         clause
     * @throws CalypsoServiceException
     */
    protected TradeArray loadInterestBearingsFromAccount(DSConnection ds, Vector<Account> accVector)
            throws CalypsoServiceException {
        String from = "product_int_bearing";
        String where = new StringBuilder()
                .append("trade_status <> 'CANCELED' AND product_desc.product_type = 'InterestBearing' AND product_desc.product_id = product_int_bearing.product_id AND product_int_bearing.payment_date = ")
                .append(Util.date2SQLString(valDate)).toString();
        TradeArray trades = new TradeArray();
        if (accVector != null) {
            if (!accVector.isEmpty()) {
                List<String> inClauses = accountVectorToIdString(accVector);
                for (String inClause : inClauses) {
                    where = new StringBuilder().append(where).append(" AND product_int_bearing.account_id IN ( ")
                            .append(inClause).append(" )").toString();
                    trades.addAll(ds.getRemoteTrade().getTrades(from, where, null, null));
                }
            }
        }

        return trades;
    }

    /**
     *
     * @param accVector
     *            The retrieved account ids, to insert them inside SQL's IN
     *            clause, returns a String[] cause Oracle doesn't support IN
     *            filtering for more than 1000 values
     * @return
     */
    protected List<String> accountVectorToIdString(Vector<Account> accVector) {
        StringBuilder builder = new StringBuilder();
        List<String> inClauses = new ArrayList<String>();
        int i = 0;
        for (Account acc : accVector) {
            builder.append(acc.getId() + ",");
            if (i++ >= 998) {
                builder.deleteCharAt(builder.lastIndexOf(","));
                inClauses.add(builder.toString());
                builder.setLength(0);
                i = 0;
            }
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.lastIndexOf(","));
            inClauses.add(builder.toString());
        }
        return inClauses;
    }

    protected JDate convertValuationDatetime(JDatetime datetime, TimeZone timezone) {
        return JDate.valueOf(datetime, timezone);
    }

    private String mapBoolean(boolean value) {
        if (value)
            return "True";
        else
            return "False";
    }

    public void setAccountId(String accId) {
        this.accId = accId;
    }

    public void flushAccountList() {
        this.accountVector.removeAllElements();
    }
}
