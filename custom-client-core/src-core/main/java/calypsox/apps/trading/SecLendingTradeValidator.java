package calypsox.apps.trading;

import com.calypso.apps.trading.ShowTrade;
import com.calypso.apps.trading.TradeValidator;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.SecLending;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class SecLendingTradeValidator implements TradeValidator {

    public static final String KEYWORD_NAME = "SecLendingTrade";
    public static final String KEYWORD_VALUE = "FICTICIO";
    private List<String> Seclending_AdjFees = Arrays.asList("RV", "RF");
    public static final String SECLENDING_ADJFEE = "Seclending_AdjFee";

    @Override
    public boolean inputInfo(Trade trade, ShowTrade w) {
        return true;
    }

    @Override
    public boolean isValidInput(Trade trade, ShowTrade w, Vector messages) {
        Book book = trade.getBook();

        if (!Util.isEmpty(book.getAttribute(SECLENDING_ADJFEE))) {
            if (Seclending_AdjFees.contains(book.getAttribute(SECLENDING_ADJFEE))) {
                trade.addKeyword(KEYWORD_NAME, KEYWORD_VALUE);
            }
        }

        if (trade.getAction().equals(Action.NEW)){
            trade.addKeyword("PartenonAccountingID","");
        }

        ((SecLending) trade.getProduct()).setPassThrough(true);

        return true;
    }
}
