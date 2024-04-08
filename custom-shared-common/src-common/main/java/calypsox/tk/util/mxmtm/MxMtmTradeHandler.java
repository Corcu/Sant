package calypsox.tk.util.mxmtm;

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
 * @author aalonsop
 */
public abstract class MxMtmTradeHandler{

    static final String FILE_SEPARATOR = ";";

    public static MxMtmTradeHandler getInstance(String productType){
        MxMtmTradeHandler matcherInstance=new RepoMtmTradeHandler();
        if(productType!=null) {
            String simpleClassName = MxMtmTradeHandler.class.getSimpleName().replace("Mx", productType);
            try {
                String fullClassName=MxMtmTradeHandler.class.getName().replace(MxMtmTradeHandler.class.getSimpleName(),simpleClassName);
                matcherInstance = Optional.of(Class.forName(fullClassName))
                        .map(MxMtmTradeHandler::createInstance).orElse(new RepoMtmTradeHandler());
            } catch (ClassNotFoundException exc) {
                Log.error(MxMtmTradeHandler.class, exc.getCause());
            }
        }
        return matcherInstance;
    }

    private static MxMtmTradeHandler createInstance(Class<?> matcherClass){
        MxMtmTradeHandler matcher=null;
        try {
            Object classInstance=matcherClass.newInstance();
            if(classInstance instanceof MxMtmTradeHandler){
                matcher= (MxMtmTradeHandler) classInstance;
            }
        } catch (InstantiationException | IllegalAccessException exc) {
          Log.error(MxMtmTradeHandler.class,exc.getCause());
        }
        return matcher;
    }

    public static JDate stringToDate(String datetime) {
        JDate date=null;
        String dFormat = "dd/MM/yyyy";
        SimpleDateFormat format = new SimpleDateFormat(dFormat);
        try {
            date=JDate.valueOf(format.parse(datetime));
        } catch (ParseException e) {
            Log.warn(Log.LOG, "Error parsing string to JDatetime (" + dFormat + ")" + e);
        }
        return date;
    }

    public void addPLMarkForTrade(List<PLMark> plMarks, Trade trade, MxMTMData mtmData, String pricingEnv, int sign) {
        PLMark plMark = new PLMark();
        plMark.setTradeId(trade.getLongId());
        Book book = BOCache.getBook(DSConnection.getDefault(), trade.getBookId());
        if (book != null) {
            plMark.setBookId(book.getId());
        }
        plMark.setValDate(mtmData.getProcessDate());
        plMark.setPricingEnvName(pricingEnv);
        plMark.setType("PL");

        addPLMarkValues(plMark,mtmData,sign);

        plMarks.add(plMark);
    }

     PLMarkValue createPLMarkValue(String name, String ccy, String ccy2, double mtm) {
        PLMarkValue npvPriceMarkValue = new PLMarkValue();
        npvPriceMarkValue.setMarkName(name);
        npvPriceMarkValue.setMarkValue(mtm);
        npvPriceMarkValue.setCurrency(ccy);
        npvPriceMarkValue.setOriginalCurrency(ccy2);
        return npvPriceMarkValue;
    }

    public abstract void addPLMarkValues(PLMark plMark, MxMTMData mtmData, int sign);

    public abstract void matchTades(Map<String, MxMTMData> mtmAllDataMap, TradeArray tradearray, String keyword);

    public abstract Map<String, MxMTMData> parseLines(List<String> lines);
}
