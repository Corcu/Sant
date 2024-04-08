package calypsox.engine.im.errorcodes;

import java.text.MessageFormat;

/**
 * This enum allows to send a set of known error codes
 * 
 */
public enum SantInitialMarginQefErrorCodeEnum {

	/** No Error. Everything is ok. */
	NoError(0, "Success"),

	/** Number of fields received don't' match the expected number of fields */
	MessageNotValid(1, "Message not Valid"),

	/** ContractId couldn't be formatted to int */
	ContractNotValid(2, "Contract not valid"),

	/** InitialMarginNotValid couldn't be formatted to Double */
	InitialMarginNotValid(3, "IM for PO o CPTY contains an invalid format"),

	/** Currency received not valid */
	CurrencyNotValid(4, "Currency not valid"),

	/** Error code not valid */
	CodeNotValid(5, "Error Code not valid"),

	;

	private int code;
	private String message;

	/**
	 * get the message for this error using params
	 * 
	 * @param params
	 *            String [] containing the information to create the message's text
	 * @return formatted text
	 */
	public String getFullTextMesssage(final String[] params) {
		final MessageFormat messageFormat = new MessageFormat(getMessage());
		final String result = messageFormat.format(params);
		return result;
	}

	private SantInitialMarginQefErrorCodeEnum(final int code, final String message) {
		setCode(code);
		setMessage(message);
	}

	/**
	 * Returns the error code
	 * 
	 * @return code
	 */
	public int getCode() {
		return this.code;
	}

	/**
	 * Set the error code
	 * 
	 * @param code
	 *            a code
	 */
	protected void setCode(final int code) {
		this.code = code;
	}

	/**
	 * returns the error's message template. For real message use the method getFullTextMesssage
	 * 
	 * @return the error's message
	 */
	public String getMessage() {
		return this.message;
	}

	/**
	 * set error's message template
	 * 
	 * @param message
	 *            error's message template
	 */
	protected void setMessage(final String message) {
		this.message = message;
	}

	/**
	 * check if a error code value exists
	 * 
	 * @param errorCode
	 *            code to find
	 * @return errorCodeEnum value if found
	 */
	public static SantInitialMarginQefErrorCodeEnum isValid(int errorCode) {
		SantInitialMarginQefErrorCodeEnum returnValue = null;

		for (SantInitialMarginQefErrorCodeEnum errorValue : SantInitialMarginQefErrorCodeEnum.values()) {
			if (errorCode == errorValue.getCode()) {
				return errorValue;
			}
		}
		return returnValue;
	}
}
