package calypsox.tk.swift.formatter;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.bo.swift.SwiftUtil;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Repo;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.refdata.PartySDIInfo;
import com.calypso.tk.service.DSConnection;

import java.util.Collection;
import java.util.Optional;
import java.util.Vector;

public class MT515SWIFTFormatter extends com.calypso.tk.swift.formatter.MT515SWIFTFormatter {

    public String parseTRADE_SETTLEMENT_PARTY(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, String format, DSConnection con) {

        PartySDIInfo psdi = (PartySDIInfo) this.getIteratorObject();
        int poId = trade.getBook().getProcessingOrgBasedId();

        String party = "UNKNOWN";
        String sep = "//";
        if (!Util.isEmpty(psdi.getPartyIdentifier())) {
            this._tagValue.setOption("R");
            party = SwiftUtil.formatParty("R", psdi.getPartyId(), (String) null, psdi.getRole(), psdi.getPartyIdentifier(),
                    psdi.getContactType(), psdi.getProductType(), poId, trade, transfer, message, con, (Object) null);
            if (!party.equals("UNKNOWN")) {
                if (party.startsWith("/")) {
                    party = party.substring(1);
                }

                sep = "/";// 1063
                if (!party.contains("/")) {
                    if (this.isIBRC(message, trade, transfer, con)) {
                        party = "IBRC/" + party;
                    } else {
                        this._tagValue.setOption("Q");
                        sep = "//";
                    }
                }
            }
        }

        if (party.equals("UNKNOWN")) {
            this._tagValue.setOption("P");
            party = SwiftUtil.formatParty("P", psdi.getPartyId(), (String) null, psdi.getRole(), psdi.getContactType(),
                    psdi.getProductType(), poId, trade, transfer, message, con, (Object) null);
        }

        if (party.equals("UNKNOWN")) {
            party = SwiftUtil.formatParty("Q", psdi.getPartyId(), (String) null, "Agent", psdi.getContactType(),
                    psdi.getProductType(), poId, trade, transfer, message, con, (Object) null);
            this._tagValue.setOption("Q");
        }

        if ("Q".equals(this._tagValue.getOption()) && party != null && party.length() > 35) {
            party = SwiftUtil.formatLongString(party, 4);
        }

        return ":" + psdi.getIdentifier() + sep + party;
    }

    public String parseINDICATOR_2(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, String format, DSConnection con) {
        String indicator = "";

        try {
            TradeTransferRule rule = this.getSecurityTransferRule(trade, con);
            if (rule != null) {
                if (Util.isTrue(this.hasProgramReference(message, trade, sender, rec, transferRules, transfer, con))) {
                    indicator = ":SETR/ECLR/REPO";
                } else {
                    indicator = ":SETR//TRAD";
                }
            }
        } catch (Exception var10) {
            Log.error("Swift", var10);
        }

        return indicator;
    }

    public boolean isChildTrade(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection con) {
        Collection cptyAttributes = trade.getCounterParty().getLegalEntityAttributes();
        if (cptyAttributes == null) return false;

        for (Object o : cptyAttributes) {
            LegalEntityAttribute legalEntityAttribute = (LegalEntityAttribute) o;
            if ("ALIAS_MT515".equals(legalEntityAttribute.getAttributeType())) {
                return !Util.isEmpty(trade.getKeywordValue("AllocatedFrom"));
            }
        }
        return false;
    }

    public String parseBIC_CODE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection con) {
        String bic = "";
        this._tagValue.setOption("P");
        if (message.getSenderRole().equals("CounterParty") && sender.getSwift() != null) {
            bic = sender.getSwift();
        }
        if (message.getReceiverRole().equals("CounterParty") && rec.getSwift() != null) {
            bic = rec.getSwift();
        }

        return ":INVE//" + bic;
    }

    public String parseALIAS(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection con) {
        String value = "";

        this._tagValue.setOption("A");
        Collection cptyAttributes = trade.getCounterParty().getLegalEntityAttributes();

        for (Object o : cptyAttributes) {
            LegalEntityAttribute legalEntityAttribute = (LegalEntityAttribute) o;
            if ("ALIAS_MT515".equals(legalEntityAttribute.getAttributeType())) {
                value = legalEntityAttribute.getAttributeValue();
            }
        }

        return ":SAFE//" + value;
    }

    @Override
    public String parsePAYMENT_INDICATOR(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {

        if (trade != null && trade.getProduct() instanceof Repo && ((Repo) trade.getProduct()).isTriparty()) { //DBVs (GC Repos)
            return ":PAYM//APMT";
        }

        return super.parsePAYMENT_INDICATOR(message, trade, sender, rec, transferRules, transfer, dsCon);
    }


    /**
     *
     * @param message
     * @param trade
     * @param sender
     * @param rec
     * @param transferRules
     * @param transfer
     * @param format
     * @param con
     * @return If Bond CLEANPRICE is needed, DIRTYPRICE otherwise
     * @throws MessageFormatException
     */
    @Override
    public String parseDEAL_PRICE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, String format, DSConnection con) throws MessageFormatException {
        Trade clonedTradeForPriceTag = Optional.ofNullable(trade)
                .filter(t -> t.getProduct() instanceof Bond)
                .map(Trade::clone)
                .map(this::swapTradePrices)
                .orElse(trade);
        return super.parseDEAL_PRICE(message, clonedTradeForPriceTag, sender, rec, transferRules, transfer, format, con);
    }

    private Trade swapTradePrices(Trade trade) {
        double dirtyPrice = trade.getNegociatedPrice();
        double cleanPrice = trade.getTradePrice();
        trade.setNegociatedPrice(cleanPrice);
        trade.setTradePrice(dirtyPrice);
        return trade;
    }
}
