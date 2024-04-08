package calypsox.tk.report;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import static calypsox.tk.report.Opt_MMOOAgreementParamsReportTemplate.MMOO_CONTRACT;
import static calypsox.tk.report.Opt_MMOOAgreementParamsReportTemplate.QUOTE_SET_NAME;

/**
 * Report of MMOO Agreements relation with their quote Set. Recovers the MMOO contracts, from them the quotes names of the 
 * associated haircuts rules (as quoteSets). Shows the relation MMOO contract <-> QuoteSet Name
 * 
 * @author Guillermo Solano
 * @version 1.0
 *
 */
public class Opt_MMOOAgreementParamsReport extends Opt_MMOOHaircutDefinitionReport {

	private static final long serialVersionUID = 4173688136057649329L;

	/**
	 * Main methods. calls the report output & manages possible errors to show to the user.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public ReportOutput load(final Vector errorMsgsP) {

		StringBuffer error = new StringBuffer();
		
		try {
			
			return getReportOutput();
			
		} catch (RemoteException e) {
			Log.error(this, e); //sonar
			error.append("Error generating Opt_MMOOAgreementParamsReport.\n");
			error.append(e.getLocalizedMessage());
			
		} catch (OutOfMemoryError e2) {
			Log.error(this, e2); //sonar
			error.append("Not enough memory to run this report.\n");
		 	
		} catch (Exception e3){
			Log.error(this, e3); //sonar
			error.append("Error generating Opt_MMOOAgreementParamsReport.\n");
			error.append(e3.getLocalizedMessage());		
		}
		
		Log.error(this, error.toString());
		errorMsgsP.add(error.toString());

		return null;
	}


	/**
	 * Get report output. First MMOO contract.
	 * 
	 * @return DefaultReportOutput
	 * @throws RemoteException
	 */
	protected DefaultReportOutput getReportOutput() throws Exception {

		final DefaultReportOutput output = new StandardReportOutput(this);
		final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();
		final JDate valueDate = super.getExecutionDate();
		super.dsConn = DSConnection.getDefault();

		// load contracts
		final List<CollateralConfig> contracts = super.loadMMOOContracts( valueDate , getReportTemplate());
		
		if (Util.isEmpty(contracts)) {
			Log.info(this, "Cannot find any open MMOO contract.\n");
			return null;
		}
		
		//gather the HR rules associated to a quote and their name (one per MMOO contract)
		final Map<String, String> contractsQuotesSetMap = super.getQuotesSetsForContracts( contracts );
		
		for (Map.Entry<String, String> entry: contractsQuotesSetMap.entrySet()){
			
			ReportRow row = new ReportRow(entry.getKey(), MMOO_CONTRACT);
			row.setProperty(QUOTE_SET_NAME, entry.getValue());
			reportRows.add(row);
		}

		output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));

		return output;
	}

} //end class
