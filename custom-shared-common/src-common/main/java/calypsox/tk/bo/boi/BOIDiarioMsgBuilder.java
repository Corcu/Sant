package calypsox.tk.bo.boi;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.MarketDataException;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.pricer.PricerRepo;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.PricerMeasureUtility;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Optional;

/**
 * @author aalonsop
 */
public abstract class BOIDiarioMsgBuilder {

    Trade trade;
    JDate processDate;
    BOIGenericFormatter formatter;


    public BOIDiarioMsgBuilder(Trade trade,JDate processDate) {
        this.trade = trade;
        this.processDate = processDate;
        this.formatter = BOIGenericFormatter.getInstance();

    }
    public abstract List<BOIDiarioBean> build(PricingEnv env);


    protected PLMark getPLMarkValue(PricingEnv pricingEnv, Trade trade, JDate date) {
        PLMark plMark=null;
        if(null!=date && null!=pricingEnv) {
            date = date.addBusinessDays(-1, Util.string2Vector("SYSTEM"));
            try {
                plMark = CollateralUtilities.retrievePLMark(trade, DSConnection.getDefault(),
                        pricingEnv.getName(), date);
            } catch (RemoteException exc) {
                Log.error(this, exc.getCause());
            }
        }
        return plMark;
    }

    protected PLMark getPLMarkValueOFFICIAL_ACCOUNTING(PricingEnv pricingEnv, Trade trade, JDate date) {
        PLMark plMark=null;
        if(null!=date && null!=pricingEnv) {
            date = date.addBusinessDays(-1, Util.string2Vector("SYSTEM"));
            try {
                plMark = CollateralUtilities.retrievePLMark(trade, DSConnection.getDefault(),
                        "OFFICIAL_ACCOUNTING", date);
            } catch (RemoteException exc) {
                Log.error(this, exc.getCause());
            }
        }
        return plMark;
    }

    protected Double getPLMark(PLMark plMark, String type){
        return null!=plMark && null!=plMark.getPLMarkValueByName(type) ? plMark.getPLMarkValueByName(type).getMarkValue() : 0.0D;
    }

    protected double convertToEUR(final double amount, final String ccy, final JDate date, final PricingEnv pricingEnv)
            throws MarketDataException {
        if (amount == 0.0) {
            return 0.0;
        }

        if (BOIStaticData.EUR.equals(ccy)) {
            return amount;
        }
        QuoteValue quote = pricingEnv.getQuoteSet().getFXQuote(BOIStaticData.EUR, ccy, date);
        if ((quote != null) && !Double.isNaN(quote.getClose())) {
            return amount / quote.getClose();
        } else {
            quote = pricingEnv.getQuoteSet().getFXQuote(ccy, BOIStaticData.EUR, date);
            if ((quote != null) && !Double.isNaN(quote.getClose())) {
                return amount * quote.getClose();
            } else {
                throw new MarketDataException("FX Quote not found for the currency combination " + ccy + "/"
                        + BOIStaticData.EUR + " or " + BOIStaticData.EUR + "/" + ccy + " on " + date.toString());
            }
        }
    }

    protected Double calculatePricerMeasure(String pmName, Trade trade, PricingEnv env){
        double pricerMeasureAmt=0.0D;
        Pricer pricer=new PricerRepo();
        PricerMeasure measure = PricerMeasureUtility.makeMeasure(pmName);
        try {
            pricer.price(trade, this.processDate.getJDatetime(), env, new PricerMeasure[]{measure});
            pricerMeasureAmt= Optional.ofNullable(measure).map(PricerMeasure::getValue)
                    .orElse(0.0D);
        } catch (PricerException exc) {
           Log.warn(this,exc.getCause());
        }
        return pricerMeasureAmt;
    }
}
