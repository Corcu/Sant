package calypsox.tk.report;

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
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.Security;
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
import java.text.SimpleDateFormat;
import java.util.*;

public class Opt_Collateral_MarginCallLogic {
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
    private static final String COLLATERAL_TYPE_CASH = "CASH";
    private static final String COLLATERAL_TYPE_SEC = "SECURITY";
    private static final String COLLATERAL_TYPE_BOTH = "BOTH";
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

    private final int _poId;
    private final int _leId;

    // added
    private static JDate processDate, valueDate;

    public Opt_Collateral_MarginCallLogic(final CollateralConfig marginCall) {
        this._poId = marginCall.getPoId();
        this._leId = marginCall.getLeId();
    }

    @SuppressWarnings("unused")
    private Opt_Collateral_MarginCallItem getKGR_Collateral_MarginCallItem(final Vector<String> errors) {
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
    public static Vector<Opt_Collateral_MarginCallItem> getReportRows(final CollateralConfig marginCall,
                                                                      final MarginCallPositionUtil marginCallPosition, final JDate jdate, final DSConnection dsConn,
                                                                      final Vector<String> errorMsgs, final String[] columns, final PricingEnv pricingEnv,
                                                                      final CollateralServiceRegistry collServReg, Vector holidaysVector) throws RemoteException {

        final Vector<Opt_Collateral_MarginCallItem> reportRows = new Vector<Opt_Collateral_MarginCallItem>();
        final Opt_Collateral_MarginCallLogic verifiedRow = new Opt_Collateral_MarginCallLogic(marginCall);
        InventoryCashPosition inventoryCashPosition = null;
        InventorySecurityPositionArray inventorySecurityPositionArray = null;
        Opt_Collateral_MarginCallItem rowCreated = null;

        // save extraction date
        processDate = jdate;
        valueDate = processDate.addBusinessDays(-1, holidaysVector);

        // Vector of current currency list.
        final List<CollateralConfigCurrency> currencies = marginCall.getEligibleCurrencies(); // 777 - Get eligible
        // currencies (all
        // contract currencies)

        // We check the number of columns to show the correct result.
        if (columns.length > 4) {
            // We check if the template selected is related to 'Positions' (Cash
            // or Security).
            if (columns[1].equals(TRANSACTIONID) || columns[2].equals(TRANSACTIONID)) {
                // Security
                if (columns[12].toUpperCase().equals(ISIN)) {
                    // get security positions for a contract
                    inventorySecurityPositionArray = getSecurityPositions(jdate, marginCall, dsConn); // #2-778
                    if (inventorySecurityPositionArray != null) {
                        for (int secPos = 0; secPos < inventorySecurityPositionArray.size(); secPos++) {
                            // don't show positions with balance=0
                            if (!verifiedRow.isBalanceZero(inventorySecurityPositionArray.get(secPos))) {
                                rowCreated = verifiedRow.getKGR_Collateral_SecurityPositions(marginCall,
                                        inventorySecurityPositionArray.get(secPos), dsConn, errorMsgs, pricingEnv);
                                if (null != rowCreated) { // If the result row is equals
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

                            inventoryCashPosition = BOPositionUtil.getCashPosition(
                                    (currencies.get(cash)).getCurrency(), "MARGIN_CALL", "ACTUAL", "TRADE", jdate,
                                    marginCall.getId(), dsConn, null); // 777 - Changed second parameter to ACTUAL
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
                                    String mappedExposureType = CollateralUtilities.initMappingInstrumentValues(dsConn,
                                            "GBO").get(exposureTypes.get(j));
                                    if (mappedExposureType != null) {

                                        // filter
                                        if (mappedExposureType.contains(CUSTOMIZED)) {
                                            mappedExposureType = "Caps and Floors";
                                        } else {
                                            mappedExposureType = mappedExposureType.substring(0,
                                                    mappedExposureType.indexOf('-'));
                                        }

                                        // check for not repeat instruments with same mapping type
                                        if (!instrumentTypesProcessed.contains(mappedExposureType)) { // 771 - Use a
                                            // filter to not
                                            // repeat types
                                            instrumentTypesProcessed.add(mappedExposureType);
                                            // filter
                                            if ((!mappedExposureType.equals(CONTRACT_IA))
                                                    && (!mappedExposureType.equals(DISPUTE_ADJ))) {
                                                reportRows.add(verifiedRow.getKGR_Collateral_InstrumentsItem(
                                                        marginCall, dsConn, mappedExposureType, errorMsgs));
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
                StaticDataFilter sdf = dsConn.getRemoteReferenceData().getStaticDataFilter(
                        "UND_" + marginCall.getName());
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
                                            rowCreated = verifiedRow.getKGR_Collateral_ProcessingOrgItem(marginCall,
                                                    po, dsConn, errorMsgs);
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
                                    rowCreated = verifiedRow.getKGR_Collateral_ProcessingOrgItem(marginCall, po,
                                            dsConn, errorMsgs);
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
    // GSM: 0992. We must show every contract, even if the LE is not configured properly
    private Opt_Collateral_MarginCallItem getKGR_Collateral_MarginCallItem(final CollateralConfig marginCall,
                                                                           final DSConnection dsConn, final Vector<String> errors) {
        final Opt_Collateral_MarginCallItem marginCallItem = new Opt_Collateral_MarginCallItem();
        String value = "";
        Vector<String> agencies = null;
        Vector<CreditRating> ownerCreditRatings = new Vector<CreditRating>();
        Vector<CreditRating> cptyCreditRatings = new Vector<CreditRating>();
        double thresholdOwner = 0.00;
        double mtaOwner = 0.00;
        double thresholdCpty = 0.00;
        double mtaCpty = 0.00;
        boolean calculateOwnerValues = true, calculateCptyValues = true;

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

        /**************** OWNER stuff *****************/

        if (ELBEandKGRutilities.isOwnerKGRcontractDependingOnRating(marginCall)) {
            // get owner credit ratings
            MarginCallCreditRatingConfiguration mccRatingConfigOwner = null;
            try {
                mccRatingConfigOwner = CollateralUtilities.getMCRatingConfiguration(marginCall.getPoRatingsConfigId());
                ownerCreditRatings = ELBEandKGRutilities.getCreditRatingsForLE(marginCall, agencies, marginCall
                        .getProcessingOrg().getId(), valueDate, mccRatingConfigOwner.getRatingType());
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
            thresholdOwner = getOwnerThresholdCcy(marginCall, ownerCreditRatings);
            mtaOwner = getOwnerMtaCcy(marginCall, ownerCreditRatings);
        }

        setThresholdOwner(marginCallItem, marginCall, thresholdOwner);
        setMtaOwner(marginCallItem, marginCall, mtaOwner);

        /**************** OWNER stuff *****************/

        /**************** CPTY stuff ******************/

        if (ELBEandKGRutilities.isCptyKGRcontractDependingOnRating(marginCall)) {
            // get cpty credit ratings
            MarginCallCreditRatingConfiguration mccRatingConfigCpty = null;
            try {
                mccRatingConfigCpty = CollateralUtilities.getMCRatingConfiguration(marginCall.getLeRatingsConfigId());
                cptyCreditRatings = ELBEandKGRutilities.getCreditRatingsForLE(marginCall, agencies, marginCall
                        .getLegalEntity().getId(), valueDate, mccRatingConfigCpty.getRatingType());
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
            thresholdCpty = getCptyThresholdCcy(marginCall, cptyCreditRatings);
            mtaCpty = getCptyMtaCcy(marginCall, cptyCreditRatings);
        }

        setThresholdCpty(marginCallItem, marginCall, thresholdCpty);
        setMtaCpty(marginCallItem, marginCall, mtaCpty);

        /**************** CPTY stuff ******************/

        // INDEPENDENT AMOUNT for both
        // indAmountOWNER
        double indAmountOwner = 0.00;
        indAmountOwner = getOwnerIndAmountCcy(marginCall, ownerCreditRatings);
        // indAmountCPTY
        double indAmountCpty = 0.00;
        indAmountCpty = getCptyIndAmountCcy(marginCall, cptyCreditRatings);

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
        final String calcPeriod = CollateralUtilities.converseCalcPeriodKGRContracts(getCalcPeriod(marginCall));
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

    // Instruments
    private Opt_Collateral_MarginCallItem getKGR_Collateral_InstrumentsItem(final CollateralConfig marginCall,
                                                                            final DSConnection dsConn, final Object value, final Vector<String> errors) {
        final Opt_Collateral_MarginCallItem marginCallInstrumentItem = new Opt_Collateral_MarginCallItem();

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
    private Opt_Collateral_MarginCallItem getKGR_Collateral_CashPositions(final CollateralConfig marginCall,
                                                                          final InventoryCashPosition inventoryCashPosition, final DSConnection dsConn, final Vector<String> errors) {
        final Opt_Collateral_MarginCallItem marginCallCashItem = new Opt_Collateral_MarginCallItem();
        double amount = 0;
        String value = "";

        if ((null != inventoryCashPosition)) {

            // Report fields
            marginCallCashItem.setHeader(getHeader() + "      "); // A?ADIR 6
            // espacios
            // blanco
            marginCallCashItem.setTransactionId(getTransactionId(inventoryCashPosition));
            marginCallCashItem.setTransactionType(getTransactionType(inventoryCashPosition));
            marginCallCashItem.setAction(getAction());
            marginCallCashItem.setBook(marginCall.getBook().getName());
            marginCallCashItem.setReconciliationType(getReconciliationType());
            marginCallCashItem.setCounterparty(getCounterparty(marginCall, dsConn));

            final String aliasEntityKGR = CollateralUtilities.getAliasEntityKGR(dsConn, this._poId);
            if (aliasEntityKGR.equals("")) {
                marginCallCashItem.setOwner(getOwner(marginCall, dsConn)); // office
            } else {
                marginCallCashItem.setOwner(aliasEntityKGR); // office
            }

            marginCallCashItem.setTransactionDate(processDate.toString());
            marginCallCashItem.setMaturityDate(processDate.addDays(7).toString());

            // Retrieve and save the value for the amount.
            amount = getCollateralAmount(inventoryCashPosition);

            // Report fields
            marginCallCashItem.setIsReceived(getIsReceived(amount));
            marginCallCashItem.setContractCcy(getContractCcyCash(inventoryCashPosition)); // currency

            value = CollateralUtilities.formatNumber(Math.abs(amount));
            if (value.contains(",")) {
                marginCallCashItem.setCollateralAmount(value.replace(',', '.'));
            } else {
                marginCallCashItem.setCollateralAmount(value);
            }

            marginCallCashItem.setContractType(getContractType(marginCall)); // agreement
            // type
            if (getMarginCallContract(marginCall) == null) {
                marginCallCashItem.setMarginCallContract(""); // agreement
                // id
            } else {
                marginCallCashItem.setMarginCallContract(getMarginCallContract(marginCall)); // agreement
                // id
            }

            // added
            value = CollateralUtilities.formatNumber(getIndependentAmount(marginCall));
            if (value.contains(",")) {
                marginCallCashItem.setIndependentAmount(value.replace(',', '.'));
            } else {
                marginCallCashItem.setIndependentAmount(value);
            }

            marginCallCashItem.setSource(getSource());

            // new field
            marginCallCashItem.setConcilia(CONCILIA_FIELD);

            return marginCallCashItem;
        }

        return null;
    }

    // Securities
    private Opt_Collateral_MarginCallItem getKGR_Collateral_SecurityPositions(final CollateralConfig marginCall,
                                                                              final InventorySecurityPosition inventorySecurityPosition, final DSConnection dsConn,
                                                                              final Vector<String> errors, final PricingEnv pricingEnv) {
        final Opt_Collateral_MarginCallItem marginCallSecItem = new Opt_Collateral_MarginCallItem();
        double amount = 0;
        String value = "";

        if ((null != inventorySecurityPosition)) { // get only alive

            // We retrieve the bond product.
            final Security security = getSecurityFromSecurityPosition(inventorySecurityPosition);
            if (security != null) {

                // Report fields
                marginCallSecItem.setHeader(getHeader() + "      "); // A?ADIR 6
                // espacios
                // blanco
                marginCallSecItem.setTransactionId(limitFieldSize(getTransactionIdSecurity(inventorySecurityPosition, security), 37));
                marginCallSecItem.setTransactionType(getTransactionTypeSecurity(inventorySecurityPosition));
                marginCallSecItem.setBook(marginCall.getBook().getName());
                marginCallSecItem.setAction(getAction());
                marginCallSecItem.setReconciliationType(getReconciliationType());
                marginCallSecItem.setCounterparty(getCounterparty(marginCall, dsConn));

                final String aliasEntityKGR = CollateralUtilities.getAliasEntityKGR(dsConn, this._poId);
                if (aliasEntityKGR.equals("")) {
                    marginCallSecItem.setOwner(getOwner(marginCall, dsConn)); // office
                } else {
                    marginCallSecItem.setOwner(aliasEntityKGR); // office
                }

                if (getIssuer(dsConn, security) != null) {
                    marginCallSecItem.setIssuer(getIssuer(dsConn, security));
                } else {
                    marginCallSecItem.setIssuer("");
                }

                marginCallSecItem.setTransactionDate(processDate.toString());
                marginCallSecItem.setMaturityDate(processDate.addDays(7).toString());

                marginCallSecItem.setBondMaturityDate(getBondMaturityDate(security) != null ? getBondMaturityDate(security).toString() : "");
                marginCallSecItem.setIsin(getIsin(security));

                // Retrieve and save the value for the amount.
                if (security instanceof Bond) {
                    amount = getCollateralAmountSecurity(inventorySecurityPosition)
                            * ((Bond) security).getFaceValue(); // #1-778 -
                    // Multiply
                    // quantity with
                    // faceValue to
                    // get
                    // nominal
                } else if (security instanceof Equity) {
                    amount = getCollateralAmountSecurity(inventorySecurityPosition);
                }

                marginCallSecItem.setIsReceived(getIsReceived(amount));

                value = CollateralUtilities.formatNumber(Math.abs(amount));
                if (value.contains(",")) {
                    marginCallSecItem.setCollateralAmount(value.replace(',', '.'));
                } else {
                    marginCallSecItem.setCollateralAmount(value);
                }

                marginCallSecItem.setContractCcy(getContractCcySec(inventorySecurityPosition)); // currency

                value = CollateralUtilities.formatNumber(getDirtyPrice(security, valueDate)); // #3-778
                if (value.contains(",")) {
                    marginCallSecItem.setDirtyPrice(value.replace(',', '.'));
                } else {
                    marginCallSecItem.setDirtyPrice(value);
                }

                value = CollateralUtilities.formatNumber(getHaircut(marginCall, inventorySecurityPosition)); // #4-778
                if (value.contains(",")) {
                    marginCallSecItem.setHaircut(value.replace(',', '.'));
                } else {
                    marginCallSecItem.setHaircut(value);
                }

                marginCallSecItem.setContractType(getContractType(marginCall)); // agreement
                // type
                if (getMarginCallContract(marginCall) == null) {
                    marginCallSecItem.setMarginCallContract(""); // agreement
                    // id
                } else {
                    marginCallSecItem
                            .setMarginCallContract(getMarginCallContract(marginCall)); // agreement
                    // id
                }
                // added
                marginCallSecItem.setSource(getSource());

                // new field
                marginCallSecItem.setConcilia(CONCILIA_FIELD);
            } else {
                return null;
            }

            return marginCallSecItem;
        }

        return null;
    }

    // Counterparties
    private Opt_Collateral_MarginCallItem getKGR_Collateral_CounterpartyItem(final CollateralConfig marginCall,
                                                                             final LegalEntity le, final DSConnection dsConn, final Vector<String> errors) {
        final Opt_Collateral_MarginCallItem marginCallCptyItem = new Opt_Collateral_MarginCallItem();

        marginCallCptyItem.setMarginCallContract(getMarginCallContract(marginCall));
        marginCallCptyItem.setCounterparty(le.getAuthName());
        marginCallCptyItem.setContractLongName(le.getName()); // cpty long name
        final String headClone = CollateralUtilities.converseHeadCloneKGRContracts(getHeadClone(marginCall));
        marginCallCptyItem.setHeadClone(headClone);

        return marginCallCptyItem;
    }

    // Processing Orgs (branch file)
    private Opt_Collateral_MarginCallItem getKGR_Collateral_ProcessingOrgItem(final CollateralConfig marginCall,
                                                                              final LegalEntity po, final DSConnection dsConn, final Vector<String> errors) {
        final Opt_Collateral_MarginCallItem marginCallPoItem = new Opt_Collateral_MarginCallItem();

        marginCallPoItem.setMarginCallContract(getMarginCallContract(marginCall));
        marginCallPoItem.setOwner(po.getAuthName());
        marginCallPoItem.setOwnerName(po.getName());
        final String headClone = CollateralUtilities.converseHeadCloneKGRContracts(getHeadClone(marginCall));
        marginCallPoItem.setHeadClone(headClone);

        return marginCallPoItem;
    }

    // ** Methods ** //
    public double getHaircut(final CollateralConfig marginCall, InventorySecurityPosition secPos) {

        double value = 0.0;

        // get HaircutProxy for contract
        CollateralFilterProxy filterProxy = new CachedCollateralFilterProxy();
        HaircutProxyFactory fact = new HaircutProxyFactory(filterProxy);
        HaircutProxy haircutProxy = fact.getProxy(marginCall.getPoHaircutName());

        Product p = secPos.getProduct();
        if (p != null) {
            // get haircut value for security
            //JRL 20/04/2016 Migration 14.4
            value = 1 - Math.abs(((haircutProxy.getHaircut(marginCall.getCurrency(), new CollateralCandidate(p), processDate,
                    true, marginCall, "Pay")))); // get
            // percentage
            // and set
            // 100-%

        }

        return value * 100;
    }

    // 778 - New method to calculate dirtyPrice, find quote value from ISIN for valDate and get close
    @SuppressWarnings("unchecked")
    public double getDirtyPrice(Security security, JDate valDate) {
        Vector<QuoteValue> vQuotes = new Vector<QuoteValue>();
        if (security != null) {

            if (security instanceof Bond) {
                String isin = ((Bond) security).getSecCode("ISIN");
                String quoteName;
                try {
                    quoteName = CollateralUtilities.getQuoteNameFromISIN(isin,
                            valDate);
                    if (!quoteName.equals("")) {
                        String clausule = "quote_name = "
                                + "'"
                                + quoteName
                                + "' AND trunc(quote_date) = to_date('"
                                + valDate
                                + "', 'dd/mm/yy') AND quote_set_name = 'DirtyPrice' AND quote_type = 'DirtyPrice'";
                        vQuotes = DSConnection.getDefault()
                                .getRemoteMarketData().getQuoteValues(clausule);
                        if ((vQuotes != null) && (vQuotes.size() > 0)) {
                            return vQuotes.get(0).getClose() * 100;
                        }
                    }
                } catch (RemoteException e1) {
                    Log.error(this, "Cannot retrieve dirty price", e1);

                }
            }

            if (security instanceof Equity) {
                String isin = ((Equity) security).getSecCode("ISIN");
                String quoteName;
                try {
                    quoteName = CollateralUtilities.getQuoteNameFromISIN(isin,
                            valDate);
                    if (!quoteName.equals("")) {
                        String clausule = "quote_name = "
                                + "'"
                                + quoteName
                                + "' AND trunc(quote_date) = to_date('"
                                + valDate
                                + "', 'dd/mm/yy') AND quote_set_name = 'OFFICIAL' AND quote_type = 'Price'";
                        vQuotes = DSConnection.getDefault()
                                .getRemoteMarketData().getQuoteValues(clausule);
                        if ((vQuotes != null) && (vQuotes.size() > 0)) {
                            return vQuotes.get(0).getClose() * 100;
                        }
                    }
                } catch (RemoteException e1) {
                    Log.error(this, "Cannot retrieve dirty price", e1);

                }
            }
        }

        return 0.00;
    }

    private String getIsin(final Security security) {
        if (security instanceof Bond) {
            return ((Bond) security).getSecCode(ISIN);
        }
        if (security instanceof Equity) {
            return ((Equity) security).getSecCode(ISIN);
        }
        return null;
    }

    private String getHeader() {
        return HEADER;
    }

    private String getContractCcyCash(final InventoryCashPosition inventoryCashPosition) {
        return inventoryCashPosition.getCurrency();
    }

    private String getContractCcySec(final InventorySecurityPosition inventorySecurityPosition) {
        return inventorySecurityPosition.getSettleCurrency(); // #5-778 - Changed return value to get position value
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
        // return marginCall.getAdditionalField(ONE_WAY);
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

    private JDate getBondMaturityDate(final Security security) {
        if (security instanceof Bond) {
            return ((Bond) security).getMaturityDate();
        }
        if (security instanceof Equity) {
            return ((Equity) security).getMaturityDate();
        }
        return null;
    }

    private String getIssuer(final DSConnection dsConn, final Security security) {
        String strToReturn = null;
        LegalEntity le = new LegalEntity();
        try {
            le = dsConn.getRemoteReferenceData().getLegalEntity(security.getIssuerId());
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

    private String getTransactionIdSecurity(final InventorySecurityPosition inventorySecurityPosition, final Security security) {
        final String transactionId = inventorySecurityPosition.getConfigId() + getIsin(security);
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
            iaReturn = Double.parseDouble(marginCall.getAdditionalField(CONTRACT_INDEPENDENT_AMOUNT));
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
                    .getInventorySecurityPositions(where.toString(), null);
            return secPositions;
        } catch (RemoteException e) {
            Log.error(Opt_Collateral_MarginCallLogic.class, e); //sonar
        }

        return null;
    }

    public Security getSecurityFromSecurityPosition(InventorySecurityPosition invSecPos) {
        if (invSecPos != null) {
            Product p = invSecPos.getProduct();
            if ((p != null) && (p instanceof Security)) {
                return (Security) p;
            }
        }
        return null;
    }

    public boolean isBalanceZero(InventorySecurityPosition invSecPos) {
        if (invSecPos != null) {

            Double balance = getCollateralAmountSecurity(invSecPos);
            if (roundPositionValue(balance, 2) == 0.0) {
                return true;
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

    public void setThresholdOwner(Opt_Collateral_MarginCallItem marginCallItem, CollateralConfig marginCall,
                                  double thresholdOwner) {
        String value = CollateralUtilities.formatNumber(Math.abs(thresholdOwner)); // set abs
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

    public void setThresholdCpty(Opt_Collateral_MarginCallItem marginCallItem, CollateralConfig marginCall,
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

    public void setMtaOwner(Opt_Collateral_MarginCallItem marginCallItem, CollateralConfig marginCall, double mtaOwner) {
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

    public void setMtaCpty(Opt_Collateral_MarginCallItem marginCallItem, CollateralConfig marginCall, double mtaCpty) {
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

    // if both LE have IA non depending on rating (ownerIA=cptyIA=contractIA) we follow this logic: ia>=0 ->
    // cptyIA=0, ownerIA=valor // ia<0->
    // ownerIA=0, cptyIA=valor
    // if not, we set each IA getted in each side
    public void setIndependentAmount(Opt_Collateral_MarginCallItem marginCallItem, CollateralConfig marginCall,
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
