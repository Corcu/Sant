package calypsox.tk.confirmation.builder;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

/**
 * @author aalonsop
 */
public class CalConfirmationCommonVarsBuilder extends CalypsoConfirmationConcreteBuilder {

    private static final String EMAIL_STR="EMAIL";
    private static final String SOURCE_SYSTEM = "CALYPSOSTC";

    protected Product product;


    public CalConfirmationCommonVarsBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
        this.product = Optional.ofNullable(trade).map(Trade::getProduct).orElse(null);
    }

    public String buildTradeId(){
        return Optional.ofNullable(trade).map(Trade::getLongId).map(String::valueOf)
              .orElse("O");
    }

    public String buildRegTradeDate() {
        return Optional.ofNullable(trade).map(Trade::getTradeDate).map(JDate::valueOf)
                .map(JDate::toString).orElse("");
    }

    public String buildRegTradeTime() {
        JDatetime dt = trade.getTradeDate();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        return simpleDateFormat.format(dt) + " UTC";
    }

    public String buildRegValueDate() {
        return Optional.ofNullable(trade).map(Trade::getSettleDate).map(JDate::valueOf)
                .map(JDate::toString).orElse("");
    }

    public String buildRegConfirmation() {
        return EMAIL_STR;
    }

    public String buildSystemSource() {
        return SOURCE_SYSTEM;
    }

    public String buildMifidTradingTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ", Locale.forLanguageTag("ES"));
        sdf.setTimeZone(TimeZone.getTimeZone("Europe/Madrid"));
        return Optional.ofNullable(trade).map(Trade::getEnteredDate)
                .map(sdf::format)
                .orElse("");
    }


    public String buildRegMaturity() {
        return Optional.ofNullable(product).map(Product::getMaturityDate).map(JDate::toString).orElse("");
    }

    public String buildBusinessDays() {
        return Optional.ofNullable(product).map(Product::getHolidays)
                .filter(holidays -> !Util.isEmpty(holidays)).map(holidays -> holidays.get(0))
                .map(Object::toString).orElse("");
    }


    public String buildMifidCostsExpensesAmount(){
        return "";
    }


    public String buildMifidCostsExpensesCurr(){
        return "";
    }


}
