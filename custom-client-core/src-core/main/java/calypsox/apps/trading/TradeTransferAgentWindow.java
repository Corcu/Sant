package calypsox.apps.trading;

import com.calypso.analytics.Util;
import com.calypso.apps.trading.TradeTransferAgentWindowWrapper;
import com.calypso.tk.bo.SDISelectorUtil;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.TransferAgent;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TradeTransferAgentWindow extends TradeTransferAgentWindowWrapper {

    private boolean enableSDIEditing = true;

    private final Map<JComboBox<?>, Integer> selectedIndex = new HashMap<>();

    public TradeTransferAgentWindow() {
        this(true);
    }

    public TradeTransferAgentWindow(boolean intiDomain) {
        this(intiDomain, true);
    }

    public TradeTransferAgentWindow(boolean intiDomain, boolean initListeners) {
        super(intiDomain);
        if (initListeners) {
            initListeners();
        }
    }

    public void initListeners() {
        getFromSDIChoice().addItemListener(this::onFromSDIChange);
        getToSDIChoice().addItemListener(this::onToSDIChange);

        getFromSDIChoice().addActionListener(e -> {
            if (getFromSDIChoice().getSelectedItem() == null) {
                getAccountFromText().setToolTipText("");
                getAccountFromText().setText("");
            }
        });

        getToSDIChoice().addActionListener(e -> {
            if (getToSDIChoice().getSelectedItem() == null) {
                getAccountToText().setToolTipText("");
                getAccountToText().setText("");
            }
        });

        getFromSDIChoice().setRenderer(new SDIItemRenderer(getFromSDIChoice().getRenderer()));
        getToSDIChoice().setRenderer(new SDIItemRenderer(getToSDIChoice().getRenderer()));

        enableSDIEditing(true);
    }

    private boolean isAgentChange(List<?> oldSDIs, List<?> newSDIs) {
        List<Integer> oldAgents = oldSDIs.stream().filter(Objects::nonNull).map(s -> ((SettleDeliveryInstruction) s).getAgentId()).distinct().collect(Collectors.toList());
        List<Integer> newAgents = newSDIs.stream().filter(Objects::nonNull).map(s -> ((SettleDeliveryInstruction) s).getAgentId()).distinct().collect(Collectors.toList());
        return !Util.isEmpty(oldAgents) && !oldAgents.equals(newAgents);
    }


    private void enableSDIEditing(boolean enableSDIEditing) {
        this.enableSDIEditing = enableSDIEditing;
    }

    public void setDefaultSDIs(Integer fromAccId, Integer toAccId) {
        List<SettleDeliveryInstruction> fromSDIs = getSDIList(getFromSDIChoice(), s -> s != null && s.getPreferredB() && (fromAccId <= 0 || fromAccId == s.getGeneralLedgerAccount()));
        List<SettleDeliveryInstruction> toSDIs = getSDIList(getToSDIChoice(), s -> s != null && s.getPreferredB() && (toAccId <= 0 || toAccId == s.getGeneralLedgerAccount()));

        setDefaultSDIs(fromSDIs, toSDIs);

    }

    public void setDefaultSDIs() {
        List<SettleDeliveryInstruction> fromSDIs = getSDIList(getFromSDIChoice(), s -> s != null && s.getPreferredB());
        List<SettleDeliveryInstruction> toSDIs = getSDIList(getToSDIChoice(), s -> s != null && s.getPreferredB());

        setDefaultSDIs(fromSDIs, toSDIs);
    }

    private void setDefaultSDIs(List<SettleDeliveryInstruction> fromSDIs, List<SettleDeliveryInstruction> toSDIs) {

        try {

            enableSDIEditing(false);

            TradeTransferRule rule = buildRule();
            SettleDeliveryInstruction selectedFromSDI = null, selectedToSDI = null;
            for (SettleDeliveryInstruction fromSDI : fromSDIs) {
                selectedFromSDI = fromSDI;
                for (SettleDeliveryInstruction toSDI : toSDIs) {
                    if (isSDIApplicable(toSDI, fromSDI, rule)) {
                        selectedToSDI = toSDI;
                        break;
                    }
                }
                if (selectedFromSDI != null && selectedToSDI != null)
                    break;
            }

            if (selectedFromSDI != null) {
                getFromSDIChoice().setSelectedItem(selectedFromSDI);
            } else {
                setNoFromSDI();
            }

            if (selectedToSDI != null) {
                getToSDIChoice().setSelectedItem(selectedToSDI);
            } else {
                setNoToSDI();
            }
        } finally {
            enableSDIEditing(true);
        }

    }

    private List<SettleDeliveryInstruction> getSDIList(JComboBox<SettleDeliveryInstruction> sdiChoice, Function<SettleDeliveryInstruction, Boolean> predicate) {
        List<SettleDeliveryInstruction> settleDeliveryInstructions = new ArrayList<>();
        for (int i = 0; i < sdiChoice.getItemCount(); i++) {
            SettleDeliveryInstruction sdi = sdiChoice.getItemAt(i);
            if (predicate.apply(sdi))
                settleDeliveryInstructions.add(sdiChoice.getItemAt(i));
        }
        return settleDeliveryInstructions;
    }

    private void onFromSDIChange(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            fromSDIChanged();
        }
        if (getFromSDIChoice().getSelectedItem() == null) {
            getAccountFromText().setToolTipText("");
            getAccountFromText().setText("");
        }
    }

    private void fromSDIChanged() {
        if (enableSDIEditing) {
            onSDIChange(true, getFromSDIChoice(), getToSDIChoice(), fromSdiId);
        }
    }


    private void onSDIChange(boolean ui, JComboBox<SettleDeliveryInstruction> driverChoice, JComboBox<SettleDeliveryInstruction> drivenChoice, int driverSdiId) {
        if (enableSDIEditing) {
            SettleDeliveryInstruction selectedDriverSDI = (SettleDeliveryInstruction) driverChoice.getSelectedItem();

            if (!ui && driverChoice.getItemCount() > 0) {
                selectedDriverSDI = driverChoice.getItemCount() > 1 && driverChoice.getItemAt(0) == null ? driverChoice.getItemAt(1) : driverChoice.getItemAt(0);
            }

            if (selectedDriverSDI == null || (!ui && driverSdiId == selectedDriverSDI.getId()))
                return;

            try {
                enableSDIEditing(false);

                TradeTransferRule rule = buildRule();

                List<SettleDeliveryInstruction> driverSdiList = ui ? Collections.singletonList(selectedDriverSDI) : getSDIList(driverChoice, s -> s != null && s.getPreferredB());
                selectedDriverSDI = null;
                SettleDeliveryInstruction selectedDrivenSDI = null;
                for (SettleDeliveryInstruction driverSDI : driverSdiList) {

                    selectedDriverSDI = driverSDI;
                    selectedDrivenSDI = (SettleDeliveryInstruction) drivenChoice.getSelectedItem();
                    if (!ui || selectedDrivenSDI == null || !isSDIApplicable(selectedDrivenSDI, selectedDriverSDI, rule)) {
                        selectedDrivenSDI = null;
                        //selected sdi is not applicable assign first applicable sdi
                        for (SettleDeliveryInstruction drivenSDI : getSDIList(drivenChoice, s -> s != null && s.getPreferredB())) {
                            if (isSDIApplicable(drivenSDI, driverSDI, rule)) {
                                selectedDrivenSDI = drivenSDI;
                                break;
                            }
                        }
                    }
                    if (selectedDriverSDI != null && selectedDrivenSDI != null)
                        break;

                }

                setSDI(driverChoice, selectedDriverSDI);
                setSDI(drivenChoice, selectedDrivenSDI);
            } finally {
                enableSDIEditing(true);
            }
        }

    }

    private void setNoFromSDI() {
        setNoSDI(getFromSDIChoice());
        fromSdiId = 0;
    }

    private void setNoToSDI() {
        setNoSDI(getToSDIChoice());
        toSdiId = 0;
    }

    private void setSDI(JComboBox<SettleDeliveryInstruction> sdiChoice, SettleDeliveryInstruction sdi) {
        if (sdi == null)
            setNoSDI(sdiChoice);
        else {
            SettleDeliveryInstruction selectedSdi = (SettleDeliveryInstruction) sdiChoice.getSelectedItem();
            if (selectedSdi == null || selectedSdi.getId() != sdi.getId())
                sdiChoice.setSelectedItem(sdi);
        }
        selectedIndex.put(sdiChoice, sdiChoice.getSelectedIndex());
    }

    private void setNoSDI(JComboBox<SettleDeliveryInstruction> sdiChoice) {
        if (sdiChoice.getItemCount() == 0)
            sdiChoice.addItem(null);
        else if (sdiChoice.getItemAt(0) != null) {
            List<SettleDeliveryInstruction> sdis = getItems(sdiChoice);
            sdiChoice.removeAllItems();
            sdiChoice.addItem(null);
            sdis.forEach(sdiChoice::addItem);
        }

        sdiChoice.setSelectedIndex(0);
    }

    private void onToSDIChange(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            toSDIChanged();
        }
        if (getToSDIChoice().getSelectedItem() == null) {
            getAccountToText().setToolTipText("");
            getAccountToText().setText("");
        }
    }


    private void toSDIChanged() {
        if (enableSDIEditing) {
            onSDIChange(true, getToSDIChoice(), getFromSDIChoice(), toSdiId);
        }
    }

    private List<SettleDeliveryInstruction> getItems(JComboBox<SettleDeliveryInstruction> sdiChoice) {
        List<SettleDeliveryInstruction> items = new ArrayList<>();
        for (int i = 0; i < sdiChoice.getItemCount(); i++) {
            items.add(sdiChoice.getItemAt(i));
        }
        return items;
    }

    private TradeTransferRule buildRule() {
        if (getTrade() == null || getTrade().getBook() == null && getSelectedBook() != null) {
            this.buildTrade(this._trade, false);
        }

        if (getTrade() != null && getTrade().getBook() != null) {
            TransferAgent trA = (TransferAgent) (getTrade().getProduct() == null ? getProduct() : getTrade().getProduct());


            String payReceive = "PAY";
            String transferType = trA.getFlowType();
            TradeTransferRule rule = new TradeTransferRule();
            rule.setPayReceive(payReceive);
            rule.setPayerLegalEntityId(this._trade.getBook().getLegalEntity().getId());
            rule.setPayerLegalEntityRole(LegalEntity.PROCESSINGORG);
            rule.setReceiverLegalEntityId(getTrade().getBook().getLegalEntity().getId());
            rule.setReceiverLegalEntityRole(LegalEntity.PROCESSINGORG);
            rule.setTransferCurrency(trA.getCurrency());
            rule.setSettlementCurrency(trA.getCurrency());
            rule.setProductType(trA.getType());
            rule.setTransferType(transferType);
            boolean isDAP = trA.getIsDAP();
            rule.setDeliveryType(isDAP ? "DAP" : "DFP");
            rule.setSecurityId(trA.getSecurityId());
            return rule;
        }
        return null;
    }

    private boolean isSDIApplicable(SettleDeliveryInstruction extsdi, SettleDeliveryInstruction sdi, TradeTransferRule rule) {
        if (!extsdi.getSettlementMethod().equals(sdi.getSettlementMethod()) || extsdi.equals(sdi))
            return false;
        Vector<?> fullRoute = SDISelectorUtil.getFullRoute(extsdi, sdi, getTrade(), rule, getTrade().getSettleDate(), DSConnection.getDefault());
        return !Util.isEmpty(fullRoute) && fullRoute.size() >= 2;
    }

    @Override
    public void reuseTradeWindow() {
        try {
            enableSDIEditing(false);
            Trade trade = getTrade();
            if (trade != null && trade.getProduct() != null) {
                ((TransferAgent) trade.getProduct()).setFromSdiId(0);
                ((TransferAgent) trade.getProduct()).setToSdiId(0);
            }
            super.reuseTradeWindow();
        } finally {
            enableSDIEditing(true);
        }

    }


    @Override
    protected void set(JComboBox choice, Vector items) {
        if (getFromSDIChoice().equals(choice) || getToSDIChoice().equals(choice)) {
            int idx = choice.getSelectedIndex();
            if (choice.getItemCount()>0 && choice.getItemAt(0)==null)
                idx--;
            Vector<?> intItems = items;

            if (!Util.isEmpty(items) && items.get(0) != null) {
                intItems = new Vector<>();
                intItems.add(null);
                intItems.addAll(items);
                idx += idx<0?0:1;
            }
            MutableComboBoxModel  model = new DefaultComboBoxModel();
            intItems.forEach(model::addElement);

            if (isAgentChange(getSDIList(choice, Objects::nonNull), items) || (choice.getItemCount()==0 && !Util.isEmpty(items))) {
                choice.setModel(model);
                setDefaultSDIs(getSDIList(getFromSDIChoice(), s->s!=null && s.getPreferredB()), getSDIList(getToSDIChoice(), s->s!=null && s.getPreferredB()));
                return;
            }
            choice.setModel(model);
            if (idx >0 && idx < model.getSize())
                choice.setSelectedIndex(idx);

        } else {
            super.set(choice, items);
        }
    }

    @Override
    public void buildTrade(Trade trade, boolean verbose) {
        super.buildTrade(trade, verbose);
        if (trade != null && trade.getProduct() != null) {
            TransferAgent ta = (TransferAgent) trade.getProduct();
            if (ta.getFromSdiId() == 0)
                onSDIChange(false, getFromSDIChoice(), getToSDIChoice(), ta.getFromSdiId());
        }

    }

    private static class SDIItemRenderer extends DefaultListCellRenderer {

        private final ListCellRenderer defaultRenderer;

        public SDIItemRenderer(ListCellRenderer defaultRenderer) {
            this.defaultRenderer = defaultRenderer;
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            Component c = defaultRenderer.getListCellRendererComponent(list, value,
                    index, isSelected, cellHasFocus);
            if (value != null) {
                c.setBackground(((SettleDeliveryInstruction) value).getPreferredB() ? Color.gray : Color.green);
            }
            return c;
        }
    }
}
