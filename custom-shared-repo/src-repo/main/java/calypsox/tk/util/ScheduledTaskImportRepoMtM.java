package calypsox.tk.util;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.product.Repo;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TradeArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class ScheduledTaskImportRepoMtM extends ScheduledTask {
	
	private static final String FILE_SEPARATOR = ";";
	
    public static final String SCHEDULED_TASK = "ScheduledTaskImportRepoMtM";
    public static final String FILE_NAME_PREFIX = "Files Name Prefix";
    public static final String FILE_PATH = "Files Path";
    public static final String FILE_DATE_FORMAT = "File Date Format";
    public static final String MX_TRADE_KEYWORD = "Murex Trade Keyword";

    
    public static final String MARKETVALUEMAN_PLMARKNAME = "MARKETVALUEMAN";
    public static final String BUYSELLCASH_PLMARKNAME = "BUYSELLCASH";
    
    private boolean returnExit = true;

    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();
    private static int SQL_GET_SIZE = 999;

    @Override
    public String getTaskInformation() {
        return "Import Repo MtM";
    }

    @Override
    protected boolean process(DSConnection ds, PSConnection ps) {
        String path = getAttribute(FILE_PATH);
        String fileName = getAttribute(FILE_NAME_PREFIX);
        String fileDateFormat = getAttribute(FILE_DATE_FORMAT);
        if (Util.isEmpty(fileDateFormat)) {
        	fileDateFormat = "yyyyMMdd";
        }
        String mxTradeKw = getAttribute(MX_TRADE_KEYWORD);

    	JDate fileDate = getValuationDatetime().getJDate(TimeZone.getDefault());
    	SimpleDateFormat df = new SimpleDateFormat(fileDateFormat);
    	String fileDateS = df.format(fileDate.getDate());
        List<File> filesToProcess = getFilesToProcess(path, fileName, fileDateS);

    	List<String> lines = new ArrayList<String>();
    	if (filesToProcess != null && filesToProcess.size() > 0) {
    		for (File fileToProcess : filesToProcess) {
    			lines.addAll(readFile(fileToProcess));
    		} 
    	}
        
        Map<String, MTMData> mtmAllDataMap = parseLines(lines);

        TradeArray tradeArray = loadTradesByK(this.getDSConnection(), mtmAllDataMap, mxTradeKw);

        if (!Util.isEmpty(tradeArray)) {
            matchTades(mtmAllDataMap, tradeArray, mxTradeKw);
            final List<MTMData> finalList = mtmAllDataMap.entrySet().stream()
                    .filter(ent -> ent.getValue().getTrade() != null)
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());

            if (!Util.isEmpty(finalList)) {
            	createAndSavePlMarks(finalList, tradeArray);
            }
            else {
            	Log.error(this, "No PLMark to be created : empty list after filtering.");
            }
            

            Log.info(this, "All trades saved.");
        }

        return this.returnExit;
    }

    /**
     * @param ds
     * @param
     * @return
     */
    private TradeArray loadTradesByK(DSConnection ds, Map<String, MTMData> mtmAllDataMap, String mxTradeKw) {
        TradeArray trades = new TradeArray();
        String st = null;

        if (!Util.isEmpty(mtmAllDataMap)) {

            List<MTMData> mtmDataList = new ArrayList<>(mtmAllDataMap.values());
            int size = SQL_GET_SIZE;
            Log.info(this, "Loading trades by Trade Keyword");
            for (int start = 0; start < mtmDataList.size(); start += size) {
                int end = Math.min(start + size, mtmDataList.size());

                final List<String> listReferences = mtmDataList.subList(start, end).stream().map(MTMData::getDealID).collect(Collectors.toList());
                String references = String.join("','", listReferences);
                StringBuilder where = new StringBuilder();
                try {
                    where.append(" TRADE_KEYWORD.KEYWORD_NAME = ");
                    where.append("'" + mxTradeKw + "'");
                    where.append(" AND TRADE_KEYWORD.KEYWORD_VALUE IN (");
                    where.append("'" + references + "'");
                    where.append(")");
                    where.append(" AND TRADE_KEYWORD.TRADE_ID = TRADE.TRADE_ID");

                    trades.addAll(ds.getRemoteTrade().getTrades("trade_keyword", where.toString(), "", null));
                } catch (CalypsoServiceException e) {
                    Log.info("Cannot get trades for ", e);
                }
            }
        }
        Log.info(this, trades.size() + " Trades loaded before filtering.");
        
        if (!Util.isEmpty(getTradeFilter())) {
        	Log.info(this, "Filtering based on Trade Filter " + getTradeFilter());
        	TradeFilter tradeFilter = BOCache.getTradeFilter(DSConnection.getDefault(), getTradeFilter());
        	if (tradeFilter != null) {
        		for (int i = trades.size() - 1; i >= 0; i--) {
        			Trade currentTrade = trades.get(i);
        			if (!tradeFilter.accept(currentTrade)) {
        				Log.info(this, "Trade is not accepted by Trade Filter : " + currentTrade.getLongId());
        				trades.remove(i);
        			}
        		}
        	}
        }
        else {
        	Log.info(this, "No Trade Filter configured, no post-filtering.");
        }
        
        Log.info(this, trades.size() + " Trades loaded after filtering.");

        return trades;
    }

    /**
     * @param mtmAllDataMap
     * @param tradearray
     * @param keyword
     */
    private void matchTades(Map<String, MTMData> mtmAllDataMap, TradeArray tradearray, String keyword) {
        Log.info(this, "Matching trades.");

        List<String> references = new ArrayList<>();
        for (Trade trade : tradearray.getTrades()) {
        	String contractID = trade.getKeywordValue(keyword);
            if (!references.contains(contractID)) {
            	StringBuilder identifier = new StringBuilder();
            	identifier.append(String.valueOf(contractID));
            	identifier.append("_");
                if(trade.getProduct() instanceof Repo){
                    Repo repo = (Repo)trade.getProduct();
                    String direction = repo.getDirection(Repo.REPO, repo.getSign());
                    if (direction.equals("Reverse")) {
                        identifier.append("B");
                    } else {
                        identifier.append("S");
                    }
                }
                MTMData mtmData = mtmAllDataMap.get(identifier.toString());
                if (mtmData != null) {
                    mtmData.setTrade(trade);
                    references.add(contractID);
                } else {
                    Log.info(this, "Trade " + trade.getLongId() + " not found on file. ");
                }
            } else {
                Log.info(this, "Duplicate trade with same REFERENCE: " + contractID + " tradeId: " + trade.getLongId());
            }
        }
    }


    /**
     * @param
     * @return
     */
    private void createAndSavePlMarks(List<MTMData> mtmAllData, TradeArray tradeArray) {
        List<PLMark> plMarks = new ArrayList<>();

        Vector holidays = getHolidays();
        if (Util.isEmpty(holidays)) {
            holidays = new Vector();
            holidays.add("SYSTEM");
        }

        HashMap<String, Double> fxRates = new HashMap<String, Double>();

        String pricingEnv = getPricingEnv();
        TradeArray tradesToSave = new TradeArray();
        if (!Util.isEmpty(mtmAllData)) {
            Log.info(this, mtmAllData.size() + " Matched trades.");

            for (MTMData mtmData : mtmAllData) {
                Trade trade = mtmData.getTrade();
                if (trade == null) {
                    continue;
                }
                
                long mirrorTradeId = trade.getMirrorTradeLongId();
                if (mirrorTradeId > 0L) {
                    Trade mirrorTrade = getTradeWithId(tradeArray, mirrorTradeId);
                    if (trade.getLongId() < mirrorTradeId) {
                        addPLMarkForTrade(plMarks, trade, mtmData, tradesToSave, pricingEnv, holidays, null, fxRates, 1);
                        if (mirrorTrade != null) {
                            addPLMarkForTrade(plMarks, mirrorTrade, mtmData, tradesToSave, pricingEnv, holidays, null, fxRates, -1);
                        }
                    } else {
                        addPLMarkForTrade(plMarks, trade, mtmData, tradesToSave, pricingEnv, holidays, null, fxRates, -1);
                        if (mirrorTrade != null) {
                            addPLMarkForTrade(plMarks, mirrorTrade, mtmData, tradesToSave, pricingEnv, holidays, null, fxRates, 1);
                        }
                    }
                } else {
                    addPLMarkForTrade(plMarks, trade, mtmData, tradesToSave, pricingEnv, holidays, null, fxRates, 1);
                }
            }
        }

        try {
            savePLMarks(plMarks);
            if (!Util.isEmpty(tradesToSave)) {
            	DSConnection.getDefault().getRemoteTrade().saveTrades(new ExternalArray(tradesToSave.toVector()));
            }
        } catch (InterruptedException e) {
            Log.error(this, "Error saving PLMarks. " + e);
        } catch (CalypsoServiceException e) {
            Log.error(this, "Error saving Trades: " + e);
        } catch (InvalidClassException e) {
            Log.error(this, "Error : " + e);
        }
    }

    private Trade getTradeWithId(TradeArray tradeArray, long id) {
        for (Trade trade : tradeArray.asList()) {
            if (trade.getLongId() == id) {
                return trade;
            }
        }

        return null;
    }

    private void addPLMarkForTrade(List<PLMark> plMarks, Trade trade, MTMData mtmData, TradeArray tradesToSave, String pricingEnv, Vector holidays, TradeFilter tradeFilter, HashMap<String, Double> fxRates, int sign) {
        PLMark plMark = new PLMark();
        plMark.setTradeId(trade.getLongId());
        Book book = BOCache.getBook(DSConnection.getDefault(), trade.getBookId());
        if (book != null) {
            plMark.setBookId(book.getId());
        }
        plMark.setValDate(mtmData.getProcessDate());
        plMark.setPricingEnvName(pricingEnv);
        plMark.setType("PL");
        
        plMark.addPLMarkValue(createPLMarkValue(MARKETVALUEMAN_PLMARKNAME, mtmData.getCurrency(), mtmData.getCurrency(), mtmData.getMarketvalueMan() * sign, ""));
        plMark.addPLMarkValue(createPLMarkValue(BUYSELLCASH_PLMARKNAME, mtmData.getCurrency(), mtmData.getCurrency(), mtmData.getBuySellCash() * sign, ""));
        
        plMarks.add(plMark);
    }


    private PLMarkValue createPLMarkValue(String name, String ccy, String ccy2, double mtm, String type) {
        PLMarkValue npvPriceMarkValue = new PLMarkValue();
        npvPriceMarkValue.setMarkName(name);
        npvPriceMarkValue.setMarkValue(mtm);
        npvPriceMarkValue.setCurrency(ccy);
        npvPriceMarkValue.setOriginalCurrency(ccy2);
        return npvPriceMarkValue;
    }

    /**
     * @param plMarks
     * @throws InterruptedException
     */
    private void savePLMarks(List<PLMark> plMarks) throws InterruptedException {
        if (!Util.isEmpty(plMarks)) {
            int size = SQL_GET_SIZE;
            Log.info(this, "Saving " + plMarks.size() + " PLMarks.");
            ExecutorService exec = Executors.newFixedThreadPool(NUM_CORES);
            try {
                for (int start = 0; start < plMarks.size(); start += size) {
                    int end = Math.min(start + size, plMarks.size());
                    List<PLMark> plMarksToSave = new ArrayList<>(plMarks.subList(start, end));
                    exec.execute(
                            new Runnable() {
                                public void run() {
                                    try {
                                        DSConnection.getDefault().getRemoteMark().saveMarksWithAudit(plMarksToSave, true);
                                    } catch (PersistenceException e) {
                                        Log.error(this, "Cannot save PLMarks. " + e);
                                    }
                                }
                            });
                }
            } finally {
                exec.shutdown();
                exec.awaitTermination(40, TimeUnit.MINUTES);
            }
        }
    }

    /**
     * @return Lines of the file
     */
    private List<String> readFile(File file) {
        final List<String> lines = new ArrayList<String>();

        BufferedReader inputFileStream = null;

        if (file != null) {
            try {
                // We read the file.
                inputFileStream = new BufferedReader(new FileReader(file));
                String line;
                while (inputFileStream.ready()) {
                    line = inputFileStream.readLine();
                    lines.add(line);
                }

                Log.info(Log.CALYPSOX, "Finished reading file" + file.getName());

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

    private Map<String, MTMData> parseLines(List<String> lines) {
        Map<String, MTMData> mtmAllDataMap = new HashMap<String, MTMData>();

        if (!Util.isEmpty(lines)) {
            for (final String line : lines) {
                if (!line.isEmpty()) {
                    try {
                    	final String[] fields = line.split(FILE_SEPARATOR);

                    	MTMData mtmData = new MTMData();

                    	mtmData.setProcessDate(stringToDate(fields[mtmData.getProcessDatePos()]));
                    	mtmData.setContractID(fields[mtmData.getContractIDPos()]);
                    	mtmData.setDealID(fields[mtmData.getDealIDPos()]);
                    	mtmData.setBlockNumber(Integer.valueOf(fields[mtmData.getBlockNumberPos()]));
                    	mtmData.setFrontID(Long.valueOf(fields[mtmData.getFrontIDPos()]));
                    	mtmData.setDealGRID(Integer.valueOf(fields[mtmData.getDealGRIDPos()]));
                    	mtmData.setCurrency(fields[mtmData.getCurrencyPos()]);
                    	mtmData.setDummy(fields[mtmData.getDummyPos()]);
                    	mtmData.setInterestCash(Double.valueOf(fields[mtmData.getInterestCashPos()]));
                    	mtmData.setBuySellCash(Double.valueOf(fields[mtmData.getBuySellCashPos()]));
                    	mtmData.setTaxComCash(Double.valueOf(fields[mtmData.getTaxComCashPos()]));
                    	mtmData.setCarryAcc(Double.valueOf(fields[mtmData.getCarryAccPos()]));
                    	mtmData.setMarketvalueMan(Double.valueOf(fields[mtmData.getMarketvalueManPos()]));
                    	mtmData.setEntity(fields[mtmData.getEntityPos()]);
                    	mtmData.setProcessID(fields[mtmData.getProcessIDPos()]);
                    	mtmData.setGeneratorID(Integer.valueOf(fields[mtmData.getGeneratorIDPos()]));
                    	mtmData.setCompraVenta(fields[mtmData.getCompraVentaPos()]);
                    	mtmData.setPastCash(Double.valueOf(fields[mtmData.getPastCashPos()]));
                    	mtmData.setFutureCash(Double.valueOf(fields[mtmData.getFutureCashPos()]));

                    	StringBuilder identifier = new StringBuilder();
                    	identifier.append(String.valueOf(mtmData.getDealID()));
                    	identifier.append("_");
                    	identifier.append(mtmData.getCompraVenta());
                        String key = identifier.toString();
                        if (mtmAllDataMap.containsKey(key)) {
                            MTMData previousMtmData = mtmAllDataMap.get(key);
                            int dealGRID = mtmData.getDealGRID();
                            if (dealGRID > previousMtmData.getDealGRID()) {
                                mtmAllDataMap.replace(key, previousMtmData, mtmData);
                            }
                        } else {
                            mtmAllDataMap.put(key, mtmData);
                        }
                    } catch (Exception e) {
                    	Log.error(this, "Cannot set line: " + line + "Error: " + e);
                    }
                }
            }
        }


        return mtmAllDataMap;
    }

    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        attributeList.addAll(super.buildAttributeDefinition());
        attributeList.add(attribute(FILE_NAME_PREFIX));
        attributeList.add(attribute(FILE_PATH));
        attributeList.add(attribute(FILE_DATE_FORMAT));
        attributeList.add(attribute(MX_TRADE_KEYWORD));

        return attributeList;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Vector<String> getAttributeDomain(final String attr, final Hashtable currentAttr) {
        return super.getAttributeDomain(attr, currentAttr);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public boolean isValidInput(final Vector messages) {
        super.isValidInput(messages);

        return messages.isEmpty();
    }

    public static JDate stringToDate(String datetime) {
        String dFormat = "dd/MM/yyyy";
        SimpleDateFormat format = new SimpleDateFormat(dFormat);
        try {
            return JDate.valueOf(format.parse(datetime));
        } catch (ParseException e) {
            Log.warn(Log.LOG, "Error parsing string to JDatetime (" + dFormat + ")" + e.toString());
            return null;
        }
    }
    
	private static List<File> getFilesToProcess(final String rootPath, final String filePrefix, final String fileDate) {
		String path = rootPath;
		File folder = new File(path);
		File[] listOfFiles = folder.listFiles();
		
		List<File> foundFiles = new ArrayList<File>();
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				File currentFile = listOfFiles[i];
				if (currentFile.getName().startsWith(filePrefix) && currentFile.getName().contains(fileDate)) {
					foundFiles.add(currentFile);
				}
			}
		}
		return foundFiles;
	}
    
    /**
     * Represents one file line
     */
    private class MTMData {
        JDate processDate;
        String contractID;
        String dealID;
        int blockNumber;
        long frontID;
        int dealGRID;
        String currency;
        String dummy;
        double interestCash;
        double buySellCash;
        double taxComCash;
        double carryAcc;
        double marketvalueMan;
        String entity;
        String processID;
        int generatorID;
        String compraVenta;
        double pastCash;
        double futureCash;
        int processDatePos = 0;
		int contractIDPos = 1;
        int dealIDPos = 2;
        int blockNumberPos = 3;
        int frontIDPos = 4;
        int dealGRIDPos = 5;
        int currencyPos = 6;
        int dummyPos = 7;
        int interestCashPos = 8;
        int buySellCashPos = 9;
        int taxComCashPos = 10;
        int carryAccPos = 11;
        int marketvalueManPos = 12;
        int entityPos = 13;
        int processIDPos = 14;
        int generatorIDPos = 15;
        int compraVentaPos = 16;
        int pastCashPos = 17;
        int futureCashPos = 18;
        Trade trade = null;
        
        
        public JDate getProcessDate() {
			return processDate;
		}

		public void setProcessDate(JDate processDate) {
			this.processDate = processDate;
		}

		public String getContractID() {
			return contractID;
		}

		public void setContractID(String contractID) {
			this.contractID = contractID;
		}

		public String getDealID() {
			return dealID;
		}

		public void setDealID(String dealID) {
			this.dealID = dealID;
		}

		public int getBlockNumber() {
			return blockNumber;
		}

		public void setBlockNumber(int blockNumber) {
			this.blockNumber = blockNumber;
		}

		public long getFrontID() {
			return frontID;
		}

		public void setFrontID(long frontID) {
			this.frontID = frontID;
		}

		public int getDealGRID() {
			return dealGRID;
		}

		public void setDealGRID(int dealGRID) {
			this.dealGRID = dealGRID;
		}

		public String getCurrency() {
			return currency;
		}

		public void setCurrency(String currency) {
			this.currency = currency;
		}

		public String getDummy() {
			return dummy;
		}

		public void setDummy(String dummy) {
			this.dummy = dummy;
		}

		public double getInterestCash() {
			return interestCash;
		}

		public void setInterestCash(double interestCash) {
			this.interestCash = interestCash;
		}

		public double getBuySellCash() {
			return buySellCash;
		}

		public void setBuySellCash(double buySellCash) {
			this.buySellCash = buySellCash;
		}

		public double getTaxComCash() {
			return taxComCash;
		}

		public void setTaxComCash(double taxComCash) {
			this.taxComCash = taxComCash;
		}

		public double getCarryAcc() {
			return carryAcc;
		}

		public void setCarryAcc(double carryAcc) {
			this.carryAcc = carryAcc;
		}

		public double getMarketvalueMan() {
			return marketvalueMan;
		}

		public void setMarketvalueMan(double marketvalueMan) {
			this.marketvalueMan = marketvalueMan;
		}

		public String getEntity() {
			return entity;
		}

		public void setEntity(String entity) {
			this.entity = entity;
		}

		public String getProcessID() {
			return processID;
		}

		public void setProcessID(String processID) {
			this.processID = processID;
		}

		public int getGeneratorID() {
			return generatorID;
		}

		public void setGeneratorID(int generatorID) {
			this.generatorID = generatorID;
		}

		public String getCompraVenta() {
			return compraVenta;
		}

		public void setCompraVenta(String compraVenta) {
			this.compraVenta = compraVenta;
		}

		public double getPastCash() {
			return pastCash;
		}

		public void setPastCash(double pastCash) {
			this.pastCash = pastCash;
		}

		public double getFutureCash() {
			return futureCash;
		}

		public void setFutureCash(double futureCash) {
			this.futureCash = futureCash;
		}

		public int getProcessDatePos() {
			return processDatePos;
		}

		public int getContractIDPos() {
			return contractIDPos;
		}

		public int getDealIDPos() {
			return dealIDPos;
		}

		public int getBlockNumberPos() {
			return blockNumberPos;
		}

		public int getFrontIDPos() {
			return frontIDPos;
		}

		public int getDealGRIDPos() {
			return dealGRIDPos;
		}

		public int getCurrencyPos() {
			return currencyPos;
		}

		public int getDummyPos() {
			return dummyPos;
		}

		public int getInterestCashPos() {
			return interestCashPos;
		}

		public int getBuySellCashPos() {
			return buySellCashPos;
		}

		public int getTaxComCashPos() {
			return taxComCashPos;
		}

		public int getCarryAccPos() {
			return carryAccPos;
		}

		public int getMarketvalueManPos() {
			return marketvalueManPos;
		}

		public int getEntityPos() {
			return entityPos;
		}

		public int getProcessIDPos() {
			return processIDPos;
		}

		public int getGeneratorIDPos() {
			return generatorIDPos;
		}

		public int getCompraVentaPos() {
			return compraVentaPos;
		}

		public int getPastCashPos() {
			return pastCashPos;
		}

		public int getFutureCashPos() {
			return futureCashPos;
		}

        public Trade getTrade() {
            return trade;
        }

        public void setTrade(Trade trade) {
            this.trade = trade;
        }
    }
}
