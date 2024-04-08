package calypsox.tk.util;


import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ScheduledTaskSantEnrichCusipCode extends ScheduledTask {


    @Override
    public String getTaskInformation() {
        return "This scheduledTask enrich cusip code to products";
    }

    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        attributeList.addAll(super.buildAttributeDefinition());
        attributeList.add(attribute("ISIN PREFIX"));

        return attributeList;
    }

    @Override
    protected boolean process(DSConnection dsConn, PSConnection psConn) {
        Vector<Product> sec;
        String prefix = getAttribute("ISIN PREFIX");
        if (Util.isEmpty(prefix)) {
            sec = getAllProducts();
        } else {
            sec = getProductsByPrefix(prefix, true);
        }
         ConcurrentLinkedQueue<Product> syncList = new ConcurrentLinkedQueue<>(sec);
        syncList.parallelStream().forEach(this::fillCusipCode);

        return true;
    }

    public void fillCusipCode(Product product) {
        String isin = product.getSecCode("ISIN");

        if (isProductActive(product) && Util.isEmpty(product.getSecCode("CUSIP")) && checkIsinCode(isin)) {
            try {
                product.setSecCode("CUSIP", getCusipFromIsin(isin));
                DSConnection.getDefault().getRemoteProduct().saveProduct(product);
            } catch (CalypsoServiceException e) {
                Log.error(this.getClass().getSimpleName(), " Cant save the product: " + product.getId() + " with ISIN: " + isin);
            }
        }
    }

    private String getCusipFromIsin(String isin) {
        return isin.substring(2, 11);
    }

    public Vector<Product> getProductsByPrefix(String prefix, Boolean ret) {
        Vector<Product> sec = null;
        try {
            sec = DSConnection.getDefault().getRemoteProduct()
                    .getProductsByCode("ISIN", prefix.concat("%"));
        } catch (CalypsoServiceException e) {
            Log.error(this.getClass().getSimpleName(), "Cant retrieve any product with prefix " + prefix.contains("%"), e);
        }
        return sec;
    }

    public Vector<Product> getAllProducts() {
        Vector<Product> sec = null;
        try {
            sec = DSConnection.getDefault().getRemoteProduct()
                    .getAllProducts("", buildWhere(), null);
        } catch (CalypsoServiceException e) {
            Log.error(this.getClass().getSimpleName(), "Cant retrieve any product ", e);
        }
        return sec;
    }

    private boolean isProductActive(Product product) {
        JDate valdate = this.getValuationDatetime().getJDate(TimeZone.getDefault());
        if (product instanceof Bond){
            return product.getMaturityDate().gte(valdate);
        } else {
           return ((Equity) product).isActive(valdate);
        }
    }

    private JDate getValdate() {
        return this.getValuationDatetime().getJDate(TimeZone.getDefault());
    }

    private String buildWhere() {
        return "trunc (MATURITY_DATE) >= " + Util.date2SQLString(getValdate());
    }

    private boolean checkIsinCode(String isin) {
        return Optional.ofNullable(isin).filter(s -> s.length() == 12).isPresent();
    }
}
