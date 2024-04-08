package calypsox.tk.report.portbreakdown;

import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.pricer.PricerRepo;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.SecFinance;
import com.calypso.tk.product.util.NotionalDate;
import com.calypso.tk.risk.pl.EmptyDisplayValue;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.PricerMeasureUtility;

import java.util.Optional;
import java.util.TimeZone;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class RepoMxPortBreakdownWrapper extends MxPortBreakdownWrapper {


    public RepoMxPortBreakdownWrapper(PLMark plMark, Trade trade, JDatetime valDate) {
        super(plMark, trade, valDate);
    }

    @Override
    protected double buildClosingPrice(Trade trade,PLMark plMark){
      return getQuoteFromRepoSecurity(trade,"DirtyPrice");
    }

    @Override
    protected void buildCleanPrice(Trade trade,PLMark plMark){
        this.cleanPrice=getQuoteFromRepoSecurity(trade,"CleanPrice");
        Repo repo= (Repo) trade.getProduct();
        this.cleanPriceCcy=Optional.ofNullable(repo.getSecurity()).map(Product::getCurrency).orElse("");
    }

    @Override
    protected void buildRepoAccruedInterest(Trade trade,PLMark plMark) {
        this.repoAccruedInterest=calculatePricerMeasure("ACCRUAL_FIRST",trade,PricingEnv.loadPE("OFFICIAL_ACCOUNTING",valDate));
        this.repoAccruedInterestCcy=(Optional.ofNullable(trade).map(Trade::getProduct).map(Product::getCurrency).orElse(""));
    }

    @Override
    protected void buildBondAccruedInterest(Trade trade,PLMark plMark) {
        this.bondAccruedInterest=this.getClosingPriceRaw()-this.getCleanPriceRaw();
        this.bondAccruedInterestCcy=this.cleanPriceCcy;
    }

    private double getQuoteFromRepoSecurity(Trade trade,String quoteSetName){
        Repo repo= (Repo) trade.getProduct();
        String quoteName=Optional.ofNullable(repo.getSecurity()).map(Product::getQuoteName).orElse("");
        return getQuoteFromQuoteSet(quoteName,quoteSetName,valDate);
    }

    private double getQuoteFromQuoteSet(String quoteName,String quoteSetName,JDatetime valDate){
        double quoteCloseValue=0.0d;
        Vector<String> quoteNames=new Vector<>();
        quoteNames.add(quoteName);
        try {
            Vector quotes=DSConnection.getDefault().getRemoteMarketData().getQuotes(valDate,quoteSetName,quoteNames);
            if(!Util.isEmpty(quotes)){
                quoteCloseValue=Optional.ofNullable(quotes.get(0))
                        .filter(q->q instanceof QuoteValue).map(q -> ((QuoteValue)q).getClose()).orElse(0.0d);
            }
        } catch (CalypsoServiceException exc) {
            Log.warn(this,exc.getCause());
        }
        return quoteCloseValue*100;
    }

    protected Double calculatePricerMeasure(String pmName, Trade trade, PricingEnv env){
        double pricerMeasureAmt=0.0D;
        Pricer pricer=new PricerRepo();
        PricerMeasure measure = PricerMeasureUtility.makeMeasure(pmName);
        try {
            pricer.price(trade, valDate, env, new PricerMeasure[]{measure});
            pricerMeasureAmt= Optional.ofNullable(measure).map(PricerMeasure::getValue)
                    .orElse(0.0D);
        } catch (PricerException exc) {
            Log.warn(this,exc.getCause());
        }
        return pricerMeasureAmt;
    }

    @Override
    public DisplayValue getCapitalFactor(Trade trade, JDate valDate) {
        DisplayValue res=new EmptyDisplayValue();
        Repo repo= (Repo) trade.getProduct();
        Bond bond= (Bond) Optional.ofNullable(repo.getSecurity()).filter(sec->sec instanceof Bond).orElse(null);
        boolean isSinkingSchedule=Optional.ofNullable(bond)
                .filter(Bond::getAmortizingFaceValueB)
                .map(Bond::getPrincipalStructure)
                .map("Schedule"::equalsIgnoreCase).orElse(false);
        if(isSinkingSchedule){
            res=new Amount(getPeriodNotionalPercentaje(bond.getAmortSchedule(),valDate));
        }
        return res;
    }

    private double getPeriodNotionalPercentaje(Vector amortSchedule,JDate valDate){
        double res=0.0d;
        if(!Util.isEmpty(amortSchedule)) {
            //Iterating from behind to get the newest period before
            for (int i = amortSchedule.size() - 1; i >= 0; i--) {
                Object nd = amortSchedule.get(i);
                if (nd instanceof NotionalDate) {
                    if (valDate.gte(((NotionalDate) nd).getStartDate())) {
                        double notionalAmt=((NotionalDate) nd).getNotionalAmt();
                        if(notionalAmt>0.0d) {
                            res = notionalAmt/ 100;
                        }
                        break;
                    }
                }
            }
        }
        return res;
    }

    protected void buildRepoAccruedBOInterest(Trade trade, PLMark plMark) {
        JDate prevValDate = valDate.getJDate(TimeZone.getDefault()).addBusinessDays(-1, Util.string2Vector("SYSTEM"));
        SecFinance product = ((Repo) trade.getProduct());
        if (product.getMaturityType().equals("OPEN") || product.getEndDate().equals(valDate.getJDate(TimeZone.getDefault()))) {
            this.repoAccruedBOInterest = calculatePricerMeasure("ACCRUAL_FIRST", trade, PricingEnv.loadPE("OFFICIAL_ACCOUNTING", prevValDate.getJDatetime()));
        } else {
            this.repoAccruedBOInterest = calculatePricerMeasure("ACCRUAL_BO", trade, PricingEnv.loadPE("OFFICIAL_ACCOUNTING", valDate));
        }
        this.repoAccruedInterestCcy = (Optional.ofNullable(trade).map(Trade::getProduct).map(Product::getCurrency).orElse(""));
    }
}
