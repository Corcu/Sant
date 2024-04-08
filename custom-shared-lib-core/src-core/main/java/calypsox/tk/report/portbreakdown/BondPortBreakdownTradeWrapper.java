package calypsox.tk.report.portbreakdown;

import calypsox.tk.report.generic.loader.SantGenericQuotesLoader;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.CollateralConfig;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class BondPortBreakdownTradeWrapper extends PortBreakdownTradeWrapper {


    public BondPortBreakdownTradeWrapper(Trade trade, CollateralConfig mcc, JDate valDate, SantGenericQuotesLoader quotesLoader) {
        super(trade, mcc, valDate, quotesLoader);
    }

    @Override
    protected void buildInstrument(Trade trade) {
        this.instrument = "BOND_FORWARD";
    }

    @Override
    void buildUnderlyings(Product product) {
        this.underlying1 = Optional.ofNullable(product).map(p -> p.getSecCode("ISIN")).orElse("");
        this.underlying2 = "";
    }

    @Override
    void buildPrincipals(Trade input) {
        Product product = Optional.ofNullable(input).map(Trade::getProduct).orElse(null);

        this.principal = Optional.ofNullable(product)
                .filter(p -> p instanceof Bond)
                .map(p -> ((Bond) p).getNominal(input.getQuantity(), input.getSettleDate()))
                .orElse(0.0d);
        this.principalCcy = Optional.ofNullable(product)
                .map(Product::getCurrency)
                .orElse("NONE");

    }
}
