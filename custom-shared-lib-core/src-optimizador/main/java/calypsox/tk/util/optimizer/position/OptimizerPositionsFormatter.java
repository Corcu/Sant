package calypsox.tk.util.optimizer.position;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import calypsox.tk.util.optimizer.position.OptimizerPositions.EnumBalanceType;
import calypsox.tk.util.optimizer.position.OptimizerPositions.EnumPositionType;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.service.DSConnection;

public class OptimizerPositionsFormatter {

	public static final String DELIMITER = "\n";

	public static final String SEPARATOR = "|";

	private static SimpleDateFormat TIMESTAMP_SDF = new SimpleDateFormat(
			"yyyy-MM-dd-HH.mm.ss");

	public static List<OptimizerExportPosition> format(
			OptimizerPositions optimPositions) {
		List<OptimizerExportPosition> optimizerExportPositions = new ArrayList<OptimizerExportPosition>();
		appendPositions(optimizerExportPositions, optimPositions);
		return optimizerExportPositions;
	}

	private static void appendPositions(
			List<OptimizerExportPosition> optimizerExportPositions,
			OptimizerPositions optimizerPositions) {
		StringBuffer sb = new StringBuffer();
		StringBuffer sbMain = new StringBuffer();
		appendMain(sbMain, optimizerPositions);
		for (EnumPositionType positionType : optimizerPositions.getPositions()
				.keySet()) {
			// if (hasPositions(positionType)) {
			StringBuffer sbPositionType = new StringBuffer();
			sbPositionType.append(positionType.toString(positionType));
			sbPositionType.append(SEPARATOR);
			for (EnumBalanceType balanceType : optimizerPositions
					.getPositions().get(positionType).keySet()) {
				if (optimizerPositions.hasPositions(positionType, balanceType)) {
					sb.append(sbMain.toString());
					sb.append(sbPositionType.toString());

					sb.append(balanceType.toString(balanceType));
					sb.append(SEPARATOR);
					for (int i = 0; i < OptimizerPositions.POSITIONS_NB_DAYS; i++) {
						sb.append(getPositionValueString(
								optimizerPositions,
								optimizerPositions.getPositions()
										.get(positionType).get(balanceType)[i]));
						sb.append(SEPARATOR);
					}
					sb.append(TIMESTAMP_SDF.format(new JDatetime()));
					sb.append(SEPARATOR);
					sb.append(optimizerPositions.getProductFamily());
					sb.append(SEPARATOR);

					// build key
					StringBuffer sbKey = new StringBuffer(sbMain.toString());
					sbKey.append(sbPositionType.toString());
					sbKey.append(balanceType.toString(balanceType));

					OptimizerExportPosition optPosExport = new OptimizerExportPosition(
							sbKey.toString(), sb.toString());
					optimizerExportPositions.add(optPosExport);

					sb = new StringBuffer();
				}
			}
			// }
		}
	}

	private static void appendMain(StringBuffer sbMain,
			OptimizerPositions optimizerPos) {
		Book book = BOCache.getBook(DSConnection.getDefault(),
				optimizerPos.getBookId());
		sbMain.append(book.getLegalEntity());
		sbMain.append(SEPARATOR);
		sbMain.append(book.getName());
		sbMain.append(SEPARATOR);
		sbMain.append(optimizerPos.getISIN());
		sbMain.append(SEPARATOR);
		sbMain.append(optimizerPos.getCurrency());
		sbMain.append(SEPARATOR);
	}

	private static String getPositionValueString(
			OptimizerPositions optimizerPositions, double posValue) {
		return Util.numberToString(Double.isNaN(posValue) ? 0.00 : posValue,
				getQuantityDecimals(optimizerPositions));
	}

	private static int getQuantityDecimals(OptimizerPositions optimizerPositions) {
		if (OptimizerPositions.BOND.equals(optimizerPositions)) {
			return ((Bond) optimizerPositions.getSecurity())
					.getQuantityDecimals(optimizerPositions.getCurrency());
		} else if (OptimizerPositions.EQUITY.equals(optimizerPositions)) {
			return ((Equity) optimizerPositions.getSecurity())
					.getQuantityDecimals(optimizerPositions.getCurrency());
		}
		return 2;
	}

	public static String formatKey(OptimizerPositions optimizerPos) {
		StringBuffer sbKey = new StringBuffer();
		Book book = BOCache.getBook(DSConnection.getDefault(),
				optimizerPos.getBookId());
		sbKey.append(book.getLegalEntity());
		sbKey.append(SEPARATOR);
		sbKey.append(book.getName());
		sbKey.append(SEPARATOR);
		sbKey.append(optimizerPos.getISIN());
		sbKey.append(SEPARATOR);
		sbKey.append(optimizerPos.getCurrency());
		sbKey.append(SEPARATOR);
		return sbKey.toString();
	}
}
