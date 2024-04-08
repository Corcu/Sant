package calypsox.tk.report;

import com.calypso.tk.collateral.service.CollateralServiceRegistry;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.refdata.CollateralConfig;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

public class SantMTMAuditItem implements Serializable {

    private static final long serialVersionUID = 123L;

    public SantMTMAuditItem() {

    }

    public static final String SANT_MTM_AUDIT_ITEM = "SantMTMAuditItem";

    public static String MARK_NAAME = "Mark Name: ";
    public static String MARK_VALUE = "Original Mark Value: ";
    public static String MARK_CURRENCY = "Currency: ";
    public static String MARK_ADJ_TYPE = "Adjustment Type: ";

    private CollateralConfig marginCallConfig;

    private String changeReason;
    private String mtmValDate;
    private String userChanged;
    private String modifDate;

    private String markCcy;
    private String markName;

    private String oldAuditLine;
    private String newAuditLine;
    private Trade trade;

    private PLMark plMark;

    private Double fxRate1;
    private Double fxRate2;

    public Double getRate1() {
        return this.fxRate1;
    }

    public void setRate1(Double rate1) {
        this.fxRate1 = rate1;
    }

    public Double getRate2() {
        return this.fxRate2;
    }

    public void setRate2(Double rate2) {
        this.fxRate2 = rate2;
    }

    public PLMark getPlMark() {
        return this.plMark;
    }

    public void setPlMark(final PLMark plMark) {
        this.plMark = plMark;
    }

    public String getMarkCcy() {
        if (Util.isEmpty(this.markCcy)) {
            // The below method sets markCcy value
            parseMarkAuditFields(getNewAuditLine(), false);
        }
        return this.markCcy;
    }

    public String getMarkName() {
        if (Util.isEmpty(this.markName)) {
            // The below method sets markCcy value
            parseMarkAuditFields(getNewAuditLine(), false);
        }
        return this.markName;
    }

    /**
     * Parses the audit line Old/New and return mark value String
     *
     * @param auditLine
     * @param isNewAuditLine
     * @return
     */
    public String parseMarkAuditFields(final String auditLine, final boolean isNewAuditLine) {
        String markValue = "";

        // String
        // auditLine="--- PLMarkValue ---\nMark ID: 10631\nMark Name: NPV\n" +
        // "Original Mark Value: 12.0\n" +
        // "Adjustment Value: 0.0\n" +
        // "Currency: EUR\n" +
        // "Display Class Name: com.calypso.tk.core.Amount\n" +
        // "Display Digits: 0\nIs Adjusted: false\n" +
        // "Adjustment Type: Actualizando con MtM Valuations\n" +
        // "Adjustment Comment: null\n";

        final String[] auditEntries = auditLine.split("\n");
        for (final String auditEntry : auditEntries) {

            if (auditEntry.startsWith(MARK_VALUE)) {
                markValue = auditEntry.substring(MARK_VALUE.length());
            }

            if (auditEntry.startsWith(MARK_CURRENCY)) {
                this.markCcy = auditEntry.substring(MARK_CURRENCY.length());
            }

            if (auditEntry.startsWith(MARK_NAAME)) {
                this.markName = auditEntry.substring(MARK_NAAME.length());
            }

            if (isNewAuditLine) {
                if (auditEntry.startsWith(MARK_ADJ_TYPE)) {
                    this.changeReason = auditEntry.substring(MARK_ADJ_TYPE.length());
                }
            }

        }

        return markValue;

    }

    public String getModifDate() {
        return this.modifDate;
    }

    public void setModifDate(final String modifDate) {
        this.modifDate = modifDate;
    }

    public String getUserChanged() {
        return this.userChanged;
    }

    public void setUserChanged(final String userChanged) {
        this.userChanged = userChanged;
    }

    public String getOldAuditLine() {
        return this.oldAuditLine;
    }

    public void setOldAuditLine(final String oldAuditLine) {
        this.oldAuditLine = oldAuditLine;
    }

    public String getNewAuditLine() {
        return this.newAuditLine;
    }

    public void setNewAuditLine(final String newAuditLine) {
        this.newAuditLine = newAuditLine;
    }

    public Trade getTrade() {
        return this.trade;
    }

    public void setTrade(final Trade trade) {
        this.trade = trade;
    }

    public JDate getTradeDate() {
        if (this.trade == null) {
            return null;
        }

        if (this.trade.getProductType().equals(Product.SEC_LENDING)) {
            String kw = this.trade.getKeywordValue("REAL_TRADE_DATE");
            if (!Util.isEmpty(kw)) {
                return JDate.valueOf(kw);
            }
        } else {
            return this.trade.getTradeDate().getJDate(TimeZone.getDefault());
        }
        return null;
    }

    public CollateralConfig getMarginCallConfig() {
        return this.marginCallConfig;
    }

    public void setMarginCallConfig(final CollateralConfig marginCallConfig) {
        this.marginCallConfig = marginCallConfig;
    }

    public String getChangeReason() {
        // if CHANGE_REASON is empty then extract it
        if (Util.isEmpty(this.changeReason)) {
            parseMarkAuditFields(getNewAuditLine(), true);
        }
        return this.changeReason;
    }

    public void setChangeReason(final String changeReason) {
        this.changeReason = changeReason;
    }

    public String getMtmValDate() {
        return this.mtmValDate;
    }

    public void setMtmValDate(final String mtmValDate) {
        this.mtmValDate = mtmValDate;
    }

    public Double getOldValue() {
        final String valStr = parseMarkAuditFields(getOldAuditLine(), false);
        try {
            return Double.valueOf(valStr);
        } catch (final Exception exc) {
            Log.error(this, exc);//Sonar
        }
        return null;
    }

    public Double getNewValue() {
        final String valStr = parseMarkAuditFields(getNewAuditLine(), true);
        try {
            return Double.valueOf(valStr);
        } catch (final Exception exc) {
            Log.error(this, exc);//Sonar
        }
        return null;

    }

    public static String getSantMtmAuditItem() {
        return SANT_MTM_AUDIT_ITEM;
    }

    public Double getMTMDiff() {
        final Double newVal = getNewValue();
        final Double oldVal = getOldValue();
        if ((newVal != null) && (oldVal != null)) {
            return newVal - oldVal;
        }

        return null;
    }

    public void buildFXRates(List<QuoteValue> quoteValues) {
        buildPrincipalCurrencies(getTrade().getProduct());

        // GSM: 11/06/2013. Fix to avoid crash if the contract is null
        if (this.marginCallConfig == null) {
            amendContractFromInternalId(); // try to recover from internal id
            if (this.marginCallConfig == null) {
                amendContractMatchTrade(); // if not try to find the contract of the trade
                if (this.marginCallConfig == null) {
                    return; // wasn't recovered, just ignored this data
                }
            }
        }

        QuoteValue qv = null;
        if (this.marginCallConfig.getCurrency().equals(this.principalCcy)) {
            this.fxRate1 = 1.0d;
        } else {
            qv = fetchFXQuoteValue(quoteValues, this.marginCallConfig.getCurrency(), this.principalCcy);
            if (qv != null) {
                this.fxRate1 = qv.getClose();
            }
        }

        if (Util.isEmpty(this.principal2Ccy)) {
            return;
        }
        qv = null;
        if (this.marginCallConfig.getCurrency().equals(this.principal2Ccy)) {
            this.fxRate2 = 1.0d;
        } else {
            qv = fetchFXQuoteValue(quoteValues, this.marginCallConfig.getCurrency(), this.principal2Ccy);
            if (qv != null) {
                this.fxRate2 = qv.getClose();
            }
        }
    }

    /**
     * If contract id keyword and internal reference fails, gather the contract from the trade.
     */
    // GSM: 11/06/2013. Fix to avoid crash if the contract is null
    private void amendContractMatchTrade() {

        if ((this.trade == null) || (this.trade.getInternalReference() == null)) {
            return;
        }

        final CollateralServiceRegistry srvReg = ServiceRegistry.getDefault();

        try {
            final CollateralConfig contract = srvReg.getCollateralDataServer().getMarginCallConfig(this.trade);

            if (contract != null) {
                setMarginCallConfig(contract);
            }

        } catch (RemoteException e) {
            Log.error(this, e);//Sonar
            return;
        }

    }

    /**
     * There is a strange error where, somehow, a trade does not have the contract ID as a keyword. This tries to amend
     * it, so tries to read the contract from the internal id.
     */
    // GSM: 11/06/2013. Fix to avoid crash if the contract is null
    private void amendContractFromInternalId() {

        if ((this.trade == null) || (this.trade.getInternalReference() == null)) {
            return;
        }

        int contractId = -1;
        try {
            contractId = Integer.valueOf(this.trade.getInternalReference());

        } catch (NumberFormatException e) {
            return;
        }

        final CollateralServiceRegistry srvReg = ServiceRegistry.getDefault();

        try {
            final CollateralConfig contract = srvReg.getCollateralDataServer().getMarginCallConfig(contractId);

            if (contract != null) {
                setMarginCallConfig(contract);
            }

        } catch (RemoteException e) {
            Log.error(this, e);//Sonar
            return;
        }
    }

    private void buildPrincipalCurrencies(Product product) {
        if (product instanceof CollateralExposure) {
            if (is2Legs(((CollateralExposure) product).getUnderlyingType())) {
                CollateralExposure collatExpo = (CollateralExposure) product;
                this.principalCcy = (String) collatExpo.getAttribute("CCY_1");
                this.principal2Ccy = (String) collatExpo.getAttribute("CCY_2");
            } else {
                this.principalCcy = product.getCurrency();
            }
        } else if (product instanceof PerformanceSwap) {
            PerformanceSwap brs = (PerformanceSwap) product;
            this.principalCcy = brs.getPrimaryLeg().getCurrency();
            this.principal2Ccy = brs.getSecondaryLeg().getCurrency();
        } else {
            this.principalCcy = product.getCurrency();
        }
    }

    public QuoteValue fetchQuoteValue(List<QuoteValue> quoteValues, String quoteName) {
        for (final QuoteValue qv : quoteValues) {
            if (qv.getName().equals(quoteName)) {
                return qv;
            }
        }
        return null;
    }

    public QuoteValue fetchFXQuoteValue(List<QuoteValue> quoteValues, String ccy1, String ccy2) {
        String fxQuoteName = "FX." + ccy1 + "." + ccy2;

        QuoteValue qv = fetchQuoteValue(quoteValues, fxQuoteName);
        if (qv == null) {
            fxQuoteName = "FX." + ccy2 + "." + ccy1;
            qv = fetchQuoteValue(quoteValues, fxQuoteName);
        }
        return qv;

    }

    private String principalCcy;
    private String principal2Ccy;

    public static boolean is2Legs(final String underlying) {
        return twoLegsUnderLyings.contains(underlying);
    }

    public static String[] twoLegs = {"CASH_FLOW_MATCHING", "INTEREST_RATE_SWAP", "FX_SWAP_NON_DELIVERABLE",
            "FX_SWAP_DELIVERABLE", "FX_NON_DELIVERABLE_FORWARD", "FX_DELIVERABLE_SPOT", "FX_DELIVERABLE_FORWARD",
            "EQUITY_SWAP", "CURRENCY_SWAP", "BASIS_SWAP"};

    public static List<String> twoLegsUnderLyings = Arrays.asList(twoLegs);

}
