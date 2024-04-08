package calypsox.tk.bo.document;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang3.math.NumberUtils;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.bo.document.SenderConfig;
import com.calypso.tk.bo.document.SenderCopyConfig;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.refdata.MimeType;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import calypsox.tk.bo.FileNetHelper;
import calypsox.tk.collateral.util.SantMarginCallUtil;
import calypsox.util.binding.CustomBindVariablesUtil;

/**
 * Sender class for method EMAIL and for gateway MCNOTIF Margin Call email
 * notification sender. Creates the email, attachs the documents if the incoming
 * action requires it and Sends the email. Finally changes the final action of
 * the Message MC WF (from TO_BE_SENT to SENT/ERROR_SENT).
 *
 * @author VARIOUS
 * @version 3.0
 * @date 28/06/2016
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class EMAILGatewayMCNOTIFFileNetDocumentSender extends EMAILGatewayMCNOTIFDocumentSender {
	private static final String Y_VALUE = "Y";
	private static final String SEND_TO_FILE_NET = "sendToFileNet";
	private static final String XLSX_EXT = ".xlsx";
	private static final String PORTFOLIO_VALUATION = "PortfolioValuation";
	private static final String MARGIN_CALL_NOTICE = "MarginCallNotice";
	private static final String COLLATERAL_POSITIONS = "CollateralPositions";
	private static final String APPLICATION_VND_MS_EXCEL = "application/vnd.ms-excel";
	private static final String FILENET_MIMETYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	private static final String GN_ID_EXT = "_GN_ID";
	private static final String FILENET = "FileNet";
	private static final String DV_FILENET_METADATA = "FILENET.metadata";
	
	/**
	 * Actions constants Message MC WF - MC_NOTIFICATION (to pass from TO_BE_SENT to
	 * SENT/ERROR_SENT).
	 */
	public static final String ERROR_SEND = "ERROR_SEND";
	public static final String SEND_AGAIN = "SEND_AGAIN";
	public static final String EVENT_TYPE_EMIR = "SEND_EMIR_COLLATERAL";
	public static final String EVENT_TYPE_PORTFOLIO_SEND = "SEND_PORTFOLIO_COLLATERAL";
	public static final String EVENT_TYPE_PORTFOLIO_REQUEST = "PORTFOLIO_REQUEST_COLLATERAL";
	public static final String VALUATION = " - Valuation ";
	public static final String PORTFOLIOREQUEST = " - Portfolio Request";

	public static final String EVENT_TYPE_BALANCE = "SEND_BALANCE_COLLATERAL";
	public static final MessageFormat noticeSubject = new MessageFormat("{0} {1} - {2}");
	public static final String EMAIL_SEPARATOR = ";";
	
	public static final String FILENET_DOC_CLASS = DomainValues.comment("FILENET.parameters", "DocClass");
	private static final String NUMBER_OF_RETRY = DomainValues.comment("FILENET.parameters", "NumMaxItems");

	@Override
	public boolean isOnline() {
		return true;
	}

	@Override
	public boolean send(DSConnection dsCon, SenderConfig config, SenderCopyConfig copyConfig, long eventId,
			AdviceDocument document, Vector copies, BOMessage message, Vector errors, String engineName,
			boolean[] saved) {
		String att = message.getAttribute(SEND_TO_FILE_NET);
		if (Util.isEmpty(att) || !att.equalsIgnoreCase(Y_VALUE)) {
			return super.send(dsCon, config, copyConfig, eventId, document, errors, message, errors, engineName, saved);
		} else {

			Log.system(EMAILGatewayMCNOTIFFileNetDocumentSender.class.getName(), "### event id: " + eventId);
			boolean success = false;
			String action = null;
			try {
				success = processMessage(document, message, dsCon, errors);
				action = success ? Action.S_SEND : ERROR_SEND;
				saveMessage(message, engineName, action, "Updated by GatewayMCNOTIFFileNetDocumentSender");
			} catch (CalypsoServiceException | CloneNotSupportedException exc) {
				if (success) {
					errors.add(exc.getMessage() + " File was generated but error saving the message with action "
							+ action);
					return false;
				} else {
					errors.add(exc.getMessage() + " File was NOT generated due to problems processing the message. ");
					return false;
				}
			} catch (Exception e) {
				Log.error(this, "Error sending files to filenet", e);
				return false;
			}
			Log.system(EMAILGatewayMCNOTIFFileNetDocumentSender.class.getName(), "### success =  " + success);
		}
		return true;
	}

	protected boolean processMessage(AdviceDocument document, BOMessage message, DSConnection dsCon, Vector errors)
			throws CalypsoServiceException {

		AdviceDocument portfolioValuationDocument = null;
		AdviceDocument collateralPositionsDocument = null;
		boolean sentToFileNet = true;
		MarginCallEntryDTO mce = null;
		CollateralConfig cc = null;
		HashMap<String, Object> metadataPortfolio = null;
		HashMap<String, Object> metadataCollateral = null;
		HashMap<String, Object> metadataMarginCall = null;

		try {
			mce = SantMarginCallUtil.getMarginCallEntryDTO(message, dsCon);
		} catch (final Exception ex) {
			Log.error(this, ex);
		}
		if (mce != null) {
			
			cc = CacheCollateralClient.getCollateralConfig(dsCon, mce.getCollateralConfigId());
			List<CalypsoBindVariable> bindVariables = CustomBindVariablesUtil
					.createNewBindVariable(message.getLongId());
			final Vector adviceDocuments = dsCon.getRemoteBackOffice()
					.getAdviceDocuments(" advice_document.advice_id=? ", null, bindVariables);
			for (Object object : adviceDocuments) {
				if (object instanceof AdviceDocument) {
					AdviceDocument advDoc = (AdviceDocument) object;
					String tempName = advDoc.getTemplateName();
					if (tempName.startsWith(PORTFOLIO_VALUATION)) {
						portfolioValuationDocument = advDoc;
					} else if (tempName.startsWith(COLLATERAL_POSITIONS)) {
						collateralPositionsDocument = advDoc;
					}
				}
			}
			portfolioValuationDocument=cloneAdviceDocIfNull(portfolioValuationDocument,document,mce,errors);
			collateralPositionsDocument=cloneAdviceDocIfNull(collateralPositionsDocument,document,mce,errors);
			if(portfolioValuationDocument==null || collateralPositionsDocument==null) {
				return false;
			}
			boolean sentToFileNetPV = true;
			byte[] finalXlsPortBreak = getEntryUnderlyingDetailsFromPortfolioBreakdown(mce);
			sentToFileNetPV &= setAdviceDocData(portfolioValuationDocument, finalXlsPortBreak, PORTFOLIO_VALUATION, mce,
					errors);
			metadataPortfolio = getMetadata(PORTFOLIO_VALUATION, message,mce,cc, dsCon);
			sentToFileNetPV = sentToFileNetPV && sendToFileNetWithRetry(message, portfolioValuationDocument,
					PORTFOLIO_VALUATION, metadataPortfolio);
			if(sentToFileNetPV) {
				DSConnection.getDefault().getRemoteBO().save(portfolioValuationDocument);
				sentToFileNet=sentToFileNetPV;
				boolean sentToFileNetCP = true;
				byte[] finalXlsCollPos = getEntryPositionsFromCollateralPositions(mce, false);
				sentToFileNetCP &= setAdviceDocData(collateralPositionsDocument, finalXlsCollPos, COLLATERAL_POSITIONS, mce,
						errors);
				metadataCollateral = getMetadata(COLLATERAL_POSITIONS, message, mce, cc, dsCon);
				sentToFileNetCP = sentToFileNetCP && sendToFileNetWithRetry(message, collateralPositionsDocument,
						COLLATERAL_POSITIONS, metadataCollateral);
				if(sentToFileNetCP) {
					DSConnection.getDefault().getRemoteBO().save(collateralPositionsDocument);
					sentToFileNet&=sentToFileNetCP;
					
					metadataMarginCall = getMetadata(MARGIN_CALL_NOTICE, message, mce, cc, dsCon);
					sentToFileNet &= sendToFileNetWithRetry(message, document, MARGIN_CALL_NOTICE, metadataMarginCall);
					document.setDatetime(new JDatetime());
				} else {
					return false; //CollateralPositions Error
				}
			} else {
				return false; //PortfolioValuation error
			}
			
		} else {
			return false; //No envia ficheros
		}
		

		return sentToFileNet;
	}
	/**
	 * @param document Document to be adjusted.
	 * @param finalXls binary data to set in the document.
	 * @param type type of document to be used by log information.
	 * @return true if document is not null.
	 */
	public AdviceDocument cloneAdviceDocIfNull(AdviceDocument document, AdviceDocument toCloneDocument, MarginCallEntryDTO mce,Vector errors) {
		if (document == null) {
			try {
				document = (AdviceDocument) toCloneDocument.clone();
				document.setId(0);
			} catch (final CloneNotSupportedException cnse) {
				Log.error(this, cnse);
				errors.add("Unable to clone the native document for the margin call entry " + mce.getId() + " "
						+ Util.exceptionToString(cnse));
				return null;
			}
		}
		return document;
	}
	/**
	 * @param document Document to be adjusted.
	 * @param finalXls binary data to set in the document.
	 * @param type type of document to be used by log information.
	 * @return true if document is not null.
	 */
	public boolean setAdviceDocData(AdviceDocument document, byte[] finalXls, String type, MarginCallEntryDTO mce,
			Vector errors) {
		if (finalXls == null) {
			final String error = "Unable to build the "+type+" for the margin call entry " + mce.getId();
			Log.error(this, error);
			errors.add(error);
			return false;
		}
		document.setMimeType(new MimeType(APPLICATION_VND_MS_EXCEL));
		document.setDocument(null);
		document.setTemplateName(type + XLSX_EXT);
		document.setBinaryDocument(finalXls);
//		document.setDatetime(new JDatetime());
		return true;
	}
	/**
	 * @param message  BOMessage of type MC_NOTIFICATION
	 * @param advDoc   Document to be sent.
	 * @param fileType Tipo de documento a enviar usado para el atributo del
	 *                 BOMessage con el GN_ID.
	 * @return true if Document was saved in fileNet and returned GN_ID is saved in
	 *         BOMessage attributes.
	 */
	public boolean sendToFileNetWithRetry(final BOMessage message, final AdviceDocument advDoc, final String fileType,
			Map<String, Object> metadata) {
		boolean retVal = sendToFileNet(message, advDoc, fileType, metadata);
		 if (NumberUtils.isCreatable(NUMBER_OF_RETRY)){
			 int numMax = Integer.parseInt(NUMBER_OF_RETRY); 
			 for (int i = 1; i < numMax && !retVal; i++) {
					retVal = sendToFileNet(message, advDoc, fileType, metadata);
				}
		 }
			return retVal;
	}

	/**
	 * @param message  BOMessage of type MC_NOTIFICATION
	 * @param advDoc   Document to be sent.
	 * @param fileType Tipo de documento a enviar usado para el atributo del
	 *                 BOMessage con el GN_ID.
	 * @return true if Document was saved in fileNet and returned GN_ID is saved in
	 *         BOMessage attributes.
	 */
	public boolean sendToFileNet(final BOMessage message, final AdviceDocument advDoc, final String fileType,  Map<String, Object> metadata) {
		
		boolean control=true;
		String debug = null;
		String fileName = fileType; //Nombre del fichero en filenet
		String mimeType = advDoc.getMimeType().toString(); //MIME type
		mimeType = mimeType.replace(APPLICATION_VND_MS_EXCEL, FILENET_MIMETYPE);
		byte[] file = advDoc.getBinaryDocument(); //Fichero en binario
		String nameService = FILENET.concat("_").concat(message.getMessageType());
			
		//Obtiene instancia de la clase helper
		FileNetHelper helper = FileNetHelper.getInstance();
		
		//Comprueba si es una insercion o actualizacion de fichero mirando si el BOMessage tiene GN_ID
		if(message.getAttribute(fileType + GN_ID_EXT) != null) {
			//Inserta fichero
			control = helper.callUpdateDocumentService(nameService, message.getAttribute(fileType + GN_ID_EXT), file, metadata);			
		} else {
			//Actualiza fichero
			control = helper.callInsertDocumentService(nameService, fileName,mimeType,file,FILENET_DOC_CLASS,  metadata, "");	
		}
		
		//Si devuelve true,  se inserta el GN_ID como atributo del BOMessage
		if (control) {
			message.setAttribute(fileType + GN_ID_EXT, helper.getDocumentId());
		} else {
			debug = (new StringBuilder())
	                .append("Error calling FileNet service ")
	                .append("message [").append(message.getLongId()).append("] ")
	                .append("status [").append(helper.getStatus()).append("] ")
	                .append("error [").append(helper.getErrorMsg()).append("] ")
	                .append("text [").append(helper.getTextMsg()).append("] ")
	                .toString();

	        Log.error(FileNetHelper.class, debug);	
	        
	        
		}
		
		return control;
		
		
	}
	
	private HashMap<String, Object> getMetadata(String fileType, BOMessage message, MarginCallEntryDTO mce, CollateralConfig cc, DSConnection dsCon) {

		Map<String, String> domainValues = DomainValues.valuesComment(DV_FILENET_METADATA);
		HashMap<String, Object> metadata = new HashMap<>();
		Object object = null;
		String tipo = "";

		for (Map.Entry<String, String> entry : domainValues.entrySet()) {

			object = null;
			tipo = "";
			String aux[] = entry.getValue().split("--"); //Obtiene el tipo
			if(aux.length > 1) {
				tipo = aux[1];
			}

			// Obtiene el objeto del cual sacar el dato
			if (entry.getValue().startsWith("MarginCallEntryDTO")) {

				object = mce;
				
			} else if (entry.getValue().startsWith("BOMessage")) {

				object = message; // Ya tenemos un objeto de tipo BOMessage

			} else if (entry.getValue().startsWith("CollateralConfig")) {
				
				if(cc != null) {
					object = cc;
				} else {
					// Obtiene objeto de tipo CollateralConfig
					try {
						
						object = CacheCollateralClient.getCollateralConfig(dsCon, mce.getCollateralConfigId());
					} catch (Exception exp) {
						Log.error(this, exp);
					}
				}

				

			} else { // Pensado para metadatos que no se obtengan de forma dinamica

				if(entry.getKey().equals("FileType")) {
					fileType = fileType.replace(COLLATERAL_POSITIONS, "Collateralpositions"); //Diferent name in Doc metadata
					metadata.put(entry.getValue(), fileType); //Guarda el metadato FileType
				} else {
					//Para valores constantes
					metadata.put(entry.getKey(), getFormatedData(aux[0],tipo));
				}
				
			}

			if (object != null) {
				
				object = getValueByReflection(object,aux[0]);
				metadata.put(entry.getKey(), getFormatedData(object,tipo));
			}    
	}
		//Add ReportDate
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		JDatetime reportDate = mce.getProcessDatetime();
		if(reportDate !=null) {
			metadata.put("ReportDate", df.format(mce.getProcessDatetime()));
		}
		
		return metadata;
	}
	
	private Object checkDateObject(Object object) {
		
		if(object instanceof JDate) {
			Date dat = ((JDate) object).getDate();
			 return dat.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		
		} else if(object instanceof JDatetime) {
			JDatetime dat = (JDatetime) object;
			return dat.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
		} else {
			return object;
		}
	}
	
	private Object getFormatedData(Object object, String tipo) {
	
		if(tipo.equals("String")) {
			return object.toString();
		} else if(tipo.equals("ArrayString")) {
			return  new String[] {object.toString()};
			
		} else {
			return object;
		}
	}
	
	private Object getValueByReflection(Object object, String aux) {
		
		int i;
		Method method = null;
		String[] methods = aux.split("\\.");
		// Recorre el comment del DomainValue para obtener el valor del metadato por
		// reflexion
		for (i = 1; i < methods.length; i++) { // El primer elemento es la clase

			// Obtiene el metodo a invocar
			try {
				method = object.getClass().getMethod(methods[i].replace("()", ""));
				object = method.invoke(object);
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException exp) {
				Log.error(this, exp);
			}

		}
		return checkDateObject(object);
	}
	
}
