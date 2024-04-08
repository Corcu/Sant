package calypsox.tk.confirmation.builder;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class CalConfirmationProcessingOrgBuilder extends CalypsoConfirmationConcreteBuilder {


    LegalEntity cpty;
    protected LEContact cptyContact;

    public CalConfirmationProcessingOrgBuilder(BOMessage boMessage, BOTransfer boTransfer, Trade trade) {
        super(boMessage, boTransfer, trade);
        this.cpty = Optional.ofNullable(trade).map(Trade::getBook).map(Book::getLegalEntity).orElse(null);
        int receiverContactId = Optional.ofNullable(boMessage).map(BOMessage::getSenderContactId).orElse(0);
        this.cptyContact = BOCache.getLegalEntityContact(DSConnection.getDefault(), receiverContactId);
    }

    public String buildBrName() {
        return Optional.ofNullable(cptyContact).map(LEContact::getContactName).orElse("");
    }

    public String buildBrEntName() {
        return Optional.ofNullable(cpty).map(LegalEntity::getName).orElse("");
    }

    public String buildBrEntCode() {
        return Optional.ofNullable(cpty).map(LegalEntity::getCode).orElse("");
    }

    public String buildBrAddress() {
        return Optional.ofNullable(cptyContact).map(LEContact::getMailingAddress).orElse("");
    }

    public String buildBrCity() {
        return Optional.ofNullable(cptyContact).map(LEContact::getCityName).orElse("");
    }

    public String buildBrPC() {
        return Optional.ofNullable(cptyContact).map(LEContact::getZipCode).orElse("");
    }

    public String buildBrCountry() {
        return Optional.ofNullable(cptyContact).map(LEContact::getCountry).orElse("");
    }

    public String buildBrFax() {
        return Optional.ofNullable(cptyContact).map(LEContact::getFax).orElse("");
    }

    public String buildBrEmail() {
        return Optional.ofNullable(cptyContact).map(LEContact::getEmailAddress).orElse("");
    }
}
