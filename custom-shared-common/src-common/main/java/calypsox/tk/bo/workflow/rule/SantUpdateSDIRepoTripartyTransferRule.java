package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.*;
import com.calypso.tk.bo.workflow.WfTransferRule;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.TradeSQL;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;

import java.sql.Connection;
import java.util.Comparator;
import java.util.Vector;
import java.util.stream.Collectors;

import static calypsox.tk.bo.util.PHConstants.*;

/**
 * SantUpdateSDIRepoTripartyTransferRule update transfer attributes PH Internal SDI id,
 * PH External SDI id, PH Settlement Method by custom SDI
 *
 * @author Ruben Garcia
 */
public class SantUpdateSDIRepoTripartyTransferRule implements WfTransferRule {


    /**
     * The CounterParty SDI role
     */
    private static final String CPTY_ROLE = "CounterParty";

    @Override
    public boolean check(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages,
                         DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (transfer != null  && transfer.getTradeLongId() > 0) {
            if (trade == null) {
                try {
                    if (dbCon != null) {
                        trade = TradeSQL.getTrade(transfer.getTradeLongId(), (Connection) dbCon);
                    } else if(dsCon != null){
                        trade = dsCon.getRemoteTrade().getTrade(transfer.getTradeLongId());
                    }
                } catch (PersistenceException | CalypsoServiceException e) {
                    Log.error(this, e);
                    return false;
                }
            }
            if (trade != null) {
                SettleDeliveryInstruction internalSDI = getTripartySDI(trade, transfer, dsCon, transfer.getInternalRole(), transfer.getInternalLegalEntityId(), messages);
                SettleDeliveryInstruction externalSDI = getTripartySDI(trade, transfer, dsCon, transfer.getExternalRole(), transfer.getExternalLegalEntityId(), messages);
                return internalSDI != null && externalSDI != null;
            }
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "This rule updates the " + PH_INTERNAL_SDI_ID + ", " + PH_EXTERNAL_SDI_ID + ", " + PH_SETTLEMENT_METHOD +
                " attributes of the transfer with the custom SDI. " +
                "To select the SDIs, use the Calypso selector and apply a custom filter:" +
                " CASH, currency trade and Repo product on the selected SDIs.";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade, Vector messages,
                          DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (transfer != null && dsCon != null && transfer.getTradeLongId() > 0) {
            if (trade == null) {
                try {
                    trade = TradeSQL.getTrade(transfer.getTradeLongId(), (Connection) dbCon);
                } catch (PersistenceException e) {
                    Log.error(this, e);
                    return false;
                }
            }
            if (trade != null) {
                String internalRole = transfer.getInternalRole();
                String externalRole = transfer.getExternalRole();
                SettleDeliveryInstruction internalSDI;
                SettleDeliveryInstruction externalSDI;
                String settlementMethod = "";
                if (!Util.isEmpty(internalRole) && CPTY_ROLE.equalsIgnoreCase(internalRole)) {
                    internalSDI = getTripartySDI(trade, transfer, dsCon, internalRole, transfer.getInternalLegalEntityId(), messages);
                    if (internalSDI != null) {
                        transfer.setSettlementMethod(internalSDI.getSettlementMethod());
                        settlementMethod = internalSDI.getSettlementMethod();
                    }
                    externalSDI = getTripartySDI(trade, transfer, dsCon, externalRole, transfer.getExternalLegalEntityId(), messages);
                } else {
                    externalSDI = getTripartySDI(trade, transfer, dsCon, externalRole, transfer.getExternalLegalEntityId(), messages);
                    if (externalSDI != null) {
                        transfer.setSettlementMethod(externalSDI.getSettlementMethod());
                        settlementMethod = externalSDI.getSettlementMethod();
                    }
                    internalSDI = getTripartySDI(trade, transfer, dsCon, internalRole, transfer.getInternalLegalEntityId(), messages);
                }

                if (internalSDI != null && externalSDI != null) {
                    transfer.setAttribute(PH_INTERNAL_SDI_ID, String.valueOf(internalSDI.getId()));
                    transfer.setAttribute(PH_EXTERNAL_SDI_ID, String.valueOf(externalSDI.getId()));
                    transfer.setAttribute(PH_SETTLEMENT_METHOD, settlementMethod);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get repo triparty SDI
     * -First, find SDI using Calypso core
     * -Filter SDI list by
     * -Currency (NOT ANY) equals trade currency
     * -Product type (NOT ANY) equals transfer product type
     * -SDI type only CASH
     *
     * @param trade    the current trade
     * @param transfer the current transfer
     * @param dsCon    the Data Server connection
     * @param role     the SDI role
     * @param leId     the SDI legal entity ID
     * @param messages the error messages
     * @return the filter custom SDI
     */
    private SettleDeliveryInstruction getTripartySDI(Trade trade, BOTransfer transfer, DSConnection dsCon, String role,
                                                     int leId, Vector<String> messages) {
        if (!Util.isEmpty(transfer.getTradeCurrency()) && !Util.isEmpty(transfer.getProductType()) && !Util.isEmpty(role)
                && leId > 0 && transfer.getSettleDate() != null) {
            if (messages == null) {
                messages = new Vector<>();
            }
            LegalEntity entity = BOCache.getLegalEntity(dsCon, leId);
            if (entity != null) {
                TradeTransferRule rule = transfer.toTradeTransferRule();
                if (rule != null) {
                    TradeTransferRule clonedRule = rule.clone();
                    SDISelector selector = SDISelectorUtil.find(trade, clonedRule);
                    if (selector != null) {
                        JDate settleDate = transfer.getSettleDate();
                        Vector<SettleDeliveryInstruction> sdis = selector.getValidSDIList(trade, clonedRule, settleDate,
                                entity.getCode(), role, new Vector<String>(), false, dsCon);
                        if (!Util.isEmpty(sdis)) {
                            sdis = sdis.stream().filter(sdi -> filterSDIByCcy(sdi.getCurrencyList(), transfer.getTradeCurrency()) &&
                                    filterSDIByProductType(sdi.getProductList(), transfer.getProductType()) &&
                                    filterCashSDI(sdi.typeToString())).collect(Collectors.toCollection(Vector::new));
                            if (!Util.isEmpty(sdis)) {
                                sdis.sort(Comparator.comparingInt(SettleDeliveryInstruction::getPriority));
                                return sdis.get(0);
                            } else {
                                String msg = "There is no SDI [ " + SettleDeliveryInstruction.S_SETTLEMENT
                                        + ", " + transfer.getProductType() + ", " + transfer.getTradeCurrency() +
                                        ", " + role + ", " + entity + "]";
                                messages.add(msg);
                                Log.error(this, msg);
                                return null;
                            }

                        } else {
                            String msg = "There is no SDI when the core filter is applied.";
                            messages.add(msg);
                            Log.error(this, msg);
                            return null;
                        }
                    } else {
                        String msg = "No selector found for this product.";
                        messages.add(msg);
                        Log.error(this, msg);
                        return null;
                    }
                }
            } else {
                String msg = "No legal entity found for this id: " + leId;
                messages.add(msg);
                Log.error(this, msg);
                return null;
            }
        }
        return null;
    }

    /**
     * Filter SDI by currency (NOT ANY) equals trade currency
     *
     * @param sdiCcy the SDI currency list
     * @param ccy    the trade currency
     * @return true if equals
     */
    private boolean filterSDIByCcy(Vector<String> sdiCcy, String ccy) {
        return !Util.isEmpty(sdiCcy) && !Util.isEmpty(ccy) && sdiCcy.contains(ccy);
    }

    /**
     * Filter SDI by product type (NOT ANY) equals transfer product type
     *
     * @param sdiProduct  the SDI product list
     * @param productType the transfer product type
     * @return true if equals
     */
    private boolean filterSDIByProductType(Vector<String> sdiProduct, String productType) {
        return !Util.isEmpty(sdiProduct) && !Util.isEmpty(productType) && sdiProduct.contains(productType);
    }

    /**
     * Filter CASH SDI
     *
     * @param sdiType the current SDI type
     * @return true if SDI type is equals CASH
     */
    private boolean filterCashSDI(String sdiType) {
        return !Util.isEmpty(sdiType) && SettleDeliveryInstruction.S_SETTLEMENT.equals(sdiType);
    }
}
