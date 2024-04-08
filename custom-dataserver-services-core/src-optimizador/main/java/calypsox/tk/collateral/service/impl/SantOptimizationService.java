package calypsox.tk.collateral.service.impl;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.PersistenceException;
import com.calypso.tk.core.Util;
import com.calypso.tk.core.sql.JResultSet;
import com.calypso.tk.core.sql.SeedAllocSQL;
import com.calypso.tk.core.sql.ioSQL;

import calypsox.tk.collateral.allocation.optimizer.importer.OptimAllocsImportConstants;
import calypsox.tk.collateral.service.LocalSantOptimizationService;
import calypsox.tk.optimization.service.RemoteSantOptimizationService;

/**
 * Custom remote services for the optimization module
 * 
 * @author Guillermo Solano
 * 
 */
@Stateless(name="calypsox.tk.optimization.service.RemoteSantOptimizationService")
@Remote(RemoteSantOptimizationService.class)
@Local(LocalSantOptimizationService.class)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class SantOptimizationService implements RemoteSantOptimizationService, LocalSantOptimizationService {

	/**
	 * Get haircut close_quote from products received as ids
	 * 
	 */
	@Override
	public Map<Integer, Double> getProductHaircutQuoteMap(Vector<Integer> productIds, String quoteSetName, JDate valDate)
			throws RemoteException {

		final Map<Integer, Double> results = new HashMap<Integer, Double>();
		Connection con = null;
		Statement stmt = null;

		// query
		final String sqlQuery = "select product_id, close_quote from quote_value, product_desc where quote_set_name = "
				+ Util.string2SQLString(quoteSetName)
				+ " and quote_value.quote_name = product_desc.quote_name and product_id IN "
				+ Util.collectionToSQLString(productIds) + " and trunc(quote_date) = " + Util.date2SQLString(valDate);

		try {
			con = ioSQL.getConnection();
			stmt = ioSQL.newStatement(con);
			final JResultSet rs = new JResultSet(stmt.executeQuery(sqlQuery));

			int fieldIndex = 1;
			while (rs.next()) {
				fieldIndex = 1;
				results.put(rs.getInt(fieldIndex++), rs.getDouble(fieldIndex++));
			}
		} catch (final Exception e) {
			Log.error(this, e);
			throw new RemoteException("SQL error: " + e.getMessage(), e);
		} finally {
			ioSQL.close(stmt);
			ioSQL.releaseConnection(con);
		}
		return results;
	}

	
	
	/* (non-Javadoc)
	 * @see calypsox.tk.optimization.service.RemoteSantOptimizationService#getAllContractNamesForIds()
	 */
	@Override
	public Map<String, Double> getAllContractNamesForIds()
			throws RemoteException {
		
		final Map<String, Double> results = new HashMap<String, Double>();
		Connection con = null;
		Statement stmt = null;
		final String sqlQuery = "select description, mrg_call_def from mrgcall_config";

		try {
			con = ioSQL.getConnection();
			stmt = ioSQL.newStatement(con);
			final JResultSet rs = new JResultSet(stmt.executeQuery(sqlQuery));

			int fieldIndex = 1;
			while (rs.next()) {
				fieldIndex = 1;
				results.put(rs.getString(fieldIndex++), rs.getDouble(fieldIndex++));
			}
		} catch (final Exception e) {
			Log.error(this, e);
			throw new RemoteException("SQL error: " + e.getMessage(), e);
		} finally {
			ioSQL.close(stmt);
			ioSQL.releaseConnection(con);
		}
		return results;
	}
	
	/* (non-Javadoc)
	 * @see calypsox.tk.optimization.service.RemoteSantOptimizationService#getOptimImportExecutionID()
	 */
	@Override
	public Long getOptimImportExecutionID(long defaultExecId) throws RemoteException {
		
		Long execId = null;
		Connection con = null;
		Statement stmt = null;
		final String sqlQuery = "select nvl(max(link_id),0) from bo_task where event_type like '"+OptimAllocsImportConstants.TASK_EXCEPTION_TYPE+"%' and link_id>="+defaultExecId;

		try {
			con = ioSQL.getConnection();
			stmt = ioSQL.newStatement(con);
			final JResultSet rs = new JResultSet(stmt.executeQuery(sqlQuery));

			int fieldIndex = 1;
			while (rs.next()) {
				fieldIndex = 1;
				execId = rs.getLong(fieldIndex++);
			}
		} catch (final Exception e) {
			Log.error(this, e);
			throw new RemoteException("SQL error: " + e.getMessage(), e);
		} finally {
			ioSQL.close(stmt);
			ioSQL.releaseConnection(con);
		}
		return execId;
	}

	/* (non-Javadoc)
	 * @see calypsox.tk.optimization.service.RemoteSantOptimizationService#nextSeed()
	 */
	@Override
	public Integer nextSeed(String seedName) throws RemoteException {
		try {
			return SeedAllocSQL.nextSeed(seedName);
		} catch (PersistenceException e) {
			Log.error(SantOptimizationService.class.getName(), e);
			throw new RemoteException("SQL error: " + e.getMessage(), e);
		}
	}
}
