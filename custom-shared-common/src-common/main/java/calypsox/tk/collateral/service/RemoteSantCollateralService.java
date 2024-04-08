package calypsox.tk.collateral.service;

import calypsox.util.InterfacePLMarkBean;
import calypsox.util.InterfaceTradeAndPLMarks;
import calypsox.util.TradeImportStatus;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.PersistenceException;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.CalypsoMonitorableServer;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Custom remote services
 *
 * @author aela
 */

/**
 * @author aela
 */
public interface RemoteSantCollateralService extends CalypsoMonitorableServer {

    public static final String SERVER_NAME = "SantCollateralService";

    /**
     * @param trade   trade to save
     * @param plmarks pl marks to save with the trade
     * @return error messages if any
     * @throws RemoteException
     */
    public long saveTradeWithPLMarks(Trade trade, InterfacePLMarkBean plMark1, InterfacePLMarkBean plMark2,
                                     InterfacePLMarkBean plMarkIA1, InterfacePLMarkBean plMarkIA2, List<TradeImportStatus> errors)
            throws RemoteException;

    /**
     * @param trade
     * @param plMark1
     * @param plMark2
     * @param plMarkIA1
     * @param plMarkIA2
     * @return
     * @throws RemoteException
     */
    public List<TradeImportStatus> saveTradeWithPLMarks(Trade trade, InterfacePLMarkBean plMark1,
                                                        InterfacePLMarkBean plMark2, InterfacePLMarkBean plMarkIA1, InterfacePLMarkBean plMarkIA2)
            throws RemoteException;

    /**
     * @param trade  trade to save
     * @param errors list if errors if any
     * @return the save trade
     * @throws RemoteException
     */
    public Trade saveTrade(Trade trade, List<String> errors) throws RemoteException;

    /**
     * @param trade  trade for which plmarks should be saved
     * @param plMark plMark to save
     * @param errors list if errors if any
     * @return the saved trade
     * @throws RemoteException
     */
    public Trade saveTradeWithPLMarks(Trade trade, PLMark plMark, List<String> errors) throws RemoteException;

    /**
     * @param tradesToSave
     * @return
     * @throws RemoteException
     */
    public Map<Integer, List<TradeImportStatus>> saveTradesWithPLMarks(List<InterfaceTradeAndPLMarks> tradesToSave)
            throws RemoteException;

    /**
     * @param trade  trade for which plmarks should be saved
     * @param plMark plMark to save
     * @param errors list if errors if any
     * @return the saved trade
     * @throws RemoteException
     */
    public void clearPricingEnvCache() throws RemoteException;

    /**
     * This locking will be used to synchronize the creation of an IA trade after a contract modification
     *
     * @param mccId contract id to lock
     * @throws RemoteException
     */
    public void acquireLockOnContract(int mccId) throws RemoteException;

    /**
     * Release the already acquired lock
     *
     * @param mccId contract id to unlock
     * @throws RemoteException
     */
    public void releaseLockOnContract(int mccId) throws RemoteException;

    /**
     * Save Sec codes of products. The aim of this method is to have a batch update of sec codes.
     *
     * @param products the products to update
     * @param secCode  the sec code concerned by the deletion
     * @throws RemoteException technical error
     */
    public void clearSecCodesBatch(Vector<Product> products, String secCode) throws RemoteException;

    /**
     * The aim of this method is to have a batch update of products.
     *
     * @param products the products to update
     * @throws RemoteException technical error
     */
    public void updateBatch(Vector<Product> products) throws RemoteException;

    /**
     * @param trade
     * @param plMark1
     * @param plMark2
     * @param plMarkIA1
     * @param plMarkIA2
     * @param plMarkClosingPrice1
     * @param plMarkNpv1
     * @return
     * @throws RemoteException
     */
    public List<TradeImportStatus> saveTradeWithPLMarks(Trade trade, InterfacePLMarkBean plMark1,
                                                        InterfacePLMarkBean plMark2, InterfacePLMarkBean plMarkIA1, InterfacePLMarkBean plMarkIA2,
                                                        InterfacePLMarkBean plMarkClosingPrice1, InterfacePLMarkBean plMarkNpv1) throws RemoteException;


    /**
     * @param @HashMap<String, String> additionalFields
     * @return
     * @throws PersistenceException
     */
    public List<CollateralConfig> getMarginCallConfigByAdditionalField(HashMap<String, String> additionalFields) throws PersistenceException;


    /**
     * @param where
     * @return
     * @throws PersistenceException
     */
    public List<Integer> getMarginCallEntryIds(String where) throws PersistenceException;

    /**
     * @param where
     * @return
     * @throws PersistenceException
     */
    public List<MarginCallEntryDTO> getMarginCallEntries(String where) throws PersistenceException;


}
