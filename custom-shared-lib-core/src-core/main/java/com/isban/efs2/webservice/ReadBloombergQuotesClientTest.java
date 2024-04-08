package com.isban.efs2.webservice;

import java.io.StringWriter;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import calypsox.tk.collateral.service.efsonlineservice.request.QuoteRequest;
import calypsox.tk.collateral.service.efsonlineservice.request.QuotesListRequest;
import calypsox.tk.collateral.service.efsonlineservice.request.QuotesListsRequests;

import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;

@SuppressWarnings("unused")
public class ReadBloombergQuotesClientTest {

	private static final String FILE_PATH = "src/com/isban/efs2/webservice/xml/";
	private static final String FILE_NAME = "Read_quotes.xml";

	public static void main(String... args) throws Exception {

		// connectToDSServer();
		// Properties ps = Defaults.getProperties();
		// String pepe = System.getProperty("user.home");

		EFS2WSService service = new EFS2WSService();
		EFS2WS port = service.getEFS2WSPort();

		Efs2Request request = new Efs2Request();

		String userName = "calypso";
		String password = "calypso";

		request.setUserName(userName);
		request.setUserPassword(password);

		request.setService("READ_BLOOMBERG_QUOTES");

		request.setDmd("REAL_TIME");

		Entry eQuotesName = new Entry();
		eQuotesName.setKey("QUOTES");
		final String xml = test1BuildXML();
		eQuotesName.setValue(xml);// (ClientUtil.getXmlFromFile(FILE_PATH + FILE_NAME));

		Parameters parameters = new Parameters();
		List<Entry> lParameter = parameters.getEntry();
		lParameter.add(eQuotesName);

		request.setParameters(parameters);

		Efs2Response response = port.handleRequest(request);

		printResponse(response);
	}

	// DS connection to the server
	private static void connectToDSServer() {

		String args[] = { "-env", "dev-co5-local", "-user", "nav_it_sup_tec", "-password", "calypso" };
		try {

			DSConnection dsConDevCo5 = ConnectionUtil.connect(args, "MainEntry");
		} catch (ConnectException e1) {
			Log.error(ReadBloombergQuotesClientTest.class, e1); //sonar
		}
	}

	public static void printResponse(Efs2Response response) {

		System.out.println("Response Code: " + response.getResponseCode());
		System.out.println("Response Desc: " + response.getResponseDescription());
		System.out.println("Internal Code: " + response.getInternalCode());
		System.out.println("Internal Desc: " + response.getInternalDescription());
		System.out.println("Response Value: " + response.getValue());

	}

	// xml generated from valid instruments on EFS side
	public static String test1BuildXML() throws JAXBException {

		String isins[] = { "ES0000011595", "ES0000011660", "ES0000011868", "ES0000012098", "XS0042695782",
				"XS0043041879", "XS0043098127", "XS0045071932" };

		QuotesListRequest quotelist1 = new QuotesListRequest();
		quotelist1.setType("bond");

		for (String inst : isins) {
			final QuoteRequest q = new QuoteRequest();
			q.setIsin(inst);
			q.setCurrency("EUR");
			quotelist1.getQuote().add(q);
		}

		final QuotesListsRequests in = new QuotesListsRequests();
		in.getQuotesList().add(quotelist1);
		// in.getQuotesList().add(quotelist2); //not mandatory!

		// xml marshalling
		JAXBContext context = JAXBContext.newInstance(QuotesListsRequests.class);
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		// m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://www.w3.org/2001/XMLSchema");
		m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

		// Write to System.out. just to check
		m.marshal(in, System.out);

		// pass to String
		String test = "";
		StringWriter sw = new StringWriter();
		m.marshal(in, sw);

		test = sw.toString();
		return test;
	}
}
