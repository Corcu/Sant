/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.product.sql;

import calypsox.tk.product.EquityCustomData;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.DeadLockException;
import com.calypso.tk.core.sql.JResultSet;
import com.calypso.tk.core.sql.ProductCustomDataSQL;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.product.Equity;

import java.sql.*;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

public class EquityCustomDataSQL extends ProductCustomDataSQL {

    public EquityCustomDataSQL() {
    }

    public static final String EQUITY_CUSTOMDATA_TABLE = "equity_custom_data";
    public static final String EQUITY_CUSTOMDATA_TABLE_HIST = "equity_custom_data_hist";
    public static final String EQUITY_CUSTOMDATA_COLS = "product_id, active_available_qty, fee, qty_on_loan, rebate, expired_date_type, "
            + "expired_date, last_update, version_num";
    public static final String INSERT_STATEMENT = "INSERT INTO " + EQUITY_CUSTOMDATA_TABLE
            + " VALUES(?,?,?,?,?,?,?,?,?)";

    public static final String LOAD_STATEMENT = "SELECT " + EQUITY_CUSTOMDATA_COLS + " FROM " + EQUITY_CUSTOMDATA_TABLE;

    @Override
    public boolean save(Product inst, ProductCustomData data, Connection con) throws PersistenceException {
        // RepoProductExtension rpe = (RepoProductExtension)data;
        boolean error = true;
        PreparedStatement stmt = null;
        try {
            EquityCustomData equityCustomData = (EquityCustomData) data;
            if (equityCustomData != null) {
                stmt = newPreparedStatement(con, INSERT_STATEMENT);
                int i = 1;

                stmt.setLong(i++, equityCustomData.getLongId());

                if (equityCustomData.getActive_available_qty() != null) {
                    stmt.setDouble(i++, equityCustomData.getActive_available_qty());
                } else {
                    stmt.setNull(i++, Types.DOUBLE);
                }

                if (equityCustomData.getFee() != null) {
                    stmt.setDouble(i++, equityCustomData.getFee());
                } else {
                    stmt.setNull(i++, Types.DOUBLE);
                }

                if (equityCustomData.getQty_on_loan() != null) {
                    stmt.setDouble(i++, equityCustomData.getQty_on_loan());
                } else {
                    stmt.setNull(i++, Types.DOUBLE);
                }

                if (equityCustomData.getRebate() != null) {
                    stmt.setDouble(i++, equityCustomData.getRebate());
                } else {
                    stmt.setNull(i++, Types.DOUBLE);
                }

                stmt.setString(i++, equityCustomData.getExpired_date_type());

                if (equityCustomData.getExpired_date() != null) {
                    Date sql_expired_date = new Date(equityCustomData.getExpired_date()
                            .getJDatetime(TimeZone.getDefault()).getTime());
                    stmt.setDate(i++, sql_expired_date);
                } else {
                    stmt.setNull(i++, Types.DATE);
                }

                if (equityCustomData.getLast_update() != null) {
                    Date sql_last_update = new Date(equityCustomData.getLast_update()
                            .getJDatetime(TimeZone.getDefault()).getTime());
                    stmt.setDate(i++, sql_last_update);
                } else {
                    stmt.setNull(i++, Types.DATE);
                }

                stmt.setInt(i++, equityCustomData.getVersion());

                stmt.executeUpdate();

            }
        } catch (Exception e) {
            display(e);
            error = false;
            throw new PersistenceException(e);
        } finally {
            close(stmt);
        }
        return error;
    }

    /**
     * Archive/Restore Function
     *
     * @param whereClause containing the product ids to be transferred.
     * @param con         connection object.
     * @param toArchive   indicate the direction of transfer if to_archive then live -->history table
     */
    @Override
    public boolean saveToFromArchive(String whereClause, Connection con, boolean toArchive, JDatetime archivedDate, List<CalypsoBindVariable> bindVariableList) throws PersistenceException {
        boolean error = true;
        Statement stmt = null;
        try {
            stmt = newStatement(con);
            String strStmt = ioSQL.getArchiveSQL(EQUITY_CUSTOMDATA_TABLE, EQUITY_CUSTOMDATA_TABLE_HIST,
                    EQUITY_CUSTOMDATA_COLS, toArchive, whereClause, archivedDate);
            stmt.execute(strStmt);
        } catch (Exception e) {
            display(e);
            error = false;
            throw new PersistenceException(e);
        } finally {
            close(stmt);
        }
        return error;
    }

    @Override
    public boolean remove(Product inst, ProductCustomData data, Connection con) throws PersistenceException {
        boolean error = true;
        Statement stmt = null;
        String strStmt = "DELETE from " + EQUITY_CUSTOMDATA_TABLE + " where product_id = " + inst.getId();
        try {
            stmt = newStatement(con);
            stmt.executeUpdate(strStmt);
        } catch (Exception e) {
            display(e);
            error = false;
            throw new PersistenceException(e);
        } finally {
            close(stmt);
        }
        return error;
    }

    /**
     * Remove the products depending on the where clauses from live table or ARCHIVED Table
     *
     * @param where       WhereClause
     * @param con         connection object.
     * @param fromArchive indicate if the Repos are to be deleted from live or archive table.
     */
    @Override
    public boolean remove(String where, Connection con, boolean fromArchive, List<CalypsoBindVariable> bindVariables) throws PersistenceException, DeadLockException {

        boolean error = true;
        Statement stmt = null;
        String equityCustomDataTable = fromArchive ? EQUITY_CUSTOMDATA_TABLE_HIST : EQUITY_CUSTOMDATA_TABLE;
        String strStmt = "DELETE from " + equityCustomDataTable + " where " + where;
        try {
            stmt = newStatement(con);
            stmt.executeUpdate(strStmt);
        } catch (Exception e) {
            display(e);
            error = false;
            throw new PersistenceException(e);
        } finally {
            close(stmt);
        }
        return error;
    }

    /**
     * Load the Repo Product sExtension information
     *
     * @param products Vector which would contains the product loaded.
     * @param from     tables to add to the from clause
     * @param where    WhereClause
     * @param con      connection object.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public boolean load(Vector products, String from, String where, Connection con, List<CalypsoBindVariable> bindVariables) throws PersistenceException {
        return load(products, from, where, con, false, bindVariables);
    }

    /**
     * Load the products
     *
     * @param products    Vector which would contains the product loaded.
     * @param from        tablenames to add to the from clause
     * @param whereClause The where clause
     * @param con         connection object.
     * @param fromArchive indicate if the Repos are to be deleted from live or archive table.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public boolean load(Vector products, String from, String where, Connection con, boolean fromArchive, List<CalypsoBindVariable> bindVariables) throws PersistenceException {
        for (int i = 0; i < products.size(); i++) {
            Equity e = (Equity) products.get(i);
            EquityCustomData customData = getEquityCustomData(e.getId(), con);

            e.setCustomData(customData);
        }

        return true;
    }

    private EquityCustomData getEquityCustomData(int id, Connection con) throws PersistenceException {
        Statement stmt = null;

        // START OA 02/01/2014
        // Avoid returning an empty EquityCustomData, as It makes the system inconsistent.
        // Better to return a null EquityCustomData when there is no data (like it is already done for BondCustom)
        EquityCustomData customData = null;
        try {
            stmt = newStatement(con);
            StringBuilder builder = new StringBuilder(LOAD_STATEMENT).append(" WHERE product_id = ").append(id);
            JResultSet rs = new JResultSet(stmt.executeQuery(builder.toString()));
            while (rs.next()) {
                customData = new EquityCustomData();
                // END OA 02/01/2014
                int i = 1;
                customData.setProductId(rs.getInt(i++));
                customData.setActive_available_qty(rs.getDouble(i++));
                customData.setFee(rs.getDouble(i++));
                customData.setQty_on_loan(rs.getDouble(i++));
                customData.setRebate(rs.getDouble(i++));
                customData.setExpired_date_type(rs.getString(i++));
                customData.setExpired_date(rs.getJDate(i++));
                customData.setLast_update(rs.getJDate(i++));
                customData.setVersion(rs.getInt(i++));
            }
            rs.close();
        } catch (Exception e) {
            display(e);
            throw new PersistenceException(e);
        } finally {
            close(stmt);
        }
        return customData;
    }

    @Override
    public boolean remove(String where, Connection con, List<CalypsoBindVariable> bindVariable) throws PersistenceException, DeadLockException {
        return false;
    }
}