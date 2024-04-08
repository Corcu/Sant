package calypsox.tk.report;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageArray;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

public class SantInterestPaymentUtil {

    public static final String  PAYMENTMSG = "PAYMENTMSG";
    public static final String  MC_INTEREST = "MC_INTEREST";

    private static SantInterestPaymentUtil instance = new SantInterestPaymentUtil();

    public synchronized static SantInterestPaymentUtil getInstance() {

        if (instance == null) {
            instance = new SantInterestPaymentUtil();
        }

        return instance;
    }


    public void reloadMessages(SantInterestPaymentRunnerEntry entry){
        if(entry.getCtTrade()!=null){
            HashMap<String, BOMessage> latestMessages = getLatestMessages(entry.getCtTrade());
            if(!Util.isEmpty(latestMessages)){
                //Updating Messages for row
                entry.setPaymentMessage(latestMessages.get(PAYMENTMSG));
                entry.setInterest(latestMessages.get(MC_INTEREST));
            }
        }else{
            entry.setPaymentMessage(null);
            entry.setInterest(null);
        }
    }

    /**
     * Return last SWIFT-> PAYMENTMSG message and last MC_INTEREST message for the trade.
     *
     * @param trade
     * @return
     */
    public HashMap<String, BOMessage> getLatestMessages(Trade trade){
        HashMap<String,BOMessage> latestMessages = new HashMap<>();

        if(null!=trade){
            StringBuilder where = new StringBuilder();
            where.append(" TRADE_ID = " + trade.getLongId());
            where.append(" AND message_type IN ('MC_INTEREST','PAYMENTMSG')");
            try {
                MessageArray messages = DSConnection.getDefault().getRemoteBO().getMessages(where.toString(), null);

                BOMessage paymentMessage = Arrays.stream(messages.getMessages())
                        .filter(message -> PAYMENTMSG.equalsIgnoreCase(message.getMessageType()))
                        .max(Comparator.comparingLong(BOMessage::getLongId)).orElse(null);

                latestMessages.put(PAYMENTMSG,paymentMessage);

                BOMessage mcInterestMessage = Arrays.stream(messages.getMessages())
                        .filter(message -> MC_INTEREST.equalsIgnoreCase(message.getMessageType()))
                        .max(Comparator.comparingLong(BOMessage::getLongId)).orElse(null);

                latestMessages.put(MC_INTEREST,mcInterestMessage);

            } catch (CalypsoServiceException e) {
                Log.error(this,"Error loading messages: " + e );
            }

        }
        return latestMessages;
    }

}
