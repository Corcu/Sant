/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.engine.advice;

import com.calypso.engine.advice.SenderEngine;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;

import java.util.Locale;

public class SBWOSenderEngine extends SenderEngine {

    public static void main(String[] args) {
        SBWOSenderEngine en = new SBWOSenderEngine(null, null, 0);
        en.preRestart();
    }

    public SBWOSenderEngine(DSConnection dsconnection, String s, int i) {
        super(dsconnection, s, i);
        /* This access throws a compiling warning
        String language = AccessController.doPrivileged(new GetPropertyAction("user.language", "en"));
        String country = AccessController.doPrivileged(new GetPropertyAction("user.country", ""));
        */
        String language = System.getProperty("user.language", "en");
        String country = System.getProperty("user.country", "");
        if (!Util.isEmpty(language) && !Util.isEmpty(country)) {
            Log.system(
                    SBWOSenderEngine.class.getName(),
                    "Setting Locale for "
                            + SBWOSenderEngine.class.getName()
                            + ": "
                            + language
                            + "_"
                            + country);
            Locale locale = new Locale(language, country);
            Locale.setDefault(locale);
        } else {
            // hardcoded for SBWOSenderEngine, to remove when tested
            Locale locale = new Locale("en", "US");
            Locale.setDefault(locale);
        }
        Log.system(
                SBWOSenderEngine.class.getName(),
                "Setting default Locale for "
                        + SBWOSenderEngine.class.getName()
                        + ": "
                        + Locale.getDefault());
    }

    @Override
    public String getEngineName() {
        return "SBWOSenderEngine";
    }
}
