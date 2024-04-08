package calypsox.tk.swift.formatter;

import calypsox.tk.swift.formatter.common.CustomSwiftTagHandler;
import calypsox.tk.swift.formatter.seclending.UtilSecLendingSWIFTFormatter;
import com.calypso.helper.CoreAPI;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.bo.swift.SwiftUtil;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Vector;

@SuppressWarnings("unused")
public class MT541CRESTSWIFTFormatter extends MT541SWIFTFormatter {

    private static final String CRST = "CRST";

    public String parseCREST_FORM(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        if (transfer == null) {
            return "";
        } else {
            return ":FORM/" + CRST + "/CRGB";
        }
    }

    public String parseCREST_RPOR(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        if (transfer == null) {
            return "";
        } else {
            return ":RPOR/" + CRST + "/TRRE";
        }
    }

    public String parseCREST_STCO(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        if (transfer == null) {
            return "";
        } else {
            return ":STCO/" + CRST + "/TRAD";
        }
    }

    public String parseCREST_TCPI(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        int id = transfer.getInternalSettleDeliveryId();
        SettleDeliveryInstruction si = BOCache.getSettleDeliveryInstruction(dsCon, id);
        String bAccount = si.getAgentIdentifier();

        if (transfer == null) {
            return "";
        } else if (bAccount.equals("CEUAG")) {
            return ":TCPI//PRIN";
        } else {
            return "";
        }
    }


    public String parseCREST_NARRATIVE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        Vector<String> domainCache = LocalCache.getDomainValues(dsCon, "CREST.IUSE");
        Vector<String> crestSwiftModeDV = (LocalCache.getDomainValues(dsCon, "CRESTSWIFT_MODE"));
        String iuse = "";
        String swiftMode = "";

        String uftqNumber = message.getAttribute("Crest_UFTQ");

        if (transfer == null) {
            return "";
        } else if (domainCache.size() > 0){
            iuse = domainCache.get(0);
        }
        if (transfer == null) {
            return "";
        } else if (crestSwiftModeDV.size() > 0){
            swiftMode = crestSwiftModeDV.get(0);
        }

        return "/HSMT/ZHDR" + SwiftMessage.END_OF_LINE +
                ":79:/HVRN/ISO" + SwiftMessage.END_OF_LINE +
                ":79:/HMDE/" + swiftMode + SwiftMessage.END_OF_LINE +
                ":79:/UFTQ/"+ uftqNumber + SwiftMessage.END_OF_LINE +
                ":79:/IUSE/" + iuse + SwiftMessage.END_OF_LINE +
                ":79:/UOPR/CREST1" + SwiftMessage.END_OF_LINE +
                ":79:/INUM/541" + SwiftMessage.END_OF_LINE +
                ":79:/IREF/CYO" + CoreAPI.getId(message);

    }

    public String parseCREST_SETTLEMENT_PARTY(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, String format, DSConnection con) {
        TAG_95 = super.parseSETTLEMENT_PARTY(message, trade, sender, rec, transferRules, transfer, format, con);
        int id = transfer.getInternalSettleDeliveryId();
        SettleDeliveryInstruction si = BOCache.getSettleDeliveryInstruction(con, id);
        String bAccount = si.getAgentIdentifier();
        int id2 = transfer.getExternalSettleDeliveryId();
        SettleDeliveryInstruction si2 = BOCache.getSettleDeliveryInstruction(con, id2);
        String mAccount = si2.getAgentIdentifier();
        this._tagValue.setOption("R");

        if (TAG_95.startsWith(":PSET//UNKNOWN")){
            this._tagValue.setOption("P");
            return TAG_95.substring(0,7) + message.getSenderAddressCode();
        } else if (TAG_95.startsWith(":SELL//") || TAG_95.startsWith(":DEAG//") || TAG_95.startsWith(":REAG//")) {
            return TAG_95.substring(0,6) + CRST + "/" + mAccount;
        } else {
            TAG_95 = ":BUYR/";
            return ":BUYR/" + CRST + "/" + bAccount;
        }
    }

    public String parseCREST_BUYR(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        if (transfer == null || TAG_95.startsWith(":BUYR/")){
            return "";
        } else {
            int id = transfer.getInternalSettleDeliveryId();
            SettleDeliveryInstruction si = BOCache.getSettleDeliveryInstruction(dsCon, id);
            String bAccount = si.getAgentIdentifier();
            return ":BUYR/" + CRST + "/" + bAccount;
        }
    }

    public String parseCREST_SECURITY_DESCRIPTION(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon){
        String resultCRESTIsin = super.parseSECURITY_DESCRIPTION(message, trade, sender, rec, transferRules, transfer, dsCon);
        return resultCRESTIsin.substring(0,17);
    }

    public String parseCREST_INDICATOR(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        String value = new CustomSwiftTagHandler().parseIndicatorForNetting(transfer, dsCon);
        String indicator = this.getIndicatorForCA(message, trade, sender, rec, transferRules, transfer, dsCon);
        if (Util.isEmpty(value) && Util.isEmpty(indicator)) {
            value = UtilSecLendingSWIFTFormatter.customizeMessageIndicator(super.parseINDICATOR(message, trade, sender, rec, transferRules, transfer, dsCon), trade, transfer);
            return value;
        }
        if (!":SETR//TRAD".equals(indicator)){
            String newIndicator = ":SETR//TRAD";
            return newIndicator;
        } else {
            return indicator;
        }
    }

    public String parseCREST_PLACE_TRADE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        if (transfer == null) {
            return "";
        } else if (trade!=null && "Equity".equals(trade.getProductType())) {
            return super.parsePLACE_TRADE(message, trade, sender, rec, transferRules, transfer, dsCon);
        } else {
            this._tagValue.setOption("B");
            return ":TRAD//EXCH/XLON";
        }
    }

    public boolean isNotTagBUYR(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection con) throws MessageFormatException {
        return !TAG_95.startsWith(":BUYR/");
    }


}
