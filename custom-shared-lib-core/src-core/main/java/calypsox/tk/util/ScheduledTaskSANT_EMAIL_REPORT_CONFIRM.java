 package calypsox.tk.util;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.calypso.tk.collateral.service.CollateralServiceRegistry;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.interfaceImporter.ImportContext;
import calypsox.util.collateral.CollateralUtilities;

/**
 * Scheduled task to send an email to confirm that a report is generated.
 * 
 * @author gsaiz
 * 
 */
public class ScheduledTaskSANT_EMAIL_REPORT_CONFIRM extends ScheduledTask {

	private static final long serialVersionUID = 123L;

    protected static final String TASK_INFORMATION = "Scheduled task to send an email to confirm that a report is generated.";
    protected static final String DESTINATION_EMAIL = "Destination email";
    protected static final String FROM_EMAIL = "From email";
    protected static final String REPORT_FILENAME = "REPORT FILENAME";
    protected static final String REPORT_FORMAT = "REPORT FORMAT";
    protected static final String REPORT_TIMESTAMP = "REPORT TIMESTAMP";
    protected static final String REPORT_TIMESTAMP_FORMAT = "REPORT TIMESTAMP FORMAT";
    private static final String SUBJECT = "Report confirmation";

    protected final ImportContext context = new ImportContext();

    protected CollateralServiceRegistry collateralServiceRegistry = ServiceRegistry.getDefault();

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {

	List<String> domainList = new ArrayList<String>();
	domainList.add("html");
	domainList.add("pdf");
	domainList.add("csv");
	domainList.add("txt");
	domainList.add("dat");
	List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
	attributeList.add(attribute(REPORT_FILENAME));
	attributeList.add(attribute(REPORT_FORMAT).domain(domainList));
	attributeList.add(attribute(REPORT_TIMESTAMP).booleanType());
	attributeList.add(attribute(REPORT_TIMESTAMP_FORMAT));
	attributeList.add(attribute(DESTINATION_EMAIL));
	attributeList.add(attribute(FROM_EMAIL));
	return attributeList;
    }

    // @Override
    // public Vector<String> getDomainAttributes() {
    //
    // Vector<String> vectorAttr = new Vector<String>();
    //
    // vectorAttr.add(REPORT_FILENAME);
    // vectorAttr.add(REPORT_FORMAT);
    // vectorAttr.add(REPORT_TIMESTAMP);
    // vectorAttr.add(REPORT_TIMESTAMP_FORMAT);
    // vectorAttr.add(DESTINATION_EMAIL);
    // vectorAttr.add(FROM_EMAIL);
    //
    // return vectorAttr;
    //
    // }
    //
    // /**
    // * @param attribute
    // * name
    // * @param hastable
    // * with the attributes declared
    // * @return a vector with the values for the attribute name
    // */
    // @SuppressWarnings({ "rawtypes" })
    // @Override
    // public Vector getAttributeDomain(String attribute, Hashtable hashtable) {
    //
    // Vector<String> vector = new Vector<String>();
    //
    // if (attribute.equals(REPORT_FORMAT)) {
    //
    // vector.add("html");
    // vector.add("pdf");
    // vector.add("csv");
    // vector.add("txt");
    // vector.add("dat");
    //
    // } else if (attribute.equals(REPORT_TIMESTAMP)) {
    //
    // vector.add("true");
    // vector.add("false");
    //
    // }
    // return vector;
    // }

    @Override
    public String getTaskInformation() {
	return TASK_INFORMATION;
    }

    /**
     * Main method of the ST
     */
    @Override
    public boolean process(DSConnection dsCon, PSConnection connPS) {

	boolean proccesOK = true;

	ArrayList<String> files = initFiles();

	proccesOK = areFilesGenerated(files);

	try {

	    List<String> to = getEmails();

	    String from = getAttribute(FROM_EMAIL);
	    try {
		if (!Util.isEmpty(to) && proccesOK) {

		    String body = getTextBody(files);

		    CollateralUtilities.sendEmail(to, SUBJECT, body, from, files);

		}
	    } catch (Exception e) {

		Log.error(LOG_CATEGORY_SCHEDULED_TASK, e);
	    }
	} catch (Exception e) {

	    ControlMErrorLogger.addError(ErrorCodeEnum.UndefinedException,
		    new String[] { "Unexpected error while importing trades", e.getMessage() });
	    Log.error(LOG_CATEGORY_SCHEDULED_TASK, e);

	    proccesOK = false;
	}

	return proccesOK;
    }

    /**
     * Get body of the email
     * 
     * @param files
     * @return
     */
    private String getTextBody(ArrayList<String> files) {
	StringBuffer body = new StringBuffer();
	String[] names = files.get(0).split("/");
	String name = names[names.length - 1];
	body.append("<br>The generation of the file [" + name + "] is completed. ");
	body.append("The calculation result file is attached. ");
	body.append("<br><br>Regards, ");
	body.append("<br>OTC Derivatives Collateral Management.");
	return body.toString();
    }

    /**
     * Checks whether the files exist or not.
     * 
     * @param files
     * @return
     */
    private boolean areFilesGenerated(ArrayList<String> files) {
	if (!files.isEmpty()) {
	    for (String namefile : files) {
		try {
		    File f = new File(namefile);
		} catch (Exception e) {
			Log.error(this, e); //sonar
		    return false;
		}

	    }
	} else {
	    return false;
	}

	return true;
    }

    /**
     * Get email destination from attribute
     * 
     * @return
     */
    private List<String> getEmails() {

	List<String> to = null;

	if (Util.isEmpty(to)) {
	    to = new ArrayList<String>();
	}

	String emails = getAttribute(DESTINATION_EMAIL);
	if (!Util.isEmpty(emails)) {
	    to.addAll(Arrays.asList(emails.split(";")));
	}
	return to;
    }

    /**
     * Init file names.
     * 
     * @return
     */
    private ArrayList<String> initFiles() {

	ArrayList<String> files = new ArrayList<String>();

	String fileFolder = getAttribute(REPORT_FILENAME);
	String fileFormat = "." + getAttribute(REPORT_FORMAT);

	String file = getRealFilename(getValuationDatetime(), fileFolder, fileFormat);

	files.add(file);

	return files;
    }

    /**
     * 
     * @param valDatetime
     * @param fileFolder
     * @param fileFormat
     * @return
     */
    private String getRealFilename(JDatetime valDatetime, String fileFolder, String fileFormat) {

	String fileName = null;
	if (!Util.isEmpty(fileFolder)) {
	    fileName = fileFolder;
	    if (getValuationDatetime(false) != null) {

		String timeStamp = getAttribute(REPORT_TIMESTAMP);
		String timeFormat = getAttribute(REPORT_TIMESTAMP_FORMAT);

		if (!Util.isEmpty(timeStamp) && timeStamp.equals("true")) {
		    String date = null;
		    if (!Util.isEmpty(timeFormat)) {
			date = Util.datetimeToString(getValuationDatetime(false), timeFormat);
		    } else {
			date = Util.dateToString(getValuationDatetime(false));
			date = date.replace('/', '-');
		    }
		    fileName += date;

		}
	    }
	}
	return fileName += fileFormat;
    }

}
