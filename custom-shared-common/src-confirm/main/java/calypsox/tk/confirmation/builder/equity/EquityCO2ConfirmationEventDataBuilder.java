package calypsox.tk.confirmation.builder.equity;


import calypsox.tk.confirmation.builder.CalConfirmationEventDataBuilder;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

import java.util.Optional;
import java.util.TimeZone;


public class EquityCO2ConfirmationEventDataBuilder extends CalConfirmationEventDataBuilder {


    public EquityCO2ConfirmationEventDataBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
    }


    public String buildInstrumentType() {
        return "Equity";
    }


    @Override
    public String buildMustBeSigned() {
        return String.valueOf(1);
    }


    public String buildSendToCompleted() {
        return String.valueOf(0);
    }


    public String getValidBODate(){
        return Optional.ofNullable(boMessage).map(BOMessage::getCreationDate).map(jDatetime -> jDatetime.getJDate(TimeZone.getDefault()))
                .map(JDate::toString)
                .orElse("");
    }


    public String get(){
        return Optional.ofNullable(boMessage).map(BOMessage::getCreationDate).map(jDatetime -> jDatetime.getJDate(TimeZone.getDefault()))
                .map(JDate::toString)
                .orElse("");
    }


    public String buildInstrumentSubType() {
        return trade.getKeywordValue("Mx_Product_SubType");
    }


}
