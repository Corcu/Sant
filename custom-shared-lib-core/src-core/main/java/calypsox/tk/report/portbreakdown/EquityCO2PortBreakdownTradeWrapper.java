package calypsox.tk.report.portbreakdown;


import calypsox.tk.report.generic.loader.SantGenericQuotesLoader;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.CollateralConfig;
import java.util.Optional;


public class EquityCO2PortBreakdownTradeWrapper extends PortBreakdownTradeWrapper {


    public EquityCO2PortBreakdownTradeWrapper(Trade trade, CollateralConfig mcc, JDate valDate, SantGenericQuotesLoader quotesLoader) {
        super(trade, mcc, valDate, quotesLoader);
    }


    @Override
    protected void buildInstrument(Trade trade) {
        this.instrument = "EQUITY_CO2";
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
                .filter(p -> p instanceof Equity && ("CO2".equalsIgnoreCase(p.getSecCode("EQUITY_TYPE")) || "VCO2".equalsIgnoreCase(p.getSecCode("EQUITY_TYPE"))))
                .map(p -> input.getQuantity() * p.getPrincipal())
                .orElse(0.0d);
        this.principalCcy = Optional.ofNullable(product)
                .map(Product::getCurrency)
                .orElse("NONE");
    }


}
