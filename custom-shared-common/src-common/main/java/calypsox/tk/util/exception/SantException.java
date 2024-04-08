package calypsox.tk.util.exception;

import com.calypso.tk.bo.Task;

public class SantException extends Throwable {

	private static final long serialVersionUID = 5109839191574115964L;

	private final String taskEvenType;
	private final String source;

	SantException(SantExceptionType exceptionType, String source, String message) {

		super(message);
		this.taskEvenType = exceptionType._type;
		this.source = source;
	}

	SantException(SantExceptionType exceptionType, String source, String message, Throwable throwable) {

		super(message, throwable);
		this.taskEvenType = exceptionType._type;
		this.source = source;
	}

	SantException(SantExceptionType exceptionType, String source, Throwable throwable) {

		super(throwable);
		this.taskEvenType = exceptionType._type;
		this.source = source;
	}

	public String getTaskEventClass() {
		return Task.EXCEPTION_EVENT_CLASS;
	}

	public String getTaskEventType() {
		return this.taskEvenType;
	}

	public String getSource() {
		return this.source;
	}
}
