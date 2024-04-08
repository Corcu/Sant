package calypsox.tk.bo.workflow.rule;

import calypsox.tk.core.CollateralStaticAttributes;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.collateral.dto.MarginCallAllocationDTO;
import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.collateral.service.dashboard.RemoteDashBoardServer;
import com.calypso.tk.core.*;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.util.*;

import static calypsox.tk.core.CollateralStaticAttributes.*;

/*
 * Checks if Trade is MArinCall and the underlying is Security AND KEY_WORD_DVP_ALLOCATION = DVP or RVP or FOP
 * If so it returns true so the trade moves forward in the workflow.
 * Otherwise returns false.
 */
public class SantCheckFatherFrontIDMarginCallTradeRule implements WfTradeRule {

    @SuppressWarnings({"rawtypes"})
    @Override
    public boolean check(final TaskWorkflowConfig wc, final Trade trade, final Trade oldTrade, final Vector messages,
                         final DSConnection dsCon, final Vector exception, final Task task, final Object dbCon, final Vector events) {

        return true;
    }

    @Override
    public String getDescription() {
        final String desc = "Checks if Trade is MArinCall and the underlying is Security "
                + "AND KEY_WORD_DVP_ALLOCATION = DVP or RVP or FOP\n"
                + "If so it returns true so the trade moves forward in the workflow \n" + "Otherwise returns false.";
        return desc;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public boolean update(final TaskWorkflowConfig wc, final Trade trade, final Trade oldTrade, final Vector messages,
                          final DSConnection dsCon, final Vector exception, final Task task, final Object dbCon, final Vector events) {

        Log.debug(SantCheckFatherFrontIDMarginCallTradeRule.class, "Update - Start");

        if (!trade.getProductType().equals(Product.MARGINCALL)) {
            return true;
        }

        final MarginCall marginCall = (MarginCall) trade.getProduct();
        if ((!marginCall.getFlowType().equals("SECURITY")) && (marginCall.getSecurity() == null)) {
            return true;
        }
        CollateralConfig marginCallConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
                marginCall.getMarginCallId());

        if (ISMA.equals(marginCallConfig.getContractType())
                && SI.equals(marginCallConfig.getAdditionalField(MC_TRIPARTY))) {
            boolean repoFound = false;
            final String fatherFrontId = trade.getKeywordValue(TK_FATHER_FRONT_ID);

            if (fatherFrontId == null) {
                messages.add("No FATHER_FRONT_ID found for the trade");
                return false;
            }

            // Check if the fatherFrontId is also FATHER_FRONT_ID of any
            // underlying Repo in the contract
            List<MarginCallDetailEntryDTO> marginCallDetailEnties = null;
            try {
                marginCallDetailEnties = getMarginCallDetailEntry(trade, dsCon);

            } catch (final Exception e) {
                Log.debug(SantCheckFatherFrontIDMarginCallTradeRule.class, e);
                messages.add(e.getMessage());
                return false;
            }

            for (final MarginCallDetailEntryDTO dto : marginCallDetailEnties) {
                try {
                    final Trade trade2 = dsCon.getRemoteTrade().getTrade(dto.getTradeId());
                    if (trade2.getProductType().equals(Product.REPO)
                            && (trade2.getKeywordValue("FATHER_FRONT_ID") != null)
                            && (!trade2.getKeywordValue("FATHER_FRONT_ID").equals(fatherFrontId))) {
                        repoFound = true;

                        final String custodianName = trade2.getKeywordValue("CUSTODIAN");
                        if (!Util.isEmpty(custodianName)) {
                            final int custodianID = getCustodianLEId(custodianName, dsCon);
                            if (custodianID != 0) {
                                trade.addKeyword("CUSTODIAN", custodianID);
                            }
                        }

                        break;
                    }
                } catch (final Exception e) {
                    Log.debug(SantCheckFatherFrontIDMarginCallTradeRule.class, e);
                    messages.add(e.getMessage());
                    return false;
                }
            }

            if (!repoFound) {
                messages.add("No corresponding underlying Repo found with FATHER_FRONT_ID=" + fatherFrontId);
                return false;
            }
        }

        // final String msg = FATHER_FRONT_ID
        // +
        // " trade keyword must be filled.[SantCheckFatherFrontIDMarginCallTradeRule]";
        // messages.add(msg);

        Log.debug(SantCheckFatherFrontIDMarginCallTradeRule.class, "Update - End");

        return true;
    }

    @SuppressWarnings({"rawtypes"})
    private int getCustodianLEId(final String custodianName, final DSConnection dsConn) {
        int custodianId = 0;
        final LegalEntity custodian = BOCache.getLegalEntity(dsConn, custodianName);

        if (custodian == null) {
            return custodianId;
        }

        final Collection legalEntityVector = custodian.getLegalEntityAttributes();
        final Iterator iter = legalEntityVector.iterator();
        while (iter.hasNext()) {
            final LegalEntityAttribute attr = (LegalEntityAttribute) iter.next();
            if (CollateralStaticAttributes.ALIAS_KPLUS.equals(attr.getAttributeType())
                    && (attr.getAttributeValue() != null)) {
                custodianId = Integer.parseInt(attr.getAttributeValue());
                return custodianId;
            }
        }

        return custodianId;
    }

    public List<MarginCallDetailEntryDTO> getMarginCallDetailEntry(final Trade trade, final DSConnection ds)
            throws Exception {
        // final DefaultDashboardServer dashboardServer = new
        // DefaultDashboardServer();
        final RemoteDashBoardServer dashboardServer = ServiceRegistry.getDefault().getDashBoardServer();

        ArrayList<String> tableList = new ArrayList<String>();
        tableList.add("margin_call_allocation");
        final List<MarginCallAllocationDTO> marginCallAllocations = dashboardServer.loadMarginCallAllocations(
                "trade_id=" + trade.getLongId(), tableList);
        if (Util.isEmpty(marginCallAllocations)) {
            throw new Exception("No Allocation found for the trade id " + trade.getLongId());
        }

        final int mcEntryId = marginCallAllocations.get(0).getMarginCallEntryId();
        tableList = new ArrayList<String>();
        tableList.add("margin_call_detail_entries");
        final List<MarginCallDetailEntryDTO> marginCallDetailEntries = dashboardServer.loadMarginCallDetailEntries(
                "mc_entry_id=" + mcEntryId, tableList);

        if (Util.isEmpty(marginCallDetailEntries)) {
            throw new Exception("No MarginCallDetailEntries found for the entry_id=" + mcEntryId + " , trade_id="
                    + trade.getLongId());
        }
        return marginCallDetailEntries;
    }
}
