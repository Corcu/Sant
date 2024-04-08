/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package com.isban.efs2.webservice;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import calypsox.tk.collateral.service.efsonlineservice.EFSException;
import calypsox.tk.collateral.service.efsonlineservice.ISINsFromBOPositions;
import calypsox.tk.collateral.service.efsonlineservice.response.QuoteResponse;
import calypsox.tk.collateral.service.efsonlineservice.response.QuotesListResponse;
import calypsox.tk.collateral.service.efsonlineservice.response.QuotesListsResponses;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;

@SuppressWarnings({ "unused" })
public class wsTests {

	// just the possible tests...
	public static void main(String pepe[]) throws Exception {

		connectToDSServer();

		// read quotes from Positions impacted on calypso
		testIsinsReadandGeneratedFromPositions();

		// marshalling and unmarshalling a simulated response from EFS
		test2_ResponseXML();

	}

	// DS connection to the server
	private static void connectToDSServer() {

		String args[] = { "-env", "dev-co5-local", "-user", "nav_it_sup_tec", "-password", "calypso" };
		try {

			DSConnection dsConDevCo5 = ConnectionUtil.connect(args, "MainEntry");
		} catch (ConnectException e1) {
			Log.error(wsTests.class, e1); //sonar
		}
	}

	// test one
	private static void testIsinsReadandGeneratedFromPositions() throws EFSException {

		ISINsFromBOPositions is = new ISINsFromBOPositions(false);
		String clause = is.inventorySqlWhereClause(JDate.getNow()).toString();

		is.gatherBOPositions(JDate.getNow());

		is.retrieveISINsListFromPositions();

		is.updateContextSubscriptionList();

	}

	// marshall y unmarshall objetos quotesListR
	public static void test2_ResponseXML() throws JAXBException, FileNotFoundException {

		final String FILE_PATH = "C:/Users/Guillermo/Dropbox/00 Everis/0 Santander - Calypso/02 - Collaterals Desarrollo/workspace_alt/EFS2WebServiceClientPrueba_v0.2/resources/";// "src/com/isban/efs2/webservice/xml/";
		final String FILE_NAME = "Response_quotes1.xml";
		final String XML = "./test1.xml";
		String file = FILE_PATH + FILE_NAME;
		String xml = getXmlFromFile(file);

		QuotesListsResponses in = new QuotesListsResponses();
		QuotesListResponse list1 = new QuotesListResponse();
		QuotesListResponse list2 = new QuotesListResponse();

		QuoteResponse q1 = new QuoteResponse();
		q1.setIsin("ES086359162");
		q1.setCurrency("EUR");
		q1.setBid(1.234);
		q1.setAsk(150.32);

		QuoteResponse q2 = new QuoteResponse();
		q2.setIsin("ES035162957");
		q2.setCurrency("EUR");
		q2.setBid(1.234);
		q2.setAsk(150.32);

		list1.setType("bond");
		list1.getQuote().add(q1);
		list1.getQuote().add(q2);

		QuoteResponse q3 = new QuoteResponse();
		q3.setIsin("UK1395473");
		q3.setCurrency("GBP");
		q3.setAsk(102.4);

		QuoteResponse q4 = new QuoteResponse();
		q4.setIsin("ES035162958");
		q4.setCurrency("EUR");
		q4.setAsk(162.5);

		list2.setType("equity");
		list2.getQuote().add(q3);
		list2.getQuote().add(q4);

		in.getQuotesList().add(list1);
		in.getQuotesList().add(list2);

		// create JAXB context and instantiate marshaller
		JAXBContext context = JAXBContext.newInstance(QuotesListsResponses.class);
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

		// Write to System.out
		m.marshal(in, System.out);

		// Write to File
		m.marshal(in, new File(XML));

		String test = "";
		StringWriter sw = new StringWriter();
		// m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://www.w3.org/2001/XMLSchema");
		m.marshal(in, sw);

		test = sw.toString();

		// get the xml
		// get variables from our xml file, created before
		System.out.println();
		System.out.println("Output from our XML File: ");
		Unmarshaller um = context.createUnmarshaller();
		QuotesListsResponses out = (QuotesListsResponses) um.unmarshal(new FileReader(XML));
		List<QuotesListResponse> list = out.getQuotesList();

		// test 2
		final Document xmlDocument = createDocument(test);
		QuotesListsResponses out2 = (QuotesListsResponses) um.unmarshal(xmlDocument);
		System.out.println(out.toString());

	}

	// show response options
	public static void printResponse(Efs2Response response) {

		System.out.println("Response Code: " + response.getResponseCode());
		System.out.println("Response Desc: " + response.getResponseDescription());
		System.out.println("Internal Code: " + response.getInternalCode());
		System.out.println("Internal Desc: " + response.getInternalDescription());
		System.out.println("Response Value: " + response.getValue());

	}

	// retrieve xml from file
	public static String getXmlFromFile(String file) {
		String payload = null;
		try {
			InputStream instream = new BufferedInputStream(new FileInputStream(file));
			int size = instream.available();
			byte[] bytesRead = new byte[size];
			instream.read(bytesRead);
			payload = new String(bytesRead);
			instream.close();
		} catch (IOException e) {
			System.out.println("Error: unable to load XML file - " + e.getMessage());
			Log.error(wsTests.class, e); //sonar
		}
		return payload;
	}

	// create a Document containing the XML
	private static Document createDocument(final String msg) {
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
			Log.error(wsTests.class, e1); //sonar
		}

		return rst;
	}

}
