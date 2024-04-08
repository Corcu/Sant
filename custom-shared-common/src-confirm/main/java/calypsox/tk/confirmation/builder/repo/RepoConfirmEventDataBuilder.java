package calypsox.tk.confirmation.builder.repo;

import calypsox.tk.confirmation.builder.CalConfirmationEventDataBuilder;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;

import java.util.Optional;
import java.util.TimeZone;

/**
 * @author aalonsop
 */
public class RepoConfirmEventDataBuilder extends CalConfirmationEventDataBuilder {

    public RepoConfirmEventDataBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
    }


    public String buildInstrumentType() {
        return "Fixed Income Repo";
    }

    @Override
    public String buildMustBeSigned() {
        return String.valueOf(0);
    }

    @Override
    public String buildOperId() {
        return Optional.ofNullable(trade).map(Trade::getExternalReference).orElse("");
    }

    public String buildExternalId() {
        return  Optional.ofNullable(trade).map(t -> t.getKeywordValue("MurexTradeID")).orElse("");
    }

    public String getValidBODate(){
        return Optional.ofNullable(boMessage).map(BOMessage::getCreationDate).map(jDatetime -> jDatetime.getJDate(TimeZone.getDefault()))
                .map(JDate::toString)
                .orElse("");
    }
}
