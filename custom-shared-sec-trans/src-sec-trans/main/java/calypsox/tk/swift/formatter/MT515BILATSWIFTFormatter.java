package calypsox.tk.swift.formatter;

import calypsox.tk.bo.workflow.rule.UpdateCounterPartySDIMessageRule;
import calypsox.tk.bo.workflow.rule.UpdateProcessingOrgSDIMessageRule;
import calypsox.tk.swift.formatter.common.CustomSecuritySWIFTFormatter;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.bo.swift.SwiftUtil;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.SimpleRepo;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

import static calypsox.tk.bo.workflow.rule.UpdateMxElectplatidKWMessageRule.MASTER_REFERENCE;

/**
 * MT515BILATSWIFTFormatter for MT515 BILAT outgoing messages
 *
 * @author Ruben Garcia
 */
public class MT515BILATSWIFTFormatter extends MT515SWIFTFormatter implements CustomSecuritySWIFTFormatter {

    /**
     * Method like native REPO_AMT without signed currency swift amount for BILAT
     *
     * @param message       the current BOMessage
     * @param trade         the current trade
     * @param sender        the current sender
     * @param rec           the current receiver
     * @param transferRules the current transfer rules
     * @param transfer      the current transfer
     * @param format        the message format
     * @param dsCon         the Data Server connection
     * @return repo amount without signed ccy
     * @throws MessageFormatException error in message
     */
    public String parseREPO_AMT_BILAT(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, String format, DSConnection dsCon) throws MessageFormatException {
        if (trade != null && trade.getProduct() != null) {
            Product product = trade.getProduct();
            double amount = 0.0;
            if (product instanceof SimpleRepo) {
                amount = ((SimpleRepo) product).getFinalSettlementAmount(trade, this._pricingEnv);
            } else if (product instanceof Repo) {
                Repo repo = (Repo) product;
                boolean bsb = repo.getBuySellBackB();
                repo.setBuySellBackB(true);
                amount = repo.getFinalSettlementAmount(trade, this._pricingEnv);
                repo.setBuySellBackB(bsb);
            }

            return ":TRTE//" + SwiftUtil.getCurrencySwiftAmount(amount, trade.getSettleCurrency());
        } else {
            return "";
        }
    }


    /**
     * Get the buyer party account for bilateral MT15 Messages
     * -If quantity < 0.0 get the PO SDI agent account
     * -Else get the MurexBilateralCounterparty SDI (Beneficiary KW and agent ETCMS) agent account
     *
     * @param message       the current message
     * @param trade         the current trade
     * @param sender        the current sender
     * @param rec           the current receiver
     * @param transferRules the current transfer rules
     * @param transfer      the current transfer
     * @param dsCon         the connection to Data Server
     * @return the buyer party bilateral account
     */
    public String parseBUYER_PARTY_BILAT(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        if (message != null && trade != null) {
            if (isETCMS(rec)) {
                if (trade.getQuantity() < 0.0) {
                    //PO
                    String poSDIId = message.getAttribute(UpdateProcessingOrgSDIMessageRule.PO_SDI_ID);
                    if (!Util.isEmpty(poSDIId)) {
                        SettleDeliveryInstruction poSDI = BOCache.getSettleDeliveryInstruction(dsCon, Integer.parseInt(poSDIId));
                        if (poSDI != null && !Util.isEmpty(poSDI.getAgentAccount())) {
                            this._tagValue.setOption("R");
                            return ":BUYR/ECLR/PART/" + poSDI.getAgentAccount();
                        }
                    }
                } else {
                    //MurexBilat
                    String ctpySDIId = message.getAttribute(UpdateCounterPartySDIMessageRule.CTPY_SDI_ID);
                    if (!Util.isEmpty(ctpySDIId)) {
                        SettleDeliveryInstruction ctpySDI = BOCache.getSettleDeliveryInstruction(dsCon, Integer.parseInt(ctpySDIId));
                        if (ctpySDI != null && !Util.isEmpty(ctpySDI.getAgentAccount())) {
                            this._tagValue.setOption("R");
                            return ":BUYR/ECLR/PART/" + ctpySDI.getAgentAccount();
                        }
                    }
                }
            } else {
                return super.parseBUYER_PARTY(message, trade, sender, rec, transferRules, transfer, dsCon);
            }
        }
        return "";
    }

    /**
     * Get the seller party account for bilateral MT15 Messages
     * -If quantity >= 0.0 get the PO SDI agent account
     * -Else get the MurexBilateralCounterparty SDI (Beneficiary KW and Agent ETCMS) agent account
     *
     * @param message       the current message
     * @param trade         the current trade
     * @param sender        the current sender
     * @param rec           the current receiver
     * @param transferRules the current transfer rules
     * @param transfer      the current transfer
     * @param dsCon         the connection to Data Server
     * @return the seller party bilateral account
     */
    public String parseSELL_PARTY_BILAT(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        if (message != null && trade != null) {
            if (isETCMS(rec)) {
                if (trade.getQuantity() < 0.0) {
                    //MurexBilat
                    String ctpySDIId = message.getAttribute(UpdateCounterPartySDIMessageRule.CTPY_SDI_ID);
                    if (!Util.isEmpty(ctpySDIId)) {
                        SettleDeliveryInstruction ctpySDI = BOCache.getSettleDeliveryInstruction(dsCon, Integer.parseInt(ctpySDIId));
                        if (ctpySDI != null && !Util.isEmpty(ctpySDI.getAgentAccount())) {
                            this._tagValue.setOption("R");
                            return ":SELL/ECLR/PART/" + ctpySDI.getAgentAccount();
                        }
                    }
                } else {
                    //PO
                    String poSDIId = message.getAttribute(UpdateProcessingOrgSDIMessageRule.PO_SDI_ID);
                    if (!Util.isEmpty(poSDIId)) {
                        SettleDeliveryInstruction poSDI = BOCache.getSettleDeliveryInstruction(dsCon, Integer.parseInt(poSDIId));
                        if (poSDI != null && !Util.isEmpty(poSDI.getAgentAccount())) {
                            this._tagValue.setOption("R");
                            return ":SELL/ECLR/PART/" + poSDI.getAgentAccount();
                        }
                    }
                }
            } else {
                return super.parseSELL_PARTY(message, trade, sender, rec, transferRules, transfer, dsCon);
            }
        }
        return "";
    }

    /**
     * Check if receiver contact has ETCMS_PROGRAM
     *
     * @param receiverContact the receiver contact
     * @return true if is ETCMS
     */
    private static boolean isETCMS(LEContact receiverContact) {
        return receiverContact != null && !Util.isEmpty(receiverContact.getAddressCode("ETCMS_PROGRAM"));
    }

    /**
     * Get the ETCMS SEME field
     *
     * @param message       the BOMessage
     * @param trade         the trade
     * @param sender        the sender
     * @param rec           the receiver
     * @param transferRules the transfer rules
     * @param transfer      the transfer
     * @param dsCon         the Data Server connection
     * @return the SEME value, Mx Electplatid trade KW or message ID and CYO
     */

    @Override
    public String parseMESSAGE_ID(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        return customizeMessageIdentifier(super.parseMESSAGE_ID(message, trade, sender, rec, transferRules, transfer, dsCon));
    }



    @Override
    public String parseMAST_MESSAGE_ID(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        String masterRef = getMasterRef(message, trade);
        return Util.isEmpty(masterRef)
                ? customizeMessageIdentifier(super.parseMAST_MESSAGE_ID(message, trade, sender, rec, transferRules, transfer, dsCon))
                : customizeMessageIdentifier(":MAST//" + masterRef);
    }

    private String getMasterRef(BOMessage message, Trade trade) {
        String masterRef = message.getAttribute(MASTER_REFERENCE);
        if (Util.isEmpty(masterRef)) {
            if (trade != null) {
                if (!Util.isEmpty(trade.getKeywordValue("Mx Electplatid"))) {
                    masterRef = trade.getKeywordValue("Mx Electplatid");
                } else if (!Util.isEmpty(trade.getKeywordValue("Mx ELECPLATID"))) {
                    masterRef = trade.getKeywordValue("Mx ELECPLATID");
                }
            }
        }
        return masterRef;
    }

    public String parseMESSAGE_LINKED_MAST_ID(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {

        String masterRef = null;super.parseMESSAGE_LINKED_ID(message, trade, sender, rec, transferRules, transfer, dsCon);
        if (message.getLinkedLongId() > 0) {
            try {
              BOMessage linkedMessage = dsCon.getRemoteBO().getMessage(message.getLinkedLongId());
              if (linkedMessage!=null) {
                    masterRef = getMasterRef(linkedMessage, trade);
              }
            } catch (CalypsoServiceException e) {
                Log.error(this, e);
                return "";
            }
        }

        return Util.isEmpty(masterRef)
                ? customizeMessageIdentifier(super.parseMESSAGE_LINKED_ID(message, trade, sender, rec, transferRules, transfer, dsCon))
                : customizeMessageIdentifier(":PREV//"  +  masterRef);
    }
}
