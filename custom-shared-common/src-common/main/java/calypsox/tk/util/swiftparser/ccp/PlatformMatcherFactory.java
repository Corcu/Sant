package calypsox.tk.util.swiftparser.ccp;

import calypsox.tk.util.swiftparser.MT540_3Matcher;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.CrestUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

/**
 * PlatformMatcherFactory build the MT54X platform matcher by swift message fields
 *
 * @author Ruben Garcia
 */
public class PlatformMatcherFactory {

    /**
     * Get the MT54X platform matcher instance
     * SENAF: :94B::TRAD//EXCH/XNAF
     * SEND: :94B::TRAD//EXCH/SEND
     * STRIPS: :22F::SETR/IBRC/CPST or :22F::SETR/IBRC/CPRT
     * MTS: default
     *
     * @param externalMessage the incoming MT54X
     * @return the MT54X platform matcher instance
     */
    public static PlatformMatcher getInstance(ExternalMessage externalMessage) {
        if (externalMessage instanceof SwiftMessage) {
            SwiftMessage swift = (SwiftMessage) externalMessage;
            SwiftFieldMessage field = swift.getSwiftField(":94B:", "TRAD//EXCH", "XNAF");
            if (field != null) {
                return new SENAFMatcher();
            }
            field = swift.getSwiftField(":94B:", "TRAD//EXCH", "SEND");
            if (field != null) {
                return new SENDMatcher();
            }
            Vector<String> eurexCpty = LocalCache.getDomainValues(DSConnection.getDefault(), "EurexCounterpartiesMatcher");
            String cpty = getEurexCpty(swift);
            if (eurexCpty.contains(cpty)) {
                return new EurexMatcher();
            }
            field = swift.getSwiftField(":22F:", "SETR/IBRC", "CPST");
            if (field != null) {
                return new STRIPSMatcher();
            }
            field = swift.getSwiftField(":22F:", "SETR/IBRC", "CPRT");
            if (field != null) {
                return new STRIPSMatcher();
            }

            if (("MT540".equals(externalMessage.getType()) || "MT541".equals(externalMessage.getType()))
                    && hasCode(swift, ":22F:", "SETR/IBRC", Arrays.asList("ALTA", "ALTB", "ALTC"))) {
                return new MT540_3Matcher();
            }

            return new MTSMatcher();
        }
        return null;
    }

    private static boolean hasCode(SwiftMessage swift, String tag, String qualifier, List<String> codes) {
        return codes.stream().anyMatch(c -> swift.getSwiftField(tag, qualifier, c) != null);
    }

    private static String getEurexCpty(SwiftMessage swiftMessage){
        String fieldValue = CrestUtil.getSwiftFieldValue(swiftMessage.getFields(),
                ":94B:", "TRAD//EXCH", null);
        if (fieldValue == null){
            return "";
        }
        if (fieldValue.indexOf("/") > 0){
            return fieldValue.substring(fieldValue.indexOf("/")+1);
        } else {
            return fieldValue;
        }
    }
}
