package calypsox.tk.bo.fiflow.model;

import calypsox.tk.bo.fiflow.model.jaxb.FIFlowField;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

/**
 * @author aalonsop
 * Represents one CalypsoToTCyCC file row
 */
public class CalypsoToTCyCCBean {

    //Identifiers
    private FIFlowField<String> idEmpr = new FIFlowField<>(4);
    private FIFlowField<String> idCent = new FIFlowField<>(4);
    private FIFlowField<String> cdstrOpe = new FIFlowField<>(2);
    private FIFlowField<String> cdPortfo = new FIFlowField<>(15);
    private FIFlowField<String> ccreFer = new FIFlowField<>(20);
    private FIFlowField<String> codProd = new FIFlowField<>(3);
    private FIFlowField<String> codsProd = new FIFlowField<>(3);

    //SettleDate
    private FIFlowField<JDate> fLiqOpe = new FIFlowField<>(10);

    //Partenon id
    private FIFlowField<String> cdoPerbo = new FIFlowField<>(7);


    //Cover
    private FIFlowField<String> indCober = new FIFlowField<>(1);
    private FIFlowField<String> cdCobert = new FIFlowField<>(1);
    private FIFlowField<String> clCobert = new FIFlowField<>(1);
    private FIFlowField<String> idCobert = new FIFlowField<>(30);

    //Currency
    private FIFlowField<String> coDivisa = new FIFlowField<>(3);
    private FIFlowField<String> codDivlq = new FIFlowField<>(3);

    //Prices
    private FIFlowField<Double> imprLimp = new FIFlowField<>(9, 6);
    private FIFlowField<Double> imprSuci = new FIFlowField<>(9, 6);

    private FIFlowField<String> idSentOp = new FIFlowField<>(1);

    //Amounts
    private FIFlowField<Long> nTtituloo = new FIFlowField<>(15);
    private FIFlowField<Double> iPrinOpe = new FIFlowField<>(17, 4);//17 y 4 decimales
    private FIFlowField<Double> imcpCorr = new FIFlowField<>(17, 4);
    private FIFlowField<Double> imEfeOpe = new FIFlowField<>(17, 4);


    private FIFlowField<String> inOpinex = new FIFlowField<>(1);

    //Dates aaaa-mm-dd
    private FIFlowField<JDate> fConOper = new FIFlowField<>(10);
    private FIFlowField<String> hConOper = new FIFlowField<>(6);

    private FIFlowField<String> cestoPbo = new FIFlowField<>(1);

    //Cods persona cpty
    private FIFlowField<String> tiPerson = new FIFlowField<>(1);
    private FIFlowField<Integer> cdPerson = new FIFlowField<>(9);

    private FIFlowField<String> cdProdux = new FIFlowField<>(1);

    //MtM
    private FIFlowField<Integer> marToMar = new FIFlowField<>(17, 4);

    //Bond
    private FIFlowField<String> cdIroi = new FIFlowField<>(40);
    private FIFlowField<String> cdRig = new FIFlowField<>(50);
    private FIFlowField<String> idStrip = new FIFlowField<>(50);

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        Field[] fields = this.getClass().getDeclaredFields();
        Arrays.stream(fields).map(this::printFIFlowFieldValue).forEach(builder::append);
        return builder.toString();
    }

    private String printFIFlowFieldValue(Object field) {
        String res = "";
        if (field instanceof Field && ((Field) field).getType().isAssignableFrom(FIFlowField.class)) {
            try {
                Optional<FIFlowField> fiFlowFieldOpt = Optional.ofNullable((FIFlowField) ((Field) field).get(this));
                res = fiFlowFieldOpt.map(FIFlowField::getContent).orElse("");
                System.out.println(((Field) field).getName() + " printed " + res);
            } catch (ClassCastException | IllegalAccessException exc) {
                Log.error(this, "Exception while printing CalypsoToTCCyCC row", exc.getCause());
            }
        }
        return res;
    }
    //GETTERS SETTERS

    public FIFlowField<String> getIdEmpr() {
        return idEmpr;
    }

    public void setIdEmpr(String idEmpr) {
        this.idEmpr.setContent(idEmpr);
    }

    public FIFlowField<String> getIdCent() {
        return idCent;
    }

    public void setIdCent(String idCent) {
        this.idCent.setContent(idCent);
    }

    public FIFlowField<String> getCodProd() {
        return codProd;
    }

    public void setCodProd(String codProd) {
        this.codProd.setContent(codProd);
    }

    public FIFlowField<String> getCdoPerbo() {
        return cdoPerbo;
    }

    public void setCdoPerbo(String cdoPerbo) {
        this.cdoPerbo.setContent(cdoPerbo);
    }

    public FIFlowField<JDate> getfConOper() {
        return fConOper;
    }

    public void setfConOper(JDate fConOper) {
        this.fConOper.setContent(fConOper);
    }


    public FIFlowField<String> getCestoPbo() {
        return cestoPbo;
    }

    public void setCestoPbo(String cestoPbo) {
        this.cestoPbo.setContent(cestoPbo);
    }

    public FIFlowField<String> getCdstrOpe() {
        return cdstrOpe;
    }

    public void setCdstrOpe(String cdstrOpe) {
        this.cdstrOpe.setContent(cdstrOpe);
    }

    public FIFlowField<String> getTiPerson() {
        return tiPerson;
    }

    public void setTiPerson(String tiPerson) {
        this.tiPerson.setContent(tiPerson);
    }

    public FIFlowField<Integer> getCdPerson() {
        return cdPerson;
    }

    public void setCdPerson(Integer cdPerson) {
        this.cdPerson.setContent(cdPerson);
    }

    public FIFlowField<String> getCodsProd() {
        return codsProd;
    }

    public void setCodsProd(String codsProd) {
        this.codsProd.setContent(codsProd);
    }

    public FIFlowField<String> getCcreFer() {
        return ccreFer;
    }

    public void setCcreFer(String ccreFer) {
        this.ccreFer.setContent(ccreFer);
    }

    public FIFlowField<String> getCdPortfo() {
        return cdPortfo;
    }

    public void setCdPortfo(String cdPortfo) {
        this.cdPortfo.setContent(cdPortfo);
    }

    public FIFlowField<Long> getnTtituloo() {
        return nTtituloo;
    }

    public void setnTtituloo(Long nTtituloo) {
        this.nTtituloo.setContent(nTtituloo);
    }

    public FIFlowField<Double> getiPrinOpe() {
        return iPrinOpe;
    }

    public void setiPrinOpe(Double iPrinOpe) {
        this.iPrinOpe.setContent(iPrinOpe);
    }

    public FIFlowField<String> getCoDivisa() {
        return coDivisa;
    }

    public void setCoDivisa(String coDivisa) {
        this.coDivisa.setContent(coDivisa);
    }

    //new

    public FIFlowField<String> getIndCober() {
        return indCober;
    }

    public void setIndCober(String indCober) {
        this.indCober.setContent(indCober);
    }

    public FIFlowField<String> getCdCobert() {
        return cdCobert;
    }

    public void setCdCobert(String cdCobert) {
        this.cdCobert.setContent(cdCobert);
    }

    public FIFlowField<String> getClCobert() {
        return clCobert;
    }

    public void setClCobert(String clCobert) {
        this.clCobert.setContent(clCobert);
    }

    public FIFlowField<String> getIdCobert() {
        return idCobert;
    }

    public void setIdCobert(String idCobert) {
        this.idCobert.setContent(idCobert);
    }

    public FIFlowField<String> getCodDivlq() {
        return codDivlq;
    }

    public void setCodDivlq(String codDivlq) {
        this.codDivlq.setContent(codDivlq);
    }

    public FIFlowField<Double> getImprLimp() {
        return imprLimp;
    }

    public void setImprLimp(Double imprLimp) {
        this.imprLimp.setContent(imprLimp);
    }

    public FIFlowField<Double> getImprSuci() {
        return imprSuci;
    }

    public void setImprSuci(Double imprSuci) {
        this.imprSuci.setContent(imprSuci);
    }

    public FIFlowField<String> getIdSentOp() {
        return idSentOp;
    }

    public void setIdSentOp(String idSentOp) {
        this.idSentOp.setContent(idSentOp);
    }

    public FIFlowField<Double> getImcpCorr() {
        return imcpCorr;
    }

    public void setImcpCorr(Double imcpCorr) {
        this.imcpCorr.setContent(imcpCorr);
    }

    public FIFlowField<Double> getImEfeOpe() {
        return imEfeOpe;
    }

    public void setImEfeOpe(Double imEfeOpe) {
        this.imEfeOpe.setContent(imEfeOpe);
    }

    public FIFlowField<String> getInOpinex() {
        return inOpinex;
    }

    public void setInOpinex(String inOpinex) {
        this.inOpinex.setContent(inOpinex);
    }

    public FIFlowField<String> gethConOper() {
        return hConOper;
    }

    public void sethConOper(String hConOper) {
        this.hConOper.setContent(hConOper);
    }

    public FIFlowField<String> getCdProdux() {
        return cdProdux;
    }

    public void setCdProdux(String cdProdux) {
        this.cdProdux.setContent(cdProdux);
    }

    public FIFlowField<Integer> getMarToMar() {
        return marToMar;
    }

    public void setMarToMar(Integer marToMar) {
        this.marToMar.setContent(marToMar);
    }

    public FIFlowField<JDate> getfLiqOpe() {
        return fLiqOpe;
    }

    public void setfLiqOpe(JDate fLiqOpe) {
        this.fLiqOpe.setContent(fLiqOpe);
    }

    public FIFlowField<String> getCdIroi() {
        return cdIroi;
    }

    public void setCdIroi(String cdIroi) {
        this.cdIroi.setContent(cdIroi);
    }

    public FIFlowField<String> getCdRig() {
        return cdRig;
    }

    public void setCdRig(String cdRig) {
        this.cdRig.setContent(cdRig);
    }

    public FIFlowField<String> getIdStrip() {
        return idStrip;
    }

    public void setIdStrip(String idStrip) {
        this.idStrip.setContent(idStrip);
    }

}
