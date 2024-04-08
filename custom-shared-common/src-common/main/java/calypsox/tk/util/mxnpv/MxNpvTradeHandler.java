package calypsox.tk.util.mxnpv;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author dmenendd
 */
public abstract class MxNpvTradeHandler {

    static final String FILE_SEPARATOR = ";";

    public static MxNpvTradeHandler getInstance(String productType){
        MxNpvTradeHandler matcherInstance= new BondNpvTradeHandler();
        if(productType!=null) {
            String simpleClassName = MxNpvTradeHandler.class.getSimpleName().replace("Mx", productType);
            try {
                String fullClassName= MxNpvTradeHandler.class.getName().replace(MxNpvTradeHandler.class.getSimpleName(),simpleClassName);
                matcherInstance = Optional.of(Class.forName(fullClassName))
                        .map(MxNpvTradeHandler::createInstance).orElse(new BondNpvTradeHandler());
            } catch (ClassNotFoundException exc) {
                Log.error(MxNpvTradeHandler.class, exc.getCause());
            }
        }
        return matcherInstance;
    }

    private static MxNpvTradeHandler createInstance(Class<?> matcherClass){
        MxNpvTradeHandler matcher=null;
        try {
            Object classInstance=matcherClass.newInstance();
            if(classInstance instanceof MxNpvTradeHandler){
                matcher= (MxNpvTradeHandler) classInstance;
            }
        } catch (InstantiationException | IllegalAccessException exc) {
          Log.error(MxNpvTradeHandler.class,exc.getCause());
        }
        return matcher;
    }

    public static JDate stringToDate(String datetime) {
        JDate date=null;
        String dFormat = "yyyy/MM/dd";
        SimpleDateFormat format = new SimpleDateFormat(dFormat);
        try {
            date=JDate.valueOf(format.parse(datetime));
        } catch (ParseException e) {
            Log.warn(Log.LOG, "Error parsing string to JDatetime (" + dFormat + ")" + e);
        }
        return date;
    }

    public void addPLMarkForTrade(List<PLMark> plMarks, Trade trade, MxNPVData npvData, String pricingEnv, int sign) {
        PLMark plMark = new PLMark();
        plMark.setTradeId(trade.getLongId());
        Book book = BOCache.getBook(DSConnection.getDefault(), trade.getBookId());
        if (book != null) {
            plMark.setBookId(book.getId());
        }
        plMark.setValDate(npvData.getProcessDate());
        plMark.setPricingEnvName(pricingEnv);
        plMark.setType("PL");

        addPLMarkValues(plMark,npvData,sign);

        plMarks.add(plMark);
    }

     PLMarkValue createPLMarkValue(String name, String ccy, String ccy2, double npv) {
        PLMarkValue npvPriceMarkValue = new PLMarkValue();
        npvPriceMarkValue.setMarkName(name);
        npvPriceMarkValue.setMarkValue(npv);
        npvPriceMarkValue.setCurrency(ccy);
        npvPriceMarkValue.setOriginalCurrency(ccy2);
        return npvPriceMarkValue;
    }

    public abstract void addPLMarkValues(PLMark plMark, MxNPVData npvData, int sign);

    public abstract void matchTrades(Map<String, MxNPVData> npvAllDataMap, TradeArray tradearray, String keyword);

    public abstract Map<String, MxNPVData> parseLines(List<String> lines);
}
