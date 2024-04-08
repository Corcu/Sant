package calypsox.tk.report.extracontable;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.LegalEntityAttribute;

import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

/**
 * @author dmenendd
 */
public class MICExtracontableEquityBuilder extends MICExtracontableTradeBuilder {

    Equity equity;

    public MICExtracontableEquityBuilder(Trade trade) {
        super(trade);
        Product tradeProduct = Optional.ofNullable(trade).map(Trade::getProduct).orElse(null);
        if (tradeProduct instanceof Equity) {
            equity = (Equity) tradeProduct;
        }

    }

    @Override
    public  MICExtracontableBean build() {
        messageBean=super.build();
        messageBean.setCodIsin(loadIsin(getEquityReference()));
        messageBean.setCodRefInGr(sourceObject.getKeywordValue("RIG_CODE"));

        messageBean.setCodGLSContrapar(sourceObject.getCounterParty().getCode());
        messageBean.setCodGLSEmisor(getIssue(getEquityReference()));
        messageBean.setCodGLSEntidad(sourceObject.getBook().getLegalEntity().getCode());//el book

        messageBean.setCodContrapar(sourceObject.getCounterParty().getExternalRef());
        messageBean.setDescCodContrapar(sourceObject.getCounterParty().getName());

        messageBean.setCodNumOpeFront(sourceObject.getExternalReference());
        messageBean.setCodNumOpeBack(String.valueOf(sourceObject.getLongId()));

        messageBean.setTcRefInt(loadIsin(getEquityReference()));

        messageBean.setCodCifEmi(getCodCifEmisor(getEquityReference()));
        
        messageBean.setFVenci(loadFechaVenci());
        
        return messageBean;
    }

    private Product getEquityReference() {
        Product product = null;
        if (Optional.ofNullable(equity).map(Equity::getProduct)
                .orElse(null) instanceof Equity) {
            product = equity.getProduct();
        }
        return product;
    }

    private String buildInternalReference(Product product) {
        return Optional.ofNullable(product)
                .map(product1 -> product1.getSecCode(BOND_SEC_CODE_REF_INTERNA))
                .orElse("");
    }

    private String loadIsin(Product security) {
        return null != security ? security.getSecCode("ISIN") : "";
    }

    private String getIssue(Product product) {
        String out = "";
        Equity equity = (Equity) product;

        if (null != equity && null !=  equity.getIssuer()){
            out = equity.getIssuer().getCode();
        }
        return out;
    }

    private String getCodCifEmisor(Product product){
        String cif =  "";
        if (product instanceof Equity) {
            Equity equity = (Equity) product;
            LegalEntity le = equity.getIssuer();
            if (le != null){
                cif = getLegalEntityAttribute(le, "TAXID");

            }
        }
        return !Util.isEmpty(cif) ? cif : "";
    }

    public String getLegalEntityAttribute(final LegalEntity le, final String att) {
        String rst = "";
        if (le != null) {
            final Collection<?> atts = le.getLegalEntityAttributes();
            // FIX in case a LE does NOT have attributes
            if (atts == null) {
                if (le != null) {
                    Log.error(this.getClass(), le.getName() + " does not have LE attributes configured");
                }
                return rst;
            }
            LegalEntityAttribute current;
            final Iterator<?> it = atts.iterator();

            while (it.hasNext() && (Util.isEmpty(rst))) {
                current = (LegalEntityAttribute) it.next();
                if (current.getAttributeType().equalsIgnoreCase(att)) {
                    rst = current.getAttributeValue();
                }
            }
        }
        return rst;
    }

    private JDate loadFechaVenci() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 31);
        calendar.set(Calendar.MONTH, 11);
        calendar.set(Calendar.YEAR, 9999);
        return JDate.valueOf(calendar.getTime());
    }

}
