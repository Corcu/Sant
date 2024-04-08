package calypsox.tk.bo;


import calypsox.tk.bo.util.PHConstants;
import calypsox.tk.bo.util.PaymentsHubUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.TemplateSelector;
import com.calypso.tk.bo.swift.SwiftUtil;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.ManualSDI;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteAccess;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


public class PaymentHubTemplateSelector implements TemplateSelector {


    private static final String PH_EXCEP_SDF_SQL = "SELECT sd_filter_name FROM sd_filter WHERE sd_filter_name LIKE ?";


    /**
     * Instantiates a new payment hub template selector.
     */
    public PaymentHubTemplateSelector() {
        // do nothing
    }


    /**
     * Financial Institution Customer Credit Transfer Template
     *
     * @return
     */
    protected String getTypeFICCT() {
        return PHConstants.MESSAGE_PH_FICCT;
    }


    /**
     * Financial Institution Credit Transfer Template
     *
     * @return
     */
    protected String getTypeFICT() {
        return PHConstants.MESSAGE_PH_FICT;
    }


    /**
     * Notice To Receive Template
     *
     * @return
     */
    protected String getTypeNTR() {
        return PHConstants.MESSAGE_PH_NTR;
    }


    /**
     * No Out Message Template
     *
     * @return
     */
    protected String getTypeNoOutMessage() {
        return PHConstants.MESSAGE_PH_NO_OUT_MESSAGE;
    }


    @Override
    public String getTemplate(final Trade trade, final BOMessage boMessage, final String name) {
        if ((boMessage == null) || Util.isEmpty(boMessage.getMessageType())) {
            Log.info(this, "Null Message");
            return null;
        }
        final DSConnection dsCon = DSConnection.getDefault();
        BOTransfer xfer = null;
        try {
            xfer = PaymentsHubUtil.getBOTransfer(boMessage.getTransferLongId());
        } catch (final Exception e) {
            Log.error(this.getClass().getName(), "Could not get transfer", e);
        }
        // Get Template by StaticDataFilter Exceptions.
        String template = getSDFExceptionTemplate(trade, xfer, boMessage, PHConstants.SDF_EXCEPTION_NAMES, dsCon);
        if (Util.isEmpty(template)) {
            if (xfer == null) {
                template = getTypeFICT();
            } else {
                // If is Direct
                if (PHConstants.SETTLEMENT_METHOD_DIRECT.equalsIgnoreCase(xfer.getSettlementMethod())) {
                    template = getTypeNoOutMessage();
                } else {
                    if (xfer.getPayReceiveType().equals(PHConstants.XFER_TYPE_RECEIVE)) {
                        template = getTypeNTR();
                    } else {
                        final boolean flag = isFinantial(dsCon, boMessage, xfer);
                        if (!flag) {
                            template = getTypeFICCT();
                        } else {
                            template = getTypeFICT();
                        }
                    }
                }
            }
        }
        return template;
    }


    /**
     * Get Payments Hub templates using the 'Comments' attribute of the accepted SDFs.
     *
     * @param trade
     * @param xfer
     * @param message
     * @param sdfExceptionNames
     * @param dsCon
     * @return Payments Hub Template
     */
    protected String getSDFExceptionTemplate(final Trade trade, final BOTransfer xfer, final BOMessage message, final String sdfExceptionNames, final DSConnection dsCon) {
        String template = "";
        final List<String> sdFiltersNames = getPaymentHubExceptions(sdfExceptionNames, dsCon);
        final List<StaticDataFilter> sdFiltersAccepted = new ArrayList<StaticDataFilter>();
        // Check Static Data Filters
        sdFiltersNames.forEach(sdFilterName -> {
            final StaticDataFilter sdf = getSDFAccepted(trade, xfer, message, sdFilterName, dsCon);
            if (null != sdf) {
                sdFiltersAccepted.add(sdf);
            }
        });
        if (!Util.isEmpty(sdFiltersAccepted)) {
            // Must only one SDF accepted
            template = sdFiltersAccepted.get(0).getComment();
            if (sdFiltersAccepted.size() > 1) {
                Log.debug(this, "There are two or more SDFs. Select the first one.");
            }
        }
        return template;
    }


    /**
     * Get SDFilters PaymentHub exceptions.
     *
     * @param sdfExceptionNames
     * @return
     */
    protected static List<String> getPaymentHubExceptions(final String sdfExceptionNames, final DSConnection dsCon) {
        final List<String> sdfNames = new ArrayList<String>();
        final RemoteAccess remoteAccess = dsCon.getRemoteAccess();
        final List<CalypsoBindVariable> bindVariables = new ArrayList<CalypsoBindVariable>();
        bindVariables.add(new CalypsoBindVariable(12, sdfExceptionNames));
        try {
            final Vector<?> rawSdfNames = remoteAccess.executeSelectSQL(PH_EXCEP_SDF_SQL, bindVariables);
            if (!Util.isEmpty(rawSdfNames)) {
                rawSdfNames.forEach(sdfName -> {
                    if (sdfName instanceof Vector<?>) {
                        final Vector<?> v = (Vector<?>) sdfName;
                        if (v.get(0) instanceof String) {
                            sdfNames.add((String) v.get(0));
                        }
                    }
                });
            }
        } catch (final Exception e) {
            Log.error(PaymentHubTemplateSelector.class, "Error getting PaymentHub StaticDataFilters.", e);
        }
        return sdfNames;
    }


    /**
     * Checks if is finantial.
     *
     * @param ds the ds
     * @param msg the msg
     * @param transfer the transfer
     * @return true, if is finantial
     */
    protected boolean isFinantial(final DSConnection ds, final BOMessage msg, final BOTransfer transfer) {
        ManualSDI manualsdi = null;
        if (transfer.isManualSDI()) {
            manualsdi = BOCache.getManualSDI(ds, transfer.getManualSDId());
        }
        final LegalEntity externalLE = BOCache.getLegalEntity(ds, transfer.getExternalLegalEntityId());
        boolean flag = SwiftUtil.getIsFinancial(msg, externalLE);
        if (manualsdi != null) {
            flag = manualsdi.getClassification();
        }
        final int origCpty = transfer.getOriginalCptyId();
        if (flag && (origCpty != 0) && (transfer.getExternalLegalEntityId() != origCpty)) {
            final LegalEntity originalLE = BOCache.getLegalEntity(ds, origCpty);
            flag = SwiftUtil.getIsFinancial(msg, originalLE);
        }
        return flag;
    }


    /**
     * Get the SDF accepted.
     *
     * @param trade
     * @param xfer
     * @param message
     * @param sdfFilterName
     * @param dsCon
     * @return
     */
    protected static StaticDataFilter getSDFAccepted(final Trade trade, final BOTransfer xfer, final BOMessage message, final String sdfFilterName, final DSConnection dsCon) {
        final StaticDataFilter sdf = getSDFilter(sdfFilterName, dsCon);
        return (sdf != null && sdf.accept(trade, xfer, message)) ? sdf : null;
    }


    /**
     * Get Static Data Filter.
     *
     * @param sdfName
     * @param dsCon
     * @return
     */
    protected static StaticDataFilter getSDFilter(final String sdfName, final DSConnection dsCon) {
        StaticDataFilter sdf = null;
        try {
            sdf = dsCon.getRemoteReferenceData().getStaticDataFilter(sdfName);
        } catch (final CalypsoServiceException e) {
            final String msg = String.format("Could not get the Static Data Filter [%s]", sdfName);
            Log.error(PaymentHubTemplateSelector.class, msg, e);
        }
        return sdf;
    }


}
