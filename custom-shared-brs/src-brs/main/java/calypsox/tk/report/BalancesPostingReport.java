package calypsox.tk.report;

import calypsox.tk.bo.acyg.BalancesPostingBean;
import com.calypso.tk.bo.BalancePosition;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.report.BalanceReport;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;

import java.util.*;
import java.util.stream.Collectors;

public class BalancesPostingReport extends BalanceReport {

    HashMap<String, ArrayList<ReportRow>> filterRows;

    @Override
    public ReportOutput load(Vector errorMsgs) {
        resetValues();

        DefaultReportOutput output = (DefaultReportOutput) super.load(errorMsgs);
        StandardReportOutput standardReportOutput = new StandardReportOutput(this);
        List<ReportRow> rows = Arrays.stream(output.getRows()).collect(Collectors.toList());
        groupRowsByPartenonAccount(rows);
        ReportRow[] finalRows = createNewRows(filterRows);

        standardReportOutput.setRows(finalRows);

        return standardReportOutput;
    }


    private void groupRowsByPartenonAccount(List<ReportRow> rows) {

        if (!Util.isEmpty(rows)) {
            rows.stream().forEach(this::fillMap);
        }
    }

    private void fillMap(ReportRow row) {
        ArrayList<ReportRow> filas = new ArrayList<>();

        String key = getKeyByRow(row);
        if (!Util.isEmpty(key)) {
            if (filterRows.containsKey(key)) {
                filas = getfinalBalances(filterRows.get(key), row);
                filterRows.put(key, filas);
            } else {
                filas.add(row);
                filterRows.put(key, filas);
            }
        }
    }
    
    private ArrayList<ReportRow> getfinalBalances(ArrayList<ReportRow> filas, ReportRow row) {
        boolean anyMatch = true;
        BalancePosition bal = (BalancePosition) row.getProperty("Default");

        Iterator<ReportRow> fila = filas.listIterator();
        while (fila.hasNext()) {
            BalancePosition oldBal = (BalancePosition) fila.next().getProperty("Default");
            if (bal.getCurrency().equalsIgnoreCase(oldBal.getCurrency())) {
                if (bal.getPositionDate().after(oldBal.getPositionDate())) {
                    fila.remove();
                } else {
                    anyMatch = false;
                }
            }
        }

        if (anyMatch) {
            filas.add(row);
        }
        return filas;
    }

    private String getKeyByRow(ReportRow row) {
        Optional<Account> account = Optional.ofNullable(row.getProperty("Account"));
        Optional<Trade> trade = Optional.ofNullable(row.getProperty("Trade"));

        if (account.isPresent() && trade.isPresent()) {
            String partenon = Optional.ofNullable(trade.get().getKeywordValue(BalancesPostingBean.PARTENONACCOUNTINGID)).orElse("");
            if (!Util.isEmpty(partenon)) {
                return partenon + "-" + account.get().getName();
            }
        }
        return "";
    }

    private ReportRow[] createNewRows(HashMap<String, ArrayList<ReportRow>> filterRows) {

        ReportRow[] reportRows = filterRows.values().stream()
                .parallel()
                .map(this::generateACyGBean)
                .map(ReportRow::new).toArray(ReportRow[]::new);

        return reportRows;

    }

    private BalancesPostingBean generateACyGBean(List<ReportRow> rows) {
        BalancesPostingBean bean = new BalancesPostingBean();

        for (ReportRow opRow : rows) {
            if (null != opRow) {
                final Optional<Trade> trade = Optional.ofNullable(opRow.getProperty("Trade"));
                final Optional<BalancePosition> balance = Optional.ofNullable(opRow.getProperty("Default"));
                final Optional<Account> account = Optional.ofNullable(opRow.getProperty("Account"));
                final Optional<String> currency = Optional.ofNullable(opRow.getProperty("Currency"));
                final Optional<PricingEnv> pricingEnv = Optional.ofNullable(opRow.getProperty("PricingEnv"));

                if (balance.isPresent() && trade.isPresent() && account.isPresent() && pricingEnv.isPresent() && currency.isPresent()) {
                    bean.setTrade(trade.get());
                    bean.setAccount(account.get());
                    bean.setBalance(balance.get());
                    bean.setProcessDate(this.getValDate());
                    bean.setCurrency(currency.get());
                    bean.setPricingEnv(pricingEnv.get());
                    bean.build();
                }
            }
        }
        return bean;
    }

    public void resetValues() {
        this.filterRows = new HashMap<>();
    }
}
