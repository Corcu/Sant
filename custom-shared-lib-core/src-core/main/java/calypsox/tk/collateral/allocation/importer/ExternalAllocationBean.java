/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.collateral.allocation.importer;

import java.util.Date;

import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import calypsox.tk.collateral.allocation.importer.mapper.ExcelExternalAllocationMapper;

public class ExternalAllocationBean {

	private CollateralConfig mcc;

	private String poShortName;
	private String ctrShortName;
	private String fatherId;
	private double nominal;
	private Date settlementDate;
	private int rowNumber;
	private int contractID;
	private MarginCallEntry entry;
	private ExcelExternalAllocationMapper mapper;
	
	public ExcelExternalAllocationMapper getMapper(){
		return mapper;
	}
	
	public void setMapper(ExcelExternalAllocationMapper newMapper){
		mapper = newMapper;
	}
	
	public MarginCallEntry getMarginCallEntry(){
		return entry;
	}
	
	public void setMarginCallEntry(MarginCallEntry mcentry){
		entry = mcentry;
	}
	
	public boolean isByName(){
		if(Util.isEmpty(ctrShortName)) return false;
		return true;
	}
	
	public int getContractID(){
		return contractID;
	}
	
	public void setContractID(int id){
		contractID = id;
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

	/**
	 * @return the poShortName
	 */
	public String getPoShortName() {
		return this.poShortName;
	}

	/**
	 * @param poShortName
	 *            the poShortName to set
	 */
	public void setPoShortName(String poShortName) {
		this.poShortName = poShortName;
	}

	/**
	 * @return the ctrShortName
	 */
	public String getCtrShortName() {
		return this.ctrShortName;
	}

	/**
	 * @param ctrShortName
	 *            the ctrShortName to set
	 */
	public void setCtrShortName(String ctrShortName) {
		this.ctrShortName = ctrShortName;
	}

	/**
	 * @return the fatherId
	 */
	public String getFatherId() {
		return this.fatherId;
	}

	/**
	 * @param fatherId
	 *            the fatherId to set
	 */
	public void setFatherId(String fatherId) {
		this.fatherId = fatherId;
	}

	/**
	 * @return the nominal
	 */
	public double getNominal() {
		return this.nominal;
	}

	/**
	 * @param nominal
	 *            the nominal to set
	 */
	public void setNominal(double nominal) {
		this.nominal = nominal;
	}

	/**
	 * @return the settlementDate
	 */
	public Date getSettlementDate() {
		return this.settlementDate;
	}

	/**
	 * @param settlementDate
	 *            the settlementDate to set
	 */
	public void setSettlementDate(Date settlementDate) {
		this.settlementDate = settlementDate;
	}

	/**
	 * @return the rowNumber
	 */
	public int getRowNumber() {
		return this.rowNumber;
	}

	/**
	 * @param rowNumber
	 *            the rowNumber to set
	 */
	public void setRowNumber(int rowNumber) {
		this.rowNumber = rowNumber;
	}

}
