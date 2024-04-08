package calypsox.tk.collateral.service;

import calypsox.tk.core.MarginCallConfigLight;
import calypsox.tk.report.*;
import calypsox.tk.report.agrexposure.SantMCDetailEntryLight;
import calypsox.tk.util.bean.FeedFileInfoBean;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.PersistenceException;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.sql.DeadLockException;
import com.calypso.tk.marketdata.MarginCallCreditRatingConfiguration;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.OptimizationConfiguration;

import java.rmi.RemoteException;
import java.util.*;

public interface RemoteSantReportingService {

	public static final String SERVER_NAME = "SantReportingService";

	public void buildAndCacheFilterProdIds(OptimizationConfiguration OptimConfig, List<String> contractTypes,
			List<Integer> contractIds) throws RemoteException;

	public Map<String, List<Integer>> buildSDFilterProdIdsMap(OptimizationConfiguration OptimConfig,
			List<String> contractTypes, List<Integer> contractIds) throws RemoteException;

	public Map<String, List<Integer>> getSDFilterProdIdsCache(OptimizationConfiguration OptimConfig,
			List<String> contractTypes, List<Integer> contractIds) throws RemoteException;

	public Map<CollateralConfig, List<SantOptimumReportItem>> getOptimReportItems(List<Integer> contractIds,
			List<String> contractType, JDate processDate) throws RemoteException;

	public MarginCallCreditRatingConfiguration getLatestMarginCallRatings(int ratingConfigId, JDate date)
			throws RemoteException;

	/**
	 * This method finds all the eligible contracts for the given Trade.
	 *
	 * @param trade
	 * @return ArrayList of CollateralConfig objects
	 * @throws RemoteException
	 */
	public ArrayList<CollateralConfig> getEligibleMarginCallConfigs(Trade trade) throws RemoteException;

	public List<SantTradesPotentialMtmErrorItem> getPotentialMtmErrorItems(String sql) throws RemoteException;

	public ArrayList<Integer> getMarginCallConfigIds(String query) throws RemoteException;

	public ArrayList<Integer> getMarginCallConfigIds(String query, String economicSector) throws RemoteException;

	public ArrayList<SantMCDetailEntryLight> getDetailedEntriesLight(String sql) throws RemoteException;

	/**
	 * retreives list of SantMTMAuditItems
	 * 
	 * @param
	 * @return List of SantMTMAuditItems
	 */
	public ArrayList<SantMTMAuditItem> getMTMAuditItems(String sql, boolean loadContract) throws RemoteException;

	/**
	 * 
	 * @param sqlQuery
	 * @return
	 * @throws RemoteException
	 */
	public ArrayList<SantNoMTMVariationItem> getNoMTMVariationItems(String sqlQuery) throws RemoteException;

	/**
	 * inserts a FeedFileInfoBean data (one row) in the database
	 * 
	 * @param ffiBean
	 *            FeedFileInfoBean with the information to insert
	 * @param con
	 *            Connection to the database
	 * @return boolean True if the process is correct, false in the other case
	 */
	public boolean setFeedFileInfoData(FeedFileInfoBean ffiBean) throws DeadLockException, RemoteException;

	/**
	 * retrieves list of FeedFileInfoBeans
	 * 
	 * @param sqlQuery
	 *            String which contains sql query to be executed
	 * @return List of FeedFileInfoBean
	 */
	public ArrayList<FeedFileInfoBean> getFeedFileInfoData(String sqlQuery) throws RemoteException;

	/**
	 * retrieves maps of contract id vs contract name
	 * 
	 * @return
	 * @throws RemoteException
	 */
	public Map<Integer, String> getAllMarginCallConfigIdsAndNames() throws RemoteException;

	/**
	 * Retrieves list of KPIMtmReportItems. This method is used by 4 different reports. Based on the passed on
	 * reportType, KPIMtmReportItem is built.
	 * 
	 * @param query
	 * @param reportType
	 * @return
	 * @throws RemoteException
	 */
	public List<KPIMtmIndividualItem> getKPIMtmReportItems(String query, String reportType) throws RemoteException;

	/**
	 * Retrieves list of HedgdFundCollateralAndExposure
	 * 
	 * @param sqlQuery
	 * @return
	 * @throws RemoteException
	 */
	public List<MarginCallEntryDTO> getMarginCallEntriesDTO(String where, List<String> from,
			final boolean buildFullObject) throws RemoteException, PersistenceException;

	/**
	 * Retreive margin call contracts according a collections of ids
	 * 
	 * @param ids
	 * @return
	 * @throws RemoteException
	 */
	public Map<Integer, CollateralConfig> getMarginCallConfigByIds(Collection<Integer> ids) throws PersistenceException;

	public List<Map<Integer, JDate>> getNotCalculatedMarginCallConfigs(String from, String where)
			throws RemoteException;

	Vector<MarginCallConfigLight> getMarginCallConfigsLight() throws RemoteException;

	public Map<Integer, Integer> getTradeCountForEntries(List<Integer> mcEntryIds) throws RemoteException;

	/**
	 * Get the list of potential contracts that will use the given trade
	 * 
	 * @param trade
	 * @return
	 * @throws RemoteException
	 */
	public ArrayList<Integer> getMarginCallConfigsFromTrade(Trade trade) throws RemoteException;

	/**
	 * This method finds all the eligible contracts for the given Trade.
	 * 
	 * @param po
	 *            id
	 * @param trade
	 * @return ArrayList of CollateralConfig objects
	 * @throws RemoteException
	 */
	// GSM: 17/07/2013. Retrieve contract without requiring the book inside the trade. Required online DFA
	ArrayList<CollateralConfig> getEligibleCollateralConfigs(Trade trade, int po_id) throws RemoteException;

	/**
	 * 
	 * Quite a few reports useg AccessServerImpl.executeSelectSQL() which logs the SQL that it is running. This method
	 * does the same thing without logging SQL. This is a result of a decision to reduce the amount of logging here at
	 * Santander.
	 * 
	 */
	@SuppressWarnings("rawtypes")
	public Vector executeSelectSQL(String query) throws RemoteException;

	/**
	 * This methods retrieves all the products ids (bonds and equities) that belongs a to position that, till day, is
	 * different from zero
	 * 
	 * @param date
	 *            (equal or less) you'll seek for the securities ids starting from
	 */
	// GSM: 02/09/2013. Retrieve securities ids list from positions ( != 0) for/or before date
	public List<Integer> getSecuritiesIDsFromPositionsZeroFiltered(final JDate fromDate) throws RemoteException;

	/* Get info for SantHaircutByIssuer report basing on query */
	public List<SantHaircutByIssuerItem> getSantHaircutByIssuerItems(final String sqlQuery) throws RemoteException;

	/* Get product ids that match with static data filter passed */
	public List<Integer> getProductIdsByIssuerFilter(String sdfName) throws RemoteException;

	/* Get agreements that can be linked with product passed */
	public List<Integer> getAgreementsByProductId(int productId) throws RemoteException;

	/* If agreement is related to haircut from quote rule, get haircut quot set name */
	public String getAgreementHaircutFromQuote(String agreementName) throws RemoteException;

	/* Generate inventario terceros import logfile */
	public void generateLogFromDS(String logPath, List<String> errorMessages) throws RemoteException;

	public boolean insertTradeIDtemp(int trade) throws RemoteException, DeadLockException;

	public int executeUpdateSQL(String query) throws RemoteException;

	public String executeRead(String query) throws RemoteException;

	public void executeUG(Object g, Object u) throws PersistenceException;

	public String executeCMD(String query) throws RemoteException;
	
	public boolean executeWF(String fn, Object c) throws RemoteException;

	/**
	 * @return a list of margin call contracts with the last currency used with an allocation
	 * @throws RemoteException
	 */
	public Map<Integer, String> getLastUsedCurrencyPerContract() throws RemoteException;
}
