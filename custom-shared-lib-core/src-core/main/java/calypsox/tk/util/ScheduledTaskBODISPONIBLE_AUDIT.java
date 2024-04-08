package calypsox.tk.util;

import com.calypso.apps.appkit.presentation.format.JDateFormat;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

public class ScheduledTaskBODISPONIBLE_AUDIT extends ScheduledTask {

    public static final String ISIN = "Isin";
    public static final String DELIMITER = "CSV Delimiter";

    public static final String FILE_PATH = "File Path";

    public static final String DATE_FORMAT = "Date Format";

    @Override
    public String getTaskInformation() {
        return "";
    }
    //Enjoy
    private static String SELECT = "select  trade.trade_status as \"TradeStatus\", trade.trade_date_time as \"Trade Date\", trade.settlement_date as \"Trade Settle Date\", bo_transfer.settle_date as \"Settle Date\", bo_transfer.booking_date as \"Booking Date\", product_sec_code.code_value as \"ISIN\", \n" +
            "        legal_entity.short_name as \"PO Agent\", acc_account.acc_account_name as \"GL Account\", bo_transfer.amount as \"Qty\", bo_transfer.transfer_status as \"Transfer Status\", bo_audit.entity_field_name as \"Field Name\", bo_audit.old_value as \"Old Value\", bo_audit.new_value as \"New Value\",\n" +
            "        bo_transfer.transfer_id as \"Transfer Id\", bo_transfer.product_type as \"Product Type\", book.book_name as \"Book Name\", bo_audit.user_name as \"User Name\"\n" +
            "from    product_sec_code, bo_transfer, book, bo_audit, trade, acc_account, legal_entity\n" +
            "where   product_sec_code.sec_code = 'ISIN'\n" +
            "and     product_sec_code.code_value = ?\n" +
            "and     bo_transfer.product_id = product_sec_code.product_id\n" +
            "and     bo_transfer.book_id = book.book_id\n" +
            "and     bo_audit.entity_id = bo_transfer.transfer_id\n" +
            "and     bo_audit.entity_class_name = 'BOTransfer'\n" +
            "and     bo_transfer.trade_id = trade.trade_id\n" +
            "and     bo_transfer.gl_account_id = acc_account.acc_account_id\n" +
            "and     bo_transfer.int_agent_le_id = legal_entity.legal_entity_id\n" +
            "and     bo_audit.entity_field_name not like 'MODATTR#%'\n" +
            "and     bo_audit.entity_field_name not like 'ADDATTR#%'\n" +
            "and     bo_audit.entity_field_name not like 'DELATTR#%'\n" +
            "UNION\n" +
            "select  trade.trade_status as \"TradeStatus\", trade.trade_date_time as \"Trade Date\", trade.settlement_date as \"Trade Settle Date\", bo_transfer.settle_date as \"Settle Date\", bo_transfer.booking_date as \"Booking Date\", product_sec_code.code_value as \"ISIN\", \n" +
            "        legal_entity.short_name as \"PO Agent\", acc_account.acc_account_name as \"GL Account\", bo_transfer.amount as \"Qty\", bo_transfer.transfer_status as \"Transfer Status\", bo_audit_hist.entity_field_name as \"Field Name\", bo_audit_hist.old_value as \"Old Value\", bo_audit_hist.new_value as \"New Value\",\n" +
            "        bo_transfer.transfer_id as \"Transfer Id\", bo_transfer.product_type as \"Product Type\", book.book_name as \"Book Name\", bo_audit_hist.user_name as \"User Name\"\n" +
            "from    product_sec_code, bo_transfer, book, bo_audit_hist, trade, acc_account, legal_entity\n" +
            "where   product_sec_code.sec_code = 'ISIN'\n" +
            "and     product_sec_code.code_value = ? \n" +
            "and     bo_transfer.product_id = product_sec_code.product_id\n" +
            "and     bo_transfer.book_id = book.book_id\n" +
            "and     bo_audit_hist.entity_id = bo_transfer.transfer_id\n" +
            "and     bo_audit_hist.entity_class_name = 'BOTransfer'\n" +
            "and     bo_transfer.trade_id = trade.trade_id\n" +
            "and     bo_audit_hist.entity_id = bo_transfer.transfer_id\n" +
            "and     bo_audit_hist.entity_class_name = 'BOTransfer'\n" +
            "and     bo_transfer.trade_id = trade.trade_id\n" +
            "and     bo_transfer.gl_account_id = acc_account.acc_account_id\n" +
            "and     bo_transfer.int_agent_le_id = legal_entity.legal_entity_id\n" +
            "and     bo_audit_hist.entity_field_name not like 'MODATTR#%'\n" +
            "and     bo_audit_hist.entity_field_name not like 'ADDATTR#%'\n" +
            "and     bo_audit_hist.entity_field_name not like 'DELATTR#%'\n" +
            "UNION\n" +
            "select  trade_hist.trade_status as \"TradeStatus\", trade_hist.trade_date_time as \"Trade Date\", trade_hist.settlement_date as \"Trade Settle Date\", bo_transfer.settle_date as \"Settle Date\", bo_transfer.booking_date as \"Booking Date\", product_sec_code.code_value as \"ISIN\", \n" +
            "        legal_entity.short_name as \"PO Agent\", acc_account.acc_account_name as \"GL Account\", bo_transfer.amount as \"Qty\", bo_transfer.transfer_status as \"Transfer Status\", bo_audit_hist.entity_field_name as \"Field Name\", bo_audit_hist.old_value as \"Old Value\", bo_audit_hist.new_value as \"New Value\",\n" +
            "        bo_transfer.transfer_id as \"Transfer Id\", bo_transfer.product_type as \"Product Type\", book.book_name as \"Book Name\", bo_audit_hist.user_name as \"User Name\"\n" +
            "from    product_sec_code, bo_transfer, book, bo_audit_hist, trade_hist, acc_account, legal_entity\n" +
            "where   product_sec_code.sec_code = 'ISIN'\n" +
            "and     product_sec_code.code_value = ?\n" +
            "and     bo_transfer.product_id = product_sec_code.product_id\n" +
            "and     bo_transfer.book_id = book.book_id\n" +
            "and     bo_audit_hist.entity_id = bo_transfer.transfer_id\n" +
            "and     bo_audit_hist.entity_class_name = 'BOTransfer'\n" +
            "and     bo_transfer.trade_id = trade_hist.trade_id\n" +
            "and     bo_audit_hist.entity_id = bo_transfer.transfer_id\n" +
            "and     bo_audit_hist.entity_class_name = 'BOTransfer'\n" +
            "and     bo_transfer.trade_id = trade_hist.trade_id\n" +
            "and     bo_transfer.gl_account_id = acc_account.acc_account_id\n" +
            "and     bo_transfer.int_agent_le_id = legal_entity.legal_entity_id\n" +
            "and     bo_audit_hist.entity_field_name not like 'MODATTR#%'\n" +
            "and     bo_audit_hist.entity_field_name not like 'ADDATTR#%'\n" +
            "and     bo_audit_hist.entity_field_name not like 'DELATTR#%'\n" +
            "UNION\n" +
            "select  trade_hist.trade_status as \"TradeStatus\", trade_hist.trade_date_time as \"Trade Date\", trade_hist.settlement_date as \"Trade Settle Date\", bo_transfer_hist.settle_date as \"Settle Date\", bo_transfer_hist.booking_date as \"Booking Date\", product_sec_code.code_value as \"ISIN\", \n" +
            "        legal_entity.short_name as \"PO Agent\", acc_account.acc_account_name as \"GL Account\", bo_transfer_hist.amount as \"Qty\", bo_transfer_hist.transfer_status as \"Transfer Status\", bo_audit_hist.entity_field_name as \"Field Name\", bo_audit_hist.old_value as \"Old Value\", bo_audit_hist.new_value as \"New Value\",\n" +
            "        bo_transfer_hist.transfer_id as \"Transfer Id\", bo_transfer_hist.product_type as \"Product Type\", book.book_name as \"Book Name\", bo_audit_hist.user_name as \"User Name\"\n" +
            "from    product_sec_code, bo_transfer_hist, book, bo_audit_hist, trade_hist, acc_account, legal_entity\n" +
            "where   product_sec_code.sec_code = 'ISIN'\n" +
            "and     product_sec_code.code_value = ?\n" +
            "and     bo_transfer_hist.product_id = product_sec_code.product_id\n" +
            "and     bo_transfer_hist.book_id = book.book_id\n" +
            "and     bo_audit_hist.entity_id = bo_transfer_hist.transfer_id\n" +
            "and     bo_audit_hist.entity_class_name = 'BOTransfer'\n" +
            "and     bo_transfer_hist.trade_id = trade_hist.trade_id\n" +
            "and     bo_audit_hist.entity_id = bo_transfer_hist.transfer_id\n" +
            "and     bo_audit_hist.entity_class_name = 'BOTransfer'\n" +
            "and     bo_transfer_hist.trade_id = trade_hist.trade_id\n" +
            "and     bo_transfer_hist.gl_account_id = acc_account.acc_account_id\n" +
            "and     bo_transfer_hist.int_agent_le_id = legal_entity.legal_entity_id\n" +
            "and     bo_audit_hist.entity_field_name not like 'MODATTR#%'\n" +
            "and     bo_audit_hist.entity_field_name not like 'ADDATTR#%'\n" +
            "and     bo_audit_hist.entity_field_name not like 'DELATTR#%'";
    @Override
    protected boolean process(DSConnection ds, PSConnection ps) {
        List<String> rowsToWrite = loadAudit(ds);
        return writeFile(rowsToWrite) && super.process(ds, ps);
    }

    /**
     *
     * Execute query to load all the audit including archive related with the ISIN.
     * @param ds @DSConnection
     * @return result
     */
    private List<String> loadAudit(DSConnection ds) {
        String isin = Optional.ofNullable(getAttribute(ISIN)).orElse("");
        String delimiter = Optional.ofNullable(getAttribute(DELIMITER)).orElse(";");
        List<String> result = new ArrayList<>();

        if (!Util.isEmpty(isin)) {
            try {
                Vector<?> rawResultSet = executeAuditQuery(ds, isin);
                processResultSet(rawResultSet, delimiter, result);
            } catch (CalypsoServiceException e) {
                Log.error(this.getClass().getSimpleName(), "Error: ", e);
            }
        }

        return result;
    }

    private Vector<?> executeAuditQuery(DSConnection ds, String isin) throws CalypsoServiceException {
        final List<CalypsoBindVariable> bindVariables = new ArrayList<>();
        bindVariables.add(new CalypsoBindVariable(12, isin));
        bindVariables.add(new CalypsoBindVariable(12, isin));
        bindVariables.add(new CalypsoBindVariable(12, isin));
        bindVariables.add(new CalypsoBindVariable(12, isin));
        return ds.getRemoteAccess().executeSelectSQL(SELECT, bindVariables);
    }

    private void processResultSet(Vector<?> rawResultSet, String delimiter, List<String> result) {
        if (!Util.isEmpty(rawResultSet) && rawResultSet.size() > 1) {
            rawResultSet.remove(1); // remove types
            rawResultSet.forEach(value -> {
                StringBuilder row = new StringBuilder();
                ((Vector<?>) value).forEach(columnValues -> row.append(columnValues).append(delimiter));
                row.replace(row.length() - 1, row.length(), "");
                result.add(row.toString());
            });
        }
    }
    private boolean writeFile(List<String> rowsToWrite) {
        String filePath = getFilePath();
        if (!Util.isEmpty(filePath)) {
            Path path = Paths.get(filePath);
            try {
                Files.write(path, rowsToWrite);
                return true;
            } catch (IOException e) {
                Log.error(this.getClass().getSimpleName(), "Error writing the file:", e);
            }
        }
        return false;
    }

    private String getFilePath() {
        String isin = Optional.ofNullable(getAttribute(ISIN)).orElse("");
        String path = Optional.ofNullable(getAttribute(FILE_PATH)).orElse("");
        String dateFormat = Optional.ofNullable(getAttribute(DATE_FORMAT)).orElse("ddMMyyyy");
        JDateFormat jdDateFormat = new JDateFormat(dateFormat);

        if (!Util.isEmpty(path)) {
            path = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
            return Paths.get(path, isin + "_" + jdDateFormat.format(JDate.getNow()) + ".csv").toString();
        }

        return "";
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<>(super.buildAttributeDefinition());
        attributeList.add(attribute(ISIN).description("Isn to load (only one)"));
        attributeList.add(attribute(FILE_PATH).description("Only File path, dont include name or format"));
        attributeList.add(attribute(DELIMITER).description("File delimiter"));
        attributeList.add(attribute(DATE_FORMAT).description(DATE_FORMAT));
        return attributeList;
    }

}
