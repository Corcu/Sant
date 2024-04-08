package calypsox.tk.upload.validator;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.refdata.CurrencyPair;
import com.calypso.tk.service.DSConnection;

public class ValidatorUtils {

    protected static boolean existCurrencyPair(String primaryCcy, String secondaryCcy) {
        CurrencyPair pair = null;
        try {
            pair = DSConnection.getDefault().
                    getRemoteReferenceData().getCurrencyPair(primaryCcy, secondaryCcy);
            return pair != null && pair.getPrimaryCode().equals(primaryCcy)
                    && pair.getQuotingCode().equals(secondaryCcy);
        } catch (CalypsoServiceException e) {
            throw new RuntimeException(e);
        }
    }

    protected static void createCurrencyPair(String primaryCcy, String secondaryCcy, DSConnection dsCon) {
        //We try to obtain the opposite ccy pair
        if (existCurrencyPair(secondaryCcy, primaryCcy)) {
            try {
                CurrencyPair pair = (CurrencyPair) dsCon.getRemoteReferenceData().
                        getCurrencyPair(secondaryCcy, primaryCcy).clone();
                pair.setPrimaryCode(primaryCcy);
                pair.setQuotingCode(secondaryCcy);
                pair.setIsPairPositionRefB(false);
                dsCon.getRemoteReferenceData().save(pair);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
