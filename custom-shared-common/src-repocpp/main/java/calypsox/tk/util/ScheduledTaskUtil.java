package calypsox.tk.util;

import com.calypso.tk.util.ScheduledTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.context.i18n.LocaleContextHolder.getTimeZone;

public class ScheduledTaskUtil {

    public static Map<String, String> getValueMap(ScheduledTask task) {
        Map<String, String> valueMap = new HashMap<>();
        Date vald = task.getValuationDatetime().getJDate(getTimeZone()).getDate();
        valueMap.put("VALUATION_DATE_YYYYMMDD", (new SimpleDateFormat("yyyyMMdd")).format(vald));
        valueMap.put("VALUATION_DATE_DDMMYYYY", (new SimpleDateFormat("ddMMyyyy")).format(vald));
        valueMap.put("VALUATION_DATE_DDMMYY", (new SimpleDateFormat("ddMMyy")).format(vald));
        valueMap.put("VALUATION_DATE_YYYY-MM-DD", (new SimpleDateFormat("yyyy-MM-dd")).format(vald));
        valueMap.put("VALUATION_DATE_DD-MM-YYYY", (new SimpleDateFormat("dd-MM-yyyy")).format(vald));
        valueMap.put("CURRENT_TIME_HHMMSS", (new SimpleDateFormat("HHmmss")).format(new Date()));
        return valueMap;
    }
}
