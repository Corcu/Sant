package calypsox.util;

import calypsox.ctm.util.CTMUploaderConstants;
import calypsox.ctm.util.PlatformAllocationTradeFilterAdapter;
import com.calypso.taskenrichment.data.enrichment.DefaultTaskEnrichmentCustom;
import com.calypso.taskenrichment.data.enrichment.TaskEnrichmentFieldConfig;
import com.calypso.tk.bo.*;
import com.calypso.tk.bo.util.ProcessTaskUtil;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.core.*;
import com.calypso.tk.product.CustomerTransfer;
import com.calypso.tk.product.InterestBearing;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.jaxb.CalypsoObject;
import com.calypso.tk.upload.jaxb.MarginCallContract;
import com.calypso.tk.upload.services.GatewayUtil;
import com.calypso.tk.util.TransferArray;

import java.util.Collection;
import java.util.Optional;
import java.util.Vector;

/**
 * @author various fbs
 */
public class TaskEnrichment extends DefaultTaskEnrichmentCustom {

    public static final String ECONOMIC_SECTOR = "ECONOMIC_SECTOR";
    public static final String MC_VALIDATION = "MC_VALIDATION";
    public static final String BIC_CODE = "BIC_CODE";
    public static final String MARGIN_CALL_CONTRACT = "MARGIN_CALL_CONTRACT";


    public String getCounterPartyBicCode(Trade trade) {
        LegalEntity le = null;
        try {
            le = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(trade.getCounterParty().getCode());
            return getBic_code(le);
        } catch (CalypsoServiceException e) {
            Log.error("Can't find any LegalEntity with that name", e);
        }
        return "";
    }

    public String getProcessingOrgBicCode(Trade trade) {
        LegalEntity le = null;
        try {
            le = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(trade.getBook().getProcessingOrgBasedId());
            return getBic_code(le);
        } catch (CalypsoServiceException e) {
            Log.error("Can't find any ProcessingOrg with that name", e);
        }
        return "";
    }

    public String getBic_code(LegalEntity legalEntity) {
        if (legalEntity != null) {
            Collection<LegalEntityAttribute> c = legalEntity.getLegalEntityAttributes();
            String bic_code = c.stream().filter(s -> !Util.isEmpty(s.getAttributeType()) &&
                    s.getAttributeType().equalsIgnoreCase(BIC_CODE)).findFirst().get().getAttributeValue();
            if (!Util.isEmpty(bic_code)) {
                return bic_code;
            }
        }
        return "";
    }

    public String getCollateralConfigId(Trade trade) throws CalypsoServiceException {

        if ("MarginCall".equalsIgnoreCase(trade.getProductType())) {
            MarginCall mc = (MarginCall) trade.getProduct();
            return String.valueOf(mc.getMarginCallConfig().getId());
        }
        if ("CustomerTransfer".equalsIgnoreCase(trade.getProductType())) {
            CustomerTransfer ct = (CustomerTransfer) trade.getProduct();
            Account account = DSConnection.getDefault().getRemoteAccounting().getAccount((int) ct.getLinkedLongId());
            return Optional.ofNullable(account).map(acc -> acc.getAccountProperty(MARGIN_CALL_CONTRACT)).orElse("");
        }
        if ("InterestBearing".equalsIgnoreCase(trade.getProductType())) {
            InterestBearing interestBearing = (InterestBearing) trade.getProduct();
            Account account = DSConnection.getDefault().getRemoteAccounting().getAccount((int) interestBearing.getLinkedLongId());
            return Optional.ofNullable(account).map(acc -> acc.getAccountProperty(MARGIN_CALL_CONTRACT)).orElse("");
        }

        return "";
    }

    public String getTransferLongId(BOTransfer transfer) {
        return String.valueOf(Optional.ofNullable(transfer).map(BOTransfer::getLongId).orElse(0L));
    }

    public String getTransferNettingType(BOTransfer transfer) {
        return Optional.ofNullable(transfer).map(BOTransfer::getNettingType).orElse("");
    }

    public String getTransferRejectReason(BOTransfer transfer) {
        return Optional.ofNullable(transfer).map(tran -> tran.getAttribute("RejectReason")).orElse("");
    }

    public String getTransferMatchingStatus(BOTransfer transfer) {
        return Optional.ofNullable(transfer).map(tran -> tran.getAttribute("Matching_Status")).orElse("");
    }

    public String getTransferMatchingReason(BOTransfer transfer) {
        return Optional.ofNullable(transfer).map(tran -> tran.getAttribute("Matching_Reason")).orElse("");
    }

    public String getTransferSettlementStatus(BOTransfer transfer) {
        return Optional.ofNullable(transfer).map(tran -> tran.getAttribute("Settlement_Status")).orElse("");
    }

    public String getTransferSettlementReason(BOTransfer transfer) {
        return Optional.ofNullable(transfer).map(tran -> tran.getAttribute("Settlement_Reason")).orElse("");
    }

    public String getTransferBookName(BOTransfer transfer) {
        String bookName = "";
        int bookId = Optional.ofNullable(transfer).map(BOTransfer::getBookId).orElse(0);
        if (bookId > 0) {
            bookName = Optional.ofNullable(BOCache.getBook(DSConnection.getDefault(), bookId))
                    .map(Book::getName).orElse("");
        }
        return bookName;
    }

    public String getTransferFromTripartyAllocationKwd(BOTransfer transfer) {
        String fromTripartyAlloc = "false";
        long tradeId = Optional.ofNullable(transfer).map(BOTransfer::getTradeLongId).orElse(0L);
        if (tradeId > 0) {
            try {
                Trade trade = DSConnection.getDefault().getRemoteTrade().getTrade(tradeId);
                fromTripartyAlloc = Optional.ofNullable(trade).map(tra -> tra.getKeywordValue("FromTripartyAllocation"))
                        .orElse("false");
            } catch (CalypsoServiceException exc) {
                Log.error(this.getClass().getSimpleName(), exc.getMessage());
            }
        }
        return fromTripartyAlloc;
    }

    public String getCollateralConfigName(Trade trade) throws CalypsoServiceException {
        final String collateralConfigId = getCollateralConfigId(trade);
        if (!Util.isEmpty(collateralConfigId)) {
            MarginCallConfig mcc = DSConnection.getDefault().getRemoteReferenceData().
                    getMarginCallConfig(Integer.parseInt(getCollateralConfigId(trade)));
            if (mcc != null) {
                return mcc.getName();
            }
        }
        return "";
    }

    public String getCollateralConfigMC_VALIDATION(MarginCallEntry entry) {
        CollateralConfig collateralConfig = entry.getCollateralConfig();
        if (!Util.isEmpty(collateralConfig.getAdditionalField(MC_VALIDATION))) {
            return collateralConfig.getAdditionalField(MC_VALIDATION);
        }
        return "";
    }

    public String getCollateralConfigECONOMIC_SECTOR(MarginCallEntry entry) {
        CollateralConfig collateralConfig = entry.getCollateralConfig();
        if (!Util.isEmpty(collateralConfig.getAdditionalField(ECONOMIC_SECTOR))) {
            return collateralConfig.getAdditionalField(ECONOMIC_SECTOR);
        }
        return "";
    }

    public String getMCLyncsID(BOMessage boMessage) {
        String lyncsId = "";
        if (boMessage != null && "MCLyncs".equals(boMessage.getGateway())) {
            try {
                CalypsoObject calypsoObject = GatewayUtil.getCalypsoObject(boMessage, new Vector(), null);
                if (calypsoObject instanceof MarginCallContract) {
                    MarginCallContract mcContract = (MarginCallContract) calypsoObject;
                    lyncsId = Optional.ofNullable(mcContract.getMarginCallDescription()).orElse("");
                }
            } catch (CalypsoException exc) {
                Log.warn(this.getClass().getSimpleName(), exc.getMessage(), exc.getCause());
            }
        }
        return lyncsId;
    }


    public String getEquityType(BOTransfer transfer) {
        return getProductSecurityCode(transfer, "EQUITY_TYPE");
    }


    public String getCrossWfEquityType(Task task, TaskEnrichmentFieldConfig config) {
        return this.getProductSecCode(task, config, "EQUITY_TYPE");
    }


    public String getTradeEquityType(Trade trade) {
        try {
            return getTrade(trade.getLongId()).getProduct().getSecCode("EQUITY_TYPE");
        } catch (Exception e) {
            Log.error(this, e + ": " + e.getMessage(), e);
        }
        return "";
        }


    public String getECMSPledge(Trade trade) {
        try {
            return getTrade(trade.getLongId()).getKeywordValue("ECMS_Auto_Unpledge");
        } catch (Exception e) {
            Log.error(this, e + ": " + e.getMessage(), e);
        }
        return "";
    }

    public String getISINDescription(Trade trade) {
        try {
            String isinDescription = DSConnection.getDefault().getRemoteTrade().getTrade(trade.getLongId()).getProduct().getDescription();
            if (isinDescription != null && isinDescription.indexOf("(") > 0) {
                return isinDescription.substring(isinDescription.indexOf("(") + 1, isinDescription.length() - 1);
            } else {
                return isinDescription;
            }
        } catch (CalypsoServiceException e) {
            e.printStackTrace();
            Log.error(this, e + ": " + e.getMessage(), e);
        }
        return "";
    }

    public String getSDIReceiver(Trade trade) {
        String query = "";
        String result = "";
        if (trade.getProduct() instanceof MarginCall) {
            MarginCall m = (MarginCall) trade.getProduct();
            m.getBuySell(trade);
            result = getSDI(trade, 1, m.getBuySell(trade)).getDescription();
        }
        return result;
    }

    public String getSDIPayer(Trade trade) {
        String query = "";
        String result = "";
        if (trade.getProduct() instanceof MarginCall) {
            MarginCall m = (MarginCall) trade.getProduct();
            result = getSDI(trade, -1, m.getBuySell(trade)).getDescription();
        }
        return result;
    }


    public SettleDeliveryInstruction getSDI(Trade trade, int payRec, int buySell) {
        String query = "";
        SettleDeliveryInstruction sdi = null;
        try {
            query = getSDIQuery(trade.getTradeCurrency(), trade.getCounterParty().getId(),
                    trade.getBook().getLegalEntity().getId(), payRec);
            sdi = (SettleDeliveryInstruction) DSConnection.getDefault().getRemoteReferenceData().
                    getSettleDeliveryInstructions(null, query, null).get(0);
        } catch (CalypsoServiceException e) {
            throw new RuntimeException(e);
        }
        return sdi;
    }

    private String getSDIQuery(String ccy, int ctp, int po, int payRec) {
        StringBuilder query = new StringBuilder();
        query.append("CURRENCY_LIST=null OR CURRENCY_LIST LIKE '%" + ccy + "%' AND");
        query.append("INT_LE='" + ctp + "' AND");
        query.append("PROCESS_ORG_ID='" + po + "' AND");
        return query.toString();
    }

    public JDatetime getTradeDate(Trade trade) {
        try {
            return getTrade(trade.getLongId()).getTradeDate();
        } catch (Exception e) {
            Log.error(this, e + ": " + e.getMessage(), e);
        }
        return JDatetime.valueOf("");
    }

    public JDatetime getTradeUpdatedDate(Trade trade) {
        try {
            return getTrade(trade.getLongId()).getUpdatedTime();
        } catch (Exception e) {
            Log.error(this, e + ": " + e.getMessage(), e);
        }
        return JDatetime.valueOf("");

    }

    public String getSettleDate(Trade trade) {
        return (trade.getSettleDate() != null) ? trade.getSettleDate().toString() : "";
    }

    public String getMurexIdTkw(Trade trade) {
        try {
            return getTrade(trade.getLongId()).getKeywordValue("Mx Global ID");
        } catch (Exception e) {
            Log.error(this, e + ": " + e.getMessage(), e);
        }
        return "";
    }

    public String getDualCcyTkw(Trade trade) {
        try {
            return getTrade(trade.getLongId()).getKeywordValue("Dual_CCY");
        } catch (Exception e) {
            Log.error(this, e + ": " + e.getMessage(), e);
        }
        return "";
    }

    public String getDeliveryType (BOTransfer transfer) {
        return transfer.getDeliveryType();
    }

    public String getGenericComment(BOTransfer transfer, String commentType){
        try {
            StringBuilder query = new StringBuilder();
            query.append("OBJECT_CLASS = 'Transfer' ");
            query.append("AND OBJECT_ID='" +transfer.getLongId() + "'");
            query.append("AND COMMENT_TYPE='" +commentType + "'");
            Vector<GenericComment> comments =
                    DSConnection.getDefault().getRemoteBackOffice().getGenericComments(null, query.toString(), null, null);
            if(!comments.isEmpty()){
                return comments.get(0).getComment();
            }
        } catch (CalypsoServiceException e) {
            Log.error(this,e);
        }
        return null;
    }

    public String getSDIReceiver(Task task, TaskEnrichmentFieldConfig config) {
        BOTransfer transfer = getTransferFromTask(task);
        if(transfer != null){
            return getSDIDescription(transfer.getReceiverSDId());
        }
        return null;
    }

    public String getSDIPayer(Task task, TaskEnrichmentFieldConfig config) {
        BOTransfer transfer = getTransferFromTask(task);
        if(transfer != null){
            return getSDIDescription(transfer.getPayerSDId());
        }
        return null;
    }

    private String getSDIDescription(int sdiId) {
        try {
            SettleDeliveryInstruction sdi =
                    DSConnection.getDefault().getRemoteReferenceData().getSettleDeliveryInstruction(sdiId);
            if(sdi!=null) {
                return sdi.getDescription();
            }
        } catch (CalypsoServiceException e) {
            throw new RuntimeException(e);
        }
        return "";
    }

    private BOTransfer getTransferFromTask(Task task) {
        BOTransfer transfer = null;
        try {
            LegalEntity cp = null;
            ProcessTaskUtil.ObjectDesc objectDesc = getObjectDesc(task);
            if ("Trade".equals(objectDesc.type)) {
                Trade trade = objectDesc.getTrade();
                if (trade == null) {
                    long tradeId = task.getTradeLongId() == 0L ? objectDesc.id : task.getTradeLongId();
                    trade = this.getTrade(tradeId);
                }
                TransferArray transferArray = DSConnection.getDefault().getRemoteBackOffice().
                        getBOTransfers(trade.getLongId());
                transfer = transferArray !=null && !transferArray.isEmpty() ? DSConnection.getDefault().getRemoteBackOffice().
                        getBOTransfers(trade.getLongId()).get(0) : null;
            } else if ("Transfer".equals(objectDesc.type)) {
                transfer = objectDesc.getTransfer();
            }
            return transfer;
        } catch (Exception var9) {
            Log.error(this, var9);
        }
        return null;
    }

    private ProcessTaskUtil.ObjectDesc getObjectDesc(Task task) {
        ProcessTaskUtil.ObjectDesc desc = task.getObjectDesc();
        if (desc == null) {
            desc = new ProcessTaskUtil.ObjectDesc(task);
            task.setObjectDesc(desc);
        }

        return desc;
    }

    public String getLEAttributeBool(Trade trade, String attribute){
        LegalEntity cp = trade.getCounterParty();
        Collection atts = cp.getLegalEntityAttributes();
        boolean gestora = false;
        boolean attributePresent = false;
        String attrValue = "";

        if (!isPlatformOrCTMBlockTrade(trade)) return "";

        for(Object o : atts){
            LegalEntityAttribute leAtt = (LegalEntityAttribute) o;
            if ("GESTORA".equals(leAtt.getAttributeType()) && Boolean.parseBoolean(leAtt.getAttributeValue())) {
                gestora = true;
            }
            if(attribute.equals(leAtt.getAttributeType())){
                attributePresent = true;
                attrValue = leAtt.getAttributeValue();
            }
        }

        if (!gestora) return "";
        if(attributePresent) return Util.isEmpty(attrValue) ? "false" : "true";

        return "false";
    }

    private boolean isPlatformOrCTMBlockTrade(Trade trade) {
        return Optional.ofNullable(trade)
                .map(t -> t.getKeywordValue(CTMUploaderConstants.TRADE_KEYWORD_BLOCK_TRADE_DETAIL))
                .map(kwdValue -> CTMUploaderConstants.CTM_STR.equals(kwdValue) || CTMUploaderConstants.PLATFORM_STR.equals(kwdValue))
                .orElse(false);
    }
}
