/**
 *
 */
package calypsox.tk.util;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.refdata.sdfilter.SDFilterOperatorType;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTaskACC_TRANSFER_INTEREST;
import com.calypso.tk.util.TaskArray;

import java.util.Hashtable;
import java.util.Vector;

/**
 * @author aalonsop
 * @description Created in Calypso v12 to v14.4 migration. Same functionality as
 *              the core one, but adding one function, sets the InterestBearing
 *              isClient keyword to false. This conditions are needed in v14 to
 *              generate de Interest transfers.
 *
 * @version 2.0 Added an StaticDataFilter to account search
 *
 */
public class ScheduledTaskSANT_ACCOUNT_TRANSFER_INTEREST extends ScheduledTaskACC_TRANSFER_INTEREST {

    public static final String SDF_ACCOUNT_PO = "Accounts SDF";
    private static final String ACCOUNT_ID = "ACCOUNT ID";
    private static final String ACCOUNT_ID_LOWER_CASE = "Account Id";
    private static final String ALL_ACC_WHERE = "acc_type = 'SETTLE' AND automatic_b = 0 AND interest_bearing = 1";
    private static final long serialVersionUID = 5479629084247762635L;

    // SuperClass uses the old attribute definition so this must use it too
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Vector getDomainAttributes() {
        final Vector v = super.getDomainAttributes();
        v.add(SDF_ACCOUNT_PO);
        return v;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Vector getAttributeDomain(final String attr, final Hashtable currentAttr) {
        Vector vector = new Vector();
        if (attr.equals(SDF_ACCOUNT_PO)) {
            try {
                vector.add("");
                vector.addAll(DSConnection.getDefault().getRemoteReferenceData().getStaticDataFilterNames());
            } catch (final CalypsoServiceException e) {
                Log.error(this.getClass(), "Error while retrieving SDF names", e);
            }
        } else {
            vector = super.getAttributeDomain(attr, currentAttr);
        }
        return vector;
    }

    @Override
    public String getTaskInformation() {
        return "This Scheduled Task generates payement transfers for the list of the given accounts  \n";
    }

    @Override
    protected String handleAccTransferInterest(DSConnection ds, PSConnection ps, TaskArray tasks) {
        String exec = null;
        Vector<Account> filteredAccounts = filterAccounts(getAttribute(SDF_ACCOUNT_PO), ds);
        exec = processTransferInterest(ds, ps, tasks, filteredAccounts);
        return exec;
    }

    /**
     *
     * @param sdfName
     * @param ds
     * @return The filtered accounts
     */
    @SuppressWarnings("unchecked")
    protected Vector<Account> filterAccounts(String sdfName, DSConnection ds) {
        StaticDataFilter sdf = setAccountIdINToSDF(BOCache.getStaticDataFilter(ds, sdfName));
        Vector<Account> filteredAccounts = new Vector<>();
        if (sdf != null) {
            try {
                Vector<Account> allAccounts = ds.getRemoteAccounting().getAccounts(ALL_ACC_WHERE, null);
                if (!Util.isEmpty(allAccounts)) {
                    for (Account acc : allAccounts) {
                        if (sdf.accept(null, null, null, null, null, acc)) {
                            filteredAccounts.addElement(acc);
                        }
                    }
                }
            } catch (CalypsoServiceException e) {
                Log.error(this.getClass(),
                        "Exception ocurred while trying to retrieve all accounts from DS: " + e.getLocalizedMessage());
                Log.error(this, e); //sonar
            }
        }
        return filteredAccounts;
    }

    /**
     * If an Account is selected in the ST atribute adds it to the
     * StaticDataFilter
     *
     * @param sdf
     * @return sdf
     */
    protected StaticDataFilter setAccountIdINToSDF(StaticDataFilter sdf) {
        String accId = getAttribute(ACCOUNT_ID);
        if (accId != null && !accId.isEmpty()) {
            StaticDataFilterElement element = new StaticDataFilterElement(ACCOUNT_ID_LOWER_CASE);
            element.setOperatorType(SDFilterOperatorType.INT_ENUM);
            Vector<String> values = new Vector<>();
            values.add(accId);
            element.setValues(values);
            if (sdf != null)
                sdf.add(element);
            else {
                sdf = new StaticDataFilter(ACCOUNT_ID, element);
            }
        }
        return sdf;
    }

    /**
     *
     * @param ds
     * @param ps
     * @param tasks
     * @param filteredAccounts
     * @return
     */
    protected String processTransferInterest(DSConnection ds, PSConnection ps, TaskArray tasks,
                                             Vector<Account> filteredAccounts) {
        String exec = null;
        for (Account acc : filteredAccounts) {
            this.setAttribute(ACCOUNT_ID, String.valueOf(acc.getId()));
            exec = super.handleAccTransferInterest(ds, ps, tasks);
        }
        return exec;
    }
}
