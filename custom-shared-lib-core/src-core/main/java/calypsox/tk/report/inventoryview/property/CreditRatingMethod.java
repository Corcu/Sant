/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report.inventoryview.property;

import java.io.Serializable;

import com.calypso.tk.core.Util;

public class CreditRatingMethod implements Serializable {

	private static final long serialVersionUID = 1L;
	private String issueOrIssuer;
	private String highestOrLowest = "";
	private String sign;
	private String creditRating;

	private String ratingAgency;
	private boolean strict;
	@SuppressWarnings("unused")
	private boolean isInternal;

	public boolean isStrict() {
		return this.strict;
	}

	public void setStrict(boolean strict) {
		this.strict = strict;
	}

	public String getIssueOrIssuer() {
		return this.issueOrIssuer;
	}

	public void setIssueOrIssuer(String issueOrIssuer) {
		this.issueOrIssuer = issueOrIssuer;
	}

	public String getHighestOrLowest() {
		return this.highestOrLowest;
	}

	public void setHighestOrLowest(String highestOrLowest) {
		this.highestOrLowest = highestOrLowest;
	}

	public String getRatingAgency() {
		return this.ratingAgency;
	}

	public void setRatingAgency(String ratingAgency) {
		this.ratingAgency = ratingAgency;
	}

	public String getSign() {
		return this.sign;
	}

	public void setSign(String sign) {
		this.sign = sign;
	}

	public String getCreditRating() {
		return this.creditRating;
	}

	public void setCreditRating(String creditRating) {
		this.creditRating = creditRating;
	}

	/**
	 * We treat it as empty if one of issueOrIssuer/creditRating/sign/ratingAgency are null
	 * 
	 * @return
	 */
	public boolean isEmpty() {
		if (Util.isEmpty(this.issueOrIssuer) || Util.isEmpty(this.ratingAgency) || Util.isEmpty(this.sign)
				|| (Util.isEmpty(this.creditRating) && !CreditRatingHelper.EXISTS.equals(this.sign))) {
			return true;
		}
		return false;
	}

	public String toTemplateString() {
		String result = "";

		if (!Util.isEmpty(this.issueOrIssuer)) {
			result += this.issueOrIssuer;
		}
		result += ";";

		if (!Util.isEmpty(this.highestOrLowest)) {
			result += this.highestOrLowest;
		}
		result += ";";

		if (!Util.isEmpty(this.sign)) {
			result += this.sign;
		}
		result += ";";

		if (!CreditRatingHelper.EXISTS.equals(this.sign)) {
			if (!Util.isEmpty(this.creditRating)) {
				result += this.creditRating;
			}
		}
		result += ";";

		if (!Util.isEmpty(this.ratingAgency)) {
			result += this.ratingAgency;
		}
		result += ";";

		result += this.strict;
		result += ";";

		return result;
	}

	public static CreditRatingMethod valueOf(String templateString, String agency, boolean isStrict) {
		CreditRatingMethod result = new CreditRatingMethod();

		if (!Util.isEmpty(templateString)) {
			return valueOf(templateString);
		} else {
			result.setRatingAgency(agency);
			result.setStrict(isStrict);
		}

		return result;
	}

	// public static CreditRatingMethod valueOf(String templateString, String agency, boolean isStrict, boolean
	// isInternal) {
	// CreditRatingMethod result = valueOf(templateString, agency, isStrict);
	// result.setInternal(isInternal);
	// return result;
	// }

	public static CreditRatingMethod valueOf(String templateString) {
		CreditRatingMethod result = new CreditRatingMethod();

		if (!Util.isEmpty(templateString)) {
			String[] split = templateString.split(";");

			if (!Util.isEmpty(split[0])) {
				result.setIssueOrIssuer(split[0]);
			}
			if (!Util.isEmpty(split[1])) {
				result.setHighestOrLowest(split[1]);
			}
			if (!Util.isEmpty(split[2])) {
				result.setSign(split[2]);
			}
			if (!Util.isEmpty(split[3])) {
				result.setCreditRating(split[3]);
			}
			if (!Util.isEmpty(split[4])) {
				result.setRatingAgency(split[4]);
			}

			if ("true".equals(split[5])) {
				result.setStrict(true);
			}
		}

		return result;
	}

	@Override
	public String toString() {
		String result = "";
		if (isEmpty()) {
			return result;
		}

		if (!Util.isEmpty(this.highestOrLowest)) {
			result += this.highestOrLowest;
			result += " ";
		}
		result += this.issueOrIssuer;
		result += " ";

		result += this.sign;

		if (!CreditRatingHelper.EXISTS.equals(this.sign)) {
			result += " ";
			result += this.creditRating;
		}

		return result;
	}

	// public boolean isInternal() {
	// return this.isInternal;
	// }
	//
	// public void setInternal(boolean isInternal) {
	// this.isInternal = isInternal;
	// }
}