package calypsox.tk.bo.obb;

import calypsox.util.OBBReportUtil;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOPosting;
import com.calypso.tk.refdata.Account;

import java.util.Optional;

/**
 * @author acd
 */
public class PerformanceSwapObbBean extends OBBGenericBean {

    public static final String TK_PARTENON_ID = "PartenonAccountingID";

    @Override
    public String loadEntity() {
        final String keywordValue = getTrade().getKeywordValue(TK_PARTENON_ID);
        return parsePartenonID(keywordValue,0,4);
    }

    @Override
    public String loadCenter() {
        final String keywordValue = getTrade().getKeywordValue(TK_PARTENON_ID);
        return parsePartenonID(keywordValue,4,8);
    }

    @Override
    public String loadProduct() {
        final String keywordValue = getTrade().getKeywordValue(TK_PARTENON_ID);
        return parsePartenonID(keywordValue,8,11);
    }

    @Override
    public String loadContract() {
        final String keywordValue = getTrade().getKeywordValue(TK_PARTENON_ID);
        return parsePartenonID(keywordValue,11,18);
    }

    @Override
    public String loadSubProduct() {
        final String keywordValue = getTrade().getKeywordValue(TK_PARTENON_ID);
        return parsePartenonID(keywordValue,18,21);
    }

    @Override
    public String loadCcyCounterValue()
    {
        if("EUR".equalsIgnoreCase(getBoPosting().getCurrency())){
            return getBoPosting().getCurrency();
        }else{
            final Optional<BOPosting> boPostingConvert = Optional.ofNullable(getBoPostingConvert());
            return boPostingConvert.map(BOPosting::getCurrency).orElse("");
        }
    }



    @Override
    public Double loadAmountCounterValue() {
        if("EUR".equalsIgnoreCase(getBoPosting().getCurrency())){
            return getBoPosting().getAmount();
        }else{
            final Optional<BOPosting> boPostingConvert = Optional.ofNullable(getBoPostingConvert());
            return boPostingConvert.map(BOPosting::getAmount).orElse(0D);
        }
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
        return OBBReportUtil.getNDir(getSubAccount());
    }

    @Override
    public String loadTopic() {
        String topic = "";
        String operatingPosition = getOperatingPosition();
        if(!Util.isEmpty(operatingPosition) && operatingPosition.substring(0,1).equalsIgnoreCase("W")){
            String sign = getSign();
            switch (sign){
                case "D" : return "LOSS";
                case "H" : return "PROFIT";
            }
        }else{
            topic = getTrade().getProductType();
        }

        return topic;
    }

    @Override
    public String loadOriginContract() {
        //TODO load calypso contract id
        
        return "";
    }

    @Override
    public String loadAccIdentifier() {
        if(this.getDoNotSetAgrego()){
            return "D";
        }else if(OBBReportUtil.isFirstWorkingDateOfMonth(getProcessDate())){
            return "A";
        }
        return "D";
    }

    private String parsePartenonID(String value, int begin, int end){
        return !Util.isEmpty(value) && value.length()>=end ? value.substring(begin,end) : "";
    }
}
