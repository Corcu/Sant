package calypsox.util;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.mo.TradeFilterCriterion;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.BondAssetBacked;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;
import com.calypso.tk.util.TransferArray;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Common util class
 */
public class SantCalypsoUtilities {

    /**
     * @return
     */
    public static synchronized SantCalypsoUtilities getInstance() {
        return new SantCalypsoUtilities();
    }

    private SantCalypsoUtilities() {
    }

    /**
     * Asynchronous processing to be able to get Bonds and AssetBackedBonds at the same time
     *
     * @param from
     * @param where
     * @param bindVariables
     * @return
     * @throws ExecutionException
     */
    public static Vector<Bond> getBondAndBondAssetBackedProducts(String from, String where, List<CalypsoBindVariable> bindVariables) throws ExecutionException {
        CompletableFuture<Vector<Bond>> bondsFuture = CompletableFuture.supplyAsync(createBondGetterSupplier(Product.BOND, from, where, bindVariables));
        CompletableFuture<Vector<Bond>> assetBackedBondFuture = CompletableFuture.supplyAsync(createBondGetterSupplier(BondAssetBacked.class.getSimpleName(), from, where, bindVariables));
        CompletableFuture<Vector<Bond>> allBondsFuture = bondsFuture.thenCombine(assetBackedBondFuture, SantCalypsoUtilities::mergeVector);

        Vector<Bond> bonds = new Vector<>();
        try {
            bonds = allBondsFuture.get();
        } catch (InterruptedException exc) {
            Thread.currentThread().interrupt();
            throw new ExecutionException(exc);
        }
        return bonds;
    }

    /**
     * @param bondType
     * @param from
     * @param where
     * @param bindVariables
     * @return Supplier class to specify the processing to be done by a CompletableFuture
     */
    private static Supplier<Vector<Bond>> createBondGetterSupplier(String bondType, String from, String where, List<CalypsoBindVariable> bindVariables) {
        return () -> {
            Vector<Bond> bonds = new Vector<>();
            try {
                bonds = DSConnection.getDefault().getRemoteProduct().getProducts(bondType, from, where, true, bindVariables);
            } catch (CalypsoServiceException exc) {
                throw new CompletionException(exc);
            }
            return bonds;
        };
    }

    /**
     * @param bonds
     * @param assetBacked
     * @return
     */
    private static Vector<Bond> mergeVector(Vector<Bond> bonds, Vector<Bond> assetBacked) {
        Vector<Bond> mergedVector = new Vector<>();
        mergedVector.addAll(bonds);
        mergedVector.addAll(assetBacked);
        return mergedVector;
    }


    //TODO comprobar si existe una mejor forma
    public static BOTransfer getTransferById(final long transferId)
            throws RemoteException {
        final TransferArray transfers = DSConnection.getDefault().getRemoteBO()
                .getTransfers(null, "bo_transfer.transfer_id=" + transferId, null);
        if (!Util.isEmpty(transfers)) {
            return transfers.get(0);
        }
        return null;
    }

    /**
     * @param tradeIdsVector
     * @return
     * @throws CalypsoServiceException
     */
    public TradeArray getTradesWithTradeFilter(Vector<String> tradeIdsVector) throws CalypsoServiceException {
        TradeArray result = new TradeArray();
        if (!Util.isEmpty(tradeIdsVector)) {
            TradeFilter tradeFilter = buildTradeIdTradeFilter(tradeIdsVector);
            result = DSConnection.getDefault().getRemoteTrade().getTrades(tradeFilter, null);
        }
        return result;
    }

    /**
     * @param tradeIdArray
     * @return
     * @throws CalypsoServiceException
     */
    public TradeArray getTradesWithTradeFilter(long[] tradeIdArray) throws CalypsoServiceException {
        Vector<String> tradeIdsVector = new Vector<>();
        if (tradeIdArray != null && tradeIdArray.length > 0) {
            tradeIdsVector = new Vector(Arrays.stream(tradeIdArray).mapToObj(l -> ((Long) l).toString()).collect(Collectors.toList()));
        }
        return getTradesWithTradeFilter(tradeIdsVector);

    }

    /**
     * @param tradeIdsVector
     * @return
     */
    private TradeFilter buildTradeIdTradeFilter(Vector<String> tradeIdsVector) {
        TradeFilter tradeFilter = new TradeFilter();
        tradeFilter.setName(this.getClass().getSimpleName());
        TradeFilterCriterion tIdList = new TradeFilterCriterion("TRADE_ID_LIST");
        tIdList.setValues(tradeIdsVector);
        tIdList.setIsInB(true);
        tradeFilter.addCriterion(tIdList);
        return tradeFilter;
    }
}
