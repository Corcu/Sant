package calypsox.tk.report;

import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.binding.CustomBindVariablesUtil;
import calypsox.util.collateral.CollateralManagerUtil;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.haircut.HaircutRule;
import com.calypso.tk.refdata.haircut.HaircutRule.HaircutMethod;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteMarketData;
import com.calypso.tk.service.RemoteProduct;

import java.rmi.RemoteException;
import java.util.*;

import static calypsox.tk.report.Opt_MMOOHaircutDefinitionReportTemplate.*;

/**
 * Report of MMOO Haircuts quotes. Recovers the MMOO contracts, from them the quotes names of the
 * associated haircuts rules (as quoteSets) and generates the rows calling the quotes and isin of the product
 *
 * @author Guillermo Solano
 * @version 1.0
 */
public class Opt_MMOOHaircutDefinitionReport extends Report {


    private static final long serialVersionUID = -6789815140260142967L;
    public static final String MMOO_CONTRACT_TYPE = "MMOO";
    protected DSConnection dsConn = null;

    /**
     * Main methods. calls the report output & manages possible errors to show to the user.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public ReportOutput load(final Vector errorMsgsP) {

        StringBuffer error = new StringBuffer();

        try {

            return getReportOutput();

        } catch (RemoteException e) {
            Log.error(this, e); //sonar
            error.append("Error generating Optimization_HaircutDefinitionReport.\n");
            error.append(e.getLocalizedMessage());

        } catch (OutOfMemoryError e2) {
            Log.error(this, e2); //sonar
            error.append("Not enough memory to run this report.\n");

        } catch (Exception e3) {
            Log.error(this, e3); //sonar
            error.append("Error generating Optimization_HaircutDefinitionReport.\n");
            error.append(e3.getLocalizedMessage());
        }

        Log.error(this, error.toString());
        errorMsgsP.add(error.toString());

        return null;
    }

    /**
     * Get report output. First MMOO contract.
     *
     * @return DefaultReportOutput
     * @throws RemoteException
     */
    private DefaultReportOutput getReportOutput() throws Exception {

        final DefaultReportOutput output = new StandardReportOutput(this);
        final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();
        final JDate valueDate = getExecutionDate();
        this.dsConn = DSConnection.getDefault();

        // load contracts
        final Collection<CollateralConfig> contracts = loadMMOOContracts(valueDate, getReportTemplate());

        if (Util.isEmpty(contracts)) {
            Log.info(this, "Cannot find any open MMOO contract.\n");
            return null;
        }

        //gather the HR rules associated to a quote and their name (one per MMOO contract)
        final Map<String, String> contractsQuotesSetMap = getQuotesSetsForContracts(contracts);
        // retrieve the set of Quotes Set names
        final Vector<String> quotesSet = getQuoteSetNames(contractsQuotesSetMap);
        //gather all the quotes from them for an specific day
        final Vector<QuoteValue> mmooQuotes = getQuotesFromQuotesSet(quotesSet, valueDate);

        // create rows: quote + isin
        for (QuoteValue q : mmooQuotes) {
            if (q == null)
                continue;

            final List<String> data = getProductData(q);

            //GSM 14/01/15. Core don't finds Product
            if (data.isEmpty()) {
                Log.error(this, "Product for MMOO quote " + q.getName() + " NOT recovered");
                continue;
            }

            final String isin = data.get(0);
            final String ccy = data.get(1);

            if (Util.isEmpty(isin)) {
                Log.error(this, "MMOO quote " + q.getName() + " is empty");
                continue;
            }

            ReportRow row = new ReportRow(q, QUOTE);
            row.setProperty(ISIN, isin);
            row.setProperty(CURRENCY, ccy);
            reportRows.add(row);
        }

        output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));

        return output;

    }

    /**
     * @return execution date of the report
     */
    protected JDate getExecutionDate() {

        JDate date = null;
        final String startDate = (String) getReportTemplate().getAttributes().get(ReportTemplate.START_DATE); //for tests

        if (!Util.isEmpty(startDate))
            date = JDate.valueOf(startDate);

        if (date == null)
            date = getReportTemplate().getValDate(); //as param from the STRunner

        if (date == null) //none, just previous businness day
            date = JDate.getNow().addBusinessDays(-1, Util.string2Vector("SYSTEM"));

        return date;
    }

    /**
     * @param contractsQuotesSetMap
     * @return set of quotes set names
     */
    private Vector<String> getQuoteSetNames(final Map<String, String> contractsQuotesSetMap) {

        final Vector<String> quotesSet = new Vector<String>(contractsQuotesSetMap.keySet().size());

        for (Map.Entry<String, String> entry : contractsQuotesSetMap.entrySet()) {
            if (!quotesSet.contains(entry.getValue())) {
                quotesSet.add(entry.getValue());
            }
        }
        return quotesSet;
    }

    /**
     * @param quotesSet
     * @param valueDate
     * @return set of quotes
     * @throws RemoteException
     */
    @SuppressWarnings("unchecked")
    private Vector<QuoteValue> getQuotesFromQuotesSet(final Vector<String> quotesSet, final JDate valueDate) throws RemoteException {

        Vector<QuoteValue> vQuotes = new Vector<QuoteValue>();
        final RemoteMarketData remoteMarketData = DSConnection.getDefault().getRemoteMarketData();

        for (String quoteSetName : quotesSet) {
            //query
            final StringBuilder sb = new StringBuilder(" quote_set_name= '").append(quoteSetName).append("'")
                    .append(" and quote_name like '%.ISIN%' and TRUNC(quote_date) = ").append(Util.date2SQLString(valueDate))
                    .append(" and close_quote is not NULL");

            vQuotes.addAll(remoteMarketData.getQuoteValues(sb.toString()));
        }

        return vQuotes;
    }

    /**
     * @param contracts to recover Haircut rules and the names of the Quote Sets when the HaircutMethod is quote type
     * @return a map {contract name, quote set } when the haircut Rule is set up to use quote Sets
     * @see buildHaircutDefinitionsMap
     */
    protected Map<String, String> getQuotesSetsForContracts(Collection<CollateralConfig> contracts) {

        final Map<String, String> contractsQuotesSetMap = new HashMap<String, String>();

        for (CollateralConfig c : contracts) {
            final Map<String, HaircutRule> haircutRulesMap = buildHaircutDefinitionsMap(c);

            for (Map.Entry<String, HaircutRule> entry : haircutRulesMap.entrySet()) {

                for (HaircutMethod hcMethod : entry.getValue().getDefinitions()) {

                    if (!hcMethod.getType().equals(HaircutRule.HAIRCUT_TYPE_QUOTE))
                        continue;

                    final String quoteSet = hcMethod.getName();
                    if (!contractsQuotesSetMap.containsKey(c.getName())) {
                        contractsQuotesSetMap.put(c.getName(), quoteSet);
                    }
                }
            }
        }
        return contractsQuotesSetMap;
    }


    /**
     * Retrieve the ISIN of the equity.
     *
     * @param qv QuoteValue corresponding to the bond, dsConn DSConnection
     * @return String with the ISIN.
     * @throws RemoteException
     */
    public List<String> getProductData(final QuoteValue qv) throws RemoteException {

        // 1. get product desc item from quote name
        // 2. obtain the bond id
        // 3. use it for obtain the bond from product table
        final RemoteProduct rp = dsConn.getRemoteProduct();
        final String clausule = "quote_name = ?";
        final Vector<ProductDesc> v = rp.getAllProductDesc(clausule, buildCalypsoBindVariable(qv));

        final List<String> data = new ArrayList<>(2);

        if (!Util.isEmpty(v)) {

            Product p = rp.getProduct(v.get(0).getId());
            if (p instanceof Bond) {

                final Bond bond = (Bond) p;
                data.add(bond.getSecCode(ISIN));
                data.add(bond.getCurrency());
            } else if (p instanceof Equity) {

                final Equity equity = (Equity) p;
                data.add(equity.getSecCode(ISIN));
                data.add(equity.getCurrency());

            }
        }
        return data; //should not happen
    }

    /**
     * @param qv
     * @return
     */
    private List<CalypsoBindVariable> buildCalypsoBindVariable(QuoteValue qv) {
        return CustomBindVariablesUtil.createNewBindVariable(qv);
    }

    /**
     * @param jDate of alive MC Contracts
     * @return the list of open MMOO contracts
     * @throws RemoteException
     */
    public static List<CollateralConfig> loadMMOOContracts(final JDate jDate, ReportTemplate reportTemplate) throws RemoteException {

        final MarginCallConfigFilter mcFilter = new MarginCallConfigFilter();
        //BAU 22/02/2016 adrian
        List<Integer> legalEntitiesIds = new ArrayList<Integer>();
        Collection<LegalEntity> legalEntities = new Vector<LegalEntity>();
        legalEntities = CollateralUtilities.filterLEPoByTemplate(reportTemplate);
        for (LegalEntity legalentity : legalEntities) {
            legalEntitiesIds.add(legalentity.getEntityId());
        }
        //filters: open & MMOO contracts
        mcFilter.setProcessingOrgIds(legalEntitiesIds);
        mcFilter.setContractTypes(Arrays.asList(new String[]{MMOO_CONTRACT_TYPE}));
        mcFilter.setStatuses(Arrays.asList(new String[]{CollateralConfig.CLOSED}));

        final List<CollateralConfig> marginCallConfigs = CollateralManagerUtil.loadCollateralConfigs(mcFilter);
        final List<CollateralConfig> mcConfigsRet = new ArrayList<CollateralConfig>();

        for (CollateralConfig contract : marginCallConfigs) {

            if ((contract == null) || (contract.getContractType() == null)) {
                continue;
            }
            if (contract.getAgreementStatus().equals(CollateralConfig.CLOSED)) {
                continue;
            }
            if (contract.getClosingDate() != null && jDate != null && contract.getClosingDate().before(jDate.getJDatetime(TimeZone.getDefault()))) {
                continue;
            }
            if (MMOO_CONTRACT_TYPE.equals(contract.getContractType())) {
                mcConfigsRet.add(contract);
            }
        }

        return mcConfigsRet;
    }


    /**
     * Build a map with contract eligible sec filter names and haircut definitions linked to these filters
     *
     * @param contract from which obtain the haircutRules of type HAIRCUT_TYPE_QUOTE
     * @return map with tandem {contract.getHaircutName(), HaircutRule}
     */
    private static Map<String, HaircutRule> buildHaircutDefinitionsMap(final CollateralConfig contract) {

        final Map<String, HaircutRule> haircutRulesMap = new HashMap<String, HaircutRule>();

        try {
            final HaircutRule hr = ServiceRegistry.getDefault().getCollateralDataServer().getHaircutRule(contract.getHaircutName());

            for (HaircutMethod hcMethod : hr.getDefinitions()) {

                if (hcMethod.getType().equals(HaircutRule.HAIRCUT_TYPE_QUOTE)) {
                    if (!haircutRulesMap.containsKey(contract.getHaircutName())) {
                        haircutRulesMap.put(contract.getHaircutName(), hr);
                    }
                    continue;
                }
            }

        } catch (Exception e) {
            Log.error(Opt_HaircutDefinitionReport.class, "Cannot get haircut definitions from haircut rule = "
                    + contract.getHaircutName() + "\n", e);
        }

        return haircutRulesMap;

    }

//	public static void main(String[] pepe) throws RemoteException, CalypsoException {
//	
//	 final String args[] = { "-env", "dev5-local", "-user", "nav_it_sup_tec", "-password", "calypso" };
//	 @SuppressWarnings("unused")
//	 DSConnection ds = null;
//	 
//	 try {
//		 ds = ConnectionUtil.connect(args, "MainEntry");
//	 
//	 } catch (ConnectException e) {
//		 e.printStackTrace();
//	 }
//	 
//	 CollateralConfig c = ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfig(1992801);
//	 @SuppressWarnings("unused")
//	HaircutRule hr = ServiceRegistry.getDefault().getCollateralDataServer().getHaircutRule(c.getHaircutName());    
//	 
//	 Opt_MMOOHaircutDefinitionReport test = new Opt_MMOOHaircutDefinitionReport();
//	 //test.getReportOutput();
//	 test.getQuotesSetsForContracts(Arrays.asList(new CollateralConfig[] { c  }));
//	
//	 System.out.println();
//	 System.exit(0);
//	 // pruebas
//	 }

}
