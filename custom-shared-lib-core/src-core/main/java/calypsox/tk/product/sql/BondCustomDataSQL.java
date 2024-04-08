/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.product.sql;

import calypsox.tk.product.BondCustomData;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.JResultSet;
import com.calypso.tk.core.sql.ProductCustomDataSQL;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.product.Bond;

import java.sql.*;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

public class BondCustomDataSQL extends ProductCustomDataSQL {

    public BondCustomDataSQL() {
    }

    public static final String BOND_CUSTOMDATA_TABLE = "bond_custom_data";
    public static final String BOND_CUSTOMDATA_TABLE_HIST = "bond_custom_data_hist";
    public static final String BOND_CUSTOMDATA_COLS = "product_id, haircut_ecb, haircut_swiss, haircut_boe, haircut_fed, haircut_eurex, version_num, "
            + "active_available_qty, fee, qty_on_loan, rebate, expired_date_type, expired_date, last_update";
    public static final String INSERT_STATEMENT = "INSERT INTO " + BOND_CUSTOMDATA_TABLE
            + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    public static final String LOAD_STATEMENT = "SELECT " + BOND_CUSTOMDATA_COLS + " FROM " + BOND_CUSTOMDATA_TABLE;

    @Override
    public boolean save(Product inst, ProductCustomData data, Connection con) throws PersistenceException {
        // RepoProductExtension rpe = (RepoProductExtension)data;
        boolean error = true;
        PreparedStatement stmt = null;
        try {
            BondCustomData bondCustomData = ((BondCustomData) data);
            if (bondCustomData != null) {
                bondCustomData.setLongId(inst.getId());
                stmt = newPreparedStatement(con, INSERT_STATEMENT);
                int i = 1;
                stmt.setLong(i++, bondCustomData.getLongId());
                if (bondCustomData.getHaircut_ecb() != null) {
                    stmt.setDouble(i++, bondCustomData.getHaircut_ecb());
                } else {
                    stmt.setNull(i++, Types.DOUBLE);
                }

                if (bondCustomData.getHaircut_swiss() != null) {
                    stmt.setDouble(i++, bondCustomData.getHaircut_swiss());
                } else {
                    stmt.setNull(i++, Types.DOUBLE);
                }

                if (bondCustomData.getHaircut_boe() != null) {
                    stmt.setDouble(i++, bondCustomData.getHaircut_boe());
                } else {
                    stmt.setNull(i++, Types.DOUBLE);
                }

                if (bondCustomData.getHaircut_fed() != null) {
                    stmt.setDouble(i++, bondCustomData.getHaircut_fed());
                } else {
                    stmt.setNull(i++, Types.DOUBLE);
                }

                stmt.setInt(i++, bondCustomData.getVersion());

                if (bondCustomData.getActive_available_qty() != null) {
                    stmt.setDouble(i++, bondCustomData.getActive_available_qty());
                } else {
                    stmt.setNull(i++, Types.DOUBLE);
                }

                if (bondCustomData.getFee() != null) {
                    stmt.setDouble(i++, bondCustomData.getFee());
                } else {
                    stmt.setNull(i++, Types.DOUBLE);
                }

                if (bondCustomData.getQty_on_loan() != null) {
                    stmt.setDouble(i++, bondCustomData.getQty_on_loan());
                } else {
                    stmt.setNull(i++, Types.DOUBLE);
                }

                if (bondCustomData.getRebate() != null) {
                    stmt.setDouble(i++, bondCustomData.getRebate());
                } else {
                    stmt.setNull(i++, Types.DOUBLE);
                }

                stmt.setString(i++, bondCustomData.getExpired_date_type());

                if (bondCustomData.getExpired_date() != null) {
                    Date sql_expired_date = new Date(bondCustomData.getExpired_date()
                            .getJDatetime(TimeZone.getDefault()).getTime());
                    stmt.setDate(i++, sql_expired_date);
                } else {
                    stmt.setNull(i++, Types.DATE);
                }

                if (bondCustomData.getLast_update() != null) {
                    Date sql_last_update = new Date(bondCustomData.getLast_update().getJDatetime(TimeZone.getDefault())
                            .getTime());
                    stmt.setDate(i++, sql_last_update);
                } else {
                    stmt.setNull(i++, Types.DATE);
                }

                // New column, should be in the end
                if (bondCustomData.getHaircut_eurex() != null) {
                    stmt.setDouble(i++, bondCustomData.getHaircut_eurex());
                } else {
                    stmt.setNull(i++, Types.DOUBLE);
                }

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
            String strStmt = ioSQL.getArchiveSQL(BOND_CUSTOMDATA_TABLE, BOND_CUSTOMDATA_TABLE_HIST,
                    BOND_CUSTOMDATA_COLS, toArchive, whereClause, archivedDate);
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
        String strStmt = "DELETE from " + BOND_CUSTOMDATA_TABLE + " where product_id = " + inst.getId();
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
    public boolean remove(String where, Connection con, boolean fromArchive, List<CalypsoBindVariable> bindVariableList) throws PersistenceException {
        boolean error = true;
        Statement stmt = null;
        String bondCustomDataTable = fromArchive ? BOND_CUSTOMDATA_TABLE_HIST : BOND_CUSTOMDATA_TABLE;
        String strStmt = "DELETE from " + bondCustomDataTable + " where " + where;
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
    @Override
    public boolean load(@SuppressWarnings("rawtypes") Vector products, String from, String whereClause, Connection con, List<CalypsoBindVariable> bindVariableList)
            throws PersistenceException {
        return load(products, from, whereClause, con, false, bindVariableList);
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
    @Override
    public boolean load(@SuppressWarnings("rawtypes") Vector products, String from, String whereClause, Connection con,
                        boolean fromArchive, List<CalypsoBindVariable> bindVariableList) throws PersistenceException {

        for (int i = 0; i < products.size(); i++) {
            Bond b = (Bond) products.get(i);
            BondCustomData customData = getBondCustomData(b.getId(), con);

            b.setCustomData(customData);
        }

        return true;
    }

    private BondCustomData getBondCustomData(int id, Connection con) throws PersistenceException {
        Statement stmt = null;

        BondCustomData customData = null;
        try {
            Double tempDbl = null;
            stmt = newStatement(con);
            StringBuilder builder = new StringBuilder(LOAD_STATEMENT).append(" WHERE product_id = ").append(id);
            JResultSet rs = new JResultSet(stmt.executeQuery(builder.toString()));
            while (rs.next()) {
                customData = new BondCustomData();
                int i = 1;
                customData.setProductId(rs.getInt(i++));

                tempDbl = rs.getDouble(i++);
                if (rs.wasNull()) {
                    customData.setHaircut_ecb(null);
                } else {
                    customData.setHaircut_ecb(tempDbl);
                }

                tempDbl = rs.getDouble(i++);
                if (rs.wasNull()) {
                    customData.setHaircut_swiss(null);
                } else {
                    customData.setHaircut_swiss(tempDbl);
                }

                tempDbl = rs.getDouble(i++);
                if (rs.wasNull()) {
                    customData.setHaircut_boe(null);
                } else {
                    customData.setHaircut_boe(tempDbl);
                }

                tempDbl = rs.getDouble(i++);
                if (rs.wasNull()) {
                    customData.setHaircut_fed(null);
                } else {
                    customData.setHaircut_fed(tempDbl);
                }

                tempDbl = rs.getDouble(i++);
                if (rs.wasNull()) {
                    customData.setHaircut_eurex(null);
                } else {
                    customData.setHaircut_eurex(tempDbl);
                }

                customData.setVersion(rs.getInt(i++));
                customData.setActive_available_qty(rs.getDouble(i++));
                customData.setFee(rs.getDouble(i++));
                customData.setQty_on_loan(rs.getDouble(i++));
                customData.setRebate(rs.getDouble(i++));
                customData.setExpired_date_type(rs.getString(i++));
                customData.setExpired_date(rs.getJDate(i++));
                customData.setLast_update(rs.getJDate(i++));
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
    public boolean remove(String where, Connection con, List<CalypsoBindVariable> bindVariableList) throws PersistenceException {
        return false;
    }

}
