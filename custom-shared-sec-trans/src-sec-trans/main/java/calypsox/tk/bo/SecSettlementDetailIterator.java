package calypsox.tk.bo;

import com.calypso.tk.bo.*;
import com.calypso.tk.bo.swift.SwiftAgent;
import com.calypso.tk.bo.swift.SwiftUtil;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.ManualSDI;
import com.calypso.tk.refdata.PartySDIInfo;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

@SuppressWarnings("unused")
public class SecSettlementDetailIterator extends SettlementDetailIterator {

    private BOTransfer transfer;
    private DSConnection dsCon;
    private Trade trade;


    public void init(BOMessage message, Trade trade, LEContact sender, LEContact receiver, BOTransfer transfer, Vector transferRules, PricingEnv env, DSConnection dsCon) {
        this.transfer = transfer;
        this.dsCon = dsCon;
        this.trade = trade;

        super.init(message, trade, sender, receiver, transfer, transferRules, env, dsCon);
    }

    protected PartySDIInfo getBuySellSDIInfo(SettleDeliveryInstruction cptysi, ManualSDI msd, String productType, String role, boolean invert, boolean isIBRC) {

        if ("TransferAgent".equals(productType) && isPortugueseSecurity()) {
            //SPECIAL CASE FOR OWN TRANSFERS OF PORTUGUESE SECURITIES
            return null;
        }

        return super.getBuySellSDIInfo(cptysi, msd, productType, role, invert, isIBRC);
    }

    protected PartySDIInfo getAgentSDIInfo(SettleDeliveryInstruction posi, SettleDeliveryInstruction cptysi, ManualSDI msd, String productType, String role, boolean resetRoute, boolean isIBRC, boolean isVPDK) {
        if ("TransferAgent".equals(productType) && isPortugueseSecurity() && ("REAG".equals(role) || "DEAG".equals(role))) {
            if ("PAY".equals(transfer.getPayReceive()) && "REAG".equals(role) ) {
                PartySDIInfo pi = super.getAgentSDIInfo(posi, cptysi, msd, productType, role, resetRoute, isIBRC, isVPDK);

                if (pi != null) {
                    int psetId = getPsetId(posi, cptysi);
                    if (psetId == pi.getPartyId()) {
                        pi.setPartyId(cptysi.getBeneficiaryId());
                        pi.setRole(cptysi.getRole());
                    }
                }
                return pi;
            } else if ("RECEIVE".equals(transfer.getPayReceive()) && "DEAG".equals(role)) {
                PartySDIInfo pi = super.getAgentSDIInfo(posi, cptysi, msd, productType, role, resetRoute, isIBRC, isVPDK);

                if (pi != null) {
                    int psetId = getPsetId(posi, cptysi);
                    if (psetId == pi.getPartyId()) {
                        pi.setPartyId(cptysi.getBeneficiaryId());
                        pi.setRole(cptysi.getRole());
                    }
                }
                return pi;
            }
        }
        return super.getAgentSDIInfo(posi, cptysi, msd, productType, role, resetRoute, isIBRC, isVPDK);
    }

    private int getPsetId(SettleDeliveryInstruction posi, SettleDeliveryInstruction cptysi) {
        int psetId = -1;
        if (cptysi != null) {
            Vector<?> listSDI = SDISelectorUtil.getFullRoute(cptysi, posi, trade, transfer.toTradeTransferRule(), transfer.getSettleDate(), dsCon);

            SwiftAgent pset = SwiftUtil.getBridgeAgent(listSDI);
            if (pset != null) {
                psetId = pset.getAgentId();
            } else {
                pset = SwiftUtil.getLastCptyAgent(listSDI);
                if (pset != null) {

                    psetId = pset.getAgentId();
                }
            }
        }
        return psetId;
    }

    private boolean isPortugueseSecurity() {
        if (transfer.getProductId() > 0) {
            Product sec = BOCache.getExchangedTradedProduct(dsCon, transfer.getProductId());
            String isin = sec == null ? null : sec.getSecCode("ISIN");
            return !Util.isEmpty(isin) && isin.startsWith("PT");
        }
        return false;
    }
}