package calypsox.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

public class SantMigrationMxFileReader {

	private boolean fileContainsHeaderLine = false;

	public SantMigrationMxFileReader(final boolean fileContainsHeaderLine) {
		this.fileContainsHeaderLine = fileContainsHeaderLine;
	}

	public boolean isFileContainsHeaderLine() {
		return this.fileContainsHeaderLine;
	}

	/**
	 * Reads a complete file and return the result as
	 * Vector<MigrationMxFileLine>.
	 * 
	 * @param fileName
	 *            Complete file name
	 * @return separator Separator between columns
	 * @throws IOException
	 *             If problems were detected while reading.
	 */
	public Vector<SantMigrationMxFileLine> readFile(final String separator,
			final String fileName) throws IOException {

		final Vector<SantMigrationMxFileLine> linesVector = new Vector<SantMigrationMxFileLine>();
		Scanner scanner = null;
		StringBuilder msgError = new StringBuilder();

		try {
			scanner = SantMigrationMxUtil.getInstance().createScanner(fileName);
			SantMigrationMxFileLine singleLine = null;
			boolean hasNext = scanner.hasNext();
			int lineNumber = 0;

			while (hasNext) {
				lineNumber++;
				final List<String> line = SantMigrationMxUtil.getInstance()
						.parseLine(scanner.nextLine(), separator);
				singleLine = readLine(line);
				if (singleLine != null) {
					singleLine.setLineNumber(lineNumber);
					linesVector.add(singleLine);
				}
				hasNext = scanner.hasNext();
			}

		} catch (IOException e) {
			msgError.append("Error while reading the file ");
			msgError.append(e.getMessage()).append("\n");
			Log.error(this, e.getMessage(), e);
			throw new IOException(msgError.toString());
		} finally {
			if (scanner != null) {
				scanner.close();
			}
		}

		// PostProcess of import file lines
		postProcess(linesVector);

		return linesVector;
	}

	/**
	 * Reads and process a line from a file. It only calls readLineSplitted.
	 * 
	 * @param line
	 *            Line to be processed.
	 * @param header
	 *            Check the header
	 * @return MigrationMxFileLine as result.
	 */
	private SantMigrationMxFileLine readLine(final List<String> line) {

		SantMigrationMxFileLine rst = null;

		if (!Util.isEmpty(line)) {
			try {
				rst = readLineSplitted(line);
			} catch (final Exception e) {

				final StringBuilder str = new StringBuilder();
				str.append("Error while processing the line ");
				str.append(line);
				str.append(". Exception was : ");
				str.append(e.toString());
				Log.error(this, str.toString(), e);
			}
		}

		return rst;
	}

	/**
	 * Read a line splitted in List<String>.
	 * 
	 * @param line
	 *            List containing all the parts of the line.
	 * 
	 * @return MigrationMxFileLine with all the data.
	 */
	private SantMigrationMxFileLine readLineSplitted(final List<String> line) {
		final SantMigrationMxFileLine item = new SantMigrationMxFileLine();
		final Vector<String> fileColumnsNames = item.getColumnNames();

		if (!fileColumnsNames.isEmpty()) {
			for (int pos = 0; pos < line.size(); pos++) {
				final String value = line.get(pos);
				final String columnName = fileColumnsNames.get(pos);
				item.setColumnValue(columnName, value);
			}
		}

		return item;
	}

	/**
	 * Post process of the lines.
	 * 
	 * By default, if the file contains header line, the post process delete it.
	 * Override this method in other case.
	 * 
	 * @param linesVector
	 * @param fileContainsHeaderLine
	 */
	public void postProcess(final Vector<SantMigrationMxFileLine> linesVector) {

		// Delete lines that don't belong to Calypso

		Log.info(
				this,
				"START SantMigrationMxLineReader.postProcess - Delete lines that don't belong to Calypso.");

		List<String> listMxRef = new ArrayList<String>();
		Vector<SantMigrationMxFileLine> vLinesAccepted = new Vector<SantMigrationMxFileLine>();
		HashMap<Integer, SantMigrationMxFileLine> eLineMap = new HashMap<Integer, SantMigrationMxFileLine>();
		boolean isCalypsoTrade = false;
		int numberLine = 0;

		for (SantMigrationMxFileLine line : linesVector) {
			numberLine++;

			if (checkLine(line)) {
				String mx211Reference = (String) line
						.getColumnValue(SantMigrationMxFileLine.NUMERO_DE_MX2);

				final Trade trade = SantMigrationMxUtil.getInstance()
						.getTradeFromMurexReference(mx211Reference);
				isCalypsoTrade = (null != trade);

				if (isCalypsoTrade && !listMxRef.contains(mx211Reference)) {
					Log.info(this, "Line Accepted: " + line.toString());
					vLinesAccepted.add(line);
					listMxRef.add(mx211Reference);
				}
			} else {
				// Add error line from file
				eLineMap.put(numberLine, line);
			}
		}

		// if (!eLineMap.isEmpty()) {
		// getErrorLineMap().putAll(eLineMap);
		// }

		Log.info(
				this,
				"SantMigrationMxLineReader - Total Error lines [Without values in NB_MX2, CONTRACT_REFERENCE or CAP_INT column]: "
						+ eLineMap.size());
		Log.info(this, "SantMigrationMxLineReader - Total lines: "
				+ linesVector.size());
		Log.info(this, "SantMigrationMxLineReader - Total lines accepted: "
				+ vLinesAccepted.size());

		linesVector.clear();
		linesVector.addAll(vLinesAccepted);

		Log.info(this, "SantMigrationMxLineReader - Total lines to process: "
				+ linesVector.size());
		Log.info(this, "END SantMigrationMxLineReader.postProcess.");
	}

	/**
	 * Check the line read.
	 * 
	 * Check if NB_MX2, CONTRACT_REFERENCE and CAP_INT columns are not empties
	 * or null.
	 * 
	 * @param line
	 * @return true if is succesfully, false in other case.
	 */
	private boolean checkLine(final SantMigrationMxFileLine line) {
		final String mx211Reference = (String) line
				.getColumnValue(SantMigrationMxFileLine.NUMERO_DE_MX2);

		final String mx31Reference = (String) line
				.getColumnValue(SantMigrationMxFileLine.CONTRACT_REFERENCE);

		final String capInt = (String) line
				.getColumnValue(SantMigrationMxFileLine.CAP_INT);

		final boolean isMx211RefOk = !Util.isEmpty(mx211Reference);
		final boolean isMx31RefOk = !Util.isEmpty(mx31Reference);
		final boolean isCapIntOk = !Util.isEmpty(capInt);

		return isMx211RefOk && isMx31RefOk && isCapIntOk;
	}

}
