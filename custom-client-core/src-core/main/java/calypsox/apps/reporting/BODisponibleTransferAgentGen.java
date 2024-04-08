package calypsox.apps.reporting;

import calypsox.tk.report.BODisponibleTransferPositionBean;
import calypsox.apps.trading.TradeTransferAgentWindow;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.core.*;
import com.calypso.tk.product.TransferAgent;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

public class BODisponibleTransferAgentGen implements Callable<List<Trade>> {
    ConcurrentLinkedQueue<String> errors;
    BODisponibleTransferPositionBean bean;
    ConcurrentLinkedQueue<Trade> generatedTransfer;

    public BODisponibleTransferAgentGen(BODisponibleTransferPositionBean bean, ConcurrentLinkedQueue<Trade> generatedTransfer, ConcurrentLinkedQueue<String> errors) {
        this.bean = bean;
        this.errors = errors;
        this.generatedTransfer = generatedTransfer;
    }

    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    @Override
    public List<Trade> call() throws Exception {
        return process();
    }

    private List<Trade> process() {
        List<Trade> transferAgentsToCreate = new ArrayList<>();

        if (null != bean) {
            List<Inventory> fromInventoryList = bean.getInvFromList();
            List<Inventory> invInventoryList = bean.getInvToList();
            Inventory invTo = invInventoryList.stream().findFirst().orElse(null);

            if (invTo != null) {
                fromInventoryList.sort(Comparator.comparingDouble(Inventory::getTotal));
                double invToTotalAmount = Math.abs(invTo.getTotal());
                double coveredAmount = 0.0;

                //Calculate TransferAgent difference and generate the new Trade
                for (Inventory invFrom : fromInventoryList) {
                    if (coveredAmount < invToTotalAmount) {
                        double tempCalc = coveredAmount + invFrom.getTotal();
                        if (tempCalc > invToTotalAmount) {
                            tempCalc = Math.abs(invToTotalAmount - coveredAmount);
                            if (tempCalc <= invFrom.getTotal()) {
                                Trade transferAgentTrade = createTransferAgentTrade(invFrom, invTo, tempCalc, errors);
                                if (null != transferAgentTrade) {
                                    transferAgentsToCreate.add(transferAgentTrade);
                                    coveredAmount += tempCalc;
                                }
                            }
                        } else {
                            Trade transferAgentTrade = createTransferAgentTrade(invFrom, invTo, invFrom.getTotal(), errors);
                            if (null != transferAgentTrade) {
                                transferAgentsToCreate.add(transferAgentTrade);
                                coveredAmount += invFrom.getTotal();
                            }
                        }
                    }
                }
            }
        }
        if (null != generatedTransfer) {
            generatedTransfer.addAll(transferAgentsToCreate);
        }

        return transferAgentsToCreate;
    }

    /**
     * @param invFrom 'From' selected positions.
     * @param invTo   'To' selected position.
     * @param amount  calculated amount to generate the TransferAgent
     * @return new TransferAgent Trade
     */
    private Trade createTransferAgentTrade(Inventory invFrom, Inventory invTo, Double amount, ConcurrentLinkedQueue<String> errors) {
        if (invFrom != null && invTo != null) {
            Book book = getDefaultBook(invFrom);
            if (book == null) {
                LegalEntity po = BOCache.getLegalEntity(DSConnection.getDefault(), getProcessingOrgId(invFrom));
                errors.add("No Default Book defined for PO: " + po.getCode());
                return null;
            }
            if (invFrom.getAgent() == null) {
                errors.add("No 'From' Account.");
                return null;
            }
            if (invTo.getAccount() == null) {
                errors.add("No 'To' Account.");
                return null;
            } else {
                try {
                    //TODO optimize this process
                    String isin = invFrom.getProduct() != null ? invFrom.getProduct().getSecCode("ISIN") : "";
                    Trade taTrade = new Trade();
                    TradeTransferAgentWindow w = new TradeTransferAgentWindow(true, false);
                    w.setSettlementDate(invFrom.getPositionDate());
                    w.setBookName(book.getName());
                    w.setProductId(invFrom.getProduct().getId());
                    w.setSecurityAmount(0.0);
                    w.setSecurityQuantity(roundingAmount(Math.abs(amount)));
                    w.setTransferTypeCash(false);
                    w.setFromAgent(invFrom.getAgent().getCode());
                    w.setToAgent(invTo.getAgent().getCode());


                    w.setDefaultSDIs(getAccountId(invFrom.getAccount()), getAccountId(invTo.getAccount()));




                    ///   w.setFromAccount(invFrom.getAccount());
                    //  w.setToAccount(invTo.getAccount());
                    w.buildTrade(taTrade, true);
                    w.getComponents();
                    w.initListeners();


                    boolean isToBloqueo = Optional.ofNullable(invTo.getAccount()).filter(acc -> "true".equalsIgnoreCase(acc.getAccountProperty("Bloqueo"))).isPresent();
                    boolean isFromBloqueo = Optional.ofNullable(invFrom.getAccount()).filter(acc -> "true".equalsIgnoreCase(acc.getAccountProperty("Bloqueo"))).isPresent();

                    Trade trade = w.getTrade();
                    TransferAgent transferAgent = (TransferAgent) trade.getProduct();

                    //CheckSDI Assignment
                    SettleDeliveryInstruction fromSDI = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), transferAgent.getFromSdiId());
                    SettleDeliveryInstruction toSDI = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), transferAgent.getToSdiId());
                    if (fromSDI == null || toSDI == null) {
                        errors.add("No SDI found from: " + invFrom.getAccount().getName() + " to: " + invTo.getAccount().getName() + " Isin: " + isin);
                        return null;
                    }

                    if (!(invFrom.getAgentId() == fromSDI.getAgentId() && invFrom.getAccount().getId() == fromSDI.getGeneralLedgerAccount())
                            || !(invTo.getAgentId() == toSDI.getAgentId() && invTo.getAccount().getId() == toSDI.getGeneralLedgerAccount())) {
                        errors.add("No valid SDI found from: " + invFrom.getAccount().getName() + " to: " + invTo.getAccount().getName() + " Isin: " + isin);
                        return null;
                    }

                    trade.setAction(Action.NEW);
                    trade.addKeyword("TransferAgentWaitingAccepted", "true");
                    if (isToBloqueo || isFromBloqueo) {
                        trade.addKeyword("isBloqueo", "true");
                    }
                    return trade;
                } catch (Exception e) {
                    Log.error(this.getClass().getSimpleName(), "Error generating TransferAgent: " + e.getMessage());
                }
            }
        }
        return null;
    }

    private int getAccountId(Account account) {
        if (account != null) {
            Account acc = account.getOriginalAccountId() != 0 ? BOCache.getAccount(DSConnection.getDefault(), account.getOriginalAccountId()) : account;
            return acc == null ? 0 : acc.getId();
        }
        return 0;
    }

    /**
     * @param amount amount
     * @return Rounding amount
     */
    protected Double roundingAmount(Double amount) {
        return RoundingMethod.roundNearest(amount, 2);

    }

    /**
     * @param from {@link Inventory}
     * @return DEFAULT_BOOK PO att from inventory
     */
    protected static Book getDefaultBook(Inventory from) {
        int poId = getProcessingOrgId(from);
        LegalEntityAttribute lea = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), poId, poId, "ProcessingOrg", "DestinationBook");
        if (lea == null) {
            lea = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), poId, poId, "ProcessingOrg", "DEFAULT_BOOK");
        }

        return lea == null ? null : BOCache.getBook(DSConnection.getDefault(), lea.getAttributeValue());
    }

    /**
     * @param from {@link Inventory}
     * @return PO id from inventory
     */
    protected static int getProcessingOrgId(Inventory from) {
        return from.getBook() != null ? from.getBook().getProcessingOrgBasedId() : from.getAgentId();
    }

}
