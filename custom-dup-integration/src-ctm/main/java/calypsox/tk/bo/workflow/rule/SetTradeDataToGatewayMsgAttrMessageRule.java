package calypsox.tk.bo.workflow.rule;

import calypsox.ctm.CTMBlockTradeEnricher;
import com.calypso.tk.bo.*;
import com.calypso.tk.bo.workflow.WfMessageRule;
import com.calypso.tk.core.CalypsoException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.jaxb.Bond;
import com.calypso.tk.upload.jaxb.CalypsoObject;
import com.calypso.tk.upload.jaxb.CalypsoTrade;
import com.calypso.tk.upload.jaxb.Product;
import com.calypso.tk.upload.services.GatewayUtil;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Optional;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class SetTradeDataToGatewayMsgAttrMessageRule implements WfMessageRule {


    @Override
    public boolean check(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Adds relevant CDUF's trade data into GATEWAYMSG's attributes";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOMessage message, BOMessage oldMessage, Trade trade, BOTransfer transfer, Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        try {
            CalypsoObject calypsObject = GatewayUtil.getCalypsoObject(message, excps, dbCon);
            Optional.ofNullable(calypsObject)
                    .filter(calypsoObject -> calypsObject instanceof CalypsoTrade)
                    .filter(calypsotrade -> isBondCDUF((CalypsoTrade) calypsotrade))
                    .ifPresent(calypsotrade -> addTradeDataIntoMsgAttributes(message, (CalypsoTrade) calypsotrade));

        } catch (CalypsoException exc) {
            Log.error(this, "Couldn't add trade attributes to GATEWAYMSG: " + exc.getCause());
        }
        return true;
    }


    public void addTradeDataIntoMsgAttributes(BOMessage boMessage, CalypsoTrade calypsoTrade) {
        boMessage.setAttribute("Counterparty", calypsoTrade.getTradeCounterParty());
        boMessage.setAttribute("TradeDate", Optional.ofNullable(calypsoTrade.getTradeDateTime()).map(XMLGregorianCalendar::toString).orElse(""));
        boMessage.setAttribute("SettleDate", Optional.ofNullable(calypsoTrade.getTradeSettleDate()).map(XMLGregorianCalendar::toString).orElse(""));
        boMessage.setAttribute("ISIN", getIsinFromCalypsoTrade(calypsoTrade));
        boMessage.setAttribute("BlockTradeDetail", getTradePlatform(calypsoTrade));
    }


    private String getIsinFromCalypsoTrade(CalypsoTrade calypsoTrade) {
        return Optional.ofNullable(calypsoTrade)
                .map(CalypsoTrade::getProduct)
                .map(Product::getBond)
                .map(Bond::getProductCodeValue)
                .orElse("");
    }

    private String getTradePlatform(CalypsoTrade calypsoTrade) {
        return Optional.ofNullable(BOCache.getLegalEntity(DSConnection.getDefault(), calypsoTrade.getTradeCounterParty()))
                .map(CTMBlockTradeEnricher::getLegalEntityGestoraType)
                .orElse("");
    }

    private boolean isBondCDUF(CalypsoTrade calypsoTrade) {
        return Bond.class.getSimpleName().equals(calypsoTrade.getProductType());
    }
}
