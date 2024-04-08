/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import calypsox.util.SantReportingUtil;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.CollateralConfigCurrency;
import com.calypso.tk.refdata.OptimizationConfiguration;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.refdata.optimization.impl.Category;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.MarginCallEntryDTOReportTemplate;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

/**
 * 
 * OptimumReport: this report is meant to display the result of optimization, category by category, in order to the user
 * to identify -contract by contract- if the allocations has been allocated in the optimal situation and what-if he had
 * used another optimization.
 * 
 * This Class that contain the output of the Optimum report. Also contains the data used to perform the report. This
 * report reuses the MarginCallAllocation template to gather all the allocations, but process it according to specific
 * output categorized upon the optimization algorithm.
 * 
 * @author Guillermo Solano
 */

public class SantOptimumReport extends Report {

	private static final long serialVersionUID = 1L;

	private JDate processDate;

	private OptimizationConfiguration optimization;
	private Map<String, List<Integer>> sdFiltersProductIdsMap;

	public SantOptimumReport() {

		this.processDate = null;
		this.optimization = null;
		this.sdFiltersProductIdsMap = null;
	}

	/*
	 * Loads the output of a report
	 * 
	 * @see com.calypso.tk.report.Report#load(java.util.Vector)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ReportOutput load(@SuppressWarnings("rawtypes") Vector errorMsgs) {

		this.processDate = getProcessDate();
		this.optimization = getOptimizationConfiguration();

		// control check: process date and optimization configuration
		if (this.processDate == null) {
			errorMsgs.add("Please, enter a Valid Process Date");
			return null;
		}

		if ((this.optimization == null)) {
			errorMsgs.add("Please, choose one Optimization Configuration");
			return null;
		}

		if (Util.isEmpty((String) getReportTemplate().get(SantOptimumReportTemplate.USE_CACHE))) {
			errorMsgs.add("Please, choose weather or not you want to use the Cache.");
			return null;
		}

		boolean useCache = Boolean.valueOf((String) getReportTemplate().get(SantOptimumReportTemplate.USE_CACHE))
				.booleanValue();

		try {
			String mcIds = (String) getReportTemplate().get(MarginCallEntryDTOReportTemplate.MARGIN_CALL_CONFIG_IDS);
			Vector<String> mcType = (Vector<String>) getReportTemplate().get(SantOptimumReportTemplate.CONTRACT_TYPE);

			if (useCache) {
				this.sdFiltersProductIdsMap = SantReportingUtil.getSantReportingService(getDSConnection())
						.getSDFilterProdIdsCache(this.optimization, mcType, Util.string2IntVector(mcIds));
				if (this.sdFiltersProductIdsMap == null) {
					errorMsgs.add("Cache is currently empty. "
							+ "Please run ScheduledTask 'BuildOptimFilterProducts' and then run the report.");
					return null;
				}
			} else {
				this.sdFiltersProductIdsMap = SantReportingUtil.getSantReportingService(getDSConnection())
						.buildSDFilterProdIdsMap(this.optimization, mcType, Util.string2IntVector(mcIds));
			}

			// main logic
			return getReportOutput(errorMsgs);

		} catch (Exception e) {
			errorMsgs.add(e.getMessage());
			Log.error(this, e);
		}

		return null;
	}

	private DefaultReportOutput getReportOutput(@SuppressWarnings("rawtypes") Vector errorMsgs) throws Exception {

		SantOptimumReport optimumReport = null;
		ReportRow[] rows = null;
		DefaultReportOutput output = null;

		optimumReport = new SantOptimumReport();
		optimumReport.setReportTemplate(super.getReportTemplate());

		Map<CollateralConfig, List<SantOptimumReportItem>> santAllocationItems1 = getSantAllocationItems1(errorMsgs);

		if (santAllocationItems1.size() > 0) {
			List<SantOptimumReportItem> allocationsPropositions = generateLinesProposedByOptimizerRows(santAllocationItems1);
			rows = getReportOutputForOptimumReport(allocationsPropositions);
		}
		// generate the default output and set the rows
		output = new DefaultReportOutput(this);
		output.setRows(rows);

		return output;
	}

	/**
	 * For each allocation associated to a contract, it checks if it matches a category of the optimizater (SDF), if
	 * not, it generates a dummyLine (blank)
	 * 
	 * @param rows
	 *            generated by MarginCallAllocationDTOReport
	 * @return List with rows formatted. There will be total Num.Allocations multiply with Total Categories Optimization
	 *         algorithm
	 * @throws Exception
	 */
	private List<SantOptimumReportItem> generateLinesProposedByOptimizerRows(
			Map<CollateralConfig, List<SantOptimumReportItem>> santAllocationItemssMap) throws Exception {

		Map<Category, Double> categoryWeightMap = getOptimizationCategoryWeightMap();
		List<SantOptimumReportItem> allocationsPropositions = new ArrayList<SantOptimumReportItem>();

		for (CollateralConfig mcConfig : santAllocationItemssMap.keySet()) {

			if (Util.isEmpty(santAllocationItemssMap.get(mcConfig))) {
				// No allocations in this contract, but we still need to generate a line per Optimizer categpory
				createNoAllocationLines(allocationsPropositions, mcConfig, categoryWeightMap);
				continue;
			}

			List<SantOptimumReportItem> allocationItems = santAllocationItemssMap.get(mcConfig);

			Collection<SantOptimumReportItem> allocationsByCategory = getAllocationTotalValueByCategory(
					allocationItems, categoryWeightMap, isReassignCategories());

			if (!Util.isEmpty(allocationsByCategory)) {
				// for each optimization category
				for (Category currentCategory : categoryWeightMap.keySet()) {
					// Category currentCategory = categoryWeightMap.get(weight);
					SantOptimumReportItem categoryAllocation = retriveSuitableAllocation(allocationsByCategory,
							currentCategory);

					// Option 1: no allocations found for this category
					if (categoryAllocation == null) {
						SantOptimumReportItem dummyEntry = SantOptimumReportItem.generateDummyEntry(mcConfig,
								currentCategory);
						dummyEntry.setEligible(isCategoryContainEligibleSecurities(dummyEntry, mcConfig));
						allocationsPropositions.add(dummyEntry); // dummy line
						continue;
					} else {
						categoryAllocation.setOptimizationPrice(currentCategory.getWeight());
						categoryAllocation
								.setEligible(isCategoryContainEligibleSecurities(categoryAllocation, mcConfig));
						allocationsPropositions.add(categoryAllocation);

						// So the current categoryAllocation has been dealt with so delete it form the collection
						allocationsByCategory.remove(categoryAllocation);
						continue;
					}
				}

				// Anything left in the collection do not match with any optimizer config
				for (SantOptimumReportItem item : allocationsByCategory) {
					item.setEligible(isCategoryContainEligibleSecurities(item, mcConfig));

					if (item.getProductId() != 0) {
						if (Util.isEmpty(item.getOptCategoryName())) {
							item.setOptCategoryName("Manual");
						} else {
							item.setOptCategoryName(item.getOptCategoryName() + "-(No more an Optimizer category)");
						}
					} else {
						item.setOptCategoryName("Manual");
					}
					allocationsPropositions.add(item);
				}

			}
		}
		return allocationsPropositions;
	}

	private boolean isCategoryContainEligibleSecurities(SantOptimumReportItem item, CollateralConfig contract) {
		if (item != null) {
			if (this.sdFiltersProductIdsMap.get(item.getOptCategoryName()) != null) {
				List<Integer> prodIdsForCategory = this.sdFiltersProductIdsMap.get(item.getOptCategoryName());
				if (Util.isEmpty(prodIdsForCategory)) {
					return false;
				}

				List<StaticDataFilter> eligibilityFilters = contract.getEligibilityFilters();
				if (!Util.isEmpty(eligibilityFilters)) {
					for (StaticDataFilter filter : eligibilityFilters) {
						List<Integer> prodIdsForContractFilter = this.sdFiltersProductIdsMap.get(filter.getName());

						if (!Util.isEmpty(prodIdsForContractFilter)
								&& containsAny(prodIdsForContractFilter, prodIdsForCategory)) {
							return true;
						}
					}
				}

			} else {
				List<CollateralConfigCurrency> eligibleCurrencies = contract.getEligibleCurrencies();
				for (CollateralConfigCurrency ccyConfig : eligibleCurrencies) {
					if (ccyConfig.getCurrency().equals(item.getOptCategoryName())) {
						return true;
					}
				}

			}

		}

		return false;
	}

	private boolean containsAny(List<Integer> list1, List<Integer> list2) {
		for (Integer val : list2) {
			if (list1.contains(val)) {
				return true;
			}
		}
		return false;
	}

	private void createNoAllocationLines(List<SantOptimumReportItem> allocationsPropositions,
			CollateralConfig mcConfig, Map<Category, Double> categoryWeightMap) {
		for (Category currentCategory : categoryWeightMap.keySet()) {
			// Category currentCategory = categoryWeightMap.get(weight);
			// Add a Dummy Line
			SantOptimumReportItem dummyEntry = SantOptimumReportItem.generateDummyEntry(mcConfig, currentCategory);
			dummyEntry.setEligible(isCategoryContainEligibleSecurities(dummyEntry, mcConfig));
			allocationsPropositions.add(dummyEntry);

		}
	}

	/**
	 * This method categorises the allocations
	 * 
	 * @param sortedContractAllocationsMap
	 * @return
	 */
	private Collection<SantOptimumReportItem> getAllocationTotalValueByCategory(
			Collection<SantOptimumReportItem> allocationItems, Map<Category, Double> categoryWeightMap,
			boolean reassignCategory) {

		Map<String, SantOptimumReportItem> totalByCategoryMap = new HashMap<String, SantOptimumReportItem>();
		List<SantOptimumReportItem> manualItems = new ArrayList<SantOptimumReportItem>();

		for (SantOptimumReportItem item : allocationItems) {
			// MarginCallAllocationFacade allocation = item.getAllocation();
			String categoryName = item.getOptCategoryName();
			if (reassignCategory || Util.isEmpty(item.getOptCategoryName())) {
				if (item.getProductId() != 0) { // Security Allocation
					for (Category category : categoryWeightMap.keySet()) {
						StaticDataFilter securityFilter = category.getSecurityFilter();

						// We are only re-assigning SecurityAllocation, so we don't need Categories of type Cash
						if (!category.getType().equals("Security") || (securityFilter == null)) {
							continue;
						}
						Product security = BOCache.getExchangedTradedProduct(DSConnection.getDefault(),
								item.getProductId());

						if (security == null) {
							break;
						}

						if (securityFilter.accept(null, security)) {
							// We found a different category after optimizer has been run
							if (!category.getName().equals(categoryName)) {
								categoryName = category.getName();
								item.setOptCategoryName(category.getName());

							}
							break;
						}
					}
				}
			}

			if (Util.isEmpty(categoryName)) {
				item.setCategoryTotal(item.getContractValue());
				manualItems.add(item);
			} else if (totalByCategoryMap.containsKey((categoryName))) {
				SantOptimumReportItem existingItem = totalByCategoryMap.get(categoryName);
				existingItem.addToCategoryTotal(item.getContractValue());

			} else {
				item.setCategoryTotal(item.getContractValue());
				totalByCategoryMap.put(categoryName, item);
			}
		}

		List<SantOptimumReportItem> finalList = new ArrayList<SantOptimumReportItem>();

		for (SantOptimumReportItem item : totalByCategoryMap.values()) {
			finalList.add(item);
		}
		if (manualItems.size() > 0) {
			finalList.addAll(manualItems);
		}
		return finalList;

	}

	/**
	 * Finds a matching allocatoin for the given Category. If no match found return null.
	 * 
	 * @param currentCategory
	 *            Collection of allocations
	 * @param currentCategory
	 *            current category under study
	 * @return next suitable allocation
	 */
	private SantOptimumReportItem retriveSuitableAllocation(Collection<SantOptimumReportItem> allocationsByCategory,
			Category currentCategory) {

		if (!Util.isEmpty(allocationsByCategory)) {
			for (SantOptimumReportItem allocationItem : allocationsByCategory) {
				if ((allocationItem.getOptCategoryName() != null)
						&& allocationItem.getOptCategoryName().equals(currentCategory.getName())) {
					return allocationItem;
				}
			}
		}

		return null;
	}

	/**
	 * @return sorted map by weight of weightedCategory
	 * @see com.calypso.tk.refdata.optimization.impl.WeightedCategory
	 */
	private Map<Category, Double> getOptimizationCategoryWeightMap() {

		List<Category> categories = this.optimization.getTarget().getCategories();
		Map<Category, Double> categoryWeightMap = new TreeMap<Category, Double>();

		for (Category wc : categories) {
			categoryWeightMap.put(wc, wc.getWeight());
		}

		return categoryWeightMap;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map<CollateralConfig, List<SantOptimumReportItem>> getSantAllocationItems1(Vector errorMsgs)
			throws Exception {

		String mcIds = (String) getReportTemplate().get(MarginCallEntryDTOReportTemplate.MARGIN_CALL_CONFIG_IDS);
		Vector<String> mcType = (Vector<String>) getReportTemplate().get(SantOptimumReportTemplate.CONTRACT_TYPE);
		Map<CollateralConfig, List<SantOptimumReportItem>> optimReportItems = SantReportingUtil
				.getSantReportingService(getDSConnection()).getOptimReportItems(Util.string2IntVector(mcIds), mcType,
						this.processDate);

		return optimReportItems;

	}

	/**
	 * @param allocationsPropositions
	 *            final list with all the allocations to be shown
	 * @return
	 */
	private ReportRow[] getReportOutputForOptimumReport(List<SantOptimumReportItem> allocationsPropositions) {

		ReportRow[] rows = new ReportRow[allocationsPropositions.size()];
		// add new OptimumEntries to rows
		for (int i = 0; i < allocationsPropositions.size(); i++) {
			SantOptimumReportItem santOptimumReportItem = allocationsPropositions.get(i);
			SantOptimumReportItemLight lightItem = new SantOptimumReportItemLight(santOptimumReportItem);
			ReportRow row = new ReportRow(lightItem, SantOptimumReportTemplate.OPTIMIZATION_REPORT);
			rows[i] = row;
		}

		return rows;
	}

	/**
	 * @return the process date from the template
	 */
	private JDate getProcessDate() {
		JDate valDate = null;
		String dateAsString = (String) getReportTemplate().get(MarginCallEntryDTOReportTemplate.PROCESS_START_DATE);
		if (Util.isEmpty(dateAsString)) {
			valDate = getReportTemplate().getValDate();
			getReportTemplate().put(MarginCallEntryDTOReportTemplate.PROCESS_START_DATE, valDate.toString());
		} else {
			valDate = JDate.valueOf(dateAsString);
		}
		return valDate;
	}

	private boolean isReassignCategories() {
		String s = (String) getReportTemplate().get(SantOptimumReportTemplate.REASSIGN_CATEGORIES);

		if (!Util.isEmpty(s) && s.equals("true")) {
			return true;
		}
		return false;
	}

	/**
	 * @return the optimization configuration choosen from the template
	 */
	@SuppressWarnings("rawtypes")
	private OptimizationConfiguration getOptimizationConfiguration() {

		OptimizationConfiguration result = null;
		// chapucita temporal, pendiente de definir con cedric.
		String name = null;
		if (getReportTemplate().get(SantOptimumReportTemplate.OPTIMIZATION_CONFIGURATION) != null) {
			name = (String) (((Vector) getReportTemplate().get(SantOptimumReportTemplate.OPTIMIZATION_CONFIGURATION))
					.get(0)); // Right now I get only the first configuration. Chapuza TBD - ask Cedric
		}

		if (!Util.isEmpty(name)) {
			try {
				result = ServiceRegistry.getDefault().getCollateralDataServer().loadOptimizationConfiguration(name);
			} catch (Exception e) {
				Log.error(this, e);
			}
		}

		return result;
	}

}
