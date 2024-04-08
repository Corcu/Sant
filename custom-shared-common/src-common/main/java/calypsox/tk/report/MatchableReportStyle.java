package calypsox.tk.report;

import calypsox.tk.report.matchable.MatchableSwiftHandler;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.matching.Matchable;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.Optional;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class MatchableReportStyle extends com.calypso.tk.report.MatchingReportStyle {

    public static String SAFEACC_TAG="97A SAFE";
    public static String PTYB="95Q PTYB";
    public static String PREP="98C PREP";
    public static String TEXA="19A TEXA";
    public static String SEME="20C SEME";
    public static String RELA="20C RELA";
    public static String TRAD="94B TRAD";
    public static String PSET="95P PSET";
    public static String SETT="19A SETT";
    public static String FUNCTION="23G Function";
    public static String PREV="20C PREV";
    public static String ISIN="35B ISIN";
    public static String ACOW="95P ACOW";
    public static String SAFE="97B SAFE";
    public static String SETR="22F SETR";
    public static String STCO="22F STCO";
    public static String DEAG="95P DEAG";
    public static String SELL="95Q SELL";
    public static String DECU="95Q DECU";
    public static String REAG="95P REAG";
    public static String BUYR="95R BUYR";
    public static String RECU="95Q RECU";
    public static String PAYM="22H PAYM";
    public static String REDE="22H REDE";
    public static String MSG_CPTY="Counterparty";

    public static String MSG_PRODUCT_TYPE="Product Type";
    public static String TRADE_MX_ID="Trade Mx Id";



    private final transient MatchableSwiftHandler swiftHandler=new MatchableSwiftHandler();


    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors){
        Object result=super.getColumnValue(row, columnId, errors);
        if(row!=null&&result==null) {
            Matchable matchable = row.getProperty("Matchable");
            try {
                BOMessage message = this.swiftHandler.getBOMessage(matchable);
                result=this.swiftHandler.handleSwiftTagColumn(columnId,message);
                if(MSG_PRODUCT_TYPE.equals(columnId)){
                    result=getUnderlyingProductType(message);
                }else if(TRADE_MX_ID.equals(columnId)){
                    result=getUnderlyingMxId(matchable,message);
                }
            } catch (RemoteException exc) {
               Log.error(this,exc.getCause());
            }
        }

        return result;
    }


    private String getUnderlyingProductType(BOMessage message){
       return Optional.ofNullable(message).map(BOMessage::getProductType).orElse("");
    }

    private String getUnderlyingMxId(Matchable matchable, BOMessage boMessage){
        String mxId="";
        long msgTradeId=Optional.ofNullable(boMessage).map(BOMessage::getTradeLongId)
                .orElse(0L);
        if(!matchable.isIncoming()&&msgTradeId>0L){
            try {
                Trade trade=DSConnection.getDefault().getRemoteTrade().getTrade(msgTradeId);
                mxId=Optional.ofNullable(trade).map(t->t.getKeywordValue("Contract Id")).orElse("");
            } catch (CalypsoServiceException exc) {
               Log.error(this,exc.getCause());
            }

        }
        return mxId;
    }

    @Override
    public TreeList getTreeList() {
        TreeList customList=new TreeList("Custom");
        customList.add(MSG_PRODUCT_TYPE);
        customList.add(TRADE_MX_ID);


        TreeList fullList=super.getTreeList();
        fullList.add(initSwiftTreeList());
        fullList.add(customList);
        return fullList;
    }

    private TreeList initSwiftTreeList(){
        TreeList swiftList=new TreeList("Swift Tags");
        for(String column:this.swiftHandler.getSwiftColumns()){
            swiftList.add(column);
        }
        return swiftList;
    }

}
