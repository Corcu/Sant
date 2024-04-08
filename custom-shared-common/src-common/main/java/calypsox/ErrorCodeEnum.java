package calypsox;

import java.text.MessageFormat;

public enum ErrorCodeEnum {

	NoError(0, "Success"),
	ScheduledTaskRunnerParamError(1, "ScheduledTaskRunner wrong parameters"),
	ScheduledTaskNotFound(2, "ScheduledTask external reference is not configured"),
	ScheduledTaskFailure(3, "Scheduled completed but failed"),
	ScheduledTaskConfigError(4, "Scheduled task configuration error "),
	ScheduledTaskUnexpectedException(5, "Scheduled task unexpected exception: {0} : {1}"),
	OutOfMemoryError(6, "java.lang.OutOfMemoryError: exiting..."),
	InputFileEmpty(10, "Input file ''{0}'' is empty"),
	InputFileNotFound(11, "Input file not found ''{0}''"),
	InputFileCanNotBeRead(12, "Input file can not be read ''{0}''"),
	InputXMLFileBadFormat(13, "Input file is not a well-formatted XML file ''{0}'': {1}"),
	InputXMLFileCanNotBeParsed(14, "Input XML file contain a non-expected format: ''{0}'': {1}"),
	OutputCVSFileCanNotBeWritten(15, "Output file can not be write: ''{0}''"),
	InputFileInvalidFormat(16, "Input file contains an invalid format: ''{0}''"),
	InputFileCanNotBeMoved(17, "Input file can not be moved {0}"),
	InputFileCanNotBeClosed(18, "Input file can not be closed {0}"),
	IOException(19, "Input/Output exception: {0}"),
	UndefinedException(20, "Exception: {0} : {1}"),
	LogException(21, "Exception occurred in log treatment during the import process."),
	ConnectException(30, "Error connecting to DataServer: {0}"),
	DataserverError(31, "Dataserver error: {0}"),
	InvalidData(40, "Invalid data {0}"),
	StaticData(41, "Invalid static data {0}"),
	MailSending(42, "Mail can't be sent"),
	ControlLine(43, "Invalid date or control digit in the control line"),
	PricingEnviroment(44, "Error obtening Pricing Enviroment"),
	MoveXmlFile(45, "Error moving xml file."),
	FileNameDate(46, "Error while looking for the date in the filename");

	private int code;
	private String message;

	/** get the message for this error using params */
	public String getFullTextMesssage(final String[] params) {
		final MessageFormat messageFormat = new MessageFormat(this.message);
		return messageFormat.format(params);
	}

	private ErrorCodeEnum(final int code, final String message) {
		this.code = code;
		this.message = message;
	}

	public int getCode() {
		return this.code;
	}

	public void setCode(final int code) {
		this.code = code;
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}
}
