
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Vector;

public class EmirFieldBuilderEXECUTIONVENUEMICCODE implements EmirFieldBuilder {
    /*
    "Nueva logica.
     Si la contrapartida es una camara, se revisa el atributo EMIR_Clearing_House para ver si la contrapartida esta definida como camara. En caso afirmativo, se tiene que informar el valor del atributo ""EMIR_ClearingHouseCode"" asociado a la misma (por ejemplo para la camara LCH (2Z1L) se informa el valor XLCH).
     Si la contrapartida no es camara, entonces hacemos lo siguiente:

        Si la keyword “EMIR_PRODUCT_TOTV”  tiene el valor ""true"" y la keyword “EMIR_MIC_CODE” no esta vacia, entonces enviamos el valor de la keyword “EMIR_MIC_CODE”
        Si la keyword “EMIR_PRODUCT_TOTV”  tiene el valor ""true"" y la keyword “EMIR_MIC_CODE” esta vacia, entonces enviamos el valor ""XOFF""
        - Si la keyword “EMIR_PRODUCT_TOTV”  tiene  valor distinto de""true"", entonces enviamos el valor ""XXXX"".



Pte de definir si se va a crear una lista en los DomainValue con el listado de camaras y su valores o se van a dar de alta nuevos atributos a nivel de contrapartida
"

    */

    @Override
    public String getValue(Trade trade) {

        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
        if (trade.getCounterParty() != null) {
            Vector<String> clearingHouses =  LocalCache.getDomainValues(DSConnection.getDefault(), EmirSnapshotReduxConstants.DV_EMIR_CLEARING_HOUSE);
            if (!Util.isEmpty(clearingHouses)) {
                if (clearingHouses.contains(trade.getCounterParty().getCode())) {
                    rst = LocalCache.getDomainValueComment(DSConnection.getDefault(),
                            EmirSnapshotReduxConstants.DV_EMIR_CLEARING_HOUSE, trade.getCounterParty().getCode());
                }
                if (!clearingHouses.contains(trade.getCounterParty().getCode())) {
                    if (!"true".equalsIgnoreCase(trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_PRODUCT_TOTV)))  {
                        rst = EmirSnapshotReduxConstants.XXXX;
                    } else  {
                        if (Util.isEmpty(trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_MIC_CODE)))  {
                            rst = EmirSnapshotReduxConstants.XOFF;
                        } else {
                            rst = trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_MIC_CODE);
                        }
                    }
                }
            }
       }

       if (Util.isEmpty(rst)) {
            rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
       }
        return rst;
    }

}
