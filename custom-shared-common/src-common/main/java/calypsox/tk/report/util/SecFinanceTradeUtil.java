package calypsox.tk.report.util;


import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.*;
import com.calypso.tk.refdata.StaticDataFilter;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Optional;
import java.util.Vector;

public class SecFinanceTradeUtil {
    private static SecFinanceTradeUtil instance = new SecFinanceTradeUtil();

    private static final String K_MXINITIAL_DIRTYPRICE =  "MXInitialDirtyPrice";
    private static final String K_MXINITIAL_PRICE =  "MXInitialEquityPrice";
    private static final String K_MXINITIAL_MARGIN =  "MXInitialMargin";
    private static final String K_MXINITIAL_CELAN_PRICE =  "MxInitialCleanPrice";

    public static final String SD_FILTER_REPO_VKS = "isRepoVoighKampffSettlement";

    public synchronized static SecFinanceTradeUtil getInstance() {
        if (instance == null) {
            instance = new SecFinanceTradeUtil();
        }
        return instance;
    }

    public boolean isInternal(Trade trade) {
        return Optional.ofNullable(trade.getMirrorBook()).isPresent();
    }

    public String isInternalOper(Trade trade) {
        return Optional.ofNullable(trade.getMirrorBook()).isPresent() ? "Y" : "N";
    }

    public boolean isOpen(Trade trade){
        return null!=trade && "OPEN".equalsIgnoreCase(((SecFinance) trade.getProduct()).getMaturityType());
    }

    public static boolean isVoighKampffSettlement(Trade trade, BOTransfer transfer) {
        if (trade ==null || !(trade.getProduct() instanceof SecFinance))
            return false;
        StaticDataFilter sdFilter = BOCache.getStaticDataFilter(DSConnection.getDefault(), SD_FILTER_REPO_VKS);
        return sdFilter.accept(trade, transfer);
    }

    public String getInflationFactor(SecFinance product) {
        if (Optional.of(product).map(SecFinance::getSecurity)
                .filter(pro -> pro instanceof Bond)
                .map(bond -> ((Bond) bond).getNotionalIndex()).isPresent())
            return "S";
        return "N";
    }

    public Collateral getCollateral(Trade trade){
        return Optional.of(trade).map(Trade::getProduct)
                .filter(s -> s instanceof SecFinance)
                .map(sec -> ((SecFinance) sec).getCollaterals())
                .map(v -> v.get(0)).orElse(null);
    }

    public String formatValue(Double value,String formatValue){
        if(!Double.isNaN(value)){
            String pattern = "0.0###########";
            if(!Util.isEmpty(formatValue)){
                pattern = formatValue;
            }
            final DecimalFormat myFormatter = new DecimalFormat(pattern);
            final DecimalFormatSymbols tmp = myFormatter.getDecimalFormatSymbols();
            tmp.setDecimalSeparator('.');
            myFormatter.setDecimalFormatSymbols(tmp);

            String format = myFormatter.format(value);
            return !"NaN".equalsIgnoreCase(format) && !"-0.00".equalsIgnoreCase(format) ? format : "0.00";
        }
        return "0.0";
    }

    public String formatValue(String value){
        if(null!=value && value.contains(",")){
            value = value.replace(",",".");
            Double aDouble = Double.valueOf(value);
            return formatValue(aDouble,"");
        }
        return "0.0";
    }

    public String getSettlementType(Trade trade){
        String s = Optional.ofNullable(trade).map(t -> ((SecFinance) t.getProduct()).getDeliveryType()).orElse("");
        switch (s){
            case "DAP":
                return "DVP";
            case "DFP":
                return "FOP";
            default:
                return "";
        }
    }

    public String getCashRateType(Cash cash){
        String rateType = Optional.ofNullable(cash).map(Cash::getRateType).orElse("");
        switch (rateType){
            case "Floating":
                return "Flotante";
            case "Fixed":
                return "Fijo";
            default:
                return "";
        }
    }

    public String getIndexName(Cash cash){
        return Optional.ofNullable(cash)
                .filter(ca -> ca.getRateIndex()!=null)
                .map(c -> c.getRateIndex().getName()+c.getRateIndex().getTenor().toString()).orElse("");
    }

    public String getSMMDIndex(Cash cash){
        StringBuilder index = new StringBuilder();
        Optional.ofNullable(cash.getRateIndex()).ifPresent(rateIdx -> {
            index.append(rateIdx.getCurrency());
            index.append(rateIdx.getName());
            index.append(rateIdx.getTenor().toString());
            index.append(rateIdx.getSource());
        });
        return  index.toString();
    }

    public String getSMMDSusiIndex(String value){
        String repoSMMDIndexSUSI = LocalCache.getDomainValueComment(DSConnection.getDefault(), "RepoSMMDIndexSUSI", value);
        return  !Util.isEmpty(repoSMMDIndexSUSI) ? repoSMMDIndexSUSI : "99999";
    }

    private String getDirtyPrice(Trade trade, ReportRow row, Vector errors){
        if(null!=trade){
            Product security = ((SecLending) trade.getProduct()).getSecurity();
            if(security!=null){
                if(security instanceof Bond){
                    return formatDecimal(trade.getKeywordAsDouble(K_MXINITIAL_DIRTYPRICE));
                }else if(security instanceof Equity){
                    return formatDecimal(trade.getKeywordAsDouble(K_MXINITIAL_PRICE));
                }
            }
        }
        return "";
    }

    public String formatDecimal(final double value) {
        String decimal = String.valueOf(value);
        final DecimalFormat myFormatter = new DecimalFormat("0.00");
        final DecimalFormatSymbols tmp = myFormatter.getDecimalFormatSymbols();
        tmp.setDecimalSeparator('.');
        myFormatter.setDecimalFormatSymbols(tmp);
        String format = myFormatter.format(value);
        return format;
    }


}
