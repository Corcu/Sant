package calypsox.tk.bo;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatter;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;

import java.io.StringWriter;
import java.util.Optional;
import java.util.Vector;

public class PARTENONMSGSecLendingMessageFormatter  extends MessageFormatter {
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
    public String generateMsg(Trade trade){
        StringWriter sw = new StringWriter();
        sw.append("CALYPSO").append(";");
        sw.append("0").append(";");
        sw.append(String.valueOf(trade.getLongId())).append(";");
        sw.append(trade.getBook().getName()).append(";");
        sw.append(trade.getCounterParty().getExternalRef()).append(";");
        sw.append(getProductSubType(trade)).append(";");

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
    public String getProductSubType(Trade trade){
        final SecLending product = (SecLending) trade.getProduct();
        String direction = product.getDirection();

        Product security = product.getSecurity();
        Book book = trade.getBook();
        String seclending_adjFee = "";
        String accountingLink = "";
        String result = "";
        if(null!=book && null!=security){
            accountingLink = trade.getBook().getAccountingBook().getName();
            seclending_adjFee = book.getAttribute("Seclending_AdjFee");
            if(!Util.isEmpty(seclending_adjFee)){
                if("RF".equalsIgnoreCase(seclending_adjFee)){
                    result = "PDVFEERF";
                }else if("RV".equalsIgnoreCase(seclending_adjFee)){
                    result = "PDVFEERV";
                }
            }else if("Borrow".equalsIgnoreCase(direction)){ //TOMADO
                if(security instanceof Bond){ //RF
                    if("Negociacion".equalsIgnoreCase(accountingLink)){
                        if(isInternal(trade)){
                            result = "PDVTRFNEGI";
                        }else{
                            result = "PDVTRFNEG";
                        }
                    }else if("Disponible para la venta".equalsIgnoreCase(accountingLink)){
                        if(isInternal(trade)){
                            result = "PDVTRFDISI";
                        }else{
                            result = "PDVTRFDIS";
                        }
                    }else if("Otros a valor razonable".equalsIgnoreCase(accountingLink)){
                        if(isInternal(trade)){
                            result = "PDVTRFRAZI";
                        }else{
                            result = "PDVTRFRAZ";
                        }
                    }else if("Inversion Crediticia".equalsIgnoreCase(accountingLink)
                            || "Inversion a vencimiento".equalsIgnoreCase(accountingLink)){
                        if(isInternal(trade)){
                            result = "PDVTRFAMOI";
                        }else{
                            result = "PDVTRFAMO";
                        }
                    }
                }else if(security instanceof Equity){ //RV
                    if(isInternal(trade)){
                        result = "PDVTRVI";
                    }else{
                        result = "PDVTRV";
                    }
                }
            }else if("Lend".equalsIgnoreCase(direction)){ //PRESTADO
                if(security instanceof Bond){ //RF
                    if("Negociacion".equalsIgnoreCase(accountingLink)){
                        if(isInternal(trade)){
                            result = "PDVPRFNEGI";
                        }else{
                            result = "PDVPRFNEG";
                        }
                    }else if("Disponible para la venta".equalsIgnoreCase(accountingLink)){
                        if(isInternal(trade)){
                            result = "PDVPRFDISI";
                        }else{
                            result = "PDVPRFDIS";
                        }
                    }else if("Otros a valor razonable".equalsIgnoreCase(accountingLink)){
                        if(isInternal(trade)){
                            result = "PDVPRFRAZI";
                        }else{
                            result = "PDVPRFRAZ";
                        }
                    }else if("Inversion Crediticia".equalsIgnoreCase(accountingLink)
                            || "Inversion a vencimiento".equalsIgnoreCase(accountingLink)){
                        if(isInternal(trade)){
                            result = "PDVPRFAMOI";
                        }else{
                            result = "PDVPRFAMO";
                        }
                    }
                }else if(security instanceof Equity){ //RV
                    if(isInternal(trade)){
                        result = "PDVPRVI";
                    }else{
                        result = "PDVPRV";
                    }
                }
            }
        }
        //comprobar si es de adjustement si el book tiene el atribute
        return result;
    }

    private boolean isInternal(Trade trade){
        return Optional.ofNullable(trade.getMirrorBook()).isPresent();
    }
}
