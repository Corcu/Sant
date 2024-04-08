package calypsox.tk.bo;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

public class SantMessageFormatterUtil {

    private static SantMessageFormatterUtil instance = new SantMessageFormatterUtil();

    public synchronized static SantMessageFormatterUtil getInstance() {

        if (instance == null) {
            instance = new SantMessageFormatterUtil();
        }

        return instance;
    }


    public String parseDELIVERER_NAME(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                      Vector transferRules, BOTransfer transfer, DSConnection dsCon) {

        SettleDeliveryInstruction sdi;
        if (transfer.getPayReceive().equalsIgnoreCase("PAY")) {
            sdi = BOCache.getSettleDeliveryInstruction(dsCon, transfer.getInternalSettleDeliveryId());
        } else {
            sdi = BOCache.getSettleDeliveryInstruction(dsCon, transfer.getExternalSettleDeliveryId());
        }
        return null != sdi ? sdi.getDescription() : "";
    }

    public String parseRECEIVER_NAME(BOMessage message, Trade trade, LEContact sender, LEContact rec,
                                     Vector transferRules, BOTransfer transfer, DSConnection dsCon) {

        SettleDeliveryInstruction sdi;
        if (transfer.getPayReceive().equalsIgnoreCase("PAY")) {
            sdi = BOCache.getSettleDeliveryInstruction(dsCon, transfer.getExternalSettleDeliveryId());
        } else {
            sdi = BOCache.getSettleDeliveryInstruction(dsCon, transfer.getInternalSettleDeliveryId());
        }
        return null != sdi ? sdi.getDescription() : "";
    }
}
