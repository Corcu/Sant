/* Actualizado por David Porras Mart?nez 22-11-11 */

package calypsox.tk.report;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.ControlMErrorLogger;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.collateral.CollateralManagerUtil;

import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.dto.SecurityAllocationDTO;
import com.calypso.tk.collateral.dto.SecurityPositionDTO;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.MarginCallReport;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;

/**
 * Report ELBE l50
 * 
 * @author David Porras
 * @version Added by Guillermo Changes MMOO 04/07
 * 
 */
public class ELBEIsinCollatReport extends MarginCallReport {

	private static final long serialVersionUID = -7804893879009553977L;
	public static final String ELBE_ISIN_COLLAT_REPORT = "ELBEIsinCollatReport";
	public static final String DATE_EXPORT = "Date to export";
	public static final String FORMAT_EXPORT = "Format date to export";
	public static final String PO = "PO";
	private static final String AGREEMENT_STATUS_CLOSED = "CLOSED";

	public static final String OLD_WAY = "New Method";
	
	// COL_OUT_019
	public static final String LEGAL_ENTITY_ROLE_PROCESSING_ORG = "ProcessingOrg";
	boolean oldway;

	@SuppressWarnings("unchecked")
	@Override
	public ReportOutput load(@SuppressWarnings("rawtypes") final Vector errorMsgsP) {

		final DefaultReportOutput output = new StandardReportOutput(this);
		final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();
		Collection<LegalEntity> legalEntities = new Vector<LegalEntity>();
		List<CollateralConfig> marginCalls = new Vector<CollateralConfig>();

		JDate jdate = null;
		final DSConnection dsConn = getDSConnection();
		final PricingEnv pricingEnv = getPricingEnv();
		final ReportTemplate reportTemp = getReportTemplate();
		oldway = checkOldWay();

		// We retrieve the different columns specified for the current report,
		// and the attribute 'Processing Org' to filter the data retrieved.
		// final Attributes attributes = reportTemp.getAttributes();
		final ArrayList<Integer> blackList = new ArrayList<Integer>();
		
		try {

			// Get date
			jdate = reportTemp.getValDate();

			// get legal entities // GSM 24/07/15. SBNA Multi-PO filter
			legalEntities = CollateralUtilities.filterLEPoByTemplate(reportTemp);
			// legalEntities = getLegalEntities((String) attributes.get(PO),
			// dsConn);

			if (!Util.isEmpty(legalEntities)) {
				// For each Legal Entity, we retrieve all margin calls
				// associated.
				for (LegalEntity legalEntity : legalEntities) {
					if (legalEntity != null) {
						// get contracts
						marginCalls = loadContracts(legalEntity.getId());
						if (!Util.isEmpty(marginCalls)) {
							for (CollateralConfig marginCall : marginCalls) {
								if (marginCall != null) {
									if (checkContract(marginCall, blackList)) {
										MarginCallEntryDTO entry = getEntries(marginCall, dsConn,jdate);													
										reportRows.addAll(execute(marginCall,entry,dsConn,jdate,pricingEnv,errorMsgsP));
										
									}
								}
							}
						}

					}
				}
			}

			output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
			return output;
		} catch (final RemoteException e) {
			Log.error(this, "ELBE_ISINCollatReport - " + e.getMessage());
			Log.error(this, e); // sonar
			ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, "Not document generated");// CONTROL-M
																												// ERROR:
		}

		return null;
	}

	public static String getActualDate() {

		final Date date = new Date();
		final SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
		final String stringDate = sdf.format(date);
		return stringDate;

	}

	public double getBalanceTitulo(final SecurityPositionDTO secPos) {
		return secPos.getContractValue();
	}

	public MarginCallEntryDTO getEntries(final CollateralConfig marginCall, final DSConnection dsConn,
			JDate date) {
		final List<Integer> mccID = new ArrayList<Integer>();
		mccID.add(marginCall.getId());
		try {
			// get entry for date and contract
			final List<MarginCallEntryDTO> entries = CollateralManagerUtil.loadMarginCallEntriesDTO(mccID, date);
			
			if ((entries != null) && (entries.size()>0)) {
				
				return entries.get(0);
			}
			
		} catch (final RemoteException e) {
			Log.error(this, "Cannot get marginCallEntry for the contract");
			Log.error(this, e); // sonar
		}
		return null;
	}

	private List<CollateralConfig> loadContracts(int ownerId) throws RemoteException {

		MarginCallConfigFilter mcFilter = new MarginCallConfigFilter();

		List<Integer> list = new ArrayList<Integer>();
		list.add(ownerId);

		mcFilter.setProcessingOrgIds(list);

		List<CollateralConfig> marginCallConfigs = CollateralManagerUtil.loadCollateralConfigs(mcFilter);

		return marginCallConfigs;
	}

	private boolean checkContract(CollateralConfig marginCall, ArrayList<Integer> blackList) {

		// check status
		if (AGREEMENT_STATUS_CLOSED.equals(marginCall.getAgreementStatus())) {
			return false;
		}
		// check black list
		if (blackList.contains(marginCall.getId())) {
			return false;
		}

		// add
		blackList.add(marginCall.getId());

		return true;
	}

	/**
	 * @param marginCall
	 * @param securityPositions
	 * @param dsConn
	 * @param jdate
	 * @param pricingEnv
	 * @param errorMsgsP
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private ArrayList<ReportRow> execute(CollateralConfig marginCall,
			MarginCallEntryDTO contractEntry, DSConnection dsConn, JDate jdate, PricingEnv pricingEnv,
			Vector errorMsgsP) {

		ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();

		HashMap<String, ELBEIsinCollatPositionItem> inventoryAssoc = inventoryAssoc(contractEntry, oldway, jdate);

		for (Entry<String, ELBEIsinCollatPositionItem> entry : inventoryAssoc.entrySet()) {
			
			ELBEIsinCollatPositionItem bean = entry.getValue();
						
			final Vector<ELBEIsinCollatItem> marginCallReportRows = ELBEIsinCollatLogic.getReportRows(marginCall,
					bean, dsConn, pricingEnv, getActualDate(), jdate, errorMsgsP, oldway);
			
			for (int j = 0; j < marginCallReportRows.size(); j++) {
				reportRows.add(new ReportRow(marginCallReportRows.get(j)));
			}
		}
		return reportRows;
	}

	/**
	 * Group positions by ISIN/Currency
	 * @param securityPositions
	 * @return 
	 */
	private HashMap<String, ELBEIsinCollatPositionItem> inventoryAssoc(MarginCallEntryDTO entry, boolean oldway, JDate jdate) {
		
		HashMap<String, ELBEIsinCollatPositionItem> posItems = new HashMap<>();
		if (entry != null) {
			if (entry.getPreviousSecurityPosition() != null && !Util.isEmpty(entry.getPreviousSecurityPosition().getPositions())) {
				for (SecurityPositionDTO positionDTO : entry.getPreviousSecurityPosition().getPositions()) {
					String key = positionDTO.getProduct().getSecCode("ISIN")+"/"+positionDTO.getProduct().getCurrency();
					if (oldway) {
						if (positionDTO != null && positionDTO.getValue() != 0) {
							if (posItems.containsKey(key)) {
								ELBEIsinCollatPositionItem bean = posItems.get(key);
								bean.setValue(bean.getValue()+positionDTO.getValue());
							} else {
								ELBEIsinCollatPositionItem bean = new ELBEIsinCollatPositionItem(positionDTO);
								posItems.put(key, bean);
							}
						}
					}else {
						if (positionDTO != null && positionDTO.getContractValue() != 0) {
							if (posItems.containsKey(key)) {
								ELBEIsinCollatPositionItem bean = posItems.get(key);
								bean.setBalanceTitulo(bean.getBalanceTitulo() + positionDTO.getContractValue());
							} else {
								ELBEIsinCollatPositionItem bean = new ELBEIsinCollatPositionItem(positionDTO);
								posItems.put(key, bean);
							}
						}
					}
				}
			}
			if (!Util.isEmpty(entry.getSecurityAllocations())) {	
				for (SecurityAllocationDTO allocationDTO : entry.getSecurityAllocations()) {
					String key = allocationDTO.getProduct().getSecCode("ISIN")+"/"+allocationDTO.getProduct().getCurrency();
					if (oldway) {
						if (allocationDTO != null && allocationDTO.getValue() != 0) {
							if (!(allocationDTO.getSettlementDate().after(jdate))) {
								if (posItems.containsKey(key)) {
									ELBEIsinCollatPositionItem bean = posItems.get(key);
									bean.setValue(bean.getValue()+allocationDTO.getValue());
																
								} else {
									ELBEIsinCollatPositionItem bean = new ELBEIsinCollatPositionItem(allocationDTO);
									posItems.put(key, bean);
								}
							}
						}
					} else {
						if (allocationDTO != null && allocationDTO.getContractValue() != 0) {
							if (!(allocationDTO.getSettlementDate().after(jdate))) {
								if (posItems.containsKey(key)) {
									ELBEIsinCollatPositionItem bean = posItems.get(key);
									bean.setBalanceTitulo(bean.getBalanceTitulo() + allocationDTO.getContractValue());
																
								} else {
									ELBEIsinCollatPositionItem bean = new ELBEIsinCollatPositionItem(allocationDTO);
									posItems.put(key, bean);
								}
							}
						}
					}
				}
				
			}
		}
		return posItems;
	}
	
	/**
	 * @return 
	 */
	private boolean checkOldWay() {
		final Boolean flag = (Boolean) getReportTemplate().get(OLD_WAY);
		if (flag != null) {
			return flag;
		}
		return false;
	}

}
