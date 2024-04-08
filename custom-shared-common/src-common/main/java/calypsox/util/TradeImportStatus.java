package calypsox.util;

import calypsox.tk.util.bean.InterfaceTradeBean;

import java.io.Serializable;

/**
 * Entity to represent an error on a trade import
 *
 * @author aela
 */
public class TradeImportStatus implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    public static final int OK = 0;
    public static final int ERROR = 1;
    public static final int WARNING = 2;
    // GSM: 15/07/2013. Trades Exclusions, based on DDR CalypsoColl - TradeInterface_Gaps_v1.2
    public static final int EXCLUDED = 3;

    protected String rowBeingImportedContent;
    protected int rowBeingImportedNb;
    protected String fieldName;
    protected String fieldValue;
    protected long tradeId;
    protected String boReference;
    protected int errorCode;
    protected String errorMessage;
    protected int importStatus;
    protected InterfaceTradeBean tradeBean;

    /**
     * @param errorCode
     * @param errorMessage
     */
    public TradeImportStatus(int errorCode, String errorMessage) {
        this(errorCode, errorMessage, ERROR);
    }

    public TradeImportStatus(int errorCode, String errorMessage, int errorType) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.importStatus = errorType;
    }

    /**
     * @param tradeBean    rows being imported
     * @param errorCode
     * @param errorMessage
     * @param errorType
     */
    public TradeImportStatus(InterfaceTradeBean tradeBean, int errorCode, String errorMessage, int errorType) {
        this.tradeBean = tradeBean;
        this.rowBeingImportedContent = tradeBean.getLineContent();
        this.rowBeingImportedNb = tradeBean.getLineNumber();
        this.tradeId = tradeBean.getTradeId();
        this.boReference = tradeBean.getBoReference();

        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.importStatus = errorType;
    }

    /**
     * @return the rowBeingImported
     */
    public String getRowBeingImportedContent() {
        return this.rowBeingImportedContent;
    }

    /**
     * @param rowBeingImported the rowBeingImported to set
     */
    public void setRowBeingImportedContent(String rowBeingImported) {
        this.rowBeingImportedContent = rowBeingImported;
    }

    /**
     * @return the errorCode
     */
    public int getErrorCode() {
        return this.errorCode;
    }

    /**
     * @param errorCode the errorCode to set
     */
    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * @return the errorMessage
     */
    public String getErrorMessage() {
        return this.errorMessage;
    }

    /**
     * @param errorMessage the errorMessage to set
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * @return the rowBeingImportedNb
     */
    public int getRowBeingImportedNb() {
        return this.rowBeingImportedNb;
    }

    /**
     * @param rowBeingImportedNb the rowBeingImportedNb to set
     */
    public void setRowBeingImportedNb(int rowBeingImportedNb) {
        this.rowBeingImportedNb = rowBeingImportedNb;
    }

    /**
     * @return the errorType
     */
    public int getErrorType() {
        return this.importStatus;
    }

    /**
     * @param errorType the errorType to set
     */
    public void setErrorType(int errorType) {
        this.importStatus = errorType;
    }

    /**
     * @return the boReference
     */
    public String getBoReference() {
        return this.boReference;
    }

    /**
     * @param boReference the boReference to set
     */
    public void setBoReference(String boReference) {
        this.boReference = boReference;
    }

    /**
     * @return the tradeId
     */
    public long getTradeId() {
        return this.tradeId;
    }

    /**
     * @param tradeId the tradeId to set
     */
    public void setTradeId(long tradeId) {
        this.tradeId = tradeId;
    }

    /**
     * @return the fieldName
     */
    public String getFieldName() {
        return this.fieldName;
    }

    /**
     * @param fieldName the fieldName to set
     */
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * @return the fieldValue
     */
    public String getFieldValue() {
        return this.fieldValue;
    }

    /**
     * @param fieldValue the fieldValue to set
     */
    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }

    /**
     * @return the status as a string
     */
    public String getLineStatus() {
        String status = "";
        switch (getErrorType()) {
            case TradeImportStatus.ERROR:
                status = "ERROR";
                break;
            case TradeImportStatus.WARNING:
                status = "WARNING";
                break;
            case TradeImportStatus.OK:
                status = "OK";
                break;
            case TradeImportStatus.EXCLUDED:
                status = "EXCLUDED";
                break;

            default:
                break;
        }
        return status;
    }

    /**
     * @return the tradeBean
     */
    public InterfaceTradeBean getTradeBean() {
        return this.tradeBean;
    }

    /**
     * @param tradeBean the tradeBean to set
     */
    public void setTradeBean(InterfaceTradeBean tradeBean) {

        if (tradeBean != null) {
            this.rowBeingImportedContent = tradeBean.getLineContent();
            this.rowBeingImportedNb = tradeBean.getLineNumber();
            this.tradeId = tradeBean.getTradeId();
            this.boReference = tradeBean.getBoReference();
        }
        this.tradeBean = tradeBean;
    }

    public boolean isOnlyWarning() {
        boolean isOnlyWarn = false;
        if (this.tradeBean != null) {
            isOnlyWarn = this.tradeBean.isWarningChecks() && !this.tradeBean.isErrorChecks();
        } else {
            isOnlyWarn = (getErrorType() == TradeImportStatus.WARNING);
        }
        return isOnlyWarn;
    }
}
