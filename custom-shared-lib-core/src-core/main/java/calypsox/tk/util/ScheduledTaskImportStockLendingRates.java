package calypsox.tk.util;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.service.DSConnection;

import calypsox.ErrorCodeEnum;
import calypsox.tk.product.BondCustomData;
import calypsox.tk.product.EquityCustomData;
import calypsox.tk.util.log.LogGeneric;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.FileUtility;

/**
 * To import StockLending rates
 **/
public class ScheduledTaskImportStockLendingRates extends AbstractProcessFeedScheduledTask {
	private static final long serialVersionUID = 123L;

	private static final String FILEPATH = "File Path";
	private static final String STARTFILENAME = "Start of File Name";
	private static final String SOURCE_SYSTEM = "Source System";
	private static final String ISIN = "ISIN";
	private static final String SEPARATOR_DOMAIN_STRING = "Separator";
	private static final String TASK_INFORMATION = "Import StockLending rates from txt file.";
	protected static final String SUMMARY_LOG = "Summary Log";
	protected static final String DETAILED_LOG = "Detailed Log";
	protected static final String FULL_LOG = "Full Log";
	// private static final String SUBJECT = "Log files for Import Bond prices
	// on " + Defaults.getEnvName();
	// private static final String DEFAULT_FROM_EMAIL =
	// "calypso@gruposantander.com";
	protected static final String PROCESS = "Import StockLending rates";
	protected static final String STATIC_DATA_LOG = "Static Data Log";
	private static final String SOURCE = "STOCK_LENDING_RATES";
	private static final String CUSTOM_TYPE = "CUSTOM";
	private static final String ALWAYS_TYPE = "ALWAYS";
	private static final String REPROCESS = "Reprocess";

	private static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

	// error types
	public enum ErrorTypes {

		FILES_NUMBER_ERROR(
				"Number of matches for the filename in the path specified is 0 or greater than 1"), LOG_CREATION_ERROR(
						"Error creating log files"), LOG_MOVEMENT_ERROR(
								"Error moving historic files"), LOG_UPDATE_ERROR(
										"Error updating log"), FIELDS_NUMBER_ERROR(
												"Error checking the number of fields"), FILE_SEARCH_ERROR(
														"Error while looking for file"), FILE_READ_ERROR(
																"Error while reading file"), STREAM_CLOSE_ERROR(
																		"Error while trying close input stream for the file"), DATA_SAVE_ERROR(
																				"There was some problem saving data on Calypso"), FIELD_ERROR(
																						"FIELD_ERROR"), FIELD_WARNING(
																								"FIELD_WARNING");

		protected String message;

		private ErrorTypes(String message) {
			this.message = message;
		}

		public String getMessage() {
			return this.message;
		}

	}

	// file & field sizes
	private static int P2_FIELDS_NUMBER = 16;
	private static int U1_FIELDS_NUMBER = 5;
	private static int UTIL_VALUES_SIZE = 4;

	// log management stuff
	private boolean processOK = true;
	private boolean controlMOK = true;
	private File P2file = null;
	private File U1file = null;
	private final LogGeneric logGen = new LogGeneric();

	private BufferedReader inputFileStream;
	private DSConnection conn;
	private JDate valDate = null;
	private final HashMap<String, Double[]> underlyingProductsData = new HashMap<String, Double[]>();

	@Override
	public String getTaskInformation() {
		return TASK_INFORMATION;
	}

	@Override
	protected List<AttributeDefinition> buildAttributeDefinition() {
		List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();

		// Gets superclass attributes
		attributeList.addAll(super.buildAttributeDefinition());

		attributeList.add(attribute(SEPARATOR_DOMAIN_STRING));
		attributeList.add(attribute(SOURCE_SYSTEM));
		attributeList.add(attribute(SUMMARY_LOG));
		attributeList.add(attribute(DETAILED_LOG));
		attributeList.add(attribute(STATIC_DATA_LOG));
		attributeList.add(attribute(FULL_LOG));
		attributeList.add(attribute(REPROCESS).booleanType());

		return attributeList;
	}

//	@SuppressWarnings("unchecked")
//	@Override
//	public Vector<String> getDomainAttributes() {
//		final Vector<String> attr = super.getDomainAttributes();
//		attr.add(SEPARATOR_DOMAIN_STRING);
//		attr.add(SOURCE_SYSTEM);
//		attr.add(SUMMARY_LOG);
//		attr.add(DETAILED_LOG);
//		attr.add(STATIC_DATA_LOG);
//		attr.add(FULL_LOG);
//		attr.add(REPROCESS);
//		return attr;
//	}
//
//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	@Override
//	public Vector getAttributeDomain(String attribute, Hashtable hashtable) {
//		Vector vector = new Vector();
//		if (attribute.equals(REPROCESS)) {
//			vector.addElement(TRUE);
//			vector.addElement(FALSE);
//		} else {
//			vector = super.getAttributeDomain(attribute, hashtable);
//		}
//		return vector;
//	}

	@SuppressWarnings({ "rawtypes" })
	@Override
	public boolean process(final DSConnection conn, final PSConnection connPS) {

		final String path = getAttribute(FILEPATH);
		final String separator = getAttribute(SEPARATOR_DOMAIN_STRING);
		final String P2startFileName = getAttribute(STARTFILENAME).substring(0,
				getAttribute(STARTFILENAME).indexOf('/'));
		final String U1startFileName = getAttribute(STARTFILENAME)
				.substring(getAttribute(STARTFILENAME).indexOf('/') + 1);
		this.conn = conn;
		final Vector holidays = getHolidays();
		final JDate processDate = getValuationDatetime().getJDate(TimeZone.getDefault());
		this.valDate = processDate.addBusinessDays(-1, holidays);

		try {
			// Create and initialize logs - shared log for both files
			createAndInitializeLogFiles();

			// get P2 file
			this.P2file = lookForFile(path, P2startFileName, processDate);
			if (this.P2file == null) {
				manageLogError(ErrorTypes.FILE_SEARCH_ERROR, P2startFileName, 0, null, null, null, null);
				writeLogFiles();
			} else {

				// Just after file verifications, this method will make a copy
				// into the
				// ./import/copy/ directory
				FileUtility.copyFileToDirectory(path + this.P2file.getName(), path + "/copy/");

				// Process P2 file
				if (fileReadProcess(this.P2file.getPath(), separator, this.logGen, this.P2file.getName(),
						P2_FIELDS_NUMBER)) {
					resetLogVariables(this.logGen);
					// get U1 file
					this.U1file = lookForFile(path, U1startFileName, processDate);
					if (this.U1file == null) {
						manageLogError(ErrorTypes.FILE_SEARCH_ERROR, U1startFileName, 0, null, null, null, null);
						writeLogFiles();
					} else {

						// Just after file verifications, this method will make
						// a copy into the
						// ./import/copy/ directory
						FileUtility.copyFileToDirectory(path + this.U1file.getName(), path + "/copy/");

						// Process U1 file
						if (fileReadProcess(this.U1file.getPath(), separator, this.logGen, this.U1file.getName(),
								U1_FIELDS_NUMBER)) {
							// Save data
							saveDataOnDB();
						}
					}
				}
			}

		} catch (IOException e) {
			Log.error(this, e); //sonar
			manageLogError(ErrorTypes.LOG_CREATION_ERROR, P2startFileName, 0, null, null, null, null);
			writeLogFiles();
		}

		// move files to ok/fail
		// postProcess(this.P2file, this.U1file, path);
		try {
			feedPostProcessForTwoFiles(this.P2file, this.U1file, path, this.logGen);
		} catch (Exception e) {
			Log.error(this, e.getMessage(), e);
			manageLogError(ErrorTypes.LOG_MOVEMENT_ERROR, null, 0, null, null, null, null);
			writeLogFiles();
		}

		// feed summary log
		this.logGen.feedGenericLogProcess(P2startFileName + "|" + U1startFileName, getAttribute(SUMMARY_LOG), PROCESS,
				this.logGen.getNumberTotal());

		// close log files.
		closeLogFiles();

		if (this.controlMOK) {
			ControlMErrorLogger.addError(ErrorCodeEnum.NoError, "");
		}

		return this.processOK;
	}

	/********** LOG_MANAGEMENT **********/
	/**
	 * Generate log files and initialize them
	 */
	public void createAndInitializeLogFiles() throws IOException {

		// Date stuff
		final Date d = new Date();
		String time = "";
		synchronized (timeFormat) {
			time = timeFormat.format(d);
		}

		// Generation for the log files.
		this.logGen.generateFiles(getAttribute(DETAILED_LOG), getAttribute(FULL_LOG), getAttribute(STATIC_DATA_LOG),
				time);

		// Fix ini logGen.total = 1
		this.logGen.decrementTotal();

		// Initialization for the logs files.
		this.logGen.initializeFiles(PROCESS);

		return;
	}

	/**
	 * Write on detailed and full logs
	 */
	public void writeLogFiles() {
		this.logGen.feedFullLog(0);
		this.logGen.feedDetailedLog(0);
	}

	/**
	 * Close log files
	 */
	public void closeLogFiles() {
		try {
			this.logGen.closeLogFiles();
		} catch (final IOException e) {
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error closing log files");
			Log.error(this, e); //sonar
			this.processOK = false;
		}
	}

	/**
	 * Reset log variables for error_count, erro_line and ok_line
	 * 
	 * @param logGen
	 *            LogGeneric to manage logs.
	 */
	public void resetLogVariables(LogGeneric logGen) {
		logGen.initializeError();
		logGen.initializeWarning();
		logGen.initializeErrorLine();
		logGen.initializeWarningLine();
		logGen.initializeOkLine();
		return;
	}

	/**
	 * Called when one operation processed, check existing errors and set
	 * error/ok line
	 * 
	 * @param logGen
	 *            LogGeneric to manage logs.
	 */
	public void fileReadProcessUpdateLogs(int lineNumber, String fileName, String line, int fieldsNumber) {

		if (this.logGen.getNumberError() > 0) {
			this.logGen.incrementRecordErrors();

		} else if (this.logGen.getNumberWarning() > 0) {
			this.logGen.incrementRecordWarning();

			// save line to include in .bad file
			if (fieldsNumber == P2_FIELDS_NUMBER) {
				this.addBadLine(line, "Error in line");
			}
			if (fieldsNumber == U1_FIELDS_NUMBER) {
				this.addBadLine2(line, "Error in line");
			}

			String sourceSys = getAttribute(SOURCE_SYSTEM);
			if (sourceSys == null) {
				sourceSys = "";
			}
			try {
				this.logGen.feedStaticDataLog(String.valueOf(this.logGen.getNumberTotal()), sourceSys);
			} catch (IOException e) {
				Log.error(this, e); //sonar
				manageLogError(ErrorTypes.LOG_UPDATE_ERROR, fileName, lineNumber, null, null, null, null);
			}

		} else if ((this.logGen.getNumberError() == 0) && (this.logGen.getNumberWarning() == 0)) {
			this.logGen.setOkLine(SOURCE, fileName, lineNumber, String.valueOf(0));
		}
		writeLogFiles();
	}

	/**
	 * For error treatment, recieve error type and manage it
	 * 
	 * @param errorType
	 *            type of error
	 * @param fileName
	 *            name of data file
	 * @param lineNumber
	 *            number of line
	 * @param errorNumber
	 *            number of error
	 * @param errorField
	 *            field with error
	 * @param ref
	 *            for identify the field
	 * @param line
	 *            line with register affected
	 */
	public void manageLogError(ErrorTypes errorType, String fileName, int lineNumber, String errorNumber,
			String errorField, String ref, String line) {

		switch (errorType) {
		case FILES_NUMBER_ERROR:
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, errorType.getMessage());
			this.logGen.setErrorNumberOfFiles(SOURCE, fileName);
			this.logGen.incrementRecordErrors();
			ControlMErrorLogger.addError(ErrorCodeEnum.InputFileNotFound, "");
			this.controlMOK = false;
			this.processOK = false;
			break;
		case LOG_CREATION_ERROR:
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, errorType.getMessage());
			this.logGen.setErrorCreatingLogFile(SOURCE, fileName);
			this.logGen.incrementRecordErrors();
			ControlMErrorLogger.addError(ErrorCodeEnum.LogException, "");
			this.processOK = false;
			break;
		case LOG_MOVEMENT_ERROR:
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, errorType.getMessage());
			this.logGen.setErrorMovingFile(SOURCE, fileName);
			this.logGen.incrementRecordErrors();
			ControlMErrorLogger.addError(ErrorCodeEnum.InputFileCanNotBeMoved, "Error moving historic files");
			this.processOK = false;
		case LOG_UPDATE_ERROR:
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, errorType.getMessage());
			this.logGen.setErrorWritingLog(SOURCE, fileName, null);
			this.logGen.incrementRecordErrors();
			this.processOK = false;
			break;
		case FIELDS_NUMBER_ERROR:
			this.logGen.setErrorBadRecordFormat(SOURCE, fileName, String.valueOf(lineNumber), "", line, "");
			this.logGen.incrementError();
			break;
		case FILE_SEARCH_ERROR:
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, errorType.getMessage());
			this.logGen.setErrorNumberOfFiles(SOURCE, fileName);
			this.logGen.incrementRecordErrors();
			ControlMErrorLogger.addError(ErrorCodeEnum.InputFileNotFound, "");
			this.controlMOK = false;
			this.processOK = false;
			break;
		case FILE_READ_ERROR:
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, errorType.getMessage());
			this.logGen.setErrorOpeningFile(SOURCE, fileName, "0");
			this.logGen.incrementRecordErrors();
			ControlMErrorLogger.addError(ErrorCodeEnum.IOException, "");
			this.controlMOK = false;
			this.processOK = false;
			break;
		case STREAM_CLOSE_ERROR:
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, errorType.getMessage());
			this.processOK = false;
			break;
		case DATA_SAVE_ERROR:
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, errorType.getMessage());
			this.logGen.setErrorSavingStockLendingRate(SOURCE, fileName, String.valueOf(lineNumber), ref, line);
			this.logGen.incrementRecordErrors();
			this.processOK = false;
			break;
		case FIELD_ERROR:
			this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, fileName, String.valueOf(lineNumber),
					errorNumber, errorField, ref, line);
			this.logGen.incrementError();
			break;
		case FIELD_WARNING:
			this.logGen.setWarningRequiredFieldNotPresentNotValid(SOURCE, fileName, String.valueOf(lineNumber),
					errorNumber, errorField, ref, line);
			this.logGen.incrementWarning();
			break;
		}

	}

	/********** IMPORT_PROCESS_MANAGEMENT **********/
	/**
	 * Process file reading line per line and getting the info.
	 * 
	 * @param filepath
	 *            String with the file path.
	 * @param separator
	 *            String with separator character.
	 * @param requiredCcy
	 *            Required currency to do the conversion.
	 * @param logGen
	 *            LogGeneric to manage logs.
	 * @param file
	 *            String with data file name.
	 * @param bResult
	 *            boolean flag to determinate where to move the file at the end
	 *            (ok or fail).
	 * @param fieldsNumber
	 *            int with number of fields of each register.
	 * @return TRUE if process OK or FALSE if any problem.
	 */
	public boolean fileReadProcess(String filepath, String separator, LogGeneric logGen, String file,
			int fieldsNumber) {

		String line = null;
		String[] values = null;
		boolean result = true;

		try {
			this.inputFileStream = new BufferedReader(new FileReader(filepath));

			// Si es carga normal, fichero incluye cabecera asi que la salto, si
			// es reproceso, no la incluye, asi que no
			// la salto
			String isReprocess = getAttribute(REPROCESS);
			if (!Util.isEmpty(isReprocess) && isReprocess.equals(Boolean.FALSE.toString())) {
				this.inputFileStream.readLine(); // saltar cabecera
			}

			for (int i = 0; ((line = this.inputFileStream.readLine()) != null); i++) {
				if (!line.startsWith("*****")) {

					// increment processed record number
					logGen.incrementTotal();
					// ini log variables
					resetLogVariables(logGen);

					// check fields number
					if ((CollateralUtilities.checkFields(line, separator.charAt(0), fieldsNumber))) {

						// get all values
						values = getValuesFromLine(line, separator);

						// update data with util values
						if (fieldsNumber == P2_FIELDS_NUMBER) { // P2 CASE
							updateDataFromP2Info(values, line, i + 1);
						}
						if (fieldsNumber == U1_FIELDS_NUMBER) { // U1 CASE
							updateDataFromU1Info(values, line, i + 1);
						}
					} else {
						manageLogError(ErrorTypes.FIELDS_NUMBER_ERROR, file, i + 1, null, null, null, line);
					}

					// Update log
					fileReadProcessUpdateLogs(i + 1, file, line, fieldsNumber);
				}
			}
		} catch (FileNotFoundException e) {
			Log.error(this, e); //sonar
			manageLogError(ErrorTypes.FILE_SEARCH_ERROR, file, 0, null, null, null, null);
			writeLogFiles();
			result = false;
		} catch (IOException e) {
			Log.error(this, e); //sonar
			manageLogError(ErrorTypes.FILE_READ_ERROR, file, 0, null, null, null, null);
			writeLogFiles();
			result = false;
		} finally {
			if (this.inputFileStream != null) {
				try {
					this.inputFileStream.close();
				} catch (final IOException e) {
					Log.error(this, e); //sonar
					manageLogError(ErrorTypes.STREAM_CLOSE_ERROR, null, 0, null, null, null, null);
					result = false;
				}
			}
		}

		return result;
	}

	/**
	 * Get util data from P2 file and store it on global hashMap.
	 * 
	 * @param values
	 *            [] String array with register fields.
	 * @param bResult
	 *            boolean flag to determinate where to move the file at the end
	 *            (ok or fail).
	 * @param line
	 *            String with line read.
	 * @param lineNumber
	 *            int with line number.
	 */
	public void updateDataFromP2Info(String values[], String line, int lineNumber) {

		Double units, loanRateAvg;

		// isin
		String isin = values[3];
		if (!isValidIsin(isin)) {
			manageLogError(ErrorTypes.FIELD_WARNING, this.P2file.getName(), lineNumber, "22", ISIN, isin, line);
			return;
		}

		// contractTypeID
		String contractTypeID = values[5];
		// loanStageID
		String loanStageID = values[6];
		// collatType
		String collatTypeID = values[7];

		// case {A,A,A}
		if (contractTypeID.equals("A") && (loanStageID.equals("A")) && (collatTypeID.equals("A"))) {
			// get units for total
			units = getValue(values[10]);
			if (units == null) {
				manageLogError(ErrorTypes.FIELD_ERROR, this.P2file.getName(), lineNumber, "22", "UNITS", values[10],
						line);
				return;
			}
			// save values
			saveUnitsForTotalValue(isin, units);
		}

		// case {O,A,N}
		else if (contractTypeID.equals("O") && (loanStageID.equals("A")) && (collatTypeID.equals("N"))) {
			// get units for fee
			units = getValue(values[10]);
			if (units == null) {
				manageLogError(ErrorTypes.FIELD_ERROR, this.P2file.getName(), lineNumber, "22", "UNITS", values[10],
						line);
				return;
			}
			// get loanRateAverage for fee
			loanRateAvg = getValue(values[13]);
			if (loanRateAvg == null) {
				manageLogError(ErrorTypes.FIELD_ERROR, this.P2file.getName(), lineNumber, "22", "LOAN_RATE_AVGE",
						values[13], line);
				return;
			}
			// save values
			saveUnitsAndLoanrateAvgForFeeValue(isin, units, loanRateAvg);
		}

		return;
	}

	/**
	 * Get util data from U1 file and store it on global hashMap.
	 * 
	 * @param values
	 *            [] String array with register fields.
	 * @param bResult
	 *            boolean flag to determinate where to move the file at the end
	 *            (ok or fail).
	 * @param line
	 *            String with line read.
	 * @param lineNumber
	 *            int with line number.
	 */
	public void updateDataFromU1Info(String values[], String line, int lineNumber) {

		Double utilPercent_units;

		// isin
		String isin = values[3];
		if (!isValidIsin(isin)) {
			manageLogError(ErrorTypes.FIELD_WARNING, this.U1file.getName(), lineNumber, "22", ISIN, isin, line);
			return;
		}
		// get utilPercentUnits
		utilPercent_units = getValue(values[5]);
		if (utilPercent_units == null) {
			manageLogError(ErrorTypes.FIELD_ERROR, this.U1file.getName(), lineNumber, "22", "UTIL_PERCENT_UNITS",
					values[5], line);
			return;
		}
		// save values
		saveUtilPercentUnits(isin, utilPercent_units);

		return;
	}

	/**
	 * Store info collected in the global hashMap on the database through
	 * CustomData stuff .
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public void saveDataOnDB() {

		for (String isin : this.underlyingProductsData.keySet()) {
			try {
				final Vector<Product> products = this.conn.getRemoteProduct().getProductsByCode(ISIN, isin);
				if (!Util.isEmpty(products)) {
					for (Product product : products) {
						if (isValidProduct(product)) {
							// bond case
							if (product instanceof Bond) {
								Bond bond = (Bond) product;
								updateBondCustomData(bond, this.underlyingProductsData.get(isin), this.valDate,
										this.conn);
							}
							// equity case
							if (product instanceof Equity) {
								Equity equity = (Equity) product;
								updateEquityCustomData(equity, this.underlyingProductsData.get(isin), this.valDate,
										this.conn);
							}
						}
					}
				}
			} catch (RemoteException e) {
				Log.error(this, e); //sonar
				manageLogError(ErrorTypes.DATA_SAVE_ERROR, "", 0, "", "", isin, "");
				writeLogFiles();
			}
		}
		return;
	}

	/**
	 * Update bond data
	 * 
	 * @param bond
	 *            Bond to update.
	 * @param values
	 *            Vector<Double> with updated values.
	 * @param valDate
	 *            JDate.
	 * @param dsConn
	 *            DSConnection.
	 */
	public void updateBondCustomData(Bond bond, Double[] values, JDate valDate, DSConnection dsConn)
			throws RemoteException {

		BondCustomData bcd = (BondCustomData) bond.getCustomData();

		// calculate values
		Double activeAvailableQty = null, qtyOnLoan = null, fee = null;

		if (values[0] != null) {
			qtyOnLoan = values[0]; // qtyOnLoan
			if (values[3] != null) {
				activeAvailableQty = (values[0] / values[3]) * 100; // activeAvailableQty
				if (activeAvailableQty.isInfinite() || activeAvailableQty.isNaN()) {
					activeAvailableQty = 0.0;
				}
			}
		}
		if ((values[1] != null) && (values[2] != null)) {
			fee = values[2] / values[1]; // fee
			if (fee.isInfinite() || fee.isNaN()) {
				fee = 0.0;
			}
		}
		// if ((values[2] != null) && (values[4] != null)) {
		// rebate = values[4] / values[2];
		// }

		// customData already exists
		if (bcd != null) {
			bcd.setActive_available_qty(activeAvailableQty);
			bcd.setQty_on_loan(qtyOnLoan);
			// check if we have to update or not
			if (bcd.getExpired_date_type() != null) {
				if (((bcd.getExpired_date_type().equals(CUSTOM_TYPE))
						&& (JDate.diff(bcd.getExpired_date(), valDate) > 0))
						|| (bcd.getExpired_date_type().equals(ALWAYS_TYPE))) {
					// update rate if we recieve it
					if (fee != null) {
						// update rate
						bcd.setExpired_date_type(ALWAYS_TYPE);
						bcd.setExpired_date(valDate);
						bcd.setLast_update(JDate.getNow());
						bcd.setFee(fee);
					}
				}
			} else {
				if (fee != null) {
					// update rate
					bcd.setExpired_date_type(ALWAYS_TYPE);
					bcd.setExpired_date(valDate);
					bcd.setLast_update(JDate.getNow());
					bcd.setFee(fee);
				}
			}
		}
		// first time, you have to create customData
		else {
			bcd = new BondCustomData();
			bcd.setActive_available_qty(activeAvailableQty);
			bcd.setQty_on_loan(qtyOnLoan);
			bcd.setFee(fee);
			bcd.setExpired_date_type(ALWAYS_TYPE);
			bcd.setExpired_date(valDate);
			bcd.setLast_update(JDate.getNow());
			bcd.setProductId(bond.getId());
			bond.setCustomData(bcd);
		}

		// save values / save bond
		this.conn.getRemoteProduct().saveProduct(bond);

	}

	/**
	 * Update equity data
	 * 
	 * @param bond
	 *            Bond to update.
	 * @param values
	 *            Vector<Double> with updated values.
	 * @param valDate
	 *            JDate.
	 * @param dsConn
	 *            DSConnection.
	 */
	public void updateEquityCustomData(Equity equity, Double[] values, JDate valDate, DSConnection dsConn)
			throws RemoteException {

		EquityCustomData ecd = (EquityCustomData) equity.getCustomData();

		// calculate values
		Double activeAvailableQty = null, qtyOnLoan = null, fee = null;

		if (values[0] != null) {
			qtyOnLoan = values[0]; // qtyOnLoan
			if (values[3] != null) {
				activeAvailableQty = (values[0] / values[3]) * 100; // activeAvailableQty
				if (activeAvailableQty.isInfinite() || activeAvailableQty.isNaN()) {
					activeAvailableQty = 0.0;
				}
			}
		}
		if ((values[1] != null) && (values[2] != null)) {
			fee = values[2] / values[1]; // fee
			if (fee.isInfinite() || fee.isNaN()) {
				fee = 0.0;
			}
		}
		// if ((values[2] != null) && (values[4] != null)) {
		// rebate = values[4] / values[2];
		// }

		// customData already exists
		if (ecd != null) {
			ecd.setActive_available_qty(activeAvailableQty);
			ecd.setQty_on_loan(qtyOnLoan);
			// check if we have to update or not
			if (ecd.getExpired_date_type() != null) {
				if (((ecd.getExpired_date_type().equals(CUSTOM_TYPE))
						&& (JDate.diff(ecd.getExpired_date(), valDate) > 0))
						|| (ecd.getExpired_date_type().equals(ALWAYS_TYPE))) {
					if (fee != null) {
						ecd.setExpired_date_type(ALWAYS_TYPE);
						ecd.setExpired_date(valDate);
						ecd.setLast_update(JDate.getNow());
						ecd.setFee(fee);
					}
				}
			} else {
				if (fee != null) {
					ecd.setExpired_date_type(ALWAYS_TYPE);
					ecd.setExpired_date(valDate);
					ecd.setLast_update(JDate.getNow());
					ecd.setFee(fee);
				}
			}
		}
		// first time you have to create customData
		else {
			ecd = new EquityCustomData();
			ecd.setActive_available_qty(activeAvailableQty);
			ecd.setQty_on_loan(qtyOnLoan);
			ecd.setFee(fee);
			ecd.setExpired_date_type(ALWAYS_TYPE);
			ecd.setExpired_date(valDate);
			ecd.setLast_update(JDate.getNow());
			ecd.setProductId(equity.getId());
			equity.setCustomData(ecd);
		}

		// save values / save equity
		this.conn.getRemoteProduct().saveProduct(equity);
	}

	/********** DATA_HASHMAP_MANAGEMENT **********/
	public Double getValue(String strValue) {

		Double value = null;
		try {
			value = Double.valueOf(strValue);
		} catch (NumberFormatException e) {
			Log.error(this, "Error converting to double");
		}
		return value;

	}

	public void saveUnitsForTotalValue(String isin, Double units) {

		Double[] valuesArray = new Double[UTIL_VALUES_SIZE];

		// save values
		if (!this.underlyingProductsData.containsKey(isin)) {
			// new isin -> put actual values
			valuesArray[0] = units; // unitsForTotal
		} else {
			// existing isin -> accumulate values
			valuesArray = this.underlyingProductsData.get(isin);
			if (valuesArray[0] != null) {
				valuesArray[0] += units; // unitsForTotal
			} else {
				valuesArray[0] = units;
			}
		}
		this.underlyingProductsData.put(isin, valuesArray);
	}

	public void saveUnitsAndLoanrateAvgForFeeValue(String isin, Double units, Double loanRateAvg) {

		Double[] valuesArray = new Double[UTIL_VALUES_SIZE];

		if (!this.underlyingProductsData.containsKey(isin)) {
			// new isin -> put actual values
			valuesArray[1] = units; // unitsForFee
			valuesArray[2] = loanRateAvg * units; // loanRateAvgForFee*unitsForFee
		} else {
			// existing isin -> accumulate values
			valuesArray = this.underlyingProductsData.get(isin);
			if (valuesArray[1] != null) {
				valuesArray[1] += units; // +unitsForFee
			} else {
				valuesArray[1] = units;
			}
			if (valuesArray[2] != null) {
				valuesArray[2] += loanRateAvg * units; // +loanRateAvgForFee*unitsForFee
			} else {
				valuesArray[2] = loanRateAvg * units;
			}
		}
		this.underlyingProductsData.put(isin, valuesArray);
	}

	public void saveUtilPercentUnits(String isin, Double utilPercentUnits) {

		Double[] valuesArray = new Double[UTIL_VALUES_SIZE];

		if (this.underlyingProductsData.containsKey(isin)) {
			valuesArray = this.underlyingProductsData.get(isin);
			valuesArray[3] = utilPercentUnits;
			this.underlyingProductsData.put(isin, valuesArray);
		}
	}

	/********** HELPER_METHODS **********/
	/**
	 * Count number of fields for a line given.
	 * 
	 * @param str
	 *            String line.
	 * @param separator
	 *            char with separator.
	 * @return number of fields.
	 */
	public int countFileds(final String str, final char separator) {
		int counter = 0;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == separator) {
				counter++;
			}
		}
		return counter;
	}

	public String[] getValuesFromLine(String line, String separator) {
		String[] values = CollateralUtilities.splitMejorado(countFileds(line, separator.charAt(0)) + 1, separator,
				false, line);
		for (int ii = 0; ii < values.length; ii++) {
			values[ii] = values[ii].trim();
		}
		return values;
	}

	@SuppressWarnings("unchecked")
	public boolean isValidIsin(String isin) {
		try {
			final Vector<Product> products = this.conn.getRemoteProduct().getProductsByCode(ISIN, isin);
			if (!Util.isEmpty(products)) {
				for (Product product : products) {
					if (isValidProduct(product)) {
						return true;
					}
				}
			}
		} catch (RemoteException e) {
			Log.error(this, "ISIN = " + isin + " is not valid.\n", e);
		}
		return false;
	}

	public boolean isValidProduct(Product product) {
		if ((product != null) && ((product instanceof Bond) || (product instanceof Equity))) {
			return true;
		}
		return false;
	}

	public File lookForFile(String path, String fileName, JDate date) {

		final String fileNameFilter = fileName;
		// name filter
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File directory, String fileName) {
				return fileName.startsWith(fileNameFilter);
			}
		};

		final File directory = new File(path);
		final File[] listFiles = directory.listFiles(filter);

		for (File file : listFiles) {

			final Long dateFileMilis = file.lastModified();
			final Date dateFile = new Date(dateFileMilis);
			final JDate jdateFile = JDate.valueOf(dateFile);

			if (JDate.diff(date, jdateFile) == 0) {
				return file;
			}

		}

		return null;

	}

	@Override
	public String getFileName() {
		// TODO Auto-generated method stub
		return null;
	}
}
