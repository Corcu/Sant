/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.product.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.core.AuditValue;
import com.calypso.tk.core.DBVersionMismatchException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.PersistenceException;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.core.sql.AuditSQL;
import com.calypso.tk.core.sql.CacheStatement;
import com.calypso.tk.core.sql.DeadLockException;
import com.calypso.tk.core.sql.ProductCustomDataSQL;
import com.calypso.tk.core.sql.ProductSQL;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.product.Security;
import com.calypso.tk.refdata.RateIndex;

/**
 * Custom Data Layer for products.
 * 
 * @author OA
 * 
 */
public final class CustomProductSQL extends ProductSQL {

	/**
	 * ################################ CONSTANTS ##################################
	 */

	/**
	 * Statement to remove a product's sec codes.
	 */
	private static final String S_REMOVE_SEC_CODES_STMT = "DELETE from product_sec_code WHERE sec_code = ? and product_id = ?";

	/**
	 * Statement to add a product's sec code.
	 */
	private static final String S_SAVE_SEC_CODES_STMT = "INSERT INTO product_sec_code(product_id, sec_code, "
			+ "code_value, code_value_ucase) VALUES(?, ?, ?, ?)";

	private static final String S_UPDATE_PRODDESC_STMT_WITHOUT_VERSION = "UPDATE product_desc SET description = ?, "
			+ "product_type = ?, product_family = ?, product_sub_type = ?, product_extended_type = ?, currency = ?, quote_name = ?, "
			+ "rate_index = ?, needs_reset_b = ?, maturity_date = ?, custom_data = ?, version_num = ?, "
			+ "und_security_id = ? , issuer_id = ?, pricer_override_key = ?, mdi_override_key = ? WHERE product_id = ?";

	private static final String S_UPDATE_PRODDESC_STMT_WITH_VERSION = S_UPDATE_PRODDESC_STMT_WITHOUT_VERSION
			+ " AND version_num = ?";

	private static final String S_REMOVE_SEC_CODES = "DELETE from product_sec_code WHERE product_id = ?";

	/**
	 * ################################ PUBLIC METHODS ##################################
	 */

	/**
	 * Clear a sec code value of products. The aim of this method is to have a batch update of sec codes.
	 * 
	 * @param products
	 *            the products to update
	 * @param con
	 *            a connection to DB
	 * @throws DeadLockException
	 *             dead lock exception
	 * @throws PersistenceException
	 *             generic persistence exception
	 */
	public static void clearSecCodes(Vector<Product> products, String secCode, Connection con)
			throws DeadLockException, PersistenceException {
		try {
			for (Product product : products) {
				Product old = ProductSQL.getProduct(product.getType(), product.getId(), con);
				if ((old != null) && (old.getVersion() != product.getVersion())) {
					throw new DBVersionMismatchException("Can not save product type="
							+ product.getClass().getSimpleName() + ", id=" + product.getId() + ", version="
							+ product.getVersion() + " due to version mismatch.");
				}
				doAudit(product, old, con);
			}
			removeSecCodesBatch(products, secCode, con);
			updateDescriptionBatch(products, false, false, con);

		} catch (PersistenceException pe) {
			throw pe;
		}
		ProductSQL.putInCache(products);
	}

	/**
	 * Clear a sec code value of products. The aim of this method is to have a batch update of sec codes.
	 * 
	 * @param products
	 *            the products to update
	 * @param con
	 *            a connection to DB
	 * @throws DeadLockException
	 *             dead lock exception
	 * @throws PersistenceException
	 *             generic persistence exception
	 */
	public static void updateBatch(Vector<Product> products, Connection con) throws DeadLockException,
			PersistenceException {
		try {
			for (Product product : products) {
				Product old = ProductSQL.getProduct(product.getType(), product.getId(), con);
				if ((old != null) && (old.getVersion() != product.getVersion())) {
					throw new DBVersionMismatchException("Can not save product type="
							+ product.getClass().getSimpleName() + ", id=" + product.getId() + ", version="
							+ product.getVersion() + " due to version mismatch.");
				}
				doAudit(product, old, con);
			}
			updateDescriptionBatch(products, true, false, con);

		} catch (PersistenceException pe) {
			throw pe;
		}
		ProductSQL.putInCache(products);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected static boolean updateDescriptionBatch(Vector products, boolean updateSecCodes, boolean updateCustomData,
			Connection con) throws PersistenceException, DeadLockException {
		Vector productsWith2ndaryMarket = null;
		Vector productsWithout2ndaryMarket = null;

		for (int i = 0, len = products.size(); i < len; i++) {
			Product p = (Product) products.get(i);
			if (p.hasSecondaryMarket()) {
				if (productsWith2ndaryMarket == null) {
					productsWith2ndaryMarket = new Vector();
				}
				productsWith2ndaryMarket.add(p);
			} else {
				if (productsWithout2ndaryMarket == null) {
					productsWithout2ndaryMarket = new Vector();
				}
				productsWithout2ndaryMarket.add(p);
			}
		}

		if (productsWith2ndaryMarket != null) {
			internalUpdateDescriptionBatch(productsWith2ndaryMarket, true, updateSecCodes, updateCustomData, con);
		}

		if (productsWithout2ndaryMarket != null) {
			internalUpdateDescriptionBatch(productsWithout2ndaryMarket, false, updateSecCodes, updateCustomData, con);
		}

		return true;
	}

	
	static private void lockProductsForUpdate(List<Product> products, Connection con) throws DeadLockException, SQLException,
			DBVersionMismatchException {
		// One issue with executeBatch. Its return value can contain SUCCESS_NO_INFO
		// which means we do not really know if the update succeed or not.
		// It's possible that the version was updated by others and DB return SUCCESS_NO_INFO
		// when no record was updated. I.e. we cannot detect DB version mismatch.
		// To address this problem, we first do an update to lock the records of the
		// specific version that we want to update. Calling executeUpdate will give us
		// the exact number of rows updated (and lock them). If the number is not right,
		// we can throw a DBVersionMismatchException.
		//
		// Lock the records we are updating by issuing
		//
		// UPDATE bo_cre set version_num = version
		// WHERE (bo_cre_id=? AND version_num=?) OR (bo_cre_id=? AND version_num=?)...

		// We allow checking ioSQL.MAX_ITEMS_IN_LIST/2 in one update statement.
		PreparedStatement updateSelectStmt = null;
		PreparedStatement updateLastStmt = null;

		try {
			final String dbVersionMismatchStr = "Number of products updated does not match the number to be updated due to version mismatch.";
			final String keyCompareCond = "(product_id=? AND version_num=?)";
			final String tableNameStr = "product_desc";

			final String orSeparator = " OR ";
			final String updateLockStr = "UPDATE " + tableNameStr + " SET version_num=version_num WHERE ";

			int lockCount = 0;
			int allowCnt = ioSQL.MAX_ITEMS_IN_LIST / 2;
			int l = products.size();

			StringBuffer stBuf = new StringBuffer();

			if (allowCnt >= l) {
				for (int i = 0; i < l; i++) {
					if (i > 0) {
						stBuf.append(orSeparator);
					}
					stBuf.append(keyCompareCond);
				}

				String selectStmtStr = updateLockStr + stBuf.toString();
				updateSelectStmt = CacheStatement.getPrepared(con, selectStmtStr);

				for (int i = 0; i < l; i++) {
					Product event = (Product) products.get(i);
					updateSelectStmt.setLong((i * 2) + 1, event.getId());
					updateSelectStmt.setInt((i * 2) + 2, event.getVersion() - 1);
				}
				lockCount = updateSelectStmt.executeUpdate();
				if (lockCount != l) {
					throw new DBVersionMismatchException(dbVersionMismatchStr);
				}
			} else {
				for (int i = 0; i < allowCnt; i++) {
					if (i > 0) {
						stBuf.append(orSeparator);
					}
					stBuf.append(keyCompareCond);
				}
				String selectStmtStr = updateLockStr + stBuf.toString();

				int lastCnt = l % allowCnt;

				String lastSelectStmtStr = null;

				if (lastCnt > 0) {
					stBuf = new StringBuffer();
					for (int i = 0; i < lastCnt; i++) {
						if (i > 0) {
							stBuf.append(orSeparator);
						}
						stBuf.append(keyCompareCond);
					}
					lastSelectStmtStr = updateLockStr + stBuf.toString();
				}

				updateSelectStmt = CacheStatement.getPrepared(con, selectStmtStr);
				int completeCnt = l / allowCnt;
				for (int i = 0; i < completeCnt; i++) {
					for (int j = 0; j < allowCnt; j++) {
						Product event = (Product) products.get((i * allowCnt) + j);
						updateSelectStmt.setLong((j * 2) + 1, event.getId());
						// The version passed in are already
						// increased by 1. need to change it to
						// the version in the DB.
						updateSelectStmt.setInt((j * 2) + 2, event.getVersion() - 1);
					}
					lockCount = updateSelectStmt.executeUpdate();

					if (lockCount != allowCnt) {
						throw new DBVersionMismatchException(dbVersionMismatchStr);
					}
				}

				if (lastCnt > 0) {
					updateLastStmt = CacheStatement.getPrepared(con, lastSelectStmtStr);
					for (int i = 0; i < lastCnt; i++) {
						Product event = (Product) products.get((completeCnt * allowCnt) + i);
						updateLastStmt.setLong((i * 2) + 1, event.getId());
						updateLastStmt.setInt((i * 2) + 2, event.getVersion() - 1);
					}
					lockCount = updateLastStmt.executeUpdate();

					if (lockCount != lastCnt) {
						throw new DBVersionMismatchException(dbVersionMismatchStr);
					}
				}
			}
		} finally {
			CacheStatement.release(updateSelectStmt);
			if (updateLastStmt != null) {
				CacheStatement.release(updateLastStmt);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private static boolean internalUpdateDescriptionBatch(Vector products, boolean hasScondaryMarket,
			boolean updateSecCodes, boolean updateCustomData, Connection con) throws PersistenceException,
			DeadLockException {
		PreparedStatement stmt = null;
		int count = 0;
		@SuppressWarnings("unused")
		int colNum = 0;

		try {

			if (hasScondaryMarket && ioSQL.isOracle()) {
				// Only when a product hasScondaryMarket then we need to lock versions.
				// Else the version is controled by the trade and we do not need to lock it.
				// Oracle will return SUCCESS_NO_INFO when doing update
				// in batch and we won't know if the update really occured
				// or not.
				// lockProductsForUpdate will lock the record and throw exception
				// if version mismatch.
				lockProductsForUpdate(products, con);
			}

			String updateStatement = null;
			if (hasScondaryMarket) {
				updateStatement = S_UPDATE_PRODDESC_STMT_WITH_VERSION;
			} else {
				updateStatement = S_UPDATE_PRODDESC_STMT_WITHOUT_VERSION;
			}

			stmt = CacheStatement.getPrepared(con, updateStatement);

			for (int i = 0; i < products.size(); i++) {

				Product product = (Product) products.elementAt(i);

				colNum = populateStmtForDescUpdate(stmt, product, hasScondaryMarket);

				if (Log.isCategoryLogged(Log.SQL)) {
					trace("Batch update Product description " + product.getId());
				}
				stmt.addBatch();
				count++;
				if (ioSQL.isBatchExecuteNeeded(updateStatement, count)) {
					if (Log.isCategoryLogged(Log.SQL)) {
						trace("ExecuteBtach updateProductDescription  ---- " + count);
					}
					int[] results = stmt.executeBatch();
					for (int j = 0, len = results.length; j < len; j++) {
						if ((results[j] != 1) && (results[j] != PreparedStatement.SUCCESS_NO_INFO)) {
							Product errorProd = (Product) products.elementAt(((i + 1) - len) + j);
							throw new DBVersionMismatchException("Failed to update product id=" + errorProd.getId()
									+ "Version = " + (errorProd.getVersion() - 1));
						}
					}
					stmt.clearBatch();
					count = 0;
				}

			}
			if (Log.isCategoryLogged(Log.SQL)) {
				trace("ExecuteBtach updateProductDescription ----");
			}
			if (count > 0) {
				if (Log.isCategoryLogged(Log.SQL)) {
					trace("ExecuteBtach updateProductDescription ---- " + count);
				}
				int[] rowsAffected = stmt.executeBatch();
				for (int i = 0; i < rowsAffected.length; i++) {
					if ((rowsAffected[i] != 1) && (rowsAffected[i] != PreparedStatement.SUCCESS_NO_INFO)) {
						// update failed or no update occured.
						// Throw versoin mismatch
						// we are processing the last batch. I.e. from
						// product.size() - rowfAffected.length to
						// product.size()
						Product errorProd = (Product) products.elementAt(((products.size() + 1) - rowsAffected.length)
								+ i);
						throw new DBVersionMismatchException("Failed to update product type="
								+ errorProd.getClass().getSimpleName() + ", id=" + errorProd.getId() + ", version="
								+ errorProd.getVersion() + " due to version mismatch.");

					}
				}

				try {
					stmt.clearBatch();
				} catch (Exception eee) {
					Log.error(Log.SQL, eee);
				}
			}

		} catch (DeadLockException e) {
			throw e;
		} catch (Exception e) {
			Log.error(Log.SQL, e);
			throw new PersistenceException(e);
		} finally {
			CacheStatement.release(stmt);
		}

		// update sec codes
		if (updateSecCodes) {
			updateSecCodesBatch(products, con);
		}

		// update custom data
		if (updateCustomData) {
			for (int j = 0; j < products.size(); j++) {

				Product product = (Product) products.elementAt(j);
				if (product.getCustomData() != null) {
					ProductCustomDataSQL pcs = getCustomDataSQL(product);
					if (pcs != null) {
						pcs.update(product, product.getCustomData(), con);
					}
				}
			}
		}
		//
		// if (product.getXMLData() != null) {
		// ProductXMLDataSQL pcs = getXMLDataSQL();
		// if (pcs != null) {
		// pcs.update(product, product.getXMLData(), con);
		// }
		// }
		//
		// CreditContingencyInfoSQL.updateInfo(product, con);
		// CallInfoSQL.updateCallInfo(product, con);
		// }
		return true;
	}

	static private ProductCustomDataSQL getCustomDataSQL(Product p) {
		if ((p == null) || (p.getCustomData() == null)) {
			return null;
		}
		String s = p.getCustomData().getClass().getName();
		int idx = s.lastIndexOf(".");
		s = s.substring(idx + 1);
		return getCustomDataSQL(s);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	static private boolean updateSecCodesBatch(Vector products, Connection con) throws PersistenceException,
			DeadLockException {
		removeSecCodesBatch(products, con);
		saveSecCodesBatch(products, con);
		return true;
	}

	@SuppressWarnings("rawtypes")
	static private boolean removeSecCodesBatch(Vector prods, Connection con) throws PersistenceException,
			DeadLockException {
		PreparedStatement stmt = null;
		int count = 0;
		try {
			stmt = CacheStatement.getPrepared(con, S_REMOVE_SEC_CODES);
			for (int i = 0; i < prods.size(); i++) {

				Product prod = (Product) prods.elementAt(i);
				int productId = prod.getId();
				stmt.setInt(1, productId);

				if (Log.isCategoryLogged(Log.SQL)) {
					trace("Batch remove sec code " + productId);
				}
				stmt.addBatch();
				count++;
				if (ioSQL.isBatchExecuteNeeded(S_REMOVE_SEC_CODES, count)) {
					if (Log.isCategoryLogged(Log.SQL)) {
						trace("ExecuteBtach removeSecCode  ---- " + count);
					}
					stmt.executeBatch();
					stmt.clearBatch();
					count = 0;
				}

			}
			if (Log.isCategoryLogged(Log.SQL)) {
				trace("ExecuteBtach removeSecCode ----");
			}
			if (count > 0) {
				if (Log.isCategoryLogged(Log.SQL)) {
					trace("ExecuteBtach removeSecCode ---- " + count);
				}
				stmt.executeBatch();
				try {
					stmt.clearBatch();
				} catch (Exception eee) {
					Log.error(Log.SQL, eee);
				}
			}

		} catch (DeadLockException e) {
			throw e;
		} catch (Exception e) {
			Log.error(Log.SQL, e);
			throw new PersistenceException(e);
		} finally {
			CacheStatement.release(stmt);
		}

		return true;
	}

	private static int populateStmtForDescUpdate(PreparedStatement stmt, Product product, boolean hasSecondaryMarket)
			throws Exception {
		int i = 1;

		if (product.getDescription() != null) {
			stmt.setString(i++, product.getDescription());
		} else {
			stmt.setNull(i++, Types.VARCHAR);
		}
		if (product.getType() != null) {
			stmt.setString(i++, product.getType());
		} else {
			stmt.setNull(i++, Types.VARCHAR);
		}
		if (product.getProductFamily() != null) {
			stmt.setString(i++, product.getProductFamily());
		} else {
			stmt.setNull(i++, Types.VARCHAR);
		}
		if (product.getSubType() != null) {
			stmt.setString(i++, product.getSubType());
		} else {
			stmt.setNull(i++, Types.VARCHAR);
		}
		if (product.getExtendedType() != null) {
			stmt.setString(i++, product.getExtendedType());
		} else {
			stmt.setNull(i++, Types.VARCHAR);
		}
		if (product.getCurrency() != null) {
			stmt.setString(i++, product.getCurrency());
		} else {
			stmt.setNull(i++, Types.VARCHAR);
		}
		if (product.getQuoteName() != null) {
			stmt.setString(i++, product.getQuoteName());
		} else {
			stmt.setNull(i++, Types.VARCHAR);
		}
		RateIndex rf = product.getRateIndex();
		if (rf != null) {
			stmt.setString(i++, rf.toString());
		} else {
			stmt.setNull(i++, Types.VARCHAR);
		}
		stmt.setBoolean(i++, product.needsReset());
		JDate date = (JDate) product.getProperty("MAT_DATE");
		if (date == null) {
			date = product.getFinalPaymentMaturityDate();
		}
		if (date != null) {
			stmt.setDate(i++, toSQLDate(date));
		} else {
			stmt.setNull(i++, Types.DATE);
		}
		if (product.getCustomData() == null) {
			stmt.setNull(i++, Types.VARCHAR);
		} else {
			String s = product.getCustomData().getClass().getName();
			int idx = s.lastIndexOf(".");
			stmt.setString(i++, s.substring(idx + 1));
		}
		stmt.setInt(i++, product.getVersion());
		stmt.setInt(i++, product.getUnderlyingSecurityId()); // for under
		// security id
		int issuerId = 0;
		if (product instanceof Security) {
			issuerId = ((Security) product).getIssuerId();
		}
		stmt.setInt(i++, issuerId);

		stmt.setString(i++, product.getPricerOverrideKey());
		stmt.setString(i++, product.getMdiOverrideKey());

		stmt.setInt(i++, product.getId());

		if (hasSecondaryMarket) {
			stmt.setInt(i++, product.getVersion() - 1);
		}

		return i - 1;
	}

	// Noted that the prodClass has to be derived from Product.
	private static boolean allowAudit(Class<?> prodClass) throws PersistenceException {

		boolean rtnVal = false;
		if (Product.class.isAssignableFrom(prodClass)) {
			// If not a product class, return false.
			// else check if the class name allow audit.
			// If not check if its superclass allow audit.
			// Until we reach Product's superclass.
			rtnVal = AuditValue.allowAudit(prodClass.getSimpleName());
			if (rtnVal == false) {
				rtnVal = allowAudit(prodClass.getSuperclass());
			}
		}

		return rtnVal;
	}

	static private boolean doAudit(Product newProduct, Product oldProduct, Connection con) throws PersistenceException {

		if (allowAudit(newProduct.getClass()) == false) {
			// Do not need to call do audit but still need to handle version.
			newProduct.setVersion(oldProduct.getVersion() + 1);
			return true;
		}
		Vector<AuditValue> audits = new Vector<AuditValue>();
		newProduct.doAudit(oldProduct, audits);

		String className = newProduct.getClass().getName();
		String classSimpleName = newProduct.getClass().getSimpleName();

		if ((audits.size() == 0) && (newProduct.getVersion() == oldProduct.getVersion())) {
			AuditValue av = AuditValue.getSavedWithNoModificationAV(newProduct, newProduct.getId());
			audits.add(av);
		}

		newProduct.setVersion(oldProduct.getVersion() + 1);

		for (int i = 0; i < audits.size(); i++) {
			AuditValue av = audits.elementAt(i);

			if (av.getEntityClassName().equals(className)) {
				av.setEntityClassName(classSimpleName);
			}

			if (Util.isEmpty(av.getEntityName())) {
				av.setEntityName(newProduct.getName());
			}
			av.setVersion(newProduct.getVersion());
		}
		AuditSQL.save(audits, con);
		return true;
	}

	// private static void clearSecCodes(Vector<Product> products, Connection con) throws PersistenceException {
	// PreparedStatement stmt = null;
	// try {
	// stmt = CacheStatement.getPrepared(con, S_CLEAR_SEC_CODES_STMT);
	// String sqlTypes = buildSQLList(types);
	//
	// stmt.setString(1, secCode);
	// stmt.setString(2, sqlTypes);
	// stmt.executeUpdate();
	//
	// if (Log.isCategoryLogged(Log.SQL)) {
	// Log.debug(Log.SQL, "ExecuteBatch removeSecCode ----");
	// }
	//
	// } catch (SQLException sqle) {
	// Log.error(Log.SQL, sqle);
	// throw new PersistenceException(sqle);
	// } finally {
	// CacheStatement.release(stmt);
	// }
	//
	// }

	/**
	 * ################################ PRIVATE METHODS ##################################
	 */

	/**
	 * Remove sec codes of products. 'Batch' removal.
	 * 
	 * @param products
	 *            the products to update
	 * @param con
	 *            a DB connection
	 * @throws DeadLockException
	 *             dead lock exception
	 * @throws PersistenceException
	 *             generic persistence exception
	 */
	private static void removeSecCodesBatch(Vector<Product> products, String secCode, Connection con)
			throws PersistenceException, DeadLockException {
		PreparedStatement stmt = null;
		int count = 0;
		try {
			stmt = CacheStatement.getPrepared(con, S_REMOVE_SEC_CODES_STMT);
			stmt.setString(1, secCode);
			for (int i = 0; i < products.size(); i++) {

				Product prod = products.elementAt(i);
				int productId = prod.getId();
				stmt.setInt(2, productId);

				if (Log.isCategoryLogged(Log.SQL)) {
					Log.debug(Log.SQL, "Batch remove sec code " + productId);
				}
				stmt.addBatch();
				count++;
				if (ioSQL.isBatchExecuteNeeded(S_REMOVE_SEC_CODES_STMT, count)) {
					if (Log.isCategoryLogged(Log.SQL)) {
						Log.debug(Log.SQL, "ExecuteBatch removeSecCode  ---- " + count);
					}
					stmt.executeBatch();
					stmt.clearBatch();
					stmt.setString(1, secCode);
					count = 0;
				}

			}
			if (Log.isCategoryLogged(Log.SQL)) {
				Log.debug(Log.SQL, "ExecuteBatch removeSecCode ----");
			}
			if (count > 0) {
				if (Log.isCategoryLogged(Log.SQL)) {
					Log.debug(Log.SQL, "ExecuteBatch removeSecCode ---- " + count);
				}
				stmt.executeBatch();
				try {
					stmt.clearBatch();
				} catch (Exception eee) {
					Log.error(Log.SQL, eee);
				}
			}

		} catch (DeadLockException e) {
			throw e;
		} catch (Exception e) {
			Log.error(Log.SQL, e);
			throw new PersistenceException(e);
		} finally {
			CacheStatement.release(stmt);
		}
	}

	/**
	 * Save sec codes of products. 'Batch' saving.
	 * 
	 * @param products
	 *            the products to update
	 * @param con
	 *            a DB connection
	 * @throws DeadLockException
	 *             dead lock exception
	 * @throws PersistenceException
	 *             generic persistence exception
	 */
	@SuppressWarnings("unchecked")
	private static void saveSecCodesBatch(Vector<Product> prods, Connection con) throws PersistenceException,
			DeadLockException {

		PreparedStatement stmt = null;
		int count = 0;

		try {
			stmt = CacheStatement.getPrepared(con, S_SAVE_SEC_CODES_STMT);
			for (int i = 0; i < prods.size(); i++) {

				Product p = prods.elementAt(i);

				Hashtable<String, String> h = p.getSecCodes();
				if ((h == null) || (h.size() == 0)) {
					continue;
				}

				stmt.setInt(1, p.getId());
				Enumeration<String> e = h.keys();
				while (e.hasMoreElements()) {
					String code = e.nextElement();
					String value = h.get(code);
					if (code != null) {
						stmt.setString(2, code);
					} else {
						stmt.setNull(2, Types.VARCHAR);
					}
					if (value != null) {
						stmt.setString(3, value);
						stmt.setString(4, value.toUpperCase());
					} else {
						stmt.setNull(3, Types.VARCHAR);
						stmt.setNull(4, Types.VARCHAR);
					}
					if (Log.isCategoryLogged(Log.SQL)) {
						Log.debug(Log.SQL, "Batch Save Sec Code " + p.getId());
					}
					stmt.addBatch();
					count++;
					if (ioSQL.isBatchExecuteNeeded(S_SAVE_SEC_CODES_STMT, count)) {
						if (Log.isCategoryLogged(Log.SQL)) {
							Log.debug(Log.SQL, "ExecuteBtach saveSecCode-- " + count);
						}
						stmt.executeBatch();
						stmt.clearBatch();
						stmt.setInt(1, p.getId());
						count = 0;
					}
				}
			}
			if (Log.isCategoryLogged(Log.SQL)) {
				Log.debug(Log.SQL, "ExecuteBatch saveSecCode ----- ");
			}
			if (count > 0) {
				if (Log.isCategoryLogged(Log.SQL)) {
					Log.debug(Log.SQL, "ExecuteBatch saveSecCode ---- " + count);
				}
				stmt.executeBatch();
				try {
					stmt.clearBatch();
				} catch (Exception eee) {
					Log.error(Log.SQL, eee);
				}
			}
		} catch (DeadLockException dle) {
			throw dle;
		} catch (Exception e) {
			try {
				stmt.clearBatch();
			} catch (Exception eee) {
				Log.error(Log.SQL, eee);
			}
			Log.error(Log.SQL, e);
			throw new PersistenceException(e);
		} finally {
			CacheStatement.release(stmt);
		}
	}

	@Override
	protected boolean save(Product prod, Connection con) throws PersistenceException, DeadLockException {
		throw new IllegalStateException(
				"Please don't use this method. consider the use of ProductSQL#save(Product, Connection)");
	}

	@Override
	protected boolean insert(Product prod, Connection con) throws PersistenceException, DeadLockException {
		throw new IllegalStateException(
				"Please don't use this method. consider the use of ProductSQL#insert(Product, Connection)");
	}

	@Override
	protected boolean remove(Product prod, Connection con) throws PersistenceException, DeadLockException {
		throw new IllegalStateException(
				"Please don't use this method. consider the use of ProductSQL#remove(Product, Connection)");
	}
}
