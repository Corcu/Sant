package calypsox.tk.report;

import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.stream.Collectors;

public class BODisponibleBlockingPositionReport extends BODisponibleSecurityPositionReport {

    @Override
    public ReportOutput load(Vector errorMsgs) {
        DefaultReportOutput output = (DefaultReportOutput) super.load(errorMsgs);
        if(null!=output){
            groupByAccountType(output);
        }
        return output;
    }

    private void groupByAccountType(DefaultReportOutput output){
        List<ReportRow> rows = Arrays.stream(output.getRows()).collect(Collectors.toList());

        rows.parallelStream().forEach(row ->{
            InventorySecurityPosition inventory = (InventorySecurityPosition) row.getProperty("Inventory");
            if(null!=inventory){
                boolean blockingAccount = isBlockingAccount(inventory.getAccount());
                boolean pigAccount = isPignoracionAccount(inventory.getAccount());

                if(blockingAccount){
                    row.setProperty("BlockingType","BLOQUEO");
                }else if(pigAccount){
                    row.setProperty("BlockingType","PIGNORACION");
                }
            }
        });
    }

    private boolean isBlockingAccount(Account account){
        return Optional.ofNullable(account).map(acc -> acc.getAccountProperty("Bloqueo")).filter("true"::equalsIgnoreCase).isPresent();
    }

    private boolean isPignoracionAccount(Account account){
        return Optional.ofNullable(account).map(acc -> acc.getAccountProperty("Pignoracion")).filter("true"::equalsIgnoreCase).isPresent();
    }

}
