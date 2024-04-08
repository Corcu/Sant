package calypsox.tk.report;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.FX;
import com.calypso.tk.product.FXForward;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReport;
import com.calypso.tk.service.DSConnection;

import java.util.*;

import static calypsox.tk.report.FXPLMarkReportStyle.getOtherMultiCcyTrade;

/**
 * @author dmenendd
 */
public class BondInventarioCompromisoReport extends TradeReport {

    private static final long serialVersionUID = 1L;
    private static final String PARTENON_KWD = "PartenonAccountingID";
    private static final String ISSUE_TYPE = "ISSUE_TYPE";


    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ReportOutput load(Vector errorMsgs) {

        final DefaultReportOutput output = (DefaultReportOutput)super.load(errorMsgs);
        if(output==null) {
            return null;
        }

        final List<ReportRow> rows = new ArrayList<ReportRow>();
        for (final ReportRow row : output.getRows()){
            Trade trade = row.getProperty(ReportRow.TRADE);
            if (trade.getProduct() instanceof Bond){
                row.setProperty(PARTENON_KWD, Optional.of(trade).
                        map(t -> t.getKeywordValue(PARTENON_KWD)).orElse("0"));
                Bond bond = (Bond) trade.getProduct();
                row.setProperty("Bond Product", bond);

                PricingEnv pricingEnv = ReportRow.getPricingEnv(row);

                JDatetime valDatetime = ReportRow.getValuationDateTime(row);
                JDate valDate = valDatetime.getJDate(pricingEnv.getTimeZone());
                row.setProperty("valDateTime", valDatetime);
                row.setProperty("valDate", valDate);

                PricingEnv pricingEnvOA = PricingEnv.loadPE("OFFICIAL_ACCOUNTING", valDatetime);
                row.setProperty("pricingEnvOA", pricingEnvOA);

                LegalEntity issuer = getIssuer(bond);
                row.setProperty("Issuer", issuer);

                Trade fxTrade = getOtherMultiCcyTrade(trade);
                row.setProperty("fxTrade", fxTrade);

                //Capital Factor values
                double poolFactor = calculatePoolFactor(bond, trade);
                double inflationFactor = getInflationFactor(bond, trade, pricingEnv);
                row.setProperty("calypsoPoolFactor", poolFactor);
                row.setProperty("calypsoCapitalFactor", poolFactor * inflationFactor);

                //FX Values
                String ccyCompra = "";
                double nominalFXCompra = 0.0;
                double nominalFXCompraContravalor = 0.0;
                double npvCompra = 0.0;
                double npvCompraContravalor = 0.0;
                String ccyVenta = "";
                double nominalFXVenta = 0.0;
                double nominalFXVentaContravalor = 0.0;
                double npvVenta = 0.0;
                double npvVentaContravalor = 0.0;
                double fxCompra;
                double fxVenta;

                if (fxTrade != null){

                    ccyCompra = getCurrency(fxTrade, true);
                    nominalFXCompra = getNominal(fxTrade, true);
                    fxCompra = CollateralUtilities.getFXRatebyQuoteSet(valDate, ccyCompra, "EUR", null);
                    nominalFXCompraContravalor = getContravalor(nominalFXCompra, fxCompra, ccyCompra);

                    ccyVenta = getCurrency(fxTrade, false);
                    nominalFXVenta = getNominal(fxTrade, false);
                    fxVenta = CollateralUtilities.getFXRatebyQuoteSet(valDate, ccyVenta, "EUR", null);
                    nominalFXVentaContravalor = getContravalor(nominalFXVenta, fxVenta, ccyVenta);

                    if (isChildBond(trade)){
                        Trade parentTrade = retrieveMotherTrade(trade);
                        npvCompra = getNPVValue(getNPVPLMark(parentTrade, valDate), true);
                        npvVenta = getNPVValue(getNPVPLMark(parentTrade, valDate), false);

                        npvCompra = trade.getQuantity() / parentTrade.getAllocatedQuantity() * npvCompra;
                        npvVenta = trade.getQuantity() / parentTrade.getAllocatedQuantity() * npvVenta;
                    } else {
                        npvCompra = getNPVValue(getNPVPLMark(trade, valDate), true);
                        npvVenta = getNPVValue(getNPVPLMark(trade, valDate), false);
                    }
                    npvCompraContravalor = getContravalor(npvCompra, fxCompra, ccyCompra);
                    npvVentaContravalor = getContravalor(npvVenta, fxVenta, ccyVenta);
                }

                row.setProperty("ccyCompra", ccyCompra);
                row.setProperty("nominalFXCompra", nominalFXCompra);
                row.setProperty("nominalFXCompraContravalor", nominalFXCompraContravalor);
                row.setProperty("ccyVenta", ccyVenta);
                row.setProperty("nominalFXVenta", nominalFXVenta);
                row.setProperty("nominalFXVentaContravalor", nominalFXVentaContravalor);
                row.setProperty("npvCompra", npvCompra);
                row.setProperty("npvVenta", npvVenta);
                row.setProperty("npvCompraContravalor", npvCompraContravalor);
                row.setProperty("npvVentaContravalor", npvVentaContravalor);
            }

            rows.add(row);
        }

        output.setRows(rows.toArray(new ReportRow[rows.size()]));
        return output;
    }

    private LegalEntity getIssuer(Bond bond) {
        LegalEntity issuer = BOCache.getLegalEntity(DSConnection.getDefault(), bond.getIssuerId());
        return null != bond && null!=issuer ? issuer : null;
    }

    private double calculatePoolFactor(Bond bond, Trade trade){
        return bond.getCurrentFactor(JDate.valueOf(trade.getSettleDate()));
    }

    private double getInflationFactor (Bond bond, Trade trade, PricingEnv pricingEnv) {
        double inflationFactor=1;
        try {
            inflationFactor=bond.getNotionalIndexFactor(trade.getSettleDate(), pricingEnv.getQuoteSet());
        } catch (FlowGenerationException exc) {
            Log.error(this,exc.getCause());;
        }
        return inflationFactor;
    }

    private String getCurrency(Trade trade, boolean isBuy){
        if(trade.getProduct() instanceof FX){
            FX fx = (FX) trade.getProduct();
            return isBuy ? fx.getPrimaryCurrency(): fx.getQuotingCurrency();
        } else {
            FXForward fx = (FXForward) trade.getProduct();
            return isBuy ? fx.getPrimaryCurrency(): fx.getQuotingCurrency();
        }
    }

    private double getNominal(Trade trade, boolean isBuy){
        if(trade.getProduct() instanceof FX){
            FX fx = (FX) trade.getProduct();
            return isBuy ? fx.getPrimaryAmount(trade).get(): fx.getQuoteAmount(trade).get();
        }
        else {
            FXForward fx = (FXForward) trade.getProduct();
            return isBuy ? fx.getPrimaryAmount(trade).get(): fx.getQuoteAmount(trade).get();
        }
    }

    private double getContravalor(double amount, double fxRate, String ccy) {
        if(fxRate != 0 && !"EUR".equalsIgnoreCase(ccy)){
            return fxRate * amount;
        } else
            return amount;
    }

    private boolean isChildBond(Trade trade){
        return !Util.isEmpty(trade.getKeywordValue("AllocatedFrom"));
    }

    private Trade retrieveMotherTrade(Trade trade){
        Trade motherTrade = null;
        try {
            int id = trade.getKeywordAsInt("AllocatedFrom");
            motherTrade = DSConnection.getDefault().getRemoteTrade().getTrade(id);
        } catch (CalypsoServiceException e) {
            Log.error(e,"Can't retrieve any trade with id:" + trade.getKeywordAsInt("AllocatedFrom"));
        }
        return motherTrade;
    }

    private double getNPVValue(Set<PLMark> marks, boolean isBuy){
        double value = 0;
        try {
            for(PLMark mark:marks){
                value = isBuy? mark.getPLMarkValueByName("NPV_LEG1").getMarkValue() : mark.getPLMarkValueByName("NPV_LEG2").getMarkValue();
            }
        }catch (NullPointerException e){
            Log.error(this, e);
            return 0;
        }
        return value;
    }

    private Set<PLMark> getNPVPLMark (Trade trade, JDate date) {
        Set<String> NPV_MarksNames = new HashSet<>();
        NPV_MarksNames.add("NPV_LEG1");
        NPV_MarksNames.add("NPV_LEG2");
        Set<PLMark> NPV_Marks = new HashSet<>();
        try {
            NPV_Marks = DSConnection.getDefault().getRemoteMarketData().getPLMarks(trade.getLongId(), NPV_MarksNames, "OFFICIAL_ACCOUNTING",date);
        } catch (CalypsoServiceException e) {
            throw new RuntimeException(e);
        }
        return NPV_Marks;
    }
}