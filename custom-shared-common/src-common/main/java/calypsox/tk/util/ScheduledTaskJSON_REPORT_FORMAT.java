package calypsox.tk.util;

import calypsox.tk.util.json.JSONReportExporter;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.util.DisplayInBrowser;
import com.calypso.tk.util.ScheduledTaskREPORT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;

/**
 * @author acd
 */
public class ScheduledTaskJSON_REPORT_FORMAT extends ScheduledTaskREPORT {

    protected static final String  JSON_FORMAT = "json";
    protected static final String  REPORT_NAME= "Calypso Report Name";
    protected static final String  ALIAS_IDENTIFIER= "Alias Identifier";
    protected static final String  PRETTY_PRINTER= "Pretty Printer";
    protected static final String  EOM= "Eom Tag Value";

    @Override
    protected String saveReportOutput(ReportOutput output, String format, String type, String[] fileNames, StringBuffer sb) {
        if(JSON_FORMAT.equalsIgnoreCase(format)){
            return doJsonExport(output,fileNames,type,sb);
        }else {
            return super.saveReportOutput(output, format, type, fileNames, sb);
        }
    }

    /**
     * @param output
     * @param fileNames
     * @param type
     * @param sb
     * @return
     */
    protected String doJsonExport(ReportOutput output, String[] fileNames,String type,StringBuffer sb){
        JSONReportExporter exporter = new JSONReportExporter();
        DefaultReportOutput defaultReportOutput = Optional.ofNullable(output).filter(DefaultReportOutput.class::isInstance).map(DefaultReportOutput.class::cast).orElse(new DefaultReportOutput(null));
        final ObjectNode jsonNodes = exporter.exportJsonReport(defaultReportOutput, Optional.ofNullable(getAttribute(REPORT_NAME)).orElse(""), Optional.ofNullable(getAttribute(ALIAS_IDENTIFIER)).orElse(""));
        String reportText = getJsonAsString(jsonNodes);
        saveReportInFile(reportText,fileNames,type,sb);
        return reportText;
    }

    /**
     * @param report
     * @param fileNames
     * @param type
     * @param sb
     */
    private void saveReportInFile(String report, String[] fileNames, String type, StringBuffer sb){
        try{
            if(!Util.isEmpty(report)){
                TimeZone.setDefault(this.getTimeZone());

                this.setFileName(DisplayInBrowser.buildDocument(report, JSON_FORMAT, fileNames[0], false, 1));
                String fileName = this.getFileName();
                if (fileName.startsWith("file://")) {
                    fileName = fileName.substring(7);
                }
                sb.append(type + " report saved in " + fileName + "\n");
                for(int i = 1; i < fileNames.length; ++i) {
                    Util.copyFile(fileName, fileNames[i]+"."+JSON_FORMAT);
                    sb.append(type).append(" report copy saved in ").append(fileNames[i]).append("\n");
                }
            }
        }catch (Exception e){
            Log.error(this,"Error saving document: " + e);
        }
    }

    @Override
    public Vector getAttributeDomain(final String attribute, final Hashtable hashtable) {
        Vector vector;
        if (attribute.equals(REPORT_FORMAT)) {
            vector = super.getAttributeDomain(attribute, hashtable);
            vector.addElement(JSON_FORMAT);
        }else if(attribute.equals(PRETTY_PRINTER)){
            vector = new Vector();
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
        result.add(REPORT_NAME);
        result.add(ALIAS_IDENTIFIER);
        result.add(PRETTY_PRINTER);
        result.add(EOM);
        return result;
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<>(super.buildAttributeDefinition());
        attributeList.add(attribute(REPORT_NAME).description("Set Array Name for Json File"));
        attributeList.add(attribute(ALIAS_IDENTIFIER).description("Alias Identifier"));
        attributeList.add(attribute(PRETTY_PRINTER).description("Pretty Format"));
        attributeList.add(attribute(EOM).description("End Of Month attribute"));
        return attributeList;
    }

    /**
     * @param reportRowsNode
     * @return
     */
    protected String getJsonAsString(ObjectNode reportRowsNode){
        final boolean isPretty = Optional.ofNullable(getAttribute(PRETTY_PRINTER)).filter("true"::equalsIgnoreCase).isPresent();
        ObjectMapper mapper = new ObjectMapper();
        try {
            if(isPretty){
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(reportRowsNode);
            }else{
                return mapper.writeValueAsString(reportRowsNode);
            }
        } catch (JsonProcessingException e) {
            Log.error(this,e);
        }
        return "";
    }
}
