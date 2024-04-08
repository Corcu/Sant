package calypsox.tk.report;

import java.util.Vector;

import com.calypso.tk.report.MarginCallEntryReportTemplate;

public class SantBostonCounterpartyLevelReportTemplate extends MarginCallEntryReportTemplate {
	public final static String AGREEMENT_NPV = "AGREEMENT_NPV";//A2.6
	public final static String AGREEMENT_NPV_POS = "AGREEMENT_NPV_POS";//A2.7
	public final static String AGREEMENT_NPV_NEG = "AGREEMENT_NPV_NEG";//A2.8
	public final static String AGREEMENT_NOMINAL_POS = "AGREEMENT_NOMINAL_POS";//A2.9
	public final static String AGREEMENT_NOMINAL_NEG = "AGREEMENT_NOMINAL_NEG";//A2.10
	public final static String AGREEMENT_ALLOCATION_POS = "AGREEMENT_ALLOCATION_POS";//A2.13
	public final static String AGREEMENT_ALLOCATION_NEG = "AGREEMENT_ALLOCATION_NEG";//A2.14
	public final static String NET_MARGIN_PAY_DATE = "NET_MARGIN_PAY_DATE";//A2.15
	public final static String USD_NEXT_MARGIN_PAYMENT = "USD_NEXT_MARGIN_PAYMENT";//A2.16
    public final static String MASTER_AGREEMENT = "MASTER_AGREEMENT";//A2.16
	
    @Override
    public void setDefaults() {
        super.setDefaults();
        Vector<String> columns = new Vector<String>();
//    	columns.addElement(CALYPSO_CPTY_ID);
        columns.addElement(AGREEMENT_NPV);
        columns.addElement(AGREEMENT_NPV_POS);
        columns.addElement(AGREEMENT_NPV_NEG);
        columns.addElement(AGREEMENT_NOMINAL_POS);
        columns.addElement(AGREEMENT_NOMINAL_NEG);
        columns.addElement(AGREEMENT_ALLOCATION_POS);
        columns.addElement(AGREEMENT_ALLOCATION_NEG);
        columns.addElement(NET_MARGIN_PAY_DATE);
        columns.addElement(USD_NEXT_MARGIN_PAYMENT);
        columns.addElement(MASTER_AGREEMENT);
        setColumns("Columns", (String[])columns.toArray(new String[columns.size()]));
    }

}
