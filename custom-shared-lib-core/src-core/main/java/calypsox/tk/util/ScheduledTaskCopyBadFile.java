package calypsox.tk.util;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;

import calypsox.util.FileUtility;

/**
 * Scheduled Task to initialize Interest & Inflation Rates.
 * 
 * @author Jose David Sevillano (josedavid.sevillano@siag.es) interface control by David Porras Mart?nez
 */
public class ScheduledTaskCopyBadFile extends AbstractProcessFeedScheduledTask {
	private static final long serialVersionUID = 123L;

	private static final String FILENAME_DATE = "Filename Date";
	private static final String FILENAME_DATE_Dminus1 = "Filename Date D-1";
	private static final String TASK_INFORMATION = "Import Market Data Interest & Inflation Rates from a CSV file.";
	protected static final String PROCESS = "Load interest and inflation rates";
	private String file = "";

	private boolean processOK = false;

	@Override
	public String getTaskInformation() {
		return TASK_INFORMATION;
	}
	
	@Override
	protected List<AttributeDefinition> buildAttributeDefinition() {
		
		List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
		attributeList.addAll(super.buildAttributeDefinition());
		attributeList.add(attribute(FILENAME_DATE).domain(Arrays.asList("No date","_date",".date")));
		attributeList.add(attribute(FILENAME_DATE_Dminus1).booleanType());		
		
		return attributeList;	
	}

//	@Override
//	public Vector<String> getDomainAttributes() {
//		final Vector<String> attr = super.getDomainAttributes();
//		attr.add(FILENAME_DATE);
//		attr.add(FILENAME_DATE_Dminus1);
//		return attr;
//	}
//
//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	@Override
//	public Vector getAttributeDomain(final String attribute, final Hashtable hashtable) {
//		Vector vector = new Vector();
//		if (attribute.equals(FILENAME_DATE)) {
//			Collection col = new ArrayList<String>();
//			col.add("No date");
//			col.add("_date");
//			col.add(".date");
//			vector.addAll(col);
//		} else if (attribute.equals(FILENAME_DATE_Dminus1)) {
//			Collection col = new ArrayList<String>();
//			col.add("true");
//			col.add("false");
//			vector.addAll(col);
//		} else {
//			vector = super.getAttributeDomain(attribute, hashtable);
//		}
//
//		return vector;
//	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean process(final DSConnection conn, final PSConnection connPS) {

		final String path = getAttribute(FILEPATH);
		String startFileName = getAttribute(STARTFILENAME);
		String resultName = "";

		final String filenameDate = getAttribute(FILENAME_DATE);

		// get filename date
		JDate jdate = this.getValuationDatetime().getJDate(TimeZone.getDefault());
		Vector holidays = getHolidays();
		if (!Util.isEmpty(getAttribute(FILENAME_DATE_Dminus1)) && getAttribute(FILENAME_DATE_Dminus1).equals(true)) {
			jdate = jdate.addBusinessDays(-1, holidays);
		}

		if (filenameDate.equals("No date")) {
			resultName = startFileName;
		} else if (filenameDate.equals("_date")) {
			String strMonth = String.valueOf(jdate.getMonth());
			if (strMonth.length() == 1) {
				strMonth = "0" + strMonth;
			}

			String strDay = String.valueOf(jdate.getDayOfMonth());
			if (strDay.length() == 1) {
				strDay = "0" + strDay;
			}
			String strDate = jdate.getYear() + "" + strMonth + "" + strDay;
			resultName = startFileName.substring(0, startFileName.indexOf(".")) + "_" + strDate
					+ startFileName.substring(startFileName.indexOf('.'));

		} else if (filenameDate.equals(".date")) {
			String strMonth = String.valueOf(jdate.getMonth());
			if (strMonth.length() == 1) {
				strMonth = "0" + strMonth;
			}

			String strDay = String.valueOf(jdate.getDayOfMonth());
			if (strDay.length() == 1) {
				strDay = "0" + strDay;
			}
			String strDate = jdate.getYear() + "" + strMonth + "" + strDay;
			resultName = startFileName.substring(0, startFileName.indexOf(".")) + "." + strDate
					+ startFileName.substring(startFileName.indexOf('.'));
		}

		String badPath = path + "fail/";
		final ArrayList<String> files = checkFile(badPath, startFileName, this.getValuationDatetime().getJDate(TimeZone.getDefault()));

		if ((files != null) && (files.size() > 0)) {
			this.file = files.get(0);
			final String fileBadPath = badPath + this.file;
			final String fileNewPath = path + resultName;
			try {
				// copyFile(new File(fileBadPath), new File(fileNewPath));
				FileUtility.moveFile(fileBadPath, fileNewPath);
			} catch (Exception e) {
				Log.error(LOG_CATEGORY_SCHEDULED_TASK, "Error copying file", e);
				return this.processOK;
			}
			this.processOK = true;
		}

		return this.processOK;

	}

	@SuppressWarnings("resource")
	public void copyFile(File s, File t) {
		try {
			FileChannel in = (new FileInputStream(s)).getChannel();
			FileChannel out = (new FileOutputStream(t)).getChannel();
			in.transferTo(0, s.length(), out);
			in.close();
			out.close();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public static ArrayList<String> checkFile(final String path, String fileName, JDate date) {
		final ArrayList<String> array = new ArrayList<String>();
		final File files = new File(path);
		final String[] listFiles = files.list();
		String strDate = "", strMonth = "", strDay = "";

		if ((null != date) && (listFiles != null) && (listFiles.length > 0)) {
			strMonth = String.valueOf(date.getMonth());
			if (strMonth.length() == 1) {
				strMonth = "0" + strMonth;
			}

			strDay = String.valueOf(date.getDayOfMonth());
			if (strDay.length() == 1) {
				strDay = "0" + strDay;
			}

			strDate = date.getYear() + "" + strMonth + "" + strDay;

			for (int i = 0; i < listFiles.length; i++) {
				if ((listFiles[i].contains("bad_" + strDate))
						&& (listFiles[i].contains(fileName.substring(0, fileName.indexOf("."))))) {
					final String f = listFiles[i];
					array.add(f);
					break;
				}
			}
		}

		return array;
	}

	@Override
	public String getFileName() {
		// TODO Auto-generated method stub
		return null;
	}
}