package calypsox.tk.util.bean;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "_id",
        "accountID",
        "NUMERO_DE_CONTRATO",
        "SUB_TIPO_PRODUCTO",
        "TIPO_PRODUCTO",
        "empresa",
        "centro"
})
@Generated("jsonschema2pojo")
public class BODisponiblePartenonBean {
    @JsonProperty("_id")
    private String id;
    @JsonProperty("accountID")
    private String accountID;
    @JsonProperty("NUMERO_DE_CONTRATO")
    private String numeroDeContrato;
    @JsonProperty("SUB_TIPO_PRODUCTO")
    private String subTipoProducto;
    @JsonProperty("TIPO_PRODUCTO")
    private String tipoProducto;
    @JsonProperty("empresa")
    private String empresa;
    @JsonProperty("centro")
    private String centro;

    @JsonProperty("_id")
    public String getId() {
        return id;
    }

    @JsonProperty("_id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("accountID")
    public String getAccountID() {
        return accountID;
    }

    @JsonProperty("accountID")
    public void setAccountID(String accountID) {
        this.accountID = accountID;
    }

    @JsonProperty("NUMERO_DE_CONTRATO")
    public String getNumeroDeContrato() {
        return numeroDeContrato;
    }

    @JsonProperty("NUMERO_DE_CONTRATO")
    public void setNumeroDeContrato(String numeroDeContrato) {
        this.numeroDeContrato = numeroDeContrato;
    }

    @JsonProperty("SUB_TIPO_PRODUCTO")
    public String getSubTipoProducto() {
        return subTipoProducto;
    }

    @JsonProperty("SUB_TIPO_PRODUCTO")
    public void setSubTipoProducto(String subTipoProducto) {
        this.subTipoProducto = subTipoProducto;
    }

    @JsonProperty("TIPO_PRODUCTO")
    public String getTipoProducto() {
        return tipoProducto;
    }

    @JsonProperty("TIPO_PRODUCTO")
    public void setTipoProducto(String tipoProducto) {
        this.tipoProducto = tipoProducto;
    }

    @JsonProperty("empresa")
    public String getEmpresa() {
        return empresa;
    }

    @JsonProperty("empresa")
    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    @JsonProperty("centro")
    public String getCentro() {
        return centro;
    }

    @JsonProperty("centro")
    public void setCentro(String centro) {
        this.centro = centro;
    }

    /**
     * @return Key for matching contract wiht positions (accountID+tipoProducto+subTipoProducto)
     */
    public String getKey(){
        return this.accountID+tipoProducto+subTipoProducto+centro;
    }

    /**
     * @return The entire partenon contract identifier. ( empresa+centro+tipoProducto+numeroDeContrato+subTipoProducto - 21 charachters in total )
     */
    public String getPartenonContract(){
        return this.empresa+centro+tipoProducto+numeroDeContrato+subTipoProducto;
    }
}