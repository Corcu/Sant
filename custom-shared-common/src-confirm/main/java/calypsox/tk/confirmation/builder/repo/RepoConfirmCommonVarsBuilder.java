package calypsox.tk.confirmation.builder.repo;

import calypsox.tk.confirmation.builder.CalConfirmationCommonVarsBuilder;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Repo;

import java.util.Optional;

public class RepoConfirmCommonVarsBuilder extends CalConfirmationCommonVarsBuilder{

    public RepoConfirmCommonVarsBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
            super(boMessage, boTransfer, trade);
        }
    @Override
    public String buildRegMaturity() {
            return Optional.ofNullable(product)
                    .filter(pr->pr instanceof Repo)
                    .map(rep-> new RepoReportUtil().getCallableOrProjectedDate((Repo)rep,JDate.getNow()))
                    .map(JDate::toString).orElse("");
        }

    @Override
    public String buildMifidTradingTime() {
        String mxEffTime = Optional.ofNullable(trade).map(s -> s.getKeywordValue("Mx EFx Time"))
                .orElse("");

        if (Util.isEmpty(mxEffTime)) mxEffTime = super.buildMifidTradingTime();

        return mxEffTime;
    }
}
