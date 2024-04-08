package calypsox.tk.bo;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.util.partenon.PartenonUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatter;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;

import java.io.StringWriter;
import java.util.Optional;
import java.util.Vector;

public class PARTENONMSGBondMessageFormatter extends MessageFormatter {

    private Bond security;
    private String atributoReferencia = "";
    private boolean cedulas = false;

    /** Code format */
    private static final String IRDBOND = "IRDBOND";
    private static final String IRDBONDFWDCASH = "IRDBONDFWDCASH";


    public String parsePARTENON_ROW(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                    BOTransfer transfer, DSConnection dsConn) {
        String partenonMessage = "";
        if(null!=trade){
            partenonMessage = generateMsg(trade);
        }
        return partenonMessage;
    }

    /**
     * Generate Message
     *
     * @param trade
     * @return
     */
    private String generateMsg(Trade trade){
        StringWriter sw = new StringWriter();
        sw.append("CALYPSO").append(";");
        //0:Alta 1:Baja 2:Modificacion
        sw.append(PartenonUtil.getInstance().getAction(trade)).append(";");
        sw.append(String.valueOf(trade.getLongId())).append(";");
        sw.append(trade.getBook().getName()).append(";");
        sw.append(trade.getCounterParty().getExternalRef()).append(";");
        sw.append(getAlias(trade)).append(";");

        return sw.toString();
    }

    /**
     * Get Alias
     *
     * @return
     */
    public String getAlias(Trade trade){

        this.security = (Bond) ((Bond) loadProduct(trade)).getSecurity();
        LegalEntity issuer = BOCache.getLegalEntity(DSConnection.getDefault(), this.security.getIssuerId());
        boolean bondForwardCash = checkbondForwardCash(trade);
        getAtributoReferencia(bondForwardCash, trade);

        //Tipo de Cartera + Tipo de papel + Descripción del producto relacionado con el sistema a tratar
        // + Dirección + Tipo de Operación + Atributo Referencia
        StringWriter sw = new StringWriter();
        //Tipo de Cartera. BondForwardCash no se informa
        if (!bondForwardCash) {
            sw.append(getTipoCartera(trade, issuer));
        }
        //Tipo de papel. Cedulas no se informa
        if(!this.cedulas){
            sw.append(getTipoPapel());
        }
        //Descripción del producto relacionado con el sistema a tratar
        if (bondForwardCash) {
            sw.append(IRDBONDFWDCASH);
        } else {
            sw.append(IRDBOND);
        }
        //Direccion
        if (bondForwardCash) {
            sw.append(BOCreUtils.getInstance().loadBuySell(trade));
        } else {
            sw.append("");
        }
        //Tipo de Operacion
        if (!bondForwardCash)
            sw.append(getTipoOperacion(trade));
        //Atributo Referencia
        sw.append(this.atributoReferencia);

        return sw.toString();
    }

    private void getAtributoReferencia(boolean bondForwardCash, Trade trade){
        LegalEntity issuer = BOCache.getLegalEntity(DSConnection.getDefault(), this.security.getIssuerId());

        //Autocartera
        if(!bondForwardCash && isAutocartera(issuer, trade)){
            this.atributoReferencia = this.atributoReferencia + "AUTOC";
        }
        //Estructuradas
        if(!bondForwardCash && "Y".equalsIgnoreCase(loadSecCode("NOTAS_ESTRUCTURADAS"))){
            this.atributoReferencia = this.atributoReferencia + "NOTAEST";
        }
        //Parametrizacion contable especial
        else if(!bondForwardCash && "Y".equalsIgnoreCase(loadSecCode("CONTABILIDAD_ESPECIAL"))){
            this.atributoReferencia = this.atributoReferencia + "CONTESP";
        }
        //Cedulas
        else if ("Y".equalsIgnoreCase(loadSecCode("IS COVERED"))  &&
                (loadSecCode("ISIN").startsWith("ES") ||
                        (loadSecCode("ISIN").startsWith("XS") && "SPAIN".equalsIgnoreCase(this.security.getCountry())))){
            this.cedulas = true;
            if (loadSecCode("COLLATERAL DESCRIPTION") != null &&
                    "0019".equalsIgnoreCase(loadSecCode("COLLATERAL DESCRIPTION")) ){
                this.atributoReferencia = this.atributoReferencia + "CEDTERR";
            } else
                this.atributoReferencia = this.atributoReferencia + "CEDHIPO";
        }
        //Subordinados/Subordinados convertibles
        else if  ("Y".equalsIgnoreCase(loadSecCode("IS_SUBORDINATED"))){
            if  ("Y".equalsIgnoreCase(loadSecCode("IS COVERED")) && isAutocartera(issuer, trade))
                this.atributoReferencia = this.atributoReferencia + "SUBORCONV";
            else
                this.atributoReferencia = this.atributoReferencia + "SUBOR";
        }
        else if (!bondForwardCash) {
            //Titulaciones J000001961
            if("Asset Backed Securities".equalsIgnoreCase(loadSecCode("INDUSTRY_SECTOR")) ||
                    "J000050747".equalsIgnoreCase(issuer.getExternalRef()) ||
                    ("084".equalsIgnoreCase(getSectorContable(issuer)) &&
                            ( (validateISIN(loadSecCode("ISIN"), "ES", 4, "3")) ||
                                    ( validateISIN(loadSecCode("ISIN"), "ES", 4, "5"))))){
                this.atributoReferencia = this.atributoReferencia + "TITULACT";
            } else if ("Mortgage Securities".equalsIgnoreCase(loadSecCode("INDUSTRY_SECTOR")) ||
                    ("086".equalsIgnoreCase(getSectorContable(issuer)) &&
                            ( (validateISIN(loadSecCode("ISIN"), "ES", 4, "3")) ||
                                    ( validateISIN(loadSecCode("ISIN"), "ES", 4, "5"))))){
                this.atributoReferencia = this.atributoReferencia + "TITULHIPO";
            }
            // Identificacion Exdividend
            else if ("Y".equalsIgnoreCase(loadSecCode("IS_EXDIVIDEND"))) {
                this.atributoReferencia = this.atributoReferencia + "EXDIV";
            }
            // Identificacion de bonos con opcionalidad
            else if ("Y".equalsIgnoreCase(loadSecCode("IS_PUTTABLE")) || "Y".equalsIgnoreCase(loadSecCode("IS_CALLABLE"))) {
                this.atributoReferencia = this.atributoReferencia + "OPC";
            }
            // Identificacion de bonos convertibles
            else if ("Y".equalsIgnoreCase(loadSecCode("IS_CONVERTIBLE"))) {
                this.atributoReferencia = this.atributoReferencia + "CONV";
            }
        }
    }

    private String getTipoCartera(Trade trade, LegalEntity issuer){
        String tipoCartera = "";

        if (isAutocartera(issuer, trade)) {
            tipoCartera = "IC";
        } else {
            if ("Inversion crediticia".equalsIgnoreCase(getAccountingLink(trade))) {
                tipoCartera = "IC";
            } else if ("Disponible para la venta".equalsIgnoreCase(getAccountingLink(trade))) {
                tipoCartera = "CDV";
            } else if ("Negociacion".equalsIgnoreCase(getAccountingLink(trade))) {
                tipoCartera = "";
            } else if ("Otros a valor razonable".equalsIgnoreCase(getAccountingLink(trade))) {
                tipoCartera = "OT";
            } else if ("Designados a Valor Razonable".equalsIgnoreCase(getAccountingLink(trade))) {
                tipoCartera = "DR";
            }
        }
        return tipoCartera;
    }

    private String getTipoPapel(){
        String tipoPapel = loadSecCode("ISSUE_TYPE");
        return tipoPapel;
    }

    private String getTipoOperacion(Trade trade){
        return Optional.ofNullable(trade.getMirrorBook()).isPresent() ? "INTERNA" : "EXTERNA";
    }

    private Product loadProduct(Trade trade) {
        if (null != trade) {
            Product product = trade.getProduct();
            if (product instanceof Bond) {
                return product;
            }
        }
        return null;
    }

    private String loadSecCode(String secCode) {
        return null != this.security ? this.security.getSecCode(secCode) : "";
    }

    private String getAccountingLink(Trade trade){
        return trade.getBook().getAccountingBook().getName();
    }

    private String getSectorContable(LegalEntity le){
        final LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, le.getId(), "ALL", "SECTORCONTABLE");
        return attr!=null ? attr.getAttributeValue() : "";
    }

    private Boolean validateISIN(String isin, String startwith,int positionValidate, String charValidate){
        boolean out;
        out = isin.startsWith(startwith);
        if (positionValidate != 0){
            out = isin.indexOf(charValidate,positionValidate - 1) == Integer.valueOf(charValidate);
        }

        return out;
    }

    private Boolean isAutocartera(LegalEntity issuer, Trade trade){
        return (issuer).equals(trade.getBook().getLegalEntity()) ? true : false;
    }

    private Boolean checkbondForwardCash(Trade trade){
        return "true".equalsIgnoreCase(trade.getKeywordValue("BondForward"))
                && "Cash".equalsIgnoreCase(trade.getKeywordValue("BondForwardType"))  ? true : false;
    }

}
