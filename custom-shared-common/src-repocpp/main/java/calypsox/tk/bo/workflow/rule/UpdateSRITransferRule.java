package calypsox.tk.bo.workflow.rule;

import calypsox.repoccp.ReconCCPConstants;
import com.calypso.tk.bo.*;
import com.calypso.tk.bo.workflow.WfTransferRule;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.PersistenceException;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.core.sql.TradeSQL;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import org.jfree.util.Log;

import java.sql.Connection;
import java.util.Vector;

/**
 * UpdateSettlementReferenceInstructedTransferRule
 * Pastes the SettlementReferenceInstructed and SettlementReferenceInstructedPlatform transfer attributes.
 *
 * @author Ruben Garcia
 */
public class UpdateSRITransferRule implements WfTransferRule {

    @Override
    public boolean check(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade,
                         Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        return true;
    }

    @Override
    public String getDescription() {
        return "This rule pastes the" + ReconCCPConstants.XFER_ATTR_SETTLEMENT_REF_INST +
                " and " + ReconCCPConstants.XFER_ATTR_SETTLEMENT_REF_INST_PLATFORM + " attributes " +
                "into the transfer.\n" +
                "Reads from the domain value " + ReconCCPConstants.UPDATE_XFER_SRI_DOMAIN_NAME + " the pair SDF\\KW and if the trade\\transfer " +
                "meets the criteria it pastes the KW on the transfer.\n" +
                "The name of the SDF is Platform_SRI, the Platform value will be pasted into the " + ReconCCPConstants.XFER_ATTR_SETTLEMENT_REF_INST_PLATFORM
                + " attribute. Example SENAF_SRI.\n" +
                "The KW value can be truncated by using the following name as a comment KWName[start,end]. Example MxElectplatid[1,6]";
    }

    @Override
    public boolean update(TaskWorkflowConfig wc, BOTransfer transfer, BOTransfer oldTransfer, Trade trade,
                          Vector messages, DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
        if (transfer != null && transfer.isPayment() && transfer.getTradeLongId() > 0) {
            if (trade == null) {
                try {
                    trade = dbCon != null ? TradeSQL.getTrade(transfer.getTradeLongId(), (Connection) dbCon)
                            : dsCon.getRemoteTrade().getTrade(transfer.getTradeLongId());
                } catch (PersistenceException | CalypsoServiceException e) {
                    Log.error(this, e);
                    excps.add(buildError(transfer.getTradeLongId(), String.format("Error updating %s, on transfer %d. %s: %s ",
                            ReconCCPConstants.XFER_ATTR_SETTLEMENT_REF_INST, transfer.getLongId(),
                            e.getClass().getSimpleName(), e.getLocalizedMessage())));
                    return false;
                }
            }

            if (trade == null) {
                excps.add(buildError(transfer.getTradeLongId(), String.format("Trade not found for update %s. Transfer: %s",
                        ReconCCPConstants.XFER_ATTR_SETTLEMENT_REF_INST, transfer)));
                //lock?
                return false;
            }

            String sdfPlatform = selectSDFPlatform(dsCon, transfer, trade);

            if (!Util.isEmpty(sdfPlatform)) {
                String keywordCode = LocalCache.getDomainValueComment(dsCon, ReconCCPConstants.UPDATE_XFER_SRI_DOMAIN_NAME, sdfPlatform);
                String valueSRI = null;
                if (!Util.isEmpty(keywordCode)) {
                    valueSRI = getTradeKeywordValue(keywordCode, trade, excps);
                    //If you want to stop the execution return false if null

                }
                String platformSRI = sdfPlatform.split("_SRI")[0];
                if (!Util.isEmpty(platformSRI)) {
                    transfer.setAttribute(ReconCCPConstants.XFER_ATTR_SETTLEMENT_REF_INST_PLATFORM, platformSRI);
                }
                if (!Util.isEmpty(valueSRI)) {
                    transfer.setAttribute(ReconCCPConstants.XFER_ATTR_SETTLEMENT_REF_INST, valueSRI);
                }
            }

        }
        return true;
    }

    /**
     * Get the trade keyword value to paste on the transfer SettlementReferenceInstructed attribute
     * If keyword code is empty the SRI does not paste
     * If keyword code contains KewordName[init,end] substring KW vale of the KeywordName
     * If keyword code contains KeywordName return KW value of the KeywordName
     *
     * @param keywordCode the comment of UpdateSettlementReferenceInstructed DV
     * @param trade       the current trade
     * @param excps       exception messages
     * @return the trade keyword value
     */
    private String getTradeKeywordValue(String keywordCode, Trade trade, Vector excps) {
        if (keywordCode.contains("[") && keywordCode.contains("]")) {
            String[] split = keywordCode.replace("]", "").split("\\[");
            if (!Util.isEmpty(split) && split.length == 2) {
                String keywordName = split[0];
                String[] positions = split[1].split(",");
                String kwValue = trade.getKeywordValue(keywordName);
                if (Util.isEmpty(kwValue)) {
                    excps.add(buildError(trade.getLongId(), String.format("The KW value %s is empty. " +
                            "The %s will not paste on the transfer.", keywordName,
                            ReconCCPConstants.XFER_ATTR_SETTLEMENT_REF_INST)));
                    return null;
                }
                if (!Util.isEmpty(positions)) {
                    try {
                        if (positions.length == 1) {
                            if(positions[0].contains("L")){
                                return kwValue.substring(kwValue.length() -
                                        Integer.parseInt(positions[0].replace("L", "")));
                            }else {
                                return kwValue.substring(Integer.parseInt(positions[0]));
                            }
                        } else if (positions.length == 2) {
                            return kwValue.substring(Integer.parseInt(positions[0]),
                                    Integer.parseInt(positions[1]));
                        } else {
                            excps.add(buildError(trade.getLongId(), String.format("The format is not correct for the comment %s of the domain" +
                                            " value %s. Example MxElectPlatid[1,6]",
                                    keywordCode, ReconCCPConstants.UPDATE_XFER_SRI_DOMAIN_NAME)));
                        }
                    } catch (NumberFormatException e) {
                        excps.add(buildError(trade.getLongId(), String.format("The format is not correct for the comment %s of the domain" +
                                        " value %s. Example MxElectPlatid[1,6]",
                                keywordCode, ReconCCPConstants.UPDATE_XFER_SRI_DOMAIN_NAME)));
                        Log.error(this, e);
                        return null;
                    } catch (StringIndexOutOfBoundsException e) {
                        excps.add(buildError(trade.getLongId(), String.format("The size of the KW %s, value %s, is outside " +
                                        "the configured limits %s. Check the settings on the DV %s or the KW value.",
                                keywordName, kwValue, keywordCode, ReconCCPConstants.UPDATE_XFER_SRI_DOMAIN_NAME)));
                        Log.error(this, e);
                        return null;
                    }
                }
            } else {
                excps.add(buildError(trade.getLongId(), String.format("The format is not correct for the comment %s of the domain" +
                                " value %s. Example MxElectPlatid[1,6]",
                        keywordCode, ReconCCPConstants.UPDATE_XFER_SRI_DOMAIN_NAME)));
                return null;
            }
        } else {
            String kwValue = trade.getKeywordValue(keywordCode);
            if(Util.isEmpty(kwValue)){
                excps.add(buildError(trade.getLongId(), String.format("The KW value %s is empty. " +
                                "The %s will not paste on the transfer.", keywordCode,
                        ReconCCPConstants.XFER_ATTR_SETTLEMENT_REF_INST)));
                return null;
            }
            return kwValue;
        }
        return null;
    }

    /**
     * Get the SDF platform name. Reads from UpdateSettlementReferenceInstructed DV get the SDF by name
     * if accept return the SDF name
     *
     * @param dsCon    the Data Server connection
     * @param transfer the current transfer
     * @param trade    the trade
     * @return the SDF name
     */
    private String selectSDFPlatform(DSConnection dsCon, BOTransfer transfer, Trade trade) {
        Vector<String> filterNames = LocalCache.getDomainValues(dsCon, ReconCCPConstants.UPDATE_XFER_SRI_DOMAIN_NAME);
        if (!Util.isEmpty(filterNames)) {
            for (String sdfName : filterNames) {
                StaticDataFilter sdf = BOCache.getStaticDataFilter(dsCon, sdfName);
                if (sdf != null && sdf.accept(trade, transfer)) {
                    return sdfName;
                }
            }
        }
        return null;
    }

    /**
     * Build BOException
     *
     * @param tradeId the current trade id
     * @param msg     the error message
     * @return the exception
     */
    private BOException buildError(long tradeId, String msg) {
        return new BOException(tradeId, "UpdateSRITransferRule", msg, BOException.INFORMATION);
    }

}
