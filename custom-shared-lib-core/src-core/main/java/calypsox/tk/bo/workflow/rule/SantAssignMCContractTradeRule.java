package calypsox.tk.bo.workflow.rule;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.util.SantReportingUtil;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/*
 * This Trade workflow rule finds if there is any MC Contract that can be applied
 * to this trade. If it finds one then the contract id is added to the trade keyword MC_CONTRACT_NUMBER.
 */
public class SantAssignMCContractTradeRule implements WfTradeRule {

    //SubTypeFacade
    public String subTypeFacade = "Facade";

    @SuppressWarnings("rawtypes")
    @Override
    public boolean check(final TaskWorkflowConfig wc, final Trade trade, final Trade oldTrade, final Vector messages,
                         final DSConnection dsCon, final Vector exception, final Task task, final Object dbCon, final Vector events) {

        return true;
    }

    @Override
    public String getDescription() {
        final String desc = "Finds a Margin Call Contract that can be applied to the trade. \n"
                + "If an MCContract is found the Contract id is set to trade keyword MC_CONTRACT_NUMBER.";
        return desc;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public boolean update(final TaskWorkflowConfig wc, final Trade trade, final Trade oldTrade, final Vector messages,
                          final DSConnection ds, final Vector exception, final Task task, final Object dbCon, final Vector events) {
        Log.debug("SantAssignMCContractTradeRule", "Update - Start");

        if (trade == null) {
            return false;
        }

        Log.debug("SantAssignMCContractTradeRule", "Trade id = " + trade.getLongId());

        try {

            List<CollateralConfig> eligibleMarginCallConfigs = SantReportingUtil.getSantReportingService(ds)
                    .getEligibleMarginCallConfigs(trade);
            eligibleMarginCallConfigs = filterOutChildsContracts(eligibleMarginCallConfigs);
            if (Util.isEmpty(eligibleMarginCallConfigs)) {
                String msg = "No applicable MarginCall Contract found for the trade " + trade.getLongId();
                msg = msg + ".[SantAssignMCContract]";
                messages.add(msg);
                return false;
            } else if (eligibleMarginCallConfigs.size() > 1) {
                Log.error(SantAssignMCContractTradeRule.class, "");
                String msg = "More than one applicable MarginCall Contracts found for the trade " + trade.getLongId();
                msg = msg + ".[SantAssignMCContract]";
                messages.add(msg);

                Log.error(SantAssignMCContractTradeRule.class, msg + ". Contracts = " + eligibleMarginCallConfigs);
                removeContractInfo(trade);
                return false;
            } else if (trade.getProductSubType().equals(subTypeFacade)) {
                Log.error(SantAssignMCContractTradeRule.class, "");
                String msg = "It is not applicable for Subtype Facade for the trade " + trade.getLongId();
                msg = msg + ".[SantAssignMCContract]";
                messages.add(msg);
                Log.error(SantAssignMCContractTradeRule.class, msg + ". Contracts = " + eligibleMarginCallConfigs);
                removeContractInfo(trade);
                return false;
            }


            final CollateralConfig marginCallConfig = eligibleMarginCallConfigs.get(0);

            trade.setInternalReference(String.valueOf(marginCallConfig.getId()));
            trade.addKeyword(CollateralStaticAttributes.MC_CONTRACT_NUMBER, marginCallConfig.getId());
            trade.addKeyword(CollateralStaticAttributes.CONTRACT_TYPE, marginCallConfig.getContractType());

            if (!Util.isEmpty(marginCallConfig
                    .getAdditionalField(CollateralStaticAttributes.MCC_ADD_FIELD_ECONOMIC_SECTOR))) {
                trade.addKeyword(CollateralStaticAttributes.MCC_ADD_FIELD_ECONOMIC_SECTOR,
                        marginCallConfig.getAdditionalField(CollateralStaticAttributes.MCC_ADD_FIELD_ECONOMIC_SECTOR));
            } else {
                // adding the below for the reporting purpose. In case if there
                // is no Economic Sector then we add '-'
                trade.addKeyword(CollateralStaticAttributes.MCC_ADD_FIELD_ECONOMIC_SECTOR, "-");
            }

            // CollateralStaticAttributes
            if (trade.getProductType().equals(CollateralStaticAttributes.SEC_LENDING)) {
                if (marginCallConfig.getAdditionalField(CollateralStaticAttributes.MCC_HAIRCUT) != null) {
                    trade.addKeyword(CollateralStaticAttributes.MCC_HAIRCUT,
                            marginCallConfig.getAdditionalField(CollateralStaticAttributes.MCC_HAIRCUT));
                }
            }

        } catch (final Exception e) {
            String msg = "Error finding MCContract for the trade " + trade.getLongId();
            msg = msg + ".[SantAssignMCContract]";
            messages.add(msg);
            Log.error("SantAssignMCContractTradeRule", e);
            removeContractInfo(trade);
            return false;
        }

        Log.debug("SantAssignMCContractTradeRule", "Update - End");

        return true;
    }

    /*
     * Remove any (if exists old info) contract related info if a Contract is not found.
     */
    private void removeContractInfo(Trade trade) {
        trade.setInternalReference(null);
        trade.removeKeyword(CollateralStaticAttributes.MC_CONTRACT_NUMBER);
        trade.removeKeyword(CollateralStaticAttributes.CONTRACT_TYPE);
        trade.removeKeyword(CollateralStaticAttributes.MCC_ADD_FIELD_ECONOMIC_SECTOR);
        trade.removeKeyword(CollateralStaticAttributes.MCC_HAIRCUT);
    }

    private List<CollateralConfig> filterOutChildsContracts(List<CollateralConfig> eligibleMarginCallConfigs) {
        List<CollateralConfig> finalConfigs = new ArrayList<>();
        if (!Util.isEmpty(eligibleMarginCallConfigs)) {
            for (CollateralConfig marginCallConfig : eligibleMarginCallConfigs) {
                if (marginCallConfig.getParentId() <= 0) {
                    finalConfigs.add(marginCallConfig);
                }
            }
        }

        return finalConfigs;
    }

}
