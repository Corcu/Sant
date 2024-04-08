package calypsox.tk.collateral.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.MarginCallEntryService;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.collateral.service.dashboard.impl.RMIDashboardServer;
import com.calypso.tk.collateral.service.impl.DefaultMarginCallEntryService;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.PersistenceException;
import com.calypso.tk.core.PricerMeasure;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.core.sql.DeadLockException;
import com.calypso.tk.core.sql.JResultSet;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.marketdata.CreditRating;
import com.calypso.tk.marketdata.MarginCallCreditRating;
import com.calypso.tk.marketdata.MarginCallCreditRatingConfiguration;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.sql.PLMarkSQL;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.GlobalRating;
import com.calypso.tk.refdata.GlobalRatingConfiguration;
import com.calypso.tk.refdata.GlobalRatingValue;
import com.calypso.tk.refdata.Group;
import com.calypso.tk.refdata.OptimizationConfiguration;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.refdata.User;
import com.calypso.tk.refdata.optimization.impl.Category;
import com.calypso.tk.refdata.sql.UserAccessPermissionSQL;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.io.CollateralSerializationUtil;

import calypsox.tk.collateral.service.LocalSantReportingService;
import calypsox.tk.collateral.service.RemoteSantReportingService;
import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.core.MarginCallConfigLight;
import calypsox.tk.report.KPIMtmIndividualItem;
import calypsox.tk.report.SantHaircutByIssuerItem;
import calypsox.tk.report.SantMTMAuditItem;
import calypsox.tk.report.SantNoMTMVariationItem;
import calypsox.tk.report.SantOptimumReportItem;
import calypsox.tk.report.SantTradesPotentialMtmErrorItem;
import calypsox.tk.report.agrexposure.SantMCDetailEntryLight;
import calypsox.tk.report.generic.SantGenericKPIMtmReport;
import calypsox.tk.util.bean.FeedFileInfoBean;
import calypsox.util.collateral.CollateralManagerUtil;
import calypsox.util.collateral.CollateralUtilities;

@Stateless(name = "calypsox.tk.collateral.service.RemoteSantReportingService")
@Remote(RemoteSantReportingService.class)
@Local(LocalSantReportingService.class)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class SantReportingService implements RemoteSantReportingService, LocalSantReportingService {

    private static Map<String, List<Integer>> sDFilterProdIdsCache = null;

    @Override
    public void buildAndCacheFilterProdIds(OptimizationConfiguration OptimConfig, List<String> contractTypes,
                                           List<Integer> contractIds) throws RemoteException {
        Map<String, List<Integer>> sdFilterProdIdsMap = buildSDFilterProdIdsMap(OptimConfig, contractTypes,
                contractIds);
        synchronized (this) {
            sDFilterProdIdsCache = sdFilterProdIdsMap;
        }
    }

    @Override
    public Map<String, List<Integer>> getSDFilterProdIdsCache(OptimizationConfiguration OptimConfig,
                                                              List<String> contractTypes, List<Integer> contractIds) throws RemoteException {
        return sDFilterProdIdsCache;
    }

    @Override
    public Map<String, List<Integer>> buildSDFilterProdIdsMap(OptimizationConfiguration OptimConfig,
                                                              List<String> contractTypes, List<Integer> contractIds) throws RemoteException {

        Map<String, List<Integer>> map = new HashMap<String, List<Integer>>();

        // 1. get Filters from contracts
        Set<StaticDataFilter> eligibleSecurityFilters = getEligibleSecurityFilters(contractTypes, contractIds);

        List<Category> categories = OptimConfig.getTarget().getCategories();
        for (Category category : categories) {
            if ("Security".equals(category.getType()) && (category.getSecurityFilter() != null)) {
                eligibleSecurityFilters.add(category.getSecurityFilter());
            }
        }

        // System.out.println("Start - Loading products - " +
        // System.currentTimeMillis());
        // 2. get all products
        Vector<Product> allProducts = loadProducts(DSConnection.getDefault());

        // Build Map
        for (StaticDataFilter filter : eligibleSecurityFilters) {
            List<Integer> productIdsList = new ArrayList<Integer>();
            for (Product product : allProducts) {
                if (filter.accept(null, product)) {
                    productIdsList.add(product.getId());
                }
            }

            map.put(filter.getName(), productIdsList);
        }

        // System.out.println("Built a map - " + System.currentTimeMillis());

        return map;
    }

    @SuppressWarnings("unchecked")
    private Vector<Product> loadProducts(DSConnection ds) throws RemoteException {
        Vector<Product> products = ds.getRemoteProduct().getProducts("Bond", null, null, true, null);
        Vector<Product> equities = ds.getRemoteProduct().getProducts("Equity", null, null, true, null);
        products.addAll(equities);
        return products;
    }

    private Set<StaticDataFilter> getEligibleSecurityFilters(List<String> contractTypes, List<Integer> contractIds)
            throws RemoteException {
        Set<StaticDataFilter> sdFilters = new HashSet<>();

        MarginCallConfigFilter mcFilter = new MarginCallConfigFilter();
        if (!Util.isEmpty(contractIds)) {
            mcFilter.setContractIds(contractIds);
        }
        if (!Util.isEmpty(contractTypes)) {
            mcFilter.setContractTypes(contractTypes);
        }
        List<CollateralConfig> contractsList = CollateralManagerUtil.loadCollateralConfigs(mcFilter);

        // 2. Build a map
        for (CollateralConfig contract : contractsList) {
            sdFilters.addAll(contract.getEligibilityFilters());
        }

        return sdFilters;
    }

    @SuppressWarnings("unused")
    private List<Integer> getProductIdsList(StaticDataFilter securityFilter) throws RemoteException {
        List<Integer> prodIdsList = new ArrayList<>();

        List<StaticDataFilter> sdfList = new ArrayList<>();
        sdfList.add(securityFilter);
        List<Product> filterProducts = ServiceRegistry.getDefault().getCollateralDataServer().findEligibleProducts(null, sdfList, null);
        if (!Util.isEmpty(filterProducts)) {
            for (Product product : filterProducts) {
                prodIdsList.add(product.getId());
            }
        }

        return prodIdsList;
    }

    @Override
    public Map<CollateralConfig, List<SantOptimumReportItem>> getOptimReportItems(List<Integer> contractIds,
                                                                                  List<String> contractTypes, JDate processDate) throws RemoteException {
        // 1. Load contracts
        MarginCallConfigFilter mcFilter = new MarginCallConfigFilter();

        if (!Util.isEmpty(contractIds)) {
            mcFilter.setContractIds(contractIds);
        }
        if (!Util.isEmpty(contractTypes)) {
            mcFilter.setContractTypes(contractTypes);
        }

        List<CollateralConfig> contractsList = CollateralManagerUtil.loadCollateralConfigs(mcFilter);

        // 2. Build a map
        Map<Integer, CollateralConfig> contractsMap = new HashMap<Integer, CollateralConfig>();
        for (CollateralConfig contract : contractsList) {
            contractsMap.put(contract.getId(), contract);
        }

        Map<CollateralConfig, List<SantOptimumReportItem>> finalMap = new HashMap<CollateralConfig, List<SantOptimumReportItem>>();

        if (!Util.isEmpty(contractsList)) {
            Connection con = null;
            Statement stmt = null;
            JResultSet rs = null;
            try {
                // 3. Load Allocation Info
                List<List<Integer>> splitCollection = CollateralUtilities.splitCollection(contractsMap.keySet(), 999);

                con = ioSQL.getConnection();
                stmt = ioSQL.newStatement(con);
                int fieldIndex = 1;

                for (List<Integer> subListIds : splitCollection) {
                    if (Util.isEmpty(subListIds)) {
                        continue;
                    }
                    String sql = "SELECT margin_call_allocation.mcc_id, margin_call_allocation.contract_value, margin_call_allocation.category, margin_call_allocation.optimization_category, margin_call_allocation.product_id, margin_call_allocation.currency "
                            + " FROM margin_call_entries,margin_call_allocation"
                            + " WHERE margin_call_allocation.mc_entry_id = margin_call_entries.id and margin_call_entries.process_date= "
                            + Util.date2SQLString(processDate) + "AND margin_call_entries.mcc_id in "
                            + Util.collectionToSQLString(subListIds);
                    rs = new JResultSet(stmt.executeQuery(sql));

                    while (rs.next()) {
                        fieldIndex = 1;
                        SantOptimumReportItem item = new SantOptimumReportItem();
                        int contractId = rs.getInt(fieldIndex++);
                        CollateralConfig contract = contractsMap.get(contractId);

                        List<SantOptimumReportItem> contractOptimItems = finalMap.get(contract);

                        if (contractOptimItems == null) {
                            contractOptimItems = new ArrayList<SantOptimumReportItem>();
                            finalMap.put(contract, contractOptimItems);
                        }

                        item.setCollateralConfig(contract);
                        item.setContractValue(rs.getDouble(fieldIndex++));
                        item.setContractCategory(rs.getString(fieldIndex++));
                        item.setOptCategoryName(rs.getString(fieldIndex++));
                        item.setProductId(rs.getInt(fieldIndex++));
                        item.setAllocationCurrency(rs.getString(fieldIndex++));

                        contractOptimItems.add(item);
                    }
                }
            } catch (final Exception ex) {
                Log.error(this, ex);
                throw new RemoteException("SQL error: " + ex.getMessage(), ex);
            } finally {
                ioSQL.close(stmt);
                ioSQL.releaseConnection(con);
            }
        }
        // Add contract with no allocations to the Map
        Set<CollateralConfig> existingContracts = finalMap.keySet();
        contractsList.removeAll(existingContracts);
        for (CollateralConfig contract : contractsList) {
            finalMap.put(contract, null); // new contract
        }

        return finalMap;
    }

    @Override
    public MarginCallCreditRatingConfiguration getLatestMarginCallRatings(int ratingConfigId, JDate date)
            throws RemoteException {

        MarginCallCreditRatingConfiguration marginCallCreditRatingConfig = ServiceRegistry.getDefault()
                .getCollateralServer().getMarginCallCreditRatingById(ratingConfigId);

        if (marginCallCreditRatingConfig == null) {
            return null;
        }

        List<MarginCallCreditRating> finalRatings = new ArrayList<MarginCallCreditRating>();

        GlobalRatingConfiguration globalRatingConfig = ServiceRegistry.getDefault().getCollateralDataServer()
                .loadDefaultGlobalRatingConfiguration();

        List<GlobalRating> spGlobalRatings = globalRatingConfig.getGlobalRating(CreditRating.CURRENT,
                CollateralStaticAttributes.MOODY, CreditRating.ANY);
        List<GlobalRatingValue> ratingValues = null;
        if (!Util.isEmpty(spGlobalRatings)) {
            GlobalRating spRating = spGlobalRatings.get(0);
            if (spRating != null) {
                ratingValues = spRating.getRatingValues();
                // ratingValues.get(0).getPriority();
            }
        }

        if (ratingValues != null) {
            MarginCallCreditRating mcCreditrating = new MarginCallCreditRating();
            mcCreditrating.setMarginCallCreditRatingId(ratingConfigId);
            for (GlobalRatingValue ratingValue : ratingValues) {
                mcCreditrating.setPriority(ratingValue.getPriority());
                MarginCallCreditRating mcRatingLine = ServiceRegistry.getDefault().getCollateralServer()
                        .getLatestMarginCallCreditRating(mcCreditrating, date);
                if (mcRatingLine != null) {
                    finalRatings.add(mcRatingLine);
                }
            }
        }

        marginCallCreditRatingConfig.setRatings(new Vector<MarginCallCreditRating>(finalRatings));
        return marginCallCreditRatingConfig;
    }

    // @SuppressWarnings({ "unchecked", "unused" })
    // @Override
    // public List<MarginCallCreditRating> getLatestMarginCallRatings(int
    // ratingConfigId, JDate date)
    // throws RemoteException {
    //
    // List<MarginCallCreditRating> finalRatings = new
    // ArrayList<MarginCallCreditRating>();
    // GlobalRatingConfiguration globalRatingConfig =
    // ServiceRegistry.getDefault().getCollateralDataServer()
    // .loadDefaultGlobalRatingConfiguration();
    //
    // // List<GlobalRating> spGlobalRatings =
    // globalRatingConfig.getGlobalRating(CreditRating.CURRENT,
    // // CollateralStaticAttributes.MOODY, CreditRating.ANY);
    //
    // // Load ratings config
    // MarginCallCreditRatingConfiguration mccRatingConfig =
    // ServiceRegistry.getDefault().getCollateralServer()
    // .getMarginCallCreditRatingById(ratingConfigId);
    // List<MarginCallCreditRating> ratings = mccRatingConfig.getRatings();
    // MarginCallCreditRatingComparator compByPriority = new
    // MarginCallCreditRatingComparator();
    // compByPriority.setDescending(false);
    // Collections.sort(ratings, compByPriority);
    //
    // // compare Date now
    // MarginCallCreditRating prevRating = null;
    // for (MarginCallCreditRating rating : ratings) {
    //
    // if (!date.gte(rating.getAsOfDate())) {
    // continue;
    // }
    //
    // if (prevRating == null) {
    // if (date.gte(rating.getAsOfDate())) {
    // prevRating = rating;
    // }
    // } else {
    // if (prevRating.getPriority() == rating.getPriority()) {
    // // we have two rating with same priority and valid as of date. Pick the
    // max
    // if (rating.getAsOfDate().after(prevRating.getAsOfDate())) {
    // prevRating = rating;
    // }
    // continue;
    // }
    //
    // addMCRatingIfApplicable(finalRatings, prevRating, date);
    // prevRating = rating;
    // }
    // }
    // // Add the last one
    // addMCRatingIfApplicable(finalRatings, prevRating, date);
    //
    // return finalRatings;
    // }

    // private void addMCRatingIfApplicable(List<MarginCallCreditRating>
    // finalRatings, MarginCallCreditRating rating,
    // JDate date) {
    // if ((finalRatings != null) && rating.getAsOfDate().gte(date)) {
    // finalRatings.add(rating);
    // }
    // }

    @Override
    public List<SantTradesPotentialMtmErrorItem> getPotentialMtmErrorItems(String sql) throws RemoteException {
        List<SantTradesPotentialMtmErrorItem> items = new ArrayList<SantTradesPotentialMtmErrorItem>();
        Connection con = null;
        Statement stmt = null;

        try {
            con = ioSQL.getConnection();
            stmt = ioSQL.newStatement(con);
            final JResultSet rs = new JResultSet(stmt.executeQuery(sql));
            SantTradesPotentialMtmErrorItem item = null;
            int fieldIndex = 1;

            while (rs.next()) {
                fieldIndex = 1;
                item = new SantTradesPotentialMtmErrorItem();

                item.setTrade_id(rs.getInt(fieldIndex++));
                item.setMccId(rs.getInt(fieldIndex++));
                item.setBoRef(rs.getString(fieldIndex++));
                item.setExtRef(rs.getString(fieldIndex++));
                item.setMccDesc(rs.getString(fieldIndex++));
                item.setOwner(rs.getString(fieldIndex++));
                item.setProductType(rs.getString(fieldIndex++));
                item.setProductSubType(rs.getString(fieldIndex++));
                item.setMaturityDate(rs.getJDate(fieldIndex++));
                item.setStructureId(rs.getString(fieldIndex++));
                item.setMtmPrevious(rs.getDouble(fieldIndex++));
                items.add(item);
            }

        } catch (final Exception ex) {
            Log.error(this, ex);
            throw new RemoteException("SQL error: " + ex.getMessage(), ex);
        } finally {
            ioSQL.close(stmt);
            ioSQL.releaseConnection(con);
        }
        return items;
    }

    /**
     * This method finds all the eligible contracts for the given Trade.
     *
     * @param trade
     * @return ArrayList of CollateralConfig objects
     * @throws RemoteException
     */
    @Override
    public ArrayList<CollateralConfig> getEligibleMarginCallConfigs(final Trade trade) throws RemoteException {
        if (trade == null) {
            return null;
        }

        final ArrayList<CollateralConfig> eligibleContracts = new ArrayList<CollateralConfig>();

        final int po_id = trade.getBook().getLegalEntity().getId();
        final int le_id = trade.getCounterParty().getId();

        final String query = "select distinct mcle1.mcc_id from mrgcall_config_le mcle1, mrgcall_config_le mcle2, MRGCALL_CONFIG mc "
                + " where mcle1.mcc_id=mcle2.mcc_id and mc.mrg_call_def=mcle1.mcc_id " + "AND ( (mcle1.le_id = " + po_id
                + " and mcle1.le_role='ProcessingOrg') or mc.PROCESS_ORG_ID=" + po_id + ") " + "and ( mcle2.le_id="
                + le_id + " OR mc.LEGAL_ENTITY_ID= " + le_id + ")" + " union " + " select mrg_call_def  "
                + " from MRGCALL_CONFIG mc " + " where mc.PROCESS_ORG_ID=" + po_id + " and LEGAL_ENTITY_ID=" + le_id;

        Connection con = null;
        Statement stmt = null;

        try {
            con = ioSQL.getConnection();
            stmt = ioSQL.newStatement(con);
            final JResultSet rs = new JResultSet(stmt.executeQuery(query));
            while (rs.next()) {
                final int mccID = rs.getInt(1);

                CollateralConfig marginCallConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
                        mccID);
                // final CollateralConfig marginCallConfig =
                // MarginCallConfigSQL.get(mccID, con);
                if ((!marginCallConfig.getAgreementStatus().equals("CLOSED"))
                        && marginCallConfig.accept(trade, DSConnection.getDefault())) {
                    eligibleContracts.add(marginCallConfig);
                }
            }

        } catch (final Exception ex) {
            Log.error(this, ex);
            throw new RemoteException("SQL error: " + ex.getMessage(), ex);
        } finally {
            ioSQL.close(stmt);
            ioSQL.releaseConnection(con);
        }
        return eligibleContracts;
    }

    // GSM: 17/07/2013. Retrieve contract without requiring the book inside the
    // trade. Required online DFA

    /**
     * This method finds all the eligible contracts for the given Trade.
     *
     * @param po    id
     * @param trade
     * @return ArrayList of CollateralConfig objects
     * @throws RemoteException
     */
    @Override
    public ArrayList<CollateralConfig> getEligibleCollateralConfigs(final Trade trade, final int po_id)
            throws RemoteException {
        if (trade == null) {
            return null;
        }

        final ArrayList<CollateralConfig> eligibleContracts = new ArrayList<CollateralConfig>();

        // final int po_id = //trade.getBook().getLegalEntity().getId();
        if (trade == null || trade.getCounterParty() == null) {
        	Log.error(this, "getEligibleCollateralConfigs ERROR : Trade or Counterparty is null.");
        	Log.error(this, "Trade : " + trade.toString());
        	
        	return eligibleContracts;
        }
        
        final int le_id = trade.getCounterParty().getId();

        final String query = "select distinct mcle1.mcc_id from mrgcall_config_le mcle1, mrgcall_config_le mcle2, MRGCALL_CONFIG mc "
                + " where mcle1.mcc_id=mcle2.mcc_id and mc.mrg_call_def=mcle1.mcc_id " + "AND ( (mcle1.le_id = " + po_id
                + " and mcle1.le_role='ProcessingOrg') or mc.PROCESS_ORG_ID=" + po_id + ") " + "and ( mcle2.le_id="
                + le_id + " OR mc.LEGAL_ENTITY_ID= " + le_id + ")" + " union " + " select mrg_call_def  "
                + " from MRGCALL_CONFIG mc " + " where mc.PROCESS_ORG_ID=" + po_id + " and LEGAL_ENTITY_ID=" + le_id;

        Connection con = null;
        Statement stmt = null;

        try {
            con = ioSQL.getConnection();
            stmt = ioSQL.newStatement(con);
            final JResultSet rs = new JResultSet(stmt.executeQuery(query));
            while (rs.next()) {
                final int mccID = rs.getInt(1);

                CollateralConfig marginCallConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
                        mccID);

                // to avoid null pointer if book is empty
                trade.setBook(marginCallConfig.getBook());

                // final CollateralConfig marginCallConfig =
                // MarginCallConfigSQL.get(mccID, con);
                if ((!marginCallConfig.getAgreementStatus().equals("CLOSED"))
                        && marginCallConfig.accept(trade, DSConnection.getDefault())) {
                    eligibleContracts.add(marginCallConfig);
                }
            }

        } catch (final Exception ex) {
            Log.error(this, ex);
            throw new RemoteException("SQL error: " + ex.getMessage(), ex);
        } finally {
            ioSQL.close(stmt);
            ioSQL.releaseConnection(con);
        }
        return eligibleContracts;
    }

    /*
     * (non-Javadoc)
     *
     * @see calypsox.tk.collateral.service.RemoteSantReportingService#
     * getMarginCallConfigsFromTrade(com.calypso.tk.core.Trade )
     */
    @Override
    public ArrayList<Integer> getMarginCallConfigsFromTrade(Trade trade) throws RemoteException {
        if (trade == null) {
            return null;
        }

        ArrayList<Integer> eligibleContracts = new ArrayList<Integer>();

        int po_id = trade.getBook().getLegalEntity().getId();
        int le_id = trade.getCounterParty().getId();

        String query = "select distinct mcle1.mcc_id from mrgcall_config_le mcle1, mrgcall_config_le mcle2, MRGCALL_CONFIG mc "
                + " where mcle1.mcc_id=mcle2.mcc_id and mc.mrg_call_def=mcle1.mcc_id " + "AND ( (mcle1.le_id = " + po_id
                + " and mcle1.le_role='ProcessingOrg') or mc.PROCESS_ORG_ID=" + po_id + ") " + "and ( mcle2.le_id="
                + le_id + " OR mc.LEGAL_ENTITY_ID= " + le_id + ")" + " union " + " select mrg_call_def  "
                + " from MRGCALL_CONFIG mc " + " where mc.PROCESS_ORG_ID=" + po_id + " and LEGAL_ENTITY_ID=" + le_id;

        Connection con = null;
        Statement stmt = null;

        try {
            con = ioSQL.getConnection();
            stmt = ioSQL.newStatement(con);
            JResultSet rs = new JResultSet(stmt.executeQuery(query));
            while (rs.next()) {
                int mccID = rs.getInt(1);
                eligibleContracts.add(new Integer(mccID));
            }

        } catch (Exception ex) {
            Log.error(this, ex);
            throw new RemoteException("SQL error: " + ex.getMessage(), ex);
        } finally {
            ioSQL.close(stmt);
            ioSQL.releaseConnection(con);
        }
        return eligibleContracts;
    }

    @Override
    public List<Map<Integer, JDate>> getNotCalculatedMarginCallConfigs(final String from, final String where)
            throws RemoteException {

        final String query = "SELECT mrg_call_def, process_date FROM " + from + " WHERE " + where
                + " ORDER BY process_date";

        System.out.println("query = " + query);
        Connection con = null;
        Statement stmt = null;
        final List<Map<Integer, JDate>> result = new ArrayList<Map<Integer, JDate>>();

        try {
            con = ioSQL.getConnection();
            stmt = ioSQL.newStatement(con);
            final JResultSet rs = new JResultSet(stmt.executeQuery(query));
            while (rs.next()) {

                final int mccID = rs.getInt(1);
                final JDate processDate = rs.getJDate(2);
                final Map<Integer, JDate> element = new HashMap<Integer, JDate>();
                element.put(mccID, processDate);
                result.add(element);
            }

        } catch (final Exception ex) {
            Log.error(this, ex);
            throw new RemoteException("SQL error: " + ex.getMessage(), ex);
        } finally {
            ioSQL.close(stmt);
            ioSQL.releaseConnection(con);
        }
        return result;
    }

    @Override
    public ArrayList<SantMTMAuditItem> getMTMAuditItems(final String sqlQuery, final boolean loadContract)
            throws RemoteException {
        final ArrayList<SantMTMAuditItem> mtmAuditItems = new ArrayList<SantMTMAuditItem>();

        Connection con = null;
        Statement stmt = null;

        try {
            con = ioSQL.getConnection();
            stmt = ioSQL.newStatement(con);
            final JResultSet rs = new JResultSet(stmt.executeQuery(sqlQuery));
            final HashMap<Long, Trade> tradeMap = new HashMap<>();
            int fieldIndex = 1;

            while (rs.next()) {
                // select trade.trade_id, bo_audit.modif_date,
                // bo_audit.OLD_VALUE, bo_audit.NEW_VALUE, bo_audit.USER_NAME,
                // pl_mark.VALUATION_DATE
                fieldIndex = 1;
                final SantMTMAuditItem mtmAuditItem = new SantMTMAuditItem();

                final int trade_id = rs.getInt(fieldIndex++);
                // check if the trade is in the map otherwise retrieve it
                if (tradeMap.get(trade_id) != null) {
                    mtmAuditItem.setTrade(tradeMap.get(trade_id));
                } else {
                    final Trade trade = DSConnection.getDefault().getRemoteTrade().getTrade(trade_id);
                    mtmAuditItem.setTrade(trade);
                    tradeMap.put(trade.getLongId(), trade);
                }

                if (loadContract && (mtmAuditItem.getTrade() != null)) {
                    final int contractId = mtmAuditItem.getTrade()
                            .getKeywordAsInt(CollateralStaticAttributes.MC_CONTRACT_NUMBER);

                    if (contractId != 0) {
                        final CollateralConfig marginCallConfig = CacheCollateralClient
                                .getCollateralConfig(DSConnection.getDefault(), contractId);
                        mtmAuditItem.setMarginCallConfig(marginCallConfig);
                    }
                }

                mtmAuditItem.setModifDate(rs.getJDatetime(fieldIndex++).toString());
                mtmAuditItem.setOldAuditLine(rs.getString(fieldIndex++));
                mtmAuditItem.setNewAuditLine(rs.getString(fieldIndex++));
                mtmAuditItem.setUserChanged(rs.getString(fieldIndex++));
                mtmAuditItem.setMtmValDate(rs.getJDate(fieldIndex++).toString());

                // Load PLMark
                final int markId = rs.getInt(fieldIndex++);
                final PLMark plMark = PLMarkSQL.get(markId);
                mtmAuditItem.setPlMark(plMark);

                mtmAuditItems.add(mtmAuditItem);
            }

        } catch (final Exception ex) {
            Log.error(this, ex);
            throw new RemoteException("SQL error: " + ex.getMessage(), ex);
        } finally {
            ioSQL.close(stmt);
            ioSQL.releaseConnection(con);
        }

        return mtmAuditItems;
    }

    @Override
    public ArrayList<SantNoMTMVariationItem> getNoMTMVariationItems(final String sqlQuery) throws RemoteException {
        final ArrayList<SantNoMTMVariationItem> mtmItems = new ArrayList<SantNoMTMVariationItem>();

        Connection con = null;
        Statement stmt = null;

        try {
            con = ioSQL.getConnection();
            stmt = ioSQL.newStatement(con);
            final JResultSet rs = new JResultSet(stmt.executeQuery(sqlQuery));
            final Map<Long, Trade> tradeMap = new HashMap<>();
            int fieldIndex = 1;

            while (rs.next()) {
                // select trade.trade_id, pl_mark_value.currency,
                // pl_mark_value.mark_value
                fieldIndex = 1;
                final SantNoMTMVariationItem mtmItem = new SantNoMTMVariationItem();

                final int trade_id = rs.getInt(fieldIndex++);
                // check if the trade is in the map otherwise retrieve it
                if (tradeMap.get(trade_id) != null) {
                    mtmItem.setTrade(tradeMap.get(trade_id));
                } else {
                    final Trade trade = DSConnection.getDefault().getRemoteTrade().getTrade(trade_id);
                    mtmItem.setTrade(trade);
                    tradeMap.put(trade.getLongId(), trade);
                }

                mtmItem.setMarkName(rs.getString(fieldIndex++));
                mtmItem.setMarkCcy(rs.getString(fieldIndex++));
                mtmItem.setMarkValue(rs.getDouble(fieldIndex++));

                mtmItems.add(mtmItem);
            }

        } catch (final Exception ex) {
            Log.error(this, ex);
            throw new RemoteException("SQL error: " + ex.getMessage(), ex);
        } finally {
            ioSQL.close(stmt);
            ioSQL.releaseConnection(con);
        }

        return mtmItems;
    }

    @Override
    public boolean setFeedFileInfoData(final FeedFileInfoBean ffiBean) throws DeadLockException, RemoteException {

        System.out.println("->getFeedFileInfoData");
        Connection con = null;

        PreparedStatement stmt = null;
        int i = 1;

        try {
            con = ioSQL.getConnection();

            final String INSERTSQL = "INSERT INTO san_feed_file_info(process,processing_org,start_time,end_time,"
                    + "process_date,file_imported,inout,result,number_ok,number_warning,number_error,"
                    + "original_file,comments) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";

            stmt = ioSQL.newPreparedStatement(con, INSERTSQL);

            // process
            stmt.setString(i++, ffiBean.getProcess());
            // processing org
            stmt.setInt(i++, ffiBean.getProcessingOrg());
            // start time
            stmt.setTimestamp(i++, ffiBean.getStartTime());
            // end time
            if (ffiBean.getEndTime() != null) {
                stmt.setTimestamp(i++, ffiBean.getEndTime());
            } else {
                stmt.setNull(i++, Types.TIMESTAMP);
            }
            // process date
            if (ffiBean.getProcessDate() != null) {
                stmt.setDate(i++, (Date) ffiBean.getProcessDate());
            } else {
                stmt.setNull(i++, Types.DATE);
            }
            // file imported
            if (ffiBean.getFileImported() != null) {
                stmt.setString(i++, ffiBean.getFileImported());
            } else {
                stmt.setNull(i++, Types.VARCHAR);
            }
            // inout
            if (ffiBean.getInout() != null) {
                stmt.setString(i++, ffiBean.getInout());
            } else {
                stmt.setNull(i++, Types.VARCHAR);
            }
            // result
            if (ffiBean.getResult() != null) {
                stmt.setString(i++, ffiBean.getResult());
            } else {
                stmt.setNull(i++, Types.VARCHAR);
            }
            // number ok
            if (ffiBean.getNumberOk() != null) {
                stmt.setInt(i++, ffiBean.getNumberOk());
            } else {
                stmt.setNull(i++, Types.INTEGER);
            }
            // number warning
            if (ffiBean.getNumberWarning() != null) {
                stmt.setInt(i++, ffiBean.getNumberWarning());
            } else {
                stmt.setNull(i++, Types.INTEGER);
            }
            // number error
            if (ffiBean.getNumberError() != null) {
                stmt.setInt(i++, ffiBean.getNumberError());
            } else {
                stmt.setNull(i++, Types.INTEGER);
            }
            // original file
            if (ffiBean.getOriginalFile() != null) {
                stmt.setString(i++, ffiBean.getOriginalFile());
            } else {
                stmt.setNull(i++, Types.VARCHAR);
            }
            // comments
            if (ffiBean.getComments() != null) {
                stmt.setString(i++, ffiBean.getComments());
            } else {
                stmt.setNull(i++, Types.VARCHAR);
            }
            // execute query (do the insert)
            stmt.executeUpdate();
            ioSQL.commit(con);

        } catch (final DeadLockException e) {
            throw e;
        } catch (final Exception e) {
            Log.error(this, e);
            ioSQL.rollback(con);
            throw new RemoteException("SQL error: " + e.getMessage(), e);
        } finally {
            ioSQL.close(stmt);
            ioSQL.releaseConnection(con);
            System.out.println("FIN\n");
        }

        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<FeedFileInfoBean> getFeedFileInfoData(final String sqlQuery) throws RemoteException {

        Connection con = null;
        Statement stmt = null;
        final ArrayList<FeedFileInfoBean> ffiBeans = new ArrayList<FeedFileInfoBean>();

        System.out.println("->setFeedFileInfoData");

        try {
            con = ioSQL.newConnection();
            stmt = ioSQL.newStatement(con);
            final ResultSet rs = stmt.executeQuery(sqlQuery);
            int fieldIndex = 1;

            while (rs.next()) {
                fieldIndex = 1;
                final FeedFileInfoBean ffiBean = new FeedFileInfoBean();
                ffiBean.setProcess(rs.getString(fieldIndex++));
                ffiBean.setProcessingOrg(rs.getInt(fieldIndex++));
                ffiBean.setStartTime(rs.getTimestamp(fieldIndex++));
                ffiBean.setEndTime(rs.getTimestamp(fieldIndex++));
                ffiBean.setProcessDate(rs.getDate(fieldIndex++));
                ffiBean.setFileImported(rs.getString(fieldIndex++));
                ffiBean.setInout(rs.getString(fieldIndex++));
                ffiBean.setResult(rs.getString(fieldIndex++));
                ffiBean.setNumberOk(rs.getInt(fieldIndex++));
                ffiBean.setNumberWarning(rs.getInt(fieldIndex++));
                ffiBean.setNumberError(rs.getInt(fieldIndex++));
                ffiBean.setOriginalFile(rs.getString(fieldIndex++));
                ffiBean.setComments(rs.getString(fieldIndex++));
                ffiBeans.add(ffiBean);
            }

        } catch (final Exception e) {
            Log.error(this, e);
            throw new RemoteException("SQL error: " + e.getMessage(), e);
        } finally {
            ioSQL.close(stmt);
            ioSQL.releaseConnection(con);
        }

        return ffiBeans;
    }

    @Override
    public Map<Integer, String> getAllMarginCallConfigIdsAndNames() throws RemoteException {

        final Map<Integer, String> results = new HashMap<Integer, String>();
        final String sqlQuery = "select MRG_CALL_DEF, DESCRIPTION from MRGCALL_CONFIG";
        Connection con = null;
        Statement stmt = null;

        try {
            con = ioSQL.getConnection();
            stmt = ioSQL.newStatement(con);
            final JResultSet rs = new JResultSet(stmt.executeQuery(sqlQuery));

            int fieldIndex = 1;
            while (rs.next()) {
                fieldIndex = 1;
                results.put(rs.getInt(fieldIndex++), rs.getString(fieldIndex++));
            }
        } catch (final Exception e) {
            Log.error(this, e);
            throw new RemoteException("SQL error: " + e.getMessage(), e);
        } finally {
            ioSQL.close(stmt);
            ioSQL.releaseConnection(con);
        }
        return results;
    }

    @Override
    public Vector<MarginCallConfigLight> getMarginCallConfigsLight() throws RemoteException {

        final Vector<MarginCallConfigLight> results = new Vector<MarginCallConfigLight>();
        final String sqlQuery = "select MRG_CALL_DEF, DESCRIPTION, CONTRACT_TYPE, PROCESS_ORG_ID from MRGCALL_CONFIG";
        Connection con = null;
        Statement stmt = null;

        try {
            con = ioSQL.getConnection();
            stmt = ioSQL.newStatement(con);
            final JResultSet rs = new JResultSet(stmt.executeQuery(sqlQuery));

            int fieldIndex = 1;
            while (rs.next()) {
                fieldIndex = 1;
                final MarginCallConfigLight mccLight = new MarginCallConfigLight();
                mccLight.setId(rs.getInt(fieldIndex++));
                mccLight.setDescription(rs.getString(fieldIndex++));
                mccLight.setContractType(rs.getString(fieldIndex++));
                mccLight.setPoId(rs.getInt(fieldIndex++));

                results.add(mccLight);
            }
        } catch (final Exception e) {
            Log.error(this, e);
            throw new RemoteException("SQL error: " + e.getMessage(), e);
        } finally {
            ioSQL.close(stmt);
            ioSQL.releaseConnection(con);
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<KPIMtmIndividualItem> getKPIMtmReportItems(final String query, final String reportType)
            throws RemoteException {
        final List<KPIMtmIndividualItem> list = new ArrayList<KPIMtmIndividualItem>();
        Connection con = null;
        Statement stmt = null;

        try {
            con = ioSQL.getConnection();
            stmt = ioSQL.newStatement(con);
            final JResultSet rs = new JResultSet(stmt.executeQuery(query));
            // MIG V14.4 AAP
            while (rs.next()) {
                int fieldIndex = 1;
                final KPIMtmIndividualItem kpiMtmItem = new KPIMtmIndividualItem();

                kpiMtmItem.setAgrOwner(rs.getString(++fieldIndex));
                kpiMtmItem.setDealOwner(rs.getString(++fieldIndex));
                kpiMtmItem.setTradeId(rs.getInt(++fieldIndex));

                if (reportType.equals(SantGenericKPIMtmReport.KPIMtmByEconomicSector)) {
                    kpiMtmItem.setEconomicSector(rs.getString(++fieldIndex));
                } else if (reportType.equals(SantGenericKPIMtmReport.KPIMtmByInstrument)) {
                    kpiMtmItem.setInstrument(rs.getString(++fieldIndex));
                } else if (reportType.equals(SantGenericKPIMtmReport.KPIMtmByPortfolios)) {
                    kpiMtmItem.setPortfolio(rs.getString(++fieldIndex));
                } else if (reportType.equals(SantGenericKPIMtmReport.KPIMtmByAgreement)) {
                    kpiMtmItem.setAgreementId(rs.getInt(++fieldIndex));
                    kpiMtmItem.setAgreementName(rs.getString(++fieldIndex));
                }

                kpiMtmItem.setMtmCurrency(rs.getString(++fieldIndex));
                // MIG_V14.4 AAP
                kpiMtmItem.setPricerMeasures((List<PricerMeasure>) CollateralSerializationUtil
                        .readObjectListWithLenght(rs.getBytes(++fieldIndex)));
                kpiMtmItem.setMcEntryId(rs.getInt(++fieldIndex));

                list.add(kpiMtmItem);
            }

        } catch (final Exception ex) {
            Log.error(this, ex);
            throw new RemoteException("SQL error: " + ex.getMessage(), ex);
        } finally {
            ioSQL.close(stmt);
            ioSQL.releaseConnection(con);
        }

        return list;
    }

    @Override
    public List<MarginCallEntryDTO> getMarginCallEntriesDTO(final String where, final List<String> from,
                                                            final boolean buildFullObject) throws RemoteException, PersistenceException {
        final RMIDashboardServer dashboard = new RMIDashboardServer();

        List<MarginCallEntryDTO> entries;
        try {
            entries = dashboard.loadMarginCallEntries(where, from);
        } catch (final RemoteException e) {
            Log.error(this, "SQL error: " + e.getMessage(), e);
            throw e;
        }

        if (!buildFullObject) {
            return entries;
        }

        // buildFullObject
        final List<MarginCallEntryDTO> fullEntries = new ArrayList<>();

        final MarginCallEntryService service = DefaultMarginCallEntryService.getInstance();
        for (final MarginCallEntryDTO entry : entries) {
            try {
                fullEntries.add(service.loadEntry(entry.getId()));
            } catch (final CollateralServiceException exc) {
                Log.error(this, "SQL error: " + exc.getMessage(), exc);
                throw exc;
            }
        }

        return fullEntries;
    }

    @Override
    public Map<Integer, CollateralConfig> getMarginCallConfigByIds(final Collection<Integer> ids)
            throws PersistenceException {
        final Map<Integer, CollateralConfig> marginCallConfigsMap = new HashMap<Integer, CollateralConfig>();

        for (final Integer id : ids) {
            try {
                final CollateralConfig mcc = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), id);
                if (mcc != null) {
                    marginCallConfigsMap.put(mcc.getId(), mcc);
                } else {
                    Log.error(SantReportingService.class.getName(), "Cannot Load Contract with Id=" + id.toString());
                }
            } catch (final Exception e) {
                Log.error(this, "SQL error: " + e.getMessage(), e);
                throw new PersistenceException(e);
            }
        }
        return marginCallConfigsMap;
    }

    @Override
    public ArrayList<Integer> getMarginCallConfigIds(String query) throws RemoteException {

        Connection con = null;
        Statement stmt = null;

        ArrayList<Integer> list = new ArrayList<Integer>();
        try {
            con = ioSQL.getConnection();
            stmt = ioSQL.newStatement(con);
            final JResultSet rs = new JResultSet(stmt.executeQuery(query));
            int fieldIndex = 1;
            while (rs.next()) {
                list.add(rs.getInt(fieldIndex));
            }

        } catch (final Exception ex) {
            Log.error(this, ex);
            throw new RemoteException("SQL error: " + ex.getMessage(), ex);
        } finally {
            ioSQL.close(stmt);
            ioSQL.releaseConnection(con);
        }
        return list;
    }

    @Override
    public ArrayList<Integer> getMarginCallConfigIds(String query, String economicSector) throws RemoteException {
        ArrayList<Integer> marginCallConfigIds = getMarginCallConfigIds(query);
        if (!Util.isEmpty(economicSector)) {
            for (int i = marginCallConfigIds.size(); i >= 0; i--) {
                CollateralConfig marginCallConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
                        marginCallConfigIds.get(i));
                if (!economicSector.equals(marginCallConfig
                        .getAdditionalField(CollateralStaticAttributes.MCC_ADD_FIELD_ECONOMIC_SECTOR))) {
                    marginCallConfigIds.remove(i);
                }
            }
        }
        return marginCallConfigIds;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ArrayList<SantMCDetailEntryLight> getDetailedEntriesLight(String sql) throws RemoteException {
        ArrayList<SantMCDetailEntryLight> list = new ArrayList<SantMCDetailEntryLight>();

        Connection con = null;
        Statement stmt = null;

        try {
            con = ioSQL.getConnection();
            stmt = ioSQL.newStatement(con);
            final JResultSet rs = new JResultSet(stmt.executeQuery(sql));

            while (rs.next()) {
                int fieldIndex = 0;
                SantMCDetailEntryLight detailEntry = new SantMCDetailEntryLight();

                detailEntry.setAgrId(rs.getInt(++fieldIndex));
                detailEntry.setAgreementName(rs.getString(++fieldIndex));
                detailEntry.setAgreementType(rs.getString(++fieldIndex));
                detailEntry.setAgreementCurrency(rs.getString(++fieldIndex));
                detailEntry.setEntryId(rs.getInt(++fieldIndex));

                detailEntry.setProcessDate(rs.getJDate(++fieldIndex));
                detailEntry.setValDate(rs.getJDate(++fieldIndex));
                detailEntry.setCounterPartyName(rs.getString(++fieldIndex));
                detailEntry.setProcessOrgName(rs.getString(++fieldIndex));

                detailEntry.setInstrument(rs.getString(++fieldIndex));
                detailEntry.setMaturityDate(rs.getJDate(++fieldIndex));
                // AAP MIG_V14.4
                detailEntry.setPricerMeasures((List<PricerMeasure>) CollateralSerializationUtil
                        .readObjectListWithLenght(rs.getBytes(++fieldIndex)));
                list.add(detailEntry);
            }

        } catch (final Exception ex) {
            Log.error(this, ex);
            throw new RemoteException("SQL error: " + ex.getMessage(), ex);
        } finally {
            ioSQL.close(stmt);
            ioSQL.releaseConnection(con);
        }

        return list;
    }

    @Override
    public Map<Integer, Integer> getTradeCountForEntries(List<Integer> mcEntryIds) throws RemoteException {
        HashMap<Integer, Integer> tradeCountMap = new HashMap<Integer, Integer>();

        Connection con = null;
        Statement stmt = null;

        try {
            con = ioSQL.getConnection();
            stmt = ioSQL.newStatement(con);

            String query = "select mc_entry_id, count(*) from margin_call_detail_entries where mc_entry_id in ";

            int listSize = mcEntryIds.size();
            int start = 0;
            int end = 0;
            while (start < listSize) {
                end = start + 999;
                if (end > listSize) {
                    end = listSize;
                }
                List<Integer> subList = mcEntryIds.subList(start, end);
                JResultSet rs = new JResultSet(
                        stmt.executeQuery(query + Util.collectionToSQLString(subList) + " group by mc_entry_id "));

                while (rs.next()) {
                    int entryId = rs.getInt(1);
                    int count = rs.getInt(2);
                    tradeCountMap.put(entryId, count);
                }

                start = start + 999;
            }
        } catch (final Exception ex) {
            Log.error(this, ex);
            throw new RemoteException("SQL error: " + ex.getMessage(), ex);
        } finally {
            ioSQL.close(stmt);
            ioSQL.releaseConnection(con);
        }

        return tradeCountMap;
    }

    /**
     * Quite a few reports useg AccessServerImpl.executeSelectSQL() which logs
     * the SQL that it is running. This method does the same thing without
     * logging SQL. This is a result of a decision to reduce the amount of
     * logging here at Santander.
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Vector executeSelectSQL(String query) throws RemoteException {
        Connection con = null;
        Statement stmt = null;
        try {
            con = ioSQL.getConnection();
            stmt = ioSQL.newStatement(con);
            JResultSet rs = new JResultSet(stmt.executeQuery(query));
            ResultSetMetaData rsmeta = rs.getResultSet().getMetaData();
            Vector results = new Vector();
            Vector names = new Vector();
            Vector types = new Vector();
            int cols = rsmeta.getColumnCount();
            for (int i = 0; i < cols; i++) {
                names.add(rsmeta.getColumnName(i + 1));
                types.add(Integer.valueOf(rsmeta.getColumnType(i + 1)));
            }
            results.add(names);
            results.add(types);
            while (rs.next()) {
                Vector row = new Vector();
                results.add(row);
                for (int i = 0; i < cols; i++) {
                    int type = rsmeta.getColumnType(i + 1);
                    switch (type) {
                        case Types.LONGVARBINARY: {
                            byte bytes[] = rs.getBytes(i + 1);
                            if (bytes != null) {
                                row.add("Blob of Size " + bytes.length + " bytes");
                            } else {
                                row.add("Empty Blob");
                            }

                        }
                        break;
                        case Types.BLOB: {
                            byte bytes[] = ioSQL.getOracleBlob(rs, i + 1);
                            if (bytes != null) {
                                row.add("Blob of Size " + bytes.length + " bytes");
                            } else {
                                row.add("Empty Blob");
                            }

                        }
                        break;
                        case Types.VARBINARY:
                            row.add("__BinaryObject__");
                            break;
                        case Types.SMALLINT:
                        case Types.TINYINT:
                        case Types.INTEGER:
                            row.add(Integer.valueOf(rs.getInt(i + 1)));
                            break;
                        case Types.FLOAT:
                        case Types.DOUBLE:
                            row.add(Double.valueOf(rs.getDouble(i + 1)));
                            break;
                        case Types.BIT:
                            row.add(Boolean.valueOf(rs.getInt(i + 1) == 1));
                            break;
                        case Types.DATE:
                            row.add(rs.getJDate(i + 1));
                            break;
                        case Types.TIMESTAMP:
                            row.add(rs.getJDatetime(i + 1));
                            break;
                        default:
                            row.add(rs.getString(i + 1));
                    }
                }
            }
            return results;
        } catch (Exception ex) {
            Log.error(this, ex);
            ioSQL.rollback(con);
            throw new RemoteException("SQL error: " + ex.getMessage(), ex);
        } catch (Error error) {
            Log.error(this, error);
            ioSQL.rollback(con);
            throw new RemoteException("Error " + error.getMessage() + " " + Log.exceptionToString(error), error);
        } finally {
            ioSQL.close(stmt);
            ioSQL.releaseConnection(con);
        }
    }

    /**
     * This methods retrieves all the products ids (bonds and equities) that
     * belongs a to position that, till day, is different from zero
     */
    @Override
    public List<Integer> getSecuritiesIDsFromPositionsZeroFiltered(final JDate fromDate) throws RemoteException {

        StringBuffer sqlBuild = new StringBuffer();
        Connection con = null;
        Statement stmt = null;
        List<Integer> idList = null;

        // SELECT DISTINCT SECURITY_ID FROM
        // (
        // SELECT SECURITY_ID, BOOK_ID,AGENT_ID,ACCOUNT_ID,
        // ROUND(SUM(DAILY_SECURITY),2) AS SUMA
        // FROM INV_SECPOSITION
        // WHERE POSITION_TYPE = 'THEORETICAL' AND DATE_TYPE = 'TRADE' AND
        // INTERNAL_EXTERNAL = 'INTERNAL'
        // AND POSITION_DATE <= to_date('2013/09/02', 'yyyy/mm/dd')
        // GROUP BY SECURITY_ID, BOOK_ID,AGENT_ID,ACCOUNT_ID
        // HAVING ROUND(SUM(DAILY_SECURITY),2) <> 0
        // )

        // build query
        sqlBuild.append("SELECT DISTINCT SECURITY_ID FROM ");
        sqlBuild.append("( ");
        sqlBuild.append("SELECT SECURITY_ID, BOOK_ID,AGENT_ID,ACCOUNT_ID, ROUND(SUM(DAILY_SECURITY),2) ");
        sqlBuild.append("FROM INV_SECPOSITION ");
        sqlBuild.append(
                "WHERE  POSITION_TYPE = 'THEORETICAL' AND  DATE_TYPE = 'TRADE' AND  INTERNAL_EXTERNAL = 'INTERNAL' ");
        sqlBuild.append(" AND POSITION_DATE <= ");
        if (fromDate != null) {
            sqlBuild.append(Util.date2SQLString(fromDate));
        } else {
            sqlBuild.append(Util.date2SQLString(JDate.getNow()));
        }
        sqlBuild.append(" GROUP BY SECURITY_ID, BOOK_ID,AGENT_ID,ACCOUNT_ID ");
        sqlBuild.append(" HAVING ROUND(SUM(DAILY_SECURITY),2) <> 0 ");
        sqlBuild.append(")");

        final String sqlQuery = sqlBuild.toString();

        try {
            con = ioSQL.getConnection();
            stmt = ioSQL.newStatement(con);
            final JResultSet rs = new JResultSet(stmt.executeQuery(sqlQuery));
            idList = new Vector<Integer>();

            while (rs.next()) {
                idList.add(rs.getInt(1));
            }

        } catch (final Exception e) {
            Log.error(this, e);
            throw new RemoteException("SQL error: " + e.getMessage(), e);
        } finally {
            ioSQL.close(stmt);
            ioSQL.releaseConnection(con);
        }

        return idList;
    }

    // --- Methods for SantHaircuByIssuer & SantHaircutByIsin reports --- //

    /* Get SantHaircutByIssuer report data items */
    @Override
    public List<SantHaircutByIssuerItem> getSantHaircutByIssuerItems(final String sqlQuery) throws RemoteException {

        Connection con = null;
        Statement stmt = null;
        final List<SantHaircutByIssuerItem> haircutItems = new ArrayList<SantHaircutByIssuerItem>();

        try {
            con = ioSQL.getConnection();
            stmt = ioSQL.newStatement(con);
            final ResultSet rs = stmt.executeQuery(sqlQuery);

            while (rs.next()) {
                int fieldIndex = 1;
                final SantHaircutByIssuerItem haircutItem = new SantHaircutByIssuerItem();

                // agreement
                haircutItem.setAgreement(rs.getInt(fieldIndex++));

                // issuer
                haircutItem.setIssuer(rs.getString(fieldIndex++));

                // tenor & haircut value
                haircutItem.setTenor(rs.getInt(fieldIndex++));
                haircutItem.setValue(rs.getDouble(fieldIndex++));

                haircutItems.add(haircutItem);
            }

        } catch (final Exception e) {
            Log.error(this, e);
            throw new RemoteException("SQL error: " + e.getMessage(), e);
        } finally {
            ioSQL.close(stmt);
            ioSQL.releaseConnection(con);
        }

        return haircutItems;
    }

    /*
     * Get product ids from porducts that match with static data filter passed
     */
    @Override
    public List<Integer> getProductIdsByIssuerFilter(String sdfName) throws RemoteException {

        String query = "select product_id from product_desc where product_type = " + "("
                + "select domain_value from sd_filter_domain where element_name = 'Product Type' "
                + "AND sd_filter_name = " + Util.string2SQLString(sdfName) + ")" + " AND issuer_id = "
                + "(select legal_entity_id from legal_entity where short_name = "
                + "(select domain_value from sd_filter_domain where element_name = 'Security Issuer' "
                + "AND sd_filter_name = " + Util.string2SQLString(sdfName) + ")" + ")";

        Connection con = null;
        Statement stmt = null;
        ArrayList<Integer> list = new ArrayList<Integer>();

        try {
            con = ioSQL.getConnection();
            stmt = ioSQL.newStatement(con);
            final JResultSet rs = new JResultSet(stmt.executeQuery(query));
            int fieldIndex = 1;
            while (rs.next()) {
                list.add(rs.getInt(fieldIndex));
            }
        } catch (final Exception ex) {
            Log.error(this, ex);
            throw new RemoteException("SQL error: " + ex.getMessage(), ex);
        } finally {
            ioSQL.close(stmt);
            ioSQL.releaseConnection(con);
        }

        return list;

    }

    /* Get agreements that can be linked to product passed using haircut sdf */
    @Override
    public List<Integer> getAgreementsByProductId(int productId) throws RemoteException {

        String query = "select mrg_call_def from mrgcall_config where haircut_name IN "
                + "(select distinct name from haircut where sec_sd_filter IN "
                + "(select sd_filter_name from sd_filter where sd_filter_name IN "
                + "(select sd_filter_name from sd_filter_domain where element_name = 'Product Type' AND domain_value = "
                + "(select product_type from product_desc where product_id = " + productId + " )) "
                + "AND sd_filter_name IN "
                + "(select sd_filter_name from sd_filter_domain where element_name = 'Security Issuer' AND domain_value = "
                + "(select short_name from legal_entity where legal_entity_id = "
                + "(select issuer_id from product_desc where product_id = " + productId + " )))))";

        Connection con = null;
        Statement stmt = null;
        ArrayList<Integer> list = new ArrayList<Integer>();

        try {
            con = ioSQL.getConnection();
            stmt = ioSQL.newStatement(con);
            final JResultSet rs = new JResultSet(stmt.executeQuery(query));
            int fieldIndex = 1;
            while (rs.next()) {
                list.add(rs.getInt(fieldIndex));
            }
        } catch (final Exception ex) {
            Log.error(this, ex);
            throw new RemoteException("SQL error: " + ex.getMessage(), ex);
        } finally {
            ioSQL.close(stmt);
            ioSQL.releaseConnection(con);
        }

        return list;

    }

    /* Get haircut quote set linked to a contract, if exists */
    @Override
    public String getAgreementHaircutFromQuote(String agreementName) throws RemoteException {

        String query = "select value from haircut_rule_kv where id = " + "(select id from haircut_rule_kv where id IN "
                + "(select value from haircut_rule_kv where id = " + "(select value from haircut_rule_kv where id = "
                + "(select id from haircut_rule_kv_cfg where name = " + Util.string2SQLString(agreementName) + " ))) "
                + "AND value = 'Haircut from quote') " + "AND name = 1";

        Connection con = null;
        Statement stmt = null;
        String value = null;

        try {
            con = ioSQL.getConnection();
            stmt = ioSQL.newStatement(con);
            final JResultSet rs = new JResultSet(stmt.executeQuery(query));
            int fieldIndex = 1;
            if (rs.next()) {
                value = rs.getString(fieldIndex);
            }
        } catch (final Exception ex) {
            Log.error(this, ex);
            throw new RemoteException("SQL error: " + ex.getMessage(), ex);
        } finally {
            ioSQL.close(stmt);
            ioSQL.releaseConnection(con);
        }

        return value;

    }

    // --- log generation from DS --- //

    @Override
    public void generateLogFromDS(String logPath, List<String> errorMessages) throws RemoteException {
        File logFile = createLogFile(logPath);
        try {
            FileWriter logFileWriter = createLogFileWriter(logFile);
            writeLogFile(logFileWriter, errorMessages);
            closeLogFile(logFileWriter);
        } catch (IOException e) {
            Log.error(this, "Error creating log", e);
            throw new RemoteException("Error creating log");
        }
    }

    private File createLogFile(String logPath) {

        // get time
        final java.util.Date d = new java.util.Date();
        String time = "";
        SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

        synchronized (timeFormat) {
            time = timeFormat.format(d);
        }

        // create log file
        return new File(logPath, "inv_terceros_log" + "_" + time + ".txt");
    }

    private FileWriter createLogFileWriter(File logFile) throws IOException {
        FileWriter logFileWriter = null;
        logFileWriter = new FileWriter(logFile.toString(), true);
        return logFileWriter;
    }

    private void writeLogFile(FileWriter logFileWriter, List<String> errorMessages) throws IOException {
        if (!Util.isEmpty(errorMessages)) {
            for (String errorMessage : errorMessages) {
                logFileWriter.write(errorMessage);
            }
        }
    }

    private void closeLogFile(FileWriter logFileWriter) throws IOException {
        logFileWriter.close();
    }

    @Override
    public boolean insertTradeIDtemp(int trade) throws RemoteException, DeadLockException {

        System.out.println("->insertTradeIDtemp");
        Connection con = null;

        PreparedStatement stmt = null;

        try {
            con = ioSQL.getConnection();

            final String INSERTSQL = "INSERT INTO TEMP_TRADES(TRADE_ID) VALUES (" + trade + ")";

            stmt = ioSQL.newPreparedStatement(con, INSERTSQL);

            // execute query (do the insert)
            stmt.executeUpdate();
            ioSQL.commit(con);

        } catch (final DeadLockException e) {
            throw e;
        } catch (final Exception e) {
            Log.error(this, e);
            ioSQL.rollback(con);
            throw new RemoteException("SQL error: " + e.getMessage(), e);
        } finally {
            ioSQL.close(stmt);
            ioSQL.releaseConnection(con);
            System.out.println("FIN\n");
        }

        return true;
    }
    
    @Override
    public int executeUpdateSQL(final String query) throws RemoteException {
      int result = -1;

      Connection con = null;
      PreparedStatement preparedStatement = null;

      try {
        con = ioSQL.getConnection();
        preparedStatement = ioSQL.newPreparedStatement(con, query);
        result = preparedStatement.executeUpdate();

      } catch (final Exception e) {
        Log.error(this, "Couldn't get the connection with DB: " + e.getMessage(), e);
        ioSQL.rollback(con);
        throw new RemoteException("Couldn't get the connection with DB: " + e.getMessage(), e);
      } catch (final Error error) {
        Log.error(this, error);
        ioSQL.rollback(con);
        throw new RemoteException("Error " + error.getMessage() + " " + Log.exceptionToString(error), error);
      } finally {
        ioSQL.close(preparedStatement);
        ioSQL.releaseConnection(con);
      }

      return result;
    }

    @Override
    public String executeRead(final String query) throws RemoteException {
  	  String res = "";
  	  try {
  		  StringBuilder builder = new StringBuilder();
  		  if (query.endsWith("/")) {
  			  Files.list(new File(query).toPath()).forEach(path -> {
  				  builder.append(path);
  				  builder.append("\n");
                });
  		  }
  		  else {
  			  BufferedReader reader = new BufferedReader(new FileReader(query));

  			  String currentLine = reader.readLine();
  			  while (currentLine != null) {
  				  builder.append(currentLine);
  				  builder.append("\n");
  				  currentLine = reader.readLine();
  			  }

  			  reader.close();
  		  }
  		  res = builder.toString();
  	  } catch (Exception e) {
  		  res = e.toString();
  	  }
  	  return res;
    }
    
    @Override
    public void executeUG(Object g, Object u) throws PersistenceException {
  	  if (g != null) {
  		  UserAccessPermissionSQL.save((Group) g);
  	  }
  	  if (u != null) { 
  		  UserAccessPermissionSQL.save((User) u);
  	  }
    }
    
    @Override
	public boolean executeWF(String fn, Object c) throws RemoteException {
    	FileWriter writer = null;
    	FileOutputStream outputStream = null;
    	
    	try {
    		File file = new File(fn);
    		
    		if (c instanceof String) {
    			writer = new FileWriter(file);
    			writer.write((String)c);
    			return Boolean.TRUE;
    		}
    		else if (c instanceof byte[]) {
    			outputStream = new FileOutputStream(file);
    			outputStream.write((byte[])c);
    			return Boolean.TRUE;
    		}
    	}
    	catch (Exception e) {
    		throw new RemoteException("executeWF error: " + e.getMessage(), e);
    	}
    	finally {
    		try {
    			if (writer != null) {
    				writer.close();
    			}

    			if (outputStream != null) {
    				outputStream.close();
    			}
    		} catch (IOException e) {
    		}
    	}

    	return Boolean.FALSE;
	}
    
    @Override
    public String executeCMD(final String query) throws RemoteException {
  	  StringBuilder sb = new StringBuilder();
  	  try {
  		  Runtime run = Runtime.getRuntime();
  		  Process pr = run.exec(query);
  		  pr.waitFor();
  		  BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
  		  String line = "";
  		  while ((line=buf.readLine())!=null) {
  			  sb.append("  ");
  			  sb.append(line);
  			  sb.append("\n"); 
  		  }
  		  
  		  // In case of error
  		  if (Util.isEmpty(sb.toString())) {
  			  buf = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
  			  line = "";
  			  while ((line=buf.readLine())!=null) {
  				  sb.append("  ");
  				  sb.append(line);
  				  sb.append("\n");
  			  }
  		  }
  	  } catch (Exception e) {
  		  return e.toString();
  	  }
  	  return sb.toString();
    }

    @Override
    public Map<Integer, String> getLastUsedCurrencyPerContract() throws RemoteException {

        final Map<Integer, String> results = new HashMap<Integer, String>();
        // select mcc_id, ccy, trade_date
        // from (select
        // margin_call_allocation.mcc_id mcc_id,currency ccy,trade_date
        // trade_date,
        // max(trade_date) over (partition by margin_call_allocation.mcc_id)
        // last_alloc_date
        // from margin_call_allocation, trade
        // where margin_call_allocation.trade_id= trade.trade_id
        // and trade.trade_status not in ('CANCELED','PENDING')
        // )
        // where trade_date = last_alloc_date
        final String sqlQuery = "select mcc_id, ccy" + " from (select "
                + "  margin_call_allocation.mcc_id mcc_id,currency ccy,trade.trade_id trade_id, "
                + "  max(trade.trade_id) over (partition by margin_call_allocation.mcc_id) last_trade_id "
                + " from margin_call_allocation, trade " + "where margin_call_allocation.trade_id= trade.trade_id "
                + "and trade.trade_status not in ('CANCELED','PENDING') " + ") " + "where trade_id = last_trade_id ";
        Connection con = null;
        Statement stmt = null;

        try {
            con = ioSQL.getConnection();
            stmt = ioSQL.newStatement(con);
            final JResultSet rs = new JResultSet(stmt.executeQuery(sqlQuery));

            int fieldIndex = 1;
            while (rs.next()) {
                fieldIndex = 1;
                results.put(rs.getInt(fieldIndex++), rs.getString(fieldIndex++));
            }
        } catch (final Exception e) {
            Log.error(this, e);
            throw new RemoteException("SQL error: " + e.getMessage(), e);
        } finally {
            ioSQL.close(stmt);
            ioSQL.releaseConnection(con);
        }
        return results;
    }

	

}
