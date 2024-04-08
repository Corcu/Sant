package calypsox.tk.confirmation.builder;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class CalConfirmationCounterpartyBuilder extends CalypsoConfirmationConcreteBuilder {

    protected final String engStr="ENG";

    protected LegalEntity cpty;
    protected LEContact cptyContact;
    CollateralConfig collateralConfig;

    public CalConfirmationCounterpartyBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
        this.cpty = Optional.ofNullable(trade).map(Trade::getCounterParty).orElse(null);
        int receiverContactId = Optional.ofNullable(boMessage).map(BOMessage::getReceiverContactId).orElse(0);
        this.cptyContact = BOCache.getLegalEntityContact(DSConnection.getDefault(), receiverContactId);
        this.collateralConfig=findMarginCallConfig(trade);
    }

    private CollateralConfig findMarginCallConfig(Trade trade){
        CollateralConfig cc=null;
       int contractId=Optional.ofNullable(trade).map(Trade::getInternalReference).filter(internal-> !Util.isEmpty(internal))
               .map(Integer::parseInt).orElse(0);
        if(contractId>0){
            cc=CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),contractId);
        }
        return cc;
    }

    public String buildCptyName() {
        return Optional.ofNullable(cpty).map(LegalEntity::getName).orElse("");
    }

    public String buildCptyCode() {
        return Optional.ofNullable(cpty).map(LegalEntity::getCode).orElse("");
    }

    public String buildCptyAddress() {
        return Optional.ofNullable(cptyContact).map(LEContact::getMailingAddress).orElse("");
    }

    public String buildCptyCity() {
        return Optional.ofNullable(cptyContact).map(LEContact::getCityName).orElse("");
    }

    public String buildCptyPostalCode() {
        return Optional.ofNullable(cptyContact).map(LEContact::getZipCode).orElse("");
    }

    public String buildCptyCountry() {
        return Optional.ofNullable(cptyContact).map(LEContact::getCountry).orElse("");
    }

    public String buildCptyFax() {
        return Optional.ofNullable(cptyContact).map(LEContact::getFax).orElse("");
    }

    public String buildCptyEmail() {
        return Optional.ofNullable(cptyContact).map(LEContact::getEmailAddress).orElse("");
    }

    public String buildCptyEmailChase() {
        return buildCptyEmail();
    }

    public String buildCptyContact() {
        return Optional.ofNullable(cptyContact).map(LEContact::getContactName).orElse("");
    }

    public String buildLanguage() {
        String addressCode="CONF_LANGUAGE";
        return Optional.ofNullable(cptyContact).map(cnt->cnt.getAddressCode(addressCode)).orElse(engStr);
    }

    /**
     * @return Always 0
     */
    public String buildCorporateIndicator() {
        return String.valueOf(0);
        //return Optional.ofNullable(cpty).map(LegalEntity::getExternalRef).orElse("");
    }

    public String buildCptyContractDate() {
        String contractDate;
        if(collateralConfig!=null){
             contractDate=collateralConfig.getAdditionalField("MA_EFFECTIVE_DATE");
            if(Util.isEmpty(contractDate)){
                contractDate=Optional.ofNullable(collateralConfig.getStartingDate()).map(JDate::valueOf)
                        .map(JDate::toString).orElse("");
            }else{
                contractDate=contractDate.split("\\s+")[0];
            }
        }else{
            contractDate=this.buildRegTradeDate();
        }
        return contractDate;
    }

    public String buildRegTradeDate() {
        return Optional.ofNullable(trade).map(Trade::getTradeDate).map(JDate::valueOf)
                .map(JDate::toString).orElse("");
    }
    public String buildCptyContractType() {
        return "ISDA";
    }

    public String buildCustomer() {
        return "SGM";
    }
}
