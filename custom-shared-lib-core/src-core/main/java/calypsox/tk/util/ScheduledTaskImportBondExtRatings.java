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
import java.util.Vector;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Defaults;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.ProductCreditRating;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.RatingValues;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteMarketData;
import com.calypso.tk.service.RemoteProduct;
import com.calypso.tk.util.email.MailException;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.bean.BondExtRatingsBean;
import calypsox.tk.util.log.LogGeneric;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.FileUtility;

public class ScheduledTaskImportBondExtRatings extends AbstractProcessFeedScheduledTask {

	// Constants
	private static final long serialVersionUID = 123L;

	private static final String ISIN = "ISIN";
	private static final String SEPARATOR_DOMAIN_STRING = "Separator";
	private static final String FILEPATH = "File Path";
	private static final String STARTFILENAME = "Start of File Name";
	private static final String TASK_INFORMATION = "Import External Ratings for Bond from a CSV file.";

	private static final String BOND_EXT_RATINGS = "BOND_EXT_RATINGS";
	private static final String SUBJECT = "Log files for Bond external Ratings on " + Defaults.getEnvName();
	private static final String DEFAULT_FROM_EMAIL = "calypso@gruposantander.com";

	protected static final String PROCESS = "Load of Bond External Ratings";
	protected static final String SUMMARY_LOG = "Summary Log";
	protected static final String DETAILED_LOG = "Detailed Log";
	protected static final String FULL_LOG = "Full Log";
	protected static final String STATIC_DATA_LOG = "Static Data Log";
	private static final String SYSTEM = "SUSI";
	private static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
	protected static SimpleDateFormat dFormat = new SimpleDateFormat("yyyymmdd");

	// variables
	private RemoteMarketData remoteMarketData;
	private RemoteProduct remoteProduct;

	private BufferedReader inputFileStream;
	private boolean result = true;
	private String file = "";

	private boolean proccesOK = true;
	private boolean controlMOK = true;

	private static final String SOURCE = "BOND_EXTERNAL_RATINGS";

	protected LogGeneric logGen = new LogGeneric();

	// added (Bean)
	private BondExtRatingsBean bondExtRatBean;

	/**
	 * @return this task information, gathered from the constant
	 *         TASK_INFORMATION
	 */
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
		attributeList.add(attribute(SUMMARY_LOG));
		attributeList.add(attribute(DETAILED_LOG));
		attributeList.add(attribute(FULL_LOG));
		attributeList.add(attribute(STATIC_DATA_LOG));

		return attributeList;
	}


	/**
	 * Main method to be executed in this Scheduled task
	 * 
	 * @param connection
	 *            to DS
	 * @param connection
	 *            to PS
	 * @return result of the process
	 */
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
		this.remoteProduct = conn.getRemoteProduct();

		try {
			// if (!this.logGen.validateFilesExistence()) {
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

						final String separator = getAttribute(SEPARATOR_DOMAIN_STRING);
						ProductCreditRating productCreditRating = null;

						String line;
						String[] values = null;
						boolean stopFile = false;

						try {
							this.inputFileStream = new BufferedReader(new FileReader(filePath));
							line = this.inputFileStream.readLine();
							for (int i = 0; !stopFile && ((line = this.inputFileStream.readLine()) != null); i++) {
								this.logGen.initializeError();
								this.logGen.initializeWarning();
								this.logGen.initializeOK();
								this.logGen.initializeErrorLine();
								this.logGen.initializeWarningLine();
								this.logGen.initializeOkLine();
								boolean save = true;
								if (!line.startsWith("*****")) {
									if (CollateralUtilities.checkFields(line, '|', 4)) {

										values = CollateralUtilities.splitMejorado(5, separator, false, line);
										for (int ii = 0; ii < values.length; ii++) {
											values[ii] = values[ii].trim();
										}
										// added (Bean)
										this.bondExtRatBean = new BondExtRatingsBean(values);

										// The process for insert a rating
										// in db checks if the rating
										// that we want to load is in the db

										// If the rating is in the db and is
										// the same that ours then we don?t
										// save the new rating

										// If the rating is not in the db
										// then we save the new rating

										// The can?t save rating message
										// appears when the rating is not
										// saved or the db consult for look
										// the rating, throws an exception
										productCreditRating = getProductCreditRating(this.bondExtRatBean.getIsin(),
												this.bondExtRatBean.getAgency(), this.bondExtRatBean.getValue(),
												this.bondExtRatBean.getFromDate(), line);
										try {
											final ProductCreditRating productRatingDB = DSConnection.getDefault()
													.getRemoteMarketData().getProductRating(productCreditRating);
											if ((this.logGen.getNumberError() == 0)
													&& (this.logGen.getNumberWarning() == 0)) {
												if ((productRatingDB != null)
														&& productRatingDB.equals(productCreditRating)) {
													if ((productRatingDB.getRatingValue() != null)
															&& productRatingDB.getRatingValue()
																	.equals(productCreditRating.getRatingValue())) {
														save = false;
													} else {
														save = false;
														productRatingDB
																.setRatingValue(productCreditRating.getRatingValue());
														this.remoteMarketData.saveProductRating(productRatingDB);
													}
												}
											}

										} catch (final RemoteException e) {
											save = false;
											this.result = false;
											this.logGen.incrementError();
											this.logGen.setErrorSavingRating(SOURCE, this.file,
													String.valueOf(this.logGen.getNumberTotal()),
													this.bondExtRatBean.getIsin(), line);
											Log.error(this, e); //sonar
											this.proccesOK = false;
										}

										if ((this.logGen.getNumberError() == 0)
												&& (this.logGen.getNumberWarning() == 0)) {
											if (productCreditRating.getProductId() != 0) {
												if (save) {
													try {
														Log.info(this, "Se procesa la línea: " + line);
														this.remoteMarketData.saveProductRating(productCreditRating);
													} catch (CalypsoServiceException e) {
														this.result = false;
														Log.error(LOG_CATEGORY_SCHEDULED_TASK,
																"Error while saving ratings", e);
														this.logGen.incrementError();
														this.logGen.setErrorSavingRating(SOURCE, this.file,
																String.valueOf(this.logGen.getNumberTotal()), values[0],
																line);

														this.proccesOK = false;
													}
												}
											} else {
												this.result = false;
											}
										}

										if (this.logGen.getNumberError() > 0) {
											this.logGen.incrementRecordErrors();
										}
										if ((this.logGen.getNumberWarning() > 0)
												&& (this.logGen.getNumberError() == 0)) {
											this.logGen.incrementRecordWarning();
											this.addBadLine(line, "Required field ISIN not present or not valid.");
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
												// this.controlMOK = false;
												ControlMErrorLogger.addError(ErrorCodeEnum.LogException,
														"Unexpected error in log file");
											}
										}

									} else {
										Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error checking the number of fields.");
										this.result = false;
										this.logGen.incrementRecordErrors();
										this.logGen.incrementTotal();
										this.logGen.setErrorBadRecordFormat(SOURCE, this.file, String.valueOf(i + 1),
												"", line, this.bondExtRatBean.getIsin());

										this.logGen.feedFullLog(0);
										this.logGen.feedDetailedLog(0);
									}
								} else {
									stopFile = true;
								}
								// close line control
							} // close for
						} catch (final FileNotFoundException e) {
							Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while looking for file:" + filePath, e);
							this.logGen.incrementRecordErrors();
							this.proccesOK = false;
							this.logGen.setErrorNumberOfFiles(SOURCE, this.file);

							this.logGen.feedFullLog(0);
							this.logGen.feedDetailedLog(0);
							this.result = false;
							this.controlMOK = false;
							ControlMErrorLogger.addError(ErrorCodeEnum.InputFileNotFound, "");
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
							this.controlMOK = false;
							ControlMErrorLogger.addError(ErrorCodeEnum.IOException,
									"Unexpected error opening the file");
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
									this.controlMOK = false;
									ControlMErrorLogger.addError(ErrorCodeEnum.IOException,
											"Unexpected error closing the file");
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

			/*
			 * } else { Log.error(LOG_CATEGORY_SCHEDULED_TASK,
			 * "Error. Log files is already existing in the system.");
			 * this.logGen.incrementRecordErrors();
			 * this.logGen.setErrorCreatingLogFile(SOURCE, startFileName);
			 * 
			 * this.logGen.feedFullLog(0); this.logGen.feedDetailedLog(0);
			 * this.proccesOK = false; this.result = false;
			 * ControlMErrorLogger.addError(ErrorCodeEnum.IOException,
			 * "log file is already existing in the system"); this.controlMOK =
			 * false; }
			 */
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
					final List<String> to = conn.getRemoteReferenceData().getDomainValues(BOND_EXT_RATINGS);
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
				// ControlMErrorLogger.addError(ErrorCodeEnum.MailSending, "");
			}

		} catch (final Exception e) {
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error. Error creating log files. \n" + e); //sonar
			this.logGen.incrementRecordErrors();
			this.logGen.setErrorMovingFile(SOURCE, startFileName);
			this.logGen.feedFullLog(0);
			this.logGen.feedDetailedLog(0);
			this.proccesOK = false;
			ControlMErrorLogger.addError(ErrorCodeEnum.InputFileCanNotBeMoved, "");
			this.controlMOK = false;
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

	/**
	 * @param values
	 *            the Isin for a bond
	 * @param values2
	 *            Agency name
	 * @param values3
	 *            Rating value
	 * @param values4
	 *            Date
	 * @param rating
	 *            Rating type
	 * @return the ProductCreditRating created.
	 * @throws Exception
	 * 
	 */
	private ProductCreditRating getProductCreditRating(final String values, final String values2, String values3,
			final String values4, final String line) {
		final ProductCreditRating prCreditRating = new ProductCreditRating();

		final String ratingValueAux = values3;
		if (ratingValueAux.contains("*")) {
			if (ratingValueAux.contains(" ")) {
				final String[] ratingValues = ratingValueAux.split(" ");
				values3 = ratingValues[0];
			} else {
				final String[] ratingValues = ratingValueAux.split("/");
				values3 = ratingValues[0];
			}
		}

		final String ratingValue = values3;
		if (ratingValue.contains("E")) {
			final String[] ratingValues = ratingValue.split("E");
			values3 = ratingValues[0];
		} else if (ratingValue.contains("e")) {
			final String[] ratingValues = ratingValue.split("e");
			values3 = ratingValues[0];
		}

		final String ratingValueAux1 = values3;
		if (ratingValueAux1.contains("(P)")) {
			final String[] ratingValues = ratingValueAux1.split("\\)");
			values3 = ratingValues[1];
		}

		try {
			prCreditRating.setProductId(getIdFromDataBase(values));
		} catch (final Exception e) {
			this.logGen.incrementWarning();
			this.logGen.setWarningRequiredFieldNotPresentNotValid(SOURCE, this.file,
					String.valueOf(this.logGen.getNumberTotal()), "22", "ISIN", this.bondExtRatBean.getIsin(),
					this.bondExtRatBean.getIsin(), line);
			Log.error(this, e); //sonar
			this.result = false;
		}

		if (values2.equals("Fitch") || values2.equals("Moodys") || values2.equals("SyP")) {
			if (!values2.equals("Moodys")) {
				if (!values2.equals("SyP")) {
					prCreditRating.setAgencyName(values2);
				} else {
					prCreditRating.setAgencyName("S&P");
				}
			} else {
				prCreditRating.setAgencyName("Moody");
			}
			prCreditRating.setRatingType("Current");
			obtainRatingValues(prCreditRating.getAgencyName(), values3, prCreditRating, line);
		} else {
			this.logGen.incrementError();
			this.logGen.setErrorAgencyNotValid(SOURCE, this.file, String.valueOf(this.logGen.getNumberTotal()),
					this.bondExtRatBean.getIsin(), line);

			this.result = false;
		}

		// We convert the String date to JDate.
		try {
			if (!values4.equals("")) {
				synchronized (dFormat) {
					dFormat.setLenient(false);
					dFormat.parse(values4);
				}
				final int day = Integer.parseInt(values4.substring(6));
				final int month = Integer.parseInt(values4.substring(4, 6));
				final int year = Integer.parseInt(values4.substring(0, 4));
				prCreditRating.setAsOfDate(JDate.valueOf(year, month, day));
			} else {
				this.logGen.incrementError();

				this.logGen.setErrorDateNotValid(SOURCE, this.file, String.valueOf(this.logGen.getNumberTotal()),
						this.bondExtRatBean.getIsin(), line);

				this.result = false;
			}
		} catch (final Exception e) {
			this.logGen.incrementError();
			this.logGen.setErrorDateNotValid(SOURCE, this.file, String.valueOf(this.logGen.getNumberTotal()),
					this.bondExtRatBean.getIsin(), line);
			Log.error(this, e); //sonar
			this.result = false;
		}

		return prCreditRating;
	}

	@SuppressWarnings("unchecked")
	private void obtainRatingValues(final String values2, final String values3,
			final ProductCreditRating prCreditRating, String line) {
		RatingValues ratValues = null;
		try {
			ratValues = DSConnection.getDefault().getRemoteReferenceData().getRatingValues();
			final Vector<String> rv = ratValues.getRatingValues(values2, "Current");

			if (rv.contains(values3)) {

				prCreditRating.setRatingValue(values3);

			} else {
				this.logGen.incrementError();
				this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.file,
						String.valueOf(this.logGen.getNumberTotal()), "45", "RATING", this.bondExtRatBean.getIsin(),
						line);
				this.result = false;
			}
		} catch (final RemoteException e) {
			this.logGen.incrementError();
			this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.file,
					String.valueOf(this.logGen.getNumberTotal()), "45", "RATING", this.bondExtRatBean.getIsin(), line);
			Log.error(this, e); //sonar
			this.result = false;
		}

	}

	/**
	 * 
	 * @param isin
	 *            that indicates the bond?s isin.
	 * @return the id for a isin.
	 * @throws RemoteException
	 */
	private int getIdFromDataBase(final String isin) throws Exception {
		@SuppressWarnings("rawtypes")
		Vector products = this.remoteProduct.getProductsByCode(ISIN, isin);

		if (products != null) {
			for (int i = 0; i < products.size(); i++) {
				Product product = (Product) products.get(i);
				if (product instanceof Bond) {
					return product.getId();
				}
			}
		}

		// Throw an exception if the isin is not found
		throw new Exception("Product Bond not found with isin=" + isin);

	}

	@Override
	public String getFileName() {
		return this.file;
	}

}
