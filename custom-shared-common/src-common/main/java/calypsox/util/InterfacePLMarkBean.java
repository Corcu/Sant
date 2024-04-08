/**
 *
 */
package calypsox.util;

import com.calypso.tk.core.JDate;

import java.io.Serializable;

/**
 * Contains pl marks information
 *
 * @author aela
 *
 */
public class InterfacePLMarkBean implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    protected JDate plMarkDate;
    protected String plMarkCurrency;
    protected double plMarkValue;

    /**
     * @return the plMarkDate
     */
    public JDate getPlMarkDate() {
        return plMarkDate;
    }

    /**
     * @param plMarkDate
     *            the plMarkDate to set
     */
    public void setPlMarkDate(JDate plMarkDate) {
        this.plMarkDate = plMarkDate;
    }

    /**
     * @return the plMarkCurrency
     */
    public String getPlMarkCurrency() {
        return plMarkCurrency;
    }

    /**
     * @param plMarkCurrency
     *            the plMarkCurrency to set
     */
    public void setPlMarkCurrency(String plMarkCurrency) {
        this.plMarkCurrency = plMarkCurrency;
    }

    /**
     * @return the plMarkValue
     */
    public double getPlMarkValue() {
        return plMarkValue;
    }

    /**
     * @param plMarkValue
     *            the plMarkValue to set
     */
    public void setPlMarkValue(double plMarkValue) {
        this.plMarkValue = plMarkValue;
    }

}
