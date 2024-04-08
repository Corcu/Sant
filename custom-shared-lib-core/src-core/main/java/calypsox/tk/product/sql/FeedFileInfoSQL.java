package calypsox.tk.product.sql;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;

import calypsox.tk.util.bean.FeedFileInfoBean;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.sql.DeadLockException;
import com.calypso.tk.core.sql.JResultSet;
import com.calypso.tk.core.sql.ioSQL;

/**
 * This class handles all interaction with the database to save, delete, modify save a ExternalTrade instrument in the
 * system.
 * 
 */
public class FeedFileInfoSQL {

	/* INSERT DATA */
	public boolean setFeedFileInfoData(FeedFileInfoBean ffiBean, Connection con) throws DeadLockException,
			RemoteException {

		PreparedStatement stmt = null;
		int i = 1;

		try {
			String INSERTSQL = "INSERT INTO feed_file_info(process,processing_org,start_time,end_time,"
					+ "process_date,file_imported,inout,result,number_ok,number_warning,number_error,"
					+ "original_file,comments) VALUES(?,?,?, ?,?,?, ?,?,?, ?,?,?, ?,?,?)";

			stmt = ioSQL.newPreparedStatement(con, INSERTSQL);

			// process
			stmt.setString(i++, ffiBean.getProcess());
			// processing org
			stmt.setInt(i++, ffiBean.getProcessingOrg());
			// start time
			stmt.setTimestamp(i++, ffiBean.getStartTime());
			// end time
			if (ffiBean.getEndTime() != null) {
				stmt.setTimestamp(i++, ffiBean.getEndTime());
			} else {
				stmt.setNull(i++, Types.TIMESTAMP);
			}
			// process date
			if (ffiBean.getProcessDate() != null) {
				stmt.setDate(i++, (Date) ffiBean.getProcessDate());
			} else {
				stmt.setNull(i++, Types.DATE);
			}
			// file imported
			if (ffiBean.getFileImported() != null) {
				stmt.setString(i++, ffiBean.getFileImported());
			} else {
				stmt.setNull(i++, Types.VARCHAR);
			}
			// inout
			if (ffiBean.getInout() != null) {
				stmt.setString(i++, ffiBean.getInout());
			} else {
				stmt.setNull(i++, Types.VARCHAR);
			}
			// result
			if (ffiBean.getResult() != null) {
				stmt.setString(i++, ffiBean.getResult());
			} else {
				stmt.setNull(i++, Types.VARCHAR);
			}
			// number ok
			if (ffiBean.getNumberOk() != null) {
				stmt.setInt(i++, ffiBean.getNumberOk());
			} else {
				stmt.setNull(i++, Types.INTEGER);
			}
			// number warning
			if (ffiBean.getNumberWarning() != null) {
				stmt.setInt(i++, ffiBean.getNumberWarning());
			} else {
				stmt.setNull(i++, Types.INTEGER);
			}
			// number error
			if (ffiBean.getNumberError() != null) {
				stmt.setInt(i++, ffiBean.getNumberError());
			} else {
				stmt.setNull(i++, Types.INTEGER);
			}
			// original file
			if (ffiBean.getOriginalFile() != null) {
				stmt.setString(i++, ffiBean.getOriginalFile());
			} else {
				stmt.setNull(i++, Types.VARCHAR);
			}
			// comments
			if (ffiBean.getComments() != null) {
				stmt.setString(i++, ffiBean.getComments());
			} else {
				stmt.setNull(i++, Types.VARCHAR);
			}
			// execute query (do the insert)
			stmt.executeUpdate();
			ioSQL.commit(con);
		}

		catch (DeadLockException e) {
			throw e;
		} catch (Exception e) {
			Log.error(this, e);
			ioSQL.rollback(con);
			throw new RemoteException("SQL error: " + e.getMessage(), e);
		} finally {
			ioSQL.close(stmt);
			ioSQL.releaseConnection(con);
		}

		return true;
	}

	/* RETRIEVE DATA */
	@SuppressWarnings("deprecation")
	public ArrayList<FeedFileInfoBean> getFeedFileInfoData(String sqlQuery) throws RemoteException {

		Connection con = null;
		Statement stmt = null;
		ArrayList<FeedFileInfoBean> ffiBeans = new ArrayList<FeedFileInfoBean>();

		try {
			con = ioSQL.newConnection();
			stmt = ioSQL.newStatement(con);
			JResultSet rs = new JResultSet(stmt.executeQuery(sqlQuery));
			int fieldIndex = 1;

			while (rs.next()) {
				fieldIndex = 1;
				FeedFileInfoBean ffiBean = new FeedFileInfoBean();
				ffiBean.setProcess(rs.getString(fieldIndex++));
				ffiBean.setProcessingOrg(rs.getInt(fieldIndex++));
				ffiBean.setStartTime(Timestamp.valueOf(rs.getJDatetime(fieldIndex++).toString()));
				ffiBean.setEndTime(Timestamp.valueOf(rs.getJDatetime(fieldIndex++).toString()));
				ffiBean.setProcessDate(Date.valueOf(rs.getJDate(fieldIndex++).toString()));
				ffiBean.setFileImported(rs.getString(fieldIndex++));
				ffiBean.setInout(rs.getString(fieldIndex++));
				ffiBean.setResult(rs.getString(fieldIndex++));
				ffiBean.setNumberOk(rs.getInt(fieldIndex++));
				ffiBean.setNumberWarning(rs.getInt(fieldIndex++));
				ffiBean.setNumberError(rs.getInt(fieldIndex++));
				ffiBean.setOriginalFile(rs.getString(fieldIndex++));
				ffiBean.setComments(rs.getString(fieldIndex++));
				ffiBeans.add(ffiBean);
			}

		} catch (Exception e) {
			Log.error(this, e);
			throw new RemoteException("SQL error: " + e.getMessage(), e);
		} finally {
			ioSQL.close(stmt);
			ioSQL.releaseConnection(con);
		}

		return ffiBeans;
	}
}
