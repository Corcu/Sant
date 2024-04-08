package calypsox.tk.report;

import java.util.Vector;

import org.jfree.util.Log;

import com.calypso.apps.util.TreeList;
import com.calypso.infra.util.Util;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

public class AccountReportStyle extends com.calypso.tk.report.AccountReportStyle {

	private final String column = "Contract Name";

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public Object getColumnValue(final ReportRow row, final String columName, final Vector errors) {

		if ("Contract Name".equals(columName)) {
			return getContractName(row);
		}

		return super.getColumnValue(row, columName, errors);
	}

	@Override
	public TreeList getTreeList() {
		final TreeList treeList = super.getTreeList();
		treeList.add(this.column);
		return treeList;

	}
	
	
	public String getContractName(final ReportRow row) {
		String contractName = "";
		int mrgCallInt = 0;
		Account linea = row.getProperty("Account");
		String attrValue = linea.getAccountProperty("MARGIN_CALL_CONTRACT");
		if (!Util.isEmpty(attrValue)) { 
		mrgCallInt = Integer.valueOf(attrValue);
		}
		if (mrgCallInt > 0) {
			try {
				contractName = DSConnection.getDefault().getRemoteReferenceData().getMarginCallConfig(mrgCallInt)
						.getName();
			} catch (CalypsoServiceException e) {
				Log.error(this, e);

			}
		}

		return contractName;
	}

}