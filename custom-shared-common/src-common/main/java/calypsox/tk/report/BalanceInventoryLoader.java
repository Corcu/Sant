package calypsox.tk.report;

import calypsox.apps.reporting.BalanzaDePagosReportTemplatePanel;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;

import java.util.*;

public class BalanceInventoryLoader {
    private static String LOG_CAT = "BalanceInventoryLoader";
    private JDate startDate;
    private JDate endDate;
    private JDatetime startDatetime;
    private JDatetime endDatetime;
    private JDatetime valuationDatetime;
    private String inventoryType;
    private PricingEnv pEnv;
    private Vector<String> errors;
    private Vector<String> holidays;
    private Vector<String> movementTypes;
    private Vector<String> balanceTypes;
    private ReportTemplate templateAsParameter;

    public BalanceInventoryLoader(String InventoryType, ReportTemplate template, JDate startDate, JDate endDate, JDatetime valuationDatetime, PricingEnv pEnv, Vector<String> holidays)  {
        this.inventoryType = InventoryType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.valuationDatetime = valuationDatetime;
        this.startDatetime = new JDatetime(startDate.asCalendar().getTime());
        this.endDatetime = new JDatetime(endDate.asCalendar().getTime());
        this.pEnv = pEnv;
        this.holidays = holidays;
        this.movementTypes = new Vector();
        this.balanceTypes = new Vector();
        this.errors = new Vector();
        this.templateAsParameter = template;
    }

    /**
     * Setup variables into Inventory Param template.
     * @param template
     */
    protected void configureTemplate(ReportTemplate template) {
        template.put("StartDate", Util.dateToString(startDate));
        template.put("EndDate", Util.dateToString(endDate));

        template.put("SEC_CODE", "ISIN");
        String s = template.get("SEC_CODE_VALUE");
        if (Util.isEmpty(s)) {
            template.put("SEC_CODE_VALUE", "ES%");
        }

        // first param overrides the one from Inventory is there any
        String isinParam =  templateAsParameter.get("ISIN");
        if (!Util.isEmpty(isinParam)) {
            template.put("SEC_CODE_VALUE", isinParam.trim());
        } else {
            String isin = template.get("SEC_CODE_VALUE");
            if (Util.isEmpty(isin)) {
                template.put("SEC_CODE_VALUE", "ES%");
            }
        }
        template.callBeforeLoad();
    }


    public ArrayList<BalanzaDePagosItem> loadData() throws CalypsoServiceException {
        ArrayList<BalanzaDePagosItem> balanzaDePagosItems = buildItemsListFromInventory();
        return balanzaDePagosItems;
    }

    private ArrayList<BalanzaDePagosItem> buildItemsListFromInventory() throws CalypsoServiceException {
        DefaultReportOutput dro = executeInventoryReport();
        if (dro == null) {
            return null;
        }
        ArrayList<BalanzaDePagosItem> positionItems = collectPositionsFromInventory(dro);
        return  positionItems;
    }

    protected ArrayList<BalanzaDePagosItem> collectPositionsFromInventory(DefaultReportOutput dro) {
        ArrayList<BalanzaDePagosItem> result = new ArrayList<>();
        ReportRow[] rows = dro.getRows();
        BOSecurityPositionReportStyle reportStyle = new BOSecurityPositionReportStyle();

        for (int i = 0; i < rows.length; i++) {
            ReportRow row = rows[i];

            InventorySecurityPosition invPosition = row.getProperty(ReportRow.INVENTORY);
            if (invPosition == null) {
                continue;
            }

            LegalEntity le = invPosition.getAgent();
            if (le == null) {
                continue;
            }

            if (BalanzaDePagosReport.isInCountryLE(le)) {
                continue;
            }

            Map positions = row.getProperty(BOPositionReport.POSITIONS);
            Product product = invPosition.getProduct();
            String isinCode = product.getSecCode("ISIN");
            OrderedString positionMovementObj = (OrderedString) reportStyle.getColumnValue(row, "Movement Type", errors);
            String positionMovement = positionMovementObj.toString();
            // Iterate position dates and collect Data
            positions.keySet().stream().forEach(
                    keyDate -> {
                        Vector<InventorySecurityPosition> values  = (Vector<InventorySecurityPosition>) positions.get(keyDate);
                        boolean isBalance = this.balanceTypes.contains(positionMovement);
                        boolean isMovement = this.movementTypes.contains(positionMovement);
                        // ignore movements on Start and End Dates
                        if (isBalance
                                && !startDate.equals(keyDate)
                                && !endDate.equals(keyDate)) {
                            return;
                        }
                        if (isMovement
                                && (startDate.equals(keyDate))) {
                                        //||endDate.equals(keyDate))) {
                            return;
                        }

                        Double  nominal = getNominal(product, positionMovement, values, isBalance);
                        if (Double.valueOf(nominal).compareTo(0.0d) == 0)
                            return;

                        BalanzaDePagosItem item = new BalanzaDePagosItem(isinCode, le.getLegalEntityId(), inventoryType, positionMovement);

                        item.setProduct(product);
                        item.setCurrency(invPosition.getSettleCurrency());
                        item.setNominal(nominal);
                        item.setPositionDate((JDate) keyDate);
                        setPositionType(item, balanceTypes, movementTypes);
                        result.add(item);
            });
        }
        return result;
    }

    protected void setPositionType(BalanzaDePagosItem item, List<String> balanceTypes, List<String> movementTypes) {
        String positionType = "";
        String positionDetail = (Util.isEmpty(item.getPositionDetail()) ? "" : item.getPositionDetail());
        if (movementTypes.contains(positionDetail)) {
            if (item.getPositionDetail().endsWith(" Out")
                    && item.getNominal() <= 0) {
                positionType = BalanzaDePagosReport.MOV_OUT;
            } else if (item.getPositionDetail().endsWith(" In")
                    && item.getNominal() > 0) {
                positionType = BalanzaDePagosReport.MOV_IN;
            } else if (item.getNominal() > 0) {
                positionType = BalanzaDePagosReport.MOV_IN;
            } else {
                positionType = BalanzaDePagosReport.MOV_OUT;
            }
        }
        if (balanceTypes.contains(positionDetail)) {
            if (startDate.equals(item.getPositionDate())) {
                positionType = BalanzaDePagosReport.INITIAL;
            } else if (endDate.equals(item.getPositionDate())) {
                positionType = BalanzaDePagosReport.FINAL;
            }
        }
        item.setPositionType(positionType);
    }

    protected Double getNominal(Product product, String movementType, Vector<InventorySecurityPosition> values, boolean isBalance) {
        Double nominal = 0.0d;
        if (isBalance) {
            nominal = InventorySecurityPosition.getTotalSecurity(values, movementType);
        } else {
            nominal = InventorySecurityPosition.getDailySecurity(values, movementType);
        }

        if (nominal != null
                && nominal != 0 ) {
            if (product instanceof Bond) {
                Bond bond = (Bond) product;
                nominal *= bond.getFaceValue();
            }
        }
        return nominal;
    }

    protected DefaultReportOutput executeInventoryReport() throws CalypsoServiceException {
        Report report = getAndConfigureInventoryReport();
        if (report == null) {
            errors.add("Error loading Inventory Report for " + inventoryType);
            return null;
        }
        Log.system(LOG_CAT, "### Loading Inventory positions for " + inventoryType + " - Start Date : " + startDate + " End Date :" + endDate);

        DefaultReportOutput dro = report.load(errors);
        if (dro == null) {
            errors.add("Error executing Inventory Report for " + inventoryType);
            return null;
        }

        Log.system(LOG_CAT, "### Total positions loaded for " + inventoryType + " : " + dro.getNumberOfRows());
        return dro;
    }

    protected Report getAndConfigureInventoryReport() throws CalypsoServiceException {
        Report report = getInventoryReport();
        String templateName = getReportTemplateName();
        ReportTemplate template = getReportTemplate(report.getType(), templateName);
        if (template == null) {
            throw new CalypsoServiceException("Error loading report template for " + inventoryType);
        }
        report.setReportTemplate(template);
        configureTemplate(template);
        configureBalanceAndMovsFromSourceTemplate(template);
        return report;
    }

    private void configureBalanceAndMovsFromSourceTemplate(ReportTemplate template) {
        this.movementTypes = new Vector<>();
        this.balanceTypes = new Vector<>();
        if (null != template.get("INV_POS_MOVE")) {
            String s = template.get("INV_POS_MOVE");
            List<String> movList = Arrays.asList(s.split(","));
            for (String strMovType : movList) {
                if (strMovType.startsWith("Movement")) {
                    this.movementTypes.add(strMovType);
                }  else if (strMovType.startsWith("Balance")) {
                    this.balanceTypes.add(strMovType);
                }
            }
        }
    }

    private BOSecurityPositionReport getInventoryReport() {
        final BOSecurityPositionReport report = new BOSecurityPositionReport();
        report.setPricingEnv(pEnv);
        report.setValuationDatetime(valuationDatetime);
        return report;
    }

    protected String getReportTemplateName() {
        return "Balanza De Pagos - " + inventoryType;
    }

    private ReportTemplate getReportTemplate(String reportTYpe, String templateName) throws CalypsoServiceException {
        final ReportTemplate template = DSConnection.getDefault().getRemoteReferenceData()
                .getReportTemplate(ReportTemplate.getReportName(reportTYpe), templateName);
        if (template == null) {
            Log.error(this, "Template " + templateName + "NOT Found.");
            errors.add("Error Loading template : " + templateName);
            return null;
        }
        return template;
    }

}
