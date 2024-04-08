package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.OptimizationConfiguration;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.refdata.optimization.impl.Category;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

public class OptimSecurityFiltersReportStyle extends ReportStyle {

	private static final long serialVersionUID = 5789236881201593198L;

	public static final String OPTIM_CONFIG_NAME = "Optim Config Name";

	public static final String CAT_NAME = "Category Name";
	public static final String CAT_DESC = "Category Desc";
	public static final String CAT_WEIGHT = "Category Weight";
	public static final String CAT_SD_FILTER = "Category SD Filter";

	public static final String FILTER_ATTR = "Filter Attr";
	public static final String FILTER_CRITERIA = "Filter Criteria";
	public static final String FILTER_VALUES = "Filter Values";

	public static final String[] DEFAULTS_COLUMNS = { OPTIM_CONFIG_NAME, CAT_DESC, CAT_WEIGHT, CAT_SD_FILTER,
			FILTER_ATTR, FILTER_CRITERIA, FILTER_VALUES };

	@Override
	public Object getColumnValue(ReportRow row, String columnName, @SuppressWarnings("rawtypes") Vector errors)
			throws InvalidParameterException {

		OptimizationConfiguration config = (OptimizationConfiguration) row
				.getProperty(OptimSecurityFiltersReportTemplate.OPTIM_CONFIG);
		Category targetCategory = (Category) row.getProperty(OptimSecurityFiltersReportTemplate.TARGET_CATEGORY);
		StaticDataFilterElement filterElement = (StaticDataFilterElement) row
				.getProperty(OptimSecurityFiltersReportTemplate.FILTER_ELEMENT);

		if (columnName.equals(OPTIM_CONFIG_NAME)) {
			return config.getName();

		} else if (columnName.equals(CAT_DESC)) {
			return targetCategory.getName();
		} else if (columnName.equals(CAT_WEIGHT)) {
			return targetCategory.getWeight();
		} else if (columnName.equals(CAT_SD_FILTER)) {
			return targetCategory.getFilterName();
		} else if (columnName.equals(FILTER_ATTR)) {
			return filterElement.getName();
		} else if (columnName.equals(FILTER_CRITERIA)) {
			// MIGRATION V14.4 18/01/2015
			return filterElement.getOperatorType() != null ? filterElement.getOperatorType().getDisplayName() : "";
		} else if (columnName.equals(FILTER_VALUES)) {
			return getElementValue(filterElement);
		}

		return null;
	}

	@SuppressWarnings({ "deprecation"})
	private String getElementValue(StaticDataFilterElement e) {
		String value = "";
		int itype = StaticDataFilterElement.string2Type(e.getTypeAsString());
		e.setType(itype);

		switch (itype) {
		case StaticDataFilterElement.IN:
		case StaticDataFilterElement.NOT_IN:
		case StaticDataFilterElement.ALL_IN:
		case StaticDataFilterElement.IN_LIST:
		case StaticDataFilterElement.STRING_ENUMERATION:
		case StaticDataFilterElement.INT_ENUM:
		case StaticDataFilterElement.NOT_IN_INT_ENUM:
		case StaticDataFilterElement.FLOAT_ENUM:
		case StaticDataFilterElement.NOT_IN_FLOAT_ENUM:
			value = Util.vector2String(e.getValues());
			break;
		case StaticDataFilterElement.INT_RANGE:
		case StaticDataFilterElement.FLOAT_RANGE:
		case StaticDataFilterElement.NOT_IN_FLOAT_RANGE:
		case StaticDataFilterElement.STRING_RANGE:
			value = getMinMaxValue(e);
			break;
		case StaticDataFilterElement.DATE_COMPARISON:

			value = e.toString();
			break;
		case StaticDataFilterElement.TENOR_RANGE:

			value = updateTenorRangeValue(e);
			break;
		case StaticDataFilterElement.DATE_RANGE:
		case StaticDataFilterElement.DATETIME_RANGE:

			value = updateDateRangeValue(e);
			break;
		case StaticDataFilterElement.KICKOFF_CUTOFF_RULE:
		case StaticDataFilterElement.DATE_RULE:
		case StaticDataFilterElement.LIKE:
		case StaticDataFilterElement.NOT_LIKE:
			value = e.getLikeValue();
			break;
		case StaticDataFilterElement.IS:
			value = e.getIsValue() ? " true" : " false";
			break;
		// case StaticDataFilterElement.IS_NULL:
		// case StaticDataFilterElement.IS_NOT_NULL:
		// break;
		}

		return value;
	}

	private String updateTenorRangeValue(StaticDataFilterElement e) {
		Object min = e.getMinValue();
		Object max = e.getMaxValue();
		if ((min == null) || (max == null)) {
			return "";
		}
		return "From " + (String) min + " to " + (String) max;
	}

	private String getMinMaxValue(StaticDataFilterElement e) {
		Object min = e.getMinValue();
		Object max = e.getMaxValue();
		return (min == null ? "<Min>" : min.toString()) + ", " + (max == null ? "<Max>" : max.toString());
	}

	private String updateDateRangeValue(StaticDataFilterElement e) {
		// BZ 40684 - modified to allow nulls and inclusive/exclusive checkbox
		Object min = e.getMinValue();
		Object max = e.getMaxValue();
		boolean minInclusive = e.isMinInclusive();
		boolean maxInclusive = e.isMaxInclusive();

		String modelMessage = "";

		if ((min == null) && (max == null)) {
			modelMessage = "";
		} else {
			if (min != null) {
				modelMessage = "From ";
				if (min instanceof JDate) {
					modelMessage += Util.dateToString((JDate) min);
				} else {
					JDatetime dtmin = (JDatetime) min;
					JDate jdmin = JDate.valueOf(dtmin, TimeZone.getDefault());
					modelMessage += Util.dateToString(jdmin) + " " + Util.timeToString(dtmin);
				}

				modelMessage += (minInclusive) ? " inclusive" : " exclusive";

			}
			if (max != null) {
				modelMessage += " To ";
				if (max instanceof JDate) {
					modelMessage += Util.dateToString((JDate) max);
				} else {
					JDatetime dtmax = (JDatetime) max;
					JDate jdmax = JDate.valueOf(dtmax, TimeZone.getDefault());
					modelMessage += Util.dateToString(jdmax) + " " + Util.timeToString(dtmax);
				}

				modelMessage += (maxInclusive) ? " inclusive" : " exclusive";

			}
		}

		return modelMessage;
	}

}
