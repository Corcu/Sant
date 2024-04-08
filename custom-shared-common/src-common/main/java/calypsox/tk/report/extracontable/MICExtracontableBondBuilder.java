package calypsox.tk.report.extracontable;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.Country;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

/**
 * @author dmenendd
 */
public class MICExtracontableBondBuilder extends MICExtracontableTradeBuilder {

    Bond bond;

    public MICExtracontableBondBuilder(Trade trade) {
        super(trade);
        Product tradeProduct = Optional.ofNullable(trade).map(Trade::getProduct).orElse(null);
        if (tradeProduct instanceof Bond) {
            bond = (Bond) tradeProduct;
        }

    }

    @Override
    public  MICExtracontableBean build() {
        messageBean=super.build();
        messageBean.setCodIsin(loadIsin(getBondReference()));

        messageBean.setIndTipOper(isInternal(sourceObject, false));

        messageBean.setMonedaContravalor("EUR");

        messageBean.setCodPaisEmisor(getCountryISO(getIssuer(getBondReference())));
        messageBean.setCodRefInGr(sourceObject.getKeywordValue("RIG_CODE"));

        messageBean.setCodGLSContrapar(sourceObject.getCounterParty().getCode());
        messageBean.setCodGLSEmisor(getCodIssuer(getBondReference()));
        messageBean.setCodGLSEntidad(sourceObject.getBook().getLegalEntity().getCode());
        messageBean.setCodPaisContrapar(getCountryISO(sourceObject.getCounterParty()));
        messageBean.setCodContrapar(sourceObject.getCounterParty().getExternalRef());
        messageBean.setCodEmisor(getCodIssuer(getBondReference()));
        messageBean.setDescCodContrapar(sourceObject.getCounterParty().getName());
        messageBean.setCodCifEmi(getCodCifEmisor(getBondReference()));

        messageBean.setFVenci(sourceObject.getSettleDate());

        messageBean.setSecBancoEspEmisor(getLEAttr(getIssuerId(getBondReference()),SECCONT_LE_ATTR, LegalEntity.COUNTERPARTY));

        messageBean.setCodTipoOpe3(isInternal(sourceObject, true));

        messageBean.setCodSentido(getSentido(getBondReference(), sourceObject));
        messageBean.setImpNominal( - sourceObject.getQuantity() * getBondReference().getPrincipal(sourceObject.getSettleDate()));
        messageBean.setCodJContrapar(sourceObject.getCounterParty().getExternalRef());

        messageBean.setCodNumOpeFront(sourceObject.getExternalReference());
        messageBean.setCodNumOpeBack(String.valueOf(sourceObject.getLongId()));

        messageBean.setCodNumEventoBack(String.valueOf(sourceObject.getVersion()));
        messageBean.setFvalor(sourceObject.getSettleDate());

        messageBean.setImpPrincipal(- sourceObject.getQuantity() * getBondReference().getPrincipal(sourceObject.getSettleDate()) * sourceObject.getTradePrice());

        messageBean.setFIniFij(getInitDateCashFlow(getBondReference()));
        messageBean.setFVenciFij(getLastDateCashFlow(getBondReference()));

        messageBean.setIndAnotCuenta(getIndicadorAnotacionCuenta(getLEAttr(getIssuerId(getBondReference()),SECCONT_LE_ATTR, LegalEntity.COUNTERPARTY), getCountryISO(getIssuer(getBondReference()))));

        messageBean.setTcRefInt(loadIsin(getBondReference()));

        return messageBean;
    }

    /**
     *
     * @return
     */
    private Product getBondReference() {
        Product product = null;
        if (Optional.ofNullable(bond).map(Bond::getProduct)
                .orElse(null) instanceof Bond) {
            product = bond.getProduct();
        }
        return product;
    }

    /**
     *
     * @param security
     * @return
     */
    private String loadIsin(Product security) {
        return null != security ? security.getSecCode("ISIN") : "";
    }

    /**
     *
     * @param product
     * @return
     */
    private String getCodIssuer(Product product) {
        Bond bond = (Bond) product;
        LegalEntity issuer = BOCache.getLegalEntity(DSConnection.getDefault(), bond.getIssuerId());
        return null != bond && null!=issuer ? issuer.getCode() : "";
    }

    /**
     *
     * @param product
     * @return
     */
    private LegalEntity getIssuer(Product product) {
        Bond bond = (Bond) product;
        LegalEntity issuer = BOCache.getLegalEntity(DSConnection.getDefault(), bond.getIssuerId());
        return null != bond && null!=issuer ? issuer : null;
    }

    /**
     *
     * @param product
     * @return
     */
    private int getIssuerId(Product product) {
        Bond bond = (Bond) product;
        LegalEntity issuer = BOCache.getLegalEntity(DSConnection.getDefault(), bond.getIssuerId());
        return null != bond && null!=issuer ? issuer.getId() : 0;
    }

    /**
     *
     * @param product
     * @return
     */
    private String getCodCifEmisor(Product product){
        String cif =  "";
        if (product instanceof Bond) {
            Bond bond = (Bond) product;
            LegalEntity le = BOCache.getLegalEntity(DSConnection.getDefault(), bond.getIssuerId());
            if (le != null){
                cif = getLegalEntityAttribute(le, "TAXID");
            }
        }
        return !Util.isEmpty(cif) ? cif : "";
    }

    /**
     *
     * @param le
     * @param att
     * @return
     */
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

    /**
     * @param product
     * @param trade
     * @return C (BUY) OR V (SELL)
     */
    private String getSentido(Product product, Trade trade) {
        return product.getBuySell(trade) == 1 ? "C" : "V";
    }

    /**
     *
     * @param le
     * @return
     */
    public static String getCountryISO(LegalEntity le) {
        if (le != null
                && !Util.isEmpty(le.getCountry())) {
            String countryName = le.getCountry();
            try {
                Country country = BOCache.getCountry(DSConnection.getDefault(), countryName);
                if (country != null) {
                    return country.getISOCode();
                }
            } catch (Exception e) {
                Log.error("MICExtracontableBondBuilder", "Error Extractin ISO Country from " + countryName + ": ", e);
            }
        }
        return "";
    }

    /**
     *
     * @param trade
     * @return
     */
    public static String isInternal(Trade trade, boolean tipOper3) {
        if(tipOper3)
            return Optional.ofNullable(trade.getMirrorBook()).isPresent() ? "0" : "1";
        else
            return Optional.ofNullable(trade.getMirrorBook()).isPresent() ? "I" : "E";
    }

    /**
     *
     * @param secFinanciero
     * @param codPais
     * @return
     */
    public static String getIndicadorAnotacionCuenta(String secFinanciero, String codPais) {
        return secFinanciero != null && secFinanciero != null ?
                ("050".equalsIgnoreCase(secFinanciero) || "060".equalsIgnoreCase(secFinanciero) ||
                        "065".equalsIgnoreCase(secFinanciero) || "070".equalsIgnoreCase(secFinanciero))
                        && "ES".equalsIgnoreCase(codPais) ? "V" : "F" : "F";
    }

    /**
     *
     * @param product
     * @return
     */
    public JDate getInitDateCashFlow(Product product){
        JDate date =  JDate.valueOf(DUMMY_DATE);
        if (product instanceof Bond) {
            CashFlowSet flows = ((Bond) product).getFlows();
            if(null!=flows){
                for(int i = 0; i < flows.size(); i++){
                    CashFlow flow = flows.get(i);
                    if("INTEREST".equalsIgnoreCase(flow.getType())){
                        date = flow.getStartDate();
                        i = flows.size();
                    }
                }
            }
        }
        return date;
    }

    /**
     *
     * @param product
     * @return
     */
    public JDate getLastDateCashFlow(Product product){
        JDate date =  JDate.valueOf(DUMMY_DATE);
        if (product instanceof Bond) {
            CashFlowSet flows = ((Bond) product).getFlows();
            if(null!=flows){
                CashFlow flow = flows.get(flows.size() - 1);
                date = flow.getEndDate();
            }
        }
        return date;
    }

}
