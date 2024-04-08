package calypsox.tk.util;

import calypsox.tk.util.json.JSONReportExporter;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.DateUtil;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.mo.TradeFilterCriterion;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.DisplayInBrowser;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;

/**
 * @author acd
 */
public class ScheduledTaskTRIPARTY_PLEDGE_JSON_REPORT extends ScheduledTaskJSON_REPORT_FORMAT {

    private static final String  ACTIVATE_EOM = "Generate with EOM Tag";
    private static final String  EOM_ON_LAST_DAY_OF_MONTH = "Eom on LastDayOfMonth";
    private static final String  EOM_ON_INTRADAY = "EOM on Intraday";


    @Override
    protected String doJsonExport(ReportOutput output, String[] fileNames,String type,StringBuffer sb){
        final Vector agregoTrades = loadTradeFilter();

        final JDate valuationDate = getValuationDatetime().getJDate(TimeZone.getDefault());
        JSONReportExporter exporter = new JSONReportExporter();
        DefaultReportOutput defaultReportOutput = Optional.ofNullable(output).filter(DefaultReportOutput.class::isInstance).map(DefaultReportOutput.class::cast).orElse(new DefaultReportOutput(null));
        final String reportName = Optional.ofNullable(getAttribute(REPORT_NAME)).orElse(defaultReportOutput.getReport().getType());
        final String reportAlias = Optional.ofNullable(getAttribute(ALIAS_IDENTIFIER)).orElse("");
        final boolean isEOM = Optional.ofNullable(getAttribute(ACTIVATE_EOM)).filter("true"::equalsIgnoreCase).isPresent();
        final boolean eomOnLastDayofMonth = Optional.ofNullable(getAttribute(EOM_ON_LAST_DAY_OF_MONTH)).filter("true"::equalsIgnoreCase).isPresent();
        final boolean eomOnIntraday = Optional.ofNullable(getAttribute(EOM_ON_INTRADAY)).filter("true"::equalsIgnoreCase).isPresent();
        String eomTag = Optional.ofNullable(getAttribute(EOM)).orElse("Y");

         ObjectNode newFileJackson = exporter.exportJsonReport(defaultReportOutput, reportName, reportAlias);
         ObjectNode reversalFileJackson = newFileJackson.deepCopy();

        //Add EOM tag only on reversal file.
        if((eomOnLastDayofMonth && isLastWorkingDayOfMonth(valuationDate) && !isEOM) || eomOnIntraday){
            addEOMTag(reversalFileJackson,reportName,eomTag,agregoTrades);
        }

        //Add EOM tag on new file.
        if(isEOM){
            addEOMTag(newFileJackson,reportName,eomTag,agregoTrades);
        }

        saveFiles(newFileJackson,reversalFileJackson,fileNames,type,sb);

        return "";
    }
    
    private Vector<?> loadTradeFilter(){
        try {
            final String tradeFilterName = getTradeFilter();
            final TradeFilter tradeFilterToLoad = BOCache.getTradeFilter(DSConnection.getDefault(), tradeFilterName);
            return Optional.ofNullable(tradeFilterToLoad).map(filter -> filter.getCriterion("TRADE_ID_LIST")).map(TradeFilterCriterion::getValues).orElse(new Vector<>());
        }catch (Exception e){
            Log.error(this,"Error loading filter");
        }
        return new Vector<>();
    }

    /**
     * @param newFile
     * @param reversalFile
     * @param fileNames
     * @param type
     * @param sb
     */
    private void saveFiles(ObjectNode newFile, ObjectNode reversalFile, String[] fileNames, String type,StringBuffer sb){
        try {
            String newFileName = "";
            String reversalFileName = "";

            if(fileNames.length>=1){
                newFileName = fileNames[0];
                reversalFileName = fileNames[1];
            }

            if(!Util.isEmpty(newFileName)){
                saveReportInFile(getJsonAsString(newFile),newFileName,type,sb);
            }
            if(!Util.isEmpty(reversalFileName)){
                saveReportInFile(getJsonAsString(reversalFile),reversalFileName,type,sb);
            }
        }catch (Exception e){
            Log.error(this,"Error saving Report: " + e);
        }
    }


    /**
     * Add eom tag on json rows
     *
     * @param jsonFile
     * @param reportName
     * @param eomTag
     */
    private void addEOMTag(ObjectNode jsonFile,String reportName,String eomTag,Vector agregoTrades){
        if(null!=jsonFile){
            final ArrayNode jsonRows = jsonFile.withArray(reportName);
            if(null!=jsonRows){
                for (Iterator it = jsonRows.elements(); it.hasNext(); ) {
                    ObjectNode objectNode = (ObjectNode)it.next();
                    if(!Util.isEmpty(agregoTrades)){
                        final String repoTradeid = objectNode.get("repoTradeid").asText();
                        if(agregoTrades.contains(repoTradeid)){
                            objectNode.put("eom",eomTag);
                        }
                    }else {
                        objectNode.put("eom",eomTag);
                    }
                }
            }
        }
    }

    /**
     * @param report
     * @param fileName
     * @param type
     * @param sb
     */
    private void saveReportInFile(String report, String fileName, String type, StringBuffer sb){
        try{
            if(!Util.isEmpty(report)){
                TimeZone.setDefault(this.getTimeZone());
                this.setFileName(DisplayInBrowser.buildDocument(report, JSON_FORMAT, fileName, false, 1));
                sb.append(type + " report saved in " + fileName + "\n");
            }
        }catch (Exception e){
            Log.error(this,"Error saving document: " + e);
        }
    }

    @Override
    public Vector getAttributeDomain(final String attribute, final Hashtable hashtable) {
        Vector vector;
       if(attribute.equals(ACTIVATE_EOM)){
            vector = new Vector();
            vector.add("true");
            vector.add("false");
        }else if(attribute.equals(EOM_ON_LAST_DAY_OF_MONTH)){
            vector = new Vector();
            vector.add("true");
            vector.add("false");
        }else if(attribute.equals(EOM_ON_INTRADAY)){
           vector = new Vector();
           vector.add("");
           vector.add("true");
           vector.add("false");
       }else {
            vector = super.getAttributeDomain(attribute, hashtable);
        }
        return vector;
    }

    @Override
    public Vector getDomainAttributes() {
        Vector result = super.getDomainAttributes();
        result.add(EOM_ON_LAST_DAY_OF_MONTH);
        result.add(ACTIVATE_EOM);
        result.add(EOM_ON_INTRADAY);
        return result;
    }


    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<>(super.buildAttributeDefinition());
        attributeList.add(attribute(ACTIVATE_EOM).description("Generate File with EOM Tag as json param"));
        attributeList.add(attribute(EOM_ON_LAST_DAY_OF_MONTH).description("Generate File with EOM Tag as json param on second file"));
        attributeList.add(attribute(EOM_ON_INTRADAY).description("Just for testing propouse"));
        return attributeList;
    }

    /**
     * Get firts woking date of a month.
     * Default Holidays "SYSTEM"
     * @param jdate
     * @return
     */
    private boolean isLastWorkingDayOfMonth(JDate jdate){
        Vector<String> holidays = getHolidays();
        if(Util.isEmpty(holidays)){
            holidays = new Vector<>();
            holidays.add("SYSTEM");
        }
        if(null!=jdate){
            return jdate.addBusinessDays(1, holidays).getMonth()>jdate.getMonth();
        }
        return false;
    }

    /**
     * Get firts woking date of a month.
     * Default Holidays "SYSTEM"
     * @param jdate
     * @return
     */
    public boolean isFirstWorkingDayOfMonth(JDate jdate){
        Vector<String> holidays = getHolidays();
        if(Util.isEmpty(holidays)){
            holidays.add("SYSTEM");
        }
        if(null!=jdate){
            final JDate prevBusDate = DateUtil.getPrevBusDate(jdate, holidays);
            return prevBusDate.getMonth()<jdate.getMonth();
        }
        return false;
    }
}
