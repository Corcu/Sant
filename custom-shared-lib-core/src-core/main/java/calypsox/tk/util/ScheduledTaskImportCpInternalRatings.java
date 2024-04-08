package calypsox.tk.util;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.bean.CpInternalRatingsBean;
import calypsox.tk.util.log.LogGeneric;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.FileUtility;
import com.calypso.tk.core.Defaults;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.CreditRating;
import com.calypso.tk.refdata.RatingValues;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteMarketData;
import com.calypso.tk.service.RemoteReferenceData;
import com.calypso.tk.util.email.MailException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

public class ScheduledTaskImportCpInternalRatings extends AbstractProcessFeedScheduledTask {
    private static final long serialVersionUID = 5558892972788536029L;

    private static final String SENIOR_UNSECURED = "SENIOR_UNSECURED";
    private static final String CURRENT = "Current";
    private static final String SEPARATOR_DOMAIN_STRING = "Separator";
    private static final String FILEPATH = "File Path";
    private static final String STARTFILENAME = "Start of File Name";
    private static final String TASK_INFORMATION = "Import Internal Ratings for Counterparty from a CSV file.";
    private boolean result = false;
    private String file = "";

    private RemoteMarketData remoteMarketData;
    private RemoteReferenceData remoteReferenceData;
    private BufferedReader inputFileStream;
    private static SimpleDateFormat timeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

    private static final String CP_INT_RATINGS = "CP_INT_RATINGS";
    private static final String SUBJECT = "Log files internal Ratings for counterparties on " + Defaults.getEnvName();
    private static final String DEFAULT_FROM_EMAIL = "calypso@gruposantander.com";

    protected static final String PROCESS = "Load of Internal Ratings for counterparties";
    protected static final String SUMMARY_LOG = "Summary Log";
    protected static final String DETAILED_LOG = "Detailed Log";
    protected static final String FULL_LOG = "Full Log";
    protected static final String STATIC_DATA_LOG = "Static Data Log";
    private static final String SYSTEM = "SUSI";

    // added (Bean)
    private CpInternalRatingsBean cpIntRatBean;

    private boolean proccesOK = true;
    private boolean controlMOK = true;

    private static final String SOURCE = "CP_INT_RATINGS";

    protected LogGeneric logGen = new LogGeneric();

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
        this.remoteReferenceData = conn.getRemoteReferenceData();

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

                        // Just after file verifications, this method will make a copy into the
                        // ./import/copy/ directory
                        FileUtility.copyFileToDirectory(filePath, path + "/copy/");

                        final String separator = getAttribute(SEPARATOR_DOMAIN_STRING);

                        String line;
                        String[] values = null;
                        boolean stopFile = false;

                        try {
                            this.inputFileStream = new BufferedReader(new FileReader(filePath));

                            for (int i = 0; !stopFile && ((line = this.inputFileStream.readLine()) != null); i++) {
                                this.logGen.initializeError();
                                this.logGen.initializeWarning();
                                this.logGen.initializeOK();
                                this.logGen.initializeErrorLine();
                                this.logGen.initializeWarningLine();
                                this.logGen.initializeOkLine();
                                boolean save = true;
                                final Vector<CreditRating> creditRatingVectorAux = new Vector<CreditRating>();
                                if (!line.startsWith("*****")) {
                                    try {
                                        if (CollateralUtilities.checkFields(line, '|', 4)) {

                                            values = CollateralUtilities.splitMejorado(5, separator, false, line);
                                            for (int ii = 0; ii < values.length; ii++) {
                                                values[ii] = values[ii].trim();
                                            }

                                            // added (Bean)
                                            this.cpIntRatBean = new CpInternalRatingsBean(values);

                                            // The process for insert a
                                            // rating
                                            // in db checks if the rating
                                            // that we want to load is in
                                            // the db

                                            // If the rating is in the db
                                            // and is
                                            // the same that ours then we
                                            // don?t
                                            // save the new rating

                                            // If the rating is not in the
                                            // db
                                            // then we save the new rating

                                            // The can?t save rating message
                                            // appears when the rating is
                                            // not
                                            // saved or the db consult for
                                            // look
                                            // the rating, throws an
                                            // exception
                                            final CreditRating creditRating = getCreditRating(this.cpIntRatBean, line);
                                            try {
                                                final Vector<CreditRating> creditRatingVectorAux1 = new Vector<CreditRating>();
                                                final CreditRating cratingDB = DSConnection.getDefault()
                                                        .getRemoteMarketData().getRating(creditRating);
                                                if ((cratingDB != null) && cratingDB.equals(creditRating)) {
                                                    if (cratingDB.getRatingValue()
                                                            .equals(creditRating.getRatingValue())) {
                                                        save = false;
                                                    } else {
                                                        save = false;
                                                        cratingDB.setRatingValue(creditRating.getRatingValue());
                                                        creditRatingVectorAux1.add(cratingDB);
                                                        this.remoteMarketData.saveRatings(creditRatingVectorAux1);
                                                    }
                                                }
                                            } catch (final RemoteException re) {
                                                save = false;
                                                this.result = false;
                                                this.logGen.incrementError();
                                                this.logGen.setErrorSavingRating(SOURCE, this.file,
                                                        String.valueOf(this.logGen.getNumberTotal()),
                                                        this.cpIntRatBean.getLegalEntity(), line);
                                                Log.error(this, re); //sonar
                                                this.proccesOK = false;
                                            }
                                            if ((this.logGen.getNumberError() == 0)
                                                    && (this.logGen.getNumberWarning() == 0)) {
                                                if (creditRating.getLegalEntityId() != 0) {
                                                    if (creditRating.getRatingValue() != null) {
                                                        creditRatingVectorAux.addElement(creditRating);
                                                        if (save) {
                                                            this.remoteMarketData.saveRatings(creditRatingVectorAux);
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
                                                this.addBadLine(line, "Required legal entity not present or not valid.");
                                            }
                                            if ((this.logGen.getNumberError() == 0)
                                                    && (this.logGen.getNumberWarning() == 0)) {
                                                this.logGen.incrementOK();
                                            }
                                            this.logGen.incrementTotal();
                                            if (this.logGen.getNumberOk() == 1) {
                                                this.logGen.setOkLine(SOURCE, this.file, i + 1, "0");

                                            }
                                            this.logGen.feedFullLog(0);
                                            this.logGen.feedDetailedLog(0);
                                            if ((this.logGen.getNumberWarning() > 0)
                                                    && (this.logGen.getNumberError() == 0)) {
                                                try {
                                                    this.logGen.feedStaticDataLog(
                                                            String.valueOf(this.logGen.getNumberTotal()), SYSTEM);
                                                } catch (final Exception e) {
                                                    Log.error(LOG_CATEGORY_SCHEDULED_TASK,
                                                            "Error. Error writing in log files.");
                                                    Log.error(this, e); //sonar
                                                    this.logGen.setErrorWritingLog(SOURCE, this.file,
                                                            String.valueOf(i + 1));

                                                    this.logGen.feedFullLog(0);
                                                    this.logGen.feedDetailedLog(0);
                                                    this.result = false;
                                                    ControlMErrorLogger.addError(ErrorCodeEnum.LogException,
                                                            "Unexpected error in log file"); // TODO LOG IO_ERROR
                                                    // this.controlMOK = false;
                                                }
                                            }
                                        } else {
                                            Log.error(LOG_CATEGORY_SCHEDULED_TASK,
                                                    "Error checking the number of fields.");
                                            this.result = false;
                                            this.logGen.incrementRecordErrors();
                                            this.logGen.incrementTotal();
                                            this.logGen.setErrorBadRecordFormat(SOURCE, this.file,
                                                    String.valueOf(i + 1), "", line, values[0]);

                                            this.logGen.feedFullLog(0);
                                            this.logGen.feedDetailedLog(0);

                                        }
                                    } catch (final RemoteException e) {
                                        this.result = false;
                                        Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error while saving ratings", e);
                                        this.logGen.incrementRecordErrors();
                                        this.logGen.setErrorSavingRating(SOURCE, this.file,
                                                String.valueOf(this.logGen.getNumberTotal()),
                                                this.cpIntRatBean.getLegalEntity(), line);

                                        this.logGen.feedFullLog(0);
                                        this.logGen.feedDetailedLog(0);
                                        this.proccesOK = false;
                                        this.logGen.incrementTotal();
                                        line = null;
                                        // ControlMErrorLogger
                                        // .addError(
                                        // ErrorCodeEnum.InvalidData,
                                        // "Unexpected error saving rating");
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
                            ControlMErrorLogger
                                    .addError(ErrorCodeEnum.IOException, "Unexpected error opening the file");
                            this.controlMOK = false;
                        } finally {
                            if (this.inputFileStream != null) {
                                try {
                                    this.inputFileStream.close();
                                } catch (final IOException e) {
                                    Log.error(LOG_CATEGORY_SCHEDULED_TASK,
                                            "Error while trying close input stream for the CSV file <" + getFileName()
                                                    + "> open previously", e);
                                    this.result = false;
                                    ControlMErrorLogger.addError(ErrorCodeEnum.IOException,
                                            "Unexpected error closing the file");
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

                    this.logGen.feedFullLog(0);
                    this.logGen.feedDetailedLog(0);
                    this.proccesOK = false;
                    this.result = false;
                    Log.error(this, e); //sonar
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
                ControlMErrorLogger
                        .addError(ErrorCodeEnum.InputFileNotFound,
                                "The number of matches for the filename in the path specified is 0 or greater than 1. Please fix the problem");
                this.controlMOK = false;
            }

            /*
             * } else { Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error. Log files is already existing in the system.");
             * this.logGen.incrementRecordErrors(); this.logGen.setErrorCreatingLogFile(SOURCE, startFileName);
             *
             * this.logGen.feedFullLog(0); this.logGen.feedDetailedLog(0); this.proccesOK = false; this.result = false;
             * ControlMErrorLogger.addError(ErrorCodeEnum.IOException, "log file is already existing in the system"); //
             * TODO // LOG // IO_ERROR this.controlMOK = false; }
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
            ControlMErrorLogger.addError(ErrorCodeEnum.LogException, "Unexpected error creating log file"); // TODO LOG
            // IO_ERROR
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
                    final List<String> to = conn.getRemoteReferenceData().getDomainValues(CP_INT_RATINGS);
                    final ArrayList<String> attachments = new ArrayList<String>();
                    attachments.add(sumLog);
                    attachments.add(this.logGen.getStringDetailedLog());
                    attachments.add(this.logGen.getStringFullLog());
                    attachments.add(this.logGen.getStringStaticDataLog());
                    CollateralUtilities.sendEmail(to, SUBJECT, "", DEFAULT_FROM_EMAIL, attachments);
                }
            } catch (final MailException me) {
                Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error. Error sending log mail.");
                Log.error(this, me); //sonar
                this.logGen.incrementRecordErrors();
                this.logGen.setErrorSentEmail(SOURCE, startFileName);

                this.logGen.feedFullLog(0);
                this.logGen.feedDetailedLog(0);
                this.proccesOK = false;
                this.result = false;
                // ControlMErrorLogger.addError(ErrorCodeEnum.MailSending, "");
            }

        } catch (final Exception e) {
            Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error. Error creating log files.");
            Log.error(this, e); //sonar
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
     * Method for obtain a object type CreditRating
     *
     * @param cpIntRatBean2 Bean with the values read
     * @return the CreditRating created.
     * @throws Exception MODIFIED (Bean)
     */
    private CreditRating getCreditRating(final CpInternalRatingsBean cpIntRatBean2, final String line) throws Exception {

        final CreditRating credRating = new CreditRating();
        int id = 0;

        if (this.cpIntRatBean.getNElem() > 3) {
            try {
                id = getLegalEntityId(this.cpIntRatBean.getLegalEntity());
                credRating.setLegalEntityId(id);
            } catch (final Exception e) {
                this.logGen.incrementWarning();
                this.logGen.setWarningRequiredFieldNotPresentNotValid(SOURCE, this.file,
                        String.valueOf(this.logGen.getNumberTotal()), "43", "LEGAL_ENTITY",
                        this.cpIntRatBean.getLegalEntity(), this.cpIntRatBean.getLegalEntity(), line);
                Log.error(this, e); //sonar
                this.result = false;
            }

            if (this.cpIntRatBean.getAgency().equals("SC")) {
                credRating.setAgencyName(this.cpIntRatBean.getAgency());
            } else {
                this.logGen.incrementError();
                this.logGen.setErrorAgencyNotValid(SOURCE, this.file, String.valueOf(this.logGen.getNumberTotal()),
                        this.cpIntRatBean.getLegalEntity(), line);
                this.result = false;
            }

            if (cpIntRatBean2.getRatingType().equals("CURRENT")) {
                credRating.setRatingType(CURRENT);
            } else {
                this.logGen.incrementError();
                this.logGen.setErrorRatingTypeNotValid(SOURCE, this.file, String.valueOf(this.logGen.getNumberTotal()),
                        "42", "RATING_TYPE", this.cpIntRatBean.getLegalEntity(), line);

                this.result = false;
            }
            credRating.setDebtSeniority(SENIOR_UNSECURED);
            obtainRatingValues(this.cpIntRatBean.getValue(), cpIntRatBean2.getRatingType(), credRating, line);

            try {
                // We convert the String date to JDate.
                synchronized (dateFormat) {
                    dateFormat.setLenient(false);
                    dateFormat.parse(this.cpIntRatBean.getFromDate());
                }
                final int day = Integer.parseInt(this.cpIntRatBean.getFromDate().substring(0, 2));
                final int month = Integer.parseInt(this.cpIntRatBean.getFromDate().substring(3, 5));
                final int year = Integer.parseInt(this.cpIntRatBean.getFromDate().substring(6));
                credRating.setAsOfDate(JDate.valueOf(year, month, day));
            } catch (final Exception e) {
                Log.error(this, e);//sonar
                this.logGen.incrementError();
                this.logGen.setErrorDateNotValid(SOURCE, this.file, String.valueOf(this.logGen.getNumberTotal()),
                        this.cpIntRatBean.getLegalEntity(), line);
                this.result = false;
            }

        } else {
            try {
                id = getLegalEntityId(this.cpIntRatBean.getLegalEntity());
                credRating.setLegalEntityId(id);
            } catch (final Exception e) {
                Log.error(this, e); //sonar
                this.logGen.incrementWarning();
                this.logGen.setWarningRequiredFieldNotPresentNotValid(SOURCE, this.file,
                        String.valueOf(this.logGen.getNumberTotal()), "43", "LEGAL_ENTITY",
                        this.cpIntRatBean.getLegalEntity(), this.cpIntRatBean.getLegalEntity(), line);

                this.result = false;
            }
            credRating.setAgencyName(this.cpIntRatBean.getAgency());
            credRating.setRatingType(CURRENT);
            credRating.setDebtSeniority(SENIOR_UNSECURED);
        }

        return credRating;
    }

    @SuppressWarnings("unchecked")
    private void obtainRatingValues(final String value, final String ratingType, final CreditRating credRating,
                                    final String line) {
        RatingValues ratValues = null;
        try {
            ratValues = DSConnection.getDefault().getRemoteReferenceData().getRatingValues();
            final Vector<String> rv = ratValues.getRatingValues(this.cpIntRatBean.getAgency(), CURRENT);

            if (rv.contains(value)) {
                credRating.setRatingValue(value);
            } else {
                this.logGen.incrementError();
                this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.file,
                        String.valueOf(this.logGen.getNumberTotal()), "45", "RATING",
                        this.cpIntRatBean.getLegalEntity(), line);
                this.result = false;
            }
        } catch (final RemoteException e) {
            Log.error(this, e); //sonar
            this.logGen.incrementError();
            this.logGen.setErrorRequiredFieldNotPresentNotValid(SOURCE, this.file,
                    String.valueOf(this.logGen.getNumberTotal()), "45", "RATING", this.cpIntRatBean.getLegalEntity(),
                    line);
            this.result = false;
        }

    }

    /**
     * Method for obtain the Id for a Legal Entity given its shortName
     *
     * @param nameLE The short name for a Legal Entity
     * @return the id for the Legal Entity
     * @throws Exception
     */
    @SuppressWarnings({"unchecked", "unused"})
    private int getLegalEntityId(final String nameLE) throws Exception {
        final String where = "SHORT_NAME=" + "'" + nameLE + "'";
        final Vector<LegalEntity> legalEntities = this.remoteReferenceData.getAllLE(where, null);
        final Iterator<LegalEntity> vectIterator = legalEntities.iterator();

        // We save all the CreditRatings into the database
        for (int i = 0; vectIterator.hasNext(); i++) {
            return legalEntities.get(i).getId();
        }

        throw new Exception("Legal Entity " + nameLE + " doesn't exist");
    }

    @Override
    public String getFileName() {
        return this.file;
    }

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
        attributeList.add(attribute(STATIC_DATA_LOG));
        attributeList.add(attribute(FULL_LOG));

        return attributeList;
    }

//	@SuppressWarnings("unchecked")
//	@Override
//	public Vector<String> getDomainAttributes() {
//		final Vector<String> attr = super.getDomainAttributes();
//		attr.add(SEPARATOR_DOMAIN_STRING);
//		attr.add(SUMMARY_LOG);
//		attr.add(DETAILED_LOG);
//		attr.add(STATIC_DATA_LOG);
//		attr.add(FULL_LOG);
//		return attr;
//	}

}
