package calypsox.tk.bo.workflow.rule;

import calypsox.tk.product.secfinance.triparty.CustomAllocationTradeListener;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TaskWorkflowConfig;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.*;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class SantMultiPageTransitionTradeRule implements WfTradeRule {

    private static final List<String> VALUES_MULTIPAGE = Arrays.asList("MORE", "LAST");

    private static final String SEC_CODE_ISIN = "ISIN";
    private static final String ACTION_NOT_EXPORTED = "TRIPARTY_NOT_EXPORTED";
    private static final String ACTION_TRIPARTY_REV_PENDING = "TRIPARTY_REV_PENDING";

    private static final String KEY_REVERSED = "ReversedAllocationTrade";
    private static final String KEY_MUREX = "MurexReversedAllocationTrade";

    private static final String KEY_TRIPARTY_NOT_EXPORTED = "TripartyNotExported";
    private static final String KEY_TRIPARTY_PENDING = "TripartyPending";

    private static final String MESSAGE = "The Triparty Multipage Allocation trade with id";
    private static final String MESSAGE1 = "The trade with id";

    @Override
    public boolean check(TaskWorkflowConfig arg0, Trade trade, Trade arg2, Vector arg3, DSConnection dsConn,
                         Vector arg5, Task arg6, Object arg7, Vector arg8) {

        return true;

    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public boolean update(TaskWorkflowConfig arg0, Trade trade, Trade arg2, Vector arg3, DSConnection dsConn,
                          Vector arg5, Task arg6, Object arg7, Vector arg8) {

        return calculateTransitionsForTripartyMultipageTrade(trade);
    }

    /**
     * Detects Triparty Multipage trades and processes them in a special way.
     * <p>
     * - If the trade is Triparty Multipage: - If trade is Reversed, it won't be
     * allowed to pass to VERIFIED state (a filter in the
     * "PENDING-->TRIPARTY_PENDING_MT569" transition will move the trade to
     * TRIPARTY_PENDING_MT569 state). The trade will be marked (with a
     * "TripartyPending" keyword, to be passed to TRIPARTY_PENDING_MT569 (by a
     * filter). - If trade is Allocation: - If exists a corresponding trade (a
     * Reversed trade with same PREP and ISIN): - If the quantity is equal: the
     * trade won't be allowed to pass to VERIFIED state, the trade will be marked
     * (with a "TripartyNotExported" keyword) to be passed to TRIPARTY_NOT_EXPORTED
     * (by a filter), and the corresponding trade will be moved to
     * TRIPARTY_NOT_EXPORTED too. - If the quantity is distinct: the trade will be
     * allowed to pass to VERIFIED state, and the corresponding trade will be moved
     * to TRIPARTY_PENDING state. - If not exists, the trade will be allowed to pass
     * to VERIFIED state.
     * <p>
     * The marks ("TripartyPending" and "TripartyNotExported" keywords in the trade)
     * will be set here, and will be used by filters associated to the
     * "PENDING-->TRIPARTY_PENDING_MT569" and "PENDING-->TRIPARTY_NOT_EXPORTED"
     * transitions (to decide whether the transition is allowed or not).
     * <p>
     * "PREP" and "ContinuationIndicator" keywords are read here. They will have
     * been already filled (by setKeywordsForTripartyMultipageTrade() method).
     *
     * @return Returns false if error
     */
    private boolean calculateTransitionsForTripartyMultipageTrade(Trade trade) {
        DSConnection dsConn = DSConnection.getDefault();
        boolean toNotExported = false; // ¿trade to be moved to TRIPARTY_NOT_EXPORTED state?
        boolean toPendingMT569 = false; // ¿trade to be moved to TRIPARTY_PENDING_MT569 state?
        Trade reversed = null;

        // Trade required
        if (trade == null)
            return false;

        // A Triparty Multipage trade?
        if (VALUES_MULTIPAGE.contains(trade.getKeywordValue(CustomAllocationTradeListener.KEYWORD_CONTINUATION))) {

            // will be set to true if all steps run fine, after last step
            for (int cOne = 1; cOne <= 1; cOne++) // one time loop, to exit the steps immediately with "break"
            {

                // Find corresponding Triparty Reversed trade (same PREP and Quantity)

                // PREP of the trade
                String prep = trade.getKeywordValue(CustomAllocationTradeListener.KEYWORD_PREP);
                if (Util.isEmpty(prep)) {
                    Log.error(this, MESSAGE + trade.getLongId() + " has no value for keyword "
                            + CustomAllocationTradeListener.KEYWORD_PREP);
                    break; // exit steps immediately, allow transition "PENDING --> VERIFIED"
                }

                // Find trades with same PREP
                Log.info(this, "Loading trades with " + CustomAllocationTradeListener.KEYWORD_PREP + " = " + prep);
                TradeArray samePrepTrades = null;
                try {
                    // do best query
                    samePrepTrades = dsConn.getRemoteTrade().getTrades("TRADE, TRADE_KEYWORD",
                            "TRADE.TRADE_STATUS LIKE 'TRIPARTY_PENDING_MT569' "
                                    + "AND TRADE_KEYWORD.KEYWORD_NAME LIKE 'PREP' "
                                    + "AND TRADE_KEYWORD.KEYWORD_VALUE LIKE '" + prep + "'"
                                    + " and TRADE_KEYWORD.TRADE_ID = TRADE.TRADE_ID",
                            "trade.trade_id", null);

                } catch (CalypsoServiceException e) {
                    Log.error(this, "Cannot load trades with keyword " + CustomAllocationTradeListener.KEYWORD_PREP
                            + " = " + prep + " : " + e);
                    break; // exit steps immediately, allow transition "PENDING --> VERIFIED"
                }

                if (samePrepTrades == null) // getTradesByKeywordNameAndValue() perhaps can return null without
                // exception
                {
                    Log.error(this, "Cannot load trades with keyword " + CustomAllocationTradeListener.KEYWORD_PREP
                            + " = " + prep);
                    break; // exit steps immediately, allow transition "PENDING --> VERIFIED"
                }

                // ISIN of the trade
                Product prod = trade.getProduct();
                if (prod == null) {
                    Log.error(this, MESSAGE + trade.getLongId() + " has no product");
                    break; // exit steps immediately, allow transition "PENDING --> VERIFIED"
                }
                if (!(prod instanceof MarginCall)) {
                    Log.error(this, MESSAGE + trade.getLongId() + " has a product that is not a margin call");
                    break; // exit steps immediately, allow transition "PENDING --> VERIFIED"
                }
                MarginCall mcall = (MarginCall) prod;
                Product security = mcall.getSecurity();
                int allocationContracID = mcall.getMarginCallId();
                if (security == null) {
                    Log.error(this, MESSAGE + trade.getLongId() + " has a margin call that has not security");
                    break; // exit steps immediately, allow transition "PENDING --> VERIFIED"
                }
                String tradeIsin = security.getSecCode(SEC_CODE_ISIN);
                if (Util.isEmpty(tradeIsin)) {
                    Log.error(this, MESSAGE + trade.getLongId() + " has not ISIN");
                    break; // exit steps immediately, allow transition "PENDING --> VERIFIED"
                }

                // Find Reversed trade with same ISIN
                Trade corresTrade = null; // corresponding trade (a Reversed trade with same PREP and ISIN)
                for (Trade spTrade : samePrepTrades.getTrades()) {
                    // Product of the spTrade
                    Product spProd = spTrade.getProduct();
                    if (spProd == null) {
                        Log.info(this, MESSAGE1 + spTrade.getLongId() + " has no product. Skipping it.");
                        continue;
                    }

                    // ISIN of the spTrade
                    if (!(spProd instanceof MarginCall)) {
                        Log.info(this, MESSAGE1 + spTrade.getLongId() + " has a product that is not a margin call");
                        continue;
                    }
                    MarginCall spMcall = (MarginCall) spProd;
                    Product spSecurity = spMcall.getSecurity();
                    if (spSecurity == null) {
                        Log.info(this, MESSAGE1 + spTrade.getLongId() + " has a margin call that has not security");
                        continue;
                    }
                    String spTradeIsin = spSecurity.getSecCode(SEC_CODE_ISIN);
                    if (Util.isEmpty(spTradeIsin)) {
                        Log.info(this, MESSAGE1 + spTrade.getLongId() + " has not ISIN");
                        continue;
                    }

                    int reversedContracID = spMcall.getMarginCallId();

                    // Checks
                    if (reversedContracID == allocationContracID && !Util.isEmpty(spTrade.getKeywordValue(KEY_REVERSED)) // Reversed
                            // trade
                            // && spProd.getSecCode(SEC_CODE_ISIN).equals(tradeIsin) // same ISIN
                            && spTradeIsin.equals(tradeIsin) // same ISIN
                            // && spTrade.getTradeCurrency().equals( trade.getTradeCurrency() ) // same
                            // trade currency (main currency of the trade)
                            && spProd.getCurrency().equals(prod.getCurrency()) // same product currency
                    ) {
                        corresTrade = spTrade;
                        break;
                    }
                }

                // Transitions
                if (corresTrade != null) // corresponding Reversed trade found
                {
                    reversed = corresTrade.clone();
                    if (Math.abs(reversed.getQuantity()) == Math.abs(trade.getQuantity())) // same quantity
                    {
                        toNotExported = true;
                        reversed.setAction(Action.valueOf(ACTION_NOT_EXPORTED));
                        reversed.addKeyword(KEY_TRIPARTY_NOT_EXPORTED, Boolean.toString(true));

                        if (Util.isEmpty(reversed.getKeywordValue(KEY_MUREX))) {
                            reversed.addKeyword(KEY_MUREX, reversed.getKeywordValue(KEY_REVERSED));
                        }

                    } else // different quantity
                    {
                        reversed.setAction(Action.valueOf(ACTION_TRIPARTY_REV_PENDING));
                    }

                    try {
                        dsConn.getRemoteTrade().save(reversed);
                    } catch (Exception e) {
                        Log.error(this, "Cannot save trade: " + reversed.getLongId() + " Error: " + e);
                    }
                }
            } // for cOne

        } // if Triparty Multipage trade

        // Add keywords to trade (we are adding it always, when the method is called ,
        // i.e. when the trade is Triparty (Multipage or not Multipage)
        trade.addKeyword(KEY_TRIPARTY_NOT_EXPORTED, Boolean.toString(toNotExported)); // a filter at
        // "PENDING-->TRIPARTY_NOT_EXPORTED"
        // transition will move the
        // trade to
        // TRIPARTY_NOT_EXPORTED state
        if (reversed != null && toNotExported) {
            trade.addKeyword(KEY_MUREX, reversed.getKeywordValue(KEY_REVERSED));
        }

        trade.addKeyword(KEY_TRIPARTY_PENDING, Boolean.toString(toPendingMT569)); // a filter at
        // "PENDING-->TRIPARTY_PENDING_MT569"
        // transition will move the trade to
        // TRIPARTY_PENDING_MT569 state

        return true;
    }

}
