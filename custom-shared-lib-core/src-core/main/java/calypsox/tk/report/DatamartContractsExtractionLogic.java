package calypsox.tk.report;

import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.ELBEandKGRutilities;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.CreditRating;
import com.calypso.tk.marketdata.MarginCallCreditRating;
import com.calypso.tk.product.CollateralExposure;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.CollateralConfigCurrency;

import java.util.List;
import java.util.Vector;

public class DatamartContractsExtractionLogic {

    private static final String SAN = "SAN";
    private static final String CPTY = "CPTY";
    private static final String CONTRACT = "CONTRACT";
    private static final String PO = "PO";
    private static final String MASTER_AGREEMENT = "MASTER_AGREEMENT";
    private static final String BILATERAL = "Bilateral";
    private static final String UNILATERAL_OWNER = "Unilateral Favour Owner";
    private static final String UNILATERAL_CPTY = "Unilateral Against Owner";
    private static final String MCC_HAIRCUT = "MCC_HAIRCUT%";
    private static final String FREQUENCY = "FREQUENCY";
    private static final String MOODY = "Moody";
    private static final String SP = "S&P";
    private static final String FITCH = "Fitch";

    public static class ContractWrapper {

        // variables
        private String csaAgreement;
        private String masterAgreement;
        private String exchangeDirection;
        private final String elegibleCollatApplyTo = CONTRACT;
        private Vector<String> elegibleCollaterals;
        private Double valuationPercent;
        private String rehypothecation;
        private String paymentFreq;
        private JDatetime valuationDates;
        private Double transferIntAmount;
        private Vector<String> eligibleCcies;
        private String interestRateCcy;
        private String interestRateCode;
        private Double interestRateSpread;
        private String valuationAgent;
        private String valuationAgentRole;
        private JDatetime valuationTime;
        private JDatetime notificationTime;
        private JDatetime resolutionTime;
        private String disputeMethod;
        private String collatSub;
        private String thresLangAvailable;
        private String thresLang;
        private final String thresApplyToOwner = SAN;
        private String ownerThresDependsOn;
        private Double ownerThresAmount;
        private Double ownerThresMoody;
        private Double ownerThresSp;
        private Double ownerThresFitch;
        private final String thresApplyToCpty = CPTY;
        private String cptyThresDependsOn;
        private Double cptyThresAmount;
        private Double cptyThresMoody;
        private Double cptyThresSp;
        private Double cptyThresFitch;
        private String mtaLangAvailable;
        private String mtaLang;
        private final String mtaApplyToOwner = SAN;
        private String ownerMtaDependsOn;
        private Double ownerMtaAmount;
        private Double ownerMtaMoody;
        private Double ownerMtaSp;
        private Double ownerMtaFitch;
        private final String mtaApplyToCpty = CPTY;
        private String cptyMtaDependsOn;
        private Double cptyMtaAmount;
        private Double cptyMtaMoody;
        private Double cptyMtaSp;
        private Double cptyMtaFitch;
        private String iaLangAvailable;
        private String iaLang;
        private final String iaApplyToOwner = SAN;
        private String ownerIaDependsOn;
        private Double ownerIaAmount;
        private Double ownerIaMoody;
        private Double ownerIaSp;
        private Double ownerIaFitch;
        private final String iaApplyToCpty = CPTY;
        private String cptyIaDependsOn;
        private Double cptyIaAmount;
        private Double cptyIaMoody;
        private Double cptyIaSp;
        private Double cptyIaFitch;

        // constructor
        public ContractWrapper(CollateralConfig mcc, JDate processDate, JDate valueDate) {
            buildContractGenericInfo(mcc);
            buildContractGeneralInfo(mcc);
            buildOwnerThresInfo(mcc, processDate, valueDate);
            buildCptyThresInfo(mcc, processDate, valueDate);
            buildOwnerMtaInfo(mcc, processDate, valueDate);
            buildCptyMtaInfo(mcc, processDate, valueDate);
            buildOwnerIaInfo(mcc, processDate, valueDate);
            buildCptyIaInfo(mcc, processDate, valueDate);
        }

        // build contract generic info
        private void buildContractGenericInfo(CollateralConfig mcc) {
            setMasterAgreement(mcc);
        }

        // build contract general info
        private void buildContractGeneralInfo(CollateralConfig mcc) {
            setExchangeDirection(mcc);
            setEligibleCollaterals(mcc);
            setValuationPercent(mcc);
            setPayment(mcc);
            setEligibleCcies(mcc);
            setInterestRate(mcc);
            setValuationAndNotification(mcc);
            setDisputeResolution(mcc);
        }

        // build owner threshold info
        private void buildOwnerThresInfo(CollateralConfig mcc, JDate processDate, JDate valueDate) {
            if (ELBEandKGRutilities.isThresholdDependingOn(mcc, CollateralConfig.GLOBAL_RATING)) {
                setOwnerThresDependsOn(CollateralConfig.GLOBAL_RATING);
                setOwnerThresMoody(mcc, processDate, valueDate);
                setOwnerThresSp(mcc, processDate, valueDate);
                setOwnerThresFitch(mcc, processDate, valueDate);
            } else {
                setOwnerThresDependsOn(mcc.getPoNewThresholdType());
                setOwnerThresAmount(mcc, PO, processDate, valueDate);
            }
        }

        // build cpty threshold info
        private void buildCptyThresInfo(CollateralConfig mcc, JDate processDate, JDate valueDate) {
            if (ELBEandKGRutilities.isCptyThresholdDependingOn(mcc, CollateralConfig.GLOBAL_RATING)) {
                setCptyThresDependsOnRat(CollateralConfig.GLOBAL_RATING);
                setCptyThresMoody(mcc, processDate, valueDate);
                setCptyThresSp(mcc, processDate, valueDate);
                setCptyThresFitch(mcc, processDate, valueDate);
            } else {
                setCptyThresDependsOnRat(mcc.getLeNewThresholdType());
                setCptyThresAmount(mcc, CPTY, processDate, valueDate);
            }
        }

        // build owner mta info
        private void buildOwnerMtaInfo(CollateralConfig mcc, JDate processDate, JDate valueDate) {
            if (ELBEandKGRutilities.isMTADependingOn(mcc, CollateralConfig.GLOBAL_RATING)) {
                setOwnerMtaDependsOnRat(CollateralConfig.GLOBAL_RATING);
                setOwnerMtaMoody(mcc, processDate, valueDate);
                setOwnerMtaSp(mcc, processDate, valueDate);
                setOwnerMtaFitch(mcc, processDate, valueDate);
            } else {
                setOwnerMtaDependsOnRat(mcc.getPoMTAType());
                setOwnerMtaAmount(mcc, PO, processDate, valueDate);
            }
        }

        // build cpty mta info
        private void buildCptyMtaInfo(CollateralConfig mcc, JDate processDate, JDate valueDate) {
            if (ELBEandKGRutilities.isCptyMTADependingOn(mcc, CollateralConfig.GLOBAL_RATING)) {
                setCptyMtaDependsOnRat(CollateralConfig.GLOBAL_RATING);
                setCptyMtaMoody(mcc, processDate, valueDate);
                setCptyMtaSp(mcc, processDate, valueDate);
                setCptyMtaFitch(mcc, processDate, valueDate);
            } else {
                setCptyMtaDependsOnRat(mcc.getLeMTAType());
                setCptyMtaAmount(mcc, CPTY, processDate, valueDate);
            }
        }

        // build owner ia info
        private void buildOwnerIaInfo(CollateralConfig mcc, JDate processDate, JDate valueDate) {
            if (ELBEandKGRutilities.isIADependingOnRating(mcc)) {
                setOwnerIaDependsOnRat(CollateralConfig.GLOBAL_RATING);
                setOwnerIaMoody(mcc, processDate, valueDate);
                setOwnerIaSp(mcc, processDate, valueDate);
                setOwnerIaFitch(mcc, processDate, valueDate);
            } else {
                setOwnerIaDependsOnRat(CollateralConfig.AMOUNT);
                setOwnerIaAmount(mcc, PO, processDate, valueDate);
            }
        }

        // build cpty ia info
        private void buildCptyIaInfo(CollateralConfig mcc, JDate processDate, JDate valueDate) {
            if (ELBEandKGRutilities.isCptyIADependingOnRating(mcc)) {
                setCptyIaDependsOnRat(CollateralConfig.GLOBAL_RATING);
                setCptyIaMoody(mcc, processDate, valueDate);
                setCptyIaSp(mcc, processDate, valueDate);
                setCptyIaFitch(mcc, processDate, valueDate);
            } else {
                setCptyIaDependsOnRat(CollateralConfig.AMOUNT);
                setCptyIaAmount(mcc, CPTY, processDate, valueDate);
            }
        }

        // ----- CONTRACT GENERIC INFO ----- //

        private void setMasterAgreement(CollateralConfig mcc) {
            CollateralConfig maAgreement = mcc.getMasterAgreement();
            if (maAgreement != null) {
                this.masterAgreement = maAgreement.getName();
            } else {
                this.masterAgreement = mcc.getAdditionalField(MASTER_AGREEMENT);
            }
        }

        // ----- CONTRACT GENERAL INFO ----- //

        private void setExchangeDirection(CollateralConfig mcc) {
            String contractDirection = mcc.getContractDirection();
            if (!Util.isEmpty(contractDirection)) {
                if (contractDirection.equals(CollateralConfig.NET_BILATERAL)) {
                    this.exchangeDirection = BILATERAL;
                } else {
                    String securedParty = mcc.getSecuredParty();
                    if (!Util.isEmpty(securedParty)) {
                        if (securedParty.equals(LegalEntity.PROCESSINGORG)) {
                            this.exchangeDirection = UNILATERAL_OWNER;
                        } else {
                            this.exchangeDirection = UNILATERAL_CPTY;
                        }
                    }
                }
            }
        }

        private void setEligibleCollaterals(CollateralConfig mcc) {
            Vector<String> elegibleCollat = mcc.getProductList();
            if (!Util.isEmpty(elegibleCollat)) {
                // If collatExposure, get subtypes list
                if (elegibleCollat.get(0).equals(CollateralExposure.PRODUCT_TYPE)) {
                    List<String> exposureTypes = mcc.getExposureTypeList();
                    if (!Util.isEmpty(exposureTypes)) {
                        Vector<String> mappedExposureTypes = new Vector<String>();
                        for (String exposureType : exposureTypes) {
                            // mapeo needed?
                            // String mappedExposureType = CollateralUtilities.initMappingInstrumentValues(
                            // DSConnection.getDefault(), "GBO").get(exposureType);
                            mappedExposureTypes.add(exposureType);
                        }
                        this.elegibleCollaterals = mappedExposureTypes;
                    }
                } else {
                    this.elegibleCollaterals = elegibleCollat;
                }
            }
        }

        private void setValuationPercent(CollateralConfig mcc) {
            String contractHaircut = mcc.getAdditionalField(MCC_HAIRCUT);
            if (!Util.isEmpty(contractHaircut)) {
                this.valuationPercent = Util.stringToAmountValue(contractHaircut);
            }
        }

        private void setPayment(CollateralConfig mcc) {
            this.paymentFreq = mcc.getAdditionalField(FREQUENCY);
        }

        private void setEligibleCcies(CollateralConfig mcc) {
            Vector<String> elegibleCciesStr = new Vector<String>();
            List<CollateralConfigCurrency> eligibleCcies = mcc.getEligibleCurrencies();
            if (!Util.isEmpty(eligibleCcies)) {
                for (CollateralConfigCurrency eligibleCcy : eligibleCcies) {
                    elegibleCciesStr.add(eligibleCcy.getCurrency());
                }
                this.eligibleCcies = elegibleCciesStr;
            }
        }

        private void setInterestRate(CollateralConfig mcc) {
            String baseCcyName = mcc.getCurrency();
            CollateralConfigCurrency baseCcy = getBaseCollateralConfigCcy(mcc.getEligibleCurrencies(), baseCcyName);
            if (baseCcy != null) {
                this.interestRateCcy = baseCcy.getCurrency();
                if (baseCcy.getRateIndex() != null) {
                    this.interestRateCode = baseCcy.getRateIndex().getSource();
                    this.interestRateSpread = baseCcy.getRateIndexSpread();
                }
            }

        }

        private CollateralConfigCurrency getBaseCollateralConfigCcy(List<CollateralConfigCurrency> eligibleCcies,
                                                                    String baseCcy) {
            if (!Util.isEmpty(eligibleCcies)) {
                for (CollateralConfigCurrency ccy : eligibleCcies) {
                    if (ccy.getCurrency().equals(baseCcy)) {
                        return ccy;
                    }
                }
            }
            return null;
        }

        @SuppressWarnings("deprecation")
        private void setValuationAndNotification(CollateralConfig mcc) {
            this.valuationTime = mcc.getValuationTime();
            this.notificationTime = mcc.getNotificationTime();
            this.valuationAgent = CollateralUtilities.getValuationAgentFromContract(mcc);
            if (!Util.isEmpty(this.valuationAgent)) {
                String valAgentType = mcc.getValuationAgentType();
                if (valAgentType.equals(CollateralConfig.PARTY_A)) {
                    this.valuationAgentRole = LegalEntity.PROCESSINGORG;
                } else if (valAgentType.equals(CollateralConfig.PARTY_B)) {
                    this.valuationAgentRole = LegalEntity.COUNTERPARTY;
                } else if (valAgentType.equals(CollateralConfig.THIRD_PARTY)) {
                    this.valuationAgentRole = LegalEntity.CALCULATION_AGENT;
                }
            }
        }

        private void setDisputeResolution(CollateralConfig mcc) {
            this.resolutionTime = mcc.getResolutionTime();
            this.disputeMethod = mcc.getDisputeMethod();
        }

        // ------ THRESHOLD ----- //

        public Double getThresholdDependingOnRating(final CollateralConfig marginCall, final int mccRatingConfigId,
                                                    CreditRating creditRating, JDate processDate, JDate valueDate) {
            MarginCallCreditRating mccCreditRating = null;
            try {
                mccCreditRating = CollateralUtilities.getLatestMCCreditRating(mccRatingConfigId,
                        creditRating.getRatingValue(), creditRating.getAgencyName(), valueDate);
            } catch (Exception e) {
                Log.error("Cannot get MarginCallCreditRating for contract=" + marginCall.getId() + ", agency="
                        + creditRating.getAgencyName() + ", rating=" + creditRating.getRatingValue(), e);
            }
            if (mccCreditRating != null) {
                return ELBEandKGRutilities.getThresholdDependingOnRating(marginCall, mccCreditRating,
                        mccRatingConfigId, processDate, valueDate);
            }
            return null;
        }

        // ---- Owner

        public Double getOwnerThresholdNonDependingOnRating(final CollateralConfig marginCall, JDate processDate,
                                                            JDate valueDate) {
            // AMOUNT
            if (ELBEandKGRutilities.isThresholdDependingOn(marginCall, CollateralConfig.AMOUNT)) {
                return ELBEandKGRutilities.getThresholdDependingOnAmount(marginCall, PO, valueDate);
            }
            // MC_PERCENT
            //MIG_V14
//			if (ELBEandKGRutilities.isThresholdDependingOn(marginCall, CollateralConfig.MC_PERCENT)) {
//				return ELBEandKGRutilities.getThresholdDependingOnMcPercent(marginCall, PO, processDate);
//			}
            // PERCENT
            if (ELBEandKGRutilities.isThresholdDependingOn(marginCall, CollateralConfig.PERCENT)) {
                return ELBEandKGRutilities.getThresholdDependingOnPercent(marginCall, PO, processDate);
            }
            // BOTH
            if (ELBEandKGRutilities.isThresholdDependingOn(marginCall, CollateralConfig.BOTH)) {
                return ELBEandKGRutilities.getThresholdDependingOnBoth(marginCall, PO, processDate, valueDate);
            }
            return null;
        }

        private void setOwnerThresDependsOn(String isDependingOn) {
            this.ownerThresDependsOn = isDependingOn;
        }

        private void setOwnerThresAmount(CollateralConfig mcc, String leType, JDate processDate, JDate valueDate) {
            this.ownerThresAmount = getOwnerThresholdNonDependingOnRating(mcc, processDate, valueDate);
        }

        private void setOwnerThresMoody(CollateralConfig mcc, JDate processDate, JDate valueDate) {
            Vector<String> contractAgencies = mcc.getEligibleAgencies();
            if (!Util.isEmpty(contractAgencies) && contractAgencies.contains(MOODY)) {
                // get moody rating
                CreditRating moodyRating = ELBEandKGRutilities.getMoody(CreditRating.CURRENT, mcc.getPoId(), valueDate);
                if (moodyRating != null) {
                    this.ownerThresMoody = getThresholdDependingOnRating(mcc, mcc.getPoRatingsConfigId(), moodyRating,
                            processDate, valueDate);
                }
            }
        }

        private void setOwnerThresSp(CollateralConfig mcc, JDate processDate, JDate valueDate) {
            Vector<String> contractAgencies = mcc.getEligibleAgencies();
            if (!Util.isEmpty(contractAgencies) && contractAgencies.contains(SP)) {
                // get sp rating
                CreditRating spRating = ELBEandKGRutilities.getSnP(CreditRating.CURRENT, mcc.getPoId(), valueDate);
                if (spRating != null) {
                    this.ownerThresSp = getThresholdDependingOnRating(mcc, mcc.getPoRatingsConfigId(), spRating,
                            processDate, valueDate);
                }
            }
        }

        private void setOwnerThresFitch(CollateralConfig mcc, JDate processDate, JDate valueDate) {
            Vector<String> contractAgencies = mcc.getEligibleAgencies();
            if (!Util.isEmpty(contractAgencies) && contractAgencies.contains(FITCH)) {
                // get fitch rating
                CreditRating fitchRating = ELBEandKGRutilities.getFitch(CreditRating.CURRENT, mcc.getPoId(), valueDate);
                if (fitchRating != null) {
                    this.ownerThresFitch = getThresholdDependingOnRating(mcc, mcc.getPoRatingsConfigId(), fitchRating,
                            processDate, valueDate);
                }
            }
        }

        // --- Cpty

        public Double getCptyThresholdNonDependingOnRating(final CollateralConfig marginCall, JDate processDate,
                                                           JDate valueDate) {
            // AMOUNT
            if (ELBEandKGRutilities.isCptyThresholdDependingOn(marginCall, CollateralConfig.AMOUNT)) {
                return ELBEandKGRutilities.getThresholdDependingOnAmount(marginCall, CPTY, valueDate);
            }
            // MC_PERCENT
            //MIG_V14
//			if (ELBEandKGRutilities.isCptyThresholdDependingOn(marginCall, CollateralConfig.MC_PERCENT)) {
//				return ELBEandKGRutilities.getThresholdDependingOnMcPercent(marginCall, CPTY, processDate);
//			}
            // PERCENT
            if (ELBEandKGRutilities.isCptyThresholdDependingOn(marginCall, CollateralConfig.PERCENT)) {
                return ELBEandKGRutilities.getThresholdDependingOnPercent(marginCall, CPTY, processDate);
            }
            // BOTH
            if (ELBEandKGRutilities.isCptyThresholdDependingOn(marginCall, CollateralConfig.BOTH)) {
                return ELBEandKGRutilities.getThresholdDependingOnBoth(marginCall, CPTY, processDate, valueDate);
            }
            return null;
        }

        private void setCptyThresDependsOnRat(String isDependingOn) {
            this.cptyThresDependsOn = isDependingOn;
        }

        private void setCptyThresAmount(CollateralConfig mcc, String leType, JDate processDate, JDate valueDate) {
            this.cptyThresAmount = getCptyThresholdNonDependingOnRating(mcc, processDate, valueDate);
        }

        private void setCptyThresMoody(CollateralConfig mcc, JDate processDate, JDate valueDate) {
            Vector<String> contractAgencies = mcc.getEligibleAgencies();
            if (!Util.isEmpty(contractAgencies) && contractAgencies.contains(MOODY)) {
                // get moody rating
                CreditRating moodyRating = ELBEandKGRutilities.getMoody(CreditRating.CURRENT, mcc.getLeId(), valueDate);
                if (moodyRating != null) {
                    this.cptyThresMoody = getThresholdDependingOnRating(mcc, mcc.getLeRatingsConfigId(), moodyRating,
                            processDate, valueDate);
                }
            }
        }

        private void setCptyThresSp(CollateralConfig mcc, JDate processDate, JDate valueDate) {
            Vector<String> contractAgencies = mcc.getEligibleAgencies();
            if (!Util.isEmpty(contractAgencies) && contractAgencies.contains(SP)) {
                // get sp rating
                CreditRating spRating = ELBEandKGRutilities.getSnP(CreditRating.CURRENT, mcc.getLeId(), valueDate);
                if (spRating != null) {
                    this.cptyThresSp = getThresholdDependingOnRating(mcc, mcc.getLeRatingsConfigId(), spRating,
                            processDate, valueDate);
                }
            }
        }

        private void setCptyThresFitch(CollateralConfig mcc, JDate processDate, JDate valueDate) {
            Vector<String> contractAgencies = mcc.getEligibleAgencies();
            if (!Util.isEmpty(contractAgencies) && contractAgencies.contains(FITCH)) {
                // get fitch rating
                CreditRating fitchRating = ELBEandKGRutilities.getFitch(CreditRating.CURRENT, mcc.getLeId(), valueDate);
                if (fitchRating != null) {
                    this.cptyThresFitch = getThresholdDependingOnRating(mcc, mcc.getLeRatingsConfigId(), fitchRating,
                            processDate, valueDate);
                }
            }
        }

        // ----- MTA ----- //

        public Double getMtaDependingOnRating(final CollateralConfig marginCall, final int mccRatingConfigId,
                                              CreditRating creditRating, JDate processDate, JDate valueDate) {
            MarginCallCreditRating mccCreditRating = null;
            try {
                mccCreditRating = CollateralUtilities.getLatestMCCreditRating(mccRatingConfigId,
                        creditRating.getRatingValue(), creditRating.getAgencyName(), valueDate);
            } catch (Exception e) {
                Log.error("Cannot get MarginCallCreditRating for contract=" + marginCall.getId() + ", agency="
                        + creditRating.getAgencyName() + ", rating=" + creditRating.getRatingValue(), e);
            }
            if (mccCreditRating != null) {
                return ELBEandKGRutilities.getMtaDependingOnRating(marginCall, mccCreditRating, mccRatingConfigId,
                        processDate, valueDate);
            }
            return null;
        }

        // --- Owner

        public Double getOwnerMtaNonDependingOnRating(final CollateralConfig marginCall, JDate processDate,
                                                      JDate valueDate) {
            // AMOUNT
            if (ELBEandKGRutilities.isMTADependingOn(marginCall, CollateralConfig.AMOUNT)) {
                return ELBEandKGRutilities.getMtaDependingOnAmount(marginCall, PO, valueDate);
            }
            // MC_PERCENT
            //MIG_V14
//			if (ELBEandKGRutilities.isMTADependingOn(marginCall, CollateralConfig.MC_PERCENT)) {
//				return ELBEandKGRutilities.getMtaDependingOnMcPercent(marginCall, PO, processDate);
//			}
            // PERCENT
            if (ELBEandKGRutilities.isMTADependingOn(marginCall, CollateralConfig.PERCENT)) {
                return ELBEandKGRutilities.getMtaDependingOnPercent(marginCall, PO, processDate);
            }
            // BOTH
            if (ELBEandKGRutilities.isMTADependingOn(marginCall, CollateralConfig.BOTH)) {
                return ELBEandKGRutilities.getMtaDependingOnBoth(marginCall, PO, processDate, valueDate);
            }
            return null;
        }

        private void setOwnerMtaDependsOnRat(String isDependingOn) {
            this.ownerMtaDependsOn = isDependingOn;
        }

        private void setOwnerMtaAmount(CollateralConfig mcc, String leType, JDate processDate, JDate valueDate) {
            this.ownerMtaAmount = getOwnerMtaNonDependingOnRating(mcc, processDate, valueDate);
        }

        private void setOwnerMtaMoody(CollateralConfig mcc, JDate processDate, JDate valueDate) {
            Vector<String> contractAgencies = mcc.getEligibleAgencies();
            if (!Util.isEmpty(contractAgencies) && contractAgencies.contains(MOODY)) {
                // get moody rating
                CreditRating moodyRating = ELBEandKGRutilities.getMoody(CreditRating.CURRENT, mcc.getPoId(), valueDate);
                if (moodyRating != null) {
                    this.ownerMtaMoody = getMtaDependingOnRating(mcc, mcc.getPoRatingsConfigId(), moodyRating,
                            processDate, valueDate);
                }
            }
        }

        private void setOwnerMtaSp(CollateralConfig mcc, JDate processDate, JDate valueDate) {
            Vector<String> contractAgencies = mcc.getEligibleAgencies();
            if (!Util.isEmpty(contractAgencies) && contractAgencies.contains(SP)) {
                // get sp rating
                CreditRating spRating = ELBEandKGRutilities.getSnP(CreditRating.CURRENT, mcc.getPoId(), valueDate);
                if (spRating != null) {
                    this.ownerMtaSp = getMtaDependingOnRating(mcc, mcc.getPoRatingsConfigId(), spRating, processDate,
                            valueDate);
                }
            }
        }

        private void setOwnerMtaFitch(CollateralConfig mcc, JDate processDate, JDate valueDate) {
            Vector<String> contractAgencies = mcc.getEligibleAgencies();
            if (!Util.isEmpty(contractAgencies) && contractAgencies.contains(FITCH)) {
                // get fitch rating
                CreditRating fitchRating = ELBEandKGRutilities.getFitch(CreditRating.CURRENT, mcc.getPoId(), valueDate);
                if (fitchRating != null) {
                    this.ownerMtaFitch = getMtaDependingOnRating(mcc, mcc.getPoRatingsConfigId(), fitchRating,
                            processDate, valueDate);
                }
            }
        }

        // --- Cpty

        public Double getCptyMtaNonDependingOnRating(final CollateralConfig marginCall, JDate processDate,
                                                     JDate valueDate) {
            // AMOUNT
            if (ELBEandKGRutilities.isCptyMTADependingOn(marginCall, CollateralConfig.AMOUNT)) {
                return ELBEandKGRutilities.getMtaDependingOnAmount(marginCall, CPTY, valueDate);
            }
            // MC_PERCENT
            //MIG_V14
//			if (ELBEandKGRutilities.isCptyMTADependingOn(marginCall, CollateralConfig.MC_PERCENT)) {
//				return ELBEandKGRutilities.getMtaDependingOnMcPercent(marginCall, CPTY, processDate);
//			}
            // PERCENT
            if (ELBEandKGRutilities.isCptyMTADependingOn(marginCall, CollateralConfig.PERCENT)) {
                return ELBEandKGRutilities.getMtaDependingOnPercent(marginCall, CPTY, processDate);
            }
            // BOTH
            if (ELBEandKGRutilities.isCptyMTADependingOn(marginCall, CollateralConfig.BOTH)) {
                return ELBEandKGRutilities.getMtaDependingOnBoth(marginCall, CPTY, processDate, valueDate);
            }
            return null;
        }

        private void setCptyMtaDependsOnRat(String isDependingOn) {
            this.cptyMtaDependsOn = isDependingOn;
        }

        private void setCptyMtaAmount(CollateralConfig mcc, String leType, JDate processDate, JDate valueDate) {
            this.cptyMtaAmount = getCptyMtaNonDependingOnRating(mcc, processDate, valueDate);
        }

        private void setCptyMtaMoody(CollateralConfig mcc, JDate processDate, JDate valueDate) {
            Vector<String> contractAgencies = mcc.getEligibleAgencies();
            if (!Util.isEmpty(contractAgencies) && contractAgencies.contains(MOODY)) {
                // get moody rating
                CreditRating moodyRating = ELBEandKGRutilities.getMoody(CreditRating.CURRENT, mcc.getLeId(), valueDate);
                if (moodyRating != null) {
                    this.cptyMtaMoody = getMtaDependingOnRating(mcc, mcc.getLeRatingsConfigId(), moodyRating,
                            processDate, valueDate);
                }
            }
        }

        private void setCptyMtaSp(CollateralConfig mcc, JDate processDate, JDate valueDate) {
            Vector<String> contractAgencies = mcc.getEligibleAgencies();
            if (!Util.isEmpty(contractAgencies) && contractAgencies.contains(SP)) {
                // get sp rating
                CreditRating spRating = ELBEandKGRutilities.getSnP(CreditRating.CURRENT, mcc.getLeId(), valueDate);
                if (spRating != null) {
                    this.cptyMtaSp = getMtaDependingOnRating(mcc, mcc.getLeRatingsConfigId(), spRating, processDate,
                            valueDate);
                }
            }
        }

        private void setCptyMtaFitch(CollateralConfig mcc, JDate processDate, JDate valueDate) {
            Vector<String> contractAgencies = mcc.getEligibleAgencies();
            if (!Util.isEmpty(contractAgencies) && contractAgencies.contains(FITCH)) {
                // get fitch rating
                CreditRating fitchRating = ELBEandKGRutilities.getFitch(CreditRating.CURRENT, mcc.getLeId(), valueDate);
                if (fitchRating != null) {
                    this.cptyMtaFitch = getMtaDependingOnRating(mcc, mcc.getLeRatingsConfigId(), fitchRating,
                            processDate, valueDate);
                }
            }
        }

        // ----- INDEP. AMOUNT ----- //

        public Double getIaDependingOnRating(final CollateralConfig marginCall, final int mccRatingConfigId,
                                             CreditRating creditRating, JDate processDate, JDate valueDate) {
            MarginCallCreditRating mccCreditRating = null;
            try {
                mccCreditRating = CollateralUtilities.getLatestMCCreditRating(mccRatingConfigId,
                        creditRating.getRatingValue(), creditRating.getAgencyName(), valueDate);
            } catch (Exception e) {
                Log.error("Cannot get MarginCallCreditRating for contract=" + marginCall.getId() + ", agency="
                        + creditRating.getAgencyName() + ", rating=" + creditRating.getRatingValue(), e);
            }
            if (mccCreditRating != null) {
                return ELBEandKGRutilities.getIndAmountDependingOnRating(marginCall, mccCreditRating,
                        mccRatingConfigId, processDate, valueDate);
            }
            return null;
        }

        public double getIaNonDependingOnRating(final CollateralConfig marginCall, JDate valueDate) {
            double ia = ELBEandKGRutilities.getContractIA(marginCall);
            String iaCcy = ELBEandKGRutilities.getContractIAccy(marginCall);
            return ia * CollateralUtilities.getFXRate(valueDate, iaCcy, marginCall.getCurrency());
        }

        // --- Owner

        private void setOwnerIaDependsOnRat(String isDependingOn) {
            this.ownerIaDependsOn = isDependingOn;
        }

        private void setOwnerIaAmount(CollateralConfig mcc, String leType, JDate processDate, JDate valueDate) {
            this.ownerIaAmount = getIaNonDependingOnRating(mcc, valueDate);
        }

        private void setOwnerIaMoody(CollateralConfig mcc, JDate processDate, JDate valueDate) {
            Vector<String> contractAgencies = mcc.getEligibleAgencies();
            if (!Util.isEmpty(contractAgencies) && contractAgencies.contains(MOODY)) {
                // get moody rating
                CreditRating moodyRating = ELBEandKGRutilities.getMoody(CreditRating.CURRENT, mcc.getPoId(), valueDate);
                if (moodyRating != null) {
                    this.ownerIaMoody = getIaDependingOnRating(mcc, mcc.getPoRatingsConfigId(), moodyRating,
                            processDate, valueDate);
                }
            }
        }

        private void setOwnerIaSp(CollateralConfig mcc, JDate processDate, JDate valueDate) {
            Vector<String> contractAgencies = mcc.getEligibleAgencies();
            if (!Util.isEmpty(contractAgencies) && contractAgencies.contains(SP)) {
                // get sp rating
                CreditRating spRating = ELBEandKGRutilities.getSnP(CreditRating.CURRENT, mcc.getPoId(), valueDate);
                if (spRating != null) {
                    this.ownerIaSp = getIaDependingOnRating(mcc, mcc.getPoRatingsConfigId(), spRating, processDate,
                            valueDate);
                }
            }
        }

        private void setOwnerIaFitch(CollateralConfig mcc, JDate processDate, JDate valueDate) {
            Vector<String> contractAgencies = mcc.getEligibleAgencies();
            if (!Util.isEmpty(contractAgencies) && contractAgencies.contains(FITCH)) {
                // get fitch rating
                CreditRating fitchRating = ELBEandKGRutilities.getFitch(CreditRating.CURRENT, mcc.getPoId(), valueDate);
                if (fitchRating != null) {
                    this.ownerIaFitch = getIaDependingOnRating(mcc, mcc.getPoRatingsConfigId(), fitchRating,
                            processDate, valueDate);
                }
            }
        }

        // --- Cpty

        private void setCptyIaDependsOnRat(String isDependingOn) {
            this.cptyIaDependsOn = isDependingOn;
        }

        private void setCptyIaAmount(CollateralConfig mcc, String leType, JDate processDate, JDate valueDate) {
            this.cptyIaAmount = getIaNonDependingOnRating(mcc, valueDate);
        }

        private void setCptyIaMoody(CollateralConfig mcc, JDate processDate, JDate valueDate) {
            Vector<String> contractAgencies = mcc.getEligibleAgencies();
            if (!Util.isEmpty(contractAgencies) && contractAgencies.contains(MOODY)) {
                // get moody rating
                CreditRating moodyRating = ELBEandKGRutilities.getMoody(CreditRating.CURRENT, mcc.getLeId(), valueDate);
                if (moodyRating != null) {
                    this.cptyIaMoody = getIaDependingOnRating(mcc, mcc.getLeRatingsConfigId(), moodyRating,
                            processDate, valueDate);
                }
            }
        }

        private void setCptyIaSp(CollateralConfig mcc, JDate processDate, JDate valueDate) {
            Vector<String> contractAgencies = mcc.getEligibleAgencies();
            if (!Util.isEmpty(contractAgencies) && contractAgencies.contains(SP)) {
                // get sp rating
                CreditRating spRating = ELBEandKGRutilities.getSnP(CreditRating.CURRENT, mcc.getLeId(), valueDate);
                if (spRating != null) {
                    this.cptyIaSp = getIaDependingOnRating(mcc, mcc.getLeRatingsConfigId(), spRating, processDate,
                            valueDate);
                }
            }
        }

        private void setCptyIaFitch(CollateralConfig mcc, JDate processDate, JDate valueDate) {
            Vector<String> contractAgencies = mcc.getEligibleAgencies();
            if (!Util.isEmpty(contractAgencies) && contractAgencies.contains(FITCH)) {
                // get fitch rating
                CreditRating fitchRating = ELBEandKGRutilities.getFitch(CreditRating.CURRENT, mcc.getLeId(), valueDate);
                if (fitchRating != null) {
                    this.cptyIaFitch = getIaDependingOnRating(mcc, mcc.getLeRatingsConfigId(), fitchRating,
                            processDate, valueDate);
                }
            }
        }

        // Getters

        public String getCsaAgreement() {
            return this.csaAgreement;
        }

        public String getMasterAgreement() {
            return this.masterAgreement;
        }

        public String getExchangeDirection() {
            return this.exchangeDirection;
        }

        public String getElegibleCollatApplyTo() {
            return this.elegibleCollatApplyTo;
        }

        public Vector<String> getElegibleCollaterals() {
            return this.elegibleCollaterals;
        }

        public Double getValuationPercent() {
            return this.valuationPercent;
        }

        public String getRehypothecation() {
            return this.rehypothecation;
        }

        public String getPaymentFreq() {
            return this.paymentFreq;
        }

        public JDatetime getValuationDates() {
            return this.valuationDates;
        }

        public Double getTransferIntAmount() {
            return this.transferIntAmount;
        }

        public Vector<String> getElegibleCcies() {
            return this.eligibleCcies;
        }

        public String getInterestRateCode() {
            return this.interestRateCode;
        }

        public Double getInterestRateSpread() {
            return this.interestRateSpread;
        }

        public String getInterestRateCcy() {
            return this.interestRateCcy;
        }

        public String getValuationAgent() {
            return this.valuationAgent;
        }

        public String getValuationAgentRole() {
            return this.valuationAgentRole;
        }

        public JDatetime getValuationTime() {
            return this.valuationTime;
        }

        public JDatetime getNotificationTime() {
            return this.notificationTime;
        }

        public JDatetime getResolutionTime() {
            return this.resolutionTime;
        }

        public String getDisputeMethod() {
            return this.disputeMethod;
        }

        public String getCollatSub() {
            return this.collatSub;
        }

        public String getThresLangAvailable() {
            return this.thresLangAvailable;
        }

        public String getThresLang() {
            return this.thresLang;
        }

        public String getThresApplyToOwner() {
            return this.thresApplyToOwner;
        }

        public String getOwnerThresDependsOn() {
            return this.ownerThresDependsOn;
        }

        public Double getOwnerThresAmount() {
            return this.ownerThresAmount;
        }

        public Double getOwnerThresMoody() {
            return this.ownerThresMoody;
        }

        public Double getOwnerThresSp() {
            return this.ownerThresSp;
        }

        public Double getOwnerThresFitch() {
            return this.ownerThresFitch;
        }

        public String getThresApplyToCpty() {
            return this.thresApplyToCpty;
        }

        public String getCptyThresDependsOn() {
            return this.cptyThresDependsOn;
        }

        public Double getCptyThresAmount() {
            return this.cptyThresAmount;
        }

        public Double getCptyThresMoody() {
            return this.cptyThresMoody;
        }

        public Double getCptyThresSp() {
            return this.cptyThresSp;
        }

        public Double getCptyThresFitch() {
            return this.cptyThresFitch;
        }

        public String getMtaLangAvailable() {
            return this.mtaLangAvailable;
        }

        public String getMtaLang() {
            return this.mtaLang;
        }

        public String getMtaApplyToOwner() {
            return this.mtaApplyToOwner;
        }

        public String getOwnerMtaDependsOn() {
            return this.ownerMtaDependsOn;
        }

        public Double getOwnerMtaAmount() {
            return this.ownerMtaAmount;
        }

        public Double getOwnerMtaMoody() {
            return this.ownerMtaMoody;
        }

        public Double getOwnerMtaSp() {
            return this.ownerMtaSp;
        }

        public Double getOwnerMtaFitch() {
            return this.ownerMtaFitch;
        }

        public String getMtaApplyToCpty() {
            return this.mtaApplyToCpty;
        }

        public String getCptyMtaDependsOn() {
            return this.cptyMtaDependsOn;
        }

        public Double getCptyMtaAmount() {
            return this.cptyMtaAmount;
        }

        public Double getCptyMtaMoody() {
            return this.cptyMtaMoody;
        }

        public Double getCptyMtaSp() {
            return this.cptyMtaSp;
        }

        public Double getCptyMtaFitch() {
            return this.cptyMtaFitch;
        }

        public String getIaLangAvailable() {
            return this.iaLangAvailable;
        }

        public String getIaLang() {
            return this.iaLang;
        }

        public String getIaApplyToOwner() {
            return this.iaApplyToOwner;
        }

        public String getOwnerIaDependsOn() {
            return this.ownerIaDependsOn;
        }

        public Double getOwnerIaAmount() {
            return this.ownerIaAmount;
        }

        public Double getOwnerIaMoody() {
            return this.ownerIaMoody;
        }

        public Double getOwnerIaSp() {
            return this.ownerIaSp;
        }

        public Double getOwnerIaFitch() {
            return this.ownerIaFitch;
        }

        public String getIaApplyToCpty() {
            return this.iaApplyToCpty;
        }

        public String getCptyIaDependsOn() {
            return this.cptyIaDependsOn;
        }

        public Double getCptyIaAmount() {
            return this.cptyIaAmount;
        }

        public Double getCptyIaMoody() {
            return this.cptyIaMoody;
        }

        public Double getCptyIaSp() {
            return this.cptyIaSp;
        }

        public Double getCptyIaFitch() {
            return this.cptyIaFitch;
        }

    }

}
