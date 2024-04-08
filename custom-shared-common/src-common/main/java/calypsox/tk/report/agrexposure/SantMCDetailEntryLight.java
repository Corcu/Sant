/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report.agrexposure;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.PricerMeasure;
import com.calypso.tk.core.Util;

import java.io.Serializable;
import java.util.List;

public class SantMCDetailEntryLight implements Serializable {

    private static final long serialVersionUID = 1L;

    private JDate processDate;
    private JDate valDate;
    private int agrId;
    private int entryId;
    private String agreementName;

    private String agreementType;
    private String agreementCurrency;
    private List<PricerMeasure> pricerMeasures;

    private String instrument;
    private String counterPartyName;
    private String processOrgName;
    private long tradeId;
    private String frontId;
    private String structure;
    private String portfolio;
    private JDate maturityDate;

    public JDate getProcessDate() {
        return this.processDate;
    }

    public void setProcessDate(JDate processDate) {
        this.processDate = processDate;
    }

    public JDate getValDate() {
        return this.valDate;
    }

    public void setValDate(JDate valDate) {
        this.valDate = valDate;
    }

    public int getAgrId() {
        return this.agrId;
    }

    public void setAgrId(int agrId) {
        this.agrId = agrId;
    }

    public int getEntryId() {
        return this.entryId;
    }

    public void setEntryId(int entryId) {
        this.entryId = entryId;
    }

    public String getAgreementName() {
        return this.agreementName;
    }

    public void setAgreementName(String agreementName) {
        this.agreementName = agreementName;
    }

    public String getAgreementType() {
        return this.agreementType;
    }

    public void setAgreementType(String agreementType) {
        this.agreementType = agreementType;
    }

    public String getAgreementCurrency() {
        return this.agreementCurrency;
    }

    public void setAgreementCurrency(String agreementCurrency) {
        this.agreementCurrency = agreementCurrency;
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

    public List<PricerMeasure> getPricerMeasures() {
        return this.pricerMeasures;
    }

    public void setPricerMeasures(List<PricerMeasure> pricerMeasures) {
        this.pricerMeasures = pricerMeasures;
    }

    public String getInstrument() {
        return this.instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    public String getCounterPartyName() {
        return this.counterPartyName;
    }

    public void setCounterPartyName(String counterPartyName) {
        this.counterPartyName = counterPartyName;
    }

    public String getProcessOrgName() {
        return this.processOrgName;
    }

    public void setProcessOrgName(String processOrgName) {
        this.processOrgName = processOrgName;
    }

    public long getTradeId() {
        return this.tradeId;
    }

    public void setTradeId(long tradeId) {
        this.tradeId = tradeId;
    }

    public String getFrontId() {
        return this.frontId;
    }

    public void setFrontId(String frontId) {
        this.frontId = frontId;
    }

    public String getStructure() {
        return this.structure;
    }

    public void setStructure(String structure) {
        this.structure = structure;
    }

    public String getPortfolio() {
        return this.portfolio;
    }

    public void setPortfolio(String portfolio) {
        this.portfolio = portfolio;
    }

    public JDate getMaturityDate() {
        return this.maturityDate;
    }

    public void setMaturityDate(JDate maturityDate) {
        this.maturityDate = maturityDate;
    }

}
