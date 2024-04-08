package calypsox.tk.export.ack.model;

import javax.xml.bind.annotation.*;


@XmlRootElement(name = "collateralTransferReturnStatus")
@XmlAccessorType(XmlAccessType.FIELD)
public class MxDefaultAckBean {

    String transactionId = "";
    String messageId = "";
    String action = "";
    String status = "";
    String errorDescription = "";


    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }


}