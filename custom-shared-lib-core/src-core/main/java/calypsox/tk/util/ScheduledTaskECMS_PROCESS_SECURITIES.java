package calypsox.tk.util;

import calypsox.tk.report.BOSecurityPositionReport;
import calypsox.tk.report.BOSecurityPositionReportTemplate;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.product.SimpleTransfer;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.ScheduledTask;

import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class ScheduledTaskECMS_PROCESS_SECURITIES extends ScheduledTask {

    public static final String ISIN = "ISIN";
    public static final String PROCORG = "ProcessingOrg";
    private static String QUERY_NON_ELEGIBLE_ISINS = "SELECT * FROM product_sec_code WHERE sec_code ='ECMS_NonElegibilityDate' AND (code_value_ucase LIKE 'currentDate')";
    private static String QUERY_ISIN_PLEDGED_POSITIONS = "";

    private static final String PO = "ProcessingOrg";
    private final static String SECURITY = "SECURITY";

    private final String PLEDGE_DOMAIN_VALUE_ACCOUNT = "ECMS_PLEDGE_ACCOUNTS";
    private DSConnection dsCon;

    @Override
    public String getTaskInformation() {
        return "Unloads all the operations from ECMS that are no more elegible for pignorations";
    }

    public boolean process(DSConnection ds, PSConnection ps) {
        this.dsCon = ds;
        ArrayList<String> isins = loadNonEligibleISINS(ds);

        for (String isin : isins) {
            Product product = isinExists(isin);
            if (product != null) {
                try {
                    boolean hasPignotions = getOpenPositions(product, isin);
                } catch (RemoteException | CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }

        }

        return true;
    }


    private ArrayList loadNonEligibleISINS(DSConnection ds) {
        ArrayList<String> nonISIN = new ArrayList<>();
        JDatetime fecha = new JDatetime(this.getCurrentDate(), dsCon.getUserDefaults().getTimeZone());
        DateFormat formatter = new SimpleDateFormat("ddMMyyyy");
        String date = formatter.format(fecha); //cambiar por ValuationDate();
        String query = QUERY_NON_ELEGIBLE_ISINS.replace("currentDate", date);
        try {
            Vector<Vector<String>> result = (Vector<Vector<String>>) ds.getRemoteAccess().executeSelectSQL(query, null);
            int count = 0;
            for (Vector row : result) {
                if (count < 2) {
                    count++;
                } else {
                    if (!row.isEmpty() && row.get(0) != null) {
                        Long productId = (Long) row.get(0);
                        if (productId != 0) {
                            int prod = Math.toIntExact(productId);
                            Product p = dsCon.getRemoteProduct().getProduct(prod);

                            if (p != null) {
                                String isin = p.getSecCode(ISIN);

                                if (!isin.isEmpty() && !nonISIN.contains(isin)) {
                                    nonISIN.add(isin);
                                }
                            }
                        }
                    }
                }
            }

        } catch (CalypsoServiceException e) {
            Log.error(this, "Could not retrieve products from database", e);
        }
        return nonISIN;
    }

    private Product isinExists(String isinCode) {
        try {
            return dsCon.getDefault().getRemoteProduct()
                    .getProductByCode(ISIN, isinCode);
        } catch (CalypsoServiceException e) {
            Log.error(this, "Could not retrieve products from database", e);
        }
        return null;
    }


    private boolean getOpenPositions(Product product, String isin) throws RemoteException, CloneNotSupportedException {
        ArrayList<ReportRow> results = new ArrayList<>();
        int account = 0;
        boolean havePignorations = false;

        ArrayList<String> accounts = new ArrayList<>();
        HashMap<String, HashSet<Integer>> accountsMap;
        accountsMap = loadPledgedAccounts();
        ReportTemplate report = dsCon.getRemoteReferenceData().getReportTemplate("BOSecurityPosition", "TestIPECMS");
        Attributes attributesOriginal = report.getAttributes();
        ArrayList<ReportOutput> reportResults = new ArrayList<>();

        if (report != null) {

            if (!isin.isEmpty()) {
                attributesOriginal.add("SEC_CODE", "ISIN");
                attributesOriginal.add("SEC_CODE_VALUE", isin);
            }

            loadResultsAccounts(accountsMap, report, attributesOriginal, results);
        }

        for (ReportRow row : results) {
            BOSecurityPositionReportTemplate reportTemplate = (BOSecurityPositionReportTemplate) row.getProperty("ReportTemplate");
            Attributes attri = reportTemplate.getAttributes();
            InventorySecurityPosition inventory = row.getProperty("Inventory");
            Trade t = new Trade();
            LegalEntity le = LegalEntity.valueOf("ECMS");
            t.setCounterParty(le); //Contrapartida ECMS
            t.setTradeDate(new JDatetime(JDate.getNow().addBusinessDays(1, Util.string2Vector("TARGET"))));
            t.setSettleDate(JDate.getNow().addBusinessDays(1, Util.string2Vector("TARGET")));

            Hashtable<String, String> keywords = new Hashtable<>();
            keywords.put("isECMSPledge", "Y");
            keywords.put("collateralAllocationType", "Substitution");
            keywords.put("ECMS_Auto_Unpledge", "Y");
            keywords.put("WorkflowSubType", "ECMSPledge");

            MarginCall marginCall = new MarginCall();
            Product security = inventory.getProduct();
            t.setTradeCurrency(security.getCurrency());
            t.setSettleCurrency(security.getCurrency());
            double notional = marginCall.getNotional(t.getSettleDate());
            marginCall.setSecurity(security);
            marginCall.setOrdererRole("ProcessingOrg");
            marginCall.setOrdererLeId(inventory.getBook().getLegalEntity().getId());
            marginCall.setFlowType("SECURITY");
            t.setBook(inventory.getBook());
            String processingOrg = attri.get("ProcessingOrg");
            //marginCall.setSecCode("ISIN", attri.get("SEC_CODE_VALUE"));
            String agentName = inventory.getAgent().getCode();
            keywords.put("ECMSCustodianPledge", agentName);
            if (!agentName.isEmpty() && agentName.equalsIgnoreCase("ECMS")) {
                String account_pledge = inventory.getAccount().getName();
                Vector<String> dvs = LocalCache.getDomainValues(DSConnection.getDefault(), "ECMS_PLEDGE_ACCOUNTS");
                ArrayList<String> array = new ArrayList<String>();
                array.addAll(dvs);

                for (int i = 0; i < array.size(); i++) {
                    String[] result = array.get(i).split(";");
                    if (result[1].equalsIgnoreCase(account_pledge)) {
                        keywords.put("CSDCustodianPledge", result[2]);
                    }
                }
            } else {
                keywords.put("CSDCustodianPledge", agentName);
            }
            if (agentName.equalsIgnoreCase("5GSR")) {
                keywords.put("SETR", "IBRC/PGCU");
            } else {
                keywords.put("SETR", "");
            }
            t.setKeywords(keywords);

            Vector<MarginCallConfig> contracts = dsCon.getRemoteReferenceData().getAllMarginCallConfig(t.getBook().getLegalEntity().getId(), le.getId());
            if (contracts.size() != 0) {
                marginCall.setLinkedLongId(contracts.firstElement().getId());
            }
            if (marginCall.getSecurity() instanceof Bond) {
                double faceValue = ((Bond) marginCall.getSecurity()).getFaceValue();
                marginCall.setPrincipal(faceValue);
                t.setQuantity(inventory.getTotal());
            } else {
                t.setQuantity(inventory.getTotal());
            }
            t.setTradePrice(getQuote(marginCall, t));
            if (t.getQuantity() > 0.0) {
                t.setAction(Action.NEW);
                t.setProduct(marginCall);
                t.getProduct().setSubType("SECURITY");
                dsCon.getRemoteTrade().save(t);
            }
        }
        return havePignorations;
    }


    private HashMap<String, HashSet<Integer>> loadPledgedAccounts() {
        Vector<String> other_pledges_dv = LocalCache.getDomainValues(DSConnection.getDefault(), PLEDGE_DOMAIN_VALUE_ACCOUNT);
        HashMap<String, HashSet<Integer>> map = new HashMap<>();
        for (int i = 0; i < other_pledges_dv.size(); i++) {
            String dv = other_pledges_dv.get(i);
            String agent = dv.split(";")[0];
            if (agent.isEmpty()) {
                continue;
            }
            String accountName = dv.split(";")[1];
            Integer accId = getIdAccountFromName(accountName);
            if (accId == 0) {
                continue;
            }
            if (map.get(agent) != null) {
                map.get(agent).add(getIdAccountFromName(accountName));
            } else {
                HashSet set = new HashSet<Integer>();
                set.add(getIdAccountFromName(accountName));
                map.put(agent, set);
            }
        }
        return map;
    }

    private DefaultReportOutput loadResults(ReportTemplate report, Attributes attributes) {
        report.setAttributes(attributes);
        BOSecurityPositionReport boReport = new BOSecurityPositionReport();
        boReport.setReportTemplate(report);
        boReport.setValuationDatetime(JDatetime.valueOf(this.getValuationDatetime().toString()));
        Vector errorMsg = new Vector();
        return (DefaultReportOutput) boReport.load(errorMsg);
    }

    @Override
    public JDatetime getValuationDatetime() {
        JDatetime valDateTime = super.getValuationDatetime();
        if (valDateTime == null) {
            valDateTime = new JDatetime();
        }

        Vector<String> holidays = Util.string2Vector("SYSTEM");
        JDate valDate = valDateTime.getJDate(TimeZone.getDefault());
        valDate = valDate.addBusinessDays(+1, holidays);

        return new JDatetime(valDate, valDateTime.getField(Calendar.HOUR_OF_DAY),
                valDateTime.getField(Calendar.MINUTE), valDateTime.getField(Calendar.SECOND), TimeZone.getDefault());
    }

    private void loadResultsAccounts(HashMap<String, HashSet<Integer>> accountsMap, ReportTemplate report, Attributes attributes, ArrayList<ReportRow> finalRows) throws CalypsoServiceException {

        int account = 0;
        DefaultReportOutput results = loadResults(report, attributes);

        ReportRow[] rows = results.getRows();
        for (int i = 0; i < rows.length; i++) {
            String po = String.valueOf(results.getValueAt(i, 0));
            if (isValidRow(rows[i], accountsMap, po)) {
                finalRows.add(rows[i]);
            }
        }
    }

    private Integer getIdAccountFromName(String acc) {
        Integer id = 0;
        Vector<Account> accounts = null;
        try {
            accounts = DSConnection.getDefault().getRemoteAccounting().getAccountsByName(acc);
            if (!accounts.isEmpty()) {
                id = accounts.get(0).getId();
            }
        } catch (CalypsoServiceException e) {
            throw new RuntimeException(e);
        }
        return id;
    }

    private boolean isValidRow(ReportRow row, HashMap<String, HashSet<Integer>> accountsMap, String po) {
        InventorySecurityPosition position = (InventorySecurityPosition) row.getProperty("Default");
        return position.getTotal() > 0 && accountsMap.get(po) != null && accountsMap.get(po).contains(position.getAccountId());
    }

    /* Create simpleTransfer product to attach on trade */
    private SimpleTransfer createSimpleTransferProduct(Book book, Product security) {

        // create simple transfer
        SimpleTransfer product = new SimpleTransfer();
        product.setFlowType(SECURITY);
        product.setOrdererRole(PO);
        product.setOrdererLeId(book.getLegalEntity().getId());
        product.setSecurity(security);
        product.setPrincipal(security.getPrincipal());

        return product;
    }

    private double getQuote(MarginCall marginCall, Trade trade) {
        double result = 0;
        StringBuilder query = new StringBuilder();
        query.append("quote_name LIKE '%");
        query.append(marginCall.getSecurity().getSecCode("ISIN"));
        query.append("%' and quote_date=");
        JDate dateQuote = JDate.getNow().addBusinessDays(-1, Util.string2Vector("TARGET"));
        query.append(Util.date2SQLString(dateQuote));
        if (marginCall.getSecurity() instanceof Bond) {
            query.append(" and quote_set_name= 'DirtyPrice'");
        } else {
            query.append(" and quote_set_name= 'OFFICIAL'");
        }
        try {
            Vector<QuoteValue> quoteValues =
                    DSConnection.getDefault().getRemoteMarketData().getQuoteValues(query.toString());
            if (quoteValues != null && !quoteValues.isEmpty()) {
                return quoteValues.get(0).getClose();
            } else {
                if (marginCall.getSecurity() instanceof Bond) {
                    return 1;
                } else {
                    return 0.01;
                }
            }
        } catch (CalypsoServiceException e) {
            throw new RuntimeException(e);
        }
    }


}
