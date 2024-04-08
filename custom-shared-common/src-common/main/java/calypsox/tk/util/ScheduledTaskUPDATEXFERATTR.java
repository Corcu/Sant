package calypsox.tk.util;

import calypsox.repoccp.ReconCCPConstants;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.workflow.TradeWorkflow;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * @author aalonsop
 */
public class ScheduledTaskUPDATEXFERATTR extends ScheduledTask {

    private final String filePathAttr="File Path";
    private final String fileNameAttr="File Name";
    private final String overrideAttrWithEmptyValue="Allow empty attribute values";
    private final DataFormatter dataFormatter = new DataFormatter();

    static String xferIdStr="TransferId";

    @Override
    public String getTaskInformation() {
        return "Updates defined transfer's attributes. " +
                "Input file must be an .xlsx file, with one mandatory TransferId column and then additional columns per attribute to update.";
    }

    @Override
    protected boolean process(DSConnection ds, PSConnection ps) {
        String filePath=getAttribute(filePathAttr);
        String fileName=getAttribute(fileNameAttr);
        List<XferBean> mappedLines=read(filePath+fileName);
        updateXferAttributes(mappedLines);
        return super.process(ds,ps);
    }

    public List<XferBean> read(String filePath){

        List<XferBean> mappedLines = new ArrayList<>();
        try {
            FileInputStream file = new FileInputStream(filePath);
            Workbook workbook = new XSSFWorkbook(file);
            Sheet sheet = workbook.getSheetAt(0);
            Map<Integer, String> header = parseHeader(sheet);
            for (Row row : sheet) {
                XferBean xferBean=new XferBean();
                for(Integer columnIndex:header.keySet()){
                    if(row.getRowNum()!=0&&!Util.isEmpty(header.get(columnIndex))) {
                        String cellValue=Optional.ofNullable(row.getCell(columnIndex))
                                .map(dataFormatter::formatCellValue).orElse("");
                        xferBean.addAttribute(header.get(columnIndex), cellValue);
                    }
                }
                mappedLines.add(xferBean);
            }
        } catch (IOException exc) {
            Log.error(this,exc);
        }
        return mappedLines;
    }

    private Map<Integer, String> parseHeader(Sheet sheet) {
        Map<Integer, String> header = new HashMap<>();
        for (Cell cell : sheet.getRow(0)) {
            header.put(cell.getColumnIndex(), dataFormatter.formatCellValue(cell));
        }
        return header;
    }

    private void updateXferAttributes(List<XferBean> mappedLines){
        boolean allowEmptyValues=getBooleanAttribute(overrideAttrWithEmptyValue,false);
        int xfersSize= mappedLines.size()-1;
        Log.system(ScheduledTask.LOG_CATEGORY,"Xfer initial size: "+xfersSize);
        int updatedXfers=0;
        for (XferBean mappedLine : mappedLines) {
            try {
                long xferId=mappedLine.getTransferId();
                if(xferId>0){
                    for(String attrName : mappedLine.attributes.keySet()) {
                        String attrValue=mappedLine.attributes.get(attrName);
                        if(!attrName.equals(xferIdStr)&&isValueAccepted(attrValue,allowEmptyValues)) {
                            if(isBuyerSellerReference(attrName)){
                                updateTradeKwd(xferId,attrName,attrValue);
                            }else {
                                DSConnection.getDefault().getRemoteBO().saveTransferAttribute(xferId, attrName, attrValue);
                                Log.system(ScheduledTask.LOG_CATEGORY, "Transfer Attribute Updated -> Id: " + xferId + "  Name: " + attrName + "  Value: " + attrValue);
                            }
                        }
                    }
                    updatedXfers++;
                }
            } catch (CalypsoServiceException e) {
                Log.error(ScheduledTask.LOG_CATEGORY, "Could not update xfer attribute: " + mappedLine.getTransferId());
            }
        }
        Log.system(ScheduledTask.LOG_CATEGORY,"Processed "+updatedXfers+" of "+xfersSize+" xfers");
    }

    private void updateTradeKwd(long xferId,String kwdName, String kwdValue){
        try {
            BOTransfer xfer = DSConnection.getDefault().getRemoteBO().getBOTransfer(xferId);
            long tradeId=Optional.ofNullable(xfer).map(BOTransfer::getTradeLongId).orElse(0L);
            if(tradeId>0L){
                Action tradeAction= Action.UPDATE;
                Trade trade=DSConnection.getDefault().getRemoteTrade().getTrade(tradeId);
                if(trade!=null&& TradeWorkflow.isTradeActionApplicable(trade,tradeAction,DSConnection.getDefault(),null)){
                    trade.setAction(tradeAction);
                    trade.addKeyword(kwdName,kwdValue);
                    DSConnection.getDefault().getRemoteTrade().save(trade);
                    Log.system(ScheduledTask.LOG_CATEGORY, "Trade KWD Updated -> Id: " + tradeId + "  Name: " + kwdName + "  Value: " + kwdValue);
                }
            }
        }catch(CalypsoServiceException exc){
            Log.error(this,exc.getCause());
        }

    }

    private boolean isValueAccepted(String attrValue, boolean allowEmptyValues){
        return !Util.isEmpty(attrValue) || allowEmptyValues;
    }

    private boolean isBuyerSellerReference(String attrName){
        return ReconCCPConstants.TRADE_KWD_BUYER_SELLER_REF.equals(attrName);
    }
    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<>();
        attributeList.add(attribute(fileNameAttr));
        attributeList.add(attribute(filePathAttr));
        attributeList.add(attribute(overrideAttrWithEmptyValue));
        return attributeList;
    }

    private static class XferBean{
        Map<String,String> attributes;

        XferBean(){
        }

        long getTransferId(){
            long xferId=0;
            if(attributes!=null){
                String attrStr=attributes.get(xferIdStr);
                try {
                    xferId = Long.parseLong(attrStr);
                }catch(NumberFormatException exc){
                    Log.error(this,exc.getCause());
                }
            }
            return xferId;
        }
        void addAttribute(String attrName, String attrValue){
            if(this.attributes==null){
                this.attributes=new HashMap<>();
            }
            this.attributes.put(attrName,attrValue);
        }
    }
}
