package calypsox.tk.util.mxnpv;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;

/**
 *
 * Represents one file line
 */
public class MxNPVData {

        JDate processDate;
        String contractID;
        String currency;
        double npv;
        Trade trade = null;

        public JDate getProcessDate() {
            return processDate;
        }

        public void setProcessDate(JDate processDate) {
            this.processDate = processDate;
        }

        public String getContractID() {
            return contractID;
        }

        public void setContractID(String contractID) {
            this.contractID = contractID;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public double getNpv() {
            return npv;
        }

        public void setNpv(double npv) {
            this.npv = npv;
        }

        public Trade getTrade() {
            return trade;
        }

        public void setTrade(Trade trade) {
            this.trade = trade;
        }

}
