package calypsox.tk.util;

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
public class ScheduledTaskUPDATETRADEKWD extends ScheduledTask {

    static String tradeIdStr = "TradeId";
    private final String filePathAttr = "File Path";
    private final String fileNameAttr = "File Name";
    private final String overrideAttrWithEmptyValue = "Allow empty kwd values";
    private final DataFormatter dataFormatter = new DataFormatter();

    @Override
    public String getTaskInformation() {
        return "Updates defined trade keywords. " +
                "Input file must be an .xlsx file, with one mandatory TradeId column and then additional columns per kwd to update.";
    }

    @Override
    protected boolean process(DSConnection ds, PSConnection ps) {
        String filePath = getAttribute(filePathAttr);
        String fileName = getAttribute(fileNameAttr);
        List<TradeKwdBean> mappedLines = read(filePath + fileName);
        updateXferAttributes(mappedLines);
        return super.process(ds, ps);
    }

    public List<TradeKwdBean> read(String filePath) {

        List<TradeKwdBean> mappedLines = new ArrayList<>();
        try {
            FileInputStream file = new FileInputStream(filePath);
            Workbook workbook = new XSSFWorkbook(file);
            Sheet sheet = workbook.getSheetAt(0);
            Map<Integer, String> header = parseHeader(sheet);
            for (Row row : sheet) {
                TradeKwdBean TradeKwdBean = new TradeKwdBean();
                for (Integer columnIndex : header.keySet()) {
                    if (row.getRowNum() != 0 && !Util.isEmpty(header.get(columnIndex))) {
                        String cellValue = Optional.ofNullable(row.getCell(columnIndex))
                                .map(dataFormatter::formatCellValue).orElse("");
                        TradeKwdBean.addAttribute(header.get(columnIndex), cellValue);
                    }
                }
                mappedLines.add(TradeKwdBean);
            }
        } catch (IOException exc) {
            Log.error(this, exc);
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

    private void updateXferAttributes(List<TradeKwdBean> mappedLines) {
        boolean allowEmptyValues = getBooleanAttribute(overrideAttrWithEmptyValue, false);
        int tradesSize = mappedLines.size() - 1;
        Log.system(ScheduledTask.LOG_CATEGORY, "Trades initial size: " + tradesSize);
        int updatedTrades = 0;
        for (TradeKwdBean mappedLine : mappedLines) {
            try {
                long tradeId = mappedLine.getTradeId();
                if (tradeId > 0) {
                    Trade trade = DSConnection.getDefault().getRemoteTrade().getTrade(tradeId);
                    for (String attrName : mappedLine.attributes.keySet()) {
                        String attrValue = mappedLine.attributes.get(attrName);
                        if (!attrName.equals(tradeIdStr) && isValueAccepted(attrValue, allowEmptyValues)) {
                            trade.addKeyword(attrName, attrValue);
                        }
                    }
                    updateTrade(trade);
                    updatedTrades++;
                }
            } catch (CalypsoServiceException e) {
                Log.error(ScheduledTask.LOG_CATEGORY, "Could not update xfer attribute: " + mappedLine.getTradeId());
            }
        }
        Log.system(ScheduledTask.LOG_CATEGORY, "Processed " + updatedTrades + " of " + tradesSize + " trades");
    }

    private void updateTrade(Trade trade) throws CalypsoServiceException {
        Action tradeAction = Action.UPDATE;
        if (trade != null && TradeWorkflow.isTradeActionApplicable(trade, tradeAction, DSConnection.getDefault(), null)) {
            trade.setAction(tradeAction);
            DSConnection.getDefault().getRemoteTrade().save(trade);
            Log.system(ScheduledTask.LOG_CATEGORY, "Trade KWDs Updated -> Id: " + trade.getLongId());
        }
    }

        private boolean isValueAccepted (String attrValue,boolean allowEmptyValues){
            return !Util.isEmpty(attrValue) || allowEmptyValues;
        }

        @Override
        protected List<AttributeDefinition> buildAttributeDefinition () {
            List<AttributeDefinition> attributeList = new ArrayList<>();
            attributeList.add(attribute(fileNameAttr));
            attributeList.add(attribute(filePathAttr));
            attributeList.add(attribute(overrideAttrWithEmptyValue));
            return attributeList;
        }

        private static class TradeKwdBean {
            Map<String, String> attributes;

            TradeKwdBean() {
            }

            long getTradeId() {
                long xferId = 0;
                if (attributes != null) {
                    String attrStr = attributes.get(tradeIdStr);
                    try {
                        xferId = Long.parseLong(attrStr);
                    } catch (NumberFormatException exc) {
                        Log.error(this, exc.getCause());
                    }
                }
                return xferId;
            }

            void addAttribute(String attrName, String attrValue) {
                if (this.attributes == null) {
                    this.attributes = new HashMap<>();
                }
                this.attributes.put(attrName, attrValue);
            }
        }
    }
