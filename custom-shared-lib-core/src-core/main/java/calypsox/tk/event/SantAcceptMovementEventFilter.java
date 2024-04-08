package calypsox.tk.event;

import calypsox.tk.util.movement.MovementEngineDispatchUtil;
import calypsox.util.SantDomainValuesUtil;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventMessage;
import com.calypso.tk.event.SenderEngineEventFilter;

import java.util.List;

public class SantAcceptMovementEventFilter extends SenderEngineEventFilter {


    /**/
    @Override
    public boolean accept(PSEvent psevent) {
        if ((psevent instanceof PSEventMessage)) {
            PSEventMessage pseventmessage = (PSEventMessage) psevent;
            BOMessage boMessage = pseventmessage.getBoMessage();

            List<String> domValues = CollateralUtilities
                    .getDomainValues(SantDomainValuesUtil.MOV_SENDER_ENG_MSG_TYPE);
            String msgType = boMessage.getMessageType();
            MovementEngineDispatchUtil dispatchUtil = MovementEngineDispatchUtil.getInstance();

            try {
                int senderEngineNumberOfInstances = dispatchUtil.getTotalMovementSenderEnginesNumber();
                int senderEngineCurrentInstanceId = dispatchUtil.getEngineInstanceId(psevent.getEngineName());
                long tradeId = boMessage.getTradeLongId();
                long engineIdForDispatching = dispatchUtil.getInstanceNumberToDispatch(tradeId, senderEngineNumberOfInstances);

                if (acceptEvent(msgType, domValues, senderEngineCurrentInstanceId, engineIdForDispatching)) {
                    Log.debug(this, "The BOMessage " + pseventmessage.getBoMessage() + " (trade id = " + tradeId +
                            ") has been proceed by the Engine: " + psevent.getEngineName());
                    return super.accept(psevent);
                }
            } catch (Exception e) {
                Log.error(this, "Error" + e);
            }
        }
        return false;
    }

    /**
     * @param msgType
     * @param domValues
     * @param senderEngineInstanceId
     * @param engineIdToDispatch
     * @return true if accepted
     */
    private boolean acceptEvent(String msgType, List<String> domValues, long senderEngineCurrentInstanceId, long engineIdForDispatching) {
        return msgType != null && !domValues.isEmpty() && domValues.contains(msgType) && senderEngineCurrentInstanceId == engineIdForDispatching;
    }
}