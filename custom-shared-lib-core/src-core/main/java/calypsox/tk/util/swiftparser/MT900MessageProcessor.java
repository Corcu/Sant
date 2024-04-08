/**
 * 
 */
package calypsox.tk.util.swiftparser;

/**
 * Extesion of the MT900MessageProccessor. 
 * Compared to core version, before saving after reprocess of the message, MarginCall Cash trade MUST be created instead of a SimpleTransfer Trade.
 * Rest the logic is the same.
 * 
 * @author Guillermo Solano
 * @version 1.0
 * @date 04/06/2016
 *
 */
public class MT900MessageProcessor extends com.calypso.tk.util.swiftparser.MT900MessageProcessor {

	/**
	 * Constructor
	 */
	public MT900MessageProcessor() {
		super();
	}
	
	/**
	 * MT900MessageProcessor override Method
	 */
//	@SuppressWarnings({ "unchecked", "rawtypes" })
//	protected void beforeSave(BOMessage message, Object indexedObject, Trade indexedTrade, BOMessage indexedMessage,
//			BOTransfer indexedTransfer, SwiftMessage swiftMessage, boolean indexed, boolean matched, Vector errors,
//			DSConnection ds, Object dbCon) {
//		
//		//System.out.println("Dentro clase custom");
//		if (swiftMessage.isAcknowledgementMessage()) {
//			return;
//		}
//		if (swiftMessage.hasProcessIssue(null)) {
//			return;
//		}
//		CashSettlementConfirmationHandler handler = createHandler();
//		ExternalPositionTradeSaver saver = (ExternalPositionTradeSaver) getObjectSaver();
//		
//		if (handler.shouldUpdateExternalPosition(swiftMessage, errors)) {
//			
//			MarginCallCashSettlementUtil util = new MarginCallCashSettlementUtil(handler, message, swiftMessage);
//			final Trade updateMCCashTrade = util.buildMarginCallCashTrade();
//			saver.add(Trade.class, updateMCCashTrade);
//		}
//	}
}
