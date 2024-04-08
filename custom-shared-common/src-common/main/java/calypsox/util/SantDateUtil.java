package calypsox.util;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Util;

import java.util.Optional;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class SantDateUtil {

    public static final Vector<String> SYSTEM_DEFAULT_HOLIDAYS= Util.string2Vector("SYSTEM");

    public static JDate addBusinessDays(JDate date, int days){
        return Optional.ofNullable(date).map(d->d.addBusinessDays(days,SYSTEM_DEFAULT_HOLIDAYS))
                .orElse(date);
    }
}
