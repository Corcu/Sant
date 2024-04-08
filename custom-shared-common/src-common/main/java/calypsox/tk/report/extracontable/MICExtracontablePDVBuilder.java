package calypsox.tk.report.extracontable;

import calypsox.util.SantDateUtil;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.product.Security;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class MICExtracontablePDVBuilder extends MICExtracontableTradeBuilder {

    private static final String CIF_LE_ATTR="TAXID";



    SecLending secLending;

    public MICExtracontablePDVBuilder(Trade trade) {
       super(trade);
        this.secLending= (SecLending) trade.getProduct();

    }

    @Override
    public MICExtracontableBean build() {
        this.messageBean=super.build();
        this.messageBean.setFIniFij(secLending.getStartDate());
        this.messageBean.setFVenciFij(getEndDate(secLending));
        this.messageBean.setFVenci(getEndDate(secLending));
        Security security=getSecFinanceSecurity();
        this.messageBean.setCodIsin(getSecIsin(security));
        this.messageBean.setCodCifEmi(getSecIssuerCif(security));
        this.messageBean.setCodRefInGr(getSecInternalReference(security));
        this.messageBean.setTcRefInt(getSecInternalReference(security));
        setIssuerSectorCode(security);
        return this.messageBean;
    }

    private void setIssuerSectorCode(Security security){
        String codSector=getCodSector(security);
        this.messageBean.setCodSector(codSector);
        this.messageBean.setSecBancoEspEmisor(codSector);
    }
    private JDate getEndDate(SecLending secLending){
        return Optional.ofNullable(secLending.getEndDate())
                .orElseGet(()-> SantDateUtil.addBusinessDays(JDate.getNow(),1));

    }

    private String getCodSector(Security security){
       return Optional.ofNullable(security).map(sec->getLEAttr(sec.getIssuerId(), SECCONT_LE_ATTR, LegalEntity.ISSUER))
        .orElse("");
    }
    private String getSecIssuerCif(Security security){
        String issuerCif="";
        if(security!=null){
            issuerCif=getLEAttr(security.getIssuerId(),CIF_LE_ATTR,LegalEntity.ISSUER);
        }
        return issuerCif;
    }
    private Security getSecFinanceSecurity() {
        Security security=null;
        Product underlyingProduct = secLending.getSecurity();
        if (underlyingProduct instanceof Security){
            security= (Security) underlyingProduct;
        }
        return security;
    }

    private String getSecInternalReference(Security security) {
    // [BAU] If the security is an equity we return the ISIN, not the REF_INTERNA code
        if (security instanceof Equity){
            return getSecuritySecCode("ISIN",security);
        }
        return getSecuritySecCode(BOND_SEC_CODE_REF_INTERNA,security);
    }

    private String getSecIsin(Security security) {
        return getSecuritySecCode("ISIN",security);
    }

    private String getSecuritySecCode(String secCode,Security bond) {
        return Optional.ofNullable(bond)
                .map(bondSecond -> bondSecond.getSecurity().getSecCode(secCode))
                .orElse("");
    }

}
