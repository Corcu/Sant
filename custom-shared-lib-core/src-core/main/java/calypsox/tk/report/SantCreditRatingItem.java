package calypsox.tk.report;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.calypso.tk.core.JDate;
import com.calypso.tk.marketdata.CreditRating;

public class SantCreditRatingItem implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String CREDIT_RATING_ITEM = "SantCreditRatingItem";

	private String legalEntity;
	private String agency;
	private String ratingType;
	private String rating;
	private JDate date;

	public SantCreditRatingItem(CreditRating creditRating) {
		build(creditRating);
		buildMap();
	}

	private void build(CreditRating creditRating) {
		this.legalEntity = creditRating.getLegalEntityName();
		this.agency = creditRating.getAgencyName();
		this.ratingType = creditRating.getRatingType();
		this.rating = creditRating.getRatingValue();
		this.date = creditRating.getAsOfDate();
	}

	private final Map<String, Object> columnMap = new HashMap<String, Object>();

	public Object getColumnValue(String columnName) {
		return this.columnMap.get(columnName);
	}

	private void buildMap() {
		this.columnMap.put(SantCreditRatingReportStyle.LEGAL_ENTITY,
				this.legalEntity);
		this.columnMap.put(SantCreditRatingReportStyle.AGENCY, this.agency);
		this.columnMap.put(SantCreditRatingReportStyle.RATING_TYPE,
				this.ratingType);
		this.columnMap.put(SantCreditRatingReportStyle.RATING, this.rating);
		this.columnMap.put(SantCreditRatingReportStyle.DATE,
				formatDate(this.date));
	}

	public String getLegalEntity() {
		return legalEntity;
	}

	public String getAgency() {
		return agency;
	}

	public String getRatingType() {
		return ratingType;
	}

	public String getRating() {
		return rating;
	}

	public JDate getDate() {
		return date;
	}

	private String formatDate(final JDate jDate) {
		return jDate.toString();
	}

}
