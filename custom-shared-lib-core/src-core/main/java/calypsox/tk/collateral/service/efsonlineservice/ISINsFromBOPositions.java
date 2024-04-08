/**
 *
 */
package calypsox.tk.collateral.service.efsonlineservice;

import calypsox.tk.collateral.service.efsonlineservice.beans.QuoteBeanIN;
import calypsox.tk.collateral.service.efsonlineservice.beans.QuoteBeanOUT.QUOTE_TYPE;
import calypsox.tk.collateral.service.efsonlineservice.interfaces.BoISINProcessingInterface;
import calypsox.util.SantReportingUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.InventorySecurityPositionArray;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Generates a list with all the relevant positions from where to extract the bonds and equities (which have an impact
 * and are relevant). It extracts from these positions the ISIN and the CCY for each product and finally puts a copy in
 * the context.
 *
 * @author Guillermo Solano
 * @version 1.1, 02/09/2013, optimize gathering securities from positions filtered by zero
 * @see BoISINProcessingInterface
 *
 */
public class ISINsFromBOPositions extends ThreadController<InventorySecurityPosition> implements
        BoISINProcessingInterface {

    /**
     * Access to the context.
     */
    private final EFSContext context;
    /**
     * stores bonds and equities
     */
    private List<QuoteBeanIN> bondsFromPositions;
    private List<QuoteBeanIN> equitiesFromPositions;

    /**
     * Map including all the bonds and equities, extracted from the a second method that actually recovers the
     * securities ids for positions that are different from zero.
     */
    private Map<Integer, ? extends Product> securitiesMap;

    /**
     * Main Constructor
     *
     * @param true to be executed as an independent thread.
     */
    public ISINsFromBOPositions(boolean threadOriented) {

        super(threadOriented);
        this.context = EFSContext.getEFSInstance();
        this.bondsFromPositions = this.equitiesFromPositions = null;
        super.setSleepTime(EFSContext.getReadPositionsSleepTime());
    }

    /**
     * Generic Constructor, multithreading activated
     */
    public ISINsFromBOPositions() {
        this(true);
    }

    /**
     * Call the main Method to read the last bonds and prices from the database and write a set of quotes subscription
     * on the context. Matches interface with ThreadController.
     *
     */
    @Override
    public void readProductsFromDatabasePositions() {

        load();
    }

    /**
     * Main method of ThreadController, contains the main logic to be executed under the thread.
     */
    @Override
    public void runThreadMethod() {

        Log.info(ISINsFromBOPositions.class, "readPosition thread started.");

        do {

            // first time, it will have to run this thread. Until it doesn't feel the context, it cannot be called the
            // webservice and thus, it cannot be called to store the quotes on DB.

            try {
                // METHOD 2: EXTRACT ISIN+CCY FROM SECURITIES
                // retrieve securities IDs from positions
                gatherBOSecuritiesIDs(JDate.getNow());
                // retrieve the list of isins+ccy
                retrieveISINsFromSecuritiesMap();

                // METHOD 1: EXTRACT ISIN+CCY FROM POS
                // retrieve bond/equities positions
                // gatherBOPositions(JDate.getNow());
                // retrieve the list of isins
                // retrieveISINsListFromPositions();

                // copy requested list to the context
                updateContextSubscriptionList();

                Log.info(ISINsFromBOPositions.class, " Time to Process in sec ReadPos iteration= " + getRunningTime());

                // increase executions counter
                super.increaseExecutionsCounter();

                // now wait till next iteration
                super.sleepThread();

            } catch (EFSException e) {

                // Exception control and Log
                super.processException(e);
            }

        } while (super.isAlive());
    }

    /**
     * if the thread is running, this will stop it (the main runThreadMethod() clause).
     */
    @Override
    public void killUpdatingBOPostionsThread() {

        super.enableThreading = false;
    }

    // /////////////////////////////////////////////////////////
    // / METHOD 1 TO GATHER SECURITIES: THROUGH POSITIONS /////
    // ///////////////////////////////////////////////////////

    /**
     * @return a list with all the relevant positions from where to extract the bonds and equities (which have an impact
     *         and are relevant). In other words, returns all the positions different from zero for products of type
     *         bonds and equities.
     * @param date
     *            to be search for the positions
     * @throws EFSException
     */
    @Override
    public void gatherBOPositions(final JDate today) throws EFSException {

        final String where = inventorySqlWhereClause(today).toString();
        final String from = inventorySqlFromClause().toString();
        InventorySecurityPositionArray secPositions = null;

        try {
            // read positions from database
            secPositions = DSConnection.getDefault().getRemoteBackOffice().getInventorySecurityPositions(from, where, null);

        } catch (RemoteException e) {
            Log.error(this, e); //sonar
            throw new EFSException("Not possible to read Positions from DB! \n" + e.getLocalizedMessage(), true);
        }

        if ((secPositions == null) || secPositions.isEmpty()) {

            throw new EFSException("No positions returned from DB. Process sleep.", false, true);
        }

        super.dataList = new ArrayList<InventorySecurityPosition>(secPositions.size());

        for (int i = 0; i < secPositions.size(); i++) {

            final InventorySecurityPosition pos = secPositions.get(i);
            final String productType = pos.getProduct().getType();

            // check positions is bond or equity
            if (productType.equalsIgnoreCase(QUOTE_TYPE.BOND.name().toLowerCase())
                    || productType.equalsIgnoreCase(QUOTE_TYPE.EQUITY.name().toLowerCase())) {

                // correct position, then added it
                super.dataList.add(pos);
            }
        } // end for
    }

    /**
     * @return true if positions have been processed and the quotes isin and ccy have been extracted; false if the
     *         dataList was empty (gatherBOPositions have to be called first).
     *
     */
    @Override
    public boolean retrieveISINsListFromPositions() {

        if ((super.dataList == null) || super.dataList.isEmpty()) {
            return false;
        }

        this.bondsFromPositions = new ArrayList<QuoteBeanIN>(super.dataList.size());
        this.equitiesFromPositions = new ArrayList<QuoteBeanIN>(super.dataList.size());
        final HashSet<String> setPosInserted = new HashSet<String>();

        for (InventorySecurityPosition pos : super.dataList) {

            final int securityId = pos.getSecurityId(); // we retrieve the product
            final Product product = BOCache.getExchangedTradedProduct(DSConnection.getDefault(), securityId);

            if (product == null) {
                continue;
            }
            // read the isin & currency
            final String ISIN = product.getSecCode("ISIN");
            final String currency = product.getCurrency();

            if ((ISIN == null) || (currency == null) || ISIN.isEmpty() || currency.isEmpty()) {
                continue;
            }

            // check if it has been read
            final String id = ISIN + currency;
            // if the product has been processed, just is discarded
            if (setPosInserted.contains(id)) {
                continue;
            }

            // build the quote bean
            final QuoteBeanIN quoteBean = new QuoteBeanIN();
            quoteBean.setCurrency(currency);
            quoteBean.setISIN(ISIN);

            if (product.getType().equals(Product.BOND)) {

                quoteBean.setType(QUOTE_TYPE.BOND);
                this.bondsFromPositions.add(quoteBean);
                setPosInserted.add(id);

            } else {

                quoteBean.setType(QUOTE_TYPE.EQUITY);
                this.equitiesFromPositions.add(quoteBean);
                setPosInserted.add(id);
            }
        }
        // end all positions processed
        return true;
    }

    // /////////////////////////////////////////////////////////
    // / METHOD 2 TO GATHER SECURITIES: THROUGH SEC. IDS //////
    // ///////////////////////////////////////////////////////

    /**
     * @return a list with all the securities IDs from where to extract the bonds and equities. In other words, returns
     *         all securities ids from positions different from zero for products of type bonds and equities.
     * @param date
     *            to be search for the positions
     * @throws EFSException
     */
    @Override
    public void gatherBOSecuritiesIDs(final JDate today) throws EFSException {

        List<Integer> secsIds = new ArrayList<Integer>();
        Map<Integer, ? extends Product> productsMap = null;

        try {

            secsIds = SantReportingUtil.getSantReportingService(DSConnection.getDefault())
                    .getSecuritiesIDsFromPositionsZeroFiltered(today);

        } catch (RemoteException e1) {
            Log.error(this, e1); //sonar
            throw new EFSException("Not possible to read Positions from DB! \n" + e1.getLocalizedMessage(), true);
        }

        try {
            // read positions from database
            productsMap = DSConnection.getDefault().getRemoteProduct().getProducts(new HashSet<Integer>(secsIds));

        } catch (RemoteException e) {
            Log.error(this, e); //sonar
            throw new EFSException("Not possible to read Positions from DB! \n" + e.getLocalizedMessage(), true);
        }

        // save it locally
        setSecuritiesMap(productsMap);
    }

    /**
     * @return true if securities have been processed and the quotes isin and ccy have been extracted; false if the
     *         this.securitiesMap was empty (gatherBOSecuritiesIDs have to be called first).
     *
     */
    @Override
    public boolean retrieveISINsFromSecuritiesMap() {

        if ((this.securitiesMap == null) || this.securitiesMap.isEmpty()) {
            return false;
        }

        this.bondsFromPositions = new ArrayList<QuoteBeanIN>();
        this.equitiesFromPositions = new ArrayList<QuoteBeanIN>();

        for (Product p : this.securitiesMap.values()) {

            // build the quote bean
            final QuoteBeanIN quoteBean = new QuoteBeanIN();

            quoteBean.setCurrency(p.getCurrency());
            quoteBean.setISIN(p.getSecCode("ISIN"));

            if (p.getProductFamily().equals(Product.BOND)) {

                quoteBean.setType(QUOTE_TYPE.BOND);
                this.bondsFromPositions.add(quoteBean);

            } else if (p.getProductFamily().equals(Product.EQUITY)) {

                quoteBean.setType(QUOTE_TYPE.EQUITY);
                this.equitiesFromPositions.add(quoteBean);

            } else {

                Log.warn(ISINsFromBOPositions.class, "Product Family does NOT belong to security ID from SQL");
            }

        }
        // end all securities processed
        return true;
    }

    /**
     * @return the securitiesMap
     */
    public Map<Integer, ? extends Product> getSecuritiesMap() {
        return this.securitiesMap;
    }

    /**
     * @param securitiesMap
     *            the securitiesMap to set
     */
    public void setSecuritiesMap(Map<Integer, ? extends Product> securitiesMap) {
        this.securitiesMap = securitiesMap;
    }

    /**
     * From the list of bonds and equities that are relevant, this method updates the context with the final
     * subscriptions list of type QuoteBeanIN. After updating the context
     *
     * @throws EFSException
     */
    @Override
    public void updateContextSubscriptionList() throws EFSException {

        if (this.context == null) {
            throw new EFSException("The context has not been initialized. Thread stopped", true);
        }

        // copy bonds to context
        if (this.bondsFromPositions != null) {
            this.context.setBondsQuotes(this.bondsFromPositions);
        }

        // copy equities to context
        if (this.equitiesFromPositions != null) {
            this.context.setEquitiesQuotes(this.equitiesFromPositions);
        }

        // erase the local copy
        this.bondsFromPositions = this.equitiesFromPositions = null;
    }

    /**
     * @return status of the thread. Number of executions and timers
     */
    @Override
    public String getStatusInfo() {

        StringBuffer sb = new StringBuffer("Thread 2: Read Positions\n");
        sb.append("     ");
        sb.append("Processing time read positions = ");
        sb.append(super.getTimeHoursMinutesSecondsString(getRunningTime())).append("  elapsed time \n");
        // sb.append(String.format("%.2g", getRunningTime() / 60.0)).append(" min\n");
        sb.append("     ");
        sb.append("Number of executions read positions = ");
        sb.append(super.executionsCounter()).append(" times \n");
        sb.append("----------------------------------------------\n");

        return sb.toString();
    }

    /***************************************************************************************************************/

    /*
     * where clause to retrieve the bonds and equities positions that are different from zero. It checks all the
     * positions that have a total columns different than zero
     */
    public StringBuffer inventorySqlWhereClause(final JDate today) {

        final StringBuffer where = new StringBuffer();

        where.append(" INV_SECPOSITION.internal_external = 'INTERNAL' ");
        where.append(" AND INV_SECPOSITION.date_type = 'TRADE' ");
        where.append(" AND INV_SECPOSITION.position_type = 'THEORETICAL'");
        where.append(" AND INV_SECPOSITION.security_id = product_desc.product_id");
        // here to select bond and equities
        where.append(" AND (PRODUCT_DESC.product_family = 'Bond' OR PRODUCT_DESC.product_family = 'Equity'");
        where.append(" OR PRODUCT_DESC.product_type = 'Bond' OR PRODUCT_DESC.product_type = 'Equity')");
        // where.append(" AND (PRODUCT_DESC.product_type = 'Bond' OR PRODUCT_DESC.product_type = 'Equity')");
        // to return the ones that have a position different to ZERO
        // Cedric explained that all totals should be checked
        // where.append(" AND INV_SECPOSITION.TOTAL_SECURITY <> 0");
        where.append("AND (INV_SECPOSITION.TOTAL_SECURITY <> 0 OR TOTAL_BORROWED <> 0 OR TOTAL_BORROWED_CA <> 0 ");
        where.append("OR  TOTAL_COLL_IN <> 0 OR  TOTAL_COLL_IN_CA <> 0 OR TOTAL_COLL_OUT <> 0 OR TOTAL_COLL_OUT_CA <> 0 ");
        where.append("OR TOTAL_LOANED <> 0 OR TOTAL_LOANED_CA <> 0 OR TOTAL_PLEDGED_IN <> 0 OR TOTAL_PLEDGED_OUT <> 0)");

        if (today != null) {
            where.append(" AND inv_secposition.position_date = ");
            where.append(" (");// BEGIN SELECT
            where.append(" select MAX(temp.position_date) from inv_secposition temp ");
            where.append(" WHERE INV_SECPOSITION.internal_external = temp.internal_external ");
            where.append(" AND INV_SECPOSITION.date_type = temp.date_type ");
            where.append(" AND INV_SECPOSITION.position_type = temp.position_type ");
            where.append(" AND INV_SECPOSITION.account_id = temp.account_id ");
            where.append(" AND INV_SECPOSITION.security_id = temp.security_id ");
            where.append(" AND INV_SECPOSITION.agent_id = temp.agent_id ");
            where.append(" AND INV_SECPOSITION.book_id = temp.book_id ");
            where.append(" AND TRUNC(temp.position_date) <= ").append(Util.date2SQLString(today));
            where.append(" )");// END SELECT
        }
        return where;
    }

    /*
     * from clause to retrieve the bonds and equities positions. In this case, only required the PRODUCT_DESC from
     * table, as INV_SECPOSITION is implicit on the calypso's call.
     */
    private StringBuffer inventorySqlFromClause() {

        return new StringBuffer("PRODUCT_DESC");
    }

}
