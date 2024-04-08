package calypsox.tk.bo.util;

import com.calypso.tk.bo.*;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Comparator;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * MessageSDIUtil get SDI using custom filter
 *
 * @author Ruben Garcia
 */
public class MessageSDIUtil {

    /**
     * Get the SDI using custom filter by agent;intermediary;intermediary2
     * -First select the SDI using core filter
     * -Then apply custom filter
     *
     * @param trade          the current trade
     * @param transfer       the current transfer
     * @param dsCon          the connection to Data Server
     * @param messages       the error messages
     * @param leCode         the legal entity code
     * @param leRole         the legal entity role CounterParty or ProcessingOrg
     * @param customFilterDV the custom Domain Value filter name
     * @return the selected SDI
     */
    public static String getSDIByCustomFilter(Trade trade, BOTransfer transfer, DSConnection dsCon, Vector messages, String leCode, String leRole, String customFilterDV) {
        if (trade != null && dsCon != null && !Util.isEmpty(leCode) && !Util.isEmpty(leRole) && !Util.isEmpty(customFilterDV)) {
            if (messages == null) {
                messages = new Vector<String>();
            }
            TradeTransferRule rule;
            if (transfer != null) {
                rule = transfer.toTradeTransferRule();
            } else {
                rule = getTransferRule(trade, dsCon);
            }

            if (rule != null) {
                LegalEntity legalEntity = BOCache.getLegalEntity(dsCon, leCode);
                if (legalEntity != null) {

                    TradeTransferRule clonedRule = rule.clone();
                    if ("CounterParty".equalsIgnoreCase(leRole)) {
                        clonedRule.setCounterParty(legalEntity.getId());
                        clonedRule.setCounterPartySDId(-1);
                    } else if ("ProcessingOrg".equalsIgnoreCase(leRole)) {
                        clonedRule.setProcessingOrg(legalEntity.getId());
                        clonedRule.setProcessingOrgSDId(-1);
                    }

                    SDISelector selector = SDISelectorUtil.find(trade, clonedRule);

                    JDate settleDate = trade.getSettleDate();
                    if (transfer != null) {
                        settleDate = transfer.getSettleDate();
                    }

                    if (selector != null) {
                        Vector<SettleDeliveryInstruction> sdis = selector.getValidSDIList(trade, clonedRule, settleDate,
                                legalEntity.getCode(), leRole, new Vector(), false, dsCon);
                        if (!Util.isEmpty(sdis)) {
                            SDIFilter sdiFilter = buildSDIFilter(trade, dsCon, customFilterDV);
                            if (sdiFilter != null) {
                                sdis = sdis.stream().filter(sdiFilter::accept).collect(Collectors.toCollection(Vector::new));
                            }
                            if (!Util.isEmpty(sdis)) {
                                sdis.sort(Comparator.comparingInt(SettleDeliveryInstruction::getPriority));
                                return String.valueOf(sdis.get(0).getId());
                            } else if (sdiFilter != null) {
                                String msg = "There is no SDI when the custom filter is applied.";
                                msg += "[SDIFilter = " + sdiFilter + "]";
                                messages.add(msg);
                                return null;

                            }
                        } else {
                            String msg = "There is no SDI when the core filter is applied.";
                            messages.add(msg);
                            return null;
                        }
                    } else {
                        String msg = "No selector found for this product.";
                        messages.add(msg);
                        return null;
                    }
                } else {
                    String msg = "There is no legalEntity in the system with the code: " + leCode;
                    messages.add(msg);
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Get the first trade transfer rule with type PRINCIPAL or SECURITY for MT515BILAT msg than not has linked transfer
     *
     * @param trade the current trade
     * @param ds    the Data Server connection
     * @return the first trade transfer rule
     */
    private static TradeTransferRule getTransferRule(Trade trade, DSConnection ds) {
        if (trade != null && ds != null) {
            Vector<TradeTransferRule> transferRules = null;
            try {
                if (trade.getCustomTransferRuleB()) {
                    transferRules = trade.getTransferRules();
                } else {
                    transferRules = BOProductHandler.buildTransferRules(trade, new Vector(), ds);
                }
            } catch (Exception e) {
                Log.error(MessageSDIUtil.class.getName(), e);
            }

            if (!Util.isEmpty(transferRules)) {
                return transferRules.stream()
                        .filter(t -> !Util.isEmpty(t.getTransferType()))
                        .filter(t -> "SECURITY".equals(t.getTransferType()) ||
                                "PRINCIPAL".equals(t.getTransferType())).findFirst().orElse(null);
            }
        }
        return null;
    }


    /**
     * Build SDI custom filter using domain value CustomSettlementDetailIteratorSDIFilter
     * Counterparty;ProductType
     * Agent;Intermediary;Intermediary2
     *
     * @param trade          the current trade
     * @param dsCon          the Data Server connection
     * @param customFilterDV the custom filter domain value name
     * @return the SDFilter object
     */
    private static SDIFilter buildSDIFilter(Trade trade, DSConnection dsCon, String customFilterDV) {
        if (trade.getCounterParty() != null && !Util.isEmpty(trade.getCounterParty().getCode()) && !Util.isEmpty(trade.getProductType())) {
            String filterComment = LocalCache.getDomainValueComment(dsCon, customFilterDV, trade.getCounterParty().getCode() + ";" + trade.getProductType());
            if (!Util.isEmpty(filterComment)) {
                String[] elements = filterComment.split(";");
                if (elements.length == 3) {
                    int agent = -1;
                    int intermediary = -1;
                    int intermediary2 = -1;
                    if (!elements[0].equals("NONE")) {
                        LegalEntity le = BOCache.getLegalEntity(dsCon, elements[0]);
                        if (le != null) {
                            agent = le.getId();
                        }
                    }

                    if (!elements[1].equals("NONE")) {
                        LegalEntity le = BOCache.getLegalEntity(dsCon, elements[1]);
                        if (le != null) {
                            intermediary = le.getId();
                        }
                    }

                    if (!elements[2].equals("NONE")) {
                        LegalEntity le = BOCache.getLegalEntity(dsCon, elements[2]);
                        if (le != null) {
                            intermediary2 = le.getId();
                        }
                    }

                    return new SDIFilter(agent, intermediary, intermediary2);

                }
            }
        }
        return null;
    }

    /**
     * Custom SDIFilter inner class
     */
    static class SDIFilter {

        /**
         * SDI agent ID
         */
        private final int agent;

        /**
         * SDI Intermediary ID
         */
        private final int intermediary;

        /**
         * SDI Intermediary2 ID
         */
        private final int intermediary2;

        /**
         * Constructor
         *
         * @param agent         SDI agent ID
         * @param intermediary  SDI intermediary ID
         * @param intermediary2 SDI intermediary2 ID
         */
        SDIFilter(int agent, int intermediary, int intermediary2) {
            this.agent = agent;
            this.intermediary = intermediary;
            this.intermediary2 = intermediary2;
        }

        /**
         * Filter SDI
         *
         * @param sdi the current SDI
         * @return true if filter
         */
        boolean accept(SettleDeliveryInstruction sdi) {
            if (agent > 0 && sdi.getAgentId() != agent) {
                return false;
            }
            if (intermediary > 0 && sdi.getIntermediaryId() != intermediary) {
                return false;
            }

            if (intermediary2 > 0 && sdi.getIntermediary2Id() != intermediary2) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "SDIFilter{" +
                    "agent=" + agent +
                    ", intermediary=" + intermediary +
                    ", intermediary2=" + intermediary2 +
                    '}';
        }
    }
}
