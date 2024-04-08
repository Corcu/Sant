package calypsox.tk.report;

import calypsox.tk.report.globalmta.CollateralConfigMTAGroup;
import com.calypso.tk.core.SignedAmount;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportRow;

import java.util.Vector;

/**
 * @author aalonsop
 */
public class GlobalMTACollateralReportStyle extends CollateralConfigReportStyle{

    public static final String TOTAL_MTA_OWNER_USD="TOTAL MTA Owner USD";
    public static final String TOTAL_MTA_CPTY_USD="TOTAL MTA Cpty USD";
    public static final String TOTAL_MTA_OWNER_EUR="TOTAL MTA Owner EUR";
    public static final String TOTAL_MTA_CPTY_EUR="TOTAL MTA Cpty EUR";
    public static final String BSNY_FLAG="BSNY";
    public static final String LIVE_TRADES_FLAG="Live Trades";
    public static final String CONTRACTNAMES="Contract Names";

    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) {
        Object result;
        CollateralConfigMTAGroup configs=row.getProperty(CollateralConfigMTAGroup.class.getSimpleName());
        if(TOTAL_MTA_OWNER_USD.equals(columnName)){
            result=new SignedAmount(configs.getTotalMTAOwner());
        }else if(TOTAL_MTA_CPTY_USD.equals(columnName)){
            result=new SignedAmount(configs.getTotalMTACpty());
        }else if(TOTAL_MTA_OWNER_EUR.equals(columnName)){
            result= new SignedAmount(configs.getTotalMTAOwnerEUR());
        }else if(TOTAL_MTA_CPTY_EUR.equals(columnName)) {
            result=new SignedAmount(configs.getTotalMTACptyEUR());
        }else if(LIVE_TRADES_FLAG.equals(columnName)) {
            result=configs.isContainsLiveTrades();
        }else if(BSNY_FLAG.equals(columnName)){
            result=configs.isContainsBSNY();
        }else if(CONTRACTNAMES.equals(columnName)){
            String contractIds="";
            for(CollateralConfig cc:configs.getCollateralConfigs()){
                if(!Util.isEmpty(contractIds)){
                    contractIds=contractIds.concat(";");
                }
                contractIds=contractIds.concat(cc.getName());
            }
            result=contractIds;
        }else{
            result=super.getColumnValue(row,columnName,errors);
        }

        return result;
    }
}
