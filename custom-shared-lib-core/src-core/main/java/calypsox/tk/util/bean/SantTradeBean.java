package calypsox.tk.util.bean;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.Trade;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Guillermo
 */
public class SantTradeBean {

    private String externalReference;
    private String NPVCcy;
    private Double npv_mtm;
    private String NPVDate;
    private String haircut;
    private String book;
    private Integer configId;
    private List<Trade> tradeList;
    private final Set<String> errors = new HashSet<>();
    private Integer lineNumber;

    /**
     *
     */
    public SantTradeBean() {

        this.externalReference = null;
        this.NPVCcy = null;
        this.npv_mtm = null;
        this.NPVDate = null;
        this.book = null;
        this.haircut = null;
        this.configId = null;
        this.tradeList = new ArrayList<>();
    }

    /**
     * @return the npv_mtm
     */
    public Double getNpv_mtm() {
        return this.npv_mtm;
    }

    /**
     * @param npv_mtm the npv_mtm to set
     */
    public void setNpv_mtm(Double npv_mtm) {
        this.npv_mtm = npv_mtm;
    }

    /**
     * @return the book
     */
    public String getBook() {
        return this.book;
    }

    /**
     * @param book the book to set
     */
    public void setBook(String book) {
        this.book = book;
    }

    /**
     * @return the errors
     */
    public Set<String> getErrors() {
        return this.errors;
    }

    public boolean hasError() {
        return !this.errors.isEmpty();
    }

    /**
     * @param errors the errors to set
     */
    public void setErrors(List<String> errors) {
        this.errors.addAll(errors);
    }

    /**
     * @param description error to add
     */
    public void addError(String description) {
        this.errors.add(description);
    }

    /**
     * @param configId the configId to set
     */
    public void setConfigId(Integer configId) {
        this.configId = configId;
    }

    /**
     * @return the externalReference
     */
    public String getExternalReference() {
        return this.externalReference;
    }

    /**
     * @param externalReference the externalReference to set
     */
    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    /**
     * @return the nPVCcy
     */
    public String getNPVCcy() {
        return this.NPVCcy;
    }

    /**
     * @param nPVCcy the nPVCcy to set
     */
    public void setNPVCcy(String nPVCcy) {
        this.NPVCcy = nPVCcy;
    }

    /**
     * @return the nPV
     */
    public Double getNPV() {
        return this.npv_mtm;
    }

    /**
     * @param nPV the nPV to set
     */
    public void setNPV(Double nPV) {
        this.npv_mtm = nPV;
    }

    /**
     * @return the nPVDate
     */
    public String getNPVDate() {
        return this.NPVDate;
    }

    /**
     * @param nPVDate the nPVDate to set
     */
    public void setNPVDate(String nPVDate) {
        this.NPVDate = nPVDate;
    }

    /**
     * @return the haircut
     */
    public String getHaircut() {
        return this.haircut;
    }

    /**
     * @param haircut the haircut to set
     */
    public void setHaircut(String haircut) {
        this.haircut = haircut;
    }

    /**
     * @return false if ext reference, NPV, NPVDate,NPV CCY are empty (at least one)
     */
    public boolean isEmpty() {

        return (Util.isEmpty(this.externalReference) || Util.isEmpty(this.NPVCcy) || Util.isEmpty(this.NPVDate) || (this.npv_mtm == null));
    }

    /**
     * @return the configId
     */
    public Integer getConfigId() {
        return this.configId;
    }

    /**
     * @param i the configId to set
     */
    public void setConfigId(int i) {
        this.configId = i;
    }

    /**
     * @return the trade
     */
    public List<Trade> getTradeList() {
        return this.tradeList;
    }

    /**
     * @return the trade
     */
    public List<Long> getReferencedList() {

        ArrayList<Long> l = new ArrayList<>();
        for (Trade t : this.tradeList) {
            l.add(t.getLongId());
        }
        return l;
    }

    /**
     * @return the trade
     */
    public Trade getTrade() {
        if (this.tradeList.isEmpty() || moreTrades4ExtRef()) {
            return null;
        }
        return this.tradeList.get(0);
    }

    public boolean moreTrades4ExtRef() {
        return this.tradeList.size() > 1;
    }

    /**
     * @param list the trade to set
     */
    public void setTradeList(List<Trade> list) {
        this.tradeList = list;
    }

    /**
     * @return the lineNumber
     */
    public Integer getLineNumber() {
        return this.lineNumber;
    }

    /**
     * @param lineNumber the lineNumber to set
     */
    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * To string override method
     */
    @Override
    public String toString() {
        return ("|id:" + this.externalReference + "|book:" + this.book + ":ccy:" + this.NPVCcy + "|npv:" + this.npv_mtm
                + "|date:" + this.NPVDate);
    }

}
