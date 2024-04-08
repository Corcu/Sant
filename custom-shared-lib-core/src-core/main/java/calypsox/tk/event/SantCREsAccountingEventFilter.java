package calypsox.tk.event;

import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.JDate;
import com.calypso.tk.event.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;


import java.text.SimpleDateFormat;
import java.util.Vector;

/**
 * Event Filter - to filter out Trades defined in the domain 'SantExcludeTrades.ProductTypes'
 *
 * @author Soma
 */
public class SantCREsAccountingEventFilter implements EventFilter {

    public static final String MARGIN_SECURITY_POSITION = "MARGIN_SECURITY_POSITION";
    public static final String CRE_REVERSAL = "REVERSAL";
    public static final String CRE_NEW = "NEW";
    public static final String DOMAIN_TIME_STAMP = "SantCREsAccounting.TimeStamp";//limit of performing hour

    @Override
    public boolean accept(final PSEvent event) {

        Vector<String> hourLimitVector = LocalCache
                .getDomainValues(DSConnection.getDefault(), DOMAIN_TIME_STAMP);

        if (event instanceof PSEventCre) {
            PSEventCre et = (PSEventCre) event;
            BOCre boCre = et.getBOCre();

            Vector<String> hol = null;

            if ((null == hol) || hol.isEmpty()) {
                hol = new Vector<String>();
                hol.add("SYSTEM");
            }

            SimpleDateFormat SDF = new SimpleDateFormat(
                    "yyyy-MM-dd-HH.mm.ss");

            String timestamp = SDF.format(JDate.getNow().getNow(null));

            //Two hours should be added because of the behavior of the event server
            int actualHour =  (getActualHour(timestamp))+(2);
            int hourLimit =  Integer.parseInt(hourLimitVector.get(0));

            if (MARGIN_SECURITY_POSITION.equalsIgnoreCase(boCre.getEventType())) {

                //filter for CRE types NEW
                if (CRE_NEW.equalsIgnoreCase(boCre.getCreType())){
                    if(actualHour<22) {
                        if (boCre.getEffectiveDate().after(JDate.getNow())) {
                            return false;
                        }
                    }else {
                        if (boCre.getEffectiveDate().after(JDate.getNow().addDays(1))) {
                            return false;
                        }
                    }
                }

                //filter for CRE types REVERSAL
                if (CRE_REVERSAL.equalsIgnoreCase(boCre.getCreType())) {

                    if(boCre.getEffectiveDate().after(JDate.getNow())){
                        return false;
                    }
                    if(boCre.getEffectiveDate().equals(JDate.getNow())
                            &&actualHour<hourLimit){
                        return false;
                    }
                }
            }

        }

        return true;
    }

    public int getActualHour(String time){

        String hourT =  time.substring(11,13);

        return Integer.parseInt(hourT);
    }

}