package calypsox.tk.report;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.tk.bo.util.CAUtil;
import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.SignedAmount;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Cash;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.product.SecFinance;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;

public class TradeReportStyle extends com.calypso.tk.report.TradeReportStyle {

    private static final String MC_DESCRIPTION = "Contract Description";
    public static final String TRADE_ID_MIC_FORMAT="Trade Id for MIC";
    public static final String VAL_DATE="VD";
    public static final String EMPTY_COLUMN="Empty";
	private static final Set<String> lstCSDRFields = new HashSet<String>(
			Arrays
			.asList(new String[] { "TRADE_KEYWORD.AggregatedGlobalAmount",
			"TRADE_KEYWORD.MonthlyConfirmedAmount", "TRADE_KEYWORD.Penalty_Amount",
					"TRADE_KEYWORD.CSDRPotencialPenaltyDaily"}));
    public static final String CASH_PAYMENT_HOLIDAYS = "Cash. Payment Holidays";
    public static final String CASH_HOLIDAYS = "Cash. Holidays";
    public static final String CASH_RESET_HOLIDAYS = "Cash. Reset Holidays";


    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {
        Trade trade = (Trade) Optional.ofNullable(row).map(r->r.getProperty("Trade")).orElse(null);
        if(MC_DESCRIPTION.equalsIgnoreCase(columnId)){
            if(trade!=null && trade.getProduct()!= null && trade.getProduct() instanceof MarginCall){
                return ((MarginCall)trade.getProduct()).getMarginCallConfig().getDescription();
            }
        }
        if(TRADE_ID_MIC_FORMAT.equalsIgnoreCase(columnId)){
            BOCreUtils creUtils=BOCreUtils.getInstance();
            long tradeId=Optional.ofNullable(trade).map(Trade::getLongId).orElse(0L);
            return creUtils.formatStringWithZeroOnLeft(tradeId,16);
        }
        if(VAL_DATE.equalsIgnoreCase(columnId)){
            if(trade!=null && trade.getProduct()!= null){

                JDate settleDate = trade.getSettleDate();
                JDateFormat format = new JDateFormat("ddMMyyyy");

                return format.format(settleDate);
            }
        }
        if(CASH_PAYMENT_HOLIDAYS.equalsIgnoreCase(columnId)){
            return Util.arrayToString(Optional.ofNullable(getSecFinanceCash(trade)).map(Cash::getPaymentHolidays).orElse(null));
        }
        if(CASH_HOLIDAYS.equalsIgnoreCase(columnId)){
            return Util.arrayToString(Optional.ofNullable(getSecFinanceCash(trade)).map(Cash::getHolidays).orElse(null));
        }
        if(CASH_RESET_HOLIDAYS.equalsIgnoreCase(columnId)){
            return Util.arrayToString(Optional.ofNullable(getSecFinanceCash(trade)).map(Cash::getResetHolidays).orElse(null));
        }
        if(EMPTY_COLUMN.equalsIgnoreCase(columnId)){
            return "";
		} else if ("Swift Event Code".equalsIgnoreCase(columnId)) {
			Product p = trade.getProduct();
			if ("CA".equals(p.getType())) {
				return CAUtil.getCASwiftEventCode(DSConnection.getDefault(), p);
			}
		}
		if (lstCSDRFields.contains(columnId)) {
			String value = (String) super.getColumnValue(row, columnId, errors);
			if (Util.isEmpty(value)) {
				return "";
			}
			double val = Util.istringToNumber(value);
			return new SignedAmount(val);
		}
        return super.getColumnValue(row, columnId, errors);

    }

    private Cash getSecFinanceCash(Trade trade){
        return Optional.ofNullable(trade)
                .map(Trade::getProduct)
                .filter(SecFinance.class::isInstance)
                .map(SecFinance.class::cast)
                .map(SecFinance::getCash)
                .orElse(null);
    }

    @Override
    public TreeList getTreeList() {
        TreeList treeList = super.getTreeList();
        String tradeCustomTree="Trade Custom";
        treeList.add(tradeCustomTree,MC_DESCRIPTION);
        treeList.add(tradeCustomTree,TRADE_ID_MIC_FORMAT);
        treeList.add(tradeCustomTree,EMPTY_COLUMN);
        treeList.add(tradeCustomTree,VAL_DATE);
        return treeList;
    }
}
