package calypsox.tk.report;

import calypsox.tk.core.SantanderUtil;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.ELBEandKGRutilities;
import com.calypso.tk.bo.InventoryCashPosition;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.collateral.filter.CollateralFilterProxy;
import com.calypso.tk.collateral.filter.impl.CachedCollateralFilterProxy;
import com.calypso.tk.collateral.optimization.candidat.CollateralCandidate;
import com.calypso.tk.collateral.service.CollateralServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.CollateralConfigCurrency;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.BOPositionUtil;
import com.calypso.tk.util.InventorySecurityPositionArray;
import com.calypso.tk.util.MarginCallPositionUtil;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class OptCustAgreementParamsLogic {
    private static final String MASTER_AGREEMENT = "MASTER_AGREEMENT";
    private static final String MA_SIGN_DATE = "MA_SIGN_DATE";
    private static final String NONE = "None";
    private static final String LEGAL_ENTITY = "Legal Entity";
    private static final String HEADER = "D01StdI#BATCHGLCSDMIL  225";
    private static final String TRANSACTION_TYPE_SEC = "COLLBOND";
    private static final String RECONCILIATION_TYPE = "P";
    private static final String ACTION = "A";
    private static final String TRANSACTION_TYPE = "COLL";
    // Constants used.
    private static final String BLANK = "";
    private static final String HEAD_CLONE = "HEAD_CLONE";
    private final String COLLATERAL_TYPE_CASH = "CASH";
    private final String COLLATERAL_TYPE_SEC = "SECURITY";
    private final String COLLATERAL_TYPE_BOTH = "BOTH";
    private static final String INSTRUMENT = "Instrument";
    private static final String OWNER = "Owner";
    private static final String COUNTERPARTY = "Counterparty";
    private static final String TRANSACTIONID = "Transaction ID";
    private static final String ISIN = "ISIN";
    private static final String CONCILIA_FIELD = "MAD_CAL_COL";
    private static final String GLOBAL_RATING = "GLOBAL RATING";
    private static final String CUSTOMIZED = "Customized";
    private static final String CONTRACT_IA = "CONTRACT_IA";
    private static final String DISPUTE_ADJ = "DISPUTE_ADJUSTMENT";
    private static final String CONTRACT_INDEPENDENT_AMOUNT = "CONTRACT_INDEPENDENT_AMOUNT";
    private static final String FIRST_CALC_DATE = "FIRST_CALC_DATE";
    private static final String PERCENT = "PERCENT";
    private static final String PO = "ProcessingOrg";

    // added
    private static final String SOURCE = "CALYPSO";
    private static final String USE_LONG_T_MOODY = "UseLongTermMoody";
    private static final String USE_LONG_T_FITCH = "UseLongTermFitch";
    private static final String USE_LONG_T_SP = "UseLongTermS&P";

    private final int _poId;
    private final int _leId;

    // added
    private static JDate processDate, valueDate;

    public OptCustAgreementParamsLogic(final CollateralConfig marginCall) {
        this._poId = marginCall.getPoId();
        this._leId = marginCall.getLeId();
        // save extraction date
        processDate = JDate.getNow();
        valueDate = processDate.addBusinessDays(-1, Util.string2Vector("SYSTEM"));
    }

    @SuppressWarnings("unused")
    private OptCustAgreementParamsItem getKGR_Collateral_MarginCallItem(final Vector<String> errors) {
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
    public static Vector<OptCustAgreementParamsItem> getReportRows(final CollateralConfig marginCall,
                                                                   final MarginCallPositionUtil marginCallPosition, final JDate jdate, final DSConnection dsConn,
                                                                   final Vector<String> errorMsgs, final String[] columns, final PricingEnv pricingEnv,
                                                                   final CollateralServiceRegistry collServReg, Vector holidaysVector) throws RemoteException {

        final Vector<OptCustAgreementParamsItem> reportRows = new Vector<OptCustAgreementParamsItem>();
        final OptCustAgreementParamsLogic verifiedRow = new OptCustAgreementParamsLogic(marginCall);
        InventoryCashPosition inventoryCashPosition = null;
        InventorySecurityPositionArray inventorySecurityPositionArray = null;
        OptCustAgreementParamsItem rowCreated = null;

        // save extraction date
        processDate = jdate;
        valueDate = processDate.addBusinessDays(-1, holidaysVector);

        // Vector of current currency list.
        final List<CollateralConfigCurrency> currencies = marginCall.getEligibleCurrencies(); // 777
        // -
        // Get
        // eligible
        // currencies
        // (all
        // contract
        // currencies)

        // We check the number of columns to show the correct result.
        if (columns.length > 4) {
            // We check if the template selected is related to 'Positions' (Cash
            // or Security).
            if (columns[1].equals(TRANSACTIONID)) {
                // Security
                if (columns[11].toUpperCase().equals(ISIN)) {
                    // get security positions for a contract
                    inventorySecurityPositionArray = getSecurityPositions(jdate, marginCall, dsConn); // #2-778
                    if (inventorySecurityPositionArray != null) {
                        for (int secPos = 0; secPos < inventorySecurityPositionArray.size(); secPos++) {
                            // don't show positions with balance=0
                            if (!verifiedRow.isBalanceZero(inventorySecurityPositionArray.get(secPos))) {
                                rowCreated = verifiedRow.getKGR_Collateral_SecurityPositions(marginCall,
                                        inventorySecurityPositionArray.get(secPos), dsConn, errorMsgs, pricingEnv);
                                if (null != rowCreated) { // If the result row
                                    // is equals
                                    // to NULL, we don't add this
                                    // row to the report.
                                    reportRows.add(rowCreated);
                                }
                            }
                        }
                    }

                } else {
                    // Cash
                    if ((currencies != null) && (currencies.size() > 0)) {

                        // First of all, we retrieve the different cash
                        // positions.
                        for (int cash = 0; cash < currencies.size(); cash++) {

                            inventoryCashPosition = BOPositionUtil.getCashPosition((currencies.get(cash)).getCurrency(),
                                    "MARGIN_CALL", "ACTUAL", "TRADE", jdate, marginCall.getId(), dsConn, null); // 777
                            // -
                            // Changed
                            // second
                            // parameter
                            // to
                            // ACTUAL
                            // We retrieve each row to show in the CSV file or
                            // the report.
                            rowCreated = verifiedRow.getKGR_Collateral_CashPositions(marginCall, inventoryCashPosition,
                                    dsConn, errorMsgs);
                            if (null != rowCreated) { // If the result row is
                                // equals to NULL, we
                                // don't add this row to
                                // the report.

                                reportRows.add(rowCreated);
                            }
                        }
                    } else if (marginCall.getCurrency() != null) {
                        final String currency = marginCall.getCurrency();
                        inventoryCashPosition = BOPositionUtil.getCashPosition(currency, "MARGIN_CALL", "ACTUAL",
                                "TRADE", jdate, marginCall.getId(), dsConn, null);
                        rowCreated = verifiedRow.getKGR_Collateral_CashPositions(marginCall, inventoryCashPosition,
                                dsConn, errorMsgs);
                        if (null != rowCreated) { // If the result row is
                            // equals to NULL, we
                            // don't add this row to
                            // the report.

                            reportRows.add(rowCreated);
                        }

                    }

                }
            } else {
                rowCreated = verifiedRow.getKGR_Collateral_MarginCallItem(marginCall, dsConn, errorMsgs);
                if (null != rowCreated) { // If the result row is equals to
                    // NULL, we don't add this row to the
                    // report.
                    reportRows.add(rowCreated);
                }
            }
        } else {
            // We check if the selected report is related to 'Instruments',
            // 'Counterparty', or 'ProcessingOrg'.
            if (columns[1].equals(INSTRUMENT)) {
                // Obtain product list
                final Vector elements = marginCall.getProductList();
                ArrayList<String> instrumentTypesProcessed = new ArrayList<String>();
                if (elements != null) {
                    for (int i = 0; i < elements.size(); i++) {
                        // if product equals "CollateralExposure" obtain
                        // exposure types
                        if (elements.get(i).equals("CollateralExposure")) {

                            final List exposureTypes = marginCall.getExposureTypeList();
                            if (exposureTypes != null) {
                                for (int j = 0; j < exposureTypes.size(); j++) {

                                    // get mapped value
                                    String mappedExposureType = CollateralUtilities
                                            .initMappingInstrumentValues(dsConn, "GBO").get(exposureTypes.get(j));
                                    if (mappedExposureType != null) {

                                        // filter
                                        if (mappedExposureType.contains(CUSTOMIZED)) {
                                            mappedExposureType = "Caps and Floors";
                                        } else {
                                            mappedExposureType = mappedExposureType.substring(0,
                                                    mappedExposureType.indexOf('-'));
                                        }

                                        // check for not repeat instruments with
                                        // same mapping type
                                        if (!instrumentTypesProcessed.contains(mappedExposureType)) { // 771
                                            // -
                                            // Use
                                            // a
                                            // filter
                                            // to
                                            // not
                                            // repeat
                                            // types
                                            instrumentTypesProcessed.add(mappedExposureType);
                                            // filter
                                            if ((!mappedExposureType.equals(CONTRACT_IA))
                                                    && (!mappedExposureType.equals(DISPUTE_ADJ))) {
                                                reportRows.add(verifiedRow.getKGR_Collateral_InstrumentsItem(marginCall,
                                                        dsConn, mappedExposureType, errorMsgs));
                                            }

                                        }

                                    }
                                    // If this instrument cannot be found in the
                                    // domain values, use the key we are looking
                                    // for directly.
                                    else {
                                        mappedExposureType = (String) exposureTypes.get(j);
                                        // filter
                                        // filter
                                        if ((!mappedExposureType.equals(CONTRACT_IA))
                                                && (!mappedExposureType.equals(DISPUTE_ADJ))) {
                                            reportRows.add(verifiedRow.getKGR_Collateral_InstrumentsItem(marginCall,
                                                    dsConn, mappedExposureType, errorMsgs));
                                        }
                                    }

                                }
                            } else {
                                // add product to report
                                reportRows.add(verifiedRow.getKGR_Collateral_InstrumentsItem(marginCall, dsConn,
                                        elements.get(i), errorMsgs));
                            }
                        } else {

                            // add product to report
                            reportRows.add(verifiedRow.getKGR_Collateral_InstrumentsItem(marginCall, dsConn,
                                    elements.get(i), errorMsgs));

                        }
                    }
                }
                // }
            } else if (columns[1].equals(OWNER)) {

                int branchesFlag = 0;

                // 1. some braches activated
                StaticDataFilter sdf = dsConn.getRemoteReferenceData()
                        .getStaticDataFilter("UND_" + marginCall.getName());
                if (sdf != null) {
                    Vector<StaticDataFilterElement> elements = sdf.getElements();
                    if (elements != null) {
                        for (StaticDataFilterElement sdfe : elements) {
                            if (sdfe.getName().equals(PO)) {
                                Vector<String> values = sdfe.getValues();
                                if (values != null) {
                                    for (String value : values) {
                                        LegalEntity po = dsConn.getRemoteReferenceData().getLegalEntity(value);
                                        if (po != null) {
                                            rowCreated = verifiedRow.getKGR_Collateral_ProcessingOrgItem(marginCall, po,
                                                    dsConn, errorMsgs);
                                            branchesFlag = 1;
                                            if (null != rowCreated) {
                                                reportRows.add(rowCreated);
                                            }
                                        }

                                    }
                                }
                            }
                        }
                    }
                }
                if (branchesFlag == 0) {
                    // 2. all branches activated
                    // This set is used to check that no PO is included twice.
                    final Set<LegalEntity> includedProcessingOrgs = new TreeSet<LegalEntity>();
                    // line for po owner
                    rowCreated = verifiedRow.getKGR_Collateral_ProcessingOrgItem(marginCall,
                            marginCall.getProcessingOrg(), dsConn, errorMsgs);
                    if (null != rowCreated) {
                        reportRows.add(rowCreated);
                        includedProcessingOrgs.add(marginCall.getProcessingOrg());
                    }
                    // lines for associated pos
                    if ((marginCall.getAdditionalPO() != null) && (marginCall.getAdditionalPO().size() > 0)) {
                        final List<LegalEntity> additionalPO = marginCall.getAdditionalPO();
                        for (int i = 0; i < additionalPO.size(); i++) {
                            final LegalEntity po = additionalPO.get(i);
                            if (po != null) {
                                if (!includedProcessingOrgs.contains(po)) {
                                    rowCreated = verifiedRow.getKGR_Collateral_ProcessingOrgItem(marginCall, po, dsConn,
                                            errorMsgs);
                                    if (null != rowCreated) {
                                        reportRows.add(rowCreated);
                                        includedProcessingOrgs.add(po);
                                    }
                                }
                            }
                        }
                    }
                }

            } else if (columns[1].equals(COUNTERPARTY)) {

                // This set is used to check that no Counterparty is included
                // twice.
                final Set<LegalEntity> includedCounterparties = new TreeSet<LegalEntity>();

                rowCreated = verifiedRow.getKGR_Collateral_CounterpartyItem(marginCall, marginCall.getLegalEntity(),
                        dsConn, errorMsgs);
                if (null != rowCreated) { // If the result row is
                    // equals to
                    // NULL, we don't add this row to the
                    // report.
                    reportRows.add(rowCreated);
                    includedCounterparties.add(marginCall.getLegalEntity());
                }

                if ((marginCall.getAdditionalLE() != null) && (marginCall.getAdditionalLE().size() > 0)) {
                    final List<LegalEntity> additionalLE = marginCall.getAdditionalLE();
                    for (int i = 0; i < additionalLE.size(); i++) {
                        final LegalEntity le = additionalLE.get(i);
                        if (!includedCounterparties.contains(le)) {
                            rowCreated = verifiedRow.getKGR_Collateral_CounterpartyItem(marginCall, le, dsConn,
                                    errorMsgs);
                            if (null != rowCreated) { // If the result row is
                                // equals
                                // to
                                // NULL, we don't add this row to the
                                // report.
                                reportRows.add(rowCreated);
                                includedCounterparties.add(le);
                            }
                        }
                    }
                }
            }
        }

        return reportRows;
    }

    /**
     * General characteristics of contracts and counterparties
     *
     * @param marginCall
     * @param dsConn
     * @param errors
     * @return
     */
    // GSM: 0992. We must show every contract, even if the LE is not configured
    // properly
    public OptCustAgreementParamsItem getKGR_Collateral_MarginCallItem(CollateralConfig marginCall,
                                                                       final DSConnection dsConn, final Vector<String> errors) {
        final OptCustAgreementParamsItem marginCallItem = new OptCustAgreementParamsItem();
        String value = "";
        Vector<String> agencies = null;
        Vector<CreditRating> ownerCreditRatings = new Vector<CreditRating>();
        Vector<CreditRating> cptyCreditRatings = new Vector<CreditRating>();
        double thresholdOwner = 0.00;
        double mtaOwner = 0.00;
        double thresholdCpty = 0.00;
        double mtaCpty = 0.00;
        boolean calculateOwnerValues = true, calculateCptyValues = true;

        // GSM: 19/02/2014. New Optimization fields

        // copy of the CollateralConfig
        marginCallItem.setCollateralConfig(marginCall);
        // haircutRuleName
        marginCallItem.setHaircutRuleName(getHairCutRuleName(marginCall));
        // rehypothecable
        marginCallItem.setRehypothecable(getRehypothecable(marginCall));
        // useLongTermS&P
        marginCallItem.setLongTermSP(getLongTermSP(marginCall));
        // useLongTermFitch
        marginCallItem.setLongTermFitch(getLongTermFitch(marginCall));
        // useLongTermMoody
        marginCallItem.setLongTermMoody(getLongTermMoody(marginCall));
        // eligible Agencies
        marginCallItem.setEligibleAgencies(getElibigleAgencies(marginCall));
        // ctpy MTA type
        marginCallItem.setCtpyMtaType(getCtpyMTAType(marginCall));
        // ctpy MTA ccy
        marginCallItem.setCtpyMtaCcy(getCtpyMTACcy(marginCall));
        // owner MTA type
        marginCallItem.setOwnerMtaType(getOwnerMTAType(marginCall));
        // owner MTA ccy
        marginCallItem.setOwnerMtaCcy(getOwnerMTACcy(marginCall));
        // ctpy threshold type
        marginCallItem.setCtpyThresholdType(getCtpyThresholdType(marginCall));
        // owner threshold type
        marginCallItem.setOwnerThresholdType(getOwnerThresholdType(marginCall));
        // initial margin ccy
        marginCallItem.setInitialMarginCcy(marginCall.getCurrency());
        // tolerance amount ccy
        marginCallItem.setToleranceAmountCcy(marginCall.getCurrency());
        // Owner Cash Offset
        marginCallItem.setOwnerCashOffset(getOwnerCashOffset(marginCall));
        // Ctpy Cash Offset
        marginCallItem.setCtpyCashOffset(getCtpyCashOffset(marginCall));
        // Owner Security Offset
        marginCallItem.setOwnerSecurityOffset(getOwnerSecurityOffset(marginCall));
        // Ctpy Security Offset
        marginCallItem.setCtpySecurityOffset(getCtpySecurityOffset(marginCall));
        // Book
        marginCallItem.setBook(marginCall.getBook().getName());
        // EOD Pricing Env
        marginCallItem.setEODPricingEnvironment(marginCall.getPricingEnvName());
        // Intraday Pricing Env
        marginCallItem.setIntradayPricingEnvironment(marginCall.getIntradayPricingEnvName());

        // GSM, end 19/02/2014

        // mcc
        marginCallItem.setMarginCallContract(getMarginCallContract(marginCall));

        // owner
        marginCallItem.setOwner(getOwner(marginCall, dsConn));

        // owner name
        marginCallItem.setOwnerName(getOwnerName(marginCall, dsConn));

        // counterparty
        marginCallItem.setCounterparty(getCounterparty(marginCall, dsConn));

        // contract type
        marginCallItem.setContractType(getContractType(marginCall));

        // masterAgreeDescription
        if (marginCall.getAdditionalField("MASTER_AGREEMENT_TYPE") != null) {
            marginCallItem.setMasterAgreeDescription(getMasterAgreeDescription(marginCall, dsConn));
        }

        // contract currency
        marginCallItem.setContractCcy(getContractCcy(marginCall));

        // get contract rating agencies
        agencies = marginCall.getEligibleAgencies();

        // Exclude from Optimizer
        marginCallItem.setExcludeFromOptimizer(getExcludeFromOptimizer(marginCall));

        /**************** OWNER stuff *****************/

        if (ELBEandKGRutilities.isOwnerKGRcontractDependingOnRating(marginCall)) {
            // get owner credit ratings
            MarginCallCreditRatingConfiguration mccRatingConfigOwner = null;
            try {
                mccRatingConfigOwner = CollateralUtilities.getMCRatingConfiguration(marginCall.getPoRatingsConfigId());
                ownerCreditRatings = ELBEandKGRutilities.getCreditRatingsForLE(marginCall, agencies,
                        marginCall.getProcessingOrg().getId(), valueDate, mccRatingConfigOwner.getRatingType());
                if (Util.isEmpty(ownerCreditRatings)) {
                    Log.error(this, "Cannot get contract agencies ratings for owner");
                    thresholdOwner = mtaOwner = 0.0;
                    calculateOwnerValues = false;
                }
            } catch (Exception e) {
                Log.error("Cannot get PO ratingMatrix for contract = " + marginCall.getName(), e);
                thresholdOwner = mtaOwner = 0.0;
                calculateOwnerValues = false;
            }
        }

        if (calculateOwnerValues) {
            if (!marginCall.getPoNewThresholdType().equals(CollateralConfig.GLOBAL_RATING)) {
                thresholdOwner = getOwnerThresholdCcy(marginCall, ownerCreditRatings);
                setThresholdOwner(marginCallItem, marginCall, thresholdOwner);
            } else {
                // thresholdOwner = getRatingThresholdAmount(marginCall,
                // processDate, marginCall.getPoRatingsConfigId(),
                // marginCall.getPoThresholdRatingDirection(),
                // marginCall.getPoId());
                marginCallItem.setThresholdOwner(
                        getRatingThresholdAmount(marginCall, processDate, marginCall.getPoRatingsConfigId(),
                                marginCall.getPoThresholdRatingDirection(), marginCall.getPoId()));
            }
            if (!marginCall.getPoMTAType().equals(CollateralConfig.GLOBAL_RATING)) {
                mtaOwner = getOwnerMtaCcy(marginCall, ownerCreditRatings);
            } else {
                mtaOwner = getRatingMtaAmount(marginCall, processDate, marginCall.getPoRatingsConfigId(),
                        marginCall.getPoMTARatingDirection(), marginCall.getPoId());
            }
        }

        setMtaOwner(marginCallItem, marginCall, mtaOwner);

        /**************** OWNER stuff *****************/

        /**************** CPTY stuff ******************/

        if (ELBEandKGRutilities.isCptyKGRcontractDependingOnRating(marginCall)) {
            // get cpty credit ratings
            MarginCallCreditRatingConfiguration mccRatingConfigCpty = null;
            try {

                mccRatingConfigCpty = CollateralUtilities.getMCRatingConfiguration(marginCall.getLeRatingsConfigId());
                cptyCreditRatings = ELBEandKGRutilities.getCreditRatingsForLE(marginCall, agencies,
                        marginCall.getLegalEntity().getId(), valueDate, mccRatingConfigCpty.getRatingType());
                if (Util.isEmpty(cptyCreditRatings)) {
                    Log.error(this, "Cannot get contract agencies ratings for cpty");
                    thresholdCpty = mtaCpty = 0.0;
                    calculateCptyValues = false;
                }
            } catch (Exception e) {
                Log.error("Cannot get Cpty ratingMatrix for contract = " + marginCall.getName(), e);
                thresholdCpty = mtaCpty = 0.0;
                calculateCptyValues = false;
            }
        }

        if (calculateCptyValues) {
            if (!CollateralConfig.GLOBAL_RATING.equals(marginCall.getLeNewThresholdType())) {
                thresholdCpty = getCptyThresholdCcy(marginCall, cptyCreditRatings);
                setThresholdCpty(marginCallItem, marginCall, thresholdCpty);

            } else {
                // thresholdCpty = getRatingThresholdAmount(marginCall,
                // processDate, marginCall.getLeRatingsConfigId(),
                // marginCall.getLeThresholdRatingDirection(),
                // marginCall.getLeId());
                marginCallItem.setThresholdCpty(
                        getRatingThresholdAmount(marginCall, processDate, marginCall.getLeRatingsConfigId(),
                                marginCall.getLeThresholdRatingDirection(), marginCall.getLeId()));
            }

            if (!CollateralConfig.GLOBAL_RATING.equals(marginCall.getLeMTAType())) {
                mtaCpty = getCptyMtaCcy(marginCall, cptyCreditRatings);
            } else {
                mtaCpty = getRatingMtaAmount(marginCall, processDate, marginCall.getLeRatingsConfigId(),
                        marginCall.getLeMTARatingDirection(), marginCall.getLeId());
            }
        }

        setMtaCpty(marginCallItem, marginCall, mtaCpty);

        /**************** CPTY stuff ******************/

        // INDEPENDENT AMOUNT for both
        // indAmountOWNER
        double indAmountOwner = 0.00;
        if (!CollateralConfig.GLOBAL_RATING.equals(marginCall.getPoIAType())) {
            indAmountOwner = getOwnerIndAmountCcy(marginCall, ownerCreditRatings);
        } else {
            indAmountOwner = getRatingIAAmount(marginCall, processDate, marginCall.getPoRatingsConfigId(),
                    marginCall.getPoIARatingDirection(), marginCall.getPoId());
        }

        // indAmountCPTY
        double indAmountCpty = 0.00;
        if (!CollateralConfig.GLOBAL_RATING.equals(marginCall.getLeIAType())) {
            indAmountCpty = getCptyIndAmountCcy(marginCall, cptyCreditRatings);
        } else {
            indAmountCpty = getRatingIAAmount(marginCall, processDate, marginCall.getLeRatingsConfigId(),
                    marginCall.getLeIARatingDirection(), marginCall.getLeId());
        }

        setIndependentAmount(marginCallItem, marginCall, indAmountOwner, indAmountCpty);

        // initial margin
        value = CollateralUtilities.formatNumber(getInitialMargin(marginCall, dsConn));

        if (value.contains(".")) {
            marginCallItem.setInitialMargin(value.substring(0, value.indexOf('.')));
        }
        if (value.contains(",")) {
            marginCallItem.setInitialMargin(value.substring(0, value.indexOf(',')));
        }

        // calculation period
        // final String calcPeriod =
        // CollateralUtilities.converseCalcPeriodKGRContracts(getCalcPeriod(marginCall));
        final String calcPeriod = marginCall.getAdditionalField("FREQUENCY");
        marginCallItem.setCalcPeriod(calcPeriod);

        // first calculation date
        marginCallItem.setFirstCalcDate(getFirstCalcDate(marginCall));

        // asset type
        final String assetType = CollateralUtilities.converseAssetTypeKGRContracts(getAssetType(marginCall));
        marginCallItem.setAssetType(assetType);

        // one way
        marginCallItem.setOneWay(getOneWay(marginCall)); // 772

        // head/clone
        final String headClone = CollateralUtilities.converseHeadCloneKGRContracts(getHeadClone(marginCall));
        marginCallItem.setHeadClone(headClone);

        // contract name
        marginCallItem.setContractLongName(getContractLongName(marginCall, dsConn));

        // masterAgreementShortName only filled if Master Agreement is
        // filled in Margin Call Contract
        if (null != marginCall.getAdditionalField(MASTER_AGREEMENT)) {
            marginCallItem.setMasterAgreementShortName(getMasterAgreementShortName(marginCall, dsConn));
        }

        // not used
        marginCallItem.setNotUsed(getNotUsed());

        // master signed date
        marginCallItem.setMasterSignedDate(getMasterSignedDate(marginCall));

        // delivery rounding
        marginCallItem.setDeliveryRounding(getDeliveryRounding(marginCall));

        // delivery rounding cpty
        value = CollateralUtilities.formatNumber(getDeliveryRoundingCpty(marginCall));
        if (value.contains(".")) {
            marginCallItem.setDeliveryRoundingCpty(value.substring(0, value.indexOf('.')));
        }
        if (value.contains(",")) {
            marginCallItem.setDeliveryRoundingCpty(value.substring(0, value.indexOf(',')));
        }

        // delivery rounding owner
        value = CollateralUtilities.formatNumber(getDeliveryRoundingOwner(marginCall));
        if (value.contains(".")) {
            marginCallItem.setDeliveryRoundingOwner(value.substring(0, value.indexOf('.')));
        }
        if (value.contains(",")) {
            marginCallItem.setDeliveryRoundingOwner(value.substring(0, value.indexOf(',')));
        }

        // tolerance amount
        value = CollateralUtilities.formatNumber(getToleranceAmount(marginCall)); // 769
        if (value.contains(".")) {
            marginCallItem.setToleranceAmount(value.substring(0, value.indexOf('.')));
        }
        if (value.contains(",")) {
            marginCallItem.setToleranceAmount(value.substring(0, value.indexOf(',')));
        }

        return marginCallItem;
    }

    /**
     * @param contract
     * @param processDate     process date
     * @param idRatingMatrix  matrix id
     * @param ratingDirection lower or higher
     * @param idLe            LE (PO or CTPY) id
     * @return the threshold of the rating matrix as a String
     */
    private static String getRatingThresholdAmount(CollateralConfig contract, JDate processDate, int idRatingMatrix,
                                                   String ratingDirection, final int idLe) {

        final Vector<String> agencies = contract.getEligibleAgencies();

        if (Util.isEmpty(agencies) || (processDate == null) || Util.isEmpty(ratingDirection)) {
            return null;
        }

        if (idRatingMatrix <= 0) {
            return null;
        }

        if ((contract == null) || (contract.getLegalEntity() == null)) {
            return null;
        }

        if (idLe < 0) {
            return null;
        }

        Vector<CreditRating> creditRatings = ELBEandKGRutilities.getCreditRatingsForLE(contract, agencies, idLe,
                processDate);

        if ((creditRatings == null) || creditRatings.isEmpty()) {
            return null;
        }

        MarginCallCreditRating ratingToday = ELBEandKGRutilities.getMccrt(creditRatings, idRatingMatrix,
                ratingDirection, processDate);

        if (ratingToday == null) {
            Log.error(CollateralizedTradesReportLogic.class,
                    "Contract rating configuration error. Contract Name: " + contract.getName());
            return null;
        }

        return ratingToday.getThreshold();
    }

    /**
     * @param contract
     * @param processDate     process date
     * @param idRatingMatrix  matrix id
     * @param ratingDirection lower or higher
     * @param idLe            LE (PO or CTPY) id
     * @return the threshold of the rating matrix as a String
     */
    private static double getRatingMtaAmount(CollateralConfig contract, JDate processDate, int idRatingMatrix,
                                             String ratingDirection, final int idLe) {

        final Vector<String> agencies = contract.getEligibleAgencies();

        if (Util.isEmpty(agencies) || (processDate == null) || Util.isEmpty(ratingDirection)) {
            return 0;
        }

        if (idRatingMatrix <= 0) {
            return 0;
        }

        if ((contract == null) || (contract.getLegalEntity() == null)) {
            return 0;
        }

        if (idLe < 0) {
            return 0;
        }

        Vector<CreditRating> creditRatings = ELBEandKGRutilities.getCreditRatingsForLE(contract, agencies, idLe,
                processDate);

        if ((creditRatings == null) || creditRatings.isEmpty()) {
            return 0;
        }

        MarginCallCreditRating ratingToday = ELBEandKGRutilities.getMccrt(creditRatings, idRatingMatrix,
                ratingDirection, processDate);

        if (ratingToday == null) {
            Log.error(CollateralizedTradesReportLogic.class,
                    "Contract rating configuration error. Contract Name: " + contract.getName());
            return 0;
        }
        // AAP MIG 14.4
        return parseStringAmountToDouble(ratingToday.getMta());
    }

    /**
     * @param contract
     * @param processDate     process date
     * @param idRatingMatrix  matrix id
     * @param ratingDirection lower or higher
     * @param idLe            LE (PO or CTPY) id
     * @return the threshold of the rating matrix as a String
     */
    private static double getRatingIAAmount(CollateralConfig contract, JDate processDate, int idRatingMatrix,
                                            String ratingDirection, final int idLe) {

        final Vector<String> agencies = contract.getEligibleAgencies();

        if (Util.isEmpty(agencies) || (processDate == null) || Util.isEmpty(ratingDirection)) {
            return 0;
        }

        if (idRatingMatrix <= 0) {
            return 0;
        }

        if ((contract == null) || (contract.getLegalEntity() == null)) {
            return 0;
        }

        if (idLe < 0) {
            return 0;
        }

        Vector<CreditRating> creditRatings = ELBEandKGRutilities.getCreditRatingsForLE(contract, agencies, idLe,
                processDate);

        if ((creditRatings == null) || creditRatings.isEmpty()) {
            return 0;
        }

        MarginCallCreditRating ratingToday = ELBEandKGRutilities.getMccrt(creditRatings, idRatingMatrix,
                ratingDirection, processDate);

        if (ratingToday == null) {
            Log.error(CollateralizedTradesReportLogic.class,
                    "Contract rating configuration error. Contract Name: " + contract.getName());
            return 0;
        }

        // AAP MIG 14.4
        return parseStringAmountToDouble(ratingToday.getIndependentAmount());
    }

    // AAP MIG 14.4
    /*
     * Handles different incoming number formats and nullPointers to avoid
     * crashes
     */
    private static Double parseStringAmountToDouble(String amount) {
        NumberFormat formatter;
        try {
            return Double.valueOf(amount);
        } catch (NumberFormatException e) {
            formatter = NumberFormat.getNumberInstance();
            try {
                return formatter.parse(amount).doubleValue();
            } catch (ParseException e1) {
                Log.error(OptCustAgreementParamsLogic.class, "Error while trying to get Double from String: " + amount);
                Log.error(OptCustAgreementParamsLogic.class, e1); //sonar
            }
        } catch (NullPointerException e) {
            Log.warn(OptCustAgreementParamsLogic.class, "Null MTA value received, a zero value double is returned. \n" + e); //sonar
            return new Double(0);
        }
        return null;
    }

    /**
     * @param contract
     * @param processDate     process date
     * @param idRatingMatrix  matrix id
     * @param ratingDirection lower or higher
     * @param idLe            LE (PO or CTPY) id
     * @return the threshold of the rating matrix as a String
     */
    @SuppressWarnings("unused")
    private static String getRatingIAType(CollateralConfig contract, JDate processDate, int idRatingMatrix,
                                          String ratingDirection, final int idLe) {

        final Vector<String> agencies = contract.getEligibleAgencies();

        if (Util.isEmpty(agencies) || (processDate == null) || Util.isEmpty(ratingDirection)) {
            return null;
        }

        if (idRatingMatrix <= 0) {
            return null;
        }

        if ((contract == null) || (contract.getLegalEntity() == null)) {
            return null;
        }

        if (idLe < 0) {
            return null;
        }

        Vector<CreditRating> creditRatings = ELBEandKGRutilities.getCreditRatingsForLE(contract, agencies, idLe,
                processDate);

        if ((creditRatings == null) || creditRatings.isEmpty()) {
            return null;
        }

        MarginCallCreditRating ratingToday = ELBEandKGRutilities.getMccrt(creditRatings, idRatingMatrix,
                ratingDirection, processDate);

        if (ratingToday == null) {
            Log.error(CollateralizedTradesReportLogic.class,
                    "Contract rating configuration error. Contract Name: " + contract.getName());
            return null;
        }

        return ratingToday.getIaType();
    }

    /**
     * @param contract
     * @param processDate     process date
     * @param idRatingMatrix  matrix id
     * @param ratingDirection lower or higher
     * @param idLe            LE (PO or CTPY) id
     * @return the threshold of the rating matrix as a String
     */
    private static String getRatingMtaType(CollateralConfig contract, JDate processDate, int idRatingMatrix,
                                           String ratingDirection, final int idLe) {

        final Vector<String> agencies = contract.getEligibleAgencies();

        if (Util.isEmpty(agencies) || (processDate == null) || Util.isEmpty(ratingDirection)) {
            return null;
        }

        if (idRatingMatrix <= 0) {
            return null;
        }

        if ((contract == null) || (contract.getLegalEntity() == null)) {
            return null;
        }

        if (idLe < 0) {
            return null;
        }

        Vector<CreditRating> creditRatings = ELBEandKGRutilities.getCreditRatingsForLE(contract, agencies, idLe,
                processDate);

        if ((creditRatings == null) || creditRatings.isEmpty()) {
            return null;
        }

        MarginCallCreditRating ratingToday = ELBEandKGRutilities.getMccrt(creditRatings, idRatingMatrix,
                ratingDirection, processDate);

        if (ratingToday == null) {
            Log.error(CollateralizedTradesReportLogic.class,
                    "Contract rating configuration error. Contract Name: " + contract.getName());
            return null;
        }

        return ratingToday.getMtaType();
    }

    /**
     * @param contract
     * @param processDate     process date
     * @param idRatingMatrix  matrix id
     * @param ratingDirection lower or higher
     * @param idLe            LE (PO or CTPY) id
     * @return the threshold of the rating matrix as a String
     */
    private static String getRatingThresholdType(CollateralConfig contract, JDate processDate, int idRatingMatrix,
                                                 String ratingDirection, final int idLe) {

        final Vector<String> agencies = contract.getEligibleAgencies();

        if (Util.isEmpty(agencies) || (processDate == null) || Util.isEmpty(ratingDirection)) {
            return null;
        }

        if (idRatingMatrix <= 0) {
            return null;
        }

        if ((contract == null) || (contract.getLegalEntity() == null)) {
            return null;
        }

        if (idLe < 0) {
            return null;
        }

        Vector<CreditRating> creditRatings = ELBEandKGRutilities.getCreditRatingsForLE(contract, agencies, idLe,
                processDate);

        if ((creditRatings == null) || creditRatings.isEmpty()) {
            return null;
        }

        MarginCallCreditRating ratingToday = ELBEandKGRutilities.getMccrt(creditRatings, idRatingMatrix,
                ratingDirection, processDate);

        if (ratingToday == null) {
            Log.error(CollateralizedTradesReportLogic.class,
                    "Contract rating configuration error. Contract Name: " + contract.getName());
            return null;
        }

        return ratingToday.getThresholdType();
    }

    // GSM: 19/02/2014. New Optimization fields

    private String getCtpyThresholdType(CollateralConfig marginCall) {

        if (CollateralConfig.GLOBAL_RATING.equals(marginCall.getLeNewThresholdType())) {
            return getRatingThresholdType(marginCall, processDate, marginCall.getLeRatingsConfigId(),
                    marginCall.getLeThresholdRatingDirection(), marginCall.getLeId());
        }

        if ((marginCall == null) || (marginCall.getLeNewThresholdType() == null)
                || marginCall.getLeNewThresholdType().isEmpty()) {
            return "";
        }
        return marginCall.getLeNewThresholdType();
    }

    private String getOwnerThresholdType(CollateralConfig marginCall) {

        if (CollateralConfig.GLOBAL_RATING.equals(marginCall.getPoNewThresholdType())) {
            return getRatingThresholdType(marginCall, processDate, marginCall.getPoRatingsConfigId(),
                    marginCall.getPoThresholdRatingDirection(), marginCall.getPoId());
        }

        if ((marginCall == null) || (marginCall.getPoNewThresholdType() == null)
                || marginCall.getPoNewThresholdType().isEmpty()) {
            return "";
        }
        return marginCall.getPoNewThresholdType();
    }

    private String getOwnerMTAType(CollateralConfig marginCall) {

        if (CollateralConfig.GLOBAL_RATING.equals(marginCall.getPoMTAType())) {
            return getRatingMtaType(marginCall, processDate, marginCall.getPoRatingsConfigId(),
                    marginCall.getPoMTARatingDirection(), marginCall.getPoId());
        }

        if ((marginCall == null) || (marginCall.getPoMTAType() == null) || marginCall.getPoMTAType().isEmpty()) {
            return "";
        }
        return marginCall.getPoMTAType();
    }

    private String getOwnerMTACcy(CollateralConfig marginCall) {

        if ((marginCall == null) || (marginCall.getPoMTAType() == null) || marginCall.getPoMTAType().isEmpty()) {
            return "";
        }
        return marginCall.getPoMTACurrency();
    }

    private String getCtpyMTAType(CollateralConfig marginCall) {

        if (CollateralConfig.GLOBAL_RATING.equals(marginCall.getLeMTAType())) {
            return getRatingMtaType(marginCall, processDate, marginCall.getLeRatingsConfigId(),
                    marginCall.getLeMTARatingDirection(), marginCall.getLeId());
        }

        if ((marginCall == null) || (marginCall.getLeMTAType() == null) || marginCall.getLeMTAType().isEmpty()) {
            return "";
        }
        return marginCall.getLeMTAType();
    }

    private String getCtpyMTACcy(CollateralConfig marginCall) {

        if ((marginCall == null) || (marginCall.getLeMTAType() == null) || marginCall.getLeMTAType().isEmpty()) {
            return "";
        }
        return marginCall.getLeMTACurrency();
    }

    private String getElibigleAgencies(CollateralConfig marginCall) {

        if ((marginCall == null) || (marginCall.getEligibleAgencies() == null)
                || marginCall.getEligibleAgencies().isEmpty()) {
            return "";
        }
        Collection<String> agencies = marginCall.getEligibleAgencies();
        StringBuffer sb = new StringBuffer();
        for (String agency : agencies) {
            sb.append(agency);
            sb.append(",");
        }
        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }

    private String getHairCutRuleName(CollateralConfig marginCall) {

        if ((marginCall != null) && (marginCall.getHaircutName() != null)) {
            return marginCall.getHaircutName();
        }
        return "";
    }

    private boolean getRehypothecable(CollateralConfig marginCall) {
        if ((marginCall != null)) {
            return marginCall.isRehypothecable();
        }
        return false;
    }

    private boolean getLongTermMoody(CollateralConfig marginCall) {

        return getLEAttribute(marginCall, USE_LONG_T_MOODY);
    }

    private boolean getLongTermFitch(CollateralConfig marginCall) {

        return getLEAttribute(marginCall, USE_LONG_T_FITCH);
    }

    private boolean getLongTermSP(CollateralConfig marginCall) {

        return getLEAttribute(marginCall, USE_LONG_T_SP);
    }

    private boolean getLEAttribute(final CollateralConfig marginCall, final String attributeName) {

        if (marginCall != null) {
            final LegalEntity le = marginCall.getLegalEntity();
            // @SuppressWarnings("unchecked")
            // Collection<String> attrCole = le.getLegalEntityAttributes();
            SantanderUtil util = SantanderUtil.getInstance();
            final String ltAttribute = util.getLegalEntityAttribute(le, attributeName);
            if ((ltAttribute == null) || ltAttribute.isEmpty()) {
                return false;
            }

            return (ltAttribute.equalsIgnoreCase("true") || ltAttribute.equals("1"));
        }
        return false;
    }

    // GSM: 19/02/2014. end block

    // Instruments
    private OptCustAgreementParamsItem getKGR_Collateral_InstrumentsItem(final CollateralConfig marginCall,
                                                                         final DSConnection dsConn, final Object value, final Vector<String> errors) {
        final OptCustAgreementParamsItem marginCallInstrumentItem = new OptCustAgreementParamsItem();

        marginCallInstrumentItem.setMarginCallContract(getMarginCallContract(marginCall));
        if (value != null) {
            marginCallInstrumentItem.setInstrument(getInstrument(value.toString()));
        }
        // marginCallInstrumentItem.setOwnerName(this.getOwnerName(marginCall,
        // dsConn)); // instrumentDescription PENDING
        final String headClone = CollateralUtilities.converseHeadCloneKGRContracts(getHeadClone(marginCall));
        marginCallInstrumentItem.setHeadClone(headClone);

        return marginCallInstrumentItem;
    }

    // Cash
    private OptCustAgreementParamsItem getKGR_Collateral_CashPositions(final CollateralConfig marginCall,
                                                                       final InventoryCashPosition inventoryCashPosition, final DSConnection dsConn, final Vector<String> errors) {
        final OptCustAgreementParamsItem marginCallCashItem = new OptCustAgreementParamsItem();
        double amount = 0;
        final char fieldSeparator = 28;
        String value = "";

        if ((null != inventoryCashPosition)) {

            // Report fields
            marginCallCashItem.setHeader(getHeader() + "      "); // A?ADIR 6
            // espacios
            // blanco
            marginCallCashItem.setTransactionId("1:" + getTransactionId(inventoryCashPosition) + fieldSeparator);
            marginCallCashItem.setTransactionType("2:" + getTransactionType(inventoryCashPosition) + fieldSeparator);
            marginCallCashItem.setAction("3:" + getAction() + fieldSeparator);
            marginCallCashItem.setReconciliationType("4:" + getReconciliationType() + fieldSeparator);
            marginCallCashItem.setCounterparty("5:" + getCounterparty(marginCall, dsConn) + fieldSeparator);

            final String aliasEntityKGR = CollateralUtilities.getAliasEntityKGR(dsConn, this._poId);
            if (aliasEntityKGR.equals("")) {
                marginCallCashItem.setOwner("7:" + getOwner(marginCall, dsConn) + fieldSeparator); // office
            } else {
                marginCallCashItem.setOwner("7:" + aliasEntityKGR + fieldSeparator); // office
            }

            marginCallCashItem.setTransactionDate("10:" + processDate.toString() + fieldSeparator);
            marginCallCashItem.setMaturityDate("16:" + processDate.addDays(7).toString() + fieldSeparator);

            // Retrieve and save the value for the amount.
            amount = getCollateralAmount(inventoryCashPosition);

            // Report fields
            marginCallCashItem.setIsReceived("93:" + getIsReceived(amount) + fieldSeparator);
            marginCallCashItem.setContractCcy("20:" + getContractCcyCash(inventoryCashPosition) + fieldSeparator); // currency

            value = CollateralUtilities.formatNumber(Math.abs(amount));
            if (value.contains(",")) {
                marginCallCashItem.setCollateralAmount("21:" + value.replace(',', '.') + fieldSeparator);
            } else {
                marginCallCashItem.setCollateralAmount("21:" + value + fieldSeparator);
            }

            marginCallCashItem.setContractType("265:" + getContractType(marginCall) + fieldSeparator); // agreement
            // type
            if (getMarginCallContract(marginCall) == null) {
                marginCallCashItem.setMarginCallContract("271:" + fieldSeparator); // agreement
                // id
            } else {
                marginCallCashItem.setMarginCallContract("271:" + getMarginCallContract(marginCall) + fieldSeparator); // agreement
                // id
            }

            // added
            value = CollateralUtilities.formatNumber(getIndependentAmount(marginCall));
            if (value.contains(",")) {
                marginCallCashItem.setIndependentAmount("232:" + value.replace(',', '.') + fieldSeparator);
            } else {
                marginCallCashItem.setIndependentAmount("232:" + value + fieldSeparator);
            }

            marginCallCashItem.setSource("266:" + getSource() + fieldSeparator);

            // new field
            marginCallCashItem.setConcilia("221:" + CONCILIA_FIELD + fieldSeparator);

            return marginCallCashItem;
        }

        return null;
    }

    // Securities
    private OptCustAgreementParamsItem getKGR_Collateral_SecurityPositions(final CollateralConfig marginCall,
                                                                           final InventorySecurityPosition inventorySecurityPosition, final DSConnection dsConn,
                                                                           final Vector<String> errors, final PricingEnv pricingEnv) {
        final OptCustAgreementParamsItem marginCallSecItem = new OptCustAgreementParamsItem();
        double amount = 0;
        final char fieldSeparator = 28;
        String value = "";

        if ((null != inventorySecurityPosition)) { // get only alive

            // We retrieve the bond product.
            final Bond bond = getBondFromSecurityPosition(inventorySecurityPosition);
            if (bond != null) {

                // Report fields
                marginCallSecItem.setHeader(getHeader() + "      "); // A?ADIR
                // 6
                // espacios
                // blanco
                marginCallSecItem.setTransactionId(
                        "1:" + limitFieldSize(getTransactionIdSecurity(inventorySecurityPosition, bond), 37)
                                + fieldSeparator);
                marginCallSecItem.setTransactionType(
                        "2:" + getTransactionTypeSecurity(inventorySecurityPosition) + fieldSeparator);
                marginCallSecItem.setAction("3:" + getAction() + fieldSeparator);
                marginCallSecItem.setReconciliationType("4:" + getReconciliationType() + fieldSeparator);
                marginCallSecItem.setCounterparty("5:" + getCounterparty(marginCall, dsConn) + fieldSeparator);

                final String aliasEntityKGR = CollateralUtilities.getAliasEntityKGR(dsConn, this._poId);
                if (aliasEntityKGR.equals("")) {
                    marginCallSecItem.setOwner("7:" + getOwner(marginCall, dsConn) + fieldSeparator); // office
                } else {
                    marginCallSecItem.setOwner("7:" + aliasEntityKGR + fieldSeparator); // office
                }

                if (getIssuer(dsConn, bond) != null) {
                    marginCallSecItem.setIssuer("72:" + getIssuer(dsConn, bond) + fieldSeparator);
                } else {
                    marginCallSecItem.setIssuer("72:" + fieldSeparator);
                }

                marginCallSecItem.setTransactionDate("10:" + processDate.toString() + fieldSeparator);
                marginCallSecItem.setMaturityDate("16:" + processDate.addDays(7).toString() + fieldSeparator);

                marginCallSecItem.setBondMaturityDate("201:" + getBondMaturityDate(bond).toString() + fieldSeparator);
                marginCallSecItem.setIsin("62:" + getIsin(bond) + fieldSeparator);

                // Retrieve and save the value for the amount.
                amount = getCollateralAmountSecurity(inventorySecurityPosition) * bond.getFaceValue(); // #1-778
                // -
                // Multiply
                // quantity
                // with
                // faceValue
                // to
                // get
                // nominal

                marginCallSecItem.setIsReceived("93:" + getIsReceived(amount) + fieldSeparator);

                value = CollateralUtilities.formatNumber(Math.abs(amount));
                if (value.contains(",")) {
                    marginCallSecItem.setCollateralAmount("21:" + value.replace(',', '.') + fieldSeparator);
                } else {
                    marginCallSecItem.setCollateralAmount("21:" + value + fieldSeparator);
                }

                marginCallSecItem.setContractCcy("20:" + getContractCcySec(inventorySecurityPosition) + fieldSeparator); // currency

                value = CollateralUtilities.formatNumber(getDirtyPrice(bond, valueDate)); // #3-778
                if (value.contains(",")) {
                    marginCallSecItem.setDirtyPrice("18:" + value.replace(',', '.') + fieldSeparator);
                } else {
                    marginCallSecItem.setDirtyPrice("18:" + value + fieldSeparator);
                }

                value = CollateralUtilities.formatNumber(getHaircut(marginCall, inventorySecurityPosition, dsConn)); // #4-778
                if (value.contains(",")) {
                    marginCallSecItem.setHaircut("61:" + value.replace(',', '.') + fieldSeparator);
                } else {
                    marginCallSecItem.setHaircut("61:" + value + fieldSeparator);
                }

                marginCallSecItem.setContractType("265:" + getContractType(marginCall) + fieldSeparator); // agreement
                // type
                if (getMarginCallContract(marginCall) == null) {
                    marginCallSecItem.setMarginCallContract("271:" + fieldSeparator); // agreement
                    // id
                } else {
                    marginCallSecItem
                            .setMarginCallContract("271:" + getMarginCallContract(marginCall) + fieldSeparator); // agreement
                    // id
                }
                // added
                marginCallSecItem.setSource("266:" + getSource() + fieldSeparator);

                // new field
                marginCallSecItem.setConcilia("221:" + CONCILIA_FIELD + fieldSeparator);
            } else {
                return null;
            }

            return marginCallSecItem;
        }

        return null;
    }

    // Counterparties
    private OptCustAgreementParamsItem getKGR_Collateral_CounterpartyItem(final CollateralConfig marginCall,
                                                                          final LegalEntity le, final DSConnection dsConn, final Vector<String> errors) {
        final OptCustAgreementParamsItem marginCallCptyItem = new OptCustAgreementParamsItem();

        marginCallCptyItem.setMarginCallContract(getMarginCallContract(marginCall));
        marginCallCptyItem.setCounterparty(le.getAuthName());
        marginCallCptyItem.setContractLongName(le.getName()); // cpty long name
        final String headClone = CollateralUtilities.converseHeadCloneKGRContracts(getHeadClone(marginCall));
        marginCallCptyItem.setHeadClone(headClone);

        return marginCallCptyItem;
    }

    // Processing Orgs (branch file)
    private OptCustAgreementParamsItem getKGR_Collateral_ProcessingOrgItem(final CollateralConfig marginCall,
                                                                           final LegalEntity po, final DSConnection dsConn, final Vector<String> errors) {
        final OptCustAgreementParamsItem marginCallPoItem = new OptCustAgreementParamsItem();

        marginCallPoItem.setMarginCallContract(getMarginCallContract(marginCall));
        marginCallPoItem.setOwner(po.getAuthName());
        marginCallPoItem.setOwnerName(po.getName());
        final String headClone = CollateralUtilities.converseHeadCloneKGRContracts(getHeadClone(marginCall));
        marginCallPoItem.setHeadClone(headClone);

        return marginCallPoItem;
    }

    // ** Methods ** //
    public double getHaircut(final CollateralConfig marginCall, InventorySecurityPosition secPos, DSConnection dsConn) {

        double value = 0.0;

        // get HaircutProxy for contract
        CollateralFilterProxy filterProxy = new CachedCollateralFilterProxy();
        HaircutProxyFactory fact = new HaircutProxyFactory(filterProxy);
        HaircutProxy haircutProxy = fact.getProxy(marginCall.getPoHaircutName());

        Product p = secPos.getProduct();
        if (p != null) {
            // get haircut value for security
            //JRL 20/04/2016 Migration 14.4
            value = 1 - Math.abs((haircutProxy.getHaircut(marginCall.getCurrency(), new CollateralCandidate(p), processDate,
                    true, marginCall, "Pay"))); // get
            // percentage
            // and set
            // 100-%

        }

        return value * 100;
    }

    // 778 - New method to calculate dirtyPrice, find quote value from ISIN for
    // valDate and get close
    @SuppressWarnings("unchecked")
    public double getDirtyPrice(Bond bond, JDate valDate) {
        Vector<QuoteValue> vQuotes = new Vector<QuoteValue>();
        if (bond != null) {

            String isin = bond.getSecCode("ISIN");
            String quoteName;
            try {
                quoteName = CollateralUtilities.getQuoteNameFromISIN(isin, valDate);
                if (!quoteName.equals("")) {
                    String clausule = "quote_name = " + "'" + quoteName + "' AND trunc(quote_date) = to_date('"
                            + valDate
                            + "', 'dd/mm/yy') AND quote_set_name = 'DirtyPrice' AND quote_type = 'DirtyPrice'";
                    vQuotes = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(clausule);
                    if ((vQuotes != null) && (vQuotes.size() > 0)) {
                        return vQuotes.get(0).getClose() * 100;
                    }
                }
            } catch (RemoteException e1) {
                Log.error(this, "Cannot retrieve dirty price", e1);

            }
        }

        return 0.00;
    }

    private String getIsin(final Bond bond) {
        return bond.getSecCode(ISIN);
    }

    private String getHeader() {
        return HEADER;
    }

    private String getContractCcyCash(final InventoryCashPosition inventoryCashPosition) {
        return inventoryCashPosition.getCurrency();
    }

    private String getContractCcySec(final InventorySecurityPosition inventorySecurityPosition) {
        return inventorySecurityPosition.getSettleCurrency(); // #5-778 -
        // Changed
        // return value
        // to get
        // position
        // value
    }

    // 769 - Changed return value to DisputeContractToleranceAmount
    private double getToleranceAmount(final CollateralConfig marginCall) {
        return marginCall.getDisputeContractToleranceAmount();
    }

    private double getDeliveryRoundingOwner(final CollateralConfig marginCall) {
        return marginCall.getPoRoundingFigure();
    }

    private double getDeliveryRoundingCpty(final CollateralConfig marginCall) {
        return marginCall.getLeRoundingFigure();
    }

    private String getDeliveryRounding(final CollateralConfig marginCall) {
        return marginCall.getPoRoundingMethod();
    }

    private String getMasterSignedDate(final CollateralConfig marginCall) {
        if (marginCall.getAdditionalField(MA_SIGN_DATE) != null) {

            return marginCall.getAdditionalField(MA_SIGN_DATE);
        }
        return BLANK;
    }

    private String getNotUsed() {
        return BLANK;
    }

    private String getHeadClone(final CollateralConfig marginCall) {
        return marginCall.getAdditionalField(HEAD_CLONE);
    }

    // 772 - Swaped values
    private String getOneWay(final CollateralConfig marginCall) {
        if (CollateralConfig.NET_BILATERAL.equals(marginCall.getContractDirection())) {
            return NONE;
        } else {
            if (LEGAL_ENTITY.equals(marginCall.getSecuredParty())) {
                return COUNTERPARTY;
            } else {
                return OWNER;
            }
        }
    }

    private JDate getBondMaturityDate(final Bond bond) {
        return bond.getMaturityDate();
    }

    private String getIssuer(final DSConnection dsConn, final Bond bond) {
        String strToReturn = null;
        LegalEntity le = new LegalEntity();
        try {
            le = dsConn.getRemoteReferenceData().getLegalEntity(bond.getIssuerId());
            if (null != le) {
                strToReturn = le.getAuthName();
            }
        } catch (final RemoteException e) {
            Log.error(this, "RemoteException: " + e);
        }

        return strToReturn;
    }

    private double getCollateralAmountSecurity(final InventorySecurityPosition inventorySecurityPosition) {
        return inventorySecurityPosition.getTotal();
    }

    private String getTransactionTypeSecurity(final InventorySecurityPosition inventorySecurityPosition) {
        return TRANSACTION_TYPE_SEC;
    }

    private String getTransactionIdSecurity(final InventorySecurityPosition inventorySecurityPosition,
                                            final Bond bond) {
        final String transactionId = inventorySecurityPosition.getConfigId() + getIsin(bond)
                + inventorySecurityPosition.getBook().getAuthName();
        return transactionId;
    }

    private String getIsReceived(final double amount) {
        // Checks if the amount is positive or negative.
        if (amount < 0) {
            return "N";
        } else {
            return "Y";
        }
    }

    private double getCollateralAmount(final InventoryCashPosition inventoryCashPosition) {

        return inventoryCashPosition.getTotal();
    }

    private String getReconciliationType() {
        return RECONCILIATION_TYPE;
    }

    private String getAction() {
        return ACTION;
    }

    private String getTransactionType(final InventoryCashPosition inventoryCashPosition) {
        return TRANSACTION_TYPE;
    }

    private String getTransactionId(final InventoryCashPosition inventoryCashPosition) {
        final String strToReturn = inventoryCashPosition.getConfigId() + "" + inventoryCashPosition.getCurrency();
        return strToReturn;
    }

    @SuppressWarnings("unused")
    private String getCalcPeriod(final CollateralConfig marginCall) {
        final DateRule dateRule = marginCall.getDateRule();
        if (null != dateRule) {
            return dateRule.getName();
        } else {
            return null;
        }
    }

    private double getInitialMargin(final CollateralConfig marginCall, final DSConnection dsConn) {

        return 0.0;
    }

    @SuppressWarnings("unused")
    private String getToleranceAmountCcy(final CollateralConfig marginCall, final DSConnection dsConn) {

        return marginCall.getCurrency();
    }

    private int getOwnerCashOffset(final CollateralConfig marginCall) {

        return marginCall.getPoCashOffset();
    }

    private int getCtpyCashOffset(final CollateralConfig marginCall) {

        return marginCall.getLeCashOffset();
    }

    private int getOwnerSecurityOffset(final CollateralConfig marginCall) {

        return marginCall.getPoSecurityOffset();
    }

    private int getCtpySecurityOffset(final CollateralConfig marginCall) {

        return marginCall.getLeSecurityOffset();
    }

    private boolean getExcludeFromOptimizer(final CollateralConfig marginCall) {

        return marginCall.isExcludeFromOptimizer();
    }

    private String getMasterAgreeDescription(final CollateralConfig marginCall, final DSConnection dsConn) {

        return getLegalEntityName(dsConn, this._leId, false);
    }

    private String getAssetType(final CollateralConfig marginCall) {
        // Retrieve the collateral type for the counterparty and processing org.
        final String leCollType = marginCall.getLeCollType();
        final String poCollType = marginCall.getPoCollType();

        // Check collateral types retrieved previously.
        if (leCollType.equals(this.COLLATERAL_TYPE_CASH) && poCollType.equals(this.COLLATERAL_TYPE_CASH)) {
            return this.COLLATERAL_TYPE_CASH;
        } else if (leCollType.equals(this.COLLATERAL_TYPE_SEC) && poCollType.equals(this.COLLATERAL_TYPE_SEC)) {
            return this.COLLATERAL_TYPE_SEC;
        } else {
            return this.COLLATERAL_TYPE_BOTH;
        }
    }

    private String getMarginCallContract(final CollateralConfig marginCall) {
        if (marginCall.getName() != null) {
            return marginCall.getName();
        }
        return BLANK;
    }

    // MODIFIED
    private String getMasterAgreementShortName(final CollateralConfig marginCall, final DSConnection dsConn) {

        String strToReturn = marginCall.getAdditionalField(MASTER_AGREEMENT);

        return strToReturn;
    }

    private String getOwner(final CollateralConfig marginCall, final DSConnection dsConn) {
        return getLegalEntityName(dsConn, this._poId, true);
    }

    private String getOwnerName(final CollateralConfig marginCall, final DSConnection dsConn) {
        return getLegalEntityName(dsConn, this._poId, false);
    }

    private String getCounterparty(final CollateralConfig marginCall, final DSConnection dsConn) {
        return getLegalEntityName(dsConn, this._leId, true);
    }

    private String getContractLongName(final CollateralConfig marginCall, final DSConnection dsConn) {
        return getLegalEntityName(dsConn, this._leId, false);
    }

    private String getContractType(final CollateralConfig marginCall) {
        return marginCall.getContractType();
    }

    private String getFirstCalcDate(final CollateralConfig marginCall) {
        if (marginCall.getAdditionalField(FIRST_CALC_DATE) != null) {
            return marginCall.getAdditionalField(FIRST_CALC_DATE);
        }
        return BLANK;
    }

    private String getContractCcy(final CollateralConfig marginCall) {
        return marginCall.getCurrency();
    }

    private String getInstrument(final String value) {
        return value;
    }

    private String getLegalEntityName(final DSConnection dsConn, final int id, final boolean shortName) {
        String strToReturn = null;

        try {
            final LegalEntity le = dsConn.getRemoteReferenceData().getLegalEntity(id);
            if (shortName) {
                strToReturn = le.getAuthName();
            } else {
                strToReturn = le.getName();
            }

        } catch (final RemoteException e) {
            Log.error(this, "Cannot get legalEntity", e);

        }

        return strToReturn;
    }

    private double getIndependentAmount(final CollateralConfig marginCall) {

        double iaReturn = 0.0;

        // option 1 - get directly additional field ia_amount
        if ((marginCall.getAdditionalField(CONTRACT_INDEPENDENT_AMOUNT) != null)
                && (!marginCall.getAdditionalField(CONTRACT_INDEPENDENT_AMOUNT).equals(""))) {
            iaReturn = parseStringAmountToDouble(marginCall.getAdditionalField(CONTRACT_INDEPENDENT_AMOUNT));
        }
        return iaReturn;

    }

    private String getSource() {
        return SOURCE;
    }

    public static String getActualDate() {

        final Date date = new Date();
        final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        final String stringDate = sdf.format(date);
        return stringDate;

    }

    // 778 - New method for get all positions for a contract
    public static InventorySecurityPositionArray getSecurityPositions(final JDate date,
                                                                      final CollateralConfig marginCall, final DSConnection dsConn) {
        StringBuilder where = new StringBuilder();
        where.append(" inv_secposition.internal_external = 'MARGIN_CALL' ");
        where.append(" AND inv_secposition.date_type = 'TRADE' ");
        where.append(" AND inv_secposition.position_type = 'THEORETICAL'");
        where.append(" AND inv_secposition.config_id = ");
        where.append(marginCall.getId());
        where.append(" AND inv_secposition.position_date = ");

        where.append(" (");// BEGIN SELECT
        where.append(" select MAX(temp.position_date) from inv_secposition temp ");
        where.append(" WHERE inv_secposition.internal_external = temp.internal_external ");
        where.append(" AND inv_secposition.date_type = temp.date_type ");
        where.append(" AND inv_secposition.position_type = temp.position_type ");
        where.append(" AND inv_secposition.config_id = temp.config_id ");
        where.append(" AND inv_secposition.account_id = temp.account_id ");
        where.append(" AND inv_secposition.security_id = temp.security_id ");
        where.append(" AND inv_secposition.agent_id = temp.agent_id ");
        where.append(" AND inv_secposition.book_id = temp.book_id ");
        where.append(" AND TRUNC(temp.position_date) <= ").append(com.calypso.tk.core.Util.date2SQLString(date));
        where.append(" )");// END SELECT

        InventorySecurityPositionArray secPositions;
        try {
            secPositions = DSConnection.getDefault().getRemoteBackOffice()
                    .getInventorySecurityPositions("", where.toString(), null);
            return secPositions;
        } catch (RemoteException e) {
            Log.error(OptCustAgreementParamsLogic.class, e); //sonar
        }

        return null;
    }

    public Bond getBondFromSecurityPosition(InventorySecurityPosition invSecPos) {
        if (invSecPos != null) {
            Product p = invSecPos.getProduct();
            if ((p != null) && (p instanceof Bond)) {
                return (Bond) p;
            }
        }
        return null;
    }

    public boolean isBalanceZero(InventorySecurityPosition invSecPos) {
        if (invSecPos != null) {
            Bond b = getBondFromSecurityPosition(invSecPos);
            if (b != null) {
                Double balance = getCollateralAmountSecurity(invSecPos) * b.getFaceValue();
                if (roundPositionValue(balance, 2) == 0.0) {
                    return true;
                }
            }
        }
        return false;
    }

    public double roundPositionValue(Double value, int decimalsNumber) {
        return BigDecimal.valueOf(value).setScale(decimalsNumber, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    public String limitFieldSize(String fieldValue, int maxSize) {
        if (fieldValue.length() > maxSize) {
            return fieldValue.substring(0, maxSize);
        } else {
            return fieldValue;
        }
    }

    // ** Threshold & MTA & IA stuff ** //
    // threshold
    public static double getOwnerThresholdCcy(final CollateralConfig marginCall, Vector<CreditRating> creditRatings) {

        // GLOBAL RATING
        if (ELBEandKGRutilities.isThresholdDependingOn(marginCall, GLOBAL_RATING)) {

            MarginCallCreditRating mccCreditRating = ELBEandKGRutilities.getMccrt(creditRatings,
                    marginCall.getPoRatingsConfigId(), marginCall.getPoThresholdRatingDirection(), valueDate);
            if (mccCreditRating != null) {
                return ELBEandKGRutilities.getThresholdDependingOnRating(marginCall, mccCreditRating,
                        marginCall.getPoRatingsConfigId(), processDate, valueDate);
            }

        }
        // AMOUNT
        if (ELBEandKGRutilities.isThresholdDependingOn(marginCall, "AMOUNT")) {
            return ELBEandKGRutilities.getThresholdDependingOnAmount(marginCall, "PO", processDate);
        }
        // MC_PERCENT
        if (ELBEandKGRutilities.isThresholdDependingOn(marginCall, "MC_PERCENT")) {
            return ELBEandKGRutilities.getThresholdDependingOnMcPercent(marginCall, "PO", processDate);
        }
        // PERCENT
        if (ELBEandKGRutilities.isThresholdDependingOn(marginCall, "PERCENT")) {
            return ELBEandKGRutilities.getThresholdDependingOnPercent(marginCall, "PO", processDate);
        }
        // BOTH
        if (ELBEandKGRutilities.isThresholdDependingOn(marginCall, "BOTH")) {
            return ELBEandKGRutilities.getThresholdDependingOnBoth(marginCall, "PO", processDate, valueDate);
        }

        return 0.00;
    }

    public void setThresholdOwner(OptCustAgreementParamsItem marginCallItem, CollateralConfig marginCall,
                                  double thresholdOwner) {
        String value = CollateralUtilities.formatNumber(Math.abs(thresholdOwner)); // set
        // abs
        if (ELBEandKGRutilities.isThresholdDependingOn(marginCall, PERCENT)) {
            if (value.contains(",")) {
                marginCallItem.setThresholdOwner(value.replace(',', '.'));
            }
        } else {
            if (value.contains(".")) {
                marginCallItem.setThresholdOwner(value.substring(0, value.indexOf('.')));
            }
            if (value.contains(",")) {
                marginCallItem.setThresholdOwner(value.substring(0, value.indexOf(',')));
            }
        }
    }

    public static double getCptyThresholdCcy(final CollateralConfig marginCall, Vector<CreditRating> creditRatings) {

        // GLOBAL RATING
        if (ELBEandKGRutilities.isCptyThresholdDependingOn(marginCall, GLOBAL_RATING)) {

            MarginCallCreditRating mccCreditRating = ELBEandKGRutilities.getMccrt(creditRatings,
                    marginCall.getLeRatingsConfigId(), marginCall.getLeThresholdRatingDirection(), valueDate);
            if (mccCreditRating != null) {
                return ELBEandKGRutilities.getThresholdDependingOnRating(marginCall, mccCreditRating,
                        marginCall.getLeRatingsConfigId(), processDate, valueDate);
            }

        }
        // AMOUNT
        if (ELBEandKGRutilities.isCptyThresholdDependingOn(marginCall, "AMOUNT")) {
            return ELBEandKGRutilities.getThresholdDependingOnAmount(marginCall, "CPTY", valueDate);
        }
        // MC_PERCENT
        if (ELBEandKGRutilities.isCptyThresholdDependingOn(marginCall, "MC_PERCENT")) {
            return ELBEandKGRutilities.getThresholdDependingOnMcPercent(marginCall, "CPTY", processDate);
        }
        // PERCENT
        if (ELBEandKGRutilities.isCptyThresholdDependingOn(marginCall, "PERCENT")) {
            return ELBEandKGRutilities.getThresholdDependingOnPercent(marginCall, "CPTY", processDate);
        }
        // BOTH
        if (ELBEandKGRutilities.isCptyThresholdDependingOn(marginCall, "BOTH")) {
            return ELBEandKGRutilities.getThresholdDependingOnBoth(marginCall, "CPTY", processDate, valueDate);
        }

        return 0.00;
    }

    public void setThresholdCpty(OptCustAgreementParamsItem marginCallItem, CollateralConfig marginCall,
                                 double thresholdCpty) {
        String value = CollateralUtilities.formatNumber(Math.abs(thresholdCpty));
        if (ELBEandKGRutilities.isCptyThresholdDependingOn(marginCall, PERCENT)) {
            if (value.contains(",")) {
                marginCallItem.setThresholdCpty(value.replace(',', '.'));
            }
        } else {
            if (value.contains(".")) {
                marginCallItem.setThresholdCpty(value.substring(0, value.indexOf('.')));
            }
            if (value.contains(",")) {
                marginCallItem.setThresholdCpty(value.substring(0, value.indexOf(',')));
            }
        }
    }

    // MTA
    public static double getOwnerMtaCcy(final CollateralConfig marginCall, Vector<CreditRating> creditRatings) {

        // GLOBAL RATING
        if (ELBEandKGRutilities.isMTADependingOn(marginCall, GLOBAL_RATING)) {

            MarginCallCreditRating mccCreditRating = ELBEandKGRutilities.getMccrt(creditRatings,
                    marginCall.getPoRatingsConfigId(), marginCall.getPoMTARatingDirection(), valueDate);
            if (mccCreditRating != null) {
                return ELBEandKGRutilities.getMtaDependingOnRating(marginCall, mccCreditRating,
                        marginCall.getPoRatingsConfigId(), processDate, valueDate);
            }

        }
        // AMOUNT
        if (ELBEandKGRutilities.isMTADependingOn(marginCall, "AMOUNT")) {
            return ELBEandKGRutilities.getMtaDependingOnAmount(marginCall, "PO", valueDate);
        }
        // MC_PERCENT
        if (ELBEandKGRutilities.isMTADependingOn(marginCall, "MC_PERCENT")) {
            return ELBEandKGRutilities.getMtaDependingOnMcPercent(marginCall, "PO", processDate);
        }
        // PERCENT
        if (ELBEandKGRutilities.isMTADependingOn(marginCall, "PERCENT")) {
            return ELBEandKGRutilities.getMtaDependingOnPercent(marginCall, "PO", processDate);
        }
        // BOTH
        if (ELBEandKGRutilities.isMTADependingOn(marginCall, "BOTH")) {
            return ELBEandKGRutilities.getMtaDependingOnBoth(marginCall, "PO", processDate, valueDate);
        }

        return 0.00;
    }

    public void setMtaOwner(OptCustAgreementParamsItem marginCallItem, CollateralConfig marginCall, double mtaOwner) {
        String value = CollateralUtilities.formatNumber(Math.abs(mtaOwner));
        if (ELBEandKGRutilities.isMTADependingOn(marginCall, PERCENT)) {
            if (value.contains(",")) {
                marginCallItem.setMtaOwner(value.replace(',', '.'));
            }
        } else {
            if (value.contains(".")) {
                marginCallItem.setMtaOwner(value.substring(0, value.indexOf('.')));
            }
            if (value.contains(",")) {
                marginCallItem.setMtaOwner(value.substring(0, value.indexOf(',')));
            }
        }
    }

    public static double getCptyMtaCcy(final CollateralConfig marginCall, Vector<CreditRating> creditRatings) {

        // GLOBAL RATING
        if (ELBEandKGRutilities.isCptyMTADependingOn(marginCall, GLOBAL_RATING)) {

            MarginCallCreditRating mccCreditRating = ELBEandKGRutilities.getMccrt(creditRatings,
                    marginCall.getLeRatingsConfigId(), marginCall.getLeMTARatingDirection(), valueDate);
            if (mccCreditRating != null) {
                return ELBEandKGRutilities.getMtaDependingOnRating(marginCall, mccCreditRating,
                        marginCall.getLeRatingsConfigId(), processDate, valueDate);
            }

        }
        // AMOUNT
        if (ELBEandKGRutilities.isCptyMTADependingOn(marginCall, "AMOUNT")) {
            return ELBEandKGRutilities.getMtaDependingOnAmount(marginCall, "CPTY", valueDate);
        }
        // MC_PERCENT
        if (ELBEandKGRutilities.isCptyMTADependingOn(marginCall, "MC_PERCENT")) {
            return ELBEandKGRutilities.getMtaDependingOnMcPercent(marginCall, "CPTY", processDate);
        }
        // PERCENT
        if (ELBEandKGRutilities.isCptyMTADependingOn(marginCall, "PERCENT")) {
            return ELBEandKGRutilities.getMtaDependingOnPercent(marginCall, "CPTY", processDate);
        }
        // BOTH
        if (ELBEandKGRutilities.isCptyMTADependingOn(marginCall, "BOTH")) {
            return ELBEandKGRutilities.getMtaDependingOnBoth(marginCall, "CPTY", processDate, valueDate);
        }

        return 0.00;
    }

    public void setMtaCpty(OptCustAgreementParamsItem marginCallItem, CollateralConfig marginCall, double mtaCpty) {
        String value = CollateralUtilities.formatNumber(Math.abs(mtaCpty));
        if (ELBEandKGRutilities.isCptyMTADependingOn(marginCall, PERCENT)) {
            if (value.contains(",")) {
                marginCallItem.setMtaCpty(value.replace(',', '.'));
            }
        } else {
            if (value.contains(".")) {
                marginCallItem.setMtaCpty(value.substring(0, value.indexOf('.')));
            }
            if (value.contains(",")) {
                marginCallItem.setMtaCpty(value.substring(0, value.indexOf(',')));
            }
        }
    }

    // IA
    public static double getOwnerIndAmountCcy(final CollateralConfig marginCall, Vector<CreditRating> creditRatings) {

        // GLOBAL RATING
        if (ELBEandKGRutilities.isIADependingOnRating(marginCall)) {

            MarginCallCreditRating mccCreditRating = ELBEandKGRutilities.getMccrt(creditRatings,
                    marginCall.getPoRatingsConfigId(), marginCall.getPoIARatingDirection(), valueDate);

            if (mccCreditRating != null) {
                return ELBEandKGRutilities.getIndAmountDependingOnRating(marginCall, mccCreditRating,
                        marginCall.getPoRatingsConfigId(), processDate, valueDate);
            }
        } else { // CONTRACT ADDITIONAL FIELD
            double IAvalue = ELBEandKGRutilities.getContractIA(marginCall);
            String IAccy = ELBEandKGRutilities.getContractIAccy(marginCall);
            return IAvalue * CollateralUtilities.getFXRate(valueDate, IAccy, marginCall.getCurrency());
        }

        return 0.00;

    }

    public static double getCptyIndAmountCcy(final CollateralConfig marginCall, Vector<CreditRating> creditRatings) {

        // GLOBAL RATING
        if (ELBEandKGRutilities.isCptyIADependingOnRating(marginCall)) {

            MarginCallCreditRating mccCreditRating = ELBEandKGRutilities.getMccrt(creditRatings,
                    marginCall.getLeRatingsConfigId(), marginCall.getLeIARatingDirection(), valueDate);

            if (mccCreditRating != null) {
                return ELBEandKGRutilities.getIndAmountDependingOnRating(marginCall, mccCreditRating,
                        marginCall.getLeRatingsConfigId(), processDate, valueDate);
            }
        } else { // CONTRACT ADDITIONAL FIELD
            double IAvalue = ELBEandKGRutilities.getContractIA(marginCall);
            String IAccy = ELBEandKGRutilities.getContractIAccy(marginCall);
            return IAvalue * CollateralUtilities.getFXRate(valueDate, IAccy, marginCall.getCurrency());
        }

        return 0.00;

    }

    // if both LE have IA non depending on rating (ownerIA=cptyIA=contractIA) we
    // follow this logic: ia>=0 ->
    // cptyIA=0, ownerIA=valor // ia<0->
    // ownerIA=0, cptyIA=valor
    // if not, we set each IA getted in each side
    public void setIndependentAmount(OptCustAgreementParamsItem marginCallItem, CollateralConfig marginCall,
                                     double indAmountOwner, double indAmountCpty) {

        // cpty depends on rating, owner not
        if (!ELBEandKGRutilities.isIADependingOnRating(marginCall)
                && ELBEandKGRutilities.isCptyIADependingOnRating(marginCall)) {
            marginCallItem.setIndependentAmountOwner(Util.numberToString(indAmountCpty, 0, null, false));
            marginCallItem.setIndependentAmountCpty("0");
        }
        // owner depends on rating, cpty not
        else if (ELBEandKGRutilities.isIADependingOnRating(marginCall)
                && !ELBEandKGRutilities.isCptyIADependingOnRating(marginCall)) {
            marginCallItem.setIndependentAmountCpty(Util.numberToString(indAmountOwner, 0, null, false));
            marginCallItem.setIndependentAmountOwner("0");
        }
        // owner&cpty not depends on rating
        else if (!ELBEandKGRutilities.isIADependingOnRating(marginCall)
                && !ELBEandKGRutilities.isCptyIADependingOnRating(marginCall)) {
            if (indAmountOwner >= 0) {
                String ia = "0";
                marginCallItem.setIndependentAmountCpty(ia);
                ia = Util.numberToString(indAmountOwner, 0, null, false);
                marginCallItem.setIndependentAmountOwner(ia);
            } else {
                String ia = "0";
                marginCallItem.setIndependentAmountOwner(ia);
                ia = Util.numberToString(-indAmountOwner, 0, null, false);
                marginCallItem.setIndependentAmountCpty(ia);
            }
            // both depends on rating
        } else {
            String ownerIA = Util.numberToString(indAmountOwner, 0, null, false);
            marginCallItem.setIndependentAmountOwner(ownerIA);
            String cptyIA = Util.numberToString(indAmountCpty, 0, null, false);
            marginCallItem.setIndependentAmountCpty(cptyIA);
        }
    }

}
