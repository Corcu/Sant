/* Actualizado por David Porras Mart?nez 22-11-11 */

package calypsox.tk.report;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.ControlMErrorLogger;
import calypsox.util.CheckRowsNumberReport;
import calypsox.util.SantReportingUtil;
import calypsox.util.collateral.CollateralManagerUtil;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.MarginCallReport;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MarginCallPositionUtil;
import com.enterprisedt.bouncycastle.pqc.crypto.mceliece.McElieceCipher;

import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;


public class KGR_Collateral_MarginCallReport extends MarginCallReport implements CheckRowsNumberReport {

    private static final long serialVersionUID = -8296629823865410802L;
    public static final String PO = "PO";
    public static final String LEGAL_ENTITY_ROLE_PROCESSING_ORG = "ProcessingOrg";
    private static final String AGREEMENT_STATUS_CLOSED = "CLOSED";
    private static final String CONTRACT_TYPE_MMOO = "MMOO";
    private static final String EXCLUDE_KGR = "EXCLUDE_MMOO_FROM_KGR_EXTRACTION";
    private static final String EXCLUDE_KGR_YES = "Yes";
    private final Map<Integer, Boolean> contractsDisputeStatus = new HashMap<Integer, Boolean>();
    public static final String CSA_FACADE = "CSA_FACADE";
    private List<CollateralConfig> mccConfigs = new ArrayList<CollateralConfig>();


    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public ReportOutput load(final Vector errorMsgsP) {

        final DefaultReportOutput output = new StandardReportOutput(this);
        final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();
        Collection<LegalEntity> legalEntities = new Vector<LegalEntity>();
        List<CollateralConfig> marginCalls = new Vector<CollateralConfig>();
        final MarginCallPositionUtil mcPositionUtil = new MarginCallPositionUtil();

        JDate jdate = null;
        final DSConnection dsConn = getDSConnection();
        PricingEnv pricingEnv = getPricingEnv();
        if (pricingEnv == null) {
            String priceenv = "";
            if (null != getReportTemplate().getAttributes().get("PricingEnvName")) {
                priceenv = getReportTemplate().getAttributes().get("PricingEnvName").toString();
                try {
                    pricingEnv = DSConnection.getDefault().getRemoteMarketData().getPricingEnv(priceenv);
                } catch (CalypsoServiceException e) {
                    Log.error(KGR_Collateral_MarginCallReport.class, "Cannot get pricingEnv for: " + priceenv + "");
                    Log.error(this, e); //sonar
                }
            } else {
                Log.warn(KGR_Collateral_MarginCallReport.class, "Cannot get pricingEnv for null PE");
            }
        }
        final ReportTemplate reportTemp = getReportTemplate();

        // We retrieve the different columns specified for the current report,
        // and the attribute 'Processing Org' to filter the data retrieved.
        final String[] columns = reportTemp.getColumns();
        // final Attributes attributes = reportTemp.getAttributes();
        Vector holidays = reportTemp.getHolidays();
        final ArrayList<Integer> blackList = new ArrayList<Integer>();
        final ArrayList<Integer> blackListCSD = new ArrayList<Integer>();
        // GSM 15/03/2016 - Add Source System for IRIS
        String irisSourceSystem = (String) reportTemp.get(KGR_Collateral_MarginCallReportTemplate.SOURCE_SYSTEM);
        if (Util.isEmpty(irisSourceSystem)) {
            Log.error(this, "Source System for IRIS is empty, using MADRID as default");
            irisSourceSystem = KGR_Collateral_MarginCallLogic.CONCILIA_FIELD;
        }
        //String collateralMaturityDate - field 16
        String irisMaturityDays = (String) reportTemp.get(KGR_Collateral_MarginCallReportTemplate.MATURITY_OFFSET);
        //in Integer format
        Integer maturityOffset = -1;
        if (Util.isEmpty(irisMaturityDays)) {
            Log.error(this, "MaturityOfsset for IRIS is empty, using MADRID 7 days default");
            maturityOffset = KGR_Collateral_MarginCallLogic.DEFAULT_MATURE_DAYS;

        } else {
            try {
                maturityOffset = Integer.parseInt(irisMaturityDays.trim());
            } catch (NumberFormatException e) {
                Log.error(this, "MaturityOfsset for IRIS is not a number, using MADRID 7 days default");
                maturityOffset = KGR_Collateral_MarginCallLogic.DEFAULT_MATURE_DAYS;
            }
        }

        try {

            // get date
            jdate = reportTemp.getValDate();
            initEntriesCache(jdate);
            // get legal entities
            // legalEntities = getLegalEntities((String) attributes.get(PO), dsConn);
            // get legal entities // GSM 24/07/15. SBNA Multi-PO filter
            legalEntities = CollateralUtilities.filterLEPoByTemplate(reportTemp);

            if (!Util.isEmpty(legalEntities)) {
                for (LegalEntity legalEntity : legalEntities) {
                    if (legalEntity != null) {
                        // get contracts
                        marginCalls = loadContracts(legalEntity.getId());
                        if (!Util.isEmpty(marginCalls)) {
                            for (CollateralConfig marginCall : marginCalls) {
                                if (marginCall != null) {
                                    // check contract
                                    if (checkContract(marginCall, blackList)) {
                                        // get report row for contract
                                        final Vector<KGR_Collateral_MarginCallItem> marginCallReportRows = KGR_Collateral_MarginCallLogic
                                                .getReportRows(marginCall, mcPositionUtil, jdate, dsConn, errorMsgsP,
                                                        columns, pricingEnv, getServiceRegistry(), holidays,
                                                        this.contractsDisputeStatus, irisSourceSystem, maturityOffset);

                                        for (KGR_Collateral_MarginCallItem mcItem : marginCallReportRows) {
                                            if (!(!Util.isEmpty(mcItem.getGlobalId()) && "CSD".equals(mcItem.getContractType()))) {
                                            	SentinelStatusWrapper sentinelWrapper = getSentinelBlockingStatus(mcItem.getMarginCallContract());
                                            	updateMCBeanWithSentinelStatus(mcItem, sentinelWrapper, columns);
                                                ReportRow row = new ReportRow(mcItem);
                                                reportRows.add(row);
                                            }
                                            //Add the CSDs (PO) y (CPTY) to de report from (VM) with Facade
                                            if (!Util.isEmpty(mcItem.getGlobalId()) && "CSA".equals(mcItem.getContractType())) {

                                                int idFacade = Integer.parseInt(mcItem.getGlobalId());
                                                CollateralConfig facade = ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfig(idFacade);

                                                if (CSA_FACADE.equals(facade.getContractType())) {

                                                    if (!Util.isEmpty(facade.getAdditionalField("IM_SUB_CONTRACTS"))) {

                                                        String[] ids = facade.getAdditionalField("IM_SUB_CONTRACTS").split(",");
                                                        List<Integer> contractIds = new ArrayList<Integer>();

                                                        for (String id : ids) {
                                                            contractIds.add(Integer.valueOf(id));
                                                        }
                                                        MarginCallConfigFilter mcFilter = new MarginCallConfigFilter();
                                                        mcFilter.setContractIds(contractIds);

                                                        List<CollateralConfig> listCC = CollateralManagerUtil.loadCollateralConfigs(mcFilter);
                                                        SentinelStatusWrapper sentinelWrapper = getSentinelBlockingStatus(listCC);
                                                        updateMCBeanWithSentinelStatus(mcItem, sentinelWrapper, columns);
                                                        for (CollateralConfig cc : listCC) {
                                                            if ("CSD".equals(cc.getContractType())) {
                                                                //Check to not duplicate contracts in the report
                                                                if (checkContract(cc, blackListCSD)) {
                                                                    /*Changed to extract the CSD contract product list*/
                                                                    final Vector<KGR_Collateral_MarginCallItem> cdsReportRows = KGR_Collateral_MarginCallLogic
                                                                            .getReportRows(cc, mcPositionUtil, jdate, dsConn, errorMsgsP,
                                                                                    columns, pricingEnv, getServiceRegistry(), holidays,
                                                                                    this.contractsDisputeStatus, irisSourceSystem, maturityOffset);

                                                                    for (KGR_Collateral_MarginCallItem item : cdsReportRows) {
                                                                        updateMCBeanWithSentinelStatus(item, sentinelWrapper,columns);
                                                                        ReportRow row = new ReportRow(item);
                                                                        reportRows.add(row);
                                                                    }
                                                                    /*temp*/
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            //////fin nuevo
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // set report rows on output
            output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));

            //Generate a task exception if the number of rows is out of an umbral defined
            HashMap<String, String> value = SantReportingUtil.getSchedTaskNameOrReportTemplate(this);
            checkAndGenerateTaskReport(output, value);

            return output;

        } catch (final RemoteException e) {
            Log.error(this, "KGR_Collateral_MarginCallReport - " + e.getMessage());
            Log.error(this, e); //sonar
            ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, "Not document generated");
        }

        return null;
    }


    private void updateMCBeanWithSentinelStatus(KGR_Collateral_MarginCallItem mcItem, SentinelStatusWrapper sentinelWrapper, String[] columns) {
        boolean isSentinelBlocked;
        if (isOwnerTemplate(columns)) {
            isSentinelBlocked = sentinelWrapper.isSentinelBlockedPO(mcItem);
        } else {
            isSentinelBlocked = sentinelWrapper.isSentinelBlockedCPTY(mcItem);
        }
        mcItem.setIsSentinelBlocked(String.valueOf(isSentinelBlocked));
    }

    private boolean isOwnerTemplate(String[] columns) {
        return Optional.ofNullable(columns).map(col -> col[1])
                .map(colName -> colName.equals(KGR_Collateral_MarginCallLogic.OWNER_SENTINEL)).orElse(false);
    }

    /**
     * CSD (PO) must be found to look for SentinelBlocking contract's additional fields
     *
     * @param listCC
     */
    private SentinelStatusWrapper getSentinelBlockingStatus(List<CollateralConfig> listCC) {
        CollateralConfig csdPO = null;
        for (CollateralConfig cc : listCC) {
            String csdType=Optional.ofNullable(cc.getAdditionalField("IM_CSD_TYPE")).orElse("");
            if ("CSD".equals(cc.getContractType()) && PO.equals(csdType)) {
                csdPO = cc;
                break;
            }
        }
        
        List<String> blockedPO = ccAdditionalFieldToList(csdPO, "SENTINEL_BLOCKED_PO");
        List<String> blockedCPTY = ccAdditionalFieldToList(csdPO, "SENTINEL_BLOCKED_CPTY");
        
        if (blockedCPTY.isEmpty()) {
        	
        	String af = csdPO != null ? csdPO.getAdditionalField("TH_MTA_€_NY_DISABLED") : "";
        	
        	if (af != null && !af.isEmpty() && Boolean.parseBoolean(af)) {
        		blockedCPTY = CollateralUtilities.getLEandAdditionalLE(csdPO);
        	}
        	
        }

        return new SentinelStatusWrapper(blockedPO, blockedCPTY);

    }
    
    private SentinelStatusWrapper getSentinelBlockingStatus(String mcName) {
        CollateralConfig csdPO;
		try {
			if (mccConfigs.isEmpty()) {
				mccConfigs = ServiceRegistry.getDefault().getCollateralDataServer().getAllMarginCallConfig();
			}
			csdPO = mccConfigs
					.stream()
					.filter(mc -> mc.getName().equals(mcName))
					.collect(Collectors.toList()).get(0);
			
			String csdType=Optional.ofNullable(csdPO.getAdditionalField("IM_CSD_TYPE")).orElse("");
			if ("CSD".equals(csdPO.getContractType()) && PO.equals(csdType)) {
				List<String> blockedPO = ccAdditionalFieldToList(csdPO, "SENTINEL_BLOCKED_PO");
				List<String> blockedCPTY = ccAdditionalFieldToList(csdPO, "SENTINEL_BLOCKED_CPTY");
				
				return new SentinelStatusWrapper(blockedPO, blockedCPTY);
			}
		} catch (Exception e) {
			Log.error(e, e.getMessage());
		}
        
		return new SentinelStatusWrapper(new ArrayList<String>(), new ArrayList<String>());

    }
    

    private List<String> ccAdditionalFieldToList(CollateralConfig cc, String afName) {
        return Optional.ofNullable(cc).map(mc -> mc.getAdditionalField(afName))
                .map(this::stringToList).orElse(new ArrayList<>());
    }

    private List<String> stringToList(String inputStr) {
        return Optional.ofNullable(inputStr).map(str -> str.split(";"))
                .map(Arrays::asList).orElse(new ArrayList<>());
    }

    private boolean checkContract(CollateralConfig marginCall, ArrayList<Integer> blackList) {

        // check status
        if (AGREEMENT_STATUS_CLOSED.equals(marginCall.getAgreementStatus())) {
            return false;
        }
        // check black list
        if (blackList.contains(marginCall.getId())) {
            return false;
        }

        // Exclude CSA Facade
        if (CSA_FACADE.equals(marginCall.getContractType())) {
            return false;
        }

        // add
        blackList.add(marginCall.getId());

        return true;
    }

    private List<CollateralConfig> loadContracts(int ownerId) throws RemoteException {

        MarginCallConfigFilter mcFilter = new MarginCallConfigFilter();

        List<Integer> list = new ArrayList<>();
        list.add(ownerId);
        mcFilter.setProcessingOrgIds(list);
        /*List<Integer> listC = new ArrayList<>();
        listC.add(85230);
        mcFilter.setLegalEntityIds(listC);*/

        List<CollateralConfig> marginCallConfig_prov = CollateralManagerUtil.loadCollateralConfigs(mcFilter);

        List<CollateralConfig> marginCallConfigs = new ArrayList<>();

        for (CollateralConfig contract : marginCallConfig_prov) {
            if (!CONTRACT_TYPE_MMOO.equals(contract.getContractType())
                    || (CONTRACT_TYPE_MMOO.equals(contract.getContractType()) && !EXCLUDE_KGR_YES.equals(contract
                    .getAdditionalField(EXCLUDE_KGR)))) {
                marginCallConfigs.add(contract);
            }
        }

        return marginCallConfigs;
    }

    public void initEntriesCache(JDate processingDate) {
        List<MarginCallEntryDTO> mcentriesDTOs;
        try {
            mcentriesDTOs = ServiceRegistry
                    .getDefault()
                    .getDashBoardServer()
                    .loadMarginCallEntries(
                            "trunc(margin_call_entries.process_datetime) = " + Util.date2SQLString(processingDate),
                            Arrays.asList("margin_call_entries"));
            if (!Util.isEmpty(mcentriesDTOs)) {
                for (MarginCallEntryDTO dto : mcentriesDTOs) {
                    this.contractsDisputeStatus.put(dto.getCollateralConfigId(), dto.getStatus().startsWith("DISPUTE"));
                }
            }
        } catch (RemoteException e) {
            Log.error(this, e);
        }

    }

    private class SentinelStatusWrapper {
        List<String> blockedPO;
        List<String> blockedCPTY;

        public SentinelStatusWrapper(List<String> blockedPO, List<String> blockedCPTY) {
            this.blockedPO = blockedPO;
            this.blockedCPTY = blockedCPTY;
        }

        public boolean isSentinelBlockedPO(KGR_Collateral_MarginCallItem mcItem) {
            return blockedPO.contains(mcItem.getOwner());
        }

        public boolean isSentinelBlockedCPTY(KGR_Collateral_MarginCallItem mcItem) {
            return blockedCPTY.contains(mcItem.getCounterparty());
        }

    }

}
