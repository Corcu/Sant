package calypsox.tk.bo.fiflow.builder.handler;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Security;
import com.calypso.tk.product.factory.QuoteTypeEnum;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class FIFlowTradeSecurityHandler {

    Bond tradeSecurity;

    QuoteValue cleanPrice;
    QuoteValue dirtyPrice;

    public void initRelatedSecutityData(Product product) {
        tradeSecurity = findSecurityFromProduct(product);
    }

    public void initRelatedSecPricesData(JDate quoteDate) {
        cleanPrice = getSecurityQuoteValue(QuoteTypeEnum.CLEANPRICE.getName(), quoteDate, QuoteTypeEnum.CLEANPRICE.getName());
        dirtyPrice = getSecurityQuoteValue(QuoteTypeEnum.DIRTYPRICE.getName(), quoteDate, QuoteTypeEnum.DIRTYPRICE.getName());
    }

    public String getSecCodeFromBond(String secCode) {
        return Optional.ofNullable(tradeSecurity).map(bond -> bond.getSecCode(secCode)).orElse("");
    }

    public String getCurrencyFromBond() {
        return Optional.ofNullable(tradeSecurity).map(Bond::getCurrency).orElse("");
    }

    QuoteValue getSecurityQuoteValue(String quoteSet, JDate quoteDate, String quoteType) {
        return Optional.ofNullable(tradeSecurity).map(Bond::getQuoteName)
                .map(name -> new QuoteValue(quoteSet, name, quoteDate, quoteType, 0D, 0D, 0D, 0D)).map(qv -> BOCache.getQuote(DSConnection.getDefault(), qv)).orElse(null);

    }

    Bond findSecurityFromProduct(Product product) {
        Bond security = null;
        if (product instanceof Security) {
            security = (Bond) Optional.ofNullable(((Security) product).getSecurity())
                    .filter(bond -> bond instanceof Bond).orElse(null);
        } else {
            Log.warn(this.getClass().getSimpleName(), "Trade's product doesn't implement Security");
        }
        return security;
    }

    public QuoteValue getCleanPrice() {
        return cleanPrice;
    }

    public QuoteValue getDirtyPrice() {
        return dirtyPrice;
    }

    public Bond getTradeSecurity() {
        return this.tradeSecurity;
    }

    public double getNotionalIndexFactor(JDate settleDate, PricingEnv pricingEnv) {
        return Optional.ofNullable(this.tradeSecurity)
                .map(bond -> {
                    double notionalFactor = 1.0d;
                    try {
                        notionalFactor = bond.getNotionalIndexFactor(settleDate, pricingEnv.getQuoteSet());
                    } catch (FlowGenerationException exc) {
                        Log.error(this, exc.getCause());
                    }
                    return notionalFactor;
                })
                .orElse(1.0d);
    }

    public DisplayValue getSecurityPriceDisplayValue(double price) {
        return Optional.ofNullable(this.tradeSecurity)
                .map(Bond::getPriceDisplayValue)
                .map(displayValue -> {
                    displayValue.set(price);
                    return displayValue;
                })
                .orElse(new BondPrice(price, 100));
    }

    public double getPrincipal(Trade trade) {
        return Optional.ofNullable(this.tradeSecurity)
                .map(bond -> bond.calcPurchasedAmount(trade))
                .orElse(0.0d);
    }


    public boolean isUDIBond(){
        return Optional.ofNullable(this.tradeSecurity)
                .map(Bond::getCurrency)
                .map("UDI"::equals)
                .orElse(false);
    }

}
