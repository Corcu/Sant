package calypsox.util;

import com.calypso.tk.core.Util;

import java.util.Collection;

/**
 * @author aalonsop
 */
public class SantReportFormattingUtil {

    /*
     * Not a singleton pattern. This must return a new instance each time to not impact performance
     */
    public static SantReportFormattingUtil getInstance() {
        return new SantReportFormattingUtil();
    }

    /**
     *
     */
    private SantReportFormattingUtil() {

    }

    /**
     * @param collection
     * @return
     */
    public Object formatEmptyCollectionForReporting(Object value) {
        Object res = value;
        if (value instanceof Collection && Util.isEmpty((Collection) value)) {
            res = "";
        }
        return res;
    }


}
