package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;


import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

public class BOReconcilePositionReportStyle extends com.calypso.tk.report.BOReconcilePositionReportStyle{

	/**
	 * 
	 */
	private static final long serialVersionUID = -90691203957392885L;
	
	public static final String CONTRACT_ID = "KOREA.Contract Id";
	public static final String CONTRACT_NAME = "KOREA.Contract name";
	public static final String COUNTERPARTY = "KOREA.Counterparty";
	public static final String COUNTERPARTY_NAME = "KOREA.Counterparty name";
	private static final String KOREA_SEPARATOR = DomainValues.comment("KOREA.MT535fields", "AccountSeparator");
	
	@Override
	@SuppressWarnings("rawtypes")
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {
		
		Object rst = null;
		
		if(columnName.equals(CONTRACT_ID)) {
			Account acc=  row.getProperty("Account");
			rst = getContractId(acc);
			
		} else if(columnName.equals(COUNTERPARTY)) {
			Account acc=  row.getProperty("Account");
			rst = getCounterparty(acc);
		} else if(columnName.equals(COUNTERPARTY_NAME)) {
			Account acc=  row.getProperty("Account");
			rst = getCounterpartyName(acc);
		} else if(columnName.equals(CONTRACT_NAME)) {
			Account acc=  row.getProperty("Account");
			rst = getContractName(acc);
		} else {
			rst = super.getColumnValue(row, columnName, errors);
		}
		
		
		
		
		return rst;
	}
	/**
	 * Get the contract Id of the position. In the CITIBANK accounts, the account is the last field.
	 * @param acc
	 * @return
	 */
	private String getContractId(Account acc) {
		String contractId = null;
		String accName = acc.getName();
		if (accName != null && !accName.isEmpty()) {
			String[] parts = accName.split(KOREA_SEPARATOR);
			if (parts.length >= 3) {
				contractId = parts[parts.length - 1];
			}
			
		}
		return contractId;
	}
	
	/**
	 * Get the counterparty code of the position. In the CITIBANK accounts, the account is the second last field.
	 * @param acc
	 * @return
	 */
	private String getCounterparty(Account acc) {
		String cpty = null;
		String accName = acc.getName();
		if (accName != null && !accName.isEmpty()) {
			String[] parts = accName.split(KOREA_SEPARATOR);
			if (parts.length >= 3) {
				cpty = parts[parts.length - 2];
			}
		}
		return cpty;
	}
	/**
	 * Get the counterparty Name
	 * @param acc account of the CITIBANK position
	 * @return the counterparty anme
	 */
	private String getCounterpartyName(Account acc) {
		String cptyCode = getCounterparty(acc);
		String leName = null;
		if(cptyCode != null && !cptyCode.isEmpty()) {
			LegalEntity le= BOCache.getLegalEntity(DSConnection.getDefault(), cptyCode);
			if(le != null) {
				leName = le.getName();
			}	
		}
		
		return leName;	
	}
	
	/**
	 * Get the contract name.
	 * @param acc account of the CITIBANK position
	 * @return contract name
	 */
	private String getContractName(Account acc) {
		String contractId = getContractId(acc);
		String contractName = null;
		if(contractId != null && !contractId.isEmpty()) {
			try {
				int mccId = Integer.parseInt(contractId);
				
				MarginCallConfig mcc = BOCache.getMarginCallConfig(DSConnection.getDefault(), mccId);
				contractName = mcc.getName();
			} catch(Exception e) {
				Log.error(this, "Error getting the Margin Call Config ID", e);
			}
			
		}
		
		return contractName;
		
	}
	

}
