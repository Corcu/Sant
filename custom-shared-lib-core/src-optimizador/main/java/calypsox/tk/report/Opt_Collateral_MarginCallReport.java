package calypsox.tk.report;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.ControlMErrorLogger;
import calypsox.util.collateral.CollateralManagerUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MarginCallPositionUtil;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

@SuppressWarnings("serial")
public class Opt_Collateral_MarginCallReport extends MarginCallReport {

    public static final String PO = "PO";
    public static final String LEGAL_ENTITY_ROLE_PROCESSING_ORG = "ProcessingOrg";
    private static final String AGREEMENT_STATUS_CLOSED = "CLOSED";

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public ReportOutput load(final Vector errorMsgsP) {

        final DefaultReportOutput output = new StandardReportOutput(this);
        final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();
        Vector<LegalEntity> legalEntities = new Vector<LegalEntity>();
        List<CollateralConfig> marginCalls = new Vector<CollateralConfig>();
        final MarginCallPositionUtil mcPositionUtil = new MarginCallPositionUtil();

        JDate jdate = null;
        final DSConnection dsConn = getDSConnection();
        final PricingEnv pricingEnv = getPricingEnv();

        final ReportTemplate reportTemp = getReportTemplate();

        // We retrieve the different columns specified for the current report,
        // and the attribute 'Processing Org' to filter the data retrieved.
        final String[] columns = reportTemp.getColumns();
        final Attributes attributes = reportTemp.getAttributes();
        Vector holidays = reportTemp.getHolidays();
        final ArrayList<Integer> blackList = new ArrayList<Integer>();

        try {

            // get date
            jdate = reportTemp.getValDate();

            // get legal entities
            legalEntities = getLegalEntities((String) attributes.get(PO),
                    dsConn);

            if (!Util.isEmpty(legalEntities)) {
                for (LegalEntity legalEntity : legalEntities) {
                    if (legalEntity != null) {
                        // get contracts
                        marginCalls = loadContracts(legalEntity.getId());
                        if (!Util.isEmpty(marginCalls)) {
                            for (CollateralConfig marginCall : marginCalls) {
                                if (marginCall != null) {
                                    // check contract
                                    if (checkContract(marginCall, blackList)) {
                                        // get report row for contract
                                        final Vector<Opt_Collateral_MarginCallItem> marginCallReportRows = Opt_Collateral_MarginCallLogic
                                                .getReportRows(marginCall,
                                                        mcPositionUtil, jdate,
                                                        dsConn, errorMsgsP,
                                                        columns, pricingEnv,
                                                        getServiceRegistry(),
                                                        holidays);
                                        for (int j = 0; j < marginCallReportRows
                                                .size(); j++) {
                                            reportRows
                                                    .add(new ReportRow(
                                                            marginCallReportRows
                                                                    .get(j)));
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
            Log.error(this,
                    "Opt_Collateral_MarginCallReport - " + e.getMessage());
            Log.error(this, e); //sonar
            ControlMErrorLogger.addError(
                    ErrorCodeEnum.OutputCVSFileCanNotBeWritten,
                    "Not document generated");
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Vector<LegalEntity> getLegalEntities(String attributePO,
                                                 DSConnection dsConn) throws RemoteException {

        if (!Util.isEmpty(attributePO)) {
            return dsConn.getRemoteReferenceData().getAllLE(
                    "SHORT_NAME = '" + attributePO + "'", null);
        } else {
            return BOCache.getLegalEntitiesForRole(dsConn,
                    LEGAL_ENTITY_ROLE_PROCESSING_ORG);
        }

    }

    private boolean checkContract(CollateralConfig marginCall,
                                  ArrayList<Integer> blackList) {

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

    private List<CollateralConfig> loadContracts(int ownerId)
            throws RemoteException {

        MarginCallConfigFilter mcFilter = new MarginCallConfigFilter();

        List<Integer> list = new ArrayList<Integer>();
        list.add(ownerId);

        mcFilter.setProcessingOrgIds(list);

        List<CollateralConfig> marginCallConfigs = CollateralManagerUtil
                .loadCollateralConfigs(mcFilter);

        return marginCallConfigs;
    }

}
