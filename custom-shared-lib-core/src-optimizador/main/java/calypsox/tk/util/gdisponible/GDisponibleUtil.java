/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.util.gdisponible;

import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.InventorySecurityPositionArray;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

public class GDisponibleUtil {

    public static final String BSTE_PO = "BSTE";

    public static final String SYSTEM_CAL = "SYSTEM";

    private static final double DELTA_POS = 0.0000001;

    // Reference used by messages sent by ScheduledTask SANT_GD_MATURE_SEC_POS
    public static String SANT_GD_MATURE_SEC_POS_REFERENCE = "SANT_GD_MATURE_SEC_POS";
    // Trade Keyword
    public static String KWD_IMPORT_SOURCE = "IMPORT_SOURCE";

    /**
     * Build all security positions by SantGDInvSecPosKey and by date
     */
    public static HashMap<JDate, HashMap<SantGDInvSecPosKey, InventorySecurityPosition>> buildSecurityPositionsNbDays(
            int processingOrgId, List<Integer> securityIds,
            List<String> productFamily, List<String> positionType,
            JDatetime valDatetime, int nbDays) {
        HashMap<SantGDInvSecPosKey, InventorySecurityPosition> currentInvSecPositions = new HashMap<SantGDInvSecPosKey, InventorySecurityPosition>();
        HashMap<JDate, HashMap<SantGDInvSecPosKey, InventorySecurityPosition>> datesInvSecPositions = new HashMap<JDate, HashMap<SantGDInvSecPosKey, InventorySecurityPosition>>();
        JDate valDate = valDatetime.getJDate(TimeZone.getDefault());
        InventorySecurityPositionArray invSecPosArray = getInvSecPosition(
                processingOrgId, securityIds, productFamily, positionType,
                valDatetime, nbDays);
        if (invSecPosArray == null) {
            return null;
        }
        InventorySecurityPosition invSecPos = null;
        for (int i = 0; i < invSecPosArray.size(); i++) {
            invSecPos = invSecPosArray.get(i);
            if (invSecPos == null) {
                continue;
            }
            SantGDInvSecPosKey invSecPosKey = new SantGDInvSecPosKey(
                    invSecPos.getSecurityId(), invSecPos.getPositionType(),
                    invSecPos.getBookId(), invSecPos.getAgentId(),
                    invSecPos.getAccountId());

            if (currentInvSecPositions.get(invSecPosKey) == null) {
                currentInvSecPositions.put(invSecPosKey, invSecPos);
            } else {
                InventorySecurityPosition oldInvSecPos = currentInvSecPositions
                        .get(invSecPosKey);
                oldInvSecPos.setTotalSecurity(oldInvSecPos.getTotalSecurity()
                        + invSecPos.getDailySecurity());
                oldInvSecPos.setTotalPledgedOut(oldInvSecPos
                        .getTotalPledgedOut() + invSecPos.getDailyPledgedOut());
            }

            try {
                InventorySecurityPosition clonedInvSecPos = (InventorySecurityPosition) currentInvSecPositions
                        .get(invSecPosKey).clone();

                JDate workingDate = invSecPos.getPositionDate().lte(valDate) ? valDate
                        : invSecPos.getPositionDate();
                if (datesInvSecPositions.get(workingDate) == null) {
                    HashMap<SantGDInvSecPosKey, InventorySecurityPosition> newInvSecPositions = new HashMap<SantGDInvSecPosKey, InventorySecurityPosition>();
                    newInvSecPositions.put(invSecPosKey, clonedInvSecPos);
                    datesInvSecPositions.put(workingDate, newInvSecPositions);
                } else {
                    datesInvSecPositions.get(workingDate).put(invSecPosKey,
                            clonedInvSecPos);
                }
            } catch (CloneNotSupportedException e) {
                Log.error(GDisponibleUtil.class.getName(), e);
            }
        }
        return datesInvSecPositions;
    }

    /**
     * Build all security positions by SantGDInvSecPosKey and by date
     */
    public static HashMap<JDate, HashMap<SantGDBookInvSecPosKey, InventorySecurityPosition>> buildSecurityBookPositionsNbDays(List<Integer> securityIds, List<Integer> bookIds,
                                                                                                                              List<String> productFamily, List<String> positionType,
                                                                                                                              JDatetime valDatetime, int nbDays) {
        HashMap<SantGDBookInvSecPosKey, InventorySecurityPosition> currentInvSecPositions = new HashMap<SantGDBookInvSecPosKey, InventorySecurityPosition>();
        HashMap<JDate, HashMap<SantGDBookInvSecPosKey, InventorySecurityPosition>> datesInvSecPositions = new HashMap<JDate, HashMap<SantGDBookInvSecPosKey, InventorySecurityPosition>>();
        JDate valDate = valDatetime.getJDate(TimeZone.getDefault());
        InventorySecurityPositionArray invSecPosArray = getInvSecBookPositions(
                securityIds, bookIds, productFamily, positionType,
                valDatetime, nbDays);
        if (invSecPosArray == null) {
            return null;
        }
        InventorySecurityPosition invSecPos = null;
        for (int i = 0; i < invSecPosArray.size(); i++) {
            invSecPos = invSecPosArray.get(i);
            if (invSecPos == null) {
                continue;
            }
            SantGDBookInvSecPosKey invSecPosKey = new SantGDBookInvSecPosKey(
                    invSecPos.getSecurityId(), invSecPos.getPositionType(),
                    invSecPos.getBookId());

            if (currentInvSecPositions.get(invSecPosKey) == null) {
                currentInvSecPositions.put(invSecPosKey, invSecPos);
            } else {
                InventorySecurityPosition oldInvSecPos = currentInvSecPositions
                        .get(invSecPosKey);
                oldInvSecPos.setDailySecurity(invSecPos.getDailySecurity());
                oldInvSecPos.setTotalSecurity(oldInvSecPos.getTotalSecurity()
                        + invSecPos.getDailySecurity());
                oldInvSecPos.setTotalPledgedOut(oldInvSecPos
                        .getTotalPledgedOut() + invSecPos.getDailyPledgedOut());
                oldInvSecPos.setTotalUnavailable(oldInvSecPos
                        .getTotalUnavailable()
                        + invSecPos.getDailyUnavailable());
            }

            try {
                InventorySecurityPosition clonedInvSecPos = (InventorySecurityPosition) currentInvSecPositions
                        .get(invSecPosKey).clone();

                JDate workingDate = invSecPos.getPositionDate().lte(valDate) ? valDate
                        : invSecPos.getPositionDate();
                clonedInvSecPos.setPositionDate(workingDate);
                if (datesInvSecPositions.get(workingDate) == null) {
                    HashMap<SantGDBookInvSecPosKey, InventorySecurityPosition> newInvSecPositions = new HashMap<SantGDBookInvSecPosKey, InventorySecurityPosition>();
                    newInvSecPositions.put(invSecPosKey, clonedInvSecPos);
                    datesInvSecPositions.put(workingDate, newInvSecPositions);
                } else {
                    datesInvSecPositions.get(workingDate).put(invSecPosKey,
                            clonedInvSecPos);
                }
            } catch (CloneNotSupportedException e) {
                Log.error(GDisponibleUtil.class.getName(), e);
            }
        }
        return datesInvSecPositions;
    }

    public static boolean isValidPosition(InventorySecurityPosition invSecPos) {
        return (Math.abs(invSecPos.getTotalSecurity()) > DELTA_POS)
                || (Math.abs(invSecPos.getTotalPledgedOut()) > DELTA_POS || (Math
                .abs(invSecPos.getTotalUnavailable()) > DELTA_POS));
    }

    /**
     * Get security positions
     */
    private static InventorySecurityPositionArray getInvSecBookPositions(List<Integer> securityIds, List<Integer> bookIds,
                                                                         List<String> productFamily, List<String> positionType,
                                                                         JDatetime valDatetime, int nbDays) {
        InventorySecurityPositionArray invArray = new InventorySecurityPositionArray();
        StringBuffer from = new StringBuffer("product_desc,book");
        StringBuffer where = new StringBuffer();
        where.append("position_date <=");
        where.append(Util.date2SQLString(valDatetime.getJDate(TimeZone.getDefault()).addDays(
                nbDays - 1)));
        if (!Util.isEmpty(positionType)) {
            where.append(" AND position_type in "
                    + Util.collectionToSQLString(positionType));
        }
        where.append(" AND date_type = 'SETTLE'");
        where.append(" AND internal_external = 'INTERNAL'");
        where.append(" AND inv_secposition.book_id = book.book_id");

        if (!Util.isEmpty(bookIds)) {
            where.append(" AND book.book_id in "
                    + Util.collectionToSQLString(bookIds));
        }

//		where.append(" AND book.legal_entity_id = ");
//		where.append(processingOrgId);

        where.append(" AND product_desc.product_id = inv_secposition.security_id");
        if (!Util.isEmpty(productFamily)) {
            where.append(" AND product_desc.product_family in "
                    + Util.collectionToSQLString(productFamily));
        }

        if (!Util.isEmpty(securityIds)) {
            where.append(" AND security_id IN (");

            int idx = 0;
            // split requests by max 999 sec ids
            while (idx <= securityIds.size()) {
                StringBuffer sbSecIds = new StringBuffer();
                sbSecIds.append(where.toString());
                sbSecIds.append(Util.collectionToString(securityIds.subList(
                        idx,
                        (idx + 999) > securityIds.size() ? securityIds.size()
                                : idx + 999)));
                sbSecIds.append(")");
                sbSecIds.append(" ORDER BY inv_secposition.security_id, inv_secposition.book_id, inv_secposition.position_date");
                try {
                    // System.out.println(sbSecIds.toString());
                    InventorySecurityPosition[] invSecPos = DSConnection
                            .getDefault()
                            .getRemoteBO()
                            .getInventorySecurityPositions(from.toString(),
                                    sbSecIds.toString(), null)
                            .getInventorySecurityPositions();
                    invArray.add(invSecPos, invSecPos.length);
                } catch (RemoteException e) {
                    Log.error(GDisponibleUtil.class.getName(), e);
                    return null;
                }
                idx += 999;
            }
        } else {
            where.append(" ORDER BY inv_secposition.security_id, inv_secposition.book_id, inv_secposition.position_date");
            try {
                invArray = DSConnection
                        .getDefault()
                        .getRemoteBO()
                        .getInventorySecurityPositions(from.toString(),
                                where.toString(), null);
            } catch (RemoteException e) {
                Log.error(GDisponibleUtil.class.getName(), e);
                return null;
            }
        }
        return invArray;
    }

    /**
     * Get security positions
     */
    private static InventorySecurityPositionArray getInvSecPosition(
            int processingOrgId, List<Integer> securityIds,
            List<String> productFamily, List<String> positionType,
            JDatetime valDatetime, int nbDays) {
        InventorySecurityPositionArray invArray = new InventorySecurityPositionArray();
        StringBuffer from = new StringBuffer("product_desc,book");
        StringBuffer where = new StringBuffer();
        where.append("position_date <=");
        where.append(Util.date2SQLString(valDatetime.getJDate(TimeZone.getDefault()).addDays(
                nbDays - 1)));
        if (!Util.isEmpty(positionType)) {
            where.append(" AND position_type in "
                    + Util.collectionToSQLString(positionType));
        }
        where.append(" AND date_type = 'SETTLE'");
        where.append(" AND internal_external = 'INTERNAL'");
        where.append(" AND inv_secposition.book_id = book.book_id");

        where.append(" AND book.legal_entity_id = ");
        where.append(processingOrgId);
        where.append(" AND product_desc.product_id = inv_secposition.security_id");
        if (!Util.isEmpty(productFamily)) {
            where.append(" AND product_desc.product_family in "
                    + Util.collectionToSQLString(productFamily));
        }

        if (!Util.isEmpty(securityIds)) {
            where.append(" AND security_id IN (");

            int idx = 0;
            // split requests by max 999 sec ids
            while (idx <= securityIds.size()) {
                StringBuffer sbSecIds = new StringBuffer();
                sbSecIds.append(where.toString());
                sbSecIds.append(Util.collectionToString(securityIds.subList(
                        idx,
                        (idx + 999) > securityIds.size() ? securityIds.size()
                                : idx + 999)));
                sbSecIds.append(")");
                sbSecIds.append(" ORDER BY inv_secposition.security_id, inv_secposition.book_id, inv_secposition.agent_id, inv_secposition.account_id, inv_secposition.position_date");
                try {
                    // System.out.println(sbSecIds.toString());
                    InventorySecurityPosition[] invSecPos = DSConnection
                            .getDefault()
                            .getRemoteBO()
                            .getInventorySecurityPositions(from.toString(),
                                    sbSecIds.toString(), null)
                            .getInventorySecurityPositions();
                    invArray.add(invSecPos, invSecPos.length);
                } catch (RemoteException e) {
                    Log.error(GDisponibleUtil.class.getName(), e);
                    return null;
                }
                idx += 999;
            }
        } else {
            where.append(" ORDER BY inv_secposition.security_id, inv_secposition.book_id, inv_secposition.agent_id, inv_secposition.account_id, inv_secposition.position_date");
            try {
                invArray = DSConnection
                        .getDefault()
                        .getRemoteBO()
                        .getInventorySecurityPositions(from.toString(),
                                where.toString(), null);
            } catch (RemoteException e) {
                Log.error(GDisponibleUtil.class.getName(), e);
                return null;
            }
        }
        return invArray;
    }
}
