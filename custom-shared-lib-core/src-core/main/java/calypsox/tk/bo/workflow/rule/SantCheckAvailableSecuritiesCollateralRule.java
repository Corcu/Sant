/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.bo.workflow.rule;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.report.BOSecurityPositionReport;
import calypsox.tk.report.SantInventoryViewReportTemplate;
import com.calypso.tk.bo.*;
import com.calypso.tk.bo.workflow.WorkflowResult;
import com.calypso.tk.bo.workflow.rule.BaseCollateralWorkflowRule;
import com.calypso.tk.collateral.MarginCallAllocation;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.SecurityAllocation;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectionUtil;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.util.*;

public class SantCheckAvailableSecuritiesCollateralRule extends BaseCollateralWorkflowRule {

    protected static String RULE_NAME = "SantCheckAvailableSecuritiesCollateralRule";

    // This rule is used in two places in the workflow. Once to check only Plain Allocations and the other to check
    // Substitutions only
    private boolean checkPlainAllocsOnly = true;

    public void setCheckPlainAllocsOnly(boolean flag) {
        this.checkPlainAllocsOnly = flag;
    }

    public boolean isCheckPlainAllocsOnly() {
        return this.checkPlainAllocsOnly;
    }

    @Override
    protected boolean isApplicable(TaskWorkflowConfig paramTaskWorkflowConfig, MarginCallEntry entry,
                                   EntityState paramEntityState1, EntityState paramEntityState2, List<String> paramList, DSConnection dsCon,
                                   List<BOException> paramList1, Task paramTask, Object paramObject, List<PSEvent> paramList2) {

        // We dont check sec positions when the below attribute is ticked
        if ((entry.getAttribute(CollateralStaticAttributes.ENTRY_ATTR_CONTINUE_EXEC_SHORT) != null)
                && (Boolean) entry.getAttribute(CollateralStaticAttributes.ENTRY_ATTR_CONTINUE_EXEC_SHORT)) {
            return true;
        }

        HashMap<Integer, List<SecurityAllocation>> pendingSecAllocsMap = getPendingSecAllocs(entry);

        PricingEnv pricingEnv;
        try {
            pricingEnv = dsCon.getRemoteMarketData().getPricingEnv(entry.getCollateralConfig().getPricingEnvName(),
                    entry.getValueDatetime());

            if (pendingSecAllocsMap.size() > 0) {
                Set<Integer> keySet = pendingSecAllocsMap.keySet();
                for (int secId : keySet) {
                    List<SecurityAllocation> list = pendingSecAllocsMap.get(secId);
                    JDate secSettlmentDate = list.get(0).getSettlementDate();
                    // 1. get sec position
                    SantSecPositionReport rep = new SantSecPositionReport();
                    Double position = rep.getPositionValue(pricingEnv, list.get(0).getBook(), secId,
                            secSettlmentDate);
                    if (position == null) {
                        position = 0.0;
                    }

                    // 2. get UnAvailabilityTransfer Qty
                    int unAvailableTransfersTotalQty = getUnAvailableTransfersTotalQty(dsCon,
                            entry.getCollateralConfigId(), secId, secSettlmentDate);

                    double positionTotal = position + unAvailableTransfersTotalQty;

                    // 3. Sec Allocation total by security
                    double secAllocationTotal = getallocationTotal(list);

                    if ((positionTotal + secAllocationTotal) < 0) {
                        for (SecurityAllocation secAlloc : list) {
                            Product product = secAlloc.getProduct();
                            String payReceive = secAlloc.getQuantity() > 0 ? "REC" : "PAY";
                            String isin = product.getSecCode("ISIN");

                            String errorMsg = "MC_CONTRACT_NUMBER [" + entry.getCollateralConfigId() + "] Allocation ["
                                    + isin + "] on settle date [" + secSettlmentDate + "] for [" + payReceive
                                    + "] [" + secAlloc.getQuantity() + "] units / [" + secAlloc.getNominal() + "] ["
                                    + secAlloc.getCurrency() + "] will make the book [" + secAlloc.getBook().getName()
                                    + "] go short. (Previous position [" + positionTotal + "].";

                            Log.error(RULE_NAME, errorMsg);
                            paramList.add(errorMsg);
                            return false;
                        }
                    }

                }
            }
        } catch (Exception e) {
            Log.error(this, e);
            paramList.add(e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    protected WorkflowResult apply(TaskWorkflowConfig taskConfig, MarginCallEntry entry, DSConnection dsCon) {
        WorkflowResult wfr = new WorkflowResult();

        wfr.success();

        return wfr;
    }

    private HashMap<Integer, List<SecurityAllocation>> getPendingSecAllocs(MarginCallEntry entry) {
        HashMap<Integer, List<SecurityAllocation>> map = new HashMap<Integer, List<SecurityAllocation>>();

        List<MarginCallAllocation> pendingSecAlloc = entry
                .getPendingMarginAllocations(SecurityAllocation.UNDERLYING_TYPE);

        for (MarginCallAllocation alloc : pendingSecAlloc) {
            if (SantExecuteExcludeSubstitutionsCollateralRule.isPlainAllocation(alloc) == isCheckPlainAllocsOnly()) {
                SecurityAllocation sec = (SecurityAllocation) alloc;
                List<SecurityAllocation> list = map.get(sec.getProductId());
                if (list == null) {
                    list = new ArrayList<SecurityAllocation>();
                    map.put(sec.getProductId(), list);
                }
                list.add(sec);
            }
        }

        return map;
    }

    private double getallocationTotal(List<SecurityAllocation> list) {
        double total = 0;
        for (SecurityAllocation secAlloc : list) {
            total += secAlloc.getQuantity();
        }
        return total;
    }

    public int getUnAvailableTransfersTotalQty(DSConnection dsCon, int mcId, int securityId, JDate date)
            throws RemoteException {
        Log.info(SantCancelContractUnAvailTradesCollateralRule.class, "In getUnAvailableTransfers()");

        String from = "trade_keyword tk2";
        String where = " trade.trade_id=tk2.trade_id"
                + " and product_desc.product_type='UnavailabilityTransfer' AND product_desc.und_security_id="
                + securityId + " and trade.trade_status not in ('CANCELED','PENDING') "
                + " and tk2.KEYWORD_NAME='MC_CONTRACT_NUMBER' and tk2.KEYWORD_VALUE='" + mcId
                + "' AND trunc(trade_date_time)<=" + Util.date2SQLString(date);

        TradeArray trades = dsCon.getRemoteTrade().getTrades(from, where, null, null);
        int totalQty = 0;
        for (int i = 0; i < trades.size(); i++) {
            Trade trade = trades.get(i);
            totalQty += trade.getQuantity();
        }

        return totalQty;
    }

    public static void main(String args[]) throws Exception {
        DSConnection ds = null;
        final String appName = "MainEntry";
        ds = ConnectionUtil.connect(args, appName);

        int secId = 11188;
        Book book = BOCache.getBook(ds, "SOMA_OPTIM_1");

        PricingEnv pricingEnv = ds.getRemoteMarketData().getPricingEnv(ds.getUserDefaults().getPricingEnvName(),
                new JDatetime());
        SantSecPositionReport rep = new SantSecPositionReport();
        Double positions = rep.getPositionValue(pricingEnv, book, 11188, JDate.getNow());

        System.out.println(positions);

        // 2. get Unavailable Trades for the MC.

        SantCheckAvailableSecuritiesCollateralRule rule = new SantCheckAvailableSecuritiesCollateralRule();
        secId = 931063;
        int mccId = 1167806;

        int unAvailableTransfers = rule.getUnAvailableTransfersTotalQty(ds, mccId, secId, JDate.getNow());
        System.out.println(unAvailableTransfers);

        System.exit(0);
    }

    @Override
    public String getDescription() {
        return "Checks if any of the plain security allocations(non Substitutions) make the current position short";
    }
}

class SantSecPositionReport extends BOSecurityPositionReport {

    private static final long serialVersionUID = 1L;

    private static final String Balance_Available = "Balance_Available";

    @SuppressWarnings("unused")
    private ReportOutput load() throws Exception {
        Vector<String> errorMsgs = new Vector<String>();
        return super.load(errorMsgs);
    }

    @SuppressWarnings({"unused", "unchecked", "rawtypes"})
    public Double getPositionValue(PricingEnv pricingEnv, Book book, int secId, JDate posDate) throws Exception {

        Vector<InventorySecurityPosition> positionsVect = new Vector<InventorySecurityPosition>();

        BOSecurityPositionReportTemplate secPositionTemplate = new BOSecurityPositionReportTemplate();

        secPositionTemplate.put(SecurityTemplateHelper.SECURITY_REPORT_TYPE, "");

        secPositionTemplate.put(BOSecurityPositionReportTemplate.POSITION_DATE, "Settle");
        secPositionTemplate.put(BOSecurityPositionReportTemplate.POSITION_CLASS, "Internal");
        secPositionTemplate.put(BOSecurityPositionReportTemplate.POSITION_TYPE, "Theoretical");
        secPositionTemplate.put(BOSecurityPositionReportTemplate.POSITION_VALUE, "Quantity");
        secPositionTemplate.put(BOSecurityPositionReportTemplate.CASH_SECURITY, "Security");
        secPositionTemplate.put(BOSecurityPositionReportTemplate.AGGREGATION, "Book");
        secPositionTemplate.put(BOPositionReportTemplate.FILTER_ZERO, "false");
        secPositionTemplate.put(BOPositionReportTemplate.MOVE, Balance_Available);
        secPositionTemplate.put(BOPositionReportTemplate.BOOK_LIST, book.getName());
        secPositionTemplate.put(SantInventoryViewReportTemplate.SEC_LIST, secId + "");
        // secPositionTemplate.put(SantInventoryViewReportTemplate.SEC_LIST, secId);

        setReportTemplate(secPositionTemplate);

        Vector<String> errorMsgs = new Vector<String>();
        this.posDate = posDate;
        initDates();
        setPricingEnv(pricingEnv);

        //
        // if (getPricingEnv() != null) {
        // PricingEnv relloadedPE = AppUtil.loadPE(getPricingEnv().getName(), getValuationDatetime());
        // setPricingEnv(pricingEnv);
        // }

        DefaultReportOutput reportOutput = (DefaultReportOutput) load(errorMsgs);
        ReportRow[] rows = reportOutput.getRows();
        for (int i = 0; i < rows.length; i++) {
            ReportRow row = rows[i];
            Inventory inventory = (Inventory) row.getProperty(ReportRow.INVENTORY);
            Hashtable positions = (Hashtable) row.getProperty(BOPositionReport.POSITIONS);
            if (inventory instanceof InventorySecurityPosition) {
                String s = Util.dateToMString(this.posDate);
                Vector<InventorySecurityPosition> datedPositions = (Vector<InventorySecurityPosition>) positions.get(s);
                if ((datedPositions == null) || (datedPositions.size() == 0)) {
                    return null;
                }
                double amount = InventorySecurityPosition.getTotalSecurity(datedPositions, Balance_Available);
                return amount;
            }
        }

        return null;
    }

    @Override
    public void initDates() {
        this._startDate = this.posDate;
        this._endDate = this.posDate;
    }

    protected JDate posDate = null;
}
