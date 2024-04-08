package calypsox.tk.report;

import calypsox.tk.collateral.util.SantMarginCallUtil;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.util.CheckRowsNumberReport;
import calypsox.util.SantReportingUtil;
import calypsox.util.collateral.CollateralManagerUtil;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.bo.InventoryCashPosition;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.Holiday;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.BOCashPositionReport;
import com.calypso.tk.report.BOSecurityPositionReportTemplate;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import org.apache.commons.lang3.ArrayUtils;
import org.jfree.util.Log;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SACCRInventoryPositionReport extends SantCollateralBOPositionReport implements CheckRowsNumberReport {


    /*
     * Process Date, introduced by date in Panel
     */
    private JDate processDate;

    /*
     * Process date
     */
    private JDate valueDate;

    /*
     *  Process date + Matury offset to business days, determined by Maturity offset in panel.
     *  If empty, default is 7
     */
    private JDate collateralMaturityDate;

    private HashMap<Integer,CollateralConfig> mexContracts = new HashMap<>();

    /**
     * Report main load
     */
    @Override
    public ReportOutput loadReport(final Vector errors) {
        //Verified mandatory fields and initialize variables
        if (!mandatoryFields(errors))
            return null;
        DefaultReportOutput reportOutput = (DefaultReportOutput) super.loadReport(errors);

        //Generate a task is the report size is out of a defined umbral
        HashMap<String, String> value = SantReportingUtil.getSchedTaskNameOrReportTemplate(this);
        if (!value.isEmpty() && value.keySet().iterator().next().equals("ScheduledTask: ")){
            checkAndGenerateTaskReport(reportOutput, value);
        }

        PricingEnv pricingEnv = getPricingEnv();
        if(null!=pricingEnv && SecLendingPositionUtil.PE_MEXICO.equalsIgnoreCase(pricingEnv.getName())){
            mexContracts.clear();
            //Generate SecVsSec Position for México report
            ReportRow[] rows = reportOutput.getRows();
            ConcurrentLinkedQueue<ReportRow> mexicoSecVsSecRows = new ConcurrentLinkedQueue<>();
            for (ReportRow row : rows) {
                enrichRowMexico(row,mexicoSecVsSecRows); //Aqui se crean las nuevas lineas de secVsSec
            }
            //Concat all Rows
            ReportRow[] finalRows = ArrayUtils.addAll(rows, mexicoSecVsSecRows.toArray(new ReportRow[mexicoSecVsSecRows.size()]));
            int ocunt = 1; //Update Row number
            for (ReportRow row : finalRows) {
                row.setProperty("ROW_NUMBER",ocunt);
                ocunt++;
            }

            reportOutput.setRows(finalRows);
        }else {
            for (ReportRow row : reportOutput.getRows()) {
                enrichRow(row);
            }
        }
        return reportOutput;
    }

    private void enrichRow(ReportRow row) {
        Inventory pos = row.getProperty("Inventory");
        CollateralConfig mcc = loadMarginCallConfig(pos);
        row.setProperty(SACCRCMPositionReportTemplate.COLLATERAL_CONFIG, mcc);
        row.setProperty("MarginCallEntryDTO", loadMarginCallEntryDTO(mcc));
        row.setProperty("Product", pos.getProduct());
        row.setProperty(SACCRCMPositionReportTemplate.MARGIN_CALL_ENTRY, loadMarginCallEntry(mcc, row.getProperty("MarginCallEntryDTO")));
        row.setProperty(SACCRCMPositionReportTemplate.COLLATERAL_MOVEMENT_TYPE, getCollateralMovementType(pos));
        row.setProperty(SACCRCMPositionReportTemplate.MARGIN_TYPE, getCollateralMarginType(mcc));
        row.setProperty("FX RATE", loadFxRate(pos, mcc));
        row.setProperty("IM_AMOUNT", getPreviousRQV(mcc, row.getProperty("MarginCallEntryDTO")));
        row.setProperty(ReportRow.ACCOUNT, loadAccount(String.valueOf(mcc.getId()), pos.getSettleCurrency()));
        buildCollateralDates(row);

    }

    private void enrichRowMexico(ReportRow row, ConcurrentLinkedQueue<ReportRow> mexicoSecVsSecRows) {
        Inventory pos = row.getProperty("Inventory");
        CollateralConfig mcc = loadMarginCallConfig(pos);
        row.setProperty(SACCRCMPositionReportTemplate.COLLATERAL_CONFIG, mcc);
        row.setProperty("MarginCallEntryDTO", loadMarginCallEntryDTO(mcc));
        row.setProperty("Product", pos.getProduct());
        row.setProperty(SACCRCMPositionReportTemplate.MARGIN_CALL_ENTRY, loadMarginCallEntry(mcc, row.getProperty("MarginCallEntryDTO")));
        row.setProperty(SACCRCMPositionReportTemplate.COLLATERAL_MOVEMENT_TYPE, getCollateralMovementType(pos));
        row.setProperty(SACCRCMPositionReportTemplate.MARGIN_TYPE, getCollateralMarginType(mcc));
        row.setProperty("FX RATE", loadFxRate(pos, mcc));
        row.setProperty("IM_AMOUNT", getPreviousRQV(mcc, row.getProperty("MarginCallEntryDTO")));
        buildCollateralDates(row);

        //Create secvssec line just one time by contract
        if(null!=mcc && !mexContracts.containsKey(mcc.getId())){
            mexContracts.put(mcc.getId(),mcc);
            List<InventorySecurityPosition> secVsSecInventorySecurityPosition = SecLendingPositionUtil.getSecVsSecInventorySecurityPosition((MarginCallEntry)row.getProperty(SACCRCMPositionReportTemplate.MARGIN_CALL_ENTRY),getValDate()); //Activar este para no tener que buscar las detailEntry desde el contrato
            for(InventorySecurityPosition inventoryPositionSecVsSec :  secVsSecInventorySecurityPosition){
                try {
                    //TODO Clone solo si es inventory de Security NO SI ES DE CASH - Corregir!! (en caso de ser de cash y encontrar un SecVsSec se tiene que crear la linea de 0.)
                    ReportRow rowCloned = row.clone();
                    inventoryPositionSecVsSec.setInternalExternal("MARGIN_CALL");
                    inventoryPositionSecVsSec.setPositionType("ACTUAL");
                    inventoryPositionSecVsSec.setDateType("SETTLE");
                    rowCloned.setProperty("Inventory",inventoryPositionSecVsSec);
                    rowCloned.setProperty("Default",inventoryPositionSecVsSec);
                    rowCloned.setProperty("POSITIONS",new HashMap<String,Inventory>().put("POSITIONS",inventoryPositionSecVsSec));
                    rowCloned.setProperty("Product", inventoryPositionSecVsSec.getProduct());
                    rowCloned.setProperty(SACCRCMPositionReportTemplate.COLLATERAL_MOVEMENT_TYPE, getCollateralMovementType(inventoryPositionSecVsSec));
                    rowCloned.setProperty("FX RATE", loadFxRate(inventoryPositionSecVsSec, mcc));
                    mexicoSecVsSecRows.add(rowCloned);
                }catch (Exception e){
                    Log.error(this.getClass().getSimpleName(), e);
                }
            }
        }
    }

    private CollateralConfig loadMarginCallConfig(Inventory pos) {
        CollateralConfig mcc = null;
        if (null == pos) {
            return null;
        }
        try {
            if (pos.getMarginCallConfigId() > 0) {
                mcc = ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfig(pos.getMarginCallConfigId());
            }
        } catch (CollateralServiceException e) {
            Log.error(this.getClass().getSimpleName(), e);
        }
        return mcc;
    }

    private MarginCallEntryDTO loadMarginCallEntryDTO(CollateralConfig mcc) {
        List<MarginCallEntryDTO> entries = new ArrayList<>();
        if (null != mcc) {
            try {
                entries = CollateralManagerUtil.loadMarginCallEntriesDTO(Arrays.asList(mcc.getId()), this.getValDate());
                if (entries.size() == 0) {
                    entries = CollateralManagerUtil.loadMarginCallEntriesDTO(Arrays.asList(mcc.getId()), this.getValDate().addBusinessDays(-1, Holiday.getCurrent().getHolidayCodeCollection()));
                }
            } catch (CollateralServiceException e) {
                Log.error(this.getClass().getSimpleName(), e);
            }
        }
        return entries.size() > 0 ? entries.stream().max(Comparator.comparing(s -> s.getId())).get() : null;
    }

    private MarginCallEntry loadMarginCallEntry(CollateralConfig mcc, MarginCallEntryDTO entryDTO) {
        MarginCallEntry entry = null;
        if (null != entryDTO) {
            entry = SantMarginCallUtil.getMarginCallEntry(entryDTO, mcc, true);
        }
        return entry;
    }
    private Amount getPreviousRQV(CollateralConfig mcc, MarginCallEntryDTO entryDTO){
   	 MarginCallEntry entry = null;
   	 double previosrqv = 0.0D;
       if (null != entryDTO) {
           entry = SantMarginCallUtil.getMarginCallEntry(entryDTO, mcc, true);
           previosrqv = entry.getPreviousRQV();
       }

       return new Amount (previosrqv);
   }

    /**
     * @param pos current position
     * @return Cash, Bond or Equity depending of the product
     */
    private String getCollateralMovementType(Inventory pos) {
        if (pos instanceof InventoryCashPosition) {
            return "Cash";
        } else if (pos.getProduct() instanceof Bond) {
            return "Bond";
        } else if (pos.getProduct() instanceof Equity) {
            return "Equity";
        }
        return "";
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
            Map<String, String> map = CollateralUtilities.initDomainValueComments("IMIRISMapping");
            String field = marginCall.getAdditionalField("GUARANTEE_TYPE");
            if (!Util.isEmpty(field)) {
                String comment = map.get(field);
                if (!Util.isEmpty(comment)) return comment;
            }
        }
        return "";
    }

    protected BOPositionReport buildSecurityTemplate(BOSecurityPositionReportTemplate secPositionTemplate) {
        String positionType = this.getReportTemplate().get(BOSecurityPositionReportTemplate.POSITION_TYPE);
        String sdf = this.getReportTemplate().get(BOSecurityPositionReportTemplate.SEC_FILTER);

        secPositionTemplate.put(SecurityTemplateHelper.SECURITY_REPORT_TYPE, "");
        secPositionTemplate.put(com.calypso.tk.report.BOSecurityPositionReportTemplate.POSITION_DATE, "Settle");
        secPositionTemplate.put(com.calypso.tk.report.BOSecurityPositionReportTemplate.POSITION_CLASS, "Margin_Call");
        secPositionTemplate.put(com.calypso.tk.report.BOSecurityPositionReportTemplate.POSITION_TYPE, !Util.isEmpty(positionType) ? positionType : "Actual");
        secPositionTemplate.put(com.calypso.tk.report.BOSecurityPositionReportTemplate.POSITION_VALUE, "Nominal");
        secPositionTemplate.put(com.calypso.tk.report.BOSecurityPositionReportTemplate.CASH_SECURITY, "Security");
        secPositionTemplate.put(com.calypso.tk.report.BOSecurityPositionReportTemplate.AGGREGATION, "ProcessingOrg");
        secPositionTemplate.put(BOSecurityPositionReportTemplate.SEC_FILTER, !Util.isEmpty(sdf) ? sdf : "");
        secPositionTemplate.put(BOPositionReportTemplate.FILTER_ZERO, "true");
        secPositionTemplate.put(BOPositionReportTemplate.MOVE, "Balance");
        secPositionTemplate.put("StartDate", getStartDate(getReportTemplate(), this.processDate).toString());
        secPositionTemplate.put("EndDate", getEndDate(getReportTemplate(), this.processDate).toString());
        final String agreementId = (String) getReportTemplate().get(SantGenericTradeReportTemplate.AGREEMENT_ID);
        if (!Util.isEmpty(agreementId)) {
            secPositionTemplate.put(BOPositionReportTemplate.CONFIG_ID, agreementId);
        }
        // GSM 30/07/15. SBNA Multi-PO filter
        final String ownersNames = CollateralUtilities.filterPoNamesByTemplate(getReportTemplate());
        // final String ownersNames = (String)
        // getReportTemplate().get(SantGenericTradeReportTemplate.PROCESSING_ORG_NAMES);
        if (!Util.isEmpty(ownersNames)) {
            secPositionTemplate.put(BOSecurityPositionReportTemplate.PROCESSING_ORG, ownersNames);
        }

        BOPositionReport secPositionReport = new BOSecurityPositionReport();
        secPositionReport.setReportTemplate(secPositionTemplate);
        secPositionReport.setPricingEnv(getPricingEnv());
        secPositionReport.setStartDate(getStartDate(getReportTemplate(), this.processDate));
        secPositionReport.setEndDate(getEndDate(getReportTemplate(), this.processDate));

        return secPositionReport;
    }

    protected BOPositionReport buildCashTemplate(BOCashPositionReportTemplate cashPositionTemplate) {
        String positionType = this.getReportTemplate().get(BOSecurityPositionReportTemplate.POSITION_TYPE);
        String sdf = this.getReportTemplate().get(BOSecurityPositionReportTemplate.SEC_FILTER);

        cashPositionTemplate.put(com.calypso.tk.report.BOSecurityPositionReportTemplate.POSITION_DATE, "Settle");
        cashPositionTemplate.put(com.calypso.tk.report.BOSecurityPositionReportTemplate.POSITION_CLASS, "Margin_Call");
        cashPositionTemplate.put(com.calypso.tk.report.BOSecurityPositionReportTemplate.POSITION_TYPE, !Util.isEmpty(positionType) ? positionType : "Actual");
        cashPositionTemplate.put(com.calypso.tk.report.BOSecurityPositionReportTemplate.POSITION_VALUE, "Nominal");
        cashPositionTemplate.put(com.calypso.tk.report.BOSecurityPositionReportTemplate.CASH_SECURITY, "Cash");
        cashPositionTemplate.put(com.calypso.tk.report.BOSecurityPositionReportTemplate.AGGREGATION, "ProcessingOrg");
        cashPositionTemplate.put(BOSecurityPositionReportTemplate.SEC_FILTER, !Util.isEmpty(sdf) ? sdf : "");
        cashPositionTemplate.put(BOPositionReportTemplate.FILTER_ZERO, "true");
        cashPositionTemplate.put(BOPositionReportTemplate.MOVE, "Balance");
        cashPositionTemplate.put("StartDate", getStartDate(getReportTemplate(), this.processDate).toString());
        cashPositionTemplate.put("EndDate", getEndDate(getReportTemplate(), this.processDate).toString());
        final String agreementId = (String) getReportTemplate().get(SantGenericTradeReportTemplate.AGREEMENT_ID);
        if (!Util.isEmpty(agreementId)) {
            cashPositionTemplate.put(BOPositionReportTemplate.CONFIG_ID, agreementId);
        }

        // GSM 20/07/15. SBNA Multi-PO filter
        final String ownersNames = CollateralUtilities.filterPoNamesByTemplate(getReportTemplate());
        // final String ownersNames = (String) getReportTemplate().get(
        // SantGenericTradeReportTemplate.PROCESSING_ORG_NAMES);
        if (!Util.isEmpty(ownersNames)) {
            cashPositionTemplate.put(BOCashPositionReportTemplate.PROCESSING_ORG, ownersNames);
        }

        BOPositionReport cashPositionReport = new BOCashPositionReport();
        cashPositionReport.setPricingEnv(getPricingEnv());
        cashPositionReport.setStartDate(getStartDate(getReportTemplate(), this.processDate));
        cashPositionReport.setEndDate(getEndDate(getReportTemplate(), this.processDate));
        cashPositionReport.setReportTemplate(cashPositionTemplate);

        return cashPositionReport;
    }

    private Amount loadFxRate(Inventory posicion, CollateralConfig collateralConfig) {
        if (null != posicion && null != collateralConfig) {
            double value = CollateralUtilities.getFXRatebyQuoteSet(getValDate(), posicion.getSettleCurrency(), collateralConfig.getCurrency(), null);
            return new Amount(value, 2);
        }
        return null;
    }

    /**
     * Stores process, value & collateral dates. Iquals for all rows.
     *
     * @param currentRow
     */
    private void buildCollateralDates(ReportRow currentRow) {

        currentRow.setProperty(SACCRCMPositionReportTemplate.COLLATERAL_PROCESS_DATE, this.processDate);
        currentRow.setProperty(SACCRCMPositionReportTemplate.COLLATERAL_VALUE_DATE, this.valueDate);
        currentRow.setProperty(SACCRCMPositionReportTemplate.COLLATERAL_MATURITY_DATE, this.collateralMaturityDate);
    }

    /**
     * @param errors
     * @return true if all mandatory filters in panel are included
     */
    private boolean mandatoryFields(final Vector<String> errors) {

        errors.clear();

        if (getReportTemplate() == null) {
            errors.add("Template not assign.");
            return false;
        }
        computeProcessDate(errors);

        //Cash, Security, Both
        if (Util.isEmpty((String) getReportTemplate().get(BOSecurityPositionReportTemplate.CASH_SECURITY)))
            errors.add("Cash/Sec cannot be empty.");

        //recover dates, common for all rows
        Integer collateralMaturityOffset = 7;// by default if empty selection in panel
        final String offset = getReportTemplate().get(SACCRCMPositionReportTemplate.MATURITY_OFFSET);
        if (!Util.isEmpty(offset)) {
            collateralMaturityOffset = Integer.parseInt(offset);
        }

        this.valueDate = this.getValDate();
        this.collateralMaturityDate = this.processDate.addBusinessDays(collateralMaturityOffset, getReportTemplate().getHolidays());

        return (Util.isEmpty(errors));
    }

    /**
     * @param errors
     * @return true if process date has been read from the template
     */
    private boolean computeProcessDate(final Vector<String> errors) {

        this.processDate = null;
        this.processDate = getDate(this._reportTemplate, getValuationDatetime().getJDate(TimeZone.getDefault()),
                TradeReportTemplate.START_DATE, TradeReportTemplate.START_PLUS, TradeReportTemplate.START_TENOR);

        if (this.processDate == null) {
            errors.add("Process Start Date cannot be empty.");
            return false;
        }
        return true;
    }
    public Account loadAccount(String id, String currency){
        Account account = null;
        final List<Account> accounts = BOCache.getAccountByAttribute(DSConnection.getDefault(), "MARGIN_CALL_CONTRACT", id);
        account = accounts.stream().filter(acc -> acc.getCurrency().equalsIgnoreCase(currency)).findFirst().orElse(null);
        return account;
    }
    @Override
    protected boolean checkProcessEndDate() {
        return false;
    }

    @Override
    protected boolean checkProcessStartDate() {
        return true;
    }
}
