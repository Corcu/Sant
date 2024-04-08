package calypsox.tk.anacredit.util;

import com.calypso.tk.core.JDate;
import com.calypso.tk.marketdata.PricingEnv;
import org.apache.axis2.databinding.types.xsd._short;

public class EurBasedAmount {

    String ccy;
    Double originalAmount = 0.0d;
    Double eurAmount = 0.0d;

    public void forceSigno(int signo) {
        originalAmount = Math.abs(originalAmount)*signo;
        eurAmount = Math.abs(eurAmount)*signo;
    }

    public EurBasedAmount(String baseCcy , Double originalAmount) {
        this.ccy = baseCcy;
        this.originalAmount = originalAmount;

    }
    public String getCcy() {
        return ccy;
    }
    public Double getOriginalAmount() {
        return originalAmount;
    }
    public Double getEurAmount() {
        return eurAmount;
    }
    public void setCcy(String ccy) {
        this.ccy = ccy;
    }
    public void setEurAmount(Double eurAmount) {
        this.eurAmount = eurAmount;
    }
    public void setOriginalAmount(Double originalAmount) {
        this.originalAmount = originalAmount;
    }

    public  EurBasedAmount  invoke(JDate date, PricingEnv pEnv) {
        double converted = AnacreditUtilities.convertToEUR(originalAmount, ccy, date, pEnv);
        this.eurAmount = converted;
        return this;
    }

}
