package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreConstantes;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import java.util.Optional;
import java.util.TimeZone;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

public class BOCreSecLending extends BOCreMarginCall {

    public BOCreSecLending(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected Double getPosition() {
        //esperar dos segundos antes del cálculo del saldo.... (-_-)
        JDate jDate = getInstance().getActualDate();
        if(BOCreConstantes.COT_REV.equalsIgnoreCase(eventType)){
            if(this.trade.getSettleDate().before(jDate)){
                doSleep();
            }
        }else if(BOCreConstantes.COT.equalsIgnoreCase(eventType)
                && this.trade.getTradeDate().getJDate(TimeZone.getDefault()).before(jDate)){
            doSleep();
        }

        final Double creAmount = getCreAmount();
        final Double cashPosition = getCashPosition();
        final Double boCresAmount = getBOCresAmount();
        generatePositionLog(cashPosition,boCresAmount ,creAmount );
        return null!=cashPosition && null!=boCresAmount ? cashPosition + boCresAmount + creAmount : 0.0;
    }


    @Override
    protected Account getAccount() {
        return super.getAccount();
    }

    @Override
    public CollateralConfig getContract() { //change
        if(null!=this.trade && this.trade.getProduct() instanceof SecLending){
            Integer contractId = ((SecLending) this.trade.getProduct()).getMarginCallContractId(this.trade);
            return CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), contractId);
        }
        return null;
    }

    private void doSleep(){
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Log.error(this,e);
        }
    }


}
