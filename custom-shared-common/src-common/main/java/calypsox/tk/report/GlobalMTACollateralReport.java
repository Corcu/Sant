package calypsox.tk.report;

import calypsox.tk.report.globalmta.CollateralConfigMTAGroup;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.refdata.sql.CollateralConfigFilter;
import com.calypso.tk.report.CollateralConfigReport;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * @author aalonsop
 */
public class GlobalMTACollateralReport extends CollateralConfigReport {


    private final Map<Integer,String> legalEntityLEICache=new HashMap<>();
    @Override
    public ReportOutput load(Vector errorMsgs) {
        DefaultReportOutput output = new DefaultReportOutput(this);
        List<CollateralConfig> configs = this.loadMarginCallConfigs(this.getReportTemplate());
        if (!Util.isEmpty(configs)) {
            Map<String, ReportRow> groupedRows=new HashMap<>();
            for(CollateralConfig cc:configs) {
                createEnrichRow(cc,groupedRows);
            }
            output.setRows(groupedRows.values().toArray(new ReportRow[0]));
        }
        return output;
    }

    protected void createEnrichRow(CollateralConfig cc, Map<String, ReportRow> groupedRows){
        String lei=getLEIAttribute(cc.getLeId());
        if(!Util.isEmpty(lei)) {
            ReportRow reportRow = Optional.ofNullable(groupedRows.get(lei)).orElse(new ReportRow(new CollateralConfigMTAGroup(this.getValDate())));
            reportRow.setProperty("MarginCallConfig", cc);
            ((CollateralConfigMTAGroup) reportRow.getProperty(CollateralConfigMTAGroup.class.getSimpleName())).addConfigToGroup(cc);
            groupedRows.put(lei, reportRow);
        }
    }

    private String getLEIAttribute(int leId){
        String lei=this.legalEntityLEICache.get(leId);
        if(Util.isEmpty(lei)) {
            List<LegalEntityAttribute> attrs = BOCache.getLegalEntityAttributes(DSConnection.getDefault(), leId);
            lei=Optional.ofNullable(attrs).map(attributes->findLeiFromAttrs(attributes,leId)).orElse("");
        }
        return lei;
    }

    private String findLeiFromAttrs(List<LegalEntityAttribute> attrs,int leId){
        String lei="";
        for(LegalEntityAttribute attribute:attrs){
            if("LEI".equals(attribute.getAttributeType())){
                lei=attribute.getAttributeValue();
                this.legalEntityLEICache.put(leId,lei);
                break;
            }
        }
        return lei;
    }
    @Override
    protected List<CollateralConfig> loadMarginCallConfigs(ReportTemplate template) {
        List<CollateralConfig> collateralConfigs=new ArrayList<>();
        CollateralConfigFilter filter=new CollateralConfigFilter();
        List<Integer> poIds = Optional.ofNullable(template.get("PROCESSING_ORG_IDS"))
                .map(ids->new ArrayList((Vector)ids)).orElse(new ArrayList<>());
        List<Integer> leIds =  Optional.ofNullable(template.get("LEGAL_ENTITY_IDS"))
                .map(ids->new ArrayList((Vector) ids)).orElse(new ArrayList<>());
        List<String> types = parseStringToCollection("MARGIN_CALL_CONFIG_TYPE",template);
        List<String> statuses = parseStringToCollection("ENTRY_STATUS",template);

        filter.setMccIds(Util.string2IntVector(template.get("MARGIN_CALL_CONFIG_IDS")));
        filter.setPoIds(poIds);
        filter.setLeIds(leIds);
        filter.setContractTypes(types);
        filter.setStatuses(statuses);
        filter.setProcessingDate(this.getValDate());


        try {
            collateralConfigs= ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfigByFilter(filter,false);
        } catch (CollateralServiceException exc) {
            Log.error(this.getClass().getSimpleName(),exc.getMessage(),exc.getCause());
        }
        return filterCollateralConfigsByPOAndAFFlag(collateralConfigs,poIds);
    }

    /**
     * This method is created to avoid a CollateralConfigFilter bug where PO filtering is not working. Also contains a
     * filter by contract's additional field and CSA VM and CSD PO filtering,
     * @return Contracts filtered by PO.
     */
    private List<CollateralConfig> filterCollateralConfigsByPOAndAFFlag(List<CollateralConfig> configs,List<Integer> poIds){
        return configs.parallelStream().filter(collateralConfig -> poIds.isEmpty()||poIds.contains(collateralConfig.getPoId()))
                    .filter(collateralConfig -> !Boolean.parseBoolean(collateralConfig.getAdditionalField("EXCLUDE_FROM_GLOBALMTA")))
                    .filter(collateralConfig -> isCSAAccepted(collateralConfig)||isCSDAccepted(collateralConfig))
                    .collect(Collectors.toList());
    }
    private List<String> parseStringToCollection(String attrName,ReportTemplate template){
        String s = template.get(attrName);
        List<String> stringToList=new ArrayList<>();
        if (!Util.isEmpty(s)) {
            stringToList = Util.stringToCollection(stringToList, s, ",", false);
        }
        return stringToList;
    }

    private boolean isCSDAccepted(CollateralConfig collateralConfig){
        return "CSD".equals(collateralConfig.getContractType())
                &&collateralConfig.getName().contains("(PO)");
    }

    private boolean isCSAAccepted(CollateralConfig collateralConfig){
        return ("CSA".equals(collateralConfig.getContractType())
                &&(collateralConfig.getName().contains("VM)")||collateralConfig.getName().startsWith("VM-")));
    }
}
