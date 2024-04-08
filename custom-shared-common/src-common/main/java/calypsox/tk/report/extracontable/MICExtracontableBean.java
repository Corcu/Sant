package calypsox.tk.report.extracontable;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

/**
 * @author dmenendd
 * Represents one CalypsoToMIC file row i
 */
public class MICExtracontableBean {

    private MICExtracontableField<String> idCent = new MICExtracontableField<>(4);
    private MICExtracontableField<String> codIsin = new MICExtracontableField<>(12);
    private MICExtracontableField<String> emprContrato = new MICExtracontableField<>(4);
    private MICExtracontableField<String> codSector = new MICExtracontableField<>(3);
    private MICExtracontableField<String> indTipOper = new MICExtracontableField<>(1);
    private MICExtracontableField<String> indPertGrupo = new MICExtracontableField<>(9);
    private MICExtracontableField<String> monedaContravalor = new MICExtracontableField<>(3);
    private MICExtracontableField<String> numContrato = new MICExtracontableField<>(7);
    private MICExtracontableField<String> numOperacDGO = new MICExtracontableField<>(8);
    private MICExtracontableField<String> indCoberCont = new MICExtracontableField<>(3);
    private MICExtracontableField<String> codPaisEmisor = new MICExtracontableField<>(3);
    private MICExtracontableField<String> codProducto = new MICExtracontableField<>(3);
    private MICExtracontableField<String> indSubCa = new MICExtracontableField<>(3);
    private MICExtracontableField<String> monContr = new MICExtracontableField<>(3);
    private MICExtracontableField<String> codRefInGr = new MICExtracontableField<>(50);
    private MICExtracontableField<String> codGLSContrapar = new MICExtracontableField<>(11);
    private MICExtracontableField<String> codGLSEmisor = new MICExtracontableField<>(6);
    private MICExtracontableField<String> codGLSEntidad = new MICExtracontableField<>(11);
    private MICExtracontableField<String> codPaisContrapar = new MICExtracontableField<>(2);
    private MICExtracontableField<String> codContrapar = new MICExtracontableField<>(11);
    private MICExtracontableField<String> codEmisor = new MICExtracontableField<>(4);
    private MICExtracontableField<String> descCodContrapar = new MICExtracontableField<>(35);
    private MICExtracontableField<String> codCifEmi = new MICExtracontableField<>(10);

    //SettleDate MaturityDate
    private MICExtracontableField<JDate> fContrata = new MICExtracontableField<>(10);
    private MICExtracontableField<JDate> fVenci = new MICExtracontableField<>(10);

    private MICExtracontableField<String> secBancoEspContrapar = new MICExtracontableField<>(3);
    private MICExtracontableField<String> secBancoEspEmisor = new MICExtracontableField<>(3);
    private MICExtracontableField<Long> tipoInteres = new MICExtracontableField<>(15);
    private MICExtracontableField<String> codPortf = new MICExtracontableField<>(15);
    private MICExtracontableField<String> codTipoOpe3 = new MICExtracontableField<>(3);
    private MICExtracontableField<String> codEstrOpe = new MICExtracontableField<>(5);
    private MICExtracontableField<String> codTipoCobertura = new MICExtracontableField<>(15);
    private MICExtracontableField<String> codSentido = new MICExtracontableField<>(1);
    private MICExtracontableField<Double> impNominal = new MICExtracontableField<>(17,2);
    private MICExtracontableField<String> codJContrapar = new MICExtracontableField<>(11);
    private MICExtracontableField<String> codNumOpeFront = new MICExtracontableField<>(30);
    private MICExtracontableField<String> codNumOpeBack = new MICExtracontableField<>(30);
    private MICExtracontableField<String> codNumEventoBack = new MICExtracontableField<>(30);

    private MICExtracontableField<JDate> fValor = new MICExtracontableField<>(10);

    private MICExtracontableField<Long> impIntereses = new MICExtracontableField<>(17);
    private MICExtracontableField<Double> impPrincipal = new MICExtracontableField<>(17,2);

    private MICExtracontableField<JDate> fIniFij = new MICExtracontableField<>(10);
    private MICExtracontableField<JDate> fVenciFij = new MICExtracontableField<>(10);

    private MICExtracontableField<String> claseContable = new MICExtracontableField<>(1);
    private MICExtracontableField<String> tipOpcion = new MICExtracontableField<>(1);
    private MICExtracontableField<String> subyRF = new MICExtracontableField<>(10);
    private MICExtracontableField<String> indSubordi = new MICExtracontableField<>(1);
    private MICExtracontableField<String> indAnotCuenta = new MICExtracontableField<>(1);
    private MICExtracontableField<String> indDerivativeImp = new MICExtracontableField<>(1);
    private MICExtracontableField<String> indSegregation = new MICExtracontableField<>(3);

    private MICExtracontableField<String> tcRefInt = new MICExtracontableField<>(21);
    private MICExtracontableField<Integer> productId = new MICExtracontableField<>(16);


    private MICExtracontableField<String> accountingRule=new MICExtracontableField<>(22);
    private MICExtracontableField<String> agente=new MICExtracontableField<>(11);
    private MICExtracontableField<String> autoCartera=new MICExtracontableField<>(2);
    private MICExtracontableField<String> internal=new MICExtracontableField<>(1);
    private MICExtracontableField<String> suv=new MICExtracontableField<>(10);
    private MICExtracontableField<String> direction=new MICExtracontableField<>(5);
    private MICExtracontableField<String> equityType=new MICExtracontableField<>(14);

    private MICExtracontableField<String> underlyingType=new MICExtracontableField<>(2);
    private MICExtracontableField<String> contractType=new MICExtracontableField<>(10);
    private MICExtracontableField<String> cdreopin =new MICExtracontableField<>(30);
    private MICExtracontableField<String> empty =new MICExtracontableField<>(93);

    private MICExtracontableField<Integer> accountId = new MICExtracontableField<>(16);
    private MICExtracontableField<Integer> cdnuopba = new MICExtracontableField<>(30);

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        Field[] fields = this.getClass().getDeclaredFields();
        Arrays.stream(fields).map(this::printFIFlowFieldValue).forEach(builder::append);
        return builder.toString();
    }

    private String printFIFlowFieldValue(Object field) {
        String res = "";
        if (field instanceof Field && ((Field) field).getType().isAssignableFrom(MICExtracontableField.class)) {
            try {
                Optional<MICExtracontableField> fiFlowFieldOpt = Optional.ofNullable((MICExtracontableField) ((Field) field).get(this));
                res = fiFlowFieldOpt.map(MICExtracontableField::getContent).orElse("");
            } catch (ClassCastException | IllegalAccessException exc) {
                Log.error(this, "Exception while printing CalypsoToMIC row", exc.getCause());
            }
        }
        return res;
    }
    //GETTERS SETTERS

    public MICExtracontableField<String> getEmpty() { return empty; }

    public MICExtracontableField<String> getCdreopin() { return cdreopin; }

    public void setCdreopin(String cdreopin) { this.cdreopin.setContent(cdreopin); }

    public MICExtracontableField<String> getIdCent() { return idCent; }

    public void setIdCent(String idCent) { this.idCent.setContent(idCent); }

    public MICExtracontableField<String> getCodIsin() { return codIsin; }

    public void setCodIsin(String codIsin) { this.codIsin.setContent(codIsin); }

    public MICExtracontableField<String> getEmprContrato() { return emprContrato; }

    public void setEmprContrato(String emprContrato) { this.emprContrato.setContent(emprContrato); }

    public MICExtracontableField<String> getCodSector() { return codSector; }

    public void setCodSector(String codSector) { this.codSector.setContent(codSector); }

    public MICExtracontableField<String> getIndTipOper() { return indTipOper; }

    public void setIndTipOper(String indTipOper) { this.indTipOper.setContent(indTipOper); }

    public MICExtracontableField<String> getIndPertGrupo() { return indPertGrupo; }

    public void setIndPertGrupo(String indPertGrupo) { this.indPertGrupo.setContent(indPertGrupo); }

    public MICExtracontableField<String> getMonedaContravalor() { return monedaContravalor; }

    public void setMonedaContravalor(String monedaContravalor) { this.monedaContravalor.setContent(monedaContravalor); }

    public MICExtracontableField<String> getNumContrato() { return numContrato; }

    public void setNumContrato(String numContrato) { this.numContrato.setContent(numContrato); }

    public MICExtracontableField<String> getNumOperacDGO() { return numOperacDGO; }

    public void setNumOperacDGO(String numOperacDGO) { this.numOperacDGO.setContent(numOperacDGO); }

    public MICExtracontableField<String> getIndCoberCont() { return indCoberCont; }

    public void setIndCoberCont(String indCoberCont) { this.indCoberCont.setContent(indCoberCont); }

    public MICExtracontableField<String> getCodPaisEmisor() { return codPaisEmisor; }

    public void setCodPaisEmisor(String codPaisEmisor) { this.codPaisEmisor.setContent(codPaisEmisor); }

    public MICExtracontableField<String> getCodProducto() { return codProducto; }

    public void setCodProducto(String codProducto) { this.codProducto.setContent(codProducto); }

    public MICExtracontableField<String> getIndSubCa() { return indSubCa; }

    public void setIndSubCa(String indSubCa) { this.indSubCa.setContent(indSubCa); }

    public MICExtracontableField<String> getMonContr() { return monContr; }

    public void setMonContr(String monContr) { this.monContr.setContent(monContr); }

    public MICExtracontableField<String> getCodRefInGr() { return codRefInGr; }

    public void setCodRefInGr(String codRefInGr) { this.codRefInGr.setContent(codRefInGr); }

    public MICExtracontableField<String> getCodGLSContrapar() { return codGLSContrapar; }

    public void setCodGLSContrapar(String codGLSContrapar) { this.codGLSContrapar.setContent(codGLSContrapar); }

    public MICExtracontableField<String> getCodGLSEmisor() { return codGLSEmisor; }

    public void setCodGLSEmisor(String codGLSEmisor) { this.codGLSEmisor.setContent(codGLSEmisor); }

    public MICExtracontableField<String> getCodGLSEntidad() { return codGLSEntidad; }

    public void setCodGLSEntidad(String codGLSEntidad) { this.codGLSEntidad.setContent(codGLSEntidad); }

    public MICExtracontableField<String> getCodPaisContrapar() { return codPaisContrapar; }

    public void setCodPaisContrapar(String codPaisContrapar) { this.codPaisContrapar.setContent(codPaisContrapar); }

    public MICExtracontableField<String> getCodContrapar() { return codContrapar; }

    public void setCodContrapar(String codContrapar) { this.codContrapar.setContent(codContrapar); }

    public MICExtracontableField<String> getCodEmisor() { return codEmisor; }

    public void setCodEmisor(String codEmisor) { this.codEmisor.setContent(codEmisor); }

    public MICExtracontableField<String> getDescCodContrapar() { return descCodContrapar; }

    public void setDescCodContrapar(String descCodContrapar) { this.descCodContrapar.setContent(descCodContrapar); }

    public MICExtracontableField<String> getCodCifEmi() { return codCifEmi; }

    public void setCodCifEmi(String codCifEmi) { this.codCifEmi.setContent(codCifEmi); }

    public MICExtracontableField<JDate> getFContrata() { return fContrata; }

    public void setFContrata(JDate fContrata) { this.fContrata.setContent(fContrata); }

    public MICExtracontableField<JDate> getFVenci() { return fVenci; }

    public void setFVenci(JDate fVenci) { this.fVenci.setContent(fVenci); }

    public MICExtracontableField<String> getSecBancoEspContrapar() { return secBancoEspContrapar; }

    public void setSecBancoEspContrapar(String secBancoEspContrapar) { this.secBancoEspContrapar.setContent(secBancoEspContrapar); }

    public MICExtracontableField<String> getSecBancoEspEmisor() { return secBancoEspEmisor; }

    public void setSecBancoEspEmisor(String secBancoEspEmisor) { this.secBancoEspEmisor.setContent(secBancoEspEmisor); }

    public MICExtracontableField<Long> getTipoInteres() { return tipoInteres; }

    public void setTipoInteres(Long tipoInteres) { this.tipoInteres.setContent(tipoInteres); }

    public MICExtracontableField<String> getCodPortf() { return codPortf; }

    public void setCodPortf(String codPortf) { this.codPortf.setContent(codPortf); }

    public MICExtracontableField<String> getCodTipoOpe3() { return codTipoOpe3; }

    public void setCodTipoOpe3(String codTipoOpe3) { this.codTipoOpe3.setContent(codTipoOpe3); }

    public MICExtracontableField<String> getCodEstrOpe() { return codEstrOpe; }

    public void setCodEstrOpe(String codEstrOpe) { this.codEstrOpe.setContent(codEstrOpe); }

    public MICExtracontableField<String> getCodTipoCobertura() { return codTipoCobertura; }

    public void setCodTipoCobertura(String codTipoCobertura) { this.codTipoCobertura.setContent(codTipoCobertura); }

    public MICExtracontableField<String> getCodSentido() { return codSentido; }

    public void setCodSentido(String codSentido) { this.codSentido.setContent(codSentido); }

    public MICExtracontableField<Double> getImpNominal() { return impNominal; }

    public void setImpNominal(Double impNominal) { this.impNominal.setContent(impNominal); }

    public MICExtracontableField<String> getCodJContrapar() { return codJContrapar; }

    public void setCodJContrapar(String codJContrapar) { this.codJContrapar.setContent(codJContrapar); }

    public MICExtracontableField<String> getCodNumOpeFront() { return codNumOpeFront; }

    public void setCodNumOpeFront(String codNumOpeFront) { this.codNumOpeFront.setContent(codNumOpeFront); }

    public MICExtracontableField<String> getCodNumOpeBack() { return codNumOpeBack; }

    public void setCodNumOpeBack(String codNumOpeBack) { this.codNumOpeBack.setContent(codNumOpeBack); }

    public MICExtracontableField<String> getCodNumEventoBack() { return codNumEventoBack; }

    public void setCodNumEventoBack(String codNumEventoBack) { this.codNumEventoBack.setContent(codNumEventoBack); }

    public MICExtracontableField<JDate> getFvalor() { return fValor; }

    public void setFvalor(JDate fValor) { this.fValor.setContent(fValor); }

    public MICExtracontableField<Long> getImpIntereses() { return impIntereses; }

    public void setImpIntereses(Long impIntereses) { this.impIntereses.setContent(impIntereses); }

    public MICExtracontableField<Double> getImpPrincipal() { return impPrincipal; }

    public void setImpPrincipal(Double impPrincipal) { this.impPrincipal.setContent(impPrincipal); }

    public MICExtracontableField<JDate> getFIniFij() { return fIniFij; }

    public void setFIniFij(JDate fIniFij) { this.fIniFij.setContent(fIniFij); }

    public MICExtracontableField<JDate> getFVenciFij() { return fVenciFij; }

    public void setFVenciFij(JDate fVenciFij) { this.fVenciFij.setContent(fVenciFij); }

    public MICExtracontableField<String> getClaseContable() {
        return claseContable;
    }

    public void setClaseContable(String claseContable) {
        this.claseContable.setContent(claseContable);
    }

    public MICExtracontableField<String> getTipOpcion() {
        return tipOpcion;
    }

    public void setTipOpcion(String tipOpcion) {
        this.tipOpcion.setContent(tipOpcion);
    }

    public MICExtracontableField<String> getSubyRF() {
        return subyRF;
    }

    public void setSubyRF(String subyRF) {
        this.subyRF.setContent(subyRF);
    }

    public MICExtracontableField<String> getIndSubordi() {
        return indSubordi;
    }

    public void setIndSubordi(String indSubordi) {
        this.indSubordi.setContent(indSubordi);
    }

    public MICExtracontableField<String> getIndAnotCuenta() {
        return indAnotCuenta;
    }

    public void setIndAnotCuenta(String indAnotCuenta) {
        this.indAnotCuenta.setContent(indAnotCuenta);
    }

    public MICExtracontableField<String> getIndDerivativeImp() {
        return indDerivativeImp;
    }

    public void setIndDerivativeImp(String indDerivativeImp) {
        this.indDerivativeImp.setContent(indDerivativeImp);
    }

    public MICExtracontableField<String> getIndSegregation() {
        return indSegregation;
    }

    public void setIndSegregation(String indSegregation) {
        this.indSegregation.setContent(indSegregation);
    }

    public MICExtracontableField<String> getTcRefInt() {
        return tcRefInt;
    }

    public void setTcRefInt(String tcRefInt) {
        this.tcRefInt.setContent(tcRefInt);
    }

    public MICExtracontableField<String> getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType.setContent(contractType);
    }

    public MICExtracontableField<String> getUnderlyingType() {
        return underlyingType;
    }
    public void setUnderlyingType(String underlyingType) {
        this.underlyingType.setContent(underlyingType);
    }

    public MICExtracontableField<String> getAccountingRule() {
        return accountingRule;
    }

    public void setAccountingRule(String accountingRule) {
        this.accountingRule.setContent(accountingRule);
    }

    public MICExtracontableField<String> getAutoCartera() {
        return autoCartera;
    }

    public void setAutoCartera(String autoCartera) {
        this.autoCartera.setContent(autoCartera);
    }

    public MICExtracontableField<Integer> getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId.setContent(productId);
    }

    public MICExtracontableField<String> getInternal() {
        return internal;
    }

    public void setInternal(String internal) {
        this.internal.setContent(internal);
    }

    public MICExtracontableField<String> getSuv() {
        return suv;
    }

    public void setSuv(String suv) {
        this.suv.setContent(suv);
    }

    public MICExtracontableField<String> getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction.setContent(direction);
    }

    public MICExtracontableField<String> getEquityType() {
        return equityType;
    }

    public void setEquityType(String equityType) {
        this.equityType.setContent(equityType);
    }

    public MICExtracontableField<String> getAgente() {
        return agente;
    }

    public void setAgente(String agente) {
        this.agente.setContent(agente);
    }

    public MICExtracontableField<Integer> getAccountId() {return this.accountId; }

    public void setAccountId(int accountId) {this.accountId.setContent(accountId); }

    public MICExtracontableField<Integer> getCdnuopba() {return this.cdnuopba; }

    public void setCdnuopba(int cdnuopba) {this.cdnuopba.setContent(cdnuopba); }
}
