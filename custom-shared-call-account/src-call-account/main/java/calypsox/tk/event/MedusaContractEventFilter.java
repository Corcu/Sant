package calypsox.tk.event;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTransfer;
import com.calypso.tk.product.*;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

import java.util.Optional;

/**
 * @author acd
 */
public class MedusaContractEventFilter implements EventFilter {

    private static final String MEDUSA = "MEDUSA";
    private static final String TRUE = "True";

    /**
     * @param event
     * @return
     */
    @Override
    public boolean accept(PSEvent event) {
        boolean res = false;
        if (event instanceof PSEventTransfer) {
            res = acceptPSEventTranfer((PSEventTransfer) event);
        }
        return res;
    }

    /**
     * @param eventTransfer
     * @return
     */
    private boolean acceptPSEventTranfer(PSEventTransfer eventTransfer) {
        Trade trade = eventTransfer.getTrade();
        boolean res = false;
        if (null != trade && trade.getProduct() instanceof MarginCall) {
            final MarginCallConfig marginCallConfig = ((MarginCall) trade.getProduct()).getMarginCallConfig();
            if (null != marginCallConfig) {
                String additionalField = marginCallConfig.getAdditionalField(MEDUSA);
                JDate contractValDate = JDate.valueOf(additionalField);
                JDate settleDate = eventTransfer.getBoTransfer().getSettleDate();
                res = settleDate.after(contractValDate) || settleDate.equals(contractValDate);
            }
        } else {
            res = acceptPerformanceSwap(eventTransfer) || acceptSeclending(eventTransfer) || acceptRepo(eventTransfer)
                    || acceptEquity(eventTransfer) || acceptBondOrRepo(eventTransfer) || acceptCA(eventTransfer)
                    || acceptCustomerTransfer(eventTransfer) || acceptPenaltyNetting(eventTransfer);
        }
        return res;
    }


    private boolean acceptPerformanceSwap(PSEventTransfer eventTransfer) {
        Product product = Optional.ofNullable(eventTransfer.getTrade()).map(Trade::getProduct).orElse(null);
        return product instanceof PerformanceSwap;
    }

    private boolean acceptSeclending(PSEventTransfer eventTransfer) {
        Product product = Optional.ofNullable(eventTransfer.getTrade()).map(Trade::getProduct).orElse(null);
        return product instanceof SecLending;
    }

    private boolean acceptRepo(PSEventTransfer eventTransfer) {
        Product product = Optional.ofNullable(eventTransfer.getTrade()).map(Trade::getProduct).orElse(null);
        return product instanceof Repo;
    }

    private boolean acceptEquity(PSEventTransfer eventTransfer) {
        Product product = Optional.ofNullable(eventTransfer.getTrade()).map(Trade::getProduct).orElse(null);
        BOTransfer xfer = eventTransfer.getBoTransfer();
        String productType = xfer.getProductType();
        boolean migrated = xfer!=null && "MIGRATED".equalsIgnoreCase(xfer.getStatus().getStatus()) ? true : false ;
        //return (product instanceof Equity) && !migrated;
        return !Util.isEmpty(productType) && "Equity".equalsIgnoreCase(productType) && !migrated;
    }

    // Filtra para los underliying de tipo Equity.
    // En algun momento habra que filtrar los titulos y comprobar los netos
    private boolean acceptCA(PSEventTransfer eventTransfer) {
        BOTransfer xfer = eventTransfer.getBoTransfer();
        if(xfer == null){
            return false;
        }
        // Netos
        Trade trade = eventTransfer.getTrade();
        if(xfer.getTradeLongId()==0){
            return false;
        }
        String role = "";
        if(trade != null){
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
                ("CounterParty".equalsIgnoreCase(role) || ("Agent".equalsIgnoreCase(role) && ("CA_BOOK".equalsIgnoreCase(book) || "CA_BOOK_SLB".equalsIgnoreCase(book))));
    }

    private boolean acceptCustomerTransfer(PSEventTransfer eventTransfer) {
        Product product = Optional.ofNullable(eventTransfer.getTrade()).map(Trade::getProduct).orElse(null);
        return product instanceof CustomerTransfer;
    }

    private boolean acceptPenaltyNetting(PSEventTransfer eventTransfer) {
        BOTransfer xfer = eventTransfer.getBoTransfer();
        boolean isPenaltyNetting=Optional.ofNullable(xfer).map(BOTransfer::getNettingType).map("Penalty"::equalsIgnoreCase).orElse(false);
        boolean isNotUnderlying=Optional.ofNullable(xfer).map(BOTransfer::getNettedTransfer).orElse(false);
        boolean isVerifiedEvent=eventTransfer.getEventType().contains("VERIFIED");
        return isPenaltyNetting && isVerifiedEvent && isNotUnderlying;
    }

    private boolean acceptBondOrRepo(PSEventTransfer eventTransfer) {
        BOTransfer xfer = eventTransfer.getBoTransfer();
        if(xfer == null){
            return false;
        }
        String prodType = xfer.getProductType();
        Product product = Optional.ofNullable(eventTransfer.getTrade()).map(Trade::getProduct).orElse(null);

        if ((!Util.isEmpty(prodType) && ("Bond".equalsIgnoreCase(prodType) || "G.Bonds".equalsIgnoreCase(prodType))) || (product instanceof Bond)){
            return true;
        }
        if ((!Util.isEmpty(prodType) && "Repo".equalsIgnoreCase(prodType)) || (product instanceof Repo)){
            return true;
        }
        String underProdType = getUnderlyingXferProductType(xfer);
        if(!Util.isEmpty(underProdType)){
            if( "Bond".equalsIgnoreCase(underProdType)
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
