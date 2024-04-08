package calypsox.tk.report;

import calypsox.util.ELBEandKGRutilities;
import calypsox.util.collateral.CollateralManagerUtil;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.InventoryCashPosition;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.collateral.filter.CollateralFilterProxy;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.filter.impl.CachedCollateralFilterProxy;
import com.calypso.tk.collateral.optimization.candidat.CollateralCandidate;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.CollateralServiceRegistry;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.BondAssetBacked;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.CollateralConfigCurrency;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.report.BOSecurityPositionReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.InventoryCashPositionArray;
import com.calypso.tk.util.InventorySecurityPositionArray;
import com.calypso.tk.util.MarginCallPositionUtil;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class KGR_Collateral_MarginCallLogic {
    protected static final String OWNER = "Owner";
    protected static final String OWNER_SENTINEL = "OwnerSentinel";
    protected static final String CONCILIA_FIELD = "MAD_CAL_COL";
    protected static final Integer DEFAULT_MATURE_DAYS = 7;
    protected static final String CONCILIA_FIELD_EQUITY = "MAD_SE";
    private static final String MASTER_AGREEMENT = "MASTER_AGREEMENT";
    private static final String MA_SIGN_DATE = "MA_SIGN_DATE";
    private static final String NONE = "None";
    private static final String LEGAL_ENTITY = "Legal Entity";
    private static final String HEADER = "D01StdI#BATCHGLCSDMIL  225";
    private static final String HEADER_EQUITY = "D01StdIDEALERGLCSDEALER  225";
    private static final String TRANSACTION_TYPE_SEC = "COLLBOND";
    private static final String TRANSACTION_TYPE_SEC_EQUITY = "COLLEQSH";
    private static final String RECONCILIATION_TYPE = "P";
    private static final String ACTION = "A";
    private static final String TRANSACTION_TYPE = "COLL";
    // Constants used.
    private static final String BLANK = "";
    private static final String HEAD_CLONE = "HEAD_CLONE";
    private static final String INSTRUMENT = "Instrument";
    private static final String COUNTERPARTY = "Counterparty";
    private static final String COUNTERPARTY_SENTINEL = "CounterpartySentinel";
    private static final String TRANSACTIONID = "Transaction ID";
    private static final String ISIN = "ISIN";
    private static final String GLOBAL_RATING = "GLOBAL RATING";
    private static final String CUSTOMIZED = "Customized";
    private static final String CONTRACT_IA = "CONTRACT_IA";
    private static final String DISPUTE_ADJ = "DISPUTE_ADJUSTMENT";
    private static final String CONTRACT_INDEPENDENT_AMOUNT = "CONTRACT_INDEPENDENT_AMOUNT";
    private static final String FREQUENCY = "FREQUENCY";
    private static final String PERCENT = "PERCENT";
    private static final String PO = "ProcessingOrg";
    @SuppressWarnings("unused")
    private static final String NOMINAL_STR = "Nominal";
    private static final String TRADE_STR = "Trade";
    private static final String MOVE_TYPE = "Balance";
    private static final String POS_CLASS_ST = "Margin_Call";
    private static final String POS_VALUE_ST = "Nominal";
    private static final String POSITION_TYPE = "KGRCollateral.PositionType";
    // added
    private static final String SOURCE = "CALYPSO";
    // Constants for CSD contracts
    private static final String CSD = "CSD";
    private static final String CSA = "CSA";
    private static final String IM_QEF_EXPORT_FILTER = "IM_QEF_EXPORT_FILTER";
    private static final String SDF = "Collateral Exposure subtype";
    private static final String SEGREGATED_COLLATERAL_CASH = "SEGREGATED_COLLATERAL_CASH";
    private static final String SEGREGATED_COLLATERAL_SEC = "SEGREGATED_COLLATERAL_SECURITIES";
    //private static final String SEGREGATED_COLLATERAL = "SEGREGATED_COLLATERAL";
    private static final String NO = "N";
    private static final String YES = "Y";
    private static final String IMIRISMapping = "IMIRISMapping";
    private static final String GUARANTEE_TYPE = "GUARANTEE_TYPE";
    private static final String IM_SUB_CONTRACTS = "IM_SUB_CONTRACTS";
    private static final String CSA_FACADE = "CSA_FACADE";
    private static final String IM_GLOBAL_ID = "IM_GLOBAL_ID";
    //private static final String filterUnd  = "UND_";
    private static final String filterUnd = "UND_";
    private static final String atrib_PO_IN = "ProcessingOrg : IN";
    private static final String atrib_PO_NOT_IN = "ProcessingOrg : NOT_IN";
    protected static boolean poFilt = false;
    // added
    private static JDate processDate;
    private static JDate valueDate;
    private final String COLLATERAL_TYPE_CASH = "CASH";
    private final String COLLATERAL_TYPE_SEC = "SECURITY";
    private final String COLLATERAL_TYPE_BOTH = "BOTH";
    private final int _poId;
    private final int _leId;
    private Map<Integer, Boolean> contractsDisputeStatus = new HashMap<>();

    public KGR_Collateral_MarginCallLogic(final CollateralConfig marginCall,
                                          Map<Integer, Boolean> contractsDisputeStatus) {
        this._poId = marginCall.getPoId();
        this._leId = marginCall.getLeId();
        this.contractsDisputeStatus = contractsDisputeStatus;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static synchronized Vector<KGR_Collateral_MarginCallItem> getReportRows(final CollateralConfig marginCall,
                                                                                   final MarginCallPositionUtil marginCallPosition, final JDate jdate, final DSConnection dsConn,
                                                                                   final Vector<String> errorMsgs, final String[] columns, final PricingEnv pricingEnv,
                                                                                   final CollateralServiceRegistry collServReg, Vector holidaysVector, Map<Integer, Boolean> disputeStatus,
                                                                                   String irisSourceSystem, Integer maturityOffset) throws RemoteException {

        final Vector<KGR_Collateral_MarginCallItem> reportRows = new Vector<KGR_Collateral_MarginCallItem>();
        final KGR_Collateral_MarginCallLogic verifiedRow = new KGR_Collateral_MarginCallLogic(marginCall,
                disputeStatus);
        InventoryCashPosition inventoryCashPosition = null;
        InventorySecurityPositionArray inventorySecurityPositionArray = null;
        KGR_Collateral_MarginCallItem rowCreated = null;

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
                if (columns[11].equalsIgnoreCase(ISIN)) {
                    // get security positions for a contract
                    inventorySecurityPositionArray = getSecurityPositions(jdate, marginCall, dsConn);

                    addMexSecVsSecPositionBeans(inventorySecurityPositionArray, marginCall, jdate, pricingEnv, irisSourceSystem);

                    // #2-778
                    if ((inventorySecurityPositionArray != null) && !inventorySecurityPositionArray.isEmpty()) {

                        for (int secPos = 0; secPos < inventorySecurityPositionArray.size(); secPos++) {
                            // don't show positions with balance=0

                            if (inventorySecurityPositionArray.get(secPos).getProduct() instanceof Bond) {
                                if (!verifiedRow.isBalanceZero(inventorySecurityPositionArray.get(secPos))) {
                                    rowCreated = verifiedRow.getKGR_Collateral_SecurityPositions(marginCall,
                                            inventorySecurityPositionArray.get(secPos), dsConn, errorMsgs, pricingEnv,
                                            irisSourceSystem, maturityOffset);
                                    if (null != rowCreated) { // If the result
                                        // row is equals
                                        // to NULL, we don't add this
                                        // row to the report.
                                        reportRows.add(rowCreated);
                                    }
                                }
                            }
                        }
                    }

                } else if (columns[12].equalsIgnoreCase(ISIN)) {

                    // get security positions for a contract
                    inventorySecurityPositionArray = getSecurityPositions(jdate, marginCall, dsConn);

                    addMexSecVsSecPositionBeans(inventorySecurityPositionArray, marginCall, jdate, pricingEnv, irisSourceSystem);

                    // #2-778
                    if ((inventorySecurityPositionArray != null) && !inventorySecurityPositionArray.isEmpty()) {

                        for (int secPos = 0; secPos < inventorySecurityPositionArray.size(); secPos++) {
                            // don't show positions with balance=0
                            // balance para equity??
                            if (inventorySecurityPositionArray.get(secPos).getProduct() instanceof Equity) {
                                // comprobar cero temporalmente eliminado
                                if (!verifiedRow.isBalanceZero(inventorySecurityPositionArray.get(secPos))) {
                                    rowCreated = verifiedRow.getKGR_Collateral_SecurityPositions(marginCall,
                                            inventorySecurityPositionArray.get(secPos), dsConn, errorMsgs, pricingEnv,
                                            irisSourceSystem, maturityOffset);
                                    if (null != rowCreated) { // If the result
                                        // row is equals
                                        // to NULL, we don't add this
                                        // row to the report.
                                        reportRows.add(rowCreated);
                                    }
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

                            String where = buildMCCashPositionQuery(jdate, marginCall,
                                    currencies.get(cash).getCurrency());
                            inventoryCashPosition = getLastCashPosition(dsConn, where);
                            // GSM 01/03/16 - Not working, error core query =
                            // AND config_id = 0 AND mcc_id = 98368
                            // inventoryCashPosition =
                            // BOPositionUtil.getCashPosition((currencies.get(cash)).getCurrency(),
                            // "MARGIN_CALL", "ACTUAL", "TRADE", jdate,
                            // marginCall.getId(), dsConn, null); // 777 -
                            // // Changed
                            // // second
                            // // parameter
                            // // to
                            // // ACTUAL
                            // We retrieve each row to show in the CSV file or
                            // the report.
                            rowCreated = verifiedRow.getKGR_Collateral_CashPositions(marginCall, inventoryCashPosition,
                                    dsConn, errorMsgs, irisSourceSystem, maturityOffset);
                            if (null != rowCreated) { // If the result row is
                                // equals to NULL, we
                                // don't add this row to
                                // the report.

                                reportRows.add(rowCreated);
                            }
                        }
                    } else if (marginCall.getCurrency() != null) {
                        final String currency = marginCall.getCurrency();
                        String where = buildMCCashPositionQuery(jdate, marginCall, currency);
                        inventoryCashPosition = getLastCashPosition(dsConn, where);

                        // GSM 01/03/16 - Not working, error core query = AND
                        // config_id = 0 AND mcc_id = 98368
                        // inventoryCashPosition =
                        // BOPositionUtil.getCashPosition(currency,
                        // "MARGIN_CALL", "ACTUAL",
                        // "TRADE", jdate, marginCall.getId(), dsConn, null);
                        rowCreated = verifiedRow.getKGR_Collateral_CashPositions(marginCall, inventoryCashPosition,
                                dsConn, errorMsgs, irisSourceSystem, maturityOffset);
                        if (null != rowCreated) { // If the result row is
                            // equals to NULL, we
                            // don't add this row to
                            // the report.

                            reportRows.add(rowCreated);
                        }

                    }

                }
            } else if (columns[1].equals(OWNER_SENTINEL)) {
                parseOwner(rowCreated, reportRows, verifiedRow, errorMsgs, dsConn, marginCall);
            } else if (columns[1].equals(COUNTERPARTY_SENTINEL)) {
                rowCreated = verifiedRow.getKGR_Collateral_CounterpartyItem(marginCall, marginCall.getLegalEntity(),
                        dsConn, errorMsgs);
                parseCPTY(rowCreated, reportRows, verifiedRow, errorMsgs, dsConn, marginCall);
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
            if (columns[1].equals(OWNER)) {
                parseOwner(rowCreated, reportRows, verifiedRow, errorMsgs, dsConn, marginCall);

            } else if (columns[1].equals(COUNTERPARTY)) {
                rowCreated = verifiedRow.getKGR_Collateral_CounterpartyItem(marginCall, marginCall.getLegalEntity(),
                        dsConn, errorMsgs);
                parseCPTY(rowCreated, reportRows, verifiedRow, errorMsgs, dsConn, marginCall);

            } else if (columns[1].equals(INSTRUMENT)) {
                // Obtain product list
                final Vector elements = marginCall.getProductList();
                ArrayList<String> instrumentTypesProcessed = new ArrayList<String>();
                if (elements != null) {
                    for (int i = 0; i < elements.size(); i++) {
                        // if product equals "CollateralExposure" obtain
                        // exposure types
                        if (elements.get(i).equals("CollateralExposure")) {
                            //**** New block for CSD contracts
                            List exposureTypes = null;
                            //Checks if the contract type is CSD
                            if (marginCall.getContractType().equals(CSD)) {
                                CollateralConfig csa = KGR_Collateral_MarginCallLogic.getCSAfromCSD(marginCall);
                                if (csa != null) {
                                    //Extract the Static Data Filter from the additional attr IM_QEF_EXPORT_FILTER
                                    String filter = marginCall.getAdditionalField(IM_QEF_EXPORT_FILTER);
                                    try {
                                        StaticDataFilter sdf = filter != null ? dsConn.getRemoteReferenceData().getStaticDataFilter(filter) : null;
                                        if (sdf != null) {
                                            //Searches for the correct filter, "Collateral Exposure subtype"
                                            for (StaticDataFilterElement element : sdf.getElements()) {
                                                if (element.getName() != null && element.getName().equals(SDF)) {
                                                    if (element.getExpandedValues() != null) {
                                                        //Reads its elements, the product list
                                                        exposureTypes = Collections.list(element.getExpandedValues().elements());
                                                    }
                                                    break;
                                                }
                                            }
                                            if (exposureTypes == null) exposureTypes = csa.getExposureTypeList();
                                        } else {
                                            exposureTypes = csa.getExposureTypeList();
                                        }
                                    } catch (CalypsoServiceException e) {
                                        Log.warn(KGR_Collateral_MarginCallLogic.class, e); //sonar
                                        exposureTypes = csa.getExposureTypeList();
                                    }
                                }
                            }
                            //If it couldn't find any products, it does the default logic
                            if (exposureTypes == null) exposureTypes = marginCall.getExposureTypeList();
                            //****

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
                                            try {
                                                mappedExposureType = mappedExposureType.substring(0, mappedExposureType.indexOf('-'));
                                            } catch (Exception e) {
                                                Log.error(KGR_Collateral_MarginCallLogic.class, e); //sonar
                                            }
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
                /*case for the UND_ filter*/
            }
        }

        return reportRows;
    }

    private static void parseCPTY(KGR_Collateral_MarginCallItem rowCreated, Vector<KGR_Collateral_MarginCallItem> reportRows, KGR_Collateral_MarginCallLogic verifiedRow, Vector<String> errorMsgs, DSConnection dsConn, CollateralConfig marginCall) {

        // This set is used to check that no Counterparty is included
        // twice.
        final Set<LegalEntity> includedCounterparties = new TreeSet<LegalEntity>();

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

    private static void parseOwner(KGR_Collateral_MarginCallItem rowCreated, Vector<KGR_Collateral_MarginCallItem> reportRows, KGR_Collateral_MarginCallLogic verifiedRow, Vector<String> errorMsgs, DSConnection dsConn, CollateralConfig marginCall) throws CalypsoServiceException {
        poFilt = false;

        StaticDataFilter sdf = dsConn.getRemoteReferenceData()
                .getStaticDataFilter(filterUnd + marginCall.getName());

        final Set<LegalEntity> includedProcessingOrgs = new TreeSet<LegalEntity>();


        if (sdf != null) {//Has got a UND_filter
            Vector<StaticDataFilterElement> elements = sdf.getElements();
            if (elements != null) {//The different kinds of Atributtes in the MAIN_ filter

                for (StaticDataFilterElement sdfe : elements) {//One iteration for every kind of filter_element

                    if (sdfe.getName().equals(PO)) { //If there are a ProcessingOrg Atributte:

                        Vector<String> values = sdfe.getValues();//PO and branches
                        List<LegalEntity> additionalPO = marginCall.getAdditionalPO();
                        if (values != null) {
                            if (atrib_PO_IN.equals(sdfe.toString().substring(0, 18))) { //filter PO_IN activated

                                LegalEntity poOwner = marginCall.getProcessingOrg();
                                for (String value : values) {
                                    LegalEntity po = dsConn.getRemoteReferenceData().getLegalEntity(value);
                                    if (po != null) {
                                        addToPORow_IN(rowCreated, marginCall, includedProcessingOrgs, reportRows, errorMsgs, dsConn, verifiedRow, po, values, poOwner, additionalPO);
                                    }

                                }

                            } else if (atrib_PO_NOT_IN.equals(sdfe.toString().substring(0, 22))) { //filter PO NOT_IN activated
                                LegalEntity poOwner = marginCall.getProcessingOrg();
                                for (String value : values) {
                                    LegalEntity po = dsConn.getRemoteReferenceData().getLegalEntity(value);
                                    if (po != null && poOwner != null) {
                                        addToPORowNotIn(rowCreated, marginCall, includedProcessingOrgs, reportRows, errorMsgs, dsConn, verifiedRow, poOwner, values, additionalPO, po);
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }

        if (!(poFilt)) { //if haven?t got any PO_ATRIBUTTE
            //final Set<LegalEntity> includedProcessingOrgs = new TreeSet<LegalEntity>();
            LegalEntity poOwner = marginCall.getProcessingOrg();
            List<LegalEntity> additionalPO = marginCall.getAdditionalPO();
            if (poOwner != null && !includedProcessingOrgs.contains(poOwner)) {
                rowCreated = verifiedRow.getKGR_Collateral_ProcessingOrgItem(marginCall, poOwner, dsConn, errorMsgs);
                if (rowCreated != null && !includedProcessingOrgs.contains(poOwner)) {
                    reportRows.add(rowCreated);
                    includedProcessingOrgs.add(poOwner);//Inserts PO in POs set.
                }
            }
            if ((additionalPO != null) && (additionalPO.size() > 0)) {
                for (int i = 0; i < additionalPO.size(); i++) {
                    if (additionalPO.get(i) != null && !includedProcessingOrgs.contains(additionalPO.get(i))) {
                        addToPORow(rowCreated, marginCall, includedProcessingOrgs, reportRows, errorMsgs, dsConn, verifiedRow, additionalPO.get(i));
                    }

                }
            }


        }

    }

    private static void addToPORowNotIn(KGR_Collateral_MarginCallItem rowCreated, CollateralConfig marginCall, Set<LegalEntity> includedProcessingOrgs,
                                        Vector<KGR_Collateral_MarginCallItem> reportRows, Vector<String> errorMsgs, DSConnection dsConn, KGR_Collateral_MarginCallLogic verifiedRow,
                                        LegalEntity poOwner, Vector<String> values, List<LegalEntity> additionalPO, LegalEntity po) {

        if (!(values.contains(poOwner.toString())) && !(includedProcessingOrgs.contains(poOwner))) {//case for the powner
            poFilt = true;
            addToPORow(rowCreated, marginCall, includedProcessingOrgs, reportRows, errorMsgs, dsConn, verifiedRow, poOwner);
        }

        if (additionalPO != null && additionalPO.size() > 0) {// if there are any additional PO
            for (int i = 0; i < additionalPO.size(); i++) {
                if (!(values.contains(additionalPO.get(i).toString()))) {
                    poFilt = true;
                    addToPORow(rowCreated, marginCall, includedProcessingOrgs, reportRows, errorMsgs, dsConn, verifiedRow, additionalPO.get(i));
                }

            }
        }


    }

    /**
     * Generate SecLending SecVsSec Position Bean for Mexico only whit PricingEnv (PE-MEXICO) or sourceSystem MEXBS_CALYPSOCSA ??????
     *
     * @param inventorySecurityPositionArray
     * @param marginCall
     * @param pricingEnv
     */
    private static void addMexSecVsSecPositionBeans(InventorySecurityPositionArray inventorySecurityPositionArray, CollateralConfig marginCall, JDate valDate, PricingEnv pricingEnv, String irisSourceSystem) {
        if (null != pricingEnv && null != inventorySecurityPositionArray && (SecLendingPositionUtil.PE_MEXICO.equalsIgnoreCase(pricingEnv.getName()) || SecLendingPositionUtil.MEXICO_SOURCESYSTEM.equalsIgnoreCase(irisSourceSystem))) {
            inventorySecurityPositionArray.addAll(SecLendingPositionUtil.getSecVsSecInventorySecurityPosition(marginCall, valDate));
        }
    }


    private static void addToPORow(KGR_Collateral_MarginCallItem rowCreated, CollateralConfig marginCall, Set<LegalEntity> includedProcessingOrgs,
                                   Vector<KGR_Collateral_MarginCallItem> reportRows, Vector<String> errorMsgs, DSConnection dsConn,
                                   KGR_Collateral_MarginCallLogic verifiedRow, LegalEntity po) {
        rowCreated = verifiedRow.getKGR_Collateral_ProcessingOrgItem(marginCall, po, dsConn, errorMsgs);
        if (rowCreated != null && !(includedProcessingOrgs.contains(po))) {
            reportRows.add(rowCreated);
            includedProcessingOrgs.add(po);//Inserts PO in POs set.
        }

    }


    private static void addToPORow_IN(KGR_Collateral_MarginCallItem rowCreated, CollateralConfig marginCall, Set<LegalEntity> includedProcessingOrgs,
                                      Vector<KGR_Collateral_MarginCallItem> reportRows, Vector<String> errorMsgs, DSConnection dsConn, KGR_Collateral_MarginCallLogic verifiedRow,
                                      LegalEntity po, Vector<String> values, LegalEntity poOwner, List<LegalEntity> additionalPO) {
        if ((po.equals(poOwner)) && !(includedProcessingOrgs.contains(poOwner))) {
            poFilt = true;
            addToPORow(rowCreated, marginCall, includedProcessingOrgs, reportRows, errorMsgs, dsConn, verifiedRow, po);
        }

        if (additionalPO != null && additionalPO.size() > 0) {// if there are any additional PO
            for (int i = 0; i < additionalPO.size(); i++) {
                if (values.contains(additionalPO.get(i).toString()) && !(includedProcessingOrgs.contains(po))) {//REVIEW ?POWNER?
                    poFilt = true;
                    addToPORow(rowCreated, marginCall, includedProcessingOrgs, reportRows, errorMsgs, dsConn, verifiedRow, po);
                }

            }
        }

    }


    private static CollateralConfig getCSAfromCSD(CollateralConfig csd) {
        String globalIDString = csd.getAdditionalField(IM_GLOBAL_ID);
        try {
            if (!Util.isEmpty(globalIDString)) {
                int globalId = Integer.parseInt(globalIDString);
                CollateralConfig facade = ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfig(globalId);
                if (CSA_FACADE.equals(facade.getContractType())) {
                    if (!Util.isEmpty(facade.getAdditionalField(IM_SUB_CONTRACTS))) {
                        String[] ids = facade.getAdditionalField(IM_SUB_CONTRACTS).split(",");
                        List<Integer> contractIds = new ArrayList<Integer>();
                        for (String id : ids) {
                            contractIds.add(Integer.valueOf(id));
                        }

                        MarginCallConfigFilter mcFilter = new MarginCallConfigFilter();
                        mcFilter.setContractIds(contractIds);
                        List<CollateralConfig> listCC = CollateralManagerUtil.loadCollateralConfigs(mcFilter);

                        for (CollateralConfig cc : listCC) {
                            if (CSA.equals(cc.getContractType())) {
                                return cc;
                            }
                        }
                    }
                }
            }
        } catch (NumberFormatException | CollateralServiceException e) {
            Log.error(KGR_Collateral_MarginCallLogic.class, e); //sonar
            return null;
        }
        return null;
    }

    // GSM 01/03/16 - Fix Migration v14
    // AAP This method's name is changed to getLastCashPositionMovements
    @SuppressWarnings({"unused", "deprecation"})
    private static InventoryCashPosition getLastCashPositionMovements(DSConnection dsConn, String where) {

        Vector<InventoryCashPosition> cashV = null;
        try {
            cashV = dsConn.getRemoteBO().getLastInventoryCashPositions("", where, null);
        } catch (CalypsoServiceException e) {
            Log.error(KGR_Collateral_MarginCallLogic.class, "Error gathering last cash position for query = " + where);
            Log.error(KGR_Collateral_MarginCallLogic.class, e); //sonar
        }

        if (!Util.isEmpty(cashV) && cashV.size() == 1)
            return cashV.get(0);
        else
            return null;
    }

    // AAP 06/04/16 - Refix MIG v14.4
    // Above method is redone because of Calypso's
    // getLastInventoryCashPositions doesnt'seem to have the same behaviour than
    // v12's
    @SuppressWarnings("unchecked")
    private static InventoryCashPosition getLastCashPosition(DSConnection dsConn, String where) {
        InventoryCashPosition position = null;
        try {
            // Complete CASH Positions Vector
            InventoryCashPositionArray positions = dsConn.getRemoteBO().getInventoryCashPositions("", where, null);
            // Filtering
            position = getLastPositions(positions.toVector());
        } catch (CalypsoServiceException e) {
            Log.error(KGR_Collateral_MarginCallLogic.class, "Error gathering last cash position for query = " + where);
            Log.error(KGR_Collateral_MarginCallLogic.class, e); //sonar
        }
        return position;
    }

    // AAP MIG 14.4 Works fine, but not proud of it, too complex for the final
    // aim
    public static InventoryCashPosition getLastPositions(Vector<InventoryCashPosition> vector) {
        Hashtable<String, InventoryCashPosition> hashtable = new Hashtable<>();
        for (int i = 0; i < vector.size(); i++) {
            InventoryCashPosition inventory = vector.get(i);
            String key = inventory.getUniqueKey(false);
            InventoryCashPosition inventory1 = hashtable.get(key);
            if (inventory1 == null) {
                inventory1 = inventory;
            } else {
                inventory1 = inventory1.getPositionDate().gte(inventory.getPositionDate()) ? inventory1 : inventory;
            }
            hashtable.put(key, inventory1);
        }
        InventoryCashPosition allBookAggregatePos = null;
        if (!hashtable.values().isEmpty()) {
            Vector<InventoryCashPosition> positions = new Vector<>(hashtable.values());
            allBookAggregatePos = positions.firstElement();
            // More O(n) complexity...
            for (int i = 1; i < positions.size(); i++) {
                allBookAggregatePos.addToTotal(positions.get(i));
            }
        }
        return allBookAggregatePos;
    }

    // GSM 01/03/16 - Fix Migration v14
    //AAP POSDATE < DATE
    private static String buildMCCashPositionQuery(JDate jdate, CollateralConfig marginCall, String currency) {

        StringBuffer where = new StringBuffer("internal_external = 'MARGIN_CALL' ");
        where.append(" AND position_date <= " + Util.date2SQLString(jdate));
        where.append(" AND position_type =  'ACTUAL' AND date_type = 'TRADE' AND config_id = 0");
        where.append(" AND mcc_id = " + marginCall.getId());
        where.append(" AND currency_code = " + Util.string2SQLString(currency));
        return where.toString();
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

        String positionType = getPositionType(dsConn);

        StringBuilder where = new StringBuilder();
        where.append(" inv_sec_balance.internal_external = 'MARGIN_CALL' ");
        where.append(" AND inv_sec_balance.date_type = 'TRADE' ");
        where.append(" AND inv_sec_balance.position_type = '" + positionType + "'");
        where.append(" AND inv_sec_balance.mcc_id = ");
        where.append(marginCall.getId());
        try {
            BOSecurityPositionReportTemplate invTemplate = buildReportTemplate(positionType);
            return DSConnection.getDefault().getRemoteInventory().getSecurityPositionsFromTo("", where.toString(), date, date, invTemplate, null);
        } catch (CalypsoServiceException exc) {
            Log.error(KGR_Collateral_MarginCallLogic.class, "Cannot get securityPositions", exc);

        }

        return null;
    }

    /**
     * @param positionType
     * @return
     */
    private static BOSecurityPositionReportTemplate buildReportTemplate(String positionType) {
        BOSecurityPositionReportTemplate invTemplate = new BOSecurityPositionReportTemplate();
        invTemplate.put(BOSecurityPositionReportTemplate.POSITION_TYPE, positionType);
        invTemplate.put(BOSecurityPositionReportTemplate.POSITION_DATE, TRADE_STR);
        invTemplate.put(BOSecurityPositionReportTemplate.MOVE, MOVE_TYPE);
        invTemplate.put(BOSecurityPositionReportTemplate.POSITION_CLASS, POS_CLASS_ST);
        invTemplate.put(BOSecurityPositionReportTemplate.POSITION_VALUE, POS_VALUE_ST);
        return invTemplate;
    }

    /**
     * @param dsConn
     * @return
     */
    private static String getPositionType(DSConnection dsConn) {
        try {
            final Vector<String> domainValues = dsConn.getRemoteReferenceData().getDomainValues(POSITION_TYPE);
            if (!Util.isEmpty(domainValues)) {
                return domainValues.get(0);
            }
        } catch (CalypsoServiceException e) {
            Log.error(KGR_Collateral_MarginCallLogic.class.getName(), "Cannot get DomainValue KGRCollateral.PositionType");
        }
        //return default
        return "ACTUAL";
    }

    // ** Threshold & MTA & IA stuff ** //
    // threshold
    public static double getOwnerThresholdCcy(final CollateralConfig marginCall, Vector<CreditRating> creditRatings, JDate processDate, JDate valueDate) {

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

    public static double getCptyThresholdCcy(final CollateralConfig marginCall, Vector<CreditRating> creditRatings, JDate processDate, JDate valueDate) {

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

    // MTA
    public static double getOwnerMtaCcy(final CollateralConfig marginCall, Vector<CreditRating> creditRatings, JDate processDate, JDate valueDate) {

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

    public static double getCptyMtaCcy(final CollateralConfig marginCall, Vector<CreditRating> creditRatings, JDate processDate, JDate valueDate) {

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

    @SuppressWarnings("unused")
    private KGR_Collateral_MarginCallItem getKGR_Collateral_MarginCallItem(final Vector<String> errors) {
        return null;
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
    private KGR_Collateral_MarginCallItem getKGR_Collateral_MarginCallItem(final CollateralConfig marginCall,
                                                                           final DSConnection dsConn, final Vector<String> errors) {
        final KGR_Collateral_MarginCallItem marginCallItem = new KGR_Collateral_MarginCallItem();
        String value = "";
        Vector<String> agencies = null;
        Vector<CreditRating> ownerCreditRatings = new Vector<CreditRating>();
        Vector<CreditRating> cptyCreditRatings = new Vector<CreditRating>();
        double thresholdOwner = 0.00;
        double mtaOwner = 0.00;
        double thresholdCpty = 0.00;
        double mtaCpty = 0.00;
        boolean calculateOwnerValues = true, calculateCptyValues = true;

        //Contract Status

        marginCallItem.setStatus(marginCall.getAgreementStatus());

        //Contract Elegible Currencies
        marginCallItem.setEligible_currency(getEligibleCurrencies(marginCall));

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
                ownerCreditRatings = ELBEandKGRutilities.getCreditRatingsForLEs(marginCall, agencies,
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
            thresholdOwner = getOwnerThresholdCcy(marginCall, ownerCreditRatings, processDate, valueDate);
            mtaOwner = getOwnerMtaCcy(marginCall, ownerCreditRatings, processDate, valueDate);
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
                cptyCreditRatings = ELBEandKGRutilities.getCreditRatingsForLEs(marginCall, agencies,
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
            thresholdCpty = getCptyThresholdCcy(marginCall, cptyCreditRatings, processDate, valueDate);
            mtaCpty = getCptyMtaCcy(marginCall, cptyCreditRatings, processDate, valueDate);
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
        marginCallItem.setCalcPeriod(getCalcPeriod(marginCall));

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

        //Is blocked for Sentinel, for reporting purposes only.
        marginCallItem.setIsSentinelBlocked(String.valueOf(false));

        return marginCallItem;
    }


    private String getEligibleCurrencies(CollateralConfig marginCall) {
        StringBuffer currencies = new StringBuffer();
        if (marginCall != null) {
            List<CollateralConfigCurrency> elig_currencies = marginCall.getEligibleCurrencies();
            for (int i = 0; i < elig_currencies.size(); i++) {
                if (i != elig_currencies.size() - 1) {
                    currencies.append(elig_currencies.get(i).getCurrency() + ", ");
                } else {
                    currencies.append(elig_currencies.get(i).getCurrency());
                }
            }
        }

        return currencies.toString();
    }

    // Instruments
    private KGR_Collateral_MarginCallItem getKGR_Collateral_InstrumentsItem(final CollateralConfig marginCall,
                                                                            final DSConnection dsConn, final Object value, final Vector<String> errors) {
        final KGR_Collateral_MarginCallItem marginCallInstrumentItem = new KGR_Collateral_MarginCallItem();

        marginCallInstrumentItem.setMarginCallContract(getMarginCallContract(marginCall));
        if (value != null) {
            marginCallInstrumentItem.setInstrument(getInstrument(value.toString()));
        }
        // marginCallInstrumentItem.setOwnerName(this.getOwnerName(marginCall,
        // dsConn)); // instrumentDescription PENDING
        final String headClone = CollateralUtilities.converseHeadCloneKGRContracts(getHeadClone(marginCall));
        marginCallInstrumentItem.setHeadClone(headClone);

        marginCallInstrumentItem.setGlobalId(marginCall.getAdditionalField("IM_GLOBAL_ID"));
        marginCallInstrumentItem.setContractType(marginCall.getContractType());

        return marginCallInstrumentItem;
    }

    // Cash
    private KGR_Collateral_MarginCallItem getKGR_Collateral_CashPositions(final CollateralConfig marginCall,
                                                                          final InventoryCashPosition inventoryCashPosition, final DSConnection dsConn, final Vector<String> errors,
                                                                          String irisSourceSystem, Integer maturityOffset) {
        final KGR_Collateral_MarginCallItem marginCallCashItem = new KGR_Collateral_MarginCallItem();
        double amount = 0;
        final char fieldSeparator = 28;
        String value = "";

        if ((null != inventoryCashPosition)) {

            // Report fields
            marginCallCashItem.setHeader(getHeader() + "      "); // ANYDIR 6 espacios
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
            //Mig. v14 - Add collateral maturity offset days
            marginCallCashItem.setMaturityDate("16:" + processDate.addDays(maturityOffset).toString() + fieldSeparator);
            //marginCallCashItem.setMaturityDate("16:" + processDate.addDays(7).toString() + fieldSeparator);

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
            marginCallCashItem
                    .setOutstandingDisputes("350:" + getOutstandingDisputeStatus(marginCall) + fieldSeparator); // agreement
            // OutstandingDisputes
            marginCallCashItem
                    .setCollateralMarginType("351:" + getCollateralMarginType(marginCall) + fieldSeparator); // agreement
            // collateralMarginType

            marginCallCashItem.setCcp("352:" + getCollateralCCP(marginCall) + fieldSeparator);// CCP

            marginCallCashItem.setSegregatedCollateral("355:" + getSegregatedCollateral(marginCall, COLLATERAL_TYPE_CASH) + fieldSeparator);//Segregated Collateral

            marginCallCashItem.setMarketValue("101:" + getMarketValue(value) + fieldSeparator);

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
            // GSM 15/03/2016 - Add Source System for IRIS
            marginCallCashItem.setConcilia("221:" + irisSourceSystem + fieldSeparator);

            return marginCallCashItem;
        }

        return null;
    }

    private String getMarketValue(String value) {
        if (value.contains(",")) {
            value = value.replace(',', '.');
        }
        return value;
    }

    // Securities
    private KGR_Collateral_MarginCallItem getKGR_Collateral_SecurityPositions(final CollateralConfig marginCall,
                                                                              final InventorySecurityPosition inventorySecurityPosition, final DSConnection dsConn,
                                                                              final Vector<String> errors, final PricingEnv pricingEnv, String irisSourceSystem, Integer maturityOffset) {

        final KGR_Collateral_MarginCallItem marginCallSecItem = new KGR_Collateral_MarginCallItem();
        double amount = 0;
        final char fieldSeparator = 28;
        String value = "";
        Double price = 0.00;

        if ((null != inventorySecurityPosition)) { // get only alive

            // We retrieve the bond product.
            final Product product = getBondFromSecurityPosition(inventorySecurityPosition);

            if (product != null) {
                price = CollateralUtilities.getQuotePriceWithParentQuoteSet(product, valueDate, pricingEnv);
                if (price == 0.00 || price.isNaN()) {
                    Log.info(this, "Price is null, discarded position line");
                    return null;
                }

                // Report fields
                marginCallSecItem.setTransactionId(
                        "1:" + limitFieldSize(getTransactionIdSecurity(inventorySecurityPosition, product), 37)
                                + fieldSeparator);
                marginCallSecItem.setTransactionType(
                        "2:" + getTransactionTypeSecurity(inventorySecurityPosition, product) + fieldSeparator);

                marginCallSecItem.setAction("3:" + getAction() + fieldSeparator);
                marginCallSecItem.setReconciliationType("4:" + getReconciliationType() + fieldSeparator);
                marginCallSecItem.setCounterparty("5:" + getCounterparty(marginCall, dsConn) + fieldSeparator);

                final String aliasEntityKGR = CollateralUtilities.getAliasEntityKGR(dsConn, this._poId);
                if (aliasEntityKGR.equals("")) {
                    marginCallSecItem.setOwner("7:" + getOwner(marginCall, dsConn) + fieldSeparator); // office
                } else {
                    marginCallSecItem.setOwner("7:" + aliasEntityKGR + fieldSeparator); // office
                }
                if (getIssuer(dsConn, product) != null) {
                    marginCallSecItem.setIssuer("72:" + getIssuer(dsConn, product) + fieldSeparator);
                } else {
                    marginCallSecItem.setIssuer("72:" + fieldSeparator);
                }

                marginCallSecItem.setTransactionDate("10:" + processDate.toString() + fieldSeparator);

                marginCallSecItem.setIsin("62:" + getIsin(product) + fieldSeparator);

                // Retrieve and save the value for the amount.
                // product and equity no tiene faceValue
                if (product instanceof Bond) {
                    marginCallSecItem.setHeader(getHeader() + "      "); // AÑADIR
                    // 6
                    String mdate = "";
                    if (getBondMaturityDate(product) != null) {
                        mdate = getBondMaturityDate(product).toString() + fieldSeparator;
                    }

                    marginCallSecItem.setBondMaturityDate("201:" + mdate);
                    marginCallSecItem.setUnderlyingDuration("353:" + mdate);
                    //Mig. v14 - Add collateral maturity offset days
                    marginCallSecItem.setMaturityDate("16:" + processDate.addDays(maturityOffset).toString() + fieldSeparator);
                    //marginCallSecItem.setMaturityDate("16:" + processDate.addDays(7).toString() + fieldSeparator);
                    marginCallSecItem.setConcilia("221:" + irisSourceSystem + fieldSeparator);
                    //BAU ACD
                    if (product instanceof BondAssetBacked) {
                        //poolFactor viene implicito en el nominal/amount
                        amount = getCollateralAmountSecurity(inventorySecurityPosition) * ((Bond) product).getPoolFactor(valueDate) * ((Bond) product).getFaceValue();
                    } else {
                        amount = getCollateralAmountSecurity(inventorySecurityPosition) * ((Bond) product).getFaceValue(); // #1-778 // -
                        // Multiply
                        // quantity
                        // with
                        // faceValue
                        // to
                        // get
                        // nominal
                    }

                } else if (product instanceof Equity) {
                    marginCallSecItem.setHeader(HEADER_EQUITY + "      ");
                    marginCallSecItem.setConcilia("221:" + irisSourceSystem + fieldSeparator);
                    marginCallSecItem.setMaturityDate("16:" + processDate.addDays(maturityOffset).toString() + fieldSeparator);


                    amount = getCollateralAmountSecurity(inventorySecurityPosition) * price;
                }

                marginCallSecItem.setSegregatedCollateral("355:" + getSegregatedCollateral(marginCall, COLLATERAL_TYPE_SEC) + fieldSeparator);

                marginCallSecItem.setIsReceived("93:" + getIsReceived(amount) + fieldSeparator);

                value = CollateralUtilities.formatNumber(Math.abs(amount));

                if (value.contains(",")) {
                    marginCallSecItem.setCollateralAmount("21:" + value.replace(',', '.') + fieldSeparator);
                } else {
                    marginCallSecItem.setCollateralAmount("21:" + value + fieldSeparator);
                }

                marginCallSecItem.setContractCcy("20:" + getContractCcySec(inventorySecurityPosition) + fieldSeparator); // currency

                if (product instanceof Bond) {
                    value = CollateralUtilities.formatNumber(price); // #3-778
                } else if (product instanceof Equity) {
                    value = CollateralUtilities
                            .formatNumber((price / price) * 100); // #3-778
                }

                if (value.contains(",")) {
                    marginCallSecItem.setDirtyPrice("18:" + value.replace(',', '.') + fieldSeparator);
                } else {
                    marginCallSecItem.setDirtyPrice("18:" + value + fieldSeparator);
                }
                String dirtyPrice = value.replace(',', '.');

                value = CollateralUtilities.formatNumber(getHaircut(marginCall, inventorySecurityPosition, dsConn)); // #4-778
                if (value.contains(",")) {
                    marginCallSecItem.setHaircut("61:" + value.replace(',', '.') + fieldSeparator);
                } else {
                    marginCallSecItem.setHaircut("61:" + value + fieldSeparator);
                }
                String hairCut = value.replace(',', '.');

                Double dpWithHaircut = null;
                try {
                    dpWithHaircut = Double.valueOf(dirtyPrice) * Double.valueOf(hairCut) / 100;
                } catch (NumberFormatException e) {
                    Log.warn(this, "Cannot convert string to double: " + e.getMessage());
                }
                String dpWithHaircutStr = "18:" + CollateralUtilities.formatNumber(dpWithHaircut) + fieldSeparator;
                dpWithHaircutStr = dpWithHaircutStr.replace(',', '.');
                marginCallSecItem.setDirtyPriceWithHaircut(dpWithHaircutStr);

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

                marginCallSecItem
                        .setOutstandingDisputes("350:" + getOutstandingDisputeStatus(marginCall) + fieldSeparator); // agreement
                // OutstandingDisputes
                marginCallSecItem.setCollateralMarginType(
                        "351:" + getCollateralMarginType(marginCall) + fieldSeparator); // agreement

                marginCallSecItem.setCcp("352:" + getCollateralCCP(marginCall) + fieldSeparator);// CCP

                marginCallSecItem.setMarketValue("101:" + getNewMarketValue(marginCallSecItem) + fieldSeparator);


            } else {
                return null;
            }

            return marginCallSecItem;
        }

        return null;
    }

    /**
     * dirtyPrice = (campo 18)
     * collateralAmount = (campo 21)
     *
     * @param marginCallSecItem {@link KGR_Collateral_MarginCallItem}
     * @return (dirtyPrice / 100) * collateralAmount 21 - collateralAmount 21
     */
    private String getNewMarketValue(KGR_Collateral_MarginCallItem marginCallSecItem) {
        String resultValue = "0.0";
        Double dirtyPrice = getValue(marginCallSecItem.getDirtyPrice());
        Double collateralAmount = getValue(marginCallSecItem.getCollateralAmount());

        if (dirtyPrice != null && collateralAmount != null) {
            Double result = (dirtyPrice / 100) * collateralAmount - collateralAmount;
            DecimalFormat df = new DecimalFormat("#0.00");
            try {
                resultValue = df.format(result);
                if (resultValue.contains(",")) {
                    resultValue = resultValue.replace(',', '.');
                }
            } catch (Exception e) {
                Log.error(this, "Cannot format marketValue: " + result + " Error: " + e);
            }
        }

        return resultValue;

    }

    /**
     * @param String value
     * @return Double value
     */
    private Double getValue(String value) {
        if (value.contains(":")) {
            String[] values = value.split(":");
            try {
                return Double.valueOf(values[1]);
            } catch (Exception e) {
                Log.error(this, "Cannot cast " + values[1] + " to number.");
            }
        }
        return 0.0;
    }

    // Counterparties
    private KGR_Collateral_MarginCallItem getKGR_Collateral_CounterpartyItem(final CollateralConfig marginCall,
                                                                             final LegalEntity le, final DSConnection dsConn, final Vector<String> errors) {
        final KGR_Collateral_MarginCallItem marginCallCptyItem = new KGR_Collateral_MarginCallItem();

        marginCallCptyItem.setMarginCallContract(getMarginCallContract(marginCall));
        marginCallCptyItem.setCounterparty(le.getAuthName());
        marginCallCptyItem.setContractLongName(le.getName()); // cpty long name
        final String headClone = CollateralUtilities.converseHeadCloneKGRContracts(getHeadClone(marginCall));
        marginCallCptyItem.setHeadClone(headClone);

        marginCallCptyItem.setGlobalId(marginCall.getAdditionalField("IM_GLOBAL_ID"));
        marginCallCptyItem.setContractType(marginCall.getContractType());

        marginCallCptyItem.setIsSentinelBlocked(String.valueOf(false));

        return marginCallCptyItem;
    }

    // Processing Orgs (branch file)
    private KGR_Collateral_MarginCallItem getKGR_Collateral_ProcessingOrgItem(final CollateralConfig marginCall,
                                                                              final LegalEntity po, final DSConnection dsConn, final Vector<String> errors) {
        final KGR_Collateral_MarginCallItem marginCallPoItem = new KGR_Collateral_MarginCallItem();

        marginCallPoItem.setMarginCallContract(getMarginCallContract(marginCall));
        marginCallPoItem.setOwner(po.getAuthName());
        marginCallPoItem.setOwnerName(po.getName());
        final String headClone = CollateralUtilities.converseHeadCloneKGRContracts(getHeadClone(marginCall));
        marginCallPoItem.setHeadClone(headClone);

        marginCallPoItem.setGlobalId(marginCall.getAdditionalField("IM_GLOBAL_ID"));
        marginCallPoItem.setContractType(marginCall.getContractType());

        marginCallPoItem.setIsSentinelBlocked(String.valueOf(false));

        return marginCallPoItem;
    }

    // ** Methods ** //
    @SuppressWarnings("deprecation")
    public double getHaircut(final CollateralConfig marginCall, InventorySecurityPosition secPos, DSConnection dsConn) {

        double value = 0.0;

        // get HaircutProxy for contract
        CollateralFilterProxy filterProxy = new CachedCollateralFilterProxy();
        HaircutProxyFactory fact = new HaircutProxyFactory(filterProxy);
        HaircutProxy haircutProxy = fact.getProxy(marginCall.getPoHaircutName());

        Product p = secPos.getProduct();
        //String isin=p.getSecCode(SecCode.ISIN);
        if (p != null) {
            // JRL 20/04/2016 Migration 14.4
            value = 1 - Math.abs((haircutProxy.getHaircut(marginCall.getCurrency(), new CollateralCandidate(p), valueDate, true, marginCall, "Pay")));
        }
        return value * 100;
    }

    private String getIsin(final Product product) {
        return product.getSecCode(ISIN);
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

    private JDate getBondMaturityDate(final Product product) {
        return product.getMaturityDate();
    }

    private String getIssuer(final DSConnection dsConn, final Product product) {
        String strToReturn = null;
        LegalEntity le = new LegalEntity();

        // Comprobar que no es cero el issuerID
        int IssuerId = 0;
        if (product instanceof Bond) {
            IssuerId = ((Bond) product).getIssuerId();
        } else if (product instanceof Equity) {
            IssuerId = ((Equity) product).getIssuerId();
        }
        // Modificar
        try {
            le = dsConn.getRemoteReferenceData().getLegalEntity(IssuerId);
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

    private String getTransactionTypeSecurity(final InventorySecurityPosition inventorySecurityPosition, Product p) {
        // comprobar que los tipos son correctos
        if (p instanceof Bond) {
            return TRANSACTION_TYPE_SEC;
        } else if (p instanceof Equity) {
            return TRANSACTION_TYPE_SEC_EQUITY;
        }
        return null;
    }

    private String getTransactionIdSecurity(final InventorySecurityPosition inventorySecurityPosition,
                                            final Product product) {
        // v14 GSM 03/03/2016 Fix get getting MC id
        String transactionId = inventorySecurityPosition.getMarginCallConfigId() + getIsin(product) + product.getCurrency();

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

    // GSM 22/03/2016 MIG 14.4
    private String getTransactionId(final InventoryCashPosition inventoryCashPosition) {
        final String strToReturn = inventoryCashPosition.getMarginCallConfigId() + ""
                + inventoryCashPosition.getCurrency();
        return strToReturn;
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

    private String getOutstandingDisputeStatus(final CollateralConfig marginCall) {
        Boolean dispute = this.contractsDisputeStatus.get(marginCall.getId());
        String outStandingDisputeStatus = "N";
        if (dispute != null) {
            outStandingDisputeStatus = dispute.booleanValue() ? "Y" : "N";
        }
        return outStandingDisputeStatus;
    }

    /**
     * Method getCollateralMarginType, return the guarantee type
     *
     * @param marginCall
     * @return mapped value from domain value IMIRISMapping
     */
    private String getCollateralMarginType(final CollateralConfig marginCall) {
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
        //any other cases returns blank
        return BLANK;
    }


    private String getCollateralCCP(final CollateralConfig marginCall) {
        return Util.isEmpty(marginCall.getAdditionalField("CCP")) ? "" : marginCall.getAdditionalField("CCP");
    }

    /**
     * Method getSegregatedCollateral, selects the correct value of segregated collateral, if it is Cash or Security,
     * CSD or CSA
     *
     * @param marginCall , CollateralConfig
     * @param type       , Cash o Security
     * @return "Y" or "N"
     */
    private String getSegregatedCollateral(final CollateralConfig marginCall, final String type) {
        //gets the contract type for default values
        String collateralMarginType = marginCall.getContractType();
        //checks if it is Cash o Security
        if (type.equals(COLLATERAL_TYPE_CASH)) {
            //gets the cash specific segregated collateral field from the contract
            if (!Util.isEmpty(marginCall.getAdditionalField(SEGREGATED_COLLATERAL_CASH))) {
                return marginCall.getAdditionalField(SEGREGATED_COLLATERAL_CASH);
            } else {
                //returns default value
                return getDefaultSegregatedCollateral(collateralMarginType, COLLATERAL_TYPE_CASH);
            }
        } else if (type.equals(COLLATERAL_TYPE_SEC)) {
            //gets the security specific segregated collateral field from the contract
            if (!Util.isEmpty(marginCall.getAdditionalField(SEGREGATED_COLLATERAL_SEC))) {
                return marginCall.getAdditionalField(SEGREGATED_COLLATERAL_SEC);
            } else {
                //returns default value
                return getDefaultSegregatedCollateral(collateralMarginType, COLLATERAL_TYPE_SEC);
            }
        } else {
            //returns default value
            return getDefaultSegregatedCollateral(collateralMarginType, BLANK);
			/*if (!Util.isEmpty(marginCall.getAdditionalField(SEGREGATED_COLLATERAL))) {
				return marginCall.getAdditionalField(SEGREGATED_COLLATERAL);
			}else{
				return getDefaultSegregatedCollateral(collateralMarginType, BLANK);
			}*/
        }
    }

    /**
     * Method getDefaultSegregatedCollateral, returns the default value for segregated collateral
     *
     * @param mcType , MarginCall contract type
     * @param scType , Cash or Security
     * @return "Y" or "N"
     */
    private String getDefaultSegregatedCollateral(final String mcType, final String scType) {
        if (mcType == null) return BLANK;
        if (scType == null) return BLANK;

        //checks if the contract type is CSA or CSD
        if (mcType.equals(CSA)) {
            //checks if it is Cash or Security
            if (scType.equals(COLLATERAL_TYPE_CASH)) {
                return NO;
            } else if (scType.equals(COLLATERAL_TYPE_SEC)) {
                return NO;
            }
            //by default "N"
            return NO;
        } else if (mcType.equals(CSD)) {
            //checks if it is Cash or Security
            if (scType.equals(COLLATERAL_TYPE_CASH)) {
                return NO;
            } else if (scType.equals(COLLATERAL_TYPE_SEC)) {
                return YES;
            }
            //by default "N"
            return NO;
        }
        //returns blank in any other case
        return BLANK;
    }

    private String getFirstCalcDate(final CollateralConfig marginCall) {
        if (marginCall.getAdditionalField(MA_SIGN_DATE) != null) {
            return marginCall.getAdditionalField(MA_SIGN_DATE);
        }
        return BLANK;
    }

    private String getCalcPeriod(final CollateralConfig marginCall) {
        if (marginCall.getAdditionalField(FREQUENCY) != null) {
            return marginCall.getAdditionalField(FREQUENCY);
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

    /**
     * @param invSecPos
     * @return
     */
    public Product getBondFromSecurityPosition(InventorySecurityPosition invSecPos) {
        Product product = null;
        if (invSecPos != null) {
            product = invSecPos.getProduct();
        }
        return product;
    }

    // MODIFICAR?? EQUITY
    public boolean isBalanceZero(InventorySecurityPosition invSecPos) {
        if (invSecPos != null) {

            if (getBondFromSecurityPosition(invSecPos) instanceof Equity) {
                Equity equity = null;
                equity = (Equity) getBondFromSecurityPosition(invSecPos);
                if (equity != null) {
                    Double balance = getCollateralAmountSecurity(invSecPos);
                    return roundPositionValue(balance, 2) == 0.0;
                }
            } else {
                Bond b = null;
                b = (Bond) getBondFromSecurityPosition(invSecPos);
                if (b != null) {
                    Double balance = getCollateralAmountSecurity(invSecPos) * b.getFaceValue();
                    return roundPositionValue(balance, 2) == 0.0;
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

    public void setThresholdOwner(KGR_Collateral_MarginCallItem marginCallItem, CollateralConfig marginCall,
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

    public void setThresholdCpty(KGR_Collateral_MarginCallItem marginCallItem, CollateralConfig marginCall,
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

    public void setMtaOwner(KGR_Collateral_MarginCallItem marginCallItem, CollateralConfig marginCall,
                            double mtaOwner) {
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

    public void setMtaCpty(KGR_Collateral_MarginCallItem marginCallItem, CollateralConfig marginCall, double mtaCpty) {
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

    // if both LE have IA non depending on rating (ownerIA=cptyIA=contractIA) we
    // follow this logic: ia>=0 ->
    // cptyIA=0, ownerIA=valor // ia<0->
    // ownerIA=0, cptyIA=valor
    // if not, we set each IA getted in each side
    public void setIndependentAmount(KGR_Collateral_MarginCallItem marginCallItem, CollateralConfig marginCall,
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
