package calypsox.tk.report;

import calypsox.tk.report.generic.loader.margincall.SantMarginCallEntry;
import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.dto.CashPositionDTO;
import com.calypso.tk.collateral.dto.MarginCallDetailEntryDTO;
import com.calypso.tk.collateral.dto.SecurityPositionDTO;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SantAllDealsItem {

    private final String mccName;
    private Double bsteExposure;
    private Double bsnyExposure;
    private Double bshkExposure;
    private Double bdsdExposure;
    private Double totalExposure;
    private String exposureBaseCcy;

    // for each items there's a collaterals map where are stored all collaterals related to mcc processed and some data
    // about
    // them (type, value, contract value)
    private final HashMap<Integer, List<Object>> collateralsInfo = new HashMap<Integer, List<Object>>();

    // for each item there's a map to store some item values to use for retrieving in ReportStyle
    private final Map<String, Object> columnMap = new HashMap<String, Object>();

    /**
     * Create new SantAllDealItem: build exposure, collaterals and map to retrieve values
     *
     * @param SantMarginCallEntry mccEntry
     */
    public SantAllDealsItem(SantMarginCallEntry mccEntry) {

        this.mccName = mccEntry.getMarginCallConfig().getName();
        buildMccExposure(mccEntry);
        buildMccCollaterals(mccEntry);
        buildMap();

    }

    /**
     * Get mcc exposure info by po branch, mcc total exposure and exposure base currency
     *
     * @param SantMarginCallEntry mccEntry
     */
    private void buildMccExposure(SantMarginCallEntry mccEntry) {

        // get exposure for each po branch
        // BSTE
        this.bsteExposure = getMccPoBranchExposure("BSTE", mccEntry.getIncludedDetailEntries());
        // BSNY
        this.bsnyExposure = getMccPoBranchExposure("BSNY", mccEntry.getIncludedDetailEntries());
        // BSHK
        this.bshkExposure = getMccPoBranchExposure("BSHK", mccEntry.getIncludedDetailEntries());
        // BDSD
        this.bdsdExposure = getMccPoBranchExposure("BDSD", mccEntry.getIncludedDetailEntries());

        // get total exposure
        this.totalExposure = mccEntry.getEntry().getNetBalance();

        // get exposure ccy
        this.exposureBaseCcy = mccEntry.getMarginCallConfig().getCurrency();

    }

    /**
     * Get po branch contract exposure from contract detail entries
     *
     * @param String poBranch
     * @param List   <MarginCallDetailEntryDTO> mccDetailEntries
     * @return double poBranchExposure
     */
    public static Double getMccPoBranchExposure(String poBranch, List<MarginCallDetailEntryDTO> mccDetailEntries) {

        Double poBranchExposure = 0.0;
        boolean isExposure = false;

        // for each trade check if po's trade matches with processed po and update po exposure
        for (MarginCallDetailEntryDTO mccDetailEntry : mccDetailEntries) {
            try {
                Trade trade = DSConnection.getDefault().getRemoteTrade().getTrade(mccDetailEntry.getTradeId());
                String tradePo = trade.getBook().getLegalEntity().getAuthName();
                if (tradePo.equals(poBranch)) {
                    isExposure = true;
                    poBranchExposure += mccDetailEntry.getMarginCallValue();
                }
            } catch (RemoteException e) {
                Log.error("Error getting trade by mccDetailEntry->tradeId = " + mccDetailEntry.getTradeId(), e);
            }
        }

        return (isExposure == true) ? poBranchExposure : null;

    }

    /**
     * Get data about mcc collaterals
     *
     * @param mccEntry
     */
    private void buildMccCollaterals(SantMarginCallEntry mccEntry) {

        buildCashCollaterals(mccEntry);
        buildSecurityCollaterals(mccEntry);

    }

    /**
     * Get data about cash collaterals and save it using collaterals map
     *
     * @param SantMarginCallEntry mccEntry
     */
    private void buildCashCollaterals(SantMarginCallEntry mccEntry) {

        if (mccEntry.getEntry().getPreviousCashPosition() == null) {
            return;
        }

        List<CashPositionDTO> mccCashPositions = mccEntry.getEntry().getPreviousCashPosition().getPositions();

        if (!Util.isEmpty(mccCashPositions)) {
            for (CashPositionDTO mccCashPosition : mccCashPositions) {
                List<Object> cashValues = new ArrayList<Object>();
                cashValues.add(mccCashPosition.getCurrency());
                cashValues.add(mccCashPosition.getValue());
                cashValues.add(mccCashPosition.getCurrency());
                cashValues.add(mccCashPosition.getContractValue());
                this.collateralsInfo.put(this.collateralsInfo.size(), cashValues);
            }
        }

    }

    /**
     * Get data about security collaterals and save it using collaterals map
     *
     * @param SantMarginCallEntry mccEntry
     */
    private void buildSecurityCollaterals(SantMarginCallEntry mccEntry) {

        if (mccEntry.getEntry().getPreviousSecurityPosition() == null) {
            return;
        }

        List<SecurityPositionDTO> mccSecPositions = mccEntry.getEntry().getPreviousSecurityPosition().getPositions();

        if (!Util.isEmpty(mccSecPositions)) {
            for (SecurityPositionDTO mccSecPosition : mccSecPositions) {
                List<Object> secValues = new ArrayList<Object>();
                secValues.add(mccSecPosition.getDescription());
                secValues.add(mccSecPosition.getNominal());
                secValues.add(mccSecPosition.getCurrency());
                secValues.add(mccSecPosition.getContractValue());
                this.collateralsInfo.put(this.collateralsInfo.size(), secValues);
            }
        }

    }

    /**
     * Build map to store some item values to use for retrieving in ReportStyle class
     */
    public void buildMap() {
        this.columnMap.put(SantAllDealsReportStyle.MCC_NAME, this.mccName);
        this.columnMap.put(SantAllDealsReportStyle.BSTE_EXPOSURE, this.bsteExposure);
        this.columnMap.put(SantAllDealsReportStyle.BSNY_EXPOSURE, this.bsnyExposure);
        this.columnMap.put(SantAllDealsReportStyle.BDSD_EXPOSURE, this.bdsdExposure);
        this.columnMap.put(SantAllDealsReportStyle.BSHK_EXPOSURE, this.bshkExposure);
        this.columnMap.put(SantAllDealsReportStyle.TOTAL_EXPOSURE, this.totalExposure);
        this.columnMap.put(SantAllDealsReportStyle.EXPOSURE_BASE_CCY, this.exposureBaseCcy);
        this.columnMap.put(SantAllDealsReportStyle.EXPOSURE_BASE_CCY, this.exposureBaseCcy);
    }

    /**
     * Get item value matching with template column name received using values map
     *
     * @param columnName
     * @return
     */
    public Object getColumnValue(String columnName) {
        return this.columnMap.get(columnName);
    }

    /**
     * Get mcc collaterals map
     *
     * @return
     */
    public HashMap<Integer, List<Object>> getCollateralsInfo() {
        return this.collateralsInfo;
    }

}
