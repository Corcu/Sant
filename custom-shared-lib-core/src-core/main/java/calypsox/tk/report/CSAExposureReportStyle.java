package calypsox.tk.report;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.core.Trade;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.InvalidParameterException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

@SuppressWarnings("serial")
public class CSAExposureReportStyle extends TradeReportStyle {
    // Constants used for the column names.
    private static final String MTM_VALUE = "MTM Base ccy";
    private static final String MTM_CURR = "Base CcY";
    private static final String MTM_DATE = "MTM_DATE";
    private static final String NOTIONAL = "Principal";
    private static final String NOTIONAL2 = "Principal 2";
    private static final String TRADE_CURR = "Principal CCY";
    private static final String TRADE_CURR2 = "Principal 2 CCY";

    private static final String FLOAT_RATE = "Rate";
    private static final String FLOAT_RATE2 = "Rate2";

    private static final String UNDERLYING = "Underlying";
    private static final String TRADE_DATE = "Trade Date";
    private static final String MAT_DATE = "Maturity Date";
    private static final String PAY_REC = "Buy Sell";

    // new
    private static final String COLLAT_AGREE = "Collateral agree";
    private static final String COLLAT_AGREE_TYPE = "Collateral Agree Type";
    private static final String OWNER = "Owner";
    private static final String DEAL_OWNER = "Deal Owner";
    private static final String IND_AMOUNT = "Ind# Amount";
    private static final String STRUCT = "Structure";
    private static final String VAL_AGENT = "Valuation Agent";
    private static final String CPTY = "CounterParty";
    private static final String TRADE_ID = "TRADE_ID";

    // GSM: new portfolio reconciliation
    private static final String USI_REFERENCE = "USI Reference";
    private static final String SD_MSP = "SD MSP";
    private static final String US_PARTY = "US Party";
    private static final String DFA_APPLICABLE = "DFA Applicable";
    private static final String FC_NFC = "fc nfc nfc+";
    private static final String EMIR_APPLICABLE = "EMIR Applicable";
    // GSM: 22/08/13. Added the 7? field for Port. Reconciliation
    private final static String UTI_APPLICABLE = "UTI Reference";

    //new spanish colums
    private static final String MTM_DATE_SP = "MTM_DATE sp";
    private static final String TRADE_DATE_SP = "Trade Date sp";
    private static final String MAT_DATE_SP = "Maturity Date sp";
    private static final String START_TRADE_DATE_SP = "Start Date sp";

    // COL_OUT_016
    // This cache stores the last trades used to generate the report
    private static final int TRADES_CACHE_SIZE = 10;
    private final Map<Long, Trade> tradesCache = new LinkedHashMap<Long, Trade>(TRADES_CACHE_SIZE) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Trade> eldest) {
            return size() >= TRADES_CACHE_SIZE;
        }
    };

    @Override
    public TreeList getTreeList() {
        final TreeList treeList = super.getTreeList();
        treeList.add(NOTIONAL);
        treeList.add(TRADE_CURR);
        treeList.add(NOTIONAL2);
        treeList.add(TRADE_CURR2);
        treeList.add(MTM_DATE);
        treeList.add(MTM_VALUE);
        treeList.add(MTM_CURR);
        treeList.add(FLOAT_RATE);
        treeList.add(FLOAT_RATE2);
        treeList.add(UNDERLYING);
        treeList.add(TRADE_DATE);
        treeList.add(MAT_DATE);
        treeList.add(PAY_REC);
        treeList.add(COLLAT_AGREE);
        treeList.add(COLLAT_AGREE_TYPE);
        treeList.add(OWNER);
        treeList.add(DEAL_OWNER);
        treeList.add(IND_AMOUNT);
        treeList.add(STRUCT);
        treeList.add(VAL_AGENT);
        treeList.add(CPTY);
        treeList.add(TRADE_ID);
        treeList.add(USI_REFERENCE);
        treeList.add(SD_MSP);
        treeList.add(US_PARTY);
        treeList.add(DFA_APPLICABLE);
        treeList.add(FC_NFC);
        treeList.add(EMIR_APPLICABLE);
        treeList.add(UTI_APPLICABLE);
        treeList.add(MTM_DATE_SP);
        treeList.add(TRADE_DATE_SP);
        treeList.add(MAT_DATE_SP);
        treeList.add(START_TRADE_DATE_SP);


        return treeList;
    }

    @Override
    public Object getColumnValue(final ReportRow row, final String columnName,
                                 @SuppressWarnings("rawtypes") final Vector errors) throws InvalidParameterException {

        final CSAExposureItem item = row.getProperty(CSAExposureItem.CSA_EXPOSURE_ITEM);

        if (columnName.compareTo(MTM_VALUE) == 0) {
            return item.getMtmValue();
        } else if (columnName.compareTo(MTM_CURR) == 0) {
            return item.getMtmCurr();
        } else if (columnName.compareTo(MTM_DATE) == 0) {
            return item.getMtmDate();
        } else if (columnName.compareTo(NOTIONAL) == 0) {
            return item.getNotional();
        } else if (columnName.compareTo(NOTIONAL2) == 0) {
            return item.getNotional2();
        } else if (columnName.compareTo(TRADE_CURR) == 0) {
            return item.getTradeCurr();
        } else if (columnName.compareTo(TRADE_CURR2) == 0) {
            return item.getTradeCurr2();
        } else if (columnName.compareTo(FLOAT_RATE) == 0) {
            return item.getFloatRate();
        } else if (columnName.compareTo(FLOAT_RATE2) == 0) {
            return item.getFloatRate2();
        } else if (columnName.compareTo(UNDERLYING) == 0) {
            return item.getUnderlying();
        } else if (columnName.compareTo(TRADE_DATE) == 0) {
            return item.getTradeDate();
        } else if (columnName.compareTo(MAT_DATE) == 0) {
            return item.getMatDate();
        } else if (columnName.compareTo(PAY_REC) == 0) {
            return item.getPayRec();
        } else if (columnName.compareTo(COLLAT_AGREE) == 0) {
            return item.getCollatAgree();
        } else if (columnName.compareTo(COLLAT_AGREE_TYPE) == 0) {
            return item.getCollatAgreeType();
        } else if (columnName.compareTo(OWNER) == 0) {
            return item.getOwner();
        } else if (columnName.compareTo(DEAL_OWNER) == 0) {
            return item.getOwner();
        } else if (columnName.compareTo(IND_AMOUNT) == 0) {
            return item.getIndAmount();
        } else if (columnName.compareTo(STRUCT) == 0) {
            return item.getStructure();
        } else if (columnName.compareTo(VAL_AGENT) == 0) {
            return item.getValAgent();
        } else if (columnName.compareTo(CPTY) == 0) {
            return item.getCpty();
        } else if (columnName.compareTo(TRADE_ID) == 0) {
            return item.getTradeID();
        } else if (columnName.compareTo(USI_REFERENCE) == 0) {
            return item.getUsiReference();
        } else if (columnName.compareTo(MTM_DATE_SP) == 0) {
            return item.getMtmDatesp();
        } else if (columnName.compareTo(TRADE_DATE_SP) == 0) {
            return item.getTradeDatesp();
        } else if (columnName.compareTo(MAT_DATE_SP) == 0) {
            return item.getMatDatesp();
        } else if (columnName.compareTo(START_TRADE_DATE_SP) == 0) {
            return item.getStartTradeDatesp();
        } else if (columnName.compareTo(SD_MSP) == 0) {
            return item.getSdMsp();
        } else if (columnName.compareTo(US_PARTY) == 0) {
            return item.getUsParty();
        } else if (columnName.compareTo(DFA_APPLICABLE) == 0) {
            return item.getDfaApplicable();
        } else if (columnName.compareTo(FC_NFC) == 0) {
            return item.getFcNfc();
        } else if (columnName.compareTo(EMIR_APPLICABLE) == 0) {
            return item.getEmirApplicable();
            // GSM: 22/08/13. Added the 7? field for Port. Reconciliation
        } else if (columnName.compareTo(UTI_APPLICABLE) == 0) {
            return item.getUti();
        } else {
            Object columnValue = null;

            // COL_OUT_016
            // Carlos Cejudo: get compressed trade from report row
            //try {
            // Try to get the trade from the cache of trades
            Long tradeId = row.getProperty(CSAExposureReport.ROW_PROPERTY_TRADE_ID);
            Trade trade = this.tradesCache.get(tradeId);
            // If the trade is not in the cache take it from the row
            if (trade == null) {
                trade = getTrade(row);
                this.tradesCache.put(tradeId, trade);
            }
            row.setProperty(ReportRow.TRADE, trade);
            columnValue = super.getColumnValue(row, columnName, errors);
            /*} catch (IOException | ClassNotFoundException e) {
                Log.error(this, "CSAExposureReportStyle - " + e.getMessage());
                Log.error(this, e); //sonar
            }*/

            // Set trade and compressed trade properties to null. If this is the last cell using this trade we won't
            // need it anymore.
            row.setProperty(ReportRow.TRADE, null);
            row.setProperty(CSAExposureReport.ROW_PROPERTY_COMPRESSED_TRADE, null);

            return columnValue;
        }
    }

    private Trade getTrade(ReportRow row) {
        return row.getProperty(CSAExposureReport.ROW_PROPERTY_COMPRESSED_TRADE);
    }
    // COL_OUT_016

    /**
     * Retrieves a compressed trade from a report row.
     *
     * @param row The row to take the trade from
     * @return The trade stored in the row
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private Trade getCompressedTrade(ReportRow row) throws IOException, ClassNotFoundException {
        Trade trade = null;

        byte[] byteArray = (byte[]) row.getProperty(CSAExposureReport.ROW_PROPERTY_COMPRESSED_TRADE);
        ByteArrayInputStream byteInput = new ByteArrayInputStream(byteArray);
        GZIPInputStream gzipInput = new GZIPInputStream(byteInput);
        ObjectInputStream input = new ObjectInputStream(gzipInput);
        trade = (Trade) input.readObject();
        input.close();

        return trade;
    }
}