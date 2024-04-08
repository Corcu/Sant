package calypsox.tk.report.exception;

import calypsox.ErrorCodeEnum;

public enum SantExceptionType {

    TECHNICAL_EXCEPTION(
	    "EX_TECHNICAL_EXCEPTION",
	    "Database connectivity/code issue",
	    null),

    INVALID_DATA_EXCEPTION(
	    "EX_INVALID_DATA_EXCEPTION",
	    "Unexpected value of a data",
	    null),

    INFORMATION("EX_INFORMATION", "Information", null),


    CONFIGURATION_EXCEPTION(
	    "EX_CONFIGURATION",
	    "Problem in the configuraiton of the component",
	    ErrorCodeEnum.ScheduledTaskConfigError),

    STATIC_DATA(
	    "EX_STATIC_DATA",
	    "Incorrect value for static data",
	    ErrorCodeEnum.StaticData),


    DOCUMENT_SENT_ERROR(
	    "EX_DOCUMENT_SENT_ERROR",
	    "Error sending a document",
	    null),

	/**
	 * The message handler for the adapter failed to parse the incoming message
	 */
	JMS_IMPORTER_PARSE(
			"EX_JMS_IMPORTER_PARSE",
			"The message handler for the adapter failed to parse the incoming message",
			null),

	/** Represent a NACK received by a message without expecifiying the type */
	NACK(
			"EX_NACK",
			"Represent a NACK received by a message without expecifiying the type",
			null),

	// CAL_BO_138
	/**
	 * Payment greater than Threshold Amount. Its OK with no EX_ prefix, that
	 * prefix is inserted automatically by Calypso when a BOException is created
	 */
	STOP_PAYMENT_THRESHOLD("EX_PAYMENT_THRESHOLD",
			"Settlement amount greater than Threshold Amount.", null),

	/** Exception in PaymentsHub Callback */
	PH_CALLBACK_EXCEPTION(
			"EX_PH_CALLBACK_EXCEPTION",
			"Exception processing PaymentsHub Callback.",
			null);






    protected String type;//Sonar
    private String description;//Sonar
    private ErrorCodeEnum errorCodeEnum;//Sonar

    SantExceptionType(String type, String description,
	    ErrorCodeEnum errorCodeEnum) {
	this.type = type;
	this.description = description;
	this.errorCodeEnum = errorCodeEnum;
    }

    /**
     * @return the type
     */
    public String getType() {
	return type;
    }

    /**
     * @param type
     *            the type to set
     */
    public void setType(String type) {
	this.type = type;
    }

    /**
     * @return the description
     */
    public String getDescription() {
	return description;
    }

    /**
     * @param description
     *            the description to set
     */
    public void setDescription(String description) {
	this.description = description;
    }

    /**
     * @return the errorCodeEnum
     */
    public ErrorCodeEnum getErrorCodeEnum() {
	return errorCodeEnum;
    }

    /**
     * @param errorCodeEnum
     *            the errorCodeEnum to set
     */
    public void setErrorCodeEnum(ErrorCodeEnum errorCodeEnum) {
	this.errorCodeEnum = errorCodeEnum;
    }

}
