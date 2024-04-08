package calypsox.tk.report;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.collateral.MarginCallDetailEntry;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.PricerMeasure;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Vector;

public class SantBostonCounterpartyLevelReportStyle extends MarginCallEntryReportStyle{
	private static final long serialVersionUID = 1235L;
	private final String PROCCES_DATE = "ProccesDate";
	private final String AGREEMENT_NPV = "Agreement_NPV";//A2.6
	private final String AGREEMENT_NPV_POS = "AGREEMENT_NPV_POS";//A2.7
	private final String AGREEMENT_NPV_NEG = "AGREEMENT_NPV_NEG";//A2.8
	private final String AGREEMENT_NOMINAL_POS = "AGREEMENT_NOMINAL_POS";//A2.9
	private final String AGREEMENT_NOMINAL_NEG = "AGREEMENT_NOMINAL_NEG";//A2.10
	private final String AGREEMENT_ALLOCATION_POS = "AGREEMENT_ALLOCATION_POS";//A2.13
	private final String AGREEMENT_ALLOCATION_NEG = "AGREEMENT_ALLOCATION_NEG";//A2.14
	private final String NET_MARGIN_PAY_DATE = "NET_MARGIN_PAY_DATE";//A2.15
	private final String USD_NEXT_MARGIN_PAYMENT = "USD_NEXT_MARGIN_PAYMENT";//A2.16
	private final String MASTER_AGREEMENT = "ADDITIONAL_FIELD.MASTER_AGREEMENT";//A2.16
	
    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {
    	
    	Object ret = null;
    	
    	if(PROCCES_DATE.equalsIgnoreCase(columnName)) {
    		JDatetime jTime = ReportRow.getValuationDateTime(row);
    		String date = SantBostonUtil.getDate(jTime);
    		ret =  date;
    	}else if(MASTER_AGREEMENT.equalsIgnoreCase(columnName)){
    		ret =  row.getProperty(SantBostonCounterpartyLevelReportTemplate.MASTER_AGREEMENT);
		}
    	else if(AGREEMENT_NPV.equalsIgnoreCase(columnName)) {
    		ret =  checkNan((Double)row.getProperty(SantBostonCounterpartyLevelReportTemplate.AGREEMENT_NPV));
    	}
    	else if(AGREEMENT_NPV_POS.equalsIgnoreCase(columnName)) {
    		ret =   checkNan((Double)row.getProperty(SantBostonCounterpartyLevelReportTemplate.AGREEMENT_NPV_POS));
    	}
    	else if(AGREEMENT_NPV_NEG.equalsIgnoreCase(columnName)) {
    		ret =   checkNan((Double)row.getProperty(SantBostonCounterpartyLevelReportTemplate.AGREEMENT_NPV_NEG));
    	}
    	else if(AGREEMENT_NOMINAL_POS.equalsIgnoreCase(columnName)) {
    		ret =  checkNan((Double)row.getProperty(SantBostonCounterpartyLevelReportTemplate.AGREEMENT_NOMINAL_POS));
    	}
    	else if(AGREEMENT_NOMINAL_NEG.equalsIgnoreCase(columnName)) {
    		ret =  checkNan((Double)row.getProperty(SantBostonCounterpartyLevelReportTemplate.AGREEMENT_NOMINAL_NEG));
    	}
    	else if(AGREEMENT_ALLOCATION_POS.equalsIgnoreCase(columnName)) {
    		ret =  checkNan((Double)row.getProperty(SantBostonCounterpartyLevelReportTemplate.AGREEMENT_ALLOCATION_POS));
    	}
    	else if(AGREEMENT_ALLOCATION_NEG.equalsIgnoreCase(columnName)) {
    		ret =  checkNan((Double)row.getProperty(SantBostonCounterpartyLevelReportTemplate.AGREEMENT_ALLOCATION_NEG));
    	}
    	else if(NET_MARGIN_PAY_DATE.equalsIgnoreCase(columnName)) {//Always an empty field
    		String payDate = "";
    		ret =  payDate;
    	}

    	else if(USD_NEXT_MARGIN_PAYMENT.equalsIgnoreCase(columnName)) {//Always an  empty field
    		String paymentNext = "";
    		ret =  paymentNext;
    	}


    	else{

    		ret =  super.getColumnValue(row, columnName, errors);
		}
    	
    	if(ret instanceof Number){
        	Number retNum = (Number)ret;
        	ret = getFormattedAmount(retNum);
        }
        
        return ret;
    	
    }


    private Double checkNan(Double value){
        if(value==null || Double.isNaN(value)){
            return 0.0;
        }else{
            return value;
        }
    }

    @Override
    public TreeList getTreeList() {
        TreeList treeList = super.getTreeList();
        treeList.add(PROCCES_DATE);
        //...and the columns:
        treeList.add(AGREEMENT_NPV);
        treeList.add(AGREEMENT_NPV_POS);
        treeList.add(AGREEMENT_NPV_NEG);
        treeList.add(AGREEMENT_NOMINAL_POS);
        treeList.add(AGREEMENT_NOMINAL_NEG);
        treeList.add(AGREEMENT_ALLOCATION_POS);
        treeList.add(AGREEMENT_ALLOCATION_NEG);
        treeList.add(NET_MARGIN_PAY_DATE);
        treeList.add(USD_NEXT_MARGIN_PAYMENT);
		treeList.add(MASTER_AGREEMENT);
        
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