package calypsox.tk.report;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.core.Log;
import com.calypso.tk.report.BondReportStyle;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.report.TransferReportStyle;
import com.calypso.tk.report.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Vector;

public class BODisponibleAuditReportStyle extends AuditReportStyle {
    public static final String FAILED_GENERIC_COMMENT = "Transfer.Comment.Fallidas";
    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {
        String[] reportStyleNames = {"Transfer", "Trade", "Bond", "LatestGenericComment"};

        return Arrays.stream(reportStyleNames)
                .map(ReportStyle::getReportStyle)
                .filter(Objects::nonNull)
                .map(ReportStyle.class::cast)
                .map(style -> style.getColumnValue(row, columnId, errors))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(super.getColumnValue(row, columnId, errors));
    }

    /**
     * @deprecated
     */
    @Override
    public TreeList getTreeList() {
        TreeList treeList = super.getTreeList();
        treeList.add(getFieldsAsTreeList(TradeReportStyle.class, "Trade"));
        TreeList transfer = getFieldsAsTreeList(TransferReportStyle.class, "Transfer");
        transfer.add(FAILED_GENERIC_COMMENT);
        treeList.add(transfer);
        treeList.add(getFieldsAsTreeList(BondReportStyle.class, "Bond"));
        return treeList;
    }

    private TreeList getFieldsAsTreeList(Class<?> clazz, String treeListName) {
        TreeList treeList = new TreeList(treeListName);
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                try {
                    Object value = field.get(clazz);
                    if (value instanceof String) {
                        treeList.add(String.valueOf(value));
                    }
                } catch (IllegalAccessException e) {
                    Log.error(this, "Error: " + e.getMessage());
                }
            }
        }
        return treeList;
    }
}
