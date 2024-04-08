package calypsox.tk.event;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTransfer;
import com.calypso.tk.product.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author aalonsop
 */
public class MedusaBOTransferEventFilter implements EventFilter {

    private static final String CPTY_ROLE = "CounterParty";
    private static final String XFERT_SECURITY = "SECURITY";
    private static final String XDELIVERY_TYPE_DAP = "DAP";
    private static final String XDELIVERY_TYPE_DFP = "DFP";


    /**
     * @param event
     * @return true if accepted
     */
    @Override
    public boolean accept(PSEvent event) {
        boolean res = false;
        if (event instanceof PSEventTransfer) {
            if (isCustomerTransferType((PSEventTransfer) event)) {
                res = acceptCustomerTransfer((PSEventTransfer) event);
            } else {
                res = (acceptPSEventTranfer((PSEventTransfer) event) && acceptPSEventTranferType((PSEventTransfer) event))
                        || acceptPSEventRepoOrEquityOrBond((PSEventTransfer) event) || acceptCA((PSEventTransfer) event)
                        || acceptSimpleXferPenalty((PSEventTransfer) event);
            }
        }
        return res;
    }

    /**
     * @param eventTransfer
     * @return true if xfer's xternal role is CPTY
     */
    private boolean acceptPSEventTranfer(PSEventTransfer eventTransfer) {
        BOTransfer xfer = eventTransfer.getBoTransfer();
        return xfer != null && CPTY_ROLE.equalsIgnoreCase(xfer.getExternalRole());
    }

    private boolean acceptPSEventTranferType(PSEventTransfer eventTransfer) {
        BOTransfer xfer = eventTransfer.getBoTransfer();
        return xfer != null && !XFERT_SECURITY.equalsIgnoreCase(xfer.getTransferType());
    }

    private boolean acceptPSEventRepoOrEquityOrBond(PSEventTransfer eventTransfer) {
        BOTransfer xfer = eventTransfer.getBoTransfer();
        String productType = xfer.getProductType();
        Product product = Optional.ofNullable(eventTransfer.getTrade()).map(Trade::getProduct).orElse(null);

        if ("Equity".equalsIgnoreCase(productType) || (isRepo(productType, product, xfer) && acceptPSEventTranfer(eventTransfer)) || isBond(productType, product, xfer)) {

            if (XFERT_SECURITY.equalsIgnoreCase(xfer.getTransferType()) &&
                    XDELIVERY_TYPE_DAP.equalsIgnoreCase(xfer.getDeliveryType())) {
                return true;
            } else if (!XFERT_SECURITY.equalsIgnoreCase(xfer.getTransferType()) &&
                    XDELIVERY_TYPE_DFP.equalsIgnoreCase(xfer.getDeliveryType())) {
                return true;
            }
        }
        return false;
    }


    // Filtra para los underliying de tipo Equity.
    // En algun momento habra que filtrar los titulos y comprobar los netos
    private boolean acceptCA(PSEventTransfer eventTransfer) {
        BOTransfer xfer = eventTransfer.getBoTransfer();
        if (xfer == null) {
            return false;
        }
        // Netos
        Trade trade = eventTransfer.getTrade();
        if (xfer.getTradeLongId() == 0) {
            return false;
        }
        String role = "";
        if (trade != null) {
            role = trade.getRole();
        }
        String book = "";
        if (trade != null) {
            book = trade.getBook().getName();
        }
        String xferType = xfer.getTransferType();
        String xferStatus = xfer.getStatus().getStatus();
        Product product = Optional.ofNullable(eventTransfer.getTrade()).map(Trade::getProduct).orElse(null);
        Product underlyingProduct = Optional.ofNullable(product).map(Product::getUnderlyingProduct).orElse(null);
        String alreadySettled = xfer.getAttribute("alreadySettled");
        return product instanceof CA && (underlyingProduct instanceof Equity || underlyingProduct instanceof Bond || underlyingProduct instanceof BondMMDiscount || underlyingProduct instanceof BondMMInterest) &&
                !"SECURITY".equalsIgnoreCase(xferType) && !"WRITE_OFF".equalsIgnoreCase(xferType) &&
                ("SETTLED".equalsIgnoreCase(xferStatus) || ("CANCELED".equalsIgnoreCase(xferStatus) && !Util.isEmpty(alreadySettled) && "true".equalsIgnoreCase(alreadySettled))) &&
                ("CounterParty".equalsIgnoreCase(role) || ("Agent".equalsIgnoreCase(role) && ("CA_BOOK".equalsIgnoreCase(book) || "CA_BOOK_SLB".equalsIgnoreCase(book)))) ;
    }

    private boolean isCustomerTransferType(PSEventTransfer eventTransfer) {
        Product product = Optional.ofNullable(eventTransfer.getTrade()).map(Trade::getProduct).orElse(null);
        return product instanceof CustomerTransfer;
    }

    private boolean acceptCustomerTransfer(PSEventTransfer eventTransfer) {
        BOTransfer xfer = eventTransfer.getBoTransfer();
        LegalEntity po = Optional.of(BOCache.getLegalEntity(DSConnection.getDefault(), xfer.getProcessingOrg()))
                .filter(s -> s.getRoleList().contains("ProcessingOrg"))
                .filter(s -> s.getCode().equals("BSTE"))
                .orElse(null);
        return Arrays.asList("TARGET2", "SWIFT").contains(xfer.getSettlementMethod()) && null != po;
    }

    private boolean acceptSimpleXferPenalty(PSEventTransfer eventTransfer) {
        boolean isSimpleXfer = Optional.ofNullable(eventTransfer.getBoTransfer())
                .map(xfer -> SimpleTransfer.class.getSimpleName().equalsIgnoreCase(xfer.getProductType())).orElse(false);
        boolean isPenaltyType = Optional.ofNullable(eventTransfer.getBoTransfer()).map(xfer -> "PENALTY".equalsIgnoreCase(xfer.getTransferType()))
                .orElse(false);
        return isSimpleXfer && isPenaltyType;

    }

    private boolean isBond(String prodType, Product product, BOTransfer xfer) {
        if ((!Util.isEmpty(prodType) && ("Bond".equalsIgnoreCase(prodType) || "G.Bonds".equalsIgnoreCase(prodType))) || (product instanceof Bond)){
            return true;
        }
        String underProdType = getUnderlyingXferProductType(xfer);
        if(!Util.isEmpty(underProdType)){
            if( "Bond".equalsIgnoreCase(underProdType)
                    || "G.Bonds".equalsIgnoreCase(underProdType)){
                return true;
            }
        }
        return false;
    }

    private boolean isRepo(String prodType, Product product, BOTransfer xfer) {
        if ((!Util.isEmpty(prodType) && "Repo".equalsIgnoreCase(prodType)) || (product instanceof Repo)){
            return true;
        }
        String underProdType = getUnderlyingXferProductType(xfer);
        if(!Util.isEmpty(underProdType)){
            if("Repo".equalsIgnoreCase(underProdType)){
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