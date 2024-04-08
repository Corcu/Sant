package calypsox.tk.anacredit.processor;

import calypsox.tk.anacredit.formatter.AnacreditFormatter;
import calypsox.tk.anacredit.util.AnacreditMapper;
import calypsox.tk.anacredit.util.AnacreditUtilities;
import calypsox.tk.anacredit.util.RepoTypeIdentifier;
import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Repo;
import com.calypso.tk.product.SecFinance;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.refdata.MarginCallConfigInterface;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.SecLendingReportStyle;
import com.calypso.tk.service.DSConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CacheModuloD {

    private ConcurrentHashMap<String, Double> _totalNominal;
    private ConcurrentHashMap<String, Double> _totalMarketValue;
    private ConcurrentHashMap<String, Double> _totalQtd;

    private ConcurrentHashMap<String, Boolean> _reportedItems;
    private ConcurrentHashMap<String, JDate> _fechaGarantia;

    public CacheModuloD() {
        _totalNominal = new ConcurrentHashMap<>();
        _totalMarketValue = new ConcurrentHashMap<>();
        _totalQtd =  new ConcurrentHashMap<>();
        _reportedItems = new ConcurrentHashMap<>();
        _fechaGarantia = new ConcurrentHashMap<>();

    }

    public boolean contains(String key) {
        return _reportedItems.containsKey(key);
    }

    public void addReported(String key) {
        _reportedItems.putIfAbsent(key, true);
    }

    public Double getTotalQtd(String key) {
        return _totalQtd.get(key);
    }
    public Double getTotalNominal(String key) {
        return _totalNominal.get(key);
    }
    public Double getTotalMarketValue(String key) {
        return _totalMarketValue.get(key);
    }

    public JDate getFechaGarantia(String key) {
        return _fechaGarantia.get(key);
    }

    public  void computeTotalMarketValue(String key, Double value ) {
        // first check is exists
        _totalMarketValue.computeIfPresent(key, (s, aDouble) -> aDouble + value);
        // then add if not
        _totalMarketValue.computeIfAbsent(key, s -> (value));
    }
    public  void computeTotalNominal(String key, Double value ) {
        // first check is exists
        _totalNominal.computeIfPresent(key, (s, aDouble) -> aDouble + value);
        // then add if not
        _totalNominal.computeIfAbsent(key, s -> (value));
    }

    public  void computeQuantity(String key, Double value ) {
        // first check is exists
        _totalQtd.computeIfPresent(key, (s, aDouble) -> aDouble + value);
        // then add if not
        _totalQtd.computeIfAbsent(key, s -> (value));
    }

    public  void computeFechaGarantia(String key, JDate value ) {
        // first check is exists
        _fechaGarantia.computeIfPresent(key, (s, aDate) -> aDate.after(value) ? value : aDate);
        // then add if not
        _fechaGarantia.computeIfAbsent(key, s -> (value));
    }


    public void  buildCacheModuloD(List<ReportRow> reportRows) {
        if (reportRows == null) {
            return;
        }
        reportRows.stream().forEach(reportRow -> {
            final Trade trade = reportRow.getProperty(ReportRow.TRADE);
            JDatetime valDateTime = reportRow.getProperty(ReportRow.VALUATION_DATETIME);
            if (!(trade.getProduct() instanceof SecFinance)) {
                return;
            }
            SecFinance secFinance = (SecFinance) trade.getProduct();

            // prestamos solo ventas
            if (secFinance instanceof SecLending
                    &&  trade.getQuantity() > 0)  {
                return;
            }

            if (secFinance instanceof SecLending) {
                MarginCallConfig mcc = getMarginCallConfig(reportRow);
                if (mcc == null) {
                    return;
                }
            }

            if (secFinance instanceof Repo) {
                RepoTypeIdentifier repoType = new RepoTypeIdentifier(trade, valDateTime).invoke();
                if (!repoType.isATA()) {
                    return;
                }
            }

            String key = buildKey(trade, secFinance);
            if (key == null) return;

            String name = secFinance instanceof  SecLending ? "NPV_COLLAT" : "Principal Amount";

            Double marketValue = reportRow.getProperty(name);
            if (marketValue != null) {
                marketValue = AnacreditUtilities.convertToEUR(marketValue, trade.getTradeCurrency(), valDateTime.getJDate(TimeZone.getDefault()), null);
                BigDecimal bd = BigDecimal.valueOf(marketValue);
                bd = bd.setScale(2, RoundingMode.HALF_UP);
                computeTotalMarketValue(key, bd.doubleValue());

            }


            Double nominal =  reportRow.getProperty(secFinance instanceof  SecLending ? "Nominal" : "Sec. Nominal (Current)");
            if (nominal != null) {
                nominal = AnacreditUtilities.convertToEUR(nominal, trade.getTradeCurrency(), valDateTime.getJDate(TimeZone.getDefault()), null);
                BigDecimal bd = BigDecimal.valueOf(nominal);
                bd = bd.setScale(2, RoundingMode.HALF_UP);
                computeTotalNominal(key, bd.doubleValue());
            }

            Double qty =  reportRow.getProperty("Quantity");
            computeQuantity(key, qty);

            computeFechaGarantia(key, trade.getTradeDate().getJDate(TimeZone.getDefault()));

        });
    }

    public static String buildKey(Trade trade, SecFinance secFinance) {

        if (secFinance instanceof SecLending) {

            MarginCallConfig mcc = BOCache.getMarginCallConfig(DSConnection.getDefault(),trade);
            if (mcc == null) {
                return null;
            }
            return  mcc.getId()  + AnacreditMapper.getPaisNegocio(trade.getBook().getLegalEntity()) + secFinance.getSecurity().getSecCode("ISIN");
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(secFinance.getSecurity().getSecCode("ISIN"));
            if (secFinance.getSecurity() instanceof Bond) {
                Bond bond = (Bond) secFinance.getSecurity();
                if (null != bond.getIssueDate()) {
                    sb.append(formatDate(bond.getIssueDate()));
                }
            }
            sb.append(AnacreditMapper.getPaisNegocio(trade.getBook().getLegalEntity()));
            String jmin =   AnacreditMapper.getJMin(trade.getCounterParty());
            if (!Util.isEmpty(jmin)) {
                sb.append(jmin);
            }
            return sb.toString();
        }
    }

    public static String formatDate(JDate date){
        if(null!=date){
            JDateFormat format = new JDateFormat("yyyyMMdd");
            return format.format(date);
        }
        return "";
    }

    public static MarginCallConfig getMarginCallConfig(ReportRow reportRow) {
        MarginCallConfigInterface<?, ?> mcConfig = SecLendingReportStyle.getSecFinanceTradeEntry(reportRow).getMarginCallContract();
        if (mcConfig != null && mcConfig.getId() > 0) {
            return (MarginCallConfig) mcConfig;
        }
        return null;
    }

}
