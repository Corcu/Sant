package calypsox.tk.bo.cremapping.event;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

import java.util.Arrays;
import java.util.Optional;
import java.util.TimeZone;

import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.CA;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.tk.bo.cremapping.util.CABOCreSettleMethodHandler;

public class BOCreCACuponAmortCST extends BOCreMarginCallSEC_POS {
	
	/**
     * Transfer attribute Failed name
     */
    private static final String FAILED = "Failed";

    /**
     * BOCre SETTLED event type
     */
    private static final String SETTLED = "SETTLED";


    public BOCreCACuponAmortCST(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    public void fillValues() {
        super.fillValues();
        this.settlementMethod = new CABOCreSettleMethodHandler().getCreSettlementMethod(this.creBoTransfer);
        this.transferAccount = getInstance().getTransferAccount(this.settlementMethod, this.creBoTransfer);
        this.partenonId = BOCreUtils.getInstance().loadPartenonId(this.trade);
        this.creDescription = getDesc(this.boCre.getDescription());

        ownIssuance = getOwnIssuance();
        bondType = getbondType();
        bedulaAttribute = getBedulaAttribute();
        claimProductType = BOCreUtils.getInstance().loadClaimProductType(this.trade);
        this.sentDateTime = JDatetime.currentTimeValueOf(JDate.getNow(), TimeZone.getDefault());

        // CA RF
        this.caType = BOCreUtils.getInstance().getCaType(this.trade);
        this.caReference = BOCreUtils.getInstance().getCAReference(this.trade);
        this.mcContractID = BOCreUtils.getInstance().loadContractID(this.trade);
        if (getContract() != null){
            this.mcContractType = getContract().getContractType();
        }

    }
    
    @Override
    protected String loadOriginalEventType() {
        String originalEventType = super.loadOriginalEventType();
        boolean failed = Boolean.parseBoolean(Optional.ofNullable(this.creBoTransfer).map(boTransfer -> boTransfer.getAttribute(FAILED)).orElse("false"));
        if(failed && null!=originalEventType && originalEventType.contains(SETTLED)){
            originalEventType = "RE".concat(originalEventType);
        }
        return originalEventType;
    }

    @Override
    protected Long loadTradeId() {
        return this.boCre.getTradeLongId();
    }

    @Override
    protected Double loadCreAmount() {
        return null != this.boCre ? this.boCre.getAmount(0) : 0.0D;
    }

    @Override
    public CollateralConfig getContract() {
        final int contractId = this.trade != null ? trade.getKeywordAsInt("CASource") : 0;
        return CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), contractId);
    }

    @Override
    protected String loadProductType() {
        return null != this.trade ? this.trade.getProductType() : "";
    }

    @Override
    protected String loadProccesingOrg() {
        return null != trade ? this.trade.getBook().getLegalEntity().getExternalRef() : "";
    }

    @Override
    protected String loadCounterParty() {
        return null != trade ? this.trade.getCounterParty().getExternalRef() : "";
    }

    @Override
    protected String getSubType() {
        return null != this.trade ? this.trade.getProductSubType() : "";
    }

    @Override
    protected String loadEndOfMonth() {
        return "";
    }

    private String getDesc(String desc) {
        return "PRINCIPAL".equalsIgnoreCase(desc) || "REDEMPTION".equalsIgnoreCase(desc) ? "AMORTIZATION" : desc;
    }

    private String getOwnIssuance() {
        CA ca = (CA) this.trade.getProduct();
        Bond bond = (Bond) ca.getSecurity();
        try {
            LegalEntity legalEntity = DSConnection.getDefault().getRemoteReferenceData().getLegalEntity(bond.getIssuerId());
            if ("BSTE".equalsIgnoreCase(legalEntity.getCode())) {
                return "SI";
            }else {
                return "NO";
            }
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error loading LE: " + bond.getIssuerId());
        }
        return "";
    }

    private String getBedulaAttribute() {
        CA ca = (CA) this.trade.getProduct();
        if (ca.getSecurity() instanceof Bond) {
            Bond bond = (Bond) ca.getSecurity();
            if (!Util.isEmpty(bond.getSecCode("IS COVERED"))) {
                return getLogicIsCovered(bond.getSecCode("IS COVERED"));
            }
        }
        return "";
    }

    private String getbondType() {

        if ("NO".equalsIgnoreCase(getBedulaAttribute())) {
            CA ca = (CA) this.trade.getProduct();
            Bond bond = (Bond) ca.getSecurity();
            if (!Util.isEmpty(bond.getSecCode("ISSUE_TYPE"))) {
                return getLogicBondType(bond.getSecCode("ISSUE_TYPE"));
            }
        }
        return "";
    }

    private String getLogicBondType(String issueType) {
        if ("BO".equalsIgnoreCase(issueType)) {
            return "BONO";
        } else if ("LT".equalsIgnoreCase(issueType)) {
            return "LETRA";
        } else if ("PG".equalsIgnoreCase(issueType)) {
            return "PAGARE";
        }
        return "";
    }

    private String getLogicIsCovered(String value) {

        if (Arrays.asList("Y", "S").contains(value)) {
            return "SI";
        } else if ("N".equalsIgnoreCase(value)) {
            return "NO";
        }
        return "";
    }


    protected String loadUnderlyingDeliveryType() {
        return "";
    }

}
