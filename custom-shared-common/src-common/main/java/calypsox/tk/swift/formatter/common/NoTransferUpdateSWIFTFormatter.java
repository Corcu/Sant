package calypsox.tk.swift.formatter.common;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.bo.swift.SwiftUtil;
import com.calypso.tk.bo.swift.TagValue;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.SDI;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.swift.formatter.TransferSWIFTFormatter;

import java.util.Vector;

/**
 * PaymentHubSWIFTFormatter
 * This class overrides methods that search on cache or BDD for transfers and use SDI, to maintain custom SDI in PH messages.
 *
 * @author Ruben Garcia
 */
public class NoTransferUpdateSWIFTFormatter extends TransferSWIFTFormatter {

    private boolean _sortCodeUsed = false;

    @Override
    public String parseCPTY_INTERMEDIARY(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                         Vector transferRules, BOTransfer transfer, String format, DSConnection dsCon) {
        String sortCode = SwiftUtil.getCptySortCode("INTERMEDIARY", trade, transfer, message, dsCon);
        boolean useSortCode = true;
        if (!Util.isEmpty(sortCode)) {
            if (sortCode.startsWith("//")) {
                sortCode = sortCode.substring(2);
            }

            if (sortCode.startsWith("/")) {
                sortCode = sortCode.substring(1);
            }

            if (sortCode.length() > 2) {
                String sc = sortCode.substring(0, 2);
                if (_sortCodes.contains(sc)) {
                    useSortCode = !this._sortCodeUsed;
                    this._sortCodeUsed = true;
                }
            }
        }

        TagValue tagValue = SwiftUtil.getCptyTagValue("INTERMEDIARY", trade, transfer, message, "PAY",
                false, useSortCode, transferRules, dsCon);
        this.setTagValue(tagValue);
        return tagValue.getValue();
    }

    @Override
    public String parseCPTY_INTERMEDIARY2(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                          Vector transferRules, BOTransfer transfer, String format, DSConnection dsCon) {
        String sortCode = SwiftUtil.getCptySortCode("INTERMEDIARY2", trade, transfer, message, dsCon);
        boolean useSortCode = true;
        if (!Util.isEmpty(sortCode)) {
            if (sortCode.startsWith("//")) {
                sortCode = sortCode.substring(2);
            }

            if (sortCode.startsWith("/")) {
                sortCode = sortCode.substring(1);
            }

            if (sortCode.length() > 2) {
                String sc = sortCode.substring(0, 2);
                if (_sortCodes.contains(sc)) {
                    useSortCode = !this._sortCodeUsed;
                    this._sortCodeUsed = true;
                }
            }
        }

        TagValue tagValue = SwiftUtil.getCptyTagValue("INTERMEDIARY2", trade, transfer,
                message, "PAY", false, useSortCode, transferRules, dsCon);
        this.setTagValue(tagValue);
        return tagValue.getValue();
    }

    @Override
    public String parseCPTY_RECEIVING_AGENT(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                            Vector transferRules, BOTransfer transfer, String format, DSConnection con) {
        if (trade == null && transfer == null) {
            return "";
        } else {
            TradeTransferRule rule;
            if (transfer != null) {
                rule = transfer.toTradeTransferRule();
            } else {
                rule = this.getTradeTransferRule(trade, transferRules, con);
            }

            if (rule == null) {
                return null;
            } else {
                SDI sdi = SwiftUtil.getReceiverSDI(rule, con, null);
                if (sdi == null) {
                    TagValue tagValue = new TagValue("57", "D", "UNKNOWN", 0);
                    this.setTagValue(tagValue);
                    return tagValue.getValue();
                } else {
                    String sortCode = SwiftUtil.getCptySortCode("AGENT", trade, transfer, message, con);
                    boolean useSortCode = true;
                    if (!Util.isEmpty(sortCode)) {
                        if (sortCode.startsWith("//")) {
                            sortCode = sortCode.substring(2);
                        }

                        if (sortCode.startsWith("/")) {
                            sortCode = sortCode.substring(1);
                        }

                        if (sortCode.length() > 2) {
                            String sc = sortCode.substring(0, 2);
                            if (_sortCodes.contains(sc)) {
                                useSortCode = !this._sortCodeUsed;
                                this._sortCodeUsed = true;
                            }
                        }
                    }

                    TagValue tagValue = SwiftUtil.getCptyTagValue("AGENT", trade, transfer, message, "PAY",
                            true, useSortCode, transferRules, con);
                    if (tagValue == null || tagValue.getValue() == null) {
                        tagValue = new TagValue("57", "D", "UNKNOWN", 0);
                    }
                    this.setTagValue(tagValue);
                    return tagValue.getValue();
                }
            }
        }
    }

    @Override
    public String parseCPTY_BENEFICIARY(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                        Vector transferRules, BOTransfer transfer, String format, DSConnection con) {
        String sortCode = SwiftUtil.getCptySortCode("BENEFICIARY", trade, transfer, message, con);
        boolean useSortCode = true;
        if (!Util.isEmpty(sortCode)) {
            if (sortCode.startsWith("//")) {
                sortCode = sortCode.substring(2);
            }

            if (sortCode.startsWith("/")) {
                sortCode = sortCode.substring(1);
            }

            if (sortCode.length() > 2) {
                String sc = sortCode.substring(0, 2);
                if (_sortCodes.contains(sc)) {
                    useSortCode = !this._sortCodeUsed;
                    this._sortCodeUsed = true;
                }
            }
        }

        TagValue tagValue = SwiftUtil.getCptyTagValue("BENEFICIARY", trade, transfer, message,
                false, useSortCode, con);
        this.setTagValue(tagValue);
        return tagValue.getValue();
    }

    public boolean hasIntermediary2(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection con) {
        TagValue tagValue = SwiftUtil.getCptyTagValue("INTERMEDIARY2", trade, transfer, message, "PAY", false, false, transferRules, con);
        if (tagValue == null) {
            return false;
        } else {
            return tagValue.getLegalEntityId() != 0;
        }
    }

    public boolean hasIntermediary(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection con) {
        TagValue tagValue = SwiftUtil.getCptyTagValue("INTERMEDIARY", trade, transfer, message, "PAY", false, false, transferRules, con);
        if (tagValue == null) {
            return false;
        } else {
            return tagValue.getLegalEntityId() != 0;
        }
    }

    public boolean isAgentNotBeneficiaryOrReceiver(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection con) {
        boolean b1 = this.isAgentNotBeneficiary(message, trade, sender, rec, transferRules, transfer, con);
        if (!b1) {
            return false;
        } else {
            boolean b2 = this.isAgentNotReceiver(message, trade, sender, rec, transferRules, transfer, con);
            return b2;
        }
    }

    public boolean isAgentNotBeneficiary(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection con) {
        TagValue tagValue = SwiftUtil.getCptyTagValue("BENEFICIARY", trade, transfer, message, false, con);
        TagValue tagValue2 = SwiftUtil.getCptyTagValue("AGENT", trade, transfer, message, false, con);
        if (tagValue.getLegalEntityId() != tagValue2.getLegalEntityId() && (!"A".equals(tagValue.getOption()) || !"A".equals(tagValue2.getOption()))) {
            return true;
        } else if (tagValue2.getValue().indexOf("//") >= 0) {
            return true;
        } else {
            String agentBicCode = tagValue2.getBicCode();
            String cptyBicCode = tagValue.getBicCode();
            return !Util.isSame(agentBicCode, cptyBicCode);
        }
    }

    public boolean isAgentNotReceiver(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection con) {
        TagValue tagValue = SwiftUtil.getCptyTagValue("AGENT", trade, transfer, message, false, con);
        if (tagValue.getLegalEntityId() != message.getReceiverId() && !"A".equals(tagValue.getOption())) {
            return true;
        } else if (tagValue.getValue().indexOf("//") >= 0) {
            return true;
        } else {
            String agentBicCode = tagValue.getBicCode();
            if (message.getReceiverContactId() > 0) {
                LEContact leC = BOCache.getLegalEntityContact(con, message.getReceiverContactId());
                if (leC != null) {
                    String receiverBicCode = leC.getSwift();
                    if (Util.isEmpty(receiverBicCode)) {
                        receiverBicCode = message.getReceiverAddressCode();
                    }

                    if (!Util.isSame(agentBicCode, receiverBicCode)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }
}
