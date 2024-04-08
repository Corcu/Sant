package calypsox.tk.util;

import com.calypso.tk.core.CalypsoException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTaskPROP_RATE_1BUSDAY;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Copy rates - Num Of Bus Days can be set to 0 (copy same date rates between Quote sets)
 *
 * @author x865229
 * date 20/01/2023
 * @see com.calypso.tk.util.ScheduledTaskPROP_RATE_1BUSDAY
 */
public class ScheduledTaskSANT_PROP_RATE extends ScheduledTaskPROP_RATE_1BUSDAY {
    private static final long serialVersionUID = 4198600620574167417L;

    private transient int numDays;
    private transient JDate nextBusDay;

    @Override
    protected boolean handlePropagateRate(DSConnection ds, PSConnection ps) throws CalypsoException {

        String numDaysStr = getAttribute(NUMOFBUSDAYS);

        if (!Util.isEmpty(numDaysStr)) {
            numDays = Integer.parseInt(numDaysStr);
            setAttribute(NUMOFBUSDAYS, standardBusDays() ? numDaysStr : "1");
        }

        return super.handlePropagateRate(ds, ps);
    }


    @Override
    @SuppressWarnings("rawtypes")
    protected Vector processQuoteValuesValDate(Vector quoteValueV) {
        setNonStandardNextBusDay();
        return super.processQuoteValuesValDate(quoteValueV);
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected Vector processQuoteValuesNextBusDay(Vector quoteValueV, Vector quoteValueNextBusDayV) {
        if (setNonStandardNextBusDay()) {
            try {
                if (!Util.isEmpty(getAttribute(INCLUDEQUOTENAMESLIKE))) {
                    String destQuoteSetName = getAttribute(DESTQUOTESET);
                    String likeSelection = buildLikeSelection();
                    quoteValueNextBusDayV = DSConnection.getDefault().getRemoteMarketData()
                            .getQuoteValues("quote_set_name =" + Util.string2SQLString(destQuoteSetName) +
                                    " AND quote_date = " + Util.date2SQLString(nextBusDay) +
                                    " AND (" + likeSelection + ")");
                } else {
                    String sourceQuoteSetName = getAttribute(SOURCEQUOTESET);
                    quoteValueNextBusDayV = DSConnection.getDefault().getRemoteMarketData()
                            .getQuoteValues(nextBusDay, sourceQuoteSetName);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return super.processQuoteValuesNextBusDay(quoteValueV, quoteValueNextBusDayV);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Vector getAttributeDomain(String attr, Hashtable currentAttr) {
        Vector domain = super.getAttributeDomain(attr, currentAttr);
        if (NUMOFBUSDAYS.equals(attr)) {
            domain.insertElementAt("0", 0);
        }
        return domain;
    }

    private String buildLikeSelection() {
        String attribute = getAttribute(INCLUDEQUOTENAMESLIKE);
        ArrayList<String> includePatterns = Util.stringToCollection(new ArrayList<>(), attribute, "|", false);
        StringBuilder likeSelection = new StringBuilder();

        for (String pattern : includePatterns) {
            String includePattern = pattern.replaceAll("[*]", "%");
            if (!Util.isEmpty(likeSelection.toString())) {
                likeSelection.append(" OR ");
            }

            likeSelection
                    .append("lower(quote_name) LIKE '")
                    .append(includePattern.toLowerCase())
                    .append("'");
        }
        return likeSelection.toString();
    }


    private boolean setNonStandardNextBusDay() {
        JDatetime valDatetime = getValuationDatetime();
        JDate valDate = JDate.valueOf(valDatetime, getTimeZone());
        nextBusDay = valDate.addBusinessDays(numDays, getHolidays());
        setNextBusDayInternal(nextBusDay);
        return !standardBusDays();
    }

    private boolean standardBusDays() {
        return numDays >= 1 && numDays <= 2;
    }

    private void setNextBusDayInternal(JDate nextBusDay) {
        try {
            Field field = ScheduledTaskPROP_RATE_1BUSDAY.class.getDeclaredField("_nextBusDay");
            field.setAccessible(true);
            field.set(this, nextBusDay);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
