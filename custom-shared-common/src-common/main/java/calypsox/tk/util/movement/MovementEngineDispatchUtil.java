package calypsox.tk.util.movement;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Vector;

/**
 * @author aalonsop
 */
public class MovementEngineDispatchUtil {

    private static final String DV_MOVEMENT_SENDER_ENGINE_NUMBER = "MOV_SENDER_MODULE_VALUE";
    private static final String MOV_SENDER_ENGINE_NAME = "MovementSenderEngine";

    private MovementEngineDispatchUtil() {
    }

    public static MovementEngineDispatchUtil getInstance() {
        return new MovementEngineDispatchUtil();
    }

    /**
     * @return
     */
    public int getTotalMovementSenderEnginesNumber() {
        Vector<String> dvs = LocalCache.getDomainValues(DSConnection.getDefault(), DV_MOVEMENT_SENDER_ENGINE_NUMBER);
        int movSenderEngineModuleValue = 0;
        if (!Util.isEmpty(dvs)) {
            try {
                movSenderEngineModuleValue = Integer.parseInt(dvs.get(0));
            } catch (NumberFormatException exc) {
                Log.error(EventFilter.class, "Couldn't parse " + DV_MOVEMENT_SENDER_ENGINE_NUMBER + " domainValue as an INT");
            }
        }
        return movSenderEngineModuleValue;
    }

    public long getInstanceNumberToDispatch(long tradeId, int numberOfMovementSenderInstances) {
        return tradeId % numberOfMovementSenderInstances;
    }

    public int getEngineInstanceId(String engineName) {
        String rawIdFromName = getIdFromEngineName(engineName);
        char charId = getCharFromEngineNameId(rawIdFromName);
        return getCharIntegerValue(charId);
    }

    private String getIdFromEngineName(String engineName) {
        return engineName.replaceAll(MOV_SENDER_ENGINE_NAME, "").toLowerCase();
    }

    private char getCharFromEngineNameId(String engineNameId) {
        char res = 'a';
        if (!Util.isEmpty(engineNameId)) {
            if (engineNameId.length() > 1) {
                String croppedString = engineNameId.substring(engineNameId.length() - 1);
                if (!Util.isEmpty(croppedString)) {
                    res = croppedString.charAt(0);
                }
            } else {
                res = engineNameId.charAt(0);
            }
        }
        return res;
    }

    /**
     * @param letter
     * @return ASCII char integer value minus 'a' value, this is the char's English alphabet position
     */
    private int getCharIntegerValue(char letter) {
        char aChar = 'a';
        char oneChar = '1';
        if (Character.isAlphabetic(letter)) {
            return letter - aChar;
        } else {
            return letter - oneChar;
        }
    }
}
