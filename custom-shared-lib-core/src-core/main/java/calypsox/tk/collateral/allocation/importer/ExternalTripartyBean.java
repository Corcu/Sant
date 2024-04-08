package calypsox.tk.collateral.allocation.importer;

import calypsox.tk.collateral.allocation.importer.mapper.ExcelExternalAllocationMapper;

import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

public class ExternalTripartyBean {

	private double nominal;
	private String currency;
	private int contractID;
	private String ctrShortName;
	private int rowNumber;
	private MarginCallEntry entry;
	protected ExcelExternalAllocationMapper mapper;//Sonar
	private CollateralConfig mcc;
	
	public boolean isByName(){
		if(Util.isEmpty(ctrShortName)) return false;
		return true;
	}
	
	public double getNominal() {
		return nominal;
	}
	
	public void setNominal(double nominal) {
		this.nominal = nominal;
	}
	
	public String getCurrency() {
		return currency;
	}
	
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	
	public int getContractID() {
		return contractID;
	}
	
	public void setContractID(int contractID) {
		this.contractID = contractID;
	}
	
	public String getCtrShortName() {
		return ctrShortName;
	}
	
	public void setCtrShortName(String ctrShortName) {
		this.ctrShortName = ctrShortName;
	}
	
	public int getRowNumber() {
		return rowNumber;
	}
	
	public void setRowNumber(int rowNumber) {
		this.rowNumber = rowNumber;
	}
	
	public MarginCallEntry getEntry() {
		return entry;
	}
	
	public void setEntry(MarginCallEntry entry) {
		this.entry = entry;
	}
	
	public ExcelExternalAllocationMapper getMapper() {
		return mapper;
	}
	
	public void setMapper(ExcelExternalAllocationMapper mapper) {
		this.mapper = mapper;
	}
	
	/**
	 * @return the mcc
	 */
	@SuppressWarnings("static-access")
	public CollateralConfig getMcc() {
		if(this.mcc != null) {
			return this.mcc;
		}
		CollateralConfig marginCallConfig = null;		
		if(isByName()){
			try {
				marginCallConfig = ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfigByCode(null, getCtrShortName());
			} catch (CollateralServiceException e) {
				Log.error(this, e);
			}
		}else{
			marginCallConfig = CacheCollateralClient.getInstance().getCollateralConfig(DSConnection.getDefault(), getContractID());
		}
		this.mcc = marginCallConfig;
		return this.mcc;
	}

	/**
	 * @param mcc
	 *            the mcc to set
	 */
	public void setMcc(CollateralConfig mcc) {
		this.mcc = mcc;
	}
	
	
}
