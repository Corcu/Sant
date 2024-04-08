/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.collateral.allocation.importer.mapper;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.jfree.util.Log;

import calypsox.tk.collateral.allocation.importer.CashExternalAllocationBean;
import calypsox.tk.collateral.allocation.importer.ExternalAllocationBean;
import calypsox.tk.collateral.allocation.importer.ExternalTripartyBean;
import calypsox.tk.collateral.allocation.importer.SecurityExternalAllocationBean;

import com.calypso.tk.collateral.CashAllocation;
import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.SecurityAllocation;
import com.calypso.tk.collateral.impl.AllocationFactory;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;

public abstract class AbstractExternalAllocationMapper implements ExternalAllocationMapper {
	public AllocationFactory allocationFactory;
	public MarginCallEntry entry;
	public PricingEnv pricingEnv;

	public AbstractExternalAllocationMapper(MarginCallEntry entry, JDate processingDate) {
		this.entry = entry;
		String peName = entry.getCollateralConfig().getPricingEnvName();
		try {
			this.pricingEnv = DSConnection.getDefault().getRemoteMarketData()
					.getPricingEnv(peName, new JDatetime(processingDate, TimeZone.getDefault()));
		} catch (RemoteException e) {
			Log.error(this, e);
		}
		this.allocationFactory = AllocationFactory.getInstance(null, entry);
	}

	@Override
	public MarginCallAllocation mapAllocation(ExternalAllocationBean allocBean, List<String> messages) throws Exception {
		if (allocBean instanceof SecurityExternalAllocationBean) {
			return mapSecurityAllocation((SecurityExternalAllocationBean) allocBean, messages);
		} else if (allocBean instanceof CashExternalAllocationBean) {
			return mapCashAllocation((CashExternalAllocationBean) allocBean, messages);
		}
		return null;
	}

	public List<MarginCallAllocation> mapListAllocation(List<? extends ExternalAllocationBean> listAllocBean,
			List<String> messages) throws Exception {
		List<MarginCallAllocation> marginCallAllocs = new ArrayList<MarginCallAllocation>();
		for (ExternalAllocationBean allocBean : listAllocBean) {
			List<String> errors = new ArrayList<String>();
			marginCallAllocs.add(mapAllocation(allocBean, errors));
			messages.addAll(errors);
		}
		return marginCallAllocs;
	}

	protected abstract CashAllocation mapCashAllocation(CashExternalAllocationBean allocBean, List<String> messages);

	protected abstract SecurityAllocation mapSecurityAllocation(SecurityExternalAllocationBean allocBean,
			List<String> messages);

	@Override
	public boolean isValidAllocation(ExternalAllocationBean allocBean, List<String> messages) throws Exception {
		if (allocBean instanceof SecurityExternalAllocationBean) {
			return isValidSecurityAllocation((SecurityExternalAllocationBean) allocBean, messages);
		} else if (allocBean instanceof CashExternalAllocationBean) {
			return isValidCashAllocation((CashExternalAllocationBean) allocBean, messages);
		}
		return false;
	}
	
	public boolean isValidTripartyAgreedAmount(ExternalTripartyBean taa, List<String> messages) throws Exception {
		return isValidTAA((ExternalTripartyBean) taa, messages);
	}

	public List<ExternalAllocationBean> getValidListAllocation(List<ExternalAllocationBean> listAllocBeans,
			List<String> messages) throws Exception {
		List<ExternalAllocationBean> validAllocs = new ArrayList<ExternalAllocationBean>();
		for (ExternalAllocationBean allocBean : listAllocBeans) {
			List<String> errors = new ArrayList<String>();
			if (isValidAllocation(allocBean, errors)) {
				validAllocs.add(allocBean);
			} else {
				messages.addAll(errors);
			}
		}
		return validAllocs;
	}

	protected abstract boolean isValidCashAllocation(CashExternalAllocationBean allocBean, List<String> messages);

	protected abstract boolean isValidSecurityAllocation(SecurityExternalAllocationBean allocBean, List<String> messages);
	
	protected abstract boolean isValidTAA(ExternalTripartyBean taa, List<String> messages);

	public AllocationFactory getAllocationFactory() {
		return this.allocationFactory;
	}

	/**
	 * @return the entry
	 */
	public MarginCallEntry getEntry() {
		return this.entry;
	}

	/**
	 * @return the pricingEnv
	 */
	public PricingEnv getPricingEnv() {
		return this.pricingEnv;
	}

}
