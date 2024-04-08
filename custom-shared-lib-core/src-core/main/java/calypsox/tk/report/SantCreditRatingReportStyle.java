package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

public class SantCreditRatingReportStyle extends ReportStyle {

	private static final long serialVersionUID = -1L;

	public static final String LEGAL_ENTITY = "Legal Entity";
	public static final String AGENCY = "Agency";
	public static final String RATING_TYPE = "Rating Type";
	public static final String RATING = "Rating";
	public static final String DATE = "Date";

	public static final String[] DEFAULT_COLUMNS = { LEGAL_ENTITY, AGENCY,
			RATING_TYPE, RATING, DATE };

	@Override
	@SuppressWarnings("rawtypes")
	public Object getColumnValue(final ReportRow row, final String columnName,
			final Vector errors) throws InvalidParameterException {

		final SantCreditRatingItem item = (SantCreditRatingItem) row
				.getProperty(SantCreditRatingItem.CREDIT_RATING_ITEM);
		return item.getColumnValue(columnName);

	}
}
