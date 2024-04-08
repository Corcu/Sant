package calypsox.tk.report.extracontable;

import calypsox.util.SantDateUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.Security;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;

/**
 * @author jonker
 */
public class MICExtracontableRepoBuilder extends MICExtracontableTradeBuilder {

    private static final String CIF_LE_ATTR = "TAXID";


    Repo repoTrade;

    public MICExtracontableRepoBuilder(Trade trade) {
        super(trade);
        this.repoTrade = (Repo) trade.getProduct();

    }

    @Override
    public MICExtracontableBean build() {
        this.messageBean = super.build();
        this.messageBean.setFIniFij(repoTrade.getStartDate());
        this.messageBean.setFVenciFij(getEndDate(repoTrade));
        this.messageBean.setFVenci(getEndDate(repoTrade));
        this.messageBean.setCodGLSContrapar(sourceObject.getCounterParty().getCode());
        this.messageBean.setCodContrapar(sourceObject.getCounterParty().getExternalRef());
        this.messageBean.setCodJContrapar(sourceObject.getCounterParty().getExternalRef());
        this.messageBean.setCodNumOpeFront(sourceObject.getExternalReference());
        Security security = getSecFinanceSecurity();
        this.messageBean.setCodIsin(getSecIsin(security));
        this.messageBean.setCodCifEmi(getSecIssuerCif(security));
        this.messageBean.setCodRefInGr(sourceObject.getKeywordValue("RIG_CODE"));
        this.messageBean.setTcRefInt(getSecInternalReference(security));
        this.messageBean.setClaseContable(getMappedAccountingBook(sourceObject));
        setIssuerSectorCode(security);

        this.messageBean.setCodGLSEntidad(null != sourceObject.getBook() ? sourceObject.getBook().getLegalEntity().getCode() : "");
        this.messageBean.setCodGLSEmisor(getIssue(repoTrade));
        this.messageBean.setCodNumOpeBack(String.valueOf(sourceObject.getLongId()));
        this.messageBean.setFvalor(sourceObject.getSettleDate());


        return this.messageBean;
    }

    private void setIssuerSectorCode(Security security) {
        String codSector = getCodSector(security);
        this.messageBean.setCodSector(codSector);
        this.messageBean.setSecBancoEspEmisor(codSector);
    }

    private JDate getEndDate(Repo repoTrade) {
        return Optional.ofNullable(repoTrade.getEndDate())
                .orElseGet(() -> SantDateUtil.addBusinessDays(JDate.getNow(), 1));

    }

    private String getIssue(Repo repo) {
        String out = "";
        if (null != repo) {
            Product security = repo.getSecurity();
            if (security instanceof Bond) {
                int issuerId = ((Bond) security).getIssuerId();
                LegalEntity legalEntity = BOCache.getLegalEntity(DSConnection.getDefault(), issuerId);
                if (null != legalEntity) {
                    out = legalEntity.getCode();
                }
            } else if (security instanceof Equity && null != ((Equity) security).getIssuer()) {
                out = ((Equity) security).getIssuer().getCode();
            }
        }
        return out;
    }

    private String getCodSector(Security security) {
        return Optional.ofNullable(security).map(sec -> getLEAttr(sec.getIssuerId(), SECCONT_LE_ATTR, LegalEntity.ISSUER))
                .orElse("");
    }

    private String getSecIssuerCif(Security security) {
        String issuerCif = "";
        if (security != null) {
            issuerCif = getLEAttr(security.getIssuerId(), CIF_LE_ATTR, LegalEntity.ISSUER);
        }
        return issuerCif;
    }

    private Security getSecFinanceSecurity() {
        Security security = null;
        Product underlyingProduct = repoTrade.getSecurity();
        if (underlyingProduct instanceof Security) {
            security = (Security) underlyingProduct;
        }
        return security;
    }

    private String getSecInternalReference(Security security) {
        return getSecuritySecCode(BOND_SEC_CODE_REF_INTERNA, security);
    }

    private String getRigCode(Trade trade) {
        return Optional.ofNullable(trade).map(t -> t.getKeywordValue("RIG_CODE")).orElse("");
    }

    private String getSecIsin(Security security) {
        return getSecuritySecCode("ISIN", security);
    }

    private String getSecuritySecCode(String secCode, Security bond) {
        return Optional.ofNullable(bond)
                .map(bondSecond -> bondSecond.getSecurity().getSecCode(secCode))
                .orElse("");
    }

    @Override
    public String getMappedAccountingBook(Trade trade) {
        String accBookName = Optional.ofNullable(trade.getBook()).map(Book::getAccountingBook)
                .map(AccountingBook::getName).orElse("");
        return MICRepoAccountingBookMapping.lookup(accBookName).mappingCode;
    }

    enum MICRepoAccountingBookMapping {
        NEGOCIACION("0"),
        DISPONIBLE_PARA_LA_VENTA("2"),
        COSTE_AMORTIZADO("2"),
        INVERSION_CREDITICIA("2"),
        INVERSION_A_VENCIMIENTO("2"),
        OTROS_A_VALOR_RAZONABLE("3"),
        INVALID("");
        String mappingCode;

        MICRepoAccountingBookMapping(String mappingCode) {
            this.mappingCode = mappingCode;
        }

        static MICRepoAccountingBookMapping lookup(String accBookName) {
            MICRepoAccountingBookMapping result;
            String mappedBookName = accBookName.toUpperCase();
            mappedBookName = mappedBookName.replace(" ", "_");
            try {
                result = MICRepoAccountingBookMapping.valueOf(mappedBookName);
            } catch (IllegalArgumentException e) {
                result = INVALID;
            }
            return result;
        }
    }
}
