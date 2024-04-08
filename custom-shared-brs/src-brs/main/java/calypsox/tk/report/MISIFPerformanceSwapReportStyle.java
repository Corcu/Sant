package calypsox.tk.report;

import calypsox.tk.bo.mis.PerSwapMisBean;
import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.util.product.BOTransferUtil;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import com.calypso.tk.util.MessageArray;

import java.security.InvalidParameterException;
import java.util.Optional;
import java.util.Vector;

public class MISIFPerformanceSwapReportStyle extends TradeReportStyle {


    public static final String OFFICIAL_ACCOUNTING = "OFFICIAL_ACCOUNTING";

    //COLUMN NAMES
    public static final String ORIGINAL_ID = "ORIGINAL_ID";
    public static final String BO_ID = "BO_ID";
    public static final String CPTY_GLCS = "CPTY_GLCS";
    public static final String USI = "USI";
    public static final String PRIOR_USI = "PRIOR_USI";
    public static final String UTI = "UTI";
    public static final String PRIOR_UTI = "PRIOR_UTI";
    public static final String CONFIRMATION_TYPE = "CONFIRMATION_TYPE";
    public static final String CONFIRMATION_DATE_TIME = "CONFIRMATION_DATE_TIME";
    public static final String UPI = "UPI";
    public static final String COLLATERALIZATION = "COLLATERALIZATION";
    public static final String ORIGIN = "ORIGIN";
    public static final String PRODUCT = "PRODUCT";
    public static final String BRANCH_ID = "BRANCH_ID";
    public static final String DEAL_ID = "DEAL_ID";
    public static final String INSTRTYPE = "INSTRTYPE";
    public static final String DIRECTION = "DIRECTION";
    public static final String CURRENCY = "CURRENCY";

    public static final String[] ADDITIONAL_COLUMNS = {ORIGINAL_ID,BO_ID,CPTY_GLCS,USI,PRIOR_USI,UTI,PRIOR_UTI,CONFIRMATION_TYPE,CONFIRMATION_DATE_TIME,UPI,
            COLLATERALIZATION,ORIGIN,PRODUCT,BRANCH_ID,DEAL_ID,INSTRTYPE,DIRECTION,CURRENCY};

    @Override
    @SuppressWarnings("rawtypes")
    public Object getColumnValue(ReportRow row, String columnId, Vector errors)
            throws InvalidParameterException {

        final Trade trade = row.getProperty(ReportRow.TRADE);

        if(trade != null){
            final Product product = trade.getProduct();
            final LegalEntity cpty = trade.getCounterParty();

            if (ORIGINAL_ID.equals(columnId)) {
                if (trade != null) {
                    return trade.getKeywordValue("MurexTradeID");
                }
            } else if (BO_ID.equals(columnId)) {
                if (trade != null) {
                    return trade.getLongId();
                }
            } else if (CPTY_GLCS.equals(columnId)) {
                if (cpty != null) {
                    return cpty.getCode();
                }
            } else if (USI.equals(columnId)) {
                if (trade != null) {
                    return trade.getKeywordValue("USI_REFERENCE");
                }
            } else if (PRIOR_USI.equals(columnId)) {
                if (trade != null) {
                    String prefix = trade.getKeywordValue("USIPrefix");
                    String value = trade.getKeywordValue("USIValue");
                    if(prefix != null && value != null) {
                        return prefix + value;
                    } else return "";
                }
            } else if (UTI.equals(columnId)) {
                if (trade != null) {
                    return (trade.getKeywordValue("UTI_REFERENCE"));
                }
            } else if (PRIOR_UTI.equals(columnId)) {
                if (trade != null) {
                    return (trade.getKeywordValue("PriorUTI"));
                }
            } else if (CONFIRMATION_TYPE.equals(columnId)) {
                try {
                    MessageArray messages = DSConnection.getDefault().getRemoteBO().getMessages(trade.getLongId());
                    for(int i = 0; i < messages.size(); i++){
                        BOMessage message = messages.get(i);
                        if(message.getMessageType().equalsIgnoreCase("P37_EXPORT")){
                            return "Not Confirmed";
                        }
                    }
                } catch (CalypsoServiceException e) {
                    e.printStackTrace();
                }
                return "";
            } else if (CONFIRMATION_DATE_TIME.equals(columnId)) {
                if (trade != null) {
                    return (trade.getKeywordValue("ConfirmationDateTime"));
                }
            } else if (UPI.equals(columnId)) {
                if (trade != null) {
                    return (trade.getKeywordValue("UPI_REFERENCE"));
                }
            } else if (COLLATERALIZATION.equals(columnId)) {
                if (trade != null) {
                    String keyword = trade.getKeywordValue("MC_CONTRACT_NUMBER");
                    if (keyword != null && !keyword.isEmpty()) {
                        CollateralConfig cc = getMarginCallContractFromTradeId(trade.getLongId());
                        if (cc != null) {
                            String emir_collateral = cc.getAdditionalField("EMIR_COLLATERAL_VALUE");
                            return emir_collateral;
                        } else return "UNCOLLATERALIZED";
                    } else return "UNCOLLATERALIZED";
                }
            } else if (ORIGIN.equals(columnId)) {
                return "800018693";
            } else if (PRODUCT.equals(columnId)) {
                return "BNDFWD";
            } else if (BRANCH_ID.equals(columnId)) {
                if (trade != null && trade.getBook() != null && trade.getBook().getLegalEntity() != null)
                    return trade.getBook().getLegalEntity().getLongId();
            } else if (DEAL_ID.equals(columnId)) {
                if (trade != null)
                    return trade.getLongId();
            } else if (INSTRTYPE.equals(columnId)) {
                if (trade != null) {
                    String partenonId = trade.getKeywordValue("PartenonAccountingID");
                    if(!Util.isEmpty(partenonId) && partenonId.length() == 21){
                        return partenonId.substring(18, 21);
                    } else return partenonId;
                    }
            } else if (DIRECTION.equals(columnId)) {
                if (trade.getProduct() instanceof Bond) {
                    Bond bond = (Bond) trade.getProduct();
                    int buySell = bond.getBuySell(trade);
                    return buySell == 1 ? "COMPRA" : "VENTA";
                }
            } else if (CURRENCY.equals(columnId)) {
                if (trade != null)
                    return trade.getTradeCurrency();
            } else return "";
            return "";
        } else {
            Optional<PerSwapMisBean> beanOpt = Optional.ofNullable(row.getProperty("PerSwapMisBean"));
            row.setProperty("Default",beanOpt.get().getTrade());
            if(beanOpt.isPresent()) {
                PerSwapMisBean bean = beanOpt.get();

                if (ORIGINAL_ID.equals(columnId)) {
                    if(beanOpt.get().getTrade() != null) {
                        return beanOpt.get().getTrade().getKeywordValue("MurexTradeID");
                    }
                } else if (BO_ID.equals(columnId)) {
                    if(bean != null) {
                        return bean.getDeal_id();
                    }
                } else if (CPTY_GLCS.equals(columnId)) {
                    if(bean != null) {
                        return bean.getGlscounterparty();
                    }
                } else if (USI.equals(columnId)) {
                    if(beanOpt.get().getTrade() != null) {
                        return beanOpt.get().getTrade().getKeywordValue("USI_REFERENCE");
                    }
                } else if (PRIOR_USI.equals(columnId)) {
                    if (beanOpt.get().getTrade() != null) {
                        String prefix = beanOpt.get().getTrade().getKeywordValue("USIPrefix");
                        String value = beanOpt.get().getTrade().getKeywordValue("USIValue");
                        if(prefix != null && value != null) {
                            return prefix + value;
                        } else return "";
                    }
                } else if (UTI.equals(columnId)) {
                    if(beanOpt.get().getTrade() != null) {
                        return (beanOpt.get().getTrade().getKeywordValue("UTI_REFERENCE"));
                    }
                } else if (PRIOR_UTI.equals(columnId)) {
                    if(beanOpt.get().getTrade() != null) {
                        return (beanOpt.get().getTrade().getKeywordValue("PriorUTI"));
                    }
                } else if (CONFIRMATION_TYPE.equals(columnId)) {
                    try {
                        MessageArray messages = DSConnection.getDefault().getRemoteBO().getMessages(beanOpt.get().getTrade().getLongId());
                        for(int i = messages.size() -1 ; i >= 0; i--){
                            BOMessage message = messages.get(i);
                            if(message.getMessageType().equalsIgnoreCase("P37_EXPORT")){
                                return "Non-Electronic";
                            }
                        }
                    } catch (CalypsoServiceException e) {
                        e.printStackTrace();
                    }
                    return "";
                } else if (CONFIRMATION_DATE_TIME.equals(columnId)) {
                    if(beanOpt.get().getTrade() != null) {
                        return (beanOpt.get().getTrade().getKeywordValue("ConfirmationDateTime"));
                    }
                } else if (UPI.equals(columnId)) {
                    if(beanOpt.get().getTrade() != null) {
                        return (beanOpt.get().getTrade().getKeywordValue("UPI_REFERENCE"));
                    }
                } else if (COLLATERALIZATION.equals(columnId)) {
                    if(beanOpt.get().getTrade() != null) {
                        String keyword = beanOpt.get().getTrade().getKeywordValue("MC_CONTRACT_NUMBER");
                        if(keyword != null && !keyword.isEmpty()){
                            CollateralConfig cc = getMarginCallContractFromTradeId(beanOpt.get().getTrade().getLongId());
                            if(cc != null){
                                String emir_collateral = cc.getAdditionalField("EMIR_COLLATERAL_VALUE");
                                return emir_collateral;
                            } else return "UNCOLLATERALIZED";
                        }else return "UNCOLLATERALIZED";
                    }
                } else if (ORIGIN.equals(columnId)) {
                    if(bean != null)
                        return bean.getOrigin();
                } else if (PRODUCT.equals(columnId)) {
                    return "BRS";
                } else if (BRANCH_ID.equals(columnId)) {
                    if(bean != null)
                        return bean.getBranch_id();
                } else if (DEAL_ID.equals(columnId)) {
                    if(bean != null)
                        return bean.getDeal_id();
                } else if (INSTRTYPE.equals(columnId)) {
                    if(bean != null)
                        return bean.getInstrtype();
                } else if (DIRECTION.equals(columnId)) {
                    if(bean != null)
                        return bean.getDirection();
                } else if (CURRENCY.equals(columnId)) {
                    if(bean != null)
                        return bean.getCurrency();
                } else return "";
            } else return "";

        }
        return "";
    }


    /**
     * @param tradeId
     * @return
     * @throws CalypsoServiceException
     */
    private static CollateralConfig getMarginCallContractFromTradeId(long tradeId) {
        Trade trade = null;
        try {
            trade = DSConnection.getDefault().getRemoteTrade().getTrade(tradeId);
        } catch (CalypsoServiceException exc) {
            Log.error(BOTransferUtil.class.getSimpleName(), exc.getCause());
        }
        return CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), getContractIdFromTrade(trade));
    }

    /**
     * @param trade
     * @return
     */
    private static int getContractIdFromTrade(Trade trade) {
        int contractId = 0;
        if (!java.util.Objects.isNull(trade)) {
            if (trade.getProduct() instanceof MarginCall) {
                contractId = ((MarginCall) trade.getProduct()).getMarginCallId();
            } else {
                Optional<Integer> integerOpt = Optional.ofNullable(trade.getKeywordValue(CollateralStaticAttributes.MC_CONTRACT_NUMBER)).map(Integer::parseInt);
                contractId = integerOpt.orElse(0);
            }
        }
        return contractId;
    }
}
