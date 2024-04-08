package calypsox.tk.util;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Defaults;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.sql.DeadLockException;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.FeedAddress;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteMarketData;
import com.calypso.tk.util.email.MailException;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.bean.FeedFileInfoBean;
import calypsox.tk.util.bean.InterestInflationRatesBean;
import calypsox.tk.util.log.LogGeneric;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.FileUtility;
import calypsox.util.SantReportingUtil;

/**
 * Scheduled Task to import Interest & Inflation Rates.
 * 
 * @author Jose David Sevillano (josedavid.sevillano@siag.es) interface control
 *         by David Porras Mart?nez
 * @modified Guillermo Solano
 * 
 */
public class ScheduledTaskImpIntInflationRatesOld extends AbstractProcessFeedScheduledTask {

	// class variables
	private static final long serialVersionUID = 123L;
	private static final String SEPARATOR_DOMAIN_STRING = "Separator";
	private static final String QUOTENAME_DOMAIN_STRING = "Quote Set Name";
	private static final String TASK_INFORMATION = "Import Market Data Interest & Inflation Rates from a CSV file.";
	private static final String INTINF = "INTINF";
	protected static final String SUMMARY_LOG = "Summary Log";
	protected static final String DETAILED_LOG = "Detailed Log";
	protected static final String FULL_LOG = "Full Log";
	protected static final String STATIC_DATA_LOG = "Static Data Log";
	private static final String SUBJECT = "Log files for Import Interest and Inflation rates on "
			+ Defaults.getEnvName();
	private static final String DEFAULT_FROM_EMAIL = "calypso@gruposantander.com";
	protected static final String PROCESS = "Load interest and inflation rates";
	private static final String SYSTEM = "ASSET_CONTROL";
	private static final String SOURCE = "INT-INF";

	private static final int MAX_NUMBER_FIELDS = 3;
	private static final String FILE_TYPE_INF_INT = "Is Interest File Type";
	protected static SimpleDateFormat dFormat = new SimpleDateFormat("yyyymmdd");

	// variables for errors control
	private static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

	protected LogGeneric logGen = new LogGeneric();

	private boolean processOK = true;
	private boolean controlMOK = true;
	private String fileName = "";

	private RemoteMarketData remoteMarketData;
	private BufferedReader inputFileStream;
	private boolean bResult = true;
	private String file = "";
	private HashMap<String, String> feedAddressMap = new HashMap<String, String>();
	// private int interestFlag = 0; // for indicates if we have an interest
	// import

	// added (Bean)
	private InterestInflationRatesBean intInfRatBean;

	@Override
	public String getTaskInformation() {
		return TASK_INFORMATION;
	}

	/**
	 * ST attributes definition
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected List<AttributeDefinition> buildAttributeDefinition() {
		List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
		// Gets superclass attributes
		attributeList.addAll(super.buildAttributeDefinition());

		attributeList.add(attribute(SEPARATOR_DOMAIN_STRING));
		try {
			attributeList.add(attribute(QUOTENAME_DOMAIN_STRING)
					.domain(new ArrayList<String>(DSConnection.getDefault().getRemoteMarketData().getQuoteSetNames())));
		} catch (CalypsoServiceException e) {
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while retrieving quotes name", e);
		}
		attributeList.add(attribute(FILE_TYPE_INF_INT).booleanType());
		attributeList.add(attribute(SUMMARY_LOG));
		attributeList.add(attribute(DETAILED_LOG));
		attributeList.add(attribute(FULL_LOG));
		attributeList.add(attribute(STATIC_DATA_LOG));

		return attributeList;
	}



	/**
	 * Main process to be done in this Schedule task
	 */
	@Override
	public boolean process(final DSConnection conn, final PSConnection connPS) {

		final String path = getAttribute(FILEPATH);
		final String startFileName = getAttribute(STARTFILENAME);

		final java.util.Date d = new java.util.Date();
		String time = "";
		boolean isInterestFile = false;

		synchronized (timeFormat) {
			time = timeFormat.format(d);
		}

		// fix
		this.logGen.decrementTotal();

		this.logGen.generateFiles(getAttribute(DETAILED_LOG), getAttribute(FULL_LOG), getAttribute(STATIC_DATA_LOG),
				time);

		this.remoteMarketData = conn.getRemoteMarketData();
		Timestamp startTime = null, endTime = null;

		// we add the header and assign the fileWriter to the logs files.
		try {
			// if (!this.logGen.validateFilesExistence()) {
			this.logGen.initializeFiles(PROCESS);

			// We check all the files kept into the path specified in the
			// configuration for the Scheduled Task.
			final String dateString = CollateralUtilities.getValDateString(this.getValuationDatetime());
			final ArrayList<String> files = CollateralUtilities.getListFiles(path, startFileName + dateString);

			// We check if the number of matches is 1.
			if (files.size() == 1) {
				this.file = files.get(0);
				this.fileName = files.get(0);
				this.logGen.initilizeStaticDataLog(this.file, "LINE");
				final String filePath = path + this.file;

				// import process start time
				startTime = new Timestamp(
						DSConnection.getDefault().getRemoteAccess().getServerCurrentDatetime().getTime());

				try {
					if (feedPreProcess(filePath)) {
						final JDate jdate = CollateralUtilities.getFileNameDate(this.file);
						// GSM: 17/05/2013. To catch exception if attribute is
						// empty
						isInterestFile = readAttributeIsInterestFile();
						if (jdate == null) {
							Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while looking for the date in the filename");
							this.bResult = false;
							this.processOK = false;
							this.logGen.incrementError();
							this.logGen.setErrorDateNotValid(SOURCE, this.file,
									String.valueOf(this.logGen.getNumberTotal()), "", "");

							ControlMErrorLogger.addError(ErrorCodeEnum.FileNameDate, "");
							this.controlMOK = false;
							this.logGen.feedFullLog(0);
							this.logGen.feedDetailedLog(0);
						} else {

							// Just after file verifications, this method will
							// make a copy into the
							// ./import/copy/ directory
							FileUtility.copyFileToDirectory(filePath, path + "/copy/");

							this.feedAddressMap = getFeedAddress(conn);
							String line;
							String[] values = null;
							HashMap<String, InterestInflationRatesBean> importedQuotes = new HashMap<String, InterestInflationRatesBean>();
							Vector<QuoteValue> quoteValues = new Vector<QuoteValue>();
							QuoteValue qv = new QuoteValue();
							int flag = 0;

							try {

								this.inputFileStream = new BufferedReader(new FileReader(filePath));
								// We read the file.
								for (int i = 0; ((line = this.inputFileStream.readLine()) != null)
										&& (flag == 0); i++) {

									if (!line.startsWith("*****")) {
										this.logGen.incrementTotal();
										this.logGen.initializeError();
										this.logGen.initializeWarning();
										this.logGen.initializeOK();
										this.logGen.initializeErrorLine();
										this.logGen.initializeWarningLine();
										this.logGen.initializeOkLine();

										if (checkIsValidFile(line)) {
											// GSM:Fix to use FEDFUNDs with date
											// & old version without date
											// GSM: modified 17/05/2013
											if (CollateralUtilities.checkFields(line, '|', 2)) {
												// if
												// (this.fileName.contains("INFLATION"))
												// {
												values = CollateralUtilities.splitMejorado(3, "|", false, line);

											} else {
												values = CollateralUtilities.splitMejorado(2, "|", false, line);
											}

											// for (int ii = 0; ii <
											// values.length; ii++) {
											// values[ii] = values[ii].trim();
											// }
											// map the splitted values. In case
											// some fields haven't been
											// received, left
											// blank
											values = mapValues(values);

											this.intInfRatBean = new InterestInflationRatesBean(values);
											importedQuotes = new HashMap<String, InterestInflationRatesBean>();
											quoteValues = new Vector<QuoteValue>();
											importedQuotes.put(this.intInfRatBean.getIndexKey(), this.intInfRatBean);
											qv = createQuoteValues(importedQuotes, jdate, line, isInterestFile);
											if (qv != null) {
												quoteValues.add(qv);
											}
											try {

												this.remoteMarketData.saveQuoteValues(quoteValues);

											} catch (final RemoteException e) {
												Log.error(LOG_CATEGORY_SCHEDULED_TASK,
														"Error while saving quotes values", e);
												this.bResult = false;
												this.processOK = false;
												this.logGen.incrementError();
												this.logGen.setErrorSavingQuote(SOURCE, startFileName,
														String.valueOf(i + 1), values[0], line);
												flag = 1;
											}

										} else {
											Log.error(LOG_CATEGORY_SCHEDULED_TASK,
													"Error checking the number of fields.");
											this.bResult = false;
											// error number 5
											this.logGen.incrementError();
											this.logGen.setErrorBadRecordFormat(SOURCE, this.file,
													String.valueOf(i + 1), "", line, values[0]);

										}
										if (this.logGen.getNumberError() > 0) {
											this.logGen.incrementRecordErrors();
										}
										if ((this.logGen.getNumberWarning() > 0)
												&& (this.logGen.getNumberError() == 0)) {
											this.logGen.incrementRecordWarning();
											try {
												this.logGen.feedStaticDataLog(
														String.valueOf(this.logGen.getNumberTotal()), SYSTEM);

											} catch (final Exception e) {
												Log.error(LOG_CATEGORY_SCHEDULED_TASK,
														"Error. Error writing in log files.");
												Log.error(this, e); //sonar
												this.logGen.setErrorWritingLog(SOURCE, this.fileName,
														String.valueOf(i + 1));

												this.logGen.feedFullLog(0);
												this.logGen.feedDetailedLog(0);

												this.bResult = false;
											}
											this.addBadLine(line, "Error in line");
										}
										if ((this.logGen.getNumberError() == 0)
												&& (this.logGen.getNumberWarning() == 0)) {
											this.logGen.incrementOK();
										}
										if (this.logGen.getNumberOk() == 1) {
											this.logGen.setOkLine(SOURCE, this.file, i + 1, "");

										}
										this.logGen.feedFullLog(0);
										this.logGen.feedDetailedLog(0);
									}
								}

							} catch (final FileNotFoundException e) {
								Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while looking for file:" + filePath, e);
								this.bResult = false;
								// error number 1
								this.logGen.incrementRecordErrors();
								this.processOK = false;
								this.logGen.setErrorNumberOfFiles(SOURCE, this.fileName);

								ControlMErrorLogger.addError(ErrorCodeEnum.InputFileNotFound, "");
								this.controlMOK = false;
								this.logGen.feedFullLog(0);
								this.logGen.feedDetailedLog(0);
							} catch (final IOException e) {
								Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while reading file:" + filePath, e);
								this.bResult = false;
								// error number 2
								this.logGen.incrementRecordErrors();
								this.processOK = false;
								this.logGen.setErrorOpeningFile(SOURCE, this.fileName, "0");

								ControlMErrorLogger.addError(ErrorCodeEnum.IOException,
										"Unexpected error opening the file");
								this.controlMOK = false;
								this.logGen.feedFullLog(0);
								this.logGen.feedDetailedLog(0);

							} finally {
								if (this.inputFileStream != null) {
									try {
										this.inputFileStream.close();
									} catch (final IOException e) {
										Log.error(LOG_CATEGORY_SCHEDULED_TASK,
												"Error while trying close input stream for the CSV file <"
														+ getFileName() + "> open previously",
												e);
										this.bResult = false;
									}
								}
							}

							// import process end time
							endTime = new Timestamp(
									DSConnection.getDefault().getRemoteAccess().getServerCurrentDatetime().getTime());

							String result;
							if (this.bResult) {
								result = "OK";
							} else {
								result = "FAIL";
							}

							// set necessary values in feed_file_info table
							// if we have an interest import
							if (isInterestFile) {
								// if (this.interestFlag == 0) {
								// String result;
								// if (this.bResult) {
								// result = "OK";
								// } else {
								// result = "FAIL";
								// }

								final Date date = new Date(getValuationDatetime().getTime());
								String strFile;
								// Fill file imported with file name less
								// _fecha.txt
								if (this.file.substring(0, 11).equals("CALINDEXMAD")) {
									strFile = this.file.substring(0, 11);
								} else {
									strFile = this.file.substring(0, 17);
								}

								final FeedFileInfoBean ffiBean = new FeedFileInfoBean("importInterestRates", 0,
										startTime, endTime, date, strFile, "IN", result,
										(this.logGen.getNumberTotal() - this.logGen.getRecordWarning()
												- this.logGen.getRecordErrors()),
										this.logGen.getRecordWarning(), this.logGen.getRecordErrors(), this.file,
										"No comments"); // review
								// values for number ok, err, war
								try {
									SantReportingUtil.getSantReportingService(conn).setFeedFileInfoData(ffiBean);

								} catch (final DeadLockException e1) {
									Log.error(this, e1); //sonar
								} catch (final RemoteException e1) {
									Log.error(this, e1); //sonar
								}
							}

						}
					} else {
						// Number of lines in file does not match with
						// number of lines in control record. Critical error
						// 3
						this.logGen.incrementRecordErrors();
						this.logGen.setErrorNumberOfLines(SOURCE, this.file);

						ControlMErrorLogger.addError(ErrorCodeEnum.ControlLine,
								"Number of lines in file does not match with number of lines in control record");
						this.controlMOK = false;
						this.processOK = false;
						this.bResult = false;
						this.logGen.feedFullLog(0);
						this.logGen.feedDetailedLog(0);
					}

				} catch (final Exception e) {
					Log.error(LOG_CATEGORY_SCHEDULED_TASK, e.toString(), e);
					this.bResult = false;
					this.logGen.incrementRecordErrors();
					this.controlMOK = false;
					this.logGen.feedFullLog(0);
					this.logGen.feedDetailedLog(0);
					this.processOK = false;

					if (e.getMessage() != null) {
						// Number of lines in file does not match with
						// number of lines in control record. Critical error
						// 3
						if (e.getMessage().equals("Error in the number of lines.")) {
							// error number 3
							// this.logGen.incrementRecordErrors();
							this.logGen.setErrorNumberOfLines(SOURCE, this.file);
							ControlMErrorLogger.addError(ErrorCodeEnum.ControlLine,
									"Number of lines in file does not match with number of lines in control record");
							// this.controlMOK = false;
							// this.logGen.feedFullLog(0);
							// this.logGen.feedDetailedLog(0);
							// this.processOK = false;
						} else if (e.getMessage().equals("Error in date of the file.")) {
							// error number 41
							// this.logGen.incrementRecordErrors();
							this.logGen.setErrorDateNotValid(SOURCE, this.file,
									String.valueOf(this.logGen.getNumberTotal()), "", "");

							ControlMErrorLogger.addError(ErrorCodeEnum.ControlLine, "Date not valid");
							// this.controlMOK = false;
							// this.logGen.feedFullLog(0);
							// this.logGen.feedDetailedLog(0);
							// this.processOK = false;
						} else if (e.getMessage().contains("Missing field ")
								&& e.getMessage().contains(". Please set the ScheduleTask properly.")) {
							this.logGen.setErrorAttributeSTmissing(SOURCE, this.file, e.getMessage());
							ControlMErrorLogger.addError(ErrorCodeEnum.ControlLine, e.getMessage());

						} else {
							// error number 2
							// this.logGen.incrementRecordErrors();
							// this.processOK = false;
							this.logGen.setErrorOpeningFile(SOURCE, this.fileName, "0");

							ControlMErrorLogger.addError(ErrorCodeEnum.LogException,
									"Unexpected error opening the file"); // TODO
																			// LOG
																			// IO_ERROR
							// this.controlMOK = false;
							// this.logGen.feedFullLog(0);
							// this.logGen.feedDetailedLog(0);
						}
					}
				}

			} else {
				Log.error(LOG_CATEGORY_SCHEDULED_TASK,
						"The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem.");
				this.bResult = false;
				// error number 1
				this.logGen.incrementRecordErrors();
				this.logGen.setErrorNumberOfFiles(SOURCE, this.file);

				ControlMErrorLogger.addError(ErrorCodeEnum.InputFileNotFound, "");
				this.controlMOK = false;
				this.logGen.feedFullLog(0);
				this.logGen.feedDetailedLog(0);
				this.processOK = false;
			}

		} catch (final IOException e2) {
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error. Error creating log files.");
			Log.error(this, e2); //sonar
			this.bResult = false;
			this.processOK = false;
			this.logGen.incrementRecordErrors();
			this.logGen.setErrorCreatingLogFile(SOURCE, startFileName);
			ControlMErrorLogger.addError(ErrorCodeEnum.LogException, "Error creating log files"); // TODO
																									// LOG
																									// IO_ERROR
			// this.controlMOK = false;
			this.logGen.feedFullLog(0);
			this.logGen.feedDetailedLog(0);
		}

		try {
			this.feedPostProcess(this.bResult);

			this.logGen.closeLogFiles();

			String sumLog = "";
			if (this.file.equals("")) {

				sumLog = this.logGen.feedGenericLogProcess(startFileName, getAttribute(SUMMARY_LOG), PROCESS,
						this.logGen.getNumberTotal());
			} else {

				sumLog = this.logGen.feedGenericLogProcess(this.file, getAttribute(SUMMARY_LOG), PROCESS,
						this.logGen.getNumberTotal());
			}
			try {
				if (!sumLog.equals("")) {
					final List<String> to = conn.getRemoteReferenceData().getDomainValues(INTINF);
					final ArrayList<String> attachments = new ArrayList<String>();
					attachments.add(sumLog);
					attachments.add(this.logGen.getStringDetailedLog());
					attachments.add(this.logGen.getStringFullLog());
					attachments.add(this.logGen.getStringStaticDataLog());
					CollateralUtilities.sendEmail(to, SUBJECT, "", DEFAULT_FROM_EMAIL, attachments);
				}
			} catch (final MailException me) {
				Log.error(LOG_CATEGORY_SCHEDULED_TASK, me);
				this.logGen.incrementRecordErrors();
				this.logGen.setErrorSentEmail(SOURCE, startFileName);
				// ControlMErrorLogger.addError(ErrorCodeEnum.MailSending, "");
				this.logGen.feedFullLog(0);
				this.logGen.feedDetailedLog(0);
				this.processOK = false;
				this.bResult = false;
			}

		} catch (final Exception e) {
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error moving historic files and creating bad file.\n");
			Log.error(this, e); //sonar
			this.bResult = false;
			this.processOK = false;
			this.logGen.incrementRecordErrors();
			this.logGen.setErrorMovingFile(SOURCE, startFileName);

			ControlMErrorLogger.addError(ErrorCodeEnum.InputFileCanNotBeMoved, "Error moving historic files");
			this.controlMOK = false;
			this.logGen.feedFullLog(0);
			this.logGen.feedDetailedLog(0);
		}

		// Close log files.
		try {
			this.logGen.closeLogFiles();
		} catch (final IOException e) {
			Log.error(this, e); //sonar
		}

		if (this.controlMOK) {
			ControlMErrorLogger.addError(ErrorCodeEnum.NoError, "");
		}
		return this.processOK;
	}

	/*
	 * Reads the new attribute from the ST to know if this file is an interest
	 * file or not. If not present, an exception is generated.
	 */
	private boolean readAttributeIsInterestFile() throws Exception {

		final String interestFile = getAttribute(FILE_TYPE_INF_INT);
		final String error = "Missing field " + FILE_TYPE_INF_INT + ". Please set the ScheduleTask properly.";

		if (((interestFile == null) || interestFile.trim().isEmpty())
				|| (!interestFile.trim().equalsIgnoreCase("true") && !interestFile.trim().equalsIgnoreCase("false"))) {
			throw new Exception(error);
		}

		return interestFile.trim().equalsIgnoreCase("true");
	}

	/*
	 * Checks current line of the file is OK
	 */
	private boolean checkIsValidFile(String line) {
		if  ((CollateralUtilities.checkFields(line, '|', 1)
				&& (this.fileName.contains("IR") || this.fileName.contains("CALINDEXMAD")))
				|| ((CollateralUtilities.checkFields(line, '|', 2)) && (this.fileName.contains("INFLATION")))
				|| ((CollateralUtilities.checkFields(line, '|', 2)) && (this.fileName.contains("IR"))))
			return true;
		else if  ((CollateralUtilities.checkFields(line, '|', 1)) && (this.fileName.contains("CAL_FX_NY")))
			return true;
		else return ((CollateralUtilities.checkFields(line, '|', 1)) && (this.fileName.contains("acny_mdr_fx")));
	}

	/*
	 * As we will have different fields formats in files during development this
	 * is intended to be able to add blanck values if we process a file with
	 * less fields than 3 (what will be really receiving in a future)
	 */
	private String[] mapValues(String[] values) {

		int totalFields = MAX_NUMBER_FIELDS;

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

	/*
	 * Gathers the feed
	 */
	@SuppressWarnings("unchecked")
	private HashMap<String, String> getFeedAddress(final DSConnection conn) {
		final HashMap<String, String> feedHash = new HashMap<String, String>();

		try {
			final Vector<FeedAddress> feeds = conn.getRemoteMarketData().getAllFeedAddress();
			if ((null != feeds) && (feeds.size() > 0)) {
				for (int i = 0; i < feeds.size(); i++) {
					feedHash.put(feeds.get(i).getFeedAddress(), feeds.get(i).getQuoteName());
				}
			}
		} catch (final RemoteException e) {
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while retrieving Feed Addresses", e);
			this.bResult = false;
		}
		return feedHash;
	}

	/**
	 * Creates the quote value in Calypso.
	 * 
	 * @param importedQuotes
	 *            HashMap with the information for a quote value retrieved from
	 *            the CSV file.
	 * @return Vector with the Quote Value created.
	 * 
	 *         MODIFIED (Bean)
	 * 
	 */
	private QuoteValue createQuoteValues(final HashMap<String, InterestInflationRatesBean> importedQuotes,
			final JDate jdate, final String line, boolean isInterest) {
		QuoteValue result = null;
		final Collection<InterestInflationRatesBean> quotes = importedQuotes.values();
		final Iterator<InterestInflationRatesBean> iterQuotes = quotes.iterator();
		final String quoteSetName = getAttribute(QUOTENAME_DOMAIN_STRING);
		QuoteValue qv = new QuoteValue();
		InterestInflationRatesBean inInfRatBean2;
		String quoteName = "";
		String type = QuoteValue.YIELD;
		JDate date = jdate;
		double price = 0.0;
		int year, month, day;

		// GSM: added boolean to decide type. // GSM: modified 17/05/2013
		if (isInterest) {
			type = QuoteValue.YIELD;
		} else {
			type = QuoteValue.PRICE;
		}

		while ((iterQuotes.hasNext())) {

			inInfRatBean2 = iterQuotes.next();
			quoteName = this.feedAddressMap.get(inInfRatBean2.getIndexKey());
			try {
				price = new Double(inInfRatBean2.getPrice()).doubleValue();
			} catch (final NumberFormatException e) {
				this.logGen.incrementError();
				this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.file,
						String.valueOf(this.logGen.getNumberTotal()), "44", "PRICE", inInfRatBean2.getIndexKey(), line);
				Log.error(this, e); //sonar
				this.bResult = false;
			}
			// We check the number of values retrieved from each row of the CSV
			// file. If we have 3 fields, we are in the Inflation Rates
			// importation.
			// Else, we are in the Indexes Rates importation.
			if (inInfRatBean2.getNElem() == 3) {
				// this.interestFlag = 1; // no interest import
				// type = QuoteValue.PRICE;
				// We retrieve the year, the month and the day for the date
				// passed in each row.
				try {
					synchronized (dFormat) {
						dFormat.setLenient(false);
						dFormat.parse(inInfRatBean2.getDate());
					}
					year = Integer.parseInt(inInfRatBean2.getDate().substring(0, 4));
					month = Integer.parseInt(inInfRatBean2.getDate().substring(4, 6));
					day = Integer.parseInt(inInfRatBean2.getDate().substring(6));
					date = JDate.valueOf(year, month, day);
				} catch (final Exception ex) {
					date = null;
					// error number 41
					this.logGen.incrementError();
					this.logGen.setErrorDateNotValid(SOURCE, this.file, String.valueOf(this.logGen.getNumberTotal()),
							inInfRatBean2.getIndexKey(), line);
					Log.error(this, ex); //sonar
					this.bResult = false;
				}
				// } else {
				// price = price / 100;
			}
			// GSM: modified 17/05/2013
			if (isInterest) {
				price = price / 100;
			}

			if ((null != quoteName) && (null != date)) {

				qv = new QuoteValue(quoteSetName, quoteName, date, type);
				qv.setClose(price);
				// Only add the QuoteValue when we have found the quote name for
				// the feed name related.
				result = qv;
			} else {

				if (quoteName == null) {
					// error number 40
					this.logGen.incrementWarning();
					this.logGen.setWarningRequiredFieldNotPresentNotValid(SOURCE, this.file,
							String.valueOf(this.logGen.getNumberTotal()), "40", "INDEX", inInfRatBean2.getIndexKey(),
							inInfRatBean2.getIndexKey(), line);

					this.bResult = false;
				}
			}
		}
		return result;
	}

	/**
	 * return name of the file
	 */
	@Override
	public String getFileName() {
		return this.file;
	}
}
