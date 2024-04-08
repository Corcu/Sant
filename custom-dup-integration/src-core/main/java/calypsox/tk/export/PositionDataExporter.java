package calypsox.tk.export;

import calypsox.tk.util.PositionExportEngineUtil;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.core.*;
import com.calypso.tk.export.DataExporterConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.HashMap;
import java.util.Map;
public class PositionDataExporter extends AdviceDocUploaderXMLDataExporter {
	
	public static String LOG_CATEGORY = "PositionDataExporter";
	
	protected Map<String,String> additionalJMSAttributes;
	private static final String PRODUCT_TYPE = "ProductType";
	private static final String JMS_ATTRIBUTE_TOPIC = "TOPIC";
	protected int nbRetryApplyActionSendOnTrade = 2;
	
	public PositionDataExporter(DataExporterConfig exporterConfig) {
		super(exporterConfig);
	}

	public void exportTrade(Trade trade) {
		super.exportTrade(trade);
		additionalJMSAttributes = getJMSAttributesFromTrade(trade);
	}
	
	@Override
	public void preSend() {
		applySendActionOnTrade(nbRetryApplyActionSendOnTrade);
		super.preSend();
	}

	public void applySendActionOnTrade(int nbRetry) {

		if (getOriginalObject() instanceof Trade) {
			Trade trade = (Trade) getOriginalObject();
			trade.setAction(Action.SEND);
			try {
				if (isAcionApplicable(trade)) {
					DSConnection.getDefault().getRemoteTrade().save(trade);
				}
			} catch (CalypsoServiceException e) {
				if (nbRetry > 0) {
					Throwable cause = e.getCause();
					if (cause instanceof PersistenceException) {
						Throwable nestedCause = ((PersistenceException) cause).getCause();
						if (nestedCause instanceof ObjectVersionMismatchedException) {
							try {
								Log.info(LOG_CATEGORY, "Trade was already changed retrying to apply SEND action on Trade " + trade.getLongId());
								this.setOriginalObject(DSConnection.getDefault().getRemoteTrade().getTrade(trade.getLongId()));
							} catch (CalypsoServiceException e1) {
								Log.error(LOG_CATEGORY, e1);
							}
							applySendActionOnTrade(--nbRetry);
							return;
						}
					}
				}
				Log.error(LOG_CATEGORY, e);
			}
		}
	}
	
	@Override
	public void postSend() {
		if(getOriginalObject() instanceof Trade) {
			if(this.getErrors().size()>0) {
				Trade trade = (Trade)getOriginalObject();
				trade.setAction(Action.FAIL);
				try {
					DSConnection.getDefault().getRemoteTrade().save(trade);
				} catch (CalypsoServiceException e) {
					Log.error(LOG_CATEGORY, e);
				}
			}
		}
		super.postSend();
	}
	
	public Map<String,String> getJMSAttributesFromTrade(Trade trade) {
		HashMap<String,String> attributes = new HashMap<String,String>();
		String productType = PositionExportEngineUtil.getSecurityType(trade);
		attributes.put(PRODUCT_TYPE, productType);
		forceTopicValue(attributes,trade);
		return attributes;
	}

	/**
	 * On 01/12/2021 every exported trade must have the TOPIC's JMS header set to Madrid (including SLB Triparty trades)
	 * This is why this value gets hardcoded. Anyway, if these criteria changes this method will become shit, so please
	 * refactor it SOLIDly.
	 * Cheers developers, take care.
	 *
	 * From your friend and neighbour Spiderman.
	 * @param attributes
	 * @param trade
	 */
	private void forceTopicValue(HashMap<String,String> attributes,Trade trade){
		String activationFlag = LocalCache.getDomainValueComment(DSConnection.getDefault(),"CodeActivationDV","DEACTIVATE_MUREXEXPORT_HARDCODED_TOPIC");
		if(!Boolean.parseBoolean(activationFlag)){
			attributes.put(JMS_ATTRIBUTE_TOPIC, "Madrid");
		}else{
			attributes.put(JMS_ATTRIBUTE_TOPIC, trade.getKeywordValue(JMS_ATTRIBUTE_TOPIC));
		}
	}

	@Override
	protected Map<String, String> getContextMap() {
		Map<String,String> contextMap = super.getContextMap();
		contextMap.putAll(additionalJMSAttributes);
		return contextMap;
	}

	private Trade reloadTrade(Trade trade) {
		try {
			trade = DSConnection.getDefault().getRemoteTrade().getTrade(((Trade) trade).getLongId());
		} catch (CalypsoServiceException e) {
			Log.error(this.getClass().getSimpleName() + "Cant  retrieve  the trade: " + ((Trade) trade).getLongId(), e);
		}
		return trade;
	}

	private boolean isAcionApplicable(Trade trade) {
		for (int retry = 0; retry < 3; retry++) {
			if (CollateralUtilities.isTradeActionApplicable(trade, trade.getAction())) {
				return true;
			}
			trade = reloadTrade(trade);
		}
		Log.error(LOG_CATEGORY, "Cant apply SEND action on Trade " + trade.getLongId() + " from " + trade.getStatus());
		return false;
	}
}
