package calypsox.tk.bo;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatter;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.product.PerformanceSwapLeg;
import com.calypso.tk.product.PerformanceSwappableLeg;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;

import java.io.StringWriter;
import java.util.Optional;
import java.util.Vector;

/**
 * @author acd
 */
public class PARTENONMSGPerformanceSwapMessageFormatter extends MessageFormatter {

    public String parsePARTENON_ROW(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                       BOTransfer transfer, DSConnection dsConn) {
        String partenonMessage = "";
        if(null!=trade){
             partenonMessage = generateMsg(trade);
        }
        return partenonMessage;
    }

    /**
     * 0:Alta 1:Baja 2:Modificacion
     * @param trade
     * @return
     */
    private String generateMsg(Trade trade){
        StringWriter sw = new StringWriter();
        sw.append("CALYPSO").append(";");
        sw.append("0").append(";");
        sw.append(String.valueOf(trade.getLongId())).append(";");
        sw.append(trade.getBook().getName()).append(";");
        sw.append(trade.getCounterParty().getExternalRef()).append(";");
        sw.append(getProductSubType(trade)).append(getMirrorBook(trade)).append(";");

        return sw.toString();
    }

    /**
     * Get Product SubType
     *
     * Si pago la financiación es comprado (entiendo que tomado) -> recibimos performance del bono
     * Si recibo la financiación es vendido (entiendo que cedido) -> Pagamos prefromance del bono
     *
     * 512
     * 510
     *
     * @return
     */
    private String getProductSubType(Trade trade){
        final PerformanceSwap product = (PerformanceSwap) trade.getProduct();
        PerformanceSwappableLeg primaryLeg = product.getPrimaryLeg();
        PerformanceSwapLeg primLeg = null;
        boolean perfLeg = false;

        if (primaryLeg instanceof PerformanceSwapLeg) {
            perfLeg = true;
            primLeg = (PerformanceSwapLeg)primaryLeg;
        }
        String primayLegDesc = null;
        if (perfLeg) {
            if (primLeg.getNotional() < 0.0D) {
                primayLegDesc = "Pay";
            } else if (primLeg.getNotional() == 0.0D && (trade.isAllocationParent() || trade.isAllocationChild()) && trade.getQuantity() < 0.0D) {
                primayLegDesc = "Pay";
            } else {
                primayLegDesc = "Receive";
            }
        }

        if("Pay".equalsIgnoreCase(primayLegDesc)){
            return "BONDS";
        }else{
            return "BONDB"; //Default
        }
    }

    private String getMirrorBook(Trade trade){
       return Optional.ofNullable(trade.getMirrorBook()).isPresent() ? "I" : "";
    }

}
