package calypsox.tk.util;

public class LagoEquitySwap {

    private LagoEquitySwapLeg loanLeg;
    private LagoEquitySwapLeg borrowLeg;

    public LagoEquitySwapLeg getLoanLeg() {
        return loanLeg;
    }

    public LagoEquitySwapLeg getBorrowLeg() {
        return borrowLeg;
    }

    public void setLoanLeg(LagoEquitySwapLeg loanLeg) {
        this.loanLeg = loanLeg;
    }

    public void setBorrowLeg(LagoEquitySwapLeg borrowLeg) {
        this.borrowLeg = borrowLeg;
    }

}
