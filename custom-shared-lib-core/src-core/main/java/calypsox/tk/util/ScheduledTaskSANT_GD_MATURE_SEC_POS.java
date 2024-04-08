/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.util;

import calypsox.tk.event.PSEventSantGDPosition;
import calypsox.tk.util.gdisponible.GDisponibleUtil;
import calypsox.tk.util.gdisponible.SantGDInvSecPosKey;
import calypsox.util.SantPositionConstants;
import calypsox.util.binding.CustomBindVariablesUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.bo.Task;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectionUtil;
import com.calypso.tk.util.ScheduledTask;
import com.calypso.tk.util.TaskArray;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ScheduledTaskSANT_GD_MATURE_SEC_POS extends ScheduledTask {

    private static final String PRODUCT_FAMILY = "Product Family";
    private static final long serialVersionUID = -1112574572101419165L;
    private static SimpleDateFormat SDF_SHORT = new SimpleDateFormat("dd/MM/yyyy");
    private static SimpleDateFormat SDF_LONG = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.713610");

    @Override
    public String getTaskInformation() {
        return "Publish a PSEventGDPosition event consumed by SantUpdatePositionEngine setting matured security positions to zero";
    }

    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
        attributeList.add(attribute(PRODUCT_FAMILY).domainName("productFamily"));
        return attributeList;
    }

//    @SuppressWarnings("unchecked")
//    @Override
//    public Vector<String> getDomainAttributes() {
//	Vector<String> v = new Vector<String>();
//	v.addElement(PRODUCT_FAMILY);
//	if (!Util.isEmpty(super.getDomainAttributes())) {
//	    v.addAll(super.getDomainAttributes());
//	}
//	return v;
//    }
//
//    /**
//     * @see com.calypso.tk.util.ScheduledTask#isValidInput(java.util.Vector)
//     */
//    @SuppressWarnings({ "unchecked", "rawtypes" })
//    @Override
//    public boolean isValidInput(Vector messages) {
//	boolean ret = super.isValidInput(messages);
//	String productFamily = getAttribute(PRODUCT_FAMILY);
//	if (Util.isEmpty(productFamily)) {
//	    messages.addElement("The product family parameter is missing");
//	    ret = false;
//	}
//	return ret;
//    }

    @Override
    public boolean process(DSConnection ds, PSConnection ps) {
        StringBuffer scheduledTaskExecLogs = new StringBuffer();
        Task task = new Task();
        task.setObjectLongId(getId());
        task.setEventClass(Task.EXCEPTION_EVENT_CLASS);
        task.setNewDatetime(getValuationDatetime());
        task.setUnderProcessingDatetime(getDatetime());
        task.setUndoTradeDatetime(getUndoDatetime());
        task.setDatetime(getDatetime());
        task.setPriority(Task.PRIORITY_NORMAL);
        task.setId(0);
        task.setSource(getType());

        List<String> productFamily = Util.string2Vector(getAttribute(PRODUCT_FAMILY));

        boolean handlingOk = false;
        try {
            loadMaturedSecuritiesAndSendEvents(getValuationDatetime(), productFamily, scheduledTaskExecLogs);

            handlingOk = scheduledTaskExecLogs.toString().length() == 0;
            if (!handlingOk) {
                task.setComment(scheduledTaskExecLogs.toString());
                task.setEventType("EX_" + BOException.EXCEPTION);
            } else {
                task.setEventType("EX_" + BOException.INFORMATION);
            }
            task.setCompletedDatetime(new JDatetime());
            task.setStatus(Task.NEW);

            TaskArray v = new TaskArray();
            v.add(task);
            getReadWriteDS(ds).getRemoteBO().saveAndPublishTasks(v, 0, null);
        } catch (Exception e) {
            Log.error(this, e);
        }
        return handlingOk;
    }

    private void loadMaturedSecuritiesAndSendEvents(JDatetime valDatetime, List<String> productFamily,
                                                    StringBuffer scheduledTaskExecLogs) {
        JDate valDate = valDatetime.getJDate(TimeZone.getDefault());
        StringBuffer whereB = new StringBuffer(
                " product_desc.product_family in (");
        List<CalypsoBindVariable> bindVariables = new ArrayList<>();
        whereB.append(CustomBindVariablesUtil.collectionToPreparedInString(productFamily, bindVariables) + ")");
        //+ Util.collectionToSQLString(productFamily));
        whereB.append(" AND product_desc.maturity_date > "
                + Util.date2SQLString(valDate.addBusinessDays(-1, Util.string2Vector("SYSTEM"))));
        whereB.append(" AND product_desc.maturity_date <= " + Util.date2SQLString(valDate));

        try {
            @SuppressWarnings("unchecked")
            Vector<Product> allSecurities = DSConnection.getDefault().getRemoteProduct().getAllProducts(null,
                    whereB.toString(), bindVariables);

            Vector<Integer> securityIds = new Vector<>();
            for (Product security : allSecurities) {
                securityIds.add(security.getId());
            }
            // securityIds = new Vector<Integer>();
            // securityIds.add(1223302);
            LegalEntity po = getProcessingOrg() == null ? BOCache.getLegalEntity(DSConnection.getDefault(), "BSTE")
                    : getProcessingOrg();
            HashMap<JDate, HashMap<SantGDInvSecPosKey, InventorySecurityPosition>> currentInvSecPositions = GDisponibleUtil
                    .buildSecurityPositionsNbDays(po.getId(), securityIds, productFamily,
                            Arrays.asList("ACTUAL", "THEORETICAL"), valDatetime, 1);
            sendGDEvents(valDate, currentInvSecPositions);
        } catch (Exception e) {
            scheduledTaskExecLogs.append(e.getMessage());
            Log.error(this, e);
        }
    }

    private void sendGDEvents(JDate valDate,
                              HashMap<JDate, HashMap<SantGDInvSecPosKey, InventorySecurityPosition>> currentInvSecPositions)
            throws RemoteException {
        List<SantGDInvSecPosKey> sentKeys = new ArrayList<SantGDInvSecPosKey>();
        if (currentInvSecPositions != null) {
            for (SantGDInvSecPosKey key : currentInvSecPositions.get(valDate).keySet()) {
                InventorySecurityPosition invSecPos = currentInvSecPositions.get(valDate).get(key);
                if ((invSecPos != null) && GDisponibleUtil.isValidPosition(invSecPos)) {
                    // test if already sent, position type is not relevant
                    key.setPositionType("**ANY**");
                    if (!sentKeys.contains(key)) {
                        PSEventSantGDPosition event = new PSEventSantGDPosition(
                                buildGDExternalMessage(invSecPos, valDate));
                        System.out.println(buildGDExternalMessage(invSecPos, valDate));
                        DSConnection.getDefault().getRemoteTrade().saveAndPublish(event);
                        sentKeys.add(key);
                    }
                }
            }
        }
    }

    private String buildGDExternalMessage(InventorySecurityPosition invSecPos, JDate valDate) {
        StringBuffer externalMsg = new StringBuffer();
        try {
            // BLOQUEO
            addPositionZero(externalMsg, invSecPos, SantPositionConstants.BLOQUEO_MAPPING, valDate);
            // ACTUAL
            addPositionZero(externalMsg, invSecPos, SantPositionConstants.ACTUAL_MAPPING, valDate);
            // THEORETICAL
            addPositionZero(externalMsg, invSecPos, SantPositionConstants.THEORETICAL_MAPPING, valDate);
        } catch (RemoteException e) {
            Log.error(ScheduledTaskSANT_GD_MATURE_SEC_POS.class.getName(), e);
        }
        return externalMsg.toString();
    }

    private void addPositionZero(StringBuffer externalMsg, InventorySecurityPosition invSecPos, String positionType,
                                 JDate valDate) throws RemoteException {
        // 1 C?digo ISIN X(12) VARCHAR2(12)
        // 2 C?digo Divisa X(03) VARCHAR2(3)
        // 3 C?digo Portfolio X(15) VARCHAR2(32)
        // 4 Nemot?cnico Custodio X(06) VARCHAR2(32)
        // 5 C?digo Cuenta Custodio X(35) VARCHAR2(35)
        // 6 Estado X(07) VARCHAR2(32)
        // 7 N? de T?tulos S9(15) Amount (32)
        // 8 Fecha valor posici?n X(10) X(10)
        // 9 Timestamp envio X(26) X(26)
        // 10. Modo Env?o X(01) X(01)
        Product product = DSConnection.getDefault().getRemoteProduct().getProduct(invSecPos.getSecurityId());
        if (product != null) {
            // ISIN
            externalMsg.append(product.getSecCode("ISIN"));
            externalMsg.append(SantPositionConstants.RESPONSE_SEPARATOR);
            // Currency
            externalMsg.append(product.getCurrency());
            externalMsg.append(SantPositionConstants.RESPONSE_SEPARATOR);
            // Book
            externalMsg.append(BOCache.getBook(DSConnection.getDefault(), invSecPos.getBookId()));
            externalMsg.append(SantPositionConstants.RESPONSE_SEPARATOR);
            // Agent
            externalMsg.append(BOCache.getLegalEntityCode(DSConnection.getDefault(), invSecPos.getAgentId()));
            externalMsg.append(SantPositionConstants.RESPONSE_SEPARATOR);
            // Account
            externalMsg.append(BOCache.getAccount(DSConnection.getDefault(), invSecPos.getAccountId()).getName());
            externalMsg.append(SantPositionConstants.RESPONSE_SEPARATOR);
            // Position Type
            externalMsg.append(positionType);
            externalMsg.append(SantPositionConstants.RESPONSE_SEPARATOR);
            // Position value
            externalMsg.append(0);
            externalMsg.append(SantPositionConstants.RESPONSE_SEPARATOR);
            // Position date
            externalMsg.append(SDF_SHORT.format(valDate.getDate(TimeZone.getDefault())));
            externalMsg.append(SantPositionConstants.RESPONSE_SEPARATOR);
            // Timestamp
            externalMsg.append(SDF_LONG.format(valDate.getDate(TimeZone.getDefault())));
            externalMsg.append(SantPositionConstants.RESPONSE_SEPARATOR);
            // Sending mode
            externalMsg.append("0");
            // End line
            externalMsg.append("\n");
        }
    }

    public static void main(String[] args) {
        DSConnection ds = null;
        try {
            // Starts connection to DataServer.
            ds = ConnectionUtil.connect(args, "ScheduledTaskSantGDMatureSecurityPosition");

            ScheduledTaskSANT_GD_MATURE_SEC_POS st = new ScheduledTaskSANT_GD_MATURE_SEC_POS();
            st.setAttribute(PRODUCT_FAMILY, "Bond");
            st.setProcessingOrg(BOCache.getLegalEntity(DSConnection.getDefault(), "BSTE"));
            st.setDatetime(JDate.getNow().getJDatetime(TimeZone.getDefault()));
            st.setCurrentDate(JDate.getNow());
            st.setValuationTime(2359);

            st.process(ds, null);

        } catch (Exception e) {
            Log.error(Log.CALYPSOX, e);
            return;
        } finally {
            DSConnection.logout();
            System.exit(0);
        }
    }
}
