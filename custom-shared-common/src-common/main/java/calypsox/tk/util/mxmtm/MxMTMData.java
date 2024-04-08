package calypsox.tk.util.mxmtm;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;

/**
 *
 * Represents one file line
 */
public class MxMTMData {

        JDate processDate;
        String contractID;
        String dealID;
        int blockNumber;
        long frontID;
        int dealGRID;
        String currency;
        String dummy;
        double interestCash;
        double buySellCash;
        double taxComCash;
        double carryAcc;
        double marketvalueMan;
        String entity;
        String processID;
        int generatorID;
        String compraVenta;
        double pastCash;
        double futureCash;
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

        public String getDealID() {
            return dealID;
        }

        public void setDealID(String dealID) {
            this.dealID = dealID;
        }

        public int getBlockNumber() {
            return blockNumber;
        }

        public void setBlockNumber(int blockNumber) {
            this.blockNumber = blockNumber;
        }

        public long getFrontID() {
            return frontID;
        }

        public void setFrontID(long frontID) {
            this.frontID = frontID;
        }

        public int getDealGRID() {
            return dealGRID;
        }

        public void setDealGRID(int dealGRID) {
            this.dealGRID = dealGRID;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getDummy() {
            return dummy;
        }

        public void setDummy(String dummy) {
            this.dummy = dummy;
        }

        public double getInterestCash() {
            return interestCash;
        }

        public void setInterestCash(double interestCash) {
            this.interestCash = interestCash;
        }

        public double getBuySellCash() {
            return buySellCash;
        }

        public void setBuySellCash(double buySellCash) {
            this.buySellCash = buySellCash;
        }

        public double getTaxComCash() {
            return taxComCash;
        }

        public void setTaxComCash(double taxComCash) {
            this.taxComCash = taxComCash;
        }

        public double getCarryAcc() {
            return carryAcc;
        }

        public void setCarryAcc(double carryAcc) {
            this.carryAcc = carryAcc;
        }

        public double getMarketvalueMan() {
            return marketvalueMan;
        }

        public void setMarketvalueMan(double marketvalueMan) {
            this.marketvalueMan = marketvalueMan;
        }

        public String getEntity() {
            return entity;
        }

        public void setEntity(String entity) {
            this.entity = entity;
        }

        public String getProcessID() {
            return processID;
        }

        public void setProcessID(String processID) {
            this.processID = processID;
        }

        public int getGeneratorID() {
            return generatorID;
        }

        public void setGeneratorID(int generatorID) {
            this.generatorID = generatorID;
        }

        public String getCompraVenta() {
            return compraVenta;
        }

        public void setCompraVenta(String compraVenta) {
            this.compraVenta = compraVenta;
        }

        public double getPastCash() {
            return pastCash;
        }

        public void setPastCash(double pastCash) {
            this.pastCash = pastCash;
        }

        public double getFutureCash() {
            return futureCash;
        }

        public void setFutureCash(double futureCash) {
            this.futureCash = futureCash;
        }


        public Trade getTrade() {
            return trade;
        }

        public void setTrade(Trade trade) {
            this.trade = trade;
        }

}
