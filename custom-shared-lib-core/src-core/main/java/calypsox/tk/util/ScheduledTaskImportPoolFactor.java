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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.BondAssetBacked;
import com.calypso.tk.product.PoolFactorEntry;
import com.calypso.tk.product.util.NotionalDate;
import com.calypso.tk.service.DSConnection;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.log.LogGeneric;
import calypsox.util.FileUtility;

/**
 * To import Pool factor
 **/
public class ScheduledTaskImportPoolFactor extends AbstractProcessFeedScheduledTask {
	private static final long serialVersionUID = 123L;

	private static final String FILEPATH = "File Path";
	private static final String STARTFILENAME = "Start of File Name";
	private static final String TASK_INFORMATION = "Import StockLending rates from txt file.";
	protected static final String SUMMARY_LOG = "Summary Log";
	protected static final String DETAILED_LOG = "Detailed Log";
	protected static final String FULL_LOG = "Full Log";
	protected static final String POOL_FACTOR = "POOL_FACTOR";
	protected static final String STATIC_DATA_LOG = "Static Data Log";
	private static final String SOURCE = "IMPORT_POOL_FACTOR";
	private static final String SOURCE_SYSTEM = "Source System";
	private static final String REF_INTERNA = "REF_INTERNA";
	
	private static final String AMORT_STRUCTURE = "Schedule";
	private static final String  NOTIONAL_TYPE = "Notional Percent";

	private static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

	// error types
	public enum ErrorTypes {

		LOG_CREATION_ERROR("Error creating log files"),
		LOG_MOVEMENT_ERROR("Error moving historic files"),
		LOG_UPDATE_ERROR("Error updating log"),
		FIELDS_NUMBER_ERROR("Error checking the number of fields"),
		FILE_SEARCH_ERROR("Error while looking for file"),
		FILE_READ_ERROR("Error while reading file"),
		STREAM_CLOSE_ERROR("Error while trying close input stream for the file"),
		DATA_SAVE_ERROR("There was some problem saving data on Calypso"),
		FIELD_ERROR("FIELD_ERROR"),
		FIELD_WARNING("FIELD_WARNING");

		protected String message;

		private ErrorTypes(String message) {
			this.message = message;
		}

		public String getMessage() {
			return this.message;
		}

	}

	private static int NUM_OF_VALUES = 5;

	// log management stuff
	private File file;
	private boolean processOK = true;
	private boolean controlMOK = true;
	private final LogGeneric logGen = new LogGeneric();

	@Override
	public String getTaskInformation() {
		return TASK_INFORMATION;
	}

	@Override
	protected List<AttributeDefinition> buildAttributeDefinition() {
		List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();

		// Gets superclass attributes
		attributeList.addAll(super.buildAttributeDefinition());

		attributeList.add(attribute(SOURCE_SYSTEM));
		attributeList.add(attribute(SUMMARY_LOG));
		attributeList.add(attribute(DETAILED_LOG));
		attributeList.add(attribute(STATIC_DATA_LOG));
		attributeList.add(attribute(FULL_LOG));

		return attributeList;
	}
	
//	@SuppressWarnings("unchecked")
//	@Override
//	public Vector<String> getDomainAttributes() {
//		final Vector<String> attr = super.getDomainAttributes();
//		attr.add(SOURCE_SYSTEM);
//		attr.add(SUMMARY_LOG);
//		attr.add(DETAILED_LOG);
//		attr.add(STATIC_DATA_LOG);
//		attr.add(FULL_LOG);
//		return attr;
//	}

	
	@Override
	public boolean process(final DSConnection conn, final PSConnection connPS) {

		final String path = getAttribute(FILEPATH);
		final String startFileName = getAttribute(STARTFILENAME);
		final JDate valueDate = getValuationDatetime().getJDate(TimeZone.getDefault());
		boolean result = true;

		try {

			// Create and initialize logs
			createAndInitializeLogFiles();

			// Get file
			this.file = lookForFile(path, startFileName, valueDate);
			if (this.file == null) {
				manageLogError(ErrorTypes.FILE_SEARCH_ERROR, startFileName, 0, null, null, null, null);
				writeLogFiles();
			} else {
				// Just after file verifications, this method will make a copy into the
				// ./import/copy/ directory
				FileUtility.copyFileToDirectory(path + this.file.getName(), path + "/copy/");
				// Process file
				result = fileReadProcess(this.file, valueDate);
			}

		} catch (IOException e) {
			Log.error(this, e);
			manageLogError(ErrorTypes.LOG_CREATION_ERROR, startFileName, 0, null, null, null, null);
			writeLogFiles();
			result = false;
		}

		// Feed summary log
		this.logGen.feedGenericLogProcess(startFileName, getAttribute(SUMMARY_LOG), SOURCE,
				this.logGen.getNumberTotal());

		// Post process - Move files to ok/fail and generate .bad file
		if (this.file != null) {
			try {
				this.feedPostProcess(result);
			} catch (Exception e) {
				Log.error(this, e);
				manageLogError(ErrorTypes.LOG_MOVEMENT_ERROR, this.file.getName(), 0, null, null, null, null);
				writeLogFiles();
			}
		}

		// close log files.
		closeLogFiles();

		if (this.controlMOK) {
			ControlMErrorLogger.addError(ErrorCodeEnum.NoError, "");
		}

		return this.processOK;
	}

	/********** LOG_MANAGEMENT **********/

	// - Create & intitialize log files - //
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
		this.logGen.initializeFiles(SOURCE);

		return;
	}

	// - Write on log files - //
	public void writeLogFiles() {
		this.logGen.feedFullLog(0);
		this.logGen.feedDetailedLog(0);
	}

	// - Close log files - //
	public void closeLogFiles() {
		try {
			this.logGen.closeLogFiles();
		} catch (final IOException e) {
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error closing log files");
			Log.error(this, e); //sonar
			this.processOK = false;
		}
	}

	// - Reinitialize log variables - //
	public void resetLogVariables() {
		this.logGen.initializeError();
		this.logGen.initializeWarning();
		this.logGen.initializeErrorLine();
		this.logGen.initializeWarningLine();
		this.logGen.initializeOkLine();
		return;
	}

	// - Update logs after process one line - //
	public void fileReadProcessUpdateLogs(String line, int lineNumber) {
		if (this.logGen.getNumberError() > 0) {
			this.logGen.incrementRecordErrors();
		}
		if (this.logGen.getNumberWarning() > 0) {
			this.logGen.incrementRecordWarning();
			this.addBadLine(line, "Error in line");
			String sourceSys = getAttribute(SOURCE_SYSTEM);
			if (sourceSys == null) {
				sourceSys = "";
			}
			try {
				this.logGen.feedStaticDataLog(String.valueOf(this.logGen.getNumberTotal()), sourceSys);
			} catch (IOException e) {
				Log.error(this, e); //sonar
				manageLogError(ErrorTypes.LOG_UPDATE_ERROR, this.file.getName(), lineNumber, null, null, null, null);
			}
		}
		if ((this.logGen.getNumberError() == 0)) {
			this.logGen.setOkLine(SOURCE, this.file.getName(), lineNumber, String.valueOf(0));
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
		case LOG_CREATION_ERROR:
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, errorType.getMessage());
			this.logGen.setErrorCreatingLogFile(SOURCE, fileName);
			this.logGen.incrementRecordErrors();
			ControlMErrorLogger.addError(ErrorCodeEnum.LogException, "");
			this.processOK = false;
			break;
		case LOG_MOVEMENT_ERROR:
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, errorType.getMessage());
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

	// - Get last file matches with name and date received - //
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

	// - Process file recieved - //
	public boolean fileReadProcess(File file, JDate valueDate) {

		boolean result = true;
		String line = null;
		Vector<String> values = null;
		BufferedReader inputFileStream = null;

		try {
			inputFileStream = new BufferedReader(new FileReader(file));
			for (int i = 0; ((line = inputFileStream.readLine()) != null); i++) {
				// eof
				if ((line.charAt(0) == 26) || line.startsWith("*****")) {
					break;
				}
				// increment processed record number
				this.logGen.incrementTotal();
				// initialize log variables
				resetLogVariables();
				// get values
				values = getValuesFromLine(line);
				// check number of values
				if (isCorrectNumberOfValues(values)) {
					// process values
					result = processValues(values, valueDate, line, i + 1);
				} else {
					manageLogError(ErrorTypes.FIELDS_NUMBER_ERROR, file.getName(), i + 1, null, null, null, line);
					result = false;
				}
				// update log for line processed
				fileReadProcessUpdateLogs(line, i + 1);
			}
		} catch (FileNotFoundException e) {
			Log.error(this, e);
			manageLogError(ErrorTypes.FILE_SEARCH_ERROR, file.getName(), 0, null, null, null, null);
			writeLogFiles();
			result = false;
		} catch (IOException e) {
			Log.error(this, e);
			manageLogError(ErrorTypes.FILE_READ_ERROR, file.getName(), 0, null, null, null, null);
			writeLogFiles();
			result = false;
		} finally {
			if (inputFileStream != null) {
				try {
					inputFileStream.close();
				} catch (final IOException e) {
					Log.error(this, e);
					manageLogError(ErrorTypes.STREAM_CLOSE_ERROR, null, 0, null, null, null, null);
					writeLogFiles();
					result = false;
				}
			}
		}
		return result;
	}

	// - Get values form line read - //
	public Vector<String> getValuesFromLine(String line) {
		Vector<String> values = new Vector<String>(Arrays.asList(line.split(" ")));
		filterValues(values);
		return values;
	}

	// - Filter recieved values removing blanks - //
	public void filterValues(Vector<String> values) {
		List<String> elementsToRemove = new ArrayList<String>();
		elementsToRemove.add("");
		values.removeAll(elementsToRemove);
	}

	// - Check number of values - //
	public boolean isCorrectNumberOfValues(Vector<String> values) {
		return values.size() == NUM_OF_VALUES;
	}

	// - Process line values (get product, get poolFactor and save it)
	public boolean processValues(Vector<String> values, JDate valueDate, String line, int lineNumber) {
		// get product
		String refInterna = values.get(0);
		
		BondAssetBacked abs = null;
		Bond bond = null;
		
		Product p = BOCache.getExchangeTradedProductByKey(getDSConnection(), REF_INTERNA, refInterna);
		Product pClone = null;
		if (p != null) {
			try {
				pClone = (Product) p.cloneIfImmutable();
			} catch (CloneNotSupportedException e) {
				Log.error(this, "Cannot clone : " + e.toString());
			}
			if (p instanceof BondAssetBacked) {
				abs = (BondAssetBacked) pClone;
			}
			else if (p instanceof Bond) {
				bond = (Bond) pClone;
			}
		}
		
		if (abs == null && bond == null) {
			manageLogError(ErrorTypes.FIELD_WARNING, this.file.getName(), lineNumber, "XXX", REF_INTERNA, refInterna, line);
			return false;
		}
		try {
			// get poolFactor value
			double poolFactorValue = getPoolFactorValue(values.get(2));
			
			if (abs != null) {
				// create poolFactor entry
				PoolFactorEntry newPoolFactorEntry = createPoolFactorEntry(poolFactorValue, valueDate);
				// create initial poolFactor if necessary
				if (!isInitialPoolFactorCreated(abs)) {
					abs.setPoolFactorSchedule(abs.createInitialPoolFactorSchedule());
				}
				// add poolFactor entry
				abs.addPoolFactorEntry(valueDate, newPoolFactorEntry);
				// save product
				getDSConnection().getRemoteProduct().saveProduct(abs);
			}
			else {
				poolFactorValue = poolFactorValue * 100.0;
				Vector<NotionalDate> amortSchedule = (Vector<NotionalDate>)bond.getAmortSchedule();
				if (amortSchedule == null) {
					amortSchedule = new Vector<NotionalDate>();
					bond.setAmortSchedule(amortSchedule);
				}
				
				StringBuilder warnMessage = new StringBuilder("Bond with ISIN ");
				warnMessage.append(bond.getSecCode("ISIN"));
				warnMessage.append(" and RefInterna ");
				warnMessage.append(refInterna);

				if (bond.getAmortizingB() == false) {
					Log.error(this, warnMessage.toString() + " - forcing AmortizingB.");
					bond.setAmortizingB(true);
				}

				if (bond.getAmortizingFaceValueB() == false) {
					Log.error(this, warnMessage.toString() + " - forcing AmortizingFaceValueB.");
					bond.setAmortizingFaceValueB(true);
				}

				if (Util.isEmpty(bond.getAmortStructure()) || !bond.getAmortStructure().equals(AMORT_STRUCTURE)) {
					Log.error(this, warnMessage.toString() + " - forcing AmortStructure.");
					bond.setAmortStructure(AMORT_STRUCTURE);
				}

				if (Util.isEmpty(bond.getNotionalType()) || !bond.getNotionalType().equals(NOTIONAL_TYPE)) {
					Log.error(this, warnMessage.toString() + " - forcing NotionalType.");
					bond.setNotionalType(NOTIONAL_TYPE);
				}

				boolean alreadyExistingDate = false;
				for (int j = 0; j < amortSchedule.size(); j++) {
					NotionalDate existingDate = amortSchedule.get(j);
					if (existingDate.getStartDate().equals(valueDate)) {
						Log.error(this, warnMessage.toString() + " - forcing Notional Amount on an existing date.");
						existingDate.setNotionalAmt(poolFactorValue);
						alreadyExistingDate = true;
					}
					else {
						double amort = existingDate.getNotionalAmt();
						if (amort > 100.0) {
							double amortPercent = (amort / bond.getTotalIssued());
							Log.error(this, warnMessage.toString() + " - forcing Notional Amount as percent. " + existingDate + " to " + amortPercent);
							existingDate.setNotionalAmt(amortPercent * 100.0);
						}
					}
				}
				
				if (!alreadyExistingDate) {
					NotionalDate newDate = new NotionalDate(valueDate, poolFactorValue);
					amortSchedule.add(newDate);
				}
				
				// save product
				getDSConnection().getRemoteProduct().saveProduct(bond);
			}
		} catch (NumberFormatException e) {
			Log.error(this, "Incorrect number format for Pool Factor field", e);
			manageLogError(ErrorTypes.FIELD_ERROR, this.file.getName(), lineNumber, "XXX", POOL_FACTOR, values.get(2),
					line);
			return false;
		} catch (RemoteException e) {
			Log.error(this, "Cannot save product on DB", e);
			manageLogError(ErrorTypes.DATA_SAVE_ERROR, this.file.getName(), lineNumber, null, null, refInterna, line);
			return false;
		}
		return true;
	}

	/********** HELPER_METHODS **********/

	// - Get poolFactor value (15 first digits are integer part, next 15 are decimal part - //
	public double getPoolFactorValue(String str) throws NumberFormatException {
		if (str.length() < 30) {
			throw new NumberFormatException();
		}
		String intPartStr = str.substring(0, 15);
		String decPartStr = "0." + str.substring(15, 30);
		double intPart = Double.valueOf(intPartStr);
		double decPart = Double.valueOf(decPartStr);
		return intPart + decPart;
	}

	// - Check if there's initial poolFactor - //
	public boolean isInitialPoolFactorCreated(BondAssetBacked abs) {
		return (abs.getPoolFactorSchedule() != null) && (abs.getPoolFactorSchedule().size() > 0);
	}

	// - Create poolFactor entry based on valueDate and poolFactor value - //
	public PoolFactorEntry createPoolFactorEntry(double value, JDate valueDate) {
		PoolFactorEntry poolFactorEntry = new PoolFactorEntry();
		poolFactorEntry.setEffectiveDate(valueDate);
		poolFactorEntry.setPoolFactor(value);
		return poolFactorEntry;
	}

	@Override
	public String getFileName() {
		return this.file.getName();
	}
}
