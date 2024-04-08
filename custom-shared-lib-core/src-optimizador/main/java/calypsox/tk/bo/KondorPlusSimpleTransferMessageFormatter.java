package calypsox.tk.bo;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.SimpleTransfer;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.service.DSConnection;

import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Vector;

public class KondorPlusSimpleTransferMessageFormatter extends KondorPlusMarginCallMessageFormatter {
    private static final String MARGIN_CALL_CONTRACT = "MARGIN_CALL_CONTRACT";
    private static final String TRUE = "true";
    private static final String FALSE = "false";

    private static final String format = "dd/MM/yyyy";
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);

    // MOVEMENT_TYPE
    private static final String IM = "IM";
    private static final String VM = "VM";
    private static final String CONTRACT_TYPE_CSD = "CSD";

    private static final String PO_SOVEREIGN = "SBWO";
    private static final String IM_CSD_TYPE = "IM_CSD_TYPE";

    public KondorPlusSimpleTransferMessageFormatter() {
        super();
    }

    // DEAL_TYPE for Security & TYPE for Cash.
    @Override
    @SuppressWarnings("rawtypes")
    public String parseDEAL_TYPE(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                 BOTransfer transfer, DSConnection dsConn) {
        // ANY VALUE
        return "3";
    }

    // INTERNAL_ID.
    @Override
    @SuppressWarnings("rawtypes")
    public String parseMARGIN_CALL_ID(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                      BOTransfer transfer, DSConnection dsConn) {
        return getMarginCallId(trade);
    }

    // MATURITY_DATE.
    @Override
    @SuppressWarnings("rawtypes")
    public String parsePRODUCT_MATURITYDATE(BOMessage message, Trade trade, LEContact po, LEContact cp,
                                            Vector paramVector, BOTransfer transfer, DSConnection dsConn) {
        SimpleTransfer st = (SimpleTransfer) trade.getProduct();
        if (st != null) {
            JDate jdate = st.getMaturityDate();
            jdate = jdate.addBusinessDays(-1, Util.string2Vector("SYSTEM"));
            if (null != jdate) {
                return this.simpleDateFormat.format(jdate.getDate(TimeZone.getDefault()));
            }
        }
        return null;
    }

    // BOND.
    @Override
    @SuppressWarnings("rawtypes")
    public String parseISINCODE(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                BOTransfer transfer, DSConnection dsConn) {
        return null;
    }

    // AMOUNT.
    @Override
    @SuppressWarnings("rawtypes")
    public String parseAMOUNT(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                              BOTransfer transfer, DSConnection dsConn) {
        SimpleTransfer st = (SimpleTransfer) trade.getProduct();
        if (null != st) {
            try {
                return formatNumber(Math.abs(trade.getQuantity()) * st.getPrincipal());
            } catch (ParseException e) {
                Log.error(this, e); //sonar
            }
        }

        return null;
    }

    // REHYPOTHECABLE.
    @Override
    @SuppressWarnings("rawtypes")
    public String parseCONTRACT_IS_REHYPOTHECABLE(BOMessage message, Trade trade, LEContact po, LEContact cp,
                                                  Vector paramVector, BOTransfer transfer, DSConnection dsConn) {
        return null;
    }

    // PRICE.
    @Override
    @SuppressWarnings("rawtypes")
    public String parsePRODUCT_PRINCIPAL(BOMessage message, Trade trade, LEContact po, LEContact cp,
                                         Vector paramVector, BOTransfer transfer, DSConnection dsConn) {
        SimpleTransfer st = (SimpleTransfer) trade.getProduct();
        try {
            return formatNumber(st.getPrincipal());
        } catch (ParseException e) {
            Log.error(KondorPlusSimpleTransferMessageFormatter.class.getName(), e);
        }
        return null;
    }

    // DEAL_TYPE for the Cash Transfers.
    @Override
    @SuppressWarnings("rawtypes")
    public String parseDEAL_TYPE_CASH(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                      BOTransfer transfer, DSConnection dsConn) {
        // ANY VALUE
        return "I";
    }

    // TRIPARTY.
    @Override
    @SuppressWarnings("rawtypes")
    public String parseTRIPARTY(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                BOTransfer transfer, DSConnection dsConn) {
        try {
            String mcId = getMarginCallId(trade);
            if (!Util.isEmpty(mcId)) {
                MarginCallConfig marginCallConfig = dsConn.getRemoteReferenceData().getMarginCallConfig(
                        Integer.valueOf(mcId));
                if (marginCallConfig != null) {
                    String mc_triparty = marginCallConfig.getAdditionalField(MC_TRIPARTY);
                    if (!"".equals(mc_triparty)
                            && (null != mc_triparty)
                            && (mc_triparty.toLowerCase().startsWith("y") || mc_triparty.toLowerCase().startsWith("t") || mc_triparty
                            .toLowerCase().startsWith("s"))) {
                        return "Y";
                    } else {
                        return "N";
                    }
                }
            }
        } catch (RemoteException e) {
            Log.error(KondorPlusSimpleTransferMessageFormatter.class.getName(), e);
        }
        return null;
    }

    /**
     * Method to check if the trade is kind of cash or transfer.
     *
     * @param message
     * @param trade
     * @param po
     * @param cp
     * @param paramVector
     * @param transfer
     * @param dsConn
     * @return The string "true" or "false" depending on the result of the check.
     */
    @Override
    @SuppressWarnings("rawtypes")
    public String parseIS_MARGINCALL_SECURITY(BOMessage message, Trade trade, LEContact po, LEContact cp,
                                              Vector paramVector, BOTransfer transfer, DSConnection dsConn) {
        return FALSE;
    }

    /**
     * Method to check if the contract has filled the additional field CLEARING MODE
     *
     * @param message
     * @param trade
     * @param po
     * @param cp
     * @param paramVector
     * @param transfer
     * @param dsConn
     * @return The string "true" or "false" depending on the result of the check.
     */
    @Override
    @SuppressWarnings("rawtypes")
    public String parseCLEARING_OK(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                   BOTransfer transfer, DSConnection dsConn) {

        String clearing = FALSE;
        try {
            String mcId = getMarginCallId(trade);
            if (!Util.isEmpty(mcId)) {
                MarginCallConfig marginCallConfig = dsConn.getRemoteReferenceData().getMarginCallConfig(
                        Integer.valueOf(mcId));
                if (marginCallConfig != null) {
                    if (!Util.isEmpty(marginCallConfig.getAdditionalField(CLEARING_MODE))) {
                        clearing = TRUE;
                    }
                }
            }
        } catch (RemoteException e) {
            Log.error(this, e);
        } catch (Exception e) {
            Log.error(this, e);
        }
        return clearing;
    }

    /**
     * Method to check if the contract has filled the additional field CLEARING MODE
     *
     * @param message
     * @param trade
     * @param po
     * @param cp
     * @param paramVector
     * @param transfer
     * @param dsConn
     * @return The string "true" or "false" depending on the result of the check.
     */
    @Override
    @SuppressWarnings("rawtypes")
    public String parseCUSTODIAN_CM(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                    BOTransfer transfer, DSConnection dsConn) {

        try {
            String mcId = getMarginCallId(trade);
            if (!Util.isEmpty(mcId)) {
                MarginCallConfig marginCallConfig = dsConn.getRemoteReferenceData().getMarginCallConfig(
                        Integer.valueOf(mcId));
                if (null != marginCallConfig) {
                    if (!Util.isEmpty(marginCallConfig.getAdditionalField(CLEARING_MODE))) {
                        return marginCallConfig.getAdditionalField(CLEARING_MODE);
                    }
                }
            }
        } catch (RemoteException e) {
            Log.error(this, e);
        }
        return "";
    }

    /**
     * Get MarginCallConfig id from Account MARGIN_CALL_CONTRACT property value
     *
     * @param trade
     * @return
     */
    private String getMarginCallId(Trade trade) {
        String marginCallId = "";
        if (acceptProductType(trade)) {
            SimpleTransfer st = (SimpleTransfer) trade.getProduct();
            int accountId = Math.toIntExact(st.getLinkedLongId());
            if (accountId != 0) {
                Account account = BOCache.getAccount(DSConnection.getDefault(), accountId);
                if (account != null) {
                    return account.getAccountProperty(MARGIN_CALL_CONTRACT);
                }
            }
        }
        return marginCallId;
    }

    // TOMADA_PRESTADA.
    @Override
    @SuppressWarnings("rawtypes")
    public String parseTOMADA_PRESTADA(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                       BOTransfer transfer, DSConnection dsConn) {
        // ANY VALUE
        return "T";
    }

    // IS_INTEREST.
    @SuppressWarnings("rawtypes")
    public String parseIS_INTEREST(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                   BOTransfer transfer, DSConnection dsConn) {
        return TRUE;
    }

    // MOVEMENT_TYPE
    @SuppressWarnings("rawtypes")
    public String parseMOVEMENT_TYPE(BOMessage message, Trade trade,
                                     LEContact po, LEContact cp, Vector paramVector,
                                     BOTransfer transfer, DSConnection dsConn) {
        String movementType = VM;
        if (acceptProductType(trade)) {
            SimpleTransfer simpleXfer = (SimpleTransfer) trade.getProduct();
            if (null != simpleXfer) {
                String mcId = getMarginCallId(trade);
                if (!Util.isEmpty(mcId)) {
                    try {
                        MarginCallConfig marginCallConfig = dsConn
                                .getRemoteReferenceData().getMarginCallConfig(
                                        Integer.valueOf(mcId));

                        if (null != marginCallConfig) {
                            if (CONTRACT_TYPE_CSD.equals(marginCallConfig
                                    .getContractType())) {
                                if (PO_SOVEREIGN.equals(marginCallConfig
                                        .getProcessingOrg().getCode())) {
                                    return IM
                                            + "-"
                                            + marginCallConfig
                                            .getAdditionalField(IM_CSD_TYPE);
                                } else {
                                    return IM;
                                }
                            } else {
                                return VM;
                            }
                        }
                    } catch (RemoteException e) {
                        Log.error(
                                KondorPlusSimpleTransferMessageFormatter.class
                                        .getName(), e);
                    }
                }
            }
        }

        return movementType;
    }

    /**
     * @param trade
     * @return
     */
    private boolean acceptProductType(Trade trade) {
        return trade != null && (SimpleTransfer.SIMPLETRANSFER.equals(trade.getProductType()) || SimpleTransfer.CUSTOMERTRANSFER.equals(trade.getProductType()));
    }

}
