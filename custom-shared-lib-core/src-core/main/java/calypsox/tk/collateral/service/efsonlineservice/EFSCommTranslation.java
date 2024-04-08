/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.collateral.service.efsonlineservice;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import com.calypso.tk.core.Log;

import calypsox.tk.collateral.service.efsonlineservice.beans.QuoteBeanIN;
import calypsox.tk.collateral.service.efsonlineservice.beans.QuoteBeanOUT;
import calypsox.tk.collateral.service.efsonlineservice.beans.QuoteBeanOUT.QUOTE_TYPE;
import calypsox.tk.collateral.service.efsonlineservice.request.QuoteRequest;
import calypsox.tk.collateral.service.efsonlineservice.request.QuotesListRequest;
import calypsox.tk.collateral.service.efsonlineservice.request.QuotesListsRequests;
import calypsox.tk.collateral.service.efsonlineservice.response.QuoteResponse;
import calypsox.tk.collateral.service.efsonlineservice.response.QuotesListResponse;
import calypsox.tk.collateral.service.efsonlineservice.response.QuotesListsResponses;

/**
 * This class is expected to make the translations between EFS and Calypso Col, for requests and responses.-------------
 * IDEA: static methods in order to: -----------------------------------------------------------------------------------
 * REQUEST: ListQuoteBean -> Transformation into XML Request to be sent from Calypso TO EFS ----------------------------
 * RESPONSE: Transformation XML Response received in Calypso FROM EFS -> ListQuoteBeanOUT quotesResponse ---------------
 * 
 * @author Guillermo Solano
 * @version 0.9
 * 
 */
public class EFSCommTranslation {

	private static final String EMPTY = "";
	private static final Object MARSHALLER_ENCODING = "UTF-8";

	// JAXB context
	private JAXBContext context = null;

	// ///////////////////////////////////////////////////////////////////////////////////////
	// //////////////////////// REQUESTS TRANSLATIONS ///////////////////////////////////////
	// /////////////////////////////////////////////////////////////////////////////////////
	/**
	 * @param quotesList
	 *            in the format of QuotesBeanIN to build the request
	 * @return QuotesListsResponses as a JABX object
	 */
	public synchronized QuotesListsRequests translateQuotesBeansIntoQuotesListRequest(final List<QuoteBeanIN> quotesList) {

		final String bondName = QUOTE_TYPE.BOND.name().toLowerCase();
		final String equityName = QUOTE_TYPE.EQUITY.name().toLowerCase();
		final QuotesListsRequests response = new QuotesListsRequests();
		final QuotesListRequest quoteList1Bonds = new QuotesListRequest();
		final QuotesListRequest quoteList2Equities = new QuotesListRequest();

		quoteList1Bonds.setType(bondName);
		quoteList2Equities.setType(equityName);

		// generate both lists
		for (QuoteBeanIN add : quotesList) {

			final QuoteRequest quote = new QuoteRequest();
			quote.setIsin(add.getISIN());
			quote.setCurrency(add.getCurrency());

			if (add.getType().equals(QUOTE_TYPE.BOND)) {
				quoteList1Bonds.getQuote().add(quote);
			} else if (add.getType().equals(QUOTE_TYPE.EQUITY)) {
				quoteList2Equities.getQuote().add(quote);
			}
		}

		// add list to request if they have elements
		if (!quoteList1Bonds.getQuote().isEmpty()) {
			response.getQuotesList().add(quoteList1Bonds);
		}

		if (!quoteList2Equities.getQuote().isEmpty()) {
			response.getQuotesList().add(quoteList2Equities);
		}

		return response;
	}

	/**
	 * @param quotesList
	 *            with quotes suscription list for bonds and equities
	 * @return a XML in the format expected to be received by EFS
	 * @throws EFSException
	 */
	public synchronized String translateQuotesListLIntoXMLRequest(final QuotesListsRequests requestObject)
			throws EFSException {

		Marshaller marshaller = null;
		initialiateJAXBContextRequest();

		try {
			marshaller = this.context.createMarshaller();

		} catch (JAXBException e) {
			Log.error(this, e); //sonar
			throw new EFSException(e.getLocalizedMessage(), true);

		}

		if ((this.context == null) || (marshaller == null)) {
			return EMPTY;
		}
		// marshall properties
		try {

			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.setProperty(Marshaller.JAXB_ENCODING, MARSHALLER_ENCODING);

		} catch (PropertyException e) {
			Log.error(this, e); //sonar
			throw new EFSException(e.getLocalizedMessage(), true);
		}

		// encapsulate the StringWriter we are going to receive
		StringWriter sw = new StringWriter();

		// marshall Object -> XML
		try {

			marshaller.marshal(requestObject, sw);

		} catch (JAXBException e) {
			Log.error(this, e); //sonar
			throw new EFSException("Not possible to parser the request into XML." + e.getLocalizedMessage(), true);
		}

		// pass to String
		return sw.toString();

	}

	// ///////////////////////////////////////////////////////////////////////////////////////
	// //////////////////////// RESPONSES TRANSLATIONS //////////////////////////////////////
	// /////////////////////////////////////////////////////////////////////////////////////

	/*
	 * @param quotesList in the format of QuotesBeanIN to build the request
	 * 
	 * @return QuotesListsResponses as a JABX object
	 */
	/**
	 * 
	 * @param quotesListResponse
	 *            in the JABX object response format
	 * @return a List with the quotes prices received
	 */
	public synchronized List<QuoteBeanOUT> translateResponseIntoQuotesBeans(QuotesListsResponses quotesListsResponse) {

		int size = 0; // allows to declare exact size of returning list
		for (QuotesListResponse l : quotesListsResponse.getQuotesList()) {
			size += l.getQuote().size(); // list bonds + list equities
		}
		final List<QuoteBeanOUT> out = new ArrayList<QuoteBeanOUT>(size);

		for (QuotesListResponse responseList : quotesListsResponse.getQuotesList()) {

			QUOTE_TYPE type = getResponseListType(responseList);

			for (QuoteResponse quote : responseList.getQuote()) {

				final QuoteBeanOUT bean = buildQuoteBeanOut(quote, type);
				out.add(bean);
			}
		}

		return out;
	}

	/*
	 * gets the quote type of the response list
	 */
	private QUOTE_TYPE getResponseListType(QuotesListResponse responseList) {

		if ((responseList == null) || (responseList.getType() == null)) {
			return null; // error //nota, si no coincide tipo no procesar
		}

		if (responseList.getType().equalsIgnoreCase(QUOTE_TYPE.BOND.name().toLowerCase())) {
			return QUOTE_TYPE.BOND;
		}

		return QUOTE_TYPE.EQUITY;
	}

	/*
	 * builds a QuoteBeanOUT from a QuoteResponse
	 */
	private static QuoteBeanOUT buildQuoteBeanOut(QuoteResponse quote, QUOTE_TYPE type) {

		final QuoteBeanOUT bean = new QuoteBeanOUT();
		bean.setISIN(quote.getIsin());
		bean.setCurrency(quote.getCurrency());
		bean.setBidPrice(quote.getBid()); // GSM: 07/10/2013 - I_121
		bean.setAskPrice(quote.getAsk());
		bean.setType(type);

		return bean;

	}

	/**
	 * @param xml
	 *            with the Response received in Calypso Col and sent by EFS
	 * @return quotesList with the prices ready to be inserted in Calypso DB
	 */
	public synchronized QuotesListsResponses traslateXMLResponseIntoQuotesList(final String xml) {

		QuotesListsResponses response = null;
		initialiateJAXBContextResponse();

		Unmarshaller unmarshall = null;
		try {
			unmarshall = this.context.createUnmarshaller();

		} catch (JAXBException e) {
			Log.error(this, e); //sonar
		}

		final Document xmlDocument = createDocument(xml);

		if ((xml == null) || xml.isEmpty() || (unmarshall == null)) {
			return null;
		}

		try {
			response = (QuotesListsResponses) unmarshall.unmarshal(xmlDocument);

		} catch (JAXBException e) {
			Log.error(this, e); //sonar
		}

		return response;
	}

	/*
	 * Encapsulates a XML String into a Document object, so the unmarshaller can process it
	 */
	private Document createDocument(final String msg) {
		Document rst = null;
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(false);
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			rst = builder.parse(new ByteArrayInputStream((msg).getBytes("UTF8")));
			if (rst == null) {
				rst = builder.parse(msg);
			}
		} catch (final Exception e1) {
			Log.error(this, e1); //sonar
		}

		return rst;
	}

	/*
	 * Allows to initiliate the context for the request
	 */
	private void initialiateJAXBContextRequest() {

		initialiateJAXBContext(true);
	}

	/*
	 * Allows to initiliate the context for the request
	 */
	private void initialiateJAXBContextResponse() {

		initialiateJAXBContext(false);
	}

	private void initialiateJAXBContext(boolean selection) {

		// create JAXB context and instantiate marshaller
		try {
			if (selection) {
				this.context = JAXBContext.newInstance(QuotesListsRequests.class);
			} else {
				this.context = JAXBContext.newInstance(QuotesListsResponses.class);
			}

		} catch (JAXBException e) {
			Log.error(this, e); //sonar
		}

	}

}
