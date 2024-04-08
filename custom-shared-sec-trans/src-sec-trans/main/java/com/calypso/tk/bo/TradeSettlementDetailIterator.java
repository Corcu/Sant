package com.calypso.tk.bo;

import com.calypso.tk.bo.swift.SWIFTFormatter;

import com.calypso.tk.bo.swift.SwiftUtil;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.SimpleRepo;
import com.calypso.tk.product.TransferAgent;
import com.calypso.tk.refdata.*;
import com.calypso.tk.service.DSConnection;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Vector;
import java.util.stream.Stream;

public class TradeSettlementDetailIterator extends SettlementDetailIterator {

    @Override
    public void init(BOMessage message, Trade trade, LEContact sender, LEContact receiver, BOTransfer transfer, Vector transferRules, PricingEnv env, DSConnection dsCon) {
        super._message = message;
        super._trade = trade;
        super._sender = sender;
        super._receiver = receiver;
        super._transfer = transfer;
        super._rules = transferRules;
        super._dsCon = dsCon;
        super._rule = getSecurityTransferRule();
        super._count = 0;
        super.setIteratorSet(this.getSettlementDetails());
    }

    public Vector getSettlementDetails() {
        Vector sds = new Vector();
        if (this._rule!=null) {
            ManualSDI msd = null;
            SettleDeliveryInstruction posi = null;
            SettleDeliveryInstruction cptysi = null;
            int poId = this._rule.getProcessingOrg();
            String productType = this._rule.getProductType();
            posi = BOCache.getSettleDeliveryInstruction(this._dsCon, this._rule.getProcessingOrgSDId());
            cptysi = BOCache.getSettleDeliveryInstruction(this._dsCon, this._rule.getCounterPartySDId());
            if (this._rule.isManualSDI()) {
                msd = BOCache.getManualSDI(this._dsCon, this._rule.getManualSDId());
            }

            int psetId = 0;
            PartySDIInfo pi = this.getPSETSDIInfo(posi, cptysi, msd, productType, poId);
            if (pi != null) {
                sds.add(pi);
                psetId = pi.getPartyId();
            }

            String dataSourceScheme = null;
            if (this.getFormatterParent() != null && this.getFormatterParent() instanceof SWIFTFormatter) {
                dataSourceScheme = ((SWIFTFormatter) this.getFormatterParent()).getDataSourceScheme();
            } else if (this._transfer != null && this._message != null) {
                dataSourceScheme = SwiftUtil.getDataSourceScheme(this._trade, this._transfer, this._message.getSettleDate(), this._dsCon);
            }

            boolean isIBRC = "IBRC".equals(dataSourceScheme) && psetId == this._message.getReceiverId();
            boolean isVPDK = "VPDK".equals(dataSourceScheme);

            String buySellRole = "SELL";
            String agentRole = "DEAG";
            String intRole = "DECU";
            String buySellRole2 = "BUYR";
            String agentRole2 = "REAG";
            String intRole2 = "RECU";

            boolean buySell = false;
            if (this._trade.getProduct().hasSecondaryMarket()) {
                if (this._trade.getQuantity() < 0.0D) {
                    buySellRole = "BUYR";
                    agentRole = "REAG";
                    intRole = "RECU";
                    buySell = true;
                }
            } else if (!(this._trade.getProduct() instanceof Repo) && !(this._trade.getProduct() instanceof SimpleRepo)) {
                if (this._trade.getQuantity() < 0.0D) {
                    buySellRole = "BUYR";
                    agentRole = "REAG";
                    intRole = "RECU";
                    buySell = true;
                }
            } else if (this._trade.getQuantity() >= 0.0D) {
                buySellRole = "BUYR";
                agentRole = "REAG";
                intRole = "RECU";
                buySell = true;
            }

            if (buySell) {
                buySellRole2 = "SELL";
                agentRole2 = "DEAG";
                intRole2 = "DECU";
            }

            boolean specialXferAgentIBRC = false;
            if (isIBRC && this._trade != null && this._trade.getProduct() != null && this._trade.getProduct() instanceof TransferAgent) {
                TransferAgent ta = (TransferAgent)this._trade.getProduct();
                SettleDeliveryInstruction sdi = BOCache.getSettleDeliveryInstruction(this._dsCon, ta.getFromSdiId());
                if (sdi != null && sdi.getAgentId() == psetId) {
                    sdi = BOCache.getSettleDeliveryInstruction(this._dsCon, ta.getToSdiId());
                    if (sdi != null && sdi.getAgentId() == psetId) {
                        specialXferAgentIBRC = true;
                    }
                }
            }

            pi = this.getBuySellSDIInfo(cptysi, msd, productType, buySellRole, false, specialXferAgentIBRC);
            if (pi != null) {
                sds.add(pi);
            }

            pi = this.getBuySellSDIInfo(posi, msd, productType, buySellRole2, false, specialXferAgentIBRC);
            if (pi != null) {
                sds.add(pi);
            }

            pi = this.getAgentSDIInfo(posi, cptysi, msd, productType, agentRole, false, isIBRC, isVPDK);
            if (pi != null) {
                if (cptysi != null && isIBRC && psetId == pi.getPartyId()) {
                    pi.setPartyId(cptysi.getBeneficiaryId());
                    pi.setRole(cptysi.getRole());
                }

                sds.add(pi);
            }

            pi = this.getAgentSDIInfo(cptysi, posi, msd, productType, agentRole2, false, isIBRC, isVPDK);
            if (pi != null) {
                if (posi != null && isIBRC && psetId == pi.getPartyId()) {
                    pi.setPartyId(posi.getBeneficiaryId());
                    pi.setRole(posi.getRole());
                }

                sds.add(pi);
            }

            if (posi == null || Util.isEmpty(posi.getAttribute("POPledgeSDI"))) {
                pi = this.getIntSDIInfo(posi, cptysi, msd, productType, intRole);
                if (pi != null) {
                    sds.add(pi);
                }
            }
            if (cptysi == null || Util.isEmpty(cptysi.getAttribute("POPledgeSDI"))) {
                pi = this.getIntSDIInfo(cptysi, posi, msd, productType, intRole2);
                if (pi != null) {
                    sds.add(pi);
                }
            }

            if (isIBRC && cptysi != null && posi !=null) {
                String cptyAgentRole = "DEAG";
                String cptyAgentRole2 = "REAG";
                if (agentRole.equals(cptyAgentRole)) {
                    cptyAgentRole = "REAG";
                    cptyAgentRole2 = "DEAG";
                }
                pi = this.getAgentSDIInfo(cptysi, posi, msd, productType, cptyAgentRole, true, isIBRC, false);
                if (pi != null) {
                    if (psetId == pi.getPartyId()) {
                        pi.setPartyId(posi.getBeneficiaryId());
                        pi.setRole(posi.getRole());
                    }

                    sds.add(pi);
                }

                pi = this.getAgentSDIInfo(posi, cptysi, msd, productType, cptyAgentRole2, true, isIBRC, false);
                if (pi != null) {
                    if (psetId == pi.getPartyId()) {
                        pi.setPartyId(cptysi.getBeneficiaryId());
                        pi.setRole(cptysi.getRole());
                    }

                    sds.add(pi);
                }
            }
        }
        return sds;
    }

    private TradeTransferRule getSecurityTransferRule(){
        String payReceive = "RECEIVE";
        if (this._trade.getProduct().hasSecondaryMarket()) {
            if (this._trade.getQuantity() < 0.0D) {
                payReceive = "PAY";
            }
        } else if (!(this._trade.getProduct() instanceof Repo) && !(this._trade.getProduct() instanceof SimpleRepo)) {
            if (this._trade.getQuantity() < 0.0D) {
                payReceive = "PAY";
            }
        } else if (this._trade.getQuantity() >= 0.0D) {
            payReceive = "PAY";
        }

        for(int i = 0; i <this._rules.size(); ++i) {
            TradeTransferRule transferRule = (TradeTransferRule)this._rules.elementAt(i);
            if (transferRule.getTransferType().equals("SECURITY") && transferRule.getPayReceive().equals(payReceive)) {
                return transferRule;
            }
        }
        return null;
    }

}
