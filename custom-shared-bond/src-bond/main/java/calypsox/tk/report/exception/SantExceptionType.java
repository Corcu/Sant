package calypsox.tk.report.exception;

import calypsox.ErrorCodeEnum;

/**
 * Enumeration of Exceptions which can be sent to the TaskStation.
 */
public enum SantExceptionType {

	/** Functional Exception - reason why ACK */
	ACC_ACK_FUNCTIONAL_EXCEPTION(
			"EX_ACC_ACK_FUNCTIONAL_EXCEPTION",
			"Functional Exception - reason why ACK",
			null),
	/** Missing Partenon IDCONTR */
	ACC_MISSINGXFERPARTENON(
			"EX_ACC_MISSINGXFERPARTENON",
			"Missing Partenon IDCONTR",
			null),
	/** Functional Exception - reason why NACK */
	ACC_NACK_FUNCTIONAL_EXCEPTION(
			"EX_ACC_NACK_FUNCTIONAL_EXCEPTION",
			"Functional Exception - reason why NACK",
			null),
	/** Unexpected value of a data */
	ACC_NACK_INVALID_DATA_EXCEPTION(
			"EX_ACC_NACK_INVALID_DATA_EXCEPTION",
			"Unexpected value of a data",
			ErrorCodeEnum.InvalidData),
	/** Database connectivity/code issue */
	ACC_NACK_TECHNICAL_EXCEPTION(
			"EX_ACC_NACK_TECHNICAL_EXCEPTION",
			"Database connectivity/code issue",
			ErrorCodeEnum.UndefinedException),
	/**
	 * Generated by scheduled task ACCOUNT_STATEMENT if scheduled Task failed.
	 */
	ACCOUNT_STATEMENT(
			"EX_ACCOUNT_STATEMENT",
			"Generated by scheduled task ACCOUNT_STATEMENT if scheduled Task failed.",
			null),
	/**
	 * Exception Generated by the Accounting Engine if the Accounting Engine is
	 * not able to retrieve Pricer attached to trade in order to generate
	 * reevaluation posting.
	 */
	ACCOUNTING_NO_PRICER(
			"EX_ACCOUNTING_NO_PRICER",
			"Exception Generated by the Accounting Engine if the Accounting Engine is not able to retrieve Pricer attached to trade in order to generate reevaluation posting.",
			null),
	/**
	 * Exception Generated by the Accounting Engine if the Accounting Engine is
	 * not able to retrieve Pricer Measure define in Pricer Measure table in
	 * order to generate reevaluation posting.
	 */
	ACCOUNTING_NO_PRICERMEASURE(
			"EX_ACCOUNTING_NO_PRICERMEASURE",
			"Exception Generated by the Accounting Engine if the Accounting Engine is not able to retrieve Pricer Measure define in Pricer Measure table in order to generate reevaluation posting.",
			null),
	/**
	 * Exception Generated by the Accounting Engine if the Accounting Engine did
	 * not find any Accounting Rule to process a specific Trade.
	 */
	ACCOUNTING_NO_RULE(
			"EX_ACCOUNTING_NO_RULE",
			"Exception Generated by the Accounting Engine if the Accounting Engine did not find any Accounting Rule to process a specific Trade.",
			null),
	/**
	 * Exception Generated by the Accounting Engine if the Accounting Set up was
	 * not correct for processing a Trade.
	 */
	ACCOUNTING_SETUP(
			"EX_ACCOUNTING_SETUP",
			"Exception Generated by the Accounting Engine if the Accounting Set up was not correct for processing a Trade.",
			null),
	/**
	 * Exception Generated by the Accounting Engine if the Accounting engine is
	 * not able to retrieve trade or when Accounting Engine is not able to
	 * retrieve transfer. OR Generated by the Accounting Engine when it is not
	 * able to retrieve an account.
	 */
	ACCOUNTING_TECH(
			"EX_ACCOUNTING_TECH",
			"Exception Generated by the Accounting Engine if the Accounting engine is not able to retrieve trade or when Accounting Engine is not able to retrieve transfer. OR Generated by the Accounting Engine when it is not able to retrieve an account.",
			null),
	/**
	 * Exception Generated by the trade price if the trade price has not a
	 * specific NPV
	 */
	ACCOUNTING_MISSING_NPV(
			"EX_ACCOUNTING_MISSING_NPV",
			"Exception Generated by the trade price if the trade price has not a specific NPV",
			null),
	/**
	 * Exception Generated by the trade price if the trade price has not a
	 * specific quote
	 */
	ACCOUNTING_MISSING_QUOTE(
			"EX_ACCOUNTING_MISSING_QUOTE",
			"Exception Generated by the trade price if the trade price has not a specific quote",
			null),
	/**
	 * Exception Generated by the Message Engine if the Advice Set up was not
	 * correct for producing an Advice.
	 */
	ADVICE_SETUP(
			"EX_ADVICE_SETUP",
			"Exception Generated by the Message Engine if the Advice Set up was not correct for producing an Advice.",
			null),
	/**
	 * Generated when Advice Set up is triggered by an Event type which is not
	 * handled by the message engine.
	 */
	ADVICE_SETUP_TECH(
			"EX_ADVICE_SETUP_TECH",
			"Generated when Advice Set up is triggered by an Event type which is not handled by the message engine.",
			null),
	/** CASH_MANAGEMENT_INTEGRATION_MT940_CLASSIC */
	CASH_MANAGEMENT_INTEGRATION_MT940_CLASSIC(
			"EX_CASH_MANAGEMENT_INTEGRATION_MT940_CLASSIC",
			"",
			null),
	/** CASH_MANAGEMENT_INTEGRATION_MT950 */
	CASH_MANAGEMENT_INTEGRATION_MT950(
			"EX_CASH_MANAGEMENT_INTEGRATION_MT950",
			"",
			null),
	/** Problem in the configuraiton of the component */
	CONFIGURATION_EXCEPTION(
			"EX_CONFIGURATION",
			"Problem in the configuraiton of the component",
			ErrorCodeEnum.ScheduledTaskConfigError),
	/**
	 * Generated by scheduled task CORPORATE_ACTION if scheduled Task failed.
	 */
	CORPORATE_ACTION(
			"EX_CORPORATE_ACTION",
			"Generated by scheduled task CORPORATE_ACTION if scheduled Task failed.",
			null),
	/** A cre was blocked */
	CRE_BLOCKED("EX_CRE_BLOCKED", "A cre was blocked", null),
	/** Error converting to PO Ccy */
	// Traded FX
	CRE_CONVERTQUOTE("EX_CRE_CONVERTQUOTE", "Error converting to PO Ccy", null),
	/** Can not generate the CRE */
	CRE_GENERATION("EX_CRE_GENERATION", "", null),
	/** Error retrieving a price form the database while generation the CRE */
	CRE_GENERATION_MISSING_PRICES_FX(
			"EX_CRE_GENERATION_MISSING_PRICES_FX",
			"Error retrieving a price form the database while generation the CRE",
			null),
	/** Error retrieving a price form the database while generation the CRE */
	CRE_GENERATION_MISSING_PRICES_MM(
			"EX_CRE_GENERATION_MISSING_PRICES_MM",
			"Error retrieving a price form the database while generation the CRE",
			null),
	/** Error making withheld amounts */
	CRE_WITHHELD_AMOUNTS(
			"EX_CRE_WITHHELD_AMOUNTS",
			"Error making withheld amounts",
			null),
	/** Generated when could not create Account acc.getName(). */
	CREATING_ACCOUNT_LINK(
			"EX_CREATING_ACCOUNT_LINK",
			"Generated when could not create Account acc.getName().",
			null),
	/** Generated when could not create Account acc.getName(). */
	CREATING_ACCOUNT_NORMAL(
			"EX_CREATING_ACCOUNT_NORMAL",
			"Generated when could not create Account acc.getName().",
			null),
	/**
	 * Generated if the engine is not able to find a Settle Account or build an
	 * Automatic Settle. Automatic Account are created by Transfer engine and/or
	 * Accounting Engine.
	 */
	CREATING_ACCOUNT_SETTLE(
			"EX_CREATING_ACCOUNT_SETTLE",
			"Generated if the engine is not able to find a Settle Account or build an Automatic Settle. Automatic Account are created by Transfer engine and/or Accounting Engine.",
			null),
	/** Generated when could not create Account acc.getName(). */
	CREATING_ACCOUNT_STOCK(
			"EX_CREATING_ACCOUNT_STOCK",
			"Generated when could not create Account acc.getName().",
			null),
	/**
	 * Error trying to make one acction on the acked cre transfer recived from
	 * DGO
	 */
	DGOACK_XFERNOTPROCESSED(
			"EX_DGOACK_XFERNOTPROCESSED",
			"Error trying to make one acction on the acked cre transfer recived from DGO",
			null),
	/**
	 * Error trying to make one acction on the nacked cre transfer recived from
	 * DGO
	 */
	DGONACK_XFERNOTPROCESSED(
			"EX_DGONACK_XFERNOTPROCESSED",
			"Error trying to make one acction on the nacked cre transfer recived from DGO",
			null),
	/**
	 * Generated by the Sender Engine when Sender Engine is not able to format
	 * or find Document Sender method or copy or save message formatted.
	 */
	DOCUMENT_SENT_ERROR(
			"EX_DOCUMENT_SENT_ERROR",
			"Generated by the Sender Engine when Sender Engine is not able to format or find Document Sender method or copy or save message formatted.",
			null),
	/**
	 * Exception Generated by the Transfer Engine if the Transfer Engine tries
	 * but cannot CANCEL and replace an existing Transfer. As a result, the user
	 * should input a manual correction.
	 */
	DUPLICATE_PAYMENT(
			"EX_DUPLICATE_PAYMENT",
			"Exception Generated by the Transfer Engine if the Transfer Engine tries but cannot CANCEL and replace an existing Transfer. As a result, the user should input a manual correction.",
			null),
	/**
	 * An exception has occurred in the system. The exception was not classified.
	 */
	EXCEPTION(
			"EX_EXCEPTION",
			"An exception has occurred in the system. The exception was not classified.",
			null),
	/**
	 * Generated when the Transfer engine is not able generate cashflow
	 * associated to trade. Or When number of Cash Flow does not match number of
	 * transfer generated.
	 */
	EXCEPTION_GENERATING_CASHFLOW(
			"EXCEPTION_GENERATING_CASHFLOW",
			"Generated when the Transfer engine is not able generate cashflow associated to trade. Or When number of Cash Flow does not match number of transfer generated.",
			null),
	/**
	 * Generated when the Transfer Engine is not able generate transfer from
	 * trade.
	 */
	GENERATING_TRANSFER(
			"EX_GENERATING_TRANSFER",
			"Generated when the Transfer Engine is not able generate transfer from trade.",
			null),
	/** EX_HOLIDAYS_CHANGES */
	HOLIDAY_CHANGES("EX_HOLIDAYS_CHANGES", "", null), /** Information */
	INFORMATION("EX_INFORMATION", "Information", null),
	/** Unexpected value of a data */
	INVALID_DATA_EXCEPTION(
			"EX_INVALID_DATA_EXCEPTION",
			"Unexpected value of a data",
			null),
	/**
	 * The message handler for the adapter failed to parse the incoming message
	 */
	JMS_IMPORTER_PARSE(
			"EX_JMS_IMPORTER_PARSE",
			"The message handler for the adapter failed to parse the incoming message",
			null),
	/**
	 * Generated when Message Engine is not able to find the legal entity with a
	 * certain role.
	 */
	LE_ROLE(
			"EX_LE_ ROLE",
			"Generated when Message Engine is not able to find the legal entity with a certain role.",
			null),
	/** Generated when log file is full. */
	LOG_FULL("EX_LOG_FULL", "Generated when log file is full.", null),
	/**
	 * Generated by the Transfer Engine when transfer engine is not able to find
	 * Some settlement instructions and attach these SDI to transfer.
	 */
	MISSING_SI(
			"EX_MISSING_SI",
			"Generated by the Transfer Engine when transfer engine is not able to find Some settlement instructions and attach these SDI to transfer.",
			null),
	/** Represent a NACK received by a message without expecifiying the type */
	NACK(
			"EX_NACK",
			"Represent a NACK received by a message without expecifiying the type",
			null),
	/** A NACK for a sent message have been received */
	NACK_CHASECONFIRM(
			"EX_CHASECONFIRM",
			"A NACK for a sent message have been received",
			null),
	/** A NACK for a sent message have been received */
	NACK_CLSCONFIRM(
			"EX_CLSCONFIRM",
			"A NACK for a sent message have been received",
			null),
	/** A NACK for a sent message have been received */
	NACK_CLSTCOPYCONFIRM(
			"EX_CLSTCOPYCONFIRM",
			"A NACK for a sent message have been received",
			null),
	/** A NACK for a sent message was received */
	NACK_MMSWIFTCONFIRM(
			"EX_NACK_MMSWIFTCONFIRM",
			"A NACK for a sent message was received",
			null),
	/** A NACK for a sent message was received */
	NACK_PAYMENTMSG(
			"EX_NACK_PAYMENTMSG",
			"A NACK for a sent message was received",
			null),
	/** A NACK for a sent message was received */
	NACK_RECEIPTMSG(
			"EX_NACK_RECEIPTMSG",
			"A NACK for a sent message was received",
			null),
	/** A NACK for a sent message was received */
	NACK_SWIFTCONFIRM(
			"EX_NACK_SWIFTCONFIRM",
			"A NACK for a sent message was received",
			null),
	/** A NACK for a sent message was received */
	NACK_DOCCONFIRM(
			"EX_NACK_DOCCONFIRM",
			"A NACK for a sent message was received",
			null),
	/**
	 * No user had yet completed the task when the task's cutoff date was
	 * reached. Now the outdated task is no longer eligible for completion.
	 */
	OUTDATED_TASK(
			"EX_OUTDATED_TASK",
			"No user had yet completed the task when the task's cutoff date was reached. Now the outdated task is no longer eligible for completion.",
			null),
	/**
	 * When task is still not applicable after the cutoff date. This type of
	 * exception has been split into 3 exception types based on task object
	 * processed (Trade, Transfer, Message).
	 */
	OUTDATED_TASK_MESSAGE(
			"EX_OUTDATED_TASK_MESSAGE",
			"When task is still not applicable after the cutoff date. This type of exception has been split into 3 exception types based on task object processed (Trade, Transfer, Message).",
			null),
	/**
	 * When task is still not applicable after the cutoff date. This type of
	 * exception has been split into 3 exception types based on task object
	 * processed (Trade, Transfer, Message).
	 */
	OUTDATED_TASK_TRADE(
			"EX_OUTDATED_TASK_TRADE",
			"When task is still not applicable after the cutoff date. This type of exception has been split into 3 exception types based on task object processed (Trade, Transfer, Message).",
			null),
	/**
	 * When task is still not applicable after the cutoff date. This type of
	 * exception has been split into 3 exception types based on task object
	 * processed (Trade, Transfer, Message).
	 */
	OUTDATED_TASK_TRANSFER(
			"EX_OUTDATED_TASK_TRANSFER",
			"When task is still not applicable after the cutoff date. This type of exception has been split into 3 exception types based on task object processed (Trade, Transfer, Message).",
			null),
	/** EX_PARSING_ERROR */
	PARSING_ERROR("EX_PARSING_ERROR", "", null),
	/** Generated by scheduled task RATE_RESET if scheduled Task failed. */
	RATE_RESET(
			"EX_RATE_RESET",
			"Generated by scheduled task RATE_RESET if scheduled Task failed.",
			null),

	/**
	 * Generated by SantRansferLiquider report if it has problem getting the
	 * Data Server connection
	 */
	REPORT_DS_FUNCTIONAL_EXCEPTION(
			"REPORT_DS_FUNCTIONAL_EXCEPTION",
			"Generated by SantRansferLiquider report if it has problem getting the Data Server connection",
			null),

	/**
	 * Some static data are missing. For example, if there is no Legal Agreement
	 * or no Contact defined for a Counterparty, you could have this type of
	 * exception.
	 */
	STATIC_DATA(
			"EX_STATIC_DATA",
			"Some static data are missing. For example, if there is no Legal Agreement or no Contact defined for a Counterparty, you could have this type of exception.",
			ErrorCodeEnum.StaticData),
	/** Database connectivity/code issue */
	TECHNICAL_EXCEPTION(
			"EX_TECHNICAL_EXCEPTION",
			"Database connectivity/code issue",
			null),
	/** Nack from CM UK */
	CMUK_NACKED("EX_CMUK_NACKED", "Nack received from CMUK", null),

	CMUK_ERROR_SENDING(
			"EX_CMUK_ERROR_SENDING",
			"Error sending CMUK Transfer message to Queue",
			null),
	CMUK_ERROR_BUILDING_MSG(
			"EX_CMUK_ERROR_BUILDING_MSG",
			"Error building CMUK Transfer message",
			null),
	/** EX_TRADE_AUTH */
	TRADE_AUTH("EX_TRADE_AUTH", "", null),
	/** Information only: when a tradeprice exists and received a new value */
	UPDATE_NPV(
			"EX_UPDATE_NPV",
			"Information only: when a tradeprice exists and received a new value",
			null),
	/**
	 * The Valuation Process by Batch has encountered some errors. (A quote or
	 * Market Data Item was missing for example.) Used in EOD_TRADE_VALUATION
	 * and EOD_POSITION_VALUATION scheduled task.
	 */
	VALUATION_PROCESS(
			"EX_VALUATION_PROCESS",
			"The Valuation Process by Batch has encountered some errors. (A quote or Market Data Item was missing for example.) Used in EOD_TRADE_VALUATION and EOD_POSITION_VALUATION scheduled task.",
			null),
	/**
	 * The Valuation Process by Batch has encountered some errors. (A quote or
	 * Market Data Item was missing for example). Used in EOD_TRADE_VALUATION
	 * and EOD_POSITION_VALUATION scheduled task.
	 */
	VALUATION_PROCESS_FAILURE(
			"EX_VALUATION_PROCESS_FAILURE",
			"The Valuation Process by Batch has encountered some errors. (A quote or Market Data Item was missing for example). Used in EOD_TRADE_VALUATION and EOD_POSITION_VALUATION scheduled task.",
			null),
	/**
	 * The Valuation Process by Batch has executed without errors. Used in
	 * EOD_TRADE_VALUATION and EOD_POSITION_VALUATION scheduled task.
	 */
	VALUATION_PROCESS_SUCCESS(
			"EX_VALUATION_PROCESS_SUCCESS",
			"The Valuation Process by Batch has executed without errors. Used in EOD_TRADE_VALUATION and EOD_POSITION_VALUATION scheduled task.",
			null),
	/** XFERASSIGNED_CANCEL */
	XFERASSIGNED_CANCEL("EX_XFERASSIGNED_CANCEL", "", null),
	/**
	 * This exception indicates that the message could be sent to the queue but
	 * its status is not SENT
	 */
	MSG_MAYBE_SENT(
			"EX_MSG_MAYBE_SENT",
			"This exception indicates that the message could be sent to the queue but its status is not SENT",
			null),
	/** A DTCC msg has been Nacked. */
	DTCC_NACK("EX_DTCC_NACK", "A DTCC msg has been Nacked.", null),
	/** A mail could not be sent to its destinatary */
	EMAIL_SEND_FAILED(
			"EX_EMAIL_SEND_FAILED.",
			"A mail could not be sent to its destinatary ",
			null),
	/** The SCRITTURA message handler failed to parse the incoming message. */
	SCRITTURA_MESSAGE_PARSE(
			"EX_SCRITTURA_MESSAGE_PARSE",
			"The SCRITTURA message handler failed to parse the incoming message.",
			null),
	/** A SCRITTURA event could not be published. */
	SCRITTURA_ERROR_EVENT(
			"EX_SCRITTURA_ERROR_EVENT",
			"A SCRITTURA event could not be published.",
			null),
	/** A SCRITTURA message could not be saved. */
	SCRITTURA_ERROR_MESSAGE(
			"EX_SCRITTURA_ERROR_MESSAGE",
			"A SCRITTURA message could not be saved.",
			null),
	/** PSEventScritturaAnswer could not be processed. */
	SCRITTURA_EVENT_NOT_PROCESSED(
			"EX_SCRITTURA_EVENT_NOT_PROCESSED",
			"PSEventScritturaAnswer could not be processed.",
			null),

	// CAL_BO_138
	/**
	 * Payment greater than Threshold Amount. Its OK with no EX_ prefix, that
	 * prefix is inserted automatically by Calypso when a BOException is created
	 */
	STOP_PAYMENT_THRESHOLD(
			"EX_PAYMENT_THRESHOLD",
			"Settlement amount greater than Threshold Amount.",
			null),

	EX_ACC_IO_TECHNICAL_EXCEPTION(
			"EX_ACC_IO_TECHNICAL_EXCEPTION",
			"Generic input / output technical exception",
			ErrorCodeEnum.UndefinedException),

	EX_ACC_CRE_BUILD(
			"EX_ACC_CRE_BUILD",
			"Cre was not sent due to a build error",
			ErrorCodeEnum.UndefinedException),

	EX_ACC_CRE_WRITTING(
			"EX_ACC_CRE_WRITTING",
			"Cre was not sent due a writting error",
			ErrorCodeEnum.UndefinedException),

	// CAL_WP_039
	EX_MSG_SBP_NACKED("EX_MSG_SBP_NACKED", "Nack received from SBP", null),

	// CAL_WP_039
	EX_SBP_SDI("EX_SBP_SDI", "Wrong SBP Incoming Message", null),

	// CAL_153_
	REFIX_AFTER_SETTLE_DATE(
			"EX_REFIX_AFTER_SETTLE_DATE",
			"There has been a Refixing event afer SettleDate",
			null),

	// CAL_462_
	EX_CPTY_DISAGREE("EX_CPTY_DISAGREE", "Cpty Disagreement", null),

	// CAL_462_
	EX_TECHNICAL_CPTY_DISAGREE(
			"EX_TECH_CPTY_DISAGREE",
			"Cpty Disagreement",
			null),

	/** Nack from CM T99A */
	CMT99A_NACKED("EX_CMT99A_NACKED", "Nack received from CMT99A", null),

	CMT99A_ERROR_SENDING(
			"EX_CMT99A_ERROR_SENDING",
			"Error sending CMT99A Transfer message to Queue",
			null),
	CMT99A_ERROR_BUILDING_MSG(
			"EX_CMT99A_ERROR_BUILDING_MSG",
			"Error building CMT99A Transfer message",
			null);

	private String description;
	private ErrorCodeEnum errorCodeEnum;

	protected String type;

	SantExceptionType(final String type, final String description,
                      final ErrorCodeEnum errorCodeEnum) {
		this.type = type;
		this.description = description;
		this.errorCodeEnum = errorCodeEnum;
	}

	/**
	 * returns the description.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * returns the errorCodeEnum.
	 *
	 * @return the errorCodeEnum
	 */
	public ErrorCodeEnum getErrorCodeEnum() {
		return errorCodeEnum;
	}

	/**
	 * returns the type.
	 *
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * set the description.
	 *
	 * @param description
	 *            the description to set
	 */
	public void setDescription(final String description) {
		this.description = description;
	}

	/**
	 * set the errorCodeEnum.
	 *
	 * @param errorCodeEnum
	 *            the errorCodeEnum to set
	 */
	public void setErrorCodeEnum(final ErrorCodeEnum errorCodeEnum) {
		this.errorCodeEnum = errorCodeEnum;
	}

	/**
	 * set the type.
	 *
	 * @param type
	 *            the type to set
	 */
	public void setType(final String type) {
		this.type = type;
	}
}