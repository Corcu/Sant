package calypsox.tk.confirmation.builder.brs;

import calypsox.tk.confirmation.builder.CalConfirmationCommonVarsBuilder;
import calypsox.tk.confirmation.handler.CalypsoConfirmationMifidHandler;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

import java.text.SimpleDateFormat;
import java.util.Optional;

/**
 * @author aalonsop
 */
public class BRSConfirmationCommonVarsBuilder extends CalConfirmationCommonVarsBuilder {

    private static final String MX_EFFECTIVE_DATE_TRADE_KWD="Mx_EffectiveDate";
    private final CalypsoConfirmationMifidHandler mifidData;

    public BRSConfirmationCommonVarsBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
        mifidData=new CalypsoConfirmationMifidHandler(trade);
    }

    @Override
    public String buildRegValueDate() {
        String date= Optional.ofNullable(trade).map(trade -> trade.getKeywordValue(MX_EFFECTIVE_DATE_TRADE_KWD))
                .orElse("");
        String[] splitedDate=date.split("-");
        if(!Util.isEmpty(splitedDate)&& splitedDate.length==3) {
            JDate jdate = JDate.valueOf(Integer.parseInt(splitedDate[0]), Integer.parseInt(splitedDate[1]), Integer.parseInt(splitedDate[2]));
            date=jdate.toString();
        }

        return date;
    }

    @Override
    public String buildMifidTradingTime() {
        String DATE_FORMAT = "HH:mm:ss";
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        String UTC_STR = "UTC";
        return formatter.format(Optional.ofNullable(trade).map(Trade::getEnteredDate)
                .orElse(null)).concat(" "+ UTC_STR);

    }
    public String buildMifidCostsExpensesAmount() {
        return mifidData.getCurrentMifidCostsExpensesAmount();
    }

    public String buildMifidCostsExpensesCurr() {
        return mifidData.getCurrentMifidCostsExpensesCurr();
    }

    public String buildMifidCostsExpensesAmountModif(){
        return mifidData.getCurrentMifidCostsExpensesAmount();
    }

    public String buildMifidCostsExpensesCurrModif(){
        return mifidData.getCurrentMifidCostsExpensesCurr();
    }

    public String buildMifidTradingTimeModif(){
        return buildMifidTradingTime();
    }
}
