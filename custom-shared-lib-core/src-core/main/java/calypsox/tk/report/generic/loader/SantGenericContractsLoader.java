/*
 *
 * Copyright (c) ISBAN: Ingenierï¿½a de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report.generic.loader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.service.CollateralServiceRegistry;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.SantReportingUtil;

public class SantGenericContractsLoader extends SantEnableThread<Integer, CollateralConfig> {

    private static final String COMMA_STR_SEPARATOR = ",";

    private static final String OPEN_STATUS = "OPEN";

    private static final String ZERO_STRING_VALUE = "0";

    private static final String EMPTY_STRING = "";

    private final String agreementIds;

    // everis - SantPortfolioBreakdown Optimization.
    private static final String domainMaxNumContractsPerList = "portfolioBreakdownMaxContractsPerThread";
    private static final String domainValueDefaultMaxNumContractsPerList = "defaultNumContracts";
    private Map<Integer, CollateralConfig> fatherContractsDataMap = new HashMap<Integer, CollateralConfig>();
    private Map<Integer, CollateralConfig> fatherRealSettleContractsDataMap = new HashMap<Integer, CollateralConfig>();

    private ArrayList<String> stringArrayOpenAgreementIds = new ArrayList<String>();
    private ArrayList<String> stringArrayRealSettleOpenAgreementIds = new ArrayList<String>();

    protected Map<Integer, CollateralConfig> contractsMap = new HashMap<>();
    protected Map<Integer, CollateralConfig> realSettleContractsMap = new HashMap<>();

    private int maxNumContracts = 0;

    // everis - SantPortfolioBreakdown Optimization.

    public SantGenericContractsLoader(boolean enableThreading, final ReportTemplate template, final String agreementIds) {
        super(template, enableThreading);
        this.agreementIds = agreementIds;
        String maxContractsString = ZERO_STRING_VALUE;
        if (template != null && template.getReportTemplateName() != null) {
            maxContractsString = CollateralUtilities.getDomainValueComment(domainMaxNumContractsPerList, template.getReportTemplateName().getTemplateName());
        }
        try {
            if (maxContractsString == null || maxContractsString.trim().equals(EMPTY_STRING)) {
                maxContractsString = CollateralUtilities.getDomainValueComment(domainMaxNumContractsPerList, domainValueDefaultMaxNumContractsPerList);
                if (maxContractsString == null) {
                    maxNumContracts = 0;
                } else {
                    maxNumContracts = Integer.parseInt(maxContractsString);
                }
            } else {
                maxNumContracts = Integer.parseInt(maxContractsString);
            }
        } catch (NumberFormatException e) {
            maxNumContracts = 0;
        } finally {
            if (maxNumContracts < 0) {
                maxNumContracts = 0;
            }
        }

    }

    public Map<Integer, CollateralConfig> getFatherContractsDataAsMap() {
        return fatherContractsDataMap;
    }

    public Map<Integer, CollateralConfig> getFatherRealSettleContractsDataMap() {
        return fatherRealSettleContractsDataMap;
    }

    // everis - SantPortfolioBreakdown Optimization.
    @Override
    protected Map<Integer, CollateralConfig> getDataMapFromDataList() {
        for (int i = 0; i < this.dataList.size(); i++) {
            CollateralConfig mcc = this.dataList.get(i);
            this.dataMap.put(mcc.getId(), mcc);
        }
        return this.dataMap;
    }

    // everis - SantPortfolioBreakdown Optimization.
    public String[] getStringListAgreementIds() {
        return (String[]) stringArrayOpenAgreementIds.toArray(new String[stringArrayOpenAgreementIds.size()]);
    }

    public String[] getStringListRealSettleOpenAgreementIds() {
        return (String[]) stringArrayRealSettleOpenAgreementIds.toArray(new String[stringArrayRealSettleOpenAgreementIds.size()]);
    }

    public Map<Integer, CollateralConfig> getContractsMap() {
        return contractsMap;
    }

    public Map<Integer, CollateralConfig> getRealSettleContractsMap() {
        return realSettleContractsMap;
    }

    // everis - SantPortfolioBreakdown Optimization.
    @Override
    protected void loadData() {

        Integer valAgentId = (Integer) this.template.get(SantGenericTradeReportTemplate.VALUATION_AGENT);
        if (valAgentId == null) {
            valAgentId = 0;
        }

        final String agrType = (String) this.template.get(SantGenericTradeReportTemplate.AGREEMENT_TYPE);

        // GSM 05/08/15. SBNA Multi-PO filter
        // everis - SantPortfolioBreakdown Optimization. Not Used.
//		final String poAgrStr = CollateralUtilities.filterPoIdsByTemplate(this.template);
        // final String poAgrStr = (String) this.template.get(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);

//		Vector<String> poAgrIds = null;
//		if (!Util.isEmpty(poAgrStr)) {
//			poAgrIds = Util.string2Vector(poAgrStr);
//		}
        // everis - SantPortfolioBreakdown Optimization.


        Collection<CollateralConfig> marginCallConfigs = new Vector<CollateralConfig>();

        if (!(Util.isEmpty(this.agreementIds))) {
            try {
                marginCallConfigs = SantReportingUtil.getSantReportingService(DSConnection.getDefault())
                        .getMarginCallConfigByIds(Util.string2IntVector(this.agreementIds)).values();
            } catch (final Exception e) {
                Log.error(this, "Cannot retrieve Margin Call Contracts", e);
            }

        } else {

            // 1. load contracts based on POId
            String poName = this.template.getAttributes().get("ProcessingOrg");
            if (!Util.isEmpty(poName)) {
                // everis - SantPortfolioBreakdown Optimization. Innecessary loop because all contracts are loaded in the first iteration.

//				for (int i = 0; i < poAgrIds.size(); i++) {
                // everis - SantPortfolioBreakdown Optimization. Not used.

//					final String poIDStr = poAgrIds.get(i);
//					final int poId = Integer.parseInt(poIDStr);
                // everis - SantPortfolioBreakdown Optimization.
                try {
                    final CollateralServiceRegistry srvReg = ServiceRegistry.getDefault();
                    MarginCallConfigFilter contractFilter = new MarginCallConfigFilter();
                    contractFilter.setProcessingOrg(poName);

                    List<CollateralConfig> temp = srvReg.getCollateralDataServer().getMarginCallConfigs(contractFilter, null);
//						final Collection<CollateralConfig> temp = srvReg.getCollateralDataServer()
//								.getAllMarginCallConfig(poId, 0);
                    marginCallConfigs.addAll(temp);
                } catch (final Exception e) {
                    Log.error(this, "Cannot retrieve Margin Call Contracts", e);
                }
//				}

            } else {

                try {
                    // 2. Retrieve ALL contracts
                    final CollateralServiceRegistry srvReg = ServiceRegistry.getDefault();
                    marginCallConfigs = srvReg.getCollateralDataServer().getAllMarginCallConfig();
                    // GSM: Not working fine since new Core 1.5.6
                    // marginCallConfigs = DSConnection.getDefault().getRemoteReferenceData().getAllMarginCallConfig(0,
                    // 0);

                } catch (final Exception e) {
                    Log.error(this, "Cannot retrieve Margin Call Contracts", e);
                }
            }
        }

        // Filter out contracts not matched for criteria
        if (marginCallConfigs.size() > 0) {

            StringBuilder sbOpenAgreementIds = new StringBuilder();
            StringBuilder sbOpenRealSettleAgreementIds = new StringBuilder();

            int countContracts = 0;
            Iterator<CollateralConfig> it = marginCallConfigs.iterator();
            while (it.hasNext()) {
                final CollateralConfig marginCallConfig = it.next();
                if (valAgentId > 0) {
                    if (marginCallConfig.getValuationAgentId() != valAgentId) {
                        continue;
                    }
                }
                if (!Util.isEmpty(agrType) && !marginCallConfig.getContractType().equals(agrType)) {
                    continue;
                }
                // Add to Map if not in the map already
                if (this.dataMap.get(marginCallConfig.getId()) == null) {
                    this.dataMap.put(marginCallConfig.getId(), marginCallConfig);

                    // everis - SantPortfolioBreakdown Optimization.
                    if (marginCallConfig.getAgreementStatus().equals(OPEN_STATUS)) {
                        final Integer fatherId = marginCallConfig.getMasterAgreementId();

                        if (marginCallConfig.getEffDateType().equalsIgnoreCase("REAL_SETTLEMENT")) {
                            this.realSettleContractsMap.put(marginCallConfig.getId(), marginCallConfig);
                            if (sbOpenRealSettleAgreementIds.length() > 0)
                                sbOpenRealSettleAgreementIds.append(COMMA_STR_SEPARATOR);
                            sbOpenRealSettleAgreementIds.append(fatherId);
                            if (maxNumContracts > 0 && countContracts >= maxNumContracts) {
                                stringArrayRealSettleOpenAgreementIds.add(sbOpenRealSettleAgreementIds.toString());
                                sbOpenRealSettleAgreementIds = new StringBuilder();
                                countContracts = 0;
                            }
                        } else {
                            this.contractsMap.put(marginCallConfig.getId(), marginCallConfig);

                            if (sbOpenAgreementIds.length() > 0) sbOpenAgreementIds.append(COMMA_STR_SEPARATOR);
                            sbOpenAgreementIds.append(fatherId);
                            if (maxNumContracts > 0 && countContracts >= maxNumContracts) {
                                stringArrayOpenAgreementIds.add(sbOpenAgreementIds.toString());
                                sbOpenAgreementIds = new StringBuilder();
                                countContracts = 0;
                            }
                        }
                        countContracts++;
                    }
                    if (marginCallConfig.getMasterAgreementId() > 0 && marginCallConfig.getMasterAgreement() != null && fatherContractsDataMap.get(marginCallConfig.getMasterAgreementId()) == null) {
                        if (marginCallConfig.getEffDateType().equalsIgnoreCase("REAL_SETTLEMENT")) {
                            fatherRealSettleContractsDataMap.put(marginCallConfig.getMasterAgreementId(), marginCallConfig.getMasterAgreement());
                        } else {
                            fatherContractsDataMap.put(marginCallConfig.getMasterAgreementId(), marginCallConfig.getMasterAgreement());
                        }
                    }
                }
            }
            // everis - SantPortfolioBreakdown Optimization.
            if (sbOpenAgreementIds.length() > 0 && (countContracts < maxNumContracts || maxNumContracts == 0)) {
                stringArrayOpenAgreementIds.add(sbOpenAgreementIds.toString());
            }
            if (sbOpenRealSettleAgreementIds.length() > 0 && (countContracts < maxNumContracts || maxNumContracts == 0)) {
                stringArrayRealSettleOpenAgreementIds.add(sbOpenRealSettleAgreementIds.toString());
            }
        }
    }
}