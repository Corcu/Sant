package calypsox.tk.util.exception;

public enum SantExceptionType {

    TECHNICAL_EXCEPTION(
	    "EX_TECHNICAL_EXCEPTION",
	    "Database connectivity/code issue"),

    INVALID_DATA_EXCEPTION(
	    "EX_INVALID_DATA_EXCEPTION",
	    "Unexpected value of a data"),

    INFORMATION("EX_INFORMATION", "Information"),

    ACC_NACK_TECHNICAL_EXCEPTION(
	    "EX_ACC_NACK_TECHNICAL_EXCEPTION",
	    "Database connectivity/code issue"),

    ACC_NACK_INVALID_DATA_EXCEPTION(
	    "EX_ACC_NACK_INVALID_DATA_EXCEPTION",
	    "Unexpected value of a data"),

    ACC_NACK_FUNCTIONAL_EXCEPTION(
	    "EX_ACC_NACK_FUNCTIONAL_EXCEPTION",
	    "Functional Exception - reason why NACK"),

    CONFIGURATION_EXCEPTION(
	    "EX_CONFIGURATION",
	    "Problem in the configuraiton of the component"),

    STATIC_DATA("EX_STATIC_DATA", "Incorrect value for static data"),

    DOCUMENT_SENT_ERROR("EX_DOCUMENT_SENT_ERROR", "Error sending a document");

    protected String _type;
    protected String _description;

    SantExceptionType(String type, String description) {
		_type = type;
		_description = description;
    }
}
