package calypsox.tk.util;

import java.util.Vector;

import calypsox.ErrorCodeEnum;

/**
 * this class is used to be able to send an error code to control - m from any
 * scheduled task. See that the attributes are static. This can work because
 * each scheduled task is running in its own java virtual machine
 */
public class ControlMErrorLogger {
    public static ErrorCodeEnum getErrorCode() {
	return errorCode;
    }

    public static Vector<String> getErrorDescription() {
	return errorDescription;
    }

    public static void addError(ErrorCodeEnum code, String description) {
	// we can log multiple errors descriptions but the code will be the
	// initial one
	if (errorCode == null) {
	    ControlMErrorLogger.errorCode = code;
	}
	if (code != null) {
	    ControlMErrorLogger.errorDescription.add(description);
	}
    }

    public static void addError(ErrorCodeEnum code, String[] args) {
	if (code != null) {
	    String description = code.getFullTextMesssage(args);
	    addError(code, description);
	}
    }

    public static void sendMessageToControlM() {
	for (String desc : errorDescription) {
	    System.out.println(desc);
	}
    }

    private static ErrorCodeEnum errorCode = null;
    private static Vector<String> errorDescription = new Vector<String>();

}
