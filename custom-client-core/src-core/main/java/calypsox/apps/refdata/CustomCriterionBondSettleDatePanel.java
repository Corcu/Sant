package calypsox.apps.refdata;

import calypsox.tk.mo.CustomCriterionBondSettleDate;
import com.calypso.apps.refdata.CustomCriterionPanelInterface;
import com.calypso.tk.mo.TradeFilterCriterion;
import com.jidesoft.swing.JideBoxLayout;

import javax.swing.*;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class CustomCriterionBondSettleDatePanel extends JPanel implements CustomCriterionPanelInterface {

    private JCheckBox enableBondSettleDate;

    public CustomCriterionBondSettleDatePanel(){
        this.enableBondSettleDate = new JCheckBox("Filter out when (SettleDate - BondSettleDays KWD) is on ValDate");
        this.enableBondSettleDate.addActionListener(list -> enableBondSettleDate.setSelected(enableBondSettleDate.isSelected()));
        JPanel innerPane = new JPanel();
        innerPane.setLayout(new JideBoxLayout(innerPane, 1));
        innerPane.add(this.enableBondSettleDate, "flexible");
        innerPane.add(Box.createVerticalGlue(), "vary");
        this.add(innerPane, "Center");
    }

    @Override
    public void clear() {
        this.enableBondSettleDate.setSelected(false);
    }

    @Override
    public TradeFilterCriterion buildCriterion() {
        TradeFilterCriterion criterion=null;
        if(this.enableBondSettleDate.isSelected()){
            criterion=new CustomCriterionBondSettleDate();
            criterion.setIsInB(true);
            Vector<String> values=new Vector<>();
            values.add("true");
            criterion.setValues(values);
        }
        return criterion;
    }

    @Override
    public void showCriterion(TradeFilterCriterion c) {
        this.enableBondSettleDate.setSelected(c.getIsInB());
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public JPanel getPanel() {
        return this;
    }

}
