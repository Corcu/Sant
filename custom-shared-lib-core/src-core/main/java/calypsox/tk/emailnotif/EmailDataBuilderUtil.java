package calypsox.tk.emailnotif;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.FormatterUtil;
import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.bo.MessageFormatter;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.CustomerTransfer;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.refdata.UserDefaults;
import com.calypso.tk.service.DSConnection;
import org.jfree.util.Log;

public class EmailDataBuilderUtil {

    private static EmailDataBuilderUtil instance = new EmailDataBuilderUtil();


    public synchronized static EmailDataBuilderUtil getInstance() {

        if (instance == null) {
            instance = new EmailDataBuilderUtil();
        }

        return instance;
    }


    public String getProcessingOrgName(BOMessage message) {
        LegalEntity po = BOCache.getLegalEntity(DSConnection.getDefault(), message.getSenderId());
        if (null != po) {
            return po.getName();
        }
        return "";
    }

    String getCptyName(BOMessage message) {
        LegalEntity cpty = BOCache.getLegalEntity(DSConnection.getDefault(), message.getReceiverId());
        if (null != cpty) {
            return cpty.getName();
        }
        return "";
    }

    String getNotificationSign() {
        return "<span class=\"normal\">Thanks and Regards <br>\n" +
                "                     Collateral Management Operations Team <br>\n" +
                "                     Group E-mail address: <a href=\"cmanage.madrid@gruposantander.com\">cmanage.madrid@gruposantander.com</a><br></span>";
    }


    String getMccName(BOMessage message) {
        MarginCallConfig mcc = BOCache.getMarginCallConfig(DSConnection.getDefault(), getMccId(message));
        if (mcc != null) {
            return mcc.getName();
        }
        return "";
    }

    Integer getMccId(BOMessage message) {
        MarginCallConfig mcc = BOCache.getMarginCallConfig(DSConnection.getDefault(), message.getStatementId());
        if (mcc == null) {
            try {
                Trade trade = DSConnection.getDefault().getRemoteTrade().getTrade(message.getTradeLongId());
                return getMccFromProduct(trade).intValue();
            } catch (CalypsoServiceException e) {
                Log.error(this.getClass().getName() + "Cant retrieve the trade: " + message.getTradeLongId());
            }
        }
        return mcc != null ? mcc.getId() : 0;
    }

    public MarginCallConfig getMcc(BOMessage message) {
        return BOCache.getMarginCallConfig(DSConnection.getDefault(), getMccId(message));
    }

    String getAcadiaAmpId(BOMessage message) {
        String ampId = "";
        MarginCallConfig mcc = BOCache.getMarginCallConfig(DSConnection.getDefault(), message.getStatementId());
        if (mcc != null) {
            ampId = mcc.getAdditionalField("ACADIA_AMPID");
        }
        return !Util.isEmpty(ampId) ? ampId : "";
    }

    String getBodyFromTemplate(BOMessage message) throws CloneNotSupportedException, CalypsoServiceException, MessageFormatException {
        DSConnection dsCon = DSConnection.getDefault();
        PricingEnv defaultPricingEnv = null;

        final BOMessage clonedMessage = (BOMessage) message.clone();
        clonedMessage.setFormatType(FormatterUtil.HTML);
        final UserDefaults userDef = dsCon.getUserDefaults();
        if (userDef != null) {
            defaultPricingEnv = dsCon.getRemoteMarketData().getPricingEnv(userDef.getPricingEnvName());
        }
        return MessageFormatter.format(defaultPricingEnv, clonedMessage, true, dsCon);
    }

   Long getMccFromProduct(Trade trade){
        if (trade.getProduct() instanceof MarginCall){
            return ((MarginCall)trade.getProduct()).getLinkedLongId();
        }
       if (trade.getProduct() instanceof CustomerTransfer){
           return trade.getKeywordAsLongId("MC_CONTRACT_NUMBER");
       }
       return null;
    }
}
