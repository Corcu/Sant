/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.rmi.RemoteException;
import java.security.InvalidParameterException;
import java.util.Vector;

import calypsox.tk.core.MarginCallConfigLight;
import calypsox.util.SantReportingUtil;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.core.Log;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

public class SantInventoryViewReportStyle extends calypsox.tk.report.BOSecurityPositionReportStyle {

	public static final String[] DEFAULT_COLUMNS = {};

	private TreeList treeList = null;
	
	private static final long serialVersionUID = 6881928559332741996L;

	@Override
	public TreeList getTreeList() {
		if (this.treeList != null) {
			return this.treeList;
		}
		@SuppressWarnings("unused")
		Vector<MarginCallConfigLight> mccLights = null;
		try {
			mccLights = SantReportingUtil.getSantReportingService(DSConnection.getDefault())
					.getMarginCallConfigsLight();
		} catch (RemoteException e) {
			Log.error(this, e); //sonar
		}

		// TODO - we'll see later if we need it

		this.treeList = super.getTreeList();

		//
		// TreeList eligibleTreeList = new TreeList("eligibleContracts");
		//
		// for (MarginCallConfigLight mccLight : mccLights) {
		// eligibleTreeList.add(mccLight.getDescription());
		// }
		// this.treeList.add(eligibleTreeList);

		return this.treeList;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {

		return super.getColumnValue(row, columnId, errors);

	}

}
