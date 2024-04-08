package calypsox.tk.report;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.ControlMErrorLogger;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TradeArray;

import java.rmi.RemoteException;
import java.util.*;

public class ReposTradeReport extends TradeReport {

    /**
     * Serial UID
     */
    private static final long serialVersionUID = 7054033623243906478L;

    public static final String REPOS_TRADE_REPORT = "ReposTradeReport";

    /*
    private static final String PRODUCT_REPO = "PRODUCT_REPO";
    private static final int NUM_TRADES_PER_STEP = 999;
    */
    private transient ReposTradeLogic reposTradeLogic = new ReposTradeLogic();

    @SuppressWarnings("unchecked")
    @Override
    public ReportOutput load(@SuppressWarnings("rawtypes") final Vector errorMsgsP) {
        final DefaultReportOutput output = new StandardReportOutput(this);
        final List<ReportRow> reportRows = new ArrayList<>();
        final DSConnection dsConn = getDSConnection();
        final PricingEnv pricingEnv = getPricingEnv();
        final ReportTemplate reportTemp = getReportTemplate();
        JDate jdate = reportTemp.getValDate();
        ArrayList<Trade> structRepos = new ArrayList<>();
        // 27/07/15. SBNA Multi-PO filter
        String poFilter = reportTemp.get("ProcessingOrg");
        if (Util.isEmpty(poFilter)) {
            poFilter = CollateralUtilities.filterPoIdsByTemplate(reportTemp);
        }
        HashSet<String> posAllowed = new HashSet<>(Util.string2Vector(poFilter));

        try {
/*            TradeArray derivatives = loadDerivatives(jdate);
            for (int k = 0; k < derivatives.size(); k++) {
                if ((derivatives.get(k).getKeywordValue("CONTRACT_TYPE") != null)
                        && (derivatives.get(k).getKeywordValue("CONTRACT_TYPE").equals("ISMA"))) {
                    structRepos.add(derivatives.get(k));
                }
            }*/

            TradeArray repos = loadRepos(jdate);

            for (int i = 0; i < repos.size(); i++) {

                // 27/07/15. SBNA Multi-PO filter
                if (filterOwners(posAllowed, repos.get(i))) {
                    continue;
                }
                //System.out.println("ReposTradeReport procesando trade:" + vTrades.get(i).getLongId() + " con MC_CONTRACT_NUMBER:" + vTrades.get(i).getKeywordAsInt("MC_CONTRACT_NUMBER"));
                final Vector<ReposTradeItem> reposTrdItem = reposTradeLogic.getReportRows(repos.get(i), jdate,
                        errorMsgsP, dsConn, pricingEnv);
                for (int j = 0; j < reposTrdItem.size(); j++) {
                    final ReportRow repRow = new ReportRow(reposTrdItem.get(j));
                    repRow.setProperty(ReportRow.TRADE, repos.get(i));
                    reportRows.add(repRow);
                }
            }

            output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
            return output;
        } catch (final RemoteException e) {
            Log.error(this, "ReposTradeReport - " + e.getMessage());
            Log.error(this, e); //sonar
            ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, "Not document generated");// CONTROL-M
            // ERROR
        }

        return null;
    }

      ///**
     //* @param jdate
     //* @return
     //* @throws CalypsoServiceException
     /*//*
    public TradeArray loadDerivatives(JDate jdate) throws CalypsoServiceException {
        String whereTradeDerivatives = "product_desc.product_type  = 'CollateralExposure' "
                + "AND product_desc.product_sub_type not in ('CONTRACT_IA', 'DISPUTE_ADJUSTMENT') "
                + "AND trunc(trade.trade_date_time) <= "
                + Util.date2SQLString(jdate)
                + " AND (product_desc.maturity_date is NULL or trunc(product_desc.maturity_date) >= "
                + Util.date2SQLString(jdate)
                + ")"
                + " AND (trade.trade_status = 'VERIFIED' OR "
                + "(trade.trade_status = 'MATURED' AND exists (select trade_id from pl_mark, pl_mark_value "
                + "where pl_mark.MARK_ID = pl_mark_value.MARK_ID AND trade.trade_id = pl_mark.trade_id AND "
                + "pl_mark_value.mark_name = 'NPV_BASE' AND pl_mark_value.mark_value != 0 AND "
                + "trunc(pl_mark.valuation_date) = " + Util.date2SQLString(jdate) + ")))";
        return DSConnection.getDefault().getRemoteTrade().getTrades("trade", whereTradeDerivatives, "trade.trade_id", null);
    }*/

    /**
     * @param jdate
     * @return
     * @throws CalypsoServiceException
     */
    public TradeArray loadRepos(JDate jdate) throws CalypsoServiceException {
        String whereTradeRepo = "trade.trade_status = 'VERIFIED' AND product_desc.product_type  = 'Repo' AND TRUNC(trade.trade_date_time) <= "
                + Util.date2SQLString(jdate)
                + " AND (product_desc.maturity_date is NULL or trunc(product_desc.maturity_date) > "
                + Util.date2SQLString(jdate) + ") ";
        return DSConnection.getDefault().getRemoteTrade().getTrades(null, whereTradeRepo, null, null);
    }

    /*
    private List<ReportRow> getTradesOldWay(DSConnection dsConn, String trades_where_clause_repo, TradeArray vTrades, HashSet<String> posAllowed, JDate jdate, List<ReportRow> reportRows, PricingEnv pricingEnv, Vector errorMsgsP, List<Trade> structRepos) throws CalypsoServiceException {

        // 1. PROCESA REPOS
        long[] tradeIds = dsConn.getRemoteTrade().getTradeIds(null, trades_where_clause_repo, 0, 0, null, null);
        //long[] tradeIds = {29866441};
        // MIG V16
        List<long[]> chunkedTradeIds = chunkTradeIdArray(tradeIds, NUM_TRADES_PER_STEP);
        for (long[] tradesInStep : chunkedTradeIds) {
            vTrades = dsConn.getRemoteTrade().getTrades(tradesInStep);

            for (int i = 0; i < vTrades.size(); i++) {

                // 27/07/15. SBNA Multi-PO filter
                if (filterOwners(posAllowed, vTrades.get(i))) {
                    continue;
                }
                //System.out.println("ReposTradeReport procesando trade:" + vTrades.get(i).getLongId() + " con MC_CONTRACT_NUMBER:" + vTrades.get(i).getKeywordAsInt("MC_CONTRACT_NUMBER"));
                final Vector<ReposTradeItem> reposTrdItem = reposTradeLogic.getReportRows(vTrades.get(i), jdate,
                        errorMsgsP, dsConn, pricingEnv);
                for (int j = 0; j < reposTrdItem.size(); j++) {
                    final ReportRow repRow = new ReportRow(reposTrdItem.get(j));
                    repRow.setProperty(ReportRow.TRADE, vTrades.get(i));
                    reportRows.add(repRow);
                }
            }
        }
        // 2. PROCESA REPOS ESTRUCTURADOS
        for (int i = 0; i < structRepos.size(); i++) {

            // 27/07/15. SBNA Multi-PO filter
            if (filterOwners(posAllowed, structRepos.get(i))) {
                continue;
            }

            final Vector<ReposTradeItem> reposTrdItem = reposTradeLogic.getReportRows(structRepos.get(i), jdate,
                    errorMsgsP, dsConn, pricingEnv);
            for (int j = 0; j < reposTrdItem.size(); j++) {
                final ReportRow repRow = new ReportRow(reposTrdItem.get(j));
                repRow.setProperty(ReportRow.TRADE, structRepos.get(i));
                reportRows.add(repRow);
            }
        }

        return reportRows;
    }*/

    /**
     * @param tradeIdArray
     * @param chunkSize
     * @return
     */
    public List<long[]> chunkTradeIdArray(long[] tradeIdArray, int chunkSize) {
        int numOfChunks = (int) Math.ceil((double) tradeIdArray.length / chunkSize);
        List<long[]> chunkedArrays = new ArrayList<>();
        for (int i = 0; i < numOfChunks; ++i) {
            int start = i * chunkSize;
            int length = Math.min(tradeIdArray.length - start, chunkSize);
            long[] temp = new long[length];
            System.arraycopy(tradeIdArray, start, temp, 0, length);
            chunkedArrays.add(temp);
        }
        //return chunkedArrays.stream();
        return chunkedArrays;
    }

    /**
     * @param posIdsAllowed
     * @param trade
     * @return if the owner of the MC to which the trade is linked is not included in the allowed ID set of POs
     */
    public static boolean filterOwners(final Set<String> posIdsAllowed, final Trade trade) {

        try {
            final CollateralConfig collateralContract = CollateralUtilities.getMarginCallConfig(trade,
                    new ArrayList<String>());

            return CollateralUtilities.filterOwners(posIdsAllowed, collateralContract);

        } catch (RemoteException e) {
            Log.error(CollateralUtilities.class, e);
        }

        return false;
    }

    // 27/07/15. SBNA Multi-PO filter
    // private boolean filterOwners(HashSet<String> posAllowed, Trade trade) {
    //
    // if ((posAllowed == null) || posAllowed.isEmpty()) {
    // return false;
    // }
    //
    // try {
    // final CollateralConfig collateralContract = CollateralUtilities.getMarginCallConfig(trade,
    // new ArrayList<String>());
    // if (collateralContract != null) {
    // LegalEntity po = collateralContract.getProcessingOrg();
    // return !posAllowed.contains(po.getId());
    // }
    //
    // } catch (RemoteException e) {
    // Log.error(this, e);
    // }
    //
    // return false;
    // }

    // COL_OUT_016

    /**
     * Get the end index of the next subarray to be taken.
     *
     * @param startIdx The start index of the subarray
     * @param length   The total length of the array from which the subarrays are taken
     * @param maxItems Maximum number of items in any subarray
     * @return The end index of the subarray
     * @author Carlos Cejudo
     */
    private int getEndIndex(int startIdx, int length, int maxItems) {
        int endIdx = (startIdx + maxItems);
        if (endIdx > length) {
            endIdx = length;
        }

        return endIdx;
    }
}
