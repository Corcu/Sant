package calypsox.tk.report.extracontable;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;
import org.apache.commons.lang.StringUtils;

import java.util.Optional;

/**
 * dmenendez
 */
public class MICExtracontableTradeBuilder extends MICExtracontableBuilder<Trade>{

    static final String BOND_SEC_CODE_REF_INTERNA = "REF_INTERNA";
    static final String SECCONT_LE_ATTR="SECTORCONTABLE";
    /*
     * CodigoSecto
     */
    static final String CODIGO_SECTOR = "123";
    static final String PARTENON_ACC_GID = "                     ";

    static final Integer CTE_21 = 21;

    //dummy date
    static final String DUMMY_DATE = "0001/01/01";

    MICExtracontableTradeBuilder(Trade trade) {
        messageBean = new MICExtracontableBean();
        this.sourceObject = trade;
    }

    public MICExtracontableBean build(){
        String partenonAccId = getAndFormatPartenonId(sourceObject.getKeywordValue("PartenonAccountingID"));

        messageBean.setIdCent(StringUtils.substring(partenonAccId,4,8));
        messageBean.setCodIsin("");
        messageBean.setEmprContrato(StringUtils.substring(partenonAccId,0,4));
        messageBean.setCodSector(CODIGO_SECTOR);

        messageBean.setIndTipOper("");
        messageBean.setIndPertGrupo("");
        messageBean.setMonedaContravalor("");

        messageBean.setNumContrato(StringUtils.substring(partenonAccId,11,18));

        messageBean.setNumOperacDGO("");
        messageBean.setIndCoberCont("");
        messageBean.setCodPaisEmisor("");

        messageBean.setCodProducto(StringUtils.substring(partenonAccId,8,11));
        messageBean.setIndSubCa(StringUtils.substring(partenonAccId,18,21));
        messageBean.setMonContr(sourceObject.getSettleCurrency());

        messageBean.setCodGLSContrapar("");
        messageBean.setCodGLSEmisor("");
        messageBean.setCodGLSEntidad("");
        messageBean.setCodPaisContrapar("");
        messageBean.setCodContrapar("");
        messageBean.setCodEmisor("");
        messageBean.setDescCodContrapar("");

        messageBean.setCodCifEmi("");
        messageBean.setFContrata(JDate.valueOf(sourceObject.getTradeDate()));
        messageBean.setFVenci(sourceObject.getMaturityDate());

        messageBean.setSecBancoEspContrapar(getLEAttr(sourceObject.getCounterParty().getId(),SECCONT_LE_ATTR, LegalEntity.COUNTERPARTY));
        messageBean.setSecBancoEspEmisor("");
        messageBean.setTipoInteres(0L);

        messageBean.setCodPortf(String.valueOf(sourceObject.getBook().getName()));

        messageBean.setCodTipoOpe3("");

        messageBean.setCodEstrOpe("ESP  ");

        messageBean.setCodTipoCobertura("");
        messageBean.setCodSentido("");
        messageBean.setImpNominal(0.0D);
        messageBean.setCodJContrapar("");
        messageBean.setCodNumOpeFront("");
        messageBean.setCodNumOpeBack("");
        messageBean.setCodNumEventoBack("");
        messageBean.setFvalor(JDate.valueOf(DUMMY_DATE));
        messageBean.setImpIntereses(0L);
        messageBean.setImpPrincipal(0.0D);

        messageBean.setFIniFij(sourceObject.getSettleDate());
        messageBean.setFVenciFij(sourceObject.getMaturityDate());

        messageBean.setClaseContable(getMappedAccountingBook(sourceObject));

        messageBean.setCdreopin(sourceObject.getKeywordValue("ROI"));

        return messageBean;
    }

    @Override
    protected String getMappedAccountingBook(Trade trade) {
        return super.getMappedAccountingBook(trade.getBook());
    }

    private String getAndFormatPartenonId(String value) {
        String ckeckedContent = value;
        if (Util.isEmpty(value)){
            ckeckedContent =  PARTENON_ACC_GID;
        } else if (value.length() <  CTE_21) {
            ckeckedContent = StringUtils.rightPad(value,CTE_21);
        }
        return ckeckedContent;
    }


    protected String getLEAttr(int leId, String attributeName, String role){
        LegalEntityAttribute attribute=
                BOCache.getLegalEntityAttribute(DSConnection.getDefault(),0,leId,role,attributeName);
        return Optional.ofNullable(attribute).map(LegalEntityAttribute::getAttributeValue).orElse("");
    }

}
