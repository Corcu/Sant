package com.isban.api.webservice;

import java.time.LocalDate;
import java.util.Date;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
/** 
 * Example of Bean class to be used in WS. It is not used.
 * @author x660030
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class UnsettleRow {
	private String comentario;
	private String producto;
	private String instrumento;
	private String folder;
	private String trader;
	private String referenciaFO;
	private String referenciaBO;
	private String operacion;
	private Date fContra;
	private LocalDate fValor;
	private String isin;
	private String descripcion;
	private String contrapartida;
	private String tipo;
	private double nominal;
	private String divisa;
	private double efectivo;
	private String gls;
	private String custodio;
	private String estadoActual;
	private String clase;
	private String causa;
	private String setr;
	private String trad;
	private String clea;
	private String placeofSettlement;
	private String recompra;
	private String referenciacontratacion;
	private String camaraLiquidacion;
	private double posicionDisponible;
	private String deadline;
	private String daysToISD;
	private String potPtyISD;
	private String potPtySize;
	private String ptyAggregate;
	private String potPtyBuyIn;
	private String potPtyExtPeriod;
	private String potPtyDefPeriod;
	private String fallida;
	private String team;
	private String subTeam;
	private String pataDeIdaVuelta;
	private LocalDate buyInDate;
	private String daysToBuyIn;

	public String getComentario() {
		return comentario;
	}

	public void setComentario(String comentario) {
		this.comentario = comentario;
	}

	public String getProducto() {
		return producto;
	}

	public void setProducto(String producto) {
		this.producto = producto;
	}

	public String getInstrumento() {
		return instrumento;
	}

	public void setInstrumento(String instrumento) {
		this.instrumento = instrumento;
	}

	public String getFolder() {
		return folder;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}

	public String getTrader() {
		return trader;
	}

	public void setTrader(String trader) {
		this.trader = trader;
	}

	public String getReferenciaFO() {
		return referenciaFO;
	}

	public void setReferenciaFO(String referenciaFO) {
		this.referenciaFO = referenciaFO;
	}

	public String getReferenciaBO() {
		return referenciaBO;
	}

	public void setReferenciaBO(String referenciaBO) {
		this.referenciaBO = referenciaBO;
	}

	public String getOperacion() {
		return operacion;
	}

	public void setOperacion(String operacion) {
		this.operacion = operacion;
	}

	public Date getfContra() {
		return fContra;
	}

	public void setfContra(Date fContra) {
		this.fContra = fContra;
	}

	public LocalDate getfValor() {
		return fValor;
	}

	public void setfValor(LocalDate fValor) {
		this.fValor = fValor;
	}

	public String getIsin() {
		return isin;
	}

	public void setIsin(String isin) {
		this.isin = isin;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	public String getContrapartida() {
		return contrapartida;
	}

	public void setContrapartida(String contrapartida) {
		this.contrapartida = contrapartida;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public double getNominal() {
		return nominal;
	}

	public void setNominal(double nominal) {
		this.nominal = nominal;
	}

	public String getDivisa() {
		return divisa;
	}

	public void setDivisa(String divisa) {
		this.divisa = divisa;
	}

	public double getEfectivo() {
		return efectivo;
	}

	public void setEfectivo(double efectivo) {
		this.efectivo = efectivo;
	}

	public String getGls() {
		return gls;
	}

	public void setGls(String gls) {
		this.gls = gls;
	}

	public String getCustodio() {
		return custodio;
	}

	public void setCustodio(String custodio) {
		this.custodio = custodio;
	}

	public String getEstadoActual() {
		return estadoActual;
	}

	public void setEstadoActual(String estadoActual) {
		this.estadoActual = estadoActual;
	}

	public String getClase() {
		return clase;
	}

	public void setClase(String clase) {
		this.clase = clase;
	}

	public String getCausa() {
		return causa;
	}

	public void setCausa(String causa) {
		this.causa = causa;
	}

	public String getSetr() {
		return setr;
	}

	public void setSetr(String setr) {
		this.setr = setr;
	}

	public String getTrad() {
		return trad;
	}

	public void setTrad(String trad) {
		this.trad = trad;
	}

	public String getClea() {
		return clea;
	}

	public void setClea(String clea) {
		this.clea = clea;
	}

	public String getPlaceofSettlement() {
		return placeofSettlement;
	}

	public void setPlaceofSettlement(String placeofSettlement) {
		this.placeofSettlement = placeofSettlement;
	}

	public String getRecompra() {
		return recompra;
	}

	public void setRecompra(String recompra) {
		this.recompra = recompra;
	}

	public String getReferenciacontratacion() {
		return referenciacontratacion;
	}

	public void setReferenciacontratacion(String referenciacontratacion) {
		this.referenciacontratacion = referenciacontratacion;
	}

	public String getCamaraLiquidacion() {
		return camaraLiquidacion;
	}

	public void setCamaraLiquidacion(String camaraLiquidacion) {
		this.camaraLiquidacion = camaraLiquidacion;
	}

	public double getPosicionDisponible() {
		return posicionDisponible;
	}

	public void setPosicionDisponible(double posicionDisponible) {
		this.posicionDisponible = posicionDisponible;
	}

	public String getDeadline() {
		return deadline;
	}

	public void setDeadline(String deadline) {
		this.deadline = deadline;
	}

	public String getDaysToISD() {
		return daysToISD;
	}

	public void setDaysToISD(String daysToISD) {
		this.daysToISD = daysToISD;
	}

	public String getPotPtyISD() {
		return potPtyISD;
	}

	public void setPotPtyISD(String potPtyISD) {
		this.potPtyISD = potPtyISD;
	}

	public String getPotPtySize() {
		return potPtySize;
	}

	public void setPotPtySize(String potPtySize) {
		this.potPtySize = potPtySize;
	}

	public String getPtyAggregate() {
		return ptyAggregate;
	}

	public void setPtyAggregate(String ptyAggregate) {
		this.ptyAggregate = ptyAggregate;
	}

	public String getPotPtyBuyIn() {
		return potPtyBuyIn;
	}

	public void setPotPtyBuyIn(String potPtyBuyIn) {
		this.potPtyBuyIn = potPtyBuyIn;
	}

	public String getPotPtyExtPeriod() {
		return potPtyExtPeriod;
	}

	public void setPotPtyExtPeriod(String potPtyExtPeriod) {
		this.potPtyExtPeriod = potPtyExtPeriod;
	}

	public String getPotPtyDefPeriod() {
		return potPtyDefPeriod;
	}

	public void setPotPtyDefPeriod(String potPtyDefPeriod) {
		this.potPtyDefPeriod = potPtyDefPeriod;
	}

	public String getFallida() {
		return fallida;
	}

	public void setFallida(String fallida) {
		this.fallida = fallida;
	}

	public String getTeam() {
		return team;
	}

	public void setTeam(String team) {
		this.team = team;
	}

	public String getSubTeam() {
		return subTeam;
	}

	public void setSubTeam(String subTeam) {
		this.subTeam = subTeam;
	}

	public String getPataDeIdaVuelta() {
		return pataDeIdaVuelta;
	}

	public void setPataDeIdaVuelta(String pataDeIdaVuelta) {
		this.pataDeIdaVuelta = pataDeIdaVuelta;
	}

	public LocalDate getBuyInDate() {
		return buyInDate;
	}

	public void setBuyInDate(LocalDate buyInDate) {
		this.buyInDate = buyInDate;
	}

	public String getDaysToBuyIn() {
		return daysToBuyIn;
	}

	public void setDaysToBuyIn(String daysToBuyIn) {
		this.daysToBuyIn = daysToBuyIn;
	}

}
