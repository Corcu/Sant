package calypsox.engine.gestorstp;

import com.calypso.tk.bo.swift.SwiftMessage;

/**
 * Utility class to import messages from GestorSTP
 *  
 * @author Carlos Cejudo
 *
 */
public class GestorSTPUtil {
	
	private static final String SWIFT_MESSAGE_END_OF_LINE_INDICATOR = "[__END_OF_LINE_INDICATOR__]";
	
	public static String fixSwiftEndOfLineCharacters(final String originalMessage) {
		String fixedMessage = originalMessage;
		
		// Replace all possible end of line character combinations with a token
		fixedMessage = fixedMessage.replace("\r\n", SWIFT_MESSAGE_END_OF_LINE_INDICATOR);
		fixedMessage = fixedMessage.replace("\n\r", SWIFT_MESSAGE_END_OF_LINE_INDICATOR);
		fixedMessage = fixedMessage.replace("\n", SWIFT_MESSAGE_END_OF_LINE_INDICATOR);
		
		// Replace all tokens with a valid end of line for SWIFT
		fixedMessage = fixedMessage.replace(SWIFT_MESSAGE_END_OF_LINE_INDICATOR, SwiftMessage.END_OF_LINE);
		
		return fixedMessage;
	}

}
