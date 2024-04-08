package calypsox.apps.trading;

import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.calypso.apps.trading.ShowTrade;
import com.calypso.apps.trading.TradeValidator;
import com.calypso.tk.core.Trade;

import calypsox.apps.reporting.SantPolandSecurityPledgeUtil;

public class MarginCallTradeValidator extends JFrame implements TradeValidator {

    private static final String REVERSE_TRADE_MESSAGE = "<html><div align=\"center\">"
            + "This reverse trade will only be canceled in Calypso.<br/>"
            + "It must be canceled in Murex manually.<br/>"
            + "Do you wish to continue?</div></html>";
    private static final String REVERSE_TRADE_DIALOG_TITLE = "Reverse trade";

    private static final long serialVersionUID = 1L;

    @Override
    public boolean inputInfo(Trade trade, ShowTrade showTrade) {
        return true;
    }

    @Override
    public boolean isValidInput(Trade trade, ShowTrade showTrade,
            Vector messages) {
        boolean validInput = true;

        // If the user wants to cancel a reverse trade, a dialog warns the user
        // that this message should be canceled in Murex manually.
        if (SantPolandSecurityPledgeUtil.isReverseTrade(trade)
                && SantPolandSecurityPledgeUtil.ACTION_REQUEST_CANCEL
                        .equals(trade.getAction())) {
            int confirmSave = JOptionPane.showConfirmDialog(this,
                    REVERSE_TRADE_MESSAGE, REVERSE_TRADE_DIALOG_TITLE,
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            validInput = confirmSave == JOptionPane.YES_OPTION;
        }

        return validInput;
    }

}
