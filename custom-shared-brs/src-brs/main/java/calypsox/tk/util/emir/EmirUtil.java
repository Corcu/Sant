package calypsox.tk.util.emir;

import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Vector;

public class EmirUtil {


    /** The Constant EMPTY . */
    private static final String EMPTY = "";

    /** The Constant DV_SOURCE_SYSTEM_CODE . */
    public static final String DV_EMIR_SOURCE_SYSTEM_CODE = "EMIR_SOURCE_SYSTEM_CODE";

    /**
     * getSourceSystem.
     *
     * @param  po
     *
     * @return String
     */
    public static String getSourceSystem(String po) {
        String result = EMPTY;

        final Vector<String> dvSourceSystemTable =
                castVector(
                        String.class,
                        LocalCache.getDomainValues(DSConnection.getDefault(),
                                DV_EMIR_SOURCE_SYSTEM_CODE));

        if (dvSourceSystemTable.contains(po)) {
            result = LocalCache.getDomainValueComment(
                    DSConnection.getDefault(), DV_EMIR_SOURCE_SYSTEM_CODE, po);
        }

        return result;
    }

    /**
     * Method for castting to Vector<clazz>.
     *
     * @param <T>
     *            Class<T>
     * @param clazz
     *            Class<T>
     * @param c
     *            Vector<T>
     * @return Vector<clazz>
     */
    private static  <T> Vector<T> castVector(final Class<? extends T> clazz,
                                             final Vector<?> c) {
        final Vector<T> r = new Vector<T>(c.size());
        for (final Object o : c) {
            r.add(clazz.cast(o));
        }
        return r;
    }

}
