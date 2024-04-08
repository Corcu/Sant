package calypsox.tk.report.extracontable;

import calypsox.util.SantDateUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.product.*;
import com.calypso.tk.service.DSConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author aalonsop
 */
public class MICExtracontablePledgeBuilder extends MICExtracontableTradeBuilder {

    private static final String CIF_LE_ATTR = "TAXID";


    Pledge pledgeTrade;

    public MICExtracontablePledgeBuilder(Trade trade) {
        super(trade);
        this.pledgeTrade = (Pledge) trade.getProduct();

    }

    @Override
    public MICExtracontableBean build() {
        this.messageBean = super.build();
        this.messageBean.setFIniFij(pledgeTrade.getStartDate());
        this.messageBean.setFVenciFij(getEndDate(pledgeTrade));
        this.messageBean.setFVenci(getEndDate(pledgeTrade));
        this.messageBean.setCodGLSContrapar(sourceObject.getCounterParty().getCode());
        this.messageBean.setCodContrapar(sourceObject.getCounterParty().getExternalRef());
        this.messageBean.setCodJContrapar(sourceObject.getCounterParty().getExternalRef());
        setNumOpeFront(sourceObject);
        Security security = getSecFinanceSecurity();
        this.messageBean.setCodIsin(getSecIsin(security));
        this.messageBean.setCodCifEmi(getSecIssuerCif(security));
        this.messageBean.setCodRefInGr(sourceObject.getKeywordValue("RIG_CODE"));
        this.messageBean.setTcRefInt(getSecInternalReference(security));
        this.messageBean.setClaseContable(getMappedAccountingBook(sourceObject));
        setIssuerSectorCode(security);

        this.messageBean.setCodGLSEntidad(null != sourceObject.getBook() ? sourceObject.getBook().getLegalEntity().getCode() : "");
        this.messageBean.setCodGLSEmisor(getIssuer(pledgeTrade));
        this.messageBean.setCodNumOpeBack(String.valueOf(sourceObject.getLongId()));

        return this.messageBean;
    }

    /**
     * Looks for Repo's MurexRootContract id
     */
    private void setNumOpeFront(Trade trade){
        try{
        String numFrontId=Optional.ofNullable(trade).map(Trade::getInternalReference)
                .filter(ref->!Util.isEmpty(ref)).map(Long::parseLong)
                .map(this::getMxRootContractKwdById).orElse("");
        this.messageBean.setCodNumOpeFront(numFrontId);
        }catch(NumberFormatException exc){
            Log.warn(this,exc);
        }

    }

    private String getMxRootContractKwdById(long tradeId){
        String res="";
        String kwdStr=("MurexRootContract");
        List<String> kwdName=new ArrayList<>();
        kwdName.add(kwdStr);
        try {
            Map<String,String> kwds=DSConnection.getDefault().getRemoteTrade().getTradeKeywords(tradeId,kwdName);
            res=Optional.ofNullable(kwds).map(mp->mp.get(kwdStr)).orElse("");
        } catch (CalypsoServiceException exc) {
           Log.error(this,exc.getCause());
        }
        return res;
    }

    private void setIssuerSectorCode(Security security) {
        String codSector = getCodSector(security);
        this.messageBean.setCodSector(codSector);
        this.messageBean.setSecBancoEspEmisor(codSector);
    }

    private JDate getEndDate(Pledge pledge) {
        return Optional.ofNullable(pledge.getEndDate())
                .orElseGet(() -> SantDateUtil.addBusinessDays(JDate.getNow(), 1));

    }

    private String getIssuer(Pledge pledge) {
        String out = "";
        if (null != pledge) {
            Product security = pledge.getSecurity();
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
        Product underlyingProduct = this.pledgeTrade.getSecurity();
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
        return MICExtracontableRepoBuilder.MICRepoAccountingBookMapping.lookup(accBookName).mappingCode;
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

        static MICExtracontablePledgeBuilder.MICRepoAccountingBookMapping lookup(String accBookName) {
            MICExtracontablePledgeBuilder.MICRepoAccountingBookMapping result;
            String mappedBookName = accBookName.toUpperCase();
            mappedBookName = mappedBookName.replace(" ", "_");
            try {
                result = MICExtracontablePledgeBuilder.MICRepoAccountingBookMapping.valueOf(mappedBookName);
            } catch (IllegalArgumentException e) {
                result = INVALID;
            }
            return result;
        }
    }
}
