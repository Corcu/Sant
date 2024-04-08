package calypsox.tk.report;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.NettingMethod;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

import java.util.*;
import java.util.stream.Collectors;

public class NettingMethodsReport extends Report {
    @Override
    public ReportOutput load(Vector errorMsgs) {
        DefaultReportOutput output = new DefaultReportOutput(this);
        List<ReportRow> rows = new ArrayList<>();

        List<NettingMethod> nettingMethods = loadAllNettingMethods();
        filterNettingMethods(nettingMethods);

        nettingMethods.forEach(method -> {
            ReportRow row = new ReportRow(method,"NettingMethod");
            LegalEntity legalEntity = loadLe(method.getLegalEntityId());
            LegalEntity processingOrg = loadLe(method.getProcessingOrgId());
            row.setProperty("LegalEntity",legalEntity);
            row.setProperty("ProcessingOrg",processingOrg);
            rows.add(row);
        });

        setRows(rows,output);

        return output;
    }

    private List<NettingMethod> loadAllNettingMethods(){
        List<NettingMethod> nettingMethods = new ArrayList<>();
        try {
            nettingMethods = DSConnection.getDefault().getRemoteReferenceData().getNettingMethods();
        } catch (CalypsoServiceException e) {
            Log.error(this,"Error loading All Netting Methods: " + e);
        }
        return nettingMethods;
    }

    private void filterNettingMethods(List<NettingMethod> nettingMethods){
        String legalEntities = getReportTemplate().get("LegalEntities");
        String role = getReportTemplate().get("Role");
        String methodFilter = getReportTemplate().get("MethodFilter");
        //Filter by Counterparty or PO
        if(!Util.isEmpty(legalEntities)){
            Set<String> leList = new HashSet<>(Util.stringToList(legalEntities));
            List<NettingMethod> collect = new ArrayList<>();
            if("ProcessingOrg".equalsIgnoreCase(role)){
                collect = nettingMethods.stream().filter(method -> leList.contains(String.valueOf(method.getProcessingOrgId()))).collect(Collectors.toList());
            }else if("CounterParty".equalsIgnoreCase(role)){
                collect = nettingMethods.stream().filter(method -> leList.contains(String.valueOf(method.getLegalEntityId()))).collect(Collectors.toList());
            }
            nettingMethods.clear();
            nettingMethods.addAll(collect);
        }

        //Filter by Method
        if(!Util.isEmpty(methodFilter)){
            List<NettingMethod> collect = nettingMethods.stream().filter(method -> methodFilter.equalsIgnoreCase(method.getSettleMethod())).collect(Collectors.toList());
            nettingMethods.clear();
            nettingMethods.addAll(collect);
        }

    }

    private LegalEntity loadLe(int id){
        return BOCache.getLegalEntity(DSConnection.getDefault(), id);
    }

    private void setRows(List<ReportRow> rows,DefaultReportOutput output){
        if(!Util.isEmpty(rows) && null!=output) {
            output.setRows(rows.toArray(new ReportRow[rows.size()]));
        }
    }


}
