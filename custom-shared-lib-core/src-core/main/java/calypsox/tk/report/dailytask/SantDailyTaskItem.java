/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report.dailytask;

import calypsox.tk.report.SantDailyTaskReportStyle;
import calypsox.tk.report.generic.loader.margincall.SantMarginCallEntry;
import calypsox.util.SantReportingUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.dto.MarginCallAllocationDTO;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.*;

import static calypsox.tk.core.CollateralStaticAttributes.MC_ENTRY_PO_MTM;

public class SantDailyTaskItem {

    private String entryStatus;// Entry Status
    private String marginCallSituation;// Margin Call Situation
    private JDate processDate;// Process Date
    private JDate eventDate;// Event Date
    private Double executedAllocAmount;// Executed Allocation Amount
    private Double notExecutedAllocAmount;// Not Executed Allocation Amount
    private Double notAllocatedAmount;// Not allocated amount
    private String baseCurrency;// Base Currency
    private Double grossExposurePriorTolerance;// Gross Exposure (Prior Tolerance)
    private Double grossExposureAfterTolerance;// Gross Exposure (After Tolerance)
    private Double independent;// Independent
    private Double threshold;// Threshold
    private Double minimumTransfer;// Minimum Transfer
    private Double balance;// Balance
    private String agreementName;// Collateral Agreement
    private String headClone;// Head/Clone Ind.
    private String agreementDescription;// Coll Agreement Desc
    private String owner;// Owner
    private String valuationAgent;// Valuation Agent
    private String calcAgentId;// Calc Agent Id
    private Double marginCall;// Margin Call
    private Double marginCallCalc;// Margin Call Calc

    private final List<String> pendingStatusList;

    private final Map<String, Object> columnMap = new HashMap<String, Object>();

    public SantDailyTaskItem(SantMarginCallEntry santEntry, List<String> MARGIN_CALL_NOT_CALCULATED_STATUS) {
        this.pendingStatusList = MARGIN_CALL_NOT_CALCULATED_STATUS;
        build(santEntry);
        buildMap();
    }

    private void build(SantMarginCallEntry santEntry) {

        MarginCallEntryDTO entryDTO = santEntry.getEntry();
        CollateralConfig contract = santEntry.getMarginCallConfig();
        computeAllocations(entryDTO);

        this.entryStatus = entryDTO.getStatus();
        this.marginCallSituation = this.pendingStatusList.contains(entryDTO.getStatus()) ? "Pending" : "Valid";
        this.processDate = entryDTO.getProcessDatetime().getJDate(TimeZone.getDefault());
        // eventDate
        // executedAllocAmount
        // notExecutedAllocAmount
        // notAllocatedAmount
        this.baseCurrency = contract.getCurrency();
        this.grossExposurePriorTolerance = entryDTO.getAttribute(MC_ENTRY_PO_MTM) == null ? entryDTO.getNetBalance()
                : (Double) entryDTO.getAttribute(MC_ENTRY_PO_MTM);
        this.grossExposureAfterTolerance = entryDTO.getNetBalance();
        this.independent = entryDTO.getIndependentAmount();
        this.threshold = entryDTO.getThresholdAmount();
        this.minimumTransfer = entryDTO.getMTAAmount();
        this.balance = entryDTO.getPreviousTotalMargin();
        this.agreementName = contract.getName();
        this.headClone = contract.getAdditionalField("HEAD_CLONE");
        this.agreementDescription = contract.getLegalEntity().getName();
        this.owner = contract.getProcessingOrg().getCode();
        this.valuationAgent = getValuationAgent(contract);
        this.calcAgentId = contract.getAdditionalField("CALC_AGENT");
        this.marginCall = entryDTO.getGlobalRequiredMargin();
        this.marginCallCalc = entryDTO.getGlobalRequiredMarginCalc();
    }

    private void computeAllocations(MarginCallEntryDTO entryDTO) {
        this.executedAllocAmount = entryDTO.getDailyCashMargin() + entryDTO.getDailySecurityMargin();
        List<MarginCallAllocationDTO> allocs = new ArrayList<>();
        allocs.addAll(entryDTO.getCashAllocations());
        allocs.addAll(entryDTO.getSecurityAllocations());

        Set<Long> tradeIds = new HashSet<>();
        this.notExecutedAllocAmount = 0.0d;
        for (MarginCallAllocationDTO alloc : allocs) {
            if ("Cancelled".equals(alloc.getStatus())) {
                continue;
            }
            if (alloc.getTradeId() <= 0) {
                this.notExecutedAllocAmount += alloc.getContractValue();
            } else {
                tradeIds.add(alloc.getTradeId());
            }
        }
        this.notAllocatedAmount = entryDTO.getGlobalRequiredMargin() - this.executedAllocAmount;
        this.eventDate = getEventDate(tradeIds);

    }

    private JDate getEventDate(Set<Long> tradeIds) {
        if (tradeIds.isEmpty()) {
            return null;
        }
        final int MAX_SQL = 1000;
        List<Long> tradeIdsList = new ArrayList<>(tradeIds);
        StringBuilder query = new StringBuilder(" select MAX(trade.entered_date) from trade where trade.trade_id in (");
        int start = 0;

        for (int i = 0; i <= (tradeIdsList.size() / MAX_SQL); i++) {
            int end = (i + 1) * MAX_SQL;
            if (end > tradeIdsList.size()) {
                end = tradeIdsList.size();
            }
            final List<Long> subList = tradeIdsList.subList(start, end);
            start = end;
            if (i >= MAX_SQL) {
                query.append(" or trade.trade_id in (");
            }
            query.append(Util.collectionToString(subList)).append(") ");
        }

        Vector<?> result = null;
        try {
            result = SantReportingUtil.getSantReportingService(DSConnection.getDefault()).executeSelectSQL(
                    query.toString());
        } catch (RemoteException e) {
            Log.error(this, " Cannot retrieve entered_date ", e);
        }
        if (!Util.isEmpty(result) && (result.size() == 3)) {
            JDatetime datetime = (JDatetime) ((Vector<?>) result.get(2)).get(0);
            return datetime.getJDate(TimeZone.getDefault());
        }
        return null;
    }

    // GSM: Incidence 250. Must show the full names, not the short names of the Valuation agent
    // getCode() replaced with getName
    private String getValuationAgent(final CollateralConfig config) {
        final String valuationType = config.getValuationAgentType();
        if (Util.isEmpty(valuationType) || CollateralConfig.NONE.equals(valuationType)) {
            return null;
        }

        if (CollateralConfig.PARTY_A.equals(valuationType)) {
            return config.getProcessingOrg().getCode();
        }

        if (CollateralConfig.PARTY_B.equals(valuationType)) {
            return config.getLegalEntity().getCode();
        }

        if (CollateralConfig.BOTH.equals(valuationType)) {
            return new StringBuilder(config.getProcessingOrg().getCode()).append(" ")
                    .append(config.getLegalEntity().getCode()).toString();
        }

        if (CollateralConfig.THIRD_PARTY.equals(valuationType)) {
            final int leId = config.getValuationAgentId();
            if (leId != 0) {
                return BOCache.getLegalEntity(DSConnection.getDefault(), leId).getCode();
            }
        }

        return null;
    }

    public Object getColumnValue(String columnName) {
        return this.columnMap.get(columnName);
    }

    private void buildMap() {
        this.columnMap.put(SantDailyTaskReportStyle.ENTRY_STATUS, this.entryStatus);
        this.columnMap.put(SantDailyTaskReportStyle.MARGIN_CALL_SITUATION, this.marginCallSituation);
        this.columnMap.put(SantDailyTaskReportStyle.PROCESS_DATE, this.processDate);
        this.columnMap.put(SantDailyTaskReportStyle.EVENT_DATE, this.eventDate);
        this.columnMap.put(SantDailyTaskReportStyle.EXEC_ALLOC_AMOUNT, format(this.executedAllocAmount));
        this.columnMap.put(SantDailyTaskReportStyle.NON_EXEC_ALLOC_AMOUNT, format(this.notExecutedAllocAmount));
        this.columnMap.put(SantDailyTaskReportStyle.NOT_ALLOC_AMOUNT, format(this.notAllocatedAmount));
        this.columnMap.put(SantDailyTaskReportStyle.BASE_CURRENCY, this.baseCurrency);
        this.columnMap.put(SantDailyTaskReportStyle.GROSS_EXPOSURE_PRIOR_TOLEANCE,
                format(this.grossExposurePriorTolerance));
        this.columnMap.put(SantDailyTaskReportStyle.GROSS_EXPOSURE_AFTER_TOLEANCE,
                format(this.grossExposureAfterTolerance));
        this.columnMap.put(SantDailyTaskReportStyle.INDEPENDENT, this.independent);
        this.columnMap.put(SantDailyTaskReportStyle.THRESHOLD, format(this.threshold));
        this.columnMap.put(SantDailyTaskReportStyle.MINIMUM_TRANSFER, format(this.minimumTransfer));
        this.columnMap.put(SantDailyTaskReportStyle.BALANCE, format(this.balance));
        this.columnMap.put(SantDailyTaskReportStyle.AGREEMENT, this.agreementName);
        this.columnMap.put(SantDailyTaskReportStyle.HEAD_CLONE, this.headClone);
        this.columnMap.put(SantDailyTaskReportStyle.AGREEMENT_DESC, this.agreementDescription);
        this.columnMap.put(SantDailyTaskReportStyle.OWNER, this.owner);
        this.columnMap.put(SantDailyTaskReportStyle.VAL_AGENT, this.valuationAgent);
        this.columnMap.put(SantDailyTaskReportStyle.CALC_AGENT, this.calcAgentId);
        this.columnMap.put(SantDailyTaskReportStyle.MARGIN_CALL, this.marginCall);
        this.columnMap.put(SantDailyTaskReportStyle.MARGIN_CALLCalc, this.marginCallCalc);

    }

    private Object format(Object value) {
        if (value instanceof Double) {
            return new Amount((Double) value, 2);
        }
        return value;
    }

}
