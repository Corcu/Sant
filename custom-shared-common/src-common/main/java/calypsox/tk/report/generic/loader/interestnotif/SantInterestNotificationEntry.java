/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report.generic.loader.interestnotif;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;

public class SantInterestNotificationEntry {

    private String callAccountName;
    private String contractName;
    private JDate date;
    private double movement;
    private String currency;
    private double principal;
    private String indexName;
    private double rate;
    private double spread;
    private double adjustedRate;
    private double dailyAccrual;
    private double totalAccrual;
    private long tradeId;
    private int callAccountId;
    private int contractId;
    private boolean adHoc;
    private String poName;
    private String watchInterest;

    LegalEntity counterparty;
    String contractType;

    public String getCallAccountName() {
        return this.callAccountName;
    }

    public void setCallAccountName(final String callAccountName) {
        this.callAccountName = callAccountName;
    }

    public String getContractName() {
        return this.contractName;
    }

    public void setContractName(final String contractName) {
        this.contractName = contractName;
    }

    public JDate getDate() {
        return this.date;
    }

    public void setDate(final JDate date) {
        this.date = date;
    }

    public double getMovement() {
        return this.movement;
    }

    public void setMovement(final double movement) {
        this.movement = movement;
    }

    public String getCurrency() {
        return this.currency;
    }

    public void setCurrency(final String currency) {
        this.currency = currency;
    }

    public double getPrincipal() {
        return this.principal;
    }

    public void setPrincipal(final double principal) {
        this.principal = principal;
    }

    public String getIndexName() {
        return this.indexName;
    }

    public void setIndexName(final String indexName) {
        this.indexName = indexName;
    }

    public double getRate() {
        return this.rate;
    }

    public void setRate(final double rate) {
        this.rate = rate;
    }

    public double getSpread() {
        return this.spread;
    }

    public void setSpread(final double spread) {
        this.spread = spread;
    }

    public double getAdjustedRate() {
        return this.adjustedRate;
    }

    public void setAdjustedRate(final double adjustedRate) {
        this.adjustedRate = adjustedRate;
    }

    public double getDailyAccrual() {
        return this.dailyAccrual;
    }

    public void setDailyAccrual(final double dailyAccrual) {
        this.dailyAccrual = dailyAccrual;
    }

    public double getTotalAccrual() {
        return this.totalAccrual;
    }

    public void setTotalAccrual(final double totalAccrual) {
        this.totalAccrual = totalAccrual;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof SantInterestNotificationEntry)) {
            return false;
        }
        final SantInterestNotificationEntry local = (SantInterestNotificationEntry) obj;
        return this.callAccountName.equals(local.callAccountName) && this.date.equals(local.date);
    }

    @Override
    public int hashCode() {
        return this.callAccountName.hashCode();
    }

    public void setTradeId(final long tradeId) {
        this.tradeId = tradeId;

    }

    public long getTradeId() {
        return this.tradeId;
    }

    public void setCallAccountId(int callAccountId) {
        this.callAccountId = callAccountId;
    }

    public int getCallAccountId() {
        return this.callAccountId;
    }

    public void setContractId(int contractId) {
        this.contractId = contractId;
    }

    public int getContractId() {
        return this.contractId;

    }

    public void setAdHoc(boolean adHoc) {
        this.adHoc = adHoc;
    }

    public boolean getAdHoc() {
        return this.adHoc;
    }

    public void setPoName(String poName) {
        this.poName = poName;
    }

    public String getPoName() {
        return this.poName;
    }

    public String getWatchInterest() {
        return this.watchInterest;
    }

    public void setWatchInterest(String watchInterest) {
        this.watchInterest = watchInterest;
    }

    public LegalEntity getCounterparty() {
        return counterparty;
    }

    public void setCounterparty(LegalEntity counterparty) {
        this.counterparty = counterparty;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }
}
