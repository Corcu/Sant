package calypsox.tk.report;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.Account;

public class SantInterestPaymentRunnerEntry {


    public Trade getCtTrade() { return this.ctTrade; }

    public void setCtTrade(Trade ctTrade) {
        this.ctTrade = ctTrade;
    }

    public Trade getIbTrade() {
        return this.ibTrade;
    }

    public void setIbTrade(Trade ibTrade) {
        this.ibTrade = ibTrade;
    }

    public Trade getSimpleXferTrade() {
        return this.simpleXferTrade;
    }

    public void setSimpleXferTrade(Trade simpleXferTrade) {
        this.simpleXferTrade = simpleXferTrade;
    }

    public Account getAccount() {
        return this.account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public JDate getProcessDate() {
        return this.processDate;
    }

    public void setProcessDate(JDate processDate) {
        this.processDate = processDate;
    }

    public String getPoOwner() {
        return this.poOwner;
    }

    public String getCptyName() {
        return this.ctpyName;
    }

    public void setPoOwner(String poOwner) {
        this.poOwner = poOwner;
    }

    public void setCptyName(String cpty) {
        this.ctpyName = cpty;
    }

    public BOMessage getPaymentMessage() {
        return paymentMessage;
    }

    public void setPaymentMessage(BOMessage paymentMessage) {
        this.paymentMessage = paymentMessage;
    }

    public BOMessage getInterest() {
        return mcInterestMessage;
    }

    public void setInterest(BOMessage mcInterestMessage) {
        this.mcInterestMessage = mcInterestMessage;
    }

    private Trade ctTrade;

    private Trade ibTrade;

    private Trade simpleXferTrade;

    private Account account;

    private JDate processDate;

    private String poOwner;

    private String ctpyName;

    private BOMessage paymentMessage;

    private BOMessage mcInterestMessage;

}
