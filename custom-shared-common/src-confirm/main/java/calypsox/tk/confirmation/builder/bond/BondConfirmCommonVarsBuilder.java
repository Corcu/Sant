package calypsox.tk.confirmation.builder.bond;

import calypsox.tk.confirmation.builder.CalConfirmationCommonVarsBuilder;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

import java.util.Optional;

public class BondConfirmCommonVarsBuilder extends CalConfirmationCommonVarsBuilder {


    public BondConfirmCommonVarsBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
    }

    @Override
    public String buildMifidTradingTime() {
        String mxEffTime = Optional.ofNullable(trade).map(s -> s.getKeywordValue("Mx EFx Time"))
                .orElse("");

        if (Util.isEmpty(mxEffTime)) mxEffTime = super.buildMifidTradingTime();

        return mxEffTime;
    }
}
