package calypsox.util;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PLMark;

/**
 * @author aalonsop
 * PLMark builder class
 * Ensures that every custom created PLMark has a 'PL' type
 */
public class SantPLMarkBuilder {

    private Trade trade;
    private String pricingEnvName;
    private JDate valDate;
    private long tradeId;
    private int bookId;

    public SantPLMarkBuilder() {
        //EMPTY
    }

    public SantPLMarkBuilder forTrade(Trade trade) {
        this.trade = trade;
        return this;
    }

    public SantPLMarkBuilder forTradeId(long tradeId) {
        this.tradeId = tradeId;
        return this;
    }

    public SantPLMarkBuilder inBook(int bookId) {
        this.bookId = bookId;
        return this;
    }

    public SantPLMarkBuilder withPricingEnv(String pricingEnvName) {
        this.pricingEnvName = pricingEnvName;
        return this;
    }

    public SantPLMarkBuilder atDate(JDate valDate) {
        this.valDate = valDate;
        return this;
    }

    /**
     * @return
     */
    private PLMark instanciateNoTrade() {
        return new PLMark();
    }

    /**
     * @return
     */
    private PLMark instanciateWithTrade() {
        return new PLMark(this.trade, null, null);
    }

    /**
     * @return
     */
    public PLMark build() {
        PLMark plMark;
        if (this.trade != null) {
            plMark = instanciateWithTrade();
        } else {
            plMark = instanciateNoTrade();
            plMark.setTradeLongId(tradeId);
            plMark.setBookId(bookId);
        }
        plMark.setType(PLMark.TYPE_MARK_PL);
        plMark.setValDate(this.valDate);
        plMark.setPricingEnvName(this.pricingEnvName);
        return plMark;
    }
}
