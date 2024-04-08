/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.collateral.service.efsonlineservice;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import com.calypso.tk.util.ConnectionUtil;

import calypsox.tk.collateral.service.efsonlineservice.beans.QuoteBeanIN;
import calypsox.tk.collateral.service.efsonlineservice.beans.QuoteBeanOUT.QUOTE_TYPE;
import calypsox.tk.collateral.service.efsonlineservice.request.QuoteRequest;
import calypsox.tk.collateral.service.efsonlineservice.request.QuotesListRequest;
import calypsox.tk.collateral.service.efsonlineservice.request.QuotesListsRequests;

//GSM: Local test of the service to read online quotes (WS EFS)

public class EFSServiceTest {

	/**
	 * @param args
	 * @throws JAXBException
	 */
	public static void main(String[] args) throws Exception {

		createDSConnetion();

		// full test, using threads
		// fullthreadTest();

		// partial threadin test
		// partialPosThreadTest();

		// takes some time, sequential test
		// fullTest();

		// short list without reading positions (to make faster the test)
		partialTest();

		System.exit(0);

	}

	// tests reading isins and ccys from positions, calls and process the WS and stores it in Quotes inside Calypso.
	public static void fullthreadTest() throws EFSException {

		ISINsFromBOPositions readPos = new ISINsFromBOPositions(true);
		EFSBloombergService callws = new EFSBloombergService(true);
		QuotesDBPersistance writeDb = new QuotesDBPersistance(true);

		readPos.readProductsFromDatabasePositions();
		System.out.println(" Time to Process in sec ReadPos = " + readPos.getRunningTime());

		callws.startEfsWebService();
		System.out.println(" Time to Process in sec wsCall = " + callws.getRunningTime());

		writeDb.updateDatabase();
		System.out.println(" Time to Process in sec storeQuotes = " + writeDb.getRunningTime());

		do {

			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				Log.error(EFSServiceTest.class, e); //class
			}

		} while (readPos.isAlive());

	}

	// tests reading isins and ccys from positions, calls and process the WS and stores it in Quotes inside Calypso.
	public static void fullTest() throws EFSException {

		ISINsFromBOPositions readPos = new ISINsFromBOPositions(false);
		EFSBloombergService callws = new EFSBloombergService(false);
		QuotesDBPersistance writeDb = new QuotesDBPersistance(false);

		readPos.readProductsFromDatabasePositions();
		System.out.println(" Time to Process in sec ReadPos = " + readPos.getRunningTime());

		callws.startEfsWebService();
		System.out.println(" Time to Process in sec wsCall = " + callws.getRunningTime());

		writeDb.updateDatabase();
		System.out.println(" Time to Process in sec storeQuotes = " + writeDb.getRunningTime());

	}

	public static void partialPosThreadTest() {

		ISINsFromBOPositions readPos = new ISINsFromBOPositions(true);
		readPos.readProductsFromDatabasePositions();
	}

	public static void partialTest() throws JAXBException, EFSException {

		// ISINsFromBOPositions readPos = new ISINsFromBOPositions(false);
		final EFSContext context = EFSContext.getEFSInstance();
		// String xml = buildXMLtest();
		context.setBondsQuotes(generateBondsTest());

		EFSBloombergService callws = new EFSBloombergService(false);
		QuotesDBPersistance writeDb = new QuotesDBPersistance(false);

		callws.startEfsWebService();
		System.out.println(" Time to Process in sec wsCall = " + callws.getRunningTime());

		writeDb.updateDatabase();
		System.out.println(" Time to Process in sec storeQuotes = " + writeDb.getRunningTime());
	}

	// just to avoid positions call
	private static List<QuoteBeanIN> generateBondsTest() {

		List<QuoteBeanIN> l = new ArrayList<QuoteBeanIN>();

		String isins[] = { "ES0000011595", "ES0000011660", "ES0000011868", "ES0000012098", "XS0042695782",
				"XS0043041879", "XS0043098127", "XS0045071932" };

		for (String inst : isins) {
			final QuoteBeanIN q = new QuoteBeanIN();
			q.setType(QUOTE_TYPE.BOND);
			q.setCurrency("EUR");
			q.setISIN(inst);
			l.add(q);
		}
		return l;
	}

	// xml generated from valid instruments on EFS side
	public static String buildXMLtest() throws JAXBException {

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

	// just to have a DS conecction through main
	private static void createDSConnetion() {

		String args[] = { "-env", "dev4-local", "-user", "nav_it_sup_tec", "-password", "calypso" };
		try {
			@SuppressWarnings("unused")
			DSConnection dsConDevCo4 = ConnectionUtil.connect(args, "MainEntry");
		} catch (ConnectException e1) {
			Log.error(EFSServiceTest.class, e1); //sonar
		}

	}
}
