/* Actualizado por David Porras Mart?nez 22-11-11 */

package calypsox.tk.report;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.ControlMErrorLogger;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.collateral.CollateralManagerUtil;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MarginCallPositionUtil;

import java.rmi.RemoteException;
import java.util.*;

public class OptCustAgreementParamsReport extends MarginCallReport {

    private static final long serialVersionUID = -8296629823865410802L;
    public static final String PO = "PO";
    public static final String LEGAL_ENTITY_ROLE_PROCESSING_ORG = "ProcessingOrg";
    private static final String AGREEMENT_STATUS_CLOSED = "CLOSED";

    // private static final String CONTRACT_TYPE_MMOO = "MMOO";

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public ReportOutput load(final Vector errorMsgsP) {

        final DefaultReportOutput output = new StandardReportOutput(this);
        final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();
        Collection<LegalEntity> legalEntities = new Vector<LegalEntity>();
        List<CollateralConfig> marginCalls = new Vector<CollateralConfig>();
        final MarginCallPositionUtil mcPositionUtil = new MarginCallPositionUtil();

        JDate jdate = null;
        final DSConnection dsConn = getDSConnection();
        final PricingEnv pricingEnv = getPricingEnv();

        final ReportTemplate reportTemp = getReportTemplate();

        // We retrieve the different columns specified for the current report,
        // and the attribute 'Processing Org' to filter the data retrieved.
        final String[] columns = reportTemp.getColumns();
        //final Attributes attributes = reportTemp.getAttributes();
        Vector holidays = reportTemp.getHolidays();
        final ArrayList<Integer> blackList = new ArrayList<Integer>();

        try {

            // get date
            jdate = reportTemp.getValDate();

            // get legal entities
            //legalEntities = getLegalEntities((String) attributes.get(PO), dsConn);
            // GSM 24/07/15. SBNA Multi-PO filter
            legalEntities = CollateralUtilities.filterLEPoByTemplate(reportTemp);

            if (!Util.isEmpty(legalEntities)) {
                for (LegalEntity legalEntity : legalEntities) {
                    if (legalEntity != null) {
                        // get contracts PO & additional POs
                        marginCalls = loadContracts(legalEntity.getId(), jdate);
                        if (!Util.isEmpty(marginCalls)) {
                            for (CollateralConfig marginCall : marginCalls) {
                                if (marginCall != null) {
                                    // check contract
                                    if (checkContract(marginCall, blackList)) {
                                        // get report row for contract

                                        final Vector<OptCustAgreementParamsItem> marginCallReportRows = OptCustAgreementParamsLogic
                                                .getReportRows(marginCall, mcPositionUtil, jdate, dsConn, errorMsgsP,
                                                        columns, pricingEnv, getServiceRegistry(), holidays);

                                        for (int j = 0; j < marginCallReportRows.size(); j++) {

                                            // GSM: 16/12/2014 - Optimizer incidence, expects MCC info
                                            final ReportRow row = new ReportRow(marginCallReportRows.get(j),
                                                    OptCustAgreementParamsItem.OPT_MARGINCALL_ITEM);
                                            row.setProperty(OptCustAgreementParamsItem.OPT_MARGIN_CALL_CONFIG,
                                                    marginCall);
                                            reportRows.add(row);


                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // set report rows on output
            output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
            return output;

        } catch (final RemoteException e) {
            Log.error(this, "KGR_Collateral_MarginCallReport - " + e.getMessage());
            Log.error(this, e); //sonar
            ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, "Not document generated");
        }

        return null;
    }


//	private Vector<LegalEntity> getLegalEntities(String attributePO, DSConnection dsConn) throws RemoteException {
//
//		if (!Util.isEmpty(attributePO)) {
//			return dsConn.getRemoteReferenceData().getAllLE(
//					"SHORT_NAME in " + Util.collectionToSQLString(Util.string2Vector(attributePO)));
//		} else {
//			return BOCache.getLegalEntitiesForRole(dsConn, LEGAL_ENTITY_ROLE_PROCESSING_ORG);
//		}
//	}

    public static boolean checkContract(CollateralConfig marginCall, ArrayList<Integer> blackList) {

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

        // Exclude CSA Facade
        if (KGR_Collateral_MarginCallReport.CSA_FACADE.equals(marginCall.getContractType())) {
            return false;
        }

        return true;
    }

    public static List<CollateralConfig> loadContracts(int ownerId) throws RemoteException {
        return loadContracts(ownerId, null);
    }

    public static List<CollateralConfig> loadContracts(int ownerId, JDate jDate) throws RemoteException {

        MarginCallConfigFilter mcFilter = new MarginCallConfigFilter();

        if (ownerId > 0) {
            List<Integer> list = new ArrayList<Integer>();
            list.add(ownerId);
            mcFilter.setProcessingOrgIds(list);
        }

        //mcFilter.setStatuses(Arrays.asList(new String[] { CollateralConfig.CLOSED }));

        List<CollateralConfig> marginCallConfig_prov = CollateralManagerUtil.loadCollateralConfigs(mcFilter);

        List<CollateralConfig> marginCallConfigs = new ArrayList<CollateralConfig>();

        for (CollateralConfig contract : marginCallConfig_prov) {

            if ((contract == null) || (contract.getContractType() == null)) {
                continue;
            }

            if (contract.getAgreementStatus().equals(CollateralConfig.CLOSED)) {
                continue;
            }

            if ((contract.getClosingDate() != null) && (jDate != null)
                    && contract.getClosingDate().before(jDate.getJDatetime(TimeZone.getDefault()))) {
                continue;
            }

            // GSM 10/10/2014. Add all contract, included MMOO!
            marginCallConfigs.add(contract);

            // if (!CONTRACT_TYPE_MMOO.equals(contract.getContractType())) {
            // marginCallConfigs.add(contract);
            // }
        }

        return marginCallConfigs;
    }
}
