package calypsox.tk.report;

import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.ReportRow;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

public class AccountingNotificationReportStyle extends SantInterestNotificationReportStyle{

    public static final String PROCCES_DATE = "ProccesDate";
    public static final String ACCOUNT_ID = "Account Id";
    public static final String BOOK = "Book";
    public static final String MUREX_OPERATION = "Murex";
    public static final String DIRECTION = "Direction";
    public static final String ACCOUNTING_SECTOR = "Sector";
    public static final String GLCS_COUNTERPARTY = "GLCS CounterParty";
    public static final String COUNTERPARTY_NAME = "CounterParty Name";
    public static final String PRODUCT = "Product";
    public static final String CD_CURRENCY = "CD Currency";
    public static final String INDEX = "Index";
    public static final String CALC = "Calc";
    //New columns BAU
    public static final String COUPON_TYPE = "CouponType";
    public static final String GUARANTEE_TYPE = "GUARANTEE_TYPE";


    public static final String SALDO_VIVO_ACTUAL = "SALDO_VIVO_ACTUAL";
    public static final String PERIODICA_ACUMULADA_SIN_LIQUIDAR = "PERIODICA_ACUMULADA_SIN_LIQUIDAR";
    public static final String INTERES_ANUAL_ACUMULADO = "INTERES_ANUAL_ACUMULADO";
    public static final String INTERES_POS_ANUAL = "INTERES_POS_ANUAL";
    public static final String INTERES_NEG_ANUAL = "INTERES_NEG_ANUAL";
    public static final String ADJUSTMENT = "ADJUSTMENT";

    public static final String[] ADDITIONAL_COLUMNS = {PROCCES_DATE, ACCOUNT_ID, BOOK, MUREX_OPERATION, DIRECTION,
            ACCOUNTING_SECTOR, GLCS_COUNTERPARTY, COUNTERPARTY_NAME,PRODUCT, CD_CURRENCY, INDEX, CALC,SALDO_VIVO_ACTUAL, PERIODICA_ACUMULADA_SIN_LIQUIDAR, INTERES_ANUAL_ACUMULADO, INTERES_POS_ANUAL,
            INTERES_NEG_ANUAL,ADJUSTMENT, COUPON_TYPE, GUARANTEE_TYPE};

    @Override
    public String[] getDefaultColumns() {
        return ADDITIONAL_COLUMNS;
    }

    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

        DecimalFormat form = new DecimalFormat("0.00");
         /*String indexregex = "(?<=/).*?(?=/)";*/

        if ((row == null) || (row.getProperty("SantInterestNotificationEntry") == null)) {
            return null;
        }

        final AccountingNotificationEntry entry = row.getProperty(SantInterestNotificationReportTemplate.ROW_DATA);

        if(PROCCES_DATE.equalsIgnoreCase(columnName)){
            JDateFormat format = new JDateFormat("dd-MMM-yy");
            return format.format(entry.getProccesDate()).toUpperCase();
        }else if(ACCOUNT_ID.equalsIgnoreCase(columnName)){
            return entry.getCallAccountId();
        }else if(BOOK.equalsIgnoreCase(columnName)){
            return entry.getBook().getName();
        }else if(MUREX_OPERATION.equalsIgnoreCase(columnName)){
            return entry.getMurexID();
        }else if(DIRECTION.equalsIgnoreCase(columnName)){
            return entry.getInterstBearingDirection();
        }else if(ACCOUNTING_SECTOR.equalsIgnoreCase(columnName)){
            Collection legalEntityAttributes = entry.getCounterparty().getLegalEntityAttributes();
            if(!Util.isEmpty(legalEntityAttributes)){
                Iterator<LegalEntityAttribute> iterator = legalEntityAttributes.iterator();
                // while loop
                while (iterator.hasNext()) {
                    LegalEntityAttribute att = iterator.next();
                    if(null!=att && "SECTOR_CONTABLE".equalsIgnoreCase(att.getAttributeType())){
                        return att.getAttributeValue();
                    }
                }
            }
            return "";
        }else if(GLCS_COUNTERPARTY.equalsIgnoreCase(columnName)){
            return entry.getCounterparty().getCode();
        }else if(COUNTERPARTY_NAME.equalsIgnoreCase(columnName)){
            return entry.getCounterparty().getName();
        }else if(PRODUCT.equalsIgnoreCase(columnName)){
            return entry.getProduct();
        }else if(CD_CURRENCY.equalsIgnoreCase(columnName)){
            return entry.getCurrency();
        }else if(INDEX.equalsIgnoreCase(columnName)){
            return entry.getIndexName();
        }else if(CALC.equalsIgnoreCase(columnName)){
            return entry.getCalc();
        }else if(SALDO_VIVO_ACTUAL.equalsIgnoreCase(columnName)){
            return form.format(entry.getCurrentLiveBalance());
        }else if(PERIODICA_ACUMULADA_SIN_LIQUIDAR.equalsIgnoreCase(columnName)){
            return form.format(entry.getUnliquidatedAccumulatedPeriodic());
        }else if(INTERES_ANUAL_ACUMULADO.equalsIgnoreCase(columnName)){
            return form.format(entry.getInteretanual());
        }else if(INTERES_POS_ANUAL.equalsIgnoreCase(columnName)){
            return form.format(entry.getAnnualPositiveInterest());
        }else if(INTERES_NEG_ANUAL.equalsIgnoreCase(columnName)){
            return form.format(entry.getAnnualNegativeInterest());
        }else if(MOVEMENT.equals(columnName) && !(Boolean)row.getProperty(AccountingNotificationReport.MOVEMENTS_CALCULATED)) {
        	return "Not calculated please reload the report";
        }else if (ADJUSTMENT.equalsIgnoreCase(columnName)){
            return entry.getAdjustement();
        }else if (COUPON_TYPE.equalsIgnoreCase(columnName)) {
            return entry.getCouponType();
        }else if (GUARANTEE_TYPE.equalsIgnoreCase(columnName)) {
            return entry.getGuaranteeType();
        }
        else return super.getColumnValue(row, columnName, errors);
        
    }
}
