package calypsox.tk.report;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Repo;
import com.calypso.tk.service.DSConnection;
import java.util.TimeZone;

public class FI105RepoBean {

    private long operacion;
    private JDate fechaContratacion;
    private JDate fechaValor;
    private String signo;
    private Double activoIni = 0.0d;
    private Double pasivoIni = 0.0d;
    private Double operacionContratada = 0.0d;
    private Double variaciones  = 0.0d;
    private Double activoFin = 0.0d;
    private Double pasivoFin = 0.0d;

    public FI105RepoBean() {
    }

    public void build(Trade trade, JDatetime valuationDatetime, JDate lastDayOfPreviousMonth, PricingEnv pEnv) {

        this.operacion = trade.getLongId();
        this.fechaContratacion = trade.getTradeDate().getJDate(TimeZone.getDefault());
        Repo repo = (Repo) trade.getProduct();
        this.fechaValor = repo.getStartDate();
        JDate valuationDate = valuationDatetime.getJDate(TimeZone.getDefault());

        this.signo = trade.computeNominal() > 0 ? "C" : "V";

        boolean isVivaAntesConValorEnElMes = this.fechaContratacion.before(lastDayOfPreviousMonth)
                && this.fechaValor.after(lastDayOfPreviousMonth) && this.fechaValor.before(valuationDate);;

        boolean isVivaAntesConValorDespues = this.fechaContratacion.before(lastDayOfPreviousMonth)
               && this.fechaValor.after(valuationDate);

        boolean isContratoValorEnElMes = this.fechaContratacion.after(lastDayOfPreviousMonth)
                && this.fechaValor.after(lastDayOfPreviousMonth) && this.fechaValor.before(valuationDate);

        boolean isContratoValorDespues = this.fechaContratacion.after(lastDayOfPreviousMonth)
                && this.fechaValor.after(valuationDate);

        if (isVivaAntesConValorEnElMes)  {
            double mtm = getMarketValueEURat( new JDatetime(lastDayOfPreviousMonth,TimeZone.getDefault()), trade, pEnv);
            setMtmIni(mtm);
            this.variaciones = getMtmIni()*-1;
        }

        if (isVivaAntesConValorDespues)  {
            double mtmIni = getMarketValueEURat( new JDatetime(lastDayOfPreviousMonth,TimeZone.getDefault()), trade, pEnv);
            double mtmFin = getMarketValueEURat(valuationDatetime, trade, pEnv);
            setMtmIni(mtmIni);
            setMtmFin(mtmFin);
            this.variaciones = getMtmFin() - getMtmIni();
        }

        if (isContratoValorEnElMes) {
            double mtm = getMarketValueEURat(trade.getTradeDate(), trade, pEnv);
            this.operacionContratada = mtm;
            this.variaciones = this.operacionContratada *-1;
        }

        if (isContratoValorDespues) {
            double mtm = getMarketValueEURat(trade.getTradeDate(), trade, pEnv);
            double mtmEnd = getMarketValueEURat( valuationDatetime, trade, pEnv);
            this.operacionContratada = mtm;
            setMtmFin(mtmEnd);
            this.variaciones = getMtmFin() - operacionContratada;
        }
    }

    private void setMtmIni(double mtmIni) {
        if (signo.equals("C") ) {
            this.activoIni = mtmIni;
        } else {
            this.pasivoIni = mtmIni;
        }
    }

    private void setMtmFin(double mtmEnd) {
        if (signo.equals("C") ) {
            this.activoFin = mtmEnd;
        } else {
            this.pasivoFin = mtmEnd;
        }
    }

    private Double getMtmIni() {
        return signo.equals("C") ? activoIni : pasivoIni;
    }

    private Double getMtmFin() {
        return signo.equals("C") ? activoFin : pasivoFin;
    }

    private Double getMarketValueEURat(JDatetime date, Trade trade, PricingEnv pEnv)  {
        PricingEnv official = PricingEnv.loadPE("OFFICIAL", date);
        Double result = 0.0d;
        try {
            JDate valDate = date.getJDate(TimeZone.getDefault());
            PLMark plMark = CollateralUtilities.retrievePLMark(trade, DSConnection.getDefault(), pEnv.getName(), valDate);
            if (plMark != null) {
                PLMarkValue plValue = CollateralUtilities.retrievePLMarkValue(plMark, "MARKETVALUEMAN");
                result = convertToEUR(plValue.getMarkValue(), plValue.getCurrency(), valDate, official);
            }
        } catch (Exception e) {
            Log.error(this, e);
        }
        return  result;
    }

    public static Double convertToEUR(Double value, String origCurrency, JDate valDate, PricingEnv env) {
        if ("EUR".equalsIgnoreCase(origCurrency)) {
            return value;
        }
        Double valueEUR = 0.0;
        try {
            double fxRate = CollateralUtilities.getFXRatebyQuoteSet(valDate, origCurrency, "EUR", env);
            valueEUR = value * fxRate;
            if(valueEUR == 0.0){
                return value;
            }
        } catch (Exception e)  {
            Log.error(Log.CALYPSOX,"Error getting FXQuotes.", e);
        }
        return valueEUR;
    }

    public long getOperacion() {
        return operacion;
    }

    public JDate getFechaContratacion() {
        return fechaContratacion;
    }

    public JDate getFechaValor() {
        return fechaValor;
    }

    public String getSigno() {
        return signo;
    }

    public Double getActivoIni() {
        return activoIni;
    }

    public Double getPasivoIni() {
        return pasivoIni;
    }

    public Double getOperacionContratada() {
        return operacionContratada;
    }

    public Double getVariaciones() {
        return variaciones;
    }

    public Double getActivoFin() {
        return activoFin;
    }

    public Double getPasivoFin() {
        return pasivoFin;
    }

}
