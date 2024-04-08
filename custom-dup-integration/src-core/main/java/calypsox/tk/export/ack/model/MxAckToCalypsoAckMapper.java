package calypsox.tk.export.ack.model;

import com.calypso.tk.publish.jaxb.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author aalonsop
 */
public class MxAckToCalypsoAckMapper {


    public CalypsoAcknowledgement map(MxDefaultAckBean mxAck){
        CalypsoAcknowledgement calAck=new CalypsoAcknowledgement();
        calAck.setCalypsoTrades(mapCalypsoTrades(mxAck));
        return calAck;
    }


    private CalypsoTrades mapCalypsoTrades(MxDefaultAckBean mxAck){
        CalypsoTradeWrapper tradeAck=new CalypsoTradeWrapper();
        CalypsoTradesWrapper tradesWrapper=new CalypsoTradesWrapper();
        if(mxAck!=null) {
            tradeAck.setCalypsoTradeId(mxAck.getTransactionId());
            tradeAck.setExternalRef(mxAck.getMessageId());
            tradeAck.setAction(mxAck.getAction());
            tradeAck.setStatus(mxAck.getStatus());
            if("NACK".equalsIgnoreCase(mxAck.getStatus())) {
                tradeAck.setErrors(mxAck.getErrorDescription());
            }
        }
        List<CalypsoTrade> tradeList=new ArrayList<>();
        tradeList.add(tradeAck.getCalypsoTrade());
        tradesWrapper.setCalypsoTradeList(tradeList);
        return tradesWrapper.getCalypsoTrades();
    }
}
