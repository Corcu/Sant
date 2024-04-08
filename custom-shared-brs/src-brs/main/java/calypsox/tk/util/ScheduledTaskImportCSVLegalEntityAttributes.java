package calypsox.tk.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSEventScheduledTask;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

public class ScheduledTaskImportCSVLegalEntityAttributes extends ScheduledTask {

	public static final String CSV_DELIMITER_ATTR = "Csv Delimiter";
	public static final String FILEPATH = "File Path";
	public static final String FILENAME = "File Name";
	
	public static final String LOG_CATEGORY = "ScheduledTaskImportCSVLegalEntityAttributes";

	@Override
	public String getTaskInformation() {
		return "Import Legal Entity Attributes from CSV";
	}

	protected String getDelimiter() {
		String delimiter = this.getAttribute(CSV_DELIMITER_ATTR);
		if (delimiter == null)
			return ",";

		return delimiter;
	}

	protected boolean process(DSConnection ds, PSConnection ps) {
		if (Log.isCategoryLogged("ScheduledTask")) {
			Log.debug("ScheduledTask", "Calling Execute ON " + this + " PublishB: " + this._publishB);
		}
		boolean ret = true;
		if (this._publishB) {
			try {
				PSEventScheduledTask ev = new PSEventScheduledTask();
				ev.setScheduledTask(this);
				ps.publish(ev);
				ret = true;

			} catch (Exception var5) {
				Log.error("ScheduledTask", var5);
				ret = false;
			}
		}

		if (this._executeB) {
			importLegalEntityAttributes();
		}
		return ret;
	}

	protected boolean importLegalEntityAttributes() {

		return processFile(this.getAttribute(FILEPATH) + "/" + this.getAttribute(FILENAME));

	}

	@Override
	public Vector<String> getDomainAttributes() {
		final Vector<String> attr = new Vector<String>();
		attr.add(CSV_DELIMITER_ATTR);
		attr.add(FILEPATH);
		attr.add(FILENAME);
		return attr;
	}

	protected boolean processFile(String file) {

		HashMap<String, List<LegalEntityAttribute>> incomingAttributes = new HashMap<String, List<LegalEntityAttribute>>();

		BufferedReader reader = null;
		try {

			reader = new BufferedReader(new FileReader(file));
			String line = null;
			int lineNumber = -1;
			while ((line = reader.readLine()) != null) {

				lineNumber++;
				Log.debug(LOG_CATEGORY, "Processing line " + lineNumber);

				String[] fields = line.split(getDelimiter());

				if (fields.length < 3) {
					Log.error(LOG_CATEGORY, "Incorrect line format : " + line);
				} else {

					String leShortName = fields[0];
					String attrType = fields[1];
					String attrValue = fields[2];

					LegalEntityAttribute leAttr = new LegalEntityAttribute();
					leAttr.setAttributeType(attrType);
					leAttr.setAttributeValue(attrValue);

					List<LegalEntityAttribute> leAttributes = incomingAttributes.get(leShortName);
					if (leAttributes == null) {
						leAttributes = new ArrayList<LegalEntityAttribute>();
						incomingAttributes.put(leShortName, leAttributes);
					}

					leAttributes.add(leAttr);
				}

			}

		} catch (Exception exc) {
			Log.error(LOG_CATEGORY, exc);
			return false;
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					Log.error(this, e); // sonar
				}
			}
		}

		for (Map.Entry<String, List<LegalEntityAttribute>> entry : incomingAttributes.entrySet()) {
			String leShortName = entry.getKey();
			LegalEntity le = BOCache.getLegalEntity(this.getDSConnection(), leShortName);
			if (le != null) {

				for (LegalEntityAttribute leAttr : entry.getValue()) {
					leAttr.setLegalEntityId(le.getLegalEntityId());
					leAttr.setLegalEntityRole(LegalEntityAttribute.ALL);
					if (!LegalEntityAttributeUnchanged(le, leAttr)) {
						try {
							getDSConnection().getRemoteReferenceData().save(leAttr);
						} catch (CalypsoServiceException e) {
							Log.error(LOG_CATEGORY, e);
						}
					}
				}

			} else {
				Log.error(LOG_CATEGORY, "Legal Entity " + leShortName + " not found.");
			}

		}

		return true;
	}

	protected boolean LegalEntityAttributeUnchanged(LegalEntity le, LegalEntityAttribute leAttr) {
		if (le.getLegalEntityAttributes() == null)
			return false;
		for (Object currentLeAttrObj : le.getLegalEntityAttributes()) {
			if (currentLeAttrObj instanceof LegalEntityAttribute) {
				LegalEntityAttribute currentLeAttr = (LegalEntityAttribute) currentLeAttrObj;
				if (leAttr.getAttributeType() != null
						&& leAttr.getAttributeType().equals(currentLeAttr.getAttributeType())
						&& leAttr.getAttributeValue() != null
						&& leAttr.getAttributeValue().equals(currentLeAttr.getAttributeValue())
						&& leAttr.getLegalEntityRole() != null
						&& leAttr.getLegalEntityRole().equals(currentLeAttr.getLegalEntityRole()))
					return true;
			}
		}
		return false;

	}

}
