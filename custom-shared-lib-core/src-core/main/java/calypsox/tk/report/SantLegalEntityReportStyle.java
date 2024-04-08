/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import static calypsox.tk.report.SantLegalEntityReportTemplate.ISO_COUNTRY;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.Country;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

public class SantLegalEntityReportStyle extends com.calypso.tk.report.LegalEntityReportStyle {

	private static final long serialVersionUID = 1L;
	public static final String TYPE = "LegalEntity";

	@Override
	@SuppressWarnings("rawtypes")
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

		if (columnName.equals(ISO_COUNTRY)) {
			final DSConnection dsConn = DSConnection.getDefault();
			LegalEntity le = (LegalEntity) row.getProperty(TYPE);
			String countryName = "";

			if (!le.getCountry().equals("NONE")) {
				countryName = le.getCountry();
				Country country = new Country();
				try {
					country = BOCache.getCountry(dsConn, countryName);
				} catch (Exception e) {
					Log.error(this, "Error Extractin ISO Country from " + countryName + ": ", e);
				}
				return country.getISOCode();
			}
			return "";
		} else {
			return super.getColumnValue(row, columnName, errors);
		}
	}

	@Override
	public TreeList getTreeList() {
		@SuppressWarnings("deprecation")
		final TreeList treeList = super.getTreeList();
		treeList.add(ISO_COUNTRY);
		return treeList;
	}

}
