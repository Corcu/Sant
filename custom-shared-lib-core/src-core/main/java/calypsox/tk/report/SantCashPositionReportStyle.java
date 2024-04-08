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

import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.InventoryCashPosition;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

public class SantCashPositionReportStyle extends SantCollateralPositionReportStyle {

	private static final long serialVersionUID = 3042186745929896861L;

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {
		Object columnValue = null;

		final InventoryCashPosition cashPosition = (InventoryCashPosition) row.getProperty(SantCashPositionReport.DEFAULT);

		if (SantCashPositionReportTemplate.COLUMN_NAME_DATE.equals(columnName)) {
			
			columnValue = row.getProperty(SantCashPositionReport.POS_DATE);
			
		} else if (SantCashPositionReportTemplate.COLUMN_NAME_CALL_ACCOUNT.equals(columnName)) {
			
			Account account = getAccountFromPosition(cashPosition, errors);
			if (account != null) {
				// GSM: Call account fix
				columnValue = account.getExternalName();
			}
		} else if (SantCashPositionReportTemplate.COLUMN_NAME_AMOUNT.equals(columnName)) {
			// GSM: fix amount and format CashPos, incidence INC_COLLAT_0735
			String faceAmountDateBO = getBOFaceAmountDateColumnNme(cashPosition);
			if (faceAmountDateBO == null) {
				columnValue = new Amount(cashPosition.getTotal());
			} else { 
				// call columnData to com.calypso.tk.report.BOSecurityPositionReportStyle
				columnValue = super.getColumnValue(row, faceAmountDateBO, errors);
			}

		} else if (SantCashPositionReportTemplate.COLUMN_NAME_AGENT.equals(columnName)) {
			columnValue = cashPosition.getAgent().getCode();
			
		} else if (SantCashPositionReportTemplate.COLUMN_NAME_BOOK.equals(columnName)) {
			columnValue = cashPosition.getBook().getName();
			
		} else if (SantCashPositionReportTemplate.COLUMN_NAME_POSITION_TYPE.equals(columnName)) {
			//v14 Mig - GSM, correction in mapping position status
			if (BOCashPositionReport.FAILED.equals(BOCashPositionReport.getPositionTypeMapping(cashPosition.getPositionType()))) {
				return "In Transit";
			} else if (BOCashPositionReport.ACTUAL.equals(BOCashPositionReport.getPositionTypeMapping(cashPosition.getPositionType()))) {
				return "Held";
			}
		}

		else {
			columnValue = super.getColumnValue(row, columnName, errors);
		}

		return columnValue;
	}

	@Override
	public TreeList getTreeList() {
		TreeList treeList = super.getTreeList();

		treeList.add(SantCashPositionReportTemplate.COLUMN_NAME_DATE);
		treeList.add(SantCashPositionReportTemplate.COLUMN_NAME_CALL_ACCOUNT);
		treeList.add(SantCashPositionReportTemplate.COLUMN_NAME_AMOUNT);
		treeList.add(SantCashPositionReportTemplate.COLUMN_NAME_POSITION_TYPE);
		treeList.add(SantCashPositionReportTemplate.COLUMN_NAME_AGENT);
		treeList.add(SantCashPositionReportTemplate.COLUMN_NAME_BOOK);

		return treeList;
	}

	/**
	 * @param cashPosition
	 *            from which the columnName is going to be generated
	 * @return a columnName with the format 18-ene-2012_Face Amount, input columnName of
	 *         com.calypso.tk.report.BOSecurityPositionReportStyle.
	 */
	private String getBOFaceAmountDateColumnNme(InventoryCashPosition cashPosition) {

		if ((cashPosition == null) || (cashPosition.getPositionDate() == null)) {
			return null;
		}

		JDate posDate = cashPosition.getPositionDate();
		String dateString = Util.dateToMString(posDate);

		return (dateString + "_" + SantCollateralPositionReportStyle.FACE_AMOUNT);
	}

	@SuppressWarnings("unchecked")
	private Account getAccountFromPosition(InventoryCashPosition cashPosition,
			@SuppressWarnings("rawtypes") Vector errors) {
		Account accountToReturn = null;

		try {
			@SuppressWarnings("rawtypes")
			//v14 Mig - fix MC id from position
			Vector accounts = DSConnection.getDefault().getRemoteAccounting()
					.getAccountByAttribute("MARGIN_CALL_CONTRACT", "" + cashPosition.getMarginCallConfigId());
			MarginCallConfig mcc = DSConnection.getDefault().getRemoteReferenceData()
					.getMarginCallConfig(cashPosition.getMarginCallConfigId());
			if ((mcc != null) && (accounts != null) && (accounts.size() > 0)) {
				for (int i = 0; (i < accounts.size()) && (accountToReturn == null); i++) {
					final Account account = (Account) accounts.get(i);
					// GSM: fix to solve last condition: was taking name, not external. Number accounts were failing
					if (account.getCurrency().equals(cashPosition.getCurrency()) && !Util.isEmpty(account.getName())
							&& !Util.isEmpty(mcc.getName()) && account.getExternalName().contains(mcc.getName())) {
						accountToReturn = account;
					}
				}
			}
		} catch (RemoteException e) {
			Log.error(this, "Could not load data from database", e);
			errors.add(e.getMessage());
		}

		return accountToReturn;
	}
}
