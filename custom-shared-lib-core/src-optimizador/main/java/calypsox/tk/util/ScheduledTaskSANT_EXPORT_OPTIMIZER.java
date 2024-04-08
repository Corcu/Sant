package calypsox.tk.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import calypsox.tk.report.StandardReportOutput;
import calypsox.tk.util.optimizer.OptimizerStatusUtil;

import com.calypso.tk.collateral.dto.CashPositionDTO;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.dto.PreviousPositionDTO;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.ConnectionUtil;

public class ScheduledTaskSANT_EXPORT_OPTIMIZER extends ScheduledTaskCSVREPORT
		implements SantCollateralOptimConstants {

	private List<MarginCallEntryDTO> listEntriesToUpdate = new ArrayList<MarginCallEntryDTO>();

	public List<MarginCallEntryDTO> getListEntriesToUpdate() {
		return listEntriesToUpdate;
	}

	public void setListEntriesToUpdate(
			List<MarginCallEntryDTO> listEntriesToUpdate) {
		this.listEntriesToUpdate = listEntriesToUpdate;
	}

	/**
	 * Constructor
	 */
	public ScheduledTaskSANT_EXPORT_OPTIMIZER() {
		super();
	}

	@Override
	public String getTaskInformation() {
		return "Export Entries to the Optimizer";
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Vector getDomainAttributes() {
		Vector<String> domainAttributes = new Vector<String>();
		Vector<String> superDomainAttributes = super.getDomainAttributes();
		if (!Util.isEmpty(superDomainAttributes)) {
			domainAttributes.addAll(superDomainAttributes);
		}
		domainAttributes.add(EXECUTION_MODE);
		domainAttributes.add(OSLA_TYPE);
		return domainAttributes;
	}

	/**
	 * @see com.calypso.tk.util.ScheduledTask#getAttributeDomain(java.lang.String,
	 *      java.util.Hashtable)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Vector getAttributeDomain(String attr, Hashtable currentAttr) {
		if (EXECUTION_MODE.equals(attr)) {
			Vector v = new Vector();
			v.add(EXECUTION_MODE_SOD);
			v.add(EXECUTION_MODE_REPROCESS);
			return v;
		} else if (OSLA_TYPE.equals(attr)) {
			Vector v = new Vector();
			v.add(OSLA_TRUE);
			v.add(OSLA_FALSE);
			return v;
		}
		return super.getAttributeDomain(attr, currentAttr);
	}

	@Override
	protected String saveReportOutput(final ReportOutput reportOutput,
			String type, final String reportName, final String[] errors,
			final StringBuffer notifications) {
		final String delimiteur = getAttribute(DELIMITEUR);
		final String showheadings = getAttribute(SHOWHEADINGS);
		final String ctrlLine = getAttribute(CTRL_LINE);
		final String fileFormat = getAttribute(REPORT_FORMAT);

		boolean bShowHeadings = false;
		String type2 = "";
		// default will be showHeadings=false
		if ((showheadings != null) && showheadings.equals("true")) {
			bShowHeadings = true;
		} else {
			bShowHeadings = false;
		}

		Log.debug(Log.CALYPSOX, "Entering ScheduledTaskReport::reportViewer");

		if ((delimiteur == null) && !"Excel".equals(fileFormat)
				&& (reportOutput instanceof StandardReportOutput)) {
			((StandardReportOutput) reportOutput).setDelimiteur("@");
			setCheckDelim(1);
		}

		if ((reportOutput instanceof StandardReportOutput)
				&& (delimiteur != null) && !delimiteur.equals("")) {
			((StandardReportOutput) reportOutput).setDelimiteur(delimiteur);
		}
		if (reportOutput instanceof StandardReportOutput) {
			((StandardReportOutput) reportOutput)
					.setShowHeadings(bShowHeadings);
		}
		if (type.equals("txt")) {
			type2 = "txt"; // for KGR export
		}
		if (type.equals("dat")) {
			type2 = "dat"; // for KGR export
		}
		if (type.equals("txt") || type.equals("dat")) {
			type = "csv";
		}

		((DefaultReportOutput) reportOutput)
				.setRows(filterRows(((DefaultReportOutput) reportOutput)
						.getRows()));

		String reportStr = super.saveReportOutput(reportOutput, type,
				reportName, errors, notifications);

		// set extension
		String fileName = getFileName();
		if (fileName.startsWith("file://")) {
			fileName = fileName.substring(7);
		}

		if (type2.equals("txt")) {
			final String str1 = fileName
					.substring(0, fileName.lastIndexOf('.'));
			fileName = str1.concat(".txt");
		}

		if (type2.equals("dat")) {
			final String str1 = fileName
					.substring(0, fileName.lastIndexOf('.'));
			fileName = str1.concat(".dat");
		}

		// delete control separator for concrete cases (KGR)
		if (getCheckDelim() == 1) {
			reportStr = removeDelimiteurs(reportStr, '@');
		}

		// add header and footer if is required
		if ((getAttribute(START_HEADER) != null)
				&& (!getAttribute(START_HEADER).equals(""))) {
			reportStr = getAttribute(START_HEADER) + "\n" + reportStr;
		}

		if ((getAttribute(FOOTER) != null)
				&& (!getAttribute(FOOTER).equals(""))) {
			reportStr = reportStr + getAttribute(FOOTER);
		}
		// generate report file, with line control if is required
		if ((ctrlLine != null) && (ctrlLine.equals("false"))) {
			return super.generateReportFile(reportOutput, reportStr, fileName,
					false);
		} else {
			return super.generateReportFile(reportOutput, reportStr, fileName,
					true);
		}
	}

	protected String generateReportFile(final ReportOutput reportOutput,
			String reportString, final String fileName, final boolean ctrlLine) {
		if (ctrlLine) {
			final String controlLine = generateControlLine(reportOutput);
			reportString = reportString + controlLine;
		}
		try {
			final FileWriter writer = new FileWriter(fileName);
			writer.write(reportString);
			writer.close();

			updateEntriesSendStatus();
		} catch (final FileNotFoundException e) {
			Log.error(
					this,
					"The filename is not valid. Please configure the scheduled task with a valid filename: "
							+ fileName, e);
		} catch (final IOException e) {
			Log.error(this, "An error ocurred while writing the files: "
					+ fileName, e);
		}
		return reportString;
	}

	private void updateEntriesSendStatus() {
		// update status send to optimizer
		String actionToApply = "UPDATE";
		for (MarginCallEntryDTO marginCallEntryDTO : getListEntriesToUpdate()) {
			int entryId;
			try {
				// FIRST LOAD CURRENT MCE AS SOME FIELDS ARE NOT RETRIEVED
				// DURING CALYPSO REPORT LOAD
				marginCallEntryDTO = ServiceRegistry
						.getDefault(DSConnection.getDefault())
						.getCollateralServer()
						.loadEntry(marginCallEntryDTO.getId());

				addOptimizerStatusAttribute(marginCallEntryDTO);
				
				entryId = ServiceRegistry
						.getDefault(DSConnection.getDefault())
						.getCollateralServer()
						.save(marginCallEntryDTO, actionToApply,
								TimeZone.getDefault());

				Log.info(this, "Entry with id " + entryId
						+ " successfully saved for the contract "
						+ marginCallEntryDTO.getCollateralConfigId());

			} catch (RemoteException e) {
				Log.warn(this, e); //sonar
				MarginCallEntryDTO reloadedEntryDTO = null;
				// TODO limit the second save just to the mismatch version
				// error
				try {
					reloadedEntryDTO = ServiceRegistry
							.getDefault(DSConnection.getDefault())
							.getCollateralServer()
							.loadEntry(marginCallEntryDTO.getId());
					
					addOptimizerStatusAttribute(reloadedEntryDTO);
					
					entryId = ServiceRegistry
							.getDefault(DSConnection.getDefault())
							.getCollateralServer()
							.save(reloadedEntryDTO, actionToApply,
									TimeZone.getDefault());
					
					Log.info(this, "Entry with id " + entryId
							+ " successfully saved for the contract "
							+ marginCallEntryDTO.getCollateralConfigId());
				} catch (RemoteException e1) {
					Log.error(ScheduledTaskSANT_EXPORT_OPTIMIZER.class.getName(), e1);
				}
			}
		}
	}

	private void addOptimizerStatusAttribute(
			MarginCallEntryDTO marginCallEntryDTO) {
		// update optimizer status
		marginCallEntryDTO.addAttribute(
				OptimizerStatusUtil.OPTIMIZER_STATUS,
				OptimizerStatusUtil.UNDER_OPTIMIZATION);

		// TODO: delete with upgrade 1.6.3
		if (Util.isEmpty(marginCallEntryDTO.getCashPositions())) {
			marginCallEntryDTO.setCashPosition(new PreviousPositionDTO<CashPositionDTO>());
		}
	}



	private ReportRow[] filterRows(ReportRow[] rows) {
		ReportRow[] filteredRows = new ReportRow[rows.length];
		List<MarginCallEntryDTO> tmpListEntriesToUpdate = new ArrayList<MarginCallEntryDTO>();
		int cpt = 0;
		for (ReportRow row : rows) {
			MarginCallEntryDTO entry = (MarginCallEntryDTO) row
					.getProperty(ReportRow.DEFAULT);
			if (entry != null) {
				CollateralConfig mcc = CacheCollateralClient
						.getCollateralConfig(DSConnection.getDefault(),
								entry.getCollateralConfigId());
				Vector<String> sendValidStatuses = LocalCache.getDomainValues(
						DSConnection.getDefault(),
						OPTIMIZER_SEND_VALID_STATUS_DV);
				if (!Util.isEmpty(sendValidStatuses)
						&& sendValidStatuses.contains(entry.getStatus())) {
					if (OSLA_TRUE.equals(getAttribute(OSLA_TYPE))
							|| (OSLA_FALSE.equals(getAttribute(OSLA_TYPE)) && !OSLA_TYPE
									.equals(mcc.getContractType()))) {
						if (EXECUTION_MODE_SOD
								.equals(getAttribute(EXECUTION_MODE))) {
							if (!EXCLUDE_FROM_SOD_OPTIMIZER_TRUE_VALUE
									.equalsIgnoreCase(mcc
											.getAdditionalField(EXCLUDE_FROM_SOD_OPTIMIZER))) {
								filteredRows[cpt++] = row;
								tmpListEntriesToUpdate.add(entry);
							}
						} else {
							filteredRows[cpt++] = row;
							tmpListEntriesToUpdate.add(entry);
						}
					}
				}
			}
		}
		setListEntriesToUpdate(tmpListEntriesToUpdate);
		return filteredRows;
	}

	@SuppressWarnings({ "resource" })
	public static void main(String[] args) {

		DSConnection ds = null;
		try {
			// Starts connection to DataServer.
			ds = ConnectionUtil.connect(args,
					"ScheduledTaskSANT_EXPORT_OPTIMIZER");

			ScheduledTaskSANT_EXPORT_OPTIMIZER st = (ScheduledTaskSANT_EXPORT_OPTIMIZER) ds
					.getRemoteBackOffice().getScheduledTaskByExternalReference(
							"EXPORT_OPT_MC_STATUS_REPRO");

			SimpleDateFormat sdf = new SimpleDateFormat("HHmm");
			int i = Integer.valueOf(sdf.format(new JDatetime()));

			st.setValuationTime(i);

			st.setCurrentDate(JDate.getNow());

			st.getAttributes().put("REPORT FILE NAME", "TEST_MC");

			st.process(DSConnection.getDefault(), null);

			String fileName = st.getFileName();
			if (!Util.isEmpty(fileName) && fileName.startsWith("file://")
					&& fileName.length() > 7) {
				fileName = fileName.substring(7);
			}

			BufferedReader buf = new BufferedReader(new FileReader(new File(
					fileName)));

			String line = null;
			StringBuffer fileContent = new StringBuffer();
			while ((line = buf.readLine()) != null) {
				fileContent.append(line + "\n");
			}
		} catch (Exception e) {
			Log.error(Log.CALYPSOX, e);
			return;
		} finally {
			DSConnection.logout();
			System.exit(0);
		}
	}
}
