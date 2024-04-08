package calypsox.tk.report;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.ReportRow;

import java.security.InvalidParameterException;
import java.util.Optional;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

public class BODisponibleBlockingPositionReportStyle extends BODisponibleSecurityPositionReportStyle {

    public static final String NIF_GARANTIA = "NIF GARANTIA";
    public static final String ACTIVO = "ACTIVO";

    public static final String NOMINAL = "VALOR NOMINAL";
    public static final String ESTRATEGIA_CONTABLE = "ESTRATEGIA CONTABLE";
    public static final String CENTRO_CONTABLE = "CENTRO CONTABLE";

    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {

        if(NIF_GARANTIA.equalsIgnoreCase(columnId)){
            return getTaxID(row);
        }else if(ACTIVO.equalsIgnoreCase(columnId)){
            return row.getProperty("BlockingType");
        }else if(ESTRATEGIA_CONTABLE.equalsIgnoreCase(columnId)){
            return Optional.ofNullable(getBook(row)).map(Book::getAccountingBook).map(AccountingBook::getName).orElse("");
        }else if(CENTRO_CONTABLE.equalsIgnoreCase(columnId)){
            Inventory inventory = row.getProperty(ReportRow.INVENTORY);
            Product product = null!=inventory ? inventory.getProduct() : null;
            String entity = Optional.ofNullable(getBook(row)).map(Book::getLegalEntity).map(LegalEntity::getCode).orElse("");
            return BOCreUtils.getInstance().getCentroContable(product, entity, false);
        }
        return super.getColumnValue(row, columnId, errors);
    }

    private String getTaxID(ReportRow row){
        Vector<LegalEntityAttribute> attrs = (Vector<LegalEntityAttribute>)Optional.ofNullable(getBook(row))
                .map(Book::getLegalEntity).map(LegalEntity::getLegalEntityAttributes).orElse(new Vector<LegalEntityAttribute>());

       return getTaxIdLEAttribute(attrs);
    }

    protected String getTaxIdLEAttribute(Vector<LegalEntityAttribute> attrs){
        AtomicReference<String> value = new AtomicReference<>("");
        if(!Util.isEmpty(attrs)){
            attrs.forEach(a -> {
                if(a.getAttributeType().equalsIgnoreCase("TAXID")){
                    value.set(a.getAttributeValue());
                }
            });
        }

        return value.get();
    }
    protected Book getBook(ReportRow row){
        return Optional.ofNullable(row).map(r -> r.getProperty("Inventory")).map(InventorySecurityPosition.class::cast).map(InventorySecurityPosition::getBook).orElse(null);
    }
}
