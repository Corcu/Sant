package calypsox.tk.util.interfaceImporter;

import static calypsox.tk.util.ScheduledTaskImportCSVExposureTrades.ALLOW_FILES_WITH_MORE_FIELDS;
import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;
import calypsox.tk.util.ScheduledTaskImportCSVExposureTrades.FILES_TYPES;
import calypsox.tk.util.bean.InterfaceTradeBean;
import calypsox.util.TradeImportTracker;

import com.calypso.tk.core.Log;

import java.util.Arrays;
import java.util.Optional;

/**
 * Read from the flat file containing the exposures trades (line per line) and builds the bean containing the exposure
 * trade.
 * 
 * @author aela
 * @version 3.1
 * @date 22/08/2013
 * 
 */
class InterfaceFileReader implements FileReader<InterfaceTradeBean> {

	private static final String END_LINE_MARK = "*****";
	private static final String FIELD_SEPARATOR = "\\";
	private static final Object EMPTY = "";

	/* variable */
	private final ImportContext context;

	// PDV: exposure trade from Murex
	private boolean isPDV = false;
	
	//SLB:
	private boolean isSLB = false;

	//Opt Columns:
	private boolean optColumns = false;

	public boolean isOptColumns() {
		return optColumns;
	}

	public void setOptColumns(boolean optColumns) {
		this.optColumns = optColumns;
	}

	/**
	 * Constructor
	 * 
	 * @param context
	 */
	public InterfaceFileReader(ImportContext context) {
		this.context = context;
	}

	/**
	 * from a flat line builds the bean containing the exposure trade.
	 * 
	 * @param useControlLine
	 */
	@Override
	public InterfaceTradeBean readLine(String record, String spliter, int lineNb, boolean useControlLine)
			throws Exception {

		if (this.isPDV) {
			return readLinePDV(record, spliter, lineNb, useControlLine);
		}

		if (ALLOW_FILES_WITH_MORE_FIELDS) {
			return readLineBothVersions(record, spliter, lineNb, useControlLine);
		}
		
		return readLineOriginal(record, spliter, lineNb);
	}

	private InterfaceTradeBean readLinePDV(String record, String spliter, int lineNb, boolean useControlLine)
			throws Exception {
		String[] values = null;

		// GSM: 07/03/2014 - Added new logic for PdV - accept not using control line
		if ((record != null) && (record.length() >= 5) && isEndLineOfFile(record)) {
			if (useControlLine) {
				return null;
			} else {
				// exception
				throw new Exception(
						"Error: Exposure Importer configured to use control line and end line reached. Check ST configuration");
			}
		}
		// if end of line is empty or line is empty
		if ((record == null) || record.isEmpty()) {
			if (!useControlLine) {
				return null;
			} else {
				// exception
				throw new Exception(
						"Error: Exposure Importer configured to NOT use control line and line is empty. Check Imported File");
			}
		}
						
		values = record.split(FIELD_SEPARATOR + spliter, -1);
		// if the number of fields in the processed line is less than 34
		// elements then raise an exception

		if (!containsCorrectNumberOfFields(values)) {

			generateErrorExceptions(record, lineNb, values);
		}

		// map the splitted values. In case some fields haven't been received, left blank
		values = mapValues(values);

		return buildNewTradeBeanFromValues(record, lineNb, values);
	}

	/*
	 * Method that allows the existance of the two versions of importations, with 34 (original) and with 41 (new) fields
	 * per line in the flat input file.
	 * 
	 * @param record
	 * 
	 * @param spliter
	 * 
	 * @param lineNb
	 * 
	 * @return
	 */
	private InterfaceTradeBean readLineBothVersions(String record, String spliter, int lineNb, boolean useControlLine)
			throws Exception {

		String[] values = null;

		// GSM: 07/03/2014 - Added new logic for PdV - accept not using control line
		if ((record != null) && (record.length() >= 5) && isEndLineOfFile(record)) {

			if (useControlLine) {
				return null;
			} else {
				// exception
				throw new Exception(
						"Error: Exposure Importer configured to use control line and end line reached. Check ST configuration");
			}

		}
		// if end of line is empty or line is empty
		if ((record == null) || record.isEmpty()) {

			if (!useControlLine) {
				return null;
			} else {
				// exception
				throw new Exception(
						"Error: Exposure Importer configured to NOT use control line and line is empty. Check Imported File");
			}
		}

		values = record.split(FIELD_SEPARATOR + spliter, -1);
		// if the number of fields in the processed line is less than 34
		// elements then raise an exception

		if (!containsCorrectNumberOfFields(values)) {

			generateErrorExceptions(record, lineNb, values);
		}

		// map the splitted values. In case some fields haven't been received, left blank
		values = mapValues(values);

		return buildNewTradeBeanFromValues(record, lineNb, values);
	}

	/*
	 * builds interface bean from the array values
	 */
	private InterfaceTradeBean buildNewTradeBeanFromValues(String record, int lineNb, String[] values) {

		InterfaceTradeBean tradeBean = new InterfaceTradeBean(values);


		if (this.isPDV) {
			tradeBean.setPDV(true);
			tradeBean.fillPDVFields(values);
		}else if (this.isSLB) {
			tradeBean.setIsSLB(true);
			tradeBean.fillSLBFields(values);
			
		}else{
			//ACD 17/05/2016 IM
			tradeBean.UPIField(values);
			
		}

		//New optional columns
		Boolean optColumns = Optional.ofNullable(this.context)
				.map(ImportContext::getTradeImportTracker)
				.map(TradeImportTracker::isOptColumns).orElse(false);
		if(optColumns){
			tradeBean.fillsbsdMsbsd(values);
			tradeBean.fillsbsProduct(values);
			tradeBean.filldayCountConvention(values);
			tradeBean.fillSwapAgentId(values);
			tradeBean.fillSwapAgent(values);
		}

		if ((tradeBean.getDirection() != null) && !tradeBean.getDirection().equals(EMPTY)) {

			if (tradeBean.getDirection().equals("BORROW")) {
				tradeBean.setDirection("Sell");

			} else if (tradeBean.getDirection().equals("LEND") || tradeBean.getDirection().equals("LOAN")) {
				tradeBean.setDirection("Buy");
			}
		}

		tradeBean.setLineNumber(lineNb);
		tradeBean.setLineContent(record);

		return tradeBean;

	}

	/**
	 * We assume that every fields with at least 33 fields to whatever number over 33.
	 * 
	 * @param values
	 * @return
	 */
	private boolean containsCorrectNumberOfFields(String[] values) {

		return (values.length >= (FILES_TYPES.OLD.getNumberOfFields() - 1));
	}

	/*
	 * Stores error in log and context. Builds the exception
	 */
	private void generateErrorExceptions(String record, int lineNb, String[] values) throws Exception {

		Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error checking the number of fields.");
		InterfaceTradeBean tradeBeanError = new InterfaceTradeBean();
		tradeBeanError.setLineContent(record);
		tradeBeanError.setLineNumber(lineNb);
		this.context.getTradeImportTracker().addError(tradeBeanError, 5, "Bad record format");
		this.context.getTradeImportTracker().incrementKOImports(1, TradeImportTracker.KO_ERROR);
		throw new Exception("Error checking the number of fields. Max number of fiels are:"
				+ FILES_TYPES.OLD.getNumberOfFields() + "or " + FILES_TYPES.NEW.getNumberOfFields() + ". Received "
				+ values.length);
	}

	/*
	 * returns true if reached end line
	 */
	private boolean isEndLineOfFile(String record) {

		return (record.substring(0, 5).equals(END_LINE_MARK));
	}

	/*
	 * As we will have different fields formats in files during development (addition of RIG code, addition of 6 fields
	 * related to DFA and EMIR, etc) this is intended to be able to add blanck values if we process a file with less
	 * fields than what will be really receiving in a future.
	 */
	private String[] mapValues(String[] values) {

		int totalFields = FILES_TYPES.NEW.getNumberOfFields();

		if (this.isPDV) {
			totalFields = 46;
		}
		
		if (this.isSLB) { //SLB maximum number of fields
			totalFields = 48;
		}

		//New optionals columns
		Boolean optColumns = Optional.ofNullable(this.context)
				.map(ImportContext::getTradeImportTracker)
				.map(TradeImportTracker::isOptColumns).orElse(false);
		if(optColumns){
			return mapAllValues(values);
		}

		String[] totalValues = new String[totalFields];

		// put blank
		for (int i = 0; i < totalFields; i++) {
			totalValues[i] = "";
		}

		final int limit = (values.length <= totalFields) ? values.length : totalFields;
		// copy read values
		for (int j = 0; j < limit; j++) {
			totalValues[j] = values[j].trim();
		}

		return totalValues;
	}


	private String[] mapAllValues(String[] values){
		String[] totalValues = new String[values.length];
		Arrays.stream(totalValues).forEach(val -> {
			val="";
		});
		for (int j = 0; j < values.length; j++) {
			totalValues[j] = values[j].trim();
		}
		return totalValues;
	}

	/*
	 * Original code
	 */
	public InterfaceTradeBean readLineOriginal(String record, String spliter, int lineNb) throws Exception {

		String[] values = null;
		InterfaceTradeBean tradeBean = null;
		if (!record.substring(0, 5).equals("*****")) {
			values = record.split("\\" + spliter, -1);
			// if the number of fields in the processed line is less than 34
			// elements then raise an exception
			if (values.length < 34) {
				Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error checking the number of fields.");
				InterfaceTradeBean tradeBeanError = new InterfaceTradeBean();
				tradeBeanError.setLineContent(record);
				tradeBeanError.setLineNumber(lineNb);
				this.context.getTradeImportTracker().addError(tradeBeanError, 5, "Bad record format");
				this.context.getTradeImportTracker().incrementKOImports(1, TradeImportTracker.KO_ERROR);
				throw new Exception("Error checking the number of fields. Max number of fiels is " + 34 + ". Received "
						+ values.length);
			}
			
			
			// map the splitted values. In case some fields haven't been received, left blank
			values = mapValues(values);
			
			tradeBean = new InterfaceTradeBean(values);
			if (tradeBean.getDirection().equals("BORROW")) {
				tradeBean.setDirection("Sell");
			} else if (tradeBean.getDirection().equals("LEND") || tradeBean.getDirection().equals("LOAN")) {
				tradeBean.setDirection("Buy");
			}
			tradeBean.setLineNumber(lineNb);
			tradeBean.setLineContent(record);
		}

		return tradeBean;
	}

	public boolean isPDV() {
		return this.isPDV;
	}

	public void setPDV(boolean isPDV) {
		this.isPDV = isPDV;
	}
	
	
	public boolean isSLB() {
		return this.isSLB;
	}

	public void setIsSLB(boolean isSLB) {
		this.isSLB = isSLB;
	}
	
	
	
}