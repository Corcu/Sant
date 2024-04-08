/* Actualizado por David Porras Mart?nez 23-11-11 */

package calypsox.tk.report;

import calypsox.util.collateral.SantCollateralConfigUtil;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.CollateralConfigReportStyle;
import com.calypso.tk.report.ReportRow;

import java.util.Locale;
import java.util.Vector;

@SuppressWarnings("serial")
public class ELBEAgreementsExtractionReportStyle extends CollateralConfigReportStyle {

	private static final String COD_LAYOUT = "CodLayout";
	private static final String EXTRACT_DATE = "ExtractDate";
	private static final String POS_TRANS_DATE = "PosTransDate";
	private static final String SOURCE_APP = "SourceApplication";
	private static final String VAL_AGENT = "ValuationAgent";
	private static final String OWNER = "Owner";
	private static final String COL_AGREEMENT = "ColAgreement";
	private static final String SHORTNAME = "Shortname";
	private static final String BASE_CCY = "BaseCCY";
	private static final String FIXING = "Fixing";
	private static final String CONT_TYPE = "ContractType";
	private static final String ACT_VAL = "ActiveValues";
	private static final String ACT_RAT = "ActiveRating";
	private static final String THRES_ACT_CCY = "ThresholdActiveCCY";
	private static final String THRES_ACT_EUR = "ThresholdActiveEUR";
	private static final String BALANCE_CCY = "BalanceCCY";
	private static final String BALANCE_EUR = "BalanceEUR";
	private static final String IA_ACT_CCY = "IndependentAmountActiveCCY";
	private static final String IA_ACT_EUR = "IndependentAmountActiveEUR";
	private static final String RAT_DOWN_1 = "RatingDown1 notch";
	private static final String THRES_ACT_DOWN_1_CCY = "ThresholdActiveDown1notchCCY";
	private static final String THRES_ACT_DOWN_1_EUR = "ThresholdActiveDown1notchEUR";
	private static final String IMP_ACT_DOWN_1_CCY = "ImpactDown1notchCCY";
	private static final String IMP_ACT_DOWN_1_EUR = "ImpactDown1notchEUR";
	private static final String IA_DOWN_1_CCY = "IndependentAmountDown1notchCCY";
	private static final String IA_DOWN_1_EUR = "IndependentAmountDown1notchEUR";
	private static final String RAT_DOWN_2 = "RatingDown2notch";
	private static final String THRES_ACT_DOWN_2_CCY = "ThresholdActiveDown2notchCCY";
	private static final String THRES_ACT_DOWN_2_EUR = "ThresholdActiveDown2notchEUR";
	private static final String IMP_ACT_DOWN_2_CCY = "ImpactDown2notchCCY";
	private static final String IMP_ACT_DOWN_2_EUR = "ImpactDown2notchEUR";
	private static final String IA_DOWN_2_CCY = "IndependentAmountDown2notchCCY";
	private static final String IA_DOWN_2_EUR = "IndependentAmountDown2notchEUR";
	private static final String RAT_DOWN_3 = "RatingDown3notch";
	private static final String THRES_ACT_DOWN_3_CCY = "ThresholdActiveDown3notchCCY";
	private static final String THRES_ACT_DOWN_3_EUR = "ThresholdActiveDown3notchEUR";
	private static final String IMP_ACT_DOWN_3_CCY = "ImpactDown3notchCCY";
	private static final String IMP_ACT_DOWN_3_EUR = "ImpactDown3notchEUR";
	private static final String IA_DOWN_3_CCY = "IndependentAmountDown3notchCCY";
	private static final String IA_DOWN_3_EUR = "IndependentAmountDown3notchEUR";
	private static final String HAIRCUT = "Haircut";
	private static final String DS_SEC = "DSecurity";
	private static final String SIGN = "Sign";
	private static final String BAL_CASH_CCY = "BalanceCashCCY";
	private static final String BAL_CASH_EUR = "BalanceCashEUR";
	private static final String BAL_STOCK_CCY = "BalanceStockCCY";
	private static final String BAL_STOCK_EUR = "BalanceStockEUR";
	private static final String STATUS = "Status";
	private static final String EVENT = "Event";
	private static final String GROSS_EXP_CCY = "GrossExposureCCY";
	private static final String GROSS_EXP_EUR = "GrossExposureEUR";
	private static final String MC_CCY = "MarginCallCCY";
	private static final String IA_ACTIVE_VALUES = "ActiveValues Independent Amount";

	// SBWO
	private static final String GROSS_EXPOSURE = "GrossExposure";
	private static final String THRES_ACT_DOWN_1 = "ThresholdDown1notch";
	private static final String THRES_ACT_DOWN_2 = "ThresholdDown2notch";
	private static final String THRES_ACT_DOWN_3 = "ThresholdDown3notch";
	private static final String MTA_ACT_DOWN_1 = "MTADown1notch";
	private static final String MTA_ACT_DOWN_2 = "MTADown2notch";
	private static final String MTA_ACT_DOWN_3 = "MTADown3notch";

	// Default columns.
	protected static final String[] DEFAULTS_COLUMNS = { COD_LAYOUT, EXTRACT_DATE, POS_TRANS_DATE, SOURCE_APP, VAL_AGENT,
			OWNER, COL_AGREEMENT, SHORTNAME, BASE_CCY, FIXING, CONT_TYPE, ACT_VAL, ACT_RAT, THRES_ACT_CCY,
			THRES_ACT_EUR, BALANCE_CCY, BALANCE_EUR, IA_ACT_CCY, IA_ACT_EUR, RAT_DOWN_1, THRES_ACT_DOWN_1_CCY,
			THRES_ACT_DOWN_1_EUR, IMP_ACT_DOWN_1_CCY, IMP_ACT_DOWN_1_EUR, IA_DOWN_1_CCY, IA_DOWN_1_EUR, RAT_DOWN_2,
			THRES_ACT_DOWN_2_CCY, THRES_ACT_DOWN_2_EUR, IMP_ACT_DOWN_2_CCY, IMP_ACT_DOWN_2_EUR, IA_DOWN_2_CCY,
			IA_DOWN_2_EUR, RAT_DOWN_3, THRES_ACT_DOWN_3_CCY, THRES_ACT_DOWN_3_EUR, IMP_ACT_DOWN_3_CCY,
			IMP_ACT_DOWN_3_EUR, IA_DOWN_3_CCY, IA_DOWN_3_EUR, HAIRCUT, DS_SEC, SIGN, BAL_CASH_CCY, BAL_CASH_EUR,
			BAL_STOCK_CCY, BAL_STOCK_EUR, STATUS, EVENT, GROSS_EXP_CCY, GROSS_EXP_EUR, MC_CCY, IA_ACTIVE_VALUES };

	@Override
	public TreeList getTreeList() {
		final TreeList treeList = super.getTreeList();
		treeList.add(COD_LAYOUT);
		treeList.add(EXTRACT_DATE);
		treeList.add(POS_TRANS_DATE);
		treeList.add(SOURCE_APP);
		treeList.add(VAL_AGENT);
		treeList.add(OWNER);
		treeList.add(COL_AGREEMENT);
		treeList.add(SHORTNAME);
		treeList.add(BASE_CCY);
		treeList.add(FIXING);
		treeList.add(CONT_TYPE);
		treeList.add(ACT_VAL);
		treeList.add(ACT_RAT);
		treeList.add(THRES_ACT_CCY);
		treeList.add(THRES_ACT_EUR);
		treeList.add(BALANCE_CCY);
		treeList.add(BALANCE_EUR);
		treeList.add(IA_ACT_CCY);
		treeList.add(IA_ACT_EUR);
		treeList.add(RAT_DOWN_1);
		treeList.add(THRES_ACT_DOWN_1_CCY);
		treeList.add(THRES_ACT_DOWN_1_EUR);
		treeList.add(IMP_ACT_DOWN_1_CCY);
		treeList.add(IMP_ACT_DOWN_1_EUR);
		treeList.add(IA_DOWN_1_CCY);
		treeList.add(IA_DOWN_1_EUR);
		treeList.add(RAT_DOWN_2);
		treeList.add(THRES_ACT_DOWN_2_CCY);
		treeList.add(THRES_ACT_DOWN_2_EUR);
		treeList.add(IMP_ACT_DOWN_2_CCY);
		treeList.add(IMP_ACT_DOWN_2_EUR);
		treeList.add(IA_DOWN_2_CCY);
		treeList.add(IA_DOWN_2_EUR);
		treeList.add(RAT_DOWN_3);
		treeList.add(THRES_ACT_DOWN_3_CCY);
		treeList.add(THRES_ACT_DOWN_3_EUR);
		treeList.add(IMP_ACT_DOWN_3_CCY);
		treeList.add(IMP_ACT_DOWN_3_EUR);
		treeList.add(IA_DOWN_3_CCY);
		treeList.add(IA_DOWN_3_EUR);
		treeList.add(HAIRCUT);
		treeList.add(DS_SEC);
		treeList.add(SIGN);
		treeList.add(BAL_CASH_CCY);
		treeList.add(BAL_CASH_EUR);
		treeList.add(BAL_STOCK_CCY);
		treeList.add(BAL_STOCK_EUR);
		treeList.add(STATUS);
		treeList.add(EVENT);
		treeList.add(GROSS_EXP_CCY);
		treeList.add(GROSS_EXP_EUR);
		treeList.add(MC_CCY);
		treeList.add(IA_ACTIVE_VALUES);

		// SBWO
		treeList.add(GROSS_EXPOSURE);
		treeList.add(THRES_ACT_DOWN_1);
		treeList.add(THRES_ACT_DOWN_2);
		treeList.add(THRES_ACT_DOWN_3);
		treeList.add(MTA_ACT_DOWN_1);
		treeList.add(MTA_ACT_DOWN_2);
		treeList.add(MTA_ACT_DOWN_3);

		return treeList;
	}

	@Override
	public Object getColumnValue(final ReportRow row, final String columnName,
			@SuppressWarnings("rawtypes") final Vector errors) {
		final ELBEAgreementsExtractionItem item = row.getProperty(ELBEAgreementsExtractionItem.ELBE_AGREE_EXT_ITEM);

		if (columnName.compareTo(COD_LAYOUT) == 0) {
			return item.getCodLayout();
		} else if (columnName.compareTo(EXTRACT_DATE) == 0) {
			return item.getExtractDate();
		} else if (columnName.compareTo(POS_TRANS_DATE) == 0) {
			return item.getPosTransDate();
		} else if (columnName.compareTo(SOURCE_APP) == 0) {
			return item.getSourceApp();
		} else if (columnName.compareTo(VAL_AGENT) == 0) {
			return item.getValAgent();
		} else if (columnName.compareTo(OWNER) == 0) {
			return item.getOwner();
		} else if (columnName.compareTo(COL_AGREEMENT) == 0) {
			return item.getColAgreement();
		} else if (columnName.compareTo(SHORTNAME) == 0) {
			return item.getShortname();
		} else if (columnName.compareTo(BASE_CCY) == 0) {
			return item.getBaseCCY();
		} else if (columnName.compareTo(FIXING) == 0) {
			return item.getFixing();
		} else if (columnName.compareTo(CONT_TYPE) == 0) {
			return item.getContractType();
		} else if (columnName.compareTo(ACT_VAL) == 0) {
			return item.getActiveValues();
		} else if (columnName.compareTo(ACT_RAT) == 0) {
			return item.getActiveRating();
		} else if (columnName.compareTo(THRES_ACT_CCY) == 0) {
			return item.getThresholdActiveCCY();
		} else if (columnName.compareTo(THRES_ACT_EUR) == 0) {
			return item.getThresholdActiveEUR();
		} else if (columnName.compareTo(BALANCE_CCY) == 0) {
			return item.getBalanceCCY();
		} else if (columnName.compareTo(BALANCE_EUR) == 0) {
			return item.getBalanceEUR();
		} else if (columnName.compareTo(IA_ACT_CCY) == 0) {
			return item.getIAActiveCCY();
		} else if (columnName.compareTo(IA_ACT_EUR) == 0) {
			return item.getIAActiveEUR();
		} else if (columnName.compareTo(RAT_DOWN_1) == 0) {
			return item.getRatDown1notch();
		} else if (columnName.compareTo(THRES_ACT_DOWN_1_CCY) == 0) {
			return item.getThresholdActiveDown1notchCCY();
		} else if (columnName.compareTo(THRES_ACT_DOWN_1_EUR) == 0) {
			return item.getThresholdActiveDown1notchEUR();
		} else if (columnName.compareTo(IMP_ACT_DOWN_1_CCY) == 0) {
			return item.getImpactDown1notchCCY();
		} else if (columnName.compareTo(IMP_ACT_DOWN_1_EUR) == 0) {
			return item.getImpactDown1notchEUR();
		} else if (columnName.compareTo(IA_DOWN_1_CCY) == 0) {
			return item.getIADown1notchCCY();
		} else if (columnName.compareTo(IA_DOWN_1_EUR) == 0) {
			return item.getIADown1notchEUR();
		} else if (columnName.compareTo(RAT_DOWN_2) == 0) {
			return item.getRatDown2notch();
		} else if (columnName.compareTo(THRES_ACT_DOWN_2_CCY) == 0) {
			return item.getThresholdActiveDown2notchCCY();
		} else if (columnName.compareTo(THRES_ACT_DOWN_2_EUR) == 0) {
			return item.getThresholdActiveDown2notchEUR();
		} else if (columnName.compareTo(IMP_ACT_DOWN_2_CCY) == 0) {
			return item.getImpactDown2notchCCY();
		} else if (columnName.compareTo(IMP_ACT_DOWN_2_EUR) == 0) {
			return item.getImpactDown2notchEUR();
		} else if (columnName.compareTo(IA_DOWN_2_CCY) == 0) {
			return item.getIADown2notchCCY();
		} else if (columnName.compareTo(IA_DOWN_2_EUR) == 0) {
			return item.getIADown2notchEUR();
		} else if (columnName.compareTo(RAT_DOWN_3) == 0) {
			return item.getRatDown3notch();
		} else if (columnName.compareTo(THRES_ACT_DOWN_3_CCY) == 0) {
			return item.getThresholdActiveDown3notchCCY();
		} else if (columnName.compareTo(THRES_ACT_DOWN_3_EUR) == 0) {
			return item.getThresholdActiveDown3notchEUR();
		} else if (columnName.compareTo(IMP_ACT_DOWN_3_CCY) == 0) {
			return item.getImpactDown3notchCCY();
		} else if (columnName.compareTo(IMP_ACT_DOWN_3_EUR) == 0) {
			return item.getImpactDown3notchEUR();
		} else if (columnName.compareTo(IA_DOWN_3_CCY) == 0) {
			return item.getIADown3notchCCY();
		} else if (columnName.compareTo(IA_DOWN_3_EUR) == 0) {
			return item.getIADown3notchEUR();
		} else if (columnName.compareTo(HAIRCUT) == 0) {
			return item.getHaircut();
		} else if (columnName.compareTo(DS_SEC) == 0) {
			return item.getDSecurity();
		} else if (columnName.compareTo(SIGN) == 0) {
			return item.getSign();
		} else if (columnName.compareTo(BAL_CASH_CCY) == 0) {
			return item.getBalanceCashCCY();
		} else if (columnName.compareTo(BAL_CASH_EUR) == 0) {
			return item.getBalanceCashEUR();
		} else if (columnName.compareTo(BAL_STOCK_CCY) == 0) {
			return item.getBalanceStockCCY();
		} else if (columnName.compareTo(BAL_STOCK_EUR) == 0) {
			return item.getBalanceStockEUR();
		} else if (columnName.compareTo(STATUS) == 0) {
			return item.getStatus();
		} else if (columnName.compareTo(EVENT) == 0) {
			return item.getEvent();
		} else if (columnName.compareTo(GROSS_EXP_CCY) == 0) { // added
			return item.getGrossExposureCCY();
		} else if (columnName.compareTo(GROSS_EXP_EUR) == 0) { // added
			return item.getGrossExposureEUR();
		} else if (columnName.compareTo(MC_CCY) == 0) { // added
			return item.getMarginCallCCY();
		} else if (columnName.compareTo(IA_ACTIVE_VALUES) == 0) { // added
			return item.getIaActiveValues();
		} else if (GROSS_EXPOSURE.equals(columnName)) {
			return formatNumber(item.getGrossExposure());
		} else if (THRES_ACT_DOWN_1.equals(columnName)) {
			return formatNumber(item.getThresholdDown1notch());
		} else if (THRES_ACT_DOWN_2.equals(columnName)) {
			return formatNumber(item.getThresholdDown2notch());
		} else if (THRES_ACT_DOWN_3.equals(columnName)) {
			return formatNumber(item.getThresholdDown3notch());
		} else if (MTA_ACT_DOWN_1.equals(columnName)) {
			return formatNumber(item.getMTADown1notch());
		} else if (MTA_ACT_DOWN_2.equals(columnName)) {
			return formatNumber(item.getMTADown2notch());
		} else if (MTA_ACT_DOWN_3.equals(columnName)) {
			return formatNumber(item.getMTADown3notch());
		} else if (CollateralConfigReportStyle.CONTRACT_DIRECTION.equals(columnName)) {
			CollateralConfig config = row.getProperty("MarginCallConfig");
			return SantCollateralConfigUtil.getContractDirectionV14Value(config);
		} else {
			return super.getColumnValue(row, columnName, errors);
		}
	}

	private static String formatNumber(final double number) {
		return Util.numberToString(number, 2, Locale.getDefault(), true);
	}
}