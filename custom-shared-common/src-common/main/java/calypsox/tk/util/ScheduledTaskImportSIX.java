package calypsox.tk.util;

import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.product.Security;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.ScheduledTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author aalonsop
 */
public class ScheduledTaskImportSIX extends ScheduledTask {

    private final String filePathAttr="File Path";
    private final String fileNameAttr="File Name";

    private final String isinStr="ISIN";

    @Override
    public String getTaskInformation() {
        return "Imports SIX file and updates related security attributes";
    }

    @Override
    protected boolean process(DSConnection ds, PSConnection ps) {
        String filePath=getAttribute(filePathAttr);
        String fileName=getAttribute(fileNameAttr);
        List<ScheduledTaskImportSIX.SIXBean> mappedLines=read(filePath+fileName);
        updateSecurities(mappedLines);
        return super.process(ds,ps);
    }

    public List<ScheduledTaskImportSIX.SIXBean> read(String filePath){
        Path path = Paths.get(filePath);

        List<ScheduledTaskImportSIX.SIXBean> mappedLines = new ArrayList<>();
        try {
            mappedLines = Files.lines(path).map(this::mapLine)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException exc) {
            Log.error(this,exc);
        }
        return mappedLines;
    }

    private ScheduledTaskImportSIX.SIXBean mapLine(String line){
        ScheduledTaskImportSIX.SIXBean bb=null;
        if(!Util.isEmpty(line)){
            String[] columns=line.split(";",-1);
            if(columns.length==10){
                String identifierType=columns[0];
                String identifier=columns[1];
                String status=columns[4];
                String instStatus=columns[6];
                String csdrPenaltyRateIndicator=columns[7];
                String csdrSDRInScopeIndicator = columns[8];
                String csdrInScopeIndicator=columns[9];

                if(isinStr.equals(identifierType)&& !Util.isEmpty(identifier)){
                    bb= new ScheduledTaskImportSIX.SIXBean(identifierType,identifier,status,csdrPenaltyRateIndicator,csdrSDRInScopeIndicator,csdrInScopeIndicator, instStatus);
                }
            }
        }
        return bb;
    }

    private void updateSecurities(List<ScheduledTaskImportSIX.SIXBean> mappedLines){
        Log.system(ScheduledTask.LOG_CATEGORY,"SIX file initial size: "+mappedLines.size());
        int updatedSecurities=0;
        for (ScheduledTaskImportSIX.SIXBean mappedLine : mappedLines) {
            try {
                Vector<Product> products = DSConnection.getDefault().getRemoteProduct().getProductsByCode(isinStr,mappedLine.identifier);
                for(Product product:products) {
                    updateProduct(product,mappedLine);
                    updatedSecurities++;
                }
            } catch (CalypsoServiceException exc) {
                Log.error(ScheduledTask.LOG_CATEGORY, "Error while processing security: " + mappedLine.identifier,exc.getCause());
            } catch (NullPointerException exc){
                Log.warn(ScheduledTask.LOG_CATEGORY, "Error. Doesn't exist security: " + mappedLine.identifier,exc.getCause());
            }
        }
        Log.system(ScheduledTask.LOG_CATEGORY,"Processed "+updatedSecurities+" of "+mappedLines.size()+" securities");
    }

    private void updateProduct(Product product, SIXBean mappedLine) throws CalypsoServiceException {
        String eligibilitySecCode="CSDR_Eligibility";
        String penCatSecCode="CSDR_Penalty_Category";
        if (product instanceof Security) {
            if(mappedLine.isActive()) {
                if(mappedLine.isDelivered()&&mappedLine.isLiable()) {
                    product.setSecCode(eligibilitySecCode, "Y");
                    if (!Util.isEmpty(mappedLine.csdrPenaltyRateIndicator)) {
                        product.setSecCode(penCatSecCode, mappedLine.csdrPenaltyRateIndicator);
                    }
                    DSConnection.getDefault().getRemoteProduct().saveProduct(product);
                }
            }else{
                product.setSecCode(eligibilitySecCode,"");
                product.setSecCode(penCatSecCode, "");
                DSConnection.getDefault().getRemoteProduct().saveProduct(product);
            }
        } else {
            Log.system(ScheduledTask.LOG_CATEGORY, "Product " + mappedLine.identifier + " not found in Calypso or is not a Security");
        }
    }
    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<>();
        attributeList.add(attribute(fileNameAttr));
        attributeList.add(attribute(filePathAttr));
        return attributeList;
    }

    private static class SIXBean{

        String identifierType;
        String identifier;
        String status;
        String instrumentStatus;
        String csdrPenaltyRateIndicator;
        String csdrInScopeIndicator;
        String csdrSDRInScopeIndicator;

        SIXBean(String identifierType,String identifier,String status,String csdrPenaltyRateIndicator,String csdrSDRInScopeIndicator,String csdrInScopeIndicator,String instrumentStatus){
            this.identifierType=identifierType;
            this.identifier=identifier;
            this.status=status;
            this.csdrPenaltyRateIndicator=Optional.ofNullable(csdrPenaltyRateIndicator)
                    .filter(str->str.length()>3).map(str->str.substring(0,4)).orElse("");
            this.csdrInScopeIndicator= Optional.ofNullable(csdrInScopeIndicator)
                    .map(str->str.replaceAll("\\s+", "")).orElse("");
            this.csdrSDRInScopeIndicator=Optional.ofNullable(csdrSDRInScopeIndicator)
                    .map(str->str.replaceAll("\\s+", "")).orElse("");
            this.instrumentStatus=Optional.ofNullable(instrumentStatus)
                    .map(str->str.replaceAll("\\s+", "")).orElse("");
        }

        boolean isDelivered(){
            return "Delivered".equalsIgnoreCase(this.status);
        }

        boolean isActive(){
            List<String> activeValues = new ArrayList<>();
            activeValues.add("8:Active");
            activeValues.add("7:Inliquidation/dissolution");
            activeValues.add("9:Indefault");
            return checkDV(this.instrumentStatus,"CSDR_SIX_instrumentStatus",activeValues, true);
        }

        boolean isLiable(){
            List<String> liableValues= new ArrayList<>();
            liableValues.add("1:Liable/applicable");
            liableValues.add("2:Potentiallyliable/applicable");
            liableValues.add("12:Liable/applicable-confirmed");

            String scopeIndDv="CSDR_SIX_InScopeIndicator";
            return checkDV(this.csdrInScopeIndicator,scopeIndDv,liableValues)
                    || checkDV(this.csdrSDRInScopeIndicator,scopeIndDv,liableValues);
        }

        boolean checkDV(String targetValue,String dvName,List<String> defaultValues){
            return checkDV(targetValue,dvName,defaultValues,false);
        }

        boolean checkDV(String targetValue,String dvName,List<String> defaultValues, boolean allowEmpty){
            boolean res=false;
            List<String> dvValues=LocalCache.getDomainValues(DSConnection.getDefault(),dvName);
            if(Util.isEmpty(dvValues)) {
               dvValues=defaultValues;
            }
            if(allowEmpty){
                dvValues.add("");
            }

            for(String value:dvValues){
                if(Optional.ofNullable(value).map(str->str.replaceAll("\\s+",""))
                        .map(v->v.equalsIgnoreCase(targetValue)).orElse(false)){
                    res=true;
                    break;
                }
            }
            return res;
        }
    }
}
