package calypsox.tk.util.bean;

import java.io.Serializable;
import java.util.Date;

public class ExternalBalanzaBean implements Serializable {

    private static final long serialVersionUID = -8877734431825022496L;

    private Date periodo;

    public String getPeriodoStr() {
        return periodoStr;
    }

    public void setPeriodoStr(String periodoStr) {
        this.periodoStr = periodoStr;
    }

    private String periodoStr;

    private String instrumento;
    private String isin;
    private String nombre_isin;
    private String nif_emmisor;
    private String epigrafe;
    private Double si_nominal;
    private Double si_valoracion;
    private Double trespaso_entrada;
    private Double trespaso_salida;
    private Double entrada_nominal;
    private Double entrada_valoracion;
    private Double salida_nominal;
    private Double salida_valoracion;
    private Double cupon_nominal;
    private Double cupon_valoracion;
    private Double sf_nominal;
    private Double sf_valoracion;

    public Date getPeriodo() {
        return periodo;
    }

    public void setPeriodo(Date periodo) {
        this.periodo = periodo;
    }

    public String getInstrumento() {
        return instrumento;
    }

    public void setInstrumento(int instrumento) {
        this.instrumento = String.valueOf(instrumento);
    }

    public String getIsin() {
        return isin;
    }

    public void setIsin(String isin) {
        this.isin = isin;
    }

    public String getNombre_isin() {
        return nombre_isin;
    }

    public void setNombre_isin(String nombre_isin) {
        this.nombre_isin = nombre_isin;
    }

    public String getEpigrafe() {
        return epigrafe;
    }

    public void setEpigrafe(String epigrafe) {
        this.epigrafe = epigrafe;
    }

    public Double getSi_nominal() {
        return si_nominal;
    }

    public void setSi_nominal(Double si_nominal) {
        this.si_nominal = si_nominal;
    }

    public Double getSi_valoracion() {
        return si_valoracion;
    }

    public void setSi_valoracion(Double si_valoracion) {
        this.si_valoracion = si_valoracion;
    }

    public Double getTrespaso_entrada() {
        return trespaso_entrada;
    }

    public void setTrespaso_entrada(Double trespaso_entrada) {
        this.trespaso_entrada = trespaso_entrada;
    }

    public Double getTrespaso_salida() {
        return trespaso_salida;
    }

    public void setTrespaso_salida(Double trespaso_salida) {
        this.trespaso_salida = trespaso_salida;
    }

    public Double getEntrada_nominal() {
        return entrada_nominal;
    }

    public void setEntrada_nominal(Double entrada_nominal) {
        this.entrada_nominal = entrada_nominal;
    }

    public Double getEntrada_valoracion() {
        return entrada_valoracion;
    }

    public void setEntrada_valoracion(Double entrada_valoracion) {
        this.entrada_valoracion = entrada_valoracion;
    }

    public Double getSalida_nominal() {
        return salida_nominal;
    }

    public void setSalida_nominal(Double salida_nominal) {
        this.salida_nominal = salida_nominal;
    }

    public Double getSalida_valoracion() {
        return salida_valoracion;
    }

    public void setSalida_valoracion(Double salida_valoracion) {
        this.salida_valoracion = salida_valoracion;
    }

    public Double getCupon_nominal() {
        return cupon_nominal;
    }

    public void setCupon_nominal(Double cupon_nominal) {
        this.cupon_nominal = cupon_nominal;
    }

    public Double getCupon_valoracion() {
        return cupon_valoracion;
    }

    public void setCupon_valoracion(Double cupon_valoracion) {
        this.cupon_valoracion = cupon_valoracion;
    }

    public Double getSf_nominal() {
        return sf_nominal;
    }

    public void setSf_nominal(Double sf_nominal) {
        this.sf_nominal = sf_nominal;
    }

    public Double getSf_valoracion() {
        return sf_valoracion;
    }

    public void setSf_valoracion(Double sf_valoracion) {
        this.sf_valoracion = sf_valoracion;
    }

    public String getNif_emmisor() { return nif_emmisor;}

    public void setNif_emmisor(String nif_emmisor) {
        this.nif_emmisor = nif_emmisor;
    }

}
