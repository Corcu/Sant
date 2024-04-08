/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.core;

import com.calypso.tk.core.PricerMeasure;

public class SantPricerMeasure extends PricerMeasure {

	// unique class id, important to avoid problems
	private static final long serialVersionUID = 2233295722838547L;

	public static final int INDEPENDENT_AMOUNT_BASE = 1001;
	public static final int NPV_BASE = 1002;
	public static final int NPV_LEG1 = 1003;
	public static final int NPV_LEG2 = 1004;
	public static final int CLOSING_PRICE = 1005;
	public static final int REPO_ACCRUED_INTEREST = 1006;
	public static final int BOND_ACCRUED_INTEREST = 1007;
	public static final int CLEAN_PRICE = 1008;
	public static final int CAPITAL_FACTOR = 1009;
	// public static final int POOL_FACTOR = 1010;
	public static final int VARIATION_MARGIN_DM = 1011;
	public static final int NPV_LEG_1_DM = 1012;
	public static final int NPV_LEG_2_DM = 1013;
	public static final int NPV_DM = 1014;
	public static final int NPV_BASE_DM = 1015;
	public static final int INDEPENDENT_AMOUNT_DM = 1016;
	public static final int INDEPENDENT_AMOUNT_BASE_DM = 1017;
	public static final int IA_SETTLEMENT_DM = 1018;
	public static final int MARGIN_CALL_DM = 1019;

	public static final String S_INDEPENDENT_AMOUNT_BASE = "INDEPENDENT_AMOUNT_BASE";
	public static final String S_NPV_BASE = "NPV_BASE";
	public static final String S_NPV_LEG1 = "NPV_LEG1";
	public static final String S_NPV_LEG2 = "NPV_LEG2";
	public static final String S_CLOSING_PRICE = "CLOSING_PRICE";
	public static final String S_REPO_ACCRUED_INTEREST = "REPO_ACCRUED_INTEREST";
	public static final String S_BOND_ACCRUED_INTEREST = "BOND_ACCRUED_INTEREST";
	public static final String S_CLEAN_PRICE = "CLEAN_PRICE";
	public static final String S_CAPITAL_FACTOR = "CAPITAL_FACTOR";

	// public static final String S_POOL_FACTOR = "POOL_FACTOR";

	public static final String S_VARIATION_MARGIN_DM = "VARIATION_MARGIN_DM";
	public static final String S_NPV_LEG_1_DM = "NPV_LEG1_DM";
	public static final String S_NPV_LEG_2_DM = "NPV_LEG2_DM";
	public static final String S_NPV_DM = "NPV_DM";
	public static final String S_NPV_BASE_DM = "NPV_BASE_DM";
	public static final String S_INDEPENDENT_AMOUNT_DM = "INDEPENDENT_AMOUNT_DM";
	public static final String S_INDEPENDENT_AMOUNT_BASE_DM = "INDEPENDENT_AMOUNT_BASE_DM";
	public static final String S_IA_SETTLEMENT_DM = "IA_SETTLEMENT_DM";

	public static final String S_IA_SETTLEMENT_BASE_DM = "IA_SETTLEMENT_BASE_DM";
	public static final String S_MARGIN_CALL_DM = "MARGIN_CALL_DM";


	public SantPricerMeasure() {
	}

	public SantPricerMeasure(final int i) {
		this._type = i;
	}

	@Override
	public String getName() {
		return toString(this._type);
	}

	public static int toInt(final String str) {
		switch (str) {
			case S_INDEPENDENT_AMOUNT_BASE:
				return INDEPENDENT_AMOUNT_BASE;
			case S_NPV_BASE:
				return NPV_BASE;
			case S_NPV_LEG1:
				return NPV_LEG1;
			case S_NPV_LEG2:
				return NPV_LEG2;
			case S_CLOSING_PRICE:
				return CLOSING_PRICE;
			case S_REPO_ACCRUED_INTEREST:
				return REPO_ACCRUED_INTEREST;
			case S_BOND_ACCRUED_INTEREST:
				return BOND_ACCRUED_INTEREST;
			case S_CLEAN_PRICE:
				return CLEAN_PRICE;
			case S_CAPITAL_FACTOR:
				return CAPITAL_FACTOR;
			case S_VARIATION_MARGIN_DM:
				return VARIATION_MARGIN_DM;
			case S_NPV_LEG_1_DM :
				return NPV_LEG_1_DM;
			case S_NPV_LEG_2_DM:
				return NPV_LEG_2_DM;
			case S_NPV_DM:
				return NPV_DM;
			case S_NPV_BASE_DM:
				return NPV_BASE_DM;
			case S_INDEPENDENT_AMOUNT_DM:
				return INDEPENDENT_AMOUNT_DM;
			case S_INDEPENDENT_AMOUNT_BASE_DM:
				return INDEPENDENT_AMOUNT_BASE_DM;
			case S_IA_SETTLEMENT_DM:
				return IA_SETTLEMENT_DM;
			case S_MARGIN_CALL_DM:
				return MARGIN_CALL_DM;
			default:
				return PricerMeasure.toInt(str);
		}
	}

	public static String toString(final int i) {
		switch (i) {
			case INDEPENDENT_AMOUNT_BASE:
				return S_INDEPENDENT_AMOUNT_BASE;
			case NPV_BASE:
				return S_NPV_BASE;
			case NPV_LEG1:
				return S_NPV_LEG1;
			case NPV_LEG2:
				return S_NPV_LEG2;
			case CLOSING_PRICE:
				return S_CLOSING_PRICE;
			case REPO_ACCRUED_INTEREST:
				return S_REPO_ACCRUED_INTEREST;
			case BOND_ACCRUED_INTEREST:
				return S_BOND_ACCRUED_INTEREST;
			case CLEAN_PRICE:
				return S_CLEAN_PRICE;
			case CAPITAL_FACTOR:
				return S_CAPITAL_FACTOR;
			case VARIATION_MARGIN_DM:
				return S_VARIATION_MARGIN_DM;
			case NPV_LEG_1_DM:
				return S_NPV_LEG_1_DM;
			case NPV_LEG_2_DM:
				return S_NPV_LEG_2_DM;
			case NPV_DM:
				return S_NPV_DM;
			case NPV_BASE_DM:
				return S_NPV_BASE_DM;
			case INDEPENDENT_AMOUNT_DM:
				return S_INDEPENDENT_AMOUNT_DM;
			case INDEPENDENT_AMOUNT_BASE_DM:
				return S_INDEPENDENT_AMOUNT_BASE_DM;
			case IA_SETTLEMENT_DM:
				return S_IA_SETTLEMENT_DM;
			case MARGIN_CALL_DM:
				return S_MARGIN_CALL_DM;
			default:
				return PricerMeasure.toString(i);
		}
	}

}
