package calypsox.tk.report;


import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.CA;
import com.calypso.tk.product.Equity;
import com.calypso.tk.report.MessageReport;
import com.calypso.tk.util.MessageArray;
import java.util.Iterator;


public class MT566toIGMessageReport extends MessageReport {


    public MessageArray loadMessages() throws Exception {
        MessageArray messageArray = super.loadMessages();
        MessageArray finalMessageArray = new MessageArray();
        if(messageArray!=null && messageArray.size()>0){
            Iterator<BOMessage> ite = messageArray.iterator();
            while (ite.hasNext()) {
                final BOMessage message = ite.next();

                BOTransfer transfer = this.getDSConnection().getRemoteBO().getBOTransfer(message.getTransferLongId());
                if(transfer==null){
                    Log.system(this.getClass().toString(), "Could not get transfer " + message.getTransferLongId() + " for message with id " + message.getLongId());
                    continue;
                }
                boolean isXferSettled = (transfer!=null && "SETTLED".equalsIgnoreCase(transfer.getStatus().getStatus())) ? true : false;

                Trade trade =  this.getDSConnection().getRemoteTrade().getTrade(message.getTradeLongId());
                if(trade==null){
                    Log.system(this.getClass().toString(), "Could not get trade " + message.getTradeLongId() + " for message with id: " + message.getLongId());
                    continue;
                }

                Book book = trade.getBook();
                if(book==null){
                    Log.system(this.getClass().toString(), "Could not get book for trade with id: " + trade.getLongId());
                    continue;
                }
                boolean isCaBook = (book!=null && "CA_BOOK".contains(book.getName())) ? true : false;

                CA ca = (CA) trade.getProduct();
                if(ca==null){
                    Log.system(this.getClass().toString(), "Could not get CA Product for trade with id: " + trade.getLongId());
                    continue;
                }

                if((trade.getProduct() instanceof CA) && (ca.getUnderlyingProduct() instanceof Equity)) {
                    Equity equity = (Equity) ca.getUnderlyingProduct();
                    boolean isIsinEspanol = !Util.isEmpty(equity.getCountry()) && "SPAIN".equalsIgnoreCase(equity.getCountry()) ? true : false;
                    Double retention = ca.calcGrossAmount(trade) - ca.calcSettlementAmount(trade);
                    if(!isIsinEspanol && retention!=0.0 && isXferSettled && isCaBook){
                        finalMessageArray.add(message);
                    }
                } else if ((trade.getProduct() instanceof CA) && (ca.getUnderlyingProduct() instanceof Bond)) {
                    //Bond bond = (Bond) ca.getUnderlyingProduct();
                    // TO_DO
                }
            }
        }
        return finalMessageArray;
    }


}
