package calypsox.tk.util;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Defaults;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.CreditRating;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.refdata.RatingValues;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteMarketData;
import com.calypso.tk.util.email.MailException;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.bean.CpExternalRatingsBean;
import calypsox.tk.util.log.LogGeneric;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.FileUtility;

public class ScheduledTaskImportCpExternalRatings extends AbstractProcessFeedScheduledTask {

	// Constants
	private static final long serialVersionUID = 123L;
	private static final String SEPARATOR_DOMAIN_STRING = "Separator";
	private static final String FILEPATH = "File Path";
	private static final String STARTFILENAME = "Start of File Name";
	private static final String TASK_INFORMATION = "Import External Ratings for Contarparty from a CSV file.";
	private boolean result = true;
	private boolean warning = false;
	private boolean dateError = false;
	private boolean agencyError = false;
	private String file = "";

	private static final int ORIGINAL_NUMBER_OF_FIELDS = 8;
	private static final int NEW_NUMBER_OF_FIELDS = 10;

	private static final String LONG_TERM_MOODY_ATT = "UseLongTermMoody";
	private static final String LONG_TERM_SP_ATT = "UseLongTermS&P";
	private static final String LONG_TERM_FITCH_ATT = "UseLongTermFitch";

	private static final String CP_EXT_RATINGS = "CP_EXT_RATINGS";
	private static final String SUBJECT = "Log files external Ratings for counterparties on " + Defaults.getEnvName();
	private static final String DEFAULT_FROM_EMAIL = "calypso@gruposantander.com";

	protected static final String PROCESS = "Load of External Ratings for counterparties";
	protected static final String SUMMARY_LOG = "Summary Log";
	protected static final String DETAILED_LOG = "Detailed Log";
	protected static final String FULL_LOG = "Full Log";
	protected static final String STATIC_DATA_LOG = "Static Data Log";
	private static final String SYSTEM = "SUSI";
	private static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
	protected static SimpleDateFormat dFormat = new SimpleDateFormat("yyyymmdd");

	// Constants
	private RemoteMarketData remoteMarketData;

	private BufferedReader inputFileStream;

	private boolean proccesOK = true;
	private boolean controlMOK = true;

	private static final String SOURCE = "CP_EXT_RATINGS";

	protected LogGeneric logGen = new LogGeneric();

	// added (Bean)
	private CpExternalRatingsBean cpExtRatBean;

	/**
	 * @return this task information, gathered from the constant
	 *         TASK_INFORMATION
	 */
	@Override
	public String getTaskInformation() {
		return TASK_INFORMATION;
	}

	/**
	 * ST attributes definition
	 */

	@Override
	protected List<AttributeDefinition> buildAttributeDefinition() {
		List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
		// Gets superclass attributes
		attributeList.addAll(super.buildAttributeDefinition());

		attributeList.add(attribute(SEPARATOR_DOMAIN_STRING));
		attributeList.add(attribute(SUMMARY_LOG));
		attributeList.add(attribute(DETAILED_LOG));
		attributeList.add(attribute(FULL_LOG));
		attributeList.add(attribute(STATIC_DATA_LOG));

		return attributeList;
	}

//	/**
//	 * @return a vector with all the domain attributes for this schedule task
//	 * 
//	 */
//	@SuppressWarnings("unchecked")
//	@Override
//	public Vector<String> getDomainAttributes() {
//		final Vector<String> attr = super.getDomainAttributes();
//		attr.add(SEPARATOR_DOMAIN_STRING);
//		attr.add(SUMMARY_LOG);
//		attr.add(DETAILED_LOG);
//		attr.add(FULL_LOG);
//		attr.add(STATIC_DATA_LOG);
//		return attr;
//	}

	/**
	 * Main method to be executed in this Scheduled task
	 * 
	 * @param connection
	 *            to DS
	 * @param connection
	 *            to PS
	 * @return result of the process
	 */
	@SuppressWarnings({ "unchecked" })
	@Override
	public boolean process(final DSConnection conn, final PSConnection connPS) {

		final String path = getAttribute(FILEPATH);
		final String startFileName = getAttribute(STARTFILENAME);

		final Date d = new Date();
		String time = "";
		synchronized (timeFormat) {
			time = timeFormat.format(d);
		}

		this.logGen.generateFiles(getAttribute(DETAILED_LOG), getAttribute(FULL_LOG), getAttribute(STATIC_DATA_LOG),
				time);

		this.remoteMarketData = conn.getRemoteMarketData();

		try {

			this.logGen.initializeFiles(PROCESS);

			// We check all the files kept into the path specified in the
			// configuration for the Scheduled Task.
			final String date = CollateralUtilities.getValDateString(this.getValuationDatetime());
			final ArrayList<String> files = CollateralUtilities.getListFiles(path, startFileName + date);

			// We check if the number of matches is 1.
			if (files.size() == 1) {
				this.file = files.get(0);
				this.logGen.initilizeStaticDataLog(this.file, "LINE");
				final String filePath = path + this.file;

				try {
					if (feedPreProcess(filePath)) {

						// Just after file verifications, this method will make
						// a copy into the
						// ./import/copy/ directory
						FileUtility.copyFileToDirectory(filePath, path + "/copy/");

						CreditRating creditRating = null;
						String line;
						String[] values = null;
						boolean stopFile = false;

						try {
							this.inputFileStream = new BufferedReader(new FileReader(filePath));
							line = this.inputFileStream.readLine(); // saltar
																	// cabecera
							for (int i = 0; !stopFile && ((line = this.inputFileStream.readLine()) != null); i++) {
								this.logGen.initializeError();
								this.logGen.initializeWarning();
								this.logGen.initializeOK();
								this.logGen.initializeErrorLine();
								this.logGen.initializeWarningLine();
								this.logGen.initializeOkLine();

								boolean save = true;
								this.warning = false;
								this.dateError = false;
								this.agencyError = false;

								final Vector<CreditRating> creditRatingVectorToSave = new Vector<CreditRating>();

								if (!line.startsWith("*****")) {

									// get values
									values = getValuesFromLine(line);

									// check values
									if (checkNumberOfFields(values)) {

										// map values
										values = mapValues(values);

										this.cpExtRatBean = new CpExternalRatingsBean(values);

										creditRating = createCreditRating(line);
										if (creditRating != null) {
											try {
												// buscar rating existente con
												// mismas caracter?sticas
												final CreditRating creditRatingFound = DSConnection.getDefault()
														.getRemoteMarketData().getRating(creditRating);

												// creditRating existente,
												// actualizamos el rating_value
												if ((creditRatingFound != null)
														&& creditRatingFound.equals(creditRating)) {
													if (creditRatingFound.getRatingValue()
															.equals(creditRating.getRatingValue())) {
														save = false;
													} else {
														save = false;
														creditRatingFound.setRatingValue(creditRating.getRatingValue());
														creditRatingVectorToSave.add(creditRatingFound);
														this.remoteMarketData.saveRatings(creditRatingVectorToSave);
													}
													// creditRating no existente
													// / asOfDate incorrecta
												} else if (creditRating.getAsOfDate() == null) {
													String where = "credit_rating.legal_entity_id='"
															+ creditRating.getLegalEntityId()
															+ "' and credit_rating.rating_agency_name='"
															+ creditRating.getAgencyName()
															+ "' order by credit_rating.as_of_date DESC";
													final Vector<CreditRating> crVector = DSConnection.getDefault()
															.getRemoteMarketData().getRatings(null, where);
													// no hay rating en el
													// sistema para esa agencia
													// y esa LE. Ponemos
													// valueDate.
													if (crVector.size() == 0) {
														creditRating.setAsOfDate(getValuationDatetime().getJDate(TimeZone.getDefault()));
														// hay rating en el
														// sistema con igual
														// rating. No guardamos.
													} else if ((crVector.size() > 0) && creditRating.getRatingValue()
															.equals(crVector.get(0).getRatingValue())) {
														save = false;
														// hay rating en el
														// sistema con distinto
														// rating del que
														// tenemos.
														// Ponemos valueDate.
													} else if (crVector.size() > 0) {
														creditRating.setAsOfDate(getValuationDatetime().getJDate(TimeZone.getDefault()));
													}
												}
											} catch (final RemoteException re) {
												save = false;
												Log.error(this, re); //sonar
												this.result = false;
												this.logGen.incrementError();
												this.logGen.setErrorSavingRating(SOURCE, this.file,
														String.valueOf(this.logGen.getNumberTotal()),
														this.cpExtRatBean.getLegalEntity(), line);
												this.proccesOK = false;
											}

											try {

												if (creditRating.getLegalEntityId() != 0) {
													if (save && (this.warning == false) && (this.dateError == false)
															&& (this.agencyError == false)) {
														creditRatingVectorToSave.addElement(creditRating);
														this.remoteMarketData.saveRatings(creditRatingVectorToSave);
													}
												} else {
													this.result = false;
												}

											} catch (final RemoteException e) {
												this.result = false;
												Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while saving ratings", e);
												this.logGen.incrementError();
												this.logGen.setErrorSavingRating(SOURCE, this.file,
														String.valueOf(this.logGen.getNumberTotal()),
														this.cpExtRatBean.getLegalEntity(), line);

												this.proccesOK = false;

											}
										}

										if (this.logGen.getNumberError() > 0) {
											this.logGen.incrementRecordErrors();
										}
										if ((this.logGen.getNumberWarning() > 0)
												&& (this.logGen.getNumberError() == 0)) {
											this.logGen.incrementRecordWarning();
											this.addBadLine(line, "Required legal entity not present or not valid.");
										}
										if ((this.logGen.getNumberError() == 0)
												&& (this.logGen.getNumberWarning() == 0)) {
											this.logGen.incrementOK();
										}
										this.logGen.incrementTotal();
										if (this.logGen.getNumberOk() == 1) {
											this.logGen.setOkLine(SOURCE, this.file, this.logGen.getNumberTotal() - 1,
													"0");

										}
										this.logGen.feedFullLog(0);
										this.logGen.feedDetailedLog(0);
										if ((this.logGen.getNumberWarning() > 0)
												&& (this.logGen.getNumberError() == 0)) {
											try {
												this.logGen.feedStaticDataLog(
														String.valueOf(this.logGen.getNumberTotal() - 1), SYSTEM);
											} catch (final Exception e) {
												Log.error(LOG_CATEGORY_SCHEDULED_TASK,
														"Error. Error writing in log files. \n" + e); //sonar
												this.logGen.setErrorWritingLog(SOURCE, this.file,
														String.valueOf(i + 1));

												this.logGen.feedFullLog(0);
												this.logGen.feedDetailedLog(0);
												this.result = false;

												ControlMErrorLogger.addError(ErrorCodeEnum.LogException,
														"Unexpected error in log file");
												this.proccesOK = false;
											}
										}

									} else {
										Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error checking the number of fields.");
										this.result = false;
										this.logGen.incrementRecordErrors();
										this.logGen.incrementTotal();
										this.logGen.setErrorBadRecordFormat(SOURCE, this.file, String.valueOf(i + 1),
												"", line, values[0]);

										this.logGen.feedFullLog(0);
										this.logGen.feedDetailedLog(0);
									}
								} else {
									stopFile = true;
								}
							}
						} catch (final FileNotFoundException e) {
							Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while looking for file:" + filePath, e);
							this.logGen.incrementRecordErrors();
							this.proccesOK = false;
							this.logGen.setErrorNumberOfFiles(SOURCE, this.file);

							this.logGen.feedFullLog(0);
							this.logGen.feedDetailedLog(0);
							this.result = false;
							ControlMErrorLogger.addError(ErrorCodeEnum.InputFileNotFound, "");
							this.controlMOK = false;
						} catch (final IOException e) {
							// Unexpected error opening the file. Critical
							// error 2
							Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while reading file:" + filePath, e);
							this.result = false;
							this.logGen.incrementRecordErrors();
							this.proccesOK = false;
							this.logGen.setErrorOpeningFile(SOURCE, this.file, String.valueOf(0));

							this.logGen.feedFullLog(0);
							this.logGen.feedDetailedLog(0);
							ControlMErrorLogger.addError(ErrorCodeEnum.IOException,
									"Unexpected error opening the file");
							this.controlMOK = false;
						} finally {
							if (this.inputFileStream != null) {
								try {
									this.inputFileStream.close();
								} catch (final IOException e) {
									Log.error(LOG_CATEGORY_SCHEDULED_TASK,
											"Error while trying close input stream for the CSV file <" + getFileName()
													+ "> open previously",
											e);
									this.result = false;
									ControlMErrorLogger.addError(ErrorCodeEnum.IOException,
											"Unexpected error closing the file");
									this.proccesOK = false;
									this.controlMOK = false;
								}
							}
						}

					} else {
						// Number of lines in file does not match with
						// number of lines in control record. Critical error
						// 3
						this.logGen.incrementRecordErrors();
						this.logGen.setErrorNumberOfLines(SOURCE, this.file);

						this.logGen.feedFullLog(0);
						this.logGen.feedDetailedLog(0);
						this.proccesOK = false;
						this.result = false;
						ControlMErrorLogger.addError(ErrorCodeEnum.ControlLine, "");
						this.controlMOK = false;
					}
				} catch (final Exception e) {
					this.logGen.incrementRecordErrors();
					this.logGen.setErrorNumberOfLines(SOURCE, this.file);
					Log.error(this, e); //sonar
					this.logGen.feedFullLog(0);
					this.logGen.feedDetailedLog(0);
					this.proccesOK = false;
					this.result = false;
					ControlMErrorLogger.addError(ErrorCodeEnum.ControlLine, "");
					this.controlMOK = false;
				}
			} else {
				Log.error(LOG_CATEGORY_SCHEDULED_TASK,
						"The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem.");
				this.result = false;
				this.logGen.incrementRecordErrors();
				this.logGen.setErrorNumberOfFiles(SOURCE, startFileName);

				this.logGen.feedFullLog(0);
				this.logGen.feedDetailedLog(0);
				Log.error(LOG_CATEGORY_SCHEDULED_TASK,
						"The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem.");
				this.proccesOK = false;
				ControlMErrorLogger.addError(ErrorCodeEnum.InputFileNotFound,
						"The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem");
				this.controlMOK = false;
			}

		} catch (final IOException e2) {
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error. Log files is already existing in the system.");
			Log.error(this, e2); //sonar
			this.logGen.incrementRecordErrors();
			this.logGen.setErrorCreatingLogFile(SOURCE, startFileName);
			this.logGen.feedFullLog(0);
			this.logGen.feedDetailedLog(0);
			this.proccesOK = false;
			this.result = false;
			ControlMErrorLogger.addError(ErrorCodeEnum.LogException, "Unexpected error creating log file");
			// this.controlMOK = false;
		}

		try {
			// We handle the errors writing them into the log files.
			this.feedPostProcess(this.result);

			this.logGen.closeLogFiles();

			String sumLog = "";
			if (this.file.equals("")) {
				sumLog = this.logGen.feedGenericLogProcess(startFileName, getAttribute(SUMMARY_LOG), PROCESS,
						this.logGen.getNumberTotal() - 1);
			} else {
				sumLog = this.logGen.feedGenericLogProcess(this.file, getAttribute(SUMMARY_LOG), PROCESS,
						this.logGen.getNumberTotal() - 1);
			}

			try {
				if (!sumLog.equals("")) {
					final List<String> to = conn.getRemoteReferenceData().getDomainValues(CP_EXT_RATINGS);
					final ArrayList<String> attachments = new ArrayList<String>();
					attachments.add(sumLog);
					attachments.add(this.logGen.getStringDetailedLog());
					attachments.add(this.logGen.getStringFullLog());
					attachments.add(this.logGen.getStringStaticDataLog());
					CollateralUtilities.sendEmail(to, SUBJECT, "", DEFAULT_FROM_EMAIL, attachments);
				}
			} catch (final MailException me) {
				Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error. Error sending log mail. \n" + me); //sonar
				this.logGen.incrementRecordErrors();
				this.logGen.setErrorSentEmail(SOURCE, startFileName);
				this.logGen.feedFullLog(0);
				this.logGen.feedDetailedLog(0);
				this.proccesOK = false;
				this.result = false;
				ControlMErrorLogger.addError(ErrorCodeEnum.MailSending, "");
			}

		} catch (final Exception e) {
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error. Error creating log files. \n" + e); //sonar
			this.logGen.incrementRecordErrors();
			this.logGen.setErrorMovingFile(SOURCE, startFileName);
			this.logGen.feedFullLog(0);
			this.logGen.feedDetailedLog(0);
			this.proccesOK = false;
			this.result = false;
			ControlMErrorLogger.addError(ErrorCodeEnum.InputFileCanNotBeMoved, "");
			// this.controlMOK = false;
		}

		try {
			this.logGen.closeLogFiles();
		} catch (final IOException e) {
			Log.error(this, e); //sonar
		}

		if (this.controlMOK) {
			ControlMErrorLogger.addError(ErrorCodeEnum.NoError, "");
		}

		return this.proccesOK;
	}

	private CreditRating createCreditRating(final String line) {

		CreditRating creditRating = new CreditRating();

		// legal Entity
		LegalEntity le = null;
		String leName = this.cpExtRatBean.getLegalEntity();
		try {
			le = BOCache.getLegalEntity(getDSConnection(), leName);
			creditRating.setLegalEntityId(le.getId());
		} catch (final Exception e) {
			Log.error(this, e); //sonar
			this.logGen.incrementWarning();
			this.logGen.setWarningRequiredFieldNotPresentNotValid(SOURCE, this.file,
					String.valueOf(this.logGen.getNumberTotal()), "43", "LEGAL_ENTITY",
					this.cpExtRatBean.getLegalEntity(), this.cpExtRatBean.getLegalEntity(), line);
			this.warning = true;
			this.result = false;
			return null;
		}

		// agency
		LegalEntityAttribute useLongTermtAttribute = null;
		String useLongTermtAttributeValue = null;
		boolean useLT = false;
		String agency = this.cpExtRatBean.getAgency();

		// FITCH
		if (agency.equals("Fitch")) {
			creditRating.setAgencyName(agency);
			useLongTermtAttribute = BOCache.getLegalEntityAttribute(getDSConnection(), 0, le.getId(), "ALL",
					LONG_TERM_FITCH_ATT);
			if (useLongTermtAttribute != null) {
				useLongTermtAttributeValue = useLongTermtAttribute.getAttributeValue();
				if (!Util.isEmpty(useLongTermtAttributeValue) && useLongTermtAttributeValue.equals("true")) {
					useLT = true;
				}
			}
			// MOODYS
		} else if (agency.equals("Moodys")) {
			creditRating.setAgencyName("Moody");
			useLongTermtAttribute = BOCache.getLegalEntityAttribute(getDSConnection(), 0, le.getId(), "ALL",
					LONG_TERM_MOODY_ATT);
			if (useLongTermtAttribute != null) {
				useLongTermtAttributeValue = useLongTermtAttribute.getAttributeValue();
				if (!Util.isEmpty(useLongTermtAttributeValue) && useLongTermtAttributeValue.equals("true")) {
					useLT = true;
				}
			}
			// SP
		} else if (agency.equals("SyP")) {
			creditRating.setAgencyName("S&P");
			useLongTermtAttribute = BOCache.getLegalEntityAttribute(getDSConnection(), 0, le.getId(), "ALL",
					LONG_TERM_SP_ATT);
			if (useLongTermtAttribute != null) {
				useLongTermtAttributeValue = useLongTermtAttribute.getAttributeValue();
				if (!Util.isEmpty(useLongTermtAttributeValue) && useLongTermtAttributeValue.equals("true")) {
					useLT = true;
				}
			}
		} else {
			this.agencyError = true;
			this.logGen.incrementError();
			this.logGen.setErrorAgencyNotValid(SOURCE, this.file, String.valueOf(this.logGen.getNumberTotal()),
					this.cpExtRatBean.getAgency(), line);
			this.result = false;
			return null;
		}

		// rating type
		creditRating.setRatingType("Current");

		// rating value & as of date
		String ratingValue = null;
		String asOfDate = null;
		if (useLT) {
			// debt seniority
			creditRating.setDebtSeniority("LONG_TERM");
			// VALUE LT
			ratingValue = this.cpExtRatBean.getValueLT();
			if (isValidRatingValue(ratingValue, creditRating, line)) {
				creditRating.setRatingValue(ratingValue);
			} else {
				return null;
			}
			asOfDate = this.cpExtRatBean.getFromDateLT();
			setAsOfDate(asOfDate, creditRating);
		} else {
			// debt seniority
			creditRating.setDebtSeniority("SENIOR_UNSECURED");
			// VALUE SU
			ratingValue = this.cpExtRatBean.getValueSU();
			String updatedValue = preprocessRatingValue(ratingValue);
			updatedValue = getParentRatingIflegalEntityRequireIt(updatedValue, creditRating, line);
			if (isValidRatingValue(updatedValue, creditRating, line)) {
				creditRating.setRatingValue(updatedValue);
			} else {
				return null;
			}
			asOfDate = this.cpExtRatBean.getFromDateSU();
			setAsOfDate(asOfDate, creditRating);
		}

		return creditRating;

	}

	// Check if LE is included in DV list to get parent rating. If not, return
	// its rating.
	@SuppressWarnings("unchecked")
	private String getParentRatingIflegalEntityRequireIt(String ratingValue, CreditRating creditRating, String line) {
		Vector<String> legalEntitiesUseParentRating = null;
		try {
			legalEntitiesUseParentRating = DSConnection.getDefault().getRemoteReferenceData()
					.getDomainValues("legalEntitiesUseParentRating");
			if (!Util.isEmpty(legalEntitiesUseParentRating)) {
				LegalEntity le = BOCache.getLEFromCache(creditRating.getLegalEntityId());
				if (legalEntitiesUseParentRating.contains(le.getCode())) {
					Vector<CreditRating> parentCreditRating = this.remoteMarketData.getRatings(null,
							"credit_rating.rating_agency_name=" + Util.string2SQLString(creditRating.getAgencyName())
									+ " and credit_rating.rating_type="
									+ Util.string2SQLString(creditRating.getRatingType())
									+ " and credit_rating.legal_entity_id=" + String.valueOf(le.getParentId())
									+ " order by as_of_date DESC");
					if (parentCreditRating.size() > 0) {
						return parentCreditRating.get(0).getRatingValue();
					}
				}
			}
		} catch (RemoteException e) {
			Log.error(this, e); //sonar
			this.logGen.incrementError();
			this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.file,
					String.valueOf(this.logGen.getNumberTotal()), "45", "RATING", this.cpExtRatBean.getLegalEntity(),
					line);
			this.result = false;
		}
		return ratingValue;
	}

	// Check if rating passed is valid (it means it's included on current type
	// agency list.
	@SuppressWarnings("unchecked")
	private boolean isValidRatingValue(String ratingValue, CreditRating creditRating, String line) {
		RatingValues ratingValues = null;
		try {
			ratingValues = DSConnection.getDefault().getRemoteReferenceData().getRatingValues();
			final Vector<String> ratingValuesVector = ratingValues.getRatingValues(creditRating.getAgencyName(),
					creditRating.getRatingType());

			if (ratingValuesVector.contains(ratingValue)) {
				return true;
			} else {
				this.logGen.incrementError();
				this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.file,
						String.valueOf(this.logGen.getNumberTotal()), "45", "RATING",
						this.cpExtRatBean.getLegalEntity(), line);
				this.result = false;
				this.warning = true;
			}
		} catch (final Exception e) {
			Log.error(this, e); //sonar
			this.logGen.incrementError();
			this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.file,
					String.valueOf(this.logGen.getNumberTotal()), "45", "RATING", this.cpExtRatBean.getLegalEntity(),
					line);
			this.result = false;
		}
		return false;
	}

	// Obtain and set asOfDate value
	private void setAsOfDate(String asOfDate, CreditRating creditRating) {
		try {
			// We convert the String date to JDate.
			synchronized (dFormat) {
				dFormat.setLenient(false);
				dFormat.parse(asOfDate);
			}
			final int day = Integer.parseInt(asOfDate.substring(6));
			final int month = Integer.parseInt(asOfDate.substring(4, 6));
			final int year = Integer.parseInt(asOfDate.substring(0, 4));
			creditRating.setAsOfDate(JDate.valueOf(year, month, day));
		} catch (final Exception e) {
			Log.error(this, e); //sonar
		}
	}

	// Read management //

	// Get values from text line and save them in string array
	private String[] getValuesFromLine(String line) {

		// String aux = line.substring(0, line.lastIndexOf("|"));
		return line.split("\\|", -1);

	}

	// Check fields number is above minimum limit
	private boolean checkNumberOfFields(String[] values) {

		return (values.length >= ORIGINAL_NUMBER_OF_FIELDS);

	}

	// Map original array to size-formated util values array
	private String[] mapValues(String[] values) {

		int totalFields = NEW_NUMBER_OF_FIELDS;

		String[] totalValues = new String[totalFields];

		// put blank
		for (int i = 0; i < totalFields; i++) {
			totalValues[i] = "";
		}

		for (int i = 0; i < values.length; i++) {
			// common to all files
			if (i < 5) {
				totalValues[i] = values[i].trim();
			}
			// old file (without long term)
			else if ((i >= 5) && (values.length == ORIGINAL_NUMBER_OF_FIELDS)) {
				totalValues[i + 1] = values[i].trim();
			}
			// new file (fitch & moodys cases)
			else if ((i >= 5) && (values.length == NEW_NUMBER_OF_FIELDS)) {
				totalValues[i] = values[i].trim();
				// new file (sp case)
			} else if ((i >= 5) && (values.length == (NEW_NUMBER_OF_FIELDS + 2))) {
				if (i == 9) {
					totalValues[i] = values[i + 2].trim();
					break;
				}
				totalValues[i] = values[i + 1].trim();
			}
		}

		return totalValues;
	}

	// Others //
	public String preprocessRatingValue(String ratingValue) {

		if (ratingValue.contains("*")) {
			if (ratingValue.contains(" ")) {
				final String[] ratingValues = ratingValue.split(" ");
				ratingValue = ratingValues[0];
			} else {
				final String[] ratingValues = ratingValue.split("/");
				ratingValue = ratingValues[0];
			}
		}

		if (ratingValue.contains("E")) {
			final String[] ratingValues = ratingValue.split("E");
			ratingValue = ratingValues[0];
		} else if (ratingValue.contains("e")) {
			final String[] ratingValues = ratingValue.split("e");
			ratingValue = ratingValues[0];
		}

		if (ratingValue.contains("(P)")) {
			final String[] ratingValues = ratingValue.split("\\)");
			ratingValue = ratingValues[1];
		}

		return ratingValue;

	}

	@Override
	public String getFileName() {
		return this.file;
	}

}
