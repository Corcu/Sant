package calypsox.tk.report;

import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.apps.appkit.presentation.format.JDatetimeFormat;
import com.calypso.tk.core.*;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Optional;
import java.util.Vector;

public class RepoTripartyPledgeReportStyle extends TradeReportStyle {

    public static final String ROW_NUM_ID = "Row Num id";
    public static final String REPO_TRADE_ID = "Repo Trade id";
    public static final String REPO_TRADE_DATE = "Repo Trade Date";
    public static final String REPO_TRADE_BOOK = "Repo Book";
    public static final String REPO_TRADE_PARTENON = "Repo Partenon";
    public static final String REPO_TRADE_NOMINAL = "Repo Nominal";
    public static final String REPO_TRADE_DIRECTION = "Repo Direction";
    public static final String REPO_TRADE_CURRENCY = "Repo Currency";


    public static final String PLEDGE_DIRECTION = "Trade Direction";
    public static final String PLEDGE_NOMINAL = "Trade Nominal";
    public static final String PLEDGE_QUANTITY = "Trade Quantity";



    public static final String MTM = "MTM";
    public static final String PRINCIPAL = "Principal";
    public static final String ACCRUAL = "Accrual";
    public static final String REPO_PRORATA = "Trade Repo Prorata";

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    public static final String PRODUCT_TYPE = "ProductType";
    public static final String UNDERLYING_PRODUCT_TYPE = "Underlying.Product Type";

    private static final String BOND = "Bond";
    private static final String EQUITY = "Equity";




    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {
        Trade fatherRepoTrade = null!=row ? (Trade)row.getProperty("FatherRepoTrade") : null;
        if(Optional.ofNullable(row).isPresent()){
            if(ROW_NUM_ID.equalsIgnoreCase(columnId)){
                return row.getProperty(RepoTripartyPledgeReportTemplate.ROW_ID);
            }else if(REPO_TRADE_ID.equalsIgnoreCase(columnId)){
                return Optional.ofNullable(fatherRepoTrade).map(Trade::getLongId).orElse(-1L);
            }else if(REPO_TRADE_DATE.equalsIgnoreCase(columnId)){
                return formatJDTime(Optional.ofNullable(fatherRepoTrade).map(Trade::getTradeDate).orElse(null));
            }else if(REPO_TRADE_BOOK.equalsIgnoreCase(columnId)){
                return Optional.ofNullable(fatherRepoTrade).map(Trade::getBook).map(Book::getName).orElse("");
            }else if(REPO_TRADE_PARTENON.equalsIgnoreCase(columnId)){
                return Optional.ofNullable(fatherRepoTrade).map(t->t.getKeywordValue("PartenonAccountingID")).orElse("");
            }else if(REPO_TRADE_DIRECTION.equalsIgnoreCase(columnId)){
                return row.getProperty(RepoTripartyPledgeReportTemplate.REPO_TRADE_DIRECTION);
            }else if(REPO_TRADE_NOMINAL.equalsIgnoreCase(columnId)){
                return formatAmount(Optional.ofNullable(fatherRepoTrade).map(Trade::getProduct).map(p -> p.computeNominal(fatherRepoTrade)).orElse(0.0D));
            }else if(PLEDGE_DIRECTION.equalsIgnoreCase(columnId)){
                return row.getProperty(RepoTripartyPledgeReportTemplate.PLEDGE_DIRECTION);
            }else if(MTM.equalsIgnoreCase(columnId)){
                return formatAmount(getAmount(row.getProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_MTM)));
            }else if(PRINCIPAL.equalsIgnoreCase(columnId)){
                return formatAmount(getAmount(row.getProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_PRINCIPAL)));
            }else if(ACCRUAL.equalsIgnoreCase(columnId)){
                return formatAmount(getAmount(row.getProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_ACCRUAL)));
            }else if(REPO_TRADE_CURRENCY.equalsIgnoreCase(columnId)){
                return Optional.ofNullable(fatherRepoTrade).map(Trade::getTradeCurrency).orElse("");
            }else if(UNDERLYING_PRODUCT_TYPE.equalsIgnoreCase(columnId)){
                return mapProductType((String) super.getColumnValue(row, columnId, errors));
            }else if(PRODUCT_TYPE.equalsIgnoreCase(columnId)){
                return mapProductType((String) super.getColumnValue(row, "Underlying.Product Type", errors));
            }else if(REPO_PRORATA.equalsIgnoreCase(columnId)){
                return formatAmount(getAmount(row.getProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE)));
            }else {
                final Object columnValue = super.getColumnValue(row, columnId, errors);
                if(columnValue instanceof SignedAmount){
                    return formatAmount(((SignedAmount)columnValue).get());
                }else if(columnValue instanceof Amount){
                    return formatAmount(((Amount)columnValue).get());
                }else if(columnValue instanceof JDate){
                    JDateFormat jDateFormat = new JDateFormat(DATE_FORMAT);
                    return jDateFormat.format(columnValue);
                }else if(columnValue instanceof JDatetime){
                    return formatJDTime(columnValue);
                }
                return columnValue;
            }
        }
        return null;
    }


    private String mapProductType(String type){
        if(BOND.equalsIgnoreCase(type) || EQUITY.equalsIgnoreCase(type)){
            return type;
        } else if(!Util.isEmpty(type) && type.contains(BOND)){
            return BOND;
        }else if(!Util.isEmpty(type) && type.contains(EQUITY)){
            return EQUITY;
        }
        return "";
    }

    /**
     * @param value
     * @return
     */
    private String formatAmount(Double value){
        final DecimalFormatSymbols decimalSymbol = new DecimalFormatSymbols();
        decimalSymbol.setDecimalSeparator('.');
        final DecimalFormat df = new DecimalFormat("#0.00", decimalSymbol);
        return df.format(value);
    }

    private String formatJDTime(Object jDatetime){
        if(Optional.ofNullable(jDatetime).isPresent()){
            JDatetimeFormat jDateFormat = new JDatetimeFormat(DATE_FORMAT);
            return jDateFormat.format(jDatetime);
        }
        return null;
    }

    /**
     * @param t
     * @return
     */
    private Double getAmount(Object t){
        return Optional.ofNullable(t).filter(Amount.class::isInstance).map(Amount.class::cast).map(Amount::get).orElse(0.0);
    }
}


