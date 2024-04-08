/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.collateral.SantMCConfigFilteringUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.CollateralServiceRegistry;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.report.CollateralConfigReport;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * SA SSR (new IRIS). Static information from Collateral Contracts.
 * This report, based on the enum REPORT_TYPES, will generate the 3 static info reports to provide this information.
 *
 * @author Guillermo Solano
 * @version 1.0
 * @Date 23/12/2016
 */
public class SACCRStaticReport extends CollateralConfigReport {

    /**
     * Serial UID
     */
    private static final long serialVersionUID = -3961574288031120846L;

    /*
     * Types of reports, mandatory
     */
    public enum REPORT_TYPES {
        COL_BRANCH("COL_Branch"),
        COL_CPTY("COL_Ctpy"),
        COL_INSTRUMENT("COL_Instrument");

        private String name;

        REPORT_TYPES(final String n) {
            this.name = n;
        }

        public String getReportName() {
            return name;
        }

        public static List<String> getReportNamesAsList() {
            return Arrays.asList(new String[]{COL_BRANCH.getReportName(), COL_CPTY.getReportName(), COL_INSTRUMENT.getReportName()});
        }

        public static REPORT_TYPES getReportType(final String name) {
            if (name.equals(COL_BRANCH.getReportName()))
                return COL_BRANCH;
            else if (name.equals(COL_CPTY.getReportName()))
                return COL_CPTY;
            else if (name.equals(COL_INSTRUMENT.getReportName()))
                return COL_INSTRUMENT;
            return null;
        }
    }

    /*
     * Report Type
     */
    private REPORT_TYPES reportType; //debe ser un enum

    // CONSTANTS
    /*
     * Number of cores available for multi-threading
     */
    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();
    //private static final String CSA_FACADE = "CSA_FACADE";
    private static final String CONTRACT_IA = "CONTRACT_IA";
    private static final String DISPUTE_ADJ = "DISPUTE_ADJUSTMENT";


    /**
     * Main method load of the report
     *
     * @param errors
     * @return rows for securities positions
     */
    @SuppressWarnings({"rawtypes", "unchecked",})
    @Override
    public ReportOutput load(Vector errors) {

        //Verified mandatory fields and initialize variables
        if (!mandatoryFields(errors))
            return null;

        final DefaultReportOutput reportOutput = new DefaultReportOutput(this);

        //avoid problems adding errors by independent threads
        final List<String> errorList = Collections.synchronizedList(errors);
        final List<ReportRow> rows = Collections.synchronizedList(new ArrayList<ReportRow>());
        final CollateralServiceRegistry srvReg = ServiceRegistry.getDefault();
        List<CollateralConfig> contracts = new ArrayList<CollateralConfig>();

        //get the filter based on the template values
        final MarginCallConfigFilter contractFilter = buildMCContractsFilter();

        try {
            //recover contract based on filter
            contracts = srvReg.getCollateralDataServer().getMarginCallConfigs(contractFilter, null);

        } catch (CollateralServiceException e) {

            Log.error(SACCRStaticReport.class, "FAIL: Collateral Service: " + e);
            errors.add("FAIL: Collateral Service: " + e.getLocalizedMessage());
            return null;
        }

        //threading
        ExecutorService taskExecutor = Executors.newFixedThreadPool(NUM_CORES);
        try {
            // Multithread execution, one contract per core
            for (final CollateralConfig marginCall : contracts) {

                taskExecutor.submit(new Runnable() {
                    @Override
                    public void run() {

                        //for each contract, multiple lines based on type of report
                        final Collection<ReportRow> threadColRows = buildReportRows(marginCall, errorList);
                        if (!threadColRows.isEmpty())
                            rows.addAll(threadColRows);
                    }
                });
            }
        } finally {
            taskExecutor.shutdown();
            //important to ensure all thread have finished
            try {
                taskExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            } catch (InterruptedException e) {
                Log.error(SACCRStaticReport.class, "FAIL: Thread interruption Service: " + e);
                errors.add("FAIL: Collateral Service: " + e.getLocalizedMessage());
            }
        }

        if (!errorList.isEmpty())
            errors.addAll(errorList);

        reportOutput.setRows(rows.toArray(new ReportRow[0]));
        return reportOutput;

    }


    /**
     * @return filter obtained from template configurations
     */
    private MarginCallConfigFilter buildMCContractsFilter() {

        final String ownerIdsStr = CollateralUtilities.filterPoIdsByTemplate(getReportTemplate());
        List<Integer> ownerIds = new ArrayList<>();
        if (!Util.isEmpty(ownerIdsStr)) {
            ownerIds = Util.string2IntVector(ownerIdsStr);
        }
        final String cpty = (String) getReportTemplate().get(SantGenericTradeReportTemplate.COUNTERPARTY);
        List<Integer> idsLe = new ArrayList<>();
        if (!Util.isEmpty(cpty)) {
            idsLe = getIdsLe(cpty);
        }

        final String typeAgrString = (String) getReportTemplate().get(SantGenericTradeReportTemplate.AGREEMENT_TYPE);
        List<String> typesAgr = new ArrayList<>();
        if (!Util.isEmpty(typeAgrString)) {
            typesAgr = Util.string2Vector(typeAgrString);
        }

        final String agreementIds = (String) getReportTemplate().get(SantGenericTradeReportTemplate.AGREEMENT_ID);
        List<Integer> contractIds = new ArrayList<>();
        if (!Util.isEmpty(agreementIds)) {
            contractIds = Util.string2IntVector(agreementIds);
        }

        return SantMCConfigFilteringUtil.getInstance().getInstance().buildMCConfigFilter(null, contractIds, ownerIds, typesAgr, idsLe);
    }

    /**
     * @param leNames
     * @return list of ids of the LE
     */
    private List<Integer> getIdsLe(final String leNames) {

        Vector<String> namesLE = Util.string2Vector(leNames);
        ArrayList<Integer> leIds = new ArrayList<Integer>();
        if (!Util.isEmpty(namesLE)) {

            for (String name : namesLE) {
                final LegalEntity le = BOCache.getLegalEntity(getDSConnection(), name.trim());
                if (le != null)
                    leIds.add(le.getId());
            }
            return leIds;

        }
        return leIds;
    }

    /**
     * @param errors
     * @return true if all mandatory fields are satisfied
     */
    private boolean mandatoryFields(final Vector<String> errors) {

        if (getReportTemplate() == null) {
            errors.add("Template not assign.");
            return false;
        }
        // getReportTemplate().setColumns( SACCRStaticReportTemplate.DEFAULT_COLUMNS_COL_BRANCH);

        String value = getReportTemplate().get(SACCRStaticReportTemplate.REPORT_TYPE);
        if (Util.isEmpty(value)) {
            errors.add("Report Type cannot be empty.");
        } else
            this.reportType = REPORT_TYPES.getReportType(value.trim());

        //poner aqui hashSet de owners y agreement type

        return (Util.isEmpty(errors));
    }

    /**
     * @param contract
     * @param errorList
     * @return builds the set of rows per contract based on the type of report
     */
    private Collection<ReportRow> buildReportRows(final CollateralConfig contract, final List<String> errorList) {

        switch (reportType) {

            case COL_BRANCH:
                return buildBranchesRows(contract, errorList);

            case COL_CPTY:
                return buildCptysRows(contract, errorList);

            case COL_INSTRUMENT:
                return buildInstrumentsRows(contract, errorList);

            default: //should not happen
                return null;
        }
    }

    /**
     * @param contract
     * @param errorList
     * @return one row per each PO (principal and additional) in the contract
     */
    private Collection<ReportRow> buildBranchesRows(CollateralConfig contract, List<String> errorList) {

        final List<ReportRow> rows = new ArrayList<ReportRow>();
        HashSet<LegalEntity> pos = new HashSet<LegalEntity>();
        pos.add(contract.getProcessingOrg());
        pos.addAll(deleteDuplicates(contract.getAdditionalPO()));

        for (LegalEntity le : pos) {

            if (isPOAccepted(le, contract)) {
                ReportRow row = new ReportRow(contract, SACCRStaticReportTemplate.COLLATERAL_CONFIG);
                row.setProperty(SACCRStaticReportTemplate.ENTITY, le);
                rows.add(row);
            }
        }

        return rows;
    }

    private boolean isPOAccepted(LegalEntity po, CollateralConfig contract) {
        Optional<StaticDataFilter> sdf;
        try {
            sdf = Optional.ofNullable(DSConnection.getDefault().getRemoteReferenceData()
                    .getStaticDataFilter("UND_" + contract.getName()));
            if (sdf.isPresent()) {
                for (StaticDataFilterElement element : sdf.get().getElements()) {
                    if (element.getName().equals("ProcessingOrg") && element.getOperatorType().getDisplayName().equals("NOT_IN") && element.getValues().contains(po.getCode())) {
                        return false;
                    }
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this.getClass().getSimpleName(), "Cant retrieve the SDF: " + "UND_" + contract.getName(), e);
        }
        return true;
    }

    /**
     * @param contract
     * @param errorList
     * @return one row per each Le entity (principal and additional) in the contract
     */
    private Collection<ReportRow> buildCptysRows(CollateralConfig contract, List<String> errorList) {

        final List<ReportRow> rows = new ArrayList<ReportRow>();
        HashSet<LegalEntity> cptys = new HashSet<LegalEntity>();
        cptys.add(contract.getLegalEntity());
        cptys.addAll(deleteDuplicates(contract.getAdditionalLE()));

        for (LegalEntity le : cptys) {

            ReportRow row = new ReportRow(contract, SACCRStaticReportTemplate.COLLATERAL_CONFIG);
            row.setProperty(SACCRStaticReportTemplate.ENTITY, le);
            rows.add(row);

        }
        return rows;
    }

    /**
     * @param contract
     * @param errorList
     * @return one row per product in the exposure types
     */
    private Collection<ReportRow> buildInstrumentsRows(CollateralConfig contract, List<String> errorList) {

        final List<ReportRow> rows = new ArrayList<ReportRow>();


        if (Util.isEmpty(contract.getExposureTypeList())) {


            for (String productType : contract.getProductList()) {

                if (Util.isEmpty(productType))
                    continue;

                ReportRow row = new ReportRow(contract, SACCRStaticReportTemplate.COLLATERAL_CONFIG);
                row.setProperty(SACCRStaticReportTemplate.PRODUCT, productType);
                rows.add(row);
            }
        } else {
            final HashSet<String> exposureTypesSet = new HashSet<String>(contract.getExposureTypeList());

            //remove unwanted types
            exposureTypesSet.remove(CONTRACT_IA);
            exposureTypesSet.remove(DISPUTE_ADJ);

            for (String productType : exposureTypesSet) {

                if (Util.isEmpty(productType))
                    continue;

                ReportRow row = new ReportRow(contract, SACCRStaticReportTemplate.COLLATERAL_CONFIG);
                row.setProperty(SACCRStaticReportTemplate.PRODUCT, productType);
                rows.add(row);
            }
        }

        return rows;
    }

    /**
     * @param list
     * @return list of entities without duplications
     */
    private List<LegalEntity> deleteDuplicates(final List<LegalEntity> list) {

        Set<LegalEntity> hs = new LinkedHashSet<>();
        hs.addAll(list);
        List<LegalEntity> filteredList = new ArrayList<>();
        filteredList.addAll(hs);
        return filteredList;
    }

    //A verificar
//	private boolean cleanContract(CollateralConfig contract) {
//			
//		// check status
//		if (CollateralConfig.CLOSED.equals(contract.getAgreementStatus())) {
//			return true;
//		}
//		// Exclude CSA Facade - if contract types not used
//		if (getReportTemplate().get(SantGenericTradeReportTemplate.AGREEMENT_TYPE) == null){
//			if (CSA_FACADE.equals(contract.getContractType())){
//				return true;
//			}
//		}
//		return false;
//	}


}
