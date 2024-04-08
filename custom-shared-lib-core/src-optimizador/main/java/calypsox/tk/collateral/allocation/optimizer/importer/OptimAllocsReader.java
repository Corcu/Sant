package calypsox.tk.collateral.allocation.optimizer.importer;

import java.text.SimpleDateFormat;

import calypsox.tk.collateral.allocation.optimizer.importer.beans.OptimAllocationBean;
import calypsox.tk.collateral.allocation.optimizer.importer.beans.OptimCashAllocationBean;
import calypsox.tk.collateral.allocation.optimizer.importer.beans.OptimSecurityAllocationBean;

import com.calypso.infra.util.Util;

/**
 * Read from the flat file containing the exposures trades (line per line) and
 * builds the bean containing the exposure trade.
 * 
 * @author aela
 * @version 3.1
 * @date 22/08/2013
 * 
 */
class OptimAllocsReader implements Reader<OptimAllocationBean> {
	/* variable */
	private final OptimAllocsImportContext context;

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat(
			"dd/MM/yyyy");

	/**
	 * Constructor
	 * 
	 * @param context
	 */
	public OptimAllocsReader(OptimAllocsImportContext context) {
		this.context = context;
	}

	/**
	 * from a flat line builds the bean containing the exposure trade.
	 */
	@Override
	public OptimAllocationBean readLine(String record) throws Exception {
		OptimAllocationBean alloc = null;
		String[] values = record.split("\\" + this.context.getRowSpliter(), -1);

		int i = 0;
		if ("Cash".equals(values[6])) {
			alloc = new OptimCashAllocationBean();
		}
		else if ("Security".equals(values[6])) {
			alloc = new OptimSecurityAllocationBean();
		}
		// alloc = new ExternalAllocationBean();
		alloc.setCollateralOwner(values[i++]);
		alloc.setContractName(values[i++]);
		alloc.setSettlementDate(dateFormat.parse(values[i++]));
		alloc.setNbCtrAllocs(readInt(values[i++]));
		alloc.setCollateralBook(values[i++]);
		alloc.setCollateralType(values[i++]);
		alloc.setUnderlyingType(values[i++]);
		alloc.setAssetOwner(values[i++]);
		alloc.setAssetISIN(values[i++]);
		alloc.setAssetCurrency(values[i++]);
		alloc.setAssetAmount(readDouble(values[i++]));
		alloc.setAssetPrice(readDouble(values[i++]));
		alloc.setAssetHaircut(readDouble(values[i++]));
		alloc.setContractValue(readDouble(values[i++]));
		alloc.setCollateralCost(readDouble(values[i++]));
		alloc.setAssetRanking(readDouble(values[i++]));
		return alloc;
	}

	/**
	 * @param value
	 * @return
	 */
	Double readDouble(String value) {
		if (Util.isEmpty(value)) {
			return null;
		}
		return Double.parseDouble(value);
	}

	/**
	 * @param value
	 * @return
	 */
	int readInt(String value) {
		if (Util.isEmpty(value)) {
			return 0;
		}
		return Integer.parseInt(value);
	}

	// public void populateAllocation(String[] values) {
	// Class<OptimAllocationBean> allocBean = OptimAllocationBean.class;
	// Annotation annotation = allocBean.getAnnotation(FileToObject.class);
	// FileToObject testerInfo = (FileToObject) annotation;
	//
	// System.out.printf("%nPriority :%s", testerInfo.priority());
	// System.out.printf("%nCreatedBy :%s", testerInfo.createdBy());
	// System.out.printf("%nTags :");
	// annotation.g
	// int tagLength = testerInfo.tags().length;
	// for (String tag : testerInfo.tags()) {
	// if (tagLength > 1) {
	// System.out.print(tag + ", ");
	// } else {
	// System.out.print(tag);
	// }
	// tagLength--;
	// }
	// }
}