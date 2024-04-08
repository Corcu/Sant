package calypsox.tk.report;

import calypsox.util.CheckRowsNumberReport;
import calypsox.util.SantReportingUtil;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.bo.InventoryCashPosition;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.ReportTemplateName;
import com.calypso.tk.service.DSConnection;
import org.apache.camel.dataformat.bindy.Format;
import org.apache.camel.dataformat.bindy.FormattingOptions;
import org.apache.camel.dataformat.bindy.format.factories.IntegerPatternFormatFactory;
import org.apache.commons.beanutils.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

public class IMAccountingReport extends Report implements CheckRowsNumberReport {
    /**
     *
     */
    private static final long serialVersionUID = 7220698258451151656L;
    private static final String POSITIONS = "POSITIONS";
    private static final String SEC = "SEC";
    private static final String CASH = "CASH";
    private static final String SECURTITY_POSITION = "BOSecurityPosition";
    private static final String CASH_POSITION = "BOCashPosition";
    private static final String ISIN = "ISIN";
    private static final String HOLIDAYS = "Holidays";

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public ReportOutput load(Vector arg0) {
        final DefaultReportOutput output = new StandardReportOutput(this);
        final ArrayList<ReportRow> reportRows = new ArrayList<>();
        final ReportTemplate reportTemplate = getReportTemplate();

        Vector holidays = new Vector<>();
        if (null != reportTemplate.getAttributes().get(HOLIDAYS)) {
            holidays = reportTemplate.getAttributes().get(HOLIDAYS);
        } else {
            holidays.add("SYSTEM");
        }

        JDate valDate = reportTemplate.getValDate();

        PricingEnv env = getPriceEnv(output, reportTemplate);

        ExecuteReport secThread = new ExecuteReport(SECURTITY_POSITION, valDate, env, holidays);
        ExecuteReport cashThread = new ExecuteReport(CASH_POSITION, valDate, env, holidays);

        // Load Reports
        startReport(secThread, reportTemplate, SEC);
        startReport(cashThread, reportTemplate, CASH);

        while (secThread.isAlive() || cashThread.isAlive()) {
            giveGraceTime(1000);
        }

        List<IMAccountingReportBean> beans = new ArrayList<>();
        beans.addAll(secThread.getBeans());
        beans.addAll(cashThread.getBeans());

        // create rows
        if (!Util.isEmpty(beans)) {
            for (IMAccountingReportBean bean : beans) {
                final ReportRow repRow = new ReportRow(bean);
                reportRows.add(repRow);
            }
        }

        output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));

        //Generate a task exception if the number of rows is out of an umbral defined
        HashMap<String , String> value = SantReportingUtil.getSchedTaskNameOrReportTemplate(this);
        checkAndGenerateTaskReport(output, value);


        return output;
    }

    public List<IMAccountingReportBean> createBeans(DefaultReportOutput reportoutput, JDate date, JDate valDate,
                                                    PricingEnv pricingEnv, Vector holidays) {
        List<IMAccountingReportBean> beans = new ArrayList<>();

        for (ReportRow row : reportoutput.getRows()) {
            beans.addAll(buildIMAccounting(row, date, valDate, pricingEnv, holidays));
        }

        return beans;
    }

    public List<IMAccountingReportBean> buildIMAccounting(ReportRow row, JDate date, JDate valDate,
                                                          PricingEnv pricingEnv, Vector holidays) {

        List<IMAccountingReportBean> beans = new ArrayList<>();
        IMAccountingReportBean bean = new IMAccountingReportBean();

        Inventory inventory = getInventory(row, date);
        if (inventory != null) {
            MarginCallConfig contract = BOCache.getMarginCallConfig(DSConnection.getDefault(),
                    inventory.getMarginCallConfigId());
            if (contract != null) {

                double price = getPrice(inventory, date, pricingEnv, holidays);
                // siempre fecha del día..
                bean.setProccesDate(formatDate(valDate)); // 6
                bean.setValueDate(formatDate(valDate)); // 6
                bean.setValueTradeDate(formatDate(valDate)); // 6
                bean.setBranchoffice("5493"); // 4
                bean.setPo("9015502"); // 7
                bean.setDirection("D"); // 1
                bean.setAmount(formatNumbers(String.valueOf(price))); // 17
                // (15,2)
                bean.setImp_eur(formatNumbers("0")); // 17 (15,2)
                bean.setCurrency(addSpaces(inventory.getSettleCurrency(), 3)); // ?
                // 3
                bean.setOrigne_amount(formatNumbers("0")); // 17 (15,2)
                bean.setOrigne_currency(addSpaces(" ", 3)); // 3
                bean.setCpty(addSpaces(contract.getLegalEntity().getCode(), 16)); // 16
                bean.setUme_sign("C"); // 1
                bean.setUme_product(addSpaces("DEUPUB", 6)); // 6
                bean.setUme_tipoper(addSpaces("COL", 4)); // 4
                bean.setProduct(addSpaces("CALL", 4)); // 4
                bean.setType_seat("N"); // 1
                bean.setMis("S"); // 1
                bean.setContractid(contractIdFormat(inventory.getMarginCallConfigId())); // 20
                bean.setSecurity(addSpaces(" ", 20)); // 20
                bean.setFolder(addSpaces(inventory.getBook().getAttribute("ALIAS_BOOK_GBO"), 10)); // 10

                if (inventory instanceof InventorySecurityPosition) {
                    Product produ = inventory.getProduct();
                    bean.setMaturityDate(addSpaces(getSecMaturity(produ), 6)); // 6
                    bean.setReference(addSpaces(inventory.getProduct().getSecCode("ISIN"), 12)); // 12
                } else if (inventory instanceof InventoryCashPosition) {
                    bean.setMaturityDate(addSpaces(" ", 6)); // 6
                    bean.setReference(addSpaces(" ", 12)); // 12
                }

                // add direction 1
                beans.add(bean);

                // create direction 2
                try {
                    bean = (IMAccountingReportBean) BeanUtils.cloneBean(bean);
                } catch (IllegalAccessException | InstantiationException | InvocationTargetException
                        | NoSuchMethodException e) {
                    Log.error("Cannot create the second direction of the position", e);

                }

                // modif bean 2
                bean.setDirection("H");
                bean.setPo("0906704");
                beans.add(bean);
            }
        }
        return beans;
    }

    // add spaces at the end
    public static String addSpaces(String field, int num) {
        StringBuilder builder = new StringBuilder();
        String fie = field;
        if (Util.isEmpty(fie)) {
            fie = " ";
        }
        int spaces = num - fie.length();
        if (spaces >= 0) {
            builder.append(fie);
            for (int i = 1; i <= spaces; i++) {
                builder.append(" "); // append a blank space
            }
        }
        return builder.toString();
    }

    // add 0 at the beginning
    private static String formatNumbers(String field) {
        String out = "";
        String number = field;
        if (!Util.isEmpty(number)) {
            try {
                final DecimalFormat format = new DecimalFormat("000000000000000.00");
                BigDecimal big = new BigDecimal(number).abs();
                out = format.format(big);
                if (out.contains(",")) {
                    out = out.replace(",", "");
                } else if (out.contains(".")) {
                    out = out.replace(".", "");
                }
            } catch (Exception e) {
                Log.error(IMAccountingReport.class, "Format error num: " + number + " Error: " + e);
            }
        }

        return out;
    }

    private static String contractIdFormat(Integer field) {
        String out = "";
        FormattingOptions options = new FormattingOptions();
        options = options.withPattern("0000000000000000000");
        options.withLocale("default");

        IntegerPatternFormatFactory patternFactory = new IntegerPatternFormatFactory();
        Format<Integer> formatter = (Format<Integer>) patternFactory.build(options);
        try {
            out = formatter.format(field);
        } catch (Exception e) {
            Log.error(IMAccountingReport.class, "Format contractid error: " + e);
        }
        return out;
    }

    // format date to ddMMyy
    private static String formatDate(JDate jdate) {
        String date = "";
        if (jdate != null) {
            SimpleDateFormat format = new SimpleDateFormat("ddMMyy", Locale.getDefault());
            date = format.format(jdate.getDate());
        }
        return date;
    }

    private class ExecuteReport extends Thread {
        private String templatename;
        private String report;
        private JDate valDate;
        private Vector holidays;
        private PricingEnv pricingEnv;
        protected List<IMAccountingReportBean> beans;

        public ExecuteReport(String report, JDate valDate, PricingEnv pricingEnv, Vector holidays) {
            this.report = report;
            this.valDate = valDate;
            this.beans = new ArrayList<>();
            this.holidays = holidays;
            this.pricingEnv = pricingEnv;
        }

        public void setTemplateName(String temp) {
            this.templatename = temp;
        }

        public List<IMAccountingReportBean> getBeans() {
            return this.beans;
        }

        @Override
        public void run() {
            Vector<String> errorMsgs = new Vector<>();
            try {
                DefaultReportOutput defaultReportOutput = null;
                ReportTemplateName templateName = new ReportTemplateName(templatename);
                ReportTemplate template = BOCache.getReportTemplate(DSConnection.getDefault(), report, templateName);
                JDate date = valDate.addBusinessDays(-1, holidays);
                if (template != null) {
                    if (SECURTITY_POSITION.equals(report)) {
                        BOSecurityPositionReport securityreport = new BOSecurityPositionReport();
                        securityreport.setReportTemplate(template);
                        securityreport.setPricingEnv(getPricingEnv());
                        securityreport.setValuationDatetime(valDate.getJDatetime());
                        securityreport.setStartDate(date);
                        securityreport.setEndDate(date);
                        defaultReportOutput = (DefaultReportOutput) securityreport.load(errorMsgs);
                    } else if (CASH_POSITION.equals(report)) {
                        BOCashPositionReport cashreport = new BOCashPositionReport();
                        cashreport.setReportTemplate(template);
                        cashreport.setPricingEnv(getPricingEnv());
                        cashreport.setValuationDatetime(date.getJDatetime());
                        cashreport.setStartDate(date);
                        cashreport.setEndDate(date);
                        defaultReportOutput = (DefaultReportOutput) cashreport.load(errorMsgs);
                    }
                    if (null != defaultReportOutput) {
                        beans.addAll(createBeans(defaultReportOutput, date, valDate, pricingEnv, holidays));
                    }
                }
            } catch (Exception e) {
                Log.error(this, "Cannot load: " + report + " Error: " + e + "Errors: " + errorMsgs);
            }
            if (!Util.isEmpty(errorMsgs)) {
                Log.info(this, errorMsgs.toString());
            }
        }
    }

    private void startReport(ExecuteReport thread, ReportTemplate reportTemplate, String templateName) {
        if (null != reportTemplate.getAttributes().get(templateName)) {
            thread.setTemplateName(reportTemplate.getAttributes().get(templateName).toString());
            thread.start();
        }
    }

    private Inventory getInventory(ReportRow row, JDate valDate) {
        if (row != null && row.getProperty(POSITIONS) != null) {
            Map<JDate, Vector<Inventory>> positions = row.getProperty(POSITIONS);
            Vector<Inventory> inventory = positions.get(valDate);
            if (!Util.isEmpty(inventory)) {
                return inventory.get(0);
            }
        }
        return null;
    }

    private String getSecMaturity(Product produ) {
        if (produ != null && !(produ instanceof Equity) && produ.getMaturityDate() != null) {
            return formatDate(produ.getMaturityDate()); // 6
        }
        return "";
    }

    private void giveGraceTime(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private double getPrice(Inventory inventory, JDate valDate, PricingEnv pricingEnv, Vector holidays) {
        double amount = inventory.getTotal();

        if (inventory instanceof InventorySecurityPosition) {
            Product product = ((InventorySecurityPosition) inventory).getProduct();
            if (product instanceof Bond) {
                amount = Math.abs(amount * ((Bond) product).getFaceValue(valDate) * getDirtyPrice(
                        ((InventorySecurityPosition) inventory).getProduct(), valDate, pricingEnv, holidays));
            } else if (product instanceof Equity) {
                amount = Math.abs(amount * getDirtyPrice(((InventorySecurityPosition) inventory).getProduct(), valDate,
                        pricingEnv, holidays));
            }
        }

        return amount;
    }

    private double getDirtyPrice(Product product, JDate valDate, PricingEnv pricingEnv, Vector holidays) {
        Vector<QuoteValue> vQuotes = new Vector<QuoteValue>();
        if (product != null && pricingEnv != null && !"".equals(pricingEnv.getQuoteSetName())) {
            String isin = product.getSecCode(ISIN);
            String quotesetName = pricingEnv.getQuoteSetName();
            try {

                String quoteName = CollateralUtilities.getQuoteNameFromISIN(isin, valDate);

                if (!Util.isEmpty(quoteName)) {
                    if (product instanceof Bond) {
                        String clausule = "quote_name = " + "'" + quoteName + "' AND trunc(quote_date) = "
                                + Util.date2SQLString(valDate) + " AND quote_set_name = '" + quotesetName + "'";
                        vQuotes = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(clausule);
                        if ((vQuotes != null) && (vQuotes.size() > 0)) {
                            return vQuotes.get(0).getClose();
                        }
                    } else if (product instanceof Equity) {
                        String clausule = "quote_name = " + "'" + quoteName + "' AND trunc(quote_date) = "
                                + Util.date2SQLString(valDate) + " AND quote_set_name = 'OFFICIAL'";
                        vQuotes = DSConnection.getDefault().getRemoteMarketData().getQuoteValues(clausule);
                        if ((vQuotes != null) && (vQuotes.size() > 0)) {
                            return vQuotes.get(0).getClose();
                        }
                    }
                }

            } catch (RemoteException e) {
                Log.error(this, "Cannot retrieve dirty price", e);
            }
        }
        return 0.00;
    }

    private PricingEnv getPriceEnv(DefaultReportOutput output, ReportTemplate reportTemplate) {
        PricingEnv env = null;
        if (output.getPricingEnv() != null) {
            env = output.getPricingEnv();
        } else if (reportTemplate.getAttributes().get("PricingEnvName") != null) {
            String pename = reportTemplate.getAttributes().get("PricingEnvName").toString();
            try {
                env = DSConnection.getDefault().getRemoteMarketData().getPricingEnv(pename);
            } catch (CalypsoServiceException e) {
                Log.error(KGR_Collateral_MarginCallReport.class, "Cannot get pricingEnv for: " + pename + " " + e);

            }
        } else {
            Log.warn(IMAccountingReport.class, "Cannot get pricingEnv for null PE");
        }
        return env;

    }
}
