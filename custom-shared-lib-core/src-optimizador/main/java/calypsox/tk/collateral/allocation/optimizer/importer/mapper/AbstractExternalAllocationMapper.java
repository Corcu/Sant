/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.collateral.allocation.optimizer.importer.mapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import calypsox.tk.collateral.allocation.optimizer.importer.beans.AllocImportErrorBean;
import calypsox.tk.collateral.allocation.optimizer.importer.beans.OptimAllocationBean;
import calypsox.tk.collateral.allocation.optimizer.importer.beans.OptimCashAllocationBean;
import calypsox.tk.collateral.allocation.optimizer.importer.beans.OptimContractAllocsBean;
import calypsox.tk.collateral.allocation.optimizer.importer.beans.OptimSecurityAllocationBean;

import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.CashAllocation;
import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.SecurityAllocation;

/**
 * @author aela
 *
 */
public abstract class AbstractExternalAllocationMapper implements
		ExternalAllocationMapper {

	/**
	 * 
	 */
	public AbstractExternalAllocationMapper() {
	}

	/* (non-Javadoc)
	 * @see calypsox.tk.collateral.allocation.optimizer.importer.mapper.ExternalAllocationMapper#mapAllocation(calypsox.tk.collateral.allocation.optimizer.importer.beans.OptimAllocationBean, java.util.List)
	 */
	@Override
	public MarginCallAllocation mapAllocation(OptimAllocationBean allocBean,
			List<AllocImportErrorBean> messages) throws Exception {
		if (allocBean.isSecurityAllocation()) {
			return mapSecurityAllocation(
					(OptimSecurityAllocationBean) allocBean, messages);
		}
		else if (allocBean.isCashAllocation()) {
			return mapCashAllocation((OptimCashAllocationBean) allocBean,
					messages);
		}
		return null;
	}

	/**
	 * Map a list of beans into a Calypso MarginCallAllocation allocations
	 * 
	 * @param listAllocBean list of allocation beans to map
	 * @param messages, any error occurred during the mapping
	 * @return a list of Calypso MarginCallAllocation corresponding to the given beans
	 * @throws Exception any thrown exception
	 */
	public List<MarginCallAllocation> mapListAllocation(
			List<? extends OptimAllocationBean> listAllocBean,
			List<AllocImportErrorBean> messages) throws Exception {
		List<MarginCallAllocation> marginCallAllocs = new ArrayList<MarginCallAllocation>();
		for (OptimAllocationBean allocBean : listAllocBean) {
			List<AllocImportErrorBean> errors = new ArrayList<AllocImportErrorBean>();
			marginCallAllocs.add(mapAllocation(allocBean, errors));
			messages.addAll(errors);
		}
		return marginCallAllocs;
	}

	/**
	 * 
	 * Map a list of beans into a Calypso CashAllocation allocations
	 * 
	 * @param allocBean
	 * @param messages
	 * @return
	 */
	protected abstract CashAllocation mapCashAllocation(
			OptimCashAllocationBean allocBean, List<AllocImportErrorBean> messages);

	protected abstract SecurityAllocation mapSecurityAllocation(
			OptimSecurityAllocationBean allocBean, List<AllocImportErrorBean> messages);

	/* (non-Javadoc)
	 * @see calypsox.tk.collateral.allocation.optimizer.importer.mapper.ExternalAllocationMapper#isValidAllocation(calypsox.tk.collateral.allocation.optimizer.importer.beans.OptimAllocationBean, java.util.List)
	 */
	@Override
	public boolean isValidAllocation(OptimAllocationBean allocBean,
			List<AllocImportErrorBean> messages) throws Exception {
		boolean isValid = true;
		if (allocBean.isSecurityAllocation()) {
			 return isValidSecurityAllocation(
					(OptimSecurityAllocationBean) allocBean, messages);
		}
		else if (allocBean.isCashAllocation()) {
			return isValidCashAllocation((OptimCashAllocationBean) allocBean,
					messages);
		}
		else if (allocBean instanceof OptimContractAllocsBean) {
			for (OptimAllocationBean alloc : ((OptimContractAllocsBean) allocBean)
					.getAllocations()) {
				isValid &= isValidAllocation(alloc, messages);
			}
			// get rid of any duplicated message. That could happen if something is wrong about the contract and each allocation validation will report it.
			if (!Util.isEmpty(messages)) {
				Set<AllocImportErrorBean> nonDuplicatedMessages = new HashSet<AllocImportErrorBean>(
						messages);
				messages.clear(); 
				messages.addAll(nonDuplicatedMessages);
			}
			return isValid;
		}
		return false;
	}

	/**
	 * @param listAllocBeans
	 * @param messages
	 * @return
	 * @throws Exception
	 */
	public List<OptimAllocationBean> getValidListAllocation(
			List<OptimAllocationBean> listAllocBeans, List<AllocImportErrorBean> messages)
			throws Exception {
		List<OptimAllocationBean> validAllocs = new ArrayList<OptimAllocationBean>();
		for (OptimAllocationBean allocBean : listAllocBeans) {
			List<AllocImportErrorBean> errors = new ArrayList<AllocImportErrorBean>();
			if (isValidAllocation(allocBean, errors)) {
				validAllocs.add(allocBean);
			}
			else {
				messages.addAll(errors);
			}
		}
		return validAllocs;
	}

	/**
	 * @param allocBean
	 * @param messages
	 * @return
	 */
	protected abstract boolean isValidCashAllocation(
			OptimCashAllocationBean allocBean, List<AllocImportErrorBean> messages);

	/**
	 * @param allocBean
	 * @param messages
	 * @return
	 */
	protected abstract boolean isValidSecurityAllocation(
			OptimSecurityAllocationBean allocBean, List<AllocImportErrorBean> messages);

}
