package calypsox.tk.collateral.service.efsonlineservice;

import java.util.List;

import calypsox.tk.collateral.service.efsonlineservice.beans.QuoteBeanIN;
import calypsox.tk.collateral.service.efsonlineservice.beans.QuoteBeanOUT;
import calypsox.tk.collateral.service.efsonlineservice.interfaces.EFSQuotesServiceInterface;
import calypsox.tk.collateral.service.efsonlineservice.request.QuotesListsRequests;
import calypsox.tk.collateral.service.efsonlineservice.response.QuotesListsResponses;
import calypsox.util.PropertiesUtils;

import com.calypso.tk.core.Log;
import com.isban.efs2.webservice.EFS2WS;
import com.isban.efs2.webservice.EFS2WSService;
import com.isban.efs2.webservice.Efs2Request;
import com.isban.efs2.webservice.Efs2Response;
import com.isban.efs2.webservice.Entry;
import com.isban.efs2.webservice.Parameters;

/**
 * Calls the web service Thread. First generates the XML petition from the last quotes subscription available on the
 * context and generates the XML message request. Then it calls the webservice and finally processes the response xml
 * and stores the last quotes read in the context
 * 
 * @author Guillermo Solano
 * @version 1.2, 07/01/2014, externalize configuration in properties.efs
 * @see EFSQuotesServiceInterface
 * 
 */
public class EFSBloombergService extends ThreadController<QuoteBeanOUT> implements EFSQuotesServiceInterface {

	/**
	 * Configuration of the EFS WebService given by the EFS team and attached in configuration.properties.efs file.
	 * Properties fields names added here.
	 */
	public final static String PROPERTIES_FILE_NAME = "configuration.properties.efs";
	private final static String SYSTEM_NAME = "SYSTEM_NAME";
	private final static String SYSTEM_PASSWORD = "SYSTEM_PASSWORD";
	private final static String EFS_SERVICE_NAME = "SERVICE_NAME";
	private final static String EFS_SERVICE_DMD_TYPE = "SERVICE_DMD_TYPE";
	private final static String ENTRY_TYPE = "ENTRY_TYPE";
	/**
	 * Mandatory fields in the properties file
	 */
	private final static String MANDATORY_FIELDS[] = { SYSTEM_NAME, SYSTEM_PASSWORD, EFS_SERVICE_NAME,
			EFS_SERVICE_DMD_TYPE, ENTRY_TYPE };
	/**
	 * Default values in case there is an error reading the properties file
	 */
	private final static String DEFAULT_VALUES[] = { "calypso", "calypso", "READ_BLOOMBERG_QUOTES", "REAL_TIME",
			"QUOTES" };

	/* class variables */
	/**
	 * Access to the context, backbone to exchange the information between threads.
	 */
	private final EFSContext context;
	/**
	 * Allows the translation between the xml requests and answers.
	 */
	private final EFSCommTranslation translator;
	/**
	 * Keeps track of the last WS answer.
	 */
	private String lastWSAnswer;
	/**
	 * Properties Util to access the configuration in properties.efs
	 */
	private final PropertiesUtils properties;

	/**
	 * Main Constructor
	 * 
	 * @param true to be executed as an independent thread.
	 */
	public EFSBloombergService(boolean threadOriented) {

		super(threadOriented);
		this.context = EFSContext.getEFSInstance();
		this.translator = new EFSCommTranslation();
		this.lastWSAnswer = "";
		this.properties = new PropertiesUtils(PROPERTIES_FILE_NAME, MANDATORY_FIELDS, DEFAULT_VALUES);
		super.setSleepTime(EFSContext.getWebServiceSleepTime());
	}

	/**
	 * Generic Constructor, multithreading activated
	 */
	public EFSBloombergService() {
		this(true);
	}

	/**
	 * Calls the web service. First generates the XML petition from the last quotes subscription available on the
	 * context, generates the XML message. Then calls the webservice and finally processes the response. Matches
	 * interface with ThreadController load method.
	 */
	@Override
	public void startEfsWebService() {

		load();
	}

	/**
	 * Main method of ThreadController, contains the main logic to be executed under the thread.
	 */
	@Override
	public void runThreadMethod() {

		Log.info(EFSBloombergService.class, "callWsThread thread started.");

		do {

			// main logic of the WS:1? generates xml, asks WS and processes the response.

			try {

				// retrieve bond/equities request beans and generate the xml petition
				final String xmlPetition = generateEFSPetitionXML();

				// call the EFS webService passing the xml request
				final Efs2Response received = callEFSWebService(xmlPetition);

				this.lastWSAnswer = wsResponseLog(received);
				Log.info(EFSBloombergService.class, this.lastWSAnswer);

				// process the response and store it in the context
				processEFSresponse(received);

				Log.info(EFSBloombergService.class, " Time to Process in sec callWsThread iteration= "
						+ getRunningTime());

				// increase executions counter
				super.increaseExecutionsCounter();

				// now wait till next iteration
				super.sleepThread();

			} catch (EFSException e) {

				// Exception control and Log
				super.processException(e);
			}

		} while (super.isAlive());

	}

	/**
	 * 
	 * @return
	 * @throws EFSException
	 */
	public String generateEFSPetitionXML() throws EFSException {

		if ((this.context == null) || (this.translator == null)) {

			throw new EFSException("Context is not initialiate. Thread cannot continue.", true);
		}

		final List<QuoteBeanIN> currentRequestBeans = this.context.getQuotesSubscription();

		if (currentRequestBeans.isEmpty()) {

			throw new EFSException("No quotes subscription yet in the context. Process sleep.", false, true);
		}

		final QuotesListsRequests currentPetition = this.translator
				.translateQuotesBeansIntoQuotesListRequest(currentRequestBeans);

		return this.translator.translateQuotesListLIntoXMLRequest(currentPetition);
	}

	/**
	 * makes the call to the EFS Webservice using their configuration parameters.
	 * 
	 * @param xml
	 *            with the input message with the format specified in the DDS.
	 * @return a valid response from the EFS system
	 * @throws EFSException
	 */
	@Override
	public Efs2Response callEFSWebService(final String xml) throws EFSException {

		EFS2WSService service = null;
		EFS2WS port = null;
		Efs2Request request = null;

		try {

			service = new EFS2WSService();
			port = service.getEFS2WSPort();
			// specific configuration of the WS request to be sent to EFS
			request = buildEFSRequest();

		} catch (Exception e) {

			throw new EFSException("Critic error building EFS WS configuration. " + e.getLocalizedMessage(), true);
		}

		// construction of the entry & parameters
		Entry eQuotesName = buildQuotesEntry(xml);
		request.setParameters(insertQuotesParams(eQuotesName));

		return port.handleRequest(request); // sends petition and retrieves the answer
	}

	/**
	 * Process the response and stores the quotes received in the context
	 * 
	 * @throws EFSException
	 *             if the xml inside response is empty
	 */
	public void processEFSresponse(Efs2Response received) throws EFSException {

		if ((this.context == null) || (this.translator == null) || (received == null)) {
			return;
		}

		final String xmlResponse = received.getValue();

		if ((xmlResponse == null) || xmlResponse.isEmpty()) {

			throw new EFSException("XML response from WS is empty. Process sleep.", false, true);
		}

		// probably should check message received

		// get EFS response
		QuotesListsResponses responseReceived = this.translator.traslateXMLResponseIntoQuotesList(xmlResponse);

		// parssing into quote reponse beans
		final List<QuoteBeanOUT> currentResponseBeans = this.translator
				.translateResponseIntoQuotesBeans(responseReceived);

		// copy into the context
		this.context.setQuotesResponse(currentResponseBeans);
	}

	/**
	 * @param response
	 *            from efs
	 * @return codes of the response, for log info purpose
	 */
	public static String wsResponseLog(Efs2Response response) {

		StringBuffer st = new StringBuffer("Response Code: " + response.getResponseCode() + "\n");
		st.append("Response Desc: " + response.getResponseDescription() + "\n");
		st.append("Internal Code: " + response.getInternalCode() + "\n");
		// st.append("Internal Desc: " + response.getInternalDescription() + "\n");

		return st.toString();

	}

	/**
	 * Stops the webservice thread
	 */
	@Override
	public void killEfsWebService() {

		super.enableThreading = false;

	}

	/**
	 * @return status of the thread. Number of executions and timers
	 */
	@Override
	public String getStatusInfo() {

		StringBuffer sb = new StringBuffer();
		sb.append("<------> LAST EFS WebService RESPONSE <------>\n");
		sb.append(this.lastWSAnswer);
		sb.append("<-----------> THREADS STATUS <--------------->\n");
		sb.append("Thread 1: WebService\n");
		sb.append("     ");
		sb.append("Processing time webService = ");
		sb.append(super.getTimeHoursMinutesSecondsString(getRunningTime())).append("  elapsed time \n");
		// sb.append(String.format("%.2g", getRunningTime() / 60.0)).append(" min\n");
		sb.append("     ");
		sb.append("Number of executions webService = ");
		sb.append(super.executionsCounter()).append(" times \n");
		sb.append("----------------------------------------------\n");

		return sb.toString();
	}

	/*
	 * Returns the parameters to be set to the EFS request. input is a Entry (that contains the XML request petition).
	 */
	private Parameters insertQuotesParams(Entry eQuotesName) {

		Parameters parameters = new Parameters();
		List<Entry> lParameter = parameters.getEntry();
		if (eQuotesName != null) {
			lParameter.add(eQuotesName);
		}

		return parameters;
	}

	/*
	 * EFS Entry that contains the XML request petition.
	 */
	private Entry buildQuotesEntry(final String xml) {

		if ((xml == null) || xml.trim().isEmpty()) {
			return null;
		}
		Entry eQuotesName = new Entry();
		eQuotesName.setKey(this.properties.getProperty((ENTRY_TYPE)));
		eQuotesName.setValue(xml);

		return eQuotesName;
	}

	/* Sets the specific configuration of the WS for Calypso Col */
	private Efs2Request buildEFSRequest() throws Exception {

		// process properties
		this.properties.process();

		// build request
		Efs2Request request = new Efs2Request();
		request.setUserName(this.properties.getProperty(SYSTEM_NAME));
		request.setUserPassword(this.properties.getProperty(SYSTEM_PASSWORD));
		request.setService(this.properties.getProperty(EFS_SERVICE_NAME));
		request.setDmd(this.properties.getProperty(EFS_SERVICE_DMD_TYPE));

		return request;
	}

	/*
	 * uses toString of the quoteBeanOut
	 */
	@SuppressWarnings("unused")
	private String generateMapKey(QuoteBeanOUT q) {

		return q.toString();

	}

}
