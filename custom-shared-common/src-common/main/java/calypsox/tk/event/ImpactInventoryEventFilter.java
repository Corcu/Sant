package calypsox.tk.event;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTransfer;

public class ImpactInventoryEventFilter implements EventFilter {

	@Override
	public boolean accept(PSEvent psEvent) {
		if(psEvent instanceof PSEventTransfer) {
			PSEventTransfer eventTransfer = (PSEventTransfer) psEvent;
			return hasImpactOnInventory(eventTransfer) ;
		}
		return true;
	}
	
	protected boolean hasImpactOnInventory(PSEventTransfer eventTransfer) {

		BOTransfer oldTransfer = eventTransfer.getOldTransfer();
		BOTransfer newTransfer = eventTransfer.getBoTransfer();
		
		if(oldTransfer==null) return true;
		if(eventTransfer.getIsResetTransferEvent()) return true;
		if(oldTransfer.getLongId()!=newTransfer.getLongId()) return true;
		if(!isEqual(eventTransfer.getStatus(),eventTransfer.getOldStatus())) return true;
		if(!(oldTransfer.getSettlementAmount()==newTransfer.getSettlementAmount())) return true;
		if(!isEqual(oldTransfer.getSettleDate(),newTransfer.getSettleDate())) return true;
		if(!isEqual(oldTransfer.getSettlementCurrency(),newTransfer.getSettlementCurrency())) return true;
		if(!isEqual(oldTransfer.getTransferType(),newTransfer.getTransferType())) return true;
		if(!isEqual(oldTransfer.getTradeCurrency(),newTransfer.getTradeCurrency())) return true;
		if(!isEqual(oldTransfer.getPayReceive(),newTransfer.getPayReceive())) return true;
		if(!isEqual(oldTransfer.getValueDate(),newTransfer.getValueDate())) return true;
		if(!isEqual(oldTransfer.getBookingDate(),newTransfer.getBookingDate())) return true;
		if(!isEqual(oldTransfer.getCallableDate(),newTransfer.getCallableDate())) return true;
		if(!isEqual(oldTransfer.getNettingType(),newTransfer.getNettingType())) return true;
		if(!isEqual(oldTransfer.getDeliveryType(),newTransfer.getDeliveryType())) return true;
		if(!isEqual(oldTransfer.getTradeDate(),newTransfer.getTradeDate())) return true;
		if(!isEqual(oldTransfer.getSettlementMethod(),newTransfer.getSettlementMethod())) return true;
		if(!isEqual(oldTransfer.getPayReceiveType(),newTransfer.getPayReceiveType())) return true;
		if(!isEqual(oldTransfer.getPayerSDStatus(),newTransfer.getPayerSDStatus())) return true;
		if(!isEqual(oldTransfer.getReceiverSDStatus(),newTransfer.getReceiverSDStatus())) return true;
		if(!isEqual(oldTransfer.getCashTransfer(),newTransfer.getCashTransfer())) return true;
		if(!isEqual(oldTransfer.getAvailableDate(),newTransfer.getAvailableDate())) return true;
		if(!isEqual(oldTransfer.getNettedTransfers(),newTransfer.getNettedTransfers())) return true;
		if(!isEqual(oldTransfer.getEndDate(),newTransfer.getEndDate())) return true;
		if(!isEqual(oldTransfer.getLiquidatedPosition(),newTransfer.getLiquidatedPosition())) return true;
		if(!isEqual(oldTransfer.getInitSettleDate(),newTransfer.getInitSettleDate())) return true;
		if(!isEqual(oldTransfer.getPayReceiveTypeWithReverse(),newTransfer.getPayReceiveTypeWithReverse())) return true;
		if(!isEqual(oldTransfer.getInventoryMaintainerOptions(),newTransfer.getInventoryMaintainerOptions())) return true;
		if(!(oldTransfer.getLongId()==newTransfer.getLongId())) return true;
		if(!(oldTransfer.getTradeLongId()==newTransfer.getTradeLongId())) return true;
		if(!(oldTransfer.getProductId()==newTransfer.getProductId())) return true;
		if(!(oldTransfer.getNominalAmount()==newTransfer.getNominalAmount())) return true;
		if(!(oldTransfer.getExternalLegalEntityId()==newTransfer.getExternalLegalEntityId())) return true;
		if(!(oldTransfer.getExternalSettleDeliveryId()==newTransfer.getExternalSettleDeliveryId())) return true;
		if(!(oldTransfer.getManualSDId()==newTransfer.getManualSDId())) return true;
		if(!(oldTransfer.getExternalAgentId()==newTransfer.getExternalAgentId())) return true;
		if(!(oldTransfer.getInternalSettleDeliveryId()==newTransfer.getInternalSettleDeliveryId())) return true;
		if(!(oldTransfer.getInternalAgentId()==newTransfer.getInternalAgentId())) return true;
		if(!(oldTransfer.getGLAccountNumber()==newTransfer.getGLAccountNumber())) return true;
		if(!(oldTransfer.getParentLongId()==newTransfer.getParentLongId())) return true;
		if(!(oldTransfer.getOtherAmount()==newTransfer.getOtherAmount())) return true;
		if(!(oldTransfer.getBookId()==newTransfer.getBookId())) return true;
		if(!(oldTransfer.getAvailableB()==newTransfer.getAvailableB())) return true;
		if(!(oldTransfer.getRealSettlementAmount()==newTransfer.getRealSettlementAmount())) return true;
		if(!(oldTransfer.getIsKnownB()==newTransfer.getIsKnownB())) return true;
		if(!(oldTransfer.getNettedTransfer()==newTransfer.getNettedTransfer())) return true;
		if(!(oldTransfer.getNettedTransferLongId()==newTransfer.getNettedTransferLongId())) return true;
		if(!(oldTransfer.getNettedTradeLongId()==newTransfer.getNettedTradeLongId())) return true;
		if(!(oldTransfer.getBundleId()==newTransfer.getBundleId())) return true;
		if(!(oldTransfer.getIsReturnB()==newTransfer.getIsReturnB())) return true;
		if(!(oldTransfer.getInternalLegalEntityId()==newTransfer.getInternalLegalEntityId())) return true;
		if(!(oldTransfer.getCashAccountNumber()==newTransfer.getCashAccountNumber())) return true;
		if(!(oldTransfer.getExternalCashSdId()==newTransfer.getExternalCashSdId())) return true;
		if(!(oldTransfer.getManualCashSDId()==newTransfer.getManualCashSDId())) return true;
		if(!(oldTransfer.getInternalCashSdId()==newTransfer.getInternalCashSdId())) return true;
		if(!(oldTransfer.getInternalCashAgentId()==newTransfer.getInternalCashAgentId())) return true;
		if(!(oldTransfer.getRealCashAmount()==newTransfer.getRealCashAmount())) return true;
		if(!(oldTransfer.getLinkedLongId()==newTransfer.getLinkedLongId())) return true;
		if(!(oldTransfer.getIsFixedB()==newTransfer.getIsFixedB())) return true;
		if(!(oldTransfer.getProcessingOrg()==newTransfer.getProcessingOrg())) return true;
		if(!(oldTransfer.getReceiverSDId()==newTransfer.getReceiverSDId())) return true;
		if(!(oldTransfer.getPayerSDId()==newTransfer.getPayerSDId())) return true;
		if(!(oldTransfer.getOriginalCptyId()==newTransfer.getOriginalCptyId())) return true;
		if(!(oldTransfer.getNettingGroup()==newTransfer.getNettingGroup())) return true;
		if(!(oldTransfer.getIntSDIVersion()==newTransfer.getIntSDIVersion())) return true;
		if(!(oldTransfer.getExtSDIVersion()==newTransfer.getExtSDIVersion())) return true;
		if(!(oldTransfer.getSign()==newTransfer.getSign())) return true;
		if(!(oldTransfer.getProjectedAmount()==newTransfer.getProjectedAmount())) return true;
		if(!(oldTransfer.getPositionAggregationId()==newTransfer.getPositionAggregationId())) return true;
		if(!(oldTransfer.getSubSecAccountId()==newTransfer.getSubSecAccountId())) return true;
		if(!(oldTransfer.getSubCashAccountId()==newTransfer.getSubCashAccountId())) return true;
		
		return false;
		
	}
	
	public static boolean isEqual(Object o1, Object o2) {
		if(o1 != null) {
			return o1.equals(o2);
		}
		return o2==null;
	}

}
