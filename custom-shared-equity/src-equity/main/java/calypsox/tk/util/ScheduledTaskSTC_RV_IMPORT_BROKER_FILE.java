package calypsox.tk.util;


import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Fee;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.product.Equity;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.util.TradeArray;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;


public class ScheduledTaskSTC_RV_IMPORT_BROKER_FILE extends ScheduledTask {


	private static final String ST_ATTR_FILE_PATH = "File Path";
	private static final String ST_ATTR_FILE_NAME = "File Name";
	private static final String ST_ATTR_FILE_EXTENSION = "File Extension";
	private static final String ST_ATTR_FIELD_DELIMITER = "Field Delimiter";
	private static final String ST_ATTR_TRADE_ACTION = "Trade Action";
	private static final String ST_ATTR_BROKER = "Broker";
	private static final String ISIN = "ISIN";
	private static final String FEE_TYPE_EXCHANGE_FEE = "EXCHANGE_FEE";
	private static final String FEE_TYPE_BRK = "BRK";
	private static final String FEE_METHOD_NONE = "NONE";
	private static final String TRADE_DIRECTION_BUY = "BUY";
	private static final String TRADE_DIRECTION_SELL = "SELL";
	private static final String TRADE_ATTR_BROKER_VALIDATION = "BROKER_VALIDATION";
	private static final String TRADE_ATTR_ID_BOOKING_BROKER = "ID_BOOKING_BROKER";
	private static final String EXCEPTION_MISSING_TRADE_BROKER_FEE = "EX_MISSING_TRADE_BROKER_FEE";
	private static final String LINE_POSITION = "LINE_POSITION";
	private static final String LINE_BUY_SELL = "BUY_SELL";
	private static final String LINE_QUANTITY = "QUANTITY";
	private static final String LINE_AVERAGE_PRICE = "AVERAGE_PRICE";
	private static final String LINE_MARKET_FEE = "MARKET_FEE";
	private static final String LINE_GROSS_AMOUNT = "GROSS_AMOUNT";
	private static final String LINE_BROKERAGE = "BROKERAGE";
	private static final String LINE_SETTLE_DATE = "SETTLE_DATE";
	private static final String LINE_ISIN_CODE = "ISIN_CODE";
	private static final String LINE_CURRENCY = "CURRENCY";
	private static final String LINE_ID_BOOKING = "ID_BOOKING";
	private static final String FOLDER_OK = "ok";

	private String cpty = "";
	private JDatetime valuationDate = null;
	private String fieldDelimiter = "";
	private String fileLocation = "";
	private String oldFileLocation = "";
	private DSConnection dsCon= null;
	private String tradeAction = "";
	private Double settlementTolerance = 0.0;
	private Double feeTolerance = 0.0;

	// List of Case A: One Trade versus One Line
	List<FeeTradeAssociationOneToOne> listOneToOne = null;
	// List of Case B: N Lines versus 1 Trade
	List<FeeTradeAssociationNtoOne> listNtoOne = null;
	// List of Case C: One Line versus N Trades
	List<FeeTradeAssociationOneToN> listOneToN = null;

	// List of associations
	List<FeeTradeAssociation> associationList = null;
	// List of associations already processed
	List<FeeTradeAssociation> processedAssocList = null;
	// List of trades already processed
	TradeArray processedTrades = null;
	// List of lines already processed
	List<String> processedLines = null;

	// Heaader mapping
	HashMap<String, String> headerMap = new HashMap<String, String>();

	@Override
	public String getTaskInformation() {
		return "This Scheduled Task import the equity broker file";
	}


	/**
	 * ST Attributes Definition
	 */
	@Override
	protected List<AttributeDefinition> buildAttributeDefinition() {
		List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
		attributeList.addAll(super.buildAttributeDefinition());
		attributeList.add(attribute(ST_ATTR_FILE_PATH));
		attributeList.add(attribute(ST_ATTR_FILE_NAME));
		attributeList.add(attribute(ST_ATTR_FILE_EXTENSION));
		attributeList.add(attribute(ST_ATTR_FIELD_DELIMITER));
		attributeList.add(attribute(ST_ATTR_BROKER));
		attributeList.add(attribute(ST_ATTR_TRADE_ACTION));
		return attributeList;
	}


	@Override
	protected boolean process(final DSConnection ds, final PSConnection ps) {
		List<HashMap<String,String>> lines = new ArrayList<>();
		TradeArray trades = null;

		// Initialize
		init(ds);

		// Get the file
		final Vector<String> vectorLines = readFileAndCheckDeltaLines();
		if(vectorLines==null || vectorLines.size()<=1) {
			Log.info(this, "The input file is empty.");
			return true;
		}

		// Read the lines from file
		lines = getInfoFromFileLines(ds, vectorLines, fieldDelimiter);
		if(lines == null || lines.size() < 1) {
			Log.info(this, "Imput file is empty");
			return true;
		}

		// Get Equity trades entered today for the counterparty
		trades = getTrades();
		if(trades == null || trades.size() == 0) {
			Log.info(this, "Trades selection is empty");
			return true;
		}

		printLogLinesAndTrades(lines, trades, 0);

		// Associate lines to trades
		initialAssociationAndGenerateCaseA(lines, trades);

		printLogAssociationCaseA();

		// New line list for cases B and C
		List<HashMap<String,String>> linesForBandC = getNewLines(lines);
		// New trade list for cases B and C
		TradeArray tradesForBandC = getNewTrades(trades);

		printLogLinesAndTrades(linesForBandC, tradesForBandC,1);

		if(tradesForBandC != null && tradesForBandC.size() > 0 && linesForBandC != null && linesForBandC.size() >0) {
			// Associate lines to trades
			associationList = initialAssociationCaseBandC(linesForBandC, tradesForBandC);
			printLogAssociationList();
			if(associationList != null && associationList.size() > 0) {
				// Generate cases B 8and C
				generateCasesBandC(associationList);
			}
		}

		printLogAssociationCaseBandC();

		manageFees();

		manageIdBooking();

		processReconciliation();

		// Summary of Lines, Trades and Cases
		summary(lines, trades);

		moveFileSubFolder(FOLDER_OK);

		return true;
	}


	private void init(final DSConnection ds) {
		this.dsCon = ds;
		this.fieldDelimiter = getAttribute(ST_ATTR_FIELD_DELIMITER);
		this.cpty = getAttribute(ST_ATTR_BROKER);
		this.valuationDate = getValuationDatetime();
		Date date = valuationDate.getJDate(TimeZone.getDefault()).getDate();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		String formatedTime = dateFormat.format(date);
		this.fileLocation = getAttribute(ST_ATTR_FILE_PATH) + getAttribute(ST_ATTR_FILE_NAME) + formatedTime + "." + getAttribute(ST_ATTR_FILE_EXTENSION);
		this.oldFileLocation = getAttribute(ST_ATTR_FILE_PATH) + FOLDER_OK  + "/" + getAttribute(ST_ATTR_FILE_NAME) + formatedTime + "." + getAttribute(ST_ATTR_FILE_EXTENSION);
		this.tradeAction = getAttribute(ST_ATTR_TRADE_ACTION);

		String settleTolerance = LocalCache.getDomainValueComment(DSConnection.getDefault(),
				"domainName", "BrokerFeeSettlementTolerance");
		if(!Util.isEmpty(settleTolerance)) {
			settlementTolerance = Double.valueOf(settleTolerance);
		}

		String feeAmountTolerance = LocalCache.getDomainValueComment(DSConnection.getDefault(),
				"domainName", "BrokerFeeAmountTolerance");
		if(!Util.isEmpty(feeAmountTolerance)) {
			feeTolerance = Double.valueOf(feeAmountTolerance);
		}

		// Initialize the list of Case A: One Line versus One Trade
		listOneToOne = new ArrayList<FeeTradeAssociationOneToOne>();

		// Initialize the list of Case B: N Lines versus One Trade
		listNtoOne = new ArrayList<FeeTradeAssociationNtoOne>();

		// Initialize the list of Case C: One Line versus N Trades
		listOneToN = new ArrayList<FeeTradeAssociationOneToN>();

		// Initialize the list of all associations
		associationList = new ArrayList<FeeTradeAssociation>();

		// Initialize the list of associations already processed
		processedAssocList = new ArrayList<FeeTradeAssociation>();

		// Initialize the arrayList of trades already processed
		processedTrades = new TradeArray();

		// Initialize the arrayList of lines already processed
		processedLines = new ArrayList<String>();

		// Header mapping
		headerMap.put("BUY_SELL", "BUY_SELL");
		headerMap.put("QUANTITY", "QUANTITY");
		headerMap.put("AVERAGE_PRICE", "AVERAGE_PRICE");
		headerMap.put("MARKET_FEE", "MARKET_FEE");
		headerMap.put("GROSS_AMOUNT", "GROSS_AMOUNT");
		headerMap.put("BROKERAGE", "BROKERAGE");
		headerMap.put("NET_AMOUNT", "NET_AMOUNT");
		headerMap.put("BOOKING_DATE", "BOOKING_DATE");
		headerMap.put("SETTLE_DATE", "SETTLE_DATE");
		headerMap.put("ID_BOOKING", "ID_BOOKING");
		headerMap.put("ISIN_CODE" ,"ISIN_CODE");
		headerMap.put("CURRENCY", "CURRENCY");
		headerMap.put("ISIN_DESCRIPTION", "ISIN_DESCRIPTION");
		headerMap.put("ALIAS", "ALIAS");
		headerMap.put("MARKET", "MARKET");
		headerMap.put("GROSS_AVERAGE_PRICE","AVERAGE_PRICE");
		headerMap.put("MARKET_TRADING_FEE","MARKET_FEE");
		headerMap.put("GROSS_CLIENT_AMOUNT","GROSS_AMOUNT");
		headerMap.put("TRADE_DATE","BOOKING_DATE");
		headerMap.put("THEOR_SETT_DATE","SETTLE_DATE");
		headerMap.put("BOOKING_ID","ID_BOOKING");
		headerMap.put("ISIN_NAME","ISIN_DESCRIPTION");
		headerMap.put("CLIENT","ALIAS");
	}



	/**
	 * Read the input file
	 *
	 * @return
	 */
	private Vector<String> readFileAndCheckDeltaLines() {

		Vector<String> vector = new Vector<String>();
		Vector<String> oldVector = new Vector<String>();
		BufferedReader inputFileStream = null;
		BufferedReader oldInputFileStream = null;

		// Checks if there is a old file
		if (!Util.isEmpty(this.oldFileLocation)) {
			try {
				oldInputFileStream = new BufferedReader(new FileReader(this.oldFileLocation));
				String line;
				while (oldInputFileStream.ready()) {
					line = oldInputFileStream.readLine();
					oldVector.add(line);
				}
				Log.info(Log.CALYPSOX, "Finished reading process of import broker fees of the old file");
			} catch (final FileNotFoundException e) {
				Log.info("File '" + this.oldFileLocation +"' not found", e);
			} catch (final Exception e) {
				Log.error("Reading Error", e);
			} finally {
				try {
					if (oldInputFileStream != null) {
						oldInputFileStream.close();
					}
				} catch (final Exception e) {
					Log.error("File Loader of old file", e);
				}
			}
		}
		else {
			Log.error(this, "Error while reading the file path or file name.");
		}

		// New file
		if (!Util.isEmpty(this.fileLocation)) {
			try {
				inputFileStream = new BufferedReader(new FileReader(this.fileLocation));
				String line;
				while (inputFileStream.ready()) {
					line = inputFileStream.readLine();
					vector.add(line);
				}
				Log.info(Log.CALYPSOX, "Finished reading process of import broker fees");
			} catch (final FileNotFoundException e) {
				Log.error("Error: File '" + this.fileLocation +"' not found", e);
			} catch (final Exception e) {
				Log.error("Reading Error", e);
			} finally {
				try {
					if (inputFileStream != null) {
						inputFileStream.close();
					}
				} catch (final Exception e) {
					Log.error("File Loader", e);
				}
			}
		}
		else {
			Log.error(this, "Error while reading the file path or file name.");
		}

		// Review duplicated lines
		if(oldVector == null || oldVector.size() < 1) {
			return vector;
		}
		else {
			Vector<String> finalVector = new Vector<String>();
			boolean delta = false;
			for (final String line : vector) {
				boolean match = false;
				for (final String oldLine : oldVector) {
					if(line.equalsIgnoreCase(oldLine)) {
						match= true;
						break;
					}
				}
				if(!match) {
					if(!delta) {
						finalVector.add(oldVector.get(0));
						delta = true;
					}
					finalVector.add(line);
				}
			}
			return finalVector;
		}

	}


	/**
	 * Return Broker Fees in a Map, from Lines
	 *
	 * @param ds
	 * @param vectorLines
	 * @return
	 */
	private List<HashMap<String,String>> getInfoFromFileLines(DSConnection ds, Vector<String> vectorLines, String delimiter) {
		final List<HashMap<String,String>> lines = new ArrayList<>();
		StringBuilder message = new StringBuilder();
		String[] header = null;
		int lineCount = 1;
		for (final String line : vectorLines) {
			// All lines less header (first one)
			if (!line.isEmpty() && vectorLines.firstElement().equals(line)) {
				header = line.split(delimiter);

				message.append("\n\n" + "*****  CABECERAS ANTES  *****" + "\n");
				for (int i = 0; i < header.length; i++){
					message.append(header[i] + " - " );
				}

				for (int i = 0; i < header.length; i++){
					String aux = header[i];
					header[i] = headerMap.get(header[i]);
					if(header[i]==null){
						header[i] = aux;
					}
				}

				message.append("\n\n" + "*****  CABECERAS DESPUES  *****" + "\n");
				for (int i = 0; i < header.length; i++){
					message.append(header[i] + " - ");
				}
			}
			else if (!line.isEmpty()) {
				String[] fields = line.split(delimiter);
				HashMap<String,String> bean = new HashMap<String,String>();
				for(int i=0;i<fields.length;i++) {
					bean.put(header[i].trim(), fields[i].trim());
				}
				bean.put(LINE_POSITION, String.valueOf(lineCount));
				lineCount++;
				lines.add(bean);
			}
		}

		Log.system(this.toString(), message.toString());
		return lines;
	}


	private TradeArray getTrades() {
		TradeArray tradeArray = null;
		Date date = valuationDate.getJDate(TimeZone.getDefault()).getDate();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String formatedDate = dateFormat.format(date);
		final StringBuilder fromClause = new StringBuilder();
		fromClause.append("product_desc product, legal_entity le, trade_keyword tk");

		final StringBuilder whereClause = new StringBuilder();
		whereClause.append("trade.trade_status = 'PENDING'");
		whereClause.append(" AND ");
		whereClause.append("TO_CHAR(trade.entered_date, 'YYYY-MM-DD') = '" + formatedDate + "'");
		whereClause.append(" AND ");
		whereClause.append("trade.product_id = product.product_id");
		whereClause.append(" AND ");
		whereClause.append("product.product_type = 'Equity'");
		whereClause.append(" AND ");
		whereClause.append("trade.trade_id = tk.trade_id");
		whereClause.append(" AND ");
		whereClause.append("tk.keyword_name = 'Broker'");
		whereClause.append(" AND ");
		whereClause.append("tk.keyword_value = '" + cpty + "'");

		try {
			tradeArray = dsCon.getRemoteTrade().getTrades(fromClause.toString(), whereClause.toString(), null, null);
		} catch (final RemoteException e) {
			Log.error(this, "Could not get the Trades.", e);
		}

		TradeArray trades = new TradeArray();
		if(tradeArray != null && tradeArray.size() > 0) {
			for (int i = 0; i < tradeArray.size(); i++) {
				Trade trade = tradeArray.get(i);
				String attrValidation = trade.getKeywordValue(TRADE_ATTR_BROKER_VALIDATION);
				if(trade != null && (attrValidation == null || (attrValidation!=null && !attrValidation.equalsIgnoreCase("true")))) {
					trades.add(trade);
				}
			}
		}

		return trades;
	}


	private void initialAssociationAndGenerateCaseA(List<HashMap<String,String>> lines, TradeArray trades){

		for (HashMap<String, String> line : lines) {
			if(!processedLines.contains(line.get(LINE_POSITION))) {
				//Trade foundTrade = null;
				for (int i = 0; i < trades.size(); i++) {
					//if (foundTrade==null && checkFeeVsTradeCaseA(line, trades.get(i)) && !tradeIsAlreadyProcessed(foundTrade)) {
					if (checkFeeVsTradeCaseA(line, trades.get(i)) && !tradeIsAlreadyProcessed(trades.get(i))) {
						if (checkReconcilitiationCaseA(line, trades.get(i))) {
							Trade foundTrade = trades.get(i);
							FeeTradeAssociationOneToOne assocOneToOne = new FeeTradeAssociationOneToOne(line, foundTrade);
							listOneToOne.add(assocOneToOne);
							processedAssocList.add(new FeeTradeAssociation(line, foundTrade, 1));
							processedTrades.add(foundTrade);
							processedLines.add(line.get(LINE_POSITION));
							break;
						}
					}
				}
			}
		}

		for (HashMap<String, String> line : lines) {
			if(!processedLines.contains(line.get(LINE_POSITION))) {
				Trade foundTrade = null;
				for (int i = 0; i < trades.size(); i++) {
					if (foundTrade == null && checkFeeVsTradeCaseA(line, trades.get(i))) {
						foundTrade = trades.get(i);
						if (!tradeIsAlreadyProcessed(foundTrade)) {
							FeeTradeAssociationOneToOne assocOneToOne = new FeeTradeAssociationOneToOne(line, foundTrade);
							listOneToOne.add(assocOneToOne);
							processedAssocList.add(new FeeTradeAssociation(line, foundTrade, 1));
							processedTrades.add(foundTrade);
							processedLines.add(line.get(LINE_POSITION));
						}
					}
				}
			}
		}

	}


	private void replaceAssocOnList(HashMap<String, String> line, Trade trade){

		final Iterator<FeeTradeAssociationOneToOne> iteratorCaseA = listOneToOne.iterator();
		while (iteratorCaseA.hasNext()) {
			FeeTradeAssociationOneToOne bean = iteratorCaseA.next();
			if(bean.getTrade().getLongId() ==  trade.getLongId()){
				bean.setLine(line);
			}
		}


		// Associate to the processed association
		final Iterator<FeeTradeAssociation> iteratorAssoc = processedAssocList.iterator();
		while (iteratorAssoc.hasNext()) {
			FeeTradeAssociation assoc = iteratorAssoc.next();
			if(assoc.getTrade().getLongId() == trade.getLongId()){
				assoc.setLine(line);
			}

		}
	}


	private List<FeeTradeAssociation> initialAssociationCaseBandC(List<HashMap<String,String>> lines, TradeArray trades){
		List<FeeTradeAssociation> assotiationList = new ArrayList<FeeTradeAssociation>();
		for (HashMap<String, String> line : lines) {
			for (int i = 0; i < trades.size(); i++) {
				if(!assocIsAlreadyProcessed(new FeeTradeAssociation(line, trades.get(i), 0), processedAssocList) && checkFeeVsTrade(line, trades.get(i))) {
					FeeTradeAssociation assotiation = new FeeTradeAssociation(line, trades.get(i), 0);
					assotiationList.add(assotiation);
				}
			}
		}
		return assotiationList;
	}


	private boolean checkReconcilitiationCaseA(HashMap<String, String> line, Trade trade) {
		boolean rst = false;
		Vector<Fee> feeList = trade.getFeesList();
		Double tradeBkrAmount = 0.0;
		if(feeList!=null && feeList.size()>0) {
			for(Fee fee : trade.getFeesList()) {
				if(fee != null && FEE_TYPE_BRK.equalsIgnoreCase(fee.getType())) {
					tradeBkrAmount = tradeBkrAmount + Math.abs(fee.getAmount());
				}
			}
		}
		Double tradePrice = Math.abs(trade.getTradePrice());
		Double tradeSettlementAmount = Math.abs(trade.getQuantity() * trade.getTradePrice());
		Double tradeQuantity = Math.abs(trade.getQuantity());

		Double linePrice = Math.abs(new Double(line.get(LINE_AVERAGE_PRICE).replace(".","").replace(",",".")));
		Double lineGrossAmount = Math.abs(new Double(line.get(LINE_GROSS_AMOUNT).replace(".","").replace(",",".")));
		Double lineBrokerage = Math.abs(new Double(line.get(LINE_BROKERAGE).replace(".","").replace(",",".")));
		Double lineQuantity = Math.abs(new Double(line.get(LINE_QUANTITY).replace(".","").replace(",",".")));

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		if(nf.format(tradePrice).equalsIgnoreCase(nf.format(linePrice)) &&
				checkToleranceSettlementAmount(tradeSettlementAmount, lineGrossAmount) &&
				checkToleranceFeeAmount(tradeBkrAmount, lineBrokerage) &&
				nf.format(tradeQuantity).equalsIgnoreCase(nf.format(lineQuantity))) {
			rst = true;
		}

		return rst;
	}


	private boolean checkFeeVsTrade(HashMap<String, String> line, Trade trade) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		String tradeIsin = trade.getProduct().getSecCode(ISIN);
		String lineCodIsin = line.get(LINE_ISIN_CODE);
		String tradeSettleDate = dateFormat.format(trade.getSettleDate().getDate());
		String lineFxTeorLiq = line.get(LINE_SETTLE_DATE);
		Equity equity = (Equity)trade.getProduct();
		String tradeBuySell = equity.getBuySell(trade)==1 ? TRADE_DIRECTION_BUY : TRADE_DIRECTION_SELL;
		String lineBuySell = line.get(LINE_BUY_SELL);

		if(tradeIsin.equalsIgnoreCase(lineCodIsin) &&
				tradeSettleDate.equalsIgnoreCase(lineFxTeorLiq) &&
				tradeBuySell.equalsIgnoreCase(lineBuySell)) {
			return true;
		}
		return false;
	}


	private boolean checkFeeVsTradeCaseA(HashMap<String, String> line, Trade trade) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);

		String tradeIsin = trade.getProduct().getSecCode(ISIN);
		String lineCodIsin = line.get(LINE_ISIN_CODE);
		String tradeSettleDate = dateFormat.format(trade.getSettleDate().getDate());
		String lineFxTeorLiq = line.get(LINE_SETTLE_DATE);
		Equity equity = (Equity)trade.getProduct();
		String tradeBuySell = equity.getBuySell(trade)==1 ? TRADE_DIRECTION_BUY : TRADE_DIRECTION_SELL;
		String lineBuySell = line.get(LINE_BUY_SELL);
		String tradeQuantity = nf.format(Math.abs(trade.getQuantity()));
		String lineQuantity = nf.format(Math.abs(new Double(line.get(LINE_QUANTITY).replace(".","").replace(",","."))));

		if(tradeIsin.equalsIgnoreCase(lineCodIsin) &&
				tradeSettleDate.equalsIgnoreCase(lineFxTeorLiq) &&
				tradeBuySell.equalsIgnoreCase(lineBuySell) &&
				tradeQuantity.equalsIgnoreCase(lineQuantity)) {
			return true;
		}
		return false;
	}


	private void summary(List<HashMap<String, String>> lines, TradeArray trades) {

		// Check if all lines exists as trades in Calypso
		summarizeLines(lines);

		// Check if all trades entered today are processed
		summarizeTrades(trades);

		// Check all cases
		summarizeCases();
	}


	private void summarizeLines(List<HashMap<String,String>> lines) {
		StringBuilder message = new StringBuilder("\n\n\n" + "---  RESUMEN  ---");
		message.append("\n\n" + "---  LINEAS NO PROCESADAS  ---" + "\n");
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");

		for (HashMap<String, String> line : lines) {
			StringBuilder taskMessage = new StringBuilder("");
			boolean found = false;

			final Iterator<FeeTradeAssociationOneToOne> iteratorA = listOneToOne.iterator();
			while (iteratorA.hasNext() && !found) {
				HashMap<String, String> beanLine = iteratorA.next().getLine();
				if(line.get(LINE_POSITION).equalsIgnoreCase(beanLine.get(LINE_POSITION))) {
					found = true;
				}
			}

			final Iterator<FeeTradeAssociationNtoOne> iteratorB = listNtoOne.iterator();
			while (iteratorB.hasNext() && !found) {
				List<HashMap<String, String>> beanLineList = iteratorB.next().getLineList();
				final Iterator<HashMap<String, String>> iteratorLineList = beanLineList.iterator();
				while (iteratorLineList.hasNext() && !found) {
					HashMap<String, String> beanLine = iteratorLineList.next();
					if(line.get(LINE_POSITION).equalsIgnoreCase(beanLine.get(LINE_POSITION))) {
						found = true;
					}
				}
			}

			final Iterator<FeeTradeAssociationOneToN> iteratorC = listOneToN.iterator();
			while (iteratorC.hasNext() && !found) {
				HashMap<String, String> beanLine = iteratorC.next().getLine();
				if(line.get(LINE_POSITION).equalsIgnoreCase(beanLine.get(LINE_POSITION))) {
					found = true;
				}
			}

			if(!found) {
				taskMessage.append("Line ");
				taskMessage.append(line.get(LINE_POSITION));
				taskMessage.append(" of broker '");
				taskMessage.append(this.cpty);
				taskMessage.append("' on ");
				taskMessage.append(dateFormat.format(this.valuationDate));
				taskMessage.append(" do not match with any trade of Calypso --> ");
				taskMessage.append("ISIN: ");
				taskMessage.append(line.get(LINE_ISIN_CODE));
				taskMessage.append(" - ");
				taskMessage.append("Direction: ");
				taskMessage.append(line.get(LINE_BUY_SELL));
				taskMessage.append(" - ");
				taskMessage.append("Quantity: ");
				taskMessage.append(line.get(LINE_QUANTITY));
				taskMessage.append(" - ");
				taskMessage.append("Settlement Date: ");
				taskMessage.append(line.get(LINE_SETTLE_DATE));
				taskMessage.append(" - ");
				taskMessage.append("Price: ");
				taskMessage.append(line.get(LINE_AVERAGE_PRICE));
				taskMessage.append(" - ");
				taskMessage.append("Settlement Amount: ");
				taskMessage.append(line.get(LINE_GROSS_AMOUNT));
				taskMessage.append(" - ");
				taskMessage.append("Brokerage: ");
				taskMessage.append(line.get(LINE_BROKERAGE));

				Task taskException = new Task();
				taskException.setStatus(Task.NEW);
				taskException.setEventClass(Task.EXCEPTION_EVENT_CLASS);
				taskException.setEventType(EXCEPTION_MISSING_TRADE_BROKER_FEE);
				taskException.setComment(taskMessage.toString());
				TaskArray taskArray = new TaskArray();
				taskArray.add(taskException);
				try {
					dsCon.getRemoteBackOffice().saveAndPublishTasks(taskArray, 0L, null);
				}
				catch (CalypsoServiceException e) {
					Log.error(this, "Could not save the exception task.");
				}

				message.append(taskMessage.toString());
				message.append("\n");
			}
		}

		Log.system(this.toString(), message.toString());
	}


	private void summarizeTrades(TradeArray trades) {
		StringBuilder message = new StringBuilder("\n" + "---  TRADES NO PROCESADOS  ---" + "\n");

		for (int i = 0; i < trades.size(); i++) {
			boolean found = false;
			Trade trade = trades.get(i);
			if (listOneToOne != null && listOneToOne.size()>0) {
				final Iterator<FeeTradeAssociationOneToOne> iteratorA = listOneToOne.iterator();
				while (iteratorA.hasNext() && !found) {
					Trade beanTrade = iteratorA.next().getTrade();
					if(trade.getLongId() == beanTrade.getLongId()) {
						found = true;
					}
				}
			}
			if (listNtoOne != null && listNtoOne.size()>0) {
				final Iterator<FeeTradeAssociationNtoOne> iteratorB = listNtoOne.iterator();
				while (iteratorB.hasNext() && !found) {
					Trade beanTrade = iteratorB.next().getTrade();
					if(trade.getLongId() == beanTrade.getLongId()) {
						found = true;
					}
				}
			}
			if (listOneToN != null && listOneToN.size()>0) {
				final Iterator<FeeTradeAssociationOneToN> iteratorC = listOneToN.iterator();
				while (iteratorC.hasNext() && !found) {
					List<Trade> beanTradeList = iteratorC.next().getTradeList();
					final Iterator<Trade> iteratorTradeList = beanTradeList.iterator();
					while (iteratorTradeList.hasNext() && !found) {
						Trade beanTrade = iteratorTradeList.next();
						if(trade.getLongId() == beanTrade.getLongId()) {
							found = true;
						}
					}
				}
			}
			if(!found) {
				message.append("Trade " + trade.getLongId() + " do match with any line of the file." + "\n");
			}
		}
		Log.system(this.toString(), message.toString());
	}


	private void summarizeCases() {
		StringBuilder message = new StringBuilder("\n\n" + "---  ASSOCIATIONS  ---" + "\n");

		if (listOneToOne != null && listOneToOne.size()>0) {
			final Iterator<FeeTradeAssociationOneToOne> iteratorA = listOneToOne.iterator();
			while (iteratorA.hasNext()) {
				FeeTradeAssociationOneToOne assocA = iteratorA.next();
				message.append("Case A: Trade " + assocA.getTrade().getLongId() + " - Line " + assocA.getLine().get(LINE_POSITION) + "\n");
			}
		}
		else {
			message.append("There is not association for the case A: 1 line vs 1 trade." + "\n");
		}

		if (listNtoOne != null && listNtoOne.size()>0) {
			final Iterator<FeeTradeAssociationNtoOne> iteratorB = listNtoOne.iterator();
			while (iteratorB.hasNext()) {
				FeeTradeAssociationNtoOne assocB = iteratorB.next();
				message.append("Case B: Trade " + assocB.getTrade().getLongId() + " - Lines " );
				List<HashMap<String, String>> beanLineList = assocB.getLineList();
				final Iterator<HashMap<String, String>> iteratorLineList = beanLineList.iterator();
				while (iteratorLineList.hasNext()) {
					message.append(iteratorLineList.next().get(LINE_POSITION) + " ");
				}
				message.append("\n");
			}
		}
		else {
			message.append("There is not association for the case B: N lines vs 1 trade." + "\n");
		}

		if (listOneToN != null && listOneToN.size()>0) {
			final Iterator<FeeTradeAssociationOneToN> iteratorC = listOneToN.iterator();
			while (iteratorC.hasNext()) {
				FeeTradeAssociationOneToN assocC = iteratorC.next();
				message.append("Case C: Trades");
				List<Trade> beanTradeList = assocC.getTradeList();
				final Iterator<Trade> iteratorTradeList = beanTradeList.iterator();
				while (iteratorTradeList.hasNext()) {
					message.append(" " + iteratorTradeList.next().getLongId());
				}
				message.append(" - Line " + assocC.getLine().get(LINE_POSITION) + "\n");
			}
		}
		else {
			message.append("There is not association for the case C: 1 line vs N trades." + "\n");
		}

		Log.system(this.toString(), message.toString());
	}


	private void generateCasesBandC(List<FeeTradeAssociation> assotiationList) {

		//TradeArray processedTradesByC = new TradeArray();
		//List<String> processedLinesByC = new ArrayList<String>();
		// Temporary association list to compare
		List<FeeTradeAssociation> tempList = new ArrayList<FeeTradeAssociation>(assotiationList);

		final Iterator<FeeTradeAssociation> iterator = assotiationList.iterator();
		while (iterator.hasNext()) {

			List<Trade> tradeList = new ArrayList<Trade>();
			List<HashMap<String,String>> lineList = new ArrayList<HashMap<String,String>>();
			FeeTradeAssociation association = iterator.next();

			if(assocIsAlreadyProcessed(association, processedAssocList)) {
				continue;
			}

			int countCaseB=0;
			int countCaseC=0;
			HashMap<String,String> line = association.getLine();
			Trade trade = association.getTrade();

			final Iterator<FeeTradeAssociation> tempIterator = tempList.iterator();
			while (tempIterator.hasNext()) {
				FeeTradeAssociation tempAssoc = tempIterator.next();
				HashMap<String,String> tempLine = tempAssoc.getLine();
				Trade tempTrade = tempAssoc.getTrade();
				if(!isEqualLineInfo(line, tempLine) && isEqualTradeInfo(trade, tempTrade)) {
					countCaseB++;
					lineList.add(tempLine);
				}
				else if(isEqualLineInfo(line, tempLine) && !isEqualTradeInfo(trade, tempTrade)) {
					countCaseC++;
					tradeList.add(tempTrade);
				}
			};

			if(countCaseB>0) {
				association.setTypeOfcase(3);
				lineList.add(line);
				FeeTradeAssociationNtoOne assocNtoOne = new FeeTradeAssociationNtoOne(lineList, trade);
				listNtoOne.add(assocNtoOne);
				// Associate to the processed association
				final Iterator<HashMap<String,String>> processedIterator = lineList.iterator();
				while (processedIterator.hasNext()) {
					HashMap<String,String> processedLine = processedIterator.next();
					processedAssocList.add( new FeeTradeAssociation(processedLine, trade, 3));
				}
			}
			else if(countCaseC>0) {
				association.setTypeOfcase(2);
				tradeList.add(trade);
				FeeTradeAssociationOneToN assocOneToN = new FeeTradeAssociationOneToN(line, tradeList);
				listOneToN.add(assocOneToN);
				// Associate to the processed association
				final Iterator<Trade> processedIterator = tradeList.iterator();
				while (processedIterator.hasNext()) {
					Trade processedTrade = processedIterator.next();
					processedAssocList.add( new FeeTradeAssociation(line, processedTrade, 2));
				}
			}
		}
	}


	private boolean assocIsAlreadyProcessed(FeeTradeAssociation association, List<FeeTradeAssociation> processedAssocList) {
		boolean rst = false;
		final Iterator<FeeTradeAssociation> iterator = processedAssocList.iterator();
		while (iterator.hasNext()) {
			FeeTradeAssociation listAssoc = iterator.next();
			if(association.getLine().equals(listAssoc.getLine()) &&
					association.getTrade().equals(listAssoc.getTrade())) {
				rst = true;
			}
		}
		return rst;
	}


	private boolean tradeIsAlreadyProcessed(Trade trade) {
		boolean rst = false;
		if(processedTrades != null && processedTrades.size() > 0) {
			for (int i = 0; i < processedTrades.size(); i++) {
				if(trade.getLongId() == processedTrades.get(i).getLongId()) {
					rst= true;
					break;
				}
			}
		}
		return rst;
	}


	private boolean lineIsAlreadyProcessed(String line) {
		boolean rst = false;
		if(processedLines != null && processedLines.size() > 0) {
			for (int i = 0; i < processedLines.size(); i++) {
				if(line.equalsIgnoreCase(processedLines.get(i))) {
					rst= true;
					break;
				}
			}
		}
		return rst;
	}


	private boolean isEqualLineInfo (HashMap<String, String> line1, HashMap<String, String> line2) {
		boolean equal = false;
		String isin1 = line1.get(LINE_ISIN_CODE);
		String isin2 = line2.get(LINE_ISIN_CODE);
		String settleDate1 = line1.get(LINE_SETTLE_DATE);
		String settleDate2 = line2.get(LINE_SETTLE_DATE);
		String buySell = line1.get(LINE_BUY_SELL);
		String buySell2 = line2.get(LINE_BUY_SELL);
		String quantity1 = line1.get(LINE_QUANTITY);
		String quantity2 = line2.get(LINE_QUANTITY);
		String linePosition1 = line1.get(LINE_POSITION);
		String linePosition2 = line2.get(LINE_POSITION);

		if(isin1.equalsIgnoreCase(isin2) && settleDate1.equalsIgnoreCase(settleDate2) &&
				buySell.equalsIgnoreCase(buySell2) && quantity1.equalsIgnoreCase(quantity2) &&
				linePosition1.equalsIgnoreCase(linePosition2)) {
			equal= true;
		}
		return equal;
	}


	private boolean isEqualTradeInfo (Trade t1, Trade t2) {
		boolean equal = false;
		String isin1 = t1.getProduct().getSecCode(ISIN);
		String isin2 = t2.getProduct().getSecCode(ISIN);
		String settleDate1 = t1.getSettleDate().toSQLString();
		String settleDate2 = t2.getSettleDate().toSQLString();;
		Equity equity1 = (Equity)t1.getProduct();
		String buySell1 = equity1.getBuySell(t1)==1 ? TRADE_DIRECTION_BUY : TRADE_DIRECTION_SELL;
		Equity equity2 = (Equity)t2.getProduct();
		String buySell2 = equity2.getBuySell(t2)==1 ? TRADE_DIRECTION_BUY : TRADE_DIRECTION_SELL;
		String titulos1 = String.valueOf(t1.getQuantity());
		String titulos2 = String.valueOf(t2.getQuantity());
		String tradeId1 = String.valueOf(t1.getLongId());
		String tradeId2 = String.valueOf(t2.getLongId());

		if(isin1.equalsIgnoreCase(isin2) && settleDate1.equalsIgnoreCase(settleDate2) &&
				buySell1.equalsIgnoreCase(buySell2) && titulos1.equalsIgnoreCase(titulos2) &&
				tradeId1.equalsIgnoreCase(tradeId2)) {
			equal= true;
		}

		return equal;
	}


	private void manageFees() {

		StringBuilder msgFees = new StringBuilder("\n\n" + "*****  MANAGE FEES  *****" + "\n");

		String countries = LocalCache.getDomainValueComment(DSConnection.getDefault(),
				"domainName", "BrokerFeeExchangeFeeCountry");
		if(Util.isEmpty(countries)) {
			return;
		}

		// List of Case A: One Trade versus One Line
		if (listOneToOne != null && listOneToOne.size()>0) {
			final Iterator<FeeTradeAssociationOneToOne> iteratorCaseA = listOneToOne.iterator();
			while (iteratorCaseA.hasNext()) {
				FeeTradeAssociationOneToOne association = iteratorCaseA.next();
				HashMap<String,String> line = association.getLine();
				Trade trade = association.getTrade();
				Equity equity = (Equity)trade.getProduct();
				if(countries.contains(equity.getCountry())) {
					createExchangeFee(line, trade);
					msgFees.append("Manage Fees Case A: Line " + line.get(LINE_POSITION) + " - Trade " + trade.getLongId() + "\n");
				}
			}
		}

		// List of Case B: N Lines versus 1 Trade
		if (listNtoOne != null && listNtoOne.size()>0) {
			final Iterator<FeeTradeAssociationNtoOne> iteratorCaseB = listNtoOne.iterator();
			while (iteratorCaseB.hasNext()) {
				FeeTradeAssociationNtoOne association = iteratorCaseB.next();
				Trade trade = association.getTrade();
				Equity equity = (Equity)trade.getProduct();
				if(countries.contains(equity.getCountry())) {
					List<HashMap<String,String>> lineList = association.getLineList();
					if(lineList != null && lineList.size() > 0) {
						HashMap<String,String> line = null;
						Double marketFee = new Double(0.0);
						final Iterator<HashMap<String, String>> iteratorLineList = lineList.iterator();
						while (iteratorLineList.hasNext()) {
							line = iteratorLineList.next();
							marketFee = marketFee + Double.valueOf(line.get(LINE_MARKET_FEE).replace(".","").replace(",","."));
						}
						line.put(LINE_MARKET_FEE, marketFee.toString().replace(".", ","));
						createExchangeFee(line, trade);
						msgFees.append("Manage Fees Case B: Line " + line.get(LINE_POSITION) + " - Trade " + trade.getLongId() + "\n");
					}
				}
			}
		}

		// List of Case C: One Line versus N Trades
		if (listOneToN != null && listOneToN.size()>0) {
			final Iterator<FeeTradeAssociationOneToN> iteratorCaseC = listOneToN.iterator();
			while (iteratorCaseC.hasNext()) {
				FeeTradeAssociationOneToN association = iteratorCaseC.next();
				HashMap<String,String> line = association.getLine();
				List<Trade> tradeList = association.getTradeList();
				Trade trade = null;
				if(tradeList != null && tradeList.size() > 0){
					for(Trade t : tradeList) {
						trade = t;
						break;
					}
				}
				if(trade != null) {
					Equity equity = (Equity)trade.getProduct();
					if(countries.contains(equity.getCountry())) {
						createExchangeFee(line, trade);
						msgFees.append("Manage Fees Case C: Line " + line.get(LINE_POSITION) + " - Trade " + trade.getLongId() + "\n");
					}
				}
			}
		}

		Log.system(this.toString(), msgFees.toString());
	}


	private void manageIdBooking() {

		StringBuilder msgIdBooking = new StringBuilder("\n\n" + "*****  ID BOOKING  *****" + "\n");

		// List of Case A: One Trade versus One Line
		if (listOneToOne != null && listOneToOne.size()>0) {
			final Iterator<FeeTradeAssociationOneToOne> iteratorCaseA = listOneToOne.iterator();
			while (iteratorCaseA.hasNext()) {
				FeeTradeAssociationOneToOne association = iteratorCaseA.next();
				HashMap<String,String> line = association.getLine();
				String idBooking = association.getLine().get(LINE_ID_BOOKING);
				Trade trade = null;
				try {
					trade = dsCon.getRemoteTrade().getTrade(association.getTrade().getLongId());
				} catch (CalypsoServiceException e1) {
					Log.info(this, "Could not retrieve trade.");
				}

				if(trade!=null && !Util.isEmpty(idBooking)) {
					trade.addKeyword(TRADE_ATTR_ID_BOOKING_BROKER, idBooking);
					trade.setAction(Action.valueOf(Action.S_AMEND));
					try {
						dsCon.getRemoteTrade().save(trade);
						msgIdBooking.append("Id Booking Case A: Trade " + trade.getLongId() + " - IdBooking " + idBooking + "\n");
					} catch (CalypsoServiceException e) {
						Log.error(this, "Could not save the keyword " + TRADE_ATTR_ID_BOOKING_BROKER + " for trade " + trade.getLongId());
					}
				}
			}
		}

		// List of Case C: One Line versus N Trades
		if (listOneToN != null && listOneToN.size()>0) {
			final Iterator<FeeTradeAssociationOneToN> iteratorCaseC = listOneToN.iterator();
			while (iteratorCaseC.hasNext()) {
				FeeTradeAssociationOneToN association = iteratorCaseC.next();
				List<Trade> tradeList = association.getTradeList();
				String idBooking = association.getLine().get(LINE_ID_BOOKING);
				Trade trade = null;
				if(tradeList!=null && tradeList.size()>0){
					for(Trade t : tradeList) {
						if (t != null) {
							Trade newTrade = null;
							try {
								newTrade = dsCon.getRemoteTrade().getTrade(t.getLongId());
							} catch (CalypsoServiceException e1) {
								Log.info(this, "Could not retrieve trade.");
							}
							newTrade.addKeyword(TRADE_ATTR_ID_BOOKING_BROKER, idBooking);
							newTrade.setAction(Action.valueOf(Action.S_AMEND));
							try {
								dsCon.getRemoteTrade().save(newTrade);
								msgIdBooking.append("Id Booking Case C: Trade " + newTrade.getLongId() + " - IdBooking " + idBooking + "\n");
							} catch (CalypsoServiceException e) {
								Log.error(this, "Could not save the keyword " + TRADE_ATTR_ID_BOOKING_BROKER + " for trade " + newTrade.getLongId());
							}
						}
					}
				}
			}
		}

		Log.system(this.toString(), msgIdBooking.toString());
	}


	private void createExchangeFee(HashMap<String,String> line, Trade trade) {
		Fee fee = new Fee();
		fee.setType(FEE_TYPE_EXCHANGE_FEE);
		fee.setCurrency(line.get(LINE_CURRENCY));
		fee.setMethod(FEE_METHOD_NONE);
		fee.setAmount(Double.valueOf(line.get(LINE_MARKET_FEE).replace(".","").replace(",",".")) * -1);
		fee.setLegalEntityId(BOCache.getLegalEntity(DSConnection.getDefault(),cpty).getId());
		JDate feeDate = JDate.valueOf(line.get(LINE_SETTLE_DATE));
		fee.setDate(feeDate);
		fee.setStartDate(feeDate);
		fee.setEndDate(feeDate);
		trade.addFee(fee);
		trade.setAction(Action.valueOf(tradeAction));
		try {
			dsCon.getRemoteTrade().save(trade);
		} catch (CalypsoServiceException e) {
			Log.error(this, "Could not save the fee for trade " + trade.getLongId());
		}
	}


	private void processReconciliation() {
		// Reconciliation Case A: One Line versus One Trade
		processReconciliationCaseA();

		// Reconciliation Case B: N Lines versus One Trade
		processReconciliationCaseB();

		// Reconciliation Case C: One Line versus N Trades
		processReconciliationCaseC();
	}


	// Reconciliation Case A: One Line versus One Trade
	private void processReconciliationCaseA() {

		StringBuilder msgReconA = new StringBuilder("\n\n" + "*****  RECONCILIATION CASE A  *****" + "\n");
		if (listOneToOne != null && listOneToOne.size()>0) {
			final Iterator<FeeTradeAssociationOneToOne> iteratorCaseA = listOneToOne.iterator();
			while (iteratorCaseA.hasNext()) {
				FeeTradeAssociationOneToOne bean = iteratorCaseA.next();
				HashMap<String,String> line = bean.getLine();
				Trade trade = null;
				try {
					trade = dsCon.getRemoteTrade().getTrade(bean.getTrade().getLongId());
				} catch (CalypsoServiceException e1) {
					Log.info(this, "Could not retrieve trade.");
				}

				if(trade != null) {
					Vector<Fee> feeList = trade.getFeesList();
					Double tradeBkrAmount = 0.0;
					if(feeList!=null && feeList.size()>0) {
						for(Fee fee : trade.getFeesList()) {
							if(fee != null && FEE_TYPE_BRK.equalsIgnoreCase(fee.getType())) {
								tradeBkrAmount = tradeBkrAmount + Math.abs(fee.getAmount());
							}
						}
					}
					Double tradePrice = Math.abs(trade.getTradePrice());
					Double tradeSettlementAmount = Math.abs(trade.getQuantity() * trade.getTradePrice());
					Double tradeQuantity = Math.abs(trade.getQuantity());

					Double linePrice = Math.abs(new Double(line.get(LINE_AVERAGE_PRICE).replace(".","").replace(",",".")));
					Double lineGrossAmount = Math.abs(new Double(line.get(LINE_GROSS_AMOUNT).replace(".","").replace(",",".")));
					Double lineBrokerage = Math.abs(new Double(line.get(LINE_BROKERAGE).replace(".","").replace(",",".")));
					Double lineQuantity = Math.abs(new Double(line.get(LINE_QUANTITY).replace(".","").replace(",",".")));

					NumberFormat nf = NumberFormat.getInstance();
					nf.setMaximumFractionDigits(2);
					Boolean attrValue = false;
					if(nf.format(tradePrice).equalsIgnoreCase(nf.format(linePrice)) &&
							checkToleranceSettlementAmount(tradeSettlementAmount, lineGrossAmount) &&
							checkToleranceFeeAmount(tradeBkrAmount, lineBrokerage) &&
							nf.format(tradeQuantity).equalsIgnoreCase(nf.format(lineQuantity))) {
						attrValue = true;
					}

					trade.addKeyword(TRADE_ATTR_BROKER_VALIDATION, attrValue.toString());
					trade.setAction(Action.valueOf(tradeAction));
					try {
						dsCon.getRemoteTrade().save(trade);
						msgReconA.append("Reconciliation Case A: Trade " + trade.getLongId() + " - Broker Validation " + attrValue.toString() + "\n");
						if(!attrValue){
							if(!nf.format(tradePrice).equalsIgnoreCase(nf.format(linePrice))){
								msgReconA.append("     (TradePrice " + nf.format(tradePrice) + " - LinePrice " + nf.format(linePrice) + ")");
							}
							if(!checkToleranceSettlementAmount(tradeSettlementAmount, lineGrossAmount)){
								msgReconA.append("     (TradeSettlementAmount " + tradeSettlementAmount.toString() + " - LineSettlementAmount " + lineGrossAmount.toString() + "  -" + this.settlementTolerance + "-)");
							}
							if(!checkToleranceFeeAmount(tradeBkrAmount, lineBrokerage)){
								msgReconA.append("     (TradeBkrAmount " + tradeBkrAmount.toString() + " - LineBkrAmount " + lineBrokerage.toString() + "  -" + this.feeTolerance + "-)");
							}
							if(!nf.format(tradeQuantity).equalsIgnoreCase(nf.format(lineQuantity))){
								msgReconA.append("     (TradeQuantity " + nf.format(tradeQuantity) + " - LineQuantity " + nf.format(lineQuantity) + ")");
							}
							msgReconA.append("\n");
						}
					} catch (CalypsoServiceException e) {
						Log.error(this, "Could not save the keyword " + TRADE_ATTR_BROKER_VALIDATION + " for trade " + trade.getLongId());
					}
				}
			}
		}
		Log.system(this.toString(), msgReconA.toString());
	}


	// Reconciliation Case B: N Lines versus One Trade
	private void processReconciliationCaseB(){

		StringBuilder msgReconB = new StringBuilder("\n\n" + "*****  RECONCILIATION CASE B  *****" + "\n");
		if (listNtoOne != null && listNtoOne.size()>0) {
			final Iterator<FeeTradeAssociationNtoOne> iteratorCaseB = listNtoOne.iterator();
			while (iteratorCaseB.hasNext()) {
				FeeTradeAssociationNtoOne bean = iteratorCaseB.next();
				List<HashMap<String,String>> lineList = bean.getLineList();
				Trade trade = null;
				try {
					trade = dsCon.getRemoteTrade().getTrade(bean.getTrade().getLongId());
				} catch (CalypsoServiceException e1) {
					Log.info(this, "Could not retrieve trade.");
				}
				if(trade != null) {
					Vector<Fee> feeList = trade.getFeesList();
					Double tradeSettlementAmount = Math.abs(trade.getQuantity() * trade.getTradePrice());
					Double tradeQuantity = Math.abs(trade.getQuantity());
					Double tradeBkrAmount = 0.0;
					if(feeList!=null && feeList.size()>0) {
						for(Fee fee : trade.getFeesList()) {
							if(fee != null && FEE_TYPE_BRK.equalsIgnoreCase(fee.getType())) {
								tradeBkrAmount = tradeBkrAmount + Math.abs(fee.getAmount());
							}
						}
					}

					Double sumLineGrossAmount = 0.0;
					Double sumLineBrokerage = 0.0;
					Double sumLineQuantity = 0.0;
					if(lineList != null && lineList.size()>0) {
						for(HashMap<String,String> line : lineList) {
							sumLineGrossAmount = sumLineGrossAmount  + Math.abs(new Double(line.get(LINE_GROSS_AMOUNT).replace(".","").replace(",",".")));
							sumLineBrokerage = sumLineBrokerage + Math.abs(new Double(line.get(LINE_BROKERAGE).replace(".","").replace(",",".")));
							sumLineQuantity = sumLineQuantity + Math.abs(new Double(line.get(LINE_QUANTITY).replace(".","").replace(",",".")));
						}
					}

					NumberFormat nf = NumberFormat.getInstance();
					nf.setMaximumFractionDigits(2);
					Boolean attrValue = false;
					if(checkToleranceSettlementAmount(tradeSettlementAmount, sumLineGrossAmount) &&
							checkToleranceFeeAmount(tradeBkrAmount, sumLineBrokerage) &&
							nf.format(tradeQuantity).equalsIgnoreCase(nf.format(sumLineQuantity))) {
						attrValue = true;
					}
					trade.addKeyword(TRADE_ATTR_BROKER_VALIDATION, attrValue.toString());
					trade.setAction(Action.valueOf(tradeAction));
					try {
						dsCon.getRemoteTrade().save(trade);
						msgReconB.append("Reconciliation Case B: Trade " + trade.getLongId() + " - Broker Validation " + attrValue.toString() + "\n");
						if(!attrValue){
							if(!checkToleranceSettlementAmount(tradeSettlementAmount, sumLineGrossAmount)){
								msgReconB.append("     (TradeSettlementAmount " + tradeSettlementAmount.toString() + " - SumLinesSettlementAmount " + sumLineGrossAmount.toString() + this.settlementTolerance + "-)");
							}
							if(!checkToleranceFeeAmount(tradeBkrAmount, sumLineBrokerage)){
								msgReconB.append("     (TradeBkrAmount " + tradeBkrAmount.toString() + " - SumLinesBkrAmount " + sumLineBrokerage.toString() + "  -" + this.feeTolerance + "-)");
							}
							if(!nf.format(tradeQuantity).equalsIgnoreCase(nf.format(sumLineQuantity))){
								msgReconB.append("     (TradeQuantity " + nf.format(tradeQuantity) + " - SumLinesQuantity " + nf.format(sumLineQuantity) + ")");
							}
							msgReconB.append("\n");
						}
					} catch (CalypsoServiceException e) {
						Log.error(this, "Could not save the keyword " + TRADE_ATTR_BROKER_VALIDATION + " for trade " + trade.getLongId());
					}
				}
			}
		}
		Log.system(this.toString(), msgReconB.toString());
	}


	// Reconciliation Case C: One Line versus N Trades
	private void processReconciliationCaseC(){

		StringBuilder msgReconC = new StringBuilder("\n\n" + "*****  RECONCILIATION CASE C  *****" + "\n");
		if (listOneToN != null && listOneToN.size()>0) {
			final Iterator<FeeTradeAssociationOneToN> iteratorCaseC = listOneToN.iterator();
			while (iteratorCaseC.hasNext()) {
				FeeTradeAssociationOneToN bean = iteratorCaseC.next();
				HashMap<String,String> line = bean.getLine();
				List<Trade> tradeList = bean.getTradeList();

				Double lineGrossAmount = Math.abs(new Double(line.get(LINE_GROSS_AMOUNT).replace(".","").replace(",",".")));
				Double lineBrokerage = Math.abs(new Double(line.get(LINE_BROKERAGE).replace(".","").replace(",",".")));
				Double lineQuantity = Math.abs(new Double(line.get(LINE_QUANTITY).replace(".","").replace(",",".")));

				Double tradeSettlementAmount = 0.0;
				Double tradeBkrAmount = 0.0;
				Double tradeQuantity = 0.0;

				if(tradeList != null && tradeList.size()>0) {
					for(Trade trade : tradeList) {
						if (trade != null) {
							Trade newTrade = null;
							try {
								newTrade = dsCon.getRemoteTrade().getTrade(trade.getLongId());
							} catch (CalypsoServiceException e1) {
								Log.info(this, "Could not retrieve trade.");
							}
							if(newTrade != null) {
								tradeSettlementAmount = tradeSettlementAmount + Math.abs(newTrade.getQuantity() * newTrade.getTradePrice());
								tradeQuantity = tradeQuantity + Math.abs(newTrade.getQuantity());
								Vector<Fee> feeList = newTrade.getFeesList();
								if(feeList!=null && feeList.size()>0) {
									for(Fee fee : newTrade.getFeesList()) {
										if(fee != null && FEE_TYPE_BRK.equalsIgnoreCase(fee.getType())) {
											tradeBkrAmount = tradeBkrAmount + Math.abs(fee.getAmount());
										}
									}
								}
							}
						}
					}
				}

				NumberFormat nf = NumberFormat.getInstance();
				nf.setMaximumFractionDigits(2);
				Boolean attrValue = false;
				if(checkToleranceSettlementAmount(tradeSettlementAmount, lineGrossAmount) &&
						checkToleranceFeeAmount(tradeBkrAmount, lineBrokerage) &&
						nf.format(tradeQuantity).equalsIgnoreCase(nf.format(lineQuantity))) {
					attrValue = true;
				}
				if(tradeList != null && tradeList.size()>0) {
					for(Trade trade : tradeList) {
						if (trade != null) {
							Trade newTrade = null;
							try {
								newTrade = dsCon.getRemoteTrade().getTrade(trade.getLongId());
							} catch (CalypsoServiceException e1) {
								Log.info(this, "Could not retrieve trade.");
							}
							if(newTrade != null) {
								newTrade.addKeyword(TRADE_ATTR_BROKER_VALIDATION, attrValue.toString());
								newTrade.setAction(Action.valueOf(tradeAction));
								try {
									dsCon.getRemoteTrade().save(newTrade);
									msgReconC.append("Reconciliation Case C: Trade " + newTrade.getLongId() + " - Broker Validation " + attrValue.toString() + "\n");
									if(!attrValue){
										if(!checkToleranceSettlementAmount(tradeSettlementAmount, lineGrossAmount)){
											msgReconC.append("     (SumTradesSettlementAmount " + tradeSettlementAmount.toString() + " - LineSettlementAmount " + lineGrossAmount.toString() + " -" + this.settlementTolerance + "-)");
										}
										if(!checkToleranceFeeAmount(tradeBkrAmount, lineBrokerage)){
											msgReconC.append("     (SumTradesBkrAmount " + tradeBkrAmount.toString() + " - LineBkrAmount " + lineBrokerage.toString() + " -" + this.feeTolerance + "-)");
										}
										if(!nf.format(tradeQuantity).equalsIgnoreCase(nf.format(lineQuantity))){
											msgReconC.append("     (SumTradesQuantity " + nf.format(tradeQuantity) + " - LineQuantity " + nf.format(lineQuantity) + ")");
										}
										msgReconC.append("\n");
									}
								} catch (CalypsoServiceException e) {
									Log.error(this, "Could not save the keyword " + TRADE_ATTR_BROKER_VALIDATION + " for trade " + newTrade.getLongId());
								}
							}
						}
					}
				}
			}
		}
		Log.system(this.toString(), msgReconC.toString());
	}


	private List<HashMap<String,String>> getNewLines(List<HashMap<String,String>> lines) {

		if(listOneToOne != null && listOneToOne.size()>0) {
			List<HashMap<String,String>> newLines = new ArrayList<>();
			if(lines != null && lines.size() > 1) {
				for (HashMap<String, String> line : lines) {
					boolean exist = false;
					final Iterator<FeeTradeAssociationOneToOne> iteratorCaseA = listOneToOne.iterator();
					while (iteratorCaseA.hasNext()) {
						FeeTradeAssociationOneToOne assoc = iteratorCaseA.next();
						if(assoc.getLine().get(LINE_POSITION) == line.get(LINE_POSITION)){
							exist = true;
							break;
						}
					}
					if(!exist){
						newLines.add(line);
					}
				}
			}
			return newLines;
		}
		else {
			return lines;
		}
	}


	private TradeArray getNewTrades(TradeArray trades) {
		if(listOneToOne != null && listOneToOne.size()>0) {
			TradeArray newTrades = new TradeArray();
			for (int i = 0; i < trades.size(); i++) {
				boolean exist = false;
				final Iterator<FeeTradeAssociationOneToOne> iteratorCaseA = listOneToOne.iterator();
				while (iteratorCaseA.hasNext()) {
					FeeTradeAssociationOneToOne assoc = iteratorCaseA.next();
					if(assoc.getTrade().getLongId() == trades.get(i).getLongId()){
						exist = true;
						break;
					}
				}
				if(!exist){
					newTrades.add(trades.get(i));
				}
			}
			return newTrades;
		}
		else {
			return trades;
		}
	}


	private boolean checkToleranceSettlementAmount(Double tradeAmount, Double lineAmount) {
		if((tradeAmount > lineAmount-this.settlementTolerance) && (tradeAmount < lineAmount+this.settlementTolerance)) {
			return true;
		}
		else {
			return false;
		}
	}


	private boolean checkToleranceFeeAmount(Double tradeFeeAmount, Double lineFeeAmount) {
		if((tradeFeeAmount > lineFeeAmount-this.feeTolerance) && (tradeFeeAmount < lineFeeAmount+this.feeTolerance)) {
			return true;
		}
		else {
			return false;
		}
	}


	private void moveFileSubFolder(String subFolder) {
		Date date = valuationDate.getJDate(TimeZone.getDefault()).getDate();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		String formatedTime = dateFormat.format(date);
		String fileDestiny = getAttribute(ST_ATTR_FILE_PATH) + subFolder + "/" + getAttribute(ST_ATTR_FILE_NAME) + formatedTime + "." + getAttribute(ST_ATTR_FILE_EXTENSION);
		try {
			FileUtilityEquity.moveFile(this.fileLocation, fileDestiny);
		}
		catch (final IOException e) {
			Log.error(this, e.getMessage(), e);
		}
	}


	private void printLogLinesAndTrades(List<HashMap<String,String>> lines, TradeArray trades, int phase){

		StringBuilder msgLines = new StringBuilder("\n");
		if(phase==0){
			msgLines.append("\n\n" + "*****  INITIAL LISTS  *****" + "\n");
		}
		else if(phase==1){
			msgLines.append("\n\n" + "*****  LISTS AFTER CASE A, READY FOR B AND C  *****" + "\n");
		}

		if(phase==0){
			msgLines.append("\n" + "---  LINES  ---  ");
		}
		else if(phase==1){
			msgLines.append("\n" + "---  LINES AFTER CASE A, READY FOR B AND C  ---  ");
		}

		if(lines != null && lines.size() > 0) {
			msgLines.append("(" + lines.size() + ")" + "\n");
			for (HashMap<String, String> line : lines) {
				for (Map.Entry<String, String> entry : line.entrySet()) {
					msgLines.append(entry.getKey() + "= " + entry.getValue() + "  ");
				}
				msgLines.append("\n");
			}
		}
		else{
			msgLines.append("Line list is empty." + "\n");
		}

		if(phase==0){
			msgLines.append("\n" + "---  TRADES  ---  ");
		}
		else if(phase==1){
			msgLines.append("\n" + "---  TRADES AFTER CASE A, READY FOR B AND C  ---  ");
		}
		if(trades != null && trades.size() > 0) {
			msgLines.append("(" + trades.size() + ")" + "\n");
			for (int i = 0; i < trades.size(); i++) {
				Equity equity = (Equity)trades.get(i).getProduct();
				String tradeBuySell = equity.getBuySell(trades.get(i))==1 ? TRADE_DIRECTION_BUY : TRADE_DIRECTION_SELL;

				msgLines.append(trades.get(i).getLongId() + " -->");
				msgLines.append("  Isin: " + trades.get(i).getProduct().getSecCode(ISIN));
				msgLines.append("  SettleDate: " + trades.get(i).getSettleDate().getDate());
				msgLines.append("  Direction: " + tradeBuySell);
				msgLines.append("  Quantity: " + trades.get(i).getQuantity());
				msgLines.append("  Price: " + trades.get(i).getTradePrice());
				msgLines.append("\n");
			}
		}
		else{
			msgLines.append("Trade list is empty." + "\n");
		}

		Log.system(this.toString(), msgLines.toString());
	}


	private void printLogAssociationList(){
		StringBuilder msgListAssotiation = new StringBuilder("\n" + "---  ASSOCIATION LIST  ---");
		if(associationList != null && associationList.size() > 0) {
			msgListAssotiation.append("  (" + associationList.size() + ")" + "\n");
			final Iterator<FeeTradeAssociation> iteratorAssot = associationList.iterator();
			while (iteratorAssot.hasNext()) {
				FeeTradeAssociation bean = iteratorAssot.next();
				msgListAssotiation.append("Linea " + bean.getLine().get(LINE_POSITION) + " - " + "Trade " + bean.getTrade().getLongId() + "\n");
			}
		}
		else{
			msgListAssotiation.append("Association List is empty." + "\n");
		}
		Log.system(this.toString(), msgListAssotiation.toString());
	}


	private void printLogAssociationCaseA(){
		StringBuilder msgListCaseA = new StringBuilder("\n" + "---  LIST CASE A  ---");
		if(listOneToOne != null && listOneToOne.size() > 0) {
			msgListCaseA.append("  (" + listOneToOne.size() + ")" + "\n");
			final Iterator<FeeTradeAssociationOneToOne> iteratorCaseA = listOneToOne.iterator();
			while (iteratorCaseA.hasNext()) {
				FeeTradeAssociationOneToOne bean = iteratorCaseA.next();
				msgListCaseA.append("Linea " + bean.getLine().get(LINE_POSITION) + " - " + "Trade " + bean.getTrade().getLongId() + "\n");
			}
		}
		else{
			msgListCaseA.append("\n" + "List Case A is empty." + "\n");
		}
		Log.system(this.toString(), msgListCaseA.toString());
	}


	private void printLogAssociationCaseBandC(){
		StringBuilder msgListCaseBandC = new StringBuilder("\n" + "---  LIST CASE B  ---");
		if(listNtoOne != null && listNtoOne.size() > 0) {
			msgListCaseBandC.append("  (" + listNtoOne.size() + ")" + "\n");
			final Iterator<FeeTradeAssociationNtoOne> iteratorCaseB = listNtoOne.iterator();
			while (iteratorCaseB.hasNext()) {
				FeeTradeAssociationNtoOne bean = iteratorCaseB.next();
				List<HashMap<String, String>> beanLineList = bean.getLineList();
				final Iterator<HashMap<String, String>> iteratorLineList = beanLineList.iterator();
				if(iteratorLineList != null) {
					msgListCaseBandC.append("Lineas ");
					while (iteratorLineList.hasNext()) {
						HashMap<String, String> beanLine = iteratorLineList.next();
						msgListCaseBandC.append(beanLine.get(LINE_POSITION) + " ");
					}
					msgListCaseBandC.append("- " + "Trade " + bean.getTrade().getLongId() + "\n");
				}
			}
		}
		else{
			msgListCaseBandC.append("\n" + "List Case B is empty." + "\n");
		}

		msgListCaseBandC.append("\n" + "---  LIST CASE C  ---");
		if(listOneToN != null && listOneToN.size() > 0) {
			msgListCaseBandC.append("  (" + listOneToN.size() + ")" + "\n");
			final Iterator<FeeTradeAssociationOneToN> iteratorCaseC = listOneToN.iterator();
			while (iteratorCaseC.hasNext()) {
				FeeTradeAssociationOneToN bean = iteratorCaseC.next();
				msgListCaseBandC.append("Linea " + bean.getLine().get(LINE_POSITION) + " - " + "Trades ");
				List<Trade> tradeList = bean.getTradeList();
				if(tradeList != null && tradeList.size() > 0){
					for(Trade trade : tradeList) {
						msgListCaseBandC.append(trade.getLongId() + " ");
					}
					msgListCaseBandC.append("\n");
				}
			}
		}
		else{
			msgListCaseBandC.append("\n" + "List Case C is empty." + "\n");
		}
		Log.system(this.toString(), msgListCaseBandC.toString());
	}


}
