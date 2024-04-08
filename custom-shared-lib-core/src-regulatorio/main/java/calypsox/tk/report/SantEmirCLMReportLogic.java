/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import calypsox.regulation.util.SantEmirUtil;
import calypsox.tk.regulation.CollateralizationDegree;
//import com.calypso.tk.util.ConnectionUtil;

/**
 * SantEmirCLMReportLogic add the logic needed in Collateral Linking Message for
 * EMIR report.
 * 
 * @author xIS16241
 * 
 */
public class SantEmirCLMReportLogic {

	/**
	 * Trade of the report.
	 */
	protected Trade trade;

	/**
	 * Margin call detail entry.
	 */
	protected MarginCallDetailEntryDTO mcde_dto;

	/**
	 * collateral config.
	 */
	protected CollateralConfig col_conf;

	/**
	 * collateralization degree.
	 */
	protected CollateralizationDegree col_deg;

	/**
	 * Valuation date of the report.
	 */
	protected final JDatetime valDate;

	/**
	 * holidays.
	 */
	@SuppressWarnings("rawtypes")
	protected final Vector holidays;

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
	 * Literal version.
	 */
	private static final String version = "Coll1.0";

	/**
	 * Literal version.
	 */
	private static final String messageType = "CollateralLink";
	/**
	 * Uti Keyword reference.
	 */
	private static final String UtiKW = "UTI_REFERENCE";

	/**
	 * Usi Keyword reference.
	 */
	private static final String UsiKW = "USI_REFERENCE";

	/**
	 * bo_reference Keyword reference.
	 */
	private static final String boReferenceKW = "BO_REFERENCE";

	/**
	 * Santander Parties.
	 */
	private static final String SANTANDER_TRADE_PARTIES = "BSNY BSTE BSHK BDSD";

	/**
	 * SantEmirCLMReportItem constructor.
	 * 
	 * @param trade
	 *            Trade.
	 * @param valDatetime
	 *            Valuation date of the report.
	 * @throws RemoteException
	 */
	public SantEmirCLMReportLogic(final Trade trade, final MarginCallDetailEntryDTO mcd, final JDatetime valDate,
			final CollateralConfig collateralConfig, @SuppressWarnings("rawtypes") final Vector holidays)
			throws RemoteException {
		this.trade = trade;
		this.valDate = valDate;
		this.mcde_dto = mcd;
		this.holidays = holidays;
		this.col_conf = collateralConfig;
		this.col_deg = new CollateralizationDegree();
	}

	/**
	 * Fills the item with the logic.
	 * 
	 * @return SantEmirCLMReportItem Item to be filled.
	 */
	public SantEmirCLMReportItem fillItem() {
		final SantEmirCLMReportItem item = new SantEmirCLMReportItem(this.trade);

		item.setFieldValue(SantEmirCLMColumns.COMMENT.toString(), getLogicComment());
		item.setFieldValue(SantEmirCLMColumns.VERSION.toString(), getLogicVersion());
		item.setFieldValue(SantEmirCLMColumns.MESSAGETYPE.toString(), getLogicMessageType());
		item.setFieldValue(SantEmirCLMColumns.MESSAGEID.toString(), getLogicMessageId());
		item.setFieldValue(SantEmirCLMColumns.ACTION.toString(), getLogicAction());
		item.setFieldValue(SantEmirCLMColumns.LEIPREFIX.toString(), getLogicLeiPrefix()); // old
		item.setFieldValue(SantEmirCLMColumns.LEIVALUE.toString(), getLogicDTCCValue()); // old
		item.setFieldValue(SantEmirCLMColumns.TRADEPARTYPREF1.toString(), getLogicTradePartyPref1()); // old
		item.setFieldValue(SantEmirCLMColumns.TRADEPARTYVAL1.toString(), getLogicTradePartyVal1DTCC()); // old
		item.setFieldValue(SantEmirCLMColumns.EXECUTIONAGENTPARTY1PREFIX.toString(),
				getLogicExecutionAgentParty1Prefix());
		item.setFieldValue(SantEmirCLMColumns.EXECUTIONAGENTPARTYVALUE1.toString(),
				getLogicExecutionAgentPartyValue1());
		item.setFieldValue(SantEmirCLMColumns.UTIPREFIX.toString(), getLogicUtiprefix());
		item.setFieldValue(SantEmirCLMColumns.UTI.toString(), getLogicUti());
		item.setFieldValue(SantEmirCLMColumns.USIPREFIX.toString(), getLogicUsiprefix());
		item.setFieldValue(SantEmirCLMColumns.USIVALUE.toString(), getLogicUsiValue());
		item.setFieldValue(SantEmirCLMColumns.TRADEPARTYTRANSACTIONID1.toString(), getLogicTradePartyTransactionId1());
		item.setFieldValue(SantEmirCLMColumns.COLLATERALPORTFOLIOCODE.toString(), getLogicCollateralPortfolioCode());
		item.setFieldValue(SantEmirCLMColumns.COLLATERALIZED.toString(), getLogicCollateralized());
		item.setFieldValue(SantEmirCLMColumns.SENDTO.toString(), getLogicSendTo());
		item.setFieldValue(SantEmirCLMColumns.PARTYREPOBLIGATION1.toString(), getLogicPartyRepObligation());
		item.setFieldValue(SantEmirCLMColumns.ACTIVITY.toString(), getLogicActivity());

		// NEW
		item.setFieldValue(SantEmirCLMColumns.LEIPREFIXnew.toString(), getLogicLeiPrefixnew()); // new
		item.setFieldValue(SantEmirCLMColumns.LEIVALUEnew.toString(), getLogicLeiValuenew()); // new
		item.setFieldValue(SantEmirCLMColumns.TRADEPARTYPREF1new.toString(), getLogicTradePartyPref1new()); // new
		item.setFieldValue(SantEmirCLMColumns.TRADEPARTYVAL1new.toString(), getLogicLeiValuenew()); // new

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
	private String getLogicAction() {
		String rst = "New";

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
	 * Get lei value.
	 * 
	 * @return lei value
	 */
	private String getLogicDTCCValue() {
		
		LegalEntity po = this.col_conf.getProcessingOrg();
		LegalEntity cpty = this.col_conf.getLegalEntity();
		
		//GSM 11/04/2017 - Fix to avoid hardcoded BSTE DTTC_ID when is needed for other PO in Linking	
		if (!Util.isEmpty(po.getAuthName()) && !(SANTANDER_TRADE_PARTIES.contains(po.getAuthName())))  {
			final String poAttrDTCCId = SantEmirUtil.getLegalEntityAttribute(po, cpty, "DTCC_LE_ID", false);
			if (!Util.isEmpty(poAttrDTCCId))
				return poAttrDTCCId;	
		}
		
		po = BOCache.getLegalEntity(DSConnection.getDefault(), "BSTE");
		LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, po.getLegalEntityId(),
				"ALL", "DTCC_LE_ID");
		// GSM: 05/08. Quick fix, return always BSTE code
		if (Util.isEmpty(attr.getAttributeValue()))
			return NULL;
		return attr.getAttributeValue();

		// String rst = NULL;
		// final boolean isCptyAttr = false;
		// if ( (!Util.isEmpty(po)) && ("BSTE".equals(po)) ) {
		// final String poAttrDTCCId =
		// SantEmirUtil.getLegalEntityAttribute(this.trade, "DTCC_LE_ID",
		// isCptyAttr);
		// if (!Util.isEmpty(poAttrDTCCId)) {
		// rst = poAttrDTCCId;
		// }
		// }

		// return rst;
	}

	private String getLogicLeiValuenew() {

		final boolean isCptyAttr = false;
		LegalEntity po = this.col_conf.getProcessingOrg();
		LegalEntity cpty = this.col_conf.getLegalEntity();
		String lei = "";
		if (!Util.isEmpty(po.getAuthName())) {
			lei = SantEmirUtil.getLegalEntityAttribute(po, cpty, "LEI", isCptyAttr);
		}
		
		if(Util.isEmpty(lei)){
			List<String> defaultPos = LocalCache.getDomainValues(DSConnection.getDefault(), "EmirDefaultPOValue");
			if(!Util.isEmpty(defaultPos)){
				String defaultPO = defaultPos.get(0);
				if(!Util.isEmpty(defaultPO)){
					LegalEntity defaultLE = BOCache.getLegalEntity(DSConnection.getDefault(), defaultPO);
					LegalEntityAttribute attr = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, defaultLE.getId(), "ALL", "LEI");
				lei = attr.getAttributeValue();
				}
			}
			
			
		}
		return lei;
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
	 * Get trade party value.
	 * 
	 * @return trade party value
	 */
	private String getLogicTradePartyVal1DTCC() {

		String rst = NULL;
		final boolean isCptyAttr = false;
		
		LegalEntity poLe = this.col_conf.getProcessingOrg();
		LegalEntity cptyLe = this.col_conf.getLegalEntity();
		
		//GSM 11/04/2017 - Fix to avoid hardcoded BSTE DTTC_ID when is needed for other PO in Linking	
		if (!Util.isEmpty(poLe.getAuthName()) && !(SANTANDER_TRADE_PARTIES.contains(poLe.getAuthName()))) {
					final String poAttrDTCCId = SantEmirUtil.getLegalEntityAttribute(poLe, cptyLe, "DTCC_LE_ID", false);
					if (!Util.isEmpty(poAttrDTCCId))
						return poAttrDTCCId;	
				}
		
		String po = this.trade.getBook().getLegalEntity().getAuthName();

		if ((!Util.isEmpty(po)) && (SANTANDER_TRADE_PARTIES.contains(po))) {
			final String poAttrDTCCId = SantEmirUtil.getLegalEntityAttribute(this.trade, "DTCC_LE_ID", isCptyAttr);
			if (!Util.isEmpty(poAttrDTCCId)) {
				rst = poAttrDTCCId;
			}
		}

		return rst;
	}

	@SuppressWarnings("unused")
	private String getLogicTradePartyVal1new() {

		String rst = NULL;
		final boolean isCptyAttr = false;

		// String po = this.trade.getBook().getLegalEntity().getAuthName();

		// if ( (!Util.isEmpty(po)) && (SANTANDER_TRADE_PARTIES.contains(po)) )
		// {
		final String poAttrDTCCId = SantEmirUtil.getLegalEntityAttribute(this.trade, "LEI", isCptyAttr);
		if (!Util.isEmpty(poAttrDTCCId)) {
			rst = poAttrDTCCId;
		}
		// }

		return rst;
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
	 * Get uti prefix from the trade keyword.
	 * 
	 * @return uti prefix
	 */
	private String getLogicUtiprefix() {

		String rst = NULL;

		if (!Util.isEmpty(this.trade.getKeywordValue(UtiKW))) {
			rst = String.valueOf(this.trade.getKeywordValue(UtiKW));
			if (rst.length() >= 10) {
				rst = rst.substring(0, 10);
			}
		}

		return rst;
	}

	/**
	 * Get uti value from the trade keyword.
	 * 
	 * @return uti value
	 */
	private String getLogicUti() {

		String rst = NULL;

		if (!Util.isEmpty(this.trade.getKeywordValue(UtiKW))) {
			rst = String.valueOf(this.trade.getKeywordValue(UtiKW));
			if (rst.length() >= 10) {
				rst = rst.substring(10, rst.length());
			}
		}

		return rst;
	}

	/**
	 * Get usi prefix from the trade keyword.
	 * 
	 * @return usi prefix
	 */
	private String getLogicUsiprefix() {

		String rst = NULL;

		if (!Util.isEmpty(this.trade.getKeywordValue(UsiKW))) {
			rst = String.valueOf(this.trade.getKeywordValue(UsiKW));
			if (rst.length() >= 10) {
				rst = rst.substring(0, 10);
			}
		}

		return rst;
	}

	/**
	 * Get usi value from the trade keyword.
	 * 
	 * @return usi value
	 */
	private String getLogicUsiValue() {

		String rst = NULL;

		if (!Util.isEmpty(this.trade.getKeywordValue(UsiKW))) {
			rst = String.valueOf(this.trade.getKeywordValue(UsiKW));
			if (rst.length() >= 10) {
				rst = rst.substring(10, rst.length());
			}
		}

		return rst;
	}

	/**
	 * Get trade party transaction id from the trade Keyword bo_refence.
	 * 
	 * @return trade party transaction id
	 */
	private String getLogicTradePartyTransactionId1() {

		String rst = NULL;

		if (!Util.isEmpty(this.trade.getKeywordValue(boReferenceKW))) {
			rst = String.valueOf(this.trade.getKeywordValue(boReferenceKW));
		}

		return rst;
	}

	/**
	 * Get collateral portfolio code.
	 * 
	 * @return collateral portfolio code
	 */
	private String getLogicCollateralPortfolioCode() {

		// GSM: 05/08. Quick fix, if not Head contract id, recovers from name
		String rst = NULL;

		if (!Util.isEmpty(String.valueOf(this.mcde_dto.getMarginCallConfigId()))) {
			CollateralConfig cc = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
					this.mcde_dto.getMarginCallConfigId());

			if (!Util.isEmpty(String.valueOf(cc.getAdditionalField("HEAD_CLONE")))) {
				if (("HEAD".equals(cc.getAdditionalField("HEAD_CLONE")))
						|| (("CLONE").equals(cc.getAdditionalField("HEAD_CLONE"))
								&& (Util.isEmpty(cc.getAdditionalField("EMIR_CLONE_VALUE_REPORTABLE"))))) {

					rst = String.valueOf(this.mcde_dto.getMarginCallConfigId());
				} else if (("CLONE").equals(cc.getAdditionalField("HEAD_CLONE"))
						&& ("YES").equalsIgnoreCase(cc.getAdditionalField("EMIR_CLONE_VALUE_REPORTABLE"))) {
					if (!Util.isEmpty(cc.getAdditionalField("MCC_HEAD"))) {
						rst = cc.getAdditionalField("MCC_HEAD");
						try {
							Integer num = Integer.parseInt(rst);
							if (num != null)
								return num.toString();
							// is an integer!
						} catch (NumberFormatException e) {

							CollateralConfig ccHead = null;
							try {
								if (!Util.isEmpty(rst))
									ccHead = ServiceRegistry.getDefault().getCollateralDataServer()
											.getMarginCallConfigByCode("Name", rst.trim());
							} catch (RemoteException e1) {
								Log.error(this, e1.getLocalizedMessage());
							}
							if (ccHead != null)
								return String.valueOf(ccHead.getId());
						}
					}
				}
			}
		}
		return rst;
	}

	/**
	 * Get collateral portfolio code.
	 * 
	 * @return collateral portfolio code
	 */
	private String getLogicCollateralized() {

		String rst = NULL;
		rst = this.col_deg.getTodayCollaterizedDegree(this.col_conf);
		if (!Util.isEmpty(rst)) {
			return rst;
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

	/**
	 * Get activity.
	 * 
	 * @return activity
	 */
	private String getLogicActivity() {
		String rst = "NEW";

		return rst;
	}

}
