package calypsox.tk.bo.obb;

import calypsox.util.OBBReportUtil;
import com.calypso.infra.util.Util;
import com.calypso.tk.refdata.Account;

import java.util.Optional;

public class BalanceObbBean extends OBBGenericBean{

    @Override
    public String loadEntity() {
        return "";
    }

    @Override
    public String loadCenter() {
        return "";
    }

    @Override
    public String loadProduct() {
        return "";
    }

    @Override
    public String loadContract() {
        return "";
    }

    @Override
    public String loadSubProduct() {
        return "";
    }

    @Override
    public String loadCcyCounterValue() {
        if("EUR".equalsIgnoreCase(getBoPosting().getCurrency())){
            return getBoPosting().getCurrency();
        }
        return "";
    }

    @Override
    public Double loadAmountCounterValue() {
        if("EUR".equalsIgnoreCase(getBoPosting().getCurrency())){
            return getBoPosting().getAmount();
        }
        return 0.0D;
    }

    @Override
    public String loadOperatingPosition() {
        final Optional<Account> account = Optional.ofNullable(getAccount());
        if(account.isPresent()){
            if("PosicionOperativa".equalsIgnoreCase(OBBReportUtil.getAccountTypeValue(account.get()))
                    || Util.isEmpty(OBBReportUtil.getAccountTypeValue(account.get()))){
                return OBBReportUtil.getAccountPosicionValue(account.get());
            }
        }
        return "";
    }

    @Override
    public String loadSign() {
        return OBBReportUtil.getSing(getCreditDebit());
    }

    @Override
    public String loadType() {
        return OBBReportUtil.getNDir(getCreditDebit());
    }

    @Override
    public String loadTopic() {
        return "PerformanceSwap";
    }

    @Override
    public String loadOriginContract() {
        return "";
    }

    @Override
    public String loadAccIdentifier() {
        if(OBBReportUtil.isFirstWorkingDateOfMonth(getProcessDate())){
            return "A";
        }
        return "D";
    }


}
