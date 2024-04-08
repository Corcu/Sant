package calypsox.tk.bo.accounting;

import java.util.List;
import java.util.stream.Collectors;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.AccountingBook;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.product.CA;
import com.calypso.tk.product.CorporateActionHandlerUtil;
import com.calypso.tk.refdata.AccountingEventConfig;
import com.calypso.tk.refdata.AccountingRule;
import com.calypso.tk.refdata.WithholdingTaxConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteBackOffice;
import com.calypso.tk.util.TransferArray;

/**
 * Created by dmenendd on 27/10/2021.
 */
public class CACreHandler extends com.calypso.tk.bo.accounting.CACreHandler{

    @Override
    public void fillAttributes(BOCre cre, Trade trade, PSEvent event, AccountingEventConfig eventConfig, AccountingRule rule, AccountingBook accoundintBook){
        super.fillAttributes(cre, trade, event, eventConfig, rule, accoundintBook);
        if (cre.getEventType().equals("NET_WITHHOLDINGTAX")) {
            customGenerateWithholdingTaxAccounting(WhtAccEvent.NET_WITHHOLDINGTAX, trade, cre, eventConfig);
        } else if(cre.getEventType().equals("RECLAIM_TAX")){
            customGenerateWithholdingTaxAccounting(WhtAccEvent.RECLAIM_TAX, trade, cre, eventConfig);
        }
		if ("NONE".equals(cre.getDescription())) {
			updateCreDescNetted(cre);
		}
    }

    private void customGenerateWithholdingTaxAccounting(WhtAccEvent accEventType, Trade trade, BOCre cre, AccountingEventConfig eventConfig) {
        CA ca = CorporateActionHandlerUtil.getCA(trade);
        double amount = ca.calcGrossAmount(trade);

        cre.setAmount(0,0);
        cre.setDescription(eventConfig.getDescription());

        WithholdingTaxConfig wtc = null;
        wtc = BOCache.getWithholdingTaxConfig(DSConnection.getDefault(), (int)trade.getKeywordAsLongId("WithholdingTaxConfigId"));

        if ((accEventType == WhtAccEvent.RECLAIM_TAX) && (wtc == null || wtc != null && !wtc.hasReclaimRate())) {
            return;
        }

        if ((accEventType == WhtAccEvent.NET_WITHHOLDINGTAX) && (wtc == null)){
            return;
        }

        switch(accEventType) {
            case RECLAIM_TAX:
                amount *= wtc.getReclaimRate();
                amount = WithholdingTaxConfig.roundWithholdingTaxAmount(amount, wtc);
                break;
            case NET_WITHHOLDINGTAX:
                amount *= wtc.getWHTRate() - wtc.getReclaimRate();
                amount = WithholdingTaxConfig.roundWithholdingTaxAmount(amount, wtc);
                break;
        }

        cre.setAmount(0,amount);
    }

    private static enum WhtAccEvent {
        RECLAIM_TAX,
        NET_WITHHOLDINGTAX;

        private WhtAccEvent() {
        }
    }
    
    /**
     * Updates CRE description on Settled Transfers compound by COUPON or AMORTIZATION
     * @param cre
     */
	private void updateCreDescNetted(BOCre cre) {
		try {
			RemoteBackOffice rbo = DSConnection.getDefault().getRemoteBackOffice();
			BOTransfer transfer = rbo.getBOTransfer(cre.getTransferLongId());
			if (transfer != null && transfer.getNettedTransfer()) {
				TransferArray ta = rbo.getNettedTransfers(cre.getTransferLongId());
				if (ta != null) {
					List<String> lTypes = ta.stream().map(t -> t.getTransferType()).distinct()
							.collect(Collectors.toList());
					if (lTypes.size() == 1 && "AMORTIZATION".equals(lTypes.get(0))) {
						cre.setDescription("REDEMPTION");
					} else if (lTypes.stream().filter(ty -> "AMORTIZATION".equals(ty) ||"INTEREST".equals(ty))
							.findFirst().isPresent()) {
						cre.setDescription("CUPON");
					}
				}
			}
		} catch (CalypsoServiceException e) {
			Log.error(this, "Unable to get transfer from Transfer ID: " + cre.getTransferLongId());
		}
	}

}
