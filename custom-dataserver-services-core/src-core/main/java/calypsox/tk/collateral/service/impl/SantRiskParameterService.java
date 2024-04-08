/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.collateral.service.impl;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.sql.JResultSet;
import com.calypso.tk.core.sql.ioSQL;

import calypsox.tk.collateral.service.LocalSantRiskParameterService;
import calypsox.tk.collateral.service.RemoteSantRiskParameterService;
import calypsox.tk.util.riskparameters.SantRiskParameter;

@Stateless(name="calypsox.tk.collateral.service.RemoteSantRiskParameterService")
@Remote(RemoteSantRiskParameterService.class)
@Local(LocalSantRiskParameterService.class)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class SantRiskParameterService implements RemoteSantRiskParameterService,LocalSantRiskParameterService {

	private static final StringBuilder INSERT_SQL = new StringBuilder();

	static {
		INSERT_SQL.append("INSERT INTO san_risk_parameter");
		INSERT_SQL
				.append("(contract_id,val_date,contract_name,contract_currency,po_threshold_currency_risk_1,po_threshold_currency_risk_2,po_threshold_currency_risk_3,po_threshold_type,po_threshold,po_threshold_currency,po_mta_currency_risk_1,po_mta_currency_risk_2,po_mta_currency_risk_3,po_mta_type,po_mta,po_mta_currency,po_rounding,le_threshold_currency_risk_1,le_threshold_currency_risk_2,le_threshold_currency_risk_3,le_threshold_type,le_threshold,le_threshold_currency,le_mta_currency_risk_1,le_mta_currency_risk_2,le_mta_currency_risk_3,le_mta_type,le_mta_amount,le_mta_currency,le_rounding)");
		INSERT_SQL.append("VALUES");
		INSERT_SQL.append("(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
	}

	@Override
	public void save(List<SantRiskParameter> rpList) throws RemoteException {

		Connection con = null;
		PreparedStatement stmt = null;
		try {
			con = ioSQL.getConnection();
			stmt = ioSQL.newPreparedStatement(con, INSERT_SQL.toString());
			int i = 1;
			for (SantRiskParameter rp : rpList) {

				// Generic Fields
				stmt.setInt(i++, rp.getContractId());
				stmt.setDate(i++, new Date(rp.getValDate().getDate(null).getTime()));
				stmt.setString(i++, rp.getCollateralAgreement());
				stmt.setString(i++, rp.getCurrencyAgreement());
				// PO Threshold Fields
				stmt.setString(i++, rp.getPoThresholdRiskLevel1());
				stmt.setString(i++, rp.getPoThresholdRiskLevel2());
				stmt.setString(i++, rp.getPoThresholdRiskLevel3());
				stmt.setString(i++, rp.getPoThresholdType());
				stmt.setString(i++, rp.getPoThreshold());
				stmt.setString(i++, rp.getPoThresholdCurrency());
				// PO MTA Fields
				stmt.setString(i++, rp.getPoMTARiskLevel1());
				stmt.setString(i++, rp.getPoMTARiskLevel2());
				stmt.setString(i++, rp.getPoMTARiskLevel3());
				stmt.setString(i++, rp.getPoMTAType());
				stmt.setString(i++, rp.getPoMTA());
				stmt.setString(i++, rp.getPoMTACurrency());
				// PO Rounding
				stmt.setString(i++, rp.getPoRounding());
				// CPTY Threshold Fields
				stmt.setString(i++, rp.getCptyThresholdRiskLevel1());
				stmt.setString(i++, rp.getCptyThresholdRiskLevel2());
				stmt.setString(i++, rp.getCptyThresholdRiskLevel3());
				stmt.setString(i++, rp.getCptyThresholdType());
				stmt.setString(i++, rp.getCptyThreshold());
				stmt.setString(i++, rp.getCptyThresholdCurrency());
				// CPTY MTA Fields
				stmt.setString(i++, rp.getCptyMTARiskLevel1());
				stmt.setString(i++, rp.getCptyMTARiskLevel2());
				stmt.setString(i++, rp.getCptyMTARiskLevel3());
				stmt.setString(i++, rp.getCptyMTAType());
				stmt.setString(i++, rp.getCptyMTA());
				stmt.setString(i++, rp.getCptyMTACurrency());
				// CPTY Rounding
				stmt.setString(i++, rp.getCptyRounding());

				stmt.executeUpdate();
				i = 1;
			}

			ioSQL.commit(con);

		} catch (final Exception e) {
			Log.error(this, e);
			ioSQL.rollback(con);
			throw new RemoteException("SQL error: " + e.getMessage(), e);
		} finally {
			ioSQL.close(stmt);
			ioSQL.releaseConnection(con);
		}

	}

	@Override
	public List<SantRiskParameter> get(String sqlQuery) throws RemoteException {
		List<SantRiskParameter> rpList = new ArrayList<SantRiskParameter>();
		Connection con = null;
		Statement stmt = null;
		try {
			con = ioSQL.getConnection();

			stmt = ioSQL.newStatement(con);
			final JResultSet rs = new JResultSet(stmt.executeQuery(sqlQuery));

			int i = 1;
			while (rs.next()) {
				SantRiskParameter rp = new SantRiskParameter();
				// Generic Fields
				rp.setContractId(rs.getInt(i++));
				rp.setValDate(rs.getJDate(i++));
				rp.setCollateralAgreement(rs.getString(i++));
				rp.setCurrencyAgreement(rs.getString(i++));
				// PO Threshold Fields
				rp.setPoThresholdRiskLevel1(rs.getString(i++));
				rp.setPoThresholdRiskLevel2(rs.getString(i++));
				rp.setPoThresholdRiskLevel3(rs.getString(i++));
				rp.setPoThresholdType(rs.getString(i++));
				rp.setPoThreshold(rs.getString(i++));
				rp.setPoThresholdCurrency(rs.getString(i++));
				// PO MTA Fields
				rp.setPoMTARiskLevel1(rs.getString(i++));
				rp.setPoMTARiskLevel2(rs.getString(i++));
				rp.setPoMTARiskLevel3(rs.getString(i++));
				rp.setPoMTAType(rs.getString(i++));
				rp.setPoMTA(rs.getString(i++));
				rp.setPoMTACurrency(rs.getString(i++));
				// PO Rounding
				rp.setPoRounding(rs.getString(i++));
				// CPTY Threshold Fields
				rp.setCptyThresholdRiskLevel1(rs.getString(i++));
				rp.setCptyThresholdRiskLevel2(rs.getString(i++));
				rp.setCptyThresholdRiskLevel3(rs.getString(i++));
				rp.setCptyThresholdType(rs.getString(i++));
				rp.setCptyThreshold(rs.getString(i++));
				rp.setCptyThresholdCurrency(rs.getString(i++));
				// CPTY MTA Fields
				rp.setCptyMTARiskLevel1(rs.getString(i++));
				rp.setCptyMTARiskLevel2(rs.getString(i++));
				rp.setCptyMTARiskLevel3(rs.getString(i++));
				rp.setCptyMTAType(rs.getString(i++));
				rp.setCptyMTA(rs.getString(i++));
				rp.setCptyMTACurrency(rs.getString(i++));
				// CPTY Rounding
				rp.setCptyRounding(rs.getString(i++));

				rpList.add(rp);
				i = 1;
			}
		} catch (final Exception e) {
			Log.error(this, e);
			throw new RemoteException("SQL error: " + e.getMessage(), e);
		} finally {
			ioSQL.close(stmt);
			ioSQL.releaseConnection(con);
		}

		return rpList;
	}

}
