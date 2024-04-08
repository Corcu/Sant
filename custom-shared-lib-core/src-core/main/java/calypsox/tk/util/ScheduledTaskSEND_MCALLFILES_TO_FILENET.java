package calypsox.tk.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import calypsox.tk.bo.FileNetHelper;

/**
 * 
 * @author x957355
 *
 */
public class ScheduledTaskSEND_MCALLFILES_TO_FILENET extends ScheduledTask {

	private static final String BUSINESS_PROCESS = "BusinessProcess";
	private static final String CONFIDENCIALITY = "ConfidentialityLevel";
	private static final String DOCUMENT_OWNER = "DocumentOwner";
	private static final String DOCUMENT_TYPE = "DocumentType";
	private static final String GDPR = "GDPR";
	private static final String GEO_OWNER_DOCUMENT = "Geographyownerdocument";
	private static final String CLIENT_NAME = "ClientName";
	private static final String REPORT_DATE = "ReportDate";
	private static final String DOC_CLASS = "docClass";
	private static final String STORAGE_PATH = "storagePath";
	private static final String DIRECTORY = "InputDir";
	private static final String FILE = "File";
	private static final String MIME_TYPE = "Mime Type";
	private static final String EXT = "File extension";
	private static final String TIMESTAMP_FILENAME = "TIMESTAMP FILENAME";
	private static final String TIMESTAMP_FORMAT = "TIMESTAMP FORMAT";

	@Override
	public String getTaskInformation() {
		return "Send 20SF or 21SF Filenet DocumentClass file to Filenet. The file will be provided "
				+ "in ST attributes.";
	}

	@Override
	protected boolean process(DSConnection ds, PSConnection ps) {
		String path = this.getAttribute(DIRECTORY);
		String file = this.getAttribute(FILE);
		String mimeType = this.getAttribute(MIME_TYPE);
		String docClass = this.getAttribute(DOC_CLASS);
		String storagePath = this.getAttribute(STORAGE_PATH);
		boolean control = false;

		file = getRealFilename(this.getValuationDatetime(), file);

		try {
			FileInputStream fileInput = new FileInputStream(path + file);
			byte[] fileContent = new byte[(int) fileInput.getChannel().size()];
			fileInput.read(fileContent);
			fileInput.close();
			FileNetHelper helper = FileNetHelper.getInstance();
			control = helper.callInsertDocumentService("FileNet_MC_NOTIFICATION", file, mimeType, fileContent, docClass,
					getMetadata(), storagePath);

			if (control) {
				Log.system(this.getClass().getSimpleName(), "Filenet Document GN_ID: " + helper.getDocumentId());
			}

		} catch (FileNotFoundException e) {
			Log.error(this, "Can not read the file: " + DIRECTORY + FILE, e);
			return false;
		} catch (IOException e) {
			Log.error(this, "Error reading the file", e);

		} catch (Exception e) {
			Log.error(this, "Error sending the file to FILENET", e);
		}
		return control;
	}

	@Override
	public Vector<String> getDomainAttributes() {
		Vector<String> v = new Vector<String>();

		v.addElement(DIRECTORY);
		v.addElement(FILE);
		v.addElement(EXT);
		v.addElement(TIMESTAMP_FILENAME);
		v.addElement(TIMESTAMP_FORMAT);
		v.addElement(BUSINESS_PROCESS);
		v.addElement(GEO_OWNER_DOCUMENT);
		v.addElement(CONFIDENCIALITY);
		v.addElement(DOCUMENT_OWNER);
		v.addElement(DOCUMENT_TYPE);
		v.addElement(GDPR);
		v.addElement(CLIENT_NAME);
		v.addElement(STORAGE_PATH);
		v.addElement(DOC_CLASS);
		v.addElement(MIME_TYPE);

		return v;
	}

	@Override
	protected List<AttributeDefinition> buildAttributeDefinition() {
		List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
		attributeList.addAll(super.buildAttributeDefinition());
		attributeList.add(attribute(DIRECTORY).description("Directory containing the file").mandatory());
		attributeList.add(attribute(FILE).description("File to send.").mandatory());
		attributeList.add(attribute(EXT).description("File extension.").mandatory());
		attributeList.add(attribute(TIMESTAMP_FILENAME).description("Search file by timestamp.").mandatory());
		attributeList.add(attribute(FILE).description("Timestamp format."));

		attributeList
				.add(attribute(BUSINESS_PROCESS).description("Value of BusinessProcess metadata field").mandatory());
		attributeList.add(
				attribute(CONFIDENCIALITY).description("Value of ConfidentialityLevel metadata field").mandatory());
		attributeList.add(attribute(DOCUMENT_OWNER).description("Value of DocumentOwner metadata field").mandatory());
		attributeList.add(attribute(DOCUMENT_TYPE).description("Value of DocumentType metadata field").mandatory());
		attributeList.add(attribute(GDPR).description("Value of GDPR metadata field").mandatory());
		attributeList.add(attribute(GEO_OWNER_DOCUMENT).description("Value of Geographyownerdocument metadata field")
				.mandatory());
		attributeList.add(attribute(CLIENT_NAME).description("Value of ClientName metadata field").mandatory());
		attributeList.add(attribute(STORAGE_PATH).description("Path where save the file in Filenet.").mandatory());
		attributeList.add(attribute(DOC_CLASS).description("Value of docClass body field").mandatory());
		attributeList.add(attribute(MIME_TYPE).description("File Mime Type").mandatory());

		return attributeList;
	}

	@Override
	public Vector<String> getAttributeDomain(String attribute, Hashtable currentAttr) {
		Vector<String> result = null;
		if (TIMESTAMP_FILENAME.equals(attribute)) {
			result = new Vector<String>();
			result.add("true");
			result.add("false");
		}

		return result;
	}

	/**
	 * Get the metadata content
	 * @return Map whit metadata
	 */
	private HashMap<String, Object> getMetadata() {
		HashMap<String, Object> metadata = new HashMap<>();

		// Set the string fields
		metadata.put(BUSINESS_PROCESS, this.getAttribute(BUSINESS_PROCESS));
		metadata.put(CONFIDENCIALITY, this.getAttribute(CONFIDENCIALITY));
		metadata.put(GEO_OWNER_DOCUMENT, this.getAttribute(GEO_OWNER_DOCUMENT));
		metadata.put(DOCUMENT_TYPE, this.getAttribute(DOCUMENT_TYPE));
		metadata.put(GDPR, this.getAttribute(GDPR));

		// Set the array values

		metadata.put(DOCUMENT_OWNER, new String[] { (this.getAttribute(DOCUMENT_OWNER)) });
		metadata.put(CLIENT_NAME, new String[] { (this.getAttribute(CLIENT_NAME)) });

		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		metadata.put(REPORT_DATE, df.format(this.getValuationDatetime()));
		return metadata;
	}

	/**
	 * Get the file name
	 * @param valDatetime valuation date time
	 * @param _fileName file name whitout extension and timestamp
	 * @return file name
	 */
	private String getRealFilename(JDatetime valDatetime, String _fileName) {
		String fileName = _fileName == null ? this.getAttribute("FILE") : _fileName;
		if (valDatetime != null) {
			String timeStamp = this.getAttribute("TIMESTAMP FILENAME");
			String timeFormat = this.getAttribute("TIMESTAMP FORMAT");
			String ext = this.getAttribute(EXT);
			if (!Util.isEmpty(timeStamp) && timeStamp.equals("true")) {

				String date = null;
				if (!Util.isEmpty(timeFormat)) {
					date = Util.datetimeToString(valDatetime, timeFormat,
							this.getTimeZone() == null ? Calendar.getInstance().getTimeZone() : this.getTimeZone());
				} else {
					date = Util.dateToString(JDate.valueOf(valDatetime,
							this.getTimeZone() == null ? Calendar.getInstance().getTimeZone() : this.getTimeZone()));
					date = date.replace('/', '-');
				}
				fileName = fileName + date;
				fileName = fileName.concat(".").concat(ext);
			}
		}

		return fileName;
	}

}
