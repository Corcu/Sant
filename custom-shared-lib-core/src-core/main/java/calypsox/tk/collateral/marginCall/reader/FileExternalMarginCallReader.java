package calypsox.tk.collateral.marginCall.reader;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import calypsox.tk.collateral.marginCall.bean.ExternalMarginCallBean;
import calypsox.tk.collateral.marginCall.bean.MarginCallImportErrorBean;
import calypsox.tk.collateral.marginCall.importer.ExternalMarginCallImportConstants;
import calypsox.tk.collateral.marginCall.importer.ExternalMarginCallImportContext;
import calypsox.tk.collateral.pdv.importer.PDVUtil;
import calypsox.tk.collateral.pdv.importer.PDVUtil.EnumMessageType;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;

public class FileExternalMarginCallReader extends
		AbstractExternalMarginCallReader {

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat(
			"dd/MM/yyyy");

	/**
	 * @param is
	 * @param context
	 */
	public FileExternalMarginCallReader(InputStream is,
			ExternalMarginCallImportContext context) {
		super(is, context);
	}

	@Override
	public ExternalMarginCallBean readMarginCall(String line,
			List<MarginCallImportErrorBean> errors) throws Exception {

		ExternalMarginCallBean mcBean = new ExternalMarginCallBean();

		try {
			List<String> fields = getFields();
			
			Map<String, String> values = PDVUtil.getFieldValues(
					EnumMessageType.COLLAT_MESSAGE, line, fields);
			
			//Checking the Contracts:
			
			
			// set pdv Message Content
			mcBean.setPdvMessageContent(line);

			// 1 set the action
			mcBean.setAction(values.get(PDVUtil.COLLAT_ACTION_FIELD));

			// 2 3 4 start by putting the trade references.
			mcBean.addAttribute(PDVUtil.COLLAT_FO_SYSTEM_FIELD,
					values.get(PDVUtil.COLLAT_FO_SYSTEM_FIELD));
			mcBean.addAttribute(PDVUtil.COLLAT_NUM_FRONT_ID_FIELD,
					values.get(PDVUtil.COLLAT_NUM_FRONT_ID_FIELD));
			mcBean.addAttribute(PDVUtil.COLLAT_COLLAT_ID_FIELD,
					values.get(PDVUtil.COLLAT_COLLAT_ID_FIELD));

			// 4 set the collat_Id (external ID)
			mcBean.setCollatId(values.get(PDVUtil.COLLAT_COLLAT_ID_FIELD));

			// 5 set the owner -- Not necessary

			// 6 set the counterparty
			mcBean.setCounterparty(values
					.get(PDVUtil.COLLAT_COUNTERPARTY_FIELD));

			// 7 set the Instrument (Collateral type)
			mcBean.setInstrument(values.get(PDVUtil.COLLAT_INSTRUMENT_FIELD));

			// 8 set the Portfolio (collateral book)
			mcBean.setPortfolio(values.get(PDVUtil.COLLAT_PORTFOLIO_FIELD));

			// 9 set the value_date (settlementDate)
			String valDate = values.get(PDVUtil.COLLAT_VALUE_DATE_FIELD);
			try {
				mcBean.setValueDate(dateFormat.parse(valDate));
			} catch (Exception e) {
				MarginCallImportErrorBean errorBean = new MarginCallImportErrorBean(
						ExternalMarginCallImportConstants.ERR_UNABLE_TO_READ_ALLOCATION,
						"Unable to read the value "
								+ (valDate == null ? "null" : valDate) + ". "
								+ line, mcBean);
				errors.add(errorBean);
				Log.error(this, e); // sonar
				return null;
			}

			// 10 set the trade_date
			String tradeDate = values.get(PDVUtil.COLLAT_TRADE_DATE_FIELD);
			try {
				mcBean.setTradeDate(dateFormat.parse(tradeDate));
			} catch (Exception e) {
				MarginCallImportErrorBean errorBean = new MarginCallImportErrorBean(
						ExternalMarginCallImportConstants.ERR_UNABLE_TO_READ_ALLOCATION,
						"Unable to read the value "
								+ (tradeDate == null ? "null" : tradeDate)
								+ ". " + line, mcBean);
				errors.add(errorBean);
				Log.error(this, e); // sonar
				return null;
			}
			
			// 11 the the collateral direction
			mcBean.setCollateralDirection(values
					.get(PDVUtil.COLLAT_DIRECTION_FIELD));

			// 12 set the amount (assetAmount)
			try {
				mcBean.setAmount(readDouble(values
						.get(PDVUtil.COLLAT_AMOUNT_FIELD)));
			} catch (Exception e) {
				MarginCallImportErrorBean errorBean = new MarginCallImportErrorBean(
						ExternalMarginCallImportConstants.ERR_UNABLE_TO_READ_ALLOCATION,
						"Unable to read the value "
								+ (values.get(PDVUtil.COLLAT_AMOUNT_FIELD) == null ? "null"
										: values.get(PDVUtil.COLLAT_AMOUNT_FIELD))
								+ ". " + line, mcBean);
				errors.add(errorBean);
				Log.error(this, e); // sonar
				return null;
			}

			// 13 set the amount_ccy (assetCurrency)
			mcBean.setAmountCcy(values.get(PDVUtil.COLLAT_AMOUNT_CCY_FIELD));

			// 14 set the underlying_type
			mcBean.setUnderlyingType(values
					.get(PDVUtil.COLLAT_UNDERLYING_TYPE_FIELD));

			// 15 set the underlying
			mcBean.setUnderlying(values.get(PDVUtil.COLLAT_UNDERLYING_FIELD));

			// 16 set the closing_price
			mcBean.setClosingPrice(values
					.get(PDVUtil.COLLAT_CLOSING_PRICE_FIELD));
			
			//17 set the SLB_BUNDLE/SLB_MUREX
			if(!Util.isEmpty(values.get(PDVUtil.SLB_BUNDLE_FIELD))) {
				mcBean.setSLB_BUNDLE(values
						.get(PDVUtil.SLB_BUNDLE_FIELD));
			}
			

		} catch (Exception e) {
			Log.error(this, e);
			MarginCallImportErrorBean errorBean = new MarginCallImportErrorBean(
					ExternalMarginCallImportConstants.ERR_UNABLE_TO_READ_ALLOCATION,
					"Unexpected error while rading the message: "
							+ e.getMessage() + " . " + line, mcBean);
			errors.add(errorBean);
			return null;
		}

		return mcBean;
	}

	/**
	 * Get the PDV fields
	 * 
	 * @return
	 */
	private List<String> getFields() {
		List<String> fields = Arrays.asList(PDVUtil.COLLAT_ACTION_FIELD,
				PDVUtil.COLLAT_FO_SYSTEM_FIELD,
				PDVUtil.COLLAT_NUM_FRONT_ID_FIELD,
				PDVUtil.COLLAT_COLLAT_ID_FIELD, PDVUtil.COLLAT_OWNER_FIELD,
				PDVUtil.COLLAT_COUNTERPARTY_FIELD,
				PDVUtil.COLLAT_INSTRUMENT_FIELD,
				PDVUtil.COLLAT_PORTFOLIO_FIELD,
				PDVUtil.COLLAT_VALUE_DATE_FIELD,
				PDVUtil.COLLAT_TRADE_DATE_FIELD,
				PDVUtil.COLLAT_DIRECTION_FIELD, PDVUtil.COLLAT_AMOUNT_FIELD,
				PDVUtil.COLLAT_AMOUNT_CCY_FIELD,
				PDVUtil.COLLAT_UNDERLYING_TYPE_FIELD,
				PDVUtil.COLLAT_UNDERLYING_FIELD,
				PDVUtil.COLLAT_CLOSING_PRICE_FIELD,
				PDVUtil.SLB_BUNDLE_FIELD);
				
		return fields;
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

}
