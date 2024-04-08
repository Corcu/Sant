package calypsox.tk.report;

import calypsox.tk.bo.cremapping.util.BOCreConstantes;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Trade;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import org.jfree.util.Log;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * AccountEnrichmentReportStyle
 *
 * @author x933435
 */
public class AccountEnrichmentReportStyle extends com.calypso.tk.report.AccountEnrichmentReportStyle {

    /**
     * The default serial version UID
     */
    private static final long serialVersionUID = 9154094153810430167L;

    /**
     * Cre Sent Status report column, get CRE Set attribute value
     */
    public static final String CRE_SENT_STATUS = "Cre Sent Status";

    /**
     * Contrato Partenon MIC report column, get <CONTRATO></CONTRATO> MIC response
     */
    public static final String CONTRATO_PARTENON_MIC = "Contrato Partenon MIC";

    /**
     * Netted Trade IDs report column, list of netted trades
     */
    public static final String NETTED_TRADE_ID = "Netted Trades Id";

    /**
     * Netted Trade PartenonAccountingID KW report column
     */
    public static final String NETTED_TRADE_PARTENON_ACCOUNTING_ID = "Netted Trades PartenonAccountingID";

    /**
     * Netted Trade MurexRootContract KW report column
     */
    public static final String NETTED_TRADE_MUREX_ROOT_CONTRACT = "Netted Trades MurexRootContract";

    /**
     * Cre resettled report column
     */
    public static final String IS_RESETTLED = "IsResettled";

    /**
     * Separator for list of elements
     */
    private static final String LIST_SEPARATOR = ";";

    /**
     * PartenonAccountingID trade KW name
     */
    private static final String PARTENON_ACCOUNTING_ID = "PartenonAccountingID";

    /**
     * MurexRootContract trade KW name
     */
    private static final String MUREX_ROOT_CONTRACT = "MurexRootContract";


    /**
     * Transfer attribute Failed name
     */
    private static final String FAILED = "Failed";

    /**
     * BOCre SETTLED event type
     */
    private static final String SETTLED = "SETTLED";

    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {
        if (row == null) {
            return null;
        } else {
            BOCre boCre = row.getProperty(ReportRow.ACCOUNT_ENRICHMENT);
            if (boCre != null) {
                if (CRE_SENT_STATUS.equals(columnId)) {
                    return getBOCreAttributeValue(boCre, BOCreConstantes.SENT);
                } else if (CONTRATO_PARTENON_MIC.equals(columnId)) {
                    return getBOCreAttributeValue(boCre, BOCreConstantes.CONTRATO_MIC);
                } else if (NETTED_TRADE_ID.equals(columnId)) {
                    return getListOfNettedTradeIds(boCre);
                } else if (NETTED_TRADE_PARTENON_ACCOUNTING_ID.equals(columnId)) {
                    return getListOfNettedTradeKeywordValues(boCre, PARTENON_ACCOUNTING_ID);
                } else if (NETTED_TRADE_MUREX_ROOT_CONTRACT.equals(columnId)) {
                    return getListOfNettedTradeKeywordValues(boCre, MUREX_ROOT_CONTRACT);
                } else if (IS_RESETTLED.equals(columnId)) {
                    return isResettled(boCre);
                }
            }
        }
        return super.getColumnValue(row, columnId, errors);
    }

    /**
     * Return the BOCre attribute value if not null
     *
     * @param boCre    the BOCre
     * @param attrName the BOCre attribute name
     * @return the BOCre attribute value
     */
    private String getBOCreAttributeValue(BOCre boCre, String attrName) {
        return Optional.ofNullable(boCre).map(c -> c.getAttributeValue(attrName)).
                filter(v -> !Util.isEmpty(v) && !"null".equalsIgnoreCase(v)).orElse("");
    }

    /**
     * Get the list of underlying netted transfers
     *
     * @param boCre the BOCre
     * @return the list of underlying netted transfers
     */
    private List<BOTransfer> getUnderlyingNettedTransfers(BOCre boCre) {
        if (boCre != null && boCre.getNettedTransferLongId() > 0L) {
            try {
                return Optional.ofNullable(DSConnection.getDefault().getRemoteBackOffice().
                                getNettedTransfers(boCre.getNettedTransferLongId())).map(t -> t.stream().
                                sorted((o1, o2) -> (int) (o1.getLongId() - o2.getLongId())).collect(Collectors.toList())).
                        orElse(null);
            } catch (CalypsoServiceException e) {
                Log.error(this, e);
            }
        }
        return null;
    }

    /**
     * Get the list of underlying netted trades
     *
     * @param boCre the BOCre
     * @return the list of underlying netted trades
     */
    private List<Trade> getTradesUnderlyingNettedTransfer(BOCre boCre) {
        List<BOTransfer> underlyingNettedTransfers = getUnderlyingNettedTransfers(boCre);
        return underlyingNettedTransfers != null ? underlyingNettedTransfers.stream().
                map(this::getTrade).filter(Objects::nonNull).collect(Collectors.toList()) : null;

    }

    /**
     * Get trade from BOTransfer
     *
     * @param transfer the transfer object
     * @return the trade from DB
     */
    private Trade getTrade(BOTransfer transfer) {
        if (transfer != null && transfer.getTradeLongId() > 0L) {
            try {
                return DSConnection.getDefault().getRemoteTrade().getTrade(transfer.getTradeLongId());
            } catch (CalypsoServiceException e) {
                Log.error(this, e);
            }
        }
        return null;
    }

    /**
     * Return a list of netted trade ids
     *
     * @param boCre the BOCre
     * @return the list of netted trade ids
     */
    private String getListOfNettedTradeIds(BOCre boCre) {
        List<BOTransfer> nettedTransfers = getUnderlyingNettedTransfers(boCre);
        return nettedTransfers != null ? Util.collectionToString(nettedTransfers.stream().
                map(BOTransfer::getTradeLongId).filter(tId -> tId > 0L).
                collect(Collectors.toList()), LIST_SEPARATOR) : "";
    }

    /**
     * Returns a list of keyword values for the netted trades
     *
     * @param boCre       the BOCre
     * @param keywordName the keyword name
     * @return a list of keyword values for the netted trades
     */
    private String getListOfNettedTradeKeywordValues(BOCre boCre, String keywordName) {
        if (!Util.isEmpty(keywordName)) {
            List<Trade> trades = getTradesUnderlyingNettedTransfer(boCre);
            return trades != null ? Util.collectionToString(trades.stream().
                    map(t -> t.getKeywordValue(keywordName)).
                    filter(v -> !Util.isEmpty(v)).collect(Collectors.toList()), LIST_SEPARATOR) : "";
        }
        return "";
    }

    /**
     * Check if BOCre is resettled, transfer fail + original event SETTLED
     *
     * @param boCre the BOCre
     * @return true if resettled
     */
    private boolean isResettled(BOCre boCre) {
        String originalEventType = boCre.getOriginalEventType();
        boolean failed = Boolean.parseBoolean(Optional.ofNullable(this.getBOTransfer(boCre.getTransferLongId())).
                map(boTransfer -> boTransfer.getAttribute(FAILED)).orElse("false"));
        return failed && !Util.isEmpty(originalEventType) && originalEventType.contains(SETTLED);
    }

    /**
     * Get the BOTransfer by id
     *
     * @param transferId the transfer id
     * @return the BOTransfer
     */
    private BOTransfer getBOTransfer(long transferId) {
        if (transferId > 0L) {
            try {
                return DSConnection.getDefault().getRemoteBackOffice().getBOTransfer(transferId);
            } catch (CalypsoServiceException e) {
                Log.error(this, e);
            }
        }
        return null;
    }
}