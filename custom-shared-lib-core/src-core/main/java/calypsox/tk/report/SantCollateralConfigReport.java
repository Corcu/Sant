/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.util.CheckRowsNumberReport;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.ELBEandKGRutilities;
import calypsox.util.SantReportingUtil;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.CreditRating;
import com.calypso.tk.marketdata.MarginCallCreditRatingConfiguration;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.AccountInterestConfig;
import com.calypso.tk.refdata.AccountInterestConfigRange;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.CollateralConfigCurrency;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.*;

/**
 * @author aela
 */
public class SantCollateralConfigReport extends CollateralConfigReport implements CheckRowsNumberReport {

    private static final long serialVersionUID = 1L;

    private static final String OWNER_NAME = "Owner Name";
    private static final String LE_FULL_NAME = "LE full name";
    private static final String ADDITIONAL_LE = "Additional Legal Entities";
    private static final String MTA_CPTY = "MTA Cpty";
    private static final String THRESHOLD_CPTY = "Threshold Cpty";
    private static final String LE_MTA_AMOUNT = "LE MTA Amount";
    private static final String LE_THRESHOLD_AMOUNT = "LE Threshold Amount";
    private static final String MTA_OWNER = "MTA Owner";
    private static final String PO_MTA_AMOUNT = "PO MTA Amount";
    private static final String THRESHOLD_OWNER = "Threshold Owner";
    private static final String PO_THRESHOLD_AMOUNT = "PO Threshold Amount";
    private static final String INITIAL_MARGIN = "Initial Margin";
    private static final String CALC_PERIOD = "Calc Period";
    private static final String ASSET_TYPE = "Asset Type";
    private static final String ONE_WAY = "One Way";
    private static final String HEAD_CLONE = "Head Clone";
    private static final String MASTER_SIGNED_DATE = "Master Signed Date";
    private static final String INDEPENDENT_AMOUNT_OWNER = "Independent Amount Owner";
    private static final String INDEPENDENT_AMOUNT_CPTY = "Independent Amount Cpty";
    private static final String FREQUENCY = "FREQUENCY";
    private static final String MA_SIGN_DATE = "MA_SIGN_DATE";
    private static final String NONE = "None";
    private static final String LEGAL_ENTITY = "Legal Entity";
    private static final String OWNER = "Owner";
    private static final String COUNTERPARTY = "Counterparty";
    private static final String IMIRISMapping = "IMIRISMapping";
    private static final String GUARANTEE_TYPE = "GUARANTEE_TYPE";
    private static final String GLOBAL_RATING = "GLOBAL RATING";
    private static final String PO_MTA_CURRENCY = "PO MTA Currency";
    private static final String LE_THRESHOLD_CURRENCY = "LE Threshold Currency";
    private static final String PO_THRESHOLD_CURRENCY = "PO Threshold Currency";
    private static final String LE_MTA_CURRENCY = "LE MTA Currency";
    private static final String PO_ELIGIBLE_CCY = "PO Eligible Ccy";
    private static final String LE_ELIGIBLE_CCY = "LE Eligible Ccy";
    private static final String MARGIN_CALL_CONTRACT = "MARGIN_CALL_CONTRACT";
    private static final String PROCESS_DATE = "ProcessDate";
    private static final String ACC_INTEREST = "AccInterest";
    private static final String INTEREST = "Interest";
    private static final String CURRENCIES = "Currencies";

    private final String CASH = "CASH";
    private final String SECURITY = "SECURITY";
    private final String BOTH = "BOTH";

    private boolean lastAllocationCurrency = true;

    @SuppressWarnings("deprecation")
    private final JDate valueDate = this._valuationDateTime.getJDate();




    @SuppressWarnings("rawtypes")
    @Override
    public ReportOutput load(Vector errors) {
        DefaultReportOutput output = new DefaultReportOutput(this);
        // get the last used currency for this contract.
        Map<Integer, String> lastCurrenciesPerContract = new HashMap<Integer, String>();
        try {
            lastCurrenciesPerContract.putAll(SantReportingUtil.getSantReportingService(DSConnection.getDefault())
                    .getLastUsedCurrencyPerContract());
        } catch (RemoteException e) {
            Log.error(this, e.getMessage() + "\n" + e); //sonar
        }
        // set the report template
        SantMCConfigReportTemplate customTempalte = (SantMCConfigReportTemplate) getReportTemplate();
        setReportTemplate(buildReportTemplate(customTempalte));
        // load the report results
        DefaultReportOutput ro = (DefaultReportOutput) super.load(errors);
        final List<ReportRow> rows = new ArrayList<ReportRow>();
        // filter on POs
        // GSM 20/07/15. SBNA Multi-PO filter
        String pos = CollateralUtilities.filterPoIdsByTemplate(getReportTemplate());
        // (String)
        // customTempalte.get(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);
        ArrayList<Integer> posToKeep = new ArrayList<Integer>();
        if (!Util.isEmpty(pos)) {
            List<String> poIdsAsString = Arrays.asList(pos.split(","));
            if (!Util.isEmpty(poIdsAsString)) {
                for (String stringPoId : poIdsAsString) {
                    posToKeep.add(Integer.parseInt(stringPoId));
                }
            }
        }

        for (final ReportRow row : ro.getRows()) {
            CollateralConfig cc = (CollateralConfig) row.getProperty(ReportRow.DEFAULT);
            String lastUsedCcy = null;
            if (cc != null) {
                lastUsedCcy = lastCurrenciesPerContract.get(cc.getId());
                row.setProperty("LastAllocationCurrency", lastUsedCcy);
                row.setProperty(PROCESS_DATE, getValDate());
                row.setProperty(SantCollateralConfigReportStyle.MARGIN_TYPE, getCollateralMarginType(cc));

                addPropertiesToRow(row, cc);
            }

            /*
             * condition is added to check if the filter wants to be applied or
             * not. True: applies, False: does not apply Eloy
             */
            if (!Util.isEmpty(lastUsedCcy) || !(this.lastAllocationCurrency)) {
                if (Util.isEmpty(posToKeep) || posToKeep.contains(cc.getProcessingOrg().getId())) {
                    rows.add(row);
                }
            }
        }
        output.setRows(rows.toArray(new ReportRow[rows.size()]));

        //Generate task if report size is out of a defined umbral
        HashMap<String, String> value = SantReportingUtil.getSchedTaskNameOrReportTemplate(this);
        if (!value.isEmpty() && value.keySet().iterator().next().equals("ScheduledTask: ")){
            checkAndGenerateTaskReport(output, value);
        }
        return output;
    }

    /**
     * @param customTempalte
     * @return
     */
    private CollateralConfigReportTemplate buildReportTemplate(SantMCConfigReportTemplate customTempalte) {

        String agrType = (String) customTempalte.get(SantGenericTradeReportTemplate.AGREEMENT_TYPE);
        if (Util.isEmpty(agrType)) {
            agrType = "ALL";
        }
        String counterparty = (String) customTempalte.get(SantGenericTradeReportTemplate.COUNTERPARTY);
        if (Util.isEmpty(counterparty)) {
            counterparty = "ALL";
        } else {
            try {
                counterparty = getMultipleLEs(counterparty);
            } catch (Exception e) {
                counterparty = "ALL";
                Log.info(this, e); //sonar
            }
        }
        String po = (String) customTempalte.get(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);
        if (Util.isEmpty(po)) {
            po = "ALL";
        } else {
            try {
                po = getMultipleLEs(po);
            } catch (Exception e) {
                po = "ALL";
                Log.info(this, e); //sonar
            }

        }
        JDate processStartDate = getDate(customTempalte, JDate.getNow(), TradeReportTemplate.START_DATE,
                TradeReportTemplate.START_PLUS, TradeReportTemplate.START_TENOR);

        if (processStartDate == null) {
            processStartDate = JDate.getNow();
        }

        /*
         * condition is added to check if the value is a null in the Schedule
         * already created, taken as True. So it will continue past the filter
         * previously passed Eloy
         */
        if (customTempalte.get(SantGenericTradeReportTemplate.LAST_ALLOCATION_CURRENCY) != null
                && !((Boolean) customTempalte.get(SantGenericTradeReportTemplate.LAST_ALLOCATION_CURRENCY))) {
            this.lastAllocationCurrency = false;
        }

        customTempalte.put(SantMCConfigReportTemplate.EXTRACTION_DATE, processStartDate);
        customTempalte.put(CollateralConfigReportTemplate.PROCESSING_ORG, po);
        customTempalte.put(CollateralConfigReportTemplate.ROLE, "ALL");
        customTempalte.put(CollateralConfigReportTemplate.LEGAL_ENTITY, counterparty);
        customTempalte.put(CollateralConfigReportTemplate.CONTRACT_TYPE, agrType);
        customTempalte.put(CollateralConfigReportTemplate.STATUS, "OPEN");
        customTempalte.put(CollateralConfigReportTemplate.DISCOUNT_CURRENCY, "ALL");

        return customTempalte;

    }

    /**
     * Eloy
     *
     * @param legalEntities legal entities ids to search in String
     * @return the String with legal entities codes
     */
    private String getMultipleLEs(String legalEntities) {
        String valuesToReturn = "";
        List<String> idsAsString = Arrays.asList(legalEntities.split(","));
        for (String id : idsAsString) {
            LegalEntity le = BOCache.getLegalEntity(DSConnection.getDefault(), Integer.parseInt(id));
            if (le != null) {
                if (Util.isEmpty(valuesToReturn)) {
                    valuesToReturn = le.getCode();
                } else {
                    valuesToReturn = valuesToReturn + "," + le.getCode();
                }
            }
        }
        if (Util.isEmpty(valuesToReturn)) {
            valuesToReturn = "ALL";
        }
        return valuesToReturn;
    }

    private void addPropertiesToRow(ReportRow row, CollateralConfig colConfig) {
        row.setProperty(OWNER_NAME, getOwnerName(colConfig));
        row.setProperty(LE_FULL_NAME, getLeFullName(colConfig));
        row.setProperty(ADDITIONAL_LE, getAdditionalLE(colConfig));
        row.setProperty(MTA_CPTY, getMtaCpty(colConfig));
        row.setProperty(LE_MTA_AMOUNT, getMtaCpty(colConfig));
        row.setProperty(LE_THRESHOLD_AMOUNT, getThresholdCpty(colConfig));
        row.setProperty(THRESHOLD_CPTY, getThresholdCpty(colConfig));
        row.setProperty(PO_MTA_AMOUNT, getMtaOwner(colConfig));
        row.setProperty(MTA_OWNER, getMtaOwner(colConfig));
        row.setProperty(PO_THRESHOLD_AMOUNT, getThresholdOwner(colConfig));
        row.setProperty(THRESHOLD_OWNER, getThresholdOwner(colConfig));
        row.setProperty(INITIAL_MARGIN, getInitialMargin(colConfig));
        row.setProperty(CALC_PERIOD, getCalcPeriod(colConfig));
        row.setProperty(ASSET_TYPE, getAssetType(colConfig));
        row.setProperty(ONE_WAY, getOneWay(colConfig));
        row.setProperty(HEAD_CLONE, getHeadClone(colConfig));
        row.setProperty(MASTER_SIGNED_DATE, getMasterSignedDate(colConfig));
        row.setProperty(INDEPENDENT_AMOUNT_OWNER, getIndependentAmountOwner(colConfig));
        row.setProperty(INDEPENDENT_AMOUNT_CPTY, getIndependentAmountCpty(colConfig));
        row.setProperty(LE_THRESHOLD_CURRENCY, getLeNewThresholdCurrency(colConfig));
        row.setProperty(PO_THRESHOLD_CURRENCY, getLeNewThresholdCurrencyPO(colConfig));
        row.setProperty(LE_MTA_CURRENCY, getCptyMTACurrency(colConfig));
        row.setProperty(PO_MTA_CURRENCY, getPoMTACurrency(colConfig));
        row.setProperty(LE_ELIGIBLE_CCY, getLeEligibleCcy(colConfig));
        row.setProperty(CURRENCIES, getParameterCurrencies(colConfig));
        setCallAccountRanges(colConfig, row);
    }

    private List<CollateralConfigCurrency> getLeEligibleCcy(CollateralConfig colConfig) {
        return colConfig.getLeEligibleCurrencies();
    }

    private void setCallAccountRanges(CollateralConfig colConfig, ReportRow row) {
        List<Account> accounts = BOCache.getAccountByAttribute(DSConnection.getDefault(), MARGIN_CALL_CONTRACT,
                String.valueOf(colConfig.getId()));
        JDate processDate = row.getProperty(PROCESS_DATE);
        List<CollateralConfigCurrency> poEligibleCurrencies = colConfig.getPoEligibleCurrencies();
        row.setProperty(PO_ELIGIBLE_CCY, poEligibleCurrencies);
        int count = 1;
        for (CollateralConfigCurrency ccy : poEligibleCurrencies) {
            for (Account account : accounts) {
                if (ccy.getCurrency().equals(account.getCurrency())) {
                    final int interestId = account.getAccountInterestConfigId(processDate, INTEREST, false);
                    AccountInterestConfig interestConfig = BOCache.getAccountInterestConfig(DSConnection.getDefault(), interestId);
                    if (interestConfig != null) {
                        AccountInterestConfigRange range = interestConfig.getRange(processDate, 1.0, ccy.getCurrency());
                        row.setProperty(ACC_INTEREST + count, range);
                        count++;
                        break;
                    }
                }
            }
        }
    }

    private String getCptyMTACurrency(CollateralConfig colConfig) {
        MarginCallCreditRatingConfiguration mccRatingConfigCpty = getmccRatingConfigCpty(colConfig);
        if (ELBEandKGRutilities.isCptyMTADependingOn(colConfig, GLOBAL_RATING)) {
            return null != mccRatingConfigCpty ? mccRatingConfigCpty.getMtaCurrency() : "";
        } else
            return colConfig.getLeMTACurrency();
    }

    private String getPoMTACurrency(CollateralConfig colConfig) {
        MarginCallCreditRatingConfiguration mccRatingConfigOwner = getmccRatingOwner(colConfig);
        if (ELBEandKGRutilities.isMTADependingOn(colConfig, GLOBAL_RATING)) {
            return null != mccRatingConfigOwner ? mccRatingConfigOwner.getMtaCurrency() : "";
        } else
            return colConfig.getPoMTACurrency();
    }

    private String getLeNewThresholdCurrency(CollateralConfig colConfig) {
        MarginCallCreditRatingConfiguration mccRatingConfigCpty = getmccRatingConfigCpty(colConfig);
        if (ELBEandKGRutilities.isCptyThresholdDependingOn(colConfig, GLOBAL_RATING)) {
            return null != mccRatingConfigCpty ? mccRatingConfigCpty.getMtaCurrency() : "";
        } else
            return colConfig.getLeNewThresholdCurrency();
    }

    private String getLeNewThresholdCurrencyPO(CollateralConfig colConfig) {
        MarginCallCreditRatingConfiguration mccRatingConfigOwner = getmccRatingOwner(colConfig);
        if (ELBEandKGRutilities.isThresholdDependingOn(colConfig, GLOBAL_RATING)) {
            return null != mccRatingConfigOwner ? mccRatingConfigOwner.getMtaCurrency() : "";
        } else
            return colConfig.getPoNewThresholdCurrency();
    }
    private String getOwnerName(final CollateralConfig colConfig) {
        String ownerName = "";
        LegalEntity processingOrg = null;
        try {
            processingOrg = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(colConfig.getPoId());
        } catch (CalypsoServiceException e) {
            Log.error(this, e);
        }
        if (processingOrg != null) {
            ownerName = processingOrg.getName();
        }
        return ownerName;
    }

    private String getLeFullName(final CollateralConfig colConfig) {
        LegalEntity leEntity = colConfig.getLegalEntity();
        if (leEntity != null) {
            return leEntity.getName();
        }
        return "";
    }

    private String getAdditionalLE(final CollateralConfig colConfig) {
        List<LegalEntity> additionalLE = colConfig.getAdditionalLE();
        return Util.arrayToString(additionalLE);
    }

    private Amount getThresholdCpty(final CollateralConfig colConfig) {
        double thresholdCpty = 0.0D;
        MarginCallCreditRatingConfiguration mccRatingConfigCpty = getmccRatingConfigCpty(colConfig);
        if (mccRatingConfigCpty != null) {
            Vector<String> agencies = colConfig.getEligibleAgencies();
            Vector<CreditRating> cptyCreditRatings = ELBEandKGRutilities.getCreditRatingsForLE(colConfig, agencies,
                    colConfig.getLegalEntity().getId(), valueDate, mccRatingConfigCpty.getRatingType());
            thresholdCpty = KGR_Collateral_MarginCallLogic.getCptyThresholdCcy(colConfig, cptyCreditRatings, valueDate, valueDate);
        }
        else
            thresholdCpty = colConfig.getLeNewThresholdAmount();
        return new Amount(thresholdCpty);
    }
    private Amount getMtaCpty(final CollateralConfig colConfig) {
        double mtaCpty = 0.0D;
        MarginCallCreditRatingConfiguration mccRatingConfigCpty = getmccRatingConfigCpty(colConfig);
        if (mccRatingConfigCpty != null) {
            Vector<String> agencies = colConfig.getEligibleAgencies();
            Vector<CreditRating> cptyCreditRatings = ELBEandKGRutilities.getCreditRatingsForLE(colConfig, agencies,
                    colConfig.getLegalEntity().getId(), valueDate, mccRatingConfigCpty.getRatingType());
            mtaCpty =  KGR_Collateral_MarginCallLogic.getCptyMtaCcy(colConfig, cptyCreditRatings, valueDate, valueDate);
        }
        else
            mtaCpty = colConfig.getLeMTAAmount();
        return new Amount(mtaCpty);
    }



    private Amount getMtaOwner(final CollateralConfig colConfig) {
        double mtaOwner = 0.0D;
        MarginCallCreditRatingConfiguration mccRatingConfigOwner = getmccRatingOwner(colConfig);
        if (mccRatingConfigOwner != null) {
            Vector<String> agencies = colConfig.getEligibleAgencies();
            Vector<CreditRating> ownerCreditRatings = ELBEandKGRutilities.getCreditRatingsForLE(colConfig, agencies,
                    colConfig.getProcessingOrg().getId(), valueDate, mccRatingConfigOwner.getRatingType());
            mtaOwner = KGR_Collateral_MarginCallLogic.getOwnerMtaCcy(colConfig, ownerCreditRatings, valueDate, valueDate);
        }
        else
            mtaOwner = colConfig.getPoMTAAmount();
        return new Amount(mtaOwner);
    }

    private Amount getThresholdOwner(final CollateralConfig colConfig) {
        double thresholdOwner = 0.0D;
        MarginCallCreditRatingConfiguration mccRatingConfigOwner = getmccRatingOwner(colConfig);
        if (mccRatingConfigOwner != null) {
            Vector<String> agencies = colConfig.getEligibleAgencies();
            Vector<CreditRating> ownerCreditRatings = ELBEandKGRutilities.getCreditRatingsForLE(colConfig, agencies,
                    colConfig.getProcessingOrg().getId(), getValDate(), mccRatingConfigOwner.getRatingType());
            thresholdOwner = KGR_Collateral_MarginCallLogic.getOwnerThresholdCcy(colConfig, ownerCreditRatings, valueDate, valueDate);
        }
        else
            thresholdOwner = colConfig.getPoNewThresholdAmount();
        return new Amount(thresholdOwner);
    }

    private Amount getInitialMargin(final CollateralConfig colConfig) {
        return new Amount(0.0D);
    }

    private String getCalcPeriod(final CollateralConfig colConfig) {
        return colConfig.getAdditionalField(FREQUENCY);
    }

    private String getAssetType(final CollateralConfig colConfig) {
        // Retrieve the collateral type for the counterparty and processing org.
        final String leCollType = colConfig.getLeCollType();
        final String poCollType = colConfig.getPoCollType();

        if (CASH.equalsIgnoreCase(leCollType) && CASH.equalsIgnoreCase(poCollType)) {
            return CASH;
        } else if (SECURITY.equalsIgnoreCase(leCollType) && SECURITY.equalsIgnoreCase(poCollType)) {
            return SECURITY;
        } else {
            return BOTH;
        }
    }

    private String getOneWay(final CollateralConfig colConfig) {
        if (CollateralConfig.NET_BILATERAL.equalsIgnoreCase(colConfig.getContractDirection())) {
            return NONE;
        } else {
            if (LEGAL_ENTITY.equals(colConfig.getSecuredParty())) {
                return COUNTERPARTY;
            } else {
                return OWNER;
            }
        }
    }

    private String getHeadClone(final CollateralConfig marginCall) {
        return marginCall.getAdditionalField(HEAD_CLONE);
    }

    private String getMasterSignedDate(final CollateralConfig colConfig) {
        return colConfig.getAdditionalField(MA_SIGN_DATE);
    }

    private Amount getIndependentAmountOwner(final CollateralConfig colConfig) {
        double independentAmount = 0.0D;
        MarginCallCreditRatingConfiguration mccRatingConfigOwner = getmccRatingOwner(colConfig);
        if (mccRatingConfigOwner != null) {
            Vector<String> agencies = colConfig.getEligibleAgencies();
            Vector<CreditRating> ownerCreditRatings = ELBEandKGRutilities.getCreditRatingsForLE(colConfig, agencies,
                    colConfig.getProcessingOrg().getId(), getValDate(), mccRatingConfigOwner.getRatingType());
            independentAmount = KGR_Collateral_MarginCallLogic.getOwnerIndAmountCcy(colConfig, ownerCreditRatings);
        }
        return new Amount(independentAmount);
    }

    private Amount getIndependentAmountCpty(final CollateralConfig colConfig) {
        double independentAmount = 0.0D;
        MarginCallCreditRatingConfiguration mccRatingConfigCpty = getmccRatingConfigCpty(colConfig);
        if (mccRatingConfigCpty != null) {
            Vector<String> agencies = colConfig.getEligibleAgencies();
            Vector<CreditRating> cptyCreditRatings = ELBEandKGRutilities.getCreditRatingsForLE(colConfig, agencies,
                    colConfig.getLegalEntity().getId(), getValDate(), mccRatingConfigCpty.getRatingType());
            independentAmount = KGR_Collateral_MarginCallLogic.getOwnerIndAmountCcy(colConfig, cptyCreditRatings);
        }
        return new Amount(independentAmount);
    }

    private MarginCallCreditRatingConfiguration getmccRatingOwner(final CollateralConfig colConfig) {
        MarginCallCreditRatingConfiguration mccRatingConfigOwner = null;

        try {
            mccRatingConfigOwner = CollateralUtilities.getMCRatingConfiguration(colConfig.getPoRatingsConfigId());
        } catch (Exception e) {
            Log.error(this, e);
        }
        return mccRatingConfigOwner;
    }

    private MarginCallCreditRatingConfiguration getmccRatingConfigCpty(final CollateralConfig colConfig) {
        MarginCallCreditRatingConfiguration mccRatingConfigCpty = null;
        try {
            mccRatingConfigCpty = CollateralUtilities.getMCRatingConfiguration(colConfig.getLeRatingsConfigId());
        } catch (Exception e) {
            Log.error(this, e);
        }
        return mccRatingConfigCpty;
    }

    /**
     * Method getCollateralMarginType, return the guarantee type
     *
     * @param marginCall
     * @return mapped value from domain value IMIRISMapping
     */
    private String getCollateralMarginType(final CollateralConfig marginCall) {
        if (marginCall != null) {
            //maps the domain value values with their comments
            Map<String, String> map = CollateralUtilities.initDomainValueComments(IMIRISMapping);
            //gets the additional field from the contract
            String field = marginCall.getAdditionalField(GUARANTEE_TYPE);
            //checks if it is not empty
            if (!Util.isEmpty(field)) {
                //get the comment related to the value
                String comment = map.get(field);
                //returns it, if it is not empty
                if (!Util.isEmpty(comment)) return comment;
            }
        }
        //any other cases returns blank
        return "";
    }

    private Vector getParameterCurrencies(CollateralConfig cc){
        return cc.getCurrencyList();
    }
}
