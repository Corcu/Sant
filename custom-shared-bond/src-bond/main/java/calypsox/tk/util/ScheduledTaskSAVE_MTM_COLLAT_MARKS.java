package calypsox.tk.util;

import calypsox.tk.util.lakemtm.CollateralMTMPLMarkBuilder;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.StreamingTradeArray;
import com.calypso.tk.util.TradeArray;

import java.util.*;

/**
 * @author aalonsop
 */
public class ScheduledTaskSAVE_MTM_COLLAT_MARKS extends ScheduledTask {

    public static final String PRICING_ENV_SOURCE = "PricingEnv of source MTM Marks";
    public static final String SOURCE_PLMARK_NAME = "Source MTM Mark Name";

    @Override
    public boolean process(DSConnection ds, PSConnection ps) {
        TradeArray trades = Optional.ofNullable(BOCache.getTradeFilter(ds, getTradeFilter()))
                .map(tf -> getTrades(ds, tf)).orElse(new StreamingTradeArray());

        List<PLMark> marksToSave = new ArrayList<>();
        for (Trade trade : trades.getTrades()) {
            PLMark sourceMark = getSourceMark(trade, ds);
            if(sourceMark!=null) {
                createAndAddCollateralMarks(sourceMark, trade, marksToSave, ds);
            }
        }
        return saveMarks(marksToSave, ds);
    }


    private TradeArray getTrades(DSConnection ds, TradeFilter tf) {
        TradeArray trades = null;
        try {
            trades = ds.getRemoteTrade().getTrades(tf, getValuationDatetime());
        } catch (CalypsoServiceException exc) {
            Log.error(this.getClass().getSimpleName(), exc.getCause());
        }
        return trades;
    }


    private PLMark getSourceMark(Trade trade, DSConnection ds) {
        PLMark mark = null;
        String sourcePricingEnv = getAttribute(PRICING_ENV_SOURCE);
        try {
             mark=ds.getRemoteMark().getMark("PL",trade.getLongId(),"",sourcePricingEnv,getValuationDatetime().getJDate(TimeZone.getDefault()));
        } catch (PersistenceException exc) {
            Log.error(this.getClass().getSimpleName(), exc.getCause());
        }
        return mark;
    }

    private void createAndAddCollateralMarks(PLMark mark, Trade trade, List<PLMark> marksToSave, DSConnection ds) {
        String collatPricingEnv = getPricingEnv();
        String sourceMarkName = getAttribute(SOURCE_PLMARK_NAME);
        PLMarkValue sourceMarkValue = Optional.ofNullable(mark).map(mv->mv.getPLMarkValueByName(sourceMarkName)).orElse(null);

        if(sourceMarkValue!=null) {
            ScheduledTaskImportLakeMtM.MTMData mtmData = new ScheduledTaskImportLakeMtM.MTMData();
            mtmData.setTrade(trade);
            mtmData.setBaseCCY(sourceMarkValue.getCurrency());
            mtmData.setBaseMtM(sourceMarkValue.getMarkValue());
            mtmData.setBook(Optional.ofNullable(BOCache.getBook(ds, mark.getBookId()))
                    .map(Book::getName).orElse(""));
            mtmData.setDate(getValuationDatetime().getJDate(TimeZone.getDefault()));

            CollateralMTMPLMarkBuilder markBuilder = new CollateralMTMPLMarkBuilder();
            markBuilder.addPLMarkForTrade(marksToSave, trade, mtmData, collatPricingEnv, new HashMap<>(), 1);
        }

    }


    private boolean saveMarks(List<PLMark> marksToSave, DSConnection ds) {
        boolean res = true;
        try {
            int numberOfSaveMarks = ds.getRemoteMark().saveMarksWithAudit(marksToSave, false);
            Log.info(this.getClass().getSimpleName(), numberOfSaveMarks + " PLMarks were saved");
        } catch (PersistenceException exc) {
            Log.error(this.getClass().getSimpleName(), exc);
            res = false;
        }
        return res;
    }


    @Override
    public String getTaskInformation() {
        return "For every trade inside TradeFilter's perimeter, gets the desired PLMark from a given PricingEnv to set needed collateral PLMarks";
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<>(super.buildAttributeDefinition());
        attributeList.add(attribute(PRICING_ENV_SOURCE));
        attributeList.add(attribute(SOURCE_PLMARK_NAME));

        return attributeList;
    }
}
