package calypsox.tk.util;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.risk.pl.TradeID;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import org.apache.commons.collections4.ListUtils;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class ScheduledTaskSTC_RV_COPY_MX_MTM extends ScheduledTask {
    @Override
    public String getTaskInformation() {
        return "Copy MTM PLMarks from D-N to D";
    }


    @Override
    protected boolean process(DSConnection ds, PSConnection ps) {
        //Load tradeIds - TF
        String tradeFilter = getTradeFilter();
        JDatetime valuationDatetime = getValuationDatetime(false);


        if(!Util.isEmpty(tradeFilter)){
            TradeFilter tf = BOCache.getTradeFilter(ds, tradeFilter);
            List<TradeID> tradeIds = ds.getRemoteTrade().getTradeIds(tf, valuationDatetime);
            Log.system(this.getClass().getName(),"Trades loaded: " + tradeIds.size());
            List<List<TradeID>> partition = ListUtils.partition(tradeIds, 999);

            //TODO Load and generate new PLMarks
            partition.stream().parallel().forEach(tradeIDS -> cloneAndSavePLMarks(tradeIDS,valuationDatetime));

        }

        return super.process(ds, ps);
    }

    private void cloneAndSavePLMarks(List<TradeID> tradesIDs,JDatetime valuationDatetime){

        if(!Util.isEmpty(tradesIDs)){
            ConcurrentLinkedQueue<PLMark> plMarksToSave = new ConcurrentLinkedQueue<>();

            List<Long> tradeIdsList = tradesIDs.stream().map(TradeID::getLongId).collect(Collectors.toList());
            Collection<PLMark> plMarks = loadPLMarks(tradeIdsList, valuationDatetime);
            if(!Util.isEmpty(plMarks)){
                plMarks.stream().parallel().forEach(plMark -> {
                    try {
                        //ValDate
                        JDate valDate = getValuationDatetime(false).getJDate(TimeZone.getDefault());

                        PLMark plMarkToSave = (PLMark) plMark.clone();
                        plMarkToSave.setId(0);
                        plMarkToSave.setVersion(0);
                        plMarkToSave.setValDate(valDate);


                        List<PLMarkValue> markValuesAsList = plMarkToSave.getMarkValuesAsList();

                        markValuesAsList.stream().forEach(mark -> {
                            mark.setMarkId(0);
                            mark.setAdjustmentType("Roll MTM D-1");
                        });

                        plMarksToSave.add(plMarkToSave);

                    } catch (CloneNotSupportedException e) {
                        Log.error("","Error: " + e);
                    }
                });

                //Save all cloned PLMarks
                savePLMarks(plMarksToSave);
            }
        }
    }


    private Collection<PLMark> loadPLMarks(List<Long> tradeIds,JDatetime valDate){
        String pricingEnvName = getPricingEnv();
        Collection<PLMark> plMarksToClone = new ArrayList<>();

        JDate jDate = valDate.getJDate(TimeZone.getDefault()).addBusinessDays(getValDateOffset(), getHolidays());
        try {
            if(!Util.isEmpty(tradeIds)){
                Collection<PLMark> plMarks = DSConnection.getDefault()
                        .getRemoteMark()
                        .getMarks("PL", tradeIds, pricingEnvName, jDate);
                Collection<PLMark> nonePLMarks = DSConnection.getDefault()
                        .getRemoteMark()
                        .getMarks("NONE", tradeIds, pricingEnvName, jDate);
                plMarksToClone.addAll(plMarks);
                plMarksToClone.addAll(nonePLMarks);
            }
        } catch (Exception e) {
            Log.error(this, e);
        }
        Log.system(this.getClass().getName(),"PLMarks: " + plMarksToClone.size());
        return plMarksToClone;
    }


    private void savePLMarks(Collection<PLMark> plMarksToSave){
        try {
            if(!Util.isEmpty(plMarksToSave)){
                DSConnection.getDefault()
                        .getRemoteMark().saveMarksWithAudit(plMarksToSave,true);
            }
        } catch (PersistenceException e) {
            Log.system("","Error: " + e);
        }
    }
}
