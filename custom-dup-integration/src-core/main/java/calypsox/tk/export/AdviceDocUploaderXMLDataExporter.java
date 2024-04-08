package calypsox.tk.export;

import java.util.Collections;
import java.util.Optional;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.Pair;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.export.DataExporterConfig;
import com.calypso.tk.export.UploaderXMLDataExporter;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.jaxb.CalypsoObject;
import com.calypso.tk.upload.services.GatewayUtil;
import com.calypso.tk.util.MessageArray;
import com.calypso.tk.util.TaskArray;

import calypsox.tk.bo.document.DataExporterTopicSender;

public class AdviceDocUploaderXMLDataExporter extends UploaderXMLDataExporter {
	
	private static final String MESSAGE_ID = "MESSAGE_ID";
	public static final String ERROR_ATTRIBUTE = "Error";
	
	public static final String SOURCE_EVENT_ATTRIBUTE = "SourceEvent";
	public static final String UPLOAD_ADVICE_DOCID_EVENT_ATTRIBUTE = "UploadAdviceDocumentID";
	public static final String DATA_ADVICE_DOCID_EVENT_ATTRIBUTE = "DataAdviceDocumentID";
	public static final String LOG_CATEGORY = "AdviceDocUploaderXMLDataExporter";
	
	public static final Action FAIL_SEND_ACTION = Action.valueOf("FAIL_SEND");
	public static final String MARKET = "MARKET";
	//List<String> staticDataFilters ;
		
	public Object sourceEvent;
	public Object originalObject;
	public Object sourceObject;
	
	protected void init() {
		super.init();
		// adding static data filters from configuration file
		Object staticDataFilters = this.getExporterConfig().getProperty("staticdatafilters");
		if(staticDataFilters!=null && staticDataFilters instanceof String) {
			Collections.addAll(super.getFilterNameList(), ((String)staticDataFilters).split(","));
		}
	}
	
	public Object getOriginalObject() {
		return originalObject;
	}

	public void setOriginalObject(Object originalObject) {
		this.originalObject = originalObject;
	}
	
	public Object getSourceObject() {
		return sourceObject;
	}

	public void setSourceObject(Object sourceObject) {
		this.sourceObject = sourceObject;
	}
	
	public Object getSourceEvent() {
		return sourceEvent;
	}

	public void setSourceEvent(Object sourceEvent) {
		this.sourceEvent = sourceEvent;
	}

	public AdviceDocUploaderXMLDataExporter(DataExporterConfig exporterConfig) {
		super(exporterConfig);
	}

    
    public void createAndSaveBOMessage() {
    	if(!this.isIgnoreObject() && this.isMessagecreation()) {
	    	createAndSaveAdviceDocument();
	        super.createUpddateBOMessage();
    	}
    }
    
	protected void createAndSaveAdviceDocument() {
		try {
			if (this.getAdviceDocument(this.getBoMessage()) == null && !Util.isEmpty(this.getDataToSend())) {
				AdviceDocument aDocument = GatewayUtil.createAdviceDocument(this.getBoMessage(), this.getDataToSend());
				if(sourceEvent!=null)
					aDocument.getAttributes().add(SOURCE_EVENT_ATTRIBUTE, sourceEvent);
				long adviceDocId = DSConnection.getDefault().getRemoteBO().save(aDocument);
				this.getBoMessage().setAttribute(DATA_ADVICE_DOCID_EVENT_ATTRIBUTE, String.valueOf(adviceDocId));
				this.getBoMessage().setAttribute(UPLOAD_ADVICE_DOCID_EVENT_ATTRIBUTE, String.valueOf(adviceDocId));
			}
		} catch (Exception arg1) {
			Log.error(this, "Couldn\'t create or retrieve adviceDocument");
		}

	}
	
    @Override
    public CalypsoObject exportObject(Object sourceObject, Class<?> classType, String type) {
    	if(sourceObject instanceof Trade) {
	    	setSourceObject(sourceObject);
	    	if(this.isMessagecreation()) {
	    		createBOMessage();
	    		getBoMessage().setMessageType(getMessageType());
	    		getBoMessage().setGateway(getMessageGateway());
	    		getBoMessage().setProductType(((Trade)sourceObject).getProductType());
	    		getBoMessage().setProductFamily(((Trade)sourceObject).getProductFamily());
	    	}
	    	fillTradeInfo(sourceObject);
	    	if(this.isMessagecreation())
	    		getBoMessage().setStatus(Status.valueOf("TO_BE_SENT"));
    	}
        return super.exportObject(sourceObject, classType, type);
    }
    
    protected String getMessageType() {
    	Object messageType = this.getExporterConfig().getProperty("messagetype");
    	if(messageType==null)
    		return getBoMessage().getMessageType();
    	return messageType.toString();
    }
    
    protected String getMessageGateway() {
    	Object gateway = this.getExporterConfig().getProperty("gateway");
    	if(gateway==null)
    		return getBoMessage().getGateway();
    	return gateway.toString();
    }
    
    protected void fillTradeInfo(Object sourceObject) {
        Trade trade;
        if (sourceObject instanceof Trade) {
        	
            trade = (Trade) sourceObject;
            BOMessage boMessage = this.getBoMessage();
            if (null != boMessage) {
                trade.addKeyword(MESSAGE_ID, String.valueOf(boMessage.getLongId()==0L?boMessage.getAllocatedLongSeed():boMessage.getLongId()));
            }
			addEquityMarketKeyword(trade);
        }
    }

	protected void addEquityMarketKeyword(Trade trade){
		if (trade.getProduct() instanceof MarginCall){
			MarginCall marginCall = (MarginCall) trade.getProduct();
			if (null != marginCall.getSecurity() && marginCall.getSecurity() instanceof Equity) {
				String exchange = ((Equity) marginCall.getSecurity()).getExchange();
				String mxLabel = ((Equity) marginCall.getSecurity()).getSecCode("LABEL_MX3_EQTY");
				trade.addKeyword("LABEL_MX3_EQTY", !Util.isEmpty(mxLabel) ? mxLabel : " ");
				if (!Util.isEmpty(exchange)) {
					trade.addKeyword(MARKET, exchange);
				}
			}
		}
	}
    
    @Override
    public void sendData() {
    	if(!isIgnoreObject()) {
    		preSend();
    		if("Topic".equalsIgnoreCase(this.getExportType()) && Util.isEmpty(this.getErrors()) && !Util.isEmpty(this.getDataToSend())) {
				(new DataExporterTopicSender(this.getProperties(), this.getSourceName())).send(this.getDs(), this.getDataToSend(), this.getContextMap(), this.getErrors());
			}
    		else {
				super.sendData();
			}
    		postSend();
      	}
    }
    	

    
	public void exportObject(Object object) {
		if(object instanceof Pair) {
			Object first = ((Pair<?, ?>) object).first();
			Object second = ((Pair<?, ?>) object).second();
			this.setSourceEvent(first);
			
			if(second instanceof Pair) {
				Pair<?,?> pair = (Pair<?,?>) second;
				setOriginalObject(pair.first());
				super.exportObject(pair.second());
			}
			else
				super.exportObject(second);
		}
		else super.exportObject(object);
	}
	
	public void preSend() {
		createAndSaveBOMessage();
	}

	/**
	 * Applies FAIL_SEND action if error occur during sending
	 */
    public void postSend() {
    		if(this.getErrors().size()>0 && this.getBoMessage()!=null) {
    			try {
    				BOMessage message = Optional.ofNullable(getMsgIfNotSaved(this.getBoMessage()))
							.map(this::cloneMsg).orElse(null);
					if(message!=null) {
						message.setAction(FAIL_SEND_ACTION);
						message.setAttribute(ERROR_ATTRIBUTE, getErrors().get(0));
						message.setDescription(getErrors().get(0));
						MessageArray messageArray = new MessageArray();
						messageArray.add(message);
						getDs().getRemoteBO().saveMessages(0L, null, messageArray, new TaskArray());
					}
    			} catch (CalypsoServiceException  e) {
    				this.getErrors().add(e.getMessage());
    				Log.error(LOG_CATEGORY, e);
    			}
    		}
        	
    }

	protected BOMessage getMsgIfNotSaved(BOMessage sourceMsg) throws CalypsoServiceException {
		BOMessage finalMsg=sourceMsg;
		if(sourceMsg.getAllocatedLongSeed()!=0L){
			 finalMsg=getDs().getRemoteBackOffice().getMessage(this.getBoMessage().getAllocatedLongSeed());
		}
		return finalMsg;
	}

	protected BOMessage cloneMsg(BOMessage msg){
		BOMessage clonedMsg=null;
		try{
			clonedMsg= (BOMessage) msg.cloneIfImmutable();
		} catch (CloneNotSupportedException e) {
			this.getErrors().add(e.getMessage());
			Log.error(LOG_CATEGORY, e);
		}
		return clonedMsg;
	}

}
