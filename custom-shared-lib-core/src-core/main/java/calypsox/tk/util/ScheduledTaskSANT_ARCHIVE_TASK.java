package calypsox.tk.util;

import com.calypso.tk.util.DataUploaderUtil;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TaskArray;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.api.CalypsoIDAPIUtil;
import com.calypso.tk.upload.sqlbindnig.UploaderSQLBindVariable;
import com.calypso.tk.upload.util.UploaderSQLBindAPI;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.bo.Task;
import com.calypso.tk.core.JDatetime;

public class ScheduledTaskSANT_ARCHIVE_TASK extends ScheduledTask {

	public static final String ARCHIVE_OPERATION = "Archive Operation Type";
	public static final String START_DATE = "Start Date";
	public static final String END_DATE = "End Date";
	public static final String EVENT_CLASS = "Event Class";
	public static final String EVENT_TYPE = "Event Type";
	public static final String MIN_ID = "Min Id";
	public static final String MAX_ID = "Max Id";
	transient DSConnection _ds;
	transient PSConnection _ps;

	public Vector<String> getDomainAttributes() {
		Vector<String> attrs = new Vector();
		attrs.add(ARCHIVE_OPERATION);
		attrs.add(EVENT_CLASS);
		attrs.add(EVENT_TYPE);
		attrs.add(START_DATE);
		attrs.add(END_DATE);
		attrs.add(MAX_ID);
		attrs.add(MIN_ID);
		return attrs;
	}

	public Vector<String> getAttributeDomain(String attr, Hashtable currentAttr) {
		Vector<String> attrList = new Vector();
		if (attr.equals("Archive Operation Type")) {
			attrList.add("Purge/Delete");
			attrList.add("Archive");
		}

		if (attr.equals("Event Type")) {
			attrList.add("EX_GATEWAY");
		}

		if (attr.equals("Event Class")) {
			attrList.add("Exception");
		}

		return attrList;
	}

	public boolean process(DSConnection ds, PSConnection ps) {
		boolean ret = true;
		this._ds = ds;
		this._ps = ps;
		if (this._publishB || this._sendEmailB) {
			ret = super.process(ds, ps);
		}

		TaskArray tasks = new TaskArray();
		String exec = null;
		if (this._executeB) {
			try {
				exec = archiveTasks();
			} catch (Exception var9) {
				Log.error("UPLOADER", var9);
				exec = "Error exceuting Archiving";
			}
		}

		Task task = new Task();
		CalypsoIDAPIUtil.setObjectId(task, (long) this.getId());
		task.setEventClass("Exception");
		task.setNewDatetime(new JDatetime());
		task.setDatetime(new JDatetime());
		task.setPriority(1);
		task.setId(0L);
		task.setStatus(0);
		if (exec == null) {
			task.setCompletedDatetime(new JDatetime());
			task.setEventType("EX_INFORMATION");
			task.setStatus(2);
			task.setComment(DataUploaderUtil.trim(this.toString(), 255));
		} else {
			task.setEventType("EX_EXCEPTION");
			task.setComment("Scheduled Task did not succeed " + this.toString() + "(" + exec + ")");
		}

		task.setUndoTradeDatetime(task.getDatetime());
		task.setCompletedDatetime(task.getDatetime());
		task.setNewDatetime(task.getDatetime());
		task.setUnderProcessingDatetime(task.getDatetime());
		task.setSource(this.getType());

		try {
			tasks.add(task);
			getReadWriteDS(ds).getRemoteBO().saveAndPublishTasks(tasks, 0L, (String) null);
		} catch (Exception var8) {
			Log.error("UPLOADER", var8);
		}

		return ret && exec == null;
	}

	public String archiveTasks() {
		try {
			List<UploaderSQLBindVariable> whereBindVariables = new ArrayList();
			String where = buildWhereForRelatedEntities("bo_task", "task_id", "task_datetime", whereBindVariables);
			if (where == null) {
				return null;
			} else {
				int count = UploaderSQLBindAPI.deleteRemoteBOTasks(where, !this.isPurgeDelete(), whereBindVariables);
				Log.debug("UPLOADER", count + " Tasks archived");
				return null;
			}
		} catch (Exception var4) {
			Log.error(this, var4);
			return null;
		}
	}

	protected String buildWhereForRelatedEntities(String tableName, String documentIdColumnName, String dateColumnName,
			List<UploaderSQLBindVariable> whereBindVariables) throws ParseException {
		JDate valDate = this.getValuationDatetime().getJDate();
		JDate fromDate = Util.stringToJDate(this.getAttribute("Start Date"));
		JDate toDate = Util.stringToJDate(this.getAttribute("End Date"));
		JDate startDate = getFromDate(valDate);
		JDate endDate = getToDate(valDate);

		if (fromDate == null && startDate == null) {
			fromDate = this.getValuationDatetime().getJDate();
		}

		if (toDate == null && endDate == null) {
			new JDate();
			toDate = JDate.getNow();
		}

		if (toDate != null && fromDate != null && toDate.before(fromDate)) {
			JDate tempDate = fromDate;
			fromDate = toDate;
			toDate = tempDate;
		}

		StringBuffer whereBuf = new StringBuffer();
		StringBuffer logBuf = new StringBuffer();
		logBuf.append("Archiving ").append(tableName);
		List<UploaderSQLBindVariable> whereBufBindVariables = new ArrayList();
		if (fromDate != null) {
			this.appendWhereDate(whereBuf, tableName, dateColumnName, " >= ", fromDate, whereBufBindVariables);
		}

		if (toDate != null) {
			this.appendWhereDate(whereBuf, tableName, dateColumnName, " <= ", toDate, whereBufBindVariables);
		}

		if (startDate != null && fromDate == null) {
			this.appendWhereDate(whereBuf, tableName, dateColumnName, " >= ",
					convertToJDatetime(startDate, 00, 00, 00, 000), whereBufBindVariables);
		}

		if (endDate != null && toDate == null) {
			this.appendWhereDate(whereBuf, tableName, dateColumnName, " <= ",
					convertToJDatetime(endDate, 23, 59, 59, 999), whereBufBindVariables);
		}

		String eventClass = this.getAttribute("Event Class");
		if (!Util.isEmpty(eventClass)) {
			this.appendWhereStringCol(whereBuf, tableName, "event_class", " = ", eventClass, whereBufBindVariables);
		}

		String eventType = this.getAttribute("Event Type");
		if (!Util.isEmpty(eventType)) {
			this.appendWhereStringCol(whereBuf, tableName, "event_type", " = ", eventType, whereBufBindVariables);
		}

		String maxId = this.getAttribute("Max Id");
		String minId = this.getAttribute("Min Id");
		if (!Util.isEmpty(minId)) {
			this.appendWhereId(whereBuf, tableName, "task_id", " >= ", minId, whereBufBindVariables);
		}

		if (!Util.isEmpty(maxId)) {
			this.appendWhereId(whereBuf, tableName, "task_id", " <= ", maxId, whereBufBindVariables);
		}

		if (whereBindVariables != null) {
			whereBindVariables.addAll(whereBufBindVariables);
		}

		Log.debug("UPLOADER", logBuf.toString());
		return whereBuf.toString();
	}

	protected void appendWhereStringCol(StringBuffer whereBuf, String tableName, String columnName, String compareOp,
			String value, List<UploaderSQLBindVariable> whereBufBindVariables) {
		if (whereBuf.length() > 0) {
			whereBuf.append(" and ");
		}

		whereBuf.append(tableName);
		whereBuf.append(".");
		whereBuf.append(columnName);
		whereBuf.append(compareOp);
		whereBuf.append(DataUploaderUtil.valueToPreparedString(value, whereBufBindVariables));
	}

	protected void appendWhereId(StringBuffer whereBuf, String tableName, String columnName, String compareOp,
			String value, List<UploaderSQLBindVariable> whereBufBindVariables) {
		if (whereBuf.length() > 0) {
			whereBuf.append(" and ");
		}

		whereBuf.append(tableName);
		whereBuf.append(".");
		whereBuf.append(columnName);
		whereBuf.append(compareOp);
		whereBuf.append(DataUploaderUtil.valueToPreparedString(value, whereBufBindVariables));
	}

	protected void appendWhereStatus(StringBuffer whereBuf, String tableName, String statusColumnName,
			String valueToSelect, List<UploaderSQLBindVariable> whereBufBindVariables) {
		if (whereBuf.length() > 0) {
			whereBuf.append(" AND ");
		}

		Vector v = Util.string2Vector(valueToSelect);
		whereBuf.append(tableName).append('.').append(statusColumnName).append(" in ")
				.append(DataUploaderUtil.collectionToPreparedInString(v, whereBufBindVariables));
	}

	protected void appendWhereDate(StringBuffer whereBuf, String tableName, String columnName, String compareOp,
			JDate date, List<UploaderSQLBindVariable> whereBufBindVariables) {
		if (date != null) {
			if (whereBuf.length() > 0) {
				whereBuf.append(" AND ");
			}

			whereBuf.append(tableName).append('.').append(columnName).append(compareOp)
					.append(DataUploaderUtil.valueToPreparedString(date, whereBufBindVariables));
		}
	}

	protected void appendWhereDate(StringBuffer whereBuf, String tableName, String columnName, String compareOp,
			JDatetime date, List<UploaderSQLBindVariable> whereBufBindVariables) {
		if (date != null) {
			if (whereBuf.length() > 0) {
				whereBuf.append(" AND ");
			}

			whereBuf.append(tableName).append('.').append(columnName).append(compareOp)
					.append(DataUploaderUtil.valueToPreparedString(date, whereBufBindVariables));
		}
	}

	protected boolean isPurgeDelete() {
		String operationType = this.getAttribute("Archive Operation Type");
		return "Purge/Delete".equalsIgnoreCase(operationType);
	}

	public String getTaskInformation() {
		String tmp = "Archive Tasks based on Creation Date, Message Type and a FilterSet.";
		return tmp;
	}

	private JDatetime convertToJDatetime(JDate date, int h, int m, int s, int ms) {
		return new JDatetime(date, h, m, s, ms, this._timeZone);
	}
}
