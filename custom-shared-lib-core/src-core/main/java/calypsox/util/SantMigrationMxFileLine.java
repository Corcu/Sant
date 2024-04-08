package calypsox.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class SantMigrationMxFileLine {

	/**
	 * The values
	 */
	protected final Map<String, String> values;

	/**
	 * The column names
	 */
	private Vector<String> columnNames;

	/**
	 * The number of line
	 */
	private int lineNumber;

	/**
	 * SANT_MIGRATION_MX_ITEM key.
	 */
	public static final String SANT_MIGRATION_MX_ITEM = "SANT_MIGRATION_MX_ITEM";

	// Columns
	public static final String CONTRACT_ORIGIN = "CONTRACT_ORIGIN";
	public static final String NUMERO_DE_MX2 = "NUMERO_DE_MX2"; // IDMx 2.11
	public static final String CONTRACT_REFERENCE = "CONTRACT_REFERENCE"; // IDMx3.1
	public static final String VERSION = "VERSION"; // TODO
	public static final String PL_CURRENCY = "PL_CURRENCY";
	public static final String INTERNAL = "INTERNAL";
	public static final String PORTFOLIO = "PORTFOLIO";
	public static final String TRN_FAMILY = "TRN_FAMILY";
	public static final String TRN_GROUP = "TRN_GROUP";
	public static final String TRN_TYPE = "TRN_TYPE";
	public static final String STATUS = "STATUS";
	public static final String EXP = "EXP"; // DD/MM/YYYY
	public static final String PROCESSDATE = "PROCESSDATE"; // DD/MM/YYYY
	public static final String CAP_INT = "CAP_INT";
	public static final String GID = "GID";
	public static final String CARGA_AGREGADA = "CARGA_AGREGADA";

	/**
	 * Instantiates a new row.
	 */
	public SantMigrationMxFileLine() {
		this.values = new HashMap<String, String>();
		this.lineNumber = 0;
		buildItem();
	}

	public void setLineNumber(final int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public int getLineNumber() {
		return this.lineNumber;
	}

	/**
	 * Gets the column value.
	 * 
	 * @param columnName
	 *            the column name
	 * @return the column value
	 */
	public Object getColumnValue(final String columnName) {
		return this.values.get(columnName);
	}

	/**
	 * Sets the column value.
	 * 
	 * @param columnName
	 *            the column name
	 * @param value
	 *            the value
	 */
	public void setColumnValue(final String columnName, final String value) {
		this.values.put(columnName, value);
	}

	/**
	 * Gets the Column Names.
	 * 
	 * @return
	 */
	public Vector<String> getColumnNames() {
		return new Vector<String>(this.columnNames);
	}

	private void buildItem() {
		this.columnNames = new Vector<String>();
		this.columnNames.add(CONTRACT_ORIGIN);
		this.columnNames.add(NUMERO_DE_MX2);
		this.columnNames.add(CONTRACT_REFERENCE);
		this.columnNames.add(VERSION);
		this.columnNames.add(PL_CURRENCY);
		this.columnNames.add(INTERNAL);
		this.columnNames.add(PORTFOLIO);
		this.columnNames.add(TRN_FAMILY);
		this.columnNames.add(TRN_GROUP);
		this.columnNames.add(TRN_TYPE);
		this.columnNames.add(STATUS);
		this.columnNames.add(EXP);
		this.columnNames.add(PROCESSDATE);
		this.columnNames.add(CAP_INT);
		this.columnNames.add(GID);
		this.columnNames.add(CARGA_AGREGADA);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Line: ");
		sb.append("CAP_INT: [").append(getColumnValue(CAP_INT)).append("] - ");
		sb.append("IDMx 2.11: [").append(getColumnValue(NUMERO_DE_MX2))
				.append("] - ");
		sb.append("IDMx 3.1: [").append(getColumnValue(CONTRACT_REFERENCE))
				.append("]");
		return sb.toString();
	}

	@Override
	protected Object clone() {
		SantMigrationMxFileLine aClone = new SantMigrationMxFileLine();
		aClone.values.putAll(this.values);
		aClone.lineNumber = this.lineNumber;
		return aClone;
	}

}
