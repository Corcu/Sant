package calypsox.tk.event;


import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.sql.BOTransferSQL;
import com.calypso.tk.core.*;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTransfer;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Repo;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.TransferArray;

import java.util.Optional;
import java.util.Vector;


public class MedusaExcludeParentAndNettedTransferEventFilter implements EventFilter {


    private static final String EXCLUDED_VALUES_DV = "MedusaExcludeParentAndNettedTypes";


    @Override
    public boolean accept(PSEvent event) {
        Boolean res = false;
        if (event instanceof PSEventTransfer) {
            PSEventTransfer eventTransfer = (PSEventTransfer) event;
            BOTransfer xfer = eventTransfer.getBoTransfer();
            String productType = xfer.getProductType();
            Product product = Optional.ofNullable(eventTransfer.getTrade()).map(Trade::getProduct).orElse(null);
            if(isBondOrRepo(productType, product, xfer)){
                if(xfer.getNettedTransferLongId()!=0 && xfer.getParentLongId()!=0) {
                    BOTransfer parentXfer = null;
                    try {
                        parentXfer = DSConnection.getDefault().getRemoteBO().getBOTransfer(xfer.getParentLongId());
                    } catch (Exception e) {
                        Log.error("MedusaExcludeParentAndNettedTransferEventFilter", e.getMessage());
                    }
                    if(parentXfer == null) {
                        try {
                            parentXfer = BOTransferSQL.getTransfer(xfer.getParentLongId());
                        }
                        catch (Exception e) {
                            Log.error("MedusaExcludeParentAndNettedTransferEventFilter", e.getMessage());
                        }
                    }
                    if(parentXfer != null) {
                        return Status.SPLIT.equalsIgnoreCase(parentXfer.getStatus().getStatus()) ? false : true;
                    }
                }
                res = true;
            }
            else {
                res = xfer.getNettedTransferLongId()!=0 && xfer.getParentLongId()!=0 && !isNettingType(xfer.getNettingType()) ? false : true;
            }
        }
        return res;
    }


    private boolean isNettingType(String nettingType){
        Vector<String> excludedTypes = LocalCache.getDomainValues(DSConnection.getDefault(),EXCLUDED_VALUES_DV);
        return excludedTypes.contains(nettingType);
    }


    private boolean isBondOrRepo(String prodType, Product product, BOTransfer xfer) {
        if ((!Util.isEmpty(prodType) && ("Bond".equalsIgnoreCase(prodType) || "G.Bonds".equalsIgnoreCase(prodType))) || (product instanceof Bond)){
            return true;
        }
        if ((!Util.isEmpty(prodType) && "Repo".equalsIgnoreCase(prodType)) || (product instanceof Repo)){
            return true;
        }
        String underProdType = getUnderlyingXferProductType(xfer);
        if(!Util.isEmpty(underProdType)){
            if("Bond".equalsIgnoreCase(underProdType)
                    || "G.Bonds".equalsIgnoreCase(underProdType)
                    || "Repo".equalsIgnoreCase(underProdType)){
                return true;
            }
        }
        return false;
    }

    private String getUnderlyingXferProductType(BOTransfer xfer){
        TransferArray underlyingTransfers = xfer.getUnderlyingTransfers();
        if (Util.isEmpty(underlyingTransfers)) {
            try {
                underlyingTransfers = DSConnection.getDefault().getRemoteBO().getNettedTransfers(xfer.getLongId());
            } catch (Exception e) {
                Log.error(this, "Error loading Netting Transfer for BOTransfer: " + xfer.getLongId() + "Error: " + e.getMessage());
            }
        }
        if (!Util.isEmpty(underlyingTransfers)) {
            BOTransfer underXfer = underlyingTransfers.get(0);
            return underXfer.getProductType();
        }
        return "";
    }


}
