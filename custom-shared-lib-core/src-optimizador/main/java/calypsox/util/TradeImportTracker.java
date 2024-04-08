package calypsox.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.ControlMErrorLogger;

import calypsox.tk.util.bean.InterfaceTradeBean;
import com.calypso.infra.util.Util;

/**
 * Utiliy class to track the process importing trades
 * 
 * @author aela
 * 
 */
public class TradeImportTracker {

	// class variables
	public static final int KO_ERROR = 1;
	public static final int KO_WARNING = 2;
	public static final int KO_EXCLUDED = 3;
	public static final String LOG_FILE_SEPARATOR = ";";
	private static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
	private final ArrayList<String> logFiles = new ArrayList<String>();
	private boolean isOnlyWarnings = true;
	private boolean isThereWarnings = false;
	private boolean isThereErrors = false;
	protected String sourceSystem;
	protected String fileBeingImported;
	protected String fileBeingImportedFullPath;
	protected int nbTradesToBeImported;
	protected int nbRowsToBeImported;
	protected int nbTradesImportedOK;
	protected int nbTradesImportedKO;
	private int nbRowsImportedOK;
	protected int nbRowsImportedKO;

	private int nbRowsError;
	private int nbRowsWarning;
	// GSM: 15/07/2013. Trades Exclusions, based on DDR CalypsoColl - TradeInterface_Gaps_v1.2
	private int nbRowsExclude;

	private int nbTradesError;
	private int nbTradesWarning;
	private int nbTradesExclude;

	protected String fullLogFileName;
	protected String warningLogFileName;
	protected String detailedLogFileName;
	protected String summaryLogFileName;
	protected String processName;

	private final Object incremerntOKLock = new Object();
	private final Object incremerntKOLock = new Object();
	protected BlockingQueue<TradeImportStatus> tradeImportErrors = null;
	protected ArrayList<TradeImportStatus> criticalErrors = null;
	
	protected ArrayList<TradeImportStatus> pdvProcessingErrors = new ArrayList<TradeImportStatus>();

	private boolean isPDV = false;

	public boolean optColumns = false;
	
	public boolean isPDV() {
		return isPDV;
	}

	public void setPDV(boolean isPDV) {
		this.isPDV = isPDV;
	}



	public ArrayList<TradeImportStatus> getPDVProcessingErrors() {
		return pdvProcessingErrors;
	}

	public void setPdvProcessingErrors(
			ArrayList<TradeImportStatus> pdvProcessingErrors) {
		this.pdvProcessingErrors = pdvProcessingErrors;
	}

	public TradeImportTracker(String sourceSystem, String fileName, String processName,
			BlockingQueue<TradeImportStatus> errorLogs) {
		this.sourceSystem = sourceSystem;
		this.fileBeingImported = fileName;
		this.processName = processName;
		this.tradeImportErrors = errorLogs;
		this.criticalErrors = new ArrayList<TradeImportStatus>();
		this.nbTradesImportedOK = 0;
		this.nbTradesImportedKO = 0;
		setNbRowsImportedOK(0);
		this.nbRowsImportedKO = 0;

		setNbRowsError(0);
		setNbRowsWarning(0);

		setNbTradesError(0);
		setNbTradesWarning(0);

	}

	/**
	 * @return the fileBeingImported
	 */
	public String getFileBeingImported() {
		return this.fileBeingImported;
	}

	/**
	 * @param fileBeingImported
	 *            the fileBeingImported to set
	 */
	public void setFileBeingImported(String fileBeingImported) {
		this.fileBeingImported = fileBeingImported;
	}

	/**
	 * @return the nbTradesToBeImported
	 */
	public int getNbTradesToBeImported() {
		return this.nbTradesToBeImported;
	}

	/**
	 * @param nbTradesToBeImported
	 *            the nbTradesToBeImported to set
	 */
	public void setNbTradesToBeImported(int nbTradesToBeImported) {
		this.nbTradesToBeImported = nbTradesToBeImported;
	}

	/**
	 * @return the nbTradesImportedOK
	 */
	public int getNbTradesImportedOK() {
		return this.nbTradesImportedOK;
	}

	/**
	 * @param nbTradesImportedOK
	 *            the nbTradesImportedOK to set
	 */
	public void setNbTradesImportedOK(int nbTradesImportedOK) {
		this.nbTradesImportedOK = nbTradesImportedOK;
	}

	/**
	 * @return the nbTradesImportedKO
	 */
	public int getNbTradesImportedKO() {
		return this.nbTradesImportedKO;
	}

	/**
	 * @param nbTradesImportedKO
	 *            the nbTradesImportedKO to set
	 */
	public void setNbTradesImportedKO(int nbTradesImportedKO) {
		this.nbTradesImportedKO = nbTradesImportedKO;
	}

	/**
	 * @param lineContent
	 * @param lineNumber
	 * @param boReference
	 * @param errorCode
	 * @param errorMessage
	 */
	public void addError(InterfaceTradeBean tradeBean, int errorCode, String errorMessage) {
		addError(new TradeImportStatus(tradeBean, errorCode, errorMessage, TradeImportStatus.ERROR));
	}

	/**
	 * New Log data to include the exclusion information
	 * 
	 * @param tradeBean
	 */
	public void addExclusion(InterfaceTradeBean tradeBean) {

		// it not a warning
		tradeBean.setWarningChecks(false);
		// set type status
		TradeImportStatus exclude = new TradeImportStatus(this.nbRowsExclude, "", TradeImportStatus.EXCLUDED);
		// line, line number, tradeId, BO reference.
		exclude.setTradeBean(tradeBean);
		// status -> must be excluded
		exclude.setErrorType(TradeImportStatus.EXCLUDED);
		this.tradeImportErrors.add(exclude);

	}

	/**
	 * @param lineContent
	 * @param lineNumber
	 * @param boReference
	 * @param errorCode
	 * @param errorMessage
	 * @param isin
	 */
	public void addWarningNoIsin(InterfaceTradeBean tradeBean, int errorCode, String errorMessage, String isin) {

		// GSM: 27/06/2013. No bond must be warning, no error
		tradeBean.setWarningChecks(true); // set Warning, if not, will fail when adding the warning to the log
		TradeImportStatus status = new TradeImportStatus(tradeBean, errorCode, errorMessage + isin,
				TradeImportStatus.WARNING);
		// status information to add the error detail data and bad file
		status.setFieldName("ISIN");
		status.setBoReference(tradeBean.getBoReference());
		status.setFieldValue(isin);
		addWarning(status);
	}

	/**
	 * @param lineContent
	 * @param lineNumber
	 * @param boReference
	 * @param errorCode
	 * @param errorMessage
	 */
	public void addOK(TradeImportStatus ok) {
		this.tradeImportErrors.add(ok);
	}

	/**
	 * @param error
	 */
	public void addError(TradeImportStatus error) {
		error.setErrorType(TradeImportStatus.ERROR);
		// set the isOnlyWarnings property
		setOnlyWarnings(false);
		setThereErrors(true);
		if (TradeInterfaceUtils.isCriticalSchedTaskErrorCode(error.getErrorCode())) {
			this.criticalErrors.add(error);
		}
		this.tradeImportErrors.add(error);
		
		if(isPDV()) { 
			pdvProcessingErrors.add(error);
		}
	}

	/**
	 * @param error
	 */
	public void addWarning(TradeImportStatus error) {
		error.setErrorType(TradeImportStatus.WARNING);
		setThereWarnings(true);
		this.tradeImportErrors.add(error);
		
		if(isPDV()) { 
			pdvProcessingErrors.add(error);
		}
	}

	/**
	 * @return true if an error occurs while handling the received file
	 */
	public static boolean isThereAnyError(List<TradeImportStatus> errors) {
		boolean thereIsAnError = false;
		for (TradeImportStatus error : errors) {
			if (TradeImportStatus.ERROR == error.getErrorType()) {
				thereIsAnError = true;
				break;
			}
		}
		return thereIsAnError;

	}

	/**
	 * @return the errorFileName
	 */
	public String getFullLogFileName() {
		return this.fullLogFileName;
	}

	/**
	 * @param errorFileName
	 *            the errorFileName to set
	 */
	public void setFullLogFileName(String errorFileName) {
		this.fullLogFileName = errorFileName;
	}

	/**
	 * @return the warningFileName
	 */
	public String getWarningLogFileName() {
		return this.warningLogFileName;
	}

	/**
	 * @param warningFileName
	 *            the warningFileName to set
	 */
	public void setWarningLogFileName(String warningFileName) {
		this.warningLogFileName = warningFileName;
	}

	/**
	 * @return the detailedLogFileName
	 */
	public String getDetailedLogFileName() {
		return this.detailedLogFileName;
	}

	/**
	 * @param detailedLogFileName
	 *            the detailedLogFileName to set
	 */
	public void setDetailedLogFileName(String detailedLogFileName) {
		this.detailedLogFileName = detailedLogFileName;
	}

	/**
	 * @return the summaryLogFileName
	 */
	public String getSummaryLogFileName() {
		return this.summaryLogFileName;
	}

	/**
	 * @param summaryLogFileName
	 *            the summaryLogFileName to set
	 */
	public void setSummaryLogFileName(String summaryLogFileName) {
		this.summaryLogFileName = summaryLogFileName;
	}

	/**
	 * @return the tradeImportErrors
	 */
	public BlockingQueue<TradeImportStatus> getTradeImportErrors() {
		return this.tradeImportErrors;
	}

	public void initLogFiles(String summaryLogFileName, String detailedLogFileName, String fullFileName,
			String warningFileName) {
		this.summaryLogFileName = summaryLogFileName;
		this.detailedLogFileName = detailedLogFileName;
		this.fullLogFileName = fullFileName;
		this.warningLogFileName = warningFileName;
	}

	/**
	 * @return the processName
	 */
	public String getProcessName() {
		return this.processName;
	}

	/**
	 * @param processName
	 *            the processName to set
	 */
	public void setProcessName(String processName) {
		this.processName = processName;
	}

	/**
	 * Takes into account how many lines and trades of type OK, ERROR, WARNING and EXCLUDE have been count
	 */
	public void incrementKOImports(int tradeNbLegs, int typeKO) {
		synchronized (this.incremerntKOLock) {

			this.nbRowsImportedKO++;

			if (tradeNbLegs == 2) {
				this.nbRowsImportedKO++;
			}

			this.nbTradesImportedKO++;

			if (typeKO == KO_WARNING) {
				setNbRowsWarning(getNbRowsWarning() + 1);
				if (tradeNbLegs == 2) {
					setNbRowsWarning(getNbRowsWarning() + 1);
				}
				setNbTradesWarning(getNbTradesWarning() + 1);
				// GSM: 15/07/2013. Trades Exclusions, based on DDR CalypsoColl - TradeInterface_Gaps_v1.2
			} else if (typeKO == KO_EXCLUDED) {

				setNbRowsExclusion(getNbRowsExclude() + 1);
				if (tradeNbLegs == 2) {
					setNbRowsExclusion(getNbRowsExclude() + 1);
				}
				setNbTradesExclusion(getNbTradesExclude() + 1);

			} else { // errros

				setNbRowsError(getNbRowsError() + 1);
				if (tradeNbLegs == 2) {
					setNbRowsError(getNbRowsError() + 1);
				}
				setNbTradesError(getNbTradesError() + 1);
			}
			// GSM: 15/07/2013. old
			// } else {
			// setNbRowsError(getNbRowsError() + 1);
			// if (tradeNbLegs == 2) {
			// setNbRowsError(getNbRowsError() + 1);
			// }
			// setNbTradesError(getNbTradesError() + 1);
			// }

		}

	}

	/**
	 * @param number
	 *            of trades excluded
	 */
	private void setNbTradesExclusion(int i) {
		this.nbTradesExclude = i;

	}

	/**
	 * @param number
	 *            of rows excluded
	 */
	private void setNbRowsExclusion(int i) {
		this.nbRowsExclude = i;

	}

	public void incrementOKImports(int tradeNbLegs) {
		synchronized (this.incremerntOKLock) {
			setNbRowsImportedOK(getNbRowsImportedOK() + 1);
			if (tradeNbLegs == 2) {
				setNbRowsImportedOK(getNbRowsImportedOK() + 1);
			}
			this.nbTradesImportedOK++;
		}
	}

	/**
	 * @return the sourceSystem
	 */
	public String getSourceSystem() {
		return this.sourceSystem;
	}

	/**
	 * @param sourceSystem
	 *            the sourceSystem to set
	 */
	public void setSourceSystem(String sourceSystem) {
		this.sourceSystem = sourceSystem;
	}

	/**
	 * @param fileName
	 * @return a file name with the current date
	 */
	public static String getFileNameWithTimeStamp(String fileName) {
		StringBuilder datedFileName = new StringBuilder(fileName);
		Date now = new Date();

		datedFileName.append("_");
		synchronized (timeFormat) {
			datedFileName.append(timeFormat.format(now));
		}
		datedFileName.append(".txt");
		// }
		return datedFileName.toString();
	}

	/**
	 * @param processStatus
	 *            flush a message to ControlM depending on the processStatus
	 */
	public void flushControlMMessages(boolean processStatus) {
		if (processStatus) {
			ControlMErrorLogger.addError(ErrorCodeEnum.NoError, "");
		} else {
			if (!Util.isEmpty(this.criticalErrors)) {
				for (TradeImportStatus error : this.criticalErrors) {
					if (error == null) {
						continue;
					}
					int errorCode = error.getErrorCode();
					ControlMErrorLogger.addError(TradeInterfaceUtils.getCtrlMErrorFromSchedTaskErrorCode(errorCode),
							error.getErrorMessage());
				}
			}
		}
	}

	/**
	 * @return the nbRowsToBeImported
	 */
	public int getNbRowsToBeImported() {
		return this.nbRowsToBeImported;
	}

	/**
	 * @param nbRowsToBeImported
	 *            the nbRowsToBeImported to set
	 */
	public void setNbRowsToBeImported(int nbRowsToBeImported) {
		this.nbRowsToBeImported = nbRowsToBeImported;
	}

	/**
	 * @return the isOnlyWarnings
	 */
	public boolean isOnlyWarnings() {
		return this.isOnlyWarnings;
	}

	/**
	 * @param isOnlyWarnings
	 *            the isOnlyWarnings to set
	 */
	public void setOnlyWarnings(boolean isOnlyWarnings) {
		this.isOnlyWarnings = isOnlyWarnings;
	}

	/**
	 * @return the nbRowsImportedOK
	 */
	public int getNbRowsImportedOK() {
		return this.nbRowsImportedOK;
	}

	/**
	 * @param nbRowsImportedOK
	 *            the nbRowsImportedOK to set
	 */
	public void setNbRowsImportedOK(int nbRowsImportedOK) {
		this.nbRowsImportedOK = nbRowsImportedOK;
	}

	/**
	 * @return the nbRowsError
	 */
	public int getNbRowsError() {
		return this.nbRowsError;
	}

	/**
	 * @param nbRowsError
	 *            the nbRowsError to set
	 */
	public void setNbRowsError(int nbRowsError) {
		this.nbRowsError = nbRowsError;
	}

	/**
	 * @return the nbRowsWarning
	 */
	public int getNbRowsWarning() {
		return this.nbRowsWarning;
	}

	/**
	 * @param nbRowsWarning
	 *            the nbRowsWarning to set
	 */
	public void setNbRowsWarning(int nbRowsWarning) {
		this.nbRowsWarning = nbRowsWarning;
	}

	/**
	 * @return the nbRowsExclude
	 */
	public int getNbRowsExclude() {
		return this.nbRowsExclude;
	}

	/**
	 * @param nbRowsExclude
	 *            the nbRowsExclude to set
	 */
	public void setNbRowsExclude(int nbRowsExclude) {
		this.nbRowsExclude = nbRowsExclude;
	}

	/**
	 * @return the nbTradesExclude
	 */
	public int getNbTradesExclude() {
		return this.nbTradesExclude;
	}

	/**
	 * @param nbTradesExclude
	 *            the nbTradesExclude to set
	 */
	public void setNbTradesExclude(int nbTradesExclude) {
		this.nbTradesExclude = nbTradesExclude;
	}

	/**
	 * @return the nbTradesError
	 */
	public int getNbTradesError() {
		return this.nbTradesError;
	}

	/**
	 * @param nbTradesError
	 *            the nbTradesError to set
	 */
	public void setNbTradesError(int nbTradesError) {
		this.nbTradesError = nbTradesError;
	}

	/**
	 * @return the nbTradesWarning
	 */
	public int getNbTradesWarning() {
		return this.nbTradesWarning;
	}

	/**
	 * @param nbTradesWarning
	 *            the nbTradesWarning to set
	 */
	public void setNbTradesWarning(int nbTradesWarning) {
		this.nbTradesWarning = nbTradesWarning;
	}

	/**
	 * @return the isThereWarnings
	 */
	public boolean isThereWarnings() {
		return this.isThereWarnings;
	}

	/**
	 * @param isThereWarnings
	 *            the isThereWarnings to set
	 */
	public void setThereWarnings(boolean isThereWarnings) {
		this.isThereWarnings = isThereWarnings;
	}

	/**
	 * @return the logFiles
	 */
	public ArrayList<String> getLogFiles() {
		return this.logFiles;
	}

	/**
	 * @return the isThereErrors
	 */
	public boolean isThereErrors() {
		return this.isThereErrors;
	}

	/**
	 * @param isThereErrors
	 *            the isThereErrors to set
	 */
	public void setThereErrors(boolean isThereErrors) {
		this.isThereErrors = isThereErrors;
	}

	/**
	 * @return the fileBeingImportedFullPath
	 */
	public String getFileBeingImportedFullPath() {
		return this.fileBeingImportedFullPath;
	}

	/**
	 * @param fileBeingImportedFullPath
	 *            the fileBeingImportedFullPath to set
	 */
	public void setFileBeingImportedFullPath(String fileBeingImportedFullPath) {
		this.fileBeingImportedFullPath = fileBeingImportedFullPath;
	}

	/**
	 * is opt columns
	 *
	 * @return {@link boolean}
	 */
	public boolean isOptColumns() {
		return optColumns;
	}

	/**
	 * set opt columns
	 *
	 * @param optColumns optColumns
	 */
	public void setOptColumns(boolean optColumns) {
		this.optColumns = optColumns;
	}
}
