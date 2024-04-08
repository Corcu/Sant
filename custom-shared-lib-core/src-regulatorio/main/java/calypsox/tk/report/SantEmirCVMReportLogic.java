/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import calypsox.regulation.util.SantEmirUtil;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

/**
 * SantEmirCVMReportLogic add the logic needed in Collateral Value Message for
 * EMIR report.
 * 
 * @author xIS16241
 * 
 */
public class SantEmirCVMReportLogic {

	/**
	 * Margin call entry dto.
	 */
	protected MarginCallEntryDTO mce_dto;

	/**
	 * collateral config.
	 */
	protected CollateralConfig col_conf;

	/**
	 * Valuation date of the report.
	 */
	protected JDatetime valDate;

	/**
	 * Legal Entity BSTE.
	 */
	protected static LegalEntity leBSTE;

	/**
	 * Holidays.
	 */
	@SuppressWarnings("rawtypes")
	protected Vector holidays;

	/**
	 * cache clone contracts.
	 */
	protected SantEmirCacheCloneContracts[] cacheCloneContracts;

	/**
	 * Literal NULL.
	 */
	private static final String NULL = null;

	/**
	 * Literal DTCC.
	 */
	private static final String DTCC = "DTCC";

	/**
	 * Literal LEI.
	 */
	private static final String LEI = "LEI";

	/**
	 * Literal ESMA.
	 */
	private static final String ESMA = "ESMA";

	/**
	 * Literal DTCCEU.
	 */
	private static final String DTCCEU = "DTCCEU";

	/**
	 * Literal DTCCEU.
	 */
	@SuppressWarnings("unused")
	private static final String LEIEU = "LEIEU";
	/**
	 * Literal version.
	 */
	private static final String version = "Coll1.0";

	/**
	 * Literal version.
	 */
	private static final String messageType = "CollateralValue";

	/**
	 * Literal trade.
	 */
	private static final String SANTANDER_TRADE_PARTIES = SantEmirUtil.getEmirReportablePOsFromDV();

	/**
	 * Date format
	 */
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

	/**
	 * Date format
	 */
	private static SimpleDateFormat sdfUTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());

	/**
	 * SantEmirCVMReportLogic constructor.
	 * 
	 * @param mce
	 *            Trade.
	 * @param MarginCallEntryDTO
	 *            Valuation date of the report.
	 * @throws RemoteException
	 */
	public SantEmirCVMReportLogic(final MarginCallEntryDTO mce, final JDatetime valDate,
			final CollateralConfig collateralConfig, @SuppressWarnings("rawtypes") final Vector holidays,
			final SantEmirCacheCloneContracts[] cacheCloneContracts) throws RemoteException {
		this.valDate = valDate;
		this.col_conf = collateralConfig;
		this.mce_dto = mce;
		this.holidays = holidays;
		this.cacheCloneContracts = cacheCloneContracts;
		SantEmirCVMReportLogic.leBSTE = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity("BSTE");
	}

	/**
	 * Fills the item with the logic.
	 * 
	 * @return SantEmirCVMReportItem Item to be filled.
	 */
	public SantEmirCVMReportItem fillItem(boolean lei, final ReportTemplate reportTemplate) {
		final SantEmirCVMReportItem item = new SantEmirCVMReportItem();

		item.setFieldValue(SantEmirCVMColumns.COMMENT.toString(), getLogicComment());
		item.setFieldValue(SantEmirCVMColumns.VERSION.toString(), getLogicVersion());
		item.setFieldValue(SantEmirCVMColumns.MESSAGETYPE.toString(), getLogicMessageType());
		item.setFieldValue(SantEmirCVMColumns.MESSAGEID.toString(), getLogicMessageId());
		item.setFieldValue(SantEmirCVMColumns.ACTION.toString(), getLogicAction());
		// modificar como en SantEmirCVM
		if (lei) {
			item.setFieldValue(SantEmirCVMColumns.LEIPREFIX.toString(), getLogicLeiPrefixnew());
			item.setFieldValue(SantEmirCVMColumns.LEIVALUE.toString(), getLeiAttribute(reportTemplate));
			item.setFieldValue(SantEmirCVMColumns.TRADEPARTYPREF1.toString(), getLogicTradePartyPref1new());
			item.setFieldValue(SantEmirCVMColumns.TRADEPARTYVAL1.toString(), getLeiAttribute(reportTemplate));
		} else {
			item.setFieldValue(SantEmirCVMColumns.LEIPREFIX.toString(), getLogicLeiPrefix());
			item.setFieldValue(SantEmirCVMColumns.LEIVALUE.toString(), getLogicLeiValue());
			item.setFieldValue(SantEmirCVMColumns.TRADEPARTYPREF1.toString(), getLogicTradePartyPref1());
			item.setFieldValue(SantEmirCVMColumns.TRADEPARTYVAL1.toString(), getLogicTradePartyVal1());
		}

		// ----------------
		item.setFieldValue(SantEmirCVMColumns.EXECUTIONAGENTPARTY1PREFIX.toString(),
				getLogicExecutionAgentParty1Prefix());
		item.setFieldValue(SantEmirCVMColumns.EXECUTIONAGENTPARTYVALUE1.toString(),
				getLogicExecutionAgentPartyValue1());
		item.setFieldValue(SantEmirCVMColumns.COLLATERALPORTFOLIOCODE.toString(), getLogicCollateralPortfolioCode());
		item.setFieldValue(SantEmirCVMColumns.COLLATERALPORTFOLIOINDICATOR.toString(),
				getLogicCollateralPortfolioIndicator());
		item.setFieldValue(SantEmirCVMColumns.VALUEOFTHECOLLATERAL.toString(), getLogicValueOfTheCollateral());
		item.setFieldValue(SantEmirCVMColumns.CURRENCYCOLLATERALVALUE.toString(), getLogicCurrencyCollateralValue());
		item.setFieldValue(SantEmirCVMColumns.COLLATERALVALUATIONDATETIME.toString(),
				getLogicCollateralValuationDateTime());
		item.setFieldValue(SantEmirCVMColumns.COLLATERALREPORTINGDATE.toString(), getLogicCollateralReportingDate());
		item.setFieldValue(SantEmirCVMColumns.SENDTO.toString(), getLogicSendTo());
		item.setFieldValue(SantEmirCVMColumns.PARTYREPOBLIGATION1.toString(), getLogicPartyRepObligation());

		return item;
	}

	/**
	 * Get comment.
	 * 
	 * @return comment
	 */
	private String getLogicComment() {

		return NULL;
	}

	/**
	 * Get version.
	 * 
	 * @return version
	 */
	private String getLogicVersion() {

		return version;
	}

	/**
	 * Get the message type.
	 * 
	 * @return messageType
	 */
	private String getLogicMessageType() {

		return messageType;
	}

	/**
	 * Get the message id.
	 * 
	 * @return messageID
	 */
	private String getLogicMessageId() {

		return NULL;
	}

	/**
	 * Get action.
	 * 
	 * @return action
	 */

	public String getLogicAction() {

		String rst = "?";

		if (!Util.isEmpty(this.col_conf.getAgreementStatus())) {
			if (this.col_conf.getAgreementStatus().equals("OPEN")) {
				rst = "New";
			}
		}
		return rst;
	}

	/**
	 * Get lei prefix.
	 * 
	 * @return lei prefix
	 */
	private String getLogicLeiPrefix() {

		return DTCC;
	}

	private String getLogicLeiPrefixnew() {

		return LEI;
	}

	/**
	 * Get Data Submitter Id.
	 * 
	 * @return Data Submitter Id
	 */
	@SuppressWarnings("unused")
	private String getLogicLeiValueCAVALCA() {

		String rst = NULL;

		final boolean isCptyAttr = false;

		LegalEntity po = this.col_conf.getProcessingOrg();
		LegalEntity cpty = this.col_conf.getLegalEntity();
		String poAttrDTCCId = null;

		if ((!Util.isEmpty(po.getAuthName())) && (SANTANDER_TRADE_PARTIES.contains(po.getAuthName()))) {

			if (!"WK15".equals(po.getAuthName())) { // Almost all entities
													// report with BSTE
													// submitter id
				poAttrDTCCId = SantEmirUtil.getLegalEntityAttribute(leBSTE, cpty, "DTCC_LE_ID", isCptyAttr);
			} else { // Cavalsa will report from Calypso Collaterals with its
						// own submitter id
				poAttrDTCCId = SantEmirUtil.getLegalEntityAttribute(po, cpty, "DTCC_LE_ID", isCptyAttr);
			}
		}

		// if (!Util.isEmpty(poAttrDTCCId)) {
		rst = poAttrDTCCId;
		// } else {
		// rst = po.getAuthName() + " has no DTCC_LE_ID attribute.";
		// }

		return rst;
	}

	/**
	 * Get lei value.
	 * 
	 * @return lei value
	 */
	private String getLogicLeiValue() {

		String rst = NULL;

		final boolean isCptyAttr = false;

		String poAttrDTCCId = null;

		LegalEntity po = this.col_conf.getProcessingOrg();
		LegalEntity cpty = this.col_conf.getLegalEntity();
		
		//GSM 11/04/2017 - Fix to avoid hardcoded BSTE DTTC_ID when is needed for other PO in Valuation
		if (!Util.isEmpty(po.getAuthName())){// && !(SANTANDER_TRADE_PARTIES.contains(po.getAuthName()))){
			poAttrDTCCId = SantEmirUtil.getLegalEntityAttribute(po, cpty, "DTCC_LE_ID", isCptyAttr);
			if (!Util.isEmpty(poAttrDTCCId))
				return poAttrDTCCId;
		}	

		if ((!Util.isEmpty(po.getAuthName())) && (SANTANDER_TRADE_PARTIES.contains(po.getAuthName()))) {
	
			if (Util.isEmpty(poAttrDTCCId))
				poAttrDTCCId = SantEmirUtil.getLegalEntityAttribute(leBSTE, cpty, "DTCC_LE_ID", isCptyAttr);
		}
		if (!Util.isEmpty(poAttrDTCCId)) {
			rst = poAttrDTCCId;
		} else {
			rst = po.getAuthName();
		}

		return rst;
	}

	/**
	 * Get lei value.
	 * 
	 * @return lei value
	 */
	@SuppressWarnings("unused")
	private String getLogicLeiValuenew(final ReportTemplate reportTemplate) {

		String rst;
		final boolean isCptyAttr = false;
		String poAttrDTCCId = "";

		LegalEntity po = this.col_conf.getProcessingOrg();
		LegalEntity cpty = this.col_conf.getLegalEntity();

		String authPos = SantEmirUtil.getEmirReportablePOsFromDV(reportTemplate);

		if ((!Util.isEmpty(po.getAuthName())) && (authPos.contains(po.getAuthName()))) {
			poAttrDTCCId = SantEmirUtil.getLegalEntityAttribute(po, cpty, "LEI", isCptyAttr);
		}
		if (!Util.isEmpty(poAttrDTCCId)) {
			rst = poAttrDTCCId;
		} else {
			rst = po.getAuthName();
		}
		return rst;
	}

	/**
	 * Get the trade party prefix 1.
	 * 
	 * @return trade party prefix 1
	 */
	private String getLogicTradePartyPref1() {

		return DTCC;
	}

	private String getLogicTradePartyPref1new() {

		return LEI;
	}

	/**
	 * Get trade party id 1.
	 * 
	 * @return trade party id 1
	 */
	private String getLogicTradePartyVal1() {

		String rst = NULL;

		final boolean isCptyAttr = false;

		LegalEntity po = this.col_conf.getProcessingOrg();
		LegalEntity cpty = this.col_conf.getLegalEntity();
		String poAttrDTCCId = null;

		if ((!Util.isEmpty(po.getAuthName())) && (SANTANDER_TRADE_PARTIES.contains(po.getAuthName()))) {

			if ("BECM".equals(po.getAuthName())) { // Banesto entity report with
													// BSTE submitter id
				poAttrDTCCId = SantEmirUtil.getLegalEntityAttribute(leBSTE, cpty, "DTCC_LE_ID", isCptyAttr);
			} else {
				poAttrDTCCId = SantEmirUtil.getLegalEntityAttribute(po, cpty, "DTCC_LE_ID", isCptyAttr);
			}
		}

		if (!Util.isEmpty(poAttrDTCCId)) {
			rst = poAttrDTCCId;
		} else {
			rst = po.getAuthName();
		}

		return rst;
	}

	private String getLeiAttribute(final ReportTemplate reportTemplate) {

		final boolean isCptyAttr = false;
		LegalEntity po = this.col_conf.getProcessingOrg();
		LegalEntity cpty = this.col_conf.getLegalEntity();
		String authPos = SantEmirUtil.getEmirReportablePOsFromDV(reportTemplate);
		if ((!Util.isEmpty(po.getAuthName())) && (authPos.contains(po.getAuthName()))) {
			return SantEmirUtil.getLegalEntityAttribute(po, cpty, "LEI", isCptyAttr);
		}else{
			List<String> defaultPos = LocalCache.getDomainValues(DSConnection.getDefault(), "EmirDefaultPOValue");
			if(!Util.isEmpty(defaultPos)){
				String defaultPO = defaultPos.get(0);
				if(!Util.isEmpty(defaultPO)){
					LegalEntity defaultLE = BOCache.getLegalEntity(DSConnection.getDefault(), defaultPO);
					LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, defaultLE.getId(), "ALL", "LEI");
				return attr.getAttributeValue();
				}
			}
		}
		return "";
	}

	/**
	 * Get the execution agent party prefix 1.
	 * 
	 * @return execution agent party prefix 1
	 */
	private String getLogicExecutionAgentParty1Prefix() {

		return NULL;
	}

	/**
	 * Get execution agent party value 1.
	 * 
	 * @return execution agent party value 1
	 */
	private String getLogicExecutionAgentPartyValue1() {

		return NULL;
	}

	/**
	 * Get collateral portfolio code.
	 * 
	 * @return collateral portfolio code
	 */
	private String getLogicCollateralPortfolioCode() {

		String rst = NULL;

		if (!Util.isEmpty(String.valueOf(this.mce_dto.getCollateralConfigId()))) {
			rst = String.valueOf(this.mce_dto.getCollateralConfigId());
		}
		return rst;
	}

	/**
	 * Get collateral portfolio indicator.
	 * 
	 * @return collateral portfolio indicator
	 */
	private String getLogicCollateralPortfolioIndicator() {

		return "true";
	}

	/**
	 * Get value of the collateral.
	 * 
	 * @return value of the collateral
	 */
	private String getLogicValueOfTheCollateral() {

		String rst = NULL;
		double value = 0.0;

		// the value of the head
		if ((this.mce_dto != null) && !Util.isEmpty(String.valueOf(this.mce_dto.getPreviousTotalMargin()))) {
			value = this.mce_dto.getPreviousTotalMargin();
		}

		int i = 0;
		while (i < this.cacheCloneContracts.length - 1) {
			// adding the value of its clone contracts
			if (this.cacheCloneContracts[i].getHeadContractId() == this.col_conf.getId()) {
				value += this.cacheCloneContracts[i].getValueCloneContract();
			}
			i++;
		}

		// Only retrieved if Total Prev Mrg is negative (the total addition of
		// values of clones contracts and the head one)
		if (value < 0.0) {
			value = -(value);
		} else {
			value = 0.0;
		}

		// VALUEOFTHECOLLATERAL format: 20 integers . 10 decimals
		DecimalFormat df = new DecimalFormat("####################.##########");
		rst = df.format(value);

		if (rst.contains(",")) {
			rst = rst.replace(",", ".").toString();
		}

		return rst;
	}

	/**
	 * Get currency of the collateral.
	 * 
	 * @return currency of the collateral
	 */
	private String getLogicCurrencyCollateralValue() {

		String rst = NULL;

		if (!Util.isEmpty(this.col_conf.getCurrency())) {
			rst = this.col_conf.getCurrency();
		}
		return rst;
	}

	/**
	 * Get collateral valuation date time.
	 * 
	 * @return collateral valuation date time
	 */
	private String getLogicCollateralValuationDateTime() {

		String rst = NULL;

		final JDate previousDate = this.valDate.getJDate(TimeZone.getDefault()).addBusinessDays(-1, this.holidays);
		final JDatetime pDt = previousDate.getJDatetime(TimeZone.getDefault());
		final JDatetime pDtAt10am = pDt.add(0, -13, -59, 0, 0);

		if (pDtAt10am != null) {
			rst = SantEmirCVMReportLogic.sdfUTC.format(pDtAt10am);
		}

		return rst;
	}

	/**
	 * Get collateral reporting date.
	 * 
	 * @return collateral reporting date
	 */
	private String getLogicCollateralReportingDate() {

		String rst = NULL;

		if (this.valDate != null) {
			rst = sdf.format(this.valDate);
		}

		return rst;
	}

	/**
	 * Get send to.
	 * 
	 * @return send to
	 */
	private String getLogicSendTo() {

		return DTCCEU;
	}

	/**
	 * Get party reporting obligation string.
	 * 
	 * @return ESMA
	 */
	private String getLogicPartyRepObligation() {

		return ESMA;
	}

}
