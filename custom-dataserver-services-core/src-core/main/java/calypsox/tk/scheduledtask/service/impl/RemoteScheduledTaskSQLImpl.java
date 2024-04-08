package calypsox.tk.scheduledtask.service.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.jfree.util.Log;

import com.calypso.tk.core.sql.ioSQL;

import calypsox.tk.scheduledtask.service.LocalScheduledTaskSQL;
import calypsox.tk.service.scheduledtask.RemoteScheduledTaskSQL;

@Stateless(name = "calypsox.tk.service.scheduledtask.RemoteScheduledTaskSQL")
@Remote(RemoteScheduledTaskSQL.class)
@Local(LocalScheduledTaskSQL.class)
public class RemoteScheduledTaskSQLImpl implements RemoteScheduledTaskSQL, LocalScheduledTaskSQL{
	
	private String QUERY = "UPDATE INV_SEC_BALANCE SET TOTAL_SECURITY = 0, TOTAL_MARGIN_CALL_REHYPO = 0 \n"
			+ "WHERE (inv_sec_balance.internal_external = '%s' AND inv_sec_balance.position_type = '%s' AND inv_sec_balance.date_type = '%s') \n"
			+ "AND inv_sec_balance.mcc_id = %s AND inv_sec_balance.config_id = 0 AND inv_sec_balance.security_id = %s";

	@Override
	public void updatePosition(String dateType, String positionType, String positionClass, String mccId,
			String productId) {
		
			String update = null;
		
			try {
			Connection con = ioSQL.getConnection();
			Statement stmt = null;
			
			stmt = ioSQL.newStatement(con);
			
			update = String.format(QUERY, positionClass, positionType, dateType, mccId, productId);
			
			stmt.executeUpdate(update);
			
		} catch (SQLException e) {
			Log.error("Error executing query: " + update, e);
		}
		
	}

}
