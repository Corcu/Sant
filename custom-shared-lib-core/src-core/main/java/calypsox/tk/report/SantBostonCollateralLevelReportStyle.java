package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.collateral.dto.MarginCallPositionDTO;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.MarginCallPositionEntryReportStyle;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

public class SantBostonCollateralLevelReportStyle extends MarginCallPositionEntryReportStyle {


    private final String PROCCES_DATE = "ProccesDate";
    private final String DIRECTION = "Direction";
    private final String LE_SHORTNAME = "LE Short Name";
    private final String USD = "Value USD";



    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

        MarginCallPositionDTO dto = row.getProperty("Default");
        PricingEnv pricingEnv = getPricingEnv(row);
        
        Object ret = null;

        if(PROCCES_DATE.equalsIgnoreCase(columnName)){
        	ret = SantBostonUtil.getDate((JDatetime)row.getProperty("ValuationDatetime"));
        }else if(DIRECTION.equalsIgnoreCase(columnName)){

            Double value = dto.getValue();
            if(value<0.0){
            	ret = "P";
            }else{
            	ret = "R";
            }
        }else if(USD.equalsIgnoreCase(columnName)){
            Double valueUSD = SantBostonUtil.convertToUSD( dto.getCurrency(),dto.getValue(),(JDatetime)row.getProperty("ValuationDatetime"),pricingEnv);
            ret = valueUSD;

        }else if(LE_SHORTNAME.equalsIgnoreCase(columnName)){
            if(null!=row.getProperty("MarginCallConfig")){
            	ret = ((CollateralConfig) row.getProperty("MarginCallConfig")).getLegalEntity();
            }
            ret = "";
//            return dto.get
        }else{
        	ret = super.getColumnValue(row, columnName, errors);
        }
        
        if(ret instanceof Number){
        	Number retNum = (Number)ret;
        	ret = getFormattedAmount(retNum);
        }
        
        return ret;

    }



    private PricingEnv getPricingEnv(ReportRow row){
        String pricningEnvName = "OFFICIAL";
        ReportOutput out = row.getProperty("ReportOutput");
        if(out!=null && !Util.isEmpty(out.getPricingEnvName())){
            pricningEnvName = out.getPricingEnvName();
        }

        PricingEnv pricingEnv = null;
        try{
            pricingEnv = DSConnection.getDefault().getRemoteMarketData().getPricingEnv(pricningEnvName);

        }catch (CalypsoServiceException e){
            Log.error(this, "Cannot load PricingEnv: " + e);
        }

        return pricingEnv;
    }

    @Override
    public TreeList getTreeList() {
        TreeList treeList = super.getTreeList();
        treeList.add(PROCCES_DATE);
        treeList.add(DIRECTION);
        treeList.add(USD);
        treeList.add(LE_SHORTNAME);
        return treeList;
    }
    
	public String getFormattedAmount(final Number value) {
		try {
			final DecimalFormat myFormatter = new DecimalFormat("###0.00000");
			final DecimalFormatSymbols tmp = myFormatter.getDecimalFormatSymbols();
			tmp.setDecimalSeparator('.');
			myFormatter.setDecimalFormatSymbols(tmp);
			return myFormatter.format(value);
		} catch (Exception e) {
			if (value != null) {
				return value.toString();
			} else {
				return "0.00000";
			}
		}
	}
}
