/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.calypso.apps.reporting.ReportTemplatePanel;
import com.calypso.tk.collateral.dto.CashPositionDTO;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.dto.SecurityPositionDTO;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Attributes;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;

import calypsox.util.collateral.CollateralManagerUtil;

/**
 * Collateral Positions report, including cash and securities (bonds and equities).
 * 
 * @author eola
 * 
 */
public class SantCollateralPositionSendBalanceReport extends SantReport {

	private static final long serialVersionUID = 1L;

	public static final String ALLOCATION_ENTRY_PROPERTY = "ALLOCATION_ENTRY";
	public static final String REPORT_DATE_PROPERTY = "REPORT_DATE";

	public static final String ALLOCATION_STATUS_IN_TRANSIT = "In Transit";
	public static final String ALLOCATION_STATUS_HELD = "Held";

	public static final String LEGAL_ENTITY_ROLE_PROCESSING_ORG = "ProcessingOrg";

	public static final String PO = "PO";

	protected JDate jdate = null;

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ReportOutput loadReport(final Vector errors) {

		if (getReportPanel() != null) {
			ReportTemplatePanel tempPanel = getReportPanel().getReportTemplatePanel();
			if (tempPanel != null) {
				tempPanel.setTemplate(getReportTemplate());
			}
		}
		final DefaultReportOutput reportOutput = new DefaultReportOutput(this);
		final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();
		final DSConnection dsConn = getDSConnection();
		final ReportTemplate reportTemp = getReportTemplate();

		try {

			// get date
			this.jdate = getProcessStartDate();
			Attributes atributos = reportTemp.getAttributes();

			int contractID = 0;

			if (atributos.get("AGREEMENT_ID") != null) {
				contractID = Integer.parseInt(atributos.get("AGREEMENT_ID").toString());
			}

			CollateralConfig marginCall = ServiceRegistry.getDefault().getCollateralDataServer()
					.getMarginCallConfig(contractID);

			if (marginCall != null) {

				// get securities associated to contract
				List<SecurityPositionDTO> secPositions = getSecurities(marginCall, dsConn, this.jdate);

				// get cash associated to contract
				List<CashPositionDTO> cashPositions = getCashPositions(marginCall, dsConn, this.jdate);

				// get report row for contract
				final List<SantCollateralPositionSendBalanceItem> positionReportRows = getPositionReportsRows(
						secPositions, cashPositions, marginCall);

				if (positionReportRows != null) {
					// add row lines to reportRow
					for (int j = 0; j < positionReportRows.size(); j++) {
						reportRows.add(new ReportRow(positionReportRows.get(j)));

					}
				}

				ReportRow[] reportOutput2 = new ReportRow[reportRows.size()];

				if ((reportRows != null) && (reportRows.size() > 0)) {
					for (int i = 0; i < reportRows.size(); i++) {
						reportOutput2[i] = reportRows.get(i);
					}
				}

				reportOutput.setRows(reportOutput2);
			}
		} catch (final Exception e) {
			Log.error(this, "Cannot load MarginCallAllocationEntries", e);
			errors.add(e.getMessage());
		}

		return reportOutput;
	}

	// get report row for contract
	public List<SantCollateralPositionSendBalanceItem> getPositionReportsRows(
			List<SecurityPositionDTO> securityPositions, List<CashPositionDTO> cashPositions,
			CollateralConfig marginCall) {

		List<SantCollateralPositionSendBalanceItem> posiciones = new ArrayList<SantCollateralPositionSendBalanceItem>();

		if (cashPositions != null) {
			// add all cash positions to List
			posiciones.addAll(getPosicionesCash(cashPositions, marginCall));
		}

		if (securityPositions != null) {
			// add all security positions to List
			posiciones.addAll(getPosicionesSec(securityPositions, marginCall));
		}

		return posiciones;

	}

	// add all cash positions to List
	private List<SantCollateralPositionSendBalanceItem> getPosicionesCash(List<CashPositionDTO> cashPositions,
			CollateralConfig marginCall) {

		List<SantCollateralPositionSendBalanceItem> posiciones = new ArrayList<SantCollateralPositionSendBalanceItem>();

		for (int i = 0; i < cashPositions.size(); i++) {

			posiciones.add(rellenarItemCash(cashPositions.get(i), marginCall));
		}

		return posiciones;
	}

	// add all security positions to List
	private List<SantCollateralPositionSendBalanceItem> getPosicionesSec(List<SecurityPositionDTO> securityPositions,
			CollateralConfig marginCall) {

		List<SantCollateralPositionSendBalanceItem> posiciones = new ArrayList<SantCollateralPositionSendBalanceItem>();

		for (int i = 0; i < securityPositions.size(); i++) {

			posiciones.add(rellenarItemSec(securityPositions.get(i), marginCall));

		}

		return posiciones;
	}

	private SantCollateralPositionSendBalanceItem rellenarItemCash(CashPositionDTO posicionCash,
			CollateralConfig marginCall) {

		SantCollateralPositionSendBalanceItem posicion = new SantCollateralPositionSendBalanceItem();

		posicion.setContractID(Integer.toString(marginCall.getId()));
		posicion.setContractType(marginCall.getContractType());
		posicion.setName(marginCall.getLegalEntity().getName());
		posicion.setPositionType("Cash");
		posicion.setIsin("");
		posicion.setNominal(new BigDecimal(posicionCash.getValue()).setScale(2, RoundingMode.HALF_DOWN).toString());
		posicion.setDirtyPrice("");
		posicion.setCurrency(posicionCash.getCurrency());
		posicion.setHaircut(Double.toString(posicionCash.getHaircut() * 100));
		posicion.setValue(new BigDecimal(posicionCash.getAllInValue()).setScale(2, RoundingMode.HALF_DOWN).toString());
		posicion.setFxRate(new BigDecimal(posicionCash.getFxRate()).setScale(2, RoundingMode.HALF_DOWN).toString());
		posicion.setBaseCCY(posicionCash.getBaseCurrency());
		posicion.setValueInBaseCCY(new BigDecimal(posicionCash.getBaseValue()).setScale(2, RoundingMode.HALF_DOWN)
				.toString());
		posicion.setMaturity("");
		posicion.setNextCouponDate("");

		return posicion;
	}

	private SantCollateralPositionSendBalanceItem rellenarItemSec(SecurityPositionDTO posicionSec,
			CollateralConfig marginCall) {

		SantCollateralPositionSendBalanceItem posicion = new SantCollateralPositionSendBalanceItem();

		posicion.setContractID(Integer.toString(marginCall.getId()));
		posicion.setContractType(marginCall.getContractType());
		posicion.setName(marginCall.getLegalEntity().getName());
		posicion.setPositionType("Security");
		posicion.setIsin(posicionSec.getProduct().getSecCode("ISIN"));
		posicion.setNominal(new BigDecimal(posicionSec.getNominal()).setScale(2, RoundingMode.HALF_DOWN).toString());
		posicion.setDirtyPrice(new BigDecimal((posicionSec.getValue() / posicionSec.getNominal()) * 100).setScale(2,
				RoundingMode.HALF_DOWN).toString());
		posicion.setCurrency(posicionSec.getCurrency());
		posicion.setHaircut(Double.toString(posicionSec.getHaircut() * 100));
		posicion.setValue(new BigDecimal(posicionSec.getValue()).setScale(2, RoundingMode.HALF_DOWN).toString());
		posicion.setFxRate(new BigDecimal(posicionSec.getFxRate()).setScale(2, RoundingMode.HALF_DOWN).toString());
		posicion.setBaseCCY(posicionSec.getBaseCurrency());
		posicion.setValueInBaseCCY(new BigDecimal(posicionSec.getBaseValue()).setScale(2, RoundingMode.HALF_DOWN)
				.toString());
		if ((posicionSec.getSecurityMaturityDate()) != null) {
			posicion.setMaturity(posicionSec.getSecurityMaturityDate().toString());
		} else {
			posicion.setMaturity("");
		}
		if ((posicionSec.getNextCouponDate()) != null) {
			posicion.setNextCouponDate(posicionSec.getNextCouponDate().toString());
		} else {
			posicion.setNextCouponDate("");
		}
		return posicion;

	}

	public List<SecurityPositionDTO> getSecurities(final CollateralConfig marginCall, final DSConnection dsConn,
			JDate date) {
		final List<Integer> mccID = new ArrayList<Integer>();
		mccID.add(marginCall.getId());
		try {
			// get entry for date and contract
			final List<MarginCallEntryDTO> entries = CollateralManagerUtil.loadMarginCallEntriesDTO(mccID, date);
			if ((entries != null) && (entries.size() > 0)) {
				// get security positions
				if (entries.get(0).getPreviousSecurityPosition() != null) {
					return entries.get(0).getPreviousSecurityPosition().getPositions();
				}
			}
		} catch (final RemoteException e) {
			Log.error(this, "Cannot get marginCallEntry for the contract" + "\n" + e); //sonar
		}
		return null;
	}

	public List<CashPositionDTO> getCashPositions(final CollateralConfig marginCall, final DSConnection dsConn,
			JDate date) {
		final List<Integer> mccID = new ArrayList<Integer>();
		mccID.add(marginCall.getId());
		try {
			// get entry for date and contract
			final List<MarginCallEntryDTO> entries = CollateralManagerUtil.loadMarginCallEntriesDTO(mccID, date);
			if ((entries != null) && (entries.size() > 0)) {
				// get cash positions
				// Fix 5.4 BAU
				if (entries.get(0).getPreviousCashPosition() != null) {
					return entries.get(0).getPreviousCashPosition().getPositions();
				}
			}
		} catch (final RemoteException e) {
			Log.error(this, "Cannot get marginCallEntry for the contract" + "\n" + e); //sonar
		}
		return null;
	}

}