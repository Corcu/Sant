/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import calypsox.ErrorCodeEnum;
import calypsox.apps.reporting.SantEmirCVMReportTemplatePanel;
import calypsox.regulation.util.SantEmirUtil;
import calypsox.tk.util.ControlMErrorLogger;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.TradeReport;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

/**
 * Class for generating the CVM message for EMIR report.
 * 
 * @author Felipe Queipo
 * @version 1.1
 * 
 */
public class SantEmirCVMReport extends TradeReport {

	// class variables
	private JDate valuationDate;
	@SuppressWarnings("rawtypes")
	private Vector holidays;

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 368714479840272166L;

	public static final String NO_VALUEDATE_ERROR_MESSAGE = "No valuation Date read from the template. Please check the configuration";
	public static final String NO_HOLIDAYS_ERROR_MESSAGE = "No holidays read from attributes. Please check the configuration";
	private final static String CLASS_NAME = SantEmirCVMReport.class.getCanonicalName();
	private final static String ERROR_RESULT = "Not document generated";

	/**
	 * instance
	 */
	private static final SantEmirCVMReport INSTANCE = new SantEmirCVMReport();
	private static SantEmirCVMReport instance = INSTANCE;

	/**
	 * Singleton access to SantEmirCVMReport.
	 * 
	 * @return singleton instance of SantEmirCVMReport.
	 */
	public static SantEmirCVMReport getInstance() {
		return instance;
	}

	/**
	 * Just for testing purposes. It should receive a mocked instance of
	 * SantEmirCVMReport.
	 * 
	 * @param mockedUtil
	 *            mocked instance of SantEmirCVMReport.
	 */
	public static void setInstance(final SantEmirCVMReport mockedUtil) {
		if (mockedUtil == null) {
			instance = INSTANCE;
		} else {
			instance = mockedUtil;
		}
	}

	/**
	 * Override method load to generate the file (the report).
	 * 
	 * @param errorsMsgs
	 *            passed by parameter
	 * @return the ReportOutput to generate the report
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public ReportOutput load(final Vector errors) {

		final DefaultReportOutput rst = new StandardReportOutput(this);
		final ArrayList<ReportRow> rows = new ArrayList<ReportRow>();
		final Set<String> checkCollateralAdded = new HashSet<String>();
		long begin = System.currentTimeMillis();

		Log.debug(CLASS_NAME, "1. CLM message generation started.");

		/*
		 * control check and retrieve the data from the template of the schedule
		 * task
		 */
		if (!readAndCheckTemplateAttributes(errors)) {

			Log.error(CLASS_NAME, "Template Attributes has NOT been received"); // log
																				// messages
			ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, ERROR_RESULT);// CONTROL-M
			return rst;
		}

		final List<MarginCallEntryDTO> mCEntries = SantEmirUtil.retriveMCAliveEntryList(errors, this.valuationDate,
				getReportTemplate()); // retrieve all MCDEntries alive ->
										// Contracts

		long end = System.currentTimeMillis();
		// System.out.println("Hasta despues de la busqueda de entries tard?: "
		// + (end - begin) / 60000.0 + " minutos.");

		Log.debug(CLASS_NAME, "Al margin call entries for emir obtained for Date " + this.valuationDate.toString());

		SantEmirCacheCloneContracts[] cacheCloneContracts_ = SantEmirCacheCloneContracts
				.initializeCacheCloneContracts(mCEntries.size());

		int i = 0;
		for (MarginCallEntryDTO mc : mCEntries) {

			if (SantEmirUtil.isCollateralContractOpened(mc)) {

				CollateralConfig col_conf = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
						mc.getCollateralConfigId());

				if (!("HEAD".equals(col_conf.getAdditionalField("HEAD_CLONE")))
						&& (!Util.isEmpty(col_conf.getAdditionalField("MCC_HEAD")))) {
					if (((!Util.isEmpty(col_conf.getAdditionalField("EMIR_CLONE_VALUE_REPORTABLE")))
							&& ("YES".equalsIgnoreCase(col_conf.getAdditionalField("EMIR_CLONE_VALUE_REPORTABLE"))))
							|| ("WK15".equals(col_conf.getProcessingOrg().getAuthName()))) {

						final List<MarginCallEntryDTO> retriveHeadMCE = SantEmirUtil.retriveHeadMCE(errors,
								this.valuationDate, col_conf.getAdditionalField("MCC_HEAD"));

						if (retriveHeadMCE != null) {
							for (MarginCallEntryDTO head_mcd : retriveHeadMCE) {
								cacheCloneContracts_[i].setCloneContractId(mc.getCollateralConfigId());
								cacheCloneContracts_[i].setHeadContractName(col_conf.getAdditionalField("MCC_HEAD"));
								cacheCloneContracts_[i].setHeadContractId(head_mcd.getCollateralConfigId());
								cacheCloneContracts_[i]
										.setValueCloneContract(SantEmirUtil.getLogicValueOfTheCollateral(mc));
								i++;
								break;
							}
						}
					}
				}
			}
		}
		SantEmirCacheCloneContracts[] cacheCloneContracts = SantEmirCacheCloneContracts.copy(cacheCloneContracts_, i);

		end = System.currentTimeMillis();
		System.out.println("Hasta despues de la busqueda de clones tard?: " + (end - begin) / 60000.0 + " minutos.");

		if (mCEntries != null) {

			for (MarginCallEntryDTO mcd : mCEntries) {

				if (!checkCollateralAdded.contains(String.valueOf(mcd.getCollateralConfigId()))
						&& SantEmirUtil.isCollateralContractOpened(mcd)) {

					try {

						CollateralConfig col_conf = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(),
								mcd.getCollateralConfigId());

						if (isHeadCloneReportable(col_conf)) {
							SantEmirCVMReportItem currentItem = createItem(mcd, col_conf, cacheCloneContracts);

							Set<String> fields = currentItem.getFieldNames();

							for (String tag : fields) {
								String value = (String) currentItem.getFieldValue(tag);
								if (!Util.isEmpty(value)) {
									SantEmirRow emirRow = new SantEmirRow();

									if (!Util.isEmpty(String.valueOf(mcd.getCollateralConfigId()))) {
										emirRow.setExternalId(String.valueOf(mcd.getCollateralConfigId()));
									}
									addSourceSystem(emirRow, col_conf);
									emirRow.setMessageType("COL");
									emirRow.setActivity(getCVMActivity(mcd));
									emirRow.setTransactionType("TRD");
									emirRow.setProduct("Collateral");

									emirRow.setTag(tag);
									emirRow.setValue(value);

									ReportRow currentRow = new ReportRow(emirRow);
									currentRow.setProperty(SantEmirCVMReportItem.ID_SantEmirCVMReportItem, emirRow);

									rows.add(currentRow);
								}
							}

							// we mark the contract as added
							checkCollateralAdded.add(String.valueOf(mcd.getCollateralConfigId()));
						}

					} catch (final Exception e) {
						Log.error(this, e);
					}

				}

			}
		}

		end = System.currentTimeMillis();
		System.out.println("Tard? en total: " + (end - begin) / 60000.0 + " minutos.");

		rst.setRows(rows.toArray(new ReportRow[rows.size()]));

		Log.debug(CLASS_NAME, "2. CLM message generated.");

		return rst;
	}

	/**
	 * Reads all the attributes passed through the schedule task template, acts
	 * as a constructor of all the require class variables and finally it checks
	 * that all the necessary data for constructing the data is available
	 * 
	 * @param errors
	 * 
	 * @return true is all the data is available, false othercase
	 */
	// All attributes to be checked HERE!!
	private boolean readAndCheckTemplateAttributes(Vector<String> errors) {

		// final Attributes attributes = getReportTemplate().getAttributes();

		// We get the valuation date to put in the export properly.
		this.valuationDate = getExecutionDate(); // getReportTemplate().getValDate();
		this.holidays = getHolidays(errors); // getReportTemplate().getHolidays();

		if (this.valuationDate == null) {

			errors.add(NO_VALUEDATE_ERROR_MESSAGE);
			Log.error(CLASS_NAME, NO_VALUEDATE_ERROR_MESSAGE);
			return false;
		}
		if (this.holidays == null) {

			errors.add(NO_HOLIDAYS_ERROR_MESSAGE);
			Log.error(CLASS_NAME, NO_HOLIDAYS_ERROR_MESSAGE);
			return false;
		}
		// check conditions necessary to continue with the processing
		return true;
	}

	@SuppressWarnings("unchecked")
	private Vector<String> getHolidays(Vector<String> errors) {

		Object holidays = getReportTemplate().getHolidays();
		if (holidays == null)
			holidays = getReportTemplate().getAttributes().get(ReportTemplate.HOLIDAYS);

		if (holidays != null) {

			if (holidays instanceof Vector)
				return ((Vector<String>) holidays);

			if (holidays instanceof String) {
				String holy = (String) holidays;
				return Util.string2Vector(holy.trim());
			}
		} // no holidays, try from system
		errors.add(NO_HOLIDAYS_ERROR_MESSAGE);
		errors.add("Used SYSTEM Calendar as default");
		return Util.string2Vector("SYSTEM");
	}

	/**
	 * @return execution date of the report
	 */
	protected JDate getExecutionDate() {

		JDate date = null;
		final String startDate = (String) getReportTemplate().getAttributes().get(ReportTemplate.START_DATE); // for
																												// tests

		if (!Util.isEmpty(startDate))
			date = JDate.valueOf(startDate);

		if (date == null)
			date = getReportTemplate().getValDate(); // as param from the
														// STRunner

		// if (date == null) //none, just previous businness day
		// date = JDate.getNow().addBusinessDays(-1,
		// Util.string2Vector("SYSTEM"));

		return date;
	}

	/**
	 * create the item by a given MarginCallEntryDTO
	 * 
	 * @param mcd
	 *            MarginCallEntryDTO
	 */
	private SantEmirCVMReportItem createItem(final MarginCallEntryDTO mcd, final CollateralConfig colconf,
			final SantEmirCacheCloneContracts[] cacheCloneContracts) {
		SantEmirCVMReportLogic emirLogic;
		SantEmirCVMReportItem item = new SantEmirCVMReportItem();
		final ReportTemplate reportTemplate = getReportTemplate();
		boolean lei = false;
		if (null != reportTemplate.getAttributes().get(SantEmirCVMReportTemplatePanel.LEI)) {
			lei = reportTemplate.getAttributes().get(SantEmirCVMReportTemplatePanel.LEI);
		}
		try {
			emirLogic = new SantEmirCVMReportLogic(mcd, this._valuationDateTime, colconf, this.holidays,
					cacheCloneContracts);

			item = emirLogic.fillItem(lei, reportTemplate);
		} catch (RemoteException e) {
			Log.error(this, e);
		}

		return item;
	}

	/**
	 * get the action for a given MarginCallEntryDTO for generating CVM
	 * 
	 * @param mcd
	 *            MarginCallEntryDTO
	 * @return action
	 */
	private String getCVMActivity(final MarginCallEntryDTO mcd) {

		String rst = "?";

		int mccId = mcd.getCollateralConfigId();

		MarginCallConfig mcc = BOCache.getMarginCallConfig(DSConnection.getDefault(), mccId);

		if (!Util.isEmpty(mcc.getAgreementStatus())) {
			if (mcc.getAgreementStatus().equals("CLOSED")) {
				rst = "CAN";
			}
			if (mcc.getAgreementStatus().equals("OPEN")) {
				rst = "NEW";
			}
		}
		return rst;
	}

	/**
	 * For knowing if the MC is reportable or not.
	 * 
	 * @param cc
	 *            CollateralConfig
	 * @return true or false
	 */
	private boolean isHeadCloneReportable(final CollateralConfig cc) {

		boolean rst = false;

		if (("HEAD").equals(cc.getAdditionalField("HEAD_CLONE"))) {
			rst = true;
		} else if (("CLONE").equals(cc.getAdditionalField("HEAD_CLONE"))
				&& (Util.isEmpty(cc.getAdditionalField("EMIR_CLONE_VALUE_REPORTABLE")))
				&& (!("WK15").equals(cc.getProcessingOrg().getAuthName()))) {
			rst = true;
		}
		return rst;
	}

	/**
	 * 
	 * @param emirRow
	 */
	private void addSourceSystem(SantEmirRow emirRow, CollateralConfig colConfig) {
		String sourceSystem = "431.4";
		LegalEntity le = colConfig.getProcessingOrg();
		if (le != null) {
			String code = le.getCode();
			if ("BFOM".equals(code)) {
				sourceSystem = "519.4";
			} else if ("5HSF".equals(code)) {
				sourceSystem = "520.4";
			} else if ("BCHB".equals(code)) {
				sourceSystem = "521.4";
			} 
		}
		emirRow.setSourceSystem(sourceSystem);
	}

}
