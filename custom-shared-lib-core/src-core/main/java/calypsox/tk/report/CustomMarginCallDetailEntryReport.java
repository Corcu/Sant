package calypsox.tk.report;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.core.Attributes;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.PersistenceException;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.MarginCallDetailEntryReport;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.service.DSConnection;

import calypsox.tk.collateral.service.RemoteSantCollateralService;

public class CustomMarginCallDetailEntryReport extends MarginCallDetailEntryReport {

	private static final String VALUE_FIELD = "VALUE_FIELD";
	private static final String ADITIONAL_VALUE = "ADITIONAL_VALUE";
	private static final String MARGIN_CALL_CONFIG_IDS = "MARGIN_CALL_CONFIG_IDS";
	private static final long serialVersionUID = 5073479401659697872L;
	
	@SuppressWarnings({ "rawtypes" })
	@Override
	public ReportOutput load(Vector error) {
		
		String contractsIds = loadContractsFilterByAdditionalField();
		
		 //set contract list on MarginCallDetailEntryTemplate
		Attributes attr = super.getReportTemplate().getAttributes();
		attr.add(MARGIN_CALL_CONFIG_IDS, contractsIds);
		super.getReportTemplate().setAttributes(attr);
		
		return super.load(error);
	}
	

	/**
	 * Load contracts filtering by additionalField
	 * 
	 * @return @String - Contracts ids
	 */
	private String loadContractsFilterByAdditionalField() {
		String contractsIds = "";
		String additionalField = this._reportTemplate.get(ADITIONAL_VALUE); 
		String valueField = this._reportTemplate.get(VALUE_FIELD);
		
		if(!Util.isEmpty(additionalField) && !Util.isEmpty(valueField)) {
			RemoteSantCollateralService remoteColService = (RemoteSantCollateralService) DSConnection.getDefault().getRMIService("baseSantCollateralService",
					RemoteSantCollateralService.class);

			HashMap<String, String> addtitionalFields = new HashMap<>();
			addtitionalFields.put(additionalField,valueField);
			
			if(remoteColService!=null) {
				try {
					List<CollateralConfig> contracts = remoteColService.getMarginCallConfigByAdditionalField(addtitionalFields);
					contractsIds = getContratcsIds(contracts);
				} catch (PersistenceException e) {
					Log.error(this,"Cannot load contracts: " + e);
				}
			}
		}
		return contractsIds;
	}
	
	/**
	 * @param @List<CollateralConfig>
	 * @return
	 */
	private String getContratcsIds(List<CollateralConfig> contracts){
		StringBuffer buffer = new StringBuffer();

		if(!Util.isEmpty(contracts)) {
			for(CollateralConfig config : contracts) {
				buffer.append(config.getId() + ",");
			}

			if(buffer.toString().contains(",")) {
				int indx = buffer.lastIndexOf(",");
				buffer.deleteCharAt(indx);
			}
		}
		
		return buffer.toString();
	}

}
