/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import com.calypso.tk.core.PricerMeasure;
import com.calypso.tk.core.Util;

import java.io.Serializable;
import java.util.List;

public class KPIMtmIndividualItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String agrOwner;
    private String dealOwner;

    protected long tradeId;
    protected int mcEntryId;
    private String economicSector;
    private String instrument;
    private String portfolio;

    private String mtmCurrency;
    @SuppressWarnings("unused")
    private double mtmValue;
    protected List<PricerMeasure> pricerMeasures;

    private int agreementId;
    private String agreementName;

    public int getAgreementId() {
        return this.agreementId;
    }

    public void setAgreementId(int agreementId) {
        this.agreementId = agreementId;
    }

    public String getAgreementName() {
        return this.agreementName;
    }

    public void setAgreementName(String agreementName) {
        this.agreementName = agreementName;
    }

    public List<PricerMeasure> getPricerMeasures() {
        return this.pricerMeasures;
    }

    public void setPricerMeasures(List<PricerMeasure> pricerMeasures) {
        this.pricerMeasures = pricerMeasures;
    }

    public String getAgrOwner() {
        return this.agrOwner;
    }

    public void setAgrOwner(final String agrOwner) {
        this.agrOwner = agrOwner;
    }

    public String getDealOwner() {
        return this.dealOwner;
    }

    public void setDealOwner(final String dealOwner) {
        this.dealOwner = dealOwner;
    }

    public long getTradeId() {
        return this.tradeId;
    }

    public void setTradeId(final int trade_id) {
        this.tradeId = trade_id;
    }

    public int getMcEntryId() {
        return this.mcEntryId;
    }

    public void setMcEntryId(final int entId) {
        this.mcEntryId = entId;
    }

    public String getEconomicSector() {
        return this.economicSector;
    }

    public void setEconomicSector(final String economicSector) {
        this.economicSector = economicSector;
    }

    public String getInstrument() {
        return this.instrument;
    }

    public void setInstrument(final String instrument) {
        this.instrument = instrument;
    }

    public String getPortfolio() {
        return this.portfolio;
    }

    public void setPortfolio(final String portfolio) {
        this.portfolio = portfolio;
    }

    public void setMtmCurrency(final String mtmCurrency) {
        this.mtmCurrency = mtmCurrency;
    }

    public String getMtmCurrency() {
        return this.mtmCurrency;
    }

    public Double getMtmValue() {
        PricerMeasure pricerMeasure = getPricerMeasure(33);
        if (pricerMeasure != null) {
            return pricerMeasure.getValue();
        }
        return null;
    }

    public PricerMeasure getPricerMeasure(int type) {
        if (!Util.isEmpty(this.pricerMeasures)) {
            for (PricerMeasure measure : this.pricerMeasures) {
                if (measure.getType() == type) {
                    return measure;
                }
            }
        }
        return null;
    }

}
