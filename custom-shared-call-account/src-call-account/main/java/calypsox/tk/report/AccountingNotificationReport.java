package calypsox.tk.report;

import calypsox.tk.report.generic.loader.AccountingNotificationLoader;
import com.calypso.tk.core.JDate;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;

import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AccountingNotificationReport extends SantInterestNotificationReport{
	
	public static final String MOVEMENTS_CALCULATED = "MOVEMENTS_CALCULATED";

    @Override
    protected boolean checkProcessStartDate() {
        return true;
    }

    @Override
    protected boolean checkProcessEndDate() {
        return false;
    }

    @Override
    protected ReportOutput getReportOutput() throws Exception {

        final DefaultReportOutput output = new DefaultReportOutput(this);
        final AccountingNotificationLoader loader = new AccountingNotificationLoader();

        JDate startYear = JDate.valueOf("01/01/" + String.valueOf(getProcessStartDate().getYear())); //TODO corregir fecha

        JDate startLast = JDate.valueOf("01/10/" + String.valueOf(getProcessStartDate().getYear() - 1));
        JDate endLast = JDate.valueOf("31/12/" + String.valueOf(getProcessStartDate().getYear() - 1));

        final List<AccountingNotificationEntry> entries = loader.loadAccounting(getReportTemplate(), startYear,
                getProcessStartDate(), getValDate());

        final List<AccountingNotificationEntry> pastEntries = loader.loadAccounting(getReportTemplate(), startLast,
                endLast, getValDate());

        Map<String, AccountingNotificationEntry> entriesMap = convertToMap(entries);
        final List<AccountingNotificationEntry> entriesFinal = joinLastWithTotal(entriesMap,pastEntries);

        final ReportRow[] rows = new ReportRow[entriesFinal.size()];

        boolean movementsCalculated = false;
        if(getReportTemplate().getVisibleColumns().contains(SantInterestNotificationReportStyle.MOVEMENT)){
        	movementsCalculated=true;
        }
        
        for (int i = 0; i < entriesFinal.size(); i++) {
            ReportRow row = new ReportRow(entriesFinal.get(i), AccountingNotificationTemplate.ROW_DATA);
            row.setProperty(MOVEMENTS_CALCULATED, movementsCalculated);
            rows[i] = row;
        }
        output.setRows(rows);

        return output;
    }

    public List<AccountingNotificationEntry> joinLastWithTotal(Map<String, AccountingNotificationEntry> entriesMap, List<AccountingNotificationEntry> pastEntries) {
        for (AccountingNotificationEntry pastEntry : pastEntries) {
            String clave = pastEntry.getKey();
            if (pastEntry.getAdjustement() != 0) {
                if (entriesMap.keySet().contains(clave)) {
                    AccountingNotificationEntry entryMap = entriesMap.get(clave);
                    entryMap.setAdjustement(entryMap.getAdjustement() + pastEntry.getAdjustement());
                    entriesMap.put(clave, entryMap);

                } else {
                    pastEntry.setCurrentLiveBalance(0.0);
                    pastEntry.setUnliquidatedAccumulatedPeriodic(0.0);
                    pastEntry.setInteretanual(0.0);
                    pastEntry.setAnnualNegativeInterest(0.0);
                    pastEntry.setAnnualPositiveInterest(0.0);
                    entriesMap.put(clave, pastEntry);
                }
            }
        }
        return entriesMap.values().stream().collect(Collectors.toList());
    }


    public Map<String, AccountingNotificationEntry> convertToMap(List<AccountingNotificationEntry> entriesList) {
        return entriesList.stream().collect(Collectors.toMap(AccountingNotificationEntry::getKey, Function.identity()));
    }

    @Override
    public ReportOutput loadReport(Vector<String> errorMsgs) {
        return super.loadReport(errorMsgs);
    }
}
