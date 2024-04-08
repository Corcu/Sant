package calypsox.tk.bo.boi;

import com.calypso.tk.core.Trade;

/**
 * @author dmenendd
 * Represents one BOIDiraio file row
 */
public class BOIDiarioBean {

    Trade trade;
    private String fecProceso;
    private String refIntragrupo = "";
    private String sistemaOrigen = "";
    private String codProducto = "";
    private String codInstrumento = "";
    private String codPortfolio = "";
    private String codOperacion = "";
    private String codEstructura = "";
    private String tipoEstructura = "";
    private String codGLCS = "";
    private String codOperacionNego = "";
    private String codDivisa  = "";
    private String codDireccion = "";
    private String codEstrategia = "";
    private String fecCaptura = "";
    private String fecContratacion = "";
    private String fecValor = "";
    private String fecVencimiento = "";
    private String principalOpe = "";
    private String principalVigor = "";
    private String referencia = "";
    private String Spread  = "";
    private String tasaInteres  = "";
    private String fecInicio = "";
    private String fecFin = "";
    private String baseCalculo = "";
    private String indCallPut = "";
    private String tipOpcion1 = "";
    private String tipOpcion2 = "";
    private String prima = "";
    private String divisaPrima = "";
    private String strike = "";
    private String saldoCajasOpe = "";
    private String saldoDevengosOpe = "";
    private String saldoMercadoOpe = "";
    private String saldoContableOpe = "";
    private String saldoCajasLocal = "";
    private String saldoDevengosLocal = "";
    private String saldoMercadoLocal = "";
    private String saldoContableLocal = "";

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();

        builder.append(fecProceso);
        builder.append(refIntragrupo);
        builder.append(sistemaOrigen);
        builder.append(codProducto);
        builder.append(codInstrumento);
        builder.append(codPortfolio);
        builder.append(codOperacion);
        builder.append(codEstructura);
        builder.append(tipoEstructura);
        builder.append(codGLCS);
        builder.append(codOperacionNego);
        builder.append(codDivisa );
        builder.append(codDireccion);
        builder.append(codEstrategia);
        builder.append(fecCaptura);
        builder.append(fecContratacion);
        builder.append(fecValor);
        builder.append(fecVencimiento);
        builder.append(principalOpe);
        builder.append(principalVigor);
        builder.append(referencia);
        builder.append(Spread );
        builder.append(tasaInteres);
        builder.append(fecInicio);
        builder.append(fecFin);
        builder.append(baseCalculo);
        builder.append(indCallPut);
        builder.append(tipOpcion1);
        builder.append(tipOpcion2);
        builder.append(prima);
        builder.append(divisaPrima);
        builder.append(strike);
        builder.append(saldoCajasOpe);
        builder.append(saldoDevengosOpe);
        builder.append(saldoMercadoOpe);
        builder.append(saldoContableOpe);
        builder.append(saldoCajasLocal);
        builder.append(saldoDevengosLocal);
        builder.append(saldoMercadoLocal);
        builder.append(saldoContableLocal);

        return builder.toString();
    }

    //GETTERS SETTERS

    public Trade getTrade() {
        return trade;
    }

    public void setTrade(Trade trade) { this.trade = trade;    }

    public String getFecProceso() { return fecProceso; }

    public void setFecProceso(String fecProceso) { this.fecProceso = fecProceso; }

    public String getRefIntragrupo() { return refIntragrupo; }

    public void setRefIntragrupo(String refIntragrupo) { this.refIntragrupo = refIntragrupo; }

    public String getSistemaOrigen() { return sistemaOrigen; }

    public void setSistemaOrigen(String sistemaOrigen) { this.sistemaOrigen = sistemaOrigen; }

    public String getCodProducto() { return codProducto; }

    public void setCodProducto(String codProducto) { this.codProducto = codProducto; }

    public String getCodInstrumento() { return codInstrumento; }

    public void setCodInstrumento(String codInstrumento) { this.codInstrumento = codInstrumento; }

    public String getCodPortfolio() { return codPortfolio; }

    public void setCodPortfolio(String codPortfolio) { this.codPortfolio = codPortfolio; }

    public String getCodOperacion() { return codOperacion; }

    public void setCodOperacion(String codOperacion) { this.codOperacion = codOperacion; }

    public String getcodEstructura() { return codEstructura; }

    public void setcodEstructura(String codEstructura) { this.codEstructura = codEstructura; }

    public String getTipoEstructura() { return tipoEstructura; }

    public void setTipoEstructura(String tipoEstructura) { this.tipoEstructura = tipoEstructura; }

    public String getCodGLCS() { return codGLCS; }

    public void setCodGLCS(String codGLCS) { this.codGLCS = codGLCS; }

    public String getCodOperacionNego() { return codOperacionNego; }

    public void setCodOperacionNego(String codOperacionNego) { this.codOperacionNego = codOperacionNego; }

    public String getCodDivisa() { return codDivisa ; }

    public void setCodDivisa(String codDivisa ) { this.codDivisa  = codDivisa ; }

    public String getCodDireccion() { return codDireccion; }

    public void setCodDireccion(String codDireccion) { this.codDireccion = codDireccion; }

    public String getCodEstrategia() { return codEstrategia; }

    public void setCodEstrategia(String codEstrategia) { this.codEstrategia = codEstrategia; }

    public String getfecCaptura() { return fecCaptura; }

    public void setfecCaptura(String fecCaptura) { this.fecCaptura = fecCaptura; }

    public String getfecContratacion() { return fecContratacion; }

    public void setfecContratacion(String fecContratacion) { this.fecContratacion = fecContratacion; }

    public String getfecValor() { return fecValor; }

    public void setfecValor(String fecValor) { this.fecValor = fecValor; }

    public String getfecVencimiento() { return fecVencimiento; }

    public void setfecVencimiento(String fecVencimiento) { this.fecVencimiento = fecVencimiento; }

    public String getPrincipalOpe() { return principalOpe; }

    public void setPrincipalOpe(String principalOpe) { this.principalOpe = principalOpe; }

    public String getPrincipalVigor() { return principalVigor; }

    public void setPrincipalVigor(String principalVigor) { this.principalVigor = principalVigor; }

    public String getReferencia() { return referencia; }

    public void setReferencia(String referencia) { this.referencia = referencia; }

    public String getSpread() { return Spread ; }

    public void setSpread(String Spread ) { this.Spread  = Spread ; }

    public String getTasaInteres() { return tasaInteres; }

    public void setTasaInteres(String tasaInteres) { this.tasaInteres = tasaInteres; }

    public String getFecInicio() { return fecInicio; }

    public void setFecInicio(String fecInicio) { this.fecInicio = fecInicio; }

    public String getFecFin() { return fecFin; }

    public void setFecFin(String fecFin) { this.fecFin = fecFin; }

    public String getBaseCalculo() { return baseCalculo; }

    public void setBaseCalculo(String baseCalculo) { this.baseCalculo = baseCalculo; }

    public String getIndCallPut() { return indCallPut; }

    public void setIndCallPut(String indCallPut) { this.indCallPut = indCallPut; }

    public String gettipOpcion1() { return tipOpcion1; }

    public void settipOpcion1(String tipOpcion1) { this.tipOpcion1 = tipOpcion1; }

    public String gettipOpcion2() { return tipOpcion2; }

    public void settipOpcion2(String tipOpcion2) { this.tipOpcion2 = tipOpcion2; }

    public String getPrima() { return prima; }

    public void setPrima(String prima) { this.prima = prima; }

    public String getDivisaPrima() { return divisaPrima; }

    public void setDivisaPrima(String divisaPrima) { this.divisaPrima = divisaPrima; }

    public String getStrike() { return strike; }

    public void setStrike(String strike) { this.strike = strike; }

    public String getSaldoCajasOpe() { return saldoCajasOpe; }

    public void setSaldoCajasOpe(String saldoCajasOpe) { this.saldoCajasOpe = saldoCajasOpe; }

    public String getSaldoDevengosOpe() { return saldoDevengosOpe; }

    public void setSaldoDevengosOpe(String saldoDevengosOpe) { this.saldoDevengosOpe = saldoDevengosOpe; }

    public String getSaldoMercadoOpe() { return saldoMercadoOpe; }

    public void setSaldoMercadoOpe(String saldoMercadoOpe) { this.saldoMercadoOpe = saldoMercadoOpe; }

    public String getSaldoContableOpe() { return saldoContableOpe; }

    public void setSaldoContableOpe(String saldoContableOpe) { this.saldoContableOpe = saldoContableOpe; }

    public String getSaldoCajasLocal() { return saldoCajasLocal; }

    public void setSaldoCajasLocal(String saldoCajasLocal) { this.saldoCajasLocal = saldoCajasLocal; }

    public String getSaldoDevengosLocal() { return saldoDevengosLocal; }

    public void setSaldoDevengosLocal(String saldoDevengosLocal) { this.saldoDevengosLocal = saldoDevengosLocal; }

    public String getSaldoMercadoLocal() { return saldoMercadoLocal; }

    public void setSaldoMercadoLocal(String saldoMercadoLocal) { this.saldoMercadoLocal = saldoMercadoLocal; }

    public String getSaldoContableLocal() { return saldoContableLocal; }

    public void setSaldoContableLocal(String saldoContableLocal) { this.saldoContableLocal = saldoContableLocal; }

}
