package calypsox.tk.report.extracontable;

import calypsox.tk.report.MicReportUtil;
import calypsox.tk.report.extracontable.filter.MrgCallContractPOFilter;
import com.calypso.executesql.support.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.Security;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.Country;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class MICExtracontableInventoryBuilder extends MICExtracontableBuilder<Inventory> {

    static final String SECCONT_LE_ATTR="SECTORCONTABLE";
    /*
     * CodigoSecto
     */
    static final String CODIGO_SECTOR = "123";
    //dummy date
    static final String DUMMY_DATE = "0001/01/01";

    Product product;

    private final String centroOpContableBookAttr="Centro OPContable GER";

    MrgCallContractPOFilter poFilter=new MrgCallContractPOFilter();

    public MICExtracontableInventoryBuilder(Inventory position) {
        messageBean = new MICExtracontableBean();
        this.sourceObject = position;
        if(this.sourceObject instanceof InventorySecurityPosition){
            int productId=((InventorySecurityPosition)position).getSecurityId();
            try {
                this.product=DSConnection.getDefault().getRemoteProduct().getProduct(productId);
            } catch (CalypsoServiceException e) {
                e.printStackTrace();
            }
        }
    }

    public MICExtracontableBean build(){

        LegalEntity issuer=getIssuer();
        Book book=this.sourceObject.getBook();
        CollateralConfig contract= CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),sourceObject.getMarginCallConfigId());

        messageBean.setIdCent(getCentro(book,contract));
        String isin=Optional.ofNullable(product).map(p->p.getSecCode("ISIN")).orElse("");
        messageBean.setCodIsin(isin);
        messageBean.setTcRefInt(isin);
        messageBean.setEmprContrato(getEmpreContrato(book,contract));
        messageBean.setCodSector(CODIGO_SECTOR);

        messageBean.setIndTipOper("");
        messageBean.setIndPertGrupo("");
        messageBean.setMonedaContravalor("");

        messageBean.setNumContrato("");

        messageBean.setNumOperacDGO("");
        messageBean.setIndCoberCont("");
        messageBean.setCodPaisEmisor(getCountryISOCode(issuer));

        messageBean.setCodProducto("495");
        messageBean.setIndSubCa(getIndSubCa());
        messageBean.setMonContr(sourceObject.getSettleCurrency());

        messageBean.setCodGLSContrapar(Optional.ofNullable(contract).map(CollateralConfig::getLegalEntity).map(LegalEntity::getCode).orElse(""));
        messageBean.setCodGLSEmisor(Optional.ofNullable(issuer).map(LegalEntity::getCode).orElse(""));
        messageBean.setCodGLSEntidad(book.getLegalEntity().getCode());
        messageBean.setCodPaisContrapar("");
        messageBean.setCodContrapar(Optional.ofNullable(issuer).map(LegalEntity::getExternalRef).orElse(""));
        messageBean.setCodEmisor("");
        messageBean.setDescCodContrapar(Optional.ofNullable(issuer).map(LegalEntity::getName).orElse(""));

        messageBean.setCodCifEmi(getLeAttr(issuer,"TAXID"));

        messageBean.setFContrata(JDate.valueOf(sourceObject.getPositionDate()));
        messageBean.setFVenci(JDate.valueOf("9999/12/31"));

        messageBean.setSecBancoEspContrapar(getLeAttr(Optional.ofNullable(contract).map(CollateralConfig::getLegalEntity).orElse(null),SECCONT_LE_ATTR));
        messageBean.setSecBancoEspEmisor(getLeAttr(issuer,SECCONT_LE_ATTR));
        messageBean.setTipoInteres(0L);

        messageBean.setCodPortf(String.valueOf(sourceObject.getBook().getName()));

        messageBean.setCodTipoOpe3("");

        messageBean.setCodEstrOpe("ESP  ");

        messageBean.setCodTipoCobertura("");
        messageBean.setCodSentido("");
        messageBean.setImpNominal(sourceObject.getTotal());
        messageBean.setCodJContrapar(Optional.ofNullable(contract).map(CollateralConfig::getLegalEntity).map(LegalEntity::getExternalRef).orElse(""));
        messageBean.setCodNumOpeFront("");
        messageBean.setCodNumOpeBack("");
        messageBean.setCodNumEventoBack("");
        messageBean.setFvalor(JDate.valueOf(DUMMY_DATE));
        messageBean.setImpIntereses(0L);
        messageBean.setImpPrincipal(sourceObject.getTotal());


        messageBean.setFIniFij(null);
        messageBean.setFVenciFij(null);

        messageBean.setClaseContable(getMappedAccountingBook(sourceObject));

        messageBean.setAccountingRule("");
        messageBean.setInternal(getInternal(book));
        messageBean.setSuv("");
        messageBean.setDirection(getDirection());
        messageBean.setAutoCartera(getAutocartera());
        messageBean.setProductId(Optional.ofNullable(this.product).map(Product::getId).orElse(0));
        messageBean.setEquityType(getEquityType());
        messageBean.setAgente("");

        messageBean.setUnderlyingType(getUnderlyingType());
        messageBean.setContractType(Optional.ofNullable(contract).map(CollateralConfig::getContractType).orElse(""));

        return messageBean;
    }

    public String getLeAttr(LegalEntity le,String attrName){
        return Optional.ofNullable(le).map(ent->getLEAttr(ent.getId(),attrName, "ALL")).orElse("");
    }
    @Override
    protected String getMappedAccountingBook(Inventory position) {
        return super.getMappedAccountingBook(position.getBook());
    }

    private String getEquityType(){
        return Optional.ofNullable(this.product).map(p->p.getSecCode("EQUITY_TYPE")).orElse("");
    }

    private String getIndSubCa(){
        String res="";
        if(this.product instanceof Equity){
            res= MicReportUtil.getIDSUCACO((Equity)product,false);
        }
        return res;
    }

    private String getInternal(Book book){
        String internal="N";
        if(Optional.ofNullable(book).map(Book::getLegalEntity).map(LegalEntity::getCode)
        .map("BSTE"::equals).orElse(false)){
            internal="Y";
        }
        return internal;
    }

    private String getUnderlyingType(){
        String type=Optional.ofNullable(this.product).map(Product::getType).map(String::toLowerCase).orElse("");
        String mappedValue="";
        if(type.contains(Bond.class.getSimpleName().toLowerCase())){
            mappedValue="RF";
        }else if(type.contains(Equity.class.getSimpleName().toLowerCase())){
            mappedValue="RV";
        }
        return mappedValue;
    }

    protected LegalEntity getIssuer(){
        LegalEntity le=null;
        if (this.product instanceof Security) {
            Security equity = (Security) product;
            le = BOCache.getLegalEntity(DSConnection.getDefault(),equity.getIssuerId());
        }
        return le;
    }

    protected String getCountryISOCode(LegalEntity le){
        String countryStr=Optional.ofNullable(le).map(LegalEntity::getCountry).orElse("");
        return Optional.ofNullable(BOCache.getCountry(DSConnection.getDefault(), countryStr))
                .map(Country::getISOCode).orElse("");
    }

    private String getAutocartera(){
        String res="";
        if (this.product instanceof Security) {
            Security sec = (Security) product;
            LegalEntity le = BOCache.getLegalEntity(DSConnection.getDefault(),sec.getIssuerId());
            res = le != null && le.getCode().equals("BSTE") ? "SI" : "NO";
        }
        return res;
    }

    private String getDirection(){
      String direction="CORTA";
      double total=this.sourceObject.getTotal();
      if(total>=0) {
          return "LARGA";
      }
      return direction;
    }

    private String getCentro(Book book,CollateralConfig config){
       String res=Optional.ofNullable(book).map(b->b.getAttribute(centroOpContableBookAttr))
               .map(str->parseCentroGerBookAttr(str,true)).orElse("");
       if(Util.isEmpty(res)){
           res=getCentroDefault(config);
       }
       return res;
    }

    private String getEmpreContrato(Book book,CollateralConfig config){
        String res=Optional.ofNullable(book).map(b->b.getAttribute(centroOpContableBookAttr))
                .map(str->parseCentroGerBookAttr(str,false)).orElse("");
        if(Util.isEmpty(res)){
            res=getEmpreContratoDefault(config);
        }
        return res;
    }

    private String parseCentroGerBookAttr(String attrValue, boolean isCentro){
        String parsedValue="";
        if(!Util.isEmpty(attrValue)&&attrValue.length()==8){
            if(isCentro){
                parsedValue=attrValue.substring(4,8);
            }else{
                parsedValue=attrValue.substring(0,4);
            }
        }
        return parsedValue;
    }
    private String getCentroDefault(CollateralConfig config){
        String centro="1999";
        if(this.poFilter.isBDSDContract(config)){
            centro="1111";
        }
        return centro;
    }

    private String getEmpreContratoDefault(CollateralConfig config){
        String centro="0049";
        if(this.poFilter.isBDSDContract(config)){
            centro="0306";
        }
        return centro;
    }
}
