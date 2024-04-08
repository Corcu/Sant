/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.apps.refdata;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.refdata.SantMarginCallStaticDataFilter;
import calypsox.tk.report.GlobalMTACollateralReport;
import calypsox.tk.report.globalmta.CollateralConfigMTAGroup;
import calypsox.util.ELBEandKGRutilities;
import calypsox.util.collateral.CollateralManagerUtil;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.apps.reporting.ReportUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.*;
import com.calypso.tk.refdata.sdfilter.SDFilterOperatorType;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.TradeArray;

import javax.swing.*;
import java.awt.*;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CustomCollateralConfigValidator implements CollateralConfigValidator {

    private static final String ONE_WAY = "OneWay";
    private static final String FULLY = "Fully";
    private static final String PARTIALLY = "Partially";
    private static final String EMIR_COLLATERAL_VALUE = "EMIR_COLLATERAL_VALUE";
    private static final String IM_SUB_CONTRACTS = "IM_SUB_CONTRACTS";
    private static final String CSD_CONTRACT_TYPE = "CSD";
    private static final String TRADE_DATE_ELEMENT = "Trade Date";
    private static final String CSA_CONTRACT_TYPE = "CSA";
    private static final String IM_VM_SWITCH_DATE_AF = "IM_VM_SWITCH_DATE";
    private static final String IM_CALCULATION_METHOD = "IM_CALCULATION_METHOD";
    private static final String IM_CALCULATION_METHOD_DEFAULT = "IM_CALCULATION_METHOD_DEFAULT";
    private static final String GROUP_IM = "IM";
    private static final String IM_GLOBAL_ID = "IM_GLOBAL_ID";
    private static final String IA_AMOUNT_DOWN_3NOTCH = "IA_AMOUNT_DOWN_3NOTCH";
    private static final String IA_AMOUNT_DOWN_2NOTCH = "IA_AMOUNT_DOWN_2NOTCH";
    private static final String IA_AMOUNT_DOWN_1NOTCH = "IA_AMOUNT_DOWN_1NOTCH";
    public static final String BOTH = "BOTH";
    public static final String AMOUNT = "AMOUNT";
    public static final String MAIN_SDF_PREFIX = "MAIN_";
    public static final String ADJUSTEMENT_SDF_PREFIX = "ADJ_";
    public static final String UNDERLYING_SDF_PREFIX = "UND_";
    public static final String SDF_GROUP_NAME = "MarginCall";
    public static final String USING_DEL_ROUNDING_TOTAL_ROUNDING = "usingDeliveryRoundingAndTotalRounding";
    private static final String MCC_ADD_FIELD_DV = "mccAdditionalField";
    private static final String MANDATORY = "|MANDATORY|";
    private static final String PO_SOVEREIGN = "SBWO";
    private static final String SBWO_GROUP = "SBWO";
    private static final int MAX_LENGTH_NAME = 27;

    // Initial Margin
    private static final String CSA_FACADE = "CSA_FACADE";
    private static final String INITIALMARGIN = "CSD";
    private static final String INITIALMARGINTYPE = "CPTY";
    private static final String VALUE_INITIAL_MARGIN_TYPE = "True";
    private static final String IM_INITIAL_MARGIN_TYPE = "IM_CSD_TYPE";
    private static final String INITIALMARGINLIST_CSAFACADE = "TECH_FACADE";
    private static final String INITIALMARGINLIST = "Initial Margin";
    private static final String IM_ACCOUNT = "TechnicalAccountIM";

    private static final String MTA_THRESHOLD_FIXING = "MTA_THRESHOLD_FIXING";
    private static final String MTA_EUR = "MTA_EUR";
    private static final String MTA_USD = "MTA_USD";
    private static final String THRESHOLD_EUR = "THRESHOLD_EUR";
    private static final String THRESLHOD_USD = "THRESLHOD_USD";
    private static final String TRUE = "TRUE";


    private Vector<String> ccyList = new Vector<String>();

    // Initial Margin
    private final ArrayList<String> listExposureType = new ArrayList<String>(
            Arrays.asList(INITIALMARGINLIST.split(",")));

    private final ArrayList<String> listExposureType_CSAFacade = new ArrayList<String>(
            Arrays.asList(INITIALMARGINLIST_CSAFACADE.split(",")));

    @SuppressWarnings("rawtypes")
    @Override
    public boolean isValidInput(CollateralConfig margincallconfig, Frame frame, Vector messages) {

        // EMIR 12/09/2017
        updateEMIRCollateralValue(margincallconfig);

        CollateralUtilities.updateSentinelValues(margincallconfig);

        return checkContractNameNotEmpty(margincallconfig, frame, messages)
                && checkPOAndLE(margincallconfig, frame, messages)
                && checkMtaAndThresholdCcy(margincallconfig, frame, messages)
                && checkPricingEnv(margincallconfig, frame, messages)
                && checkEffectiveDate(margincallconfig, frame, messages)
                && checkBeforeRoundingMTA(margincallconfig, frame, messages)
                && checkAlwaysRoundReturnMargin(margincallconfig, frame, messages)
                && checkLegalEntityRole(margincallconfig, frame, messages)
                && checkIACCYNotEmpty(margincallconfig, frame, messages)
                && handleContractIndpendentAmout(margincallconfig, messages)
                && checkNumericValuesAddFieldsforIA(margincallconfig, messages)
                && checkValAgentTypeNotEmpty(margincallconfig, frame, messages)
                && checkHedgeFundReportAddFieldNotEmpty(margincallconfig, frame, messages)
                && handleDefaultAddFieldValues(margincallconfig, frame, messages)
                && checkRatingDirectionNotEmpty(margincallconfig, frame, messages)
                && checkRatingConfigsNotEmpty(margincallconfig, frame, messages)
                && checkMandatoryAdditionalFields(margincallconfig, frame, messages) // new
                && checkExposureType(margincallconfig, frame, messages) // BAU
                // 5.5
                && checkCharacterContractName(margincallconfig, frame, messages) // BAU
                // 5.8

                && checkInitialMargin(margincallconfig, frame, messages)
                && checkInitialMarginType(margincallconfig, frame, messages)
                && setUpIMAdditionalFields(margincallconfig, frame, messages)
                && setUpVmSwitchDate(margincallconfig, frame, messages)
                && isValidGlobalMTA(margincallconfig);
    }

    private boolean isValidGlobalMTA(CollateralConfig margincallconfig) {
        if(!isCSAAccepted(margincallconfig) && !isCSDAccepted(margincallconfig)){
            return true;
        }
        CollateralConfigMTAGroup mtaGroup=getGlobalMTAData(margincallconfig);
        boolean res=true;

        if(mtaGroup!=null) {
            //New or upadated MTA Amount
            double newLEMTAAmount = margincallconfig.getLeMTAAmount();
            double oldLEMTAAmount = 0D;
            double newPOMTAAmount = margincallconfig.getPoMTAAmount();
            double oldPOMTAAmount = 0D;
            //Previous MTA Amount
            Optional<CollateralConfig> config = mtaGroup.getCollateralConfigs().stream().filter(s -> s.getId() == margincallconfig.getId()).findFirst();
            if (config.isPresent()) {
                oldLEMTAAmount = config.get().getLeMTAAmount();
                oldPOMTAAmount = config.get().getPoMTAAmount();
            }
            //FX oldMTA and newMTA to USD currency
            double fxQuote = CollateralUtilities.getFXRate(JDate.getNow(), margincallconfig.getCurrency(), "USD");
            double oldLEMTAAmountUSD = fxQuote * oldLEMTAAmount;
            double newLEMTAAmountUSD = fxQuote * newLEMTAAmount;
            double oldPOMTAAmountUSD = fxQuote * oldPOMTAAmount;
            double newPOMTAAmountUSD = fxQuote * newPOMTAAmount;
            //TotalMTA Amount´s validation
            boolean isLEThresholdExceeded = mtaGroup.isThresholdCptyExceeded(newLEMTAAmountUSD - oldLEMTAAmountUSD, "USD");
            boolean isPOThresholdExceeded = mtaGroup.isThresholdPOExceeded(newPOMTAAmountUSD - oldPOMTAAmountUSD, "USD");
            //Warning message
            if (isLEThresholdExceeded) {
                DecimalFormat df = new DecimalFormat("###,###.###");
                int dialogResult = JOptionPane.showConfirmDialog(null, "Contract's global MTA is being exceeded:\n -LE total LEI's MTA: " +
                        df.format(mtaGroup.getTotalMTACpty() - oldLEMTAAmountUSD + newLEMTAAmountUSD) + " USD" + System.lineSeparator() +
                        "Would you like to continue saving?", "GlobalMTA Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (dialogResult == 1) {
                    res = false;
                }
            }
            if (isPOThresholdExceeded) {
                DecimalFormat df = new DecimalFormat("###,###.###");
                int dialogResult = JOptionPane.showConfirmDialog(null, "Contract's global MTA is being exceeded:\n -PO total LEI's MTA: " +
                        df.format(mtaGroup.getTotalMTAOwner() - oldPOMTAAmountUSD + newPOMTAAmountUSD) + " USD" + System.lineSeparator() +
                        "Would you like to continue saving?", "GlobalMTA Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (dialogResult == 1) {
                    res = false;
                }
            }
        }
        return res;
    }

    private CollateralConfigMTAGroup getGlobalMTAData(CollateralConfig margincallconfig) {
        DefaultReportOutput output=null;
        Vector legalEntities = new Vector();
        Vector leIds = new Vector();
        Vector poId=new Vector<>();
        poId.add(margincallconfig.getPoId());

        ReportTemplate template = ReportUtil.loadTemplate("GlobalMTA", "GlobalMTACollateral", null);
        if(template!=null) {
            LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0,margincallconfig.getLeId(),"ALL", "LEI");
            if (attr != null) {
                try {
                    legalEntities = DSConnection.getDefault().getRemoteReferenceData().getLegalEntities("LEI",attr.getAttributeValue());
                } catch (CalypsoServiceException e) {
                    throw new RuntimeException(e);
                }
            }
            for (int i = 0; i < legalEntities.size(); i++) {
                    LegalEntity le = (LegalEntity) legalEntities.get(i);
                    leIds.add(le.getId());
            }

            template.put("PROCESSING_ORG_IDS", poId);
            template.put("LEGAL_ENTITY_IDS", leIds);
            GlobalMTACollateralReport report = new GlobalMTACollateralReport();
            report.setValuationDatetime(new JDatetime());
            report.setReportTemplate(template);
            output = (DefaultReportOutput) report.load(new Vector());
        }
        return (CollateralConfigMTAGroup) Optional.ofNullable(output).map(DefaultReportOutput::getRows)
                .filter(reportRows -> reportRows.length>0)
                .map(reportRows -> reportRows[0]).map(row -> row.getProperty(CollateralConfigMTAGroup.class.getSimpleName()))
                .orElse(null);
    }

    private boolean isCSDAccepted(CollateralConfig collateralConfig){
        return "CSD".equals(collateralConfig.getContractType()) &&
                collateralConfig.getName().contains("(PO)");
    }

    private boolean isCSAAccepted(CollateralConfig collateralConfig){
        return ("CSA".equals(collateralConfig.getContractType())
                &&(collateralConfig.getName().contains("VM)")||collateralConfig.getName().contains("(VM")||collateralConfig.getName().startsWith("VM-")));
    }

    // check mandatory additional fields
    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean checkMandatoryAdditionalFields(CollateralConfig margincallconfig, Frame frame, Vector messages) {

        boolean isValid = true;

        // get mandatory fields
        Vector<String> mandatoryAddFields = new Vector<String>();
        Vector<String> addFields = CollateralUtilities.getDomainValues(MCC_ADD_FIELD_DV);
        for (String addField : addFields) {
            String comment = CollateralUtilities.getDomainValueComment(MCC_ADD_FIELD_DV, addField);
            if (comment.contains(MANDATORY)) {
                mandatoryAddFields.add(addField);
            }
        }

        // check mandatory fields
        for (String mandatoryAddField : mandatoryAddFields) {
            if (Util.isEmpty(margincallconfig.getAdditionalField(mandatoryAddField))) {
                messages.add("Additional field " + mandatoryAddField + " cannot be empty.\n");
                isValid = false;
            }
        }

        return isValid;

    }

    // check owner & cpty rating dependency in order to avoid save contract when
    // rating config field is blank
    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean checkRatingConfigsNotEmpty(CollateralConfig margincallconfig, Frame frame, Vector messages) {
        boolean result = true;
        // check if owner is depending on rating (takes in account threshold,
        // MTA and IA)
        if (ELBEandKGRutilities.isOwnerKGRcontractDependingOnRating(margincallconfig)) {
            // check owner rating matrix
            if (margincallconfig.getPoRatingsConfigId() == 0) {
                messages.add("Owner rating config cannot be empty. Please select one.\n");
                result = false;
            }
        }
        // check if cpty is depending on rating (takes in account threshold, MTA
        // and IA)
        if (ELBEandKGRutilities.isCptyKGRcontractDependingOnRating(margincallconfig)) {
            // check cpty rating matrix
            if (margincallconfig.getLeRatingsConfigId() == 0) {
                messages.add("Cpty rating config cannot be empty. Please select one.\n");
                result = false;
            }
        }
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean checkIACCYNotEmpty(CollateralConfig margincallconfig, Frame frame, Vector messages) {
        if (Util.isEmpty(margincallconfig.getAdditionalField("CONTRACT_INDEPENDENT_AMOUNT"))) {
            return true;
        }

        String iaCcy = margincallconfig.getAdditionalField("CONTRACT_IA_CCY");
        if (Util.isEmpty(iaCcy)) {
            messages.add("Additional field CONTRACT_IA_CCY cannot be empty.\n");
            return false;
        }
        if (!isValidCurrency(iaCcy)) {
            messages.add("CONTRACT_IA_CCY value " + iaCcy + " is not a vaid currency.");
            return false;
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean checkRatingDirectionNotEmpty(CollateralConfig margincallconfig, Frame frame, Vector messages) {
        if (margincallconfig != null) {
            boolean result = true;
            if (CollateralConfig.GLOBAL_RATING.equals(margincallconfig.getPoMTAType())
                    && (Util.isEmpty(margincallconfig.getPoMTARatingDirection()))) {
                messages.add("\nPO MTA Rating direction cannot be empty");
                result = false;
            }
            if (CollateralConfig.GLOBAL_RATING.equals(margincallconfig.getPoNewThresholdType())
                    && (Util.isEmpty(margincallconfig.getPoThresholdRatingDirection()))) {
                messages.add("\nPO Threshold Rating direction cannot be empty");
                result = false;
            }

            if (CollateralConfig.GLOBAL_RATING.equals(margincallconfig.getLeMTAType())
                    && (Util.isEmpty(margincallconfig.getLeMTARatingDirection()))) {
                messages.add("\nLE MTA Rating direction cannot be empty");
                result = false;
            }
            if (CollateralConfig.GLOBAL_RATING.equals(margincallconfig.getLeNewThresholdType())
                    && (Util.isEmpty(margincallconfig.getLeThresholdRatingDirection()))) {
                messages.add("\nLE Threshold Rating direction cannot be empty");
                result = false;
            }

            return result;
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean checkContractNameNotEmpty(CollateralConfig margincallconfig, Frame frame, Vector messages) {
        if ((margincallconfig != null) && Util.isEmpty(margincallconfig.getName())) {
            messages.add("Agreement name cannot be empty.\n");
            return false;
        }
        return true;
    }

    @SuppressWarnings("rawtypes")
    private boolean handleDefaultAddFieldValues(CollateralConfig margincallconfig, Frame frame, Vector messages) {
        if ((margincallconfig != null) && Util.isEmpty(margincallconfig.getAdditionalField("ORDERER_ROLE"))) {
            margincallconfig.setAdditionalField("ORDERER_ROLE", "Client");
            margincallconfig.setAdditionalField("USE_MARGIN_CALL_ACCOUNT", "True");
        }

        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean checkValAgentTypeNotEmpty(CollateralConfig margincallconfig, Frame frame, Vector messages) {
        if ((margincallconfig != null) && Util.isEmpty(margincallconfig.getValuationAgentType())) {
            messages.add("Valuation Agent Type cannot be empty.\n");
            return false;
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean checkHedgeFundReportAddFieldNotEmpty(CollateralConfig margincallconfig, Frame frame,
                                                         Vector messages) {
        if ((margincallconfig != null) && Util.isEmpty(margincallconfig.getAdditionalField("HEDGE_FUNDS_REPORT"))) {
            messages.add("Addition field HEDGE_FUNDS_REPORT cannot be empty.\n");
            return false;
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean checkAlwaysRoundReturnMargin(CollateralConfig margincallconfig, Frame frame, Vector messages) {
        if ((margincallconfig != null) && (margincallconfig.getAdditionalField("ALWAYS_ROUND_RETURN_MARGIN") != null)
                && !margincallconfig.getAdditionalField("ALWAYS_ROUND_RETURN_MARGIN")
                .equals(USING_DEL_ROUNDING_TOTAL_ROUNDING)) {
            messages.add("\nAddition field ALWAYS_ROUND_RETURN_MARGIN should be '" + USING_DEL_ROUNDING_TOTAL_ROUNDING
                    + "'.\n");
            return false;
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean checkBeforeRoundingMTA(CollateralConfig margincallconfig, Frame frame, Vector messages) {

        if ((margincallconfig != null) && margincallconfig.isRoundingBeforeMTA()) {
            messages.add("\nRounding Before MTA should be false.");
            return false;
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean checkPricingEnv(CollateralConfig margincallconfig, Frame frame, Vector messages) {
        String mcPricingEnvDomain = "";
        if (PO_SOVEREIGN.equals(margincallconfig.getProcessingOrg().getCode())) {
            mcPricingEnvDomain = LocalCache.getDomainValueComment(DSConnection.getDefault(),
                    "SBWO_Sant_CustomMarginCallConfigValidator", "PricingEnv");
            if (Util.isEmpty(mcPricingEnvDomain) || !mcPricingEnvDomain.equals(margincallconfig.getPricingEnvName())) {
                messages.add("\nPricingEnv should be  " + mcPricingEnvDomain);
                return false;
            }
        } else {
            mcPricingEnvDomain = LocalCache.getDomainValueComment(DSConnection.getDefault(),
                    "Sant_CustomMarginCallConfigValidator", "PricingEnv");
        }
        if (Util.isEmpty(margincallconfig.getPricingEnvName())) {
            messages.add("\nPricingEnv should be  " + mcPricingEnvDomain);
            return false;
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean checkLegalEntityRole(CollateralConfig margincallconfig, Frame frame, Vector messages) {
        if (!Util.isEmpty(margincallconfig.getLeRole())) {
            if (!margincallconfig.getLeRole().equals("CounterParty")) {
                messages.add("\nLegal Entity Role should be CounterParty.");
                return true;
            }
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean checkEffectiveDate(CollateralConfig margincallconfig, Frame frame, Vector messages) {
    	if (margincallconfig.getContractType().equals("OSLA")) {
    		return true;
    	}
        String mcEffectiveDateDomain = "";

        if(margincallconfig.getProductList().stream().anyMatch("Repo"::equalsIgnoreCase)){
            mcEffectiveDateDomain = LocalCache.getDomainValueComment(DSConnection.getDefault(),
                    "Sant_CustomMarginCallConfigValidator_Repo", "Effective Date");
            final List<String> validEffectiveDates = Pattern.compile(",").splitAsStream(mcEffectiveDateDomain).map(String::trim).collect(Collectors.toList());
            final String effDateType = margincallconfig.getEffDateType();
            mcEffectiveDateDomain = validEffectiveDates.stream().filter(effDateType::equalsIgnoreCase).findFirst().orElse("");

        } else if (PO_SOVEREIGN.equals(margincallconfig.getProcessingOrg().getCode())) {
            mcEffectiveDateDomain = LocalCache.getDomainValueComment(DSConnection.getDefault(),
                    "SBWO_Sant_CustomMarginCallConfigValidator", "Effective Date");
        } else {
            mcEffectiveDateDomain = LocalCache.getDomainValueComment(DSConnection.getDefault(),
                    "Sant_CustomMarginCallConfigValidator", "Effective Date");
        }
        if (Util.isEmpty(mcEffectiveDateDomain) || !mcEffectiveDateDomain.equals(margincallconfig.getEffDateType())) {
            messages.add("\nEffective Date should be " + mcEffectiveDateDomain);
            return false;
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean checkPOAndLE(CollateralConfig margincallconfig, Frame frame, Vector messages) {

        LegalEntity legalEntity = margincallconfig.getLegalEntity();
        LegalEntity processingOrg = margincallconfig.getProcessingOrg();

        if ((processingOrg != null) && processingOrg.equals(legalEntity)) {
            messages.add("\nLegal Entity and PO must be different.");
        }

        List<LegalEntity> childLEs = margincallconfig.getAdditionalLE();
        List<LegalEntity> childPOs = margincallconfig.getAdditionalPO();

        if (!Util.isEmpty(childLEs) && childLEs.contains(processingOrg)) {
            messages.add("\nPO shouldn't be in the additional LE list.");
        }

        if (!Util.isEmpty(childPOs) && childPOs.contains(legalEntity)) {
            messages.add("\nContract Legal Entity shouldn't be in the additional PO list.");
        }

        if (messages.size() > 0) {
            return false;
        } else {
            return true;
        }

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean checkMtaAndThresholdCcy(CollateralConfig margincallconfig, Frame frame, Vector messages) {

        if ((margincallconfig.getPoMTAType().equals(AMOUNT) || margincallconfig.getPoMTAType().equals(BOTH))
                && Util.isEmpty(margincallconfig.getPoMTACurrency())) {

            messages.add("\nPO MTA Currency must be specified.");
        }

        if ((margincallconfig.getLeMTAType().equals(AMOUNT) || margincallconfig.getLeMTAType().equals(BOTH))
                && Util.isEmpty(margincallconfig.getLeMTACurrency())) {
            messages.add("\nLE MTA Currency must be specified.");
        }

        if ((margincallconfig.getPoNewThresholdType().equals(AMOUNT)
                || margincallconfig.getPoNewThresholdType().equals(BOTH))
                && Util.isEmpty(margincallconfig.getPoNewThresholdCurrency())) {
            messages.add("\nPO Threshold Currency must be specified.");
        }

        if ((margincallconfig.getLeNewThresholdType().equals(AMOUNT)
                || margincallconfig.getLeNewThresholdType().equals(BOTH))
                && Util.isEmpty(margincallconfig.getLeNewThresholdCurrency())) {
            messages.add("\nLE Threshold Currency must be specified.");
        }

        if (messages.size() > 0) {
            return false;
        } else {
            return true;
        }

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean handleContractIndpendentAmout(CollateralConfig margincallconfig, Vector messages) {

        //
        String iaCcy = margincallconfig.getAdditionalField("CONTRACT_IA_CCY");
        if (!Util.isEmpty(iaCcy)) {
            iaCcy = iaCcy.toUpperCase();
            if (!isValidCurrency(iaCcy)) {
                messages.add(" Additional field CONTRACT_IA_CCY value " + iaCcy + " is not a vaid currency.");
                return false;
            } else {
                margincallconfig.setAdditionalField("CONTRACT_IA_CCY", iaCcy);
            }
        }

        // check that the contract has a static data filter to inclure this
        // CONTRACT_IA trade.
        StaticDataFilter sdf = null;
        if (!isValidSDF(margincallconfig)) {
        	// For SecLending, do not create new SDFilter
        	if (margincallconfig.getAdditionalField("AUTO_SL_CONTRACT") == null || !margincallconfig.getAdditionalField("AUTO_SL_CONTRACT").equals("true")) {
        		try {
        			sdf = createProductStaticDataFilter(margincallconfig);
        		} catch (RemoteException e) {
        			Log.info(this, e); // sonar
        			messages.add("Unable to create product static data filter for the contract.");
        			return false;
        		}
        		margincallconfig.setProdStaticDataFilterName(sdf.getName());
        	}
        } else {
            try {
                sdf = DSConnection.getDefault().getRemoteReferenceData()
                        .getStaticDataFilter(margincallconfig.getProdStaticDataFilterName());
            } catch (CalypsoServiceException e) {
                Log.error(this, "Cannot load " + margincallconfig.getProdStaticDataFilterName() + "for contract "
                        + margincallconfig.getId());
                Log.error(this, e); // sonar
            }
            if (sdf != null) {
                Vector errors = new Vector();
                Vector<StaticDataFilter> sdfElements = sdf.getLinkedStaticDatafilters(errors);
                boolean isADJFilterDefined = false;
                boolean isUNDFilterDefined = false;
                if (!Util.isEmpty(sdfElements)) {
                    for (StaticDataFilter tmpSDF : sdfElements) {
                        String filterName = tmpSDF.getName();
                        if (filterName.startsWith(ADJUSTEMENT_SDF_PREFIX)) {
                            isADJFilterDefined = true;
                        }
                        if (filterName.startsWith(UNDERLYING_SDF_PREFIX)) {
                            isUNDFilterDefined = true;
                        }
                    }
                }
                if (!isADJFilterDefined) {
                    messages.add("Please add the adjustement static data filter to the Product Filter.");
                    return false;
                }

                if (!isUNDFilterDefined) {
                    messages.add("Please add the underlying static data filter to the Product Filter.");
                    return false;
                }

            }
        }

        String ctrIA = margincallconfig.getAdditionalField("CONTRACT_INDEPENDENT_AMOUNT");
        if (Util.isEmpty(ctrIA)) {
            return true;
        }

        Double ia = 0.0;
        try {
            ia = Double.valueOf(ctrIA);
        } catch (Exception e) {
            Log.error(this, e);
            messages.add("Unknown Independent Amount value");
            return false;
        }

        if (ia == 0.0) {
            return true;
        }
        // check that the contract eligible product list contains the
        // CONTRACT_IA type;

        if (!margincallconfig.getProductList().contains("CollateralExposure")) {
            messages.add(
                    "CollateralExposure should be added to the product list when contract independent amount (additional field) is set.");
            return false;
        }

        if ((margincallconfig.getExposureTypeList() != null) && (margincallconfig.getExposureTypeList().size() > 0)) {
            if (!margincallconfig.getExposureTypeList().contains("CONTRACT_IA")) {
                messages.add(
                        "CONTRACT_IA should be added to the Exposure types list when contract independent amount (additional field) is set.");
                return false;
            }
        }

        // Trade iaTrade = null;
        try {
            // before looking for the existence of the IA trade, if we're
            // creating the contract then stop the process
            // and warn the user to save the IA after
            // the contract creation

            if (margincallconfig.getId() == 0) {
                messages.add(
                        "The contract should be created before an independent amount (additional field) can be set ");
                return false;
            }
            TradeArray iaTrades = DSConnection.getDefault().getRemoteTrade().getTrades("product_collateral_exposure",
                    "trade.product_id=product_collateral_exposure.product_id and product_collateral_exposure.mcc_id="
                            + margincallconfig.getId()
                            + " and underlying_type='CONTRACT_IA' and trade.trade_status<>'CANCELED' ",
                    "trade.trade_id", null);

            if (!Util.isEmpty(iaTrades) && (iaTrades.size() > 0)) {
                if (iaTrades.size() > 1) {
                    messages.add("More than one independent amount trade found for the contract.");
                    return false;
                }
            }

        } catch (Exception e) {
            Log.error(this, e);
            messages.add("Unable to get the independent amount trade from the contract.");
            return false;
        }
        // Trade oldTrade = null;
        // try {
        // if (iaTrade != null) {
        // oldTrade = (Trade) iaTrade.clone();
        // }
        // } catch (CloneNotSupportedException ce) {
        // Log.error(this, ce);
        // }
        // // create a independent amout trade for the current contract
        // boolean iaTradeHandledCorrectly = false;
        // try {
        // iaTradeHandledCorrectly =
        // createUpdateIAExposureTrade(margincallconfig, ia, iaTrade, oldTrade);
        // } catch (Exception e) {
        // Log.error(this, e);
        // iaTrade = null;
        // }
        //
        // if (!iaTradeHandledCorrectly) {
        // messages.add("Unable to create/update the indpenedent amount trade
        // for the contract "
        // + margincallconfig.getId());
        // return false;
        //
        // }

        return true;
    }

    // /**
    // * @param mcc
    // * @param ia
    // * @param trade
    // * @return
    // * @throws RemoteException
    // */
    // private boolean createUpdateIAExposureTrade(MarginCallConfig mcc, Double
    // ia, Trade trade, Trade oldTrade)
    // throws Exception {
    // Calendar cal = Calendar.getInstance(mcc.getValuationTimeZone());
    // cal.setTimeInMillis(mcc.getStartingDate().getTime());
    // cal.set(Calendar.AM_PM, Calendar.PM);
    // cal.set(Calendar.HOUR, 11);
    // cal.set(Calendar.MINUTE, 59);
    // cal.set(Calendar.SECOND, 59);
    //
    // JDatetime tradeDateTime = new JDatetime(cal.getTime());
    //
    // DSConnection dsCon = DSConnection.getDefault();
    // CollateralExposure product = null;
    // if (trade == null) {
    // trade = new Trade();
    // trade.setAction(Action.NEW);
    // product = new CollateralExposure();
    // trade.setProduct(product);
    // } else {
    // trade.setAction(Action.valueOf("UNPRICE"));
    // product = (CollateralExposure) trade.getProduct();
    // }
    //
    // // set trade properties
    //
    // trade.setTraderName(dsCon.getUser());
    // trade.setCounterParty(BOCache.getLegalEntity(dsCon, mcc.getLeId()));
    // trade.setTradeCurrency(mcc.getCurrency());
    // trade.setSettleCurrency(mcc.getCurrency());
    // trade.setBook(mcc.getBook());
    // trade.addKeyword("BO_REFERENCE", "IA");
    // trade.addKeyword(CollateralStaticAttributes.MC_CONTRACT_NUMBER,
    // mcc.getId());
    //
    // // set trade and product dates
    // trade.setSettleDate(mcc.getStartingDate().getJDate(TimeZone.getDefault()));
    // trade.setTradeDate(tradeDateTime);
    // product.setEnteredDatetime(new JDatetime());
    // product.setStartDate(mcc.getStartingDate().getJDate(TimeZone.getDefault()));
    // // product.setMaturityDate(entry.getProcessDate());
    // // set the end date for the product
    // // product.setEndDate(entry.getProcessDate());
    // product.setDirection((ia >= 0 ? "Buy" : "Sell"), trade);
    // // set the product properties
    // product.setPrincipal(ia);
    // product.setSubType("CONTRACT_IA");
    // product.setUnderlyingType("CONTRACT_IA");
    // product.setCurrency(mcc.getCurrency());
    // // link this trade to the entry contract
    // product.setMccId(mcc.getId());
    // product.addAttribute("CONTRACT_ID", "" + mcc.getId());
    // // if (oldTrade == null) {
    // // // PendingModification pendingModifs = AuthUtil.getNewDiff(trade,
    // dsCon.getUser(), new JDatetime());
    // // // pendingModifs.setEntityName("" + mcc.getId());
    // // //
    // DSConnection.getDefault().getRemoteReferenceData().save(pendingModifs);
    // // GenericComment gc = null;
    // // // task.setUserComment(comment);
    // // gc = new GenericComment(trade);
    // // gc.setEnteredUser(DSConnection.getDefault().getUser());
    // // gc.setDocument(objectToBytes(trade), GenericComment.OBJECT, false);
    // // gc.setEnteredDatetime(new JDatetime());
    // // gc.setComment("Trade to be created/updated for the MCC");
    // // gc.setType("MarginCallConfig");
    // // DSConnection.getDefault().getRemoteBO().saveGenericComment(gc);
    // //
    // // } else {
    // // Vector diffs = new Vector();
    // // AuthUtil.buildDiff(trade, oldTrade, diffs, dsCon.getUser(), new
    // JDatetime());
    // // if (diffs.size() > 0) {
    // //
    // DSConnection.getDefault().getRemoteReferenceData().savePendingModifications(diffs);
    // // }
    // // }
    //
    // GenericComment genComment = null;
    // // task.setUserComment(comment);
    // genComment = new GenericComment(trade);
    // genComment.setEnteredUser(DSConnection.getDefault().getUser());
    // genComment.setDocument(objectToBytes(trade), GenericComment.OBJECT,
    // false);
    // genComment.setEnteredDatetime(new JDatetime());
    // ObjectDescription mccObjDesc = new ObjectDescription();
    // mccObjDesc.setId(mcc.getId());
    // mccObjDesc.setVersion(mcc.getVersion());
    // mccObjDesc.setClassName("MarginCallConfig");
    //
    // genComment.setObjectDescription(mccObjDesc);
    // genComment.setComment("Trade to be created/updated for the MCC");
    // genComment.setType("MarginCallConfigValidation");
    // DSConnection.getDefault().getRemoteBO().saveGenericComment(genComment);
    //
    // // long tradeId = dsCon.getRemoteTrade().save(trade);
    // // if (tradeId > 0) {
    // // return dsCon.getRemoteTrade().getTrade(tradeId);
    // // }
    // return true;
    // }

    private boolean isValidSDF(CollateralConfig mcc) {
        if ((mcc.getContractType().equals(INITIALMARGIN))) {
            mcc.setProdStaticDataFilterName("");
            return true;
        }
        if (Util.isEmpty(mcc.getProdStaticDataFilterName())) {
            return false;
        } else {
            String mccFilterName = getFilterNameToSave(MAIN_SDF_PREFIX + mcc.getName());
            String inputedFilterName = getFilterNameToSave(mcc.getProdStaticDataFilterName());
            if (inputedFilterName.equals(mccFilterName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generates the StaticDataFilter.
     *
     * @param mcc Issuer for the Security, used as a parameter in the SDF.
     * @return The new StaticDataFilter created.
     * @throws RemoteException
     */
    @SuppressWarnings({"unchecked", "rawtypes", "deprecation"})
    private StaticDataFilter createProductStaticDataFilter(CollateralConfig mcc) throws RemoteException {

        // check if the sdf already exists
        StaticDataFilter existingSDF = DSConnection.getDefault().getRemoteReferenceData()
                .getStaticDataFilter(getFilterNameToSave(MAIN_SDF_PREFIX + mcc.getName()));

        if (existingSDF != null) {
            return existingSDF;
        }

        StaticDataFilter mainSDFilter = new StaticDataFilter(getFilterNameToSave(MAIN_SDF_PREFIX + mcc.getName()));
        StaticDataFilter adjSDFilter = new StaticDataFilter(
                getFilterNameToSave(ADJUSTEMENT_SDF_PREFIX + mcc.getName()));
        StaticDataFilter undSDFilter = new StaticDataFilter(getFilterNameToSave(UNDERLYING_SDF_PREFIX + mcc.getName()));

        if ((Util.isEmpty(mcc.getProdStaticDataFilterName())) || (mcc.getId() == 0)) {

            Set<String> setGroup = new HashSet<String>();

            if (PO_SOVEREIGN.equals(mcc.getProcessingOrg().getCode())) {
                setGroup.add(SBWO_GROUP);
            } else {
                setGroup.add(SDF_GROUP_NAME);
            }

            StaticDataFilterElement mainSDFElement = new StaticDataFilterElement(StaticDataFilterElement.IN_SD_FILTER);
            StaticDataFilterElement adjSDFElementIsCollateralized = new StaticDataFilterElement(
                    SantMarginCallStaticDataFilter.IS_COLLATERALIZABLE_TRADE);
            StaticDataFilterElement adjSDFElementMCC_ID = new StaticDataFilterElement(
                    "KEYWORD." + CollateralStaticAttributes.MC_CONTRACT_NUMBER);

            // Values for mcc id element.
            Vector<String> values = new Vector<String>();
            values.add("" + mcc.getId());
            adjSDFElementMCC_ID.setValues(values);
            adjSDFElementMCC_ID.setType(StaticDataFilterElement.STRING_ENUMERATION);

            // Values for is collateralized trade element.
            adjSDFElementIsCollateralized.setIsValue(false);
            adjSDFElementIsCollateralized.setType(StaticDataFilterElement.IS);

            adjSDFilter.setComment("SDF Created automatically at contract validation for technical trades");
            adjSDFilter.setGroups(setGroup);
            Vector<StaticDataFilterElement> elements = new Vector<StaticDataFilterElement>();
            elements.add(adjSDFElementIsCollateralized);
            elements.add(adjSDFElementMCC_ID);
            adjSDFilter.setElements(elements);
            boolean sdfSaved = DSConnection.getDefault().getRemoteReferenceData().save(adjSDFilter);
            if (!sdfSaved) {
                throw new RemoteException("Static Data Filter " + adjSDFilter.getName() + " not saved.");
            }

            // Values for is collateralized trade element.
            adjSDFElementIsCollateralized.setIsValue(true);
            adjSDFElementIsCollateralized.setType(StaticDataFilterElement.IS);

            undSDFilter.setComment("SDF Created automatically at contract validation for underlying trades");
            undSDFilter.setGroups(setGroup);
            elements = new Vector<StaticDataFilterElement>();
            elements.add(adjSDFElementIsCollateralized);
            undSDFilter.setElements(elements);
            sdfSaved = DSConnection.getDefault().getRemoteReferenceData().save(undSDFilter);
            if (!sdfSaved) {
                throw new RemoteException("Static Data Filter " + adjSDFilter.getName() + " not saved.");
            }

            // Values for Product Type element.
            values = new Vector<String>();
            values.add(adjSDFilter.getName());
            values.add(undSDFilter.getName());

            mainSDFElement.setValues(values);
            mainSDFElement.setType(StaticDataFilterElement.IN);

            mainSDFilter.setComment("SDF Created automatically at contract validation");
            mainSDFilter.setGroups(setGroup);
            elements = new Vector<StaticDataFilterElement>();
            elements.add(mainSDFElement);
            mainSDFilter.setElements(elements);

            sdfSaved = DSConnection.getDefault().getRemoteReferenceData().save(mainSDFilter);
            if (!sdfSaved) {
                throw new RemoteException("Static Data Filter " + mainSDFilter.getName() + " not saved.");
            }

            return mainSDFilter;
        } else {

            mainSDFilter = DSConnection.getDefault().getRemoteReferenceData()
                    .getStaticDataFilter(mcc.getProdStaticDataFilterName());
            mainSDFilter.setName(MAIN_SDF_PREFIX + mcc.getName());
            Vector errors = new Vector();
            Vector<StaticDataFilter> sdfElements = mainSDFilter.getLinkedStaticDatafilters(errors);

            if (!Util.isEmpty(sdfElements)) {
                for (StaticDataFilter tmpSDF : sdfElements) {
                    String filterName = tmpSDF.getName();
                    if (filterName.startsWith(ADJUSTEMENT_SDF_PREFIX)) {
                        adjSDFilter = DSConnection.getDefault().getRemoteReferenceData()
                                .getStaticDataFilter(tmpSDF.getName());
                        adjSDFilter.setName(ADJUSTEMENT_SDF_PREFIX + mcc.getName());
                        boolean sdfSaved = DSConnection.getDefault().getRemoteReferenceData().save(adjSDFilter);
                        if (!sdfSaved) {
                            throw new RemoteException("Static Data Filter " + adjSDFilter.getName() + " not saved.");
                        } else {
                            if (mcc.getId() != 0) {
                                DSConnection.getDefault().getRemoteReferenceData()
                                        .removeStaticDataFilter(tmpSDF.getName());
                            }
                        }
                    }
                    if (filterName.startsWith(UNDERLYING_SDF_PREFIX)) {
                        undSDFilter = DSConnection.getDefault().getRemoteReferenceData()
                                .getStaticDataFilter(tmpSDF.getName());
                        undSDFilter.setName(UNDERLYING_SDF_PREFIX + mcc.getName());
                        boolean sdfSaved = DSConnection.getDefault().getRemoteReferenceData().save(undSDFilter);
                        if (!sdfSaved) {
                            throw new RemoteException("Static Data Filter " + adjSDFilter.getName() + " not saved.");
                        } else {
                            if (mcc.getId() != 0) {
                                DSConnection.getDefault().getRemoteReferenceData()
                                        .removeStaticDataFilter(tmpSDF.getName());
                            }
                        }
                    }
                }

                // Values for Product Type element.
                Vector<String> values = new Vector<String>();
                values.add(adjSDFilter.getName());
                values.add(undSDFilter.getName());

                Vector<StaticDataFilterElement> elementsMain = mainSDFilter.getElements();
                Vector<StaticDataFilterElement> elements = new Vector<StaticDataFilterElement>();

                for (StaticDataFilterElement element : elementsMain) {

                    if (element.getName().equals(StaticDataFilterElement.IN_SD_FILTER)) {
                        StaticDataFilterElement mainSDFElement = new StaticDataFilterElement(
                                StaticDataFilterElement.IN_SD_FILTER);
                        mainSDFElement.setValues(values);
                        mainSDFElement.setType(StaticDataFilterElement.IN);
                        elements.add(mainSDFElement);
                    } else {
                        StaticDataFilterElement mainSDFElement = new StaticDataFilterElement(element.getName());
                        mainSDFElement.setValues(element.getValues());
                        // MIGRATION V14.4
                        mainSDFElement.setOperatorType(element.getOperatorType());
                        elements.add(mainSDFElement);
                    }
                }
                mainSDFilter.setElements(elements);
            }

            boolean sdfSaved = DSConnection.getDefault().getRemoteReferenceData().save(mainSDFilter);
            if (!sdfSaved) {
                throw new RemoteException("Static Data Filter " + mainSDFilter.getName() + " not saved.");
            } else {
                DSConnection.getDefault().getRemoteReferenceData()
                        .removeStaticDataFilter(mcc.getProdStaticDataFilterName());
            }

            return mainSDFilter;
        }

    }

    private String getFilterNameToSave(String filterName) {
        if (Util.isEmpty(filterName)) {
            return filterName;
        }
        if (filterName.length() > 32) {
            return filterName.substring(0, 32);

        } else {
            return filterName;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked", "unused"})
    private boolean checkNumericValuesAddFieldsforIA(CollateralConfig margincallconfig, Vector messages) {
        String addFieldIA_Notch1 = margincallconfig.getAdditionalField(IA_AMOUNT_DOWN_1NOTCH);
        String addFieldIA_Notch2 = margincallconfig.getAdditionalField(IA_AMOUNT_DOWN_2NOTCH);
        String addFieldIA_Notch3 = margincallconfig.getAdditionalField(IA_AMOUNT_DOWN_3NOTCH);

        if (!Util.isEmpty(addFieldIA_Notch1)) {
            try {
                Double doubleValue = Double.parseDouble(addFieldIA_Notch1);
            } catch (NumberFormatException nfe) {
                messages.add("\nYou have to type a numeric value in the Additional Field IA_AMOUNT_DOWN_1NOTCH\n");
            }
        }
        if (!Util.isEmpty(addFieldIA_Notch2)) {
            try {
                Double doubleValue = Double.parseDouble(addFieldIA_Notch2);
            } catch (NumberFormatException nfe) {
                messages.add("\nYou have to type a numeric value in the Additional Field IA_AMOUNT_DOWN_2NOTCH\n");
            }
        }
        if (!Util.isEmpty(addFieldIA_Notch3)) {
            try {
                Double doubleValue = Double.parseDouble(addFieldIA_Notch3);
            } catch (NumberFormatException nfe) {
                messages.add("\nYou have to type a numeric value in the Additional Field IA_AMOUNT_DOWN_3NOTCH\n");
            }
        }

        if (messages.size() > 0) {
            return false;
        } else {
            return true;
        }
    }

    private boolean isValidCurrency(String ccy) {
        if (Util.isEmpty(ccy)) {
            return false;
        }

        if (Util.isEmpty(this.ccyList)) {
            this.ccyList = LocalCache.getCurrencies();
        }
        return this.ccyList.contains(ccy);
    }

    // BAU 5.5 - Field Exposure Type will be mandatory. This method check the
    // field when we save a contract
    // BAU 5.7 - The Product List must contains type "CollateralExposure"
    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean checkExposureType(CollateralConfig margincallconfig, Frame frame, Vector messages) {
        if (margincallconfig.getContractType().equals(INITIALMARGIN)) {
            margincallconfig.setExposureTypeList(this.listExposureType);
            return true;
        }
        if (margincallconfig.getContractType().equals(CSA_FACADE)) {
            margincallconfig.setExposureTypeList(this.listExposureType_CSAFacade);
            return true;
        }
        if (margincallconfig.getProductList().contains("CollateralExposure")) {
            if (Util.isEmpty(margincallconfig.getExposureTypeList())) {
                messages.add("The field 'Exposure Types' cannot be empty");
                return false;
            }
        }
        return true;
    }

    // BAU 5.8 - Max. number of characters in contract?s name
    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean checkCharacterContractName(CollateralConfig margincallconfig, Frame frame, Vector messages) {
        String maxLengthName = LocalCache.getDomainValueComment(DSConnection.getDefault(), "CodeActivationDV", "MaxLengthMCCName");
        int maxLengthNameValue;

        //If the domain value does not have the comment defined or does not exist or is not numeric
        if(Util.isEmpty(maxLengthName) || !isNumeric(maxLengthName)) {
            maxLengthNameValue = MAX_LENGTH_NAME;
        }else{
            maxLengthNameValue = Integer.valueOf(maxLengthName);
        }

        if (margincallconfig.getName().length() > maxLengthNameValue) {
            messages.add("Maximum number of characters in contract`s name is " + maxLengthNameValue);
            return false;
        }
        //Check Contract's name with a list of characters excluded, they are storaged on DV ContractNameCharsExcluded
        if (!checkExcludeCharacters(margincallconfig, messages)) {
            return false;
        }
        return true;
    }

    //method that tests if a character string is numeric
    private boolean isNumeric(String str){
        try{
            Integer.parseInt(str);
            return true;
        }catch(NumberFormatException e){
            return false;
        }
    }

    @SuppressWarnings({"rawtypes"})
    private boolean checkInitialMargin(CollateralConfig margincallconfig, Frame frame, Vector messages) {

        if (margincallconfig.getContractType().equals(INITIALMARGIN)) {
            Vector<String> accounts = CollateralUtilities.getDomainValues(IM_ACCOUNT);
            int po = 0;
            if (Util.isNumber(Integer.toString(margincallconfig.getPoId()))) {
                po = margincallconfig.getPoId();
            }

            if (accounts.size() > 0) {
                String nameAccount = accounts.get(0);
                Account accountId = BOCache.getAccount(DSConnection.getDefault(), nameAccount);
                Account accountPO = BOCache.getAccount(DSConnection.getDefault(), nameAccount, po, "ANY");

                if (accountPO != null && Util.isNumber(Integer.toString(accountPO.getId()))) {
                    margincallconfig.setAccountId(accountPO.getId());
                }

                if (accountPO == null && Util.isNumber(Integer.toString(accountId.getId()))) {
                    margincallconfig.setAccountId(accountId.getId());
                }

                margincallconfig.setInitialMargin(true);

                if (Util.isEmpty(margincallconfig.getContractGroup())) {
                    margincallconfig.setContractGroup(CollateralConfig.MARGIN_TYPE_IM);
                }
            }
        }

        if ((margincallconfig.getContractType().equals(CSA_FACADE))) {
            margincallconfig.setWorkflowSubtype(CollateralConfig.SUBTYPE_FACADE);

            if (Util.isEmpty(margincallconfig.getContractGroup())) {
                margincallconfig.setContractGroup(CollateralConfig.MARGIN_TYPE_IM);
            }
        }

        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean checkInitialMarginType(CollateralConfig margincallconfig, Frame frame, Vector messages) {
        Vector domainValues = null;
        if (margincallconfig.getContractType().equals(INITIALMARGIN)
                && (Util.isEmpty(margincallconfig.getAdditionalField(IM_INITIAL_MARGIN_TYPE)))) {
            messages.add("\nAdditional field IM_CSD_TYPE is empty, should be CTPY or PO.\n");
            return false;
        }

        try {
            domainValues = DSConnection.getDefault().getRemoteReferenceData().getDomainValues("IM_BLOCK_MSG_CSD_CPTY");
        } catch (CalypsoServiceException e) {
            Log.error(this, "Couldn't load DV IM_BLOCK_MSG_CSD_CPTY Error: " + e.getMessage());
            Log.error(this, e); // sonar
            return false;
        }

        if (!Util.isEmpty(domainValues) && !Util.isEmpty(margincallconfig.getProcessingOrg().getCode())
                && domainValues.contains(margincallconfig.getProcessingOrg().getCode())
                && margincallconfig.getContractType().equals(INITIALMARGIN)
                && (margincallconfig.getAdditionalField(IM_INITIAL_MARGIN_TYPE).equals(INITIALMARGINTYPE))) {
            margincallconfig.setAdditionalField("BLOCK_MESSAGE_PAY_CASH", VALUE_INITIAL_MARGIN_TYPE);
            margincallconfig.setAdditionalField("BLOCK_MESSAGE_PAY_SECURITIES", VALUE_INITIAL_MARGIN_TYPE);
            margincallconfig.setAdditionalField("BLOCK_MESSAGE_PAY_SECURITIES_EQTY", VALUE_INITIAL_MARGIN_TYPE);
            margincallconfig.setAdditionalField("BLOCK_MESSAGE_RECEIVE_CASH", VALUE_INITIAL_MARGIN_TYPE);
            margincallconfig.setAdditionalField("BLOCK_MESSAGE_RECEIVE_SECURITIES", VALUE_INITIAL_MARGIN_TYPE);
            margincallconfig.setAdditionalField("BLOCK_MESSAGE_RECEIVE_SECURITIES_EQTY", VALUE_INITIAL_MARGIN_TYPE);

        }
        return true;
    }

    @SuppressWarnings("rawtypes")
    private boolean setUpIMAdditionalFields(CollateralConfig margincallconfig, Frame frame, Vector messages) {
        boolean result = true;

        if (GROUP_IM.equalsIgnoreCase(margincallconfig.getContractGroup())) {
            if (!CollateralConfig.SUBTYPE_FACADE.equalsIgnoreCase(margincallconfig.getSubtype())) {

                result = false;
                Map<String, String> additionalFields = margincallconfig.getAdditionalFields();
                SantIMAdditionalInfoFrame keywordFrame = null;
                Map<String, String> fields = new HashMap<String, String>();

                fields.put(IM_GLOBAL_ID, margincallconfig.getAdditionalField(IM_GLOBAL_ID));

                if (CSA_CONTRACT_TYPE.equalsIgnoreCase(margincallconfig.getContractType())) {
                    fields.putAll(getImCalculationFields(additionalFields));
                }

                //new MTA_THRESHOLD_FIXING Values
                addMtaThresholdFixing(fields, margincallconfig);

                keywordFrame = new SantIMAdditionalInfoFrame(frame, margincallconfig, fields, new Vector<String>());

                if (keywordFrame != null) {
                    keywordFrame.setVisible(true);
                    String buttonSelected = keywordFrame.buttonSelected;

                    if (!Util.isEmpty(buttonSelected)) {
                        result = buttonSelected.equals(keywordFrame.okButton.getText());
                    }
                }

            } else if (CollateralConfig.SUBTYPE_FACADE.equalsIgnoreCase(margincallconfig.getSubtype())
                    && margincallconfig.getId() == 0) {
                margincallconfig.setAdditionalField("IM_SUB_CONTRACTS", "");
            }
        }

        return result;
    }

    /**
     * @param additionalFields
     * @return
     */
    private Map<String, String> getImCalculationFields(Map<String, String> additionalFields) {
        Map<String, String> imFields = new HashMap<String, String>();

        for (Map.Entry<String, String> field : additionalFields.entrySet()) {
            String key = field.getKey();

            // im_calculation_method_default should not appear on the pup-up
            if (!IM_CALCULATION_METHOD_DEFAULT.equalsIgnoreCase(key) && key.contains(IM_CALCULATION_METHOD)) {
                imFields.put(key, field.getValue());
            }
        }

        return imFields;
    }

    @SuppressWarnings("rawtypes")
    private boolean setUpVmSwitchDate(CollateralConfig margincallconfig, Frame frame, Vector messages) {
        boolean result = true;

        String imVmSwitchDate = margincallconfig.getAdditionalField(IM_VM_SWITCH_DATE_AF);

        if (!Util.isEmpty(imVmSwitchDate)) {
            JDate switchDate = JDate.valueOf(imVmSwitchDate);
            // -1 day to get previous day at 23:59:59
            switchDate = switchDate.addBusinessDays(-1, null);
            JDatetime switchDatetime = switchDate.getJDatetime(TimeZone.getDefault());

            String sdfName = "MAIN_" + margincallconfig.getName();
            StaticDataFilter sdf = null;
            try {
                sdf = DSConnection.getDefault().getRemoteReferenceData().getStaticDataFilter(sdfName);
            } catch (CalypsoServiceException e1) {
                Log.error(this, "Cannot load " + sdfName);
                Log.error(this, e1); // sonar
            }

            if (sdf != null && CSA_CONTRACT_TYPE.equalsIgnoreCase(margincallconfig.getContractType())) {
                StaticDataFilter newSdf = sdf.clone();
                result = false;

                Vector<StaticDataFilterElement> elements = sdf.getElements();
                Vector<StaticDataFilterElement> newElements = new Vector<StaticDataFilterElement>();

                // remove value Date filter if exists
                for (StaticDataFilterElement element : elements) {
                    if (!TRADE_DATE_ELEMENT.equalsIgnoreCase(element.getName())) {
                        newElements.add(element);
                    }
                }

                StaticDataFilterElement valueDate = new StaticDataFilterElement();
                valueDate.setName(TRADE_DATE_ELEMENT);
                valueDate.setParentName(sdfName);
                valueDate.setOperatorType(SDFilterOperatorType.DATETIME_RANGE);

                if (GROUP_IM.equalsIgnoreCase(margincallconfig.getContractGroup())) {
                    // modify MAIN_ SDF with Min date
                    valueDate.setMaxValue(null);
                    valueDate.setMinValue(switchDatetime);
                    valueDate.setMinInclusive(true);
                } else {
                    valueDate.setMinValue(null);
                    valueDate.setMaxValue(switchDatetime);
                }

                // save sdf
                try {
                    newElements.add(valueDate);
                    newSdf.setElements(newElements);
                    DSConnection.getDefault().getRemoteReferenceData().save(newSdf);

                    result = true;
                } catch (CalypsoServiceException e) {
                    StringBuffer msg = new StringBuffer("Couldn't save SDF [");
                    msg.append(sdfName).append("]: ").append(e.getMessage());

                    Log.error(this, msg.toString());
                    Log.error(this, e); // sonar
                    result = false;
                }
            }
        }

        return result;
    }

    /**
     * Introduces the EMIR collateral logic based on IM configuration into the
     * additional field EMIR_COLLATERAL_VALUE. Options are Fully, Partially or
     * OneWay, Uncollateralized
     *
     * @param contract
     */

    private static void updateEMIRCollateralValue(CollateralConfig contract) {

        if (contract == null)
            return;

        if (!isContractTypeIsCSD(contract))
            return;

        CollateralConfig facade = getFacade(contract);
        if (facade != null) {
            CollateralConfig csd = getCSDfromFacade(facade);
            if (csd == null) {
                contract.setAdditionalField(EMIR_COLLATERAL_VALUE, PARTIALLY);
            } else {
                String direction = csd.getContractDirection();
                if (!Util.isEmpty(direction)) {
                    if (direction.equals(CollateralConfig.NET_BILATERAL)) {
                        contract.setAdditionalField(EMIR_COLLATERAL_VALUE, FULLY);
                    } else if (direction.equals(CollateralConfig.NET_UNILATERAL)) {
                        contract.setAdditionalField(EMIR_COLLATERAL_VALUE, ONE_WAY);
                    } else {
                        contract.setAdditionalField(EMIR_COLLATERAL_VALUE, PARTIALLY);
                    }
                }
            }
        } else {
            contract.setAdditionalField(EMIR_COLLATERAL_VALUE, PARTIALLY);
        }
    }


    private static CollateralConfig getCSDfromFacade(CollateralConfig facade) {

        if (CSA_FACADE.equals(facade.getContractType())) {
            if (!Util.isEmpty(facade.getAdditionalField(IM_SUB_CONTRACTS))) {
                String[] ids = facade.getAdditionalField(IM_SUB_CONTRACTS).split(",");
                List<Integer> contractIds = new ArrayList<Integer>();
                for (String id : ids) {
                    contractIds.add(Integer.valueOf(id));
                }

                MarginCallConfigFilter mcFilter = new MarginCallConfigFilter();
                mcFilter.setContractIds(contractIds);
                List<CollateralConfig> listCC = loadCollConfigFromFilter(mcFilter);

                for (CollateralConfig cc : listCC) {
                    if (CSD_CONTRACT_TYPE.equals(cc.getContractType())) {
                        return cc;
                    }
                }
            }
        }
        return null;
    }

    private static CollateralConfig getFacade(CollateralConfig colConfig) {
        String globalIdString = colConfig.getAdditionalField(IM_GLOBAL_ID);
        CollateralConfig facade = null;
        if (!Util.isEmpty(globalIdString)) {
            int globalId = Integer.parseInt(globalIdString);
            try {
                facade = ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfig(globalId);
            } catch (CollateralServiceException e) {
                Log.error(CustomCollateralConfigValidator.class,
                        "Could not get FACADE contract with ID: " + globalIdString + "\n" + e);
            }
        }
        return facade;
    }

    private static List<CollateralConfig> loadCollConfigFromFilter(MarginCallConfigFilter mcFilter) {
        List<CollateralConfig> listCC = new ArrayList<>();
        try {
            listCC = CollateralManagerUtil.loadCollateralConfigs(mcFilter);
        } catch (CollateralServiceException e) {
            Log.error(CustomCollateralConfigValidator.class, e); //sonar
        }
        return listCC;
    }

    private static boolean isContractTypeIsCSD(CollateralConfig cc) {
        String contractType = cc.getContractType();
        return  CSD_CONTRACT_TYPE.equals(contractType);
    }

    private void addMtaThresholdFixing(Map<String, String> fields, CollateralConfig margincallconfig) {
        if (TRUE.equalsIgnoreCase(margincallconfig.getAdditionalField(MTA_THRESHOLD_FIXING))) {
            fields.put(MTA_EUR, margincallconfig.getAdditionalField(MTA_EUR));
            fields.put(MTA_USD, margincallconfig.getAdditionalField(MTA_USD));
            fields.put(THRESHOLD_EUR, margincallconfig.getAdditionalField(THRESHOLD_EUR));
            fields.put(THRESLHOD_USD, margincallconfig.getAdditionalField(THRESLHOD_USD));
        }
    }

    private Vector<String> getContractNameCharacterExcluded() {
        Vector<String> dv = null;
        try {
            dv = DSConnection.getDefault().getRemoteReferenceData().getDomainValues("ContractNameCharsExcluded");
        } catch (CalypsoServiceException e) {
            Log.system(this, "Cant find the domain value: ContractNameCharsExcluded", e);
        }
        return dv;
    }

    private boolean checkExcludeCharacters(CollateralConfig marginCallConfig, Vector messages) {
        Vector<String> characterExcluded = getContractNameCharacterExcluded();
        if (null != characterExcluded) {
            for (String character : characterExcluded) {
                if (marginCallConfig.getName().contains(character)) {
                    messages.add("Contract`s name can't contain any characters like " + characterExcluded.toString());
                    return false;
                }
            }
        }
        return true;
    }

}