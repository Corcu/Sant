package calypsox.tk.report.extracontable;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.AccountingBook;
import com.calypso.tk.core.Book;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;

/**
 * @author aalonsop
 */
public abstract class MICExtracontableBuilder<T> {

    MICExtracontableBean messageBean;
    T sourceObject;


    public abstract MICExtracontableBean build();

    protected abstract String getMappedAccountingBook(T object);

    protected String getMappedAccountingBook(Book book){
        String accBookName= Optional.ofNullable(book).map(Book::getAccountingBook)
                .map(AccountingBook::getName).orElse("");
        return MICExtracontableBuilder.MICAccountingBookMapping.lookup(accBookName).mappingCode;
    }

    protected enum MICAccountingBookMapping {
        NEGOCIACION("0"),
        DISPONIBLE_PARA_LA_VENTA("1"),
        COSTE_AMORTIZADO("2"),
        INVERSION_CREDITICIA("2"),
        INVERSION_A_VENCIMIENTO("2"),
        OTROS_A_VALOR_RAZONABLE("8"),
        INVALID("");
        String mappingCode;

        MICAccountingBookMapping(String mappingCode) {
            this.mappingCode = mappingCode;
        }

        static MICAccountingBookMapping lookup(String accBookName) {
            MICAccountingBookMapping result;
            String mappedBookName=accBookName.toUpperCase();
            mappedBookName=mappedBookName.replace(" ","_");
            try {
                result = MICAccountingBookMapping.valueOf(mappedBookName);
            } catch (IllegalArgumentException e) {
                result= MICAccountingBookMapping.INVALID;
            }
            return result;
        }
    }

    protected String getLEAttr(int leId, String attributeName, String role){
        LegalEntityAttribute attribute=
                BOCache.getLegalEntityAttribute(DSConnection.getDefault(),0,leId,role,attributeName);
        return Optional.ofNullable(attribute).map(LegalEntityAttribute::getAttributeValue).orElse("");
    }
}
