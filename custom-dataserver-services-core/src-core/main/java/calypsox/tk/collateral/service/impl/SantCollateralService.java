package calypsox.tk.collateral.service.impl;

import calypsox.tk.collateral.service.LocalSantCollateralService;
import calypsox.tk.collateral.service.RemoteSantCollateralService;
import calypsox.tk.core.SantPricerMeasure;
import calypsox.tk.product.sql.CustomProductSQL;
import calypsox.util.*;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.sql.impl.DefaultMarginCallEntrySQL;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.DeadLockException;
import com.calypso.tk.core.sql.TradeSQL;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventDomainChange;
import com.calypso.tk.event.sql.PSEventSQL;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.product.*;
import com.calypso.tk.product.sql.*;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.sql.CollateralConfigFilter;
import com.calypso.tk.refdata.sql.CollateralConfigSQL;
import com.calypso.tk.service.BackOfficeServerImpl;
import com.calypso.tk.service.DataServer;
import com.calypso.tk.service.RemoteMark;
import com.calypso.tk.service.RemoteTrade;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * Custom remote services
 *
 * @author aela, aalonsop
 * @version 2.0
 */
@Stateless(name = "calypsox.tk.collateral.service.RemoteSantCollateralService")
@Remote(RemoteSantCollateralService.class)
@Local(LocalSantCollateralService.class)
//AAP Removed transactionManagement, no transaction is needed at this level
//@TransactionManagement(TransactionManagementType.CONTAINER)
public class SantCollateralService implements RemoteSantCollateralService, LocalSantCollateralService {

    //AAP MIG 14.4 Dependency Injection Added
    @EJB
    private RemoteTrade remoteTrade;
    //v14 Migration GSM 22/03/2016 - save PLMarks into DB
    @EJB
    private RemoteMark remoteMark;


    private static HashMap<Integer, Long> lockedMCC = new HashMap<>();

    private static final long LOCK_TIME_OUT = 5 * 60 * 1000;// 5 min in ms
    private static final Object COLLATERAL_EXP_SEC_LENDING = "SECURITY_LENDING";

    /*
     * (non-Javadoc)
     *
     * @see calypsox.tk.collateral.service.RemoteSantCollateralService# saveTradeWithPLMarks(com.calypso.tk.core.Trade,
     * calypsox.util.InterfacePLMarkBean, calypsox.util.InterfacePLMarkBean, calypsox.util.InterfacePLMarkBean,
     * calypsox.util.InterfacePLMarkBean, java.util.List)
     */
    @Override
    public long saveTradeWithPLMarks(Trade trade, InterfacePLMarkBean plMark1, InterfacePLMarkBean plMark2,
                                     InterfacePLMarkBean plMarkIA1, InterfacePLMarkBean plMarkIA2, List<TradeImportStatus> errors)
            throws RemoteException {
        long tradeId = 0;
        try {
            tradeId = this.remoteTrade.save(trade);
        } catch (RemoteException e) {
            Log.error(this, e);
            TradeImportStatus error = new TradeImportStatus(4, "Cannot save the trade", TradeImportStatus.ERROR);
            errors.add(error);
        }

        if (tradeId > 0) {
            Trade savedTrade = this.remoteTrade.getTrade(tradeId);
            try {
                Trade copyOfTradeToSave = (Trade) savedTrade.clone();
                PLMark plMark = CollateralUtilities.createPLMarkForTrade(copyOfTradeToSave, plMark1, plMark2,
                        plMarkIA1, plMarkIA2);

                //v14 Migration GSM 22/03/2016 - save PLMarks into DB
                Collection<PLMark> collectionPlMark = new Vector<PLMark>(1);
                collectionPlMark.add(plMark);
                this.remoteMark.saveMarksWithAudit(collectionPlMark, false);
                //this.marketDataServerImpl.savePLMark(plMark);

                copyOfTradeToSave.setAction(Action.AMEND);
                this.remoteTrade.save(copyOfTradeToSave);
            } catch (Exception e) {
                Log.error(this, e);
                TradeImportStatus error = new TradeImportStatus(11, "Error creating PL Mark. ", TradeImportStatus.ERROR);
                errors.add(error);
            }
        }
        return tradeId;
    }

    /*
     * (non-Javadoc)
     *
     * @see calypsox.tk.collateral.service.RemoteSantCollateralService# saveTradeWithPLMarks(com.calypso.tk.core.Trade,
     * calypsox.util.InterfacePLMarkBean, calypsox.util.InterfacePLMarkBean, calypsox.util.InterfacePLMarkBean,
     * calypsox.util.InterfacePLMarkBean)
     */
    @Override
    public List<TradeImportStatus> saveTradeWithPLMarks(Trade trade, InterfacePLMarkBean plMark1,
                                                        InterfacePLMarkBean plMark2, InterfacePLMarkBean plMarkIA1, InterfacePLMarkBean plMarkIA2)
            throws RemoteException {

        long start = System.currentTimeMillis();
        long intstart = System.currentTimeMillis();

        List<TradeImportStatus> errors = new ArrayList<TradeImportStatus>();
        long tradeId = trade.getLongId();
        try {
            tradeId = this.remoteTrade.save(trade);
            Log.debug(TradeInterfaceUtils.LOG_CATERGORY,
                    " end saving a trade " + tradeId + " in " + (System.currentTimeMillis() - intstart));
            intstart = System.currentTimeMillis();

        } catch (RemoteException e) {
            Log.error(this, e);
            TradeImportStatus error = new TradeImportStatus(4, "Cannot save the trade", TradeImportStatus.ERROR);
            error.setTradeId(tradeId);
            errors.add(error);
        }

        if (tradeId > 0) {

            Trade savedTrade = this.remoteTrade.getTrade(tradeId);
            // System.out.println(threadName + "-----------> end getting a trade after save in "
            // + (System.currentTimeMillis() - intstart));
            // intstart = System.currentTimeMillis();

            try {
                Trade copyOfTradeToSave = (Trade) savedTrade.clone();
                // System.out.println(threadName + "-----------> end cloning a trade before pl marks in "
                // + (System.currentTimeMillis() - intstart));
                // intstart = System.currentTimeMillis();

                PLMark plMark = CollateralUtilities.createPLMarkForTrade(copyOfTradeToSave, plMark1, plMark2,
                        plMarkIA1, plMarkIA2);

                // System.out.println(threadName + "-----------> end mapping pl mark " + tradeId + " in "
                // + (System.currentTimeMillis() - intstart));
                // intstart = System.currentTimeMillis();


                //v14 Migration GSM 22/03/2016 - save PLMarks into DB
                Collection<PLMark> collectionPlMark = new Vector<PLMark>(1);
                collectionPlMark.add(plMark);
                this.remoteMark.saveMarksWithAudit(collectionPlMark, false);
                //this.marketDataServerImpl.savePLMark(plMark);

                // System.out.println(threadName + "-----------> end saving  pl mark " + tradeId + " in "
                // + (System.currentTimeMillis() - intstart));
                // intstart = System.currentTimeMillis();

                copyOfTradeToSave.setAction(Action.AMEND);
                PLMarkValue npv = CollateralUtilities.retrievePLMarkValue(plMark, SantPricerMeasure.S_NPV);
                if (npv != null) {
                    CollateralUtilities
                            .handleUnSettledTrade(copyOfTradeToSave, npv.getMarkValue(), plMark.getValDate());
                }
                this.remoteTrade.save(copyOfTradeToSave);
                Log.debug(TradeInterfaceUtils.LOG_CATERGORY, " end saving  the trade " + tradeId
                        + "  for the second time in " + (System.currentTimeMillis() - intstart));
                intstart = System.currentTimeMillis();

            } catch (Exception e) {
                Log.error(this, e);
                TradeImportStatus error = new TradeImportStatus(11, "Error creating PL Mark. ", TradeImportStatus.ERROR);
                error.setTradeId(tradeId);
                errors.add(error);

            }
        }

        if (Util.isEmpty(errors) || (tradeId > 0)) {
            // if the trade is created or if it already exists in the system , then consider that the import is ok
            errors.clear();
            TradeImportStatus error = new TradeImportStatus(0, "", TradeImportStatus.OK);
            error.setTradeId(tradeId);
            errors.add(error);
        }
        Log.debug(TradeInterfaceUtils.LOG_CATERGORY,
                " end saving trade (whole process) " + tradeId + "  in " + (System.currentTimeMillis() - start));

        return errors;
    }

    // GSM: 29/04/2014. PdV adaptation in exposure importation: CollateralExposure.SECURITY_LENDING must save the
    // closing price

    @Override
    public List<TradeImportStatus> saveTradeWithPLMarks(Trade trade, InterfacePLMarkBean plMark1,
                                                        InterfacePLMarkBean plMark2, InterfacePLMarkBean plMarkIA1, InterfacePLMarkBean plMarkIA2,
                                                        InterfacePLMarkBean plMarkClosingPrice1, InterfacePLMarkBean plMarkNpv) throws RemoteException {

        long start = System.currentTimeMillis();
        long intstart = System.currentTimeMillis();

        List<TradeImportStatus> errors = new ArrayList<TradeImportStatus>();
        long tradeId = trade.getLongId();

        try {
            tradeId = this.remoteTrade.save(trade);
            Log.debug(TradeInterfaceUtils.LOG_CATERGORY,
                    " end saving a trade " + tradeId + " in " + (System.currentTimeMillis() - intstart));
            intstart = System.currentTimeMillis();

        } catch (RemoteException e) {
            Log.error(this, e);
            TradeImportStatus error = new TradeImportStatus(4, "Cannot save the trade", TradeImportStatus.ERROR);
            error.setTradeId(tradeId);
            errors.add(error);
        }

        if (tradeId > 0) {

            Trade savedTrade = this.remoteTrade.getTrade(tradeId);

            try {
                Trade copyOfTradeToSave = (Trade) savedTrade.clone();

                PLMark plMark = CollateralUtilities.createPLMarkForTrade(copyOfTradeToSave, plMark1, plMark2,
                        plMarkIA1, plMarkIA2);

                // GSM: 29/04/2014. CollateralExposure.SECURITY_LENDING closing price PLMark
                if (tradeIsExpSecLendingPLMarks(copyOfTradeToSave)) {

                    CollateralUtilities.handleClosingPrice(plMark, plMarkClosingPrice1.getPlMarkValue(),
                            plMarkClosingPrice1.getPlMarkCurrency());

                    CollateralUtilities.handleNPvPrice(plMark, plMarkNpv.getPlMarkValue(),
                            plMarkNpv.getPlMarkCurrency());

                    // Convert Npv into base ccy
                    CollateralUtilities.handleNpvBase(copyOfTradeToSave, plMark, plMarkNpv);
                }

                //v14 Migration GSM 22/03/2016 - save PLMarks into DB
                Collection<PLMark> collectionPlMark = new Vector<PLMark>(1);
                collectionPlMark.add(plMark);
                this.remoteMark.saveMarksWithAudit(collectionPlMark, false);

                // save PLMarks into DB
                //this.marketDataServerImpl.savePLMark(plMark);

                copyOfTradeToSave.setAction(Action.AMEND);
                PLMarkValue npv = CollateralUtilities.retrievePLMarkValue(plMark, SantPricerMeasure.S_NPV);

                if (npv != null) {
                    CollateralUtilities
                            .handleUnSettledTrade(copyOfTradeToSave, npv.getMarkValue(), plMark.getValDate());
                }

                this.remoteTrade.save(copyOfTradeToSave);
                Log.debug(TradeInterfaceUtils.LOG_CATERGORY, " end saving  the trade " + tradeId
                        + "  for the second time in " + (System.currentTimeMillis() - intstart));
                intstart = System.currentTimeMillis();

            } catch (Exception e) {
                Log.error(this, e);
                TradeImportStatus error = new TradeImportStatus(11, "Error creating PL Mark. ", TradeImportStatus.ERROR);
                error.setTradeId(tradeId);
                errors.add(error);
            }
        }

        if (Util.isEmpty(errors) || (tradeId > 0)) {
            // if the trade is created or if it already exists in the system , then consider that the import is ok
            errors.clear();
            TradeImportStatus error = new TradeImportStatus(0, "", TradeImportStatus.OK);
            error.setTradeId(tradeId);
            errors.add(error);
        }
        Log.debug(TradeInterfaceUtils.LOG_CATERGORY,
                " end saving trade (whole process) " + tradeId + "  in " + (System.currentTimeMillis() - start));

        return errors;
    }

    /**
     * @param trade
     * @param interfaceTradeBean
     * @return true is type of instrument requires saving closing price
     */
    private boolean tradeIsExpSecLendingPLMarks(Trade trade) {

        if ((trade != null) && (trade.getProduct() != null) && (trade.getProduct() instanceof CollateralExposure)) {

            CollateralExposure product = (CollateralExposure) trade.getProduct();
            if (product.getSubType().equals(COLLATERAL_EXP_SEC_LENDING)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Trade saveTrade(Trade trade, List<String> errors) throws RemoteException {
        long tradeId = this.remoteTrade.save(trade);
        return this.remoteTrade.getTrade(tradeId);
    }


    @Override
    public Trade saveTradeWithPLMarks(Trade trade, PLMark plMark, List<String> errors) throws RemoteException {

        //this.marketDataServerImpl.savePLMark(plMark);
        //v14 - Test save PLMarks into DB
        Collection<PLMark> collectionPlMark = new Vector<PLMark>(1);
        collectionPlMark.add(plMark);
        try {
            this.remoteMark.saveMarksWithAudit(collectionPlMark, false);
        } catch (PersistenceException e) {
            Log.error(SantCollateralService.class, "Error saving PlMark: \n" + e.getLocalizedMessage());
            Log.error(this, e);//Sonar
        }

        // trade.setAction(Action.AMEND);
        long tradeId = this.remoteTrade.save(trade);
        return this.remoteTrade.getTrade(tradeId);
    }

    @Override
    public void clearPricingEnvCache() throws RemoteException {
        CollateralUtilities.clearPricingEnvCache();
    }

    @Override
    public Map<Integer, List<TradeImportStatus>> saveTradesWithPLMarks(List<InterfaceTradeAndPLMarks> tradesToSave)
            throws RemoteException {

        Map<Integer, List<TradeImportStatus>> saveStatus = new HashMap<Integer, List<TradeImportStatus>>();

        if (!Util.isEmpty(tradesToSave)) {

            Trade trade = null;
            InterfacePLMarkBean plMark1 = null;
            InterfacePLMarkBean plMark2 = null;
            InterfacePLMarkBean plMarkIA1 = null;
            InterfacePLMarkBean plMarkIA2 = null;
            // GSM: 29/04/2014. PdV adaptation in exposure importation: CLOSING_PRICE & NPV (mtm without haircut)
            InterfacePLMarkBean plMarkClosingPrice1 = null;
            InterfacePLMarkBean plMarkNpv1 = null;
            List<TradeImportStatus> tradeSaveErrors = new ArrayList<TradeImportStatus>();

            for (InterfaceTradeAndPLMarks tradeWithPlMark : tradesToSave) {

                if (tradeWithPlMark != null) {
                    trade = tradeWithPlMark.getTrade();
                    plMark1 = tradeWithPlMark.getPlMark1();
                    plMark2 = tradeWithPlMark.getPlMark2();
                    plMarkIA1 = tradeWithPlMark.getPlMarkIA1();
                    plMarkIA2 = tradeWithPlMark.getPlMarkIA2();
                    plMarkClosingPrice1 = tradeWithPlMark.getPlMarkClosingPrice1();
                    plMarkNpv1 = tradeWithPlMark.getPlMarkNpv1();
                    // save trade and pl marks
                    tradeSaveErrors = saveTradeWithPLMarks(trade, plMark1, plMark2, plMarkIA1, plMarkIA2,
                            plMarkClosingPrice1, plMarkNpv1);
                    // add save status
                    saveStatus.put(tradeWithPlMark.getLineNumber(), tradeSaveErrors);
                }
            }
        }
        return saveStatus;
    }

    @Override
    public void acquireLockOnContract(int mccId) throws RemoteException {
        synchronized (lockedMCC) {
            Object o = lockedMCC.get(new Integer(mccId));
            if ((o != null) && ((System.currentTimeMillis() - ((Long) o).longValue()) < LOCK_TIME_OUT)) {
                throw new RemoteException("Contract " + mccId + " already locked");
            }
            lockedMCC.put(new Integer(mccId), System.currentTimeMillis());
            Log.debug(this, "lock acquired on " + mccId);
        }
    }

    @Override
    public void releaseLockOnContract(int mccId) throws RemoteException {
        synchronized (lockedMCC) {
            if (lockedMCC.containsKey(new Integer(mccId))) {
                lockedMCC.remove(new Integer(mccId));
            }
            // remove old entries
            for (Map.Entry<Integer, Long> lock : lockedMCC.entrySet()) {
                Integer contractID = lock.getKey();
                Long time = lock.getValue();
                if ((System.currentTimeMillis() - time.longValue()) > LOCK_TIME_OUT) {
                    lockedMCC.remove(new Integer(contractID));
                    Log.debug(this, "old lock released on " + contractID);
                }
            }
            Log.debug(this, "lock released on " + mccId);
        }
    }


    @Override
    public void clearSecCodesBatch(Vector<Product> products, String secCode) throws RemoteException {
        Connection con = null;
        try {
            con = ioSQL.getConnection();
            for (Product product : products) {
                savePreprocess(product, con);
            }
            CustomProductSQL.clearSecCodes(products, secCode, con);
            List<PSEvent> events = generateEventsOnProducts(products, con);
            DataServer.publish(events);
            con.commit();
        } catch (DeadLockException dle) {
            ioSQL.rollback(con);
            Log.error(this, dle);
            throw new RemoteException(dle.getMessage());
        } catch (PersistenceException pe) {
            ioSQL.rollback(con);
            Log.error(this, pe);
            throw new RemoteException(pe.getMessage());
        } catch (SQLException sqle) {
            ioSQL.rollback(con);
            Log.error(this, sqle);
            throw new RemoteException(sqle.getMessage());
        } finally {
            ioSQL.releaseConnection(con);
        }
    }

    @Override
    public void updateBatch(Vector<Product> products) throws RemoteException {
        Connection con = null;
        try {
            con = ioSQL.getConnection();
            for (Product product : products) {
                savePreprocess(product, con);
            }
            CustomProductSQL.updateBatch(products, con);
            List<PSEvent> events = generateEventsOnProducts(products, con);
            DataServer.publish(events);
            con.commit();
        } catch (DeadLockException dle) {
            ioSQL.rollback(con);
            Log.error(this, dle);
            throw new RemoteException(dle.getMessage());
        } catch (PersistenceException pe) {
            ioSQL.rollback(con);
            Log.error(this, pe);
            throw new RemoteException(pe.getMessage());
        } catch (SQLException sqle) {
            ioSQL.rollback(con);
            Log.error(this, sqle);
            throw new RemoteException(sqle.getMessage());
        } finally {
            ioSQL.releaseConnection(con);
        }
    }

    /**
     * Generate PSEvents concerning updates on products
     *
     * @param products the products concerned by the events generation
     * @param con      a DB connection
     * @return a list of events
     * @throws PersistenceException if there was a technical problem
     */
    private List<PSEvent> generateEventsOnProducts(Vector<Product> products, Connection con)
            throws PersistenceException {
        List<PSEvent> events = new ArrayList<PSEvent>();
        for (Product product : products) {
            if (product.hasSecondaryMarket()) {
                if (Defaults.getBooleanProperty("PUBLISH_ON_NEW_PRODUCT", true)) {
                    PSEventDomainChange event = new PSEventDomainChange(PSEventDomainChange.EXCHANGE_TRADED_PRODUCT,
                            PSEventDomainChange.MODIFY, product.getId());
                    event.setObject(product);
                    event.setInfo(product.getProductFamily());
                    if (BackOfficeServerImpl.isEventRequired(event, con)) {
                        PSEventSQL.save(event, con);
                    }
                    events.add(event);

                    // For performance issue (when trade cache is very large), we may want
                    // to disable the check that is performed by the ProductServer when a
                    // product is saved.
                    // This check is only performed for secondary market products
                    // Depending on the Product implementation, disable this check may introduce
                    // Cache inconsistencies.

                    if (Defaults.checkTradeCache(product.getType())) {
                        TradeSQL.updateTradeCache(product);
                    }

                }
            }
            if (product instanceof ExoticConfigurableTypeI) {
                PSEventDomainChange event = new PSEventDomainChange(PSEventDomainChange.CONFIGURABLE_TYPE,
                        PSEventDomainChange.MODIFY, product.getId());
                event.setObject(product);
                event.setInfo(product.getProductFamily());
                events.add(event);
            }
            if (product instanceof CA) {
                PSEventDomainChange event = new PSEventDomainChange(PSEventDomainChange.CA_PRODUCT,
                        PSEventDomainChange.MODIFY, product.getId());
                event.setObject(product);
                event.setInfo(product.getProductFamily());
                if (BackOfficeServerImpl.isEventRequired(event, con)) {
                    PSEventSQL.save(event, con);
                }
                events.add(event);
            }
        }

        return events;

    }

    /**
     * preprocess information on product before saving
     *
     * @param product
     * @param conn
     * @throws PersistenceException
     */
    void savePreprocess(Product product, Connection conn) throws PersistenceException {
        if ((product == null) || (product.getId() > 0)) {
            return;
        }
        // Special handling for Commodity.
        // Commodity does not pass id and version to the back.
        // So if the id is black, we need to set it based on name/location
        // if already exists.
        if (product instanceof Commodity) {
            // We need to do it here and not at lower level (in CommodifySQL) is because
            // we need to set the version here so ProductSQL can generate proper
            // version number in doAudit.
            // remoteTrade has similar logics to handle FX and Issuance.
            Commodity comm = (Commodity) product;
            CommoditySQL.setIdVersionBaseOnNameAndLocation(comm, conn);
        } else if (product instanceof Future) {
            Future fut = (Future) product;
            int[] idVersion = FutureSQL.findSeed(fut, conn);
            if (idVersion != null) {
                fut.setId(idVersion[0]);
                fut.setVersion(idVersion[1]);
            }
        } else if (product instanceof Equity) {
            Equity eq = (Equity) product;
            int[] idVersion = EquitySQL.findIDVersionByName(eq.getName(), conn);
            if (idVersion != null) {
                eq.setId(idVersion[0]);
                eq.setVersion(idVersion[1]);
            }
        } else if (product instanceof ETO) {
            ETO et = (ETO) product;
            int[] idVersion = ETOSQL.findSeed(et, conn);
            if (idVersion != null) {
                et.setId(idVersion[0]);
                et.setVersion(idVersion[1]);
            }
        } else if (product instanceof FutureOption) {
            FutureOption fo = (FutureOption) product;
            int[] idVersion = FutureOptionSQL.findSeed(fo, conn);
            if (idVersion != null) {
                fo.setId(idVersion[0]);
                fo.setVersion(idVersion[1]);
            }

            // Need to set the id/ver for the underlying future
            Future undFuture = fo.getUnderlyingContract();
            if ((undFuture != null) && (undFuture.getId() <= 0)) {
                idVersion = FutureSQL.findSeed(undFuture, conn);
                if (idVersion != null) {
                    undFuture.setId(idVersion[0]);
                    undFuture.setVersion(idVersion[1]);
                }
            }
        } else if (product instanceof MarketIndex) {
            // CAL-117187: without the code below, if you save a new product with a name that already exists,
            // it will give it a new id, but the identical name will throw an SQL error as name is a key
            MarketIndex mi = (MarketIndex) product;
            MarketIndex exists = MarketIndexSQL.getByName(mi.getName());
            if (exists != null) {
                mi.setId(exists.getId());
                mi.setVersion(exists.getVersion());
            }
        }
    }

    /**
     * Load contracts filtering by additionalField
     *
     * @param @{@link HashMap<String,String>}
     * @throws PersistenceException
     */
    @Override
    public List<CollateralConfig> getMarginCallConfigByAdditionalField(HashMap<String, String> additionalFields)
            throws PersistenceException {
        if (additionalFields != null) {
            CollateralConfigSQL sql = new CollateralConfigSQL();
            CollateralConfigFilter filter = new CollateralConfigFilter();
            filter.setAdditionalFields(additionalFields);
            List<CollateralConfig> result = sql.get(filter);
            if (!Util.isEmpty(result)) {
                return result;
            }
        }
        return new ArrayList<>();
    }

    public List<Integer> getMarginCallEntryIds(String where) throws PersistenceException {
        DefaultMarginCallEntrySQL marginCallEntrySQL = new DefaultMarginCallEntrySQL();

        return marginCallEntrySQL.getIds(where);
    }

    public List<MarginCallEntryDTO> getMarginCallEntries(String where) throws PersistenceException {
        DefaultMarginCallEntrySQL marginCallEntrySQL = new DefaultMarginCallEntrySQL();
        return marginCallEntrySQL.get(marginCallEntrySQL.getIds(where));
    }

}
