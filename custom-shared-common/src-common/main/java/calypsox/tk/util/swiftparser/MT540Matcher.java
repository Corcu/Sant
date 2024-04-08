package calypsox.tk.util.swiftparser;

import calypsox.tk.util.swiftparser.ccp.PlatformMatcher;
import calypsox.tk.util.swiftparser.ccp.PlatformMatcherFactory;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;

import java.util.Arrays;
import java.util.Vector;

/**
 * MT540Matcher
 *
 * @author Ruben Garcia
 */
public class MT540Matcher extends com.calypso.tk.util.swiftparser.MT540Matcher {

    private static final String[] ACCEPTED_INDICATORS = {"ALTA", "ALTB", "ALTC"};

    @Override
    public Object index(ExternalMessage swiftMess, PricingEnv env, DSConnection ds, Object dbCon, Vector errors) throws MessageParseException {
        Object obj = super.index(swiftMess, env, ds, dbCon, errors);

        if (obj == null) {
            PlatformMatcher matcher = PlatformMatcherFactory.getInstance(swiftMess);
            if (matcher != null) {
                obj = matcher.index(swiftMess, env, ds, dbCon, errors);
            }
        }

        return obj;
    }

    @Override
    public boolean match(ExternalMessage externalMessage, Object object, BOMessage indexedMessage, BOTransfer indexedTransfer, PricingEnv env, DSConnection ds, Object dbCon, Vector errors) throws MessageParseException {
        SwiftMessage msg = (SwiftMessage) externalMessage;
        if (hasIndicator(msg))
            return matchGeneric(externalMessage, object, indexedMessage, indexedTransfer, env, ds, dbCon, errors, "MT540");

        return super.match(externalMessage, object, indexedMessage, indexedTransfer, env, ds, dbCon, errors);
    }

    private boolean hasIndicator(SwiftMessage msg) {
        SwiftFieldMessage swiftField = msg.getSwiftField(":22F:", "SETR", null);
        return swiftField != null && Arrays.stream(ACCEPTED_INDICATORS).anyMatch(i -> swiftField.getValue().endsWith("/" + i));
    }
}
