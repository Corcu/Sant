/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.report;

import com.calypso.apps.util.AppUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.bo.inventory.SpecificInventoryPositionValues;
import com.calypso.tk.bo.inventory.SpecificInventorySecurityPositionValues;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.*;

import static calypsox.tk.report.BOSecurityPositionReportTemplate.*;

public class ECMSDisponibilidadReport extends com.calypso.tk.report.BOSecurityPositionReport {

    private static final long serialVersionUID = 1373155839304048874L;

    //Constants
    protected static final String PRICING_ENV = "PricingEnvName";
    public static final String CORE_FLAG = "Core Flag";
    protected static final String EUR_NAME = "EUR";
    protected static final String BALANCE = "Balance";
    private static final String DIRTY_PRICE_STR = "DirtyPrice";
    private final String PLEDGE_DOMAIN_VALUE_ACCOUNT = "ECMS_OTHER_PLEDGE_ACCOUNTS";
    
	private static final String OTHER_PLEDGE_DOMAIN_VALUE_ACCOUNT = "ECMS_OTHER_PLEDGE_ACCOUNTS";
	private static final String ECMS_PLEDGE_DOMAIN_VALUE_ACCOUNT = "ECMS_PLEDGE_ACCOUNTS";
	private static final String DV_SEPARATOR = ";";

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public ReportOutput load(Vector errorMsgs) {

        String pricingEnvName = DIRTY_PRICE_STR;
        Vector bookAgreegate = LocalCache.getDomainValues(DSConnection.getDefault(),
				"ECMS_AGREEGATE_POS_STM_BOOKS");
        

        if (getPricingEnv() != null && !Util.isEmpty(getPricingEnv().getName())) {
            pricingEnvName = getPricingEnv().getName();
        }

        PricingEnv relloadedPE = AppUtil.loadPE(pricingEnvName, getValuationDatetime());
        setPricingEnv(relloadedPE);


        Map<String, Double> principals = new HashMap<>();
        DefaultReportOutput reportOutput = (DefaultReportOutput) super.load(errorMsgs);
        Map<String, Double> totalPO = new HashMap<>();
        Map<String, Double> totalPortfolio = new HashMap<>();
        Map<String, Double> otherPledges = new HashMap<>();
        Map<String, Double> totalECB = new HashMap<>();
        Map<String, Double> totalAvailable = new HashMap<>();
        Map<Integer, Product> inventoryProductCache = new HashMap<>();
        ArrayList<ReportRow> ECMSrows = new ArrayList<>();
		Map<String, Double> totalAssetSTM = new HashMap<>();
		ArrayList<String> allAccounts = null;
        ReportRow[] rows = initReportRows(reportOutput);

        ArrayList<String> accountsFinal = new ArrayList<>();
        Vector<String> pledgeAccounts = LocalCache.getDomainValues(DSConnection.getDefault(), "ECMS_PLEDGE_ACCOUNTS");
       
        for(String account: pledgeAccounts){
            if(!accountsFinal.contains(account)) {
                accountsFinal.add(account);
            }
        }
        


//        accounts[0] = LocalCache.getDomainValues(DSConnection.getDefault(), "SCF_EUROCLEAR_ECMS_PLEDGE_ACCOUNT").get(0);
//        accounts[1] = LocalCache.getDomainValues(DSConnection.getDefault(), "MAD_EUROCLEAR_ECMS_PLEDGE_ACCOUNT").get(0);
//        accounts[2] = LocalCache.getDomainValues(DSConnection.getDefault(), "SCF_IBERCLEAR_ECMS_PLEDGE_ACCOUNT").get(0);
//        accounts[3] = LocalCache.getDomainValues(DSConnection.getDefault(), "MAD_IBERCLEAR_ECMS_PLEDGE_ACCOUNT").get(0);

        ArrayList<String> pledge_accounts = new ArrayList<>();
        ArrayList<String> valid_accounts = new ArrayList<>();
        Vector<String> other_pledges_dv = LocalCache.getDomainValues(DSConnection.getDefault(), PLEDGE_DOMAIN_VALUE_ACCOUNT);
        for(int i = 0; i < other_pledges_dv.size(); i++){
            String dv = other_pledges_dv.get(i);
            String agent = dv.split(";")[2];
            for(int j = 0; j < pledgeAccounts.size(); j++){
                if(pledgeAccounts.get(j).split(";")[2].equalsIgnoreCase(agent)){
                    pledge_accounts.add(dv.split(";")[1]);
                    valid_accounts.add(pledgeAccounts.get(j).split(";")[3]);
                }
            }
        }

        String ecmsAccountName = "";
        String ecmsAccountName2 = "";

        for (int i = 0; i < rows.length; i++) {
            ReportRow row = rows[i];
            InventorySecurityPosition inventory = row.getProperty(ReportRow.INVENTORY);
            int productId = inventory.getSecurityId();

            Product product = null;
            if (inventoryProductCache.containsKey(productId)) {
                product = inventoryProductCache.get(productId);
            } else {
                product = inventory.getProduct();
                inventoryProductCache.put(productId, product);
            }

            row.setProperty(PRICING_ENV_PROPERTY, getPricingEnv());
            if (product instanceof Bond) {
                row.setProperty(QUOTE_SET_PROPERTY, "DirtyPrice");
            } else if (product instanceof Equity) {
                row.setProperty(QUOTE_SET_PROPERTY, "OFFICIAL");
            } else {
                row.setProperty(QUOTE_SET_PROPERTY, getPricingEnv().getQuoteSet().toString());
            }

            row.setProperty(BOSecurityPositionReportStyle.INV_PRODUCT, product);

            JDate startDate = super.getStartDate();

            if (this.getReportTemplate() != null && this.getReportTemplate().get("StartTenor") != null) {
                int startTenorDays = Integer.parseInt(this.getReportTemplate().get("StartTenor").toString().substring(0, 1));
                if (this.getReportTemplate().get("Tenors") != null) {
                    int tenorDays = Integer.parseInt(this.getReportTemplate().get("Tenors").toString().substring(0, 1));
                    if (tenorDays != 0) {
                        startDate = startDate.addBusinessDays(tenorDays, Util.string2Vector("SYSTEM"));
                    }
                }

                if (startTenorDays != 0) {
                    startDate = startDate.addDays(startTenorDays);
                }

            }
            row.setProperty("TOTAL_ASSETS", calculateTotal_Assets(row, startDate));
            row.setProperty("StartDate", startDate);
            row.setProperty("CurrentDateTime", new JDatetime());

            if (product instanceof Bond) {
                Bond b = (Bond) product;
                row.setProperty("FaceValue", b.getFaceValue());
                row.setProperty("Principal", b.getPrincipal(getValDate()));
            }

            String isin = product.getSecCode("ISIN");

            String po = inventory.getBook().getLegalEntity().getCode();
            String account = "";
            if (inventory.getAccount() != null) {
                account = inventory.getAccount().getName();
            }
//
//            if (accounts[0].split(";")[1].contains(po)) {
//                ecmsAccountName2 = accounts[0].split(";")[1];
//            } else if (accounts[1].split(";")[1].contains(po)) {
//                ecmsAccountName2 = accounts[1].split(";")[1];
//            } else {
//                ecmsAccountName2 = "";
//            }

            allAccounts = getEcmsAccounts();

            String book = inventory.getBook().getName();
            if ( !allAccounts.contains(account)) {
                if (totalPO.containsKey(isin + po)) {
                    double assets = row.getProperty("TOTAL_ASSETS");
                    double total = assets + totalPO.get(isin + po);
                    totalPO.put(isin + po, total);
                } else totalPO.put(isin + po, row.getProperty("TOTAL_ASSETS"));
            }

            if (totalAvailable.containsKey(isin + po + account)) {
                double assets = row.getProperty("TOTAL_ASSETS");
                double total = assets + totalAvailable.get(isin + po + account);
                totalAvailable.put(isin + po + account, total);
            } else totalAvailable.put(isin + po + account, row.getProperty("TOTAL_ASSETS"));

            if (!allAccounts.contains(account)) {
                if (totalPortfolio.containsKey(isin + book)) {
                    double assets = row.getProperty("TOTAL_ASSETS");
                    double total = assets + totalPortfolio.get(isin + book);
                    totalPortfolio.put(isin + book, total);
                } else totalPortfolio.put(isin + book, row.getProperty("TOTAL_ASSETS"));
            }

            String otherPledgesAccount = "";
            LegalEntity agent = inventory.getAgent();
            if(pledge_accounts.contains(account)){
                otherPledgesAccount = "CUENTAOTHERPLEDGESCORRECTA";
                
                if (otherPledges.containsKey(isin + book + po + agent + otherPledgesAccount)) {
                    //Comprobar que las cuentas sean las de pledges, si lo son, se suman, si no lo son
                    double assets = row.getProperty("TOTAL_ASSETS");
                    double total = assets + otherPledges.get(isin + book + po + agent + otherPledgesAccount);
                    otherPledges.put(isin + book + po + agent + otherPledgesAccount, total);
                } else otherPledges.put(isin + book + po + agent + otherPledgesAccount, row.getProperty("TOTAL_ASSETS"));
            }

            if (!account.isEmpty() && allAccounts.contains(account) && totalECB.containsKey(isin + po + account+agent)) {
                double assets = row.getProperty("TOTAL_ASSETS");
                double total = assets + totalECB.get(isin + po + account+ agent);
                totalECB.put(isin + po + account +agent, total);
            } else if (!account.isEmpty() && allAccounts.contains(account)) totalECB.put(isin + po + account +agent, row.getProperty("TOTAL_ASSETS"));


            for (String accountECMS:accountsFinal) {
                //EUROCLEAR
                ecmsAccountName = accountECMS.split(";")[3];
                //CUENTA NOMBRE LARGO
                ecmsAccountName2 = accountECMS.split(";")[1];

                if (!account.isEmpty() && (account.equalsIgnoreCase(ecmsAccountName2)
                        || account.equalsIgnoreCase(ecmsAccountName))) {
                    ECMSrows.add(row);
                }
            }
            
			if (!bookAgreegate.isEmpty() && bookAgreegate.contains(book)
					|| "STM".equals(inventory.getBook().getAttribute("Desk"))) {

				double assets = row.getProperty("TOTAL_ASSETS");
				if (product instanceof Bond) {
					double principal = row.getProperty("Principal");
					assets = assets * principal;
				}
				if (totalAssetSTM.containsKey(isin + po)) {
					double total = assets + totalAssetSTM.get(isin + po);
					totalAssetSTM.put(isin + po, total);
				}else {

				totalAssetSTM.put(isin + po, assets);
				}
			}
        }

        for (ReportRow row : rows) {
            Inventory inventory = row.getProperty(ReportRow.INVENTORY);
            LegalEntity agent = inventory.getAgent();
            Product product = row.getProperty(BOSecurityPositionReportStyle.INV_PRODUCT);
            String isin = product.getSecCode("ISIN");
            String po = "";
            String cartera = "";
            if (inventory.getBook() != null) {
                po = inventory.getBook().getLegalEntity().getCode();
                cartera = inventory.getBook().getName();
            }

            String account = "";
            if (inventory.getAccount() != null) {
                account = inventory.getAccount().getName();
            }
            String book = inventory.getBook().getName();
            //para calcular los campos TotalAvailabletByPOandISIN y TotalAvailableByBook ahora sumamos todas las posiciones que tenemos menos las que están asociadas a las cuentas de ECMS. Tampoco tendriamos que tener en cuenta las posiciones
            // asociadas a las cuentas que tenemos definidas en el domainvalue ECMS_OTHER_PLEDGE_ACCOUNTS
            if (totalPO.containsKey(isin + po)) {
                row.setProperty("TOTAL_ASSETS_POR_PO", totalPO.get(isin + po));
            } else {
                totalPO.put(isin + po, row.getProperty("TOTAL_ASSETS"));
                row.setProperty("TOTAL_ASSETS_POR_PO", totalPO.get(isin + po));
            }

            if (!account.isEmpty() && totalECB.containsKey(isin + po + account+agent)) {
                row.setProperty("TOTAL_ECB", totalECB.get(isin + po + account));
            } else if (!account.isEmpty()){
                totalECB.put(isin + po + account+agent, 0.0D);
                row.setProperty("TOTAL_ECB", totalECB.get(isin + po + account+agent));
            }

            if (!account.isEmpty() && totalAvailable.containsKey(isin + po + account)) {
                row.setProperty("TOTAL_AVAILABLE", totalAvailable.get(isin + po + account));
            } else if (!account.isEmpty()){
                totalAvailable.put(isin + po + account, row.getProperty("TOTAL_ASSETS"));
                row.setProperty("TOTAL_AVAILABLE", totalAvailable.get(isin + po + account));
            }

            //para calcular los campos TotalAvailabletByPOandISIN y TotalAvailableByBook ahora sumamos todas las posiciones que tenemos menos las que están asociadas a las cuentas de ECMS. Tampoco tendriamos que tener en cuenta las posiciones
            // asociadas a las cuentas que tenemos definidas en el domainvalue ECMS_OTHER_PLEDGE_ACCOUNTS
            if (!book.isEmpty() && totalPortfolio.containsKey(isin + book)) {
                row.setProperty("TOTAL_PORTFOLIO", totalPortfolio.get(isin + book));
            } else if (!book.isEmpty() && !allAccounts.contains(account)) {
                totalPortfolio.put(isin + book, row.getProperty("TOTAL_PORTFOLIO"));
            }

            if(valid_accounts.contains(account)) {
                String otherPledgesAccount = "CUENTAOTHERPLEDGESCORRECTA";
                if (otherPledges.containsKey(isin + book + po + agent + otherPledgesAccount)) {
                    row.setProperty("OTHER_PLEDGES", otherPledges.get(isin + book + po + agent + otherPledgesAccount));
                } else otherPledges.put(isin + book + po + agent + otherPledgesAccount, row.getProperty("OTHER_PLEDGES"));
            }




            Double principal = 0.0;
            if(!principals.containsKey(isin)){
                principal = product.getPrincipal(getValDate());
                principals.put(isin, principal);
            } else{
                principal = principals.get(isin);
            }


            String poECB = "";
            String carteraECB = "";

            HashMap<String, String> accountNames = new HashMap<String, String>();

            for (String accountName: accountsFinal) {
                if(accountName.split(";")[0].equals(po) && !accountNames.containsKey(accountName.split(";")[3])){
                    accountNames.put(accountName.split(";")[3],accountName.split(";")[3]);
                    accountNames.put(accountName.split(";")[1],accountName.split(";")[1]);
                }
                //EUROCLEAR
                ecmsAccountName = accountNames.get(accountName.split(";")[3]);
                //CUENTA NOMBRE LARGO
                ecmsAccountName2 = accountNames.get(accountName.split(";")[1]);

                if (!account.isEmpty() && account.equalsIgnoreCase(ecmsAccountName2)) {
                    for (ReportRow rowECB : ECMSrows) {
                        Inventory inventoryECB = rowECB.getProperty(ReportRow.INVENTORY);
                        Product productECB = inventoryECB.getProduct();
                        String isinECB = productECB.getSecCode("ISIN");
                        if (inventory.getBook() != null) {
                            poECB = inventoryECB.getBook().getLegalEntity().getCode();
                            carteraECB = inventoryECB.getBook().getName();
                        }

                        if (inventoryECB.getAccount() != null) {
                            String accountECB = inventoryECB.getAccount().getName();

                            if (isinECB != null && isinECB.equalsIgnoreCase(isin)
                                    && po != null && po.equalsIgnoreCase(poECB)
                                    && carteraECB != null && carteraECB.equalsIgnoreCase(cartera)
                                    && accountECB != null && accountECB.equalsIgnoreCase(ecmsAccountName2)) {
                            	 String xferOurAgentShortName = accountName.split(";")[2];
                                 double ecbAssets = calculatePledged_ECB(isin, ecmsAccountName2, xferOurAgentShortName, carteraECB, pledge_accounts);
                                 row.setProperty("ECB_Contrario", ecbAssets);
                                break;
                            }
                        }
                    }
                }

                if (!account.isEmpty() && account.equalsIgnoreCase(ecmsAccountName)) {
                    for (ReportRow rowECB : rows) {
                        Inventory inventoryECB = rowECB.getProperty(ReportRow.INVENTORY);
                        Product productECB = inventoryECB.getProduct();
                        String isinECB = productECB.getSecCode("ISIN");

                        if (inventory.getBook() != null) {
                            poECB = inventoryECB.getBook().getLegalEntity().getCode();
                            carteraECB = inventoryECB.getBook().getName();
                        }

                        if (inventoryECB.getAccount() != null) {
                            String accountECB = inventoryECB.getAccount().getName();

                            if (isinECB != null && isinECB.equalsIgnoreCase(isin)
                                    && po != null && po.equalsIgnoreCase(poECB)
                                    && carteraECB != null && carteraECB.equalsIgnoreCase(cartera)
                                    && accountECB != null && accountECB.equalsIgnoreCase(ecmsAccountName)) {
                                String xferOurAgentShortName = accountName.split(";")[2];
                                double ecbAssets = calculatePledged_ECB(isin, ecmsAccountName, xferOurAgentShortName, carteraECB, pledge_accounts);
                                row.setProperty("ECB_Contrario", ecbAssets);
                                break;
                            }
                        }
                    }
                }

            }         

            if("BSTE".equals(po) && "STM".equals(inventory.getBook().getAttribute("Desk"))){
     		
				row.setProperty("TOTAL_ASSET_STM", totalAssetSTM.get(isin + po ));
            }
        }

        return reportOutput;
    }

    private ReportRow[] initReportRows(DefaultReportOutput reportOutput) {
        ReportRow[] rows = new ReportRow[0];
        if (reportOutput != null) {
            rows = reportOutput.getRows();
        }
        return rows;
    }

    @Override
    public JDatetime getValuationDatetime() {
        //check if core flag is set
        if (checkCoreFlag()) {
            return super.getValuationDatetime();
        } else {
            JDatetime valDateTime = super.getValuationDatetime();
            if (valDateTime == null) {
                valDateTime = new JDatetime();
            }

            Vector<String> holidays = Util.string2Vector("SYSTEM");
            JDate valDate = valDateTime.getJDate(TimeZone.getDefault());
            valDate = valDate.addBusinessDays(-1, holidays);

            return new JDatetime(valDate, valDateTime.getField(Calendar.HOUR_OF_DAY),
                    valDateTime.getField(Calendar.MINUTE), valDateTime.getField(Calendar.SECOND), TimeZone.getDefault());
        }
    }


    /**
     * @param template
     * @param valDate
     * @return start date from template
     */
    protected JDate getStartDate(ReportTemplate template, JDate valDate) {
        return getDate(template, valDate, TradeReportTemplate.START_DATE, TradeReportTemplate.START_PLUS,
                TradeReportTemplate.START_TENOR);
    }

    /**
     * @param template
     * @param valDate
     * @return end date from template
     */
    protected JDate getEndDate(ReportTemplate template, JDate valDate) {
        return getDate(template, valDate, TradeReportTemplate.END_DATE, TradeReportTemplate.END_PLUS,
                TradeReportTemplate.END_TENOR);
    }

    private boolean checkCoreFlag() {
        final Boolean flag = getReportTemplate().get(CORE_FLAG);
        if (flag != null) {
            return flag;
        }
        return false;
    }

    protected Vector getHolidays() {
        Vector holidays = new Vector<>();
        if (getReportTemplate().getHolidays() != null) {
            holidays = getReportTemplate().getHolidays();
        } else {
            holidays.add("SYSTEM");
        }
        return holidays;

    }

    private double calculateTotal_Assets(ReportRow row, JDate startDate) {
        HashMap<JDate, Vector<Inventory>> positions = (HashMap) row.getProperty("POSITIONS");
        SpecificInventoryPositionValues.SpecificInventoryPositionValueContext posContext = (SpecificInventoryPositionValues.SpecificInventoryPositionValueContext) row.getProperty("SpecificInventoryPosition");
        SpecificInventorySecurityPositionValues specificInventorySecurityPositionValues = (SpecificInventorySecurityPositionValues) row.getProperty("SpecificInventoryPositionValue");

        Vector datedPositions = (Vector) positions.get(startDate);
        ReportRowKey uniqueKey = (ReportRowKey) row.getUniqueKey();
        String moveType = uniqueKey.getMoveType();

        double result = InventorySecurityPosition.getTotalSecurity(datedPositions, moveType, posContext);
        return result;
    }

    private double calculatePledged_ECB(String isin, String ecmsAccountName, String xferOurAgentShortName, String carteraECB, ArrayList<String> pledge_accounts) {
        double result = 0.0D;
        try {
            DSConnection rds = this.getDSConnection(true);
            ReportTemplate rt = rds.getRemoteReferenceData().getReportTemplate("Transfer", "ECMS_Transfer_Pledged");
            Attributes attr = rt.getAttributes();
            attr.put("SecCode", "ISIN");
            attr.put("SecCodeValue", isin);
            attr.put("Book", carteraECB);
            rt.setAttributes(attr);
            TransferReport transferReport = new TransferReport();
            transferReport.setReportTemplate(rt);
            DefaultReportOutput ro = (DefaultReportOutput) transferReport.load(new Vector());
            ReportRow[] rows = ro.getRows();
            for (ReportRow row : rows) {
                BOTransfer boTransfer = row.getProperty("BOTransfer");
                LegalEntity le = BOCache.getLegalEntity(rds, boTransfer.getInternalAgentId());
                Account account = BOCache.getAccount(rds, boTransfer.getGLAccountNumber());
                if (boTransfer.getStatus().equals(Status.SETTLED) && ecmsAccountName.equalsIgnoreCase(account.getAuthName()) && xferOurAgentShortName.equalsIgnoreCase(le.getAuthName())) {
                    double sum = boTransfer.getNominalAmount();
                    if (boTransfer.getPayReceiveType().equals("PAY")) {
                        sum *= -1.0D;
                    }
                    result += sum;
                }
            }
        } catch (CalypsoServiceException e) {
            Log.error(this, e.getCause());
        }
        //setear en la row una nueva property, con el isin+po+book,
        return Math.abs(result);
    }
    
	private ArrayList<String> getEcmsAccounts() {
		ArrayList<String> accountsFinal = new ArrayList<>();
		Vector<String> pledgeAccounts = LocalCache.getDomainValues(DSConnection.getDefault(), ECMS_PLEDGE_DOMAIN_VALUE_ACCOUNT);
		Vector<String> other_pledges = LocalCache.getDomainValues(DSConnection.getDefault(), OTHER_PLEDGE_DOMAIN_VALUE_ACCOUNT);
		
		for(String account: pledgeAccounts){
           
            accountsFinal.add(account.split(DV_SEPARATOR)[1]);  
        }
		for(String account: other_pledges){
	           
            accountsFinal.add(account.split(DV_SEPARATOR)[1]);  
        }

		return accountsFinal;
	}

}
