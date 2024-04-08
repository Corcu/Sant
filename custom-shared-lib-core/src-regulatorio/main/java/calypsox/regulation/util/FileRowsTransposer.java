/**
 * 
 */
package calypsox.regulation.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.TradeArray;




/**
 * 
 * Modify the given file transposing columns into lines
 * 
 * @author aela
 * 
 */
public class FileRowsTransposer {

	private static MessageFormat rowFormatter = new MessageFormat("{0},{1},CLK,{2},TRD,ColLinking,{3},{4}");

	/**
	 * 
	 * Modify the given file transposing its columns into lines
	 * 
	 * @param fileNamePath
	 *            the full path name for the file to transpose
	 * @param fileSeparator
	 *            fields separator in the file
	 * @param headerLabels
	 *            if it's provided, it will be used as columns names otherwise
	 *            the first line will be considered as columns names
	 */
	public static void transposeFile(String fileNamePath, String fileSeparator, String[] headerLabels) {
		// open the file

		BufferedReader reader = null;
		String record = null;
		BufferedWriter writer = null;

		try {

			String[] rowColumns = null;
			String tradeRef = null;
			String sourceSystem = null;
			String tradeAction = null;
			String columnLabel = null;
			String[] columnLabels = null;
			String columnValue = null;

			// prepare the final output file

			File fileOut = new File(fileNamePath + ".in.tmp");
			File fileIn = new File(fileNamePath);

			FileWriter fileWriter = new FileWriter(fileOut);
			writer = new BufferedWriter(fileWriter);

			reader = new BufferedReader(new FileReader(fileIn));
			// loop over the file rows
			while ((record = reader.readLine()) != null) {
				if (columnLabels == null) {
					if (headerLabels == null) {
						// get the headers from the file
						columnLabels = record.split(fileSeparator);
					} else {
						columnLabels = headerLabels;
					}
					continue;
				}
				// for each row transpose the columns into rows
				rowColumns = record.split(fileSeparator);
				if (rowColumns != null && rowColumns.length < 15) {
					continue;
				}
				tradeRef = rowColumns[15];
				tradeAction = rowColumns[20];
				sourceSystem = getSourceSystem(rowColumns, columnLabels);

				int i = 0;
				for (String column : rowColumns) {
					columnLabel = columnLabels[i];
					columnValue = column;

					if (Util.isEmpty(columnValue)) {
						i++;
						continue;
					}

					Object[] rowArgs = { tradeRef, sourceSystem, tradeAction, columnLabel, columnValue };
					// flush the transposed rows into the new file
					if (!"Activity".equals(columnLabel)) {
						writer.write(rowFormatter.format(rowArgs));
						writer.newLine();
					}
					i++;
				}
			}
			writer.flush();

			fileOut.renameTo(fileIn);
		} catch (Exception e) {
			Log.error(FileRowsTransposer.class, e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					Log.error(FileRowsTransposer.class, e);
				}
			}

			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					Log.error(FileRowsTransposer.class, e);
				}
			}
		}
	}

	private static String getSourceSystem(final String[] rowColumns, final String[] columnLabels) {
		String sourceSystem = "431.4";
		LegalEntity le = null;
		TradeArray trades = new TradeArray();
		String boref = getBOReference(rowColumns, columnLabels);
		
		try {
			trades = DSConnection.getDefault().getRemoteTrade().getTradesByKeywordNameAndValue("BO_REFERENCE", boref);
		} catch (CalypsoServiceException e) {
			Log.info("Cannot get trades for BO_REFERENCE : " + boref + ". Try second option", e);
		}
		if(!Util.isEmpty(trades) && trades.get(0).getBook() != null){
			le = trades.get(0).getBook().getLegalEntity();
		}else{
			String portfolioCode = getPortfolioCode(rowColumns, columnLabels);
			if(isNumeric(portfolioCode)){
				CollateralConfig colConf = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
						Integer.valueOf(portfolioCode));
				if (colConf != null) {
					le = colConf.getProcessingOrg();
				}
			}
		}
		if (le != null) {
			String code = le.getCode();
			if ("BFOM".equals(code)) {
				sourceSystem = "519.4";
			} else if ("5HSF".equals(code)) {
				sourceSystem = "520.4";
			} else if ("BCHB".equals(code)) {
				sourceSystem = "521.4";
			}
		}
		
		return sourceSystem;
	}
	
	private static String getBOReference(final String[] rowColumns, final String[] columnLabels) {
		String borefrence = "";
		for (int i = 0; i < columnLabels.length; i++) {
			if ("TRADEPARTYTRANSACTIONID1".equals(columnLabels[i])) {
				borefrence = rowColumns[i];
				break;
			}
		}
		return borefrence;
	}
	
	private static String getPortfolioCode(final String[] rowColumns, final String[] columnLabels) {
		String portfolioCode = "";
		for (int i = 0; i < columnLabels.length; i++) {
			if ("COLLATERALPORTFOLIOCODE".equals(columnLabels[i])) {
				portfolioCode = rowColumns[i];
				break;
			}
		}
		return portfolioCode;
	}
	
	private static boolean isNumeric(String cadena){
		try {
			Integer.parseInt(cadena);
			return true;
		} catch (NumberFormatException nfe){
			return false;
		}
	}

//	 public static void main(String[] args) {
//	 FileRowsTransposer.transposeFile(
//			 "/calypso_interfaces/reports/CON_CALCOL_LNK#20170411.csv", ";", null);
//	 }
}
