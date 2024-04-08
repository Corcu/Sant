/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
/**
 * 
 */
package calypsox.tk.collateral.allocation.reader;

import java.io.InputStream;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import calypsox.tk.collateral.allocation.bean.AllocImportErrorBean;
import calypsox.tk.collateral.allocation.bean.CashExternalAllocationBean;
import calypsox.tk.collateral.allocation.bean.ExternalAllocationBean;
import calypsox.tk.collateral.allocation.bean.SecurityExternalAllocationBean;
import calypsox.tk.collateral.allocation.importer.ExternalAllocationImportConstants;
import calypsox.tk.collateral.allocation.importer.ExternalAllocationImportContext;
import calypsox.tk.collateral.pdv.importer.PDVUtil;
import calypsox.tk.collateral.pdv.importer.PDVUtil.EnumMessageType;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;

/**
 * @author aela
 * 
 */
public class StringExternalAllocationReader extends
		AbstractExternalAllocationReader {

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat(
			"dd/MM/yyyy");

	/**
	 * @param is
	 * @param context
	 */
	public StringExternalAllocationReader(InputStream is,
			ExternalAllocationImportContext context) {
		super(is, context);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * calypsox.tk.collateral.allocation.reader.AbstractExternalAllocationReader
	 * #readAllocation(java.lang.String, java.util.List)
	 */
	@Override
	public ExternalAllocationBean readAllocation(String message,
			List<AllocImportErrorBean> errors) throws Exception {
		return stringToAllocBean(message, errors);
	}

	/**
	 * from a flat line builds the bean containing the exposure trade.
	 */
	public ExternalAllocationBean stringToAllocBean(String message,
			List<AllocImportErrorBean> errors) throws Exception {
		ExternalAllocationBean alloc = null;
		// String[] values = message.split("\\" + this.context.getRowSpliter(),
		// -1);

		// private String contractName;
		// private double assetAmount;
		// private Date settlementDate;
		// private String collateralBook;
		// private String collateralType;
		// private String underlyingType;
		// private String assetCurrency;
		// private Double contractValue;
		try {

			List<String> fields = Arrays.asList(PDVUtil.COLLAT_ACTION_FIELD,
					PDVUtil.COLLAT_FO_SYSTEM_FIELD,
					PDVUtil.COLLAT_NUM_FRONT_ID_FIELD,
					PDVUtil.COLLAT_INSTRUMENT_FIELD,
					PDVUtil.COLLAT_PORTFOLIO_FIELD,
					PDVUtil.COLLAT_VALUE_DATE_FIELD,
					PDVUtil.COLLAT_AMOUNT_FIELD, 
					PDVUtil.COLLAT_AMOUNT_CCY_FIELD,
					PDVUtil.COLLAT_UNDERLYING_FIELD,
					PDVUtil.COLLAT_UNDERLYING_TYPE_FIELD,
					PDVUtil.COLLAT_DIRECTION_FIELD,
					//PDVUtil.COLLAT_IS_FINANCEMENT_FIELD,
					PDVUtil.COLLAT_CLOSING_PRICE_FIELD,
					PDVUtil.COLLAT_COLLAT_ID_FIELD);

			Map<String, String> values = PDVUtil.getFieldValues(
					EnumMessageType.COLLAT_MESSAGE, message, fields);
			if ("COLLAT_CASH".equals(values
					.get(PDVUtil.COLLAT_INSTRUMENT_FIELD))) {
				alloc = new CashExternalAllocationBean();
				alloc.setCollateralType(values
						.get(PDVUtil.COLLAT_INSTRUMENT_FIELD));
			}
			else if ("COLLAT_SECURITY".equals(values
					.get(PDVUtil.COLLAT_INSTRUMENT_FIELD))) {
				SecurityExternalAllocationBean secAlloc = new SecurityExternalAllocationBean();
				secAlloc.setAssetISIN(values
						.get(PDVUtil.COLLAT_UNDERLYING_FIELD));
				secAlloc.setCollateralType(values
						.get(PDVUtil.COLLAT_INSTRUMENT_FIELD));
				//

				try {
					secAlloc.setAssetPrice(readDouble(values
							.get(PDVUtil.COLLAT_CLOSING_PRICE_FIELD)));
				}
				catch (Exception e) {
					AllocImportErrorBean errorBean = new AllocImportErrorBean(
							ExternalAllocationImportConstants.ERR_UNABLE_TO_READ_ALLOCATION,
							"Unable to read the value "
									+ (values.get(PDVUtil.COLLAT_AMOUNT_FIELD) == null ? "null"
											: values.get(PDVUtil.COLLAT_AMOUNT_FIELD))
									+ ". " + message, alloc);
					errors.add(errorBean);
					Log.error(this, e); //sonar
					return null;
				}
				//
				alloc = secAlloc;

			}
			//set pdv Message Content
			alloc.setPdvMessageContent(message);
			// set the external ID
			alloc.setExternalId(values
						.get(PDVUtil.COLLAT_COLLAT_ID_FIELD));
			// set the action
			alloc.setAction(values
					.get(PDVUtil.COLLAT_ACTION_FIELD));		
			// the the collateral dierction
			alloc.setCollateralDirection(values
					.get(PDVUtil.COLLAT_DIRECTION_FIELD));		
			

			// start by putting the trade references.
			alloc.addAttribute(PDVUtil.COLLAT_FO_SYSTEM_FIELD,
					values.get(PDVUtil.COLLAT_FO_SYSTEM_FIELD));
			alloc.addAttribute(PDVUtil.COLLAT_NUM_FRONT_ID_FIELD,
					values.get(PDVUtil.COLLAT_NUM_FRONT_ID_FIELD));

			String settlmentDate = values.get(PDVUtil.COLLAT_VALUE_DATE_FIELD);
			try {
				alloc.setSettlementDate(dateFormat.parse(settlmentDate));
			}
			catch (Exception e) {
				AllocImportErrorBean errorBean = new AllocImportErrorBean(
						ExternalAllocationImportConstants.ERR_UNABLE_TO_READ_ALLOCATION,
						"Unable to read the value "
								+ (settlmentDate == null ? "null"
										: settlmentDate) + ". " + message,
						alloc);
				errors.add(errorBean);
				Log.error(this, e); //sonar
				return null;
			}

			alloc.setCollateralBook(values.get(PDVUtil.COLLAT_PORTFOLIO_FIELD));
			alloc.setAssetCurrency(values.get(PDVUtil.COLLAT_AMOUNT_CCY_FIELD));
			try {
				alloc.setAssetAmount(readDouble(values
						.get(PDVUtil.COLLAT_AMOUNT_FIELD)));
			}
			catch (Exception e) {
				AllocImportErrorBean errorBean = new AllocImportErrorBean(
						ExternalAllocationImportConstants.ERR_UNABLE_TO_READ_ALLOCATION,
						"Unable to read the value "
								+ (values.get(PDVUtil.COLLAT_AMOUNT_FIELD) == null ? "null"
										: values.get(PDVUtil.COLLAT_AMOUNT_FIELD))
								+ ". " + message, alloc);
				errors.add(errorBean);
				Log.error(this, e); //sonar
				return null;
			}

			// set the allocation attributes
			/*alloc.addAttribute(PDVUtil.COLLAT_IS_FINANCEMENT_FIELD,
					values.get(PDVUtil.COLLAT_IS_FINANCEMENT_FIELD));
			alloc.addAttribute(PDVUtil.COLLAT_DELIVERY_TYPE_FIELD,
					values.get(PDVUtil.COLLAT_DELIVERY_TYPE_FIELD));
					*/
			alloc.addAttribute(PDVUtil.COLLAT_COLLAT_ID_FIELD,
					values.get(PDVUtil.COLLAT_COLLAT_ID_FIELD));

		}
		catch (Exception e) {
			Log.error(this, e);
			AllocImportErrorBean errorBean = new AllocImportErrorBean(
					ExternalAllocationImportConstants.ERR_UNABLE_TO_READ_ALLOCATION,
					"Unexpected error while rading the message: "
							+ e.getMessage() + " . " + message, alloc);
			errors.add(errorBean);
			return null;
		}

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
}
