package calypsox.util;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOPosting;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.service.DSConnection;

import java.util.TimeZone;

public class OBBReportUtil {

    /**
     * Get firts woking date of a month.
     * Default Holidays "SYSTEM"
     * @param jdate
     * @return
     */
    public static synchronized boolean isFirstWorkingDateOfMonth(JDate jdate){
        if(null!=jdate){
            final JDate prevBusDate = DateUtil.getPrevBusDate(jdate, Util.string2Vector("SYSTEM"));
            return prevBusDate.getMonth()<jdate.getMonth();
        }
        return false;
    }

    /**
     * Get firts woking date of a month.
     * Default Holidays "SYSTEM"
     * @param jdate
     * @return
     */
    public static synchronized boolean isLastWorkingDateOfMonth(JDate jdate){
        if(null!=jdate){
           return jdate.addBusinessDays(1,Util.string2Vector("SYSTEM")).getMonth()>jdate.getMonth();
        }
        return false;
    }

    /**
     * @param credit_debit
     * @return
     */
    public static synchronized String getSing(String credit_debit){
        switch (credit_debit){
            case "Debit":
                return "D";
            case "Credit":
                return "H";
            default:
                return "";
        }
    }

    /**
     * @param subAccount
     * @return
     */
    public static synchronized String getNDir(String subAccount){
        if(Util.isEmpty(subAccount)){
            return "2";
        }else{
            return "4";
        }
    }

    public static Account getAccount(String creditDebit, BOPosting posting){
        if(null!=posting){
            int accountID = 0;
            if("Credit".equalsIgnoreCase(creditDebit)){
                accountID = posting.getCreditAccountId();
            }else if("Debit".equalsIgnoreCase(creditDebit)){
                accountID = posting.getDebitAccountId();
            }
            if(accountID > 0){
                try {
                    return DSConnection.getDefault().getRemoteAccounting().getAccount(accountID);
                } catch (CalypsoServiceException e) {
                    Log.error(OBBReportUtil.class.getName(),"Error loading Account: " + accountID + " "+ e.getCause());
                }
            }
        }

        return null;
    }


    public static String getAccountTypeValue(Account account){
        if(null!=account){
            return null!= account.getAccountProperty("AccountType") ? account.getAccountProperty("AccountType") : "";
        }
        return "";
    }

    public static String getAccountPosicionValue(Account account){
        if(null!=account){
            return null!= account.getAccountProperty("Posicion") ? account.getAccountProperty("Posicion") : "";
        }
        return "";
    }

}
