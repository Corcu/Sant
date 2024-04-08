package calypsox.tk.bo.cremapping.event;

import calypsox.ErrorCodeEnum;
import calypsox.tk.bo.cremapping.util.BOCreUtils;
import calypsox.tk.report.util.UtilReport;
import calypsox.util.OBBReportUtil;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.*;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.scheduling.service.RemoteSchedulingService;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.ScheduledTask;

import java.util.Vector;

public class BOCreMarginCallSEC_POS extends BOCreMarginCall {

    private static final String SEC_CODE = "ISIN";
    private Product security;

    public BOCreMarginCallSEC_POS(BOCre cre, Trade trade) {
        super(cre, trade);
    }

    @Override
    protected void init() {
        super.init();
        this.security = BOCreUtils.getInstance().loadSecurity(this.boCre);
    }

    @Override
    public void fillValues() {
        this.creDescription = "NPV";
        this.isin =  BOCreUtils.getInstance().loadIsin(this.security);
        this.underlyingType  = BOCreUtils.getInstance().loadUnderlyingType(this.security);
        this.valorization = loadValorization();
        this.portfolioStrategy = BOCreUtils.getInstance().loadPortfolioStrategy(this.boCre);
        this.currency2 = UtilReport.getCurrencyPair(loadCurrency());

    }

    @Override
    public CollateralConfig getContract() {
        String contractId = null!=this.boCre ? this.boCre.getAttributeValue("ContractId") : "";
        if(!Util.isEmpty(contractId)){
            return CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), Integer.parseInt(contractId));
        }
        return null;
    }

    @Override
    protected Double loadCreAmount(){
        return null!=this.boCre ? this.boCre.getAmount(1) : 0.0D;
    }

    @Override
    protected Account getAccount() {
        return null;
    }

    @Override
    protected Long loadTradeId() {
        return 0L;
    }

    @Override
    protected String loadCurrency() {
        return null != security ? security.getCurrency() : "";
    }

    @Override
    protected Double getPosition() {
        return 0.0;
    }

    @Override
    protected String loadStm() {
        return "";
    }

    @Override
    public JDate getCancelationDate() {
        return null;
    }

    @Override
    protected String loadIdentifierIntraEOD() {
        return "";
    }

    @Override
    protected String loadProductType() {
        return "MarginCall";
    }

    @Override
    protected String loadProccesingOrg(){
        return null!=this.collateralConfig ? this.collateralConfig.getProcessingOrg().getExternalRef() : "";
    }

    @Override
    protected String getSubType(){
        return "SECURITY";
    }

    @Override
    protected String loadCounterParty() {
        return null!=this.collateralConfig ? this.collateralConfig.getLegalEntity().getExternalRef() : "";
    }

    @Override
    protected String loadContractType() {
        final String contractType = super.loadContractType();
        if("CSD".equalsIgnoreCase(contractType)){
            return "IM";
        }
        return contractType;
    }

    private Double loadValorization(){
        Double ccyPairFx = UtilReport.getFXCurrencyPair(loadCurrency());
        return super.loadCreAmount() * ccyPairFx;
    }

    @Override
    protected String loadCreEventType() {
        String eventType = super.loadCreEventType();
        if(eventType.length()>16){
            return "MARGIN_SECURITY";
        }
        return eventType;
    }

    @Override
    protected String loadEndOfMonth() {
        ScheduledTask sct = null;
        String externalRef = getStName();

        try {
            RemoteSchedulingService schedulingService = DSConnection
                    .getDefault().getService(RemoteSchedulingService.class);
            sct = schedulingService
                    .getScheduledTaskByExternalReference(externalRef);
            if(null!=sct){
                return sct.getAttribute("Agrego");
            }
        } catch (final Exception exc) {
            String message = String.format(
                    "Could not retrieve Scheduled Task with external reference \"%s\"",
                    externalRef);
            Log.error(this, message, exc);
        }
        return "";
    }

    private String getStName(){
        Vector<String> domainValues = LocalCache.getDomainValues(DSConnection.getDefault(), "AgregoStName");
        if(!Util.isEmpty(domainValues)){
            return domainValues.get(0);
        }
        return "CUSTOM_EOD_SEC_MARGINCALL_VALUATION";
    }
}
