/**
 *
 */
package calypsox.tk.core;

import com.calypso.tk.bo.*;
import com.calypso.tk.bo.sql.BOMessageSQL;
import com.calypso.tk.bo.sql.BOTransferSQL;
import com.calypso.tk.bo.workflow.BOTransferWorkflow;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.AuditSQL;
import com.calypso.tk.core.sql.TradeSQL;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.product.flow.CashFlowInterest;
import com.calypso.tk.service.BackOfficeServerImpl;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.DataServer;
import com.calypso.tk.util.ComparatorFactory;
import com.calypso.tk.util.MessageArray;
import com.calypso.tk.util.TransferArray;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

/**
 * @author ramous
 *
 */

/**
 * @author ramous
 *
 */
public class WorkflowUtil {

    public static final String WF_DATE_FORMAT = "dd/MM/yyyy";
    private static final String WF_TIME_FORMAT = "HH:mm";

    public static final String XFER_VALUE_DATE_ATTR = "TransferValueDate";
    public static final String CONFIRMATION_TYPE_ATTR = "ConfirmationType";
    public static final String MATCHING_STATUS_ATTR = "MatchingStatus";
    public static final String MATCHING_STATUS_TRUE = "Matched";
    public static final String MATCHING_STATUS_FALSE = "Unmatched";
    public static final String CONFIRMATION_TYPE_FIXED = "Fixed";
    public static final String CONFIRMATION_TYPE_FLOAT_FIRST = "FirstFloat";
    public static final String CONFIRMATION_TYPE_FLOAT_INTERMEDIATE = "IntermediateFloat";
    public static final String CONFIRMATION_TYPE_TERMINATION = "TradeTermination";
    public static final String CONFIRMATION_TYPE_PARTIAL_TERMINATION_FLOATING = "TradePartialTerminationFloat";
    public static final String CONFIRMATION_TYPE_PARTIAL_TERMINATION_FIX = "TradePartialTerminationFix";
    public static final String CONFIRMATION_TYPE_FLOAT_FINAL = "FinalFloat";
    public static final String CASHFLOW_TYPE_INTEREST = "INTEREST";
    public static final String CASHFLOW_TYPE_WTH = "WITHHOLDINGTAX";

    public static final String XFER_STATUS_SETTLED_S = "SETTLED_S";

    private static final String WORKFLOW_UTIL = "WorkflowUtil";

    public final static String TRUE = "true";

    /*** System codes ***/
    public static final String SYSTEM_CALYPSO = "Calypso";
    public static final String SYSTEM_MUREX = "Murex";
    public static final String SYSTEM_E_FX = "e-Fx";
    public static final String SYSTEM_MYSIS = "Mysis";
    public static final String SYSTEM_FXALL = "FxAll";
    public static final String SYSTEM_FXALL2 = "FXAll";
    public static final String SYSTEM_CURRENX = "Currenex";

    public static final String SYSTEM_CALYPSO_CODE = "CAL";
    public static final String SYSTEM_MUREX_CODE = "M31";
    public static final String SYSTEM_E_FX_CODE = "EFX";
    public static final String SYSTEM_MYSIS_CODE = "MYS";
    public static final String SYSTEM_FXALL_CODE = "FAL";
    public static final String SYSTEM_CURRENX_CODE = "CUR";

    /*** Product codes ***/

    public static final String NDF = "FXNDF";
    public static final String SWAP = "FXSwap";
    public static final String SPOT = "FX";
    public static final String FORWARD = "FXForward";
    public static final String DEPOSITS = "Cash";
    public static final String SECLENDING = "SecLending";
    public static final String REPO = "Repo";
    public static final String BOND_FORWARD = "Bondtrue";
    public static final String BOND_SPOT = "Bondfalse";

    public static final String NDF_CODE = "NDF";
    public static final String SWAP_CODE = "FXSWP";
    public static final String SPOT_CODE = "FXSPT";
    public static final String FORWARD_CODE = "FXFWD";
    public static final String DEPOSITS_CODE = "DEP";
    public static final String SECLENDING_CODE = "STKBRW";
    public static final String REPO_CODE = "REP";
    public static final String BOND_FORWARD_CODE = "BNDFWD";
    public static final String BOND_SPOT_CODE = "BNDSPT";

    public static final Vector<Integer> CONTACT_BIC_VALID_LENGTH = new Vector<Integer>();

    static {
        CONTACT_BIC_VALID_LENGTH.add(11);
        CONTACT_BIC_VALID_LENGTH.add(8);
    }

    public static final Map<String, String> XLATED_SYSTEM_CODE = new java.util.Hashtable<String, String>();
    public static final Map<String, String> XLATED_PRODUCT_CODE = new java.util.Hashtable<String, String>();

    static {
        // init XLATED_SYSTEM_CODE
        XLATED_SYSTEM_CODE.put(SYSTEM_CALYPSO, SYSTEM_CALYPSO_CODE);
        XLATED_SYSTEM_CODE.put(SYSTEM_MUREX, SYSTEM_MUREX_CODE);
        XLATED_SYSTEM_CODE.put(SYSTEM_E_FX, SYSTEM_E_FX_CODE);
        XLATED_SYSTEM_CODE.put(SYSTEM_MYSIS, SYSTEM_MYSIS_CODE);
        XLATED_SYSTEM_CODE.put(SYSTEM_FXALL, SYSTEM_FXALL_CODE);
        XLATED_SYSTEM_CODE.put(SYSTEM_FXALL2, SYSTEM_FXALL_CODE);
        XLATED_SYSTEM_CODE.put(SYSTEM_CURRENX, SYSTEM_CURRENX_CODE);

        // init XLATED_PRODUCT_CODE
        XLATED_PRODUCT_CODE.put(NDF, NDF_CODE);
        XLATED_PRODUCT_CODE.put(SWAP, SWAP_CODE);
        XLATED_PRODUCT_CODE.put(SPOT, SPOT_CODE);
        XLATED_PRODUCT_CODE.put(FORWARD, FORWARD_CODE);
        XLATED_PRODUCT_CODE.put(DEPOSITS, DEPOSITS_CODE);
        XLATED_PRODUCT_CODE.put(SECLENDING, SECLENDING_CODE);
        XLATED_PRODUCT_CODE.put(REPO, REPO_CODE);
        XLATED_PRODUCT_CODE.put(BOND_FORWARD, BOND_FORWARD_CODE);
        XLATED_PRODUCT_CODE.put(BOND_SPOT, BOND_SPOT_CODE);

    }

    public static WorkflowUtil instance;

    private WorkflowUtil() {
    }

    public static WorkflowUtil getInstance() {
        if (instance == null) {
            instance = new WorkflowUtil();
        }
        return instance;
    }

    /**
     *
     * Only to JUnit. You can use that to replace the instance with a mockito
     * instance
     */
    public static void setInstance(final WorkflowUtil mockInstance) {
        instance = mockInstance;
    }

    /**
     *
     * Translate a Date to String using the WF_DATE_FORMAT
     *
     * @param date
     * @return
     */
    public String getDateAsString(final Date date) {
        String res = null;
        res = new SimpleDateFormat(WF_DATE_FORMAT).format(date);
        return res;
    }

    /**
     *
     * Translate a String to a JDate using the WF_DATE_FORMAT
     *
     * @param dateString
     * @return
     * @throws ParseException
     */
    public JDate getJDateFromString(final String dateString)
            throws ParseException {
        JDate res = null;
        final Date date = new SimpleDateFormat(WF_DATE_FORMAT)
                .parse(dateString);
        res = JDate.valueOf(date);

        return res;
    }

    /**
     *
     * Translate a Date to String using the WF_DATE_FORMAT
     *
     * @param date
     * @return
     */
    public String getJDateAsString(final JDate date) {
        final Date d = date.getDate(TimeZone.getDefault());
        return getDateAsString(d);
    }

    /**
     * get the first Cashflow basing on payment Date
     *
     * @param trade
     * @return
     * @throws Exception
     */
    public CashFlow getFirstCashFlow(final Trade trade) throws Exception {
        CashFlow res = null;
        final CashFlowSet cashFlows = trade.getProduct().getFlows();
        if ((null == cashFlows) || cashFlows.isEmpty()) {
            Log.error(WorkflowUtil.class, "No CashFlow found.");
            throw new Exception("No CashFlow found.");
        }
        cashFlows.sort(ComparatorFactory.getCashFlowDateComparator());
        res = cashFlows.elementAt(0);
        return res;
    }

    /**
     * get the last Cashflow basing on payment Date
     *
     * @param trade
     * @return
     * @throws Exception
     */
    public CashFlow getLastCashFlow(final Trade trade) throws Exception {
        CashFlow res = null;
        final CashFlowSet cashFlows = trade.getProduct().getFlows();
        if ((null == cashFlows) || cashFlows.isEmpty()) {
            Log.error(WorkflowUtil.class, "No CashFlow found.");
            throw new Exception("No CashFlow found.");
        }
        cashFlows.sort(ComparatorFactory.getCashFlowDateComparator());
        res = cashFlows.findLastFlowByPaymentDate();
        return res;
    }

    /**
     * @param trade
     * @return
     * @throws Exception
     */
    public CashFlowInterest getLastInterestCashFlow(final Trade trade) {
        CashFlowInterest res = null;
        final CashFlowSet cashFlows = trade.getProduct().getFlows();
        if ((null == cashFlows) || cashFlows.isEmpty()) {
            Log.error(WorkflowUtil.class, "No CashFlow found.");
            return null;
        }
        cashFlows.sort(ComparatorFactory.getCashFlowDateComparator());
        res = (CashFlowInterest) cashFlows.findLastFlow(CASHFLOW_TYPE_INTEREST);
        if (null == res) {
            Log.error(WorkflowUtil.class, "No INTEREST CashFlow found.");
            return null;
        }
        return res;
    }

    /**
     * get cash Flow by Reset date
     *
     * @param trade
     * @param rateResetdate
     * @return
     * @throws Exception
     */
    public CashFlowInterest getCashFlowByResetDate(final Trade trade,
                                                   final String rateResetdate) {
        CashFlowInterest res = null;
        final CashFlowSet cashFlows = trade.getProduct().getFlows();
        if ((null == cashFlows) || cashFlows.isEmpty()) {
            Log.error(WorkflowUtil.class, "No CashFlow found.");
            return null;
        }
        @SuppressWarnings("unchecked") final Vector<CashFlow> cashFlowV = (Vector<CashFlow>) cashFlows.toVector();
        // run over all cashFlows to find Interest onew and compare their reset
        // date to teh given reset date
        for (final CashFlow cashflow : cashFlowV) {
            if (cashflow instanceof CashFlowInterest) {
                final CashFlowInterest cashFlowInterest = (CashFlowInterest) cashflow;
                if (rateResetdate
                        .equals(getDateAsString(cashFlowInterest.getResetDate()
                                .getDate(trade.getBook().getLocation())))) {
                    res = cashFlowInterest;

                }
            }
        }
        return res;
    }

    /**
     * @param trade
     * @return
     * @throws Exception
     */
    public CashFlowInterest getFirstInterestCashFlow(final Trade trade) {
        CashFlowInterest res = null;
        final CashFlowSet cashFlows = trade.getProduct().getFlows();
        if ((null == cashFlows) || cashFlows.isEmpty()) {
            Log.error(WorkflowUtil.class, "No CashFlow found.");
            return null;
        }
        cashFlows.sort(ComparatorFactory.getCashFlowDateComparator());
        res = (CashFlowInterest) cashFlows
                .findFirstFlow(CASHFLOW_TYPE_INTEREST);
        if (null == res) {
            Log.error(WorkflowUtil.class, "No INTEREST CashFlow found.");
            return null;
        }
        return res;
    }

    /**
     * Retrieve BOTransfer list associated to a Trade with <i>Persistence API or
     * remote RMI API</i>, depending code is executed on Data Server or a Client
     * (Main Entry, Engine ..)
     *
     * @param tradeId
     * @param ds
     * @return
     * @throws RemoteException
     * @throws WorkflowException
     * @throws PersistenceException
     */
    @SuppressWarnings("unchecked")
    public Vector<BOTransfer> getTransfersAsVector(final long tradeId,
                                                   final DSConnection ds) throws RemoteException, WorkflowException,
            PersistenceException {

        TransferArray transfers = null;
        if (tradeId <= 0) {
            return null;
        } else if (!isDataServer()) {
            transfers = ds.getRemoteBO().getBOTransfers(tradeId);
        } else {
            Log.debug(WorkflowUtil.class,
                    "_isDataServer = true, then call DS API");
            transfers = BOTransferSQL.getTransfers(tradeId); // DataServer
        }
        return ((transfers == null) ? null : transfers.toVector());
    }

    /**
     * Retrieve BOTransfer list associated to a Trade with <i>Persistence API or
     * remote RMI API</i>, depending code is executed on Data Server or a Client
     * (Main Entry, Engine ..)
     *
     * @param tradeId
     * @param ds
     * @param dbCon
     * @return
     * @throws RemoteException
     * @throws WorkflowException
     * @throws PersistenceException
     */
    public TransferArray getTransfers(final long tradeId, final DSConnection ds,
                                      final Object dbCon) throws RemoteException, PersistenceException {

        TransferArray transfers = null;
        if (tradeId <= 0) {
            return null;
        } else if (dbCon == null) {
            transfers = ds.getRemoteBO().getBOTransfers(tradeId);
        } else {
            Log.debug(WorkflowUtil.class,
                    "_isDataServer = true, then call DS API");
            transfers = BOTransferSQL.getTransfers(tradeId, (Connection) dbCon); // DataServer
        }
        return transfers;
    }

    /**
     * Retrieve BOTransfer list associated to a where clause with <i>Persistence
     * API or remote RMI API</i>, depending code is executed on Data Server or a
     * Client (Main Entry, Engine ..)
     *
     * @param where
     * @param ds
     * @return
     * @throws RemoteException
     * @throws WorkflowException
     * @throws PersistenceException
     */
    public TransferArray getTransfers(final String where, final DSConnection ds)
            throws RemoteException, PersistenceException {

        TransferArray transfers = null;
        if (!isDataServer()) {
            transfers = ds.getRemoteBO().getBOTransfers(where, null);
        } else {
            Log.debug(WorkflowUtil.class,
                    "_isDataServer = true, then call DS API");
            transfers = BOTransferSQL.getTransfers(where, null); // DataServer
        }
        return transfers;
    }

    /**
     * Retrieve BOTransfer <i>Persistence API or remote RMI API</i>, depending
     * code is executed on Data Server or a Client (Main Entry, Engine ..)
     *
     * @param transferId
     * @param ds
     * @return
     * @throws RemoteException
     * @throws WorkflowException
     * @throws PersistenceException
     */
    public BOTransfer getTransfer(final int transferId, final DSConnection ds,
                                  final Object dbCon) throws PersistenceException, RemoteException {

        BOTransfer transfer = null;
        if (dbCon != null) {
            Log.debug(WorkflowUtil.class, "dbCon != null, then call DS API");
            transfer = BOTransferSQL.getTransfer(transferId);
        } else {
            transfer = ds.getRemoteBO().getBOTransfer(transferId);
        }
        return transfer;
    }

    /**
     * Retrieve Trade with <i>Persistence API or remote RMI API</i>, depending
     * code is executed on Data Server or a Client (Main Entry, Engine ..)
     *
     * @param tradeId
     * @param dsCon
     * @param dbCon
     * @return
     * @throws PersistenceException
     * @throws RemoteException
     */
    public Trade getTrade(final long tradeId, final DSConnection dsCon)
            throws PersistenceException, RemoteException {
        Trade trade = null;
        if (isDataServer()) {
            Log.debug(WorkflowUtil.class, "dbCon != null, then call DS API");
            trade = TradeSQL.getTrade(tradeId);
        } else {
            trade = dsCon.getRemoteTrade().getTrade(tradeId);
        }
        return trade;
    }

    /**
     * Retrieve Trade with <i>Persistence API or remote RMI API</i>, depending
     * code is executed on Data Server or a Client (Main Entry, Engine ..)
     *
     * @param tradeId
     * @param dsCon
     * @param dbCon
     * @return
     * @throws PersistenceException
     * @throws RemoteException
     */
    public Trade getTrade(final long tradeId, final DSConnection dsCon,
                          final Object dbCon) throws PersistenceException, RemoteException {
        Trade trade = null;
        if (dbCon != null) {
            Log.debug(WorkflowUtil.class, "dbCon != null, then call DS API");
            trade = TradeSQL.getTrade(tradeId, (Connection) dbCon);
        } else {
            trade = dsCon.getRemoteTrade().getTrade(tradeId);
        }
        return trade;
    }

    /**
     * Retrieve Messages with <i>Persistence API or remote RMI API</i>,
     * depending code is executed on Data Server or a Client (Main Entry, Engine
     * ..)
     *
     * @param tradeId
     * @param dsCon
     * @param dbCon
     * @return
     * @throws PersistenceException
     * @throws RemoteException
     */
    public MessageArray getMessages(final long tradeId,
                                    final DSConnection dsCon, final Object dbCon)
            throws PersistenceException, RemoteException {
        MessageArray tradeMessages = null;
        if (dbCon != null) {
            Log.debug(WorkflowUtil.class, "dbCon != null, then call DS API");
            tradeMessages = BOMessageSQL.getMessages(tradeId,
                    (Connection) dbCon);
        } else {
            tradeMessages = dsCon.getRemoteBO().getMessages(tradeId);
        }
        return tradeMessages;
    }

    /**
     * Retrieve Messages with <i>Persistence API or remote RMI API</i>,
     * depending code is executed on Data Server or a Client (Main Entry, Engine
     * ..)
     *
     * @param where
     * @param dsCon
     * @param dbCon
     * @return
     * @throws PersistenceException
     * @throws RemoteException
     */
    public MessageArray getMessages(final String where, final DSConnection dsCon)
            throws PersistenceException, RemoteException {
        MessageArray tradeMessages = null;
        if (isDataServer()) {
            Log.debug(WorkflowUtil.class, "dbCon != null, then call DS API");
            tradeMessages = BOMessageSQL.getMessages(where, null);

        } else {
            tradeMessages = dsCon.getRemoteBO().getMessages(where, null);
        }
        return tradeMessages;
    }

    /**
     * Retrieve Messages related to Transfer with <i>Persistence API or remote
     * RMI API</i>, depending code is executed on Data Server or a Client (Main
     * Entry, Engine ..)
     *
     * @param transferId
     * @param dsCon
     * @param dbCon
     * @return
     * @throws PersistenceException
     * @throws RemoteException
     */
    public MessageArray getTransferMessages(final int transferId,
                                            final DSConnection dsCon, final Object dbCon)
            throws PersistenceException, RemoteException {
        MessageArray tradeMessages = null;
        if (dbCon != null) {
            Log.debug(WorkflowUtil.class, "dbCon != null, then call DS API");
            tradeMessages = BOMessageSQL.getTransferMessages(transferId,
                    (Connection) dbCon);
        } else {
            tradeMessages = dsCon.getRemoteBO().getTransferMessages(transferId);
        }
        return tradeMessages;
    }

    /**
     * Retrieve Message with <i>Persistence API or remote RMI API</i>, depending
     * code is executed on Data Server or a Client (Main Entry, Engine ..)
     *
     * @param messageId
     * @param dsCon
     * @param dbCon
     * @return
     * @throws PersistenceException
     * @throws RemoteException
     */
    public BOMessage getMessage(final int messageId, final DSConnection dsCon,
                                final Object dbCon) throws PersistenceException, RemoteException {
        BOMessage message = null;
        if (dbCon != null) {
            Log.debug(WorkflowUtil.class, "dbCon != null, then call DS API");
            message = BOMessageSQL.getMessage(messageId, (Connection) dbCon);
        } else {
            message = DSConnection.getDefault().getRemoteBO()
                    .getMessage(messageId);
        }
        return message;
    }

    /**
     * Returns a TransferArray containing netted BOTransfer objects related by a
     * given id.
     *
     * @param transferId
     * @param ds
     * @return
     * @throws RemoteException
     * @throws WorkflowException
     * @throws PersistenceException
     */
    @SuppressWarnings("unchecked")
    public Vector<BOTransfer> getNettedTransfers(final long transferId,
                                                 final DSConnection ds) throws RemoteException, WorkflowException,
            PersistenceException {

        TransferArray transfers = null;
        if (transferId <= 0) {
            return null;
        } else if (!isDataServer()) {
            transfers = ds.getRemoteBO().getNettedTransfers(transferId);
        } else {
            Log.debug(WorkflowUtil.class,
                    "_isDataServer = true, then call DS API");
            transfers = BOTransferSQL.getNettedTransfers(transferId); // DataServer
        }
        return ((transfers == null) ? null : transfers.toVector());
    }

    /**
     * Checks if is data server.
     *
     * @return true when code is executed in Data Server
     */
    public boolean isDataServer() {
        return DataServer._isDataServer;
    }

    /**
     * @param xfer
     * @param ds
     * @param con
     * @param events
     * @return
     * @throws RemoteException
     * @throws WorkflowException
     * @throws PersistenceException
     */
    public long saveTransfer(final BOTransfer xfer, final DSConnection ds,
                             final Connection con, final Vector<PSEvent> events)
            throws RemoteException, WorkflowException, PersistenceException {

        long result = 0;
        if (xfer == null) {
            return 0;
        } else if (isDataServer()) {
            Log.debug(WorkflowUtil.class,
                    "_isDataServer = true, then call DS API");
            result = BackOfficeServerImpl.save(xfer, 0, WORKFLOW_UTIL, "", con,
                    events);
        } else if (ds != null) {
            result = ds.getRemoteBO().save(xfer, 0, WORKFLOW_UTIL);
        }
        return result;
    }

    /**
     * @param action
     * @param trade
     * @param xfer
     * @param attrs
     * @param kws
     * @param ds
     * @param con
     * @param events
     * @return
     * @throws RemoteException
     * @throws WorkflowException
     * @throws PersistenceException
     * @throws CloneNotSupportedException
     */
    @SuppressWarnings("unchecked")
    public long applyAction(final Action action, final Trade trade,
                            final BOTransfer xfer, final Map<String, String> attrs,
                            final DSConnection ds, final Connection con,
                            final Vector<PSEvent> events,
                            @SuppressWarnings("rawtypes") final Vector excps)
            throws RemoteException, WorkflowException, PersistenceException,
            CloneNotSupportedException {

        if ((action == null) || (xfer == null)) {
            return 0;
        } else {
            final BOTransfer newXfer = (BOTransfer) xfer.cloneIfImmutable();
            if (!BOTransferWorkflow.isTransferActionApplicable(newXfer, trade,
                    action, ds, con)) {
                Log.error(WORKFLOW_UTIL, "Action " + action.toString()
                        + "Not Applicable");

                final BOException ev = new BOException(trade.getLongId(),
                        WORKFLOW_UTIL, "Error :" + "Action "
                        + action.toString()
                        + " Not Applicable on Transfer "
                        + newXfer.getLongId());
                ev.setType(BOException.EXCEPTION);
                excps.addElement(ev);

                return 0;
            }
            if (attrs != null) {
                for (final Map.Entry<String, String> it : attrs.entrySet()) {
                    newXfer.setAttribute(it.getKey(), it.getValue());
                }
            }
            newXfer.setAction(action);
            return saveTransfer(newXfer, ds, con, events);
        }
    }

    /**
     * return all Principal Transfers related to a trade
     *
     * @param tradeId
     * @param dsCon
     * @return
     * @throws RemoteException
     * @throws PersistenceException
     */
    @SuppressWarnings("unchecked")
    public Vector<BOTransfer> getPricipalTransfers(final long tradeId,
                                                   final DSConnection dsCon) throws RemoteException,
            PersistenceException {
        TransferArray transfers = null;
        final String where = "TRADE_ID= " + tradeId
                + " and TRANSFER_TYPE='PRINCIPAL'";
        if (tradeId <= 0) {
            return null;
        } else if (!isDataServer()) {
            transfers = dsCon.getRemoteBO().getBOTransfers(where, null);
        } else {
            Log.debug(WorkflowUtil.class,
                    "_isDataServer = true, then call DS API");
            transfers = BOTransferSQL.getTransfers(where, null); // DataServer
        }
        return ((transfers == null) ? null : transfers.toVector());
    }

    /**
     * return all Fixed Interest Transfers with corresponding value date
     *
     * @param trade
     * @param messageXferValuedate
     * @param dsCon
     * @return
     * @throws RemoteException
     * @throws PersistenceException
     */
    @SuppressWarnings("unchecked")
    public Vector<BOTransfer> getFixedInterestTransfersByValueDate(
            final Trade trade, final String messageXferValuedate,
            final DSConnection dsCon) throws RemoteException,
            PersistenceException {
        final Vector<BOTransfer> res = new Vector<BOTransfer>();
        TransferArray transfers = null;
        final String where = "TRADE_ID= " + trade.getLongId()
                + " and (TRANSFER_TYPE='" + WorkflowUtil.CASHFLOW_TYPE_INTEREST
                + "' or TRANSFER_TYPE='" + WorkflowUtil.CASHFLOW_TYPE_WTH
                + "')";

        // get all Interest transfers
        if (trade.getLongId() <= 0) {
            return null;
        } else if (!isDataServer()) {
            transfers = dsCon.getRemoteBO().getBOTransfers(where, null);
        } else {
            Log.debug(WorkflowUtil.class,
                    "_isDataServer = true, then call DS API");
            transfers = BOTransferSQL.getTransfers(where, null); // DataServer
        }

        // protect iterator
        if (null == transfers) {
            return null;
        }

        // look for fixed transfer with corresponding value date.
        for (final BOTransfer transfer : (Vector<BOTransfer>) transfers
                .toVector()) {
            final String transferValueDate = getDateAsString((transfer
                    .getValueDate().getDate(trade.getBook().getLocation())));
            if (messageXferValuedate.equals(transferValueDate)
                    && (transfer.getSettlementAmount() != 0.0)) {
                res.add(transfer);
            }
        }
        return res;
    }

    /**
     * return Transfers giving their value dates and types
     *
     * @param trade
     * @param messageXferValuedate
     * @param type
     * @param dsCon
     * @return
     * @throws RemoteException
     * @throws PersistenceException
     */
    @SuppressWarnings("unchecked")
    public Vector<BOTransfer> getTransfersByValueDate(final Trade trade,
                                                      final String messageXferValuedate, final String type,
                                                      final DSConnection dsCon) throws RemoteException,
            PersistenceException {

        TransferArray transfers = null;
        String typeWhere = "";
        String where = "TRADE_ID= " + trade.getLongId()
                + " and TO_CHAR(VALUE_DATE, 'DD/MM/YYYY')='"
                + messageXferValuedate + "'";
        if (null != type) {
            if (WorkflowUtil.CASHFLOW_TYPE_INTEREST.equalsIgnoreCase(type)) {
                typeWhere = "(TRANSFER_TYPE='" + type + "' or TRANSFER_TYPE='"
                        + WorkflowUtil.CASHFLOW_TYPE_WTH + "')";
            } else {
                typeWhere = "TRANSFER_TYPE='" + type + "'";
            }
            where += " and " + typeWhere;
        }

        // get all Interest transfers
        if (trade.getLongId() <= 0) {
            return null;
        } else if (!isDataServer()) {
            transfers = dsCon.getRemoteBO().getBOTransfers(where, null);
        } else {
            Log.debug(WorkflowUtil.class,
                    "_isDataServer = true, then call DS API");
            transfers = BOTransferSQL.getTransfers(where, null); // DataServer
        }

        return ((transfers == null) ? null : transfers.toVector());
    }

    /**
     * @param confirmationType
     * @param trade
     * @param message
     * @param dsCon
     * @param dbCon
     * @param events
     * @param excps
     * @return
     * @throws RemoteException
     * @throws WorkflowException
     * @throws PersistenceException
     */
    public Vector<BOTransfer> getRelatedTransfer(final String confirmationType,
                                                 final Trade trade, final BOMessage message, final DSConnection dsCon)
            throws RemoteException, WorkflowException, PersistenceException {
        Vector<BOTransfer> res = null;

        if (WorkflowUtil.CONFIRMATION_TYPE_FIXED.equals(confirmationType)
                || WorkflowUtil.CONFIRMATION_TYPE_PARTIAL_TERMINATION_FIX
                .equals(confirmationType)) {
            Log.debug(WorkflowUtil.class,
                    "Fixed Confirmation Type, set then all the transfers to Matched");
            res = WorkflowUtil.getInstance().getTransfersAsVector(
                    trade.getLongId(), dsCon);

        } else if (WorkflowUtil.CONFIRMATION_TYPE_FLOAT_FIRST
                .equals(confirmationType)) {
            Log.debug(
                    WorkflowUtil.class,
                    "First Float Confirmation Type, set then 1 to N-1 capital Transfer, and applicable Interest Transfer to Matched");
            final Vector<BOTransfer> capitalInterests = WorkflowUtil
                    .getInstance().getPricipalTransfers(trade.getLongId(), dsCon);

            // remove the last Capital refund
            res = removeLastTransfer(capitalInterests);

            // Add Interest transfer if applicable
            final String valueDate = message
                    .getAttribute(WorkflowUtil.XFER_VALUE_DATE_ATTR);
            if ((null != valueDate) && !"".equals(valueDate)) {
                res.addAll(WorkflowUtil
                        .getInstance()
                        .getFixedInterestTransfersByValueDate(
                                trade,
                                message.getAttribute(WorkflowUtil.XFER_VALUE_DATE_ATTR),
                                dsCon));
            }

        } else if (WorkflowUtil.CONFIRMATION_TYPE_FLOAT_INTERMEDIATE
                .equals(confirmationType)) {
            Log.debug(
                    WorkflowUtil.class,
                    "Intermediate Float Confirmation Type, set then applicable Interest Transfer to Matched");
            res = WorkflowUtil.getInstance().getTransfersByValueDate(trade,
                    message.getAttribute(WorkflowUtil.XFER_VALUE_DATE_ATTR),
                    WorkflowUtil.CASHFLOW_TYPE_INTEREST, dsCon);

        } else if (WorkflowUtil.CONFIRMATION_TYPE_FLOAT_FINAL
                .equals(confirmationType)
                || WorkflowUtil.CONFIRMATION_TYPE_TERMINATION
                .equals(confirmationType)
                || WorkflowUtil.CONFIRMATION_TYPE_PARTIAL_TERMINATION_FLOATING
                .equals(confirmationType)) {
            Log.debug(
                    WorkflowUtil.class,
                    "Last Float Confirmation Type, set applicable Interst Transfer and last capital Transferto Matched");
            res = WorkflowUtil.getInstance().getTransfersByValueDate(trade,
                    message.getAttribute(WorkflowUtil.XFER_VALUE_DATE_ATTR),
                    null, dsCon);
        }

        return res;
    }

    @SuppressWarnings("unchecked")
    private Vector<BOTransfer> removeLastTransfer(
            final Vector<BOTransfer> transfers) {
        if ((null == transfers) || (transfers.size() == 0)
                || (transfers.size() == 1)) {
            return (Vector<BOTransfer>) transfers.clone();
        }

        int lastTransferIndex = 0;

        for (int i = 0; i < transfers.size(); i++) {
            if (transfers.get(i).getValueDate()
                    .gte(transfers.get(lastTransferIndex).getValueDate())) {
                lastTransferIndex = i;
            }
        }

        transfers.remove(lastTransferIndex);
        return (Vector<BOTransfer>) transfers.clone();
    }

    public void updateXferRelatedMessages(final BOTransfer xfer,
                                          final DSConnection dsCon) {
        try {

            long xferId = 0;
            if (xfer.getNettedTransferLongId() != 0) {
                xferId = xfer.getNettedTransferLongId();
            } else {
                xferId = xfer.getLongId();
            }
            MessageArray transferMessages = null;
            if (!isDataServer()) {
                transferMessages = dsCon.getRemoteBO().getTransferMessages(
                        xferId);
            } else {
                Log.debug(WorkflowUtil.class,
                        "_isDataServer = true, then call DS API");
                transferMessages = BOMessageSQL.getTransferMessages(xferId);
            }

            for (int j = 0; j < transferMessages.size(); j++) {
                final BOMessage mess = transferMessages.elementAt(j);
                if (mess.getMessageType().equals(BOMessage.PAYMENTMSG)
                        || mess.getMessageType().equals(BOMessage.RECEIPTMSG)) {
                    BOMessage newMess;
                    newMess = (BOMessage) mess.clone();
                    newMess.setAction(Action.UPDATE);
                    dsCon.getRemoteBO().save(newMess, 0, null);
                }
            }
        } catch (final Exception ex) {
            Log.error(
                    WorkflowUtil.class,
                    "SantSetMatched/UnmatchedTransferMessageRule. Can not Apply action UPDATE to Msg Id:"
                            + xfer.getLongId(), ex);
        }
    }

    /**
     * Check if the Legal Entity is Intragrupo
     *
     * @param counterParty
     * @param dsCon
     * @return
     * @throws RemoteException
     */
    @SuppressWarnings("unchecked")
    public boolean isLEIntragrupo(final LegalEntity counterParty,
                                  final DSConnection dsCon) throws RemoteException {
        @SuppressWarnings("rawtypes") final Vector leAtt = BOCache.getLegalEntityAttributes(dsCon,
                counterParty.getId());

        return TRUE.equalsIgnoreCase(SantanderUtil.getInstance()
                .getLEAttributeValue(leAtt,
                        KeywordConstantsUtil.LE_ATTRIBUTE_INTRAGRUPO));

    }

    /**
     *
     * Translate a Time to String using the WF_TIME_FORMAT
     *
     * @param date
     * @return
     */
    public String getTimeAsString(final Date date) {
        String res = null;
        res = new SimpleDateFormat(WF_TIME_FORMAT).format(date);
        return res;
    }

    // @SuppressWarnings("unchecked")
    // public boolean isRelatedTransferSettled(final BOMessage message,
    // final Trade trade) {
    // boolean discard = false;
    //
    // Log.debug(WorkflowUtil.class, "Trade Action is =" + trade.getAction());
    // // Retrieve the message related Trade. In Case of Swap,
    // // retrieves
    // // the corresponding leg of the message argument
    // final Trade relatedDeal = SantanderUtil.getInstance().getTrade(trade,
    // message);
    // final String transferDateKeyword = relatedDeal
    // .getKeywordValue(DateKeyword.KEYWORD_TRANSFER_DATE.getName());
    //
    // final JDate terminationDate = relatedDeal.getTerminationDate();
    // if (!relatedDeal.getProductType().equalsIgnoreCase(DEPOSITS)
    // && (null != terminationDate)
    // && !relatedDeal.getSettleDate().after(terminationDate)) {
    // discard = true;
    // }
    //
    // if (transferDateKeyword != null) {
    // JDate transferDate = new JDate();
    // final SimpleDateFormat dateFormat = new SimpleDateFormat(
    // DateKeyword.KEYWORD_TRANSFER_DATE.getFormat());
    // try {
    // transferDate = JDate.valueOf(dateFormat
    // .parse(transferDateKeyword));
    // if (!relatedDeal.getSettleDate().after(transferDate)) {
    // discard = true;
    // }
    // } catch (final ParseException e) {
    // discard = true;
    // }
    // }
    // // NONE in the case of REPROCESS trade
    // if ((!discard)
    // && (Action.TERMINATE.equals(relatedDeal.getAction())
    // || isProcessTrade(relatedDeal)
    // || "BO_AMEND".equalsIgnoreCase(relatedDeal.getAction()
    // .toString()) || "PROCESS"
    // .equalsIgnoreCase(relatedDeal.getAction().toString()))) {
    // TransferArray transArr = null;
    // try {
    // transArr = BOTransferSQL.getTransfers(message.getTradeLongId());
    // } catch (final PersistenceException e) {
    // Log.error(WorkflowUtil.class,
    // "Cannot retrieve transfers from database for trade id = "
    // + relatedDeal.getId(), e);
    // }
    //
    // if ((transArr != null) && (transArr.size() != 0)) {
    // final Iterator<BOTransfer> iter = transArr.iterator();
    // while (iter.hasNext() && !discard) {
    // final BOTransfer trans = iter.next();
    // // That Are Already in status SETTLED
    // // Of Direction PAY
    // // And Transfer ValueDate is equal to the Message
    // // SettleDate (extracted from related trade/leg)
    // final String transferValueDate = getDateAsString((trans
    // .getValueDate().getDate(relatedDeal.getBook()
    // .getLocation())));
    // if (Status.S_SETTLED.equals(trans.getStatus())
    // || WorkflowUtil.XFER_STATUS_SETTLED_S
    // .equalsIgnoreCase(trans.getStatus()
    // .getStatus())) {
    // if (relatedDeal.getProduct() instanceof Cash) {
    // if (!(WorkflowUtil.CONFIRMATION_TYPE_FIXED
    // .equalsIgnoreCase(message
    // .getAttribute(WorkflowUtil.CONFIRMATION_TYPE_ATTR)))
    // && (transferValueDate
    // .equalsIgnoreCase(message
    // .getAttribute(WorkflowUtil.XFER_VALUE_DATE_ATTR)))) {
    // // Then the Message is to be discarded.
    // discard = true;
    // }
    // } else {
    // if (trans.getValueDate().equals(
    // JDate.valueOf(relatedDeal.getSettleDate()))) {
    // discard = true;
    // }
    // }
    // }
    // }
    // }
    // }
    // return discard;
    // }

    public boolean isProcessTrade(final Trade trade) {
        AuditValue[] audits = null;
        try {
            audits = AuditSQL.loadLatest("Trade", trade.getLongId());
        } catch (final PersistenceException e) {
            Log.error(WorkflowUtil.class,
                    "Cannot retrieve audit for trade id = " + trade.getLongId(), e);
        }
        if ((audits == null) || (audits.length == 0)) {
            return false;
        }

        for (final AuditValue av : audits) {
            Log.debug(WorkflowUtil.class,
                    "Audit field name = " + av.getFieldName());
            if (av.getFieldName().equals("PROCESS")
                    && (av.getVersion() == trade.getVersion())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if 'single' Transfers (non netted) are matched, based on trade
     * Keyword for FX and Transfer Attribute for MM
     *
     * @param transfer
     * @param trade
     * @return
     */
    public boolean isTransferMatched(final BOTransfer transfer,
                                     final Trade trade) {

        boolean isMatched = true;

        if (!Status.isCanceled(transfer.getStatus())
                && (SantanderUtil.TRANSFER_TYPE_PRINCIPAL
                .equalsIgnoreCase(transfer.getTransferType())
                || SantanderUtil.TRANSFER_TYPE_INTEREST
                .equalsIgnoreCase(transfer.getTransferType())
                || SantanderUtil.TRANSFER_TYPE_WTHTAX
                .equalsIgnoreCase(transfer.getTransferType()) || SantanderUtil.TRANSFER_TYPE_ACCRUAL
                .equalsIgnoreCase(transfer.getTransferType()))) {
            if (trade.getProductType().equals(Product.FX)
                    || trade.getProductType().equals(Product.FXFORWARD)) {

                isMatched = SantanderUtil.TRADE_KEYWORD_STATUS_MATCHED
                        .equalsIgnoreCase(trade
                                .getKeywordValue(SantanderUtil.TRADE_KEYWORD_MATCHING_STATUS));

            } else if (trade.getProductType().equals(Product.FXNDF)) {

                isMatched = SantanderUtil.TRADE_KEYWORD_STATUS_MATCHED
                        .equalsIgnoreCase(trade
                                .getKeywordValue(SantanderUtil.TRADE_KEYWORD_MATCHING_STATUS_FIXING));

            } else if (trade.getProductType().equals(Product.FXSWAP)) {
                // Check if transfer is from the NearLeg or FarLeg of the FXSwap
                if (!transfer.getIsReturnB()) {
                    // Near leg

                    isMatched = SantanderUtil.TRADE_KEYWORD_STATUS_MATCHED
                            .equalsIgnoreCase(trade
                                    .getKeywordValue(SantanderUtil.TRADE_KEYWORD_MATCHING_STATUS));
                } else {
                    // Far leg
                    isMatched = SantanderUtil.TRADE_KEYWORD_STATUS_MATCHED
                            .equalsIgnoreCase(trade
                                    .getKeywordValue(SantanderUtil.TRADE_KEYWORD_MATCHING_STATUS_FAR));
                }
            } else if (trade.getProductType().equalsIgnoreCase(
                    WorkflowUtil.DEPOSITS)) {
                // then Loan/Deposits
                isMatched = SantanderUtil.TRADE_KEYWORD_STATUS_MATCHED
                        .equalsIgnoreCase(transfer
                                .getAttribute(WorkflowUtil.MATCHING_STATUS_ATTR));
            }
        }

        return isMatched;
    }

    @SuppressWarnings("unchecked")
    public boolean isNettedTranferMatched(final BOTransfer transfer,
                                          final DSConnection dsCon, final Task task) throws RemoteException,
            WorkflowException, PersistenceException {
        boolean isMatched = true;

        Vector<BOTransfer> underlyingTransfers = null;
        if (transfer.getLongId() == 0) {
            underlyingTransfers = (null != transfer.getUnderlyingTransfers()) ? transfer
                    .getUnderlyingTransfers().toVector() : null;
        } else {
            underlyingTransfers = getNettedTransfers(transfer.getLongId(), dsCon);
        }

        if (null != underlyingTransfers) {
            for (final BOTransfer transfer2 : underlyingTransfers) {
                Trade trade2 = null;

                if (!isDataServer()) {
                    trade2 = dsCon.getRemoteTrade().getTrade(
                            transfer2.getTradeLongId());
                } else {
                    Log.debug(WorkflowUtil.class,
                            "_isDataServer = true, then call DS API");
                    trade2 = TradeSQL.getTrade(transfer2.getTradeLongId());
                }

                if (trade2 == null) {
                    continue;
                }
                isMatched = isMatched && isTransferMatched(transfer2, trade2);
            }
        }

        if (!isMatched) {
            final String msg = "Cannot execute netting. Some underlyings are Unmatched";

            task.setComment(msg);
            return false;
        }

        return isMatched;
    }

    /**
     * The rule should basically identify within an incoming payment message for
     * the product type FXSwap, whether the transfer related is from the NearLeg
     * or FarLeg of the FXSwap. Once it is known that the message is related to
     * a transfer that belongs to a specific leg of the Swap, we should check
     * the value of certain keywords as follows: - if msg belongs to the NearLeg
     * of the Swap, ckecks the value of trade keyword 'MatchingStatus'. In case
     * value = 'Matched' -> return 'true' otherwise 'false' - if msg belongs to
     * the FarLeg of the Swap, checks the value of the trade keyword
     * 'FarMatchingStatus'. In case value = 'Matched' return 'true' otherwise
     * 'false'
     *
     * @param transfer
     * @param message
     * @param dsCon
     * @param excps
     * @param task
     * @param trade
     * @return
     */
    @SuppressWarnings("unchecked")
    public boolean isRelatedConfirmationMatched(final BOTransfer transfer,
                                                final BOMessage message, final DSConnection dsCon,
                                                @SuppressWarnings("rawtypes") final Vector excps, final Task task,
                                                final Trade trade) {

        boolean bReturn = true;
        try {

            BOTransfer trans = DSConnection.getDefault().getRemoteBO()
                    .getBOTransfer(message.getTransferLongId());
            Trade tradeFX = DSConnection.getDefault().getRemoteTrade()
                    .getTrade(message.getTradeLongId());

            if (!isDataServer()) {
                trans = dsCon.getRemoteBO().getBOTransfer(
                        message.getTransferLongId());
                tradeFX = dsCon.getRemoteTrade().getTrade(message.getTradeLongId());

            } else {
                Log.debug(WorkflowUtil.class,
                        "_isDataServer = true, then call DS API");
                trans = BOTransferSQL.getTransfer(message.getTransferLongId());
                tradeFX = TradeSQL.getTrade(message.getTradeLongId());
            }

            if (trans != null) {
                if (trans.getNettedTransfer()) {
                    bReturn = isNettedTranferMatched(transfer,
                            DSConnection.getDefault(), new Task());
                } else {
                    bReturn = isTransferMatched(transfer, tradeFX);
                }
            }
            Log.info("calypsox.tk.util.StatusVerifiedAndMatchedUtil",
                    "StatusVerifiedAndMatchedUtil.bCheckSwapMatched End returning "
                            + bReturn);
        } catch (final Exception e) {
            Log.error(WorkflowUtil.class,
                    "StatusVerifiedAndMatchedUtil Exception", e);
            final BOException ev = new BOException(message.getTradeLongId(),
                    "StatusVerifiedAndMatchedUtil", "Error :" + e.getMessage());
            ev.setType(BOException.EXCEPTION);
            excps.addElement(ev);
            return false;
        }
        return bReturn;
    }
}
