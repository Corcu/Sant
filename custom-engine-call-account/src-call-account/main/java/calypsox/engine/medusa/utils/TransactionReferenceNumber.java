package calypsox.engine.medusa.utils;

import calypsox.tk.bo.swift.SwiftUtilPublic;
import calypsox.tk.core.SantanderUtil;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.rmi.RemoteException;
import java.util.Formatter;
import java.util.Vector;

/**
 * The Class TransactionReferenceNumber.
 */
public class TransactionReferenceNumber {

    /** The Constant FX_PREFIX. */
    public static final String FX_PREFIX = "GTX";

    /** The Constant NETTED_PREFIX. */
    public static final String NETTED_PREFIX = "GTN";

    /** The Constant MM_PREFIX. */
    public static final String MM_PREFIX = "GTM";

    /**
     * Generate the TRN based on the message id and the Product. For FX products
     * the the TRN prefix is GTX. For a netted transfer the prefix is GTN
     * 
     * @param msg
     *            the msg
     * @return the trn
     * @throws RemoteException
     *             the remote exception
     */
    public static String getTRN(final BOMessage msg) throws RemoteException {
        final Trade trade = DSConnection.getDefault().getRemoteTrade()
                .getTrade(msg.getTradeLongId());
        return getTRN(msg, trade);
    }

    /**
     * Gets the trn.
     * 
     * @param msg
     *            the msg
     * @param trade
     *            the trade
     * @return the trn
     */
    // CAL_BO_140
    public static String getTRN(final BOMessage msg, final Trade trade) {
        String prefix = "";
        final long msgId = msg.getLongId();
        String POName = "";
        if (trade != null) {
            POName = trade.getBook().getLegalEntity().getAuthName();
            final String productType = trade.getProductType();
            if (("FX".equals(productType)) || ("FXSwap".equals(productType))
                    || ("FXForward".equals(productType))
                    || ("FXNDF".equals(productType))) {
                // case of a FX trade
                prefix = FX_PREFIX;
            } else if (Product.MONEYMARKETCASH.equals(productType)) {
                prefix = MM_PREFIX;
            }
        } else {
            // case of a netted transfer
            prefix = NETTED_PREFIX;
        }

        // CAL_BO_140
        String version = "00";
        if ( isTradedFX(POName)) {
            final int documentVersion = SwiftUtilPublic.getDocumentVersion(
                    DSConnection.getDefault(), msg);
            version = SantanderUtil.getInstance().mapIntToChar(documentVersion,
                    26);
        }

        final Formatter formatter = new Formatter();
        final String result = formatter.format(prefix + version + "%1$011d",
                msgId).toString();
        formatter.close();
        return result;
    }

    public static boolean isTradedFX(final String POName) {
        if ( Util.isEmpty(POName)) {
            return false;
        }
        final Vector<?> tradedFXPos = LocalCache.getDomainValues(
                DSConnection.getDefault(),
                MedusaKeywordConstantsUtil.DOMAIN_VALUE_TRADED_FX_PO);
        if (!Util.isEmpty(tradedFXPos) && tradedFXPos.contains(POName)) {
            return true;
        }
        return false;
    }
}
