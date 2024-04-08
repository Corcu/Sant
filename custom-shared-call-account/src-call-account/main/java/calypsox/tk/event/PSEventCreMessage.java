package calypsox.tk.event;

import com.calypso.tk.event.PSEventCre;
/**
 * @author acd
 */
public class PSEventCreMessage extends PSEventCre {
    String message;
    Integer contractId;
    Double accountBalance;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getContractId() {
        return contractId;
    }

    public void setContractId(Integer contractId) {
        this.contractId = contractId;
    }

    public Double getAccountBalance() {
        return accountBalance;
    }

    public void setAccountBalance(Double accountBalance) {
        this.accountBalance = accountBalance;
    }
}
