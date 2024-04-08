package calypsox.tk.bo;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.CashFlowSet;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.SecFinance;
import com.calypso.tk.service.DSConnection;

import java.util.Arrays;
import java.util.Optional;
import java.util.Vector;
import java.util.stream.Collectors;

public class BORepoHandler extends com.calypso.tk.bo.BORepoHandler {

    public Vector generateTransfers(Trade trade, PricingEnv env, Vector exceptions, DSConnection dsCon, JDate today, JDate tradeDate, CashFlowSet cashFlows){
        Vector vect =super.generateTransfers(trade,env,exceptions,dsCon,today,tradeDate,cashFlows);

        if(isRepoTriparty(trade) && !Util.isEmpty(vect)){
            vect = Arrays.stream(vect.toArray()).filter(BOTransfer.class::isInstance).map(BOTransfer.class::cast)
                    .filter(this::filterXfersByType).collect(Collectors.toCollection(Vector::new));
        }

        return vect;
    }


    /**
     * Return True on BondForwardType : Cash and BondForward : true
     * @param trade
     * @return
     */
    private boolean isRepoTriparty(Trade trade){
        return Optional.ofNullable(trade).filter(t -> "Triparty".equalsIgnoreCase(t.getProductSubType())).isPresent();
    }

    /**
     * Xfer to filter add new if needed
     */
    enum XfersToFilter { SECURITY };

    /**
     * Remove all xfer by XferTypes
     * @param xfer
     * @return
     */
    private boolean filterXfersByType(BOTransfer xfer ){
        return Arrays.stream(XfersToFilter.values()).noneMatch(v -> v.name().equalsIgnoreCase(Optional.ofNullable(xfer).map(BOTransfer::getTransferType).orElse("")));
    }

    @Override
    protected void setAttributes(BOTransfer transfer, SecFinance secFinance) {
        super.setAttributes(transfer, secFinance);
        if (secFinance.isTriparty() && transfer.getValueDate().equals(secFinance.getStartDate()) && secFinance.getEndDate()!=null )  {
            transfer.setAttribute("CollateralReturnDate", Util.idateToString(secFinance.getEndDate()));
        }
    }
}
