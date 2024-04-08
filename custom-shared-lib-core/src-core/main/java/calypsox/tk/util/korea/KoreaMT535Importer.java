package calypsox.tk.util.korea;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.DomainValues;

/**
 * 
 * @author x957355
 *
 */
public class KoreaMT535Importer {
	
	public static final String DATE = DomainValues.comment("KOREA.MT535fields", "Date");
	public static final String PLEDGORD_ACCOUNT = DomainValues.comment("KOREA.MT535fields", "Pledger Account");
	public static final String PLEDGORD_NAME = DomainValues.comment("KOREA.MT535fields", "Pledger Name");
	public static final String PLEDGEE_ACCOUNT = DomainValues.comment("KOREA.MT535fields", "Pledgee Account");
	public static final String PLEDGEE_NAME = DomainValues.comment("KOREA.MT535fields", "Pledgee Name");
	public static final String CONTRACT = DomainValues.comment("KOREA.MT535fields", "Contract");

	/**
	 * Class that reads the excel file
	 * @param excelAbsoluteFileName filepath
	 * @return map with a string formed with a key and the data extracted from the file. The key consists
	 * in a '-' separated list of the excel values that should generate the external positions.
	 */
	public Map<String, LinkedList<HashMap<String, String>>> readExcelFile(String excelAbsoluteFileName) {
		HashMap<String, LinkedList<HashMap<String, String>>> map = null;

		Workbook wb = null;
		

		try {
			wb = WorkbookFactory.create(new FileInputStream(excelAbsoluteFileName));
			if (wb != null) {
				Sheet sheet = wb.getSheetAt(0);

				if (sheet == null) {
					Log.error(this, "Failed to import excel sheet in file:  " + excelAbsoluteFileName);
					return map;
				}
				map = getRowsAsList(sheet);
			}
			wb.close();
		} catch (IOException | EncryptedDocumentException | InvalidFormatException e) {
			Log.error(this, "Can not read the file " + excelAbsoluteFileName, e);
		}
		return map;
	}

	/**
	 * Get the data from the excel rows and formated it. It will obtain the map using a series of fields as a key, 
	 * in this way you can create the position for the different products that affect the same account
	 * @param sheet excel sheet to read
	 * @return data from the excel sheet
	 */
	private HashMap<String, LinkedList<HashMap<String, String>>> getRowsAsList(Sheet sheet) {
		HashMap<String, LinkedList<HashMap<String, String>>> map = new HashMap<>();
		List<String> keyHeaders = Arrays.asList(DATE, PLEDGORD_ACCOUNT, PLEDGORD_NAME, PLEDGEE_ACCOUNT,
				PLEDGEE_NAME,CONTRACT);
		int numRows = sheet.getPhysicalNumberOfRows();
		ArrayList<String> header = new ArrayList<>();

		if (numRows <= 1) {
			Log.error(this, "The file doesnt contain any row");
			return map;
		}

		for (int row = 0; row < numRows; ++row) {
			StringBuilder key = new StringBuilder();
			if (row == 0) { // Get the header names
				Row xssfRow = sheet.getRow(row);
				for (int cell = 0; cell < xssfRow.getLastCellNum(); ++cell) {
					header.add(xssfRow.getCell(cell).toString().trim().replace("\n", " ").replace("\r", " "));
				}

			} else { //Get the information

				Row xssfRow = sheet.getRow(row);
				DataFormatter formatter = new DataFormatter();
				if (xssfRow != null) {
					LinkedList<HashMap<String, String>> data = new LinkedList<>();
					HashMap<String, String> rowValues = new HashMap<>();
					for (int cell = 0; cell < xssfRow.getLastCellNum(); ++cell) {

						Cell xssfCell = xssfRow.getCell(cell);
						String cellValue = "";
						if(header.get(cell).equals(CONTRACT) || header.get(cell).equals(DATE)
								 || header.get(cell).equals(PLEDGEE_ACCOUNT) || header.get(cell).equals(PLEDGORD_ACCOUNT)) {
							cellValue = formatter.formatCellValue(xssfCell);
							if(cellValue.isEmpty())
								cellValue = " ";
						} else {
							cellValue = xssfCell.toString();
						}
						
						rowValues.put(header.get(cell), cellValue); // Inserts the column name and the column value

					}
					for (int i = 0; i < keyHeaders.size(); i++) { //Obtain the key conformed by the first five fields
						if (i != 0) {
							key.append("-");
						}
						key.append(rowValues.get(keyHeaders.get(i)));
					}
					if (map.containsKey(key.toString())) {
						map.get(key.toString()).add(rowValues);
					} else {
						data.add(rowValues);
						map.put(key.toString(), data);
					}

				}

			}
		}

		return map;
	}

}
