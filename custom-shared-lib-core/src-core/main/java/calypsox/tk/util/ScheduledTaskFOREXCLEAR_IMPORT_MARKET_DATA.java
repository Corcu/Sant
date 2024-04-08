package calypsox.tk.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.refdata.CurrencyPair;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import calypsox.tk.util.log.LogGeneric;
import calypsox.util.ForexClearFileReader;
import calypsox.util.ForexClearSTUtil;

public class ScheduledTaskFOREXCLEAR_IMPORT_MARKET_DATA extends ScheduledTask{

	public static final String SCHEDULED_TASK = "ScheduledTaskFOREXCLEAR_IMPORT_BOND_QUOTES";

	protected static final String QUOTE_SET = "Quote Set";
	protected static final String QUOTE_TYPE = "Quote Type";
	private static final List<String> DISCARD_CURRENCYS = Arrays.asList("EUR/USD", "EUR/GBP", "GBP/USD"); //Paolo rules!
	
	
	private boolean returnExit = true;
	private String quoteType = "";
	private String quoteSet = "";
	
	protected String fileName = "";
	protected String path = "";
	
	
	// Logs
    protected LogGeneric logGen = new LogGeneric();
	
	@Override
	public String getTaskInformation() {
		
		return "Import REP00018 - Daily Exchange Rates_ 1.TXT";
	}

	@Override
	protected boolean process(DSConnection ds, PSConnection ps) {
		this.quoteSet = getAttribute(QUOTE_SET);
		this.quoteType = getAttribute(QUOTE_TYPE);
		path = getAttribute(ForexClearSTUtil.FILE_PATH);
		final JDate date = this.getValuationDatetime().getJDate(TimeZone.getDefault());
		
		fileName = getAttribute(ForexClearSTUtil.FILE_NAME);
		fileName = ForexClearSTUtil.getFileName(date, fileName);
		 
		List<QuoteValue> quotes = new ArrayList<>();
		
		startLogs(date);

		if (!ForexClearFileReader.copyFile(path, fileName)) {
            Log.error(this, "ERROR: Failed to copy file");
            this.logGen.incrementError();
            this.logGen.setErrorMovingFile(this.getClass().getSimpleName(),
                    fileName);
            ForexClearSTUtil.returnErrorLog(logGen, false, date, fileName, path,
                    getAttribute(ForexClearSTUtil.SUMMARY_LOG),
                    this.getClass().getSimpleName());
            return false;
        }
		
		List<String> lines = readFile();
		
		List<ExChangeRate> excahngerates = adaptLines(lines);
		
		quotes = createQuotes(excahngerates);
		
		saveQuoteValue(ds,quotes);

		 // post process
        try {
            ForexClearFileReader.postProcess(this.returnExit, date, fileName,
                    path);
        } catch (Exception e1) {
            Log.error(this, e1); // sonar
            this.logGen.incrementError();
            this.logGen.setErrorMovingFile(this.getClass().getSimpleName(),
                    fileName);
        }
		
		stopLogs();
		
		return this.returnExit;
	}
	
	/**
	 * @return Lines of the file REP00018
	 */
	private List<String> readFile() {
		final List<String> lines = new ArrayList<String>();
		
		BufferedReader inputFileStream = null;
		
		if(!Util.isEmpty(fileName) && !Util.isEmpty(path)) {
			try {
				// We read the file.
				inputFileStream = new BufferedReader(new FileReader(path + fileName));
				String line;
				while (inputFileStream.ready()) {
					line = inputFileStream.readLine();
					lines.add(line);
					this.logGen.incrementTotal();
				}
	
				Log.info(Log.CALYPSOX, "Finished reading process of Collateral Prices file");
	
			} catch (final FileNotFoundException e) {
	
				Log.error("Error: File didn't found", e);
				this.returnExit = false;
	
			} catch (final Exception e) {
				Log.error("Reading Error", e);
				this.returnExit = false;
	
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
		
		return lines;
		
	}
	
	private List<ExChangeRate> adaptLines(List<String> lines) {
		List<ExChangeRate> rates = new ArrayList<>();
		
		if(!Util.isEmpty(lines)) {
			for (final String line : lines) {

				// All lines less header (first one)
				if (!line.isEmpty() && !lines.get(0).equals(line)) {
					try {
						ExChangeRate rate = new ExChangeRate();
						final String[] fields = line.split("\t");
						rate.setCobdate(stringToDate(fields[0])); //format to date
						rate.setFromCurrency(fields[1]);
						rate.setFromCurrencyName(fields[2]);
						rate.setToCurrency(fields[3]);
						rate.setToCurrencyName(fields[4]);
						rate.setExChangeRate(Double.valueOf(fields[5]));
						
						rates.add(rate);
					} catch (Exception e) {
						Log.error(this, "Cannot set line: " + line + "Error: " + e);
					}
				}
			}
		}
		
		
		return rates;
	}
	
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        attributeList.addAll(super.buildAttributeDefinition());
        attributeList.add(attribute(ForexClearSTUtil.FILE_NAME));
        attributeList.add(attribute(ForexClearSTUtil.FILE_PATH));
        
        attributeList.add(attribute(QUOTE_SET));
        attributeList.add(attribute(QUOTE_TYPE));
        
        // Logs
        attributeList.add(attribute(ForexClearSTUtil.SUMMARY_LOG));
        attributeList.add(attribute(ForexClearSTUtil.DETAILED_LOG));
        attributeList.add(attribute(ForexClearSTUtil.FULL_LOG));
        attributeList.add(attribute(ForexClearSTUtil.STATIC_DATA_LOG));
        // Logs
        return attributeList;
    }
    
    
    private void startLogs(final JDate date) {
        String time = "";
        synchronized (ForexClearSTUtil.timeFormat) {
        		final Date d = new Date();
            time = ForexClearSTUtil.timeFormat.format(d);
        }
        this.logGen.generateFiles(getAttribute(ForexClearSTUtil.DETAILED_LOG),
                getAttribute(ForexClearSTUtil.FULL_LOG),
                getAttribute(ForexClearSTUtil.STATIC_DATA_LOG), time);
        try {
            this.logGen.initializeFiles(this.getClass().getSimpleName());
        } catch (IOException e1) {
            this.logGen.incrementError();
            this.logGen.setErrorCreatingLogFile(this.getClass().getSimpleName(),
                    fileName);
            Log.error(this, e1);
        }
    }
    
    private void stopLogs() {
        try {
            this.logGen.feedGenericLogProcess(fileName,
                    getAttribute(ForexClearSTUtil.SUMMARY_LOG),
                    this.getClass().getSimpleName(),
                    this.logGen.getNumberTotal() - 1);
            this.logGen.feedFullLog(0);
            this.logGen.feedDetailedLog(0);
            //add OK lines
            this.logGen.initializeErrorLine();
            this.logGen.feedFullLog(0);
            this.logGen.closeLogFiles();
        } catch (final IOException e) {
            Log.error(this, e); // sonar
        }
    }
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Vector<String> getAttributeDomain(final String attr, final Hashtable currentAttr) {

		if(attr.equals(QUOTE_SET)) {
			Vector v = null;
			try {
				v = DSConnection.getDefault().getRemoteMarketData().getQuoteSetNames();
			} catch (final RemoteException e) {
				Log.error(this, e);
				Log.error(this, "Error loading Quote Set Names");
			}
			return v;
		}

		return super.getAttributeDomain(attr, currentAttr);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public boolean isValidInput(final Vector messages) {
		super.isValidInput(messages);

		if (getAttribute(QUOTE_SET).isEmpty()) {
			messages.add("The attribute QUOTE_SET is mandatory.");
		}

		if (getAttribute(QUOTE_TYPE).isEmpty()) {
			messages.add("The attribute QUOTE_TYPE is mandatory.");
		}
		
		return messages.isEmpty();
	}
	
	
	private List<QuoteValue> createQuotes(List<ExChangeRate> exChangeRates) {
		List<QuoteValue> quotes = new ArrayList<>();
		List<CurrencyPair> duplicated = new ArrayList<>();
		
		if(!Util.isEmpty(exChangeRates) && !Util.isEmpty(this.quoteSet) && !Util.isEmpty(this.quoteType)) {
			for(ExChangeRate exrate : exChangeRates) {
				if(discardCurrencies(exrate.getFromCurrency(),exrate.getToCurrency())) {
					CurrencyPair pair = getCurrencyPair(exrate.getFromCurrency(),exrate.getToCurrency());
					if(pair!=null && !duplicated.contains(pair)) {
						duplicated.add(pair);
						JDate date = exrate.getCobdate().getJDate(TimeZone.getDefault());
						QuoteValue result = new QuoteValue(this.quoteSet, pair.getQuoteName(),date, this.quoteType);
						result.setClose(exrate.getExChangeRate());
						logGen.incrementOK();
						logGen.setOkLine(this.getClass().getSimpleName(), " " + pair + " - Price: " + exrate.getExChangeRate(),logGen.getNumberOk(), " ");
						Log.info(this,this.quoteSet + "-"+this.quoteType+": "+ pair.getQuoteName() + " Value: "+ result.getClose());
						quotes.add(result);
					}else {
						logGen.incrementRecordErrors();
						logGen.setErrorSavingQuote(this.getClass().getSimpleName(), " " + exrate.getFromCurrency() +"."+exrate.getToCurrency() + " - Price: " + exrate.getExChangeRate()," " + (logGen.getNumberOk() + 1), "", "");
						Log.error(this, "Pair does not exist or duplicated in the file: " + exrate.getFromCurrency() +"."+exrate.getToCurrency());
					}
				}else {
					logGen.incrementRecordErrors();
					logGen.setErrorSavingQuote(this.getClass().getSimpleName(), " " + exrate.getFromCurrency() +"."+exrate.getToCurrency() + " - Price: " + exrate.getExChangeRate()," " + (logGen.getNumberOk() + 1), "", "");
					Log.error(this, "Discard this currency pair: " + exrate.getFromCurrency() +"."+exrate.getToCurrency());
				}
				
			}
		}
		
		return quotes;

	}
	
	private void saveQuoteValue(DSConnection dsconn,List<QuoteValue> quotes) {
		if(!Util.isEmpty(quotes)) {
			Vector<QuoteValue> quoteValues = new Vector(quotes);
			try {
				dsconn.getRemoteMarketData().saveQuoteValues(quoteValues);
			} catch (final RemoteException e) {
				Log.error(this, "Cannot save QuoteValues Error: " + e);
				this.returnExit = false;
			}
		}
	}
	
	
	private CurrencyPair getCurrencyPair(String tocurrency, String fromCurrency) {
		CurrencyPair pair = null;
		
			try {
				if (!fromCurrency.equals(tocurrency)) {
					pair = DSConnection.getDefault().getRemoteReferenceData().getCurrencyPair(fromCurrency, tocurrency);
					if (pair == null) {
						pair = DSConnection.getDefault().getRemoteReferenceData().getCurrencyPair(tocurrency, fromCurrency);
					}
				}
			} catch (Exception e) {
				Log.error(this, "Cannot retrieve currency pair", e);
			}
		return pair;
	}
	
	 public static JDatetime stringToDate(String datetime) {
		 SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy h:mm:ss");
		try {
			 return new JDatetime(format.parse(datetime));
			} catch (ParseException arg2) {
			   Log.warn(Log.LOG, "Error parsing string to JDatetime (\"dd/MM/yyyy h:mm:ss\")" + arg2);
		       return null;
			}
	 }

		
		protected String getDestinyFolder() {

			String outputFolder = getAttribute(ForexClearSTUtil.FILE_PATH);
			outputFolder = outputFolder.substring(0, outputFolder.length() - 1);
			return (new StringBuilder().append(outputFolder).append("/copy/")).toString();
		}
		
	private boolean discardCurrencies(String fromCurrency, String toCurrency ) {
		String pair = fromCurrency+"/"+toCurrency;
		if(DISCARD_CURRENCYS.contains(pair)) {
			return false;
		}
		return true;
	}
	
	private class ExChangeRate{
		private JDatetime cobdate;
		private String fromCurrency;
		private String fromCurrencyName;
		private String toCurrency;
		private String toCurrencyName;
		private Double changeRate;
		
		public JDatetime getCobdate() {
			return cobdate;
		}
		public void setCobdate(JDatetime cobdate) {
			this.cobdate = cobdate;
		}
		public String getFromCurrency() {
			return fromCurrency;
		}
		public void setFromCurrency(String fromCurrency) {
			this.fromCurrency = fromCurrency;
		}
		public String getFromCurrencyName() {
			return fromCurrencyName;
		}
		public void setFromCurrencyName(String fromCurrencyName) {
			this.fromCurrencyName = fromCurrencyName;
		}
		public String getToCurrency() {
			return toCurrency;
		}
		public void setToCurrency(String toCurrency) {
			this.toCurrency = toCurrency;
		}
		public String getToCurrencyName() {
			return toCurrencyName;
		}
		public void setToCurrencyName(String toCurrencyName) {
			this.toCurrencyName = toCurrencyName;
		}
		public Double getExChangeRate() {
			return changeRate;
		}
		public void setExChangeRate(Double exChangeRate) {
			this.changeRate = exChangeRate;
		}
	}

}
