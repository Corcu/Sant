package calypsox.tk.report;

import com.calypso.tk.bo.Inventory;
import com.calypso.tk.report.ReportRow;

import java.util.ArrayList;
import java.util.List;

/**
 * @author acd
 */
public class BODisponibleTransferPositionBean {
    String key;
    List<ReportRow> positiveList;
    List<ReportRow> negativeList;
    List<Inventory> invToList;
    List<Inventory> invFromList;

    public BODisponibleTransferPositionBean(String key) {
        this.key = key;
        positiveList = new ArrayList<>();
        negativeList = new ArrayList<>();
        invToList = new ArrayList<>();
        invFromList = new ArrayList<>();
    }
    public String getKey() {
        return key;
    }
    public List<ReportRow> getPositiveList() {
        return positiveList;
    }
    public List<ReportRow> getNegativeList() {
        return negativeList;
    }
    public List<Inventory> getInvToList() {
        return invToList;
    }
    public List<Inventory> getInvFromList() {
        return invFromList;
    }


    public void addInvToList(Inventory invTo) {
        if(this.invToList==null){
            this.invToList = new ArrayList<>();
        }
        this.invToList.add(invTo);
    }
    public void addInvFromList(Inventory invFrom) {
        if(this.invFromList==null){
            this.invFromList = new ArrayList<>();
        }
        this.invFromList.add(invFrom);
    }
    public void addOnPositiveList(ReportRow row) {
        if(this.positiveList==null){
            this.positiveList = new ArrayList<>();
        }
        this.positiveList.add(row);
    }
    public void addOnNegativeList(ReportRow row) {
        if(this.negativeList==null){
            this.negativeList = new ArrayList<>();
        }
        this.negativeList.add(row);
    }

    public void cleanFromToList(){
        invFromList.clear();
        invToList.clear();
    }
}
