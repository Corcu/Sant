package calypsox.tk.bo;


import calypsox.util.partenon.PartenonUtil;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatter;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;
import java.io.StringWriter;
import java.util.Vector;


public class PARTENONMSGCAMessageFormatter extends MessageFormatter {


    /** Code format */
    private static final String BOBOIRDCA = "BOBOIRDCA";


    public String parsePARTENON_ROW(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector, BOTransfer transfer, DSConnection dsConn) {
        String partenonMessage = "";
        if(null!=trade){
            partenonMessage = generateMsg(trade);
        }
        return partenonMessage;
    }


    /**
     * Generate Message
     *
     * @param trade
     * @return
     */
    private String generateMsg(Trade trade){
        StringWriter sw = new StringWriter();
        sw.append("CALYPSO").append(";");
        sw.append(PartenonUtil.getInstance().getAction(trade)).append(";");
        sw.append(String.valueOf(trade.getLongId())).append(";");
        sw.append(trade.getBook().getName()).append(";");
        sw.append(trade.getCounterParty().getExternalRef()).append(";");
        sw.append(getAlias(trade)).append(";");

        return sw.toString();
    }


    /**
     * Get Alias
     *
     * @return
     */
    public String getAlias(Trade trade){
        //Descripción del producto relacionado con el sistema a tratar
        return BOBOIRDCA;
    }

}
