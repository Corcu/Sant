package calypsox.tk.event;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTransfer;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author acd
 */
public class MedusaTransfersEventFilter implements EventFilter {
    private static final String CPTY_ROLE = "CounterParty";
    private static final String XFERT_SECURITY = "SECURITY";
    private static final String MEDUSA = "MEDUSA";
    private static final String EXCLUDED_VALUES_DV = "MedusaExcludeParentAndNettedTypes";
    private static final String INCLUDE_PRODUCT = "MedusaIncludeTempProducts";
    private static final String ACCEPTED_EVENT_TYPES = "MedusaAcceptedEventTypes";
    private static final String XDELIVERY_TYPE_DAP = "DAP";
    private static final String XDELIVERY_TYPE_DFP = "DFP";
    private static final String BOND_PRODUCT_GROUP = "G.Bonds";

    @Override
    public boolean accept(PSEvent event) {
        if(event instanceof PSEventTransfer){
            return acceptEvent((PSEventTransfer)event);
        }
        return false;
    }

    private boolean acceptEvent(PSEventTransfer eventTransfer){
        final String productType = getProductTypeFromEvent(eventTransfer);

        return  acceptedEventTypes(eventTransfer)
                && (acceptMarginCall(productType, eventTransfer)
                || acceptPerformanceSwap(productType,eventTransfer)
                || acceptSecLending(productType,eventTransfer)
                || acceptCustomerTransfer(productType, eventTransfer)
                || acceptRepo(productType, eventTransfer)
                || acceptBond(productType,eventTransfer)
                || acceptEquity(productType,eventTransfer)
                || acceptCallAccount(productType,eventTransfer)
                || acceptSimpleTransferPenalty(productType,eventTransfer)
                //Special Accepts
                || acceptPenaltyNetting(eventTransfer)
                || acceptAnyCustomProduct(productType,eventTransfer));
    }

    private boolean acceptedEventTypes(PSEventTransfer eventTransfer){
        String eventType = eventTransfer.getEventType();
        List<String> values = DomainValues.values(ACCEPTED_EVENT_TYPES);
        if(!Util.isEmpty(values)){
            return values.stream().anyMatch(eventType::equalsIgnoreCase);
        }
        return true;
    }

    private boolean acceptMarginCall(String productType,PSEventTransfer eventTransfer){
        boolean res = false;
        if(Product.MARGINCALL.equalsIgnoreCase(productType) && acceptedNettingTypes(eventTransfer.getBoTransfer()) && commonAccepts(eventTransfer)){
            final MarginCallConfig marginCallConfig = Optional.ofNullable(eventTransfer.getTrade()).map(Trade::getProduct)
                    .filter(MarginCall.class::isInstance)
                    .map(MarginCall.class::cast)
                    .map(MarginCall::getMarginCallConfig)
                    .orElse(null);
            if (null != marginCallConfig) {
                JDate contractValDate = Optional.ofNullable(marginCallConfig.getAdditionalField(MEDUSA)).map(JDate::valueOf).orElse(null);
                JDate settleDate = eventTransfer.getBoTransfer().getSettleDate();
                res = settleDate.after(contractValDate) || settleDate.equals(contractValDate);
            }
        }
        return res;
    }

    private boolean acceptPerformanceSwap(String productType,PSEventTransfer eventTransfer){
        return Product.PERFORMANCESWAP.equalsIgnoreCase(productType) && acceptedNettingTypes(eventTransfer.getBoTransfer()) && commonAccepts(eventTransfer);
    }
    private boolean acceptSecLending(String productType,PSEventTransfer eventTransfer){
        return Product.SEC_LENDING.equalsIgnoreCase(productType) && acceptedNettingTypes(eventTransfer.getBoTransfer()) && commonAccepts(eventTransfer);
    }

    private boolean acceptCustomerTransfer(String productType, PSEventTransfer eventTransfer) {
        if(Product.CUSTOMERTRANSFER.equalsIgnoreCase(productType) && acceptedNettingTypes(eventTransfer.getBoTransfer())){
            final BOTransfer xfer = eventTransfer.getBoTransfer();
            final int entityId = Optional.ofNullable(xfer).map(BOTransfer::getProcessingOrg).orElse(0);
            boolean isbste = false;
            try {
                isbste = Optional.ofNullable(BOCache.getLegalEntity(DSConnection.getDefault(),entityId)).map(LegalEntity::getCode).filter("BSTE"::equalsIgnoreCase).isPresent();
            } catch (Exception e) {
                Log.error(this.getClass().getSimpleName(),"Error loading LegalEntity: " + entityId + " Error: " + e.getMessage());
            }
            return isbste && Stream.of("TARGET2", "SWIFT").anyMatch(value -> value.equalsIgnoreCase(Optional.ofNullable(xfer).map(BOTransfer::getSettlementMethod).orElse("")));
        }
        return false;
    }

    private boolean acceptRepo(String productType, PSEventTransfer eventTransfer){
        return Product.REPO.equalsIgnoreCase(productType)
                && acceptTransferRoles(eventTransfer)
                && checkDapDfp(eventTransfer.getBoTransfer())
                && checkSplitParentTransfer(eventTransfer.getBoTransfer());
    }

    private boolean acceptBond(String productType, PSEventTransfer eventTransfer){
        return (Product.BOND.equalsIgnoreCase(productType)
                || BOND_PRODUCT_GROUP.equalsIgnoreCase(productType))
                && checkDapDfp(eventTransfer.getBoTransfer())
                && checkSplitParentTransfer(eventTransfer.getBoTransfer());
    }
    private boolean acceptEquity(String productType,PSEventTransfer eventTransfer){
        if(Product.EQUITY.equalsIgnoreCase(productType) && acceptedNettingTypes(eventTransfer.getBoTransfer()) && checkDapDfp(eventTransfer.getBoTransfer())){
            boolean isMigrated = Optional.ofNullable(eventTransfer.getBoTransfer())
                    .map(BOTransfer::getStatus).map(Status::getStatus)
                    .filter("MIGRATED"::equalsIgnoreCase).isPresent();
            return !isMigrated;
        }
        return false;
    }

    private boolean acceptCallAccount(String productType,PSEventTransfer eventTransfer){
        if(Product.CALL_ACCOUNT.equalsIgnoreCase(productType) && acceptedNettingTypes(eventTransfer.getBoTransfer())){
            BOTransfer boTransfer = eventTransfer.getBoTransfer();
            Trade trade = eventTransfer.getTrade();
            String role = "";
            if (trade != null) {
                role = trade.getRole();
            }
            String xferType = boTransfer.getTransferType();
            String xferStatus = boTransfer.getStatus().getStatus();
            Product product = Optional.ofNullable(eventTransfer.getTrade()).map(Trade::getProduct).orElse(null);
            Product underlyingProduct = Optional.ofNullable(product).map(Product::getUnderlyingProduct).orElse(null);
            return underlyingProduct instanceof Equity && "SETTLED".equalsIgnoreCase(xferStatus) && !"SECURITY".equalsIgnoreCase(xferType) && ("CounterParty".equalsIgnoreCase(role) || "Agent".equalsIgnoreCase(role));
        }
        return false;
    }

    private boolean acceptPenaltyNetting(PSEventTransfer eventTransfer){
        if(commonAccepts(eventTransfer) && acceptedNettingTypes(eventTransfer.getBoTransfer())){
            final BOTransfer xfer = eventTransfer.getBoTransfer();
            boolean isPenaltyNetting = Optional.ofNullable(xfer).map(BOTransfer::getNettingType).map("Penalty"::equalsIgnoreCase).orElse(false);
            boolean isNotUnderlying = Optional.ofNullable(xfer).map(BOTransfer::getNettedTransfer).orElse(false);
            boolean isVerifiedEvent = eventTransfer.getEventType().contains("VERIFIED");
            return isPenaltyNetting && isVerifiedEvent && isNotUnderlying;
        }
        return false;
    }

    private boolean acceptAnyCustomProduct(String productType, PSEventTransfer eventTransfer){
        return acceptedNettingTypes(eventTransfer.getBoTransfer()) && acceptProductFromDV(productType);
    }

    private boolean checkSplitParentTransfer(BOTransfer xfer){
        if(xfer.getNettedTransferLongId()!=0 && xfer.getParentLongId()!=0) {
            try {

                return !Optional.ofNullable(DSConnection.getDefault().getRemoteBO().getBOTransfer(xfer.getParentLongId()))
                        .map(BOTransfer::getStatus)
                        .map(Status::getStatus)
                        .filter(Status.SPLIT::equalsIgnoreCase).isPresent();
            } catch (Exception e) {
                Log.error(this.getClass().getSimpleName(), e.getMessage());
            }
        }
        return true;
    }

    private String getProductTypeFromEvent(PSEventTransfer eventTransfer){

        Trade trade = eventTransfer.getTrade();
        if(null==trade){
            long tradeLongId = Optional.of(eventTransfer).map(PSEventTransfer::getBoTransfer).map(BOTransfer::getTradeLongId).orElse(0L);
            try {
                if(tradeLongId>0){
                    trade = DSConnection.getDefault().getRemoteTrade().getTrade(tradeLongId);
                }
            } catch (Exception e) {
                Log.error(this.getClass().getSimpleName(),"Error loading trade: " + tradeLongId + " Error: " + e.getMessage());
            }
        }

        if(null!=trade){
            return Optional.ofNullable(trade.getProduct()).map(Product::getType).orElse("");
        }

        return getProductTypeFromUnderlyingXfers(eventTransfer.getBoTransfer());
    }

    /**
     * Get product Type for some Netting Xfers
     * Las xfer pueden tener productos distintos, este method no es fiable 100% para indentificar el producto del xfer.
     * En caso de cross product es necesario definir que producto coger.
     * @param xfer @{@link BOTransfer}
     * @return Product type of the BOTransfer event.
     */
    private String getProductTypeFromUnderlyingXfers(BOTransfer xfer){
        if(null!=xfer){
            TransferArray underlyingTransfers = xfer.getUnderlyingTransfers();
            if (Util.isEmpty(underlyingTransfers)) {
                try {
                    underlyingTransfers = DSConnection.getDefault().getRemoteBO().getNettedTransfers(xfer.getLongId());
                } catch (Exception e) {
                    Log.error(this.getClass().getSimpleName(),"Error loading netted xfers: " + xfer.getLongId() + " Error: " + e.getMessage());
                }
            }
            if (!Util.isEmpty(underlyingTransfers)) {
                BOTransfer underXfer = underlyingTransfers.get(0);
                return underXfer.getProductType();
            }
        }
        return "";
    }

    private boolean acceptSimpleTransferPenalty(String productType, PSEventTransfer eventTransfer) {
       if(Product.SIMPLETRANSFER.equalsIgnoreCase(productType) && acceptedNettingTypes(eventTransfer.getBoTransfer())){
           return Optional.ofNullable(eventTransfer.getBoTransfer()).map(xfer -> "PENALTY".equalsIgnoreCase(xfer.getTransferType()))
                   .orElse(false);
       }
        return false;
    }

    private boolean isExcludeNetting(String nettingType){
        return DomainValues.values(EXCLUDED_VALUES_DV).stream().anyMatch(nettingType::equalsIgnoreCase);
    }

    private boolean acceptProductFromDV(String productType){
        return DomainValues.values(INCLUDE_PRODUCT).stream().anyMatch(productType::equalsIgnoreCase);
    }

    private boolean commonAccepts(PSEventTransfer eventTransfer){
        return acceptTransferRoles(eventTransfer) && acceptTransferTypes(eventTransfer);
    }

    private boolean acceptTransferRoles(PSEventTransfer eventTransfer) {
        BOTransfer xfer = eventTransfer.getBoTransfer();
        return xfer != null && CPTY_ROLE.equalsIgnoreCase(xfer.getExternalRole());
    }
    private boolean acceptTransferTypes(PSEventTransfer eventTransfer) {
        BOTransfer xfer = eventTransfer.getBoTransfer();
        return xfer != null && !XFERT_SECURITY.equalsIgnoreCase(xfer.getTransferType());
    }
    private boolean acceptedNettingTypes(BOTransfer xfer){
        return null!=xfer && (xfer.getNettedTransferLongId() == 0 || xfer.getParentLongId() == 0 || isExcludeNetting(xfer.getNettingType()));
    }

    private boolean checkDapDfp(BOTransfer xfer){
        if (XFERT_SECURITY.equalsIgnoreCase(xfer.getTransferType()) &&
                XDELIVERY_TYPE_DAP.equalsIgnoreCase(xfer.getDeliveryType())) {
            return true;
        } else if (!XFERT_SECURITY.equalsIgnoreCase(xfer.getTransferType()) &&
                XDELIVERY_TYPE_DFP.equalsIgnoreCase(xfer.getDeliveryType())) {
            return true;
        }
        return false;
    }

}
