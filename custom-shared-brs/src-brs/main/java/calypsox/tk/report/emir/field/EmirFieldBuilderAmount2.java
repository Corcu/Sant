package calypsox.tk.report.emir.field;

//import calypsox.tk.report.SantDTCCGTRUtil;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.tk.util.emir.LegType;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteSet;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.product.PerformanceSwapLeg;
import com.calypso.tk.product.SwapLeg;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.TimeZone;

public class EmirFieldBuilderAmount2 implements EmirFieldBuilder {


    @Override
    public String getValue(Trade trade) {
        Double rst = 0.0;


        if (trade.getProduct()  instanceof PerformanceSwap) {
                PerformanceSwap perfSwap = (PerformanceSwap) trade.getProduct();
                PerformanceSwapLeg pLeg = (PerformanceSwapLeg)perfSwap.getPrimaryLeg();
                if (pLeg == null || !(pLeg.getReferenceProduct() instanceof Bond)) {
                    pLeg = (PerformanceSwapLeg)perfSwap.getSecondaryLeg();
                }

            if (pLeg != null && pLeg.getReferenceProduct() instanceof Bond) {
                Bond bond = (Bond) pLeg.getReferenceProduct();
                rst = pLeg.getInitialPrice();


                PricingEnv pEnv = PricingEnv.loadPE(EmirSnapshotReduxConstants.PRICING_ENV_DIRTY_PRICE, trade.getUpdatedTime());
                if (pEnv != null) {
                    final QuoteSet quoteSet = pEnv.getQuoteSet();
                    //ojo, mirar fecha valueDate
                    JDate quoteDate = trade.getUpdatedTime().getJDate(TimeZone.getDefault());
                    QuoteValue productQuote = quoteSet.getProductQuote(bond, quoteDate, pEnv.getName());
                    if ((productQuote != null) && (!Double.isNaN(productQuote.getClose()))) {
                        rst =  productQuote.getClose();
                    }
                }

            }
        }

        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        DecimalFormat decimalFormat = (DecimalFormat)numberFormat;
        decimalFormat.applyPattern("0.########");
        String rstStr = decimalFormat.format(rst);

        return rstStr;

    }

}
