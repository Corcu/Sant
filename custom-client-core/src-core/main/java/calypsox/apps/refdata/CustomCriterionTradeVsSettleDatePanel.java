package calypsox.apps.refdata;

import calypsox.tk.mo.CustomCriterionTradeVsSettleDate;
import com.calypso.apps.refdata.CustomCriterionPanelInterface;
import com.calypso.tk.mo.TradeFilterCriterion;
import com.jidesoft.swing.JideBoxLayout;

import javax.swing.*;
import java.util.Objects;
import java.util.Optional;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class CustomCriterionTradeVsSettleDatePanel extends JPanel implements CustomCriterionPanelInterface {

    private final JComboBox<CustomCriterionTradeVsSettleDate.CriterionComparatorValue> comparatorSelector;

    public CustomCriterionTradeVsSettleDatePanel(){
        this.comparatorSelector = new JComboBox<>(CustomCriterionTradeVsSettleDate.getComparatorValueList());
        JPanel innerPane = new JPanel();
        innerPane.setLayout(new JideBoxLayout(innerPane, 1));
        innerPane.add(this.comparatorSelector, "flexible");
        innerPane.add(Box.createVerticalGlue(), "vary");
        this.add(innerPane, "Center");
    }

    @Override
    public void clear() {
        this.comparatorSelector.setSelectedItem(CustomCriterionTradeVsSettleDate.CriterionComparatorValue.NONE);
    }

    @Override
    public TradeFilterCriterion buildCriterion() {
        CustomCriterionTradeVsSettleDate criterion=null;
        boolean isFilteringEnabled= !CustomCriterionTradeVsSettleDate.CriterionComparatorValue.NONE
                .equals(comparatorSelector.getSelectedItem());
        if(isFilteringEnabled){
            criterion=new CustomCriterionTradeVsSettleDate();
            criterion.setIsInB(true);
            Vector<String> values=new Vector<>();
            values.add(Optional.ofNullable(this.comparatorSelector.getSelectedItem()).map(Object::toString)
                    .orElse(CustomCriterionTradeVsSettleDate.CriterionComparatorValue.NONE.toString()));
            criterion.setValues(values);
        }
        return criterion;
    }

    @Override
    public void showCriterion(TradeFilterCriterion c) {
        if(c instanceof CustomCriterionTradeVsSettleDate) {
            this.comparatorSelector.setSelectedItem(((CustomCriterionTradeVsSettleDate) c).getComparatorValue());
        }
    }

    @Override
    public String getHelp() {
        return "Compare the Trade Date with the Settle Date for RECONCCP:\n" +
                "\tNETTING_GROSS:\n" +
                "\t\tBond: TradeDate = SettleDate\n" +
                "\t\tRepo|BSB: Tradedate = StartDate\n" +
                "\tMTINEXTDAY:\n" +
                "\t\tBond: TradeDate <> SettleDate\n" +
                "\t\tRepo|BSB: Trade Date <> StartDate\n" +
                "\t\tRepo|BSB: Trade Date <> EndDate\n";
    }

    @Override
    public JPanel getPanel() {
        return this;
    }
}
