package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.workflow.WfTransferRule;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.TransferAgent;
import com.calypso.tk.product.UnavailabilityTransfer;

import java.util.Optional;

/**
 * @author acd
 */
public interface SantUnavailabilityTransferTradeTransferRule extends WfTransferRule {

    String ATTR_UNAVAILABILITYTRANSFER_TRADE_ID = UnavailabilityTransfer.class.getSimpleName() + ".TradeId";
    String ATTR_UNAVAILABILITYTRANSFER_ORIGIN_TRADE_ID = UnavailabilityTransfer.class.getSimpleName() + ".OriginTradeId";
    String UNAVALABILTITY_REASON_BLOQUEO = "Bloqueo";
    String UNAVALABILTITY_REASON_PIGNORACION = "Pignoracion";

    default boolean isUnavailabilityTransferTradeActionValid(Trade trade, BOTransfer transfer){
        if (trade == null || transfer.getNettedTransfer() || !"SECURITY".equals(transfer.getTransferType())
                || !TransferAgent.class.getSimpleName().equals(transfer.getProductFamily())) {
            return false;
        }
        return "RECEIVE".equals(transfer.getPayReceive());
    }

    default Long getUnavailabilityTransferTradeId(BOTransfer transfer){
        return Optional.ofNullable(transfer).map(t -> t.getAttribute(ATTR_UNAVAILABILITYTRANSFER_TRADE_ID)).map(Long::parseLong).orElse(0L);
    }


}
