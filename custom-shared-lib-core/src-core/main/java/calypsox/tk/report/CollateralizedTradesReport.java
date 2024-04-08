/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import calypsox.ErrorCodeEnum;
import calypsox.tk.report.CollateralizedTradesReportLogic.ResponseWrapper;
import calypsox.tk.util.ControlMErrorLogger;
import calypsox.util.TradeCollateralizationService.TradeCollateralizationConstants.DFA_OUTPUT_FIELDS;
import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.core.Attributes;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;

import java.util.*;

/**
 * Generates a report with all the today collateralized trades using the output format specified in the Dodd Frank
 * Service DDR. This report is expected to be run as a schedule task report.
 *
 * @author Guillermo Solano
 * @version 1.1
 */
public class CollateralizedTradesReport extends Report {

    /* SERIAL UID */
    private static final long serialVersionUID = -8024446738877030917L;

    /* Constanst */
    public static final String COLLATERALIZED_TRADES_REPORT = "CollateralizedTradesReport";
    public static final String ROW_PROPERTY_ID = "CollateralizedTrade";
    public static final String NO_VALUEDATE_ERROR_MESSAGE = "No valuation Date read from the template. Please check the configuration";
    private final static String CLASS_NAME = CollateralizedTradesReport.class.getCanonicalName();
    private final static String ERROR_RESULT = "Not document generated";

    // class variables
    private JDate valuationDate;

    /**
     * Override method load to generate the file (the report).
     *
     * @param errorsMsgs passed by parameter
     * @return the ReportOutput to generate the report
     */
    @SuppressWarnings("unchecked")
    @Override
    public ReportOutput load(@SuppressWarnings("rawtypes") Vector vector) {

        final DefaultReportOutput output = new StandardReportOutput(this);
        final List<ReportRow> reportRowsList = new ArrayList<ReportRow>();
        final Set<String> checkOperationsDuplicity = new HashSet<String>();

        Log.debug(CLASS_NAME, "1. DFA report started.");

        /* control check and retrieve the data from the template of the schedule task */
        if (!readAndCheckTemplateAttributes(vector)) {

            Log.error(CLASS_NAME, "Template Attributes has NOT been received"); // log messages
            ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, ERROR_RESULT);// CONTROL-M
            return output;
        }

        final List<MarginCallDetailEntryDTO> mCDEntriesList = CollateralizedTradesReportLogic.retriveMCAliveTradesList(
                vector, this.valuationDate); // retrieve all MCDetailEntries alive -> Trades

        Log.debug(CLASS_NAME,
                "Margin call detail entries for derivates obtained for Date " + this.valuationDate.toString());

        for (MarginCallDetailEntryDTO mcde : mCDEntriesList) {

            final List<ResponseWrapper> responses = new Vector<ResponseWrapper>();

            // GSM 15/07/15. SBNA Multi-PO filter, returns null if must be filtered
            final Map<DFA_OUTPUT_FIELDS, String> collateralTrade = CollateralizedTradesReportLogic
                    .buildOutputMapFromMCDetailEntryDTO(mcde, responses, this.valuationDate, super.getReportTemplate());

            if (collateralTrade == null) {
                continue;
            }

            // GSM: 27/05/13. Ensure no duplications can occur
            /*
             * In case a contract is manually modified it can happen that a trade matches, during the generation of this
             * report, more than one contract. As specified by GBO-ITR, the prefer to just announce the first operation
             * if this happens.
             */
            final String uniqueTradeID = buildUniqueBoID(collateralTrade);

            if (!checkOperationsDuplicity.contains(uniqueTradeID)) {

                final ReportRow repRow = new ReportRow(mcde);
                repRow.setProperty(ROW_PROPERTY_ID, collateralTrade);
                reportRowsList.add(repRow);
                // we mark the operation as added
                checkOperationsDuplicity.add(uniqueTradeID);

            }

        }

        output.setRows(reportRowsList.toArray(new ReportRow[0]));

        Log.debug(CLASS_NAME, "2. DFA report generated.");

        return output;
    }

    /*
     * Builds the unique ID of an operation as the tandem BO_REFERENCE + BO_SYSTEM.
     */
    private String buildUniqueBoID(Map<DFA_OUTPUT_FIELDS, String> collateralTrade) {

        if ((collateralTrade == null) || collateralTrade.isEmpty()) {
            return "";
        }

        String temp = collateralTrade.get(DFA_OUTPUT_FIELDS.BO_SOURCE_SYSTEM);
        final String bo_system = (temp == null) || temp.isEmpty() ? "" : temp;
        temp = collateralTrade.get(DFA_OUTPUT_FIELDS.BO_EXTERNAL_REFERENCE);
        final String bo_reference = (temp == null) || temp.isEmpty() ? "" : temp;

        return (bo_reference + bo_system);
    }

    /**
     * Reads all the attributes passed through the schedule task template, acts as a constructor of all the require
     * class variables and finally it checks that all the necessary data for constructing the data is available
     *
     * @param vector
     * @return true is all the data is available, false othercase
     */
    // All attributes to be checked HERE!!
    private boolean readAndCheckTemplateAttributes(Vector<String> vector) {

        @SuppressWarnings("unused") final Attributes attributes = getReportTemplate().getAttributes();

        // We get the valuation date to put in the export properly.
        this.valuationDate = getReportTemplate().getValDate();

        if (this.valuationDate == null) {
            vector.add(NO_VALUEDATE_ERROR_MESSAGE);
            Log.error(CLASS_NAME, NO_VALUEDATE_ERROR_MESSAGE);
            return false;
        }

        // check conditions necessary to continue with the processing
        return true;

    }

}
