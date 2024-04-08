package calypsox.tk.bo.cremapping.event;

import calypsox.tk.bo.cremapping.util.BOCreConstantes;
import calypsox.tk.bo.cremapping.util.BOCreUtils;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;
import java.util.TimeZone;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

public class BOCreSecLendingCOLLATERAL extends BOCreSecLending {
    public BOCreSecLendingCOLLATERAL(BOCre cre, Trade trade) {
        super(cre, trade);
    }


    public void fillValues() {
       super.fillValues();
       this.internal = BOCreUtils.getInstance().isInternal(this.trade);
       this.sentDateTime = JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault());
    }

    @Override
    protected Double getPosition() {
        final Double creAmount = getCreAmount();
        final Double cashPosition = getCashPosition();
        final Double boCresAmount = getBOCresAmount();
        getInstance().generatePositionLog(this.boCre,this.tradeId,cashPosition,boCresAmount ,creAmount );
        return null!=cashPosition && null!=boCresAmount ? cashPosition + boCresAmount + creAmount : 0.0;
    }

    @Override
    public Double getCashPosition() {
        JDate valDate = null;
        valDate = getInstance().addBusinessDays(this.boCre.getEffectiveDate(),-1);
        return getInstance().getInvLastCashPosition(this.collateralConfig,this.trade, BOCreConstantes.DATE_TYPE_TRADE,BOCreConstantes.THEORETICAL,valDate);
    }

    @Override
    protected String buildWhere(){
        JDate valDate = this.boCre.getTradeDate();
        if(null!=this.collateralConfig ){
            return getInstance().buildWhere(this.boCre.getEventType(), this.collateralConfig.getId(),valDate);
        }
        return "";
    }

    @Override
    protected String loadProductType() {
        return "SecLending";
    }

    @Override
    protected Double getCreAmount() {
        try {
            BOTransfer boTransfer = DSConnection.getDefault().getRemoteBO().getBOTransfer(boCre.getTransferLongId());
            if (Optional.ofNullable(boTransfer).isPresent() && boTransfer.getTradeDate().before(boCre.getEffectiveDate())) {
                return 0.0;
            }
        } catch (CalypsoServiceException e) {
            Log.error(this.getClass().getSimpleName(), "Cant retrieve any transfer with id: " + boCre.getTransferLongId());
        }
        return this.amount1;
    }

    protected Double getBOCresAmount() {
        JDate effectiveDate = this.boCre.getEffectiveDate();
        String from = getInstance().buildFrom(this.boCre.getEventType()).concat(", bo_transfer");
        String where = buildWhere().concat("and bo_cre.transfer_id = bo_transfer.transfer_id and trunc(bo_transfer.TRADE_DATE) >= " + Util.date2SQLString(effectiveDate));
        return getInstance().getBOCresAmount(from, where, this.boCre, false);
    }
}
