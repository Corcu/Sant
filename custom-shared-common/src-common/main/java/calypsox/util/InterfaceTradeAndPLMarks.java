/**
 *
 */
package calypsox.util;

import calypsox.tk.util.bean.InterfaceTradeBean;
import com.calypso.tk.core.Trade;

import java.io.Serializable;

/**
 *
 * Bean for Trade and PL Marks associated
 *
 * @author aela
 * @version 1.1
 * @date 29/04/2014
 *
 */
public class InterfaceTradeAndPLMarks implements Serializable {

    private static final long serialVersionUID = 1457987742917496872L;

    protected Trade trade;
    protected InterfacePLMarkBean plMark1;
    protected InterfacePLMarkBean plMark2;
    protected InterfacePLMarkBean plMarkIA1;
    protected InterfacePLMarkBean plMarkIA2;
    protected InterfacePLMarkBean plMarkClosingPrice1 = null;
    protected InterfacePLMarkBean plMarkNpv1 = null;

    protected int lineNumber;
    protected InterfaceTradeBean tradeBean;

    /**
     * @return the trade
     */
    public Trade getTrade() {
        return this.trade;
    }

    /**
     * @param trade
     *            the trade to set
     */
    public void setTrade(Trade trade) {
        this.trade = trade;
    }

    /**
     * @return the plMark1
     */
    public InterfacePLMarkBean getPlMark1() {
        return this.plMark1;
    }

    /**
     * @param plMark1
     *            the plMark1 to set
     */
    public void setPlMark1(InterfacePLMarkBean plMark1) {
        this.plMark1 = plMark1;
    }

    /**
     * @return the plMark2
     */
    public InterfacePLMarkBean getPlMark2() {
        return this.plMark2;
    }

    /**
     * @param plMark2
     *            the plMark2 to set
     */
    public void setPlMark2(InterfacePLMarkBean plMark2) {
        this.plMark2 = plMark2;
    }

    /**
     * @return the plMarkIA1
     */
    public InterfacePLMarkBean getPlMarkIA1() {
        return this.plMarkIA1;
    }

    /**
     * @param plMarkIA1
     *            the plMarkIA1 to set
     */
    public void setPlMarkIA1(InterfacePLMarkBean plMarkIA1) {
        this.plMarkIA1 = plMarkIA1;
    }

    /**
     * @return the plMarkIA2
     */
    public InterfacePLMarkBean getPlMarkIA2() {
        return this.plMarkIA2;
    }

    /**
     * @param plMarkIA2
     *            the plMarkIA2 to set
     */
    public void setPlMarkIA2(InterfacePLMarkBean plMarkIA2) {
        this.plMarkIA2 = plMarkIA2;
    }

    /**
     * @return the tradeBean
     */
    public InterfaceTradeBean getTradeBean() {
        return this.tradeBean;
    }

    /**
     * @param tradeBean
     *            the tradeBean to set
     */
    public void setTradeBean(InterfaceTradeBean tradeBean) {
        this.tradeBean = tradeBean;
    }

    /**
     * @return number of line
     */
    public int getLineNumber() {
        return this.lineNumber;

    }

    /**
     * @param lineNumber
     *            the lineNumber to set
     */
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     *
     * @param plMarkClosingPrice
     */
    // GSM: 29/04/2014. PdV adaptation in exposure importation: CLOSING_PRICE Pl_Mark added
    public void setPlMarkClosingPrice1(InterfacePLMarkBean plMarkClosingPrice) {
        this.plMarkClosingPrice1 = plMarkClosingPrice;
    }

    /**
     * @return the plMarkClosingPrice1
     */
    public InterfacePLMarkBean getPlMarkClosingPrice1() {
        return this.plMarkClosingPrice1;
    }

    /**
     *
     * @param plMarkNpv
     *            , MtM without the haircut
     */
    public void setPlMarkNpv(InterfacePLMarkBean plMarkNpv) {
        this.plMarkNpv1 = plMarkNpv;

    }

    // GSM: 29/04/2014. PdV adaptation: save of the mtm without the Haircut

    /**
     * @return the plMarkNpv1
     */
    public InterfacePLMarkBean getPlMarkNpv1() {
        return this.plMarkNpv1;
    }

    /**
     * @param plMarkNpv1
     *            the plMarkNpv1 to set
     */
    public void setPlMarkNpv1(InterfacePLMarkBean plMarkNpv1) {
        this.plMarkNpv1 = plMarkNpv1;
    }

}
