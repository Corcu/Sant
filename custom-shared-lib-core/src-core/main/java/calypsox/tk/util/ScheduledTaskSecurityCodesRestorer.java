package calypsox.tk.util;

import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class ScheduledTaskSecurityCodesRestorer extends ScheduledTask {

    private static final String WHERE = "ENTITY_CLASS_NAME='Bond' AND MODIF_DATE BETWEEN TO_DATE ('?1', 'yyyy/MM/dd') AND TO_DATE ('?2', 'yyyy/MM/dd') AND ENTITY_FIELD_NAME LIKE 'DELCODE%'";

    private static final String LOG_CAT = "com.calypso.SYSTEM.ScheduledTask";
    private static final long serialVersionUID = 1241479532468867588L;

    @Override
    public String getTaskInformation() {
        return "Recovers deleted Bond's/Equity codes from BOAudit. The process tries to restore all the codes deleted at the given valuation date";
    }

    private String buildWhere() {
        String fullWhere = WHERE;
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd");
        JDatetime dayBeforeValDate = this.getValuationDatetime().add(-1, 0, 0, 0, 0);
        JDatetime dayAfterValDate = this.getValuationDatetime().add(1, 0, 0, 0, 0);
        fullWhere = fullWhere.replace("?1", dateFormatter.format(dayBeforeValDate));
        fullWhere = fullWhere.replace("?2", dateFormatter.format(dayAfterValDate));
        return fullWhere;
    }

    @Override
    public boolean process(DSConnection ds, PSConnection ps) {
        try {
            Log.warn(LOG_CAT, "Start Timestamp: " + System.currentTimeMillis());
            Vector<AuditValue> audits = ds.getRemoteTrade().getAudit(buildWhere(), null, null);
            Set<Integer> set = new HashSet();
            int counterA = 0;
            for (AuditValue auditValue : audits) {
                set.add(auditValue.getEntityId());
                counterA++;
            }
            Log.warn(LOG_CAT, counterA + " auditValues Retrieved!!! ");
            System.out.println(counterA + " auditValues Retrieved!!! ");
            Map<Integer, ? extends Product> bonds = ds.getRemoteProduct().getProducts(set);
            Log.warn(LOG_CAT, "Found " + bonds.size() + " bonds to process!!!");
            System.out.println("Found " + bonds.size() + " bonds to process!!!");
            for (AuditValue auditValue : audits) {
                Product bondOrEquity = bonds.get(auditValue.getEntityId());
                String codeName = getCodeName(auditValue);
                if (Util.isEmpty(bondOrEquity.getSecCode(codeName))) {
                    bondOrEquity.setSecCode(codeName, auditValue.getField().getOldValue());
                }
            }
            counterA = 0;
            for (Product bond : bonds.values()) {
                if (bond.getId() != 18218 && bond.getId() != 30214151) {
                    ds.getRemoteProduct().saveProduct(bond);
                    Log.warn(LOG_CAT, "Saved bond with ISIN: " + bond.getSecCode("ISIN"));
                    System.out.println("Saved bond with ISIN: " + bond.getSecCode("ISIN"));
                    counterA++;
                }
            }
            Log.warn(LOG_CAT, "A total of " + counterA + "bonds were saved!!!");
        } catch (Exception exc) {
            Log.error(this, "Error", exc.getCause());
        }
        return true;
    }

    private String getCodeName(AuditValue auditValue) {
        String res = "";
        String fieldName = auditValue.getFieldName();
        if (!Util.isEmpty(fieldName)) {
            String[] array = fieldName.split("#");
            if (array != null && array.length > 1) {
                res = array[1];
            }
        }
        Log.warn(LOG_CAT, "End Timestamp: " + System.currentTimeMillis());
        return res;
    }
}
