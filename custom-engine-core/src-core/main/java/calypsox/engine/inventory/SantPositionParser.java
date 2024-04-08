/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.engine.inventory;

import static calypsox.engine.inventory.SantPositionConstants.ACTUAL;
import static calypsox.engine.inventory.SantPositionConstants.FIELDS_NUMBER;
import static calypsox.engine.inventory.SantPositionConstants.FIELDS_SEPARATOR;
import static calypsox.engine.inventory.SantPositionConstants.LINE_SEPARATOR;
import static calypsox.engine.inventory.SantPositionConstants.THEORETICAL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.calypso.tk.core.JDate;
import com.calypso.tk.util.importexport.JDateComparator;
//import static calypsox.engine.inventory.SantPositionConstants.EMPTY;

import calypsox.engine.inventory.util.PositionLogHelper;

/**
 * Parses the new positions lines and generates errors if any occur.
 * 
 * @author Patrice Guerrido & Guillermo Solano
 * @version 1.0
 * 
 */
public class SantPositionParser {

	/**
	 * true for testing mode (file watcher)
	 */
	private final boolean testing;

	/**
	 * Map with Bloqueos
	 */
	private final Map<JDate, SantPositionBean> bloqueoMap;

	/**
	 * Map with actuales
	 */
	private final Map<JDate, SantPositionBean> actualMap;

	/**
	 * Map with theoricals
	 */
	private final Map<JDate, SantPositionBean> theorMap;

	/**
	 * Constructor
	 * 
	 * @param testing
	 *            true for testing mode on
	 */
	public SantPositionParser(final boolean testing) {

		this.testing = testing;
		this.bloqueoMap = new HashMap<JDate, SantPositionBean>();
		this.actualMap = new HashMap<JDate, SantPositionBean>();
		this.theorMap = new HashMap<JDate, SantPositionBean>();
	}

	/**
	 * Constructor (testing disabled)
	 */
	public SantPositionParser() {
		this(false);
	}

	/**
	 * Parsers the received file message (N positions)
	 * 
	 * @param file
	 * @param errors
	 */
	public List<PositionLogHelper> parseFile(final String file) { // , final Vector<String> errors) {

		// clear previous positions received (old maps)
		clearPreviousParserData();

		// List to keep the status track for each row
		final List<PositionLogHelper> messLogTrack = new ArrayList<PositionLogHelper>();

		// separate rows
		final String[] rows = file.split(LINE_SEPARATOR);

		// adapt index to test or normal mode
		int i = 0;
		if (this.testing) {
			i = 1;// Exclude header
		}
		// main loop to iterate the positions received in the message
		for (; i < rows.length; i++) {

			// row track helper
			final PositionLogHelper rowLog = new PositionLogHelper();

			// current row: clean up any strange character (if any)
			// final String currentPosRow = removeSpecialCharacters(rows[i]);

			// parser the row and build the bean
			final SantPositionBean pos = parseRow(rows[i], rowLog);

			// track the row, bean and status
			rowLog.setRowBean(rows[i], pos);
			messLogTrack.add(rowLog);

			if (!rowLog.hasParserErrors()) {
				// add new positions to correct map
				addToMap(pos);
			}

		}
		return messLogTrack;
	}

	/**
	 * @return if the last message has at least one position to be inserted (was parsered OK)
	 */
	public boolean hasNewPositionsRows() {

		return (!this.actualMap.isEmpty() || !this.bloqueoMap.isEmpty() || !this.theorMap.isEmpty());
	}

	/**
	 * @return Bloqueos Map
	 */
	@SuppressWarnings("unchecked")
	public Map<JDate, SantPositionBean> getBloqueoMap() {
		TreeMap<JDate, SantPositionBean> treeMap = new TreeMap<JDate, SantPositionBean>(new JDateComparator());
		treeMap.putAll(this.bloqueoMap);
		return treeMap;
	}

	/**
	 * @return Actuales Map
	 */
	@SuppressWarnings("unchecked")
	public Map<JDate, SantPositionBean> getActualMap() {
		TreeMap<JDate, SantPositionBean> treeMap = new TreeMap<JDate, SantPositionBean>(new JDateComparator());
		treeMap.putAll(this.actualMap);
		return treeMap;
	}

	/**
	 * @return Theorical's Map
	 */
	@SuppressWarnings("unchecked")
	public Map<JDate, SantPositionBean> getTheorMap() {
		TreeMap<JDate, SantPositionBean> treeMap = new TreeMap<JDate, SantPositionBean>(new JDateComparator());
		treeMap.putAll(this.theorMap);
		return treeMap;
	}

	/**
	 * Resets the maps and previous parsing errors (if we had any)
	 */
	private void clearPreviousParserData() {

		this.bloqueoMap.clear();
		this.actualMap.clear();
		this.theorMap.clear();
	}

	/**
	 * remove characters not in [a-zA-Z_0-9| -/.:], special chars
	 * 
	 * @param s
	 *            to clean
	 * @return s withouth any char not included in the previous set
	 */
	// private static String removeSpecialCharacters(final String s) {
	//
	// if ((s == null) || s.isEmpty()) {
	// return EMPTY;
	// }
	// // replacement, special are "not normal" chars, so:
	// final String notNormal = "[^A-Za-z0-9" + SantPositionConstants.RESPONSE_SEPARATOR + " @+-/.:_" + "]";
	// return s.replaceAll(notNormal, EMPTY).trim();
	// }

	/**
	 * Parsers a row
	 * 
	 * @param row
	 *            to parsers
	 * @param rowLog
	 * @param errors
	 *            vector
	 * @return a pos if parser row was fine, null otherwise
	 */
	public SantPositionBean parseRow(final String row, final PositionLogHelper rowLog) {

		final String[] columns = row.split(FIELDS_SEPARATOR);

		// check we have all the fields
		if (columns.length != FIELDS_NUMBER) {

			return SantPositionBean.tryBuildPosBeanFromIncorrectLine(row, rowLog);
		}
		// correct number of fields
		return new SantPositionBean(columns, rowLog);
	}

	/**
	 * A valid read position is added to the correct type of map
	 * 
	 * @param santUpdatePos
	 */
	// package visibility for unit test purpose
	void addToMap(final SantPositionBean santUpdatePos) {

		final String posType = santUpdatePos.getPositionType();

		if (THEORETICAL.equals(posType)) {
			this.theorMap.put(santUpdatePos.getPositionDate(), santUpdatePos);

		} else if (ACTUAL.equals(posType)) {
			this.actualMap.put(santUpdatePos.getPositionDate(), santUpdatePos);

		} else {
			this.bloqueoMap.put(santUpdatePos.getPositionDate(), santUpdatePos);
		}
	}

} // END CLASS
