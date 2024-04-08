package calypsox.tk.report;

import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.AuditValue;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.core.sql.SQLQuery;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.sql.CollateralConfigFilter;
import com.calypso.tk.report.AuditReport;
import com.calypso.tk.report.CollateralConfigReport;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * @author aalonsop
 */
public class MCLyncsReport extends CollateralConfigReport {

    @Override
    protected List<CollateralConfig> loadMarginCallConfigs(ReportTemplate template) {
        List<Integer> collatConfigIds=getCollatConfigIds();
        List<CollateralConfig> collateralConfigs=new ArrayList<>();
        if(!Util.isEmpty(collatConfigIds)) {
            CollateralConfigFilter ccFilter=new CollateralConfigFilter();
            ccFilter.setMccIds(collatConfigIds);
            try {
                collateralConfigs=ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfigByFilter(ccFilter, true);
                collateralConfigs=collateralConfigs.stream().filter(collateralConfig -> !Util.isEmpty(collateralConfig.getAdditionalField("TRLYNCS_ID"))).collect(Collectors.toList());
            } catch (CollateralServiceException exc) {
                Log.error(this.getClass().getSimpleName(),exc.getMessage());
            }
        }
        return collateralConfigs;
    }

    private List<Integer> getCollatConfigIds(){
        List<Integer> configIds=new ArrayList<>();
        AuditReport auditReport = new AuditReport();
        DefaultReportOutput outputAudit = (DefaultReportOutput) auditReport.load(buildMarginCallConfigSQL(), new Vector());
        for (ReportRow row : outputAudit.getRows()) {
            int ccId = Optional.ofNullable((AuditValue) row.getProperty(ReportRow.AUDIT)).map(AuditValue::getEntityId)
                    .orElse(0);
            if (ccId > 0) {
                configIds.add(ccId);
            }
        }
        return configIds;
    }

    /**
     * @return
     */
    private SQLQuery buildMarginCallConfigSQL() {
        int cutoffHour=Optional.ofNullable(this.getReportTemplate().get("UpdatedStartDate"))
                .map(cutoffStr->Integer.valueOf((String) cutoffStr)).orElse(0);
        SQLQuery query = new SQLQuery();
        query.appendWhereClause("ENTITY_CLASS_NAME = 'CollateralConfig'");
        query.appendWhereClause("MODIF_DATE BETWEEN " + Util.datetime2SQLString(buildJDatetime(this.getValDate(),cutoffHour,0,0,0))
                        + "AND " + Util.datetime2SQLString(buildJDatetime(this.getValDate(),getPreviousHour(cutoffHour),59,59,999)));
        query.appendWhereClause("ENTITY_FIELD_NAME = '_CREATE_'");
        return query;
    }

    private JDatetime buildJDatetime(JDate jdate, int hour, int minutes, int seconds, int millis){
        return new JDatetime(JDate.valueOf(jdate.getYear(), jdate.getMonth(),jdate.getDayOfMonth()), hour, minutes, seconds, millis, TimeZone.getTimeZone("Europe/Madrid"));
    }

    private int getPreviousHour(int hour){
        int previousHour=hour-1;
        if(hour==0){
            previousHour=23;
        }
        return previousHour;
    }
}
